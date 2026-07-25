package com.example.rpggame.data.auth

interface AuthRepository {
    suspend fun authenticateWithFirebase(googleIdToken: String): Result<String>
    suspend fun getValidBearerToken(): String?
    fun getCurrentUserId(): String?
    fun signOut()
}
