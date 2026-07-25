package com.example.rpggame.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.rpggame.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleAuthManager(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun signInWithGoogle(): String? {
        val webClientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            Log.w("GoogleAuthManager", "default_web_client_id resource not found", e)
            ""
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                googleIdTokenCredential.idToken
            } else {
                Log.e("GoogleAuthManager", "Received unrecognized credential type")
                null
            }
        } catch (e: GetCredentialException) {
            Log.e("GoogleAuthManager", "GetCredentialException during sign in", e)
            null
        } catch (e: Exception) {
            Log.e("GoogleAuthManager", "Unexpected error during Google Sign-In", e)
            null
        }
    }
}
