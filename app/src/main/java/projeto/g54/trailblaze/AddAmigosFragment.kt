package projeto.g54.trailblaze

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
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
import projeto.g54.trailblaze.databinding.FragmentAddAmigosBinding
import projeto.g54.trailblaze.databinding.ItemAmigoAddBinding
import projeto.g54.trailblaze.models.AddFriends
import java.text.SimpleDateFormat
import java.util.*


class AddAmigosFragment : Fragment() {

    private var adapter: FindFriendsFirestoreRecyclerAdapter? = null

    private var _binding: FragmentAddAmigosBinding? = null
    private val binding get() = _binding!!

    private lateinit var searchView: SearchView
    private lateinit var searchViewList: RecyclerView

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var friendRequestRef: CollectionReference
    private lateinit var friendRef: CollectionReference

    private var senderUserId: String? = null
    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddAmigosBinding.inflate(inflater, container, false)

        searchView = binding.searchView

        searchViewList = binding.amigosListView

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        senderUserId = auth.uid

        friendRequestRef = FirebaseFirestore.getInstance().collection("FriendRequests")
        friendRef = FirebaseFirestore.getInstance().collection("Friends")

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchPeople(newText!!)
                return true
            }

        })

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

    fun searchPeople(text: String) {
        val firstQuery =
            db.collection("users").orderBy("nome", Query.Direction.ASCENDING).startAt(text)
                .endAt(text + "\uf8ff")

        val options = FirestoreRecyclerOptions.Builder<AddFriends>()
            .setQuery(
                firstQuery
            ) { snapshot ->
                val addFriends: AddFriends = snapshot.toObject(AddFriends::class.java)!!
                if (snapshot.id != auth.uid) {
                    addFriends
                } else {
                    AddFriends()
                }
            }.build()

        adapter = FindFriendsFirestoreRecyclerAdapter(options, requireContext())
        adapter!!.startListening()
        searchViewList.adapter = adapter
    }


    private fun sendFriendRequest(receivedUserId: String) {
        val docSent = hashMapOf(
            "request_type" to "sent"
        )

        val docReceived = hashMapOf(
            "request_type" to "received"
        )
        friendRequestRef.document("$senderUserId").collection(receivedUserId)
            .document("request_type").set(docSent).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    friendRequestRef.document(receivedUserId).collection("$senderUserId")
                        .document("request_type").set(docReceived).addOnCompleteListener {
                            if (it.isSuccessful) {
                                Toast.makeText(
                                    requireContext(),
                                    "Request Send!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                }
            }
    }

    private fun cancelFriendRequest(receivedUserId: String) {
        friendRequestRef.document("$senderUserId").collection(receivedUserId)
            .document("request_type").delete().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    friendRequestRef.document(receivedUserId).collection("$senderUserId")
                        .document("request_type").delete().addOnCompleteListener {
                            if (it.isSuccessful) {
                                Toast.makeText(
                                    requireContext(),
                                    "Request Cancelled!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                }
            }
    }

    private fun acceptFriendRequest(receivedUserId: String) {
        val sdf = SimpleDateFormat("dd-MMMM-yyyy", Locale.US)
        val currentDate = sdf.format(Date())
        val docSent = hashMapOf(
            "date" to currentDate
        )
        friendRef.document("FriendDocument").collection("$senderUserId").document(receivedUserId)
            .set(docSent)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    friendRef.document("FriendDocument").collection(receivedUserId)
                        .document("$senderUserId")
                        .set(docSent).addOnCompleteListener { task2 ->
                            if (task2.isSuccessful) {
                                friendRequestRef.document("$senderUserId")
                                    .collection(receivedUserId)
                                    .document("request_type").delete()
                                    .addOnCompleteListener { task3 ->
                                        if (task3.isSuccessful) {
                                            friendRequestRef.document(receivedUserId)
                                                .collection("$senderUserId")
                                                .document("request_type").delete()
                                                .addOnCompleteListener {
                                                    if (it.isSuccessful) {
                                                        Toast.makeText(
                                                            requireContext(),
                                                            "Request Accepted!",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                }
                                        }
                                    }
                            }
                        }
                }
            }
    }

    override fun onStop() {
        super.onStop()

        if (adapter != null) {
            adapter!!.stopListening()
        }
    }


    private inner class FindFriendsViewHolder(val amigoBinding: ItemAmigoAddBinding) :
        RecyclerView.ViewHolder(amigoBinding.root) {
        fun setVariables(nome: String, photoUrl: String, ctx: Context) {
            amigoBinding.name.text = nome
            Glide.with(ctx).load(photoUrl)
                .into(amigoBinding.profileImage)
        }
    }

    private inner class FindFriendsFirestoreRecyclerAdapter constructor(
        options: FirestoreRecyclerOptions<AddFriends>,
        private val ctx: Context
    ) :
        FirestoreRecyclerAdapter<AddFriends, FindFriendsViewHolder>(options) {
        override fun onBindViewHolder(
            findFriendsViewHolder: FindFriendsViewHolder,
            position: Int,
            addFriends: AddFriends
        ) {
            friendRef.document("FriendDocument")
                .collection("$senderUserId")
                .document(snapshots.getSnapshot(position).id).addSnapshotListener { snapshot, _ ->
                    if (snapshot == null || !snapshot.exists()) {
                        if (addFriends.nome != null) {
                            findFriendsViewHolder.setVariables(
                                addFriends.nome!!,
                                addFriends.photoUrl!!,
                                ctx
                            )
                            friendRequestRef.document("$senderUserId")
                                .collection(snapshots.getSnapshot(position).id)
                                .document("request_type").addSnapshotListener { snapshot, _ ->
                                    if (snapshot != null && snapshot.exists()) {
                                        val requestType = snapshot.getString("request_type")
                                        if (requestType.equals("sent")) {
                                            findFriendsViewHolder.amigoBinding.adicionar.visibility =
                                                View.GONE
                                            findFriendsViewHolder.amigoBinding.accept.visibility =
                                                View.GONE
                                            findFriendsViewHolder.amigoBinding.refuse.visibility =
                                                View.VISIBLE
                                            findFriendsViewHolder.amigoBinding.refuse.setOnClickListener {
                                                cancelFriendRequest(snapshots.getSnapshot(position).id)
                                            }
                                        } else if (requestType.equals("received")) {
                                            findFriendsViewHolder.amigoBinding.adicionar.visibility =
                                                View.GONE
                                            findFriendsViewHolder.amigoBinding.accept.visibility =
                                                View.VISIBLE
                                            findFriendsViewHolder.amigoBinding.refuse.visibility =
                                                View.VISIBLE
                                            findFriendsViewHolder.amigoBinding.accept.setOnClickListener {
                                                acceptFriendRequest(snapshots.getSnapshot(position).id)
                                            }
                                            findFriendsViewHolder.amigoBinding.refuse.setOnClickListener {
                                                cancelFriendRequest(snapshots.getSnapshot(position).id)
                                            }
                                        }
                                    } else {
                                        if (findFriendsViewHolder.amigoBinding.cardView.visibility == View.VISIBLE) {
                                            findFriendsViewHolder.amigoBinding.adicionar.visibility =
                                                View.VISIBLE
                                            findFriendsViewHolder.amigoBinding.accept.visibility =
                                                View.GONE
                                            findFriendsViewHolder.amigoBinding.refuse.visibility =
                                                View.GONE
                                            findFriendsViewHolder.amigoBinding.adicionar.setOnClickListener {
                                                sendFriendRequest(snapshots.getSnapshot(position).id)
                                            }
                                        }
                                    }
                                }
                        } else {
                            findFriendsViewHolder.amigoBinding.cardView.visibility = View.GONE
                        }
                    } else {
                        findFriendsViewHolder.amigoBinding.cardView.visibility = View.GONE
                    }
                }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): FindFriendsViewHolder {
            return FindFriendsViewHolder(
                ItemAmigoAddBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent, false
                )
            )
        }
    }
}
