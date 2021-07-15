package dam.a42363.trailblaze

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.firebase.geofire.GeoLocation
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.geojson.Point
import dam.a42363.trailblaze.databinding.FragmentAmigosBinding
import dam.a42363.trailblaze.databinding.FragmentPerfilBinding


class AmigosFragment : Fragment() {
    private lateinit var navController: NavController
    var _binding: FragmentAmigosBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAmigosBinding.inflate(inflater, container, false)

        binding.addAmigos.setOnClickListener{
            navController.navigate(R.id.action_amigosFragment_to_addAmigosFragment)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        if (activity != null && this.activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.VISIBLE
        }
    }

}