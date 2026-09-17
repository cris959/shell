package saludfinanciera.finanzas.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import saludfinanciera.finanzas.dto.request.LoginRequest;
import saludfinanciera.finanzas.dto.response.AuthResponse;
import saludfinanciera.finanzas.model.Usuario;
import saludfinanciera.finanzas.service.AuthService;

@Controller
public class WebAuthController {

    private final AuthService authService;

    public WebAuthController(AuthService authService) {
        this.authService = authService;
    }

    // 1. Mostrar la vista de Login pasándole una instancia vacía para el formulario
    @GetMapping("/login")
    public String mostrarLogin(Model model) {
        // Inicializamos el record con valores vacíos para que Thymeleaf no falle al renderizar
        model.addAttribute("loginRequest", new LoginRequest("", ""));
        return "auth/login";
    }

    // 2. Procesar el formulario de Login desde la web
    @PostMapping("/login")
    public String procesarLogin(@ModelAttribute("loginRequest") LoginRequest loginRequest,
                                HttpServletResponse responseHttp,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        try {
            AuthResponse response = authService.login(loginRequest);

            // Crear una cookie segura con el token JWT
            Cookie cookie = new Cookie("JWT_TOKEN", response.token()); // Asumiendo que tu record tiene get o el campo token
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(24 * 60 * 60); // 1 día
            responseHttp.addCookie(cookie);

            return "redirect:/dashboard";

        } catch (Exception e) {
            model.addAttribute("error", "Correo o contraseña incorrectos.");
            return "auth/login";
        }
    }

    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "auth/registro"; // Busca registro.html en templates
    }

    @PostMapping("/registro")
    public String registrarUsuario(@ModelAttribute Usuario usuario, Model model) {
        try {
            authService.registrarUsuarioWeb(usuario);
            return "redirect:/login?registrado";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/registro";
        }
    }

    @GetMapping("/recuperar-password")
    public String mostrarFormularionRecuperacion() {
        return "auth/recuperar-password"; // Carga la vista para solicitar el correo
    }

    @PostMapping("/recuperar-password")
    public String procesarRecuperacion(@RequestParam("email") String email, Model model) {
        // Aquí puedes implementar la lógica para buscar el usuario y generar un token o enviar un correo.
        // Por ahora, simularemos un mensaje de éxito:
        model.addAttribute("exito", "Si el correo está registrado, recibirás instrucciones para restablecer tu contraseña.");
        return "auth/recuperar-password";
    }
}