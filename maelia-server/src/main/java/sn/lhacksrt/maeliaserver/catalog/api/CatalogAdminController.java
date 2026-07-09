package sn.lhacksrt.maeliaserver.catalog.api;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.lhacksrt.maeliaserver.catalog.api.dto.DataSpecDto;
import sn.lhacksrt.maeliaserver.catalog.api.dto.DataSpecUpsertRequest;
import sn.lhacksrt.maeliaserver.catalog.api.dto.FieldSpecUpsertRequest;
import sn.lhacksrt.maeliaserver.catalog.domain.port.in.CatalogAdminUseCase;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gestion manuelle du catalogue (création/modification/suppression de fichiers et de champs).
 * Séparé du contrôleur de lecture {@link DataSpecController}. (Auth ADMIN à brancher en M9.)
 */
@RestController
@RequestMapping("/api/v1/admin/dataspecs")
public class CatalogAdminController {

    private final CatalogAdminUseCase admin;

    public CatalogAdminController(CatalogAdminUseCase admin) {
        this.admin = admin;
    }

    // ---- DataSpec ----

    @PostMapping
    public ResponseEntity<DataSpecDto> create(@Valid @RequestBody DataSpecUpsertRequest req) {
        return ResponseEntity.ok(DataSpecDto.from(admin.createDataSpec(req)));
    }

    @PutMapping("/{id}")
    public DataSpecDto update(@PathVariable String id, @Valid @RequestBody DataSpecUpsertRequest req) {
        return DataSpecDto.from(admin.updateDataSpec(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id,
                                       @RequestParam(defaultValue = "false") boolean force) {
        admin.deleteDataSpec(id, force);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/duplicate")
    public DataSpecDto duplicate(@PathVariable String id, @RequestParam String newId) {
        return DataSpecDto.from(admin.duplicateDataSpec(id, newId));
    }

    @GetMapping("/{id}/usage")
    public CatalogAdminUseCase.Usage usage(@PathVariable String id) {
        return admin.usage(id);
    }

    // ---- Fields ----

    @PostMapping("/{id}/fields")
    public DataSpecDto addField(@PathVariable String id, @Valid @RequestBody FieldSpecUpsertRequest req) {
        return DataSpecDto.from(admin.addField(id, req));
    }

    @PutMapping("/{id}/fields/{fieldId}")
    public DataSpecDto updateField(@PathVariable String id, @PathVariable UUID fieldId,
                                   @Valid @RequestBody FieldSpecUpsertRequest req) {
        return DataSpecDto.from(admin.updateField(id, fieldId, req));
    }

    @DeleteMapping("/{id}/fields/{fieldId}")
    public DataSpecDto deleteField(@PathVariable String id, @PathVariable UUID fieldId) {
        return DataSpecDto.from(admin.deleteField(id, fieldId));
    }

    @PutMapping("/{id}/fields:reorder")
    public DataSpecDto reorder(@PathVariable String id, @RequestBody Map<String, List<UUID>> body) {
        return DataSpecDto.from(admin.reorderFields(id, body.getOrDefault("orderedFieldIds", List.of())));
    }
}
