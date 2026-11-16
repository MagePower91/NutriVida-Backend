// Ficha clínica de un paciente: una por paciente, asignada a su nutricionista.
// Guarda el objetivo, los antecedentes, el plan alimenticio y el historial de mediciones.

package cl.nutrivida.clinico.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fichas_clinicas")
public class FichaClinica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String pacienteCorreo;

    // El nutricionista se identifica por el correo de su cuenta (claim "correo" del token).
    @Column(nullable = false, length = 100)
    private String nutricionistaCorreo;

    @Column(length = 100)
    private String nutricionistaNombre;

    @Column(length = 500)
    private String objetivo;

    @Column(length = 1000)
    private String antecedentes;

    @Column(length = 4000)
    private String planAlimenticio;

    private LocalDateTime planActualizadoEn;

    @Column(nullable = false)
    private LocalDateTime creadaEn;

    @OneToMany(mappedBy = "ficha", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fecha ASC, id ASC")
    private List<Medicion> mediciones = new ArrayList<>();

    public FichaClinica() { }

    public FichaClinica(String pacienteCorreo, String nutricionistaCorreo, String nutricionistaNombre, LocalDateTime creadaEn) {
        this.pacienteCorreo = pacienteCorreo;
        this.nutricionistaCorreo = nutricionistaCorreo;
        this.nutricionistaNombre = nutricionistaNombre;
        this.creadaEn = creadaEn;
    }

    public void agregarMedicion(Medicion medicion) {
        medicion.setFicha(this);
        mediciones.add(medicion);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPacienteCorreo() { return pacienteCorreo; }
    public String getNutricionistaCorreo() { return nutricionistaCorreo; }
    public String getNutricionistaNombre() { return nutricionistaNombre; }
    public String getObjetivo() { return objetivo; }
    public void setObjetivo(String objetivo) { this.objetivo = objetivo; }
    public String getAntecedentes() { return antecedentes; }
    public void setAntecedentes(String antecedentes) { this.antecedentes = antecedentes; }
    public String getPlanAlimenticio() { return planAlimenticio; }
    public void setPlanAlimenticio(String planAlimenticio) { this.planAlimenticio = planAlimenticio; }
    public LocalDateTime getPlanActualizadoEn() { return planActualizadoEn; }
    public void setPlanActualizadoEn(LocalDateTime planActualizadoEn) { this.planActualizadoEn = planActualizadoEn; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
    public List<Medicion> getMediciones() { return mediciones; }
}
