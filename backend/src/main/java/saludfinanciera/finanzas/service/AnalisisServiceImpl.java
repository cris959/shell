package saludfinanciera.finanzas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import saludfinanciera.finanzas.client.NlpDataClient;
import saludfinanciera.finanzas.dto.request.AnalisisInputDTO;
import saludfinanciera.finanzas.dto.request.TransaccionItemDTO;
import saludfinanciera.finanzas.dto.response.AnalisisOutputDTO;
import saludfinanciera.finanzas.model.AnalisisFinanciero;
import org.springframework.stereotype.Service;
import saludfinanciera.finanzas.model.Transaccion;
import saludfinanciera.finanzas.repository.AnalisisFinancieroRepository;
import saludfinanciera.finanzas.repository.TransaccionRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalisisServiceImpl implements AnalisisService{

    // Declaración manual del logger
    private static final Logger log = LoggerFactory.getLogger(AnalisisServiceImpl.class);

    private final TransaccionRepository transaccionRepository;
    private final AnalisisFinancieroRepository analisisRepository;
    private final NlpDataClient nlpDataClient;
    private final CsvParserService csvParserService;

    public AnalisisServiceImpl(TransaccionRepository transaccionRepository, AnalisisFinancieroRepository analisisRepository, NlpDataClient nlpDataClient, CsvParserService csvParserService) {
        this.transaccionRepository = transaccionRepository;
        this.analisisRepository = analisisRepository;
        this.nlpDataClient = nlpDataClient;
        this.csvParserService = csvParserService;
    }

    // =========================================================================
    // 1. Generar análisis de perfil (JSON)
    // =========================================================================
    @Override
    @Transactional
    public AnalisisOutputDTO generarAnalisisPerfil(String usuarioId, AnalisisInputDTO inputDTO) {
        if (usuarioId == null || usuarioId.isBlank()) {
            throw new IllegalArgumentException("El ID de usuario es obligatorio para registrar el análisis.");
        }

        log.info("🤖 Procesando análisis de perfil financiero con IA para el usuario: {}", usuarioId);

        AnalisisOutputDTO respuestaNlp = nlpDataClient.analizarPerfil(inputDTO);

        if (respuestaNlp == null) {
            throw new IllegalStateException("El servicio de análisis de IA no devolvió una respuesta válida.");
        }

        persistirAnalisis(usuarioId.trim(), inputDTO, respuestaNlp);

        return respuestaNlp;
    }

    // =========================================================================
    // 2. Procesar y analizar transacciones desde CSV
    // =========================================================================
    @Override
    @Transactional
    public AnalisisOutputDTO procesarYAnalizarCsv(
            String usuarioId,
            MultipartFile file,
            Double ingresoMensual,
            Double ahorroActual,
            Double metaAhorro,
            Double nivelEndeudamiento
    ) {
        if (usuarioId == null || usuarioId.isBlank()) {
            throw new IllegalArgumentException("El ID de usuario es obligatorio para procesar el archivo.");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo CSV no puede estar vacío.");
        }

        log.info("📄 Parseando archivo CSV y generando análisis para el usuario: {}", usuarioId);

        // 1. Parsear el CSV a DTOs
        List<TransaccionItemDTO> transaccionesCsv = csvParserService.parsearTransacciones(file);

        // 2. Persistir las transacciones en la BD
        List<Transaccion> entidadesTransacciones = transaccionesCsv.stream()
                .map(dto -> Transaccion.builder()
                        .usuarioId(usuarioId.trim())
                        .monto(dto.monto()) // Pasa directamente el BigDecimal del DTO
                        .categoria(dto.categoria())
                        .fechaTransaccion(dto.fecha() != null ? dto.fecha().atStartOfDay() : LocalDateTime.now())
                        .descripcion(dto.descripcion())
                        .tipo("EGRESO")
                        .analisis(null)
                        .build())
                .toList();

        transaccionRepository.saveAll(entidadesTransacciones);

        // 3. Manejo de nulos e instanciación del DTO para Python
        Double ingresoMensualVal = (ingresoMensual != null) ? ingresoMensual : 0.0;
        Double ahorroActualVal = (ahorroActual != null) ? ahorroActual : 0.0;
        Double metaAhorroVal = (metaAhorro != null) ? metaAhorro : 0.0;
        Integer nivelEndeudamientoInt = (nivelEndeudamiento != null) ? nivelEndeudamiento.intValue() : 0;

        AnalisisInputDTO inputDTO = new AnalisisInputDTO(
                ingresoMensualVal,
                ahorroActualVal,
                nivelEndeudamientoInt,
                "MENSUAL",
                "Análisis masivo de transacciones desde CSV",
                metaAhorroVal,
                transaccionesCsv
        );

        // 4. Invocar al servicio NLP
        AnalisisOutputDTO respuestaNlp = nlpDataClient.analizarPerfil(inputDTO);

        if (respuestaNlp == null) {
            throw new IllegalStateException("El servicio NLP no devolvió una respuesta válida al procesar el CSV.");
        }

        // 5. Persistir el análisis y vincularlo
        persistirAnalisis(usuarioId.trim(), inputDTO, respuestaNlp);

        return respuestaNlp;
    }

    // =========================================================================
    // 3. Obtener historial de análisis por usuario
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public List<AnalisisOutputDTO> obtenerAnalisisPorUsuario(String usuarioId) {
        if (usuarioId == null || usuarioId.isBlank()) {
            throw new IllegalArgumentException("El ID de usuario es obligatorio para consultar el historial.");
        }

        log.info("🔍 Consultando historial de análisis para el usuario: {}", usuarioId);

        //                        .findByUsuarioId
        return analisisRepository.findByUsuarioIdWithRecomendaciones(usuarioId.trim()).stream()
                .map(entidad -> mapToAnalisisOutputDTO(entidad))
                .toList();
    }

    // =========================================================================
    // Métodos Auxiliares Privados carga de alnalisis_id
    // =========================================================================
    private void persistirAnalisis(String usuarioId, AnalisisInputDTO inputDTO, AnalisisOutputDTO respuestaNlp) {
        Map<String, Double> resumenGastosConvertido = Map.of();
        if (respuestaNlp.resumenGastos() != null) {
            resumenGastosConvertido = respuestaNlp.resumenGastos().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> (entry.getValue() instanceof Number n) ? n.doubleValue() : 0.0
                    ));
        }

        // Resguardo para evitar pasar null a la columna NOT NULL de la Base de Datos
        Integer nivelEndeudamientoVal = (inputDTO.nivelEndeudamiento() != null)
                ? inputDTO.nivelEndeudamiento()
                : 0;

        AnalisisFinanciero analisis = AnalisisFinanciero.builder()
                .usuarioId(usuarioId)
                .ingresoMensual(inputDTO.ingresoMensual() != null ? inputDTO.ingresoMensual() : 0.0)
                .nivelEndeudamiento(nivelEndeudamientoVal) // <-- CAMBIO AQUÍ (Garantiza nunca enviar null)
                .frecuenciaAhorro(inputDTO.frecuenciaAhorro())
                .descripcion(inputDTO.descripcion())
                .valor(inputDTO.valor())
                .perfilFinanciero(respuestaNlp.perfilFinanciero() != null ? respuestaNlp.perfilFinanciero() : "DESCONOCIDO")
                .probabilidad(respuestaNlp.probabilidad() != null ? respuestaNlp.probabilidad() : 0.0)
                .totalGastado(respuestaNlp.totalGastado() != null ? respuestaNlp.totalGastado() : 0.0)
                .capacidadAhorroMensual(respuestaNlp.capacidadAhorroMensual() != null ? respuestaNlp.capacidadAhorroMensual() : 0.0)
                .porcentajeTasaAhorro(respuestaNlp.porcentajeTasaAhorro() != null ? respuestaNlp.porcentajeTasaAhorro() : 0.0)
                .progresoMetaAhorro(respuestaNlp.progresoMetaAhorro() != null ? respuestaNlp.progresoMetaAhorro() : 0.0)
                .mesesParaMeta(respuestaNlp.mesesParaMeta() != null ? respuestaNlp.mesesParaMeta() : 0.0)
                .resumenGastos(resumenGastosConvertido)
                .recomendaciones(respuestaNlp.recomendaciones() != null ? respuestaNlp.recomendaciones() : List.of())
                .build();

        // 1. Guardar el análisis y capturar la entidad persistida con su ID generado
        AnalisisFinanciero analisisGuardado = analisisRepository.save(analisis);

        // 2. Buscar las transacciones del usuario que no tienen análisis asignado
        List<Transaccion> transaccionesSinAnalisis = transaccionRepository.findByUsuarioIdAndAnalisisIsNull(usuarioId);

        // 3. Vincular el análisis recién creado y guardar en lote
        if (!transaccionesSinAnalisis.isEmpty()) {
            transaccionesSinAnalisis.forEach(t -> t.setAnalisis(analisisGuardado));
            transaccionRepository.saveAll(transaccionesSinAnalisis);
        }
    }

    // ================================================================================
    // Métodos Auxiliares Privados para construir el mapa y la lista de recomendaciones
    // ================================================================================

    private AnalisisOutputDTO mapToAnalisisOutputDTO(AnalisisFinanciero entidad) {
        // 1. Mapear de forma segura Map<String, Double> a Map<String, Object>
        Map<String, Object> resumenObj = new HashMap<>();
        if (entidad.getResumenGastos() != null) {
            resumenObj.putAll(entidad.getResumenGastos());
        }

        // 2. Retornar el DTO asegurando que no haya colecciones nulas
        return new AnalisisOutputDTO(
                entidad.getPerfilFinanciero(),
                entidad.getProbabilidad(),
                resumenObj,
                entidad.getRecomendaciones() != null ? entidad.getRecomendaciones() : List.of(),
                entidad.getTotalGastado(),
                entidad.getCapacidadAhorroMensual(),
                entidad.getPorcentajeTasaAhorro(),
                entidad.getProgresoMetaAhorro(),
                entidad.getMesesParaMeta()
        );
    }
}