package dam.a42363.trailblaze

import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dam.a42363.trailblaze.databinding.FragmentEditarPerfilBinding
import dam.a42363.trailblaze.databinding.FragmentExplorarBinding

class EditarPerfilFragment : Fragment() {

    var _binding: FragmentEditarPerfilBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var navController: NavController


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        _binding = FragmentEditarPerfilBinding.inflate(inflater, container, false)

        db = FirebaseFirestore.getInstance()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        val storageRef = storage.reference

//        val email = binding.email
        val name = binding.nome
        val lastName = binding.apelido
        val profileImage = binding.profileImage
        val save = binding.save


        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnCompleteListener { task: Task<DocumentSnapshot?> ->
                    if (task.isSuccessful) {
                        val documentSnapshot = task.result
                        if (documentSnapshot!!.exists()) {

                            val fullName = documentSnapshot.getString("nome")?.split(" ")

                            name.setText(fullName?.get(0))
                            lastName.setText(fullName?.get(1))
//                            email.setText(documentSnapshot.getString("email"))

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


        save.setOnClickListener {
            val txtFirstName = name.text.toString()
            val txtLastName = lastName.text.toString()
//            val email = email.text.toString()

            if (TextUtils.isEmpty(txtFirstName) || TextUtils.isEmpty(txtLastName)) {
                Toast.makeText(this.activity, "Empty credentials!!", Toast.LENGTH_SHORT)
                    .show()
            } else {
                storageRef.child("images/defaultPic.jpg").downloadUrl.addOnSuccessListener {
                    Log.d("URL", it.toString())
                    val updates: MutableMap<String, Any> = HashMap()
                    updates["nome"] = "$txtFirstName $txtLastName"
//                    updates["email"] = email
                    updates["photoUrl"] = it.toString()

                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName("$txtFirstName $txtLastName")
                        .setPhotoUri(Uri.parse(it.toString()))
                        .build()

                    user?.updateProfile(profileUpdates)

                    db.collection("users").document(user!!.uid).update(updates)
                    navController.navigate(R.id.action_editarPerfilFragment_to_perfilFragment)

                }.addOnFailureListener {
                    Log.d("URL", "Fail", it)
                }
            }
        }


    }

}