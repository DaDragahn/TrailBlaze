package dam.a42363.trailblaze

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dam.a42363.trailblaze.adapters.ImageAdapter
import dam.a42363.trailblaze.databinding.FragmentFotosBinding
import dam.a42363.trailblaze.databinding.FragmentTrailsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class FotosFragment : Fragment() {

    private lateinit var storage: StorageReference
    private lateinit var onlineId: String
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var fotosListView: RecyclerView
    private lateinit var navController: NavController
    var _binding: FragmentFotosBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFotosBinding.inflate(inflater, container, false)

        fotosListView = binding.fotosListView
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        onlineId = auth.currentUser!!.uid
        storage = FirebaseStorage.getInstance().reference

        listPhotos()

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

    private fun listPhotos() = CoroutineScope(Dispatchers.IO).launch {
        try {
            val loc = db.collection("locations").get().await()
            val images = storage.child("images/${onlineId}/locations").listAll().await()
            val imageUrls = mutableListOf<String>()
            val imagePaths = mutableListOf<String>()
            val imageName = mutableListOf<String>()
            for (image in images.prefixes) {
                for (doc in loc.documents) {
                    val routeName = doc.getString("nome")
                    if (doc.id == image.name) {
                        val inside = image.listAll().await()
                        val url = inside.items[0].downloadUrl.await()
                        imagePaths.add(image.path)
                        imageUrls.add(url.toString())
                        imageName.add(routeName!!)
                    }
                }
            }
            withContext(Dispatchers.Main) {
                val imageAdapter = ImageAdapter(imageUrls, imagePaths, imageName, navController, false)
                fotosListView.apply {
                    adapter = imageAdapter
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}