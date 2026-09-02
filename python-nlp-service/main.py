from fastapi import FastAPI
from dotenv import load_dotenv

# Carga las variables de entorno definidas en el archivo .env
load_dotenv()

from app.schemas.analisis import AnalisisInputDTO, AnalisisOutputDTO
from app.services.nlp_service import analizar_perfil_financiero

app = FastAPI(title="Python NLP Service - Salud Financiera")

@app.get("/health")
def health_check():
    return {"status": "ok", "service": "python-nlp"}

@app.post("/api/v1/analizar-perfil", response_model=AnalisisOutputDTO)
def analizar_perfil(data: AnalisisInputDTO):
    return analizar_perfil_financiero(data)