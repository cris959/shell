from fastapi import APIRouter
from app.schemas.analisis import AnalisisInputDTO, AnalisisOutputDTO
from app.services.nlp_service import analizar_perfil_financiero

router = APIRouter(prefix="/api/v1", tags=["Análisis NLP"])

@router.post("/analizar-perfil", response_model=AnalisisOutputDTO)
def analizar_perfil(data: AnalisisInputDTO):
    return analizar_perfil_financiero(data)