//Los 18 servicios del frontend

package cl.nutrivida.catalogo.config;

import cl.nutrivida.catalogo.model.Categoria;
import cl.nutrivida.catalogo.model.Servicio;
import cl.nutrivida.catalogo.repository.CategoriaRepository;
import cl.nutrivida.catalogo.repository.ServicioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CategoriaRepository categorias;
    private final ServicioRepository servicios;

    public DataSeeder(CategoriaRepository categorias, ServicioRepository servicios) {
        this.categorias = categorias;
        this.servicios = servicios;
    }

    @Override
    public void run(String... args) {
        if (categorias.count() > 0) return;

        Categoria con = categorias.save(new Categoria("Consulta", "Atención individual con nutricionista"));
        Categoria pla = categorias.save(new Categoria("Plan especializado", "Programas de varias sesiones"));
        Categoria eva = categorias.save(new Categoria("Evaluación", "Mediciones y análisis nutricionales"));
        Categoria tal = categorias.save(new Categoria("Taller grupal", "Actividades grupales con cupos limitados"));

        servicios.saveAll(List.of(
                s("CN001", con, "Primera consulta nutricional", "50 min", "Presencial", 35000, 0, null, null, "Evaluación inicial: anamnesis, antropometría completa y diseño del primer plan alimenticio."),
                s("CN002", con, "Control nutricional (seguimiento)", "30 min", "Presencial", 25000, 0, null, null, "Seguimiento mensual: medición de indicadores y ajuste del plan vigente."),
                s("CN003", con, "Control nutricional quincenal", "30 min", "Presencial", 22000, 0, null, null, "Seguimiento intensivo cada 15 días. Recomendado en los primeros 2 meses."),
                s("CN004", con, "Teleconsulta nutricional", "30 min", "Online (video)", 20000, 0, null, null, "Consulta de seguimiento vía videollamada. Requiere contar con consulta presencial previa."),
                s("CN005", con, "Consulta de urgencia / reagendada", "30 min", "Presencial", 28000, 0, null, null, "Para pacientes que requieren atención fuera de su control habitual."),
                s("PL001", pla, "Plan pérdida de peso (1 mes)", "—", "Presencial", 65000, 0, 2, null, "Incluye primera consulta + 1 control quincenal + plan alimenticio personalizado + seguimiento por WhatsApp."),
                s("PL002", pla, "Plan pérdida de peso (3 meses)", "—", "Presencial", 170000, 10, 6, null, "Incluye primera consulta + 5 controles + 3 planes mensuales + seguimiento continuo."),
                s("PL003", pla, "Plan nutrición deportiva (1 mes)", "—", "Presencial", 70000, 0, 10, null, "Para deportistas y personas con actividad física frecuente. Cálculo de requerimientos energéticos y proteicos."),
                s("PL004", pla, "Plan control diabetes / hipertensión", "—", "Presencial", 75000, 0, 10, null, "Plan adaptado para patologías metabólicas. Coordinación con médico tratante si aplica."),
                s("PL005", pla, "Plan alimentación vegetariana/vegana", "—", "Presencial", 68000, 0, 10, null, "Diseñado para garantizar aporte adecuado de proteínas, hierro, vitamina B12 y calcio sin productos animales."),
                s("PL006", pla, "Plan alimentación infantil (2-12 años)", "—", "Presencial", 65000, 0, 10, null, "Evaluación nutricional pediátrica y diseño de plan adaptado a la etapa de desarrollo del niño."),
                s("EV001", eva, "Antropometría completa", "20 min", "Presencial", 18000, 15, null, null, "Peso, talla, IMC, circunferencia de cintura, cadera, brazo y % de grasa corporal con bioimpedanciometría."),
                s("EV002", eva, "Bioimpedanciometría", "15 min", "Presencial", 12000, 0, null, null, "Medición de composición corporal: masa grasa, masa muscular, agua corporal y edad metabólica."),
                s("EV003", eva, "Encuesta de hábitos alimentarios", "20 min", "Presencial", 10000, 0, null, null, "Análisis del patrón alimentario actual. Identificación de déficit y excesos nutricionales."),
                s("EV004", eva, "Análisis de exámenes de laboratorio", "20 min", "Presencial", 15000, 0, null, null, "Interpretación de hemograma, perfil bioquímico y lipídico en contexto nutricional."),
                s("TG001", tal, "Taller de alimentación saludable", "90 min", "Presencial (grupo)", 15000, 20, null, 10, "Conceptos básicos de alimentación equilibrada y lectura de etiquetas."),
                s("TG002", tal, "Taller de cocina nutritiva", "120 min", "Presencial (grupo)", 20000, 0, null, 8, "Preparación de recetas saludables. Incluye degustación."),
                s("TG003", tal, "Taller nutrición para deportistas", "90 min", "Presencial (grupo)", 18000, 0, null, 12, "Hidratación, nutrición pre y post entrenamiento, suplementación básica.")
        ));
    }

    private Servicio s(String codigo, Categoria cat, String nombre, String duracion, String modalidad,
                       int precio, int descuento, Integer sesiones, Integer cupos, String descripcion) {
        Servicio s = new Servicio();
        s.setCodigo(codigo);
        s.setCategoria(cat);
        s.setNombre(nombre);
        s.setDuracion(duracion);
        s.setModalidad(modalidad);
        s.setProfesional("Nutricionista");
        s.setPrecio(precio);
        s.setDescuento(descuento);
        s.setSesiones(sesiones);
        s.setCupos(cupos);
        s.setDescripcion(descripcion);
        s.setActivo(true);
        return s;
    }
}