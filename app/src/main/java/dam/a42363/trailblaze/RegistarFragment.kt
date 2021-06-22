package dam.a42363.trailblaze

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

class RegistarFragment : Fragment() {

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var auth: FirebaseAuth

    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        if (activity != null && this.activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.GONE
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_registar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        email = view.findViewById(R.id.email)
        password = view.findViewById(R.id.password)
        val register = view.findViewById<Button>(R.id.register)
        auth = FirebaseAuth.getInstance()
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
                navController.navigate(R.id.action_registarFragment_to_criarPerfilFragment)
            } else {
                Toast.makeText(this.activity, "Registration Failed", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

}