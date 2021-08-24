package dam.a42363.trailblaze

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.navigation.NavController
import dam.a42363.trailblaze.databinding.FragmentDefinicoesBinding
import dam.a42363.trailblaze.databinding.FragmentPartilharBinding

class DefinicoesFragment : Fragment() {

    private var _binding: FragmentDefinicoesBinding? = null
    private val binding get() = _binding!!

    private lateinit var navController: NavController


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        _binding = FragmentDefinicoesBinding.inflate(inflater, container, false)

        val unidades = resources.getStringArray(R.array.unidades)
        val linguagem = resources.getStringArray(R.array.linguagem)

        val unidadesArrayAdapter =
            ArrayAdapter(requireContext(), R.layout.item_dropdown, unidades)
        val linguagemArrayAdapter =
            ArrayAdapter(requireContext(), R.layout.item_dropdown, linguagem)
        binding.unidadesTextView.setAdapter(unidadesArrayAdapter)
        binding.linguagemTextView.setAdapter(linguagemArrayAdapter)
        return binding.root
    }

}