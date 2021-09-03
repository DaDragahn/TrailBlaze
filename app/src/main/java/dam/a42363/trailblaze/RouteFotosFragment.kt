package dam.a42363.trailblaze

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dam.a42363.trailblaze.adapters.ImageAdapter
import dam.a42363.trailblaze.databinding.FragmentRouteFotosBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class RouteFotosFragment : Fragment() {
    private lateinit var storage: StorageReference
    private lateinit var path: String
    private lateinit var fotosListView: RecyclerView
    private lateinit var navController: NavController
    private var _binding: FragmentRouteFotosBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRouteFotosBinding.inflate(inflater, container, false)

        fotosListView = binding.fotosListView
        path = arguments?.getString("path").toString()
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
            val images = storage.child(path).listAll().await()
            val imageUrls = mutableListOf<String>()
            for (image in images.items) {
                val url = image.downloadUrl.await()
                imageUrls.add(url.toString())
            }
            withContext(Dispatchers.Main) {
                val imageAdapter =
                    ImageAdapter(imageUrls, mutableListOf(), mutableListOf(), navController, false)
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