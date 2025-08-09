package com.example.dogmeup

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class ClientHomeActivity : AppCompatActivity() {

    private lateinit var tvWelcomeClient: TextView
    private lateinit var btnFindSitter: Button
    private lateinit var btnSettings: Button
    private lateinit var btnLogout: Button
    private lateinit var btnMyOrders: Button
    private lateinit var btnServiceHistory: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val pendingBookings = mutableListOf<DocumentSnapshot>()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_home)

        tvWelcomeClient = findViewById(R.id.tvWelcomeClient)
        btnFindSitter = findViewById(R.id.btnFindSitter)
        btnSettings = findViewById(R.id.btnSettings)
        btnLogout = findViewById(R.id.btnLogout)
        btnMyOrders = findViewById(R.id.btnMyOrders)
        btnServiceHistory = findViewById(R.id.btnServiceHistory) // ← כפתור ההיסטוריה החדש

        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { doc ->
                    val fullName = doc.getString("fullName") ?: "Dog Lover"
                    tvWelcomeClient.text = "Welcome, $fullName 🐶"
                }
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

        btnMyOrders.setOnClickListener {
            startActivity(Intent(this, MyOrdersActivity::class.java)) // הזמנות עתידיות
        }

        btnServiceHistory.setOnClickListener {
            startActivity(Intent(this, ClientHistoryActivity::class.java)) // היסטוריית שירותים (חוות דעת סיטרים עליי)
        }
    }

    override fun onResume() {
        super.onResume()
        val userId = auth.currentUser?.uid ?: return
        autoCompletePastBookingsForClient(userId) {
            // אחרי שהשלמנו אוטומטית הזמנות שעבר זמנן, נטען פופ־אפים
            loadAllPendingClientReviews(userId)
        }
    }


    private fun loadAllPendingClientReviews(userId: String) {
        db.collection("bookings")
            .whereEqualTo("clientId", userId)
            .whereEqualTo("status", "completed")
            .whereEqualTo("reviewSubmittedClient", false)
            .get()
            .addOnSuccessListener { result ->
                pendingBookings.clear()
                pendingBookings.addAll(result.documents)
                currentIndex = 0
                if (pendingBookings.isNotEmpty()) {
                    showNextReviewDialog()
                }
            }
    }

    private fun showNextReviewDialog() {
        if (currentIndex >= pendingBookings.size) return

        val booking = pendingBookings[currentIndex]
        val sitterName = booking.getString("sitterName") ?: "Your sitter"
        val sitterId = booking.getString("sitterId") ?: return
        val bookingId = booking.id

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_review, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
        val commentInput = dialogView.findViewById<EditText>(R.id.etComment)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Rate $sitterName")
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Submit", null)
            .create()

        dialog.setCanceledOnTouchOutside(false)

        dialog.setOnShowListener {
            val btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btn.setOnClickListener {
                val rating = ratingBar.rating.toInt()
                val comment = commentInput.text.toString().trim()

                if (rating <= 0) {
                    Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val review = hashMapOf(
                    "bookingId" to bookingId,
                    "clientId" to (auth.currentUser?.uid ?: ""),
                    "sitterId" to sitterId,
                    "sitterName" to sitterName,
                    "rating" to rating,
                    "comment" to comment,
                    "reviewerId" to (auth.currentUser?.uid ?: ""),
                    "reviewerRole" to "client",
                    "timestamp" to Timestamp.now()
                )

                db.collection("reviews").add(review).addOnSuccessListener { ref ->
                    val updates = mapOf(
                        "reviewSubmittedClient" to true,
                        "clientReviewId" to ref.id
                    )
                    db.collection("bookings").document(bookingId)
                        .update(updates)
                        .addOnSuccessListener {
                            dialog.dismiss()
                            currentIndex++
                            showNextReviewDialog()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to update booking", Toast.LENGTH_LONG).show()
                        }
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed to save review", Toast.LENGTH_LONG).show()
                }
            }
        }

        dialog.show()
    }

    private fun autoCompletePastBookingsForClient(clientId: String, onDone: () -> Unit) {
        val now = com.google.firebase.Timestamp.now()
        db.collection("bookings")
            .whereEqualTo("clientId", clientId)
            .whereEqualTo("status", "upcoming")
            .whereLessThanOrEqualTo("endAt", now)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) { onDone(); return@addOnSuccessListener }
                val batch = db.batch()
                snap.documents.forEach { doc -> batch.update(doc.reference, "status", "completed") }
                batch.commit().addOnCompleteListener { onDone() }
            }
            .addOnFailureListener { onDone() }
    }

}
