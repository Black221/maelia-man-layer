package sn.lhacksrt.maeliaserver.dataset.application.referential;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Lecteur minimal du format dBASE (.dbf) — juste ce qu'il faut pour proposer les valeurs
 * distinctes d'une colonne d'attributs d'un shapefile (référentiels de scénario : parcelles,
 * ZH, points météo, types de sol). On ne dépend pas de GeoTools : l'en-tête + les
 * descripteurs de champ + les enregistrements à largeur fixe suffisent.
 *
 * Format (dBASE III/IV) : en-tête 32 octets, puis descripteurs de champ 32 octets terminés
 * par 0x0D, puis les enregistrements (1 octet d'effacement + champs à largeur fixe).
 * Encodage attribut : ISO-8859-1 (comme les .dbf MAELIA, cf. lecture dbfread latin-1).
 */
public final class DbfReader {

    private record Field(String name, int offset, int length) {}

    private DbfReader() {}

    /** Noms des colonnes dans l'ordre du fichier. */
    public static List<String> fieldNames(byte[] dbf) {
        List<String> names = new ArrayList<>();
        for (Field f : parseFields(dbf)) names.add(f.name());
        return names;
    }

    /**
     * Valeurs distinctes (non vides, triées par 1re apparition) de la colonne {@code column}
     * (comparaison insensible à la casse sur le nom de champ). Liste vide si la colonne est absente.
     */
    public static List<String> distinctColumn(byte[] dbf, String column) {
        List<Field> fields = parseFields(dbf);
        Field target = null;
        for (Field f : fields) {
            if (f.name().equalsIgnoreCase(column)) { target = f; break; }
        }
        if (target == null) return List.of();

        int headerSize = u16(dbf, 8);
        int recordSize = u16(dbf, 10);
        int numRecords = u32(dbf, 4);
        if (recordSize <= 0) return List.of();

        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (int i = 0; i < numRecords; i++) {
            int recStart = headerSize + i * recordSize;
            if (recStart + recordSize > dbf.length) break;      // fichier tronqué
            if ((dbf[recStart] & 0xFF) == 0x2A) continue;        // enregistrement supprimé ('*')
            int from = recStart + target.offset();
            int to = Math.min(from + target.length(), dbf.length);
            String v = new String(dbf, from, to - from, StandardCharsets.ISO_8859_1).trim();
            if (!v.isEmpty()) values.add(v);
        }
        return new ArrayList<>(values);
    }

    private static List<Field> parseFields(byte[] b) {
        List<Field> fields = new ArrayList<>();
        if (b == null || b.length < 33) return fields;
        int headerSize = u16(b, 8);
        int pos = 32;
        int offset = 1; // 1er octet de chaque enregistrement = drapeau d'effacement
        while (pos + 32 <= b.length && pos < headerSize && (b[pos] & 0xFF) != 0x0D) {
            StringBuilder name = new StringBuilder();
            for (int i = 0; i < 11; i++) {
                int c = b[pos + i] & 0xFF;
                if (c == 0) break;
                name.append((char) c);
            }
            int length = b[pos + 16] & 0xFF;
            fields.add(new Field(name.toString().trim(), offset, length));
            offset += length;
            pos += 32;
        }
        return fields;
    }

    private static int u16(byte[] b, int i) {
        return (b[i] & 0xFF) | ((b[i + 1] & 0xFF) << 8);
    }

    private static int u32(byte[] b, int i) {
        return (b[i] & 0xFF) | ((b[i + 1] & 0xFF) << 8)
                | ((b[i + 2] & 0xFF) << 16) | ((b[i + 3] & 0xFF) << 24);
    }
}
