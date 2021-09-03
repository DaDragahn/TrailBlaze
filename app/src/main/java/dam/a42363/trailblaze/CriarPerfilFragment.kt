package dam.a42363.trailblaze

import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dam.a42363.trailblaze.databinding.FragmentCriarPerfilBinding
import timber.log.Timber

class CriarPerfilFragment : Fragment() {

    private lateinit var firstName: EditText
    private lateinit var lastName: EditText
    private lateinit var save: Button
    private lateinit var email: String

    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var navController: NavController

    private var _binding: FragmentCriarPerfilBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCriarPerfilBinding.inflate(inflater, container, false)

        firstName = binding.firstName
        lastName = binding.lastName

        save = binding.save

        email = arguments?.getString("email").toString()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        if (activity != null && this.activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.GONE
        }

        save.setOnClickListener {
            val txtFirstName = firstName.text.toString()
            val txtLastName = lastName.text.toString()

            if (TextUtils.isEmpty(txtFirstName) || TextUtils.isEmpty(txtLastName)) {
                Toast.makeText(this.activity, "Empty credentials!!", Toast.LENGTH_SHORT)
                    .show()
            } else {
                val auth = FirebaseAuth.getInstance()
                val user = auth.currentUser

                db = FirebaseFirestore.getInstance()
                storage = FirebaseStorage.getInstance()

                val storageRef = storage.reference

                storageRef.child("images/defaultPic.jpg").downloadUrl.addOnSuccessListener {
                    val updates: MutableMap<String, Any> = HashMap()
                    updates["nome"] = "$txtFirstName $txtLastName"
                    updates["email"] = email
                    updates["photoUrl"] = it.toString()

                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName("$txtFirstName $txtLastName")
                        .setPhotoUri(Uri.parse(it.toString()))
                        .build()

                    user?.updateProfile(profileUpdates)

                    db.collection("users").document(user!!.uid).set(updates)
                    navController.navigate(R.id.action_criarPerfilFragment_to_perfilFragment)

                }.addOnFailureListener {
                    Timber.tag("URL").d(it, "Fail")
                }
            }
        }
    }
}