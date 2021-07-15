package dam.a42363.trailblaze

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
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import dam.a42363.trailblaze.databinding.FragmentExplorarListBinding
import dam.a42363.trailblaze.models.RouteInfo

class ExplorarListFragment : Fragment(), RouteRecyclerAdapter.OnListListener {
    private lateinit var navController: NavController

    private var _binding: FragmentExplorarListBinding? = null
    private val binding get() = _binding!!
    private var doubleBackToExitPressedOnce = false
    private lateinit var routesListView: RecyclerView
    private lateinit var center: GeoLocation
    var routeInfoList: ArrayList<RouteInfo> = ArrayList()
    private lateinit var db: FirebaseFirestore
    private lateinit var routeAdapter: RouteRecyclerAdapter

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExplorarListBinding.inflate(inflater, container, false)


        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        if (activity != null && this.activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.VISIBLE
        }

        binding.lista.setOnClickListener() {
            navController.navigate(R.id.action_explorarListFragment_to_explorarFragment)
        }
        db = FirebaseFirestore.getInstance()
        val point = arguments?.getString("local")?.let { Point.fromJson(it) }
        if (point != null) {
            center = GeoLocation(point.latitude(), point.longitude())
        }
        updateRouteInfo()
    }

    private fun updateRouteInfo() {
        val radiusInM = (2 * 1000).toDouble()
        val bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInM)
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
                val matchingDocs: MutableList<DocumentSnapshot> =
                    java.util.ArrayList()
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
                    val name = docSnap.getString("nome")!!
                    val localidade = docSnap.getString("localidade")!!
                    val author = docSnap.getString("autor")!!
                    val id = docSnap.id
                    val route = RouteInfo(name, localidade, author, id)

                    routeInfoList.add(route)
                }
                routesListView = binding.routesListView
                routeAdapter = RouteRecyclerAdapter(routeInfoList, this)

                routesListView.adapter = routeAdapter
            }
    }

    override fun onNoteClick(position: Int) {
        val bundle = bundleOf(
            "feature" to routeInfoList[position].id
        )
        routeInfoList.clear()

        navController.navigate(
            R.id.action_explorarListFragment_to_fullInfoFragment,
            bundle
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        routeInfoList.clear()
    }
}