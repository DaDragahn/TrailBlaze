package dam.a42363.trailblaze

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.Navigation
import dam.a42363.trailblaze.databinding.FragmentEditarPerfilBinding
import dam.a42363.trailblaze.databinding.FragmentTerminarBinding

class TerminarFragment : Fragment() {

    private lateinit var navController: NavController

    var _binding: FragmentTerminarBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentTerminarBinding.inflate(inflater, container, false)

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        val ratingBar = binding.ratingBar
        val finalizarBtn = binding.finalizarBtn

        finalizarBtn.setOnClickListener {
            val msg = ratingBar.rating.toString()
            Toast.makeText(requireContext(), "Rating is:$msg", Toast.LENGTH_SHORT).show()
        }

        if (activity != null && this.activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.GONE
        }


        val message = binding.messageTxt

        ratingBar.setOnClickListener {

            if (ratingBar.rating <= 1.0) {
                message.text = "Bummer"
            }

            if (ratingBar.rating <= 2.0 && ratingBar.rating > 1.0) {
                message.text = "Could be better"
            }

            if (ratingBar.rating <= 3.0 && ratingBar.rating > 2.0) {
                message.text = "Not Bad"
            }

            if (ratingBar.rating <= 4.0 && ratingBar.rating > 3.0) {
                message.text = "Nice Experience"
            }

            if (ratingBar.rating <= 5.0 && ratingBar.rating > 4.0) {
                message.text = "Loved it!!"
            }

        }


    }

}