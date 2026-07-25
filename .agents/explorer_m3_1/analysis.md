# Detailed Technical Analysis: Milestone 3 (Home Screen Active Rooms & Character Creation Wizard UI)

## Executive Summary
This analysis establishes the technical architecture, UI blueprints, state management strategy, and dependency requirements for Milestone 3 of Paradoxo RPG. The scope encompasses enhancing the Home Screen (`RoomsScreen.kt`) with dynamic active rooms management and a Floating Action Button (FAB), designing a 4-step Character Creation Wizard with Gemini Imagen 3 avatar generation, and specifying the missing image loading dependencies in Android Gradle configuration.

---

## 1. Existing Architecture Evaluation

### 1.1 Navigation Framework (`Navigation.kt` & `NavigationKeys.kt`)
* **Core Technology:** `androidx.navigation3` (Navigation 3 runtime and UI modules).
* **Key Mechanisms:** Screens are represented by type-safe `@Serializable` data objects/classes implementing `NavKey`.
* **Current Navigation Keys (`NavigationKeys.kt`):**
  * `Rooms : NavKey` (Home screen / Active rooms)
  * `Characters : NavKey` (Character roster)
  * `LoreCampaigns : NavKey` (Lore & campaigns)
  * `Profile : NavKey` (User account profile)
  * `GameSession(val sessionId: String) : NavKey` (Gameplay room)
* **Shell Composition (`Navigation.kt`):**
  * Uses `ModalNavigationDrawer` with `ModalDrawerSheet` for main navigation.
  * Scaffold wraps `NavDisplay` and displays a `TopAppBar`.

### 1.2 Network & Data Layer (`ApiClient.kt` & `RpgApiService.kt`)
* **HTTP Client:** Retrofit 2.9.0 + OkHttp 4.12.0 with `HttpLoggingInterceptor` and custom `AuthInterceptor` injecting Firebase ID Token (`Bearer <token>`).
* **Base URL:** `http://127.0.0.1:8000/` (mapped via `adb reverse tcp:8000 tcp:8000` for physical USB device execution).
* **Current Endpoints:** Currently only defines `POST /action`.
* **Backend Capability (`python_middleware/main.py`):** Backend already features `POST /generate-image` handling `ImageGenerationRequest(session_id, prompt_description, aspect_ratio)` and returning `{"status": "success", "image_url": "...", "is_mock": bool}`.

---

## 2. Blueprint: Home Screen (`RoomsScreen.kt` & Active Rooms)

### 2.1 UI Component Architecture
The Home Screen will transition from a static mock screen to a dynamic dashboard with state-aware loading, empty state handling, and interactive room joining/creation.

```
+-------------------------------------------------------+
|  TopAppBar ("Minhas Salas")                           |
+-------------------------------------------------------+
|  LazyColumn                                           |
|  +-------------------------------------------------+  |
|  | [Card] Taverna do Javali                        |  |
|  | Jogadores: 3/4 | Mestre: GM Gemini              |  |
|  | [ Entrar na Sala ]                              |  |
|  +-------------------------------------------------+  |
|  +-------------------------------------------------+  |
|  | [Card] Masmorra de Cristal                      |  |
|  | Jogadores: 2/4 | Mestre: GM Gemini              |  |
|  | [ Entrar na Sala ]                              |  |
|  +-------------------------------------------------+  |
|                                                       |
|                                            ( + FAB )  |
+-------------------------------------------------------+
```

### 2.2 Floating Action Button & Modal Creation Dialog
* **FAB Integration:** Positioned using `Scaffold(floatingActionButton = { ... })` with `ExtendedFloatingActionIcon` or `FloatingActionButton` featuring `Icons.Default.Add`.
* **Click Action:** Triggers an `AlertDialog` / `ModalBottomSheet` allowing users to choose:
  1. **Criar Nova Sala:** Input room name, campaign background, and max players.
  2. **Entrar com Código:** Input room join code for private sessions.

### 2.3 State Management & Data Models
* **Data Model:**
  ```kotlin
  data class RoomItem(
      val id: String,
      val name: String,
      val description: String,
      val currentPlayers: Int,
      val maxPlayers: Int = 4,
      val hostName: String = "GM Gemini",
      val status: String = "ACTIVE"
  )
  ```
* **ViewModel (`RoomsViewModel`):**
  * Exposes `StateFlow<RoomsUiState>` containing `Loading`, `Success(rooms)`, `Error(message)`.
  * Synchronizes with Firebase Realtime Database node `users/{uid}/sessions/` or public rooms registry.

---

## 3. Blueprint: Multi-Step Character Creation Wizard Flow

To provide a seamless step-by-step experience without cluttering global drawer navigation, the wizard will utilize dedicated `NavKey` entries and a shared flow ViewModel (`CharacterWizardViewModel`).

### 3.1 Wizard Navigation Routes (`NavigationKeys.kt` Extensions)
```kotlin
@Serializable data object CharacterWizardGenerateArt : NavKey
@Serializable data class CharacterWizardApproveArt(val prompt: String, val tempImageUrl: String) : NavKey
@Serializable data class CharacterWizardSheet(val approvedImageUrl: String) : NavKey
```

### 3.2 Detailed Step Breakdown

#### Step 1: Art Generation (`GenerateArtScreen.kt`)
* **Objective:** Allow player to describe their character avatar prompt and call Gemini Imagen 3 backend.
* **UI Elements:**
  * Multiline `OutlinedTextField` for prompt (e.g. *"Guerreiro elfo de cabelos prateados, armadura de placas reluzente, estilo arte digital RPG"*).
  * Aspect ratio selector (`1:1` default).
  * Primary Button: `"Gerar Avatar com IA"`.
  * Loading Indicator: `CircularProgressIndicator` with progress feedback string.
* **Network Call:** Calls Retrofit `RpgApiService.generateImage(req)`.
* **Preview Container:** Displays image generated via Coil `AsyncImage`.

#### Step 2: Art Approval (`ApproveArtScreen.kt`)
* **Objective:** Review generated avatar before finalizing stats.
* **UI Elements:**
  * High-resolution avatar view with rounded corner clipping (`Card` / `Surface`).
  * Display of applied prompt tags.
  * Dual Action Buttons:
    * `Regerar` (Navigates back to Step 1 or triggers retry).
    * `Aprovar Avatar` (Navigates to Step 3 with `tempImageUrl`).

#### Step 3: Character Sheet (`CharacterSheetScreen.kt`)
* **Objective:** Complete character stats, backstory, and class setup.
* **UI Elements:**
  * Top header showing approved avatar thumbnail.
  * **Input Fields:**
    * Nome do Personagem (`OutlinedTextField`).
    * Classe (`ExposedDropdownMenuBox` with options: *Guerreiro, Mago, Ladino, Clérigo, Patrulheiro, Bárbaro*).
    * Atributos (Point-buy distribution system for *Força, Destreza, Constituição, Inteligência, Sabedoria, Carisma*).
    * História de Origem / Backstory multiline text field.

#### Step 4: Save Character & Roster Sync
* **Objective:** Persist character payload to backend/Firebase and return to character list.
* **Payload Structure:**
  ```kotlin
  data class CharacterCreatePayload(
      val id: String = UUID.randomUUID().toString(),
      val name: String,
      val clazz: String,
      val level: Int = 1,
      val avatarUrl: String,
      val attributes: Map<String, Int>,
      val backstory: String
  )
  ```
* **Save Execution:** `CharacterWizardViewModel` posts character payload to Firebase RTDB under `users/{uid}/characters/{charId}`.
* **Completion Navigation:** Upon success, backstack pops wizard screens and returns to `Characters` screen with refreshed roster state.

---

## 4. Image Loading Library Audit & Gradle Dependency Setup

### 4.1 Dependency Audit Result
* **Finding:** Neither `coil-compose` nor any other image loading library is currently declared in `android_app/gradle/libs.versions.toml` or `android_app/app/build.gradle.kts`.
* **Requirement:** Adding Coil is strictly necessary for displaying avatars generated via `POST /generate-image` (Firebase Storage URLs / HTTPS images).

### 4.2 Exact Gradle Setup Required

#### 1. `android_app/gradle/libs.versions.toml`
Add under `[versions]`:
```toml
coil = "2.6.0"
```
Add under `[libraries]`:
```toml
coil-compose = { module = "io.coil-kt:coil-compose", version.ref = "coil" }
```

#### 2. `android_app/app/build.gradle.kts`
Add inside `dependencies { ... }`:
```kotlin
// Image Loading
implementation(libs.coil.compose)
```

### 4.3 Retrofit API Interface Extension (`RpgApiService.kt`)
To support image generation from Android, `RpgApiService.kt` requires the following data classes and endpoint method:
```kotlin
data class ImageGenerationRequest(
    val session_id: String,
    val prompt_description: String,
    val aspect_ratio: String = "1:1"
)

data class ImageGenerationResponse(
    val status: String,
    val image_url: String,
    val is_mock: Boolean,
    val error: String? = null
)

// Inside interface RpgApiService:
@POST("/generate-image")
suspend fun generateImage(@Body request: ImageGenerationRequest): ImageGenerationResponse
```

---

## 5. Summary & Handoff Checklist
1. **RoomsScreen:** Add Scaffold with FAB (`+`), dynamic `RoomsViewModel`, and room creation dialog.
2. **Character Wizard Flow:** Implement 3 new navigation keys (`CharacterWizardGenerateArt`, `CharacterWizardApproveArt`, `CharacterWizardSheet`), corresponding screens, and `CharacterWizardViewModel`.
3. **Coil Integration:** Add Coil 2.6.0 dependency to `libs.versions.toml` and `build.gradle.kts`.
4. **Retrofit API:** Add `POST /generate-image` definition to `RpgApiService.kt`.
