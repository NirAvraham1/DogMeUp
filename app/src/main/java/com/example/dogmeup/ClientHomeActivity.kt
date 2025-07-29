package com.example.dogmeup

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class ClientHomeActivity : AppCompatActivity() {

    private lateinit var tvWelcomeClient: TextView
    private lateinit var btnFindSitter: Button
    private lateinit var btnSettings: Button
    private lateinit var btnLogout: Button
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_home)

        tvWelcomeClient = findViewById(R.id.tvWelcomeClient)
        btnFindSitter = findViewById(R.id.btnFindSitter)
        btnSettings = findViewById(R.id.btnSettings)
        btnLogout = findViewById(R.id.btnLogout)

        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { doc ->
                    val fullName = doc.getString("fullName") ?: "Dog Lover"
                    tvWelcomeClient.text = "Welcome, $fullName 🐶"
                }

            checkForPendingReview(userId)
        }

        btnFindSitter.setOnClickListener {
            startActivity(Intent(this, FindSitterActivity::class.java))
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        val btnMyOrders = findViewById<Button>(R.id.btnMyOrders)
        btnMyOrders.setOnClickListener {
            startActivity(Intent(this, MyOrdersActivity::class.java))
        }
    }

    private fun checkForPendingReview(userId: String) {
        db.collection("bookings")
            .whereEqualTo("clientId", userId)
            .whereEqualTo("status", "completed")
            .whereEqualTo("reviewSubmitted", false)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val booking = result.documents[0]
                    showReviewDialog(booking)
                }
            }
    }

    private fun showReviewDialog(booking: DocumentSnapshot) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_review, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
        val commentInput = dialogView.findViewById<EditText>(R.id.etComment)

        AlertDialog.Builder(this)
            .setTitle("Rate Your Sitter")
            .setView(dialogView)
            .setPositiveButton("Submit") { _, _ ->
                val rating = ratingBar.rating.toInt()
                val comment = commentInput.text.toString()
                val sitterId = booking.getString("sitterId") ?: return@setPositiveButton
                val sitterName = booking.getString("sitterName") ?: ""
                val bookingId = booking.id

                val review = hashMapOf(
                    "bookingId" to bookingId,
                    "clientId" to auth.currentUser?.uid,
                    "sitterId" to sitterId,
                    "sitterName" to sitterName,
                    "rating" to rating,
                    "comment" to comment,
                    "timestamp" to com.google.firebase.Timestamp.now()
                )

                db.collection("reviews").add(review)
                db.collection("bookings").document(bookingId)
                    .update("reviewSubmitted", true)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
