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
import com.google.firebase.firestore.FirebaseFirestore

class CriarPerfilFragment : Fragment() {

    private lateinit var firstName: EditText
    private lateinit var lastName: EditText
    private lateinit var save: Button

    private lateinit var db: FirebaseFirestore

    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        if (activity != null && this.activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.GONE
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_criar_perfil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        firstName = view.findViewById(R.id.firstName)
        lastName = view.findViewById(R.id.lastName)

        save = view.findViewById<Button>(R.id.save)

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

                val updates: MutableMap<String, Any> = HashMap()
                updates["name"] = "$txtFirstName $txtLastName"

                db.collection("users").document(user!!.uid).set(updates)
                navController.navigate(R.id.action_signInFragment_to_perfilFragment)
            }
        }
    }

}