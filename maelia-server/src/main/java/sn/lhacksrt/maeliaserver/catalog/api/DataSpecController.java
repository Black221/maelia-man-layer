package sn.lhacksrt.maeliaserver.catalog.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.lhacksrt.maeliaserver.catalog.api.dto.DataSpecDto;
import sn.lhacksrt.maeliaserver.catalog.domain.port.in.CatalogUseCase;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dataspecs")
public class DataSpecController {

    private final CatalogUseCase catalog;

    public DataSpecController(CatalogUseCase catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public List<DataSpecDto> list() {
        return catalog.getAllDataSpecs().stream().map(DataSpecDto::from).toList();
    }

    /**
     * Retourne les DataSpec applicables à une ModelingConfiguration.
     * Corps : {"assolementMethod":"DONNEES_ENTREE","modules":["agricole","hydrographique"],...}
     */
    @PostMapping("/applicable")
    public List<DataSpecDto> applicable(@RequestBody Map<String, Object> modelingConfig) {
        return catalog.getApplicableDataSpecs(modelingConfig).stream().map(DataSpecDto::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataSpecDto> get(@PathVariable String id) {
        return catalog.getDataSpec(id)
                .map(DataSpecDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
