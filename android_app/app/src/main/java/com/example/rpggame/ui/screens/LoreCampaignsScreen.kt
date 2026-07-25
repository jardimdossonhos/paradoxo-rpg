package com.example.rpggame.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rpggame.data.LoreItem
import com.example.rpggame.data.RpgRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LoreViewModel : ViewModel() {
    val loresState: StateFlow<List<LoreItem>> = RpgRepository.lores.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            RpgRepository.refreshData()
        }
    }

    fun createLore(title: String, summary: String, fullText: String, onResult: (LoreItem?) -> Unit) {
        viewModelScope.launch {
            val lore = RpgRepository.addLore(title, summary, fullText)
            onResult(lore)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoreCampaignsScreen(
    onLoreClick: (String) -> Unit = {},
    showCreateDialog: Boolean = false,
    onDismissCreateDialog: () -> Unit = {},
    viewModel: LoreViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val lores by viewModel.loresState.collectAsState()
    var internalShowDialog by remember { mutableStateOf(false) }

    val isDialogOpen = showCreateDialog || internalShowDialog

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Lores e Campanhas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items = lores, key = { it.id }) { lore ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    onClick = { onLoreClick(lore.id) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(lore.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            lore.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    if (isDialogOpen) {
        var loreTitle by remember { mutableStateOf("") }
        var loreSummary by remember { mutableStateOf("") }
        var loreFullText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                internalShowDialog = false
                onDismissCreateDialog()
            },
            title = { Text("Criar Nova Lore/Campanha") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = loreTitle,
                        onValueChange = { loreTitle = it },
                        label = { Text("Título da Lore") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = loreSummary,
                        onValueChange = { loreSummary = it },
                        label = { Text("Resumo Breve") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = loreFullText,
                        onValueChange = { loreFullText = it },
                        label = { Text("Texto Completo (A IA usará isso)") },
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = loreTitle.isNotBlank() && loreSummary.isNotBlank() && loreFullText.isNotBlank(),
                    onClick = {
                        viewModel.createLore(loreTitle, loreSummary, loreFullText) { newLore ->
                            internalShowDialog = false
                            onDismissCreateDialog()
                            if (newLore != null) {
                                onLoreClick(newLore.id)
                            }
                        }
                    }
                ) {
                    Text("Criar Lore")
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
}
