package cl.nutrivida.ms.citas.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "nutricionistas")
public class Nutricionista {

    @Id
    private String id;

    @Column(nullable = false)
    private String nombre;

    private String especialidad;

    private String desde;

    private String hasta;

    private String correo;

    @Column(length = 1000)
    private String resena;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "nutricionista_dias",
        joinColumns = @JoinColumn(name = "nutricionista_id")
    )
    @Column(name = "dia")
    private List<Integer> dias = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "nutricionista_etiquetas",
        joinColumns = @JoinColumn(name = "nutricionista_id")
    )
    @Column(name = "etiqueta")
    private List<String> etiquetas = new ArrayList<>();

    public Nutricionista() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public String getDesde() {
        return desde;
    }

    public void setDesde(String desde) {
        this.desde = desde;
    }

    public String getHasta() {
        return hasta;
    }

    public void setHasta(String hasta) {
        this.hasta = hasta;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getResena() {
        return resena;
    }

    public void setResena(String resena) {
        this.resena = resena;
    }

    public List<Integer> getDias() {
        return dias;
    }

    public void setDias(List<Integer> dias) {
        this.dias = dias;
    }

    public List<String> getEtiquetas() {
        return etiquetas;
    }

    public void setEtiquetas(List<String> etiquetas) {
        this.etiquetas = etiquetas;
    }
}