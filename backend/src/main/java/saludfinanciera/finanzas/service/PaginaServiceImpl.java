package saludfinanciera.finanzas.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import saludfinanciera.finanzas.model.AnalisisFinanciero;
import saludfinanciera.finanzas.model.Transaccion;
import saludfinanciera.finanzas.model.Usuario;
import saludfinanciera.finanzas.repository.AnalisisFinancieroRepository;
import saludfinanciera.finanzas.repository.TransaccionRepository;
import saludfinanciera.finanzas.repository.UsuarioRepository;

import java.security.Principal;
import java.util.List;

@Service
public class PaginaServiceImpl implements  PaginaService {

    private final AnalisisFinancieroRepository analisisRepository;
    private final TransaccionRepository transaccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public PaginaServiceImpl(AnalisisFinancieroRepository analisisRepository,
                             TransaccionRepository transaccionRepository,
                             UsuarioRepository usuarioRepository,
                             PasswordEncoder passwordEncoder) {
        this.analisisRepository = analisisRepository;
        this.transaccionRepository = transaccionRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void cargarDashboard(Principal principal, Model model) {
        if (principal == null) {
            return;
        }

        String email;
        String name = "Usuario Google";
        boolean isOAuth2 = principal instanceof OAuth2AuthenticationToken;

        if (isOAuth2) {
            var oauthToken = (OAuth2AuthenticationToken) principal;
            email = oauthToken.getPrincipal().getAttribute("email");
            name = oauthToken.getPrincipal().getAttribute("name");
        } else {
            email = principal.getName();
        }

        String finalName = (name != null && !name.isBlank()) ? name : "Usuario Google";

        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);

        if (usuario == null) {
            usuario = new Usuario();
            usuario.setEmail(email);
            usuario.setNombre(finalName);
            usuario.setActivo(true);
            usuario.setPassword(passwordEncoder.encode("OAUTH2_USER_SECURE"));
            System.out.printf(" >>> AUTO-REGISTRO DESDE DASHBOARD: %s%n", email);
            usuario = usuarioRepository.save(usuario);
        } else if (isOAuth2) {
            usuario.setNombre(finalName);
            usuario = usuarioRepository.save(usuario);
        }

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
        model.addAttribute("usuario", usuario);
    }
}