package dam.a42363.trailblaze

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dam.a42363.trailblaze.adapters.ListImageAdapter
import dam.a42363.trailblaze.databinding.FragmentListarImagensPartilharBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ListarImagensPartilharFragment : Fragment() {

    private var user: String? = null
    private lateinit var auth: FirebaseAuth
    private var idTrail: String? = null
    private var _binding: FragmentListarImagensPartilharBinding? = null
    private val binding get() = _binding!!

    private lateinit var navController: NavController
    private val storageRef = FirebaseStorage.getInstance().reference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListarImagensPartilharBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        user = auth.currentUser?.uid
        idTrail = arguments?.getString("idTrail")

        listFiles()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        if (activity != null && this.activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.GONE
        }

    }

    private fun listFiles() = CoroutineScope(Dispatchers.IO).launch {

        try {
            val images = storageRef.child("images/${user}/locations/${idTrail}").listAll().await()

            val imageUrls = mutableListOf<String>()
            for (image in images.items) {
                val url = image.downloadUrl.await()
                imageUrls.add(url.toString())
            }

            val rvFotos = binding.fotosListView

            withContext(Dispatchers.Main) {
                val imageAdapter =
                    ListImageAdapter(imageUrls, navController, (activity as MainActivity))
                rvFotos.apply {
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