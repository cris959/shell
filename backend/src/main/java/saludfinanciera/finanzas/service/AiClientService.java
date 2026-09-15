package saludfinanciera.finanzas.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AiClientService {

    private final RestClient restClient;

    // Puedes inyectar un RestClient configurado previamente o instanciarlo apuntando a tu microservicio Python (ej. http://localhost:8000)
    public AiClientService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
// Uso en Local ** .baseUrl("http://localhost:8000") ** // URL donde corre tu app de Python
                .baseUrl("http://python-nlp:8000")
                .build();
    }
    public String consultarMicroservicioPython(String prompt, String emailUsuario) {
        // Objeto DTO o record que espera tu API de Python
        record AiRequest(String query, String email) {}
        record AiResponse(String respuesta) {}

        try {
            AiResponse response = restClient.post()
                    .uri("/api/v1/consultar-ia") // Endpoint de tu FastAPI/Python
                    .body(new AiRequest(prompt, emailUsuario))
                    .retrieve()
                    .body(AiResponse.class);

            return response != null ? response.respuesta : "No se obtuvo respuesta de la IA.";
        } catch (Exception e) {
            return String.format("Error al comunicarse con el microservicio de IA: %s", e.getMessage());
        }
    }
}
