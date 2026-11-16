package cl.nutrivida.clinico.controller;

import cl.nutrivida.clinico.dto.FichaDtos.ActualizarFichaRequest;
import cl.nutrivida.clinico.dto.FichaDtos.CrearFichaRequest;
import cl.nutrivida.clinico.dto.FichaDtos.FichaResponse;
import cl.nutrivida.clinico.dto.FichaDtos.MedicionRequest;
import cl.nutrivida.clinico.dto.FichaDtos.PacienteResumen;
import cl.nutrivida.clinico.service.FichaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// El correo del paciente va en la ruta: {pacienteCorreo:.+} acepta el punto del dominio.
@RestController
@RequestMapping("/api/v1/fichas")
@Tag(name = "Fichas clínicas", description = "Mediciones, IMC, progreso y plan alimenticio: solo el paciente y su nutricionista")
public class FichaController {

    private final FichaService fichaService;

    public FichaController(FichaService fichaService) {
        this.fichaService = fichaService;
    }

    @Operation(summary = "Mi ficha", description = "Paciente: su ficha, mediciones, progreso y plan alimenticio.")
    @GetMapping("/mia")
    @PreAuthorize("hasRole('PACIENTE')")
    public FichaResponse miFicha(@AuthenticationPrincipal Jwt usuario) {
        return fichaService.miFicha(usuario);
    }

    @Operation(summary = "Mis pacientes", description = "Nutricionista: las fichas que tiene asignadas.")
    @GetMapping
    @PreAuthorize("hasRole('NUTRICIONISTA')")
    public List<PacienteResumen> misPacientes(@AuthenticationPrincipal Jwt usuario) {
        return fichaService.misPacientes(usuario);
    }

    @Operation(summary = "Ver la ficha de un paciente", description = "Solo el propio paciente o su nutricionista asignado.")
    @GetMapping("/{pacienteCorreo:.+}")
    @PreAuthorize("hasAnyRole('PACIENTE', 'NUTRICIONISTA')")
    public FichaResponse obtener(@PathVariable String pacienteCorreo, @AuthenticationPrincipal Jwt usuario) {
        return fichaService.obtener(pacienteCorreo, usuario);
    }

    @Operation(summary = "Abrir la ficha de un paciente", description = "Nutricionista: queda como su nutricionista asignado.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('NUTRICIONISTA')")
    public FichaResponse crear(@Valid @RequestBody CrearFichaRequest datos, @AuthenticationPrincipal Jwt usuario) {
        return fichaService.crear(datos, usuario);
    }

    @Operation(summary = "Actualizar objetivo, antecedentes y plan alimenticio", description = "Solo el nutricionista asignado.")
    @PutMapping("/{pacienteCorreo:.+}")
    @PreAuthorize("hasRole('NUTRICIONISTA')")
    public FichaResponse actualizar(@PathVariable String pacienteCorreo, @Valid @RequestBody ActualizarFichaRequest datos,
                                    @AuthenticationPrincipal Jwt usuario) {
        return fichaService.actualizar(pacienteCorreo, datos, usuario);
    }

    @Operation(summary = "Registrar una medición", description = "Solo el nutricionista asignado. El IMC se calcula solo.")
    @PostMapping("/{pacienteCorreo:.+}/mediciones")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('NUTRICIONISTA')")
    public FichaResponse registrarMedicion(@PathVariable String pacienteCorreo, @Valid @RequestBody MedicionRequest datos,
                                           @AuthenticationPrincipal Jwt usuario) {
        return fichaService.registrarMedicion(pacienteCorreo, datos, usuario);
    }

    @Operation(summary = "Eliminar una medición", description = "Solo el nutricionista asignado.")
    @DeleteMapping("/{pacienteCorreo:.+}/mediciones/{medicionId}")
    @PreAuthorize("hasRole('NUTRICIONISTA')")
    public FichaResponse eliminarMedicion(@PathVariable String pacienteCorreo, @PathVariable Long medicionId,
                                          @AuthenticationPrincipal Jwt usuario) {
        return fichaService.eliminarMedicion(pacienteCorreo, medicionId, usuario);
    }
}
