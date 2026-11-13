package cl.nutrivida.ordenes.repository;

import cl.nutrivida.ordenes.model.Orden;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrdenRepository extends JpaRepository<Orden, Long> {
    List<Orden> findByRunPacienteOrderByFechaDesc(String runPaciente);
    List<Orden> findAllByOrderByFechaDesc();
}