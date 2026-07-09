package sn.lhacksrt.maeliaserver.dataset.api.dto;

import sn.lhacksrt.maeliaserver.dataset.domain.model.Dataset;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DatasetResponse(
        UUID id,
        UUID projectId,
        String dataSpecId,
        String instanceKey,
        String status,
        int recordCount,
        List<Map<String, Object>> records,
        Instant createdAt,
        Instant updatedAt
) {
    public static DatasetResponse from(Dataset d) {
        return new DatasetResponse(
                d.getId(), d.getProjectId(), d.getDataSpecId(), d.getInstanceKey(),
                d.getStatus().name(), d.getRecordCount(),
                d.getRecords(), d.getCreatedAt(), d.getUpdatedAt()
        );
    }

    public static DatasetResponse summary(Dataset d) {
        return new DatasetResponse(
                d.getId(), d.getProjectId(), d.getDataSpecId(), d.getInstanceKey(),
                d.getStatus().name(), d.getRecordCount(),
                List.of(), d.getCreatedAt(), d.getUpdatedAt()
        );
    }
}
