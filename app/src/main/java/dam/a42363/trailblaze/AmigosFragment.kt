package dam.a42363.trailblaze

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dam.a42363.trailblaze.databinding.FragmentAmigosBinding
import dam.a42363.trailblaze.databinding.ItemAmigoBinding
import dam.a42363.trailblaze.models.Friends


class AmigosFragment : Fragment() {
    private var adapter: FindFriendsFirestoreRecyclerAdapter? = null
    private lateinit var navController: NavController
    var _binding: FragmentAmigosBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private lateinit var userRef: CollectionReference
    private lateinit var friendRef: Query
    private lateinit var auth: FirebaseAuth
    private lateinit var onlineId: String
    private lateinit var amigosListView: RecyclerView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAmigosBinding.inflate(inflater, container, false)

        binding.addAmigos.setOnClickListener {
            navController.navigate(R.id.action_amigosFragment_to_addAmigosFragment)
        }
        amigosListView = binding.amigosListView
        auth = FirebaseAuth.getInstance()
        onlineId = auth.currentUser!!.uid
        db = FirebaseFirestore.getInstance()
        friendRef = db.collection("Friends").document("FriendDocument").collection(onlineId)
        userRef = db.collection("users")
        displayAllFriends()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        if (activity != null && this.activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.VISIBLE
        }
    }

    private fun displayAllFriends() {
        val options = FirestoreRecyclerOptions.Builder<Friends>()
            .setQuery(friendRef, Friends::class.java).build()

        adapter = FindFriendsFirestoreRecyclerAdapter(options, requireContext())
        adapter!!.startListening()
        amigosListView.adapter = adapter

    }

    override fun onStop() {
        super.onStop()

        if (adapter != null) {
            adapter!!.stopListening()
        }
    }

    inner class FindFriendsViewHolder(val amigoBinding: ItemAmigoBinding) :
        RecyclerView.ViewHolder(amigoBinding.root) {
        fun setVariables(nome: String, photoUrl: String, ctx: Context) {
            amigoBinding.name.text = nome
            Glide.with(ctx).load(photoUrl)
                .into(amigoBinding.profileImage)
        }
    }

    private inner class FindFriendsFirestoreRecyclerAdapter(
        options: FirestoreRecyclerOptions<Friends>,
        private val ctx: Context
    ) :
        FirestoreRecyclerAdapter<Friends, FindFriendsViewHolder>(options) {

        override fun onBindViewHolder(
            holder: FindFriendsViewHolder,
            position: Int,
            model: Friends
        ) {
            Log.d("TAG", snapshots.getSnapshot(position).id)
            val usersID = snapshots.getSnapshot(position).id

            userRef.document(usersID).addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val userName: String = snapshot.getString("nome")!!
                    val photoUrl: String = snapshot.getString("photoUrl")!!
                    holder.setVariables(userName, photoUrl, ctx)
                    holder.amigoBinding.adicionar.visibility = View.INVISIBLE
                }
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): FindFriendsViewHolder {
            return FindFriendsViewHolder(
                ItemAmigoBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent, false
                )
            )
        }
    }
}