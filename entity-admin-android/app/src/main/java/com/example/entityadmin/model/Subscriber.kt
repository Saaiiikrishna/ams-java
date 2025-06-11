package com.example.entityadmin.model

data class Subscriber(
    val id: String, // Assuming String ID, adjust if Long or other type from backend
    val name: String?, // Nullable to handle backend inconsistencies
    val email: String?, // Nullable to handle backend inconsistencies
    val nfcCardUid: String? // Nullable if not always present
)
