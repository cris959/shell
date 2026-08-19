import os
import warnings
from dotenv import load_dotenv

# Silencia las advertencias de metadatos de campos en Pydantic
warnings.filterwarnings("ignore", message="The 'alias' attribute with value")
warnings.filterwarnings("ignore", message="The 'validation_alias' attribute with value")
warnings.filterwarnings("ignore", message="The 'serialization_alias' attribute with value")

# Carga las variables definidas en el archivo .env
load_dotenv()

from fastapi import FastAPI
from app.api.router import router as analisis_router

app = FastAPI(
    title="Python NLP Financial Service",
    description="Microservicio de ML/NLP e IA para análisis de perfil financiero",
    version="1.0.0"
)

# Registramos las rutas de app/api/router.py
app.include_router(analisis_router)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)