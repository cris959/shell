from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv

# Carga las variables de entorno definidas en el archivo .env
load_dotenv()

from app.schemas.analisis import AnalisisInputDTO, AnalisisOutputDTO, ConsultaAiInputDTO, ConsultaAiOutputDTO
from app.services.nlp_service import analizar_perfil_financiero, procesar_consulta_ai

app = FastAPI(title="Python NLP Service - Salud Financiera")

# --- CONFIGURACIÓN DE CORS (Obligatoria para evitar el error 405) ---
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Permite peticiones desde cualquier origen (puedes poner ["http://localhost:8080"] si prefieres)
    allow_credentials=True,
    allow_methods=["*"],  # Permite todos los métodos (GET, POST, OPTIONS, etc.)
    allow_headers=["*"],  # Permite todas las cabeceras
)

@app.get("/health")
def health_check():
    return {"status": "ok", "service": "python-nlp"}

@app.post("/api/v1/analizar-perfil", response_model=AnalisisOutputDTO)
def analizar_perfil(data: AnalisisInputDTO):
    return analizar_perfil_financiero(data)

# Añade esta función que faltaba en el main
@app.post("/api/v1/consultar-ia", response_model=ConsultaAiOutputDTO)
def post_consultar_ia(data: ConsultaAiInputDTO):
    return procesar_consulta_ai(data)