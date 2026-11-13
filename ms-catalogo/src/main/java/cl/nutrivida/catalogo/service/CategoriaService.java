package cl.nutrivida.catalogo.service;

import cl.nutrivida.catalogo.dto.CatalogoDtos.CategoriaRequest;
import cl.nutrivida.catalogo.dto.CatalogoDtos.CategoriaResponse;
import cl.nutrivida.catalogo.exception.ApiException;
import cl.nutrivida.catalogo.model.Categoria;
import cl.nutrivida.catalogo.repository.CategoriaRepository;
import cl.nutrivida.catalogo.repository.ServicioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categorias;
    private final ServicioRepository servicios;

    public CategoriaService(CategoriaRepository categorias, ServicioRepository servicios) {
        this.categorias = categorias;
        this.servicios = servicios;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categorias.findAll().stream().map(CategoriaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtener(Long id) {
        return CategoriaResponse.de(buscar(id));
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest req) {
        String nombre = req.nombre().trim();
        if (categorias.existsByNombreIgnoreCase(nombre)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una categoría con ese nombre.");
        }
        return CategoriaResponse.de(categorias.save(new Categoria(nombre, req.descripcion())));
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest req) {
        Categoria c = buscar(id);
        String nombre = req.nombre().trim();
        boolean nombreDeOtra = categorias.findByNombreIgnoreCase(nombre)
                .filter(otra -> !otra.getId().equals(id))
                .isPresent();
        if (nombreDeOtra) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una categoría con ese nombre.");
        }
        c.setNombre(nombre);
        c.setDescripcion(req.descripcion());
        return CategoriaResponse.de(c);
    }

    @Transactional
    public void eliminar(Long id) {
        Categoria c = buscar(id);
        if (servicios.existsByCategoriaId(id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "No se puede eliminar: la categoría tiene servicios asociados.");
        }
        categorias.delete(c);
    }

    private Categoria buscar(Long id) {
        return categorias.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Categoría no encontrada."));
    }
}