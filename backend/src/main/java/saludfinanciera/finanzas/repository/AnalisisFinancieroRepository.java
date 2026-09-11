package saludfinanciera.finanzas.repository;


import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import saludfinanciera.finanzas.model.AnalisisFinanciero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalisisFinancieroRepository extends JpaRepository<AnalisisFinanciero, Long> {

    // Consulta derivada para obtener todos los análisis de un usuario
    List<AnalisisFinanciero> findByUsuarioId(String usuarioId);

    // Solucion error LazyInitializationException
    @Query("SELECT DISTINCT a FROM AnalisisFinanciero a LEFT JOIN FETCH a.recomendaciones WHERE a.usuarioId = :usuarioId")
    List<AnalisisFinanciero> findByUsuarioIdWithRecomendaciones(@Param("usuarioId") String usuarioId);

    // Nueva forma para el detalle con JOIN FETCH para evitar LazyException:
    @Query("SELECT a FROM AnalisisFinanciero a LEFT JOIN FETCH a.recomendaciones LEFT JOIN FETCH a.resumenGastos WHERE a.id = :id")
    Optional<AnalisisFinanciero> findByIdWithDetails(@Param("id") Long id);

    // Trae los últimos 5 movimientos filtrados por el usuario autenticado
    Optional<AnalisisFinanciero> findTopByUsuarioIdOrderByIdDesc(String usuarioId);
}