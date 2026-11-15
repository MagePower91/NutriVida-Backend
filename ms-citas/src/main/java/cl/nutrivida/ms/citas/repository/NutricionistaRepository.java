package cl.nutrivida.ms.citas.repository;

import cl.nutrivida.ms.citas.model.Nutricionista;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;

public interface NutricionistaRepository extends JpaRepository<Nutricionista, String> {

    // Relaciona la cuenta del token (correo) con su ficha de nutricionista
    Optional<Nutricionista> findByCorreoIgnoreCase(String correo);
}