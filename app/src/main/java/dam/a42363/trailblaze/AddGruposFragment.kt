package dam.a42363.trailblaze

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.annotation.RequiresApi
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
import dam.a42363.trailblaze.databinding.FragmentAddGruposBinding
import dam.a42363.trailblaze.databinding.ItemAmigoConvidarBinding
import dam.a42363.trailblaze.models.Friends
import java.time.LocalDateTime

class AddGruposFragment : Fragment() {

    private lateinit var navController: NavController
    private var _binding: FragmentAddGruposBinding? = null
    private val binding get() = _binding!!
    private lateinit var amigosListView: RecyclerView
    private lateinit var userRef: CollectionReference
    private lateinit var friendRef: Query
    private lateinit var auth: FirebaseAuth
    private lateinit var onlineId: String
    private lateinit var db: FirebaseFirestore
    private var adapter: FindFriendsFirestoreRecyclerAdapter? = null
    private var groupName: EditText? = null
    private val friendsArray = ArrayList<String>()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddGruposBinding.inflate(inflater, container, false)

        amigosListView = binding.amigosListView

        auth = FirebaseAuth.getInstance()
        onlineId = auth.currentUser!!.uid
        db = FirebaseFirestore.getInstance()
        friendRef = db.collection("Friends").document("FriendDocument").collection(onlineId)
        userRef = db.collection("users")
        groupName = binding.nome
        displayAllFriends()
        binding.start.setOnClickListener {
            createGroup()
        }
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createGroup() {
        val currentDateTime = LocalDateTime.now()

        if (!groupName?.equals("")!! || groupName != null) {
            val group = db.collection("Groups").document()

            db.collection("users").document(onlineId).get().addOnCompleteListener {
                if (it.isSuccessful) {
                    val documentSnapshot = it.result
                    if (documentSnapshot!!.exists()) {
                        for (id in friendsArray) {
                            val docSent = hashMapOf(
                                "idInvite" to group.id,
                                "idReceived" to id,
                                "nome" to documentSnapshot.getString("nome"),
                                "photoUrl" to documentSnapshot.getString("photoUrl"),
                                "type" to "Group",
                                "time" to currentDateTime
                            )
                            db.collection("Invites").document()
                                .set(docSent)
                        }
                        val groupArray = ArrayList<String>()
                        groupArray.add(onlineId)
                        val groupSent = hashMapOf(
                            "nome" to groupName!!.text.toString(),
                            "photoUrl" to "",
                            "groupArray" to groupArray
                        )
                        group.set(groupSent)
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        if (activity != null && this.activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.GONE
        }

        binding.backBtn.setOnClickListener {
            navController.popBackStack()
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
                            friendsArray.add(usersID)
                        } else {
                            friendsArray.remove(usersID)
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