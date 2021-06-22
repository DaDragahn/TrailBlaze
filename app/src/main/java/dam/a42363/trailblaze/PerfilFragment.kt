package dam.a42363.trailblaze

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class PerfilFragment : Fragment() {

    private lateinit var navController: NavController
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_perfil, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user == null && auth.uid == null) {
            navController.navigate(R.id.action_perfilFragment_to_signInFragment)
        } else {

            if (activity != null && this.activity is MainActivity) {
                (activity as MainActivity).bottomNavigationView?.visibility = View.VISIBLE
            }

            val logout = view.findViewById<Button>(R.id.logout)
            val email = view.findViewById<TextView>(R.id.user)
            val name = view.findViewById<TextView>(R.id.firstName)
            val profileImage = view.findViewById<ImageView>(R.id.profileImage)


            if (user != null) {
                email.text = user.email!!.toString()
            }

            name.text = user?.displayName

            if (user?.displayName != null) {
                Glide.with(this).load(user.photoUrl).into(profileImage)
            }

            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(user!!.uid).get()
                .addOnCompleteListener { task: Task<DocumentSnapshot?> ->
                    if (task.isSuccessful) {
                        val documentSnapshot = task.result
                        if (documentSnapshot!!.exists()) {
                            name.text = documentSnapshot.getString("name")
                        } else {
                            Log.d("MainActivity", "No such document")
                        }
                    } else {
                        Log.d("MainActivity", "get failed with ", task.exception)
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