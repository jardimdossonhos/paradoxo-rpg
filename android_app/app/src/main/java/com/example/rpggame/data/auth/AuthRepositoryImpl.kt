package com.example.rpggame.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override suspend fun authenticateWithFirebase(googleIdToken: String): Result<String> {
        return try {
            val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Firebase user is null after authentication")
            val tokenResult = user.getIdToken(true).await()
            val bearerToken = tokenResult.token ?: throw Exception("JWT token is null")
            Result.success(bearerToken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getValidBearerToken(): String? {
        val currentUser = firebaseAuth.currentUser ?: return null
        return try {
            val tokenResult = currentUser.getIdToken(false).await()
            tokenResult.token
        } catch (e: Exception) {
            null
        }
    }

    override fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }
}
