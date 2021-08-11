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
import androidx.core.net.toUri
import com.bumptech.glide.Glide


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

        Glide.with(this).load(url).into(fullImage)

        toolbar.inflateMenu(R.menu.share_menu)

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.shareBtn -> {


//                    val uri = Uri.parse(url?.toUri().toString())

                    if (url != null) {
                        Log.d("TAG", url.toUri().toString())
                    }

                    val shareIntent: Intent = Intent().apply {
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, url?.toUri())
                        type = "image/*"
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share"))


//                    val intent = Intent(Intent.ACTION_SEND)
//                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//                    intent.type = "image/jpg"
//                    intent.putExtra(Intent.EXTRA_TEXT, "Teste")
//                    if (url != null) {
//                        intent.putExtra(Intent.EXTRA_STREAM, url.toUri())
//                    }
//                    startActivity(Intent.createChooser(intent, "Share"))

                    true
                }
                else -> false
            }
        }

        return binding.root
    }

}