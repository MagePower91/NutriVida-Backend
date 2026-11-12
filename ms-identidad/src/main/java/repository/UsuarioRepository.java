package cl.nutrivida.identidad.repository;

import cl.nutrivida.identidad.model.Rol;
import cl.nutrivida.identidad.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {
    Optional<Usuario> findByCorreoIgnoreCase(String correo);
    boolean existsByCorreoIgnoreCase(String correo);
    long countByRolAndActivoTrue(Rol rol);
}