package dam.a42363.trailblaze.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import dam.a42363.trailblaze.MainActivity
import dam.a42363.trailblaze.databinding.ItemFotoBinding


class ListImageAdapter(
    private val urls: List<String>,
    val navController: NavController,
    val mainActivity: MainActivity
) : RecyclerView.Adapter<ListImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val fotoBinding: ItemFotoBinding) :
        RecyclerView.ViewHolder(fotoBinding.root)

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
        Glide.with(holder.itemView).load(url).into(holder.fotoBinding.image)
        holder.fotoBinding.image.setOnClickListener {
            mainActivity.imageUrl = url
            navController.popBackStack()
        }
    }

}