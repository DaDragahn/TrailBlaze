package dam.a42363.trailblaze

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import dam.a42363.trailblaze.databinding.FragmentCameraBinding
import dam.a42363.trailblaze.utils.Constants
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {
    private lateinit var callback: OnBackPressedCallback
    private var idTrail: String? = null
    private lateinit var storageRef: StorageReference
    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var auth: FirebaseAuth
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        callback = requireActivity().onBackPressedDispatcher.addCallback(this) {

        }

        callback.isEnabled = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
        auth = FirebaseAuth.getInstance()
        storageRef = FirebaseStorage.getInstance().reference
        idTrail = arguments?.getString("idTrail")
        startCamera()

        binding.cameraBtn.setOnClickListener {
            callback.isEnabled = true
            binding.cameraBtn.visibility = View.GONE
            binding.circularProgressBar.visibility = View.VISIBLE
            takePhoto()
        }


        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity != null && this.activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView?.visibility = View.GONE
        }
    }

    private fun getOutputDirectory(): File {
        val mediaDir = requireActivity().externalMediaDirs.firstOrNull()?.let { mFile ->
            File(mFile, resources.getString(R.string.app_name)).apply {
                mkdirs()
            }
        }

        return if (mediaDir != null && mediaDir.exists())
            mediaDir else requireActivity().filesDir
    }

    private fun takePhoto() {

        val imageCapture = imageCapture ?: return
        val photoFile =
            File(
                outputDirectory,
                SimpleDateFormat(
                    Constants.FILE_NAME_FORMAT,
                    Locale.getDefault()
                ).format(System.currentTimeMillis()) + ".jpg"
            )

        val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOption,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)

                    val userRefImagesRef =
                        storageRef.child("images/${auth.currentUser?.uid}/locations/${idTrail}/${photoFile.name}")
                    val uploadTask: UploadTask = userRefImagesRef.putFile(savedUri)
                    uploadTask.addOnFailureListener {
                        Log.d(Constants.TAG, "onError: ${it.message}", it)
                    }
                        .addOnSuccessListener {   // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                            val msg = "Photo Saved"

                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT)
                                .show()
                            binding.circularProgressBar.visibility = View.GONE
                            binding.cameraBtn.visibility = View.VISIBLE
                            callback.isEnabled = false
                        }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.d(Constants.TAG, "onError: ${exception.message}", exception)
                }
            })
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity())

        cameraProviderFuture.addListener({
            val preview = Preview.Builder().build().also { mPreview ->
                mPreview.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

            } catch (e: Exception) {
                Log.d(Constants.TAG, "startCamera Fail: ", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

}