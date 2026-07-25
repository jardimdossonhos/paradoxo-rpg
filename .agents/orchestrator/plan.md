# Paradoxo RPG - Project Architectural & Visual Evolution Plan

## Architecture Overview
- **Thin Client Android (`android_app/`)**: Native Kotlin, Jetpack Compose, MVVM architecture, Retrofit for HTTP calls, Credential Manager for Google Sign-In.
- **Python Middleware (`python_middleware/`)**: FastAPI framework (`main.py`), Firebase Admin SDK (Auth & RTDB & Storage), Google Gemini 1.5 Flash API with Pydantic Structured Outputs.
- **Firebase Schema (`firebase_schema/`)**: RTDB JSON rules and mock state structure (`paradoxo-rpg`), isolated per user (`/users/{uid}/...`).

## Milestones Breakdown

| # | Milestone Name | Scope & Deliverables | Dependencies | Status |
|---|----------------|----------------------|--------------|--------|
| M1 | Backend Python & DB (R1) | RTDB user isolation schema (`/users/{uid}/...`), Firebase Admin Auth JWT validation middleware (HTTP 401 on missing/invalid token), secret Lore guidelines in Gemini `system_instruction`, native image generation route via Gemini with Firebase Storage upload fallback. | None | DONE |
| M2 | Android Auth & Nav (R2-1) | Credential Manager API Google Sign-In integration, Retrofit auth interceptor sending Bearer JWT token, Navigation Drawer refactoring (replacing Bottom Navigation) with menu options: "Minhas Salas", "Meus Personagens", "Lores e Campanhas", "Perfil". | M1 | DONE |
| M3 | Android UI & Wizard (R2-2) | Home screen with active rooms list and FAB (+), Character Creation Wizard flow (Generate Art -> Approve -> Fill Sheet -> Save) connected to backend image & character endpoints. | M2 | DONE |
| M4 | E2E Testing & Hardening | Automated verification of Gradle CLI build (`.\gradlew.bat build`), backend FastAPI startup on port 8000, JWT security verification, image generation fallback tests, UI wizard navigation verification. | M1, M2, M3 | DONE |

## Interface Contracts

### Python Middleware API Contracts
1. `GET /health` -> `200 OK`
2. `POST /api/auth/verify` Header: `Authorization: Bearer <JWT>` -> `200 OK { "uid": "..." }` or `401 Unauthorized`.
3. `POST /api/images/generate` Header: `Authorization: Bearer <JWT>`, Body: `{ "prompt": "..." }` -> `200 OK { "image_url": "..." }` (Storage URL or mock URL fallback).
4. `POST /api/characters` Header: `Authorization: Bearer <JWT>`, Body: Character Sheet JSON -> `201 Created`.
5. `GET /api/rooms` Header: `Authorization: Bearer <JWT>` -> `200 OK [ Active Rooms ]`.

## Acceptance Criteria Verification
- [ ] Backend python starts without errors on port 8000.
- [ ] Gemini image generation routes work with Firebase Storage upload / mock URL fallback.
- [ ] Requests with invalid or missing JWT receive status HTTP 401 Unauthorized.
- [ ] `.\gradlew.bat build` compiles Android project without syntax or lint errors.
- [ ] Navigation Drawer opens and displays all required menu options.
- [ ] Character Wizard navigates correctly through Art Generation -> Sheet Filling -> Saving.
