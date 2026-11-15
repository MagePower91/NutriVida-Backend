
package cl.nutrivida.ms.citas.config;

import cl.nutrivida.ms.citas.model.Nutricionista;
import cl.nutrivida.ms.citas.repository.NutricionistaRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CargaDatosIniciales {

    @Bean
    CommandLineRunner cargarNutricionistas(
            NutricionistaRepository repository) {

        return args -> {

            guardarSiNoExiste(repository, crear(
                "NUT001",
                "Carolina Fuentes",
                "Obesidad y síndrome metabólico",
                "09:00", "17:00",
                "carolina.fuentes@nutrivida.cl",
                "Acompaña procesos de baja de peso y control de enfermedades metabólicas con planes realistas.",
                List.of(1, 3, 5),
                List.of("pérdida de peso", "obesidad", "diabetes",
                        "hipertensión", "metabólico")
            ));

            guardarSiNoExiste(repository, crear(
                "NUT002",
                "Rodrigo Sepúlveda",
                "Nutrición deportiva y rendimiento",
                "09:00", "14:00",
                "rodrigo.sepulveda@nutrivida.cl",
                "Calcula requerimientos energéticos y proteicos para deportistas y personas activas.",
                List.of(2, 4, 6),
                List.of("deporte", "deportistas", "rendimiento",
                        "masa muscular", "suplementación")
            ));

            guardarSiNoExiste(repository, crear(
                "NUT003",
                "Daniela Morales",
                "Alimentación vegetariana, vegana y trastornos alimentarios",
                "08:00", "13:00",
                "daniela.morales@nutrivida.cl",
                "Diseña dietas sin productos animales con aporte adecuado de proteínas, hierro y vitamina B12.",
                List.of(1, 2, 3, 4, 5),
                List.of("vegetariana", "vegana", "vegetariano",
                        "vegano", "tca", "trastornos alimentarios")
            ));

            guardarSiNoExiste(repository, crear(
                "NUT004",
                "Felipe Araya",
                "Nutrición pediátrica y familiar",
                "14:00", "19:00",
                "felipe.araya@nutrivida.cl",
                "Evalúa a niños de 2 a 12 años y orienta a la familia completa en sus hábitos.",
                List.of(2, 3, 4, 5),
                List.of("niños", "infantil", "pediátrica",
                        "familia", "adolescentes")
            ));
        };
    }

    private Nutricionista crear(
            String id,
            String nombre,
            String especialidad,
            String desde,
            String hasta,
            String correo,
            String resena,
            List<Integer> dias,
            List<String> etiquetas) {

        Nutricionista n = new Nutricionista();

        n.setId(id);
        n.setNombre(nombre);
        n.setEspecialidad(especialidad);
        n.setDesde(desde);
        n.setHasta(hasta);
        n.setCorreo(correo);
        n.setResena(resena);
        n.setDias(dias);
        n.setEtiquetas(etiquetas);

        return n;
    }

    private void guardarSiNoExiste(
            NutricionistaRepository repository,
            Nutricionista nutricionista) {

        if (!repository.existsById(nutricionista.getId())) {
            repository.save(nutricionista);
        }
    }
}
