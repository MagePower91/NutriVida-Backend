
package cl.nutrivida.ms.citas.repository;

import cl.nutrivida.ms.citas.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findByReferenciaCompra(String referenciaCompra);

    List<Plan> findByPacienteCorreoIgnoreCase(String pacienteCorreo);

    List<Plan> findByPacienteCorreoIgnoreCaseOrderByCreadoEnDesc(String pacienteCorreo);

    List<Plan> findAllByOrderByCreadoEnDesc();
}
