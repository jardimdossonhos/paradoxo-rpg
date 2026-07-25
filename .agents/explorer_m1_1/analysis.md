# Milestone 1: Technical Investigation & Architecture Specification (Backend Python & DB)

**Agent:** `explorer_m1_1`  
**Workspace:** `c:\Users\Dell\.gemini\antigravity\scratch\rpg_game`  
**Date:** June 28, 2026  
**Status:** Completed Investigation (Read-Only)

---

## 1. Executive Summary & Scope Overview

This document presents a comprehensive, read-only technical investigation for **Milestone 1 (Backend Python & DB)** of the Paradoxo RPG project. The backend architecture consists of a thin native client (Android/Compose), a Python FastAPI middleware running locally or hosted on Render/Free Tier, and Firebase services (Realtime Database, Authentication, and Storage), orchestrated via Google Gemini API (`google.genai` SDK) acting as the Game Master (GM).

### Core Investigation Findings:
1. **Middleware Baseline**: Current `main.py` implements basic state retrieval and updating under global `/sessions/{session_id}` using `google.genai` Client (`gemini-2.5-flash`). It lacks auth guardrails, multi-user isolation, secret lore management, and image generation routes.
2. **Database Isolation**: Current structure puts all sessions in a shared root space. Moving to `/users/{uid}/sessions/{session_id}` ensures user data privacy and strict alignment with Firebase Security Rules (`$uid === auth.uid`).
3. **Authentication Gaps**: The API routes currently accept unauthenticated requests. A robust FastAPI dependency utilizing `firebase_admin.auth.verify_id_token` must be introduced to inspect `Authorization: Bearer <token>` headers and enforce 401 Unauthorized responses.
4. **Lore Injection Mechanism**: Lore is currently embedded statically in system prompt strings. Injecting dynamic, secret lore guidelines into `system_instruction` requires a modular lore provider that appends hidden DM rules without exposing them directly to player-facing text.
5. **Image Generation Pipeline**: Native scene generation requires implementing a dedicated endpoint (`POST /generate-image`) using Gemini/Imagen API, with storage pipeline uploading images directly to Firebase Storage (`storage.bucket()`) and returning public URLs, supported by a resilient fallback to a mock URL on quota exhaustion.
6. **Dependency & Runtime Audit**: Dependencies in `requirements.txt` cover basic needs but lack explicit packages for Firebase Storage management (`google-cloud-storage`, `pillow`).

---

## 2. Component 1: Python Middleware Codebase & Structure Analysis

### File Inventory & Inspection
- `python_middleware/main.py`:
  - Uses `fastapi`, `google.genai` (new SDK), `firebase_admin`.
  - Endpoint `/action`: Parses `PlayerAction(session_id, player_id, action)`, reads `sessions/{session_id}`, invokes Gemini with structured JSON requirements (`response_mime_type="application/json"`), updates Firebase RTDB with new turn counter, updated location, player inventory, NPC status/relationships, and chat history.
- `python_middleware/requirements.txt`:
  - Contains: `fastapi==0.110.0`, `uvicorn==0.29.0`, `google-genai>=0.2.0`, `firebase-admin==6.5.0`, `pydantic>=2.12.5`, `python-dotenv==1.0.1`.
- `python_middleware/.env`:
  - Contains credentials: `GEMINI_API_KEY` and `DATABASE_URL` (`https://paradoxo-rpg-default-rtdb.firebaseio.com/`).
- `python_middleware/serviceAccountKey.json`:
  - Firebase Admin SDK credentials present in the directory.
- Test Suite Analysis (`test_firebase.py`, `test_gemini.py`, `test_genai.py`, `test_models.py`):
  - `test_firebase.py`: Verifies basic connection to Firebase RTDB (`sessions/test`).
  - `test_genai.py`: Verifies `google.genai` Client with `gemini-2.5-flash`.
  - `test_gemini.py`: Uses deprecated `google.generativeai` package and `gemini-3.5-flash` model name (should be updated/aligned with `google.genai`).
  - `test_models.py`: Utility to list supported models.

---

## 3. Component 2: Firebase RTDB Schema & User Isolation Design

### Current RTDB Structure (`firebase_schema/mock_state.json`)
Currently, data is structured under a global top-level key:
```json
{
  "sessions": {
    "mock_session_001": {
      "turn": 1,
      "location": "Taverna do Javali",
      "players": { ... },
      "npcs": { ... },
      "chat_history": [ ... ]
    }
  }
}
```

### Isolated Per-User RTDB Schema Proposal (`/users/{uid}/...`)
To ensure total data isolation and security compliance with multi-tenant practices, all user-specific data must be scoped under their Firebase Auth User ID (`uid`):

```json
{
  "users": {
    "USER_AUTH_UID_12345": {
      "profile": {
        "username": "Aragorn_Player",
        "created_at": 1700000000
      },
      "sessions": {
        "session_001": {
          "session_id": "session_001",
          "turn": 1,
          "location": "Taverna do Javali",
          "players": {
            "player1": {
              "name": "Aragorn",
              "inventory": ["Espada Curta", "Poção de Vida"]
            }
          },
          "npcs": {
            "npc_bartender": {
              "name": "Garrick",
              "status": "Vivo",
              "relationships": {
                "player1": "🤝 Aliado"
              },
              "memory": "O bartender acabou de servir uma bebida para Aragorn."
            }
          },
          "chat_history": [
            {
              "role": "system",
              "text": "Vocês entram na taverna do Javali."
            }
          ]
        }
      }
    }
  }
}
```

### Firebase RTDB Security Rules (`firebase_schema/rules.json`)
The revised `rules.json` enforces that authenticated users can only access their own subtree:

```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "auth != null && auth.uid === $uid",
        ".write": "auth != null && auth.uid === $uid"
      }
    }
  }
}
```

### Database Reference Code Changes (`main.py`)
In `main.py`, database access points must be updated to include the authenticated `uid`:
```python
# Before:
session_ref = db.reference(f"sessions/{action_req.session_id}")

# After (using verified JWT uid):
session_ref = db.reference(f"users/{authenticated_uid}/sessions/{action_req.session_id}")
```

---

## 4. Component 3: FastAPI Authentication & Firebase Admin Auth JWT Verification

### Security Requirements
1. Every protected route (`/action`, `/generate-image`) must require an HTTP header: `Authorization: Bearer <Firebase_ID_Token>`.
2. Missing or malformed headers must instantly return HTTP 401 Unauthorized.
3. Tokens must be verified using `firebase_admin.auth.verify_id_token(token)`.
4. Expired or invalid tokens must return HTTP 401 Unauthorized.

### Proposed Implementation (`auth.py` or integrated into `main.py`)

```python
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from firebase_admin import auth

security = HTTPBearer()

async def verify_firebase_token(credentials: HTTPAuthorizationCredentials = Depends(security)) -> dict:
    token = credentials.credentials
    try:
        decoded_token = auth.verify_id_token(token)
        return decoded_token  # Contains 'uid', 'email', etc.
    except auth.ExpiredIdTokenError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authentication token has expired.",
            headers={"WWW-Authenticate": "Bearer"},
        )
    except auth.InvalidIdTokenError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid authentication token.",
            headers={"WWW-Authenticate": "Bearer"},
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=f"Authentication failed: {str(e)}",
            headers={"WWW-Authenticate": "Bearer"},
        )
```

### Route Integration Pattern
```python
@app.post("/action")
async def process_action(
    action_req: PlayerAction,
    user: dict = Depends(verify_firebase_token)
):
    uid = user["uid"]
    session_ref = db.reference(f"users/{uid}/sessions/{action_req.session_id}")
    # Proceed with game logic...
```

---

## 5. Component 4: Gemini Integration & Secret Lore Injection Engine

### Current Gemini Setup Analysis
In `main.py`, Gemini is called via `google.genai` SDK using `gemini-2.5-flash`:
```python
client = genai.Client(api_key=api_key)
response = client.models.generate_content(
    model='gemini-2.5-flash',
    contents=prompt,
    config=types.GenerateContentConfig(
        response_mime_type="application/json",
        system_instruction=system_instruction
    )
)
```

### Lore Guidelines Injection Architecture
Secret Lore guidelines represent hidden game world secrets, NPC motivations, plot twists, and strict GM behavior constraints that must inform Gemini's reasoning without being dumped directly into player output.

#### Structure of `lore_guidelines.json` (or module):
```json
{
  "world_lore": "O reino de Eldoria está sob uma maldição antiga provocada pelo Culto das Sombras. O bartender Garrick é secretamente um informante dos rebeldes.",
  "gm_rules": [
    "Nunca revele os segredos dos NPCs diretamente a menos que o jogador faça uma rolagem/ação de investigação bem-sucedida.",
    "Mantenha o tom sombrio e misterioso.",
    "Atualize a memória dos NPCs com detalhes sobre alianças e traições."
  ]
}
```

#### Code Snippet for Dynamic System Instruction Assembly:
```python
def build_system_instruction(session_lore: str = "") -> str:
    base_instruction = (
        "Você é o Mestre de Jogo (GM) de um RPG de texto medieval fantasioso.\n"
        "Seu papel é processar a ação do jogador com base no estado atual do mundo e nos relacionamentos dos NPCs.\n"
    )
    secret_lore = (
        "\n--- DIRETRIZES DE LORE SECRETO DO MUNDO (SECRET GM LORE) ---\n"
        "ATENÇÃO GM: O texto a seguir contém segredos do mundo e motivações oculta dos NPCs. "
        "Use esta informação APENAS para guiar as reações dos NPCs e os rumos da história. "
        "NÃO revele estes segredos explicitamente ao jogador a menos que a ação dele justifique a descoberta.\n"
        f"{session_lore}\n"
        "-----------------------------------------------------------\n"
    )
    formatting_rules = (
        "Regras de saída:\n"
        "1. Reaja de forma lógica e narrativa à ação do jogador.\n"
        "2. Atualize relacionamentos de NPCs e inventários dos jogadores.\n"
        "3. A resposta DEVE seguir estritamente o formato JSON solicitado.\n"
    )
    return base_instruction + secret_lore + formatting_rules
```

---

## 6. Component 5: Native Image Generation Route & Firebase Storage Pipeline

### Route Design & Specifications
A new endpoint `POST /generate-image` will accept scene descriptions or prompt requests, invoke Gemini/Imagen API to generate visual assets for scenes/NPCs, and upload them to Firebase Storage.

### Data Models
```python
class ImageGenerationRequest(BaseModel):
    session_id: str
    prompt_description: str
    aspect_ratio: Optional[str] = "1:1"
```

### Detailed Implementation Blueprint (with Storage Integration & Fallback)

```python
import io
import uuid
import logging
from firebase_admin import storage

logger = logging.getLogger("uvicorn")

MOCK_IMAGE_URL = "https://storage.googleapis.com/paradoxo-rpg.appspot.com/mock_scene_placeholder.png"

@app.post("/generate-image")
async def generate_scene_image(
    req: ImageGenerationRequest,
    user: dict = Depends(verify_firebase_token)
):
    uid = user["uid"]
    image_bytes = None
    
    # 1. Attempt Native Image Generation via Gemini / Imagen API
    try:
        # Using Google GenAI SDK for Imagen / Image Generation
        result = client.models.generate_images(
            model='imagen-3.0-generate-002',
            prompt=f"Fantasy RPG style digital art, highly detailed: {req.prompt_description}",
            config=types.GenerateImagesConfig(
                number_of_images=1,
                output_mime_type="image/png",
                aspect_ratio=req.aspect_ratio or "1:1"
            )
        )
        if result.generated_images:
            image_bytes = result.generated_images[0].image.image_bytes
    except Exception as e:
        logger.warning(f"Gemini Image Generation failed or quota exceeded: {e}. Falling back to mock image.")

    # 2. Upload to Firebase Storage if image generation succeeded
    if image_bytes:
        try:
            bucket = storage.bucket()
            file_filename = f"users/{uid}/sessions/{req.session_id}/scenes/{uuid.uuid4()}.png"
            blob = bucket.blob(file_filename)
            blob.upload_from_string(image_bytes, content_type="image/png")
            blob.make_public()  # Make accessible to thin client
            return {"status": "success", "image_url": blob.public_url, "is_mock": False}
        except Exception as e:
            logger.error(f"Firebase Storage upload failed: {e}. Returning mock image.")
            return {"status": "success", "image_url": MOCK_IMAGE_URL, "is_mock": True, "error": str(e)}
            
    # 3. Fallback Return
    return {"status": "success", "image_url": MOCK_IMAGE_URL, "is_mock": True}
```

---

## 7. Component 6: Python Dependencies, Environment & Execution Audit

### Requirements Verification & Updates
Current `requirements.txt`:
```text
fastapi==0.110.0
uvicorn==0.29.0
google-genai>=0.2.0
firebase-admin==6.5.0
pydantic>=2.12.5
python-dotenv==1.0.1
```

**Recommended Dependencies Additions**:
- `google-cloud-storage>=2.14.0` (Explicitly recommended for Firebase Storage blob management).
- `pillow>=10.2.0` (For image processing or verification if needed).

### Environment Configuration (`.env` Audit)
The `.env` file must include the storage bucket configuration:
```env
GEMINI_API_KEY=your_gemini_api_key_here
DATABASE_URL=https://paradoxo-rpg-default-rtdb.firebaseio.com/
STORAGE_BUCKET=paradoxo-rpg.appspot.com
```

### Virtualenv & Execution Commands
- **Windows**:
  ```powershell
  python -m venv venv
  .\venv\Scripts\activate
  pip install -r requirements.txt
  python main.py
  ```
- **Linux / Render Deployment**:
  ```bash
  python3 -m venv venv
  source venv/bin/activate
  pip install -r requirements.txt
  uvicorn main:app --host 0.0.0.0 --port 8000
  ```

---

## 8. Verification Method & Proposed Patches Summary

To verify the future implementation of these backend specifications:
1. **Auth Test**: Send HTTP POST to `/action` without `Authorization` header and confirm HTTP 401 response. Send valid Firebase JWT and verify HTTP 200.
2. **User Isolation Test**: Check Firebase Console to verify state updates occur under `/users/{uid}/sessions/{session_id}` and that non-owner tokens cannot read another user's path.
3. **Image Route Test**: Trigger `/generate-image`. Verify image blob is uploaded to Firebase Storage under `users/{uid}/...` and public URL is returned. Simulate API key quota failure and verify seamless fallback to mock URL.
