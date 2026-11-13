package cl.nutrivida.catalogo.repository;

import cl.nutrivida.catalogo.model.Servicio;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ServicioRepository extends JpaRepository<Servicio, String> {

    List<Servicio> findByActivoTrue();
    List<Servicio> findByCategoriaId(Long categoriaId);
    List<Servicio> findByCategoriaIdAndActivoTrue(Long categoriaId);
    boolean existsByCategoriaId(Long categoriaId);

    /** Bloquea la fila mientras se descuentan cupos, para que dos compras simultáneas no vendan el mismo cupo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Servicio s where s.codigo = :codigo")
    Optional<Servicio> buscarParaActualizar(@Param("codigo") String codigo);
}