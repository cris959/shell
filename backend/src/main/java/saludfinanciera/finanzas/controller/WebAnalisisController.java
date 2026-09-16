package saludfinanciera.finanzas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import saludfinanciera.finanzas.client.NlpDataClient;
import saludfinanciera.finanzas.dto.request.AnalisisInputDTO;
import saludfinanciera.finanzas.dto.request.TransaccionItemDTO;
import saludfinanciera.finanzas.dto.response.AnalisisOutputDTO;
import saludfinanciera.finanzas.model.AnalisisFinanciero;
import saludfinanciera.finanzas.model.Usuario;
import saludfinanciera.finanzas.repository.AnalisisFinancieroRepository;
import saludfinanciera.finanzas.repository.UsuarioRepository;
import saludfinanciera.finanzas.service.CsvParserService;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class WebAnalisisController {

    private static final Logger logger = LoggerFactory.getLogger(WebAnalisisController.class);

    private final NlpDataClient nlpDataClient;
    private final AnalisisFinancieroRepository analisisFinancieroRepository;
    private final CsvParserService csvParserService;
    private final UsuarioRepository usuarioRepository;

    public WebAnalisisController(NlpDataClient nlpDataClient, AnalisisFinancieroRepository analisisFinancieroRepository, CsvParserService csvParserService, UsuarioRepository usuarioRepository) {
        this.nlpDataClient = nlpDataClient;
        this.analisisFinancieroRepository = analisisFinancieroRepository;
        this.csvParserService = csvParserService;
        this.usuarioRepository = usuarioRepository;
    }

    // 1. Mostrar la vista del formulario de análisis
    @GetMapping("/analisis")
    public String mostrarAnalisis(Model model) {
        return "nuevo-analisis"; // Busca el archivo nuevo-analisis.html en src/main/resources/templates/
    }

    // 2. Procesar los datos manuales o el archivo CSV enviado desde la vista
    @PostMapping("/analisis/procesar")
    public String procesarAnalisis(
            @RequestParam(required = false) BigDecimal ingresoMensual,
            @RequestParam(required = false) BigDecimal totalDeudas,
            @RequestParam(required = false) String frecuenciaAhorro,
            @RequestParam(required = false) BigDecimal montoInversion,
            @RequestParam(required = false) BigDecimal objetivoPresupuesto,
            @RequestParam(required = false) BigDecimal pagoMensualDeuda,
            @RequestParam(required = false) Integer cantidadSuscripciones,
            @RequestParam(required = false) BigDecimal fondoEmergencia,
            @RequestParam(required = false) MultipartFile file,
            // RECIBIMOS EL JSON CRUDO DESDE EL INPUT DE JAVASCRIPT
            @RequestParam(required = false) String transaccionesJson,
            // 🛡️ CAMBIADO A PRINCIPAL PARA SOPORTAR GOOGLE OAUTH2 Y LOGIN TRADICIONAL
            Principal principal,
            Model model) {

        // 🛡️ OBTENEMOS EL EMAIL REAL DEL USUARIO AUTENTICADO DE FORMA SEGURA
        String usuarioId = "INVITADO";
        String resolvedName = "Usuario Google";

        if (principal != null) {
            if (principal instanceof OAuth2AuthenticationToken oauthToken) {
                usuarioId = oauthToken.getPrincipal().getAttribute("email");
                String googleName = oauthToken.getPrincipal().getAttribute("name");
                if (googleName != null) {
                    resolvedName = googleName;
                }
            } else {
                usuarioId = principal.getName();
            }
        }

        // 🛡️ AUTO-REGISTRO DEFENSIVO: Si el usuario está autenticado pero no existe en la BD, lo creamos al vuelo
        if (!"INVITADO".equals(usuarioId)) {
            final String finalUsuarioId = usuarioId; // 👈 Constante efectivamente final para la lambda
            final String finalName = resolvedName;

            usuarioRepository.findByEmail(finalUsuarioId).orElseGet(() -> {
                Usuario nuevo = new Usuario();
                nuevo.setEmail(finalUsuarioId);
                nuevo.setNombre(finalName);
                nuevo.setActivo(true);
                nuevo.setPassword(new BCryptPasswordEncoder().encode("OAUTH2_USER_SECURE"));
                logger.info(" >>> AUTO-REGISTRO DESDE PROCESAR ANALISIS: {}", finalUsuarioId);
                return usuarioRepository.save(nuevo);
            });
        }


        AnalisisInputDTO inputDTO;

        // Parsear el JSON de la tabla manual si viene presente
        logger.info("👉 JSON RECIBIDO DE JS: {}", transaccionesJson);
        List<TransaccionItemDTO> listaTransaccionesManual = new ArrayList<>();

        if (transaccionesJson != null && !transaccionesJson.isBlank()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

                listaTransaccionesManual = mapper.readValue(
                        transaccionesJson,
                        new com.fasterxml.jackson.core.type.TypeReference<>() {}
                );
                logger.info("✅ Transacciones manuales parseadas con éxito: {}", listaTransaccionesManual.size());
            } catch (Exception e) {
                logger.error("❌ ERROR AL PARSEAR JSON DE TRANSACCIONES: {}", e.getMessage(), e);
            }
        }

        // Validar si el usuario cargó un archivo CSV
        if (file != null && !file.isEmpty()) {
            try {
                // 👇 USAMOS EL NUEVO MÉTODO QUE DEVUELVE LAS TRANSACCIONES Y EL INGRESO CALCULADO
                saludfinanciera.finanzas.dto.response.CsvParseResult resultadoCsv = csvParserService.parsearTransaccionesConIngresos(file);

                // Si el ingreso manual no fue provisto o es cero, usamos el calculado automáticamente del CSV
                BigDecimal ingresoFinal = (ingresoMensual != null && ingresoMensual.compareTo(BigDecimal.ZERO) > 0)
                        ? ingresoMensual
                        : resultadoCsv.ingresoTotalCalculado();

                inputDTO = new AnalisisInputDTO(
                        ingresoFinal.doubleValue(),
                        fondoEmergencia != null ? fondoEmergencia.doubleValue() : 0.0,
                        totalDeudas != null ? totalDeudas.intValue() : 0,
                        frecuenciaAhorro != null ? frecuenciaAhorro : "MENSUAL",
                        "Análisis financiero masivo desde archivo CSV",
                        objetivoPresupuesto != null ? objetivoPresupuesto.doubleValue() : 0.0,
                        resultadoCsv.transacciones()
                );
            } catch (Exception e) {
                model.addAttribute("error", "Error al procesar el archivo CSV: %s".formatted(e.getMessage()));
                return "nuevo-analisis";
            }
        } else {
            // Lógica manual con las transacciones parseadas desde el JSON
            inputDTO = new AnalisisInputDTO(
                    ingresoMensual != null ? ingresoMensual.doubleValue() : 0.0,
                    fondoEmergencia != null ? fondoEmergencia.doubleValue() : 0.0,
                    totalDeudas != null ? totalDeudas.intValue() : 0,
                    frecuenciaAhorro != null ? frecuenciaAhorro : "MENSUAL",
                    "Evaluación de capacidad de ahorro e inversión manual",
                    valorMetaOInversion(montoInversion, objetivoPresupuesto),
                    listaTransaccionesManual
            );
        }

        // 3. Enviar a Python, guardar y mostrar resultado...
        AnalisisOutputDTO resultadoAnalisis = nlpDataClient.analizarPerfil(inputDTO);

        try {
            AnalisisFinanciero analisis = AnalisisFinanciero.builder()
                    .usuarioId(usuarioId) // 👈 Ahora guardará correctamente el email del usuario de Google
                    .ingresoMensual(inputDTO.ingresoMensual())
                    .nivelEndeudamiento(inputDTO.nivelEndeudamiento())
                    .frecuenciaAhorro(inputDTO.frecuenciaAhorro())
                    .descripcion(inputDTO.descripcion())
                    .valor(inputDTO.valor())
                    .perfilFinanciero(resultadoAnalisis.perfilFinanciero())
                    .probabilidad(resultadoAnalisis.probabilidad())
                    .totalGastado(resultadoAnalisis.totalGastado())
                    .capacidadAhorroMensual(resultadoAnalisis.capacidadAhorroMensual())
                    .porcentajeTasaAhorro(resultadoAnalisis.porcentajeTasaAhorro())
                    .progresoMetaAhorro(resultadoAnalisis.progresoMetaAhorro())
                    .mesesParaMeta(resultadoAnalisis.mesesParaMeta())
                    .fechaCreacion(LocalDateTime.now())
                    .build();

            if (resultadoAnalisis.recomendaciones() != null) {
                Set<String> recomendacionesUnicas = resultadoAnalisis.recomendaciones().stream()
                        .filter(r -> r != null && !r.isBlank())
                        .map(String::trim)
                        .collect(Collectors.toSet());
                analisis.setRecomendaciones(recomendacionesUnicas);
            }

            if (resultadoAnalisis.resumenGastos() != null) {
                Map<String, Double> resumenConvertido = new HashMap<>();
                for (Map.Entry<String, Object> entry : resultadoAnalisis.resumenGastos().entrySet()) {
                    if (entry.getValue() instanceof Number n) {
                        resumenConvertido.put(entry.getKey(), n.doubleValue());
                    }
                }
                analisis.setResumenGastos(resumenConvertido);
            }

            analisisFinancieroRepository.save(analisis);
            logger.info("✅ Análisis financiero guardado exitosamente para el usuario: {}", usuarioId);

        } catch (Exception e) {
            logger.error("❌ No se pudo guardar el análisis en la base de datos: {}", e.getMessage(), e);
        }

        model.addAttribute("resultado", resultadoAnalisis);
        return "resultado-analisis";
    }

    // funcion auxiliar para determinar el valor objetivo
    private Double valorMetaOInversion(BigDecimal inversion, BigDecimal objetivo) {
        if (objetivo != null) return objetivo.doubleValue();
        if (inversion != null) return inversion.doubleValue();
        return 0.0;
    }
}