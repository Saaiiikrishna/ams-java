package com.example.entityadmin.model

data class Attendee(
    val id: String,
    val name: String,
    val email: String,
    val checkInTime: String,
    val status: String = "Present"
)
