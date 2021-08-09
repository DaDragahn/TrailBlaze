package dam.a42363.trailblaze

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.Navigation
import dam.a42363.trailblaze.databinding.FragmentFiltrosBinding


class FiltrosFragment : Fragment() {

    private lateinit var navController: NavController

    var _binding: FragmentFiltrosBinding? = null
    private val binding get() = _binding!!
    private var dificuldadeArray = ArrayList<String>()
    private val modalidadeArray = ArrayList<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFiltrosBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        if (activity != null && this.activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.GONE
        }

        binding.facilChp.setOnCheckedChangeListener { _, isChecked ->
            changeDificuldadeArray(isChecked, "Fácil")
        }

        binding.moderadoChp.setOnCheckedChangeListener { _, isChecked ->
            changeDificuldadeArray(isChecked, "Moderado")
        }

        binding.dificilChp.setOnCheckedChangeListener { _, isChecked ->
            changeDificuldadeArray(isChecked, "Dificil")
        }

        binding.caminhadaChp.setOnCheckedChangeListener { _, isChecked ->
            changeModalidadeArray(isChecked, "Caminhada")
        }

        binding.corridaChp.setOnCheckedChangeListener { _, isChecked ->
            changeModalidadeArray(isChecked, "Corrida")
        }

        binding.ciclismoChp.setOnCheckedChangeListener { _, isChecked ->
            changeModalidadeArray(isChecked, "Ciclismo")
        }

        binding.aplicarBtn.setOnClickListener {
            (activity as MainActivity).modalidadeArray = modalidadeArray
            (activity as MainActivity).dificuldadeArray = dificuldadeArray
            navController.navigate(R.id.action_filtrosFragment_to_explorarFragment)
        }

        binding.reset.setOnClickListener {
            (activity as MainActivity).modalidadeArray.clear()
            (activity as MainActivity).dificuldadeArray.clear()
            navController.navigate(
                R.id.action_filtrosFragment_to_explorarFragment
            )
        }
    }

    private fun changeModalidadeArray(checked: Boolean, modalidade: String) {
        if (checked) {
            modalidadeArray.add(modalidade)
        } else {
            modalidadeArray.remove(modalidade)
        }
    }

    private fun changeDificuldadeArray(checked: Boolean, dificuldade: String) {
        if (checked) {
            dificuldadeArray.add(dificuldade)
        } else {
            dificuldadeArray.remove(dificuldade)
        }
    }
}