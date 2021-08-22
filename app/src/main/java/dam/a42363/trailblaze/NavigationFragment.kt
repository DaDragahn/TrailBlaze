package dam.a42363.trailblaze

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.os.Looper.getMainLooper
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.route.NavigationMapRoute
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import dam.a42363.trailblaze.databinding.FragmentNavigationBinding
import dam.a42363.trailblaze.utils.Constants
import timber.log.Timber
import java.lang.ref.WeakReference
import kotlin.properties.Delegates
import android.content.Context.SENSOR_SERVICE
import android.content.SharedPreferences

import androidx.core.content.ContextCompat.getSystemService
import kotlin.math.sqrt


class NavigationFragment : Fragment(), OnMapReadyCallback, PermissionsListener,
    SensorEventListener {

    private var timeWhenStopped: Long = 0
    private lateinit var mapView: MapView
    private var mapboxMap: MapboxMap? = null
    private var mapCamera: NavigationCamera? = null
    private var navigationMapRoute: NavigationMapRoute? = null
    private var mapboxNavigation: MapboxNavigation? = null
    private lateinit var locationComponent: LocationComponent

    private lateinit var navController: NavController

    private lateinit var auth: FirebaseAuth

    //    private val mapboxReplayer = MapboxReplayer()
    private var check by Delegates.notNull<Boolean>()
    private lateinit var permissionsManager: PermissionsManager
    private var _binding: FragmentNavigationBinding? = null
    private lateinit var optimizedRoute: DirectionsRoute
    private val binding get() = _binding!!

    private val DEFAULT_INTERVAL_IN_MILLISECONDS = 2000L
    private val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5

    private val db = FirebaseFirestore.getInstance()
    private val ICON_GEOJSON_SOURCE_ID = "icon-source-id"

    private lateinit var slidingUpPanelLayout: SlidingUpPanelLayout
    private var destroy: Boolean = false
    private var idTrail: String? = null
    private val lobbyArray = ArrayList<String>()

    private var sensorManager: SensorManager? = null
    private var sensor: Sensor? = null

    private val magnitudePrevious = 0.0f
    private var stepCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            binding.sairCardView.visibility = View.VISIBLE
            binding.sairBtn.setOnClickListener {
                db.collection("Trails").document(idTrail!!).collection("TrailsCollection")
                    .document(auth.uid!!).delete()
                navController.navigate(R.id.action_navigationFragment_to_explorarFragment)
            }
//            binding.voltarBtn.setOnClickListener {
//                binding.sairCardView.visibility = View.GONE
//            }
        }

        callback.isEnabled
    }

    @SuppressLint("LogNotTimber")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Mapbox.getInstance(requireContext(), getString(R.string.mapbox_access_token))
        if (_binding != null) {
            binding.chronometer.base = SystemClock.elapsedRealtime() + timeWhenStopped
            binding.chronometer.start()
            return binding.root
        }
        _binding = FragmentNavigationBinding.inflate(inflater, container, false)
        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        slidingUpPanelLayout = binding.slidingLayout

        slidingUpPanelLayout.shadowHeight = 0

        slidingUpPanelLayout.addPanelSlideListener(object :
            SlidingUpPanelLayout.PanelSlideListener {
            override fun onPanelSlide(panel: View?, slideOffset: Float) {
                Log.i(TAG, "onPanelSlide, offset $slideOffset")
            }

            override fun onPanelStateChanged(
                panel: View?,
                previousState: PanelState?,
                newState: PanelState
            ) {
                Log.i(TAG, "onPanelStateChanged $newState")
            }
        })
        slidingUpPanelLayout.setFadeOnClickListener {
            slidingUpPanelLayout.panelState = PanelState.COLLAPSED
        }

        optimizedRoute = DirectionsRoute.fromJson(arguments?.getString("route")!!)
        idTrail = arguments?.getString("idTrail")
        check = arguments?.getBoolean("individual")!!
        Log.v("RecordRoute", idTrail.toString())
        Log.v("RecordRoute", "$optimizedRoute")

        binding.chronometer.start()

        sensorManager = activity?.getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        if (activity != null && this.activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.GONE
        }

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user == null && auth.uid == null) {
            navController.navigate(R.id.action_navigationFragment_to_signInFragment)
        }

        binding.cameraBtn.setOnClickListener {

            if (allPermissionGranted()) {
//                Toast.makeText(requireContext(), "We have Permission", Toast.LENGTH_SHORT)
//                    .show()
                timeWhenStopped = binding.chronometer.base - SystemClock.elapsedRealtime()
                binding.chronometer.stop()
                val bundle = bundleOf("idTrail" to idTrail)
                navController.navigate(
                    R.id.action_navigationFragment_to_cameraFragment,
                    bundle
                )

            } else {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    Constants.REQUIRED_PERMISSIONS,
                    Constants.REQUEST_CODE
                )
            }
        }
        binding.voltarBtn.setOnClickListener {
            val chronoText = binding.chronometer.text
            binding.chronometer.stop()
            val bundle =
                bundleOf("idTrail" to idTrail, "time" to chronoText)
            navController.navigate(
                R.id.action_navigationFragment_to_terminarFragment,
                bundle
            )
        }

        binding.terminarBtn.setOnClickListener {
            val chronoText = binding.chronometer.text
            binding.chronometer.stop()
            val bundle =
                bundleOf("idTrail" to idTrail, "time" to chronoText)
            navController.navigate(
                R.id.action_navigationFragment_to_terminarFragment,
                bundle
            )
        }
    }

    private fun allPermissionGranted() =
        Constants.REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                requireActivity().baseContext,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }


    @SuppressLint("MissingPermission")
    fun onStartNavigation() {
        locationComponent.cameraMode = CameraMode.TRACKING_GPS
        locationComponent.renderMode = RenderMode.GPS
        mapCamera?.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
        mapCamera?.start(optimizedRoute)
        mapboxNavigation?.startTripSession()
    }

    private fun initMarkerIconSymbolLayer(loadedMapStyle: Style) {
        // Add the marker image to map
        loadedMapStyle.addImage(
            "icon-image", BitmapFactory.decodeResource(
                this.resources, R.drawable.mapbox_marker_icon_default
            )
        )

        // Add the source to the map
        loadedMapStyle.addSource(
            GeoJsonSource(
                ICON_GEOJSON_SOURCE_ID
            )
        )
        loadedMapStyle.addLayer(
            SymbolLayer("icon-layer-id", ICON_GEOJSON_SOURCE_ID).withProperties(
                PropertyFactory.iconImage("icon-image"),
                PropertyFactory.iconSize(1f),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconOffset(arrayOf(0f, -7f))
            )
        )
    }

    @SuppressLint("LogNotTimber", "MissingPermission")
    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap

        mapboxMap.setStyle(
            Style.OUTDOORS
        ) { style ->
            val uiSettings = mapboxMap.uiSettings
            uiSettings.setCompassMargins(0, 200, 50, 0)
            uiSettings.setCompassFadeFacingNorth(false)
            enableLocationComponent(style)
            initMarkerIconSymbolLayer(style)
            val navigationOptions = MapboxNavigation
                .defaultNavigationOptionsBuilder(requireContext(), Mapbox.getAccessToken())
//                .locationEngine(ReplayLocationEngine(mapboxReplayer))
                .build()
            mapboxNavigation = MapboxNavigation(navigationOptions)
//            mapboxNavigation!!.registerRouteProgressObserver(replayProgressObserver)
            mapboxNavigation!!.registerArrivalObserver(arrivalObserver)
//            mapboxReplayer.pushRealLocation(requireContext(), 0.0)
//            mapboxReplayer.play()

            mapCamera = NavigationCamera(mapboxMap)
            mapCamera?.addProgressChangeListener(mapboxNavigation!!)
            navigationMapRoute =
                NavigationMapRoute.Builder(mapView, mapboxMap, viewLifecycleOwner)
                    .withMapboxNavigation(mapboxNavigation)
                    .withVanishRouteLineEnabled(true)
                    .build()

            val locationEngineRequest =
                LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                    .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                    .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME) // sets the maximum wait time in milliseconds for location updates. Locations determined at intervals but delivered in batch based on wait time. Batching is not supported by all engines.
                    .build()

            mapboxNavigation!!.navigationOptions.locationEngine.requestLocationUpdates(
                locationEngineRequest, locationEngineCallback,
                getMainLooper()
            )

            mapboxNavigation!!.navigationOptions.locationEngine.getLastLocation(
                locationEngineCallback
            )
            val routes: ArrayList<DirectionsRoute> = ArrayList()
            routes.add(optimizedRoute)
            Log.v("RecordRoute", "$routes[0]")
            navigationMapRoute?.addRoutes(routes)
            mapboxNavigation!!.setRoutes(routes)
            onStartNavigation()
        }

    }

    private fun addUserMarkers(style: Style) {
        val userMarkerList: ArrayList<Feature> = ArrayList()
        if (lobbyArray.isNotEmpty()) {
            db.collection("Trails").document(idTrail!!).collection("TrailsCollection")
                .whereIn(FieldPath.documentId(), lobbyArray).get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        userMarkerList.add(
                            Feature.fromGeometry(
                                Point.fromJson(document.getString("LastLocation")!!)
                            )
                        )
                    }
                    val iconSource =
                        style.getSourceAs<GeoJsonSource>(ICON_GEOJSON_SOURCE_ID)
                    iconSource?.setGeoJson(FeatureCollection.fromFeatures(userMarkerList))
                }
                .addOnFailureListener { exception ->
                    Timber.tag("TAG").d(exception, "Error getting documents: ")
                }
        }
//        val iconSource = style.getSourceAs<GeoJsonSource>(ICON_GEOJSON_SOURCE_ID)
//        iconSource?.setGeoJson(featureCollection)
    }

    private fun checkLobby(updates: MutableMap<String, Any>) {
        if (check == null && !check) {
            if (idTrail != null) {
                db.collection("Trails").document(idTrail!!).collection("TrailsCollection")
                    .document(auth.uid!!).update(updates)

                db.collection("Trails").document(idTrail!!).collection("TrailsCollection")
                    .get()
                    .addOnSuccessListener { result ->
                        for (document in result) {
                            if (document.id != auth.uid!!)
                                lobbyArray.add(document.id)
                        }
                        addUserMarkers(mapboxMap?.style!!)
                    }
            }
        }
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(
            requireContext(),
            R.string.user_location_permission_explanation,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            mapboxMap?.setStyle(Style.OUTDOORS) { style ->
                val uiSettings = mapboxMap!!.uiSettings
                uiSettings.setCompassMargins(0, 300, 50, 0)
                uiSettings.setCompassFadeFacingNorth(false)
                enableLocationComponent(style)
            }
        } else {
            Toast.makeText(
                requireContext(),
                R.string.user_location_permission_not_granted,
                Toast.LENGTH_LONG
            ).show()
            requireActivity().finish()
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(requireContext())) {

            // Get an instance of the component
            locationComponent = mapboxMap?.locationComponent!!

            // Set the LocationComponent activation options
            val locationComponentActivationOptions =
                LocationComponentActivationOptions.builder(requireContext(), loadedMapStyle)
                    .useDefaultLocationEngine(false)
                    .build()

            // Activate with the LocationComponentActivationOptions object
            locationComponent.activateLocationComponent(locationComponentActivationOptions)

            // Enable to make component visible
            locationComponent.isLocationComponentEnabled = true

            // Set the component's camera mode
            locationComponent.cameraMode = CameraMode.TRACKING

            // Set the component's render mode
            locationComponent.renderMode = RenderMode.COMPASS

            binding.recenterBtn.setOnClickListener {
                locationComponent.setCameraMode(
                    CameraMode.TRACKING_GPS,
                    750L,
                    15.0,
                    null,
                    0.0,
                    null
                )
                locationComponent.zoomWhileTracking(15.0)
            }

        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(requireActivity())
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {

        if (event != null) {
            Toast.makeText(requireContext(), "ENTERED", Toast.LENGTH_SHORT).show()
            val xAcceleration = event.values[0]
            val yAcceleration = event.values[1]
            val zAcceleration = event.values[2]

            val magnitude =
                sqrt((xAcceleration * xAcceleration + yAcceleration * yAcceleration + zAcceleration * zAcceleration).toDouble())

            val magnitudeDelta = magnitude - magnitudePrevious

            if (magnitudeDelta > 3) {
                stepCount++
            }

            binding.totalPassos.text = "$stepCount"

            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)

        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        mapboxNavigation?.registerArrivalObserver(arrivalObserver)
        mapCamera?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()

        val sharedPreferences: SharedPreferences? = activity?.getPreferences(Context.MODE_PRIVATE)
        stepCount = sharedPreferences!!.getInt("stepCount", 0)


    }

    override fun onStop() {
        super.onStop()
        mapCamera?.onStop()
        mapboxNavigation?.unregisterArrivalObserver(arrivalObserver)
        mapboxNavigation?.navigationOptions?.locationEngine?.removeLocationUpdates(
            locationEngineCallback
        )
        mapView.onStop()

        val sharedPreferences: SharedPreferences? = activity?.getPreferences(Context.MODE_PRIVATE)

        val editor: SharedPreferences.Editor? = sharedPreferences?.edit()
        editor?.clear()
        editor?.putInt("stepCount", stepCount)
        editor?.apply()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()

        val sharedPreferences: SharedPreferences? = activity?.getPreferences(Context.MODE_PRIVATE)

        val editor: SharedPreferences.Editor? = sharedPreferences?.edit()
        editor?.clear()
        editor?.putInt("stepCount", stepCount)
        editor?.apply()
    }

    override fun onDestroy() {
        mapView.onDestroy()
//        mapboxNavigation?.unregisterRouteProgressObserver(replayProgressObserver)
        mapboxNavigation?.unregisterArrivalObserver(arrivalObserver);
        mapboxNavigation?.stopTripSession()
        mapboxNavigation?.onDestroy()
        db.collection("Trails").document(idTrail!!).collection("TrailsCollection")
            .document(auth.uid!!).delete()
        super.onDestroy()
    }

    override fun onDestroyView() {
        if (destroy) {
            mapView.onDestroy()
//        mapboxNavigation?.unregisterRouteProgressObserver(replayProgressObserver)
            mapboxNavigation?.unregisterArrivalObserver(arrivalObserver);
            mapboxNavigation?.stopTripSession()
            mapboxNavigation?.onDestroy()
            binding.chronometer.stop()
        }
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mapView.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    private val locationEngineCallback = MyLocationEngineCallback(this)

    class MyLocationEngineCallback(fragment: NavigationFragment?) :
        LocationEngineCallback<LocationEngineResult> {

        private var fragmentWeakReference: WeakReference<NavigationFragment> =
            WeakReference(fragment)

        override fun onSuccess(result: LocationEngineResult) {
            val fragment: NavigationFragment? = fragmentWeakReference.get()
            if (fragment != null) {
                fragment.updateLocation(result.locations)
                val updates: MutableMap<String, Any> =
                    HashMap()
                val latitude = result.lastLocation!!.latitude
                val longitude = result.lastLocation!!.longitude
                updates["LastLocation"] = Point.fromLngLat(longitude, latitude).toJson()
                fragment.clearVariables()
                fragment.checkLobby(updates)
            }
        }

        override fun onFailure(exception: Exception) {
            Timber.i(exception)
        }

    }

    private fun clearVariables() {
        lobbyArray.clear()
//        val iconSource = mapboxMap?.style!!.getSourceAs<GeoJsonSource>(ICON_GEOJSON_SOURCE_ID)
//        iconSource?.setGeoJson(FeatureCollection.fromFeatures(listOf()))
    }

    private fun updateLocation(locations: List<Location>) {
        locationComponent.forceLocationUpdate(locations, false)
    }

//    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)

    private var arrivalObserver: ArrivalObserver = object : ArrivalObserver {
        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {

        }

        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            binding.terminarCardView.visibility = View.VISIBLE
        }
    }

//    private val routeProgressObserver = object : RouteProgressObserver {
//        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
//            try {
//                routeProgress.currentState.let { currentState ->
//                    val state = currentState
//                    Log.e("State", state.toString())
//                    if (state == RouteProgressState.ROUTE_COMPLETE && routeProgress.distanceRemaining < 10) {
//                        Toast.makeText(
//                            requireContext(),
//                            "You have arrived at your destination!",
//                            Toast.LENGTH_LONG
//                        ).show()
//                    } else if (state == RouteProgressState.OFF_ROUTE) {
//                        Log.e("< <OFF> >", " off route > > > > > > > > > > > > > > > > > > >")
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e("Nav Err", e.message!!)
//            }
//        }
//    }
//
}