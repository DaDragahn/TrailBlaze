package dam.a42363.trailblaze

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.geocoding.v5.GeocodingCriteria
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.core.exceptions.ServicesException
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import dam.a42363.trailblaze.databinding.FragmentPartilharBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.util.*


class PartilharFragment : Fragment() {

    private lateinit var storageRef: StorageReference
    private var idTrail: String? = null
    private lateinit var navController: NavController
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var user: FirebaseUser

    private var _binding: FragmentPartilharBinding? = null
    private var routeCoordinates: FeatureCollection? = null
    private val binding get() = _binding!!
    private var currentRoute: DirectionsRoute? = null
    private lateinit var origin: Point
    private lateinit var destination: Point
    private val TAG = "TrackingLocation"
    private var routeJson: String? = null
    private var isDestroy: Boolean = true
    private var imageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
        }

        callback.isEnabled
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentPartilharBinding.inflate(inflater, container, false)
        isDestroy = true
        imageUrl = (activity as MainActivity).imageUrl
        routeJson = arguments?.getString("routeCoordinates")
        idTrail = arguments?.getString("idTrail")
        routeCoordinates = FeatureCollection.fromJson(routeJson!!)
        drawLines(routeCoordinates!!)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        user = auth.currentUser!!
        storageRef = FirebaseStorage.getInstance().reference

        binding.excludeBtn.setOnClickListener {
            navController.navigate(R.id.action_partilharFragment_to_explorarFragment)
        }
        binding.partilharBtn.setOnClickListener {
            savedRouteOnDatabase()
        }

        if (imageUrl!!.isNotBlank() && imageUrl!!.isNotEmpty()) {
            binding.addImage.visibility = View.GONE
            Glide.with(this).load(imageUrl).into(binding.placeImage)
            binding.placeImage.visibility = View.VISIBLE
        }
        binding.addImage.setOnClickListener {
            isDestroy = false
            val bundle = bundleOf("idTrail" to idTrail)
            navController.navigate(
                R.id.action_partilharFragment_to_listarImagensPartilharFragment,
                bundle
            )
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        val modalidades = resources.getStringArray(R.array.modalidades)
        val dificuldade = resources.getStringArray(R.array.dificuldade)
        val modalidadeArrayAdapter =
            ArrayAdapter(requireContext(), R.layout.item_dropdown, modalidades)
        val dificuldadeArrayAdapter =
            ArrayAdapter(requireContext(), R.layout.item_dropdown, dificuldade)
        binding.modalidadesTextView.setAdapter(modalidadeArrayAdapter)
        binding.dificuldadeTextView.setAdapter(dificuldadeArrayAdapter)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        if (activity != null && this.activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.GONE
        }

    }

    private fun reverseGeocode(point: Point) {
        try {
            val client = MapboxGeocoding.builder()
                .accessToken(getString(R.string.mapbox_access_token))
                .query(Point.fromLngLat(point.longitude(), point.latitude()))
                .geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
                .build()

            client.enqueueCall(object : Callback<GeocodingResponse> {
                @SuppressLint("SetTextI18n")
                override fun onResponse(
                    call: Call<GeocodingResponse>,
                    response: Response<GeocodingResponse>
                ) {
                    if (response.body() != null) {
                        val results = response.body()!!.features()
                        if (results.size > 0) {
                            val feature = results[0]
                            binding.partidaFimTextView.text =
                                feature.text() + ", " + feature.context()?.get(1)?.text()

                        } else {
                            Log.d(TAG, "lig Current location is nothing")
                        }
                    }
                }

                override fun onFailure(call: Call<GeocodingResponse>, throwable: Throwable) {
                    Log.e(TAG, "Geocoding Failure", throwable)
                }
            })
        } catch (servicesException: ServicesException) {
            Log.e(TAG, servicesException.toString())
            servicesException.printStackTrace()
        }
    }

    private fun drawLines(featureCollection: FeatureCollection) {
        val features: List<Feature>? = featureCollection.features()
        if (features != null && features.isNotEmpty()) {
            val feature: Feature = features[0]
            requestRoute(feature)
        }
    }

    private fun requestRoute(feature: Feature) {
        var index = 2
        var points: List<Point> =
            (Objects.requireNonNull(feature.geometry()) as LineString).coordinates()
        origin = points.first()
        destination = points.last()
        points = points.drop(1)
        points = points.dropLast(1)
        Log.v("TrackingLocation", "$origin" + "$destination")
        while (points.size > 23) {
            points = points.drop(index++)
        }
        Log.v("TrackingLocation", points.size.toString())
        if (points.size > 1) {
            MapboxDirections.builder()
                .accessToken(Mapbox.getAccessToken()!!)
                .profile(DirectionsCriteria.PROFILE_WALKING)
                .origin(origin)
                .destination(destination)
                .waypoints(points)
                .steps(true)
                .build().enqueueCall(object : Callback<DirectionsResponse> {
                    @SuppressLint("SetTextI18n")
                    override fun onResponse(
                        call: Call<DirectionsResponse?>,
                        response: Response<DirectionsResponse?>
                    ) {
                        if (response.isSuccessful && response.body()!!.routes().isNotEmpty()) {
                            currentRoute =
                                response.body()!!.routes()[0]
//                            optimizedRoute = LineString.fromPolyline(
//                                currentRoute?.geometry()!!,
//                                Constants.PRECISION_6
//                            )
                            val distance: Float = (currentRoute!!.distance() / 1000f).toFloat()
                            if (distance < 1) {
                                binding.distanciaTextView.text =
                                    "%.0f".format(currentRoute!!.distance()) + "m"
                            } else {
                                binding.distanciaTextView.text = "%.1f".format(distance) + "Km"
                            }
                            reverseGeocode(origin)
//                            Log.d(
//                                "TrackingLocation",
//                                optimizedRoute!!.coordinates().size.toString()
//                            )
                            Log.d("TrackingLocation", currentRoute?.toJson()!!)

                        } else {
                            // If the response code does not response "OK" an error has occurred.
                            Timber.e("MapboxMapMatching failed with %s", response.code())
                        }
                    }

                    override fun onFailure(
                        call: Call<DirectionsResponse?>?,
                        throwable: Throwable?
                    ) {
                        Timber.e(throwable, "MapboxMapMatching error")
                    }
                })
        }
    }

    //
    private fun savedRouteOnDatabase() {
        val hash = GeoFireUtils.getGeoHashForLocation(
            GeoLocation(
                origin.latitude(),
                origin.longitude()
            )
        )
        val updates: MutableMap<String, Any> =
            HashMap()
        updates["nome"] = binding.nomeTextView.text.toString()
        updates["autor"] = user.displayName.toString()
        updates["uid"] = user.uid
        updates["descricao"] = binding.descricaoTextView.text.toString()
        when (binding.dificuldadeTextView.text.toString()) {
            getString(R.string.facil) -> {
                updates["dificuldade"] = "Fácil"
            }
            getString(R.string.moderado) -> {
                updates["dificuldade"] = "Moderado"
            }
            getString(R.string.dificil) -> {
                updates["dificuldade"] = "Difícil"
            }
        }
        when (binding.modalidadesTextView.text.toString()) {
            getString(R.string.caminhada) -> {
                updates["modalidade"] = "Caminhada"
            }
            getString(R.string.corrida) -> {
                updates["modalidade"] = "Corrida"
            }
            getString(R.string.ciclismo) -> {
                updates["modalidade"] = "Ciclismo"
            }
        }
        updates["distancia"] = binding.distanciaTextView.text.toString()
        updates["geohash"] = hash
        updates["localidade"] = binding.partidaFimTextView.text.toString()
        updates["route"] = currentRoute?.toJson()!!
        updates["fotoBanner"] = imageUrl.toString()
        (activity as MainActivity).imageUrl = ""
        val doc = db.collection("locations").document(idTrail!!)
        doc.set(updates).addOnCompleteListener {
            if (it.isSuccessful) {
                isDestroy = false
                navController.navigate(R.id.action_partilharFragment_to_explorarFragment)
            }
        }
    }

    override fun onDestroyView() {
        if (isDestroy) {
            storageRef.child("images/${auth.currentUser?.uid}/locations/${idTrail}").listAll()
                .addOnSuccessListener {
                    it.items.forEach { ref ->
                        ref.delete()
                    }
                }
        }
        super.onDestroyView()
    }

    override fun onDestroy() {
        if (isDestroy) {
            storageRef.child("images/${auth.currentUser?.uid}/locations/${idTrail}").listAll()
                .addOnSuccessListener {
                    it.items.forEach { ref ->
                        ref.delete()
                    }
                }
        }
        super.onDestroy()
    }
}
