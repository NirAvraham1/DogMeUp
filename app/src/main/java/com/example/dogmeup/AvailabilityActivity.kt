package com.example.dogmeup

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dogmeup.databinding.ActivityAvailabilityBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AvailabilityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAvailabilityBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isSitter: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAvailabilityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load user data
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    isSitter = document.getBoolean("isSitter") ?: false
                    setupUI()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user info", Toast.LENGTH_SHORT).show()
            }

        binding.btnSaveAvailability.setOnClickListener {
            // נוכל להוסיף כאן את השמירה בפועל בשלב הבא
            Toast.makeText(this, "Saving availability...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupUI() {
        if (isSitter) {
            binding.tvAvailabilityTitle.text = "Set Your Available Hours"
        } else {
            binding.tvAvailabilityTitle.text = "Choose Hours You Need a Sitter"
        }
    }
}
