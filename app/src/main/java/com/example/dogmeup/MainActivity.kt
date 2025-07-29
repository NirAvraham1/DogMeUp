package com.example.dogmeup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var btnFindSitter: Button
    private lateinit var btnOfferSitting: Button
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FirebaseAuth.getInstance().signOut()

        tvWelcome = findViewById(R.id.tvWelcome)
        btnFindSitter = findViewById(R.id.btnFindSitter)
        btnOfferSitting = findViewById(R.id.btnOfferSitting)
        btnLogout = findViewById(R.id.btnLogout)

        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid

        if (userId == null) {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val fullName = document.getString("fullName") ?: "User"
                    tvWelcome.text = "Welcome, $fullName!"

                    btnFindSitter.setOnClickListener {
                        startActivity(Intent(this, FindSitterActivity::class.java))
                    }

                    btnOfferSitting.setOnClickListener {
                        startActivity(Intent(this, OfferSittingActivity::class.java))
                    }

                    btnLogout.setOnClickListener {
                        auth.signOut()
                        startActivity(Intent(this, RegisterActivity::class.java))
                        finish()
                    }
                } else {
                    Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                    auth.signOut()
                    startActivity(Intent(this, RegisterActivity::class.java))
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }
}
