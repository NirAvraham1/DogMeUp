package com.example.dogmeup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

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

        // אם למשתמש כבר יש אימייל מחשבון Google/Email - נציג וננעל את השדה
        if (!existingEmail.isNullOrEmpty()) {
            etEmail.setText(existingEmail)
            etEmail.isEnabled = false
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val isSitter = cbBecomeSitter.isChecked
            val currentUser = auth.currentUser
            val userId = currentUser?.uid

            if (currentUser == null || userId == null) {
                Toast.makeText(this, "אין משתמש מחובר. נסי להתחבר מחדש.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (fullName.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "נא למלא שם מלא ואימייל.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // בדיקה אם כבר קיים ספק אימייל/סיסמה למשתמש
            val alreadyHasPasswordProvider =
                currentUser.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }

            fun continueToSaveProfile() {
                saveProfile(userId, fullName, email, isSitter)
            }

            if (!alreadyHasPasswordProvider) {
                // אם אין ספק password - נדרוש סיסמה ונקשר את האימייל+סיסמה לחשבון
                if (password.length < 6) {
                    Toast.makeText(this, "סיסמה חייבת להיות באורך 6 תווים לפחות.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val credential = EmailAuthProvider.getCredential(email, password)
                currentUser.linkWithCredential(credential)
                    .addOnSuccessListener {
                        continueToSaveProfile()
                    }
                    .addOnFailureListener { e ->
                        Log.e("CompleteProfile", "linkWithCredential failed", e)
                        Toast.makeText(
                            this,
                            "קישור אימייל/סיסמה נכשל: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        // במקרים של "requires recent login" צריך לבצע reauthenticate לפני link.
                    }
            } else {
                // כבר יש ספק password → שומרים פרופיל בלי לנסות לקשר שוב
                continueToSaveProfile()
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
            .set(updates, SetOptions.merge()) // חשוב: merge כדי ליצור/לעדכן בבטחה
            .addOnSuccessListener {
                Toast.makeText(this, "הפרופיל עודכן בהצלחה.", Toast.LENGTH_SHORT).show()

                // ניתוב לפי סוג משתמש, עם ניקוי היסטוריית המסכים
                val nextIntent = if (isSitter) {
                    Intent(this, SitterSetupActivity::class.java)
                } else {
                    Intent(this, ClientHomeActivity::class.java)
                }
                nextIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(nextIntent)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("CompleteProfile", "saveProfile failed", e)
                Toast.makeText(this, "עדכון פרופיל נכשל: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
