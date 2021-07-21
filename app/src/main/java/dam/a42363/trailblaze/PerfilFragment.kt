package dam.a42363.trailblaze

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import dam.a42363.trailblaze.databinding.FragmentExplorarBinding
import dam.a42363.trailblaze.databinding.FragmentPerfilBinding
import io.alterac.blurkit.BlurLayout

class PerfilFragment : Fragment() {

    private lateinit var navController: NavController
    private lateinit var auth: FirebaseAuth

    var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!
    private var doubleBackToExitPressedOnce = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val backCallback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (doubleBackToExitPressedOnce) {
                activity?.finishAndRemoveTask()          // Handle the back button event
            }

            doubleBackToExitPressedOnce = true
            Toast.makeText(requireContext(), "Please click BACK again to exit", Toast.LENGTH_SHORT)
                .show()

            Handler(Looper.getMainLooper()).postDelayed(
                Runnable { doubleBackToExitPressedOnce = false },
                2000
            )
        }

        backCallback.isEnabled
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentPerfilBinding.inflate(inflater, container, false)

        binding.amigosBtn.setOnClickListener{
            navController.navigate(R.id.action_perfilFragment_to_amigosFragment)
        }

        binding.trilhosBtn.setOnClickListener{
            navController.navigate(R.id.action_perfilFragment_to_trrailsFragment)
        }
        // Inflate the layout for this fragment
        return binding.root
//        inflater.inflate(R.layout.fragment_perfil, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        blurLayout.startBlur()

        navController = Navigation.findNavController(view)

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user == null && auth.uid == null) {
            navController.navigate(R.id.action_perfilFragment_to_signInFragment)
        } else {

            if (activity != null && this.activity is MainActivity) {
                (activity as MainActivity).bottomNavigationView?.visibility = View.VISIBLE
            }

            val logout = binding.logout
            val email = binding.user
            val name = binding.firstName
            val profileImage = binding.profileImage


            if (user != null) {
                val db = FirebaseFirestore.getInstance()
                db.collection("users").document(user.uid).get()
                    .addOnCompleteListener { task: Task<DocumentSnapshot?> ->
                        if (task.isSuccessful) {
                            val documentSnapshot = task.result
                            if (documentSnapshot!!.exists()) {
                                name.text = documentSnapshot.getString("nome")
                                email.text = documentSnapshot.getString("email")
                                if (!documentSnapshot.getString("photoUrl").equals(""))
                                    Glide.with(this).load(documentSnapshot.getString("photoUrl"))
                                        .into(profileImage)
                            } else {
                                Log.d("MainActivity", "No such document")
                            }
                        } else {
                            Log.d("MainActivity", "get failed with ", task.exception)
                        }
                    }
            }
            logout.setOnClickListener {
                auth.signOut()
                Toast.makeText(this.activity, "Logged Out", Toast.LENGTH_SHORT).show()
                navController.navigate(R.id.action_perfilFragment_to_signInFragment)

            }
        }
    }
}