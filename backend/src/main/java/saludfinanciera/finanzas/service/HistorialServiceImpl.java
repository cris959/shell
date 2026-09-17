package saludfinanciera.finanzas.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import saludfinanciera.finanzas.model.AnalisisFinanciero;
import saludfinanciera.finanzas.model.Usuario;
import saludfinanciera.finanzas.repository.AnalisisFinancieroRepository;
import saludfinanciera.finanzas.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@Service
public class HistorialServiceImpl implements HistorialService{

    private final AnalisisFinancieroRepository analisisFinancieroRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public HistorialServiceImpl(AnalisisFinancieroRepository analisisFinancieroRepository,
                                UsuarioRepository usuarioRepository,
                                PasswordEncoder passwordEncoder) {
        this.analisisFinancieroRepository = analisisFinancieroRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private String extraerEmail(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken oauthToken) {
            return oauthToken.getPrincipal().getAttribute("email");
        }
        return principal.getName();
    }

    @Override
    public void cargarDatosHistorial(Principal principal, Model model) {
        String email;
        String resolvedName = "Usuario Google";

        if (principal instanceof OAuth2AuthenticationToken oauthToken) {
            email = oauthToken.getPrincipal().getAttribute("email");
            String googleName = oauthToken.getPrincipal().getAttribute("name");
            if (googleName != null) {
                resolvedName = googleName;
            }
        } else {
            email = principal.getName();
        }

        final String finalName = resolvedName;

        Usuario usuarioActualizado = usuarioRepository.findByEmail(email).orElseGet(() -> {
            Usuario nuevo = new Usuario();
            nuevo.setEmail(email);
            nuevo.setNombre(finalName);
            nuevo.setActivo(true);
            nuevo.setPassword(passwordEncoder.encode("OAUTH2_USER_SECURE"));
            return usuarioRepository.save(nuevo);
        });

        model.addAttribute("nombreUsuario", usuarioActualizado.getEmail());
        model.addAttribute("usuario", usuarioActualizado);

        List<AnalisisFinanciero> listaAnalisis = analisisFinancieroRepository.findByUsuarioIdWithRecomendaciones(usuarioActualizado.getEmail());
        model.addAttribute("listaAnalisis", listaAnalisis);
    }

    @Override
    @Transactional
    public void cargarDetalleAnalisis(Long id, Principal principal, Model model) {
        String usuarioIdActual = extraerEmail(principal);

        AnalisisFinanciero analisis = analisisFinancieroRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Análisis financiero no encontrado con ID: %d".formatted(id)));

        if (analisis.getUsuarioId() == null || !analisis.getUsuarioId().equalsIgnoreCase(usuarioIdActual)) {
            throw new RuntimeException("No tienes permisos para ver este análisis.");
        }

        model.addAttribute("analisis", analisis);
    }

    @Override
    public void actualizarInfoUsuario(Principal principal, String nombre, String email, String passwordActual, String passwordNueva) {
        String emailActual = extraerEmail(principal);

        Usuario usuario = usuarioRepository.findByEmail(emailActual)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setNombre(nombre);
        usuario.setEmail(email);

        if (passwordNueva != null && !passwordNueva.isBlank()) {
            if (passwordActual == null || passwordActual.isBlank()) {
                throw new RuntimeException("Debes ingresar tu contraseña actual para establecer una nueva.");
            }

            if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
                throw new RuntimeException("La contraseña actual es incorrecta.");
            }

            usuario.setPassword(passwordEncoder.encode(passwordNueva));
        }

        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void actualizarPerfilFinanciero(Principal principal, Map<String, String> params) {
        String ingresoMensual = params.get("ingresoMensual");
        String valorTotalDeudas = params.get("valorTotalDeudas");
        String pagoMensualDeuda = params.get("pagoMensualDeuda");
        String fondoEmergencia = params.get("fondoEmergencia");
        String frecuenciaAhorro = params.get("frecuenciaAhorro");

        if (ingresoMensual == null || ingresoMensual.isBlank() ||
                valorTotalDeudas == null || valorTotalDeudas.isBlank() ||
                pagoMensualDeuda == null || pagoMensualDeuda.isBlank() ||
                fondoEmergencia == null || fondoEmergencia.isBlank() ||
                frecuenciaAhorro == null || frecuenciaAhorro.isBlank()) {

            throw new RuntimeException("Por favor, completa todos los campos financieros. No se permiten datos vacíos.");
        }

        String emailActual = extraerEmail(principal);

        Usuario usuario = usuarioRepository.findByEmail(emailActual)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        try {
            usuario.setIngresoMensual(new BigDecimal(ingresoMensual.trim()));
            usuario.setValorTotalDeudas(new BigDecimal(valorTotalDeudas.trim()));
            usuario.setPagoMensualDeuda(new BigDecimal(pagoMensualDeuda.trim()));
            usuario.setFondoEmergencia(new BigDecimal(fondoEmergencia.trim()));
            usuario.setFrecuenciaAhorro(frecuenciaAhorro.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Formato de número inválido en los campos financieros.");
        }

        usuarioRepository.save(usuario);
    }
}