package dam.a42363.trailblaze

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dam.a42363.trailblaze.databinding.FragmentTrailsBinding
import dam.a42363.trailblaze.databinding.ItemRouteRatingBinding
import dam.a42363.trailblaze.models.RouteInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class TrailsFragment : Fragment() {
    private var adapter: FindTrailsFirestoreRecyclerAdapter? = null
    private lateinit var navController: NavController
    var _binding: FragmentTrailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private lateinit var routeRef: Query
    private lateinit var auth: FirebaseAuth
    private lateinit var onlineId: String
    private lateinit var trailsListView: RecyclerView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrailsBinding.inflate(inflater, container, false)

        trailsListView = binding.routesListView
        auth = FirebaseAuth.getInstance()
        onlineId = auth.currentUser!!.uid
        db = FirebaseFirestore.getInstance()
        routeRef = db.collection("locations").whereEqualTo("uid", onlineId)
        displayUserTrails()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        binding.backBtn.setOnClickListener {
            navController.popBackStack()
        }

        if (activity != null && this.activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.GONE
        }
    }

    private fun displayUserTrails() {
        val options = FirestoreRecyclerOptions.Builder<RouteInfo>()
            .setQuery(routeRef, RouteInfo::class.java).build()

        adapter = FindTrailsFirestoreRecyclerAdapter(options)
        adapter!!.startListening()
        trailsListView.adapter = adapter
    }

    inner class FindTrailsViewHolder(val routeBinding: ItemRouteRatingBinding) :
        RecyclerView.ViewHolder(routeBinding.root) {
        fun setVariables(
            nome: String, author: String, localidade: String, fotoBanner: String,
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
                withContext(Dispatchers.Main) {
                    if (ratingsArray.isNotEmpty()) {
                        val average = ratingsArray.average().toFloat()
                        routeBinding.ratingBar.rating = average
                    }
                    routeBinding.reviewCount.text = "(${ratingsArray.size})"
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
            holder.routeBinding.cardView.setOnClickListener {
                Toast.makeText(
                    requireContext(),
                    snapshots.getSnapshot(position).id,
                    Toast.LENGTH_LONG
                ).show()
            }
            holder.routeBinding.cardView.setOnClickListener {
                val bundle = bundleOf(
                    "feature" to snapshots.getSnapshot(position).id,
                    "route" to snapshots.getSnapshot(position).getString("route")
                )

                navController.navigate(
                    R.id.action_trailsFragment_to_fullInfoFragment,
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
}