package dam.a42363.trailblaze.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import dam.a42363.trailblaze.R
import dam.a42363.trailblaze.databinding.ItemFotoBinding
import dam.a42363.trailblaze.databinding.ItemFotoCollectionBinding

class ImageAdapter(
    private val urls: List<String>,
    private val paths: List<String>,
    private val names: List<String>,
    val navController: NavController,
    private val share: Boolean
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    private lateinit var path: String
    private lateinit var name: String

    inner class ImageViewHolder(val fotoBinding: ItemFotoCollectionBinding) :
        RecyclerView.ViewHolder(fotoBinding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {

        return ImageViewHolder(
            ItemFotoCollectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    }

    override fun getItemCount(): Int {

        return urls.size

    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {

        val url = urls[position]
        if (paths.isNotEmpty() && names.isNotEmpty()) {
            path = paths[position]
            name = names[position]
        }
        Glide.with(holder.itemView).load(url).into(holder.fotoBinding.image)
        if (share) {
            holder.fotoBinding.image.setOnClickListener {
                val bundle = bundleOf("url" to url)
                navController.navigate(R.id.action_terminarFragment_to_imageShareFragment, bundle)
            }
        } else if (!share && paths.isNotEmpty()) {
            holder.fotoBinding.routeName.text = name
            holder.fotoBinding.image.setOnClickListener {
                val bundle = bundleOf("path" to path)
                navController.navigate(R.id.action_fotosFragment_to_routeFotosFragment, bundle)
            }
        }
    }

}