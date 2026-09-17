package saludfinanciera.finanzas.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import saludfinanciera.finanzas.client.NlpDataClient;
import saludfinanciera.finanzas.dto.request.AnalisisInputDTO;
import saludfinanciera.finanzas.dto.request.TransaccionItemDTO;
import saludfinanciera.finanzas.dto.response.AnalisisOutputDTO;
import saludfinanciera.finanzas.dto.response.CsvParseResult;
import saludfinanciera.finanzas.model.AnalisisFinanciero;
import saludfinanciera.finanzas.model.Usuario;
import saludfinanciera.finanzas.repository.AnalisisFinancieroRepository;
import saludfinanciera.finanzas.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalisisServiceWebImpl implements  AnalisisServiceWeb {

    private static final Logger logger = LoggerFactory.getLogger(AnalisisServiceWebImpl.class);

    private final NlpDataClient nlpDataClient;
    private final AnalisisFinancieroRepository analisisFinancieroRepository;
    private final CsvParserService csvParserService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AnalisisServiceWebImpl(NlpDataClient nlpDataClient,
                               AnalisisFinancieroRepository analisisFinancieroRepository,
                               CsvParserService csvParserService,
                               UsuarioRepository usuarioRepository,
                               PasswordEncoder passwordEncoder) {
        this.nlpDataClient = nlpDataClient;
        this.analisisFinancieroRepository = analisisFinancieroRepository;
        this.csvParserService = csvParserService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public AnalisisOutputDTO procesarAnalisis(
            BigDecimal ingresoMensual,
            BigDecimal totalDeudas,
            String frecuenciaAhorro,
            BigDecimal montoInversion,
            BigDecimal objetivoPresupuesto,
            BigDecimal pagoMensualDeuda,
            Integer cantidadSuscripciones,
            BigDecimal fondoEmergencia,
            MultipartFile file,
            String transaccionesJson,
            Principal principal) {

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

        // Auto-registro defensivo
        if (!"INVITADO".equals(usuarioId)) {
            final String finalUsuarioId = usuarioId;
            final String finalName = resolvedName;

            usuarioRepository.findByEmail(finalUsuarioId).orElseGet(() -> {
                Usuario nuevo = new Usuario();
                nuevo.setEmail(finalUsuarioId);
                nuevo.setNombre(finalName);
                nuevo.setActivo(true);
                nuevo.setPassword(passwordEncoder.encode("OAUTH2_USER_SECURE"));
                logger.info(" >>> AUTO-REGISTRO DESDE PROCESAR ANALISIS: {}", finalUsuarioId);
                return usuarioRepository.save(nuevo);
            });
        }

        AnalisisInputDTO inputDTO;
        List<TransaccionItemDTO> listaTransaccionesManual = new ArrayList<>();

        logger.info("👉 JSON RECIBIDO DE JS: {}", transaccionesJson);
        if (transaccionesJson != null && !transaccionesJson.isBlank()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());

                listaTransaccionesManual = mapper.readValue(
                        transaccionesJson,
                        new TypeReference<>() {}
                );
                logger.info("✅ Transacciones manuales parseadas con éxito: {}", listaTransaccionesManual.size());
            } catch (Exception e) {
                logger.error("❌ ERROR AL PARSEAR JSON DE TRANSACCIONES: {}", e.getMessage(), e);
            }
        }

        if (file != null && !file.isEmpty()) {
            CsvParseResult resultadoCsv = csvParserService.parsearTransaccionesConIngresos(file);

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
        } else {
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

        AnalisisOutputDTO resultadoAnalisis = nlpDataClient.analizarPerfil(inputDTO);

        try {
            AnalisisFinanciero analisis = AnalisisFinanciero.builder()
                    .usuarioId(usuarioId)
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

        return resultadoAnalisis;
    }

    private Double valorMetaOInversion(BigDecimal inversion, BigDecimal objetivo) {
        if (objetivo != null) return objetivo.doubleValue();
        if (inversion != null) return inversion.doubleValue();
        return 0.0;
    }
}