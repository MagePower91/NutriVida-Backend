// Un control del paciente: peso, talla y cintura en una fecha.
// El IMC no se guarda: se calcula al responder a partir del peso y la talla (dato derivado, 3FN).

package cl.nutrivida.clinico.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "mediciones")
public class Medicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ficha_id", nullable = false)
    private FichaClinica ficha;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal peso;          // kg

    @Column(nullable = false)
    private Integer talla;            // cm

    @Column(precision = 5, scale = 1)
    private BigDecimal cintura;       // cm, opcional

    @Column(length = 300)
    private String nota;

    @Column(length = 100)
    private String registradaPor;     // correo del nutricionista

    public Medicion() { }

    public Medicion(LocalDate fecha, BigDecimal peso, Integer talla, BigDecimal cintura, String nota, String registradaPor) {
        this.fecha = fecha;
        this.peso = peso;
        this.talla = talla;
        this.cintura = cintura;
        this.nota = nota;
        this.registradaPor = registradaPor;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public FichaClinica getFicha() { return ficha; }
    public void setFicha(FichaClinica ficha) { this.ficha = ficha; }
    public LocalDate getFecha() { return fecha; }
    public BigDecimal getPeso() { return peso; }
    public Integer getTalla() { return talla; }
    public BigDecimal getCintura() { return cintura; }
    public String getNota() { return nota; }
    public String getRegistradaPor() { return registradaPor; }
}
