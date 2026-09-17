package saludfinanciera.finanzas.service;

import org.springframework.ui.Model;

import java.security.Principal;
import java.util.Map;

public interface HistorialService {

    void cargarDatosHistorial(Principal principal, Model model);
    void cargarDetalleAnalisis(Long id, Principal principal, Model model);
    void actualizarInfoUsuario(Principal principal, String nombre, String email, String passwordActual, String passwordNueva);
    void actualizarPerfilFinanciero(Principal principal, Map<String, String> params);
}
