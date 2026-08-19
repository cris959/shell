import os
import json
import logging
import joblib
from pathlib import Path
from typing import Dict, Tuple, Any, Optional, List
import pandas as pd
from groq import Groq
from mistralai import Mistral

from app.schemas.analisis import AnalisisInputDTO, AnalisisOutputDTO

logger = logging.getLogger(__name__)

MODELS_DIR = Path(__file__).resolve().parent.parent / "models"

# Carga de Artefactos ML/NLP
try:
    nlp_categorizer = joblib.load(MODELS_DIR / "transaction_categorizer.pkl")
    profile_artifact = joblib.load(MODELS_DIR / "financial_profile_model.pkl")
    
    profile_model = profile_artifact['model']
    target_encoder = profile_artifact['target_encoder']
    feature_names = profile_artifact['features']
    logger.info(f"✓ Modelos ML/NLP (.pkl) cargados desde {MODELS_DIR}")
except Exception as e:
    logger.error(f"❌ Error al cargar artefactos .pkl: {e}")
    nlp_categorizer, profile_model, target_encoder, feature_names = None, None, None, []

# Clientes LLM
groq_client = Groq(api_key=os.getenv("GROQ_API_KEY"))
mistral_client = Mistral(api_key=os.getenv("MISTRAL_API_KEY"))

# modificacion nivel_endeudamiento
def calcular_nivel_endeudamiento(ingreso_mensual: float, total_deudas: float) -> int:
    """
    Calcula la escala de endeudamiento (0 a 4) según la relación Deudas / Ingreso:
    0: <= 10% | 1: 11%-30% | 2: 31%-40% | 3: 41%-60% | 4: > 60%
    """
    if ingreso_mensual <= 0:
        return 0 if total_deudas <= 0 else 4

    ratio = total_deudas / ingreso_mensual

    if ratio <= 0.10:
        return 0
    elif ratio <= 0.30:
        return 1
    elif ratio <= 0.40:
        return 2
    elif ratio <= 0.60:
        return 3
    else:
        return 4

# Normalizado a Mayusculas
def procesar_perfil_y_resumen(data: AnalisisInputDTO) -> Dict[str, Any]:
    resumen_gastos: Dict[str, float] = {}

    # Mapeo seguro de atributos para prevenir mismatches con el DTO
    ingreso_mensual = float(getattr(data, "ingreso_mensual", 0.0) or 0.0)
    ahorro_actual_val = float(getattr(data, "ahorro_actual", 0.0) or 0.0)
    meta_ahorro_val = float(getattr(data, "meta_ahorro", 0.0) or getattr(data, "valor", 0.0) or 0.0)
    nivel_endeudamiento_in = getattr(data, "nivel_endeudamiento", None)

    total_deudas = 0.0

  # 1. Agrupar transacciones si viene un historial (Carga masiva CSV)
    if hasattr(data, "historial_transacciones") and data.historial_transacciones:
        items = [t.model_dump() if hasattr(t, "model_dump") else t for t in data.historial_transacciones]
        df = pd.DataFrame(items)
        if not df.empty and "monto" in df.columns:
            if "categoria" not in df.columns or df["categoria"].isnull().all():
                if nlp_categorizer and "descripcion" in df.columns:
                    df["categoria"] = nlp_categorizer.predict(df["descripcion"])
                else:
                    df["categoria"] = "GENERAL"
            
            # NORMALIZACIÓN: Rellenar nulos, limpiar espacios y convertir a MAYÚSCULAS
            df["categoria"] = df["categoria"].fillna("GENERAL").astype(str).str.strip().str.upper()
            
            resumen_series = df.groupby("categoria")["monto"].sum()
            resumen_gastos = {str(k): float(v) for k, v in resumen_series.to_dict().items()}

            # Extraer deudas automáticas del historial
            categorias_deuda = {"DEUDA", "PRESTAMO", "CREDITO", "TARJETA_CREDITO", "TARJETA DE CREDITO"}
            total_deudas = sum(
                abs(v) for k, v in resumen_gastos.items() if k in categorias_deuda
            )

    # 2. EVALUAR 'ELIF': Solo sumar movimiento individual si NO hubo historial masivo CSV
    elif hasattr(data, "descripcion") and data.descripcion and hasattr(data, "valor") and (data.valor or 0) > 0:
        cat_predicha = nlp_categorizer.predict([data.descripcion])[0] if nlp_categorizer else "GENERAL"
        cat_normalizada = str(cat_predicha).strip().upper()
        monto_val = float(data.valor)
        resumen_gastos[cat_normalizada] = monto_val

        if cat_normalizada in {"DEUDA", "PRESTAMO", "CREDITO", "TARJETA_CREDITO", "TARJETA DE CREDITO"}:
            total_deudas = monto_val

    # 3. Determinar Nivel de Endeudamiento (Manual vs Calculado)
    if nivel_endeudamiento_in is not None and float(nivel_endeudamiento_in) > 0:
        nivel_endeudamiento = float(nivel_endeudamiento_in)
    else:
        nivel_endeudamiento = float(calcular_nivel_endeudamiento(ingreso_mensual, total_deudas))

    # ---------------------------------------------------------------------
    # 4. Métricas Financieras
    # ---------------------------------------------------------------------
    total_gastado = round(
        sum(abs(v) for k, v in resumen_gastos.items() if k != "INGRESO"), 2
    )
    capacidad_ahorro = max(0.0, round(ingreso_mensual - total_gastado, 2))
    
    tasa_ahorro_pct = round((capacidad_ahorro / ingreso_mensual) * 100, 2) if ingreso_mensual > 0 else 0.0
    
    progreso_meta_pct = round((ahorro_actual_val / meta_ahorro_val) * 100, 2) if meta_ahorro_val > 0 else 0.0

    monto_faltante = max(0.0, meta_ahorro_val - ahorro_actual_val)
    if monto_faltante == 0:
        meses_para_meta = 0.0
    elif capacidad_ahorro > 0:
        meses_para_meta = round(monto_faltante / capacidad_ahorro, 1)
    else:
        meses_para_meta = 0.0

    # ---------------------------------------------------------------------
    # 5. Predicción Random Forest
    # ---------------------------------------------------------------------
    relacion_deuda = nivel_endeudamiento
    tasa_ahorro_ratio = capacidad_ahorro / ingreso_mensual if ingreso_mensual > 0 else 0.0

    if profile_model and target_encoder:
        input_df = pd.DataFrame([{
            'ingreso_mensual': ingreso_mensual,
            'gasto_mensual_total': total_gastado,
            'relacion_deuda_ingreso': relacion_deuda,
            'tasa_ahorro': tasa_ahorro_ratio
        }])[feature_names]

        pred_idx = profile_model.predict(input_df)[0]
        probs = profile_model.predict_proba(input_df)[0]
        perfil_calculado = target_encoder.inverse_transform([pred_idx])[0]
        probabilidad = float(probs[pred_idx])
    else:
        perfil_calculado = "En riesgo"
        probabilidad = 0.99

    return {
        "perfil_calculado": str(perfil_calculado),
        "probabilidad": round(probabilidad, 2),
        "resumen_gastos": resumen_gastos,
        "total_gastado": total_gastado,
        "capacidad_ahorro": capacidad_ahorro,
        "tasa_ahorro_pct": tasa_ahorro_pct,
        "progreso_meta_pct": progreso_meta_pct,
        "meses_para_meta": meses_para_meta,
        "nivel_endeudamiento": nivel_endeudamiento
    }


def analizar_perfil_financiero(data: AnalisisInputDTO) -> AnalisisOutputDTO:
    # 1. Obtener la métrica procesada y el diccionario de resumen
    res = procesar_perfil_y_resumen(data)
    
    perfil_calculado = res.get("perfil_calculado", "EN OBSERVACION")
    probabilidad = res.get("probabilidad", 0.75)
    resumen_gastos = res.get("resumen_gastos", {})
    meses_para_meta = res.get("meses_para_meta")
    nivel_endeudamiento_final = res.get("nivel_endeudamiento", 0)
    
    texto_meses = f"{meses_para_meta} meses" if meses_para_meta is not None else "no alcanzable con la capacidad actual"

    CANTIDAD_RECOMENDACIONES = 2

    prompt = f"""
    Genera un análisis financiero en formato JSON estricto con la siguiente clave:
    - "recomendaciones": lista de EXACTAMENTE {CANTIDAD_RECOMENDACIONES} consejos concisos en texto basándote en:
      * Perfil del usuario: {perfil_calculado}
      * Ingreso mensual: {data.ingreso_mensual or 0.0}
      * Ahorro actual: {data.ahorro_actual or 0.0}
      * Meta ahorro: {data.meta_ahorro or data.valor or 0.0} (Estimación para lograrla: {texto_meses})
      * Endeudamiento (Escala 0-4): {nivel_endeudamiento_final}
    
    Responde ÚNICAMENTE un objeto JSON válido con la clave "recomendaciones".
    """

    recomendaciones: List[str] = []

    # 2. Intento con Proveedor Primario (Groq)
    try:
        logger.info("Procesando recomendaciones con proveedor primario (Groq)...")
        response = groq_client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[{"role": "user", "content": prompt}],
            response_format={"type": "json_object"}
        )
        content = response.choices[0].message.content
        data_dict = json.loads(content)
        recomendaciones = data_dict.get("recomendaciones", [])[:CANTIDAD_RECOMENDACIONES]

    except Exception as e:
        logger.error(f"❌ FALLÓ GROQ: {type(e).__name__} - {e}. Activando respaldo (Mistral AI)...")

        # 3. Intento con Proveedor de Respaldo (Mistral)
        try:
            logger.info("Procesando recomendaciones con proveedor de respaldo (Mistral)...")
            response = mistral_client.chat.complete(
                model="mistral-small-latest",
                messages=[{"role": "user", "content": prompt}],
                response_format={"type": "json_object"}
            )
            content = response.choices[0].message.content
            data_dict = json.loads(content)
            recomendaciones = data_dict.get("recomendaciones", [])[:CANTIDAD_RECOMENDACIONES]

        except Exception as err:
            logger.error(f"❌ FALLÓ MISTRAL: {type(err).__name__} - {err}. Aplicando recomendaciones por defecto.")
            meta_val = data.meta_ahorro or data.valor or 0.0
            recomendaciones = [
                f"Mantén tu capacidad de ahorro enfocado en tu meta de ${meta_val:,.2f}.",
                "Revisa y controla tus gastos principales en el resumen por categoría."
            ]

    # 4. Construcción del DTO de respuesta con mapeos seguros
    return AnalisisOutputDTO(
        perfil_financiero=perfil_calculado,
        probabilidad=probabilidad,
        resumen_gastos=resumen_gastos,
        recomendaciones=recomendaciones,
        total_gastado=res.get("total_gastado", sum(resumen_gastos.values())),
        capacidad_ahorro_mensual=res.get("capacidad_ahorro", 0.0),
        porcentaje_tasa_ahorro=res.get("tasa_ahorro_pct", 0.0),
        progreso_meta_ahorro=res.get("progreso_meta_pct", 0.0),
        meses_para_meta=meses_para_meta if meses_para_meta is not None else 0.0
    )