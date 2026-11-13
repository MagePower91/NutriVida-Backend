package cl.nutrivida.ordenes.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "items_carrito")
public class ItemCarrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 9)
    private String runPaciente;

    @Column(nullable = false, length = 5)
    private String codigoServicio;

    @Column(nullable = false, length = 100)
    private String nombreServicio;

    @Column(nullable = false)
    private int precio;                    // precio final al momento de agregarlo (referencial)

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Column(length = 100)
    private String nutricionista;

    @Column(nullable = false)
    private LocalDateTime agregadoEn;

    public ItemCarrito() { }

    public ItemCarrito(String runPaciente, String codigoServicio, String nombreServicio, int precio,
                       LocalDateTime fechaHora, String nutricionista) {
        this.runPaciente = runPaciente;
        this.codigoServicio = codigoServicio;
        this.nombreServicio = nombreServicio;
        this.precio = precio;
        this.fechaHora = fechaHora;
        this.nutricionista = nutricionista;
        this.agregadoEn = LocalDateTime.now();
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

    public String getCodigoServicio() {
        return codigoServicio;
    }

    public void setCodigoServicio(String codigoServicio) {
        this.codigoServicio = codigoServicio;
    }

    public String getNombreServicio() {
        return nombreServicio;
    }

    public void setNombreServicio(String nombreServicio) {
        this.nombreServicio = nombreServicio;
    }

    public int getPrecio() {
        return precio;
    }

    public void setPrecio(int precio) {
        this.precio = precio;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getNutricionista() {
        return nutricionista;
    }

    public void setNutricionista(String nutricionista) {
        this.nutricionista = nutricionista;
    }

    public LocalDateTime getAgregadoEn() {
        return agregadoEn;
    }

    public void setAgregadoEn(LocalDateTime agregadoEn) {
        this.agregadoEn = agregadoEn;
    }
}