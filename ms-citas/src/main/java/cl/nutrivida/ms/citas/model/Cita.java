package cl.nutrivida.ms.citas.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "citas")
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String pacienteCorreo;

    private String correoConfirmacion;

    @Column(nullable = false)
    private String codigoServicio;

    private String nombreServicio;

    private String nutricionista;

    private boolean nutricionistaPreferido;

    @Column(length = 1000)
    private String motivo;

    private String ordenId;

    private String planId;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    private MetodoPago metodoPago;

    @Enumerated(EnumType.STRING)
    private EstadoCita estadoCita;

    @Enumerated(EnumType.STRING)
    private EstadoPago estadoPago;

    private LocalDateTime limiteConfirmacion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(precision = 10, scale = 2)
    private BigDecimal montoPagado;

    @Column(precision = 10, scale = 2)
    private BigDecimal montoReembolsado;

    private LocalDateTime creadaEn;

    private LocalDateTime fechaHoraAnterior;

    private Integer reprogramaciones = 0;

    public Cita() {
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPacienteCorreo() {
        return pacienteCorreo;
    }

    public void setPacienteCorreo(String pacienteCorreo) {
        this.pacienteCorreo = pacienteCorreo;
    }

    public String getCorreoConfirmacion() {
        return correoConfirmacion;
    }

    public void setCorreoConfirmacion(String correoConfirmacion) {
        this.correoConfirmacion = correoConfirmacion;
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

    public String getNutricionista() {
        return nutricionista;
    }

    public void setNutricionista(String nutricionista) {
        this.nutricionista = nutricionista;
    }

    public boolean isNutricionistaPreferido() {
        return nutricionistaPreferido;
    }

    public void setNutricionistaPreferido(boolean nutricionistaPreferido) {
        this.nutricionistaPreferido = nutricionistaPreferido;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getOrdenId() {
        return ordenId;
    }

    public void setOrdenId(String ordenId) {
        this.ordenId = ordenId;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(MetodoPago metodoPago) {
        this.metodoPago = metodoPago;
    }

    public EstadoCita getEstadoCita() {
        return estadoCita;
    }

    public void setEstadoCita(EstadoCita estadoCita) {
        this.estadoCita = estadoCita;
    }

    public EstadoPago getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(EstadoPago estadoPago) {
        this.estadoPago = estadoPago;
    }

    public LocalDateTime getLimiteConfirmacion() {
        return limiteConfirmacion;
    }

    public void setLimiteConfirmacion(LocalDateTime limiteConfirmacion) {
        this.limiteConfirmacion = limiteConfirmacion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public BigDecimal getMontoPagado() {
        return montoPagado;
    }

    public void setMontoPagado(BigDecimal montoPagado) {
        this.montoPagado = montoPagado;
    }

    public BigDecimal getMontoReembolsado() {
        return montoReembolsado;
    }

    public void setMontoReembolsado(BigDecimal montoReembolsado) {
        this.montoReembolsado = montoReembolsado;
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }

    public void setCreadaEn(LocalDateTime creadaEn) {
        this.creadaEn = creadaEn;
    }

    public LocalDateTime getFechaHoraAnterior() {
        return fechaHoraAnterior;
    }

    public void setFechaHoraAnterior(LocalDateTime fechaHoraAnterior) {
        this.fechaHoraAnterior = fechaHoraAnterior;
    }

    public Integer getReprogramaciones() {
        return reprogramaciones;
    }

    public void setReprogramaciones(Integer reprogramaciones) {
        this.reprogramaciones = reprogramaciones;
    }
}