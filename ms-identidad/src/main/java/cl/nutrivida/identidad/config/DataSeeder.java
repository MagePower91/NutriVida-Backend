package cl.nutrivida.identidad.config;

import cl.nutrivida.identidad.model.Rol;
import cl.nutrivida.identidad.model.Usuario;
import cl.nutrivida.identidad.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;

    public DataSeeder(UsuarioRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (repo.count() > 0) return;

        String r = "La Araucanía", c = "Temuco", d = "Av. Alemania 0671";
        Usuario paciente = u("195556660", "Javiera", "Soto Paredes", "paciente@gmail.com", "paciente1", Rol.PACIENTE, r, c, "Los Aromos 1234");
        paciente.setFechaNacimiento(LocalDate.of(1994, 5, 18));

        repo.saveAll(List.of(
                u("761920839", "Administrador", "NutriVida", "admin@gmail.com", "admin123", Rol.ADMINISTRADOR, r, c, d),
                u("123456785", "Recepción", "NutriVida", "recepcion@gmail.com", "recepcion", Rol.RECEPCION, r, c, d),
                u("151112226", "Carolina", "Fuentes M.", "carolina.fuentes@nutrivida.cl", "nutri123", Rol.NUTRICIONISTA, r, c, d),
                u("16222333K", "Rodrigo", "Sepúlveda A.", "rodrigo.sepulveda@nutrivida.cl", "nutri123", Rol.NUTRICIONISTA, r, c, d),
                u("173334443", "Daniela", "Morales C.", "daniela.morales@nutrivida.cl", "nutri123", Rol.NUTRICIONISTA, r, c, d),
                u("184445557", "Felipe", "Araya R.", "felipe.araya@nutrivida.cl", "nutri123", Rol.NUTRICIONISTA, r, c, d),
                paciente
        ));
    }

    private Usuario u(String run, String nombre, String apellidos, String correo, String clave,
                      Rol rol, String region, String comuna, String direccion) {
        return new Usuario(run, nombre, apellidos, correo, encoder.encode(clave), rol, region, comuna, direccion);
    }
}