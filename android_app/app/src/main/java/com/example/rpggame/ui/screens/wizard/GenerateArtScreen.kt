package com.example.rpggame.ui.screens.wizard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rpggame.data.auth.AuthRepository
import com.example.rpggame.data.auth.AuthRepositoryImpl
import com.example.rpggame.data.network.ApiClient
import com.example.rpggame.data.network.ImageGenerationRequest
import com.example.rpggame.data.network.RpgApiService
import kotlinx.coroutines.launch

class GenerateArtViewModel(
    private val authRepository: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {
    private val api: RpgApiService = ApiClient.createService(authRepository)

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun generateAvatar(
        prompt: String,
        onSuccess: (String) -> Unit
    ) {
        if (prompt.isBlank()) return
        isLoading = true
        errorMessage = null

        viewModelScope.launch {
            try {
                val response = api.generateImage(
                    ImageGenerationRequest(
                        session_id = "wizard_character_creation",
                        prompt_description = prompt,
                        aspect_ratio = "1:1"
                    )
                )
                isLoading = false
                if (response.status == "success" && response.image_url.isNotBlank()) {
                    onSuccess(response.image_url)
                } else {
                    errorMessage = response.error ?: "Falha ao gerar imagem."
                }
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "Erro de conexão: ${e.localizedMessage}"
            }
        }
    }
}

@Composable
fun GenerateArtScreen(
    onArtGenerated: (prompt: String, imageUrl: String) -> Unit,
    viewModel: GenerateArtViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var promptText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Passo 1: Descreva seu Avatar",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "O Mestre Gemini usará IA para criar um avatar exclusivo para o seu personagem.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = promptText,
            onValueChange = { promptText = it },
            label = { Text("Descrição Visual do Personagem") },
            placeholder = { Text("Ex: Guerreiro elfo de cabelos prateados, armadura de placas reluzente, arte digital RPG") },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            maxLines = 5,
            enabled = !viewModel.isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        viewModel.errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (viewModel.isLoading) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Gerando avatar mágico via Gemini...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Button(
                onClick = {
                    viewModel.generateAvatar(promptText) { imageUrl ->
                        onArtGenerated(promptText, imageUrl)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = promptText.isNotBlank()
            ) {
                Text("Gerar Avatar com IA")
            }
        }
    }
}
