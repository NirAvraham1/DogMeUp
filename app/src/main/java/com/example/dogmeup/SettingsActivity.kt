package com.example.dogmeup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : AppCompatActivity() {

    private lateinit var btnEditProfile: Button
    private lateinit var btnAboutApp: Button
    private lateinit var btnBackToHome: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnAboutApp = findViewById(R.id.btnAbout)
        btnBackToHome = findViewById(R.id.btnBackToHome)

        btnEditProfile.setOnClickListener {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                db.collection("users").document(userId).get()
                    .addOnSuccessListener { document ->
                        val isSitter = document.getBoolean("isSitter") ?: false
                        val intent = if (isSitter) {
                            Intent(this, SitterSetupActivity::class.java)
                        } else {
                            Intent(this, CompleteProfileActivity::class.java)
                        }
                        startActivity(intent)
                    }
            }
        }

        btnAboutApp.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("About DogMeUp")
                .setMessage("DogMeUp v1.0\n\nDeveloped by Nir Avraham.\n\nFor questions or support, contact support@dogmeup.com")
                .setPositiveButton("OK", null)
                .show()
        }

        btnBackToHome.setOnClickListener {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                db.collection("users").document(userId).get()
                    .addOnSuccessListener { document ->
                        val isSitter = document.getBoolean("isSitter") ?: false
                        val intent = if (isSitter) {
                            Intent(this, SitterHomeActivity::class.java)
                        } else {
                            Intent(this, ClientHomeActivity::class.java)
                        }
                        startActivity(intent)
                        finish()
                    }
            }
        }
    }
}
