package dam.a42363.trailblaze

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import dam.a42363.trailblaze.databinding.FragmentDefinicoesBinding

class DefinicoesFragment : Fragment() {

    private var _binding: FragmentDefinicoesBinding? = null
    private val binding get() = _binding!!

    private lateinit var navController: NavController


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

        val res: Resources = resources
        val conf: Configuration = res.configuration

        when (conf.locale.language) {
            "en" -> {
                binding.linguagemTextView.setText(getString(R.string.ingles), false)
            }
            "pt" -> {
                binding.linguagemTextView.setText(getString(R.string.portugues), false)
            }
        }

        binding.linguagemTextView.setOnItemClickListener { _, _, _, id ->
            when (id) {
                0L -> {
                    setLocale("pt")
                }
                1L -> {
                    setLocale("en")
                }
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        if (activity != null && this.activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.VISIBLE
        }

        binding.backBtn.setOnClickListener {
            navController.popBackStack()
        }
    }

    private fun setLocale(lang: String) {

        val editor: SharedPreferences.Editor =
            requireActivity().getSharedPreferences("Settings", MODE_PRIVATE).edit()
        editor.putString("My_Lang", lang)
        editor.apply()

        val refresh = Intent(requireContext(), MainActivity::class.java)
        requireActivity().finish()
        startActivity(refresh)
    }
}