// Plan de varias sesiones contratado por un paciente (RN-13). Se paga completo online, una sola vez,
// y el paciente agenda sus sesiones de a una sin volver a pagar mientras le queden sesiones.
// Las sesiones son citas normales con planId y precio 0 (el pago está en el plan).

package cl.nutrivida.ms.citas.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "planes")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String pacienteCorreo;

    @Column(nullable = false, length = 5)
    private String codigoServicio;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private int sesionesTotales;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montoPagado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPlan estadoPlan = EstadoPlan.ACTIVO;

    private String ordenId;

    @Column(length = 1000)
    private String motivo;

    private LocalDateTime creadoEn;

    public Plan() { }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPacienteCorreo() { return pacienteCorreo; }
    public void setPacienteCorreo(String pacienteCorreo) { this.pacienteCorreo = pacienteCorreo; }
    public String getCodigoServicio() { return codigoServicio; }
    public void setCodigoServicio(String codigoServicio) { this.codigoServicio = codigoServicio; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public int getSesionesTotales() { return sesionesTotales; }
    public void setSesionesTotales(int sesionesTotales) { this.sesionesTotales = sesionesTotales; }
    public BigDecimal getMontoPagado() { return montoPagado; }
    public void setMontoPagado(BigDecimal montoPagado) { this.montoPagado = montoPagado; }
    public EstadoPlan getEstadoPlan() { return estadoPlan; }
    public void setEstadoPlan(EstadoPlan estadoPlan) { this.estadoPlan = estadoPlan; }
    public String getOrdenId() { return ordenId; }
    public void setOrdenId(String ordenId) { this.ordenId = ordenId; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
}
