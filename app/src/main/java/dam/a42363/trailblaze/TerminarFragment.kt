package dam.a42363.trailblaze

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dam.a42363.trailblaze.adapters.ImageAdapter
import dam.a42363.trailblaze.databinding.FragmentTerminarBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class TerminarFragment : Fragment() {

    private var user: String? = null
    private var idTrail: String? = null
    private lateinit var navController: NavController

    var _binding: FragmentTerminarBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val storageRef = FirebaseStorage.getInstance().reference


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentTerminarBinding.inflate(inflater, container, false)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        user = auth.currentUser?.uid
        idTrail = arguments?.getString("idTrail")
        listFiles()

        // Inflate the layout for this fragment
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)
        val time: String? = arguments?.getString("time")
        binding.tempoPercurso.text = "Tempo: $time"
        db.collection("locations").document(idTrail!!).get().addOnSuccessListener {
            binding.nomePercurso.text = "Nome: ${it?.getString("nome")}"
            binding.distanciaPercurso.text =
                "Distância: ${it?.getString("distancia")}"
        }
        val ratingBar = binding.ratingBar
        val finalizarBtn = binding.finalizarBtn

        finalizarBtn.setOnClickListener {
            val msg = ratingBar.rating.toString()
            val updates: MutableMap<String, Any> = HashMap()
            updates["rating"] = msg
            db.collection("Rating").document("RatingDocument").collection(idTrail!!)
                .document(user!!).set(updates)
            navController.navigate(R.id.action_terminarFragment_to_explorarFragment)
        }

        if (activity != null && this.activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.GONE
        }

        val message = binding.messageTxt

        ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            if (rating == 0f) {
                message.text = ""
            }
            if (rating > 0.0 && rating <= 1.0) {
                message.text = "Bummer"
            }
            if (rating <= 2.0 && rating > 1.0) {
                message.text = "Could be better"
            }
            if (rating <= 3.0 && rating > 2.0) {
                message.text = "Not Bad"
            }
            if (rating <= 4.0 && rating > 3.0) {
                message.text = "Nice Experience"
            }
            if (rating <= 5.0 && rating > 4.0) {
                message.text = "Loved it!!"
            }
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
                    ImageAdapter(imageUrls, mutableListOf(), navController, false)
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