package cl.nutrivida.ms.citas.service;


import cl.nutrivida.ms.citas.model.Nutricionista;
import cl.nutrivida.ms.citas.repository.NutricionistaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NutricionistaService {

    private final NutricionistaRepository nutricionistaRepository;

    public NutricionistaService(NutricionistaRepository nutricionistaRepository) {
        this.nutricionistaRepository = nutricionistaRepository;
    }

    public List<Nutricionista> listarNutricionistas() {
        return nutricionistaRepository.findAll();
    }

    public Optional<Nutricionista> buscarPorId(String id) {
        return nutricionistaRepository.findById(id);
    }

    public Nutricionista guardarNutricionista(Nutricionista nutricionista) {
        return nutricionistaRepository.save(nutricionista);
    }

    public void eliminarNutricionista(String id) {
        nutricionistaRepository.deleteById(id);
    }
}