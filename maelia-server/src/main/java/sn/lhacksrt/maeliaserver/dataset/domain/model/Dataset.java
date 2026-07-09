package sn.lhacksrt.maeliaserver.dataset.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Dataset {

    private final UUID id;
    private final UUID projectId;
    private final String dataSpecId;
    /**
     * Nom de fichier de l'instance pour les types multi-instance (ex. "2018.csv" pour la
     * série climatique AAAA.csv). Null = dataset unique du type (cas standard).
     */
    private final String instanceKey;
    private DatasetStatus status;
    private List<Map<String, Object>> records;
    private final Instant createdAt;
    private Instant updatedAt;

    private Dataset(UUID id, UUID projectId, String dataSpecId, String instanceKey,
                    DatasetStatus status, List<Map<String, Object>> records,
                    Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.projectId = projectId;
        this.dataSpecId = dataSpecId;
        this.instanceKey = instanceKey;
        this.status = status;
        this.records = new ArrayList<>(records);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Dataset create(UUID projectId, String dataSpecId) {
        return create(projectId, dataSpecId, null);
    }

    public static Dataset create(UUID projectId, String dataSpecId, String instanceKey) {
        Instant now = Instant.now();
        return new Dataset(UUID.randomUUID(), projectId, dataSpecId, instanceKey,
                DatasetStatus.VIDE, List.of(), now, now);
    }

    public static Dataset reconstitute(UUID id, UUID projectId, String dataSpecId, String instanceKey,
                                       DatasetStatus status, List<Map<String, Object>> records,
                                       Instant createdAt, Instant updatedAt) {
        return new Dataset(id, projectId, dataSpecId, instanceKey, status, records, createdAt, updatedAt);
    }

    public void replaceRecords(List<Map<String, Object>> newRecords) {
        this.records = new ArrayList<>(newRecords);
        this.status = newRecords.isEmpty() ? DatasetStatus.VIDE : DatasetStatus.EN_COURS;
        this.updatedAt = Instant.now();
    }

    public void markValid() {
        this.status = DatasetStatus.VALIDE;
        this.updatedAt = Instant.now();
    }

    public void markInvalid() {
        this.status = DatasetStatus.INVALIDE;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public String getDataSpecId() { return dataSpecId; }
    public String getInstanceKey() { return instanceKey; }
    public DatasetStatus getStatus() { return status; }
    public List<Map<String, Object>> getRecords() { return records; }
    public int getRecordCount() { return records.size(); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
