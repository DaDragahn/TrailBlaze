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
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dam.a42363.trailblaze.databinding.FragmentNotificationsBinding
import dam.a42363.trailblaze.databinding.ItemNotificationBinding
import dam.a42363.trailblaze.models.Invites
import java.time.LocalDateTime
import java.time.Month

class NotificationsFragment : Fragment() {
    private var adapter: GetInvitesFirestoreRecyclerAdapter? = null
    private lateinit var navController: NavController
    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private lateinit var locationRef: CollectionReference
    private lateinit var grupoRef: CollectionReference
    private lateinit var auth: FirebaseAuth
    private lateinit var onlineId: String
    private lateinit var inviteRef: Query
    private lateinit var inviteListView: RecyclerView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        onlineId = auth.currentUser!!.uid

        locationRef = db.collection("locations")
        grupoRef = db.collection("Groups")
        inviteListView = binding.inviteListView
        displayAllNotifications()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        if (activity != null && this.activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.VISIBLE
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun displayAllNotifications() {
        inviteRef = db.collection("Invites")

        val options = FirestoreRecyclerOptions.Builder<Invites>()
            .setQuery(inviteRef, Invites::class.java).build()

        adapter = GetInvitesFirestoreRecyclerAdapter(options, requireContext())
        adapter!!.startListening()
        inviteListView.adapter = adapter
    }

    override fun onStop() {
        super.onStop()

        if (adapter != null) {
            adapter!!.stopListening()
        }
    }

    inner class GetInvitesViewHolder(val notificationBinding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(notificationBinding.root) {
        @SuppressLint("SetTextI18n")
        fun setTrailVariables(
            nome: String,
            photoUrl: String,
            routeName: String,
            ctx: Context
        ) {
            notificationBinding.name.text =
                "$nome ${getString(R.string.convitePercurso)} $routeName"
            Glide.with(ctx).load(photoUrl)
                .into(notificationBinding.profileImage)
        }

        @SuppressLint("SetTextI18n")
        fun setGroupVariables(
            nome: String,
            photoUrl: String,
            grupoName: String,
            ctx: Context
        ) {
            notificationBinding.name.text =
                "$nome ${getString(R.string.conviteGrupo)} $grupoName"
            Glide.with(ctx).load(photoUrl)
                .into(notificationBinding.profileImage)
        }
    }

    private inner class GetInvitesFirestoreRecyclerAdapter(
        options: FirestoreRecyclerOptions<Invites>,
        private val ctx: Context
    ) :
        FirestoreRecyclerAdapter<Invites, GetInvitesViewHolder>(options) {

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onBindViewHolder(
            holder: GetInvitesViewHolder,
            position: Int,
            model: Invites
        ) {
            val idReceived = snapshots.getSnapshot(position).getString("idReceived")
            if (idReceived.equals(onlineId)) {
                val idInvite = snapshots.getSnapshot(position).getString("idInvite")
                val id = snapshots.getSnapshot(position).id
                val name = snapshots.getSnapshot(position).getString("nome")
                val photoUrl = snapshots.getSnapshot(position).getString("photoUrl")
                when (snapshots.getSnapshot(position).getString("type")) {
                    "Group" -> {
                        if (idInvite != null) {
                            grupoRef.document(idInvite).addSnapshotListener { snapshot, _ ->
                                if (snapshot != null && snapshot.exists()) {
                                    val data = snapshot.data
                                    val grupoName: String = data?.get("nome") as String
                                    val groupArray: ArrayList<String> =
                                        data["groupArray"] as ArrayList<String>
                                    if (name != null) {
                                        if (photoUrl != null) {
                                            holder.setGroupVariables(name, photoUrl, grupoName, ctx)
                                        }
                                    }
                                    holder.notificationBinding.acceptBtn.setOnClickListener {
                                        groupArray.add(onlineId)

                                        db.collection("Groups").document(idInvite)
                                            .update("groupArray", groupArray)
                                        db.collection("Invites")
                                            .document(id).delete()
                                    }
                                    holder.notificationBinding.excludeBtn.setOnClickListener {
                                        db.collection("Invites")
                                            .document(id).delete()
                                    }
                                }
                            }
                        }
                    }
                    "Trail" -> {
                        var currentDateTime = LocalDateTime.now()
                        currentDateTime = currentDateTime.plusHours(1)
                        val timeMap = snapshots.getSnapshot(position).get("time") as HashMap<*, *>

                        val time = LocalDateTime.of(
                            (timeMap["year"] as Long).toInt(),
                            Month.valueOf(timeMap["month"] as String),
                            (timeMap["dayOfMonth"] as Long).toInt(),
                            (timeMap["hour"] as Long).toInt(),
                            (timeMap["minute"] as Long).toInt(),
                            (timeMap["second"] as Long).toInt()
                        )
                        if (time.isBefore(currentDateTime)) {
                            val idRoute = snapshots.getSnapshot(position).getString("idRoute")

                            if (idRoute != null) {
                                locationRef.document(idRoute).addSnapshotListener { snapshot, _ ->
                                    if (snapshot != null && snapshot.exists()) {
                                        val routeName = snapshot.getString("nome")!!
                                        val routeInfo = snapshot.getString("route")!!
                                        if (name != null) {
                                            if (photoUrl != null) {
                                                holder.setTrailVariables(
                                                    name,
                                                    photoUrl,
                                                    routeName,
                                                    ctx
                                                )
                                            }
                                        }
                                        holder.notificationBinding.acceptBtn.setOnClickListener {
                                            val lobbySent = hashMapOf(
                                                "nome" to auth.currentUser!!.displayName,
                                                "LastLocation" to ""
                                            )
                                            if (idInvite != null) {
                                                db.collection("Trails").document(idInvite)
                                                    .collection("TrailsCollection")
                                                    .document(onlineId)
                                                    .set(lobbySent)
                                            }
                                            db.collection("Invites")
                                                .document(id).delete()
                                            val bundle =
                                                bundleOf(
                                                    "route" to routeInfo,
                                                    "idLobby" to idInvite,
                                                    "idTrail" to idRoute
                                                )

                                            navController.navigate(
                                                R.id.action_notificationsFragment_to_navigationFragment,
                                                bundle
                                            )
                                        }
                                        holder.notificationBinding.excludeBtn.setOnClickListener {
                                            db.collection("Invites")
                                                .document(id).delete()
                                        }
                                    }
                                }
                            }
                        } else {
                            holder.notificationBinding.cardView.visibility = View.GONE
                        }
                    }
                }
            } else {
                holder.notificationBinding.cardView.visibility = View.GONE
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): GetInvitesViewHolder {
            return GetInvitesViewHolder(
                ItemNotificationBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent, false
                )
            )
        }
    }
}