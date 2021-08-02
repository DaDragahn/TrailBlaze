package dam.a42363.trailblaze

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import dam.a42363.trailblaze.databinding.FragmentEditarPerfilBinding
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream


class EditarPerfilFragment : Fragment() {

    var _binding: FragmentEditarPerfilBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var isNewPhoto: Boolean = false
    private lateinit var navController: NavController
    private lateinit var photoUrl: String
    private lateinit var img: ByteArray


    @SuppressLint("LogNotTimber")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment

        _binding = FragmentEditarPerfilBinding.inflate(inflater, container, false)

        db = FirebaseFirestore.getInstance()
        val getImage = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) {
            isNewPhoto = true
            binding.profileImage.setImageURI(it)
            val iStream: InputStream? = requireActivity().contentResolver.openInputStream(it)
            img = iStream?.let { it1 -> getBytes(it1) }!!
        }
        binding.alterarFoto.setOnClickListener {
            getImage.launch("image/*")
        }
        return binding.root
    }

    @Throws(IOException::class)
    fun getBytes(inputStream: InputStream): ByteArray {
        val byteBuffer = ByteArrayOutputStream()
        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)
        var len: Int
        while (inputStream.read(buffer).also { len = it } != -1) {
            byteBuffer.write(buffer, 0, len)
        }
        return byteBuffer.toByteArray()
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
                            photoUrl = documentSnapshot.getString("photoUrl").toString()
                            if (photoUrl != "")
                                Glide.with(this).load(photoUrl)
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
                if (isNewPhoto) {
                    //val userRef = storageRef.child("user.jpg")

                    val userRefImagesRef = storageRef.child("images/${user?.uid}/user.jpg")
                    val uploadTask: UploadTask = userRefImagesRef.putBytes(img)
                    uploadTask.addOnFailureListener {
                        // Handle unsuccessful uploads
                    }
                        .addOnSuccessListener {   // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                            userRefImagesRef.downloadUrl.addOnSuccessListener {
                                photoUrl = "$it"
                                val updates: MutableMap<String, Any> = HashMap()
                                updates["nome"] = "$txtFirstName $txtLastName"
                                updates["photoUrl"] = photoUrl

                                val profileUpdates = UserProfileChangeRequest.Builder()
                                    .setDisplayName("$txtFirstName $txtLastName")
                                    .setPhotoUri(Uri.parse(photoUrl))
                                    .build()

                                user?.updateProfile(profileUpdates)

                                db.collection("users").document(user!!.uid).update(updates)
                                navController.navigate(R.id.action_editarPerfilFragment_to_perfilFragment)
                            }
                        }
                } else {
                    val updates: MutableMap<String, Any> = HashMap()
                    updates["nome"] = "$txtFirstName $txtLastName"
//                    updates["email"] = email
                    updates["photoUrl"] = photoUrl

                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName("$txtFirstName $txtLastName")
                        .setPhotoUri(Uri.parse(photoUrl))
                        .build()

                    user?.updateProfile(profileUpdates)

                    db.collection("users").document(user!!.uid).update(updates)
                    navController.navigate(R.id.action_editarPerfilFragment_to_perfilFragment)
                }
            }
        }


    }

}