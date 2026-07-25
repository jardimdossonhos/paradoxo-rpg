## 2026-06-28T22:09:24Z
You are worker_m1_1 operating in c:\Users\Dell\.gemini\antigravity\scratch\rpg_game\.agents\worker_m1_1.
Your task is to implement Milestone 1 (Backend Python & DB).

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A Forensic Auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Inputs:
Read the exploration findings and architectural blueprints in:
- `c:\Users\Dell\.gemini\antigravity\scratch\rpg_game\.agents\explorer_m1_1\handoff.md`
- `c:\Users\Dell\.gemini\antigravity\scratch\rpg_game\.agents\explorer_m1_1\analysis.md`

Tasks to Implement:
1. **Firebase RTDB User Isolation**:
   Refactor `firebase_schema/rules.json` and `mock_state.json` so sessions and user data are isolated under `/users/{uid}/...`. Update `python_middleware/main.py` references to access user-scoped RTDB paths `/users/{uid}/sessions/{session_id}`.
2. **Firebase Admin Auth JWT Verification**:
   In `python_middleware/main.py`, implement FastAPI `HTTPBearer` security dependency using `firebase_admin.auth.verify_id_token`. Ensure all protected routes verify the Bearer token and raise `HTTPException(status_code=401, detail="...")` when the token is missing, invalid, or expired.
3. **Secret Lore System Instruction**:
   Inject secret Lore guidelines into Gemini's `system_instruction` in `main.py` so that game mechanics and hidden lore are enforced by the AI GM without leaking internal instructions into narrative text.
4. **Native Image Generation & Storage Upload**:
   Implement route for native image generation (via Gemini/Imagen API) in `main.py` with direct upload to Firebase Storage bucket (`storage.bucket()`). Implement robust fallback logic returning a mock URL if API quota is exceeded or fails, ensuring full Storage integration code is implemented. Update `requirements.txt` and `.env` if necessary.
5. **Verification**:
   Test Python backend startup (`python -m py_compile main.py` or running uvicorn test) to verify syntax and imports. Document build and test results.

Deliverables:
- Log implementation details in `c:\Users\Dell\.gemini\antigravity\scratch\rpg_game\.agents\worker_m1_1\changes.md`.
- Provide complete verification evidence and handoff report in `c:\Users\Dell\.gemini\antigravity\scratch\rpg_game\.agents\worker_m1_1\handoff.md`. Send a message to orchestrator upon completion.
