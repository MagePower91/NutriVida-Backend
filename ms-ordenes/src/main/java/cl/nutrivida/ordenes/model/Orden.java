package cl.nutrivida.ordenes.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ordenes")
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 9)
    private String runPaciente;

    @Column(length = 100)
    private String correoPaciente;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private int total;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(length = 4)
    private String ultimos4Tarjeta;        // nunca se guarda la tarjeta completa

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<DetalleOrden> detalles = new ArrayList<>();

    public void agregarDetalle(DetalleOrden detalle) {
        detalle.setOrden(this);
        detalles.add(detalle);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRunPaciente() {
        return runPaciente;
    }

    public void setRunPaciente(String runPaciente) {
        this.runPaciente = runPaciente;
    }

    public String getCorreoPaciente() {
        return correoPaciente;
    }

    public void setCorreoPaciente(String correoPaciente) {
        this.correoPaciente = correoPaciente;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getUltimos4Tarjeta() {
        return ultimos4Tarjeta;
    }

    public void setUltimos4Tarjeta(String ultimos4Tarjeta) {
        this.ultimos4Tarjeta = ultimos4Tarjeta;
    }

    public List<DetalleOrden> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleOrden> detalles) {
        this.detalles = detalles;
    }
}