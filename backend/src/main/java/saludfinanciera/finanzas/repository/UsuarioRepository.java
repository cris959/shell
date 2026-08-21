package saludfinanciera.finanzas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;
import saludfinanciera.finanzas.model.Usuario;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Retorna UserDetails para la autenticación en Spring Security
    Optional<UserDetails> findByEmail(String email);

    // Funcion utilitario opcional para comprobar si el email ya existe al registrarse
    Boolean existsByEmail(String email);
}
