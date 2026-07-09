package sn.lhacksrt.maeliaserver.scenario.api;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import sn.lhacksrt.maeliaserver.scenario.api.dto.ScenarioRequest;
import sn.lhacksrt.maeliaserver.scenario.api.dto.ScenarioResponse;
import sn.lhacksrt.maeliaserver.scenario.application.service.ScenarioService;
import sn.lhacksrt.maeliaserver.scenario.domain.model.Scenario;

import java.util.List;
import java.util.UUID;

/**
 * CRUD scénarios pour un projet.
 * GET    /api/v1/projects/{projectId}/scenarios
 * POST   /api/v1/projects/{projectId}/scenarios
 * GET    /api/v1/projects/{projectId}/scenarios/{id}
 * PUT    /api/v1/projects/{projectId}/scenarios/{id}
 * DELETE /api/v1/projects/{projectId}/scenarios/{id}
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/scenarios")
public class ScenarioController {

    private final ScenarioService service;

    public ScenarioController(ScenarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<ScenarioResponse> list(@PathVariable UUID projectId) {
        return service.listByProject(projectId).stream()
                .map(ScenarioResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<ScenarioResponse> create(
            @PathVariable UUID projectId,
            @Valid @RequestBody ScenarioRequest req,
            UriComponentsBuilder ucb) {
        Scenario scenario = service.create(projectId, req.name(), req.description(),
                req.parameterValues());
        return ResponseEntity
                .created(ucb.path("/api/v1/projects/{pid}/scenarios/{id}")
                        .buildAndExpand(projectId, scenario.getId()).toUri())
                .body(ScenarioResponse.from(scenario));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScenarioResponse> get(@PathVariable UUID projectId, @PathVariable UUID id) {
        return service.findById(id)
                .filter(s -> s.getProjectId().equals(projectId))
                .map(s -> ResponseEntity.ok(ScenarioResponse.from(s)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScenarioResponse> update(
            @PathVariable UUID projectId,
            @PathVariable UUID id,
            @Valid @RequestBody ScenarioRequest req) {
        Scenario scenario = service.update(id, req.name(), req.description(),
                req.parameterValues());
        return ResponseEntity.ok(ScenarioResponse.from(scenario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(@PathVariable UUID projectId, @PathVariable UUID id) {
        service.archive(id);
        return ResponseEntity.noContent().build();
    }
}
