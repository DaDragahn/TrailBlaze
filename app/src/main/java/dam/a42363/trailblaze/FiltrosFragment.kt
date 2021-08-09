package dam.a42363.trailblaze

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import dam.a42363.trailblaze.databinding.FragmentFiltrosBinding
import dam.a42363.trailblaze.databinding.FragmentTerminarBinding


class FiltrosFragment : Fragment() {

    private lateinit var navController: NavController

    var _binding: FragmentFiltrosBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        _binding = FragmentFiltrosBinding.inflate(inflater, container, false)



        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity != null && this.activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.GONE
        }

        val facilChp = binding.facilChp

        if (facilChp.isChecked) {

            facilChp.setChipStrokeColorResource(R.color.trailGreen)
            facilChp.setTextColor(Color.parseColor("#3F9E00"))
        }


    }
}