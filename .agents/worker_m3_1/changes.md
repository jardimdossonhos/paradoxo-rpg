# Implementation Changes Log — Milestone 3

## 1. Coil Image Dependency Setup
- **File:** `android_app/gradle/libs.versions.toml`
  - Added `coil = "2.6.0"` under `[versions]`.
  - Added `coil-compose = { module = "io.coil-kt:coil-compose", version.ref = "coil" }` under `[libraries]`.
- **File:** `android_app/app/build.gradle.kts`
  - Added `implementation(libs.coil.compose)` under `dependencies`.

## 2. Retrofit API Service Extension
- **File:** `android_app/app/src/main/java/com/example/rpggame/data/network/RpgApiService.kt`
  - Created Data Transfer Objects: `ImageGenerationRequest(session_id, prompt_description, aspect_ratio)` and `ImageGenerationResponse(status, image_url, is_mock, error)`.
  - Defined endpoint `@POST("/generate-image") suspend fun generateImage(@Body request: ImageGenerationRequest): ImageGenerationResponse`.

## 3. Data Layer & Repository
- **File:** `android_app/app/src/main/java/com/example/rpggame/data/RpgRepository.kt`
  - Created single source of truth managing reactive state for active room listings (`RoomItem`) and created character roster (`CharacterItem`).
  - Added `addRoom()` and `addCharacter()` functions with thread-safe `StateFlow` updates.

## 4. Home Screen Active Rooms & FAB Integration
- **File:** `android_app/app/src/main/java/com/example/rpggame/ui/screens/RoomsScreen.kt`
  - Introduced `RoomsViewModel` to observe active rooms from `RpgRepository`.
  - Refactored UI to render dynamic cards with player counters (`players/maxPlayers`), host information ("GM Gemini"), and join actions.
  - Added modal creation and room join dialogs triggered via Floating Action Button (+) or join code buttons.

## 5. Character Roster & Creation Wizard Flow
- **File:** `android_app/app/src/main/java/com/example/rpggame/NavigationKeys.kt`
  - Added `@Serializable` navigation keys: `CharacterWizardGenerateArt`, `CharacterWizardApproveArt(prompt, tempImageUrl)`, and `CharacterWizardSheet(approvedImageUrl)`.
- **File:** `android_app/app/src/main/java/com/example/rpggame/ui/screens/CharactersScreen.kt`
  - Refactored roster screen with `CharactersViewModel` to display created characters with Coil avatars and "+ Criar Personagem" wizard launch trigger.
- **File:** `android_app/app/src/main/java/com/example/rpggame/ui/screens/wizard/GenerateArtScreen.kt`
  - Implemented Step 1 UI for entering prompt descriptions, invoking `generateImage` via `GenerateArtViewModel`, and rendering loading feedback.
- **File:** `android_app/app/src/main/java/com/example/rpggame/ui/screens/wizard/ApproveArtScreen.kt`
  - Implemented Step 2 UI displaying generated avatar previews via Coil `AsyncImage` with "Approve Avatar" and "Regerar" actions.
- **File:** `android_app/app/src/main/java/com/example/rpggame/ui/screens/wizard/CharacterSheetScreen.kt`
  - Implemented Step 3 UI form for character name, class selection dropdown, 6-point attribute steppers (FOR, DES, CON, INT, SAB, CAR), and backstory text area. Persists character payload to `RpgRepository`.
- **File:** `android_app/app/src/main/java/com/example/rpggame/Navigation.kt`
  - Integrated wizard routes into Navigation 3 `entryProvider`.
  - Configured top bar title and back navigation logic for wizard steps.
  - Configured Scaffold Floating Action Buttons for room creation and character creation.
