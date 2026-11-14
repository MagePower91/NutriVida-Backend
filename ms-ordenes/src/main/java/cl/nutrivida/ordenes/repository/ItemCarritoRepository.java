package cl.nutrivida.ordenes.repository;

import cl.nutrivida.ordenes.model.ItemCarrito;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ItemCarritoRepository extends JpaRepository<ItemCarrito, Long> {
    List<ItemCarrito> findByRunPacienteOrderByFechaHoraAsc(String runPaciente);
    long countByRunPaciente(String runPaciente);
    boolean existsByRunPacienteAndFechaHora(String runPaciente, LocalDateTime fechaHora);
    Optional<ItemCarrito> findByIdAndRunPaciente(Long id, String runPaciente);
    void deleteByRunPaciente(String runPaciente);
}