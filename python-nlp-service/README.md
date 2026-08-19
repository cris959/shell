# Python NLP Service - Análisis Financiero Inteligente

![FastAPI](https://img.shields.io/badge/FastAPI-005571?style=for-the-badge&logo=fastapi)
![Python](https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Groq](https://img.shields.io/badge/Groq-F05032?style=for-the-badge&logo=git&logoColor=white)
![Mistral AI](https://img.shields.io/badge/Mistral_AI-FF7000?style=for-the-badge)

Microservicio desarrollado en **Python** con **FastAPI** encargado de procesar datos financieros e interactuar con modelos de lenguaje de gran escala (LLM) a través de la plataforma **Groq** para generar diagnósticos, niveles de riesgo y recomendaciones automatizadas.

---

## 🛠️ Tecnologías y Herramientas

- **Lenguaje:** Python 3.13
- **Framework Web:** FastAPI + Uvicorn
- **Validación de Datos:** Pydantic v2 (estricta compatibilidad `snake_case`)
- **Entrenamiento y Experimentación:** Google Colab / Jupyter Notebooks (`.ipynb`) integrados en VS Code
- **Modelos de IA:**
- **Groq SDK:** Modelo Entrenado / Fine-Tuned *(Proveedor Primario)*
- **Mistral AI SDK:** `mistral-small-latest` *(Proveedor Secundario / Fallback)*
- **Contenedores y Orquestación:** Docker & Docker Compose

---

## 🔄 Diagrama de Resiliencia



```mermaid
%%{init: {'theme': 'base', 'themeVariables': { 'primaryColor': '#007bff', 'edgeLabelBackground':'#ffffff', 'tertiaryColor': '#fff'}}}%%
graph TD
    %% Definición de Estilos
    classDef api fill:#f9f,stroke:#333,stroke-width:2px,color:black;
    classDef logic fill:#e1f5fe,stroke:#0277bd,stroke-width:1px,color:black;
    classDef success fill:#c8e6c9,stroke:#2e7d32,stroke-width:2px,color:black;
    classDef fail fill:#ffcdd2,stroke:#c62828,stroke-width:2px,color:black;
    classDef critical fill:#d32f2f,stroke:#333,stroke-width:2px,color:white;

    %% Nodos Principales
    Start(<strong>Inicio:</strong> POST /analizar-perfil):::api
    ProcessInput[Validar Entrada con Pydantic v2<br/>AnalisisInputDTO - snake_case]:::logic
    BuildPrompt[Construir Prompt Financiero Dinámico]:::logic

    %% Intento Primario (Groq - Modelo Entrenado)
    subgraph TryGroq [Intento Primario: Groq API]
        GroqCall[Llamar a Groq <br/> <strong>Modelo Entrenado / Fine-Tuned</strong>]:::logic
        CheckGroq{¿Petición Exitosa?}:::logic
    end

    %% Salida Exitosa
    ParseOutput(Parsear y serializar en snake_case <br/> <strong>AnalisisOutputDTO</strong>):::success
    End(<strong>Fin:</strong> Respuesta HTTP 200 OK para Spring Boot):::api

    %% Intento Secundario (Mistral AI)
    subgraph TryMistral [Intento Secundario: Mistral AI - Fallback]
        LogErrorGroq[Loguear Error: Groq Falló]:::fail
        MistralCall[Llamar a Mistral AI <br/> Modelo: open-mistral-7b]:::logic
        CheckMistral{¿Petición Exitosa?}:::logic
    end

    %% Fallo Crítico
    LogErrorMistral[Loguear Error Crítico: Mistral Falló]:::fail
    RaiseError(Lanzar RuntimeError <br/> HTTP 500: Fallo Crítico):::critical

    %% Conexiones y Flujo
    Start --> ProcessInput
    ProcessInput --> BuildPrompt
    BuildPrompt --> GroqCall
    GroqCall --> CheckGroq

    %% Caminos Groq
    CheckGroq -- "Sí (OK)" --> ParseOutput
    CheckGroq -- "No (Fallo/Timeout)" --> LogErrorGroq

    %% Caminos Mistral
    LogErrorGroq --> MistralCall
    MistralCall --> CheckMistral
    CheckMistral -- "Sí (OK)" --> ParseOutput
    CheckMistral -- "No (Fallo/Timeout)" --> LogErrorMistral
    LogErrorMistral --> RaiseError

    %% Conexión a Fin
    ParseOutput --> End


```


## 📁 Estructura del Proyecto


```text
python-nlp-service/
├── app/
│   ├── api/                     # Endpoints y definición de rutas (router.py)
│   ├── models/                  # Modelos de dominio e indicadores financieros
│   ├── schemas/                 # DTOs y contratos Pydantic v2 (analisis.py)
│   └── services/                # Lógica de IA, prompt engineering y fallback (nlp_service.py)
├── notebooks/                   # Entrenamiento y experimentación del modelo
│   ├── raw_data/                # Datos fuentes / sin procesar
│   ├── df_modelo_pf.csv         # Dataset estructurado para el modelo
│   └── entrenamiento_mvp.ipynb  # Notebook de fine-tuning / entrenamiento
├── .env                         # Variables de entorno y llaves de API
├── Dockerfile                   # Configuración del contenedor Docker
├── main.py                      # Punto de entrada de FastAPI
└── requirements.txt
```

🔑 Configuración del Entorno (.env)
Crea un archivo **.env** en la raíz del proyecto con la siguiente estructura (nunca incluir este archivo en el control de versiones):

````
GROQ_API_KEY=gsk_tu_clave_api_de_groq_aqui
MISTRAL_API_KEY=gsk_tu_clave_api_de_groq_aqui
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

___

## 📚 Documentación Interactiva

Una vez iniciado el servicio, puedes acceder a la interfaz Swagger UI para probar endpoints interactivamente:
👉 **http://127.0.0.1:8000/docs**

---

## 🐳 Ejecución con Docker

1. **Construir la imagen de Docker**

```bash
docker build -t python-nlp-service .
```

1- Ejecutar el contenedor

Mapeando el puerto 8000 y pasando el archivo de variables de entorno .env:

```bash
docker run -d -p 8000:8000 --env-file .env --name nlp-container python-nlp-service
```

2- Acceso a la API dentro del contenedor

👉 http://127.0.0.1:8000/docs

___


## 📌 Endpoints de la API

### 1. Verificación de Estado
- **GET** `/health`

**Respuesta HTTP 200 OK:**
```json
{
  "status": "ok",
  "service": "python-nlp"
}
```
2. Análisis de Perfil Financiero
POST **/analizar-perfil**

Estructura de Entrada (**AnalisisInputDTO**) — JSON en **snake_case**:

```json
{
  "usuario_id": "USR-1001",
  "ingreso_mensual": 650000.0,
  "frecuencia_ahorro": "MENSUAL",
  "descripcion": "Supermercado Coto compras semana y pago de suscripciones",
  "valor": 42500.0,
  "ahorro_actual": 120000.0,
  "meta_ahorro": 500000.0,
  "monto_proxima_meta": 100000.0,
  "historial_transacciones": [
    {
      "monto": 15000.0,
      "descripcion": "Combustible",
      "categoria": "Transporte"
    }
  ]
}
```
Estructura de Salida (**AnalisisOutputDTO**) — JSON en **snake_case**:

```json
{
  "perfil_financiero": "Conservador",
  "probabilidad": 0.85,
  "resumen_gastos": {
    "Alimentacion": 42500.0,
    "Transporte": 15000.0
  },
  "recomendaciones": [
    "Mantener el nivel de gasto controlado en supermercado.",
    "Destinar el excedente mensual directamente a la meta de ahorro."
  ],
  "total_gastado": 57500.0,
  "capacidad_ahorro_mensual": 592500.0,
  "porcentaje_tasa_ahorro": 91.15,
  "progreso_meta_ahorro": 24.0,
  "meses_para_meta": 0.64
}
```
## Imagen de swagger


![Vista de Swagger](assets//imagen-data.png)
___

## 🗺️ Hoja de Ruta: Integración FastAPI + Backend

```mermaid

%%{init: {'theme': 'base', 'themeVariables': { 'primaryColor': '#007bff', 'edgeLabelBackground':'#ffffff', 'tertiaryColor': '#fff'}}}%%
flowchart LR
    A[Frontend / App Client] -->|HTTP POST| B[Spring Boot Backend]
    B -->|WebClient / REST snake_case| C[FastAPI Service Docker / OCI]
    C -->|API Request| D[Groq AI Modelo Entrenado]
    C -.->|Fallback si Groq falla| E[Mistral AI Fallback]
    D -->|JSON Output| C
    E -->|JSON Output| C
    C -->|AnalisisOutputDTO| B
    B -->|Respuesta HTTP| A
```
___

1. Definir la infraestructura de red (Dónde correrá el contenedor)
Antes de programar en Java, necesitás saber cómo se van a ver ambos servicios:

* En desarrollo local:

* Si ejecutas Spring Boot localmente y FastAPI en Docker (puerto 8000), la URL base del microservicio será:
 
**http://localhost:8000** (o **[http://host.docker.internal:8000]** **(http://host.docker.internal:8000)** si Spring Boot también corre en Docker).

* En producción (ej. OCI / Ubuntu server):

* Desplegar el contenedor de FastAPI en el servidor y, si usás Nginx como Reverse Proxy, exponer el endpoint o mantenerlo en una red interna privada de Docker (**docker-network**) para que solo Spring Boot pueda pegarle por HTTP.

2. Configurar la comunicación en Spring Boot
En tu proyecto backend Java (Spring Boot 3), la forma más eficiente y moderna de consumir este microservicio es con **WebClient** (Spring WebFlux):


___

## Distribución de responsabilidades
FastAPI (Python - **http://localhost:8000/api/v1/analizar-perfil**):

* Procesa y agrupa el historial de transacciones (Pandas + NLP).

* Evalúa métricas numéricas y ejecuta el modelo de Random Forest.

* Llama a los modelos de lenguaje (Groq / Mistral) para redactar las recomendaciones personalizadas.

* Devuelve la respuesta procesada en JSON.

* Spring Boot (Java - Backend principal):

* Gestión de entidades y persistencia: Guardar usuarios, transacciones, metas de ahorro y presupuestos en la base de datos (PostgreSQL/MySQL).

* Procesamiento de archivos: Lectura e ingesta inicial de archivos CSV.

* Seguridad y autenticación: Manejo de JWT, Roles, Login y Registro.

* Orquestación: Recibir la petición del cliente HTTP/Frontend, invocar internamente al microservicio de Python mediante **WebClient/RestTemplate**, aplicar fallbacks en caso de error y retornar la respuesta final unificada.

## Arquitectura de comunicación

````plaintext
[ Cliente / Frontend / Postman ]
               │
               ▼
   [ Java Spring Boot Backend ]  <--->  [ Base de Datos ]
               │
   (Llamada interna WebClient/Feign)
               │
               ▼
 [ Python FastAPI Microservicio ] ───> [ Groq / Mistral AI ]
````

**Tu backend Java actúa como el Gateway y Orquestador principal, mientras que Python es un microservicio de IA especializado que responde bajo demanda.**