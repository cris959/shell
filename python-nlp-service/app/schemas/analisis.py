from pydantic import BaseModel, Field
from typing import List, Optional

class TransaccionItemDTO(BaseModel):
    monto: float
    tipo: str
    categoria: str
    descripcion: Optional[str] = None
    fecha_transaccion: Optional[str] = None # Opcional según tu DTO de Java

class AnalisisInputDTO(BaseModel):
    ingreso_mensual: float = Field(..., alias="ingreso_mensual")
    ahorro_actual: Optional[float] = Field(None, alias="ahorro_actual")
    nivel_endeudamiento: Optional[int] = Field(0, alias="nivel_endeudamiento")
    frecuencia_ahorro: str = Field(..., alias="frecuencia_ahorro")
    descripcion: str = Field(..., alias="descripcion")
    valor: float = Field(..., alias="valor")
    historial_transacciones: List[TransaccionItemDTO] = Field(default_factory=list, alias="historial_transacciones")

    class Config:
        populate_by_name = True

class AnalisisOutputDTO(BaseModel):
    perfil_financiero: str
    probabilidad: float
    resumen_gastos: dict
    recomendaciones: List[str]
    total_gastado: float
    capacidad_ahorro_mensual: float
    porcentaje_tasa_ahorro: float
    progreso_meta_ahorro: float
    meses_para_meta: float