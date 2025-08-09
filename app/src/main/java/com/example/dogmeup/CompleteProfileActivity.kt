package com.example.dogmeup

import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Button

class CompleteProfileActivity : AppCompatActivity() {

    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var cbBecomeSitter: CheckBox
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complete_profile)

        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        cbBecomeSitter = findViewById(R.id.cbBecomeSitter)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val user = auth.currentUser
        val existingEmail = user?.email

        if (!existingEmail.isNullOrEmpty()) {
            etEmail.setText(existingEmail)
            etEmail.isEnabled = false
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val isSitter = cbBecomeSitter.isChecked
            val userId = user?.uid

            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || userId == null) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // קישור חשבון לסיסמה ואימייל
            val credential = EmailAuthProvider.getCredential(email, password)
            user.linkWithCredential(credential)
                .addOnSuccessListener {
                    saveProfile(userId, fullName, email, isSitter)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to link email/password: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun saveProfile(userId: String, fullName: String, email: String, isSitter: Boolean) {
        val updates = mapOf(
            "fullName" to fullName,
            "email" to email,
            "isSitter" to isSitter
        )

        db.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                val intent = if (isSitter)
                    Intent(this, SitterSetupActivity::class.java)
                else
                    Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update profile: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }
}
