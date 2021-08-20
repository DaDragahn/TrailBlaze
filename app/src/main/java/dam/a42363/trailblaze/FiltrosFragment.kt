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
        setClicked()
        binding.cancel.setOnClickListener {
            navController.popBackStack()
        }

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
            modalidadeArray.clear()
            dificuldadeArray.clear()
            setUnchecked()
        }
    }

    private fun setClicked() {
        if ((activity as MainActivity).dificuldadeArray.isNotEmpty()) {
            if ((activity as MainActivity).dificuldadeArray.contains("Fácil")) {
                binding.facilChp.isChecked = true
                dificuldadeArray.add("Fácil")
            }
            if ((activity as MainActivity).dificuldadeArray.contains("Moderado")) {
                binding.moderadoChp.isChecked = true
                dificuldadeArray.add("Moderado")
            }
            if ((activity as MainActivity).dificuldadeArray.contains("Dificil")) {
                binding.dificilChp.isChecked = true
                dificuldadeArray.add("Dificil")
            }
        }

        if ((activity as MainActivity).modalidadeArray.isNotEmpty()) {
            if ((activity as MainActivity).modalidadeArray.contains("Caminhada")) {
                binding.caminhadaChp.isChecked = true
                modalidadeArray.add("Caminhada")
            }
            if ((activity as MainActivity).modalidadeArray.contains("Corrida")) {
                binding.corridaChp.isChecked = true
                modalidadeArray.add("Corrida")
            }
            if ((activity as MainActivity).modalidadeArray.contains("Ciclismo")) {
                binding.ciclismoChp.isChecked = true
                modalidadeArray.add("Ciclismo")
            }
        }
    }

    private fun setUnchecked() {
        binding.facilChp.isChecked = false
        binding.moderadoChp.isChecked = false
        binding.dificilChp.isChecked = false
        binding.caminhadaChp.isChecked = false
        binding.corridaChp.isChecked = false
        binding.ciclismoChp.isChecked = false
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