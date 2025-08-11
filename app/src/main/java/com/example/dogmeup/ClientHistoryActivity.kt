package com.example.dogmeup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ClientHistoryActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_history)

        val rv = findViewById<RecyclerView>(R.id.recyclerViewClientHistory)
        val tvEmpty = findViewById<TextView>(R.id.tvEmptyMessage)

        rv.layoutManager = LinearLayoutManager(this)

        val uid = auth.currentUser?.uid ?: return

        db.collection("reviews")
            .whereEqualTo("clientId", uid)
            .whereEqualTo("reviewerRole", "sitter")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                Log.d("ClientHistory", "reviews found=${snap.size()}")
                val list = snap.documents.mapNotNull { it.toObject(Review::class.java) }
                if (list.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    rv.adapter = ReviewsAdapter(emptyList())
                } else {
                    tvEmpty.visibility = View.GONE
                    rv.adapter = ReviewsAdapter(list)
                }
            }
            .addOnFailureListener { e ->
                Log.e("ClientHistory", "Failed to load history", e)
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()

                rv.adapter = ReviewsAdapter(emptyList())
                findViewById<TextView>(R.id.tvEmptyMessage).visibility = View.VISIBLE
            }
    }
}
