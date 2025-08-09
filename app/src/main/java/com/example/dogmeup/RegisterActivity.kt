package com.example.dogmeup

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dogmeup.databinding.ActivityRegisterBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val RC_GOOGLE_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        // הגדרת הנראות ההתחלתית של שדה שם הכלב לפי מצב הצ'קבוקס
        binding.editTextDogName.visibility = if (binding.cbBecomeSitter.isChecked) View.GONE else View.VISIBLE

        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()


        binding.cbBecomeSitter.setOnCheckedChangeListener { _, isChecked ->
            binding.editTextDogName.visibility = if (isChecked) View.GONE else View.VISIBLE
        }

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val isSitter = document.getBoolean("isSitter") ?: false
                        val nextActivity = if (isSitter) {
                            SitterHomeActivity::class.java
                        } else {
                            ClientHomeActivity::class.java
                        }
                        startActivity(Intent(this, nextActivity))
                        finish()
                    }
                }
            return
        }

        initGoogleSignIn()

        binding.btnRegister.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val phone = binding.editTextPhone.text.toString().trim()
            val dogName = binding.editTextDogName.text.toString().trim()
            val isSitter = binding.cbBecomeSitter.isChecked

            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val userId = result.user?.uid ?: return@addOnSuccessListener

                    val userData = hashMapOf(
                        "fullName" to fullName,
                        "email" to email,
                        "phone" to phone,
                        "isSitter" to isSitter
                    )
                    if (!isSitter) {
                        userData["dogName"] = dogName
                    }

                    db.collection("users").document(userId)
                        .set(userData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                            val nextActivity = if (isSitter) {
                                SitterSetupActivity::class.java
                            } else {
                                ClientHomeActivity::class.java
                            }
                            startActivity(Intent(this, nextActivity))
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Firestore error: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Auth error: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }

        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        binding.btnPhoneLogin.setOnClickListener {
            startActivity(Intent(this, PhoneRegisterActivity::class.java))
        }
    }

    private fun initGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                auth.signInWithCredential(credential)
                    .addOnSuccessListener { result ->
                        val userId = result.user?.uid ?: return@addOnSuccessListener
                        val isSitter = binding.cbBecomeSitter.isChecked
                        val phone = binding.editTextPhone.text.toString().trim()
                        val dogName = binding.editTextDogName.text.toString().trim()

                        db.collection("users").document(userId).get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val existingSitter = document.getBoolean("isSitter") ?: false
                                    val nextActivity = if (existingSitter) {
                                        SitterHomeActivity::class.java
                                    } else {
                                        ClientHomeActivity::class.java
                                    }
                                    startActivity(Intent(this, nextActivity))
                                    finish()
                                } else {
                                    val fullName = account.displayName ?: ""
                                    val userData = hashMapOf(
                                        "fullName" to fullName,
                                        "email" to (account.email ?: ""),
                                        "phone" to phone,
                                        "isSitter" to isSitter
                                    )
                                    if (!isSitter) {
                                        userData["dogName"] = dogName
                                    }

                                    db.collection("users").document(userId)
                                        .set(userData)
                                        .addOnSuccessListener {
                                            Toast.makeText(this, "Google Sign-In success", Toast.LENGTH_SHORT).show()
                                            val nextActivity = if (isSitter) {
                                                SitterSetupActivity::class.java
                                            } else {
                                                ClientHomeActivity::class.java
                                            }
                                            startActivity(Intent(this, nextActivity))
                                            finish()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(this, "Firestore error: ${it.message}", Toast.LENGTH_LONG).show()
                                        }
                                }
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Google Sign-In failed: ${it.message}", Toast.LENGTH_LONG).show()
                    }

            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
