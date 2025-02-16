package com.example.myvideolist.ui.dashboard

import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myvideolist.databinding.FragmentAddVideoBinding

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage


class AddVideoFragment : Fragment() {

    private var _binding: FragmentAddVideoBinding? = null
    private val binding get() = _binding!!

    private val VIDEO_PICK_GALLERY_CODE = 100
    private val VIDEO_PICK_CAMERA_CODE = 101
    private val CAMERA_REQUEST_CODE = 102
    private lateinit var cameraPermissions: Array<String>
    private lateinit var progressDialog: ProgressDialog
    private var videoUri: Uri? = null
    private var title: String? = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentAddVideoBinding.inflate(inflater, container, false)
        val root: View = binding.root

        cameraPermissions =    arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        progressDialog = ProgressDialog(requireContext())
        progressDialog.setTitle("Please wait")
        progressDialog.setMessage("Uploading video...")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.picVideoFab.setOnClickListener {
            showPickDialog()
        }
        binding.btnUploadVideo.setOnClickListener {
            title = binding.tvTitleVideo.text.toString().trim()
            if (TextUtils.isEmpty(title)) {
            Toast.makeText(requireContext(), "Title is required", Toast.LENGTH_SHORT).show()
            }
            else {
            if (videoUri == null) {
                Toast.makeText(requireContext(), "Pick the video first", Toast.LENGTH_SHORT)
                .show()
            }
            else {
                uploadVideoToFireBase()        }
        }}

        return root
    }

    private fun uploadVideoToFireBase() {
        progressDialog.show()
        val timeStamp = "" + System.currentTimeMillis()

        //prepare  filePath and Name  for FireBaseStorage
        val filePathAndName = "Videos/video_$timeStamp"

         //FireBaseStorage reference
        val storageReference = FirebaseStorage.getInstance().getReference(filePathAndName)
        // upload video using uri of video to FirebaseStorage
        storageReference.putFile(videoUri!!)
            .addOnSuccessListener { taskSnapshot ->
            // uploaded, get uri of uploaded video
                val uriTask = taskSnapshot.storage.downloadUrl
            while (!uriTask.isSuccessful);
                val downloadUri = uriTask.result
            if (uriTask.isSuccessful) {
                //  download video_url is received successfully
                // we can add  video details to Firebase Database

                val hashMap = HashMap<String, Any>()
                hashMap["id"] = "$timeStamp"
                hashMap["title"] = "$title"
                hashMap["timestamp"] ="$timeStamp"
                hashMap["videoUri"] = "$downloadUri"
                //put the above info to Firebase Database
                val dbReference = FirebaseDatabase.getInstance().getReference("Videos")
                dbReference.child(timeStamp)
                    .setValue(hashMap)
                    .addOnSuccessListener {
                        //video info added to to FirebaseDatabase successfully
                        progressDialog.dismiss()
                        Toast.makeText(requireContext(), "Video uploaded", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        //failed to add video info  to FirebaseDatabase
                        progressDialog.dismiss()
                        Toast.makeText(requireContext(), "Failed to upload video to DataBase, " +
                                "please try again later" , Toast.LENGTH_SHORT).show()
                    }
            }
        }        .addOnFailureListener {
            progressDialog.dismiss()
                Toast.makeText(requireContext(), "Failed to upload Video", Toast.LENGTH_SHORT).show()
        }}


    private fun showPickDialog() {    // options for display in AlertDialog
        val options = arrayOf("Camera","Gallery" )
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Pick Video From")        .setItems(options) { dialogInterface, i ->
            //handle item clicks
        if (i == 0) {
            //camera clicked
        if (!checkCameraPermissions()) {
            //permissions was  not allowed, request
        requestCameraPermissions()
        } else {
            //permissions was allowed, pick video
            videoPickCamera()
        }
    } else {
        // gallery clicked
        videoPickGallery()
    }
}        .show()
}
private fun requestCameraPermissions() {
    requireActivity().requestPermissions(
    cameraPermissions,
        CAMERA_REQUEST_CODE
)}
private fun checkCameraPermissions(): Boolean {
    val result1 = requireActivity().checkSelfPermission(
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    val result2 = requireActivity().checkSelfPermission(
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
    return result1 && result2
}

private fun videoPickGallery() {
    val intent = Intent()
    intent.type = "video/*"
    intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(
                Intent.createChooser(intent, "choose video"),
                VIDEO_PICK_GALLERY_CODE    )
}
private fun videoPickCamera() {
    val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
    startActivityForResult(intent, VIDEO_PICK_CAMERA_CODE)
}
//work with permission results
override fun onRequestPermissionsResult(
    requestCode: Int,
       permissions: Array<out String>,
    grantResults: IntArray
) {
    when(requestCode){
    CAMERA_REQUEST_CODE ->
        if(grantResults.size > 0){
        //check if  permissions allowed or denied
            val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
        val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
            if(cameraAccepted && storageAccepted){
            //both permissions allowed
            videoPickCamera()
        }
            else{
            //both or of those permissions are denied
                Toast.makeText(requireContext(),
            "Camera or storage permissions are denied", Toast.LENGTH_SHORT).show()                }
    }
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)}

override
fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if(resultCode == RESULT_OK){        //video is picked from camera or gallery
        if(requestCode == VIDEO_PICK_CAMERA_CODE){            //video picked from camera
            videoUri = data!!.data
            setVideoToPage()
        }        else if(requestCode == VIDEO_PICK_GALLERY_CODE){
            //video picked from gallery
            videoUri = data!!.data
            setVideoToPage()
        }
    }
    super.onActivityResult(requestCode, resultCode, data)
}


private fun setVideoToPage() {
    val mediaController = MediaController(requireActivity())
    mediaController.setAnchorView(binding.videoView)
    binding.videoView.setMediaController(mediaController)
    binding.videoView.setVideoURI(videoUri)
    binding.videoView.requestFocus()
    binding.videoView.setOnPreparedListener {
        binding.videoView.pause()
    }
}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}