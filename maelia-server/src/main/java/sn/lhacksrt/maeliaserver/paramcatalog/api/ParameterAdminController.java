package sn.lhacksrt.maeliaserver.paramcatalog.api;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.lhacksrt.maeliaserver.paramcatalog.api.dto.ParameterSpecDto;
import sn.lhacksrt.maeliaserver.paramcatalog.api.dto.ParameterSpecUpsertRequest;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.port.in.ParameterAdminUseCase;

/**
 * Gestion manuelle du catalogue de paramètres de simulation (création/modification/suppression).
 * Pendant {@link ParameterController} (lecture). (Auth ADMIN à brancher en M9.)
 *
 * POST   /api/v1/admin/scenario-parameters            → crée un paramètre
 * PUT    /api/v1/admin/scenario-parameters/{gamlName} → met à jour
 * DELETE /api/v1/admin/scenario-parameters/{gamlName} → supprime
 */
@RestController
@RequestMapping("/api/v1/admin/scenario-parameters")
public class ParameterAdminController {

    private final ParameterAdminUseCase admin;

    public ParameterAdminController(ParameterAdminUseCase admin) {
        this.admin = admin;
    }

    @PostMapping
    public ResponseEntity<ParameterSpecDto> create(@Valid @RequestBody ParameterSpecUpsertRequest req) {
        return ResponseEntity.ok(ParameterSpecDto.from(admin.create(req)));
    }

    @PutMapping("/{gamlName}")
    public ParameterSpecDto update(@PathVariable String gamlName,
                                   @Valid @RequestBody ParameterSpecUpsertRequest req) {
        return ParameterSpecDto.from(admin.update(gamlName, req));
    }

    @DeleteMapping("/{gamlName}")
    public ResponseEntity<Void> delete(@PathVariable String gamlName) {
        admin.delete(gamlName);
        return ResponseEntity.noContent().build();
    }
}
