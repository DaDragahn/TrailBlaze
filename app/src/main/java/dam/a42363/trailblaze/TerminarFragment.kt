package dam.a42363.trailblaze

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import dam.a42363.trailblaze.databinding.FragmentTerminarBinding

import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.storage.FirebaseStorage
import dam.a42363.trailblaze.adapters.ImageAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception


class TerminarFragment : Fragment() {

    private lateinit var navController: NavController

    var _binding: FragmentTerminarBinding? = null
    private val binding get() = _binding!!


    private val storageRef = FirebaseStorage.getInstance().reference


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentTerminarBinding.inflate(inflater, container, false)


        listFiles()

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)


        val ratingBar = binding.ratingBar
        val finalizarBtn = binding.finalizarBtn

        finalizarBtn.setOnClickListener {
            val msg = ratingBar.rating.toString()
            Toast.makeText(requireContext(), "Rating is:$msg", Toast.LENGTH_SHORT).show()
        }

        if (activity != null && this.activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.GONE
        }

        val message = binding.messageTxt

        ratingBar.setOnRatingBarChangeListener { ratingBar, rating, fromUser ->
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

        val shareBtn = binding.shareBtn

        shareBtn.setOnClickListener {

            val i = Intent(Intent.ACTION_SEND)
            i.type = "text/plain"
            val sharebody = "look all Programmings"
            val subject = "http://www.youtube.com/avadhtutor/pla..."
            i.putExtra(Intent.EXTRA_SUBJECT, sharebody)
            i.putExtra(Intent.EXTRA_TEXT, subject)
            startActivity(Intent.createChooser(i, "TrailBlaze"))

//            val bitmap = fullImage.getDrawable().bitmap
//
//            val bitmapPath: String =
//                MediaStore.Images.Media.inserImage(getContentResolver(), bitmap, "title", null)
//
//            val uri = Uri.parse(bitmapPath)
//
//            val intent = Intent(Intent.ACTION_SEND)
//            intent.type = "image/png"
//            intent.putExtra(Intent.EXTRA_STREAM, uri)
//            intent.putExtra(Intent.EXTRA_TEXT, " ")
//            startActivity(Intent.createChooser(intent, "Share"))

        }


    }

    private fun listFiles() = CoroutineScope(Dispatchers.IO).launch {

        try {
            val images = storageRef.child("testeShare/").listAll().await()
            val imageUrls = mutableListOf<String>()
            for (image in images.items) {
                val url = image.downloadUrl.await()
                imageUrls.add(url.toString())
            }

            val rvFotos = binding.fotosListView

            withContext(Dispatchers.Main) {
                val imageAdapter = ImageAdapter(imageUrls)
                rvFotos.apply {
                    adapter = imageAdapter
//                    layoutManager = GridLayoutManager(requireContext(), 2)
                }

            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
        }

    }

}