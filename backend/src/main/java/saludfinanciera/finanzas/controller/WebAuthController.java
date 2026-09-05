package saludfinanciera.finanzas.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import saludfinanciera.finanzas.dto.request.LoginRequest;
import saludfinanciera.finanzas.dto.response.AuthResponse;
import saludfinanciera.finanzas.service.AuthService;

@Controller
public class WebAuthController {

    private final AuthService authService;

    public WebAuthController(AuthService authService) {
        this.authService = authService;
    }

    // 1. Mostrar la vista de Login pasándole una instancia vacía para el formulario
    @GetMapping("/login")
    String mostrarLogin(Model model) {
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

    // 3. Mostrar la vista del Dashboard (¡Este era el que faltaba y causaba el error 500!)
    @GetMapping("/dashboard")
    public String mostrarDashboard(Model model) {
        return "dashboard"; // Busca templates/dashboard.html
    }

    // 4. Mostrar la vista para ingresar datos y ver análisis
    @GetMapping("/analisis")
    public String mostrarAnalisis(Model model) {
        // Aquí puedes pasar un objeto vacío si vas a usar un formulario Thymeleaf para enviar datos
        return "analisis"; // Busca templates/analisis.html
    }
}
