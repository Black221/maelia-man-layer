package sn.lhacksrt.maeliaserver.paramcatalog.domain.port.in;

import sn.lhacksrt.maeliaserver.paramcatalog.api.dto.ParameterSpecUpsertRequest;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.model.ParameterSpec;

/** Gestion manuelle (admin) du catalogue de paramètres de simulation. */
public interface ParameterAdminUseCase {

    ParameterSpec create(ParameterSpecUpsertRequest req);

    /** Met à jour un paramètre existant (le gamlName, identifiant, n'est pas modifiable). */
    ParameterSpec update(String gamlName, ParameterSpecUpsertRequest req);

    void delete(String gamlName);
}
