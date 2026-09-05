package saludfinanciera.finanzas.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("mensaje", "¡Bienvenido a la aplicación con Thymeleaf!");
        return "index"; // Esto buscará el archivo index.html en src/main/resources/templates/
    }
}
