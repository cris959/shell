package saludfinanciera.finanzas.config;

import io.micrometer.common.lang.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/**") // Aplica a TODOS los endpoints de la API
                        .allowedOriginPatterns("*") // Permite cualquier origen de forma compatible con credenciales
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH") // Métodos HTTP permitidos
                        .allowedHeaders("*") // Permite todos los headers (Content-Type, Authorization, etc.)
                        .allowCredentials(true); // Permite el uso de credenciales/cookies
            }
        };
    }
}                   //  ** Ambivalente (Local y OCI) **  //