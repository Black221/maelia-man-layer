package sn.lhacksrt.maeliaserver.simulation.api;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import sn.lhacksrt.maeliaserver.simulation.api.dto.RunStatusResponse;
import sn.lhacksrt.maeliaserver.simulation.domain.model.SimulationRun;
import sn.lhacksrt.maeliaserver.simulation.domain.port.in.LaunchRunUseCase;
import sn.lhacksrt.maeliaserver.simulation.domain.port.out.SimulationRunRepository;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints de simulation liés à un projet (M4).
 * POST /api/v1/projects/{projectId}/runs?scenarioId=…  → lance un run
 * GET  /api/v1/projects/{projectId}/runs               → liste des runs du projet
 * GET  /api/v1/runs/{id}                               → statut d'un run
 */
@RestController
public class RunController {

    private final LaunchRunUseCase launchRun;
    private final SimulationRunRepository repository;

    public RunController(LaunchRunUseCase launchRun, SimulationRunRepository repository) {
        this.launchRun = launchRun;
        this.repository = repository;
    }

    @PostMapping("/api/v1/projects/{projectId}/runs")
    public ResponseEntity<RunStatusResponse> launch(
            @PathVariable UUID projectId,
            @RequestParam UUID scenarioId,
            UriComponentsBuilder ucb) {

        SimulationRun run = launchRun.launchForProject(projectId, scenarioId);
        return ResponseEntity
                .accepted()
                .location(ucb.path("/api/v1/runs/{id}").buildAndExpand(run.getId()).toUri())
                .body(RunStatusResponse.from(run));
    }

    @GetMapping("/api/v1/projects/{projectId}/runs")
    public List<RunStatusResponse> listForProject(@PathVariable UUID projectId) {
        return repository.findByProject(projectId).stream()
                .map(RunStatusResponse::from).toList();
    }

    @GetMapping("/api/v1/runs/{id}")
    public ResponseEntity<RunStatusResponse> get(@PathVariable UUID id) {
        return repository.findById(id)
                .map(run -> ResponseEntity.ok(RunStatusResponse.from(run)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
