package saludfinanciera.finanzas.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import saludfinanciera.finanzas.service.PaginaService;

import java.security.Principal;

@Controller
public class PaginaWebController {

    private final PaginaService paginaService;

    public PaginaWebController(PaginaService paginaService) {
        this.paginaService = paginaService;
    }

    @GetMapping("/legal/privacidad")
    public String politicaPrivacidad() {
        return "legal/privacidad";
    }

    @GetMapping("/legal/terminos")
    public String terminosServicio() {
        return "legal/terminos";
    }

    @GetMapping("/contacto")
    public String contacto() {
        return "contacto";
    }

    @GetMapping("/configuracion")
    public String configuracion() {
        return "configuracion";
    }

    @GetMapping("/ayuda")
    public String ayuda() {
        return "ayuda";
    }

    @GetMapping("/dashboard")
    public String verDashboard(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        paginaService.cargarDashboard(principal, model);
        return "dashboard";
    }
}