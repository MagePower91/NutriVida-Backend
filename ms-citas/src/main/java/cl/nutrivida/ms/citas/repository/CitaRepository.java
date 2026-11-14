
package cl.nutrivida.ms.citas.repository;

import cl.nutrivida.ms.citas.model.Cita;
import cl.nutrivida.ms.citas.model.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    // Buscar citas de un nutricionista en un horario determinado
    List<Cita> findByNutricionistaAndFechaHora(
            String nutricionista,
            LocalDateTime fechaHora
    );

    // Buscar citas de un paciente
    List<Cita> findByPacienteCorreo(String pacienteCorreo);

    // Buscar citas según su estado
    List<Cita> findByEstadoCita(EstadoCita estadoCita);

    List<Cita> findByNutricionistaAndFechaHoraAndEstadoCitaIn(
    String nutricionista,
    LocalDateTime fechaHora,
    List<EstadoCita> estados
);


}
