package sn.lhacksrt.maeliaserver.paramcatalog.domain.port.in;

import sn.lhacksrt.maeliaserver.paramcatalog.domain.model.ParameterGroup;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.model.ParameterSpec;

import java.util.List;

/** Lecture du catalogue de paramètres de simulation. */
public interface ParameterCatalogUseCase {

    List<ParameterGroup> getGroups();

    List<ParameterSpec> getParameters();
}
