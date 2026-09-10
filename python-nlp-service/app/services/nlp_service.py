import os
import json
import urllib.request
import urllib.error
from groq import Groq
from app.schemas.analisis import (
    AnalisisInputDTO,
    AnalisisOutputDTO,
    ConsultaAiInputDTO,
    ConsultaAiOutputDTO
)

# ==========================================
# 1. FUNCIÓN DE ANÁLISIS FINANCIERO PROFUNDO
# ==========================================
def analizar_perfil_financiero(data: AnalisisInputDTO) -> AnalisisOutputDTO:
    # 1. Obtener y limpiar la API Key principal (Groq)
    raw_key = os.getenv("GROQ_API_KEY")
    api_key = raw_key.strip() if raw_key else None

    if not api_key:
        print("\n[AVISO]: No se encontró GROQ_API_KEY, intentando pasar directamente al respaldo...\n")

    # 2. Formatear el historial de transacciones para el prompt
    transacciones_texto = "\n".join([
        f"- Tipo: {t.tipo}, Categoría: {t.categoria}, Monto: ${t.monto}, Descripción: {t.descripcion}"
        for t in data.historial_transacciones
    ]) if data.historial_transacciones else "Sin transacciones registradas"

    prompt = f"""
                 Eres un experto asesor financiero y analizador de datos.
                 Analiza los siguientes datos del usuario:
                 - Ingreso mensual: ${data.ingreso_mensual}
                 - Ahorro actual: ${data.ahorro_actual}
                 - Nivel de endeudamiento (0-100): {data.nivel_endeudamiento}
                 - Frecuencia de ahorro: {data.frecuencia_ahorro}
                 - Meta/Descripción del análisis: {data.descripcion}
                 - Valor objetivo de la meta: ${data.valor}
                 - Historial de transacciones:
                 {transacciones_texto}

                 Calcula las métricas financieras (total_gastado, capacidad_ahorro_mensual, porcentaje_tasa_ahorro, progreso_meta_ahorro, meses_para_meta).
                 OBLIGATORIO: Genera EXACTAMENTE un array de 3 (tres) recomendaciones financieras prácticas y útiles basadas estrictamente en estos números dentro del campo "recomendaciones". No agregues más de 3 elementos en la lista.

                 Responde ÚNICAMENTE con un JSON válido que cumpla con esta estructura exacta:
                 {{
                     "perfil_financiero": "SALUDABLE",
                     "probabilidad": 0.85,
                     "resumen_gastos": {{
                         "Alimentacion": 8000.0,
                         "Transporte": 3000.0
                     }},
                     "recomendaciones": [
                         "Recomendación 1 aquí.",
                         "Recomendación 2 aquí.",
                         "Recomendación 3 aquí."
                     ],
                     "total_gastado": 11000.0,
                     "capacidad_ahorro_mensual": 150.0,
                     "porcentaje_tasa_ahorro": 25.5,
                     "progreso_meta_ahorro": 10.0,
                     "meses_para_meta": 12.0
                 }}
                 """

    # 3. Intento Principal con Groq
    if api_key:
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
                model="openai/gpt-oss-120b",
                response_format={"type": "json_object"}
            )

            contenido_json = chat_completion.choices[0].message.content
            print(f"\n JSON CRUDO DE LA IA (ANÁLISIS):\n{contenido_json}\n")
            return AnalisisOutputDTO.model_validate_json(contenido_json)

        except Exception as e:
            print(f"\n[GROQ ERROR]: {e}. Cambiando a IA de respaldo (Mistral vía urllib)...\n")

    # 4. Intento de Respaldo con Mistral AI usando urllib
    try:
        raw_mistral_key = os.getenv("MISTRAL_API_KEY")
        mistral_key = raw_mistral_key.strip() if raw_mistral_key else None

        if mistral_key:
            url = "https://api.mistral.ai/v1/chat/completions"
            payload = {
                "model": "mistral-large-latest",
                "messages": [
                    {
                        "role": "system",
                        "content": "Eres un asistente financiero experto que responde exclusivamente en formato JSON estructurado válido."
                    },
                    {
                        "role": "user",
                        "content": prompt,
                    }
                ],
                "response_format": {"type": "json_object"}
            }

            data_bytes = json.dumps(payload).encode("utf-8")
            req = urllib.request.Request(
                url,
                data=data_bytes,
                headers={
                    "Authorization": f"Bearer {mistral_key}",
                    "Content-Type": "application/json"
                },
                method="POST"
            )

            with urllib.request.urlopen(req, timeout=30) as response:
                response_body = response.read().decode("utf-8")
                data_resp = json.loads(response_body)
                contenido_json = data_resp["choices"][0]["message"]["content"]
                print(f"\n JSON CRUDO MISTRAL (ANÁLISIS):\n{contenido_json}\n")
                return AnalisisOutputDTO.model_validate_json(contenido_json)
        else:
            print("\n[MISTRAL AVISO]: No se configuró MISTRAL_API_KEY en el entorno.\n")

    except Exception as e_mistral:
        print(f"\n[MISTRAL ERROR]: {e_mistral}\n")

    # 5. Respuesta de emergencia si fallan ambas IA
    return AnalisisOutputDTO(
        perfil_financiero="EN OBSERVACION",
        probabilidad=0.0,
        resumen_gastos={},
        recomendaciones=["Servicio en modo degradado: Fallaron los proveedores de IA principales y de respaldo."],
        total_gastado=0.0,
        capacidad_ahorro_mensual=0.0,
        porcentaje_tasa_ahorro=0.0,
        progreso_meta_ahorro=0.0,
        meses_para_meta=0.0
    )
# ==========================================
# 2. FUNCIÓN DE CONSULTA RÁPIDA (DESDE EL HEADER)
# ==========================================
def procesar_consulta_ai(data: ConsultaAiInputDTO) -> ConsultaAiOutputDTO:
    """
    Versión híbrida: plantillas predefinidas dinámicas para temas comunes
    y respaldo con IA para consultas complejas.
    """
    query_lower = data.query.lower()
    respuesta_texto = ""

    # Detección dinámica del nombre del usuario (busca en el DTO o usa un término amigable)
    nombre_usuario = getattr(data, "nombre", None) or getattr(data, "usuario", None) or "amigo/a"

    # ==========================================
    # PLANTILLAS POR TIPO DE CONSULTA (SIN IA)
    # ==========================================
    # AHORRO / METAS / OBJETIVOS
    if any(k in query_lower for k in ["ahorr", "meta", "objetivo", "3.000.000", "3 millones", "6 meses"]):
        respuesta_texto = (
            f"¡Hola, {nombre_usuario}! Para llegar a 3.000.000 en 6 meses, necesitas ahorrar 500.000 por mes.\n\n"
            "1. Genera ingresos extra: freelance o venta de cosas (+300.000).\n"
            "2. Recorta gastos: cancela suscripciones y negocia servicios (+150.000).\n"
            "3. Automatiza: transfiere el ahorro el mismo día que cobres.\n\n"
            "¿Quieres ayuda con un plan semanal?"
        )
    # GASTOS / RECORTAR
    elif any(k in query_lower for k in ["gasto", "gastar", "recortar", "ahorrar gastos"]):
        respuesta_texto = (
            f"¡Hola, {nombre_usuario}! Para reducir gastos y ahorrar más:\n\n"
            "1. Cancela suscripciones que no uses (streaming, apps).\n"
            "2. Negocia servicios (internet, seguros, teléfono).\n"
            "3. Planifica comidas y compra al por mayor.\n\n"
            "¿Quieres que revisemos tus gastos de vivienda o transporte?"
        )
    # INGRESOS / GANAR / EXTRA
    elif any(k in query_lower for k in ["ingreso", "ganar", "extra", "freelance", "trabajo"]):
        respuesta_texto = (
            f"¡Hola, {nombre_usuario}! Para aumentar ingresos rápidamente:\n\n"
            "1. Ofrece freelance con tus habilidades (diseño, programación).\n"
            "2. Vende artículos que no uses (tecnología, ropa).\n"
            "3. Busca horas extra o trabajos temporales.\n\n"
            "¿Quieres ideas específicas para tu perfil?"
        )
    # INVERSIÓN / RENDIMIENTO / PLAZO FIJO
    elif any(k in query_lower for k in ["invertir", "inversión", "rendimiento", "plazo fijo", "interés"]):
        respuesta_texto = (
            f"¡Hola, {nombre_usuario}! Para hacer rendir tu ahorro en 6 meses:\n\n"
            "1. Usa cuentas de alta rentabilidad (2-3% anual).\n"
            "2. Considera plazos fijos a 3-6 meses (4-5% anual).\n"
            "3. Evita inversiones volátiles (acciones, cripto).\n\n"
            "¿Quieres que comparemos opciones seguras?"
        )
    # PRESUPUESTO / PLAN / ORGANIZAR
    elif any(k in query_lower for k in ["presupuesto", "plan", "organizar", "tabla", "calcular"]):
        respuesta_texto = (
            f"¡Hola, {nombre_usuario}! Para armar un presupuesto efectivo:\n\n"
            "1. Registra todos tus ingresos y gastos semanales.\n"
            "2. Asigna un límite a cada categoría (comida, transporte, ocio).\n"
            "3. Revisa y ajusta cada viernes.\n\n"
            "¿Quieres una plantilla de presupuesto semanal?"
        )
    # DEUDAS / PRÉSTAMOS
    elif any(k in query_lower for k in ["deuda", "préstamo", "tarjeta", "credito"]):
        respuesta_texto = (
            f"¡Hola, {nombre_usuario}! Para reducir deudas rápidamente:\n\n"
            "1. Prioriza la deuda con mayor interés (método avalanche).\n"
            "2. Negocia tasas más bajas con tus acreedores.\n"
            "3. Usa ingresos extra para abonos adicionales.\n\n"
            "¿Quieres un plan de pago personalizado?"
        )
    # EMERGENCIA / FONDO / RESERVA
    elif any(k in query_lower for k in ["emergencia", "fondo", "reserva", "imprevisto"]):
        respuesta_texto = (
            f"¡Hola, {nombre_usuario}! Para armar un fondo de emergencia:\n\n"
            "1. Apunta a 3-6 meses de gastos básicos.\n"
            "2. Usa una cuenta separada de fácil acceso.\n"
            "3. Automatiza transferencias mensuales pequeñas.\n\n"
            "¿Quieres calcular cuánto necesitas para tu fondo?"
        )
    # ==========================================
    # FALLBACK CON IA (para consultas complejas no cubiertas)
    # ==========================================
    else:
        raw_key = os.getenv("GROQ_API_KEY")
        api_key = raw_key.strip() if raw_key else None

        fallback_generico = (
            f"¡Hola, {nombre_usuario}! Cuéntame más sobre tu objetivo financiero.\n\n"
            "Puedo ayudarte con:\n"
            "1. Planes de ahorro personalizados.\n"
            "2. Ideas para reducir gastos.\n"
            "3. Estrategias para aumentar ingresos.\n\n"
            "¿Qué te gustaría trabajar primero?"
        )

        if api_key:
            try:
                client = Groq(api_key=api_key)
                system_prompt = (
                    f"Responde saludando a {nombre_usuario} y usa SOLO 3-4 líneas de texto plano.\n"
                    "Prohibido: tablas (|), títulos (#), negritas (**), emojis.\n"
                    "Sé amigable y directo en PESOS argentinos. Máximo 200 caracteres."
                )

                chat_completion = client.chat.completions.create(
                    messages=[
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": f"Consulta: {data.query}"}
                    ],
                    model="llama-3.3-70b-versatile",
                    max_tokens=80,
                    temperature=0.2
                )

                respuesta_ia = chat_completion.choices[0].message.content.strip()

                # Validar que no tenga formato prohibido
                if not any(p in respuesta_ia for p in ["|", "#", "**", "---"]) and len(respuesta_ia) < 250:
                    respuesta_texto = respuesta_ia
                else:
                    respuesta_texto = fallback_generico
            except Exception:
                respuesta_texto = fallback_generico
        else:
            respuesta_texto = fallback_generico

    return ConsultaAiOutputDTO(respuesta=respuesta_texto)