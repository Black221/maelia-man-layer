package sn.lhacksrt.maeliaserver.catalog.domain.port.in;

import sn.lhacksrt.maeliaserver.catalog.domain.model.DataSpec;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CatalogUseCase {

    List<DataSpec> getAllDataSpecs();

    /** Retourne les DataSpec applicables à une ModelingConfiguration donnée (évalue requiredIf). */
    List<DataSpec> getApplicableDataSpecs(Map<String, Object> modelingConfig);

    Optional<DataSpec> getDataSpec(String id);
}
