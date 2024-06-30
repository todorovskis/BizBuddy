package com.example.meetbylocationapp

data class User(
    val name: String = "",
    val email: String = "",
    val title: String = "",
    val age: Int = 0,
    val country: String = "",
    val upForADrink: Boolean = false,
    val profileImageUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val online: Boolean = false,
    var likedUsers: ArrayList<String> = ArrayList(),
    var matchedUsers: ArrayList<String> = ArrayList(),
    val linkedinUrl: String = ""
)

