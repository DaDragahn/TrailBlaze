package dam.a42363.trailblaze

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dam.a42363.trailblaze.databinding.FragmentGuardadosBinding
import dam.a42363.trailblaze.databinding.ItemRouteRatingBinding
import dam.a42363.trailblaze.models.RouteInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class GuardadosFragment : Fragment() {
    private val favoriteArray: ArrayList<String> = ArrayList()
    private lateinit var trailsListView: RecyclerView
    private var adapter: FindTrailsFirestoreRecyclerAdapter? = null
    private lateinit var routeRef: Query
    private var user: FirebaseUser? = null
    private lateinit var navController: NavController
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    var _binding: FragmentGuardadosBinding? = null
    private val binding get() = _binding!!
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
        _binding = FragmentGuardadosBinding.inflate(inflater, container, false)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        user = auth.currentUser
        trailsListView = binding.routesListView
        getFavorites()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        if (user == null && auth.uid == null) {
            navController.navigate(R.id.action_guardadosFragment_to_signInFragment)
        } else {
            if (activity != null && this.activity is MainActivity) {
                (activity as MainActivity).bottomNavigationView?.visibility = View.VISIBLE
            }
        }
    }


    private fun getFavorites() = CoroutineScope(Dispatchers.IO).launch {
        try {
            val favorites =
                db.collection("Favorite").document("FavoriteDocument").collection(user!!.uid).get()
                    .await()
            for (favorite in favorites.documents)
                if (favorite.getBoolean("favorite") == true) {
                    favoriteArray.add(favorite.id)
                }
            withContext(Dispatchers.Main) {
                displayFavoriteTrails()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun displayFavoriteTrails() {
        if (favoriteArray.isNotEmpty()) {
            routeRef = db.collection("locations").whereIn(FieldPath.documentId(), favoriteArray)
            val options = FirestoreRecyclerOptions.Builder<RouteInfo>()
                .setQuery(routeRef, RouteInfo::class.java).build()

            adapter = FindTrailsFirestoreRecyclerAdapter(options)
            adapter!!.startListening()
            trailsListView.adapter = adapter
        }
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
            routeBinding.likeFullBtn.visibility = View.VISIBLE
            routeBinding.likeBtn.visibility = View.GONE

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
            holder.routeBinding.likeFullBtn.setOnClickListener {
                val updates: MutableMap<String, Any> = HashMap()
                updates["favorite"] = false
                db.collection("Favorite").document("FavoriteDocument")
                    .collection(user!!.uid)
                    .document(snapshots.getSnapshot(position).id).set(updates)
                holder.routeBinding.likeBtn.visibility = View.VISIBLE
                holder.routeBinding.likeFullBtn.visibility = View.GONE
            }
            holder.routeBinding.likeBtn.setOnClickListener {
                val updates: MutableMap<String, Any> = HashMap()
                updates["favorite"] = true
                db.collection("Favorite").document("FavoriteDocument")
                    .collection(user!!.uid)
                    .document(snapshots.getSnapshot(position).id).set(updates)
                holder.routeBinding.likeBtn.visibility = View.GONE
                holder.routeBinding.likeFullBtn.visibility = View.VISIBLE
            }
            holder.routeBinding.cardView.setOnClickListener {
                Toast.makeText(
                    requireContext(),
                    snapshots.getSnapshot(position).id,
                    Toast.LENGTH_LONG
                ).show()
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