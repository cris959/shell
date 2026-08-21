package saludfinanciera.finanzas.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "DTO para la autenticación de usuarios")
public record LoginRequest(

        @Schema(description = "Correo electrónico registrado", example = "juan.perez@email.com")
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Formato de email inválido")
        String email,

        @Schema(description = "Contraseña del usuario", example = "Password123!")
        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}