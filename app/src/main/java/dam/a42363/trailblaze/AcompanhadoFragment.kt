package dam.a42363.trailblaze

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
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
import com.bumptech.glide.request.target.Target
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dam.a42363.trailblaze.databinding.FragmentAcompanhadoBinding
import dam.a42363.trailblaze.databinding.ItemAmigoConvidarBinding
import dam.a42363.trailblaze.databinding.ItemGrupoBinding
import dam.a42363.trailblaze.models.Friends
import dam.a42363.trailblaze.models.Grupo
import java.time.LocalDateTime


class AcompanhadoFragment : Fragment() {

    var _binding: FragmentAcompanhadoBinding? = null
    private val binding get() = _binding!!

    private lateinit var amigosListView: RecyclerView
    private lateinit var grupoListView: RecyclerView

    private lateinit var db: FirebaseFirestore
    private lateinit var userRef: CollectionReference
    private lateinit var friendRef: Query
    private lateinit var grupoRef: Query
    private lateinit var auth: FirebaseAuth

    private lateinit var onlineId: String

    private lateinit var navController: NavController

    private var amigoAdapter: FindFriendsFirestoreRecyclerAdapter? = null
    private var grupoAdapter: GroupFinderFirestoreRecyclerAdapter? = null
    private val friendsArray = ArrayList<String>()

    private var optimizedRoute: String? = null
    private var feature: String? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentAcompanhadoBinding.inflate(inflater, container, false)
        amigosListView = binding.amigosListView
        grupoListView = binding.grupoListView
        auth = FirebaseAuth.getInstance()
        onlineId = auth.currentUser!!.uid
        db = FirebaseFirestore.getInstance()
        friendRef = db.collection("Friends").document("FriendDocument").collection(onlineId)
        userRef = db.collection("users")
        grupoRef = db.collection("Groups")

        displayAllFriends()
        displayAllGroups()

        optimizedRoute = arguments?.getString("route")!!
        feature = arguments?.getString("feature")!!

        binding.start.setOnClickListener {
            inviteAndStartTrail()
        }

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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun inviteAndStartTrail() {
        val currentDateTime = LocalDateTime.now()

        val lobby = db.collection("Trails").document()

        db.collection("users").document(onlineId).get().addOnCompleteListener {
            if (it.isSuccessful) {
                val documentSnapshot = it.result
                if (documentSnapshot!!.exists()) {
                    for (id in friendsArray) {
                        val docSent = hashMapOf(
                            "idInvite" to lobby.id,
                            "idReceived" to id,
                            "nome" to documentSnapshot.getString("nome"),
                            "photoUrl" to documentSnapshot.getString("photoUrl"),
                            "idRoute" to feature,
                            "time" to currentDateTime,
                            "type" to "Trail"
                        )
                        db.collection("Invites").document()
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

        val bundle = bundleOf(
            "route" to optimizedRoute,
            "idTrail" to feature,
            "idLobby" to lobby.id,
            "individual" to false
        )

        navController.navigate(
            R.id.action_escolherModoFragment_to_navigationFragment,
            bundle
        )
    }

    private fun displayAllFriends() {
        val options = FirestoreRecyclerOptions.Builder<Friends>()
            .setQuery(friendRef, Friends::class.java).build()

        amigoAdapter = FindFriendsFirestoreRecyclerAdapter(options, requireContext())
        amigoAdapter!!.startListening()
        amigosListView.adapter = amigoAdapter

    }

    private fun displayAllGroups() {
        val options = FirestoreRecyclerOptions.Builder<Grupo>()
            .setQuery(grupoRef, Grupo::class.java).build()

        grupoAdapter = GroupFinderFirestoreRecyclerAdapter(options, requireContext())
        grupoAdapter!!.startListening()
        grupoListView.adapter = grupoAdapter
    }

    override fun onStop() {
        super.onStop()

        if (amigoAdapter != null) {
            amigoAdapter!!.stopListening()
        }
        if (grupoAdapter != null) {
            grupoAdapter!!.stopListening()
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

    inner class GroupFinderViewHolder(val grupoBinding: ItemGrupoBinding) :
        RecyclerView.ViewHolder(grupoBinding.root) {
        @SuppressLint("SetTextI18n")
        fun setVariables(nome: String, photoUrl: String, membros: String, ctx: Context) {
            grupoBinding.name.text = nome
            grupoBinding.membros.text = membros
            Glide.with(ctx).load(photoUrl).override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .into(grupoBinding.image)
        }
    }

    private inner class GroupFinderFirestoreRecyclerAdapter(
        options: FirestoreRecyclerOptions<Grupo>,
        private val ctx: Context
    ) :
        FirestoreRecyclerAdapter<Grupo, GroupFinderViewHolder>(options) {

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onBindViewHolder(
            holder: GroupFinderViewHolder,
            position: Int,
            model: Grupo
        ) {
            val data = snapshots.getSnapshot(position).data
            val groupArray: List<String> = data?.get("groupArray") as List<String>
            if (groupArray.contains(onlineId)) {
                val nome: String = data["nome"] as String
                val photoUrl = data["photoUrl"].toString()
                holder.setVariables(nome, photoUrl, groupArray.size.toString(), ctx)
                holder.grupoBinding.cardView.setOnClickListener {
                    binding.decisionCardView.visibility = View.VISIBLE
                    binding.aceitarBtn.setOnClickListener {
                        inviteGroupandStartTrail(groupArray)
                    }
                    binding.recusarBtn.setOnClickListener {
                        binding.decisionCardView.visibility = View.GONE
                    }
                }
            } else {
                holder.grupoBinding.cardView.visibility = View.GONE
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): GroupFinderViewHolder {
            return GroupFinderViewHolder(
                ItemGrupoBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent, false
                )
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun inviteGroupandStartTrail(groupArray: List<String>) {
        val currentDateTime = LocalDateTime.now()

        val lobby = db.collection("Trails").document()

        db.collection("users").document(onlineId).get().addOnCompleteListener {
            if (it.isSuccessful) {
                val documentSnapshot = it.result
                if (documentSnapshot!!.exists()) {
                    for (userID in groupArray) {
                        if (userID != onlineId) {
                            val docSent = hashMapOf(
                                "idInvite" to lobby.id,
                                "idReceived" to userID,
                                "nome" to documentSnapshot.getString("nome"),
                                "photoUrl" to documentSnapshot.getString("photoUrl"),
                                "idRoute" to feature,
                                "time" to currentDateTime,
                                "type" to "Trail"
                            )
                            db.collection("Invites").document()
                                .set(docSent)
                        }
                    }
                    val lobbySent = hashMapOf(
                        "nome" to documentSnapshot.getString("nome"),
                        "LastLocation" to ""
                    )
                    lobby.collection("TrailsCollection").document(onlineId).set(lobbySent)
                }
            }
        }

        val bundle =
            bundleOf(
                "route" to optimizedRoute,
                "idTrail" to feature,
                "idLobby" to lobby.id,
                "individual" to false
            )

        navController.navigate(
            R.id.action_escolherModoFragment_to_navigationFragment,
            bundle
        )
    }
}