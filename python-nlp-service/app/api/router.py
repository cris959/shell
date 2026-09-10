from fastapi import APIRouter
from app.schemas.analisis import AnalisisInputDTO, AnalisisOutputDTO, ConsultaAiInputDTO, ConsultaAiOutputDTO
from app.services.nlp_service import analizar_perfil_financiero, procesar_consulta_ai

router = APIRouter(prefix="/api/v1", tags=["Análisis NLP"])

@router.post("/analizar-perfil", response_model=AnalisisOutputDTO)
def analizar_perfil(data: AnalisisInputDTO):
    return analizar_perfil_financiero(data)

@router.post("/consultar-ia", response_model=ConsultaAiOutputDTO)
def consultar_ia(data: ConsultaAiInputDTO):
    return procesar_consulta_ai(data)