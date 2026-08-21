package saludfinanciera.finanzas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import saludfinanciera.finanzas.model.Perfil;
import saludfinanciera.finanzas.model.PerfilNombre;

import java.util.Optional;

@Repository
public interface PerfilRepository extends JpaRepository<Perfil, Long> {

    // Busca un perfil específico por su nombre de Enum (ej: ROLE_USER)
    Optional<Perfil> findByNombre(PerfilNombre nombre);
}
