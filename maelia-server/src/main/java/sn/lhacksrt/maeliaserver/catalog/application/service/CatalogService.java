package sn.lhacksrt.maeliaserver.catalog.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.lhacksrt.maeliaserver.catalog.domain.model.DataSpec;
import sn.lhacksrt.maeliaserver.catalog.domain.model.FieldSpec;
import sn.lhacksrt.maeliaserver.catalog.domain.port.in.CatalogUseCase;
import sn.lhacksrt.maeliaserver.catalog.infrastructure.persistence.DataSpecJpaEntity;
import sn.lhacksrt.maeliaserver.catalog.infrastructure.persistence.DataSpecJpaRepository;
import sn.lhacksrt.maeliaserver.catalog.infrastructure.persistence.FieldSpecJpaEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Transactional(readOnly = true)
public class CatalogService implements CatalogUseCase {

    private final DataSpecJpaRepository repository;
    private final RequiredIfEvaluator evaluator;

    public CatalogService(DataSpecJpaRepository repository, RequiredIfEvaluator evaluator) {
        this.repository = repository;
        this.evaluator = evaluator;
    }

    @Override
    public List<DataSpec> getAllDataSpecs() {
        return repository.findAllOrdered().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<DataSpec> getApplicableDataSpecs(Map<String, Object> modelingConfig) {
        return repository.findAllOrdered().stream()
                .filter(ds -> ds.isRequired() || evaluator.isRequired(ds.getRequiredIf(), modelingConfig))
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<DataSpec> getDataSpec(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    private DataSpec toDomain(DataSpecJpaEntity e) {
        List<FieldSpec> fields = e.getFields().stream()
                .map(this::fieldToDomain)
                .collect(Collectors.toList());

        return new DataSpec(
                e.getId(), e.getModule(), e.getFolder(), e.getFileName(),
                e.getFileType(), e.getCsvFormat(),
                sn.lhacksrt.maeliaserver.catalog.domain.model.Orientation.fromString(e.getOrientation()),
                e.getMatrixValueStartIndex(), e.getDelimiter(),
                e.getGeneration(),
                e.isRequired(), e.getRequiredIf(), e.getTemporalResolution(),
                e.isMultiInstance(), e.getInstancePattern(), e.getFileNamePattern(), e.getSaisieMode(),
                e.getDescription(), e.getFieldsStatus(), e.getOrigin(),
                splitDependsOn(e.getDependsOn()), fields
        );
    }

    private static List<String> splitDependsOn(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split("\\|")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private FieldSpec fieldToDomain(FieldSpecJpaEntity f) {
        List<String> allowed = f.getAllowedValues() != null
                ? Arrays.asList(f.getAllowedValues().split("\\|"))
                : List.of();

        return new FieldSpec(
                f.getId(), f.getLabel(), f.getPosition(), f.getInfoType(),
                f.getUnit(), f.isRequired(), f.getRequiredIf(),
                f.getReferencesDataSpec(), f.getDescription(),
                f.getListSeparator(), allowed, f.getSortOrder()
        );
    }
}
