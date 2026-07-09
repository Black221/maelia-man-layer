package sn.lhacksrt.maeliaserver.dataset.domain.model;

import java.util.List;

public record ValidationReport(
        boolean valid,
        int recordCount,
        List<ValidationIssue> issues
) {
    public static ValidationReport ok(int count) {
        return new ValidationReport(true, count, List.of());
    }

    public static ValidationReport failed(int count, List<ValidationIssue> issues) {
        return new ValidationReport(false, count, issues);
    }

    public int errorCount() {
        return (int) issues.stream().filter(i -> "ERROR".equals(i.severity())).count();
    }
}
