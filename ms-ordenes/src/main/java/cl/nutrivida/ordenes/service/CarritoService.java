package cl.nutrivida.ordenes.service;

import cl.nutrivida.ordenes.client.CatalogoClient;
import cl.nutrivida.ordenes.client.CatalogoClient.ServicioCatalogo;
import cl.nutrivida.ordenes.dto.OrdenesDtos.AgregarItemRequest;
import cl.nutrivida.ordenes.dto.OrdenesDtos.CarritoResponse;
import cl.nutrivida.ordenes.dto.OrdenesDtos.ItemResponse;
import cl.nutrivida.ordenes.exception.ApiException;
import cl.nutrivida.ordenes.model.ItemCarrito;
import cl.nutrivida.ordenes.repository.ItemCarritoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CarritoService {

    public static final int MAXIMO_ITEMS = 10;

    private final ItemCarritoRepository items;
    private final CatalogoClient catalogo;

    public CarritoService(ItemCarritoRepository items, CatalogoClient catalogo) {
        this.items = items;
        this.catalogo = catalogo;
    }

    @Transactional(readOnly = true)
    public CarritoResponse ver(String run) {
        List<ItemResponse> lista = items.findByRunPacienteOrderByFechaHoraAsc(run).stream()
                .map(ItemResponse::de).toList();
        int total = lista.stream().mapToInt(ItemResponse::precio).sum();
        return new CarritoResponse(lista, lista.size(), total, MAXIMO_ITEMS);
    }

    @Transactional
    public ItemResponse agregar(String run, AgregarItemRequest req) {
        if (items.countByRunPaciente(run) >= MAXIMO_ITEMS) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "El carrito admite como máximo " + MAXIMO_ITEMS + " atenciones.");
        }
        LocalDateTime fechaHora = req.fechaHora().withSecond(0).withNano(0);
        if (items.existsByRunPacienteAndFechaHora(run, fechaHora)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya tienes en el carrito otra atención a esa misma hora.");
        }

        // Comunicación entre microservicios: el precio y la disponibilidad los decide ms-catalogo
        ServicioCatalogo s = catalogo.obtenerServicio(req.codigoServicio().trim().toUpperCase());
        if (!s.activo()) {
            throw new ApiException(HttpStatus.CONFLICT, "El servicio «" + s.nombre() + "» no está disponible.");
        }
        if (s.cupos() != null && s.cupos() <= 0) {
            throw new ApiException(HttpStatus.CONFLICT, "El taller «" + s.nombre() + "» no tiene cupos disponibles.");
        }

        ItemCarrito nuevo = new ItemCarrito(run, s.codigo(), s.nombre(), s.precioFinal(), fechaHora, req.nutricionista());
        return ItemResponse.de(items.save(nuevo));
    }

    @Transactional
    public void quitar(String run, Long id) {
        ItemCarrito item = items.findByIdAndRunPaciente(id, run)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Esa atención no está en tu carrito."));
        items.delete(item);
    }

    @Transactional
    public void vaciar(String run) {
        items.deleteByRunPaciente(run);
    }
}