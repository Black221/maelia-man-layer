package sn.lhacksrt.maeliaserver.paramcatalog.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.lhacksrt.maeliaserver.paramcatalog.api.dto.ParameterGroupDto;
import sn.lhacksrt.maeliaserver.paramcatalog.api.dto.ParameterSpecDto;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.port.in.ParameterCatalogUseCase;

import java.util.List;

/**
 * Catalogue des paramètres de simulation (M7).
 * GET /api/v1/scenario-parameters         → tous les paramètres (typés, groupés)
 * GET /api/v1/scenario-parameters/groups  → les groupes
 */
@RestController
@RequestMapping("/api/v1/scenario-parameters")
public class ParameterController {

    private final ParameterCatalogUseCase catalog;

    public ParameterController(ParameterCatalogUseCase catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public List<ParameterSpecDto> list() {
        return catalog.getParameters().stream().map(ParameterSpecDto::from).toList();
    }

    @GetMapping("/groups")
    public List<ParameterGroupDto> groups() {
        return catalog.getGroups().stream().map(ParameterGroupDto::from).toList();
    }
}
