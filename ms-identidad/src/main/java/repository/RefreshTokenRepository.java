package cl.nutrivida.identidad.repository;

import cl.nutrivida.identidad.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    @Modifying
    @Query("update RefreshToken t set t.revocado = true where t.usuario.run = :run")
    void revocarTodosDe(@Param("run") String run);

    @Modifying
    @Query("delete from RefreshToken t where t.usuario.run = :run")
    void eliminarTodosDe(@Param("run") String run);
}