package com.example.dogmeup

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class SitterHomeActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var layoutAvailabilities: LinearLayout
    private lateinit var btnAddAvailability: Button
    private lateinit var btnLogout: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val pendingBookings = mutableListOf<DocumentSnapshot>()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sitter_home)

        // מוסיף padding תחתון דינמי כדי שה-Logout לא יכוסה
        val root = findViewById<LinearLayout>(R.id.rootContainer)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val extra = (16 * resources.displayMetrics.density).toInt() // מרווח נוסף
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, bars.bottom + extra)
            insets
        }

        tvWelcome = findViewById(R.id.tvWelcome)
        layoutAvailabilities = findViewById(R.id.layoutAvailabilities)
        btnAddAvailability = findViewById(R.id.btnAddAvailability)
        btnLogout = findViewById(R.id.btnLogout)

        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId)
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
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        val btnMyOrders = findViewById<Button>(R.id.btnMyOrders)
        btnMyOrders.setOnClickListener {
            startActivity(Intent(this, MyOrdersActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val userId = auth.currentUser?.uid ?: return

        loadAvailabilities(userId)

        autoCompletePastBookingsForSitter(userId) {
            // אחרי השלמה אוטומטית, נטען פופ־אפים לסיטר
            loadAllPendingSitterReviews(userId)
        }
    }

    private fun loadAvailabilities(userId: String) {
        layoutAvailabilities.removeAllViews()

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("availability")
            .get()
            .addOnSuccessListener { result ->
                val sdfDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val now = Calendar.getInstance().time

                for (doc in result) {
                    val date = doc.getString("date") ?: continue
                    val start = doc.getString("startTime") ?: continue
                    val end = doc.getString("endTime") ?: "-"

                    val fullStart = "$date $start"
                    val startDate = try { sdfDateTime.parse(fullStart) } catch (_: Exception) { null }

                    if (startDate != null && startDate.after(now)) {
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

    private fun loadAllPendingSitterReviews(userId: String) {
        db.collection("bookings")
            .whereEqualTo("sitterId", userId)
            .whereEqualTo("status", "completed")
            .whereEqualTo("reviewSubmittedSitter", false)
            .get()
            .addOnSuccessListener { result ->
                pendingBookings.clear()
                pendingBookings.addAll(result.documents)
                currentIndex = 0
                if (pendingBookings.isNotEmpty()) {
                    showNextReviewDialogForSitter()
                }
            }
    }

    private fun showNextReviewDialogForSitter() {
        if (currentIndex >= pendingBookings.size) return

        val booking = pendingBookings[currentIndex]
        val clientId = booking.getString("clientId") ?: return
        val bookingId = booking.id

        FirebaseFirestore.getInstance().collection("users").document(clientId)
            .get()
            .addOnSuccessListener { userDoc ->
                val clientName = userDoc.getString("fullName")
                    ?: booking.getString("clientName")
                    ?: "Client"

                val dialogView = layoutInflater.inflate(R.layout.dialog_review, null)
                val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
                val commentInput = dialogView.findViewById<EditText>(R.id.etComment)

                val dialog = AlertDialog.Builder(this)
                    .setTitle("Rate $clientName")
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
                            "clientId" to clientId,
                            "sitterId" to (auth.currentUser?.uid ?: ""),
                            "sitterName" to (tvWelcome.text?.toString()?.removePrefix("Welcome, ") ?: ""),
                            "clientName" to clientName,
                            "rating" to rating,
                            "comment" to comment,
                            "reviewerId" to (auth.currentUser?.uid ?: ""),
                            "reviewerRole" to "sitter",
                            "timestamp" to com.google.firebase.Timestamp.now()
                        )

                        db.collection("reviews").add(review).addOnSuccessListener { ref ->
                            val updates = mapOf(
                                "reviewSubmittedSitter" to true,
                                "sitterReviewId" to ref.id
                            )
                            db.collection("bookings").document(bookingId)
                                .update(updates)
                                .addOnSuccessListener {
                                    dialog.dismiss()
                                    currentIndex++
                                    showNextReviewDialogForSitter()
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
            .addOnFailureListener {
                // אפשר להוסיף טיפול בשגיאה אם רוצים
            }
    }

    private fun autoCompletePastBookingsForSitter(sitterId: String, onDone: () -> Unit) {
        val now = com.google.firebase.Timestamp.now()
        db.collection("bookings")
            .whereEqualTo("sitterId", sitterId)
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
