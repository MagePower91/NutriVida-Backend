package cl.nutrivida.ms.citas.repository;

import cl.nutrivida.ms.citas.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    List<Plan> findByPacienteCorreoIgnoreCaseOrderByCreadoEnDesc(String pacienteCorreo);

    List<Plan> findAllByOrderByCreadoEnDesc();
}
