import os
import json
from dotenv import load_dotenv
from google import genai
from google.genai import types

load_dotenv()
client = genai.Client(api_key=os.environ.get("GEMINI_API_KEY"))

system_instruction = """Você é o Mestre de Jogo (GM). Retorne os dados em JSON, com as chaves: narrative_text, location, npcs, players."""
prompt = "Jogador 'player1' realizou a ação: 'oi'. Descreva as consequências."

try:
    print("Calling generate_content...")
    response = client.models.generate_content(
        model='gemini-2.5-flash',
        contents=prompt,
        config=types.GenerateContentConfig(
            response_mime_type="application/json",
            system_instruction=system_instruction
        )
    )
    print("Response received!")
    print(response.text)
except Exception as e:
    print(f"Error: {e}")
