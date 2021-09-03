package dam.a42363.trailblaze

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore.Images
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dam.a42363.trailblaze.databinding.FragmentImageShareBinding


class ImageShareFragment : Fragment() {

    private lateinit var navController: NavController

    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var fullImage: AppCompatImageView

    private lateinit var rotateLeftBtn: FloatingActionButton

    private var _binding: FragmentImageShareBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImageShareBinding.inflate(inflater, container, false)
        val url = arguments?.getString("url")

        toolbar = binding.toolbar
        toolbar.inflateMenu(R.menu.share_menu)

        Glide.with(this)
            .asBitmap()
            .load(url)
            .into(object : CustomTarget<Bitmap>() {
                @RequiresApi(Build.VERSION_CODES.Q)
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    val path: String =
                        Images.Media.insertImage(
                            requireActivity().contentResolver,
                            resource,
                            Uri.parse(url).path,
                            null
                        )

                    val screenshotUri = Uri.parse(path)

                    fullImage = binding.fullImage

                    fullImage.setImageBitmap(resource)


                    toolbar.setOnMenuItemClickListener {
                        when (it.itemId) {
                            R.id.shareBtn -> {
                                val shareIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_STREAM, screenshotUri)
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Acabei de percorrer um percurso no TrailBlaze!"
                                    )
                                    type = "image/*"
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }
                                startActivity(Intent.createChooser(shareIntent, "Partilhar com"))

                                true
                            }
                            else -> false
                        }
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }

            })

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        if (activity != null && this.activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.GONE
        }

        binding.backBtn.setOnClickListener {
            navController.popBackStack()
        }
    }
}