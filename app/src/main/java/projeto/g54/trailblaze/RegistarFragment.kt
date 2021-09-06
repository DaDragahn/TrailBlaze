package projeto.g54.trailblaze

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import projeto.g54.trailblaze.databinding.FragmentRegistarBinding

class RegistarFragment : Fragment() {

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var register: MaterialButton
    private lateinit var auth: FirebaseAuth

    private lateinit var navController: NavController

    private var _binding: FragmentRegistarBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistarBinding.inflate(inflater, container, false)

        email = binding.email
        password = binding.password
        register = binding.register
        auth = FirebaseAuth.getInstance()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        if (activity != null && this.activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.GONE
        }

        register.setOnClickListener {
            val txtEmail = email.text.toString()
            val txtPassword = password.text.toString()
            if (TextUtils.isEmpty(txtEmail) || TextUtils.isEmpty(txtPassword)) {
                Toast.makeText(this.activity, "Empty credentials!!", Toast.LENGTH_SHORT)
                    .show()
            } else if (txtPassword.length < 6) {
                Toast.makeText(this.activity, "Password too short!!", Toast.LENGTH_SHORT)
                    .show()
            } else {
                registerUser(txtEmail, txtPassword)
            }
        }
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this.activity, "Register Successful", Toast.LENGTH_SHORT)
                    .show()
                val bundle = bundleOf(
                    "email" to email
                )
                navController.navigate(R.id.action_registarFragment_to_criarPerfilFragment, bundle)
            } else {
                Toast.makeText(this.activity, "Registration Failed", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}