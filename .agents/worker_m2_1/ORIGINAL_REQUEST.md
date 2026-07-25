## 2026-06-28T22:15:14Z
You are worker_m2_1 operating in c:\Users\Dell\.gemini\antigravity\scratch\rpg_game\.agents\worker_m2_1.
Your task is to implement Milestone 2 (Android Auth & Navigation Architecture).

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A Forensic Auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Inputs:
Read the exploration findings and architectural blueprints in:
- `c:\Users\Dell\.gemini\antigravity\scratch\rpg_game\.agents\explorer_m2_1\handoff.md`
- `c:\Users\Dell\.gemini\antigravity\scratch\rpg_game\.agents\explorer_m2_1\analysis.md`

Tasks to Implement:
1. **Dependencies**: Update `android_app/gradle/libs.versions.toml` and `android_app/app/build.gradle.kts` to add Firebase Auth (`firebase-auth-ktx`), Credential Manager (`androidx.credentials`), Google ID (`googleid`), and OkHttp logging interceptor.
2. **Authentication Architecture**: Implement `GoogleAuthManager` and `AuthRepository` in Kotlin using Credential Manager API (`androidx.credentials`) to perform Google Sign-In and fetch valid JWT tokens.
3. **Retrofit AuthInterceptor**: Create an `AuthInterceptor` in the OkHttp client setup that dynamically attaches `Authorization: Bearer <JWT>` to outbound HTTP requests targeting the backend middleware.
4. **Navigation Drawer Refactoring**: Refactor `Navigation.kt` and navigation keys to implement a Jetpack Compose `ModalNavigationDrawer` (or `NavigationDrawer`) replacing the bottom navigation layout. Ensure the menu drawer contains options for: "Minhas Salas", "Meus Personagens", "Lores e Campanhas", and "Perfil".
5. **Compilation Verification**: Verify that the Android application builds without syntax or lint errors by running `.\gradlew.bat assembleDebug` or `.\gradlew.bat build` inside `android_app/`.

Deliverables:
- Log implementation details in `c:\Users\Dell\.gemini\antigravity\scratch\rpg_game\.agents\worker_m2_1\changes.md`.
- Provide complete verification evidence and handoff report in `c:\Users\Dell\.gemini\antigravity\scratch\rpg_game\.agents\worker_m2_1\handoff.md`. Send a message to orchestrator upon completion.
