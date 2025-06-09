package com.example.entityadmin.model

// For creating/updating a subscriber
data class SubscriberRequest(
    val name: String,
    val email: String,
    val nfcCardUid: String?
)
