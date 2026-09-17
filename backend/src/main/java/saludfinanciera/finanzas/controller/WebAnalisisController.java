package saludfinanciera.finanzas.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import saludfinanciera.finanzas.dto.response.AnalisisOutputDTO;
import saludfinanciera.finanzas.service.AnalisisServiceWeb;


import java.math.BigDecimal;
import java.security.Principal;
import java.util.*;

@Controller
public class WebAnalisisController {

    private static final Logger logger = LoggerFactory.getLogger(WebAnalisisController.class);

    private final AnalisisServiceWeb analisisService;

    public WebAnalisisController(AnalisisServiceWeb analisisService) {
        this.analisisService = analisisService;
    }


    // 1. Mostrar la vista del formulario de análisis
    @GetMapping("/analisis")
    public String mostrarAnalisis(Model model) {
        return "nuevo-analisis";
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
            @RequestParam(required = false) String transaccionesJson,
            Principal principal,
            Model model) {

        try {
            AnalisisOutputDTO resultadoAnalisis = analisisService.procesarAnalisis(
                    ingresoMensual, totalDeudas, frecuenciaAhorro, montoInversion,
                    objetivoPresupuesto, pagoMensualDeuda, cantidadSuscripciones,
                    fondoEmergencia, file, transaccionesJson, principal
            );

            model.addAttribute("resultado", resultadoAnalisis);
            return "resultado-analisis";

        } catch (Exception e) {
            logger.error("❌ Error al procesar el análisis: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al procesar el análisis: %s".formatted(e.getMessage()));
            return "nuevo-analisis";
        }
    }
}