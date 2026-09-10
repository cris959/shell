import os
import json
import urllib.request
from dotenv import load_dotenv

load_dotenv()
api_key = os.getenv("MISTRAL_API_KEY")

print(f"Probando llave: {api_key[:6]}... (longitud: {len(api_key) if api_key else 0})")

url = "https://api.mistral.ai/v1/models"
req = urllib.request.Request(
    url,
    headers={"Authorization": f"Bearer {api_key}"},
    method="GET"
)

try:
    with urllib.request.urlopen(req) as response:
        print("¡ÉXITO! La API Key de Mistral es válida.")
        print(response.read().decode("utf-8")[:200]) # Muestra un fragmento
except Exception as e:
    print(f"❌ FALLÓ LA PRUEBA: {e}")