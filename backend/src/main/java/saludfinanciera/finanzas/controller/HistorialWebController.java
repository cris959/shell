package saludfinanciera.finanzas.controller;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import saludfinanciera.finanzas.model.AnalisisFinanciero;
import saludfinanciera.finanzas.model.Usuario;
import saludfinanciera.finanzas.repository.AnalisisFinancieroRepository;
import saludfinanciera.finanzas.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@Controller
public class HistorialWebController {

    private final AnalisisFinancieroRepository analisisFinancieroRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public HistorialWebController(AnalisisFinancieroRepository analisisFinancieroRepository, UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.analisisFinancieroRepository = analisisFinancieroRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @GetMapping("/historial")
    public String verHistorial(
            Principal principal,
            Model model) {

        if (principal == null) {
            return "redirect:/login";
        }

        String email;
        String resolvedName = "Usuario Google";

        // Extraemos el email y el nombre de forma segura con Pattern Matching
        if (principal instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken oauthToken) {
            email = oauthToken.getPrincipal().getAttribute("email");
            String googleName = oauthToken.getPrincipal().getAttribute("name");
            if (googleName != null) {
                resolvedName = googleName;
            }
        } else {
            email = principal.getName();
        }

        // Creamos una constante efectivamente final para usarla dentro de la lambda
        final String finalName = resolvedName;

        // Buscamos al usuario en la BD, si no existe lo auto-registramos
        Usuario usuarioActualizado = usuarioRepository.findByEmail(email).orElseGet(() -> {
            Usuario nuevo = new Usuario();
            nuevo.setEmail(email);
            nuevo.setNombre(finalName);
            nuevo.setActivo(true);
            nuevo.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("OAUTH2_USER_SECURE"));
            return usuarioRepository.save(nuevo);
        });

        model.addAttribute("nombreUsuario", usuarioActualizado.getEmail());
        model.addAttribute("usuario", usuarioActualizado);

        // Usamos el email para buscar su historial de análisis
        List<AnalisisFinanciero> listaAnalisis = analisisFinancieroRepository.findByUsuarioIdWithRecomendaciones(usuarioActualizado.getEmail());
        model.addAttribute("listaAnalisis", listaAnalisis);

        return "historial";
    }


    @GetMapping("/historial/detalle/{id}")
    @Transactional
    public String verDetalleAnalisis(
            @PathVariable Long id,
            Principal principal,
            Model model) {

        if (principal == null) {
            return "redirect:/login";
        }

        // Extraemos el email de forma segura para ambos tipos de autenticación
        String usuarioIdActual;
        if (principal instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken oauthToken) {
            usuarioIdActual = oauthToken.getPrincipal().getAttribute("email");
        } else {
            usuarioIdActual = principal.getName();
        }

        // Buscamos el análisis con los detalles
        AnalisisFinanciero analisis = analisisFinancieroRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Análisis financiero no encontrado con ID: %d".formatted(id)));

        // Validación de permisos robusta usando el email extraído
        if (analisis.getUsuarioId() == null || !analisis.getUsuarioId().equalsIgnoreCase(usuarioIdActual)) {
            throw new RuntimeException("No tienes permisos para ver este análisis.");
        }

        System.out.println("--- DEPURANDO ANALISIS ---");
        System.out.printf("Total Gastado: %s%n", analisis.getTotalGastado());
        System.out.printf("Resumen Gastos: %s%n", analisis.getResumenGastos());

        model.addAttribute("analisis", analisis);
        return "detalle-analisis";
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

        String emailActual;
        if (principal instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken oauthToken) {
            emailActual = oauthToken.getPrincipal().getAttribute("email");
        } else {
            emailActual = principal.getName();
        }

        Usuario usuario = usuarioRepository.findByEmail(emailActual)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setNombre(nombre);
        usuario.setEmail(email);

        // Si el usuario intenta cambiar la contraseña, validamos la actual
        if (passwordNueva != null && !passwordNueva.isBlank()) {
            if (passwordActual == null || passwordActual.isBlank()) {
                redirectAttributes.addFlashAttribute("error", "Debes ingresar tu contraseña actual para establecer una nueva.");
                return "redirect:/historial";
            }

            // Verificamos que la contraseña actual sea correcta usando el PasswordEncoder
            if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
                redirectAttributes.addFlashAttribute("error", "La contraseña actual es incorrecta.");
                return "redirect:/historial";
            }

            // Si es correcta, encriptamos y guardamos la nueva
            usuario.setPassword(passwordEncoder.encode(passwordNueva));
        }

        usuarioRepository.save(usuario);
        redirectAttributes.addFlashAttribute("exito", "Información básica actualizada con éxito.");
        return "redirect:/historial";
    }

    @PostMapping("/historial/actualizar-financiero")
    @Transactional
    public String actualizarPerfilFinanciero(
            Principal principal,
            @RequestParam(required = false) String ingresoMensual,
            @RequestParam(required = false) String valorTotalDeudas,
            @RequestParam(required = false) String pagoMensualDeuda,
            @RequestParam(required = false) String fondoEmergencia,
            @RequestParam(required = false) String frecuenciaAhorro,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        // Validamos que los campos obligatorios no vengan vacíos
        if (ingresoMensual == null || ingresoMensual.isBlank() ||
                valorTotalDeudas == null || valorTotalDeudas.isBlank() ||
                pagoMensualDeuda == null || pagoMensualDeuda.isBlank() ||
                fondoEmergencia == null || fondoEmergencia.isBlank() ||
                frecuenciaAhorro == null || frecuenciaAhorro.isBlank()) {

            redirectAttributes.addFlashAttribute("error", "Por favor, completa todos los campos financieros. No se permiten datos vacíos.");
            return "redirect:/historial";
        }

        String emailActual;
        if (principal instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken oauthToken) {
            emailActual = oauthToken.getPrincipal().getAttribute("email");
        } else {
            emailActual = principal.getName();
        }

        Usuario usuario = usuarioRepository.findByEmail(emailActual)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        try {
            // Asignamos usando los setters reales de tu entidad
            usuario.setIngresoMensual(new BigDecimal(ingresoMensual.trim()));
            usuario.setValorTotalDeudas(new BigDecimal(valorTotalDeudas.trim()));
            usuario.setPagoMensualDeuda(new BigDecimal(pagoMensualDeuda.trim()));
            usuario.setFondoEmergencia(new BigDecimal(fondoEmergencia.trim()));
            usuario.setFrecuenciaAhorro(frecuenciaAhorro.trim());
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute("error", "Formato de número inválido en los campos financieros.");
            return "redirect:/historial";
        }

        usuarioRepository.save(usuario);
        redirectAttributes.addFlashAttribute("exito", "Perfil financiero actualizado con éxito.");
        return "redirect:/historial";
    }
}