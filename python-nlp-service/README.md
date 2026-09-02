# Python NLP Service - Análisis Financiero Inteligente

Microservicio desarrollado en **Python** con **FastAPI** encargado de procesar datos financieros e interactuar con modelos de lenguaje de gran escala (LLM) a través de la plataforma **Groq** para generar diagnósticos, niveles de riesgo y recomendaciones automatizadas.

---

## 🛠️ Tecnologías y Herramientas

- **Lenguaje:** Python 3.13
- **Framework Web:** FastAPI + Uvicorn
- **Validación de Datos:** Pydantic v2
- **Motor de IA:** Groq SDK (`llama-3.3-70b-versatile`)
- **Gestión de Entorno:** `python-dotenv`
- **Contenerización:** Docker

---

## 📁 Estructura del Proyecto


```text
python-nlp-service/
├── app/
│   ├── api/             # Controladores y endpoints
│   ├── schemas/         # DTOs y validaciones de Pydantic
│   └── services/        # Lógica de negocio e integración con Groq
├── .dockerignore        # Archivos excluidos del contexto de Docker
├── .env                 # Variables de entorno (API Keys / Configuración)
├── .gitignore           # Archivos e historial excluidos de Git
├── Dockerfile           # Instrucciones para la construcción del contenedor
├── main.py              # Punto de entrada de la aplicación FastAPI
└── requirements.txt     # Dependencias del proyecto
```

🔑 Configuración del Entorno (.env)
Crea un archivo **.env** en la raíz del proyecto con la siguiente estructura (nunca incluir este archivo en el control de versiones):

````
GROQ_API_KEY=gsk_tu_clave_api_de_groq_aqui
````


🚀 Ejecución en Desarrollo (Local)
1. Crear y activar el entorno virtual

```bash
python -m venv .venv
# En Windows (PowerShell / CMD):
.venv\Scripts\activate
```
2. Instalar dependencias

```bash
pip install -r requirements.txt
```

3. Iniciar el servidor con Hot-Reload

```bash
uvicorn main:app --host 127.0.0.1 --port 8000 --reload
```

Acceso a la documentación interactiva (Swagger UI):
👉 **http://127.0.0.1:8000/docs**

🐳 Ejecución con Docker
1. Construir la imagen de Docker

```bash
docker build -t python-nlp-service .
```

2. Ejecutar el contenedor
Si utilizas un puerto mapeado alternativo (por ejemplo **8000** para evitar conflictos de puerto):

```bash

docker run -d -p 8005:8000 --env-file .env --name nlp-container python-nlp-service
```

Acceso a la API en Docker:
👉 **http://127.0.0.1:8005/docs**

## 📌 Endpoints de la API
1. Verificación de Estado
* **GET /health**

* Respuesta: **{"status": "ok", "service": "python-nlp"}**

2. Análisis de Perfil Financiero
* **POST /api/v1/analizar-perfil**

* Estructura de Entrada **(AnalisisInputDTO)**:

```json
{
  "usuario_id": 1,
  "ingresos_totales": 850000.00,
  "transacciones": [
    {
      "categoria": "Entretenimiento",
      "monto": 120000.00,
      "descripcion": "Restaurantes y salidas"
    }
  ]
}

```

* Estructura de Salida **(AnalisisOutputDTO)**:

```json
{
  "diagnostico": "El usuario presenta un nivel de gasto...",
  "nivel_riesgo": "BAJO",
  "recomendaciones": [
    "Establecer un límite mensual...",
    "Destinar un porcentaje fijo a ahorro..."
  ]
}

```
___

## 🗺️ Hoja de Ruta: Integración FastAPI + Backend

```
[ Frontend / App ] ──► [ Spring Boot Backend ] ──(WebClient / REST)──► [ FastAPI (Docker / OCI) ] ──► [ Groq AI ]
```

1. Definir la infraestructura de red (Dónde correrá el contenedor)
Antes de programar en Java, necesitás saber cómo se van a ver ambos servicios:

* En desarrollo local:

* Si ejecutas Spring Boot localmente y FastAPI en Docker (puerto 8000), la URL base del microservicio será:
 
**http://localhost:8000** (o **[http://host.docker.internal:8000]** **(http://host.docker.internal:8000)** si Spring Boot también corre en Docker).

* En producción (ej. OCI / Ubuntu server):

* Desplegar el contenedor de FastAPI en el servidor y, si usás Nginx como Reverse Proxy, exponer el endpoint o mantenerlo en una red interna privada de Docker (**docker-network**) para que solo Spring Boot pueda pegarle por HTTP.

2. Configurar la comunicación en Spring Boot
En tu proyecto backend Java (Spring Boot 3), la forma más eficiente y moderna de consumir este microservicio es con **WebClient** (Spring WebFlux):

A. Agregar la propiedad en **application.yml** / **application.properties**

```
YAML
app:
  services:
    nlp-ai:
      base-url: ${NLP_SERVICE_URL:http://localhost:8000}
```
B. Crear los DTOs de correspondencia en Java
Deben matchear exactamente los esquemas JSON de tu FastAPI:

```java
// Request DTO
public record AnalisisInputDTO(
    Long usuarioId,
    BigDecimal ingresosTotales,
    List<TransaccionDTO> transacciones
) {}

// Response DTO
public record AnalisisOutputDTO(
    String diagnostico,
    String nivelRiesgo,
    List<String> recomendaciones
) {}
```

C. Implementar el cliente REST con **WebClient**

```java
@Service
public class NlpClientService {

    private final WebClient webClient;

    public NlpClientService(WebClient.Builder builder, @Value("${app.services.nlp-ai.base-url}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    public Mono<AnalisisOutputDTO> obtenerAnalisisFinanciero(AnalisisInputDTO request) {
        return this.webClient.post()
                .uri("/api/v1/analizar-perfil")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AnalisisOutputDTO.class);
    }
}
```


3. Orquestar la Lógica de Negocio y Resiliencia
Para que la experiencia del usuario sea fluida y robusta:

* Manejo de Tiempos de Espera (Timeouts): Configurar un timeout razonable en el **WebClient** (ej. 5 a 10 segundos) por si el servicio de IA demora en responder o sufre latencia.

* Fallback / Tolerancia a fallos: Si el servicio de Python o la API de Groq no responde, implementar un mecanismo de fallback (por ejemplo con Resilience4j) que devuelva un diagnóstico genérico sin hacer caer la petición del usuario.

* Procesamiento Asíncrono / Reactivo: Dado que la llamada a la IA toma unos milisegundos extra, podés manejar la llamada de forma asíncrona o guardarla en base de datos para notificar al usuario cuando el reporte esté listo.


4. Pruebas de Integración (End-to-End)
Levantar el contenedor **nlp-container**.

Iniciar la aplicación Spring Boot.

Enviar una petición a Spring Boot desde Postman/Swagger y verificar que la respuesta incluya el objeto **AnalisisOutputDTO** generado por Llama 3.3.
