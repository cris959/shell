package saludfinanciera.finanzas.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal Usuario usuarioLogueado,
            Model model) {

        if (usuarioLogueado == null) {
            return "redirect:/login";
        }

        Usuario usuarioActualizado = usuarioRepository.findById(usuarioLogueado.getId())
                .orElse(usuarioLogueado);

        String usuarioId = usuarioActualizado.getEmail();

        model.addAttribute("nombreUsuario", usuarioActualizado.getEmail());
        model.addAttribute("usuario", usuarioActualizado);

        // Usamos el email (String)
        List<AnalisisFinanciero> listaAnalisis = analisisFinancieroRepository.findByUsuarioIdWithRecomendaciones(usuarioId);
        model.addAttribute("listaAnalisis", listaAnalisis);

        return "historial";
    }


    @GetMapping("/historial/detalle/{id}")
    @Transactional
    public String verDetalleAnalisis(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogueado,
            Model model) {

        // Usamos la consulta con JOIN FETCH
        AnalisisFinanciero analisis = analisisFinancieroRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Análisis financiero no encontrado con ID: %d".formatted(id)));

        // (Opcional de seguridad) Verificar que el análisis pertenezca realmente al usuario logueado
        String usuarioIdActual = (usuarioLogueado != null) ? usuarioLogueado.getEmail() : "";
        if (!analisis.getUsuarioId().equals(usuarioIdActual)) {
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
            @AuthenticationPrincipal Usuario usuarioLogueado,
            @RequestParam String nombre,
            @RequestParam String email,
            @RequestParam(required = false) String passwordActual,
            @RequestParam(required = false) String passwordNueva,
            RedirectAttributes redirectAttributes) {

        if (usuarioLogueado == null) {
            return "redirect:/login";
        }

        Usuario usuario = usuarioRepository.findById(usuarioLogueado.getId())
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
            @AuthenticationPrincipal Usuario usuarioLogueado,
            @RequestParam(required = false) String ingresoMensual,
            @RequestParam(required = false) String valorTotalDeudas,
            @RequestParam(required = false) String pagoMensualDeuda,
            @RequestParam(required = false) String fondoEmergencia,
            @RequestParam(required = false) String frecuenciaAhorro,
            RedirectAttributes redirectAttributes) {

        if (usuarioLogueado == null) {
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

        Usuario usuario = usuarioRepository.findById(usuarioLogueado.getId())
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