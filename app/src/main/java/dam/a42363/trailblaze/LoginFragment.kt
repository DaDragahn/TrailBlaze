package dam.a42363.trailblaze

import android.os.Bundle
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

class LoginFragment : Fragment() {

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var login: Button
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
        return inflater.inflate(R.layout.fragment_login, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        email = view.findViewById(R.id.email)
        password = view.findViewById(R.id.password)

        login = view.findViewById(R.id.login)

        auth = FirebaseAuth.getInstance()

        login.setOnClickListener {
            val txtEmail = email.text.toString()
            val txtPassword = password.text.toString()
            loginUser(txtEmail, txtPassword)
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnSuccessListener {
            Toast.makeText(this.activity, "Login Successful", Toast.LENGTH_SHORT).show()
            navController.navigate(R.id.action_loginFragment_to_perfilFragment)
        }
    }

}