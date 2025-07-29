package com.example.dogmeup

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class SitterSetupActivity : AppCompatActivity() {

    private lateinit var etBio: EditText
    private lateinit var etRate: EditText
    private lateinit var ivProfilePhoto: ImageView
    private lateinit var btnSave: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private var selectedImageUri: Uri? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1001
        private const val PERMISSION_REQUEST_CODE = 2001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sitter_setup)

        etBio = findViewById(R.id.etBio)
        etRate = findViewById(R.id.etRate)
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto)
        btnSave = findViewById(R.id.btnSaveBio)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        ivProfilePhoto.setOnClickListener {
            checkAndRequestPermissions()
        }

        btnSave.setOnClickListener {
            val bio = etBio.text.toString().trim()
            val rateText = etRate.text.toString().trim()
            val rate = rateText.toIntOrNull() ?: 0
            val userId = auth.currentUser?.uid

            if (bio.isEmpty() || rate <= 0) {
                Toast.makeText(this, "Please enter a valid bio and hourly rate", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userId == null) {
                Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedImageUri == null) {
                Toast.makeText(this, "Please select a profile photo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fileName = "users/$userId/profile.jpg"
            val ref = storage.reference.child(fileName)

            ref.putFile(selectedImageUri!!)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { uri ->
                        val updates = mapOf(
                            "bio" to bio,
                            "rate" to rate,
                            "profilePhotoUrl" to uri.toString()
                        )

                        db.collection("users").document(userId)
                            .update(updates)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, SitterHomeActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to update Firestore: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST_CODE)
            } else {
                openFileChooser()
            }
        } else {
            openFileChooser()
        }
    }

    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openFileChooser()
        } else {
            Toast.makeText(this, "Permission denied to access images", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            ivProfilePhoto.setImageURI(selectedImageUri)
        }
    }
}
