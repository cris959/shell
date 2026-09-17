package saludfinanciera.finanzas.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import saludfinanciera.finanzas.service.HistorialService;

import java.security.Principal;
import java.util.Map;

@Controller
public class HistorialWebController {

    private final HistorialService historialService;


    public HistorialWebController(HistorialService historialService) {
        this.historialService = historialService;
    }

    @GetMapping("/historial")
    public String verHistorial(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        historialService.cargarDatosHistorial(principal, model);
        return "historial";
    }

    @GetMapping("/historial/detalle/{id}")
    public String verDetalleAnalisis(@PathVariable Long id, Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        try {
            historialService.cargarDetalleAnalisis(id, principal, model);
            return "detalle-analisis";
        } catch (RuntimeException e) {
            return "redirect:/historial?error=%s".formatted(e.getMessage());
        }
    }

    @PostMapping("/historial/actualizar-info")
    public String actualizarInformacionBasica(
            Principal principal,
            @RequestParam String nombre,
            @RequestParam String email,
            @RequestParam(required = false) String passwordActual,
            @RequestParam(required = false) String passwordNueva,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        try {
            historialService.actualizarInfoUsuario(principal, nombre, email, passwordActual, passwordNueva);
            redirectAttributes.addFlashAttribute("exito", "Información básica actualizada con éxito.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/historial";
    }

    @PostMapping("/historial/actualizar-financiero")
    public String actualizarPerfilFinanciero(
            Principal principal,
            @RequestParam Map<String, String> params,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        try {
            historialService.actualizarPerfilFinanciero(principal, params);
            redirectAttributes.addFlashAttribute("exito", "Perfil financiero actualizado con éxito.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/historial";
    }
}