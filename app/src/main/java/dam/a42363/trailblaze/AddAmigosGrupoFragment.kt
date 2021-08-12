package dam.a42363.trailblaze

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.recyclerview.widget.RecyclerView
import dam.a42363.trailblaze.databinding.FragmentAddAmigosBinding
import dam.a42363.trailblaze.databinding.FragmentAddAmigosGrupoBinding


class AddAmigosGrupoFragment : Fragment() {

    private var _binding: FragmentAddAmigosGrupoBinding? = null
    private val binding get() = _binding!!

    private lateinit var searchView: SearchView
    private lateinit var searchViewList: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        _binding = FragmentAddAmigosGrupoBinding.inflate(inflater, container, false)

        searchView = binding.searchView

        searchViewList = binding.amigosListView

        return binding.root
    }

}