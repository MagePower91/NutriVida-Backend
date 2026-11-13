//Aquí están todas las reglas de negocio.

package cl.nutrivida.identidad.service;

import cl.nutrivida.identidad.dto.AuthDtos.RegistroRequest;
import cl.nutrivida.identidad.dto.UsuarioRequest;
import cl.nutrivida.identidad.dto.UsuarioResponse;
import cl.nutrivida.identidad.exception.ApiException;
import cl.nutrivida.identidad.model.Rol;
import cl.nutrivida.identidad.model.Usuario;
import cl.nutrivida.identidad.repository.RefreshTokenRepository;
import cl.nutrivida.identidad.repository.UsuarioRepository;
import cl.nutrivida.identidad.util.Run;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository usuarios, RefreshTokenRepository refreshTokens, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(String tipo) {
        List<Usuario> lista = (tipo == null || tipo.isBlank())
                ? usuarios.findAll()
                : usuarios.findByRol(Rol.desdeTexto(tipo));
        return lista.stream().map(UsuarioResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(String run) {
        return UsuarioResponse.de(buscar(run));
    }

    /** Registro público: siempre crea un Paciente. */
    @Transactional
    public Usuario registrarPaciente(RegistroRequest r) {
        return crearEntidad(new UsuarioRequest(r.run(), r.nombre(), r.apellidos(), r.correo(),
                r.contrasena(), "Paciente", r.region(), r.comuna(), r.direccion(), r.fechaNacimiento()));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest req) {
        return UsuarioResponse.de(crearEntidad(req));
    }

    @Transactional
    public UsuarioResponse actualizar(String run, UsuarioRequest req, String runSesion) {
        Usuario u = buscar(run);
        if (!Run.normalizar(req.run()).equals(u.getRun())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El RUN no se puede modificar.");
        }
        String correo = req.correo().trim().toLowerCase();
        boolean correoDeOtro = usuarios.findByCorreoIgnoreCase(correo)
                .filter(otro -> !otro.getRun().equals(u.getRun()))
                .isPresent();
        if (correoDeOtro) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo.");
        }
        Rol nuevoRol = Rol.desdeTexto(req.tipoUsuario());
        if (u.getRol() == Rol.ADMINISTRADOR && nuevoRol != Rol.ADMINISTRADOR) {
            if (u.getRun().equals(runSesion)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "No puedes quitarte tu propio rol de administrador.");
            }
            protegerUltimoAdmin(u);
        }
        aplicarDatos(u, req, nuevoRol);
        if (req.contrasena() != null && !req.contrasena().isBlank()) {
            validarLargoClave(req.contrasena());
            u.setContrasena(encoder.encode(req.contrasena()));
        }
        return UsuarioResponse.de(u);
    }

    @Transactional
    public UsuarioResponse cambiarEstado(String run, boolean activo, String runSesion) {
        Usuario u = buscar(run);
        if (!activo) {
            if (u.getRun().equals(runSesion)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "No puedes desactivar tu propia cuenta.");
            }
            protegerUltimoAdmin(u);
            refreshTokens.revocarTodosDe(u.getRun()); // cierra sus sesiones
        }
        u.setActivo(activo);
        return UsuarioResponse.de(u);
    }

    @Transactional
    public void eliminar(String run, String runSesion) {
        Usuario u = buscar(run);
        if (u.getRun().equals(runSesion)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No puedes eliminar tu propia cuenta.");
        }
        protegerUltimoAdmin(u);
        refreshTokens.eliminarTodosDe(u.getRun());
        usuarios.delete(u);
    }

    // ---------- reglas internas ----------

    private Usuario crearEntidad(UsuarioRequest req) {
        String run = Run.normalizar(req.run());
        if (!Run.esValido(run)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El RUN no es válido.");
        }
        if (usuarios.existsById(run)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un usuario con ese RUN.");
        }
        if (usuarios.existsByCorreoIgnoreCase(req.correo().trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo.");
        }
        if (req.contrasena() == null || req.contrasena().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña es obligatoria.");
        }
        validarLargoClave(req.contrasena());

        Usuario u = new Usuario();
        u.setRun(run);
        aplicarDatos(u, req, Rol.desdeTexto(req.tipoUsuario()));
        u.setContrasena(encoder.encode(req.contrasena()));
        u.setActivo(true);
        return usuarios.save(u);
    }

    private void aplicarDatos(Usuario u, UsuarioRequest req, Rol rol) {
        u.setNombre(req.nombre().trim());
        u.setApellidos(req.apellidos().trim());
        u.setCorreo(req.correo().trim().toLowerCase());
        u.setRol(rol);
        u.setRegion(req.region());
        u.setComuna(req.comuna());
        u.setDireccion(req.direccion());
        u.setFechaNacimiento(req.fechaNacimiento());
    }

    private void protegerUltimoAdmin(Usuario u) {
        if (u.getRol() == Rol.ADMINISTRADOR && u.isActivo()
                && usuarios.countByRolAndActivoTrue(Rol.ADMINISTRADOR) <= 1) {
            throw new ApiException(HttpStatus.CONFLICT, "Debe quedar al menos un administrador activo.");
        }
    }

    private void validarLargoClave(String clave) {
        if (clave.length() < 4 || clave.length() > 10) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña debe tener entre 4 y 10 caracteres.");
        }
    }

    private Usuario buscar(String run) {
        return usuarios.findById(Run.normalizar(run))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));
    }
}