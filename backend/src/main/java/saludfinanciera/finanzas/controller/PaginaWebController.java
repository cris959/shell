package saludfinanciera.finanzas.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import saludfinanciera.finanzas.model.AnalisisFinanciero;
import saludfinanciera.finanzas.model.Transaccion;
import saludfinanciera.finanzas.model.Usuario;
import saludfinanciera.finanzas.repository.AnalisisFinancieroRepository;
import saludfinanciera.finanzas.repository.TransaccionRepository;
import saludfinanciera.finanzas.repository.UsuarioRepository;

import java.security.Principal;
import java.util.List;

@Controller
public class PaginaWebController {

    private final AnalisisFinancieroRepository analisisRepository;
    private final TransaccionRepository transaccionRepository;
    private final UsuarioRepository usuarioRepository;

    public PaginaWebController(AnalisisFinancieroRepository analisisRepository, TransaccionRepository transaccionRepository, UsuarioRepository usuarioRepository) {
        this.analisisRepository = analisisRepository;
        this.transaccionRepository = transaccionRepository;
        this.usuarioRepository = usuarioRepository;
    }

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

    @GetMapping("/dashboard")
    public String verDashboard(Model model, Principal principal) {
        if (principal != null) {
            String email = principal.getName();
            Usuario usuario = (Usuario) usuarioRepository.findByEmail(email).orElse(null);

            if (usuario != null) {
                // Usamos el email (String) para ambas consultas
                AnalisisFinanciero ultimoAnalisis = analisisRepository.findTopByUsuarioIdOrderByIdDesc(usuario.getEmail()).orElse(null);
                List<Transaccion> ultimosMovimientos = transaccionRepository.findTop5ByUsuarioIdOrderByIdDesc(usuario.getEmail());

                if (ultimoAnalisis != null) {
                    model.addAttribute("perfilFinanciero", ultimoAnalisis.getPerfilFinanciero());
                    model.addAttribute("capacidadAhorro", ultimoAnalisis.getCapacidadAhorroMensual());
                } else {
                    model.addAttribute("perfilFinanciero", "Sin analizar");
                    model.addAttribute("capacidadAhorro", 0.0);
                }

                model.addAttribute("ultimosMovimientos", ultimosMovimientos);
            }
        }
        return "dashboard";
    }
}