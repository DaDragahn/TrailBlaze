package dam.a42363.trailblaze

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import dam.a42363.trailblaze.databinding.FragmentContribuirBinding

class ContribuirFragment : Fragment(), OnMapReadyCallback, PermissionsListener {
    var _binding: FragmentContribuirBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap

    private var permissionsManager: PermissionsManager? = null

    private val ICON_GEOJSON_SOURSE_ID = "icon-source-id"
    private val ICON_GEOJSON_LAYER_ID = "icon-layer-id"

    private lateinit var locationComponent: LocationComponent

    private lateinit var cardView: CardView

    private lateinit var db: FirebaseFirestore
    private lateinit var navController: NavController

    private lateinit var auth: FirebaseAuth

    private lateinit var recordButton: FloatingActionButton
    private lateinit var playButton: FloatingActionButton
    private lateinit var pauseButton: FloatingActionButton
    private lateinit var stopButton: FloatingActionButton
    private lateinit var chronometerHolder: CardView
    private lateinit var chronometer: Chronometer

    var timeWhenStopped: Long = 0


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        Mapbox.getInstance(requireContext(), getString(R.string.mapbox_access_token))
        _binding = FragmentContribuirBinding.inflate(inflater, container, false)

        mapView = binding.mapView

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        db = FirebaseFirestore.getInstance()
        recordButton = binding.recordBtn
        playButton = binding.playBtn
        pauseButton = binding.pauseBtn
        stopButton = binding.stopBtn
        chronometerHolder = binding.chronometerHolder
        chronometer = binding.chronometer

        recordButton.setOnClickListener {
            recordButton.visibility = View.INVISIBLE
            pauseButton.visibility = View.VISIBLE
            stopButton.visibility = View.VISIBLE
            chronometerHolder.visibility = View.VISIBLE
            chronometer.base = SystemClock.elapsedRealtime() + timeWhenStopped;
            chronometer.start()
        }

        pauseButton.setOnClickListener {
            playButton.visibility = View.VISIBLE
            pauseButton.visibility = View.INVISIBLE
            timeWhenStopped = chronometer.base - SystemClock.elapsedRealtime();
            chronometer.stop();
        }

        playButton.setOnClickListener {
            pauseButton.visibility = View.VISIBLE
            playButton.visibility = View.INVISIBLE
            chronometer.base = SystemClock.elapsedRealtime() + timeWhenStopped;
            chronometer.start()
        }
        stopButton.setOnClickListener {
            chronometer.base = SystemClock.elapsedRealtime()
            timeWhenStopped = 0
            navController.navigate(R.id.action_contribuirFragment_to_partilharFragment)
        }


        return binding.root
//        inflater.inflate(R.layout.fragment_contribuir, container, false)
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
            locationComponent = mapboxMap!!.locationComponent
            // Set the LocationComponent activation options
            val locationComponentActivationOptions =
                LocationComponentActivationOptions.builder(requireContext(), loadedMapStyle)
//                    .useDefaultLocationEngine(false)
                    .build()

            // Activate with the LocationComponentActivationOptions object
            locationComponent.activateLocationComponent(locationComponentActivationOptions)

            // Enable to make component visible
            locationComponent.isLocationComponentEnabled = true

            // Set the component's camera mode
            locationComponent.cameraMode = CameraMode.TRACKING

            // Set the component's render mode
            locationComponent.renderMode = RenderMode.COMPASS

//            initLocationEngine()
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager!!.requestLocationPermissions(requireActivity())
        }
    }


    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        TODO("Not yet implemented")
    }

    override fun onPermissionResult(granted: Boolean) {
        TODO("Not yet implemented")
    }

}