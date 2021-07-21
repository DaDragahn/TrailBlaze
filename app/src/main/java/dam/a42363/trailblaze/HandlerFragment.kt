package dam.a42363.trailblaze

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.firebase.auth.FirebaseAuth
import dam.a42363.trailblaze.utils.SampleMethods

class HandlerFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var navController: NavController

    private lateinit var progressBar: ProgressBar
    private lateinit var requestPermissionLauncher : ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    val user = auth.currentUser
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (user != null && auth.uid != null) {
                            val currentUser = SampleMethods.getUser(auth, user)
                            val bundle = bundleOf("User" to currentUser)
                            navController.navigate(
                                R.id.action_handlerFragment_to_explorarFragment,
                                bundle
                            )
                            progressBar.visibility = View.INVISIBLE

                        } else {
                            navController.navigate(R.id.action_handlerFragment_to_explorarFragment)
                            progressBar.visibility = View.INVISIBLE
                        }
                    }, 3000)
                } else {
                    Toast.makeText(
                        requireActivity().applicationContext,
                        "Permission Denied",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_handler, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        progressBar = view.findViewById(R.id.circular_progress_bar)

        auth = FirebaseAuth.getInstance()

        if (checkLocationPermission()) {
            val user = auth.currentUser
            Handler(Looper.getMainLooper()).postDelayed({
                if (user != null && auth.uid != null) {
                    val currentUser = SampleMethods.getUser(auth, user)
                    val bundle = bundleOf("User" to currentUser)
                    navController.navigate(R.id.explorarFragment, bundle)
                    progressBar.visibility = View.INVISIBLE

                } else {
                    navController.navigate(R.id.action_handlerFragment_to_explorarFragment)
                    progressBar.visibility = View.INVISIBLE
                }
            }, 3000)
        }
    }

    private fun checkLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                requireActivity().applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return false
        }
        return true
    }
}