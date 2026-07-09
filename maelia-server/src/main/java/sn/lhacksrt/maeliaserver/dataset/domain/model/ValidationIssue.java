package sn.lhacksrt.maeliaserver.dataset.domain.model;

public record ValidationIssue(
        String field,
        Integer rowIndex,
        String severity,   // ERROR | WARNING
        String message
) {
    public static ValidationIssue error(String field, Integer row, String msg) {
        return new ValidationIssue(field, row, "ERROR", msg);
    }
    public static ValidationIssue warning(String field, Integer row, String msg) {
        return new ValidationIssue(field, row, "WARNING", msg);
    }
}
