package projeto.g54.trailblaze

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import projeto.g54.trailblaze.databinding.FragmentSignInBinding
import java.util.*

class SignInFragment : Fragment(){

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    private lateinit var navController: NavController
    private lateinit var signInButton: SignInButton
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var loginBtn: MaterialButton
    private lateinit var registerBtn: MaterialButton

    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSignInBinding.inflate(inflater, container, false)

        loginBtn = binding.login
        registerBtn = binding.register
        signInButton = binding.googleSignIn

        return binding.root
    }

    @SuppressLint("UseRequireInsteadOfGet")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        if (activity != null && this.activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.GONE
        }

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = this.activity?.let { GoogleSignIn.getClient(it, gso) }!!

        signInButton.setOnClickListener {
            signIn()
        }

        loginBtn.setOnClickListener {
            navController.navigate(R.id.action_signInFragment_to_loginFragment)
        }
        registerBtn.setOnClickListener {
            navController.navigate(R.id.action_signInFragment_to_registarFragment)
        }

    }

    private fun updateUI(user: FirebaseUser?) {
        val db = FirebaseFirestore.getInstance()
        val updates: MutableMap<String, Any> =
            HashMap()
        updates["nome"] = user?.displayName.toString()
        updates["email"] = user?.email.toString()
        updates["photoUrl"] = user?.photoUrl.toString()
        db.collection("users").document(user!!.uid).set(updates).addOnSuccessListener {
            navController.navigate(R.id.action_signInFragment_to_perfilFragment)
        }
    }

    private fun signIn() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    @SuppressLint("LogNotTimber")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val exception = task.exception
            if (task.isSuccessful) {
                try {
                    // Google Sign In was successful, authenticate with Firebase
                    val account = task.getResult(ApiException::class.java)!!
                    Log.d("GoogleActivity", "firebaseAuthWithGoogle:" + account.id)
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    // Google Sign In failed, update UI appropriately
                    Log.w("GoogleActivity", "Google sign in failed", e)
                }
            } else {
                Log.w("GoogleActivity", exception.toString())
            }
        }
    }

    @SuppressLint("LogNotTimber")
    private fun firebaseAuthWithGoogle(idToken: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        this.activity?.let {
            auth.signInWithCredential(credential)
                .addOnCompleteListener(it) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("GoogleActivity", "signInWithCredential:success")
                        val user = auth.currentUser
                        updateUI(user)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("GoogleActivity", "signInWithCredential:failure", task.exception)
                        updateUI(null)
                    }
                }
        }
    }
}