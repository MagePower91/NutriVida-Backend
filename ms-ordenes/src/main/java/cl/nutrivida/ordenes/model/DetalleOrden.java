package cl.nutrivida.ordenes.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "detalles_orden")
public class DetalleOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private Orden orden;

    @Column(nullable = false, length = 5)
    private String codigoServicio;

    @Column(nullable = false, length = 100)
    private String nombreServicio;

    @Column(nullable = false)
    private int precio;

    private LocalDateTime fechaHora;

    @Column(length = 100)
    private String nutricionista;

    // Referencias a los registros creados en ms-citas.
    private Long citaId;
    private Long planId;

    // Seguimiento del registro del plan en ms-citas.
    // NO_APLICA (no es plan), REGISTRADO o PENDIENTE. Opcional para no romper detalles antiguos.
    @Column(name = "estado_registro_plan", length = 20)
    private String estadoRegistroPlan = "NO_APLICA";

    @Column(name = "referencia_compra", length = 100)
    private String referenciaCompra;

    @Column(name = "sesiones_totales")
    private Integer sesionesTotales;

    public DetalleOrden() { }

    public DetalleOrden(String codigoServicio, String nombreServicio, int precio,
                        LocalDateTime fechaHora, String nutricionista) {
        this.codigoServicio = codigoServicio;
        this.nombreServicio = nombreServicio;
        this.precio = precio;
        this.fechaHora = fechaHora;
        this.nutricionista = nutricionista;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Orden getOrden() { return orden; }
    public void setOrden(Orden orden) { this.orden = orden; }

    public String getCodigoServicio() { return codigoServicio; }
    public void setCodigoServicio(String codigoServicio) { this.codigoServicio = codigoServicio; }

    public String getNombreServicio() { return nombreServicio; }
    public void setNombreServicio(String nombreServicio) { this.nombreServicio = nombreServicio; }

    public int getPrecio() { return precio; }
    public void setPrecio(int precio) { this.precio = precio; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public String getNutricionista() { return nutricionista; }
    public void setNutricionista(String nutricionista) { this.nutricionista = nutricionista; }

    public Long getCitaId() { return citaId; }
    public void setCitaId(Long citaId) { this.citaId = citaId; }

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }

    public String getEstadoRegistroPlan() { return estadoRegistroPlan; }
    public void setEstadoRegistroPlan(String estadoRegistroPlan) {
        this.estadoRegistroPlan = estadoRegistroPlan;
    }

    public String getReferenciaCompra() { return referenciaCompra; }
    public void setReferenciaCompra(String referenciaCompra) {
        this.referenciaCompra = referenciaCompra;
    }

    public Integer getSesionesTotales() { return sesionesTotales; }
    public void setSesionesTotales(Integer sesionesTotales) {
        this.sesionesTotales = sesionesTotales;
    }
}
