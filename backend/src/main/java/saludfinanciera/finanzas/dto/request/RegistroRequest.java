package saludfinanciera.finanzas.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO para la solicitud de registro de un nuevo usuario")
public record RegistroRequest(

        @Schema(description = "Nombre completo del usuario", example = "Juan Pérez")
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @Schema(description = "Correo electrónico del usuario", example = "juan.perez@email.com")
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Debe proporcionar un email válido")
        String email,

        @Schema(description = "Contraseña de acceso (mínimo 6 caracteres)", example = "Password123!")
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        String password
) {
}