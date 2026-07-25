package com.example.rpggame.ui.screens.wizard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.rpggame.data.CharacterItem
import com.example.rpggame.data.RpgRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterSheetScreen(
    approvedImageUrl: String,
    onCharacterSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    var characterName by remember { mutableStateOf("") }
    var expandedClassDropdown by remember { mutableStateOf(false) }
    val classes = listOf("Guerreiro", "Mago", "Ladino", "Clérigo", "Patrulheiro", "Bárbaro")
    var selectedClass by remember { mutableStateOf(classes.first()) }

    var str by remember { mutableIntStateOf(10) }
    var dex by remember { mutableIntStateOf(10) }
    var con by remember { mutableIntStateOf(10) }
    var int by remember { mutableIntStateOf(10) }
    var wis by remember { mutableIntStateOf(10) }
    var cha by remember { mutableIntStateOf(10) }

    var backstoryText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Passo 3: Ficha do Personagem",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Defina os detalhes, atributos e história de seu aventureiro.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (approvedImageUrl.isNotBlank()) {
            AsyncImage(
                model = approvedImageUrl,
                contentDescription = "Avatar Aprovado",
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = characterName,
            onValueChange = { characterName = it },
            label = { Text("Nome do Personagem") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        ExposedDropdownMenuBox(
            expanded = expandedClassDropdown,
            onExpandedChange = { expandedClassDropdown = !expandedClassDropdown },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedClass,
                onValueChange = {},
                readOnly = true,
                label = { Text("Classe") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedClassDropdown) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expandedClassDropdown,
                onDismissRequest = { expandedClassDropdown = false }
            ) {
                classes.forEach { clazz ->
                    DropdownMenuItem(
                        text = { Text(clazz) },
                        onClick = {
                            selectedClass = clazz
                            expandedClassDropdown = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Atributos Principais",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))

        AttributeRow("Força (FOR)", str) { str = (str + it).coerceIn(8, 18) }
        AttributeRow("Destreza (DES)", dex) { dex = (dex + it).coerceIn(8, 18) }
        AttributeRow("Constituição (CON)", con) { con = (con + it).coerceIn(8, 18) }
        AttributeRow("Inteligência (INT)", int) { int = (int + it).coerceIn(8, 18) }
        AttributeRow("Sabedoria (SAB)", wis) { wis = (wis + it).coerceIn(8, 18) }
        AttributeRow("Carisma (CAR)", cha) { cha = (cha + it).coerceIn(8, 18) }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = backstoryText,
            onValueChange = { backstoryText = it },
            label = { Text("História de Origem / Backstory") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(24.dp))

        val coroutineScope = rememberCoroutineScope()

        Button(
            enabled = characterName.isNotBlank(),
            onClick = {
                val attrs = mapOf(
                    "Força" to str,
                    "Destreza" to dex,
                    "Constituição" to con,
                    "Inteligência" to int,
                    "Sabedoria" to wis,
                    "Carisma" to cha
                )
                coroutineScope.launch {
                    RpgRepository.addCharacter(
                        name = characterName,
                        clazz = selectedClass,
                        backstory = backstoryText,
                        avatarUrl = approvedImageUrl,
                        attributes = attrs
                    )
                    onCharacterSaved()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Salvar Personagem")
        }
    }
}

@Composable
fun AttributeRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { onChange(-1) },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Text("-")
            }
            Text(
                text = "$value",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            OutlinedButton(
                onClick = { onChange(1) },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Text("+")
            }
        }
    }
}
