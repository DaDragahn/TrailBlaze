package dam.a42363.trailblaze

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
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
import java.util.*

class FullInfoFragment : Fragment(), OnMapReadyCallback {

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

        // Inflate the layout for this fragment
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        cardView.setBackgroundResource(R.drawable.cardview_info)

        db = FirebaseFirestore.getInstance()

        val feature = arguments?.get("feature")

        db.collection("locations").document("$feature").get().addOnCompleteListener {
            if (it.isSuccessful) {
                val document = it.result
                activeRoute = DirectionsRoute.fromJson(document?.getString("route"))
                optimizedRoute =
                    LineString.fromPolyline(activeRoute.geometry()!!, Constants.PRECISION_6)
                binding.nome.text = document?.getString("nome")
                binding.localidade.text = document?.getString("localidade")
                binding.dificuldade.text =
                    "Dificuldade: ${document?.getString("dificuldade")}"
                binding.modalidade.text = document?.getString("modalidade")
                binding.autor.text = document?.getString("autor")
                binding.descricao.text = document?.getString("descricao")
                mapView.getMapAsync(this)
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