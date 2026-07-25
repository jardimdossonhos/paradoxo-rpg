package com.example.rpggame.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onSignInClick: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Nomi RPG",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Bem-vindo(a) ao seu motor de RPG com IA.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(64.dp))

            if (isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Autenticando...", style = MaterialTheme.typography.bodyMedium)
            } else {
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        onSignInClick(
                            { // onSuccess
                                isLoading = false
                                onLoginSuccess()
                            },
                            { error -> // onError
                                isLoading = false
                                errorMessage = error
                                scope.launch {
                                    snackbarHostState.showSnackbar(error)
                                }
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Entrar com o Google")
                }
            }
        }
    }
}
