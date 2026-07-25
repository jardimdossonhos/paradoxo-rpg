# Implementation Details - Milestone 2 (Android Auth & Navigation Architecture)

## 1. Dependency Management Updates
* **`android_app/gradle/libs.versions.toml`**:
  * Added version catalog entries for `firebaseAuth` (`22.3.1`), `credentials` (`1.3.0`), `googleId` (`1.1.1`), and `okhttp` (`4.12.0`).
  * Declared libraries: `firebase-auth-ktx`, `androidx-credentials`, `androidx-credentials-play-services`, `googleid`, `kotlinx-coroutines-play-services`, and `okhttp-logging-interceptor`.
* **`android_app/app/build.gradle.kts`**:
  * Configured implementation dependencies for Firebase Auth, Credential Manager, Google ID, OkHttp Logging Interceptor, Coroutines Play Services, and Compose Material Icons Extended.

## 2. Authentication Architecture Integration
* **`GoogleAuthManager.kt` (`data/auth/`)**:
  * Implemented Credential Manager API (`androidx.credentials.CredentialManager`) with `GetGoogleIdOption` requesting the server client ID (`R.string.default_web_client_id`).
* **`AuthRepository.kt` & `AuthRepositoryImpl.kt` (`data/auth/`)**:
  * Implemented Firebase Auth token exchange using `GoogleAuthProvider.getCredential(...)` and `.signInWithCredential(...)`.
  * Exposed `getValidBearerToken()` using `getIdToken(false)` to fetch active Firebase JWT Bearer tokens asynchronously.
  * Implemented `getCurrentUserId()` and `signOut()`.

## 3. Retrofit Network Architecture & Interceptor
* **`AuthInterceptor.kt` (`data/network/`)**:
  * Created OkHttp `Interceptor` implementation that calls `authRepository.getValidBearerToken()` and injects `Authorization: Bearer <JWT>` header into outbound requests.
* **`ApiClient.kt` & `RpgApiService.kt` (`data/network/`)**:
  * Centralized Retrofit service builder targeting `http://127.0.0.1:8000/` with 60s connect/read/write timeouts, attaching `AuthInterceptor` and `HttpLoggingInterceptor`.

## 4. Jetpack Compose Navigation Drawer Refactoring
* **`NavigationKeys.kt`**:
  * Defined serializable navigation destinations matching specifications: `Rooms`, `Characters`, `LoreCampaigns`, `Profile`, and `GameSession(val sessionId: String)`.
* **`Navigation.kt`**:
  * Implemented `ModalNavigationDrawer` wrapping `Scaffold` and `NavDisplay`.
  * Configured `ModalDrawerSheet` with navigation options for "Minhas Salas", "Meus Personagens", "Lores e Campanhas", and "Perfil".
* **Screen Composables (`ui/screens/`)**:
  * Created `RoomsScreen`, `CharactersScreen`, `LoreCampaignsScreen`, `ProfileScreen`, and `GameSessionScreen` with genuine UI rendering and navigation actions.
  * Resolved Kotlin compiler type inference and missing dependency references (`Coil` / `items` signature) in `CharactersScreen.kt` and `RoomsScreen.kt`.

* **`MainActivity.kt`**:
  * Wired `AuthRepositoryImpl` and `GoogleAuthManager` into `MainNavigation`, establishing the clean thin-client architecture.
