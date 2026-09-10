from pydantic import BaseModel, Field
from typing import List, Optional

class TransaccionItemDTO(BaseModel):
    monto: float
    tipo: Optional[str] = "GASTO"
    categoria: Optional[str] = "GENERAL"
    descripcion: Optional[str] = None
    fecha_transaccion: Optional[str] = None

class AnalisisInputDTO(BaseModel):
    ingreso_mensual: float
    ahorro_actual: Optional[float] = None
    nivel_endeudamiento: Optional[int] = 0
    frecuencia_ahorro: str
    descripcion: str
    valor: float
    historial_transacciones: List[TransaccionItemDTO] = Field(default_factory=list)

    class Config:
        populate_by_name = True

class AnalisisOutputDTO(BaseModel):
    perfil_financiero: str
    probabilidad: float
    resumen_gastos: dict
    recomendaciones: List[str] = Field(
        default_factory=lambda: [
            "Monitorea de cerca tus gastos diarios en transporte y alimentación.",
            "Establece un porcentaje fijo de ahorro automático al recibir tus ingresos.",
            "Evalúa reducir pequeños gastos hormiga para acelerar tu meta."
        ]
    )
    total_gastado: float
    capacidad_ahorro_mensual: float
    porcentaje_tasa_ahorro: float
    progreso_meta_ahorro: float
    meses_para_meta: float

class ConsultaAiInputDTO(BaseModel):
    query: str
    email: str

class ConsultaAiOutputDTO(BaseModel):
    respuesta: str