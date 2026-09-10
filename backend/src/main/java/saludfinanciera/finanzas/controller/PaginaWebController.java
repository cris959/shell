package saludfinanciera.finanzas.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaginaWebController {

    @GetMapping("/legal/privacidad")
    public String politicaPrivacidad() {
        return "legal/privacidad"; // Nombre del archivo HTML en templates/legal/privacidad.html
    }

    @GetMapping("/legal/terminos")
    public String terminosServicio() {
        return "legal/terminos"; // Nombre del archivo HTML en templates/legal/terminos.html
    }

    @GetMapping("/contacto")
    public String contacto() {
        return "contacto"; // Nombre del archivo HTML en templates/contacto.html
    }

    @GetMapping("/configuracion")
    public String configuracion() {
        return "configuracion"; // Carga configuracion.html
    }

    @GetMapping("/ayuda")
    public String ayuda() {
        return "ayuda"; // Carga ayuda.html
    }
}