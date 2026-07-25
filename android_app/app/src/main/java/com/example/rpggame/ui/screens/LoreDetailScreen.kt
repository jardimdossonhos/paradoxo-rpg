package com.example.rpggame.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rpggame.data.RpgRepository
import kotlinx.coroutines.launch

@Composable
fun LoreDetailScreen(
    loreId: String,
    onStartRoom: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lore = RpgRepository.lores.value.find { it.id == loreId }

    if (lore == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("Lore não encontrada.")
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = lore.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = lore.fullText,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))

        val coroutineScope = rememberCoroutineScope()
        var isCreatingRoom by remember { mutableStateOf(false) }

        Button(
            enabled = !isCreatingRoom,
            onClick = {
                isCreatingRoom = true
                coroutineScope.launch {
                    val newRoom = RpgRepository.addRoom(name = lore.title, description = lore.summary)
                    if (newRoom != null) {
                        onStartRoom(newRoom.id)
                    }
                    isCreatingRoom = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (isCreatingRoom) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text("Iniciar Campanha")
            }
        }
    }
}
