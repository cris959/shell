import os
from groq import Groq
from app.schemas.analisis import AnalisisInputDTO, AnalisisOutputDTO

def analizar_perfil_financiero(data: AnalisisInputDTO) -> AnalisisOutputDTO:
    # 1. Obtener y limpiar la API Key
    raw_key = os.getenv("GROQ_API_KEY")
    api_key = raw_key.strip() if raw_key else None

    if not api_key:
        return AnalisisOutputDTO(
            diagnostico="[ERROR] La GROQ_API_KEY no está configurada correctamente en el .env",
            nivel_riesgo="ALTO",
            recomendaciones=["Revisar el archivo .env en la raíz del proyecto."]
        )

    # 2. Formatear transacciones para el prompt
    transacciones_texto = "\n".join([
        f"- Categoría: {t.categoria}, Monto: ${t.monto}, Descripción: {t.descripcion}"
        for t in data.transacciones
    ]) if data.transacciones else "Sin transacciones registradas"

    prompt = f"""
    Eres un experto asesor financiero. Analiza los siguientes datos:
    - Ingresos totales: ${data.ingresos_totales}
    - Transacciones:
    {transacciones_texto}

    Genera un diagnóstico, un nivel de riesgo ("BAJO", "MEDIO", "ALTO") y 3 recomendaciones.
    Responde ÚNICAMENTE con un JSON con la estructura exacta:
    {{
        "diagnostico": "...",
        "nivel_riesgo": "BAJO",
        "recomendaciones": ["...", "...", "..."]
    }}
    """

    # 3. Llamada segura a Groq
    try:
        client = Groq(api_key=api_key)

        chat_completion = client.chat.completions.create(
            messages=[
                {
                    "role": "system",
                    "content": "Eres un asistente financiero que responde exclusivamente en formato JSON estructurado."
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
            diagnostico=f"Error al conectar con la IA de Groq: {str(e)}",
            nivel_riesgo="MEDIO",
            recomendaciones=["Verificar la validez de la GROQ_API_KEY en la consola de Groq."]
        )