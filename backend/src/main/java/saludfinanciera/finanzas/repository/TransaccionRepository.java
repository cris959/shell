package saludfinanciera.finanzas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import saludfinanciera.finanzas.model.Transaccion;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {
    List<Transaccion> findByUsuarioId(String usuarioId);

    // Buscar por ID solo si está activa
    Optional<Transaccion> findByIdAndActivoTrue(Long id);

    // Listar por usuario solo las transacciones activas
    List<Transaccion> findByUsuarioIdAndActivoTrue(String usuarioId);

    // Devuelve todas las transacciones de un usuario que aún tienen analisis_id en null
    List<Transaccion> findByUsuarioIdAndAnalisisIsNull(String usuarioId);

    // Trae los últimos 5 movimientos filtrados por el usuario autenticado
    List<Transaccion> findTop5ByUsuarioIdOrderByIdDesc(String usuarioId);
}
