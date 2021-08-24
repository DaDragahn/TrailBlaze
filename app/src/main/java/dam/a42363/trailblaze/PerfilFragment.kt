package dam.a42363.trailblaze

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import dam.a42363.trailblaze.databinding.FragmentPerfilBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PerfilFragment : Fragment() {

    private var user: FirebaseUser? = null
    private lateinit var db: FirebaseFirestore
    private lateinit var navController: NavController
    private lateinit var auth: FirebaseAuth

    private lateinit var toolbar: androidx.appcompat.widget.Toolbar

    var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private var doubleBackToExitPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.perfil_menu, menu)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)

        binding.amigosBtn.setOnClickListener {
            navController.navigate(R.id.action_perfilFragment_to_amigosFragment)
        }

        binding.trilhosBtn.setOnClickListener {
            navController.navigate(R.id.action_perfilFragment_to_trailsFragment)
        }

        binding.gruposBtn.setOnClickListener {
            navController.navigate(R.id.action_perfilFragment_to_gruposFragment)
        }

        binding.fotosBtn.setOnClickListener {
            navController.navigate(R.id.action_perfilFragment_to_fotosFragment)
        }

        toolbar = binding.toolbar

        toolbar.inflateMenu(R.menu.perfil_menu)

        auth = FirebaseAuth.getInstance()
        user = auth.currentUser
        db = FirebaseFirestore.getInstance()

        return binding.root
    }

    private fun getVariables() = CoroutineScope(Dispatchers.IO).launch {
        try {
            val groups =
                db.collection("Groups").whereArrayContains("groupArray", user!!.uid).get().await()
            val friends =
                db.collection("Friends").document("FriendDocument").collection(user!!.uid).get()
                    .await()
            val trails = db.collection("locations").whereEqualTo("uid", user!!.uid).get().await()
            withContext(Dispatchers.Main) {
                binding.numAmigos.text = friends.documents.size.toString()
                binding.numGrupos.text = groups.documents.size.toString()
                binding.numTrilhos.text = trails.documents.size.toString()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.editarPerfil -> {
                    navController.navigate(R.id.action_perfilFragment_to_editarPerfilFragment)
                    true
                }
                R.id.defenicoes -> {
                    navController.navigate(R.id.action_perfilFragment_to_definicoesFragment)
                    true
                }
                R.id.logout -> {
                    logOut()
                    true
                }
                else -> false
            }
        }

        if (user == null && auth.uid == null) {
            navController.navigate(R.id.action_perfilFragment_to_signInFragment)
        } else {

            if (activity != null && this.activity is MainActivity) {
                (activity as MainActivity).bottomNavigationView?.visibility = View.VISIBLE
            }

            val name = binding.firstName
            val profileImage = binding.profileImage

            if (user != null) {
                val db = FirebaseFirestore.getInstance()
                db.collection("users").document(user!!.uid).get()
                    .addOnCompleteListener { task: Task<DocumentSnapshot?> ->
                        if (task.isSuccessful) {
                            val documentSnapshot = task.result
                            if (documentSnapshot!!.exists()) {
                                name.text = documentSnapshot.getString("nome")
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
        }
        getVariables()
    }


    private fun logOut() {
        auth.signOut()
        Toast.makeText(this.activity, "Logged Out", Toast.LENGTH_SHORT).show()
        navController.navigate(R.id.action_perfilFragment_to_signInFragment)
    }


}