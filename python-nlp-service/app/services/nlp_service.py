import os
from groq import Groq
from app.schemas.analisis import AnalisisInputDTO, AnalisisOutputDTO

def analizar_perfil_financiero(data: AnalisisInputDTO) -> AnalisisOutputDTO:
    # 1. Obtener y limpiar la API Key
    raw_key = os.getenv("GROQ_API_KEY")
    api_key = raw_key.strip() if raw_key else None

    if not api_key:
        return AnalisisOutputDTO(
            perfil_financiero="ERROR CONFIG",
            probabilidad=0.0,
            resumen_gastos={},
            recomendaciones=["Revisar el archivo .env en la raíz del proyecto para configurar la GROQ_API_KEY."],
            total_gastado=0.0,
            capacidad_ahorro_mensual=0.0,
            porcentaje_tasa_ahorro=0.0,
            progreso_meta_ahorro=0.0,
            meses_para_meta=0.0
        )

    # 2. Formatear el historial de transacciones para el prompt usando el nombre correcto
    transacciones_texto = "\n".join([
        f"- Tipo: {t.tipo}, Categoría: {t.categoria}, Monto: ${t.monto}, Descripción: {t.descripcion}"
        for t in data.historial_transacciones
    ]) if data.historial_transacciones else "Sin transacciones registradas"

    prompt = f"""
    Eres un experto asesor financiero. Analiza los siguientes datos del usuario:
    - Ingreso mensual: ${data.ingreso_mensual}
    - Ahorro actual: ${data.ahorro_actual}
    - Nivel de endeudamiento (0-100): {data.nivel_endeudamiento}
    - Frecuencia de ahorro: {data.frecuencia_ahorro}
    - Meta/Descripción del análisis: {data.descripcion}
    - Valor objetivo de la meta: ${data.valor}
    - Historial de transacciones:
    {transacciones_texto}

    Genera un análisis financiero completo y responde ÚNICAMENTE con un JSON que cumpla exactamente con esta estructura:
    {{
        "perfil_financiero": "SALUDABLE" | "EN OBSERVACION" | "EN RIESGO",
        "probabilidad": 0.85,
        "resumen_gastos": {{
            "CATEGORIA_1": 100.0,
            "CATEGORIA_2": 200.0
        }},
        "recomendaciones": ["Recomendación 1", "Recomendación 2", "Recomendación 3"],
        "total_gastado": 300.0,
        "capacidad_ahorro_mensual": 150.0,
        "porcentaje_tasa_ahorro": 25.5,
        "progreso_meta_ahorro": 10.0,
        "meses_para_meta": 12.0
    }}
    """

    # 3. Llamada segura a Groq
    try:
        client = Groq(api_key=api_key)

        chat_completion = client.chat.completions.create(
            messages=[
                {
                    "role": "system",
                    "content": "Eres un asistente financiero experto que responde exclusivamente en formato JSON estructurado válido."
                },
                {
                    "role": "user",
                    "content": prompt,
                }
            ],
            model="llama-3.3-70b-versatile",
            response_format={"type": "json_object"}
        )

        contenido_json = chat_completion.choices[0].message.content
        return AnalisisOutputDTO.model_validate_json(contenido_json)

    except Exception as e:
        print(f"\n[GROQ ERROR]: {e}\n")
        return AnalisisOutputDTO(
            perfil_financiero="EN OBSERVACION",
            probabilidad=0.0,
            resumen_gastos={},
            recomendaciones=[f"Servicio en modo degradado por error con la IA: {str(e)}"],
            total_gastado=0.0,
            capacidad_ahorro_mensual=0.0,
            porcentaje_tasa_ahorro=0.0,
            progreso_meta_ahorro=0.0,
            meses_para_meta=0.0
        )