package saludfinanciera.finanzas.infra.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import saludfinanciera.finanzas.model.Usuario;
import saludfinanciera.finanzas.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UsuarioRepository usuarioRepository;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, UsuarioRepository usuarioRepository) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.usuarioRepository = usuarioRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Spring Security detecta automáticamente tu AutenticacionService y el PasswordEncoder
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
    /**
     * 1. CONFIGURACIÓN PARA APIS REST (Usa JWT)
     * Se aplica únicamente a rutas que comiencen con /api/
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 2. CONFIGURACIÓN PARA VISTAS WEB / THYMELEAF (Usa Sesiones HTTP y FormLogin)
     * Se aplica a  lo demás (dashboard, historial, login, etc.)
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/registro", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        .requestMatchers("/dashboard", "/analisis/**", "/historial/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                      .loginPage("/login")
                      .loginProcessingUrl("/login")
                      .usernameParameter("email") // 👈 Clave para que reconozca tu input 'email'
                      .passwordParameter("password")
                      .defaultSuccessUrl("/dashboard", true)
                      .permitAll()
               )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(this.oauth2UserService(usuarioRepository)) // 👈 Mapeo personalizado para Google
                       )
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    /**
     * Servicio para capturar y procesar los atributos que envía Google al iniciar sesión
     */
    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService(UsuarioRepository usuarioRepository) {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        return request -> {
            OAuth2User oauth2User = delegate.loadUser(request);

            String email = oauth2User.getAttribute("email");
            String name = oauth2User.getAttribute("name");

            log.info("Google Login exitoso - Email: {}, Nombre: {}", email, name);

            // Buscar o registrar automáticamente
            Usuario usuario = usuarioRepository.findByEmail(email).orElseGet(() -> {
                Usuario nuevo = new Usuario();
                nuevo.setEmail(email);
                nuevo.setNombre(name != null ? name : "Usuario Google");
                nuevo.setActivo(true);
                nuevo.setPassword(new BCryptPasswordEncoder().encode("OAUTH2_USER_SECURE"));
                log.info("Nuevo usuario registrado automáticamente en la BD: {}", email);
                return usuarioRepository.save(nuevo);
            });

            // Opcional: inyectar los atributos si tu entidad los necesita
            usuario.setAttributes(oauth2User.getAttributes());

            // Retornamos directamente nuestra entidad Usuario (que ya es UserDetails y OAuth2User)
            return usuario;
        };
    }
}