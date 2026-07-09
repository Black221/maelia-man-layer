package sn.lhacksrt.maeliaserver.dataset.api.dto;

import sn.lhacksrt.maeliaserver.dataset.domain.model.ValidationIssue;
import sn.lhacksrt.maeliaserver.dataset.domain.model.ValidationReport;

import java.util.List;

public record ValidationReportDto(
        boolean valid,
        int recordCount,
        int errorCount,
        List<IssueDto> issues
) {
    public record IssueDto(String field, Integer rowIndex, String severity, String message) {}

    public static ValidationReportDto from(ValidationReport r) {
        return new ValidationReportDto(
                r.valid(), r.recordCount(), r.errorCount(),
                r.issues().stream()
                        .map(i -> new IssueDto(i.field(), i.rowIndex(), i.severity(), i.message()))
                        .toList()
        );
    }
}
