package dam.a42363.trailblaze

import android.annotation.SuppressLint
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
import dam.a42363.trailblaze.databinding.FragmentNotificationsBinding
import dam.a42363.trailblaze.databinding.ItemAmigoBinding
import dam.a42363.trailblaze.databinding.ItemNotificationBinding
import dam.a42363.trailblaze.models.Invites
import java.time.LocalDateTime

class NotificationsFragment : Fragment() {
    private var adapter: GetInvitesFirestoreRecyclerAdapter? = null
    private lateinit var navController: NavController
    var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private lateinit var locationRef: CollectionReference
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
        var currentDateTime = LocalDateTime.now()
        currentDateTime = currentDateTime.minusHours(1)
        inviteRef = db.collection("Invites").document("InviteDocument").collection(onlineId)
            .whereGreaterThan("time", currentDateTime)

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
        fun setVariables(
            nome: String,
            photoUrl: String,
            routeName: String,
            ctx: Context
        ) {
            notificationBinding.name.text = "$nome enviou-te um convite para percorrer $routeName"
            Glide.with(ctx).load(photoUrl)
                .into(notificationBinding.profileImage)
        }
    }

    private inner class GetInvitesFirestoreRecyclerAdapter(
        options: FirestoreRecyclerOptions<Invites>,
        private val ctx: Context
    ) :
        FirestoreRecyclerAdapter<Invites, GetInvitesViewHolder>(options) {

        override fun onBindViewHolder(
            holder: GetInvitesViewHolder,
            position: Int,
            model: Invites
        ) {
            val id = snapshots.getSnapshot(position).id
            val name = snapshots.getSnapshot(position).getString("nome")
            val photoUrl = snapshots.getSnapshot(position).getString("photoUrl")
            val idTrail = snapshots.getSnapshot(position).getString("idTrail")
            val idRoute = snapshots.getSnapshot(position).getString("idRoute")

            if (idRoute != null) {
                locationRef.document(idRoute).addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        val routeName = snapshot.getString("nome")!!
                        val routeInfo = snapshot.getString("route")!!
                        if (name != null) {
                            if (photoUrl != null) {
                                holder.setVariables(name, photoUrl, routeName, ctx)
                            }
                        }
                        holder.notificationBinding.acceptBtn.setOnClickListener {
                            val lobbySent = hashMapOf(
                                "nome" to auth.currentUser!!.displayName,
                                "LastLocation" to ""
                            )
                            if (idTrail != null) {
                                db.collection("Trails").document(idTrail)
                                    .collection("TrailsCollection").document(onlineId)
                                    .set(lobbySent)
                            }
                            db.collection("Invites").document("InviteDocument").collection(onlineId)
                                .document(id).delete()
                            val bundle = bundleOf("route" to routeInfo, "idTrail" to idTrail)

                            navController.navigate(
                                R.id.action_notificationsFragment_to_navigationFragment,
                                bundle
                            )
                        }
                        holder.notificationBinding.excludeBtn.setOnClickListener {
                            db.collection("Invites").document("InviteDocument").collection(onlineId)
                                .document(id).delete()
                        }
                    }
                }
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