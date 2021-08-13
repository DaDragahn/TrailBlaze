package dam.a42363.trailblaze

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import dam.a42363.trailblaze.databinding.FragmentGrupoBinding
import dam.a42363.trailblaze.databinding.ItemAmigoBinding
import dam.a42363.trailblaze.models.AddFriends
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.InputStream

class GrupoFragment : Fragment() {

    private lateinit var groupArray: ArrayList<String>
    private lateinit var amigosListView: RecyclerView
    private lateinit var adapter: GroupFinderFirestoreRecyclerAdapter
    private lateinit var userRef: CollectionReference
    private lateinit var groupRef: DocumentSnapshot
    private var groupId: String? = null
    private lateinit var db: FirebaseFirestore
    private lateinit var onlineId: String
    private lateinit var auth: FirebaseAuth
    private lateinit var navController: NavController
    private lateinit var img: ByteArray
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar

    var _binding: FragmentGrupoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment

        _binding = FragmentGrupoBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        onlineId = auth.currentUser!!.uid
        db = FirebaseFirestore.getInstance()

        val storageRef = FirebaseStorage.getInstance().reference

        groupId = arguments?.getString("groupID")

        userRef = db.collection("users")

        amigosListView = binding.amigosListView

        val getImage = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) {
            binding.groupImage.setImageURI(it)
            val iStream: InputStream? = requireActivity().contentResolver.openInputStream(it)
            img = iStream?.readBytes()!!

            val userRefImagesRef = storageRef.child("images/groups/${groupId}/groupPhoto.jpg")
            val uploadTask: UploadTask = userRefImagesRef.putBytes(img)
            uploadTask.addOnFailureListener {
                // Handle unsuccessful uploads
            }
                .addOnSuccessListener {   // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                    Log.d("RecordRoute", it.metadata.toString())
                    userRefImagesRef.downloadUrl.addOnSuccessListener { uri ->
                        val photoUrl = "$uri"
                        val updates: MutableMap<String, Any> = HashMap()
                        updates["photoUrl"] = photoUrl
                        db.collection("Groups").document(groupId!!)
                            .update(updates)
                    }
                }
        }

        toolbar = binding.toolbar
        toolbar.inflateMenu(R.menu.invite_menu)

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.invite -> {
                    val bundle = bundleOf("groupId" to groupId)
                    navController.navigate(
                        R.id.action_grupoFragment_to_addAmigosGrupoFragment,
                        bundle
                    )
                    true
                }
                R.id.leave -> {
                    leaveGroup()
                    true
                }

                R.id.editarFoto -> {
                    getImage.launch("image/*")
                    true
                }

                else -> false
            }
        }
        db.collection("Groups").document(groupId!!).get().addOnSuccessListener {
            val photoUrl = it.getString("photoUrl").toString()
            if (photoUrl != "")
                Glide.with(this).load(photoUrl).override(SIZE_ORIGINAL, SIZE_ORIGINAL)
                    .into(binding.groupImage)
        }
        displayGroupMembers()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        if (activity != null && this.activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.VISIBLE
        }
    }

    private fun leaveGroup() = CoroutineScope(Dispatchers.IO).launch {
        try {
            db.collection("Groups").document(groupId!!)
                .update("groupArray", FieldValue.arrayRemove(onlineId)).await()
            withContext(Dispatchers.Main) {
                navController.navigate(R.id.action_grupoFragment_to_gruposFragment)
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun displayGroupMembers() = CoroutineScope(Dispatchers.IO).launch {
        try {
            groupRef = groupId?.let { db.collection("Groups").document(it).get().await() }!!
            val data = groupRef.data
            groupArray = data?.get("groupArray") as ArrayList<String>
            withContext(Dispatchers.Main) {
                val options = FirestoreRecyclerOptions.Builder<AddFriends>()
                    .setQuery(userRef, AddFriends::class.java).build()
                adapter = GroupFinderFirestoreRecyclerAdapter(options, requireContext())
                adapter.startListening()
                amigosListView.adapter = adapter
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    inner class GroupFinderViewHolder(val amigoBinding: ItemAmigoBinding) :
        RecyclerView.ViewHolder(amigoBinding.root) {
        fun setVariables(nome: String, photoUrl: String, ctx: Context) {
            amigoBinding.name.text = nome
            Glide.with(ctx).load(photoUrl)
                .into(amigoBinding.profileImage)
        }
    }

    private inner class GroupFinderFirestoreRecyclerAdapter(
        options: FirestoreRecyclerOptions<AddFriends>,
        private val ctx: Context
    ) :
        FirestoreRecyclerAdapter<AddFriends, GroupFinderViewHolder>(options) {

        override fun onBindViewHolder(
            holder: GroupFinderViewHolder,
            position: Int,
            model: AddFriends
        ) {
            holder.amigoBinding.adicionar.visibility = View.GONE
            val data = snapshots.getSnapshot(position).data
            val id = snapshots.getSnapshot(position).id
            if (groupArray.contains(id)) {
                holder.setVariables(data?.get("nome") as String, data["photoUrl"] as String, ctx)
            } else {
                holder.amigoBinding.cardView.visibility = View.GONE
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): GroupFinderViewHolder {
            return GroupFinderViewHolder(
                ItemAmigoBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent, false
                )
            )
        }
    }
}