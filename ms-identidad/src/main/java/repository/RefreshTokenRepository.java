package cl.nutrivida.identidad.repository;

import cl.nutrivida.identidad.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
}