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

class ImageAdapter(
    val urls: List<String>,  val paths: List<String>, val navController: NavController, val share: Boolean
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val fotoBinding: ItemFotoBinding) :
        RecyclerView.ViewHolder(fotoBinding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {

        return ImageViewHolder(
            ItemFotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    }

    override fun getItemCount(): Int {

        return urls.size

    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {

        val url = urls[position]
        val path = paths[position]
        Glide.with(holder.itemView).load(url).into(holder.fotoBinding.image)
        if (share) {
            holder.fotoBinding.image.setOnClickListener {
                val bundle = bundleOf("url" to url)
                navController.navigate(R.id.action_terminarFragment_to_imageShareFragment, bundle)
            }
        }
        else{
            holder.fotoBinding.image.setOnClickListener {
                Log.d("RecordRoute",path)
            }
        }
    }

}