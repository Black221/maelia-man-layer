package sn.lhacksrt.maeliaserver.paramcatalog.domain.port.out;

import sn.lhacksrt.maeliaserver.paramcatalog.domain.model.ParameterGroup;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.model.ParameterSpec;

import java.util.List;
import java.util.Optional;

/** Port de persistance du catalogue de paramètres de simulation. */
public interface ParameterCatalogRepository {

    void saveGroups(List<ParameterGroup> groups);

    void saveSpecs(List<ParameterSpec> specs);

    List<ParameterGroup> findAllGroups();

    List<ParameterSpec> findAllSpecs();

    long countSpecs();

    // --- Gestion unitaire (admin) ---

    ParameterSpec saveSpec(ParameterSpec spec);

    Optional<ParameterSpec> findSpec(String gamlName);

    boolean existsSpec(String gamlName);

    void deleteSpec(String gamlName);
}
