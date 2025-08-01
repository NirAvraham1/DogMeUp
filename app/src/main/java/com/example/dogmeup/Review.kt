package com.example.dogmeup

import com.google.firebase.Timestamp

data class Review(
    val bookingId: String = "",
    val clientId: String = "",
    val sitterId: String = "",
    val sitterName: String = "",
    val comment: String = "",
    val rating: Int = 0,
    val timestamp: Timestamp = Timestamp.now()
)

