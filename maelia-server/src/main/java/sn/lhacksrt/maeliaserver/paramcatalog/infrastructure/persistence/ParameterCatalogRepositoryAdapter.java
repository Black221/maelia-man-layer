package sn.lhacksrt.maeliaserver.paramcatalog.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.model.ParamType;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.model.ParameterGroup;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.model.ParameterSpec;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.port.out.ParameterCatalogRepository;

import java.util.Arrays;
import java.util.List;

@Repository
public class ParameterCatalogRepositoryAdapter implements ParameterCatalogRepository {

    private final ParameterGroupJpaRepository groupRepo;
    private final ParameterSpecJpaRepository specRepo;

    public ParameterCatalogRepositoryAdapter(ParameterGroupJpaRepository groupRepo,
                                             ParameterSpecJpaRepository specRepo) {
        this.groupRepo = groupRepo;
        this.specRepo = specRepo;
    }

    @Override
    public void saveGroups(List<ParameterGroup> groups) {
        groupRepo.saveAll(groups.stream()
                .map(g -> new ParameterGroupJpaEntity(g.id(), g.label(), g.sortOrder(), g.parentId()))
                .toList());
    }

    @Override
    public void saveSpecs(List<ParameterSpec> specs) {
        specRepo.saveAll(specs.stream().map(this::toEntity).toList());
    }

    @Override
    public List<ParameterGroup> findAllGroups() {
        return groupRepo.findAll().stream()
                .map(e -> new ParameterGroup(e.getId(), e.getLabel(), e.getSortOrder(), e.getParentId()))
                .toList();
    }

    @Override
    public List<ParameterSpec> findAllSpecs() {
        return specRepo.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public long countSpecs() {
        return specRepo.count();
    }

    @Override
    public ParameterSpec saveSpec(ParameterSpec spec) {
        specRepo.save(toEntity(spec));
        return spec;
    }

    @Override
    public java.util.Optional<ParameterSpec> findSpec(String gamlName) {
        return specRepo.findById(gamlName).map(this::toDomain);
    }

    @Override
    public boolean existsSpec(String gamlName) {
        return specRepo.existsById(gamlName);
    }

    @Override
    public void deleteSpec(String gamlName) {
        specRepo.deleteById(gamlName);
    }

    private ParameterSpecJpaEntity toEntity(ParameterSpec s) {
        String allowed = (s.allowedValues() == null || s.allowedValues().isEmpty())
                ? null : String.join("|", s.allowedValues());
        return new ParameterSpecJpaEntity(s.gamlName(), s.label(), s.group(), s.type().name(),
                s.defaultValue(), s.unit(), allowed, s.visibleIf(), s.enabledIf(),
                s.optionsDataSpec(), s.advanced(), s.sortOrder());
    }

    private ParameterSpec toDomain(ParameterSpecJpaEntity e) {
        List<String> allowed = (e.getAllowedValues() == null || e.getAllowedValues().isBlank())
                ? null : Arrays.asList(e.getAllowedValues().split("\\|", -1));
        return new ParameterSpec(e.getGamlName(), e.getLabel(), e.getGroupId(),
                ParamType.fromString(e.getType()), e.getDefaultValue(), e.getUnit(),
                allowed, e.getVisibleIf(), e.getEnabledIf(), e.getOptionsDataSpec(),
                e.isAdvanced(), e.getSortOrder());
    }
}
