package sn.lhacksrt.maeliaserver.dataset.application.csv;

import org.springframework.stereotype.Component;
import sn.lhacksrt.maeliaserver.catalog.domain.model.DataSpec;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Importe un flux CSV en enregistrements normalisés, en respectant l'orientation du DataSpec.
 * Délègue au {@link CsvOrientationCodec} (gère COLUMN_HEADER, LINE_NUMBER et le transposé
 * FIELDS_AS_ROWS, le délimiteur du DataSpec, le BOM).
 */
@Component
public class CsvImportService {

    private final CsvOrientationCodec codec;

    public CsvImportService(CsvOrientationCodec codec) {
        this.codec = codec;
    }

    public List<Map<String, Object>> parse(InputStream csv, DataSpec spec) throws IOException {
        return codec.read(csv, spec);
    }
}
