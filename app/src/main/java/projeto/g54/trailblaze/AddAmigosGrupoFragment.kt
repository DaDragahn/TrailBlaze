package projeto.g54.trailblaze

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import projeto.g54.trailblaze.databinding.FragmentAddAmigosGrupoBinding
import projeto.g54.trailblaze.databinding.ItemAmigoAddBinding
import projeto.g54.trailblaze.models.Friends
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDateTime


class AddAmigosGrupoFragment : Fragment() {
    private var adapter: FindFriendsFirestoreRecyclerAdapter? = null
    private var _binding: FragmentAddAmigosGrupoBinding? = null
    private val binding get() = _binding!!

    private var groupId: String? = null
    private lateinit var searchView: SearchView
    private lateinit var searchViewList: RecyclerView
    private lateinit var groupRef: DocumentSnapshot
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var groupArray: ArrayList<String>
    private lateinit var onlineId: String
    private lateinit var userRef: CollectionReference

    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment

        _binding = FragmentAddAmigosGrupoBinding.inflate(inflater, container, false)

        searchView = binding.searchView

        searchViewList = binding.amigosListView

        groupId = arguments?.getString("groupId")
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        onlineId = auth.currentUser!!.uid
        userRef = db.collection("users")
//        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
//            override fun onQueryTextSubmit(query: String?): Boolean {
//                return true
//            }
//
//            override fun onQueryTextChange(newText: String?): Boolean {
//                searchPeople(newText!!)
//                return true
//            }
//
//        })
        searchPeople()
        return binding.root
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

    private fun searchPeople() = CoroutineScope(Dispatchers.IO).launch {
        try {
            groupRef = groupId?.let { db.collection("Groups").document(it).get().await() }!!
            val data = groupRef.data
            groupArray = data?.get("groupArray") as ArrayList<String>
            val friendRef = db.collection("Friends").document("FriendDocument").collection(onlineId)
            val options = FirestoreRecyclerOptions.Builder<Friends>()
                .setQuery(friendRef, Friends::class.java).build()

            withContext(Dispatchers.Main) {
                adapter = FindFriendsFirestoreRecyclerAdapter(options, requireContext())
                adapter!!.startListening()
                searchViewList.adapter = adapter
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onStop() {
        super.onStop()

        if (adapter != null) {
            adapter!!.stopListening()
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun addGroup(usersID: String) {
        val currentDateTime = LocalDateTime.now()
        db.collection("users").document(onlineId).get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot!!.exists()) {
                val docSent = hashMapOf(
                    "idInvite" to groupId,
                    "idReceived" to usersID,
                    "nome" to documentSnapshot.getString("nome"),
                    "photoUrl" to documentSnapshot.getString("photoUrl"),
                    "type" to "Group",
                    "time" to currentDateTime
                )
                db.collection("Invites").document()
                    .set(docSent)
            }
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
        options: FirestoreRecyclerOptions<Friends>,
        private val ctx: Context
    ) :
        FirestoreRecyclerAdapter<Friends, FindFriendsViewHolder>(options) {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onBindViewHolder(
            findFriendsViewHolder: FindFriendsViewHolder,
            position: Int,
            addFriends: Friends
        ) {
            val usersID = snapshots.getSnapshot(position).id
            userRef.document(usersID).addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists() && !groupArray.contains(usersID)) {
                    val userName: String = snapshot.getString("nome")!!
                    val photoUrl: String = snapshot.getString("photoUrl")!!
                    findFriendsViewHolder.setVariables(userName, photoUrl, ctx)
                    db.collection("Invites").whereEqualTo("idReceived", usersID).get()
                        .addOnSuccessListener {
                            if (it.documents.isEmpty()) {
                                findFriendsViewHolder.amigoBinding.adicionar.setOnClickListener {
                                    addGroup(usersID)
                                }
                            } else {
                                for (invite in it.documents) {
                                    if (invite.getString("type").equals("Group")) {
                                        findFriendsViewHolder.amigoBinding.adicionar.visibility = View.GONE
                                    } else {
                                        findFriendsViewHolder.amigoBinding.adicionar.setOnClickListener {
                                            addGroup(usersID)
                                        }
                                    }
                                }
                            }
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
