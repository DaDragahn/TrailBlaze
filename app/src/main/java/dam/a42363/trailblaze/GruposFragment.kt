package dam.a42363.trailblaze

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import dam.a42363.trailblaze.databinding.FragmentGruposBinding
import dam.a42363.trailblaze.databinding.FragmentPerfilBinding

class GruposFragment : Fragment() {

    private lateinit var navController: NavController
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar

    var _binding: FragmentGruposBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        setHasOptionsMenu(true)


        _binding = FragmentGruposBinding.inflate(inflater, container, false)

        toolbar = binding.toolbar

        toolbar.inflateMenu(R.menu.grupo_menu)

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.grupo_menu, menu)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.adicionarGrupo -> {
                    navController.navigate(R.id.action_gruposFragment_to_addGruposFragment)
                    true
                }

                else -> false
            }
        }

    }

}