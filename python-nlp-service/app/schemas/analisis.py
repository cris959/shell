from typing import List, Dict, Optional
from pydantic import BaseModel, Field, ConfigDict, AliasChoices, model_validator


class TransaccionItemDTO(BaseModel):
    monto: float = Field(default=0.0, validation_alias=AliasChoices("monto", "valor"))
    descripcion: str = Field(default="", validation_alias="descripcion")
    categoria: Optional[str] = Field(default="General", validation_alias="categoria")
    
    
# ENTRADA: Acepta tanto snake_case (Postman/Spring Boot) como camelCase
class AnalisisInputDTO(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    usuario_id: Optional[str] = Field(default=None, validation_alias=AliasChoices("usuario_id", "usuarioId"))
    ingreso_mensual: float = Field(default=0.0, validation_alias=AliasChoices("ingreso_mensual", "ingresoMensual"))
    
    # Opcional (default=None) para permitir que el servicio calcule el nivel automáticamente cuando no se envía
    nivel_endeudamiento: Optional[float] = Field(default=None, validation_alias=AliasChoices("nivel_endeudamiento", "nivelEndeudamiento"))
    total_deudas: Optional[float] = Field(default=0.0, validation_alias=AliasChoices("total_deudas", "totalDeudas"))
    
    frecuencia_ahorro: Optional[str] = Field(default="MENSUAL", validation_alias=AliasChoices("frecuencia_ahorro", "frecuenciaAhorro"))
    descripcion: Optional[str] = Field(default="", validation_alias="descripcion")
    valor: Optional[float] = Field(default=0.0, validation_alias="valor")
    
    ahorro_actual: float = Field(default=0.0, validation_alias=AliasChoices("ahorro_actual", "ahorroActual"))
    meta_ahorro: float = Field(default=0.0, validation_alias=AliasChoices("meta_ahorro", "metaAhorro"))
    monto_proxima_meta: float = Field(default=0.0, validation_alias=AliasChoices("monto_proxima_meta", "montoProximaMeta"))
    
    historial_transacciones: Optional[List[TransaccionItemDTO]] = Field(
        default_factory=list, 
        validation_alias=AliasChoices("historial_transacciones", "historialTransacciones")
    )
    
    # Si meta_ahorro viene en 0.0 pero 'valor' trae el monto, se sincronizan automáticamente
    @model_validator(mode="after")
    def sincronizar_meta_ahorro(self):
        if self.meta_ahorro == 0.0 and self.valor and self.valor > 0:
            self.meta_ahorro = self.valor
        return self


# SALIDA: Envía a Spring Boot estrictamente en snake_case
class AnalisisOutputDTO(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    perfil_financiero: str = Field(..., serialization_alias="perfil_financiero")
    probabilidad: float
    resumen_gastos: Dict[str, float] = Field(default_factory=dict, serialization_alias="resumen_gastos")
    recomendaciones: List[str]
    total_gastado: float = Field(..., serialization_alias="total_gastado")
    capacidad_ahorro_mensual: float = Field(..., serialization_alias="capacidad_ahorro_mensual")
    porcentaje_tasa_ahorro: float = Field(..., serialization_alias="porcentaje_tasa_ahorro")
    progreso_meta_ahorro: float = Field(..., serialization_alias="progreso_meta_ahorro")
    meses_para_meta: Optional[float] = Field(default=None, serialization_alias="meses_para_meta")