package saludfinanciera.finanzas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class WebAnalisisController {

    private static final Logger logger = LoggerFactory.getLogger(WebAnalisisController.class);

    private final NlpDataClient nlpDataClient;
    private final AnalisisFinancieroRepository analisisFinancieroRepository;

    public WebAnalisisController(NlpDataClient nlpDataClient, AnalisisFinancieroRepository analisisFinancieroRepository) {
        this.nlpDataClient = nlpDataClient;
        this.analisisFinancieroRepository = analisisFinancieroRepository;
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
            //  RECIBIMOS EL JSON CRUDO DESDE EL INPUT DE JAVASCRIPT
            @RequestParam(required = false) String transaccionesJson,
            @AuthenticationPrincipal Usuario usuarioLogueado, // Tu entidad que implementa UserDetails
            Model model) {

        // 🛡️ OBTENEMOS EL ID O EMAIL REAL DEL USUARIO AUTENTICADO
        // (Ajusta .getId().toString() o .getEmail() según cómo guardes el usuarioId en tu BD)
        String usuarioId = (usuarioLogueado != null) ? usuarioLogueado.getEmail() : "INVITADO";

        AnalisisInputDTO inputDTO;

        // Parsear el JSON de la tabla manual si viene presente
        logger.info("👉 JSON RECIBIDO DE JS: {}", transaccionesJson);
        List<TransaccionItemDTO> listaTransaccionesManual = new ArrayList<>();

        // Parsear el JSON de la tabla manual si viene presente
        if (transaccionesJson != null && !transaccionesJson.isBlank()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                // Registra el módulo de fechas si usas LocalDate en tu DTO
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

                listaTransaccionesManual = mapper.readValue(
                        transaccionesJson,
                        new com.fasterxml.jackson.core.type.TypeReference<>() {}
                );
                logger.info("✅ Transacciones parseadas con éxito: {}", listaTransaccionesManual.size()); //  NUEVO
            } catch (Exception e) {
                logger.error("❌ ERROR AL PARSEAR JSON DE TRANSACCIONES: {}", e.getMessage(), e);
            }
        }

        // Validar si el usuario cargó un archivo CSV
        if (file != null && !file.isEmpty()) {
            try {
                List<TransaccionItemDTO> transaccionesDelCsv = parsearCsvTransacciones(file);
                inputDTO = new AnalisisInputDTO(
                        ingresoMensual != null ? ingresoMensual.doubleValue() : 0.0,
                        fondoEmergencia != null ? fondoEmergencia.doubleValue() : 0.0,
                        totalDeudas != null ? totalDeudas.intValue() : 0,
                        frecuenciaAhorro != null ? frecuenciaAhorro : "MENSUAL",
                        "Análisis financiero masivo desde archivo CSV",
                        objetivoPresupuesto != null ? objetivoPresupuesto.doubleValue() : 0.0,
                        transaccionesDelCsv
                );
            } catch (Exception e) {
                model.addAttribute("error", "Error al procesar el archivo CSV: %s".formatted(e.getMessage()));
                return "nuevo-analisis";
            }
        } else {
            // Lógica manual con las transacciones parseadas perfectamente desde el JSON
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
                    .usuarioId(usuarioId) //  AQUÍ USAMOS LA VARIABLE DINÁMICA EN LUGAR DE "USR-1001"
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
                        .collect(Collectors.toSet()); // Esto elimina cualquier duplicado automáticamente
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

        } catch (Exception e) {
            logger.error("No se pudo guardar el análisis en la base de datos: {}", e.getMessage(), e);
        }

        model.addAttribute("resultado", resultadoAnalisis);
        return "resultado-analisis";
    }

    // Forma auxiliar para parsear el CSV usando TransaccionItemDTO
    private List<TransaccionItemDTO> parsearCsvTransacciones(MultipartFile file) {
        List<TransaccionItemDTO> transacciones = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            boolean primeraLinea = true;

            while ((linea = reader.readLine()) != null) {
                if (linea.isBlank()) continue;

                if (primeraLinea && (linea.toLowerCase().contains("descripcion") || linea.toLowerCase().contains("monto"))) {
                    primeraLinea = false;
                    continue;
                }
                primeraLinea = false;

                String[] partes = linea.split(",");
                if (partes.length >= 3) {
                    String descripcion = partes[0].trim();
                    BigDecimal monto = new BigDecimal(partes[1].trim());
                    String tipo = partes[2].trim().toUpperCase();
                    String categoria = partes.length > 3 ? partes[3].trim() : "OTROS";

                    TransaccionItemDTO transaccion = new TransaccionItemDTO(
                            LocalDate.now(),
                            descripcion,
                            monto,
                            categoria
                    );

                    transacciones.add(transaccion);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("No se pudo leer el formato del archivo CSV: %s".formatted(e.getMessage()), e);
        }

        return transacciones;
    }

    // Forma auxiliar opcional para determinar el valor objetivo
    private Double valorMetaOInversion(BigDecimal inversion, BigDecimal objetivo) {
        if (objetivo != null) return objetivo.doubleValue();
        if (inversion != null) return inversion.doubleValue();
        return 0.0;
    }
}