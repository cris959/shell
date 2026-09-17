package saludfinanciera.finanzas.service;

import org.springframework.ui.Model;

import java.security.Principal;

public interface PaginaService {
    void cargarDashboard(Principal principal, Model model);
}