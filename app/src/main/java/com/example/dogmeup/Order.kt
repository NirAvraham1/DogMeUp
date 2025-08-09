package com.example.dogmeup

data class Order(
    val clientId: String = "",
    val sitterId: String = "",
    val sitterName: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val rate: Int = 0,
    val photoUrl: String = "",
    var status: String = "upcoming",
    var reviewSubmittedClient: Boolean = false,
    var clientReviewId: String? = null
)

