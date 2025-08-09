package com.example.dogmeup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class PhoneRegisterActivity : AppCompatActivity() {

    private lateinit var etPhoneNumber: EditText
    private lateinit var btnSendCode: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks

    private lateinit var verificationId: String // נשמור את הקוד שיגיע

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_register)

        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        btnSendCode = findViewById(R.id.btnSendCode)
        auth = FirebaseAuth.getInstance()

        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                // במקרים מסוימים הקוד ייכנס לבד – לא נטפל בזה כרגע
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Toast.makeText(this@PhoneRegisterActivity, "Verification Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                super.onCodeSent(verificationId, token)
                this@PhoneRegisterActivity.verificationId = verificationId

                Toast.makeText(this@PhoneRegisterActivity, "Code Sent! Now enter the code", Toast.LENGTH_SHORT).show()

                // כאן נעבור לשלב הבא - למסך שבו מקישים את הקוד
                val intent = Intent(this@PhoneRegisterActivity, VerifyCodeActivity::class.java)
                intent.putExtra("verificationId", verificationId)
                startActivity(intent)
            }
        }

        btnSendCode.setOnClickListener {
            val phoneNumber = etPhoneNumber.text.toString().trim()
            if (phoneNumber.isNotEmpty()) {
                sendVerificationCode(phoneNumber)
            } else {
                Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendVerificationCode(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }
}
