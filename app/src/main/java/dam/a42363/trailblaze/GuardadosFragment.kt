package dam.a42363.trailblaze

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.firebase.auth.FirebaseAuth

class GuardadosFragment : Fragment() {

    private lateinit var navController: NavController
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_guardados, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user == null && auth.uid == null) {
            navController.navigate(R.id.action_guardadosFragment_to_signInFragment)
        } else {
            if (activity != null && this.activity is MainActivity) {
                (activity as MainActivity).bottomNavigationView?.visibility = View.VISIBLE
            }
        }
    }

}