package sn.lhacksrt.maeliaserver.catalog.domain.model;

import java.util.List;

public record DataSpec(
        String id,
        String module,
        String folder,
        String fileName,
        String fileType,
        String csvFormat,
        Orientation orientation,
        Integer matrixValueStartIndex,
        String delimiter,
        String generation,
        boolean required,
        String requiredIf,
        String temporalResolution,
        boolean multiInstance,
        String instancePattern,
        String fileNamePattern,
        String saisieMode,
        String description,
        String fieldsStatus,
        String origin,
        List<String> dependsOn,
        List<FieldSpec> fields
) {
    /** Délimiteur effectif (MAELIA = ';' par défaut). */
    public char effectiveDelimiter() {
        return (delimiter != null && !delimiter.isEmpty()) ? delimiter.charAt(0) : ';';
    }

    /** Index 0-based de la 1re colonne de valeurs en mode transposé (défaut = 1). */
    public int effectiveMatrixValueStartIndex() {
        return (matrixValueStartIndex != null && matrixValueStartIndex >= 1) ? matrixValueStartIndex : 1;
    }

    public boolean isTransposed() {
        return orientation == Orientation.FIELDS_AS_ROWS;
    }

    /**
     * Vrai si {@code candidateFileName} est une instance de ce type multi-instance,
     * d'après {@code fileNamePattern} (match complet, insensible à la casse).
     * Ex. serieClimatique (AAAA.csv, pattern \d{4}\.csv) reconnaît 2018.csv.
     */
    public boolean matchesInstanceFileName(String candidateFileName) {
        if (!multiInstance || fileNamePattern == null || fileNamePattern.isBlank()
                || candidateFileName == null || candidateFileName.isBlank()) {
            return false;
        }
        try {
            return java.util.regex.Pattern
                    .compile(fileNamePattern, java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(candidateFileName.trim())
                    .matches();
        } catch (java.util.regex.PatternSyntaxException e) {
            return false;
        }
    }

    /**
     * Tous les DataSpec dont ce fichier dépend : références par identifiant portées
     * par les champs (FieldSpec.referencesDataSpec) + dépendances implicites (dependsOn).
     * Auto-références exclues, sans doublons, ordre stable.
     */
    public java.util.LinkedHashSet<String> allDependencyIds() {
        java.util.LinkedHashSet<String> deps = new java.util.LinkedHashSet<>();
        if (fields != null) {
            for (FieldSpec f : fields) {
                if (f.referencesDataSpec() != null && !f.referencesDataSpec().isBlank()) {
                    deps.add(f.referencesDataSpec());
                }
            }
        }
        if (dependsOn != null) deps.addAll(dependsOn);
        deps.remove(id);
        return deps;
    }
}
