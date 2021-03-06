package projeto.g54.trailblaze

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper.getMainLooper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.mapbox.android.core.location.*
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
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
import projeto.g54.trailblaze.databinding.FragmentContribuirBinding
import projeto.g54.trailblaze.utils.Constants
import timber.log.Timber
import java.lang.ref.WeakReference

class ContribuirFragment : Fragment(), OnMapReadyCallback, PermissionsListener {
    private lateinit var storageRef: StorageReference
    private lateinit var idTrail: String
    private var _binding: FragmentContribuirBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap

    private var permissionsManager: PermissionsManager? = null

    private lateinit var locationComponent: LocationComponent
    private var locationEngine: LocationEngine? = null
    private val DEFAULT_INTERVAL_IN_MILLISECONDS = 20000L
    private val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5

    private lateinit var db: FirebaseFirestore
    private lateinit var navController: NavController

    private lateinit var auth: FirebaseAuth

    private lateinit var recordButton: FloatingActionButton
    private lateinit var playButton: FloatingActionButton
    private lateinit var pauseButton: FloatingActionButton
    private lateinit var stopButton: FloatingActionButton
    private lateinit var chronometerHolder: CardView
    private lateinit var chronometer: Chronometer
    private lateinit var actionText: TextView

    private var timeWhenStopped: Long = 0
    var isRecording: Boolean = false
    var isPlaying: Boolean = false
    private var isDestroy: Boolean = true
    var routeCoordinates: ArrayList<Point> = ArrayList()

    private val callback: TrackingLocationCallback =
        TrackingLocationCallback(this)
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

            Handler(getMainLooper()).postDelayed(
                { doubleBackToExitPressedOnce = false },
                2000
            )
        }

        backCallback.isEnabled
    }

    @SuppressLint("ResourceAsColor")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (_binding != null) {
            isPlaying = true
            isDestroy = true
            pauseButton.visibility = View.VISIBLE
            playButton.visibility = View.GONE
            chronometer.base = SystemClock.elapsedRealtime() + timeWhenStopped
            chronometer.start()
            return binding.root
        }
        Mapbox.getInstance(requireContext(), getString(R.string.mapbox_access_token))
        _binding = FragmentContribuirBinding.inflate(inflater, container, false)

        mapView = binding.mapView

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        db = FirebaseFirestore.getInstance()
        storageRef = FirebaseStorage.getInstance().reference
        recordButton = binding.recordBtn
        playButton = binding.playBtn
        pauseButton = binding.pauseBtn
        stopButton = binding.stopBtn
        chronometerHolder = binding.chronometerHolder
        chronometer = binding.chronometer
        actionText = binding.actionText

        idTrail = db.collection("locations").document().id

        binding.cameraBtn.setOnClickListener {

            if (allPermissionGranted()) {
                timeWhenStopped = binding.chronometer.base - SystemClock.elapsedRealtime()
                binding.chronometer.stop()
                isPlaying = false
                isDestroy = false
                val bundle = bundleOf("idTrail" to idTrail)
                navController.navigate(R.id.action_contribuirFragment_to_cameraFragment, bundle)

            } else {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    Constants.REQUIRED_PERMISSIONS,
                    Constants.REQUEST_CODE
                )
            }
        }

        recordButton.setOnClickListener {
            isRecording = true
            isPlaying = true
            binding.cameraBtn.visibility = View.VISIBLE
            recordButton.visibility = View.GONE
            pauseButton.visibility = View.VISIBLE
            stopButton.visibility = View.VISIBLE
            chronometerHolder.visibility = View.VISIBLE
            chronometer.base = SystemClock.elapsedRealtime() + timeWhenStopped
            chronometer.start()
            routeCoordinates.clear()
            routeCoordinates.add(
                Point.fromLngLat(
                    locationComponent.lastKnownLocation!!.longitude,
                    locationComponent.lastKnownLocation!!
                        .latitude
                )
            )
        }

        pauseButton.setOnClickListener {
            isPlaying = false
            actionText.setText(R.string.pausar)
            playButton.visibility = View.VISIBLE
            pauseButton.visibility = View.GONE
            timeWhenStopped = chronometer.base - SystemClock.elapsedRealtime()
            chronometer.stop()
        }

        playButton.setOnClickListener {
            isPlaying = true
            actionText.setText(R.string.a_gravar)
            pauseButton.visibility = View.VISIBLE
            playButton.visibility = View.GONE
            chronometer.base = SystemClock.elapsedRealtime() + timeWhenStopped
            chronometer.start()
        }
        stopButton.setOnClickListener {
            isRecording = false
            isPlaying = false
            chronometer.base = SystemClock.elapsedRealtime()
            timeWhenStopped = 0
            routeCoordinates.add(
                Point.fromLngLat(
                    locationComponent.lastKnownLocation!!.longitude,
                    locationComponent.lastKnownLocation!!.latitude
                )
            )

            val lineString: LineString = LineString.fromLngLats(routeCoordinates)

            val feature = Feature.fromGeometry(lineString)

            val featureJson = FeatureCollection.fromFeature(feature).toJson()

            locationEngine?.removeLocationUpdates(callback)

            val bundle = bundleOf(
                "routeCoordinates" to featureJson, "idTrail" to idTrail
            )
            isDestroy = false
            navController.navigate(R.id.action_contribuirFragment_to_partilharFragment, bundle)
        }


        return binding.root
    }

    private fun allPermissionGranted() =
        Constants.REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                requireActivity().baseContext,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user == null && auth.uid == null) {
            navController.navigate(R.id.action_contribuirFragment_to_signInFragment)
        } else {

            if (activity != null && this.activity is MainActivity) {
                (activity as MainActivity).bottomNavigationView?.visibility = View.VISIBLE
            }
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap

        mapboxMap.setStyle(
            Style.OUTDOORS
        ) { style ->
            val uiSettings = mapboxMap.uiSettings
            uiSettings.setCompassMargins(0, 200, 50, 0)
            uiSettings.setCompassFadeFacingNorth(false)
            enableLocationComponent(style)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(requireContext())) {

            // Get an instance of the component
            locationComponent = mapboxMap.locationComponent
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

            initLocationEngine()
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager!!.requestLocationPermissions(requireActivity())
        }
    }

    @SuppressLint("MissingPermission")
    private fun initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(requireContext())
        val request = LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build()
        locationEngine!!.requestLocationUpdates(request, callback, getMainLooper())
        locationEngine!!.getLastLocation(callback)
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
            if (mapboxMap.style != null) {
                enableLocationComponent(mapboxMap.style!!)
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

    private class TrackingLocationCallback(fragment: ContribuirFragment?) :
        LocationEngineCallback<LocationEngineResult> {
        private var fragmentWeakReference: WeakReference<ContribuirFragment> =
            WeakReference(fragment)

        override fun onSuccess(result: LocationEngineResult) {
            val fragment: ContribuirFragment? = fragmentWeakReference.get()
            if (fragment != null) {
                val location = result.lastLocation ?: return

                // Pass the new location to the Maps SDK's LocationComponent
                if (result.lastLocation != null) {
                    fragment.mapboxMap.locationComponent
                        .forceLocationUpdate(location)
                }
                if (fragment.isRecording && fragment.isPlaying) {
                    val latitude = result.lastLocation!!.latitude
                    val longitude = result.lastLocation!!.longitude

                    fragment.routeCoordinates.add(
                        Point.fromLngLat(
                            longitude,
                            latitude
                        )
                    )
                }
            }
        }

        override fun onFailure(exception: Exception) {
            Timber.d(exception.localizedMessage)
            val fragment: ContribuirFragment? = fragmentWeakReference.get()
            if (fragment != null) {
                Toast.makeText(
                    fragment.context, exception.localizedMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }
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

    override fun onDestroy() {
        super.onDestroy()
        locationEngine?.removeLocationUpdates(callback)
        mapView.onDestroy()
        if (isDestroy) {
            storageRef.child("images/${auth.currentUser?.uid}/locations/${idTrail}").listAll()
                .addOnSuccessListener {
                    it.items.forEach { ref ->
                        ref.delete()
                    }
                }
        }
    }
    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}