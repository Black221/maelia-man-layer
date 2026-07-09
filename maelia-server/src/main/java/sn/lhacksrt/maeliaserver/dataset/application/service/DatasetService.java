package sn.lhacksrt.maeliaserver.dataset.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import sn.lhacksrt.maeliaserver.catalog.domain.model.DataSpec;
import sn.lhacksrt.maeliaserver.catalog.domain.port.in.CatalogUseCase;
import sn.lhacksrt.maeliaserver.dataset.application.csv.CsvImportService;
import sn.lhacksrt.maeliaserver.dataset.application.validation.ValidationEngine;
import sn.lhacksrt.maeliaserver.dataset.domain.model.Dataset;
import sn.lhacksrt.maeliaserver.dataset.domain.model.ValidationReport;
import sn.lhacksrt.maeliaserver.dataset.domain.port.in.DatasetQueryPort;
import sn.lhacksrt.maeliaserver.dataset.domain.port.out.DatasetRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class DatasetService implements DatasetQueryPort {

    private final DatasetRepository repository;
    private final CatalogUseCase catalog;
    private final CsvImportService csvImport;
    private final ValidationEngine validator;

    public DatasetService(DatasetRepository repository, CatalogUseCase catalog,
                          CsvImportService csvImport, ValidationEngine validator) {
        this.repository = repository;
        this.catalog = catalog;
        this.csvImport = csvImport;
        this.validator = validator;
    }

    /** Crée ou retrouve le dataset pour (projectId, dataSpecId). */
    public Dataset getOrCreate(UUID projectId, String dataSpecId) {
        return repository.findByProjectAndDataSpec(projectId, dataSpecId)
                .orElseGet(() -> {
                    Dataset ds = Dataset.create(projectId, dataSpecId);
                    return repository.save(ds);
                });
    }

    @Transactional(readOnly = true)
    public Optional<Dataset> findById(UUID id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Dataset> listByProject(UUID projectId) {
        return repository.findByProject(projectId);
    }

    /** Vue minimale pour les autres contextes (ex. complétude projet). */
    @Override
    @Transactional(readOnly = true)
    public List<DatasetView> findByProject(UUID projectId) {
        return repository.findByProject(projectId).stream()
                .map(d -> new DatasetView(d.getDataSpecId(), d.getStatus().name(), d.getRecordCount()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByDataSpec(String dataSpecId) {
        return repository.countByDataSpec(dataSpecId);
    }

    /** Remplace tous les enregistrements du dataset. */
    public Dataset upsertRecords(UUID datasetId, List<Map<String, Object>> records) {
        Dataset dataset = getOrThrow(datasetId);
        dataset.replaceRecords(records);
        return repository.save(dataset);
    }

    /** Import CSV : parse + remplace les enregistrements. */
    public Dataset importCsv(UUID projectId, String dataSpecId, MultipartFile file) throws Exception {
        DataSpec spec = catalog.getDataSpec(dataSpecId)
                .orElseThrow(() -> new IllegalArgumentException("DataSpec inconnu : " + dataSpecId));

        List<Map<String, Object>> records = csvImport.parse(file.getInputStream(), spec);
        Dataset dataset = getOrCreate(projectId, dataSpecId);
        dataset.replaceRecords(records);
        return repository.save(dataset);
    }

    /** Valide le dataset et persiste les issues. */
    public ValidationReport validate(UUID datasetId) {
        Dataset dataset = getOrThrow(datasetId);
        DataSpec spec = catalog.getDataSpec(dataset.getDataSpecId())
                .orElseThrow(() -> new IllegalArgumentException("DataSpec inconnu : " + dataset.getDataSpecId()));

        ValidationReport report = validator.validate(spec, dataset.getRecords());

        if (report.valid()) {
            dataset.markValid();
        } else {
            dataset.markInvalid();
        }
        repository.save(dataset);
        repository.saveValidationIssues(datasetId, report.issues());

        return report;
    }

    @Transactional(readOnly = true)
    public List<sn.lhacksrt.maeliaserver.dataset.domain.model.ValidationIssue> getIssues(UUID datasetId) {
        return repository.findIssues(datasetId);
    }

    private Dataset getOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + id));
    }
}
