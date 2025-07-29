package com.example.dogmeup

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PaymentActivity : AppCompatActivity() {

    private lateinit var tvPaymentStatus: TextView
    private lateinit var btnReturnHome: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        tvPaymentStatus = findViewById(R.id.tvPaymentStatus)
        btnReturnHome = findViewById(R.id.btnReturnHome)
        btnReturnHome.visibility = Button.GONE

        // קבלת מידע מה־Intent
        val sitterId = intent.getStringExtra("sitterId") ?: ""
        val availabilityId = intent.getStringExtra("availabilityId") ?: ""
        val fullName = intent.getStringExtra("fullName") ?: ""
        val rate = intent.getIntExtra("rate", 0)
        val photoUrl = intent.getStringExtra("photoUrl") ?: ""
        val availability = intent.getStringExtra("availability") ?: ""

        // פיצול זמינות (דוגמה: "2025-06-22 09:00–12:00")
        val parts = availability.split(" ")
        val date = parts.getOrNull(0) ?: ""
        val timeRange = parts.getOrNull(1) ?: ""
        val times = timeRange.split("–")
        val startTime = times.getOrNull(0) ?: ""
        val endTime = times.getOrNull(1) ?: ""

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // סימולציית תשלום
        Handler(Looper.getMainLooper()).postDelayed({
            tvPaymentStatus.text = "Payment Approved 🎉"

            val db = FirebaseFirestore.getInstance()

            // שלב 1: הסרת הזמינות מהסיטר
            db.collection("users")
                .document(sitterId)
                .collection("availability")
                .document(availabilityId)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Availability removed", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to remove availability: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            // שלב 2: שמירת ההזמנה
            val booking = hashMapOf(
                "sitterId" to sitterId,
                "sitterName" to fullName,
                "rate" to rate,
                "date" to date,
                "startTime" to startTime,
                "endTime" to endTime,
                "clientId" to userId,
                "photoUrl" to photoUrl
            )
            db.collection("bookings")
                .add(booking)
                .addOnSuccessListener {
                    Toast.makeText(this, "Booking saved", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to save booking: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            btnReturnHome.visibility = Button.VISIBLE
        }, 2000)

        // חזרה למסך הבית
        btnReturnHome.setOnClickListener {
            startActivity(Intent(this, ClientHomeActivity::class.java))
            finish()
        }
    }
}
