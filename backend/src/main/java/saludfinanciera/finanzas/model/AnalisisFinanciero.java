package saludfinanciera.finanzas.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Entity
@Table(name = "analisis_financiero")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AnalisisFinanciero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include // Solo compara por el ID
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private String usuarioId;

    // --- DATOS FINANCIEROS DE ENTRADA ---
    @Column(name = "ingreso_mensual", nullable = false)
    private Double ingresoMensual;

    @Column(name = "nivel_endeudamiento", nullable = false)
    private Integer nivelEndeudamiento;

    @Column(name = "frecuencia_ahorro", nullable = false)
    private String frecuenciaAhorro;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @Column(name = "valor", nullable = false)
    private Double valor;

    // --- RESPUESTA DE PYTHON / IA ---
    @Column(name = "perfil_financiero")
    private String perfilFinanciero;

    @Column(name = "probabilidad")
    private Double probabilidad;

    @Column(name = "total_gastado")
    private Double totalGastado;

    @Column(name = "capacidad_ahorro_mensual")
    private Double capacidadAhorroMensual;

    @Column(name = "porcentaje_tasa_ahorro")
    private Double porcentajeTasaAhorro;

    @Column(name = "progreso_meta_ahorro")
    private Double progresoMetaAhorro;

    @Column(name = "meses_para_meta")
    private Double mesesParaMeta;

    // 1. Mapeo de Resumen de Gastos (Map<Categoria, Monto>)
    @ElementCollection
    @CollectionTable(name = "analisis_resumen_gastos", joinColumns = @JoinColumn(name = "analisis_id"))
    @MapKeyColumn(name = "categoria")
    @Column(name = "monto")
    @Builder.Default
    private Map<String, Double> resumenGastos = new HashMap<>();

    // 2. Mapeo de Recomendaciones
    @ElementCollection
    @CollectionTable(name = "analisis_recomendaciones", joinColumns = @JoinColumn(name = "analisis_id"))
    @Column(name = "recomendacion", length = 1000)
    @Builder.Default
    private Set<String> recomendaciones = new HashSet<>(); // Cambiado a Set evita duplicados

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "analisis", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Transaccion> transacciones = new ArrayList<>();

    @PrePersist
    protected void prePersist() {
        this.fechaCreacion = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    // --- MÉTODOS HELPER PARA LA RELACIÓN BIDIRECCIONAL ---
    public void addTransaccion(Transaccion transaccion) {
        transacciones.add(transaccion);
        transaccion.setAnalisis(this);
    }

    public void removeTransaccion(Transaccion transaccion) {
        transacciones.remove(transaccion);
        transaccion.setAnalisis(null);
    }
}