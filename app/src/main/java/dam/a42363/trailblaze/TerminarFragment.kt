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

        ratingBar.setOnRatingBarChangeListener { ratingBar, rating, fromUser ->

            if (rating == 0f) {
                message.text = ""
            }

            if (rating > 0.0 && rating <= 1.0) {
                message.text = "Bummer"
            }

            if (rating <= 2.0 && rating > 1.0) {
                message.text = "Could be better"
            }

            if (rating <= 3.0 && rating > 2.0) {
                message.text = "Not Bad"
            }

            if (rating <= 4.0 && rating > 3.0) {
                message.text = "Nice Experience"
            }

            if (rating <= 5.0 && rating > 4.0) {
                message.text = "Loved it!!"
            }

        }

    }

}