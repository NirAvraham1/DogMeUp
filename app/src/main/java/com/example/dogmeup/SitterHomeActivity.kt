package com.example.dogmeup

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SitterHomeActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var layoutAvailabilities: LinearLayout
    private lateinit var btnAddAvailability: Button
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sitter_home)

        tvWelcome = findViewById(R.id.tvWelcome)
        layoutAvailabilities = findViewById(R.id.layoutAvailabilities)
        btnAddAvailability = findViewById(R.id.btnAddAvailability)
        btnLogout = findViewById(R.id.btnLogout)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener { doc ->
                    val fullName = doc.getString("fullName") ?: "Sitter"
                    tvWelcome.text = "Welcome, $fullName!"
                }

            loadAvailabilities(userId)
        }

        btnAddAvailability.setOnClickListener {
            startActivity(Intent(this, AddAvailabilityActivity::class.java))
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        val btnMyOrders = findViewById<Button>(R.id.btnMyOrders)

        btnMyOrders.setOnClickListener {
            val intent = Intent(this, MyOrdersActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onResume() {
        super.onResume()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        loadAvailabilities(userId)
    }

    private fun loadAvailabilities(userId: String) {
        layoutAvailabilities.removeAllViews()

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("availability")
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val date = doc.getString("date") ?: "-"
                    val start = doc.getString("startTime") ?: "-"
                    val end = doc.getString("endTime") ?: "-"

                    val tv = TextView(this).apply {
                        text = "🗓 $date | ⏰ $start - $end"
                        textSize = 16f
                        setPadding(8, 8, 8, 8)
                        setTextColor(resources.getColor(android.R.color.black))
                    }

                    layoutAvailabilities.addView(tv)
                }
            }
    }
}
