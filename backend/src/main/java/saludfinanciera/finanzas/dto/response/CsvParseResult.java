package saludfinanciera.finanzas.dto.response;

import saludfinanciera.finanzas.dto.request.TransaccionItemDTO;

import java.math.BigDecimal;
import java.util.List;

public record CsvParseResult(
        List<TransaccionItemDTO> transacciones,
        BigDecimal ingresoTotalCalculado
) {}