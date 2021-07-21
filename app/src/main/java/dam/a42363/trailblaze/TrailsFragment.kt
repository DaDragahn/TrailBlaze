package dam.a42363.trailblaze

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dam.a42363.trailblaze.databinding.FragmentTrailsBinding
import dam.a42363.trailblaze.databinding.ItemRouteBinding
import dam.a42363.trailblaze.models.RouteInfo


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

    inner class FindTrailsViewHolder(val routeBinding: ItemRouteBinding) :
        RecyclerView.ViewHolder(routeBinding.root) {
        fun setVariables(nome: String, author: String, localidade: String) {
            routeBinding.name.text = nome
            routeBinding.author.text = author
            routeBinding.localidade.text = localidade
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
            val author = snapshots.getSnapshot(position).getString("autor")!!
            val localidade = snapshots.getSnapshot(position).getString("localidade")!!
            holder.setVariables(name,author,localidade)
            holder.routeBinding.cardView.setOnClickListener{
                Toast.makeText(requireContext(),snapshots.getSnapshot(position).id,Toast.LENGTH_LONG).show()
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): FindTrailsViewHolder {
            return FindTrailsViewHolder(
                ItemRouteBinding.inflate(
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