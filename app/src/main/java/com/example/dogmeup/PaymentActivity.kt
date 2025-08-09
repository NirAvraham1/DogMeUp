package com.example.dogmeup

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

class PaymentActivity : AppCompatActivity() {

    private lateinit var tvPaymentStatus: TextView
    private lateinit var btnReturnHome: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        tvPaymentStatus = findViewById(R.id.tvPaymentStatus)
        btnReturnHome = findViewById(R.id.btnReturnHome)
        btnReturnHome.visibility = Button.GONE

        // ----- קלט מה-Intent -----
        val sitterId = intent.getStringExtra("sitterId") ?: ""
        val availabilityId = intent.getStringExtra("availabilityId") ?: ""
        val sitterFullName = intent.getStringExtra("fullName") ?: ""
        val rate = intent.getIntExtra("rate", 0)
        val clientPhotoUrl = intent.getStringExtra("photoUrl") ?: ""
        val availability = intent.getStringExtra("availability") ?: "" // "yyyy-MM-dd HH:mm–HH:mm" או "yyyy-MM-dd HH:mm-HH:mm"

        // פיענוח תאריך ושעות (תמיכה גם במקף רגיל וגם במקף ארוך)
        val parts = availability.trim().split(" ", limit = 2)
        val date = parts.getOrNull(0) ?: ""
        val timeRange = parts.getOrNull(1)?.replace('–', '-') ?: ""
        val times = timeRange.split("-", limit = 2)
        val startTime = times.getOrNull(0) ?: ""
        val endTime = times.getOrNull(1) ?: ""

        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: ""

        // ----- "תשלום" מדומה -----
        Handler(Looper.getMainLooper()).postDelayed({
            tvPaymentStatus.text = "Payment Approved 🎉"

            val db = FirebaseFirestore.getInstance()

            // 1) מחיקת הזמינות מהסיטר (לא קריטי להמשך השמירה)
            db.collection("users")
                .document(sitterId)
                .collection("availability")
                .document(availabilityId)
                .delete()
                .addOnSuccessListener { Toast.makeText(this, "Availability removed", Toast.LENGTH_SHORT).show() }
                .addOnFailureListener { e -> Toast.makeText(this, "Failed to remove availability: ${e.message}", Toast.LENGTH_SHORT).show() }

            // 2) שליפת שם הלקוח ושמירת ההזמנה
            fetchClientNameAndSaveBooking(
                db = db,
                authDisplayName = auth.currentUser?.displayName,
                userId = userId,
                sitterId = sitterId,
                sitterFullName = sitterFullName,
                rate = rate,
                date = date,
                startTime = startTime,
                endTime = endTime,
                clientPhotoUrl = clientPhotoUrl
            )

        }, 1500)

        // חזרה למסך הבית
        btnReturnHome.setOnClickListener {
            startActivity(Intent(this, ClientHomeActivity::class.java))
            finish()
        }
    }

    private fun fetchClientNameAndSaveBooking(
        db: FirebaseFirestore,
        authDisplayName: String?,
        userId: String,
        sitterId: String,
        sitterFullName: String,
        rate: Int,
        date: String,
        startTime: String,
        endTime: String,
        clientPhotoUrl: String
    ) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val clientName = userDoc.getString("fullName") ?: authDisplayName ?: "Dog Lover"
                saveBooking(
                    db = db,
                    sitterId = sitterId,
                    sitterFullName = sitterFullName,
                    rate = rate,
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    clientId = userId,
                    clientName = clientName,
                    clientPhotoUrl = clientPhotoUrl
                )
            }
            .addOnFailureListener {
                val clientName = authDisplayName ?: "Dog Lover"
                saveBooking(
                    db = db,
                    sitterId = sitterId,
                    sitterFullName = sitterFullName,
                    rate = rate,
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    clientId = userId,
                    clientName = clientName,
                    clientPhotoUrl = clientPhotoUrl
                )
            }
    }

    private fun saveBooking(
        db: FirebaseFirestore,
        sitterId: String,
        sitterFullName: String,
        rate: Int,
        date: String,
        startTime: String,
        endTime: String,
        clientId: String,
        clientName: String,
        clientPhotoUrl: String
    ) {
        // המרת מחרוזות זמן ל-Timestamp אמיתי (לטובת עדכונים אוטומטיים/סינון)
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val startLdt = runCatching { LocalDateTime.parse("$date $startTime", fmt) }.getOrNull()
        val endLdt = runCatching { LocalDateTime.parse("$date $endTime", fmt) }.getOrNull()
        val startAt = startLdt?.let { Timestamp(Date.from(it.atZone(ZoneId.systemDefault()).toInstant())) }
        val endAt = endLdt?.let { Timestamp(Date.from(it.atZone(ZoneId.systemDefault()).toInstant())) }

        val booking = hashMapOf(
            "sitterId" to sitterId,
            "sitterName" to sitterFullName,
            "rate" to rate,
            "date" to date,
            "startTime" to startTime,
            "endTime" to endTime,
            "clientId" to clientId,
            "clientName" to clientName,
            "photoUrl" to clientPhotoUrl,

            // לשני הפופאפים:
            "status" to "upcoming",              // בסיום שירות לשנות ל-"completed"
            "reviewSubmittedClient" to false,
            "reviewSubmittedSitter" to false,

            // טיימסטמפים שימושיים:
            "startAt" to (startAt ?: Timestamp.now()),
            "endAt" to (endAt ?: Timestamp.now()),
            "createdAt" to Timestamp.now()
        )

        db.collection("bookings")
            .add(booking)
            .addOnSuccessListener {
                Toast.makeText(this, "Booking saved", Toast.LENGTH_SHORT).show()
                btnReturnHome.visibility = Button.VISIBLE
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save booking: ${e.message}", Toast.LENGTH_SHORT).show()
                btnReturnHome.visibility = Button.VISIBLE
            }
    }
}
