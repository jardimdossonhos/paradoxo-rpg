package com.example.rpggame

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.rpggame.data.auth.AuthRepository
import com.example.rpggame.data.auth.AuthRepositoryImpl
import com.example.rpggame.data.auth.GoogleAuthManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val authRepository: AuthRepository by lazy { AuthRepositoryImpl() }
    private val googleAuthManager: GoogleAuthManager by lazy { GoogleAuthManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val apiService = com.example.rpggame.data.network.ApiClient.createService(authRepository)
        com.example.rpggame.data.RpgRepository.initialize(apiService)
        
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(
                        authRepository = authRepository,
                        onSignInClick = { onSuccess, onError -> performGoogleSignIn(onSuccess, onError) },
                        onSignOutClick = { authRepository.signOut() }
                    )
                }
            }
        }
    }

    private fun performGoogleSignIn(onSuccess: () -> Unit, onError: (String) -> Unit) {
        lifecycleScope.launch {
            try {
                val googleIdToken = googleAuthManager.signInWithGoogle()
                if (googleIdToken != null) {
                    val result = authRepository.authenticateWithFirebase(googleIdToken)
                    result.onSuccess { token ->
                        Log.d("MainActivity", "Successfully authenticated with Firebase JWT token: ${token.take(15)}...")
                        onSuccess()
                    }.onFailure { error ->
                        Log.e("MainActivity", "Firebase authentication failed", error)
                        onError(error.localizedMessage ?: "Erro de autenticação no Firebase")
                    }
                } else {
                    Log.w("MainActivity", "Google Sign-In returned null token")
                    onError("Cancelado pelo usuário ou erro no Google Sign-In.")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Sign-in error", e)
                onError(e.localizedMessage ?: "Erro ao iniciar o Google Sign-In")
            }
        }
    }
}
