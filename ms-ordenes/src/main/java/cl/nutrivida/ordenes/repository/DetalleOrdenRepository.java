package cl.nutrivida.ordenes.repository;

import cl.nutrivida.ordenes.model.DetalleOrden;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleOrdenRepository extends JpaRepository<DetalleOrden, Long> {

    @EntityGraph(attributePaths = "orden")
    List<DetalleOrden> findByEstadoRegistroPlanAndOrden_Estado(
            String estadoRegistroPlan,
            String estadoOrden
    );
}
