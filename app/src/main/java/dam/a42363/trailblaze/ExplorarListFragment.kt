package dam.a42363.trailblaze

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import dam.a42363.trailblaze.databinding.FragmentExplorarListBinding
import dam.a42363.trailblaze.databinding.ItemRouteRatingBinding
import dam.a42363.trailblaze.models.RouteInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ExplorarListFragment : Fragment() {
    private var user: String? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var navController: NavController
    private var adapter: FindTrailsFirestoreRecyclerAdapter? = null
    private var _binding: FragmentExplorarListBinding? = null
    private val binding get() = _binding!!
    private var doubleBackToExitPressedOnce = false
    private lateinit var routesListView: RecyclerView
    private var center: GeoLocation? = null
    var routeInfoList: ArrayList<RouteInfo> = ArrayList()
    private lateinit var db: FirebaseFirestore

    private val REQUEST_CODE_AUTOCOMPLETE = 1

    private lateinit var mainDificuldadeArray: java.util.ArrayList<String>
    private lateinit var mainModalidadeArray: java.util.ArrayList<String>

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
                { doubleBackToExitPressedOnce = false },
                2000
            )
        }

        backCallback.isEnabled
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
//        if (_binding != null) {
//            return binding.root
//        }
        _binding = FragmentExplorarListBinding.inflate(inflater, container, false)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        user = auth.currentUser?.uid

        mainModalidadeArray = (activity as MainActivity).modalidadeArray
        mainDificuldadeArray = (activity as MainActivity).dificuldadeArray

        if (center == null) {
            val point = arguments?.getString("local")?.let { Point.fromJson(it) }
            binding.searchView.setText(arguments?.getString("searchText"))
            if (point != null) {
                center = GeoLocation(point.latitude(), point.longitude())
            }
        }

        binding.filtro.setOnClickListener {

            navController.navigate(R.id.action_explorarListFragment_to_filtrosFragment)
        }

        binding.lista.setOnClickListener {
            val local = Point.fromLngLat(
                center!!.longitude,
                center!!.latitude
            )
            val bundle = bundleOf(
                "local" to local.toJson(), "searchText" to binding.searchView.text.toString()
            )
            navController.navigate(R.id.action_explorarListFragment_to_explorarFragment, bundle)
        }
        routesListView = binding.routesListView

        initSearchFab()

        updateRouteInfo()
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        if (activity != null && this.activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.VISIBLE
        }
    }

    private fun initSearchFab() {
        binding.searchView.isFocusable = false
        binding.searchView.setOnClickListener {
            val placeOptions = PlaceOptions.builder()
                .backgroundColor(Color.parseColor("#EEEEEE"))
                .limit(10)
                .build(PlaceOptions.MODE_CARDS)

            val intent: Intent = PlaceAutocomplete.IntentBuilder()
                .accessToken(
                    (if (Mapbox.getAccessToken() != null) Mapbox.getAccessToken() else getString(
                        R.string.mapbox_access_token
                    )).toString()
                )
                .placeOptions(
                    placeOptions
                )
                .build(requireActivity())
            startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE)
            binding.searchView.clearFocus()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode === Activity.RESULT_OK && requestCode === REQUEST_CODE_AUTOCOMPLETE) {

            // Retrieve selected location's CarmenFeature
            val selectedCarmenFeature = PlaceAutocomplete.getPlace(data)

            // Create a new FeatureCollection and add a new Feature to it using selectedCarmenFeature above.
            // Then retrieve and update the source designated for showing a selected location's symbol layer icon


            // Move map camera to the selected location

//            binding.searchView.setQuery("${selectedCarmenFeature.text()}", false)
            binding.searchView.setText("${selectedCarmenFeature.text()}")
            binding.searchView.clearFocus()
            center = GeoLocation(
                (selectedCarmenFeature.geometry() as Point?)!!.latitude(),
                (selectedCarmenFeature.geometry() as Point?)!!.longitude()
            )
            updateRouteInfo()
        }
    }

    private fun updateRouteInfo() {
        val radiusInM = (2 * 1000).toDouble()
        val bounds = GeoFireUtils.getGeoHashQueryBounds(center!!, radiusInM)
        val tasks: MutableList<Task<QuerySnapshot>> = java.util.ArrayList()
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
                val matchingDocs: MutableList<String> =
                    java.util.ArrayList()
                for (task in tasks) {
                    val snap = task.result
                    for (doc in snap!!.documents) {
                        var check = true
                        val activeRoute = DirectionsRoute.fromJson(doc.getString("route")!!)
                        val optimizedRoute =
                            LineString.fromPolyline(activeRoute.geometry()!!, Constants.PRECISION_6)
                        val lat = optimizedRoute.coordinates().first().latitude()
                        val lng = optimizedRoute.coordinates().first().longitude()

                        // We have to filter out a few false positives due to GeoHash
                        // accuracy, but most will match
                        val docLocation = GeoLocation(lat, lng)
                        val distanceInM =
                            GeoFireUtils.getDistanceBetween(docLocation, center!!)
                        if (distanceInM <= radiusInM) {

                            if (mainModalidadeArray.isNotEmpty()) {
                                if (!mainModalidadeArray.contains(doc.getString("modalidade")))
                                    check = false
                            }
                            if (mainDificuldadeArray.isNotEmpty()) {
                                if (!mainDificuldadeArray.contains(
                                        doc.getString(
                                            "dificuldade"
                                        )
                                    )
                                )
                                    check = false
                            }
                            if (check) {
                                matchingDocs.add(doc.id)
                            }
                        }
                    }
                }
                if (matchingDocs.isNotEmpty()) {
                    val query = db.collection("locations")
                        .whereIn(FieldPath.documentId(), matchingDocs)
                    displayUserTrails(query)
                } else {
                    val query = db.collection("empty")
                    displayUserTrails(query)
                }
            }
    }


    private fun displayUserTrails(query: Query) {
        val options = FirestoreRecyclerOptions.Builder<RouteInfo>()
            .setQuery(query, RouteInfo::class.java).build()

        adapter = FindTrailsFirestoreRecyclerAdapter(options)
        adapter!!.startListening()
        routesListView.adapter = adapter
    }

    inner class FindTrailsViewHolder(val routeBinding: ItemRouteRatingBinding) :
        RecyclerView.ViewHolder(routeBinding.root) {
        fun setVariables(
            nome: String,
            author: String,
            localidade: String,
            fotoBanner: String,
            ctx: Context
        ) {
            routeBinding.name.text = nome
            routeBinding.author.text = author
            routeBinding.localidade.text = localidade
            Glide.with(ctx).load(fotoBanner)
                .into(routeBinding.image)
        }

        @SuppressLint("SetTextI18n")
        fun setRating(id: String) = CoroutineScope(Dispatchers.IO).launch {
            try {
                val ratingsArray = mutableListOf<Float>()
                val ratings =
                    db.collection("Rating").document("RatingDocument").collection(id).get()
                        .await()
                for (rating in ratings.documents) {
                    rating.getString("rating")?.let { ratingsArray.add(it.toFloat()) }
                }
                val favorite =
                    db.collection("Favorite").document("FavoriteDocument").collection(user!!)
                        .document(id).get()
                        .await()
                withContext(Dispatchers.Main) {
                    if (ratingsArray.isNotEmpty()) {
                        val average = ratingsArray.average().toFloat()
                        routeBinding.ratingBar.rating = average
                    }
                    routeBinding.reviewCount.text = "(${ratingsArray.size})"
                    if (favorite.getBoolean("favorite") == true) {
                        routeBinding.likeBtn.visibility = View.GONE
                        routeBinding.likeFullBtn.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
                }
            }

        }
    }

    private inner class FindTrailsFirestoreRecyclerAdapter(
        options: FirestoreRecyclerOptions<RouteInfo>
    ) :
        FirestoreRecyclerAdapter<RouteInfo, FindTrailsViewHolder>(options) {


        override fun onBindViewHolder(
            holder: FindTrailsViewHolder,
            position: Int,
            model: RouteInfo
        ) {
            val name = snapshots.getSnapshot(position).getString("nome")!!
            val author = snapshots.getSnapshot(position).getString("distancia")!!
            val localidade = snapshots.getSnapshot(position).getString("localidade")!!
            val fotoBanner = snapshots.getSnapshot(position).getString("fotoBanner")!!
            holder.setRating(snapshots.getSnapshot(position).id)
            holder.setVariables(name, author, localidade, fotoBanner, requireContext())
            holder.routeBinding.likeBtn.setOnClickListener {
                val updates: MutableMap<String, Any> = HashMap()
                updates["favorite"] = true
                db.collection("Favorite").document("FavoriteDocument")
                    .collection(user!!)
                    .document(snapshots.getSnapshot(position).id).set(updates)
                holder.routeBinding.likeBtn.visibility = View.GONE
                holder.routeBinding.likeFullBtn.visibility = View.VISIBLE
            }
            holder.routeBinding.likeFullBtn.setOnClickListener {
                val updates: MutableMap<String, Any> = HashMap()
                updates["favorite"] = false
                db.collection("Favorite").document("FavoriteDocument")
                    .collection(user!!)
                    .document(snapshots.getSnapshot(position).id).set(updates)
                holder.routeBinding.likeBtn.visibility = View.VISIBLE
                holder.routeBinding.likeFullBtn.visibility = View.GONE
            }
            holder.routeBinding.cardView.setOnClickListener {
                val bundle = bundleOf(
                    "feature" to snapshots.getSnapshot(position).id,
                    "route" to snapshots.getSnapshot(position).getString("route")
                )
                routeInfoList.clear()

                navController.navigate(
                    R.id.action_explorarListFragment_to_fullInfoFragment,
                    bundle
                )
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): FindTrailsViewHolder {
            return FindTrailsViewHolder(
                ItemRouteRatingBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent, false
                )
            )
        }

    }

    override fun onStop() {
        super.onStop()

        if (adapter != null) {
            adapter!!.stopListening()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        routeInfoList.clear()
    }
}