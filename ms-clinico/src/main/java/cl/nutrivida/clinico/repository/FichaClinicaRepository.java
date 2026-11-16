package cl.nutrivida.clinico.repository;

import cl.nutrivida.clinico.model.FichaClinica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FichaClinicaRepository extends JpaRepository<FichaClinica, Long> {

    Optional<FichaClinica> findByPacienteCorreoIgnoreCase(String pacienteCorreo);

    boolean existsByPacienteCorreoIgnoreCase(String pacienteCorreo);

    // Pacientes asignados a un nutricionista
    List<FichaClinica> findByNutricionistaCorreoIgnoreCaseOrderByPacienteCorreoAsc(String nutricionistaCorreo);
}
