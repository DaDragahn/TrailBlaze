package dam.a42363.trailblaze

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
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
import dam.a42363.trailblaze.databinding.FragmentExplorarBinding
import java.util.*


class ExplorarFragment : Fragment(), OnMapReadyCallback, PermissionsListener,
    MapboxMap.OnMapClickListener {

    var _binding: FragmentExplorarBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap

    private var permissionsManager: PermissionsManager? = null

    private val ICON_GEOJSON_SOURSE_ID = "icon-source-id"
    private val ICON_GEOJSON_LAYER_ID = "icon-layer-id"

    private var locationComponent: LocationComponent? = null

    private lateinit var cardView: CardView

    private lateinit var db: FirebaseFirestore
    private lateinit var navController: NavController
    private var doubleBackToExitPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val backCallback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (doubleBackToExitPressedOnce) {
                activity?.finishAndRemoveTask()          // Handle the back button event
            }

            doubleBackToExitPressedOnce = true
            Toast.makeText(requireContext(), "Please click BACK again to exit", Toast.LENGTH_SHORT)
                .show()

            Handler(Looper.getMainLooper()).postDelayed(
                Runnable { doubleBackToExitPressedOnce = false },
                2000
            )
        }

        backCallback.isEnabled
    }

    @SuppressLint("UseCompatLoadingForColorStateLists")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        Mapbox.getInstance(requireContext(), getString(R.string.mapbox_access_token))
        _binding = FragmentExplorarBinding.inflate(inflater, container, false)

        mapView = binding.mapView

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        cardView = binding.cardViewInfo

        db = FirebaseFirestore.getInstance()

        binding.iniciarBtn.backgroundTintList =
            view?.resources?.getColorStateList(R.color.trailGreen)

        binding.dirBtn.backgroundTintList =
            view?.resources?.getColorStateList(R.color.orange)


        // Inflate the layout for this fragment
        return binding.root
//        inflater.inflate(R.layout.fragment_explorar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)


        cardView.setBackgroundResource(R.drawable.cardview_info)

        if (activity != null && this.activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.VISIBLE
            cardView.visibility = View.GONE
        }

        binding.lista.setOnClickListener {
            val local = Point.fromLngLat(
                locationComponent?.lastKnownLocation!!.longitude,
                locationComponent?.lastKnownLocation!!
                    .latitude
            )
            val bundle = bundleOf(
                "local" to local.toJson()
            )
            navController.navigate(R.id.action_explorarFragment_to_explorarListFragment, bundle)

        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.OUTDOORS) { style ->
            val uiSettings = mapboxMap.uiSettings
            uiSettings.setCompassMargins(0, 200, 50, 0)
            uiSettings.setCompassFadeFacingNorth(false)
            enableLocationComponent(style)
            initMarkerIconSymbolLayer(style)
            Log.d(
                "TESTE",
                locationComponent?.lastKnownLocation?.latitude.toString()
            )
            checkGeoQuery(style)
            mapboxMap.addOnMapClickListener(this)
        }
    }

    private fun checkGeoQuery(style: Style) {
        val markerList: MutableList<Feature> = ArrayList()
        val center = GeoLocation(
            locationComponent?.lastKnownLocation!!.latitude,
            locationComponent!!.lastKnownLocation!!
                .longitude
        )
        val radiusInM = (2 * 1000).toDouble()
        val bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInM)
        val tasks: MutableList<Task<QuerySnapshot>> = ArrayList()
        for (b in bounds) {
            val q = db.collection("locations")
                .orderBy("geohash")
                .startAt(b.startHash)
                .endAt(b.endHash)
            tasks.add(q.get())
        }

        // Collect all the query results together into a single list
        Tasks.whenAllComplete(tasks)
            .addOnCompleteListener {
                val matchingDocs: MutableList<DocumentSnapshot> =
                    ArrayList()
                for (task in tasks) {
                    val snap = task.result
                    for (doc in snap!!.documents) {
                        val activeRoute = DirectionsRoute.fromJson(doc.getString("route")!!)
                        val optimizedRoute =
                            LineString.fromPolyline(activeRoute.geometry()!!, Constants.PRECISION_6)
                        val lat = optimizedRoute.coordinates().first().latitude()
                        val lng = optimizedRoute.coordinates().first().longitude()

                        // We have to filter out a few false positives due to GeoHash
                        // accuracy, but most will match
                        val docLocation = GeoLocation(lat, lng)
                        val distanceInM =
                            GeoFireUtils.getDistanceBetween(docLocation, center)
                        if (distanceInM <= radiusInM) {
                            matchingDocs.add(doc)
                        }
                    }
                }
                for (docSnap in matchingDocs) {
                    val activeRoute = DirectionsRoute.fromJson(docSnap.getString("route")!!)
                    val optimizedRoute =
                        LineString.fromPolyline(activeRoute.geometry()!!, Constants.PRECISION_6)
                    val lat = optimizedRoute.coordinates().first().latitude()
                    val lng = optimizedRoute.coordinates().first().longitude()
                    val marker = Point.fromLngLat(lng, lat)
                    markerList.add(
                        Feature.fromGeometry(
                            Point.fromLngLat(marker.longitude(), marker.latitude()),
                            null,
                            docSnap.id
                        )
                    )
                }
                mapboxMap.getStyle {
                    val iconSource = it.getSourceAs<GeoJsonSource>(ICON_GEOJSON_SOURSE_ID)
                    iconSource?.setGeoJson(FeatureCollection.fromFeatures(markerList))
                }
            }
    }

    private fun initMarkerIconSymbolLayer(style: Style) {
        // Add the marker image to map
        style.addImage(
            "icon-image", BitmapFactory.decodeResource(
                this.resources, R.drawable.mapbox_marker_icon_default
            )
        )
        // Add the source to the map
        style.addSource(
            GeoJsonSource(
                ICON_GEOJSON_SOURSE_ID
            )
        )
        style.addLayer(
            SymbolLayer(ICON_GEOJSON_LAYER_ID, ICON_GEOJSON_SOURSE_ID).withProperties(
                PropertyFactory.iconImage("icon-image"),
                PropertyFactory.iconSize(1f),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true)
            )
        )
    }

    @SuppressLint("LogNotTimber", "SetTextI18n")
    override fun onMapClick(point: LatLng): Boolean {
        val screenPoint = mapboxMap.projection.toScreenLocation(point)
//        Toast.makeText(
//            requireContext(), screenPoint.y.toString(),
//            Toast.LENGTH_SHORT
//        ).show()
        val featuresList: List<Feature> =
            mapboxMap.queryRenderedFeatures(screenPoint, ICON_GEOJSON_LAYER_ID)
        if (featuresList.isNotEmpty()) {
            for (feature in featuresList) {
                Log.v("Geoquery", feature.toJson())
                db.collection("locations").document("${feature.id()}").get().addOnCompleteListener {
                    if (it.isSuccessful) {
                        val document = it.result
                        val route = document?.getString("route")

                        binding.nome.text = document?.getString("nome")
                        binding.localidade.text = document?.getString("localidade")
                        binding.distancia.text = "Distância: ${document?.getString("distancia")}"
                        binding.dificuldade.text =
                            "Dificuldade: ${document?.getString("dificuldade")}"

                        (activity as MainActivity).bottomNavigationView?.visibility = View.GONE
                        cardView.visibility = View.VISIBLE

                        binding.dirBtn.setOnClickListener {
                            val bundle = bundleOf("feature" to feature.id())
                            navController.navigate(
                                R.id.action_explorarFragment_to_fullInfoFragment,
                                bundle
                            )
                        }
                        binding.iniciarBtn.setOnClickListener {
                            val bundle = bundleOf("route" to route)
                            navController.navigate(
                                R.id.action_explorarFragment_to_navigationFragment,
                                bundle
                            )
                        }
                    }
                }
            }
            return true
        }
        if (screenPoint.y < 1130) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.VISIBLE
            cardView.visibility = View.GONE
        }
        return false
    }

    @SuppressWarnings("MissingPermission")
    private fun enableLocationComponent(style: Style) {

        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(requireContext())) {

            // Get an instance of the component
            locationComponent = mapboxMap.locationComponent

            // Activate with a built LocationComponentActivationOptions object
            locationComponent!!.activateLocationComponent(
                LocationComponentActivationOptions.builder(
                    requireContext(),
                    style
                ).build()
            )

            Log.d(
                "TESTE",
                locationComponent?.lastKnownLocation?.latitude.toString()
            )

            // Enable to make component visible
            locationComponent!!.isLocationComponentEnabled = true

            // Set the component's camera mode
            locationComponent!!.cameraMode = CameraMode.TRACKING

            // Set the component's render mode
            locationComponent!!.renderMode = RenderMode.COMPASS

            binding.recenterBtn.setOnClickListener {
                locationComponent!!.setCameraMode(
                    CameraMode.TRACKING_GPS,
                    750L,
                    15.0,
                    null,
                    0.0,
                    null
                )
                locationComponent!!.zoomWhileTracking(15.0)
            }
        } else {
            permissionsManager = PermissionsManager(this)

            permissionsManager?.requestLocationPermissions(requireActivity())

        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDestroy()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            mapboxMap.setStyle(Style.OUTDOORS) { style ->
                val uiSettings = mapboxMap.uiSettings
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

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        TODO("Not yet implemented")
    }


}