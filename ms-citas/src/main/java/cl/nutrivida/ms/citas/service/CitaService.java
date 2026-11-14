package cl.nutrivida.ms.citas.service;

import cl.nutrivida.ms.citas.exception.HorarioNoLaboralException;
import cl.nutrivida.ms.citas.exception.HorarioOcupadoException;
import cl.nutrivida.ms.citas.model.Cita;
import cl.nutrivida.ms.citas.model.EstadoCita;
import cl.nutrivida.ms.citas.repository.CitaRepository;
import cl.nutrivida.ms.citas.repository.NutricionistaRepository;
import org.springframework.stereotype.Service;
import cl.nutrivida.ms.citas.model.Nutricionista;
import java.time.LocalTime;

import java.util.List;
import java.util.Optional;

@Service
public class CitaService {

    private final CitaRepository citaRepository;

    private final NutricionistaRepository nutricionistaRepository;

   public CitaService(
        CitaRepository citaRepository,
        NutricionistaRepository nutricionistaRepository) {

    this.citaRepository = citaRepository;
    this.nutricionistaRepository = nutricionistaRepository;
    }

    public List<Cita> listarCitas() {
        return citaRepository.findAll();
    }

    public Optional<Cita> buscarPorId(Long id) {
        return citaRepository.findById(id);
    }

  public Cita guardarCita(Cita cita) {

    if (cita.getNutricionista() != null && cita.getFechaHora() != null) {
    validarHorarioLaboral(cita);
    }

    if (cita.getNutricionista() != null && cita.getFechaHora() != null) {

        List<Cita> conflictos =
                citaRepository.findByNutricionistaAndFechaHoraAndEstadoCitaIn(
                        cita.getNutricionista(),
                        cita.getFechaHora(),
                        List.of(EstadoCita.POR_CONFIRMAR, EstadoCita.CONFIRMADA)
                );

        boolean horarioOcupado = conflictos.stream()
                .anyMatch(existente ->
                        !existente.getId().equals(cita.getId())
                );

        if (horarioOcupado) {
            throw new HorarioOcupadoException(
    "El nutricionista ya tiene una cita en ese horario."
    );
        }
    }

    return citaRepository.save(cita);
}


private void validarHorarioLaboral(Cita cita) {

    Nutricionista nutricionista = nutricionistaRepository
            .findById(cita.getNutricionista())
            .orElseThrow(() -> new IllegalArgumentException(
                    "No se encontró el nutricionista."
            ));

    // Obtener el día de la semana: lunes = 1, domingo = 7
    int dia = cita.getFechaHora().getDayOfWeek().getValue();

    // Obtener la hora solicitada
    LocalTime hora = cita.getFechaHora().toLocalTime();

    // Obtener el horario de atención
    LocalTime desde = LocalTime.parse(nutricionista.getDesde());
    LocalTime hasta = LocalTime.parse(nutricionista.getHasta());

    // Validar día y hora de atención
    if (!nutricionista.getDias().contains(dia)
            || hora.isBefore(desde)
            || !hora.plusHours(1).isAfter(hora)
            || hora.plusHours(1).isAfter(hasta)) {

        throw new HorarioNoLaboralException(
                "El nutricionista no atiende en el día u horario solicitado."
        );
    }
}


    public void eliminarCita(Long id) {
        citaRepository.deleteById(id);
    }
}