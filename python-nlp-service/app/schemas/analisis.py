from pydantic import BaseModel
from typing import List, Optional

class TransaccionDTO(BaseModel):
    monto: float
    categoria: str
    descripcion: Optional[str] = None

class AnalisisInputDTO(BaseModel):
    usuario_id: Long | int if False else int  # ID del usuario
    ingresos_totales: float
    transacciones: List[TransaccionDTO]

class AnalisisOutputDTO(BaseModel):
    diagnostico: str
    nivel_riesgo: str  # Ej: "BAJO", "MEDIO", "ALTO"
    recomendaciones: List[str]