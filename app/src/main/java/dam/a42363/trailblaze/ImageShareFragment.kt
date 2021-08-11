package dam.a42363.trailblaze

import android.R.attr
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import dam.a42363.trailblaze.databinding.FragmentImageShareBinding
import dam.a42363.trailblaze.databinding.FragmentPerfilBinding
import android.graphics.Bitmap
import android.R.attr.bitmap
import android.content.ContentValues
import android.util.Log


class ImageShareFragment : Fragment() {

    private lateinit var navController: NavController

    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var fullImage: AppCompatImageView

    var _binding: FragmentImageShareBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment

        _binding = FragmentImageShareBinding.inflate(inflater, container, false)
        val url = arguments?.getString("url")

        toolbar = binding.toolbar
        fullImage = binding.fullImage

        toolbar.inflateMenu(R.menu.share_menu)

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.shareBtn -> {

                    val drawable = fullImage.drawable as BitmapDrawable
                    val bitmap = drawable.bitmap

                    val bitmapPath: String = ContentValues().apply {

                    }.toString()


//                    val bitmapPath: String = MediaStore.Images.Media.inserImage(
//                        requireActivity().contentResolver,
//                        attr.bitmap,
//                        "title",
//                        null
//                    )

                    val uri = Uri.parse(bitmapPath)

                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "image/png"
                    intent.putExtra(Intent.EXTRA_STREAM, uri)
                    intent.putExtra(Intent.EXTRA_TEXT, " ")
                    startActivity(Intent.createChooser(intent, "Share"))

                    true
                }
                else -> false
            }
        }

        return binding.root
    }

}