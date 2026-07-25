# Milestone 2: Android Auth & Navigation Architecture Analysis Report

## Executive Summary
This report presents a read-only architectural investigation and technical design blueprint for **Milestone 2 (Android Auth & Navigation Architecture)** of the Paradoxo RPG Android thin client.

The current baseline application features a minimal Navigation 3 runtime setup with a temporary monolithic `MainActivity.kt` containing an inline Retrofit setup targeting `http://127.0.0.1:8000/`. To achieve production readiness, this analysis details:
1. Complete inventory and structural assessment of `android_app/`.
2. Integration blueprint for Google Credential Manager API (`androidx.credentials`) and Firebase Auth (`FirebaseAuth`).
3. Modularized network architecture featuring an OkHttp `AuthInterceptor` for automatic JWT Bearer token injection.
4. Comprehensive design blueprint for replacing single-screen navigation with a Jetpack Compose `ModalNavigationDrawer` containing the required sections ("Minhas Salas", "Meus Personagens", "Lores e Campanhas", "Perfil").
5. Gradle configuration assessment and compatibility analysis.

---

## 1. Inventory & Assessment of `android_app/`

### File Tree & Current Purpose
```
android_app/
├── build.gradle.kts                 # Root project plugins (AGP, Compose Compiler, Kotlin Serialization, Google Services)
├── settings.gradle.kts              # Plugin repositories & dependency resolution management
├── gradle.properties                # JVM args, AndroidX flags, Kotlin code style
├── gradle/libs.versions.toml        # Central version catalog
└── app/
    ├── build.gradle.kts             # App module build configuration & dependencies
    ├── google-services.json         # Firebase client configuration
    └── src/main/
        ├── AndroidManifest.xml      # App manifest (INTERNET permission, cleartext traffic enabled)
        └── java/com/example/rpggame/
            ├── MainActivity.kt      # ComponentActivity entry point + temporary inline ViewModel & Retrofit setup
            ├── Navigation.kt        # Top-level NavDisplay & NavBackStack baseline
            ├── NavigationKeys.kt    # Navigation 3 serializable NavKey destinations
            ├── data/
            │   └── DataRepository.kt# Sample repository flow interface & implementation
            ├── theme/               # Material 3 color palette & typography
            └── ui/main/
                ├── MainScreen.kt    # Greeting Composable & UI state handling
                └── MainScreenViewModel.kt # Sample UI State ViewModel
```

### Key Findings & Deficiencies
* **Monolithic Setup in `MainActivity.kt`**: Lines 25–195 of `MainActivity.kt` harbor a hardcoded `RpgViewModel`, `RpgApiService` interface, and `RpgGameScreen` UI composable. This temporary code directly builds an `OkHttpClient` and `Retrofit` instance inside the ViewModel constructor rather than using clean repository abstractions.
* **Navigation 3 Baseline**: The project already incorporates AndroidX Navigation 3 (`androidx.navigation3:navigation3-ui:1.0.1`, `navigation3-runtime:1.0.1`), but currently only defines a single root route (`Main`).
* **Missing Dependencies**: While Firebase Realtime Database (`20.3.0`) and Retrofit (`2.9.0`) are declared in `app/build.gradle.kts`, dependencies for **Firebase Auth**, **Google Credential Manager**, **Google ID**, and **OkHttp Logging Interceptor** are missing from `libs.versions.toml` and `app/build.gradle.kts`.

---

## 2. Credential Manager API & Firebase Auth Integration Blueprint

### Overview
Modern Android standards (Android 14+ / API level 34+) require using `androidx.credentials.CredentialManager` alongside `com.google.android.libraries.identity.googleid.GetGoogleIdOption` to request Google ID tokens. This replaces deprecated `GoogleSignInClient` flows.

### Dependency Extensions Required
In `gradle/libs.versions.toml`:
```toml
[versions]
firebaseAuth = "22.3.1"
credentials = "1.3.0"
googleId = "1.1.1"
okhttp = "4.12.0"

[libraries]
firebase-auth-ktx = { module = "com.google.firebase:firebase-auth-ktx", version.ref = "firebaseAuth" }
androidx-credentials = { module = "androidx.credentials:credentials", version.ref = "credentials" }
androidx-credentials-play-services = { module = "androidx.credentials:credentials-play-services-auth", version.ref = "credentials" }
google-id = { module = "com.google.android.libraries.identity.googleid:googleid", version.ref = "googleId" }
okhttp-logging-interceptor = { module = "com.squareup.okhttp3:logging-interceptor", version.ref = "okhttp" }
```

In `app/build.gradle.kts`:
```kotlin
dependencies {
    // Firebase Auth
    implementation(libs.firebase.auth.ktx)
    // Credential Manager & Google ID
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.google.id)
    // OkHttp Logging
    implementation(libs.okhttp.logging.interceptor)
}
```

### Authentication Flow Architecture
1. **Google ID Token Request (`GoogleAuthManager.kt`)**:
   Constructs a `GetCredentialRequest` with `GetGoogleIdOption` specifying the Web Client ID (extracted from `google-services.json` `client_type: 3`).
   ```kotlin
   class GoogleAuthManager(private val context: Context) {
       private val credentialManager = CredentialManager.create(context)

       suspend fun signInWithGoogle(): String? {
           val googleIdOption = GetGoogleIdOption.Builder()
               .setFilterByAuthorizedAccounts(false)
               .setServerClientId(context.getString(R.string.default_web_client_id))
               .setAutoSelectEnabled(true)
               .build()

           val request = GetCredentialRequest.Builder()
               .addCredentialOption(googleIdOption)
               .build()

           val result = credentialManager.getCredential(context, request)
           val credential = result.credential

           if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
               val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
               return googleIdTokenCredential.idToken
           }
           return null
       }
   }
   ```

2. **Firebase Credential Exchange (`AuthRepositoryImpl.kt`)**:
   Exchanges the Google ID token for a Firebase user session and retrieves the Firebase JWT Bearer token:
   ```kotlin
   class AuthRepositoryImpl(
       private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
   ) : AuthRepository {

       override suspend fun authenticateWithFirebase(googleIdToken: String): Result<String> {
           return try {
               val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
               val authResult = firebaseAuth.signInWithCredential(credential).await()
               val user = authResult.user ?: throw Exception("User is null")
               val tokenResult = user.getIdToken(true).await()
               val bearerToken = tokenResult.token ?: throw Exception("JWT token is null")
               Result.success(bearerToken)
           } catch (e: Exception) {
               Result.failure(e)
           }
       }

       override suspend fun getValidBearerToken(): String? {
           val currentUser = firebaseAuth.currentUser ?: return null
           return try {
               val tokenResult = currentUser.getIdToken(false).await()
               tokenResult.token
           } catch (e: Exception) {
               null
           }
       }
   }
   ```

---

## 3. Retrofit Network Architecture & `AuthInterceptor` Blueprint

### Problem Statement
The Python FastAPI middleware enforces JWT authentication via HTTP Bearer headers (`Authorization: Bearer <token>`). Outbound requests from Retrofit to endpoints such as `/action`, `/rooms`, `/characters`, etc., must automatically inject this header without manual duplication across ViewModel API calls.

### Proposed Architecture (`data/network/`)
Create a dedicated package `com.example.rpggame.data.network` containing:
1. `AuthInterceptor.kt`
2. `ApiClient.kt`
3. `RpgApiService.kt`

### `AuthInterceptor.kt` Implementation
```kotlin
package com.example.rpggame.data.network

import com.example.rpggame.data.auth.AuthRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val authRepository: AuthRepository
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Fetch valid Firebase JWT token (blocking call within OkHttp execution thread)
        val token = runBlocking { authRepository.getValidBearerToken() }

        val requestBuilder = originalRequest.newBuilder()
        if (!token.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
```

### OkHttpClient & Retrofit Builder (`ApiClient.kt`)
```kotlin
package com.example.rpggame.data.network

import com.example.rpggame.data.auth.AuthRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "http://127.0.0.1:8000/"

    fun createService(authRepository: AuthRepository): RpgApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS) // Preserved for LLM responses
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(authRepository))
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RpgApiService::class.java)
    }
}
```

---

## 4. Jetpack Compose Navigation Architecture Blueprint (`ModalNavigationDrawer`)

### Current Navigation Structure
Currently, `Navigation.kt` wraps a `NavDisplay` backed by `rememberNavBackStack(Main)`. `NavigationKeys.kt` only defines `data object Main : NavKey`.

### Refactoring to `ModalNavigationDrawer`

#### Step 1: Destination Definitions in `NavigationKeys.kt`
Define serializable navigation destinations matching user requirements:
```kotlin
package com.example.rpggame

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Rooms : NavKey         // "Minhas Salas"
@Serializable data object Characters : NavKey    // "Meus Personagens"
@Serializable data object LoreCampaigns : NavKey // "Lores e Campanhas"
@Serializable data object Profile : NavKey       // "Perfil"
@Serializable data class GameSession(val sessionId: String) : NavKey // Active gameplay screen
```

#### Step 2: Drawer Metadata Definition
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

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
```

#### Step 3: Refactored `MainNavigation.kt` Blueprint
```kotlin
package com.example.rpggame

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Rooms)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Determine current top destination for drawer selection state
    val currentEntry = backStack.lastOrNull()

    ModalNavigationDrawer(
        drawerState = drawerState,
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
                TopAppBar(
                    title = {
                        val titleText = when (currentEntry) {
                            Rooms -> "Minhas Salas"
                            Characters -> "Meus Personagens"
                            LoreCampaigns -> "Lores e Campanhas"
                            Profile -> "Perfil"
                            is GameSession -> "Sessão de Jogo"
                            else -> "Paradoxo RPG"
                        }
                        Text(titleText)
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Abrir Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.padding(innerPadding),
                onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                entryProvider = entryProvider {
                    entry<Rooms> { RoomsScreen(onJoinSession = { id -> backStack.add(GameSession(id)) }) }
                    entry<Characters> { CharactersScreen() }
                    entry<LoreCampaigns> { LoreCampaignsScreen() }
                    entry<Profile> { ProfileScreen() }
                    entry<GameSession> { key -> GameSessionScreen(sessionId = key.sessionId) }
                }
            )
        }
    }
}
```

---

## 5. Gradle Build Configuration & Verification

### Status of Gradle Executable Verification
* Command executed: `.\gradlew.bat assembleDebug` in `android_app/`.
* Build Result: `BUILD SUCCESSFUL in 1m 22s` (37 actionable tasks: 10 executed, 27 up-to-date).
* Target Compatibility: SDK 36 compile/target, Java 17 toolchain, Kotlin 2.3.20.

### Prerequisites for Build Execution
Before running builds after implementing authentication dependencies, verify that:
1. `google-services.json` is present in `android_app/app/` (verified present).
2. Local Java JDK 17 environment variable (`JAVA_HOME`) points to a valid JDK 17 installation.

---

## 6. Recommendations for Implementation Phase (Milestone 2 Next Steps)
1. **Dependency Update**: Add `firebase-auth-ktx`, `androidx.credentials`, `google-id`, and `okhttp-logging-interceptor` to `libs.versions.toml` and `app/build.gradle.kts`.
2. **Refactor `MainActivity.kt`**: Extract inline `RpgViewModel` and `RpgGameScreen` into `ui/game/` and clean up network instantiations.
3. **Module Architecture**: Create `data/auth/`, `data/network/`, and separate screen packages under `ui/` (`ui/rooms/`, `ui/characters/`, `ui/lore/`, `ui/profile/`).
