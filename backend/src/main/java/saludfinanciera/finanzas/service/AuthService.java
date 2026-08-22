package saludfinanciera.finanzas.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import saludfinanciera.finanzas.dto.request.LoginRequest;
import saludfinanciera.finanzas.dto.request.RegistroRequest;
import saludfinanciera.finanzas.dto.response.AuthResponse;
import saludfinanciera.finanzas.infra.security.JwtTokenProvider;
import saludfinanciera.finanzas.model.Perfil;
import saludfinanciera.finanzas.model.PerfilNombre;
import saludfinanciera.finanzas.model.Usuario;
import saludfinanciera.finanzas.repository.PerfilRepository;
import saludfinanciera.finanzas.repository.UsuarioRepository;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthService(UsuarioRepository usuarioRepository, PerfilRepository perfilRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider, AuthenticationManager authenticationManager) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }
    public AuthResponse registrar(RegistroRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("El email ya se encuentra registrado");
        }

        // Buscar el perfil por defecto (ROLE_USER)
        Perfil perfilDefault = perfilRepository.findByNombre(PerfilNombre.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Perfil por defecto no encontrado"));

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setEmail(request.email());
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setPerfil(perfilDefault);
        usuario.setActivo(true);

        usuarioRepository.save(usuario);

        // Generar token para autologin inmediato tras el registro
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        String token = jwtTokenProvider.generarToken(authentication);
        return new AuthResponse(token);
    }

    public AuthResponse login(LoginRequest request) {
        // --- LOGS DE DIAGNÓSTICO ---
        Usuario usuarioBD = (Usuario) usuarioRepository.findByEmail(request.email())
                .orElse(null);

        if (usuarioBD == null) {
            System.out.println("❌ DIAGNÓSTICO: El usuario no existe en la BD o 'activo' es false (por @SQLRestriction).");
        } else {
            boolean coincide = passwordEncoder.matches(request.password(), usuarioBD.getPassword());
            System.out.println("🔍 DIAGNÓSTICO:");
            System.out.println("   - Email: " + request.email());
            System.out.println("   - Clave recibida (plana): " + request.password());
            System.out.println("   - Hash en BD: " + usuarioBD.getPassword());
            System.out.println("   - ¿Coinciden?: " + (coincide ? "✅ SÍ" : "❌ NO"));
        }
        // ---------------------------

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        String token = jwtTokenProvider.generarToken(authentication);
        return new AuthResponse(token);
    }
}