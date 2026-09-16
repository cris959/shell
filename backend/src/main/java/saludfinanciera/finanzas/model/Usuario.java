package saludfinanciera.finanzas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Entity(name = "Usuario")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "usuarios")
@SQLDelete(sql = "UPDATE usuarios SET activo = false WHERE id = ?")
@SQLRestriction("activo = true")
public class Usuario implements UserDetails, org.springframework.security.oauth2.core.user.OAuth2User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private String email;

    private String password;

    @Column(name = "activo", nullable = false, columnDefinition = "boolean default true")
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "perfil_id")
    private Perfil perfil;


    @Column(precision = 12, scale = 2)
    private BigDecimal ingresoMensual;

    @Column(precision = 12, scale = 2)
    private BigDecimal valorTotalDeudas;

    @Column(precision = 12, scale = 2)
    private BigDecimal pagoMensualDeuda;

    @Column(precision = 12, scale = 2)
    private BigDecimal fondoEmergencia;

    private String frecuenciaAhorro; // Ej: "Semanal", "Quincenal", "Mensual"

    // nuenvos metodos para google //
    // Guarda los atributos de OAuth2 de forma transitoria si los necesitas
    @Transient
    private Map<String, Object> attributes;

    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    @Override
    public String getName() {
        return this.email; // O el identificador principal
    }
    // nuenvos metodos para google //

    // Funcion calculado para Thymeleaf (Devuelve un int o BigDecimal con el porcentaje)
    public int getNivelEndeudamiento() {
        if (ingresoMensual == null || ingresoMensual.compareTo(BigDecimal.ZERO) == 0 || valorTotalDeudas == null) {
            return 0;
        }
        // Ejemplo simple de cálculo: (Deudas / Ingreso) * 100 o ajustalo a tu lógica de negocio
        BigDecimal calculo = valorTotalDeudas
                .divide(ingresoMensual, 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return calculo.intValue();
    }

@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    if (perfil == null || perfil.getNombre() == null) {
        // Si no tiene perfil asignado (ej. usuarios de Google), les damos un rol por defecto
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
    return List.of(new SimpleGrantedAuthority(perfil.getNombre().name()));
}

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    // --- MÉTODOS USERDETAILS CORREGIDOS ---

    @Override
    public boolean isAccountNonExpired() {
        return true; // Podés agregar @SuppressWarnings("SameReturnValue") si querés silenciar la advertencia de valor fijo
    }

    @Override
    public boolean isEnabled() {
        // Usa el valor del campo 'activo' en lugar de hardcodear 'true'
        return Boolean.TRUE.equals(this.activo);
    }
}