package com.example.rpggame.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rpggame.data.RoomItem
import com.example.rpggame.data.RpgRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.example.rpggame.data.auth.AuthRepository
import com.example.rpggame.data.auth.AuthRepositoryImpl

class RoomsViewModel(
    private val authRepository: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {
    val currentUid = authRepository.getCurrentUserId()

    val roomsState: StateFlow<List<RoomItem>> = RpgRepository.rooms.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            RpgRepository.refreshData()
        }
    }

    fun createRoom(name: String, description: String, onResult: (RoomItem?) -> Unit) {
        viewModelScope.launch {
            val room = RpgRepository.addRoom(name, description)
            onResult(room)
        }
    }

    fun deleteRoom(roomId: String) {
        viewModelScope.launch {
            RpgRepository.deleteRoom(roomId)
        }
    }
}

@Composable
fun RoomsScreen(
    onJoinSession: (String) -> Unit,
    showCreateDialog: Boolean = false,
    onDismissCreateDialog: () -> Unit = {},
    viewModel: RoomsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val rooms by viewModel.roomsState.collectAsState()
    var internalShowDialog by remember { mutableStateOf(false) }
    var joinCodeText by remember { mutableStateOf("") }
    var showJoinCodeDialog by remember { mutableStateOf(false) }

    val isDialogOpen = showCreateDialog || internalShowDialog

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Salas Ativas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = { showJoinCodeDialog = true }) {
                Text("Entrar com Código")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (rooms.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhuma sala ativa no momento. Crie uma nova sala!", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items = rooms, key = { it.id }) { room ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(room.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = "${room.players}/${room.maxPlayers} Jogadores",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(room.description, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Criador: ${room.hostName}", style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (room.adminUid == viewModel.currentUid && viewModel.currentUid != null) {
                                        OutlinedButton(
                                            onClick = { viewModel.deleteRoom(room.id) },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text("Deletar")
                                        }
                                    }
                                    Button(onClick = { onJoinSession(room.id) }) {
                                        Text("Entrar na Sala")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isDialogOpen) {
        var roomName by remember { mutableStateOf("") }
        var roomDescription by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                internalShowDialog = false
                onDismissCreateDialog()
            },
            title = { Text("Criar Nova Sala") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = roomName,
                        onValueChange = { roomName = it },
                        label = { Text("Nome da Sala") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = roomDescription,
                        onValueChange = { roomDescription = it },
                        label = { Text("Descrição da Campanha") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = roomName.isNotBlank(),
                    onClick = {
                        viewModel.createRoom(roomName, roomDescription) { newRoom ->
                            internalShowDialog = false
                            onDismissCreateDialog()
                            if (newRoom != null) {
                                onJoinSession(newRoom.id)
                            }
                        }
                    }
                ) {
                    Text("Criar e Entrar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    internalShowDialog = false
                    onDismissCreateDialog()
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showJoinCodeDialog) {
        AlertDialog(
            onDismissRequest = { showJoinCodeDialog = false },
            title = { Text("Entrar com Código") },
            text = {
                OutlinedTextField(
                    value = joinCodeText,
                    onValueChange = { joinCodeText = it },
                    label = { Text("Código da Sala (ex: mock_session_001)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    enabled = joinCodeText.isNotBlank(),
                    onClick = {
                        val code = joinCodeText.trim()
                        showJoinCodeDialog = false
                        joinCodeText = ""
                        onJoinSession(code)
                    }
                ) {
                    Text("Entrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinCodeDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
