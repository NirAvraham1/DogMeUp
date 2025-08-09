package com.example.dogmeup

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SitterDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sitter_details)

        val ivSitterPhoto = findViewById<ImageView>(R.id.ivSitterPhoto)
        val tvSitterName = findViewById<TextView>(R.id.tvSitterName)
        val tvSitterBio = findViewById<TextView>(R.id.tvSitterBio)
        val tvSitterRate = findViewById<TextView>(R.id.tvSitterRate)
        val tvSitterAvailability = findViewById<TextView>(R.id.tvSitterAvailability)
        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnSelectSitter = findViewById<Button>(R.id.btnSelectSitter)
        val recyclerViewReviews = findViewById<RecyclerView>(R.id.recyclerViewReviews)

        val name = intent.getStringExtra("fullName")
        val bio = intent.getStringExtra("bio")
        val rate = intent.getIntExtra("rate", 0)
        val photoUrl = intent.getStringExtra("photoUrl")
        val availability = intent.getStringExtra("availability")
        val sitterId = intent.getStringExtra("sitterId") ?: ""
        val availabilityId = intent.getStringExtra("availabilityId") ?: ""

        tvSitterName.text = name ?: "Unknown"
        tvSitterBio.text = bio ?: "No bio available"
        tvSitterRate.text = "Rate: ₪$rate/hour"
        tvSitterAvailability.text = "Available: $availability"

        Glide.with(this)
            .load(photoUrl)
            .placeholder(R.drawable.ic_default_profile)
            .into(ivSitterPhoto)

        btnBack.setOnClickListener { finish() }

        btnSelectSitter.setOnClickListener {
            val intent = Intent(this, PaymentActivity::class.java).apply {
                putExtra("sitterId", sitterId)
                putExtra("availabilityId", availabilityId)
                putExtra("fullName", name)
                putExtra("rate", rate)
                putExtra("photoUrl", photoUrl)
                putExtra("availability", availability)
            }
            startActivity(intent)
        }

        recyclerViewReviews.layoutManager = LinearLayoutManager(this)


        FirebaseFirestore.getInstance().collection("reviews")
            .whereEqualTo("sitterId", sitterId)
            .whereEqualTo("reviewerRole", "client")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val reviews = result.documents.mapNotNull { it.toObject(Review::class.java) }
                recyclerViewReviews.adapter = ReviewsAdapter(reviews)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load reviews", Toast.LENGTH_SHORT).show()
            }
    }
}
