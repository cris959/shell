package saludfinanciera.finanzas.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import saludfinanciera.finanzas.dto.response.CsvParseResult;
import saludfinanciera.finanzas.dto.request.TransaccionItemDTO;
import saludfinanciera.finanzas.exception.CsvProcessingException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvParserService {

    private static final Logger logger = LoggerFactory.getLogger(CsvParserService.class);

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,                 // yyyy-MM-dd
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),        // dd/MM/yyyy
            DateTimeFormatter.ofPattern("dd-MM-yyyy")         // dd-MM-yyyy
    );

    public CsvParseResult parsearTransaccionesConIngresos(MultipartFile file) {
        List<TransaccionItemDTO> transacciones = new ArrayList<>();
        BigDecimal ingresoTotal = BigDecimal.ZERO;

        if (file == null || file.isEmpty()) {
            return new CsvParseResult(transacciones, ingresoTotal);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            // Limpiamos el stream por si el archivo CSV trae un BOM (Byte Order Mark) de UTF-8
            reader.mark(1);
            if (reader.read() != 0xFEFF) {
                reader.reset(); // Si no tiene BOM, regresa al inicio
            }

            // Configuración resiliente: Tolera mayúsculas/minúsculas y espacios en cabeceras
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setIgnoreSurroundingSpaces(true)
                    .build();

            CSVParser csvParser = new CSVParser(reader, format);

            // LOG DE DEPURACIÓN: Muestra si las cabeceras se mapearon bien
            System.out.printf("\uD83D\uDC49 Cabeceras detectadas en CSV: %s%n", csvParser.getHeaderNames());

            for (CSVRecord record : csvParser) {
                String fechaStr = obtenerCampoSeguro(record, "fecha");
                String descripcion = obtenerCampoSeguro(record, "descripcion");
                String montoStr = obtenerCampoSeguro(record, "monto");
                String categoria = obtenerCampoSeguro(record, "categoria");

                // LOG DE DEPURACIÓN: Ver qué lee exactamente cada fila
                System.out.printf("\uD83D\uDCC4 Leyendo fila - Fecha: %s, Desc: %s, Monto: %s%n", fechaStr, descripcion, montoStr);

                if (!montoStr.isBlank() && !descripcion.isBlank()) {
                    LocalDate fecha = parsearFecha(fechaStr);
                    BigDecimal monto = new BigDecimal(montoStr.replace("$", "").replace(",", ".").trim());

                    // Si la categoría viene vacía en el CSV, se envía null para que Python NLP la categorice
                    String categoriaFinal = categoria.isBlank() ? null : categoria.trim().toUpperCase();

                    // Si el monto es positivo, lo sumamos automáticamente como parte de los ingresos totales
                    if (monto.compareTo(BigDecimal.ZERO) > 0) {
                        ingresoTotal = ingresoTotal.add(monto);
                    }

                    transacciones.add(new TransaccionItemDTO(fecha, descripcion, monto, categoriaFinal));
                }
            }

            System.out.printf("✅ Total transacciones procesadas con éxito: %d%n", transacciones.size());
            System.out.printf("\uD83D\uDCB0 Ingreso total calculado desde CSV: %s%n", ingresoTotal);

        } catch (Exception e) {
            logger.error("❌ Error al procesar el archivo CSV: {}", e.getMessage(), e);
            throw new CsvProcessingException("Error al procesar el archivo CSV. Verifica las columnas (fecha, descripcion, monto, categoria).", e);
        }

        return new CsvParseResult(transacciones, ingresoTotal);
    }

    // Método tradicional mantenido por compatibilidad
    public List<TransaccionItemDTO> parsearTransacciones(MultipartFile file) {
        return parsearTransaccionesConIngresos(file).transacciones();
    }

    private String obtenerCampoSeguro(CSVRecord record, String nombreColumna) {
        return record.isMapped(nombreColumna) && record.get(nombreColumna) != null
                ? record.get(nombreColumna).trim()
                : "";
    }

    private LocalDate parsearFecha(String fechaStr) {
        if (fechaStr.isBlank()) {
            return LocalDate.now();
        }
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(fechaStr, formatter);
            } catch (DateTimeParseException ignored) {
                // Intenta con el siguiente formato
            }
        }
        throw new IllegalArgumentException("Formato de fecha no soportado en CSV: %s".formatted(fechaStr));
    }
}