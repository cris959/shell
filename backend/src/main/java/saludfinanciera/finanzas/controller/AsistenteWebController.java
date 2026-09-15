package saludfinanciera.finanzas.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import saludfinanciera.finanzas.service.AiClientService;

import java.util.Map;


@Controller
public class AsistenteWebController {

    private final AiClientService aiClientService;

    public AsistenteWebController(AiClientService aiClientService) {
        this.aiClientService = aiClientService;
    }

@PostMapping("/api/asistente/consultar") // <-- Cambiado a POST y alineado con la URL del fetch
@ResponseBody
public Map<String, String> consultarIA(@RequestBody Map<String, String> requestBody) {

    String query = requestBody.get("query");
    String email = requestBody.get("email");

    if (email == null || email.isEmpty()) {
        email = "invitado@finanzas.com";
    }

    // Llamas a tu servicio que se comunica con el contenedor de Python ("http://python-nlp:8000")
    String respuestaIa = aiClientService.consultarMicroservicioPython(query, email);

    // Devolvemos un JSON con la estructura exacta que lee tu JavaScript: data["respuesta"]
    return Map.of("respuesta", respuestaIa != null ? respuestaIa : "No se obtuvo respuesta de la IA.");
 }
}