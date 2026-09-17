package saludfinanciera.finanzas.service;

import org.springframework.web.multipart.MultipartFile;
import saludfinanciera.finanzas.dto.response.AnalisisOutputDTO;

import java.math.BigDecimal;
import java.security.Principal;

public interface AnalisisServiceWeb {
    AnalisisOutputDTO procesarAnalisis(
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
            Principal principal
    );
}