package dam.a42363.trailblaze

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore.Images
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import com.bumptech.glide.Glide
import dam.a42363.trailblaze.databinding.FragmentImageShareBinding


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
        _binding = FragmentImageShareBinding.inflate(inflater, container, false)
        val url = arguments?.getString("url")
        //Log.d("RecordRoute", url.toString())
        val loadedImage: Bitmap = Glide
            .with(requireContext())
            .asBitmap()
            .load(url)
            .submit()
            .get()
        val path: String =
            Images.Media.insertImage(requireActivity().contentResolver, loadedImage, "", null)
        val screenshotUri = Uri.parse(path)
        toolbar = binding.toolbar
        fullImage = binding.fullImage

        toolbar.inflateMenu(R.menu.share_menu)

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.shareBtn -> {
                    val shareIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, screenshotUri)
                        type = "image/*"
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    startActivity(Intent.createChooser(shareIntent, "Teste"))

                    true
                }
                else -> false
            }
        }

        return binding.root
    }

}