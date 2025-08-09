package com.example.dogmeup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class VerifyCodeActivity : AppCompatActivity() {

    private lateinit var etCode: EditText
    private lateinit var btnVerify: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var verificationId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_code)

        etCode = findViewById(R.id.etCode)
        btnVerify = findViewById(R.id.btnVerify)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        verificationId = intent.getStringExtra("verificationId") ?: ""

        btnVerify.setOnClickListener {
            val code = etCode.text.toString().trim()
            if (code.isNotEmpty()) {
                val credential = PhoneAuthProvider.getCredential(verificationId, code)
                auth.signInWithCredential(credential)
                    .addOnSuccessListener { result ->
                        val userId = result.user?.uid ?: return@addOnSuccessListener

                        // יצירת מסמך ריק עם מזהה משתמש, נשלח רק את מספר הטלפון
                        val userData = hashMapOf(
                            "phone" to result.user?.phoneNumber
                        )

                        db.collection("users").document(userId)
                            .set(userData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Phone login successful", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, CompleteProfileActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Firestore error: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Login failed: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(this, "Please enter the code", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
