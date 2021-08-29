package dam.a42363.trailblaze

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import dam.a42363.trailblaze.databinding.FragmentFullInfoBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

class FullInfoFragment : Fragment(), OnMapReadyCallback {

    private var route: String? = null
    private var feature: String? = null
    private lateinit var optimizedRoute: LineString
    private lateinit var activeRoute: DirectionsRoute
    private val ICON_GEOJSON_SOURSE_ID = "icon-source-id"
    private val ICON_GEOJSON_LAYER_ID = "icon-layer-id"
    private val ROUTE_COLOR = "#3bb2d0"
    private val POLYLINE_WIDTH = 5f

    private lateinit var mainDificuldadeArray: ArrayList<String>
    private lateinit var mainModalidadeArray: ArrayList<String>

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap

    lateinit var navController: NavController
    private var _binding: FragmentFullInfoBinding? = null
    private val binding get() = _binding!!

    private lateinit var cardView: CardView

    private lateinit var db: FirebaseFirestore


    @SuppressLint("UseCompatLoadingForColorStateLists")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        Mapbox.getInstance(requireContext(), getString(R.string.mapbox_access_token))

        context?.let { Mapbox.getInstance(it, getString(R.string.mapbox_access_token)) }

        _binding = FragmentFullInfoBinding.inflate(inflater, container, false)

        cardView = binding.cardViewInfo

        mapView = binding.mapView

        mapView.onCreate(savedInstanceState)

        mainModalidadeArray = (activity as MainActivity).modalidadeArray
        mainDificuldadeArray = (activity as MainActivity).dificuldadeArray

        binding.iniciarBtn.backgroundTintList =
            view?.resources?.getColorStateList(R.color.trailGreen)
        feature = arguments?.getString("feature")
        route = arguments?.getString("route")
        binding.iniciarBtn.setOnClickListener {

            binding.decisionCardView.visibility = View.VISIBLE

            binding.individualBtn.setOnClickListener {
                val bundle = bundleOf(
                    "route" to route,
                    "idTrail" to feature,
                    "individual" to true
                )
                navController.navigate(
                    R.id.action_fullInfoFragment_to_navigationFragment,
                    bundle
                )
            }

            binding.acompanhadoBtn.setOnClickListener {
                val bundle =
                    bundleOf("route" to route, "feature" to feature)
                navController.navigate(
                    R.id.action_fullInfoFragment_to_escolherModoFragment,
                    bundle
                )
            }
        }
        binding.ratingBar.isFocusable = false
        db = FirebaseFirestore.getInstance()
        setRating()
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        if (activity != null && this.activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.GONE
        }

        binding.close.setOnClickListener {
            binding.decisionCardView.visibility = View.GONE
        }

        cardView.setBackgroundResource(R.drawable.cardview_info)

        db.collection("locations").document("$feature").get().addOnCompleteListener {
            if (it.isSuccessful) {
                val document = it.result
                activeRoute = DirectionsRoute.fromJson(route)
                optimizedRoute =
                    LineString.fromPolyline(activeRoute.geometry()!!, Constants.PRECISION_6)
                binding.nome.text = document?.getString("nome")
                binding.localidade.text = document?.getString("localidade")
                when ("${document?.getString("dificuldade")}") {
                    "Fácil" -> {
                        binding.dificuldade.text =
                            getString(R.string.facil)
                    }
                    "Moderada" -> {
                        binding.dificuldade.text =
                            getString(R.string.moderado)
                    }
                    "Difícil" -> {
                        binding.dificuldade.text =
                            getString(R.string.dificil)
                    }
                }
                when ("${document?.getString("modalidade")}") {
                    "Caminhada" -> {
                        binding.modalidade.text =
                            getString(R.string.caminhada)
                    }
                    "Corrida" -> {
                        binding.modalidade.text =
                            getString(R.string.corrida)
                    }
                    "Ciclismo" -> {
                        binding.modalidade.text =
                            getString(R.string.ciclismo)
                    }
                }
                Glide.with(this).load(document?.getString("fotoBanner"))
                    .into(binding.fotoBanner)
                binding.descricao.text = document?.getString("descricao")
                mapView.getMapAsync(this)
            }
        }

    }

    @SuppressLint("SetTextI18n")
    private fun setRating() = CoroutineScope(Dispatchers.IO).launch {
        try {
            val ratingsArray = mutableListOf<Float>()
            val ratings =
                db.collection("Rating").document("RatingDocument").collection("$feature").get()
                    .await()
            for (rating in ratings.documents) {
                rating.getString("rating")?.let { ratingsArray.add(it.toFloat()) }
            }
            withContext(Dispatchers.Main) {
                if (ratingsArray.isEmpty()) {
                    binding.reviewTextView.text = getString(R.string.nenhum)
                    binding.ratingBar.visibility = View.GONE
                } else {
                    val average = ratingsArray.average().toFloat()
                    val count = ratingsArray.size
                    binding.reviewTextView.text = "$average"
                    binding.reviewCount.text = "($count)"
                    binding.ratingBar.rating = average
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        val origin = optimizedRoute.coordinates().first()
        val position = CameraPosition.Builder()
            .target(LatLng(origin.latitude(), origin.longitude()))
            .zoom(13.0)
            .build()
        mapboxMap.cameraPosition = position
        mapboxMap.setStyle(Style.OUTDOORS) { style ->
            val uiSettings = mapboxMap.uiSettings
            uiSettings.setAllGesturesEnabled(false)
            initMarkerIconSymbolLayer(style)
            initOptimizedRouteLineLayer(style)
            val source = style.getSourceAs<GeoJsonSource>("route-source-id")

            source?.setGeoJson(optimizedRoute)
        }
    }

    private fun initMarkerIconSymbolLayer(style: Style) {
        // Add the marker image to map
        style.addImage(
            "icon-image", BitmapFactory.decodeResource(
                this.resources, R.drawable.mapbox_marker_icon_default
            )
        )
        val markerList: MutableList<Feature> = ArrayList()
        markerList.add(Feature.fromGeometry(optimizedRoute.coordinates().first()))
        markerList.add(Feature.fromGeometry(optimizedRoute.coordinates().last()))
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
        val iconSource =
            style.getSourceAs<GeoJsonSource>(ICON_GEOJSON_SOURSE_ID)
        iconSource?.setGeoJson(FeatureCollection.fromFeatures(markerList))
    }

    private fun initOptimizedRouteLineLayer(loadedMapStyle: Style) {
        loadedMapStyle.addSource(GeoJsonSource("route-source-id"))
        loadedMapStyle.addLayerBelow(
            LineLayer("route-layer-id", "route-source-id")
                .withProperties(
                    PropertyFactory.lineColor(Color.parseColor(ROUTE_COLOR)),
                    PropertyFactory.lineWidth(POLYLINE_WIDTH)
                ), "icon-layer-id"
        )
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


}