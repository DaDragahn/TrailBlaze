package dam.a42363.trailblaze

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dam.a42363.trailblaze.databinding.FragmentAddAmigosBinding
import dam.a42363.trailblaze.databinding.ItemAmigoBinding
import dam.a42363.trailblaze.databinding.ItemRouteBinding
import dam.a42363.trailblaze.models.AddFriends

class AddAmigosFragment : Fragment() {

    private var _binding: FragmentAddAmigosBinding? = null
    private val binding get() = _binding!!
    private lateinit var searchView: SearchView
    private lateinit var searchViewList: RecyclerView
    private lateinit var db: FirebaseFirestore
    private var adapter: FindFriendsFirestoreRecyclerAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddAmigosBinding.inflate(inflater, container, false)

        searchView = binding.searchView

        searchViewList = binding.amigosListView
        db = FirebaseFirestore.getInstance()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchPeople(newText!!)
                return true
            }

        })

        return binding.root
    }

    fun searchPeople(text: String) {
        val query = db.collection("users").orderBy("nome", Query.Direction.ASCENDING).startAt(text)
            .endAt(text + "\uf8ff")

        val options = FirestoreRecyclerOptions.Builder<AddFriends>()
            .setQuery(query, AddFriends::class.java)
            .build()

        val adapter = FindFriendsFirestoreRecyclerAdapter(options, requireContext())
        adapter.startListening()
        searchViewList.adapter = adapter
    }


    override fun onStop() {
        super.onStop()

        if (adapter != null) {
            adapter!!.stopListening()
        }
    }


    private inner class FindFriendsViewHolder(private val amigoBinding: ItemAmigoBinding) :
        RecyclerView.ViewHolder(amigoBinding.root) {
        fun setVariables(nome: String, photoUrl: String, ctx: Context) {
            amigoBinding.name.text = nome
            Glide.with(ctx).load(photoUrl)
                .into(amigoBinding.profileImage)
        }
    }

    private inner class FindFriendsFirestoreRecyclerAdapter constructor(
        options: FirestoreRecyclerOptions<AddFriends>,
        private val ctx: Context
    ) :
        FirestoreRecyclerAdapter<AddFriends, FindFriendsViewHolder>(options) {
        override fun onBindViewHolder(
            findFriendsViewHolder: FindFriendsViewHolder,
            position: Int,
            addFriends: AddFriends
        ) {
            findFriendsViewHolder.setVariables(addFriends.nome!!, addFriends.photoUrl!!, ctx)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FindFriendsViewHolder {
            return FindFriendsViewHolder(
                ItemAmigoBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent, false
                )
            )
        }
    }
}
