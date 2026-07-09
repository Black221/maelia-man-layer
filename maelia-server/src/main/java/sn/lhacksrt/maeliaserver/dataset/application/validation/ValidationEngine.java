package sn.lhacksrt.maeliaserver.dataset.application.validation;

import org.springframework.stereotype.Component;
import sn.lhacksrt.maeliaserver.catalog.domain.model.DataSpec;
import sn.lhacksrt.maeliaserver.catalog.domain.model.FieldSpec;
import sn.lhacksrt.maeliaserver.dataset.domain.model.ValidationIssue;
import sn.lhacksrt.maeliaserver.dataset.domain.model.ValidationReport;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validateur piloté par les FieldSpec du DataSpec.
 *
 * Niveau 1 — Structurel : présence des champs requis dans chaque enregistrement.
 * Niveau 2 — Typage : chaque valeur respecte son infoType et ses allowedValues.
 * (Niveau 3 — Référentiel : géré dans M4 quand les datasets cibles sont disponibles.)
 */
@Component
public class ValidationEngine {

    /**
     * Marqueurs « non applicable » de MAELIA : valeurs sentinelles valides, à ne PAS soumettre
     * au typage ni aux valeurs autorisées, et qui satisfont un champ requis (valeur explicite).
     */
    private static final Set<String> NA_SENTINELS = Set.of("NA", "[NA]", "N/A", "NAN", "NULL");

    private static boolean isNotApplicable(String v) {
        return v != null && NA_SENTINELS.contains(v.trim().toUpperCase());
    }

    public ValidationReport validate(DataSpec spec, List<Map<String, Object>> records) {
        List<ValidationIssue> issues = new ArrayList<>();

        if (records.isEmpty()) {
            issues.add(ValidationIssue.warning(null, null, "Le dataset est vide."));
            return ValidationReport.failed(0, issues);
        }

        List<FieldSpec> fields = spec.fields();

        for (int rowIdx = 0; rowIdx < records.size(); rowIdx++) {
            Map<String, Object> row = records.get(rowIdx);
            validateRow(fields, row, rowIdx, issues);
        }

        boolean valid = issues.stream().noneMatch(i -> "ERROR".equals(i.severity()));
        return valid
                ? ValidationReport.ok(records.size())
                : ValidationReport.failed(records.size(), issues);
    }

    private void validateRow(List<FieldSpec> fields, Map<String, Object> row,
                              int rowIdx, List<ValidationIssue> issues) {
        for (FieldSpec field : fields) {
            Object value = row.get(field.label());

            // Niveau 1 — présence
            if (field.required() && (value == null || value.toString().isBlank())) {
                issues.add(ValidationIssue.error(field.label(), rowIdx,
                        "Champ requis manquant : " + field.label()));
                continue;
            }

            if (value == null || value.toString().isBlank()) continue;

            String strVal = value.toString().trim();

            // Valeur sentinelle "non applicable" (NA, [NA], N/A…) : acceptée telle quelle,
            // on n'applique ni allowedValues ni typage. Le champ requis est satisfait.
            if (isNotApplicable(strVal)) continue;

            // Niveau 2 — allowedValues
            if (field.allowedValues() != null && !field.allowedValues().isEmpty()) {
                if (!field.allowedValues().contains(strVal)) {
                    issues.add(ValidationIssue.error(field.label(), rowIdx,
                            "Valeur non autorisée : '" + strVal + "'. Valeurs attendues : "
                                    + field.allowedValues()));
                }
            }

            // Niveau 2 — typage
            String type = field.infoType();
            if (type == null) continue;
            switch (type) {
                case "Integer" -> {
                    try { Integer.parseInt(strVal); }
                    catch (NumberFormatException e) {
                        issues.add(ValidationIssue.error(field.label(), rowIdx,
                                "Entier attendu, trouvé : '" + strVal + "'"));
                    }
                }
                case "Double" -> {
                    try { Double.parseDouble(strVal.replace(",", ".")); }
                    catch (NumberFormatException e) {
                        issues.add(ValidationIssue.error(field.label(), rowIdx,
                                "Nombre décimal attendu, trouvé : '" + strVal + "'"));
                    }
                }
                case "Boolean" -> {
                    if (!strVal.equalsIgnoreCase("true") && !strVal.equalsIgnoreCase("false")
                            && !strVal.equals("O") && !strVal.equals("N")
                            && !strVal.equals("1") && !strVal.equals("0")) {
                        issues.add(ValidationIssue.warning(field.label(), rowIdx,
                                "Booléen ambigu : '" + strVal + "'"));
                    }
                }
                case "Date" -> {
                    if (!isValidDate(strVal)) {
                        issues.add(ValidationIssue.error(field.label(), rowIdx,
                                "Date invalide : '" + strVal + "'"));
                    }
                }
            }
        }
    }

    private boolean isValidDate(String val) {
        // Formats courants MAELIA : j/m/aaaa, aaaa-mm-jj, jjmmaaaa
        try { LocalDate.parse(val); return true; } catch (DateTimeParseException ignored) {}
        try {
            String[] parts = val.split("/");
            if (parts.length == 3) { LocalDate.of(Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[1]), Integer.parseInt(parts[0])); return true; }
        } catch (Exception ignored) {}
        return false;
    }
}
