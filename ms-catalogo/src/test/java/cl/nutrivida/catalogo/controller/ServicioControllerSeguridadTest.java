package cl.nutrivida.catalogo.controller;

import cl.nutrivida.catalogo.security.SecurityConfig;
import cl.nutrivida.catalogo.service.ServicioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Descontar cupos es una llamada interna: solo ms-ordenes (rol SERVICIO) o un administrador.
@WebMvcTest(ServicioController.class)
@Import(SecurityConfig.class)
class ServicioControllerSeguridadTest {

    @Autowired MockMvc mvc;
    @MockitoBean ServicioService servicioService;
    @MockitoBean JwtDecoder jwtDecoder;

    private static final String CUERPO = "{\"cantidad\": 1}";

    @Test
    void unPacienteNoPuedeDescontarCupos() throws Exception {
        mvc.perform(patch("/api/v1/servicios/TG001/cupos")
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PACIENTE"))))
                .andExpect(status().isForbidden());
        verify(servicioService, never()).reservarCupos(anyString(), anyInt());
    }

    @Test
    void sinTokenNoSePuedeDescontarCupos() throws Exception {
        mvc.perform(patch("/api/v1/servicios/TG001/cupos").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void msOrdenesConSuTokenDeServicioSiPuede() throws Exception {
        mvc.perform(patch("/api/v1/servicios/TG001/cupos")
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SERVICIO"))))
                .andExpect(status().isOk());
        verify(servicioService).reservarCupos("TG001", 1);
    }
}
