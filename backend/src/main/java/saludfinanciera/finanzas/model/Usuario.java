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

import java.util.Collection;
import java.util.List;

@Entity(name = "Usuario")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "usuarios")
@SQLDelete(sql = "UPDATE usuarios SET activo = false WHERE id = ?")
@SQLRestriction("activo = true")
public class Usuario implements UserDetails {

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

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (perfil == null || perfil.getNombre() == null) {
            return List.of();
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