
package cl.nutrivida.ms.citas.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "planes",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_plan_referencia_compra",
            columnNames = "referencia_compra"
        )
    }
)
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "referencia_compra", nullable = false, length = 100)
    private String referenciaCompra;

    @Column(name = "orden_id", nullable = false, length = 50)
    private String ordenId;

    @Column(name = "paciente_correo", nullable = false, length = 150)
    private String pacienteCorreo;

    @Column(name = "codigo_servicio", nullable = false, length = 5)
    private String codigoServicio;

    // Se conserva la columna "nombre" de la versión integrada.
    // getNombreServicio() y setNombreServicio() son alias del mismo campo.
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "sesiones_totales", nullable = false)
    private int sesionesTotales;

    @Column(name = "monto_pagado", precision = 10, scale = 2)
    private BigDecimal montoPagado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_plan", nullable = false)
    private EstadoPlan estadoPlan = EstadoPlan.ACTIVO;

    @Column(length = 1000)
    private String motivo;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    public Plan() { }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReferenciaCompra() {
        return referenciaCompra;
    }

    public void setReferenciaCompra(String referenciaCompra) {
        this.referenciaCompra = referenciaCompra;
    }

    public String getOrdenId() {
        return ordenId;
    }

    public void setOrdenId(String ordenId) {
        this.ordenId = ordenId;
    }

    public String getPacienteCorreo() {
        return pacienteCorreo;
    }

    public void setPacienteCorreo(String pacienteCorreo) {
        this.pacienteCorreo = pacienteCorreo;
    }

    public String getCodigoServicio() {
        return codigoServicio;
    }

    public void setCodigoServicio(String codigoServicio) {
        this.codigoServicio = codigoServicio;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNombreServicio() {
        return nombre;
    }

    public void setNombreServicio(String nombreServicio) {
        this.nombre = nombreServicio;
    }

    public int getSesionesTotales() {
        return sesionesTotales;
    }

    public void setSesionesTotales(int sesionesTotales) {
        this.sesionesTotales = sesionesTotales;
    }

    public BigDecimal getMontoPagado() {
        return montoPagado;
    }

    public void setMontoPagado(BigDecimal montoPagado) {
        this.montoPagado = montoPagado;
    }

    public EstadoPlan getEstadoPlan() {
        return estadoPlan;
    }

    public void setEstadoPlan(EstadoPlan estadoPlan) {
        this.estadoPlan = estadoPlan;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }
}
