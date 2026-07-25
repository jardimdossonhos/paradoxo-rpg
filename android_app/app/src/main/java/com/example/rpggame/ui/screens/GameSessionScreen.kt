package com.example.rpggame.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rpggame.data.auth.AuthRepository
import com.example.rpggame.data.auth.AuthRepositoryImpl
import com.example.rpggame.data.network.ApiClient
import com.example.rpggame.data.network.PlayerAction
import com.example.rpggame.data.network.RpgApiService
import kotlinx.coroutines.launch

class RpgViewModel(
    private val authRepository: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {
    private val _chatHistory = mutableStateListOf<String>()
    val chatHistory: List<String> = _chatHistory
    
    private val _currentLocation = mutableStateOf("Taverna do Javali")
    val currentLocation: State<String> = _currentLocation

    private val api: RpgApiService = ApiClient.createService(authRepository)

    init {
        _chatHistory.add("[Sistema]: Vocês entram na taverna. Garrick sorri para Aragorn.")
    }

    fun sendAction(actionText: String) {
        if (actionText.isBlank()) return
        
        _chatHistory.add("[Você]: $actionText")
        
        viewModelScope.launch {
            try {
                val response = api.sendAction(
                    PlayerAction(
                        session_id = "mock_session_001",
                        player_id = authRepository.getCurrentUserId() ?: "player1",
                        action = actionText
                    )
                )
                _chatHistory.add("[Mestre]: ${response.message}")
            } catch (e: Exception) {
                android.util.Log.e("RPGGame", "Falha de conexão", e)
                _chatHistory.add("[Erro]: ${e.message}")
            }
        }
    }
}

@Composable
fun GameSessionScreen(
    sessionId: String,
    viewModel: RpgViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    
    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Paradoxo RPG (Sessão: $sessionId)", 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 18.sp
                )
                Text(
                    text = "Local: ${viewModel.currentLocation.value}", 
                    fontSize = 14.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            reverseLayout = false
        ) {
            items(viewModel.chatHistory) { message ->
                val isSystem = message.startsWith("[Sistema]")
                val isDM = message.startsWith("[Mestre]")
                val color = when {
                    isSystem -> Color(0xFF4CAF50)
                    isDM -> Color(0xFFFF9800)
                    else -> Color.White
                }
                
                Text(
                    text = message,
                    color = color,
                    modifier = Modifier.padding(vertical = 4.dp),
                    fontSize = 16.sp
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("O que você faz?") },
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    viewModel.sendAction(inputText)
                    inputText = ""
                },
                modifier = Modifier.height(56.dp)
            ) {
                Text("Enviar")
            }
        }
    }
}
