package com.example.dogmeup

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class EditProfileActivity : AppCompatActivity() {

    private lateinit var ivEditProfilePhoto: ImageView
    private lateinit var etEditFullName: EditText
    private lateinit var etEditBio: EditText
    private lateinit var btnSaveProfile: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var selectedImageUri: Uri? = null
    private lateinit var etEditPhone: EditText


    companion object {
        private const val PICK_IMAGE_REQUEST = 1010
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        ivEditProfilePhoto = findViewById(R.id.ivEditProfilePhoto)
        etEditFullName = findViewById(R.id.etEditFullName)
        etEditBio = findViewById(R.id.etEditBio)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)
        etEditPhone = findViewById(R.id.editTextPhone)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        loadCurrentProfile()

        ivEditProfilePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
        }

        btnSaveProfile.setOnClickListener {
            saveProfileChanges()
        }
    }

    private fun loadCurrentProfile() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                etEditFullName.setText(doc.getString("fullName") ?: "")
                etEditPhone.setText(doc.getString("phone") ?: "")
                etEditBio.setText(doc.getString("bio") ?: "")

                val photoUrl = doc.getString("profilePhotoUrl")
                if (!photoUrl.isNullOrEmpty()) {
                    Glide.with(this).load(photoUrl).into(ivEditProfilePhoto)
                }
            }
    }

    private fun saveProfileChanges() {
        val userId = auth.currentUser?.uid ?: return
        val fullName = etEditFullName.text.toString().trim()
        val phone = etEditPhone.text.toString().trim()
        val bio = etEditBio.text.toString().trim()

        if (fullName.isEmpty()) {
            Toast.makeText(this, "Full name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = hashMapOf(
            "fullName" to fullName,
            "phone" to phone,
            "bio" to bio
        )

        if (selectedImageUri != null) {
            val ref = storage.reference.child("users/$userId/profile.jpg")
            ref.putFile(selectedImageUri!!)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { uri ->
                        updates["profilePhotoUrl"] = uri.toString()

                        db.collection("users").document(userId)
                            .update(updates as Map<String, Any>)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                    }
                }
        } else {
            db.collection("users").document(userId)
                .update(updates as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                    finish()
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            ivEditProfilePhoto.setImageURI(selectedImageUri)
        }
    }
}
