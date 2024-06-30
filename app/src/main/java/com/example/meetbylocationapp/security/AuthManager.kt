package com.example.meetbylocationapp.security

interface AuthManager {
    suspend fun createUser(email: String, password: String)
    suspend fun authenticate(email: String, password: String)
    suspend fun logout()
}