package com.example.entityadmin.model

data class Subscriber(
    val id: String, // Assuming String ID, adjust if Long or other type from backend
    val name: String,
    val email: String,
    val nfcCardUid: String? // Nullable if not always present
)
