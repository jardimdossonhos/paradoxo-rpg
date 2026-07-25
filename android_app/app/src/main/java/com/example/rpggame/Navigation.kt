package com.example.rpggame

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.rpggame.data.auth.AuthRepository
import com.example.rpggame.ui.screens.*
import com.example.rpggame.ui.screens.wizard.*
import kotlinx.coroutines.launch

data class DrawerNavigationItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val navKey: NavKey
)

val drawerItems = listOf(
    DrawerNavigationItem("Minhas Salas", Icons.Filled.MeetingRoom, Icons.Outlined.MeetingRoom, Rooms),
    DrawerNavigationItem("Meus Personagens", Icons.Filled.Person, Icons.Outlined.Person, Characters),
    DrawerNavigationItem("Lores e Campanhas", Icons.Filled.Book, Icons.Outlined.Book, LoreCampaigns),
    DrawerNavigationItem("Perfil", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle, Profile)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(
    authRepository: AuthRepository? = null,
    onSignInClick: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _ -> },
    onSignOutClick: () -> Unit = {}
) {
    val initialRoute = if (authRepository?.getCurrentUserId() != null) Rooms else Login
    val backStack = rememberNavBackStack(initialRoute)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val currentEntry = backStack.lastOrNull()
    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var showCreateLoreDialog by remember { mutableStateOf(false) }

    val isWizardOrSessionRoute = currentEntry is GameSession || 
        currentEntry == CharacterWizardGenerateArt || 
        currentEntry is CharacterWizardApproveArt || 
        currentEntry is CharacterWizardSheet

    val isLoginRoute = currentEntry == Login

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isWizardOrSessionRoute && !isLoginRoute,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Paradoxo RPG",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                drawerItems.forEach { item ->
                    val selected = currentEntry == item.navKey
                    NavigationDrawerItem(
                        label = { Text(item.title) },
                        selected = selected,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (!selected) {
                                backStack.clear()
                                backStack.add(item.navKey)
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (!isLoginRoute) {
                    TopAppBar(
                        title = {
                            val titleText = when (currentEntry) {
                                Rooms -> "Minhas Salas"
                                Characters -> "Meus Personagens"
                                LoreCampaigns -> "Lores e Campanhas"
                                Profile -> "Perfil"
                                is GameSession -> "Sessão de Jogo"
                                CharacterWizardGenerateArt -> "Criar Personagem - Arte"
                                is CharacterWizardApproveArt -> "Criar Personagem - Aprovação"
                                is CharacterWizardSheet -> "Criar Personagem - Ficha"
                                else -> "Paradoxo RPG"
                            }
                            Text(titleText)
                        },
                        navigationIcon = {
                            if (isWizardOrSessionRoute || backStack.size > 1) {
                                IconButton(onClick = { backStack.removeLastOrNull() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                                }
                            } else {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Abrir Menu")
                                }
                            }
                        }
                    )
                }
            },
            floatingActionButton = {
                when (currentEntry) {
                    Rooms -> {
                        FloatingActionButton(onClick = { showCreateRoomDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Criar Sala")
                        }
                    }
                    Characters -> {
                        FloatingActionButton(onClick = { backStack.add(CharacterWizardGenerateArt) }) {
                            Icon(Icons.Default.Add, contentDescription = "Criar Personagem")
                        }
                    }
                    LoreCampaigns -> {
                        FloatingActionButton(onClick = { showCreateLoreDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Criar Lore/Campanha")
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.padding(innerPadding),
                onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                entryProvider = entryProvider {
                    entry<Login> {
                        LoginScreen(
                            onSignInClick = onSignInClick,
                            onLoginSuccess = {
                                backStack.clear()
                                backStack.add(Rooms)
                            }
                        )
                    }
                    entry<Rooms> { 
                        RoomsScreen(
                            onJoinSession = { id -> backStack.add(GameSession(id)) },
                            showCreateDialog = showCreateRoomDialog,
                            onDismissCreateDialog = { showCreateRoomDialog = false }
                        ) 
                    }
                    entry<Characters> { 
                        CharactersScreen(
                            onStartWizard = { backStack.add(CharacterWizardGenerateArt) },
                            onCharacterClick = { id -> backStack.add(CharacterDetail(id)) }
                        ) 
                    }
                    entry<CharacterDetail> { key ->
                        CharacterDetailScreen(characterId = key.characterId)
                    }
                    entry<LoreCampaigns> { 
                        LoreCampaignsScreen(
                            onLoreClick = { loreId -> backStack.add(LoreDetail(loreId)) },
                            showCreateDialog = showCreateLoreDialog,
                            onDismissCreateDialog = { showCreateLoreDialog = false }
                        ) 
                    }
                    entry<LoreDetail> { key ->
                        LoreDetailScreen(
                            loreId = key.loreId,
                            onStartRoom = { roomId ->
                                backStack.clear()
                                backStack.add(GameSession(roomId))
                            }
                        )
                    }
                    entry<Profile> {
                        ProfileScreen(
                            currentUserId = authRepository?.getCurrentUserId(),
                            onSignOutClick = {
                                onSignOutClick()
                                backStack.clear()
                                backStack.add(Login)
                            }
                        )
                    }
                    entry<GameSession> { key ->
                        GameSessionScreen(sessionId = key.sessionId)
                    }
                    entry<CharacterWizardGenerateArt> {
                        GenerateArtScreen(
                            onArtGenerated = { prompt, imageUrl ->
                                backStack.add(CharacterWizardApproveArt(prompt, imageUrl))
                            }
                        )
                    }
                    entry<CharacterWizardApproveArt> { key ->
                        ApproveArtScreen(
                            prompt = key.prompt,
                            imageUrl = key.tempImageUrl,
                            onApprove = { approvedUrl ->
                                backStack.add(CharacterWizardSheet(approvedUrl))
                            },
                            onRegenerate = {
                                backStack.removeLastOrNull()
                            }
                        )
                    }
                    entry<CharacterWizardSheet> { key ->
                        CharacterSheetScreen(
                            approvedImageUrl = key.approvedImageUrl,
                            onCharacterSaved = {
                                while (backStack.lastOrNull() != null && backStack.lastOrNull() != Characters && backStack.size > 1) {
                                    backStack.removeLastOrNull()
                                }
                            }
                        )
                    }
                }
            )
        }
    }
}
