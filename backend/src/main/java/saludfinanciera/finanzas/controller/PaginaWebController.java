package saludfinanciera.finanzas.controller;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
            String email;
            String name = "Usuario Google";

            // Extraemos el email dependiendo de si entra por OAuth2 o Login tradicional
            if (principal instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken oauthToken) {
                email = oauthToken.getPrincipal().getAttribute("email");
                name = oauthToken.getPrincipal().getAttribute("name");
            } else {
                email = principal.getName();
            }

            // Determinamos el nombre de manera segura para la lambda
            String finalName = (name != null) ? name : "Usuario Google";

            // Buscamos al usuario, si no existe lo creamos al vuelo
            Usuario usuario = usuarioRepository.findByEmail(email).orElseGet(() -> {
                Usuario nuevo = new Usuario();
                nuevo.setEmail(email);
                nuevo.setNombre(finalName); // Usamos la variable efectivamente final
                nuevo.setActivo(true);
                nuevo.setPassword(new BCryptPasswordEncoder().encode("OAUTH2_USER_SECURE"));
                System.out.printf(" >>> AUTO-REGISTRO DESDE DASHBOARD: %s%n", email);
                return usuarioRepository.save(nuevo);
            });

            // Consultas financieras usando el email
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
            model.addAttribute("usuario", usuario); // 👈 Esto inyectará el objeto real en Thymeleaf
        }
        return "dashboard";
    }
}