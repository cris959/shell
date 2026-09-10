package saludfinanciera.finanzas.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import saludfinanciera.finanzas.model.Usuario;
import saludfinanciera.finanzas.service.AiClientService;


@Controller
public class AsistenteWebController {

    private final AiClientService aiClientService;

    public AsistenteWebController(AiClientService aiClientService) {
        this.aiClientService = aiClientService;
    }

    @GetMapping("/asistente/consultar")
    @ResponseBody // <-- Importante para que responda texto/JSON y no busque una vista HTML
    public String consultarIA(
            @RequestParam("query") String query,
            @ModelAttribute("usuarioGlobal") Usuario usuario) {

        String email = (usuario != null && usuario.getEmail() != null)
                ? usuario.getEmail()
                : "invitado@finanzas.com";

        // Retorna directamente el texto generado por la IA para la ventana emergente
        return aiClientService.consultarMicroservicioPython(query, email);
    }
}
