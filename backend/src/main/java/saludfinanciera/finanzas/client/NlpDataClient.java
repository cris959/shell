package saludfinanciera.finanzas.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import saludfinanciera.finanzas.dto.request.AnalisisInputDTO;
import saludfinanciera.finanzas.dto.response.AnalisisOutputDTO;

import java.util.List;
import java.util.Map;

@Component
public class NlpDataClient {

    private static final Logger log = LoggerFactory.getLogger(NlpDataClient.class);
    private final RestClient nlpRestClient;

    public NlpDataClient(
            RestClient.Builder restClientBuilder,
            @Value("${python.nlp.service.url:http://localhost:8000}") String nlpServiceUrl
    ) {
        this.nlpRestClient = restClientBuilder
                .baseUrl(nlpServiceUrl)
                .build();
    }

    /**
     * Envía la estructura completa (datos + historialTransacciones) hacia FastAPI
     */
    public AnalisisOutputDTO analizarPerfil(AnalisisInputDTO inputDTO) {
        try {
            // Intenta llamar al microservicio de Python NLP
            return nlpRestClient.post()
                    .uri("/api/v1/analizar-perfil") // O /categorizar según el caso
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(inputDTO)
                    .retrieve()
                    .body(AnalisisOutputDTO.class);
        } catch (Exception e) {
            //  AGREGA ESTAS LÍNEAS PARA VER EL ERROR REAL EN LA CONSOLA
            log.error("❌ Error en cliente REST de Python NLP al analizar perfil: {}", e.getMessage(), e);
            // FALLBACK TEMPORAL: Si Python no responde o no está disponible,
            // devolvemos un objeto Mock para no cortar el flujo de Spring Boot
            return new AnalisisOutputDTO(
                    "EN OBSERVACION",                                     // 1. String perfilFinanciero
                    0.82,                                                 // 2. Double probabilidad
                    Map.of("ALIMENTACION", 420.0, "TRANSPORTE", 300.0),   // 3. Map<String, Object> resumenGastos
                    List.of("Servicio en modo degradado. Revisa tus gastos manualmente."), // 4. List<String> recomendaciones
                    720.0,                                                // 5. Double totalGastado
                    280.0,                                                // 6. Double capacidadAhorroMensual
                    28.0,                                                 // 7. Double porcentajeTasaAhorro
                    0.0,                                                  // 8. Double progresoMetaAhorro
                    0.0                                                   // 9. Double mesesParaMeta
            );
        }
    }
    /**
     * Categoriza una descripción individual mediante el microservicio NLP
     */
    public String categorizarDescripcion(String descripcion) {
        try {
            Map<String, String> requestBody = Map.of("descripcion", descripcion);

            CategorizacionResponse response = nlpRestClient.post()
                    .uri("/api/v1/categorizar")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(CategorizacionResponse.class);

            return (response != null && response.categoria() != null && !response.categoria().isBlank())
                    ? response.categoria().trim().toUpperCase()
                    : "OTROS";

        } catch (Exception e) {
            log.warn("⚠️ Error al categorizar descripción con Python NLP ('{}'): {}", descripcion, e.getMessage());
            return "OTROS";
        }
    }

    /**
     * Record auxiliar para deserializar la respuesta de categorización
     */
    public record CategorizacionResponse(String categoria) {}
}