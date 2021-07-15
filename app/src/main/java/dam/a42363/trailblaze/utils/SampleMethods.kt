package dam.a42363.trailblaze.utils

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import dam.a42363.trailblaze.models.User

object SampleMethods {



    fun getUser(auth: FirebaseAuth, user: FirebaseUser): User {

        lateinit var name: String

        val email: String = user.email!!.toString()

        name = user.displayName!!

        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(user.uid).get()
            .addOnCompleteListener { task: Task<DocumentSnapshot?> ->
                if (task.isSuccessful) {
                    val documentSnapshot = task.result
                    if (documentSnapshot!!.exists()) {
                        name = documentSnapshot.getString("name").toString()
                    } else {
                        Log.d("MainActivity", "No such document")
                    }
                } else {
                    Log.d("MainActivity", "get failed with ", task.exception)
                }
            }

        return User(name, email)
    }
}