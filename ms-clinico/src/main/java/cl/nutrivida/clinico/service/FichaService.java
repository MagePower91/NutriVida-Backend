// Privacidad de los datos de salud (caso NutriVida): la ficha solo la ven el paciente y su nutricionista.
// Solo el nutricionista asignado la crea o la edita; el paciente solo la consulta.
// Administración y recepción no ven datos clínicos.

package cl.nutrivida.clinico.service;

import cl.nutrivida.clinico.dto.FichaDtos.ActualizarFichaRequest;
import cl.nutrivida.clinico.dto.FichaDtos.CrearFichaRequest;
import cl.nutrivida.clinico.dto.FichaDtos.FichaResponse;
import cl.nutrivida.clinico.dto.FichaDtos.MedicionRequest;
import cl.nutrivida.clinico.dto.FichaDtos.PacienteResumen;
import cl.nutrivida.clinico.exception.ApiException;
import cl.nutrivida.clinico.model.FichaClinica;
import cl.nutrivida.clinico.model.Medicion;
import cl.nutrivida.clinico.repository.FichaClinicaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class FichaService {

    static final String PACIENTE = "PACIENTE";
    static final String NUTRICIONISTA = "NUTRICIONISTA";

    private final FichaClinicaRepository fichas;
    private final Clock reloj;

    public FichaService(FichaClinicaRepository fichas, Clock reloj) {
        this.fichas = fichas;
        this.reloj = reloj;
    }

    // El paciente consulta su propia ficha, sus mediciones y su plan alimenticio.
    @Transactional(readOnly = true)
    public FichaResponse miFicha(Jwt usuario) {
        return fichas.findByPacienteCorreoIgnoreCase(correoDe(usuario))
                .map(FichaResponse::de)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Aún no tienes ficha clínica. Tu nutricionista la crea en tu primera consulta."));
    }

    // "Mis pacientes": las fichas asignadas al nutricionista de la sesión.
    @Transactional(readOnly = true)
    public List<PacienteResumen> misPacientes(Jwt usuario) {
        return fichas.findByNutricionistaCorreoIgnoreCaseOrderByPacienteCorreoAsc(correoDe(usuario)).stream()
                .map(PacienteResumen::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public FichaResponse obtener(String pacienteCorreo, Jwt usuario) {
        FichaClinica ficha = buscar(pacienteCorreo);
        if (!puedeVer(ficha, usuario)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Los datos clínicos solo los ven el paciente y su nutricionista.");
        }
        return FichaResponse.de(ficha);
    }

    // El nutricionista abre la ficha de un paciente nuevo y queda como su nutricionista asignado.
    @Transactional
    public FichaResponse crear(CrearFichaRequest datos, Jwt usuario) {
        exigirNutricionista(usuario);
        String paciente = datos.pacienteCorreo().trim().toLowerCase();
        if (fichas.existsByPacienteCorreoIgnoreCase(paciente)) {
            throw new ApiException(HttpStatus.CONFLICT, "Este paciente ya tiene una ficha clínica.");
        }
        FichaClinica ficha = new FichaClinica(paciente, correoDe(usuario), usuario.getClaimAsString("nombre"), ahora());
        ficha.setObjetivo(datos.objetivo());
        ficha.setAntecedentes(datos.antecedentes());
        return FichaResponse.de(fichas.save(ficha));
    }

    // Objetivo, antecedentes y plan alimenticio. Si el plan cambia, se registra cuándo.
    @Transactional
    public FichaResponse actualizar(String pacienteCorreo, ActualizarFichaRequest datos, Jwt usuario) {
        FichaClinica ficha = fichaEditable(pacienteCorreo, usuario);
        ficha.setObjetivo(datos.objetivo());
        ficha.setAntecedentes(datos.antecedentes());
        if (!Objects.equals(datos.planAlimenticio(), ficha.getPlanAlimenticio())) {
            ficha.setPlanAlimenticio(datos.planAlimenticio());
            ficha.setPlanActualizadoEn(ahora());
        }
        return FichaResponse.de(fichas.save(ficha));
    }

    @Transactional
    public FichaResponse registrarMedicion(String pacienteCorreo, MedicionRequest datos, Jwt usuario) {
        FichaClinica ficha = fichaEditable(pacienteCorreo, usuario);
        ficha.agregarMedicion(new Medicion(datos.fecha(), datos.peso(), datos.talla(), datos.cintura(), datos.nota(), correoDe(usuario)));
        ficha.getMediciones().sort((medicionA, medicionB) -> medicionA.getFecha().compareTo(medicionB.getFecha()));
        return FichaResponse.de(fichas.save(ficha));
    }

    @Transactional
    public FichaResponse eliminarMedicion(String pacienteCorreo, Long medicionId, Jwt usuario) {
        FichaClinica ficha = fichaEditable(pacienteCorreo, usuario);
        boolean quitada = ficha.getMediciones().removeIf(medicion -> medicionId.equals(medicion.getId()));
        if (!quitada) throw new ApiException(HttpStatus.NOT_FOUND, "La medición no existe en esta ficha.");
        return FichaResponse.de(fichas.save(ficha));
    }

    // ---------- Reglas de acceso ----------

    static boolean puedeVer(FichaClinica ficha, Jwt usuario) {
        return switch (rolDe(usuario)) {
            case PACIENTE -> ficha.getPacienteCorreo().equalsIgnoreCase(correoDe(usuario));
            case NUTRICIONISTA -> esSuNutricionista(ficha, usuario);
            default -> false; // administración y recepción no ven datos clínicos
        };
    }

    private static boolean esSuNutricionista(FichaClinica ficha, Jwt usuario) {
        return NUTRICIONISTA.equals(rolDe(usuario)) && ficha.getNutricionistaCorreo().equalsIgnoreCase(correoDe(usuario));
    }

    private FichaClinica fichaEditable(String pacienteCorreo, Jwt usuario) {
        exigirNutricionista(usuario);
        FichaClinica ficha = buscar(pacienteCorreo);
        if (!esSuNutricionista(ficha, usuario)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo el nutricionista asignado puede editar esta ficha.");
        }
        return ficha;
    }

    private static void exigirNutricionista(Jwt usuario) {
        if (!NUTRICIONISTA.equals(rolDe(usuario))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo un nutricionista puede registrar datos clínicos.");
        }
    }

    private FichaClinica buscar(String pacienteCorreo) {
        return fichas.findByPacienteCorreoIgnoreCase(pacienteCorreo.trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Este paciente no tiene ficha clínica."));
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    private static String rolDe(Jwt usuario) {
        String rol = usuario.getClaimAsString("rol");
        return rol == null ? "" : rol;
    }

    private static String correoDe(Jwt usuario) {
        return usuario.getClaimAsString("correo");
    }
}
