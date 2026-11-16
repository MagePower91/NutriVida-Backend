package cl.nutrivida.ms.citas.repository;

import cl.nutrivida.ms.citas.model.Nutricionista;
import org.springframework.data.jpa.repository.JpaRepository;


public interface NutricionistaRepository extends JpaRepository<Nutricionista, String> {
}