package dam.a42363.trailblaze

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
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
import dam.a42363.trailblaze.databinding.FragmentEscolherModoBinding
import dam.a42363.trailblaze.databinding.ItemAmigoConvidarBinding
import dam.a42363.trailblaze.models.Friends
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class EscolherModoFragment : Fragment() {

    var _binding: FragmentEscolherModoBinding? = null
    private val binding get() = _binding!!
    private lateinit var amigosListView: RecyclerView
    private lateinit var db: FirebaseFirestore
    private lateinit var userRef: CollectionReference
    private lateinit var friendRef: Query
    private lateinit var auth: FirebaseAuth
    private lateinit var onlineId: String
    private lateinit var navController: NavController
    private var adapter: FindFriendsFirestoreRecyclerAdapter? = null
    private val friendsArray = ArrayList<String>()
    private var optimizedRoute: String? = null
    private var feature: String? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentEscolherModoBinding.inflate(inflater, container, false)
        amigosListView = binding.amigosListView
        auth = FirebaseAuth.getInstance()
        onlineId = auth.currentUser!!.uid
        db = FirebaseFirestore.getInstance()
        friendRef = db.collection("Friends").document("FriendDocument").collection(onlineId)
        userRef = db.collection("users")
        displayAllFriends()
        binding.start.setOnClickListener {
            inviteAndStartTrail()
        }

        optimizedRoute = arguments?.getString("route")!!
        feature = arguments?.getString("feature")!!
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun inviteAndStartTrail() {
        var currentDateTime = LocalDateTime.now()

        val lobby = db.collection("Trails").document()

        db.collection("users").document(onlineId).get().addOnCompleteListener {
            if (it.isSuccessful) {
                val documentSnapshot = it.result
                if (documentSnapshot!!.exists()) {
                    val docSent = hashMapOf(
                        "idInvite" to lobby.id,
                        "nome" to documentSnapshot.getString("nome"),
                        "photoUrl" to documentSnapshot.getString("photoUrl"),
                        "idRoute" to feature,
                        "time" to currentDateTime,
                        "type" to "Trail"
                    )
                    for (id in friendsArray) {
                        db.collection("Invites").document("InviteDocument").collection(id)
                            .document(onlineId)
                            .set(docSent)
                    }
                    val lobbySent = hashMapOf(
                        "nome" to documentSnapshot.getString("nome"),
                        "LastLocation" to ""
                    )
                    lobby.collection("TrailsCollection").document(onlineId).set(lobbySent)
                }
            }
        }

        val bundle = bundleOf("route" to optimizedRoute, "idTrail" to lobby.id)

        navController.navigate(
            R.id.action_escolherModoFragment_to_navigationFragment,
            bundle
        )
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

    inner class FindFriendsViewHolder(val amigoBinding: ItemAmigoConvidarBinding) :
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
            val usersID = snapshots.getSnapshot(position).id

            userRef.document(usersID).addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val userName: String = snapshot.getString("nome")!!
                    val photoUrl: String = snapshot.getString("photoUrl")!!
                    holder.setVariables(userName, photoUrl, ctx)
                    holder.amigoBinding.check.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            if (friendsArray.isEmpty())
                                binding.start.visibility = View.VISIBLE
                            friendsArray.add(usersID)
                        } else {
                            friendsArray.remove(usersID)
                            if (friendsArray.isEmpty())
                                binding.start.visibility = View.GONE
                        }
                    }
                }
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): FindFriendsViewHolder {
            return FindFriendsViewHolder(
                ItemAmigoConvidarBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent, false
                )
            )
        }
    }
}