package cl.nutrivida.catalogo.service;

import cl.nutrivida.catalogo.dto.CatalogoDtos.ServicioRequest;
import cl.nutrivida.catalogo.dto.CatalogoDtos.ServicioResponse;
import cl.nutrivida.catalogo.exception.ApiException;
import cl.nutrivida.catalogo.model.Categoria;
import cl.nutrivida.catalogo.model.Servicio;
import cl.nutrivida.catalogo.repository.CategoriaRepository;
import cl.nutrivida.catalogo.repository.ServicioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ServicioService {

    private final ServicioRepository servicios;
    private final CategoriaRepository categorias;

    public ServicioService(ServicioRepository servicios, CategoriaRepository categorias) {
        this.servicios = servicios;
        this.categorias = categorias;
    }

    @Transactional(readOnly = true)
    public List<ServicioResponse> listar(Long categoriaId, boolean soloActivos) {
        List<Servicio> lista;
        if (categoriaId != null) {
            lista = soloActivos ? servicios.findByCategoriaIdAndActivoTrue(categoriaId)
                    : servicios.findByCategoriaId(categoriaId);
        } else {
            lista = soloActivos ? servicios.findByActivoTrue() : servicios.findAll();
        }
        return lista.stream().map(ServicioResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ServicioResponse obtener(String codigo) {
        return ServicioResponse.de(buscar(codigo));
    }

    @Transactional
    public ServicioResponse crear(ServicioRequest req) {
        String codigo = req.codigo().trim().toUpperCase();
        if (servicios.existsById(codigo)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un servicio con el código " + codigo + ".");
        }
        Servicio s = new Servicio();
        s.setCodigo(codigo);
        aplicarDatos(s, req);
        return ServicioResponse.de(servicios.save(s));
    }

    @Transactional
    public ServicioResponse actualizar(String codigo, ServicioRequest req) {
        Servicio s = buscar(codigo);
        if (!req.codigo().trim().equalsIgnoreCase(s.getCodigo())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El código no se puede modificar.");
        }
        aplicarDatos(s, req);
        return ServicioResponse.de(s);
    }

    @Transactional
    public void eliminar(String codigo) {
        servicios.delete(buscar(codigo));
    }

    /** Lo usa ms-ordenes al pagar un taller: descuenta cupos disponibles. */
    @Transactional
    public ServicioResponse reservarCupos(String codigo, int cantidad) {
        Servicio s = servicios.buscarParaActualizar(codigo.trim().toUpperCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Servicio no encontrado."));
        if (!s.isActivo()) {
            throw new ApiException(HttpStatus.CONFLICT, "El servicio no está disponible.");
        }
        if (s.getCupos() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Este servicio no maneja cupos.");
        }
        if (s.getCupos() < cantidad) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "No quedan cupos suficientes. Disponibles: " + s.getCupos() + ".");
        }
        s.setCupos(s.getCupos() - cantidad);
        return ServicioResponse.de(s);
    }

    // ---------- reglas internas ----------

    private void aplicarDatos(Servicio s, ServicioRequest req) {
        Categoria cat = categorias.findById(req.categoriaId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "La categoría indicada no existe."));

        if (cat.getNombre().equalsIgnoreCase("Taller grupal") && req.cupos() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Los talleres grupales deben indicar sus cupos.");
        }
        if (cat.getNombre().equalsIgnoreCase("Plan especializado") && req.sesiones() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Los planes deben indicar el número de sesiones.");
        }

        s.setNombre(req.nombre().trim());
        s.setCategoria(cat);
        s.setDuracion(req.duracion());
        s.setModalidad(req.modalidad());
        s.setProfesional(req.profesional() == null ? "Nutricionista" : req.profesional());
        s.setPrecio(req.precio());
        s.setDescuento(req.descuento());
        s.setSesiones(req.sesiones());
        s.setCupos(req.cupos());
        s.setDescripcion(req.descripcion());
        s.setActivo(req.activo() == null || req.activo());
    }

    private Servicio buscar(String codigo) {
        return servicios.findById(codigo.trim().toUpperCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Servicio no encontrado."));
    }
}