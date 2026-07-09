package sn.lhacksrt.maeliaserver.catalog.domain.model;

/**
 * Orientation d'un fichier de données MAELIA.
 *
 * <ul>
 *   <li>{@link #FIELDS_AS_COLUMNS} : cas standard. Les champs sont des colonnes ; chaque
 *       enregistrement est une ligne. Le mode d'entête est porté par {@code csvFormat}
 *       (COLUMN_HEADER = 1re ligne nommée, LINE_NUMBER = positionnel sans entête).</li>
 *   <li>{@link #FIELDS_AS_ROWS} : transposé (ex. {@code reglesDeDecisions.csv}). Chaque champ
 *       est une ligne (la 1re colonne porte la clé/label du champ) ; chaque enregistrement est
 *       une colonne, à partir de {@code matrixValueStartIndex}.</li>
 * </ul>
 */
public enum Orientation {
    FIELDS_AS_COLUMNS,
    FIELDS_AS_ROWS;

    public static Orientation fromString(String value) {
        if (value == null || value.isBlank()) return FIELDS_AS_COLUMNS;
        try {
            return Orientation.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            // Tolérance : ancienne valeur "MATRIX" = transposé.
            return "MATRIX".equalsIgnoreCase(value.trim()) ? FIELDS_AS_ROWS : FIELDS_AS_COLUMNS;
        }
    }
}
