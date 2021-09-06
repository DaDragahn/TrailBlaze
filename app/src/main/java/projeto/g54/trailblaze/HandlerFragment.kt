package projeto.g54.trailblaze

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.firebase.auth.FirebaseAuth
import projeto.g54.trailblaze.databinding.FragmentHandlerBinding
import projeto.g54.trailblaze.utils.SampleMethods
import java.util.*


class HandlerFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var navController: NavController

    private lateinit var progressBar: ProgressBar
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    private var _binding: FragmentHandlerBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val editor: SharedPreferences =
            requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val lang: String? = editor.getString("My_Lang", "")
        val config: Configuration = requireActivity().baseContext.resources.configuration
        if ("" != lang && !config.locale.language.equals(lang)) {
            val locale = Locale(lang!!)
            Locale.setDefault(locale)
            config.locale = locale
            requireActivity().baseContext.resources.updateConfiguration(
                config,
                requireActivity().baseContext.resources.displayMetrics
            )
        }
        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    val user = auth.currentUser
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (user != null && auth.uid != null) {
                            val currentUser = SampleMethods.getUser(user)
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
                    activity?.finish()
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHandlerBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        progressBar = binding.circularProgressBar
        auth = FirebaseAuth.getInstance()

        if (checkLocationPermission()) {
            val user = auth.currentUser
            Handler(Looper.getMainLooper()).postDelayed({
                if (user != null && auth.uid != null) {
                    val currentUser = SampleMethods.getUser(user)
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