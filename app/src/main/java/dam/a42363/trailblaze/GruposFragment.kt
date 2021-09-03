package dam.a42363.trailblaze

import android.content.Context
import android.os.Bundle
import android.view.*
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dam.a42363.trailblaze.databinding.FragmentGruposBinding
import dam.a42363.trailblaze.databinding.ItemGrupoBinding
import dam.a42363.trailblaze.models.Grupo

class GruposFragment : Fragment() {

    private lateinit var navController: NavController
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar

    private var _binding: FragmentGruposBinding? = null
    private val binding get() = _binding!!
    private lateinit var groupoListView: RecyclerView
    private lateinit var grupoRef: Query
    private lateinit var auth: FirebaseAuth
    private lateinit var onlineId: String
    private lateinit var db: FirebaseFirestore
    private var adapter: GroupFinderFirestoreRecyclerAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        _binding = FragmentGruposBinding.inflate(inflater, container, false)

        toolbar = binding.toolbar

        toolbar.inflateMenu(R.menu.grupo_menu)

        auth = FirebaseAuth.getInstance()
        onlineId = auth.currentUser!!.uid
        db = FirebaseFirestore.getInstance()

        grupoRef = db.collection("Groups")

        groupoListView = binding.grupoListView
        displayUserGroups()
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.grupo_menu, menu)
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

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.adicionarGrupo -> {
                    navController.navigate(R.id.action_gruposFragment_to_addGruposFragment)
                    true
                }

                else -> false
            }
        }

    }

    private fun displayUserGroups() {
        val options = FirestoreRecyclerOptions.Builder<Grupo>()
            .setQuery(grupoRef, Grupo::class.java).build()
        adapter = GroupFinderFirestoreRecyclerAdapter(options, requireContext())
        adapter!!.startListening()
        groupoListView.adapter = adapter

    }

    override fun onStop() {
        super.onStop()

        if (adapter != null) {
            adapter!!.stopListening()
        }
    }

    inner class GroupFinderViewHolder(val grupoBinding: ItemGrupoBinding) :
        RecyclerView.ViewHolder(grupoBinding.root) {
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
                    val bundle = bundleOf("groupID" to snapshots.getSnapshot(position).id)
                    navController.navigate(R.id.action_gruposFragment_to_grupoFragment, bundle)
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
}