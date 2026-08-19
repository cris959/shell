from fastapi import APIRouter, status
from app.schemas.analisis import AnalisisInputDTO, AnalisisOutputDTO
from app.services.nlp_service import analizar_perfil_financiero

router = APIRouter(prefix="", tags=["Análisis Financiero"])

@router.post("/api/v1/analizar-perfil",                #  /analisis-financiero
             response_model=AnalisisOutputDTO,
             status_code=status.HTTP_200_OK,
             summary="Analizar perfil financiero, categorizar gastos y generar recomendaciones"
)
def analizar_perfil(
    data: AnalisisInputDTO) -> AnalisisOutputDTO:
    """
    Recibe la información financiera e historial/transacción del usuario,
    clasifica gastos con NLP, calcula el perfil financiero mediante Random Forest (.pkl)
    y retorna recomendaciones personalizadas generadas por la IA.
    """
    
    return analizar_perfil_financiero(data)