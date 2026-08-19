package saludfinanciera.finanzas.repository;


import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import saludfinanciera.finanzas.model.AnalisisFinanciero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalisisFinancieroRepository extends JpaRepository<AnalisisFinanciero, Long> {

    // Consulta derivada para obtener todos los análisis de un usuario
    List<AnalisisFinanciero> findByUsuarioId(String usuarioId);

    // Solucion error LazyInitializationException
    @Query("SELECT DISTINCT a FROM AnalisisFinanciero a LEFT JOIN FETCH a.recomendaciones WHERE a.usuarioId = :usuarioId")
    List<AnalisisFinanciero> findByUsuarioIdWithRecomendaciones(@Param("usuarioId") String usuarioId);
}