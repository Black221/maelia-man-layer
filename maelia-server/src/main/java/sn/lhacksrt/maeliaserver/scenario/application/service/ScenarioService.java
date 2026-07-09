package sn.lhacksrt.maeliaserver.scenario.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.model.ParamType;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.model.ParameterSpec;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.port.in.ParameterCatalogUseCase;
import sn.lhacksrt.maeliaserver.scenario.domain.model.Scenario;
import sn.lhacksrt.maeliaserver.scenario.domain.port.out.ScenarioRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class ScenarioService {

    private final ScenarioRepository repository;
    private final ParameterCatalogUseCase catalog;

    public ScenarioService(ScenarioRepository repository, ParameterCatalogUseCase catalog) {
        this.repository = repository;
        this.catalog = catalog;
    }

    public Scenario create(UUID projectId, String name, String description,
                           Map<String, Object> parameterValues) {
        Map<String, Object> values = validate(parameterValues);
        return repository.save(Scenario.create(projectId, name, description, values));
    }

    @Transactional(readOnly = true)
    public List<Scenario> listByProject(UUID projectId) {
        return repository.findByProject(projectId);
    }

    @Transactional(readOnly = true)
    public Optional<Scenario> findById(UUID id) {
        return repository.findById(id);
    }

    public Scenario update(UUID id, String name, String description,
                           Map<String, Object> parameterValues) {
        Scenario scenario = getOrThrow(id);
        scenario.update(name, description, validate(parameterValues));
        return repository.save(scenario);
    }

    public void archive(UUID id) {
        Scenario scenario = getOrThrow(id);
        scenario.archive();
        repository.save(scenario);
    }

    /**
     * Valide les valeurs contre le catalogue : rejette les clés inconnues et les valeurs
     * hors {@code allowedValues} (ENUM) ; ignore les valeurs nulles (= pas d'override).
     */
    private Map<String, Object> validate(Map<String, Object> values) {
        Map<String, Object> clean = new HashMap<>();
        if (values == null || values.isEmpty()) return clean;

        Map<String, ParameterSpec> specs = catalog.getParameters().stream()
                .collect(Collectors.toMap(ParameterSpec::gamlName, Function.identity(), (a, b) -> a));

        for (Map.Entry<String, Object> e : values.entrySet()) {
            String key = e.getKey();
            Object val = e.getValue();
            if (val == null || (val instanceof String s && s.isBlank())) continue;

            ParameterSpec spec = specs.get(key);
            if (spec == null) {
                throw new IllegalArgumentException("Paramètre de simulation inconnu : " + key);
            }
            if (spec.type() == ParamType.ENUM && spec.allowedValues() != null
                    && !spec.allowedValues().contains(val.toString())) {
                throw new IllegalArgumentException(
                        "Valeur invalide pour " + key + " : '" + val + "' (attendu : " + spec.allowedValues() + ")");
            }
            clean.put(key, val);
        }
        return clean;
    }

    private Scenario getOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scenario not found: " + id));
    }
}
