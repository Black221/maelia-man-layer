package sn.lhacksrt.maeliaserver.paramcatalog.application.service;

import org.springframework.stereotype.Service;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.model.ParameterGroup;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.model.ParameterSpec;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.port.in.ParameterCatalogUseCase;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.port.out.ParameterCatalogRepository;

import java.util.Comparator;
import java.util.List;

@Service
public class ParameterCatalogService implements ParameterCatalogUseCase {

    private final ParameterCatalogRepository repository;

    public ParameterCatalogService(ParameterCatalogRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ParameterGroup> getGroups() {
        return repository.findAllGroups().stream()
                .sorted(Comparator.comparingInt(ParameterGroup::sortOrder)
                        .thenComparing(ParameterGroup::id))
                .toList();
    }

    @Override
    public List<ParameterSpec> getParameters() {
        return repository.findAllSpecs().stream()
                .sorted(Comparator.comparing(ParameterSpec::group)
                        .thenComparingInt(ParameterSpec::sortOrder)
                        .thenComparing(ParameterSpec::gamlName))
                .toList();
    }
}
