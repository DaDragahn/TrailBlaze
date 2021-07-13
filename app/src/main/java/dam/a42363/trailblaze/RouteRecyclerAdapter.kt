package dam.a42363.trailblaze

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dam.a42363.trailblaze.databinding.ItemRouteBinding

class RouteRecyclerAdapter(
    private val items: ArrayList<RouteInfo>,
    private val mOnListListener: OnListListener
) :
    RecyclerView.Adapter<RouteRecyclerAdapter.ViewHolder>() {

    interface OnListListener {
        fun onNoteClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemRouteBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false
            ), mOnListListener
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val route = items[position]
        holder.name.text = route.name
        holder.author.text = route.author
        holder.localidade.text = route.localidade
        holder.cardView.setOnClickListener {
            mOnListListener.onNoteClick(holder.adapterPosition)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class ViewHolder(listItemBinding: ItemRouteBinding, private var onListListener: OnListListener) :
        RecyclerView.ViewHolder(listItemBinding.root), View.OnClickListener {
        var name = listItemBinding.name
        var localidade = listItemBinding.localidade
        var author = listItemBinding.author
        var cardView = listItemBinding.cardView
//
//        init {
//            cardView.setOnClickListener(this)
//        }

        override fun onClick(p0: View?) {
//            onListListener.onNoteClick(bindingAdapterPosition)
        }

    }
}