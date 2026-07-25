import os
import json
import google.generativeai as genai
from pydantic import BaseModel, Field
from typing import Dict
from dotenv import load_dotenv

load_dotenv()
genai.configure(api_key=os.environ.get("GEMINI_API_KEY"))

class NPCSchema(BaseModel):
    name: str = Field(description="Name of the NPC")
    status: str = Field(description="Current status (e.g. Vivo, Morto, Ferido)")
    relationships: Dict[str, str] = Field(description="Dictionary mapping player_id to relationship status")
    memory: str = Field(description="Updated memory of the NPC regarding recent events")

class PlayerSchema(BaseModel):
    name: str = Field(description="Name of the player")
    inventory: list[str] = Field(description="Updated inventory list of the player")

class TurnStateUpdate(BaseModel):
    narrative_text: str = Field(description="The narrative text describing the outcome of the action to be shown to the players")
    location: str = Field(description="Current location of the players")
    npcs: Dict[str, NPCSchema] = Field(description="Updated status of the NPCs involved")
    players: Dict[str, PlayerSchema] = Field(description="Updated status of the players involved")

system_instruction = "Você é o Mestre de Jogo (GM). Retorne os dados em JSON."
prompt = "Jogador 'player1' realizou a ação: 'oi'. Descreva as consequências."

try:
    print("Initializing model...")
    model = genai.GenerativeModel(
        model_name='gemini-3.5-flash',
        system_instruction=system_instruction
    )
    print("Calling generate_content...")
    response = model.generate_content(
        prompt,
        generation_config=genai.GenerationConfig(
            response_mime_type="application/json",
            response_schema=TurnStateUpdate,
        )
    )
    print("Response received!")
    print(response.text)
except Exception as e:
    print(f"Error: {e}")
