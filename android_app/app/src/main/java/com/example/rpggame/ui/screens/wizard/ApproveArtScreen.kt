package com.example.rpggame.ui.screens.wizard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import android.R as AndroidR

@Composable
fun ApproveArtScreen(
    prompt: String,
    imageUrl: String,
    onApprove: (approvedImageUrl: String) -> Unit,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Passo 2: Aprovar Avatar",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Confira a arte gerada pelo Mestre IA para o seu personagem.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .size(260.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Avatar Gerado",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = painterResource(id = AndroidR.drawable.ic_menu_gallery),
                fallback = painterResource(id = AndroidR.drawable.ic_menu_gallery)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Prompt Utilizado:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = prompt.ifBlank { "Nenhum prompt detalhado." },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRegenerate,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text("Regerar")
            }
            Button(
                onClick = { onApprove(imageUrl) },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text("Aprovar Avatar")
            }
        }
    }
}
