package com.example.dogmeup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

class ReviewsAdapter(private val reviews: List<Review>) :
    RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder>() {

    class ReviewViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRating: TextView = view.findViewById(R.id.tvRating)
        val tvComment: TextView = view.findViewById(R.id.tvComment)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]
        holder.tvRating.text = "⭐".repeat(review.rating)
        holder.tvComment.text = review.comment

        // המרת Timestamp לפורמט תאריך קריא
        val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            .format(review.timestamp.toDate())

        holder.tvTimestamp.text = formattedDate
    }

    override fun getItemCount() = reviews.size
}
