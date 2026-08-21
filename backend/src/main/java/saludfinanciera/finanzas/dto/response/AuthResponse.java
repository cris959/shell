package saludfinanciera.finanzas.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO de respuesta tras una autenticación o registro exitoso")
public record AuthResponse(

        @Schema(description = "Token JWT generado", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqdWFuLnBlcmV6QGVtYWlsLmNvbSIs...")
        String token,

        @Schema(description = "Tipo de esquema de autorización", example = "Bearer")
        String tipoToken
) {
    // Constructor compacto para asignar "Bearer" por defecto
    public AuthResponse(String token) {
        this(token, "Bearer");
    }
}