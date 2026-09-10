package saludfinanciera.finanzas.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import saludfinanciera.finanzas.model.Usuario;
import saludfinanciera.finanzas.repository.UsuarioRepository;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final UsuarioRepository usuarioRepository;

    public GlobalControllerAdvice(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @ModelAttribute("usuarioGlobal")
    public Usuario agregarUsuarioGlobal(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        // authentication.getName() te da el email o username con el que inició sesión
        String emailOUsername = authentication.getName();

        // Buscamos el usuario real y fresco en la base de datos
        return (Usuario) usuarioRepository.findByEmail(emailOUsername).orElse(null);
    }
}