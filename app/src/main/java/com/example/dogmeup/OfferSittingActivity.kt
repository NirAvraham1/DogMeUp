package com.example.dogmeup

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class OfferSittingActivity : AppCompatActivity() {

    private lateinit var layoutAvailabilities: LinearLayout
    private lateinit var btnAddAvailability: Button
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offer_sitting)

        layoutAvailabilities = findViewById(R.id.layoutAvailabilities)
        btnAddAvailability = findViewById(R.id.btnAddAvailability)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        btnAddAvailability.setOnClickListener {
            startActivity(Intent(this, AddAvailabilityActivity::class.java))
        }

        loadAvailabilities()
    }

    override fun onResume() {
        super.onResume()
        loadAvailabilities()
    }

    private fun loadAvailabilities() {
        layoutAvailabilities.removeAllViews()
        val userId = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(userId)
            .collection("availability")
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val id = doc.id
                    val date = doc.getString("date") ?: "-"
                    val start = doc.getString("startTime") ?: "-"
                    val end = doc.getString("endTime") ?: "-"

                    val container = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(8, 8, 8, 8)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    val tv = TextView(this).apply {
                        text = "🗓 $date | ⏰ $start - $end"
                        textSize = 16f
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    val btnDelete = ImageButton(this).apply {
                        setImageResource(android.R.drawable.ic_menu_delete)
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        setOnClickListener {
                            showDeleteConfirmation(userId, id)
                        }
                    }

                    container.addView(tv)
                    container.addView(btnDelete)
                    layoutAvailabilities.addView(container)
                }
            }
    }

    private fun showDeleteConfirmation(userId: String, docId: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Availability")
            .setMessage("Are you sure you want to delete this availability?")
            .setPositiveButton("Yes") { _: DialogInterface, _: Int ->
                db.collection("users")
                    .document(userId)
                    .collection("availability")
                    .document(docId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Availability deleted", Toast.LENGTH_SHORT).show()
                        loadAvailabilities()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
