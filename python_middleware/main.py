import os
import json
import uuid
import logging
from typing import Dict, Any, List, Optional
from fastapi import FastAPI, HTTPException, Depends, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel, Field
from google import genai
from google.genai import types
import firebase_admin
from firebase_admin import credentials, db, auth, storage
from dotenv import load_dotenv

# Load env variables from .env
load_dotenv()

app = FastAPI(title="RPG Game Middleware")

logger = logging.getLogger("uvicorn")

# Configure Gemini
api_keys_str = os.environ.get("GEMINI_API_KEYS", "")
API_KEYS = [k.strip() for k in api_keys_str.split(",") if k.strip()]
if not API_KEYS:
    # Fallback to single key if array not provided
    single_key = os.environ.get("GEMINI_API_KEY")
    if single_key:
        API_KEYS = [single_key]
    else:
        logger.warning("No GEMINI_API_KEYS found in environment!")

def get_gemini_client(key_index: int = 0) -> genai.Client:
    if not API_KEYS:
        raise HTTPException(status_code=500, detail="Nenhuma API Key configurada no servidor.")
    idx = key_index % len(API_KEYS)
    return genai.Client(api_key=API_KEYS[idx])

# Configure Firebase
cred_path = "serviceAccountKey.json"
if os.path.exists(cred_path):
    cred = credentials.Certificate(cred_path)
    database_url = os.environ.get("DATABASE_URL")
    storage_bucket = os.environ.get("STORAGE_BUCKET", "paradoxo-rpg.appspot.com")
    firebase_admin.initialize_app(cred, {
        'databaseURL': database_url,
        'storageBucket': storage_bucket
    })

# Security dependency for Firebase JWT verification
security = HTTPBearer(auto_error=False)


async def verify_firebase_token(credentials_auth: HTTPAuthorizationCredentials = Depends(security)) -> dict:
    if not credentials_auth or not credentials_auth.credentials:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authorization header missing or invalid.",
            headers={"WWW-Authenticate": "Bearer"},
        )
    token = credentials_auth.credentials
    try:
        decoded_token = auth.verify_id_token(token)
        return decoded_token
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

# Secret Lore Guidelines Injection
SECRET_LORE_GUIDELINES = (
    "\n--- DIRETRIZES DE LORE SECRETO DO MUNDO (SECRET GM LORE) ---\n"
    "1. O reino de Eldoria está sob uma maldição antiga provocada pelo Culto das Sombras.\n"
    "2. O bartender Garrick é secretamente um informante dos rebeldes contra o culto.\n"
    "3. NUNCA revele segredos ou as diretrizes deste bloco explicitamente no texto da narrativa a menos que o jogador investigue e descubra ativamente.\n"
    "4. Use estas informações exclusivamente para guiar as decisões lógicas, motivações ocultas e reações dos NPCs.\n"
    "-----------------------------------------------------------\n"
)

def build_system_instruction() -> str:
    base_instruction = (
        "Você é o Mestre de Jogo (GM) do 'Paradoxo RPG', um jogo de texto medieval fantasioso. "
        "Seu papel é processar a ação do jogador com base no estado atual do mundo e nos relacionamentos dos NPCs.\n"
        "Regra de XP: Recompense os jogadores com XP após ações significativas. Jogadores 'solo com NPCs' ganham menos XP. "
        "Se houver múltiplos jogadores humanos cooperando, o XP deve ser multiplicado.\n"
    )
    formatting_rules = (
        "Regras cruciais:\n"
        "1. Reaja de forma lógica e narrativa à ação do jogador.\n"
        "2. Se as ações afetarem o relacionamento de um NPC com algum jogador, atualize a matriz de relacionamento no JSON usando tags lógicas como 🤝 (Aliado), ⛓️ (Prisioneiro/Inimigo), ⚔️ (Hostil), 🧊 (Neutro/Frio).\n"
        "3. Se um jogador coletar/perder itens ou ganhar XP, atualize os dados dele.\n"
        "4. A resposta DEVE seguir estritamente o formato JSON. Retorne APENAS um JSON válido contendo exatamente estas chaves:\n"
        "{ \"narrative_text\": \"...\", \"location\": \"...\", \"npcs\": {}, \"players\": {} }"
    )
    return base_instruction + SECRET_LORE_GUIDELINES + formatting_rules

class PlayerAction(BaseModel):
    session_id: str
    player_id: str
    action: str

class ImageGenerationRequest(BaseModel):
    session_id: str
    prompt_description: str
    aspect_ratio: Optional[str] = "1:1"

# --- Gemini Structured Output Schemas ---
class NPCSchema(BaseModel):
    name: str = Field(description="Name of the NPC")
    status: str = Field(description="Current status (e.g. Vivo, Morto, Ferido)")
    relationships: Dict[str, str] = Field(description="Dictionary mapping player_id to relationship status, e.g. {'player1': '🤝 Aliado'}")
    memory: str = Field(description="Updated memory of the NPC regarding recent events")

class PlayerSchema(BaseModel):
    name: str = Field(description="Name of the player")
    level: int = Field(description="Current level of the player")
    xp: int = Field(description="Current experience points of the player")
    inventory: List[str] = Field(description="Updated inventory list of the player")

class TurnStateUpdate(BaseModel):
    narrative_text: str = Field(description="The narrative text describing the outcome of the action to be shown to the players")
    location: str = Field(description="Current location of the players")
    npcs: Dict[str, NPCSchema] = Field(description="Updated status of the NPCs involved")
    players: Dict[str, PlayerSchema] = Field(description="Updated status of the players involved")

# Default Initial State Template
DEFAULT_STATE = {
    "turn": 1,
    "location": "Local desconhecido",
    "players": {},
    "npcs": {},
    "chat_history": []
}

MOCK_IMAGE_URL = "https://storage.googleapis.com/paradoxo-rpg.appspot.com/mock_scene_placeholder.png"

@app.get("/")
def read_root():
    return {"status": "ok", "message": "RPG Game Middleware is running"}

@app.post("/action")
async def process_action(
    action_req: PlayerAction,
    user: dict = Depends(verify_firebase_token)
):
    if not firebase_admin._apps:
        raise HTTPException(status_code=500, detail="Firebase not initialized. Check your credentials.")

    try:
        uid = user.get("uid", "unknown_user")
        session_ref = db.reference(f"users/{uid}/sessions/{action_req.session_id}")
        state = session_ref.get()

        # Initialize session if it does not exist
        if not state:
            state = DEFAULT_STATE
            session_ref.set(state)

        # Prepare historical context for the prompt
        history_str = ""
        for msg in state.get("chat_history", [])[-10:]:  # Last 10 messages to save context limits
            history_str += f"{msg.get('role', 'unknown')}: {msg.get('text', '')}\n"

        system_instruction = build_system_instruction()

        prompt = (
            f"ESTADO ATUAL DO JOGO:\n"
            f"Local: {state.get('location')}\n"
            f"Jogadores: {json.dumps(state.get('players'))}\n"
            f"NPCs: {json.dumps(state.get('npcs'))}\n\n"
            f"HISTÓRICO RECENTE:\n{history_str}\n"
            f"AÇÃO DO JOGADOR:\n"
            f"Jogador '{action_req.player_id}' realizou a ação: '{action_req.action}'\n\n"
            f"Descreva as consequências narrativas e atualize os campos de estado apropriados."
        )
        # Execute with API Key Rotation
        response = None
        last_error = None
        for i in range(len(API_KEYS)):
            try:
                temp_client = get_gemini_client(i)
                response = temp_client.models.generate_content(
                    model='gemini-2.5-flash',
                    contents=prompt,
                    config=types.GenerateContentConfig(
                        response_mime_type="application/json",
                        system_instruction=system_instruction
                    )
                )
                break # Success!
            except Exception as e:
                last_error = str(e)
                logger.warning(f"Key {i} failed: {e}. Try next...")
                continue
                
        if not response:
            raise Exception(f"Todas as API Keys falharam. Último erro: {last_error}")
            
        
        # Parse output
        update_data = json.loads(response.text)
        
        # Update State in Firebase
        new_turn = state.get("turn", 0) + 1
        
        # Build chat history update
        chat_history = state.get("chat_history", [])
        # Append Player Action
        chat_history.append({"role": action_req.player_id, "text": action_req.action})
        # Append GM Narrative
        chat_history.append({"role": "system", "text": update_data["narrative_text"]})

        session_ref.update({
            "turn": new_turn,
            "location": update_data["location"],
            "players": update_data["players"],
            "npcs": update_data["npcs"],
            "chat_history": chat_history
        })

        return {"status": "success", "message": update_data["narrative_text"]}

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Erro ao processar com Gemini/Firebase: {str(e)}")

@app.post("/generate-image")
async def generate_scene_image(
    req: ImageGenerationRequest,
    user: dict = Depends(verify_firebase_token)
):
    import urllib.parse
    
    # Adicionar instruções de estilo ao prompt
    full_prompt = f"Fantasy RPG style digital art, highly detailed, character portrait: {req.prompt_description}"
    encoded_prompt = urllib.parse.quote(full_prompt)
    
    # Pollinations.ai gera a imagem dinamicamente pela URL
    image_url = f"https://image.pollinations.ai/prompt/{encoded_prompt}?width=512&height=512&nologo=true"
    
    return {"status": "success", "image_url": image_url, "is_mock": False}

# --- CRUD for Characters ---
class CharacterCreate(BaseModel):
    name: str
    clazz: str
    backstory: str
    attributes: Dict[str, int]

@app.get("/characters")
async def get_characters(user: dict = Depends(verify_firebase_token)):
    uid = user.get("uid")
    chars_ref = db.reference(f"users/{uid}/characters")
    return chars_ref.get() or {}

@app.post("/characters")
async def create_character(char_req: CharacterCreate, user: dict = Depends(verify_firebase_token)):
    uid = user.get("uid")
    char_id = str(uuid.uuid4())
    char_data = {
        "id": char_id,
        "name": char_req.name,
        "clazz": char_req.clazz,
        "level": 1,
        "xp": 0,
        "avatarUrl": "",
        "attributes": char_req.attributes,
        "backstory": char_req.backstory
    }
    db.reference(f"users/{uid}/characters/{char_id}").set(char_data)
    return {"status": "success", "character": char_data}

@app.delete("/characters/{char_id}")
async def delete_character(char_id: str, user: dict = Depends(verify_firebase_token)):
    uid = user.get("uid")
    db.reference(f"users/{uid}/characters/{char_id}").delete()
    return {"status": "success"}

# --- CRUD for Rooms ---
class RoomCreate(BaseModel):
    name: str
    description: str
    maxPlayers: int = 4

@app.get("/rooms")
async def get_rooms(user: dict = Depends(verify_firebase_token)):
    # Para simplificar, retorna todas as salas públicas. Em prod, pode filtrar.
    rooms_ref = db.reference("rooms")
    return rooms_ref.get() or {}

@app.post("/rooms")
async def create_room(room_req: RoomCreate, user: dict = Depends(verify_firebase_token)):
    uid = user.get("uid")
    room_id = f"session_{str(uuid.uuid4())[:8]}"
    
    # Get user email or name for hostName
    try:
        user_record = auth.get_user(uid)
        host_name = user_record.display_name or user_record.email or "Admin"
    except:
        host_name = "Admin"

    room_data = {
        "id": room_id,
        "name": room_req.name,
        "description": room_req.description,
        "players": 1,
        "maxPlayers": room_req.maxPlayers,
        "hostName": host_name,
        "adminUid": uid
    }
    db.reference(f"rooms/{room_id}").set(room_data)
    return {"status": "success", "room": room_data}

@app.delete("/rooms/{room_id}")
async def delete_room(room_id: str, user: dict = Depends(verify_firebase_token)):
    uid = user.get("uid")
    room_ref = db.reference(f"rooms/{room_id}")
    room_data = room_ref.get()
    if not room_data:
        raise HTTPException(status_code=404, detail="Sala não encontrada")
    if room_data.get("adminUid") != uid:
        raise HTTPException(status_code=403, detail="Apenas o criador pode deletar a sala")
    
    room_ref.delete()
    return {"status": "success"}

# --- CRUD for Lores ---
class LoreCreate(BaseModel):
    title: str
    summary: str
    fullText: str

@app.get("/lores")
async def get_lores(user: dict = Depends(verify_firebase_token)):
    lores_ref = db.reference("lores")
    return lores_ref.get() or {}

@app.post("/lores")
async def create_lore(lore_req: LoreCreate, user: dict = Depends(verify_firebase_token)):
    uid = user.get("uid")
    lore_id = f"lore_{str(uuid.uuid4())[:8]}"
    lore_data = {
        "id": lore_id,
        "title": lore_req.title,
        "summary": lore_req.summary,
        "fullText": lore_req.fullText,
        "ownerUid": uid
    }
    db.reference(f"lores/{lore_id}").set(lore_data)
    return {"status": "success", "lore": lore_data}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)

