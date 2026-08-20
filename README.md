## 🚀 Backend - Sistema de Gestión y Salud Financiera con IA (Team 17)
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger-OpenAPI_3.0-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)
![Postman](https://img.shields.io/badge/Postman-FF6C37?style=for-the-badge&logo=postman&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Version](https://img.shields.io/badge/Version-v1.0.0-blue?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

API RESTful desarrollada en Java con **Spring Boot 3**, diseñada para la gestión de transacciones financieras personales, ingestión masiva de datos mediante archivos CSV y evaluación automatizada de perfiles de riesgo respaldada por Inteligencia Artificial y Machine Learning.

El sistema funciona como un orquestador central: gestiona la persistencia de datos, aplica reglas de negocio, centraliza el manejo global de excepciones y se comunica de manera transparente con un microservicio analítico en Python (FastAPI) que ejecuta modelos de NLP y Random Forest.

## 🏗️ Arquitectura del Sistema
El ecosistema está construido bajo una arquitectura de microservicios contenida en Docker:

````mermaid
flowchart TD
    Client["Cliente / Frontend / Postman"]
    
    subgraph DockerContainer ["Entorno Docker Compose"]
        SpringBoot["Spring Boot API Gateway / Backend<br/>(Puerto 8008)"]
        PostgresDB[("PostgreSQL 16<br/>(Puerto 5432)")]
        PythonNLP["Python FastAPI / NLP & ML<br/>(Puerto 8000)"]
    end

    Client -->|Peticiones HTTP / Rest| SpringBoot
    SpringBoot -->|JPA / Hibernate| PostgresDB
    SpringBoot <-->|JSON / RestClient| PythonNLP
````
## 🧰 Tecnologías Utilizadas
* Lenguaje & Framework: Java 21 / Spring Boot 3.x

* Persistencia & Datos: Spring Data JPA, Hibernate, PostgreSQL 16

* Documentación de API: OpenAPI 3 / Swagger UI (**/swagger-ui.html**)

* Integración IA/ML: FastAPI (Python), Pandas, Scikit-Learn (Random Forest)

* Contenedores: Docker & Docker Compose

* Pruebas de API: Postman Collection v2.1 (**Team17.postman_collection.json**)

## ⚡ Guía de Inicio Rápido (Despliegue con Docker)
Requisitos Previos
* Docker Desktop instalado y en ejecución.
* Git.

Pasos para levantar la aplicación
1. Clonar el repositorio:

````bash
git clone https://github.com/TuUsuario/HackatonG9-LATAM-Team17.git
cd HackatonG9-LATAM-Team17
````

2. Desplegar los contenedores con Docker Compose:

````bash
docker compose up --build -d
````
3. Verificar estado de los servicios:

````bash
docker compose ps
````
4. Acceso a las aplicaciones:

► Spring Boot API Base: **http://localhost:8008**

► Swagger UI Documentation: **http://localhost:8008/swagger-ui.html**

► Python NLP Service (Swagger): **http://localhost:8000/docs**

► PostgreSQL: **localhost:5432** 
## 📡 Endpoints del AnalisisController
Base Path: **/api/v1/analisis**

1. Procesar Análisis Financiero con IA
   Envía los datos financieros del usuario para que el microservicio en Python genere el perfil y análisis correspondiente.

► URL: **/api/v1/analisis/procesar**

► Método: **POST**

► Headers: **Content-Type: application/json**

► Códigos de Éxito: **201 Created**

Cuerpo de la Petición (**AnalisisInputDTO**)

````json
{
  "usuarioId": "USR-1001",
  "transacciones": [
    {
      "descripcion": "Supermercado compras de la semana",
      "monto": 42500.00,
      "tipo": "EGRESO",
      "categoria": "Alimentación"
    },
    {
      "descripcion": "Cobro de sueldo",
      "monto": 650000.00,
      "tipo": "INGRESO",
      "categoria": "Salario"
    }
  ]
}
````
Respuesta Exitosa (**AnalisisOutputDTO** - HTTP 201 Created)

````json
{
  "id": 1,
  "usuarioId": "USR-1001",
  "perfilFinanciero": "CONSERVADOR",
  "nivelRiesgo": "BAJO",
  "recomendaciones": [
    "Mantener un fondo de emergencia equivalente a 3 meses de gastos.",
    "Considerar instrumentos de bajo riesgo como Plazo Fijo o FCI."
  ],
  "fechaAnalisis": "2026-07-31T10:39:00"
}
````
Posibles Errores

| Código HTTP               | Excepción                       | DTO devuelto          | Descripción                                                     | Ejemplo                                                                                                                                                                                                                                |
| ------------------------- | ------------------------------- | --------------------- | --------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 400 Bad Request           | MethodArgumentNotValidException | ErrorResponseDTO      | Errores de validación en campos requeridos o reglas de negocio. | json<br>{<br>  "timestamp": "2026-07-31T11:00:00",<br>  "status": 400,<br>  "error": "Bad Request",<br>  "message": "Errores de validación",<br>  "details": {<br>    "transacciones": "La lista no puede estar vacía"<br>  }<br>}<br> |
| 502 Bad Gateway           | HttpStatusCodeException         | PythonServiceErrorDTO | El microservicio de Python retornó un error 4xx o 5xx.          | json<br>{<br>  "timestamp": "2026-07-31T11:15:00",<br>  "status": 502,<br>  "error": "Bad Gateway",<br>  "message": "El servicio de Python retornó un error"<br>}<br>                                                                  |
| 503 Service Unavailable   | ResourceAccessException         | PythonServiceErrorDTO | No se pudo establecer conexión con el servicio de Python.       | json<br>{<br>  "timestamp": "2026-07-31T11:20:00",<br>  "status": 503,<br>  "error": "Service Unavailable",<br>  "message": "No se pudo conectar con el servicio de análisis"<br>}<br>                                                 |
| 504 Gateway Timeout       | TimeoutException                | AIServiceErrorDTO     | El servicio de IA tardó demasiado en responder.                 | json<br>{<br>  "timestamp": "2026-07-31T11:25:00",<br>  "status": 504,<br>  "error": "Gateway Timeout",<br>  "message": "El servicio de IA tardó demasiado en responder"<br>}<br>                                                      |
| 500 Internal Server Error | Exception                       | ErrorResponseDTO      | Error interno no controlado del servidor.                       | json<br>{<br>  "timestamp": "2026-07-31T11:35:00",<br>  "status": 500,<br>  "error": "Internal Server Error",<br>  "message": "Error interno del servidor"<br>}<br>                                                                    |

2. Registrar Transacción Individual
Permite la carga puntual de una transacción en la base de datos.

► URL: **/api/v1/analisis/transacciones**

► Método: **POST**

► Headers: **Content-Type: application/json**

► Códigos de Éxito: **201 Created**

Payload de Entrada (**TransaccionDTO**)

````json
{
  "descripcion": "Pago de servicio de luz",
  "monto": 24500.00,
  "tipo": "EGRESO",
  "categoria": "Servicios"
}
````
Respuesta Exitosa (**TransaccionResponseDTO** - HTTP 201 Created)

````json
{
  "id": 3,
  "usuarioId": "USR-DEFAULT",
  "monto": 24500.00,
  "tipo": "EGRESO",
  "descripcion": "Pago de servicio de luz",
  "categoria": "Servicios",
  "fechaTransaccion": "2026-07-31T10:52:00"
}
````
3. Obtener Todas las Transacciones
Devuelve la lista global de todas las transacciones almacenadas en la base de datos.

► URL: **/api/v1/analisis/transacciones**

► Método: **GET**

► Códigos de Éxito: **200 OK**

4. Obtener Transacciones por Usuario
Consulta el historial de transacciones asociadas a un identificador de usuario específico.

► URL: **/api/v1/analisis/transacciones/usuario/{usuarioId}**

► Método: **GET**

► Parámetros de Ruta: **usuarioId** (String, requerido) -> Ej. **USR-1001**

► Códigos de Éxito: **200 OK**, **204 No Content**

## 📄 Resumen de Referencia Rápida de Endpoints
🟢 Análisis Financiero (**/api/v1/analisis**)

| Método | Ruta RESTful                         | Descripción                                                                        | Payload de la solicitud                                     | Respuesta               | Estado exitoso          | Errores comunes    |
| ------ | ------------------------------------ | ---------------------------------------------------------------------------------- | ----------------------------------------------------------- | ----------------------- | ----------------------- | ------------------ |
| POST   | /api/v1/analisis/perfil/{usuarioId}  | Generar un análisis financiero mediante inferencia de IA.                          | @RequestBody AnalisisInputDTO                               | AnalisisOutputDTO       | 201 Created             | 400, 502, 503, 504 |
| POST   | /api/v1/analisis/csv/{usuarioId}     | Realizar una carga masiva desde un archivo CSV y ejecutar el análisis mediante IA. | MultipartFile + @ModelAttribute                             | AnalisisOutputDTO       | 200 OK                  | 400, 500           |
| GET    | /api/v1/analisis/usuario/{usuarioId} | Obtener el historial de análisis de un usuario.                                    | No requiere cuerpo; recibe usuarioId como variable de ruta. | List<AnalisisOutputDTO> | 200 OK / 204 No Content | 500                |

🔵 Transacciones (**/api/v1/transacciones**)

| Método | Ruta RESTful                              | Descripción                                                        | Payload de la solicitud                                     | Respuesta                    | Estado exitoso          | Errores comunes |
| ------ | ----------------------------------------- | ------------------------------------------------------------------ | ----------------------------------------------------------- | ---------------------------- | ----------------------- | --------------- |
| POST   | /api/v1/transacciones                     | Registrar una nueva transacción individual.                        | @RequestBody TransaccionDTO                                 | TransaccionResponseDTO       | 201 Created             | 400, 409, 500   |
| GET    | /api/v1/transacciones/{id}                | Consultar el detalle de una transacción mediante su identificador. | No requiere cuerpo; recibe id como variable de ruta.        | TransaccionResponseDTO       | 200 OK                  | 404, 500        |
| GET    | /api/v1/transacciones/usuario/{usuarioId} | Listar las transacciones activas de un usuario.                    | No requiere cuerpo; recibe usuarioId como variable de ruta. | List<TransaccionResponseDTO> | 200 OK / 204 No Content | 500             |
| PUT    | /api/v1/transacciones/{id}                | Actualizar los datos de una transacción activa.                    | @RequestBody TransaccionDTO                                 | TransaccionResponseDTO       | 200 OK                  | 400, 404, 500   |
| DELETE | /api/v1/transacciones/{id}                | Realizar una eliminación lógica (soft delete) de una transacción.  | No requiere cuerpo; recibe id como variable de ruta.        | Sin contenido                | 204 No Content          | 404, 500        |

## 🌐 Configuración Centralizada de CORS (**CorsConfig**)

Se definió una configuración global para permitir las peticiones de orígenes cruzados de forma segura e interceptar todas las peticiones entrantes (/**).

► Ubicación: **saludfinanciera.finanzas.config.CorsConfig**

````java
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                        .allowedHeaders("*");
            }
        };
    }
}
````
## 🛡️ Arquitectura Global de Excepciones
Ubicación del paquete: **saludfinanciera.finanzas.exception**

El interceptor centralizado **@RestControllerAdvice** captura todas las excepciones lanzadas por el dominio o clientes externos y responde con DTOs estructurados.

Matriz de Cobertura de Errores

| Capa / origen                | Excepción interceptada                   | Código HTTP               | DTO devuelto          | Descripción                                                                                 |
| ---------------------------- | ---------------------------------------- | ------------------------- | --------------------- | ------------------------------------------------------------------------------------------- |
| Validación del DTO           | MethodArgumentNotValidException          | 400 Bad Request           | ErrorResponseDTO      | Detalle de los errores de validación por campo, como @NotNull y @Valid.                     |
| Recursos inexistentes        | ResourceNotFoundException                | 404 Not Found             | ErrorResponseDTO      | La entidad o el registro solicitado no fue encontrado.                                      |
| Persistencia / base de datos | DataIntegrityViolationException          | 409 Conflict              | DataErrorResponseDTO  | Violación de restricciones SQL, registros duplicados o conflictos con llaves únicas.        |
| Microservicio de Python      | HttpStatusCodeException                  | 502 Bad Gateway           | PythonServiceErrorDTO | El servicio de Python respondió con un error 4xx o 5xx.                                     |
| Red / conexión               | ResourceAccessException                  | 503 Service Unavailable   | PythonServiceErrorDTO | No fue posible establecer o mantener la conexión con el microservicio.                      |
| Tiempo de espera de IA       | TimeoutException, SocketTimeoutException | 504 Gateway Timeout       | AIServiceErrorDTO     | Se alcanzó el tiempo límite durante la inferencia del modelo de IA (isTimeout: true).       |
| Falla del motor de IA        | AIServiceUnavailableException            | 503 Service Unavailable   | AIServiceErrorDTO     | El motor o modelo de IA no se encuentra disponible o produjo un error durante la ejecución. |
| Error general                | Exception                                | 500 Internal Server Error | ErrorResponseDTO      | Error genérico no controlado por la aplicación.                                             |

## 📌 Integración con el Microservicio de IA (**POST /analizar-perfil**)
El microservicio Python expone un endpoint único basado en el Principio de Responsabilidad Única. El backend Spring Boot actúa como orquestador del flujo:

````mermaid
flowchart TD
    A[Cliente HTTP / Postman]
    B[Spring Boot<br/>Procesamiento y API]
    C[Python - IA<br/>NLP y Random Forest]
    D[Base de datos PostgreSQL]
    E[Respuesta al cliente]

    A -->|1. Envía datos JSON o CSV| B
    B -->|2. Persiste transacciones| D
    B -->|3. POST /analizar-perfil| C
    C -->|4. Devuelve métricas y recomendaciones| B
    B -->|5. Vincula resultados| D
    B -->|6. Responde al cliente| E
````

Flujo Interno en Python:
1. Categorización NLP: Asigna categorías (**ALIMENTACION**, **SERVICIOS**, etc.) a descripciones no clasificadas.

2. Cálculo Financiero: Determina total de egresos, capacidad de ahorro y proyecciones.

3. Inferencia ML (Random Forest): Clasifica el perfil de riesgo del usuario y calcula la probabilidad asociada.

## 📋 Hojas de Ruta por Equipo
🎨 Equipo Frontend (React / Web / Mobile)
Consumo de Endpoints: Conectarse al Gateway **POST http://localhost:8008/api/v1/analisis/procesar**.

Dashboard Financiero: Mostrar indicadores de estado (ej. "Conservador", "Moderado"), barras de probabilidad y tarjetas de recomendaciones.

Manejo de Estados de Carga: Implementar skeletons o loaders interactivos mientras el backend realiza la inferencia con el servicio NLP.

## ☕ Equipo Backend (Siguientes Pasos)
1. Implementación de capa de seguridad con Spring Security & JWT.

2. Configuración de despliegue en ambiente cloud (Oracle Cloud Infrastructure - OCI).