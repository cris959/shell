package saludfinanciera.finanzas.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import saludfinanciera.finanzas.dto.request.LoginRequest;
import saludfinanciera.finanzas.dto.request.RegistroRequest;
import saludfinanciera.finanzas.dto.response.AuthResponse;
import saludfinanciera.finanzas.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Endpoints para registro y acceso a la plataforma mediante JWT")
public class AutenticacionController {

    private final AuthService  authService;

    public AutenticacionController(AuthService authService) {
        this.authService = authService;
    }


    @Operation(summary = "Registrar un nuevo usuario", description = "Crea un nuevo usuario asignándole el perfil estándar y retorna un token JWT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario registrado con éxito"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o email ya en uso")
    })
    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        AuthResponse response = authService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Iniciar sesión", description = "Autentica al usuario mediante email/password y genera un token JWT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autenticación exitosa"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}