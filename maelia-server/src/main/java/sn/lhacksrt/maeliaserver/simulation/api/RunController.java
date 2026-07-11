package sn.lhacksrt.maeliaserver.simulation.api;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import sn.lhacksrt.maeliaserver.scenario.domain.model.Scenario;
import sn.lhacksrt.maeliaserver.scenario.domain.port.out.ScenarioRepository;
import sn.lhacksrt.maeliaserver.simulation.api.dto.RunStatusResponse;
import sn.lhacksrt.maeliaserver.simulation.domain.model.SimulationRun;
import sn.lhacksrt.maeliaserver.simulation.domain.port.in.LaunchRunUseCase;
import sn.lhacksrt.maeliaserver.simulation.domain.port.out.SimulationRunRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final ScenarioRepository scenarioRepository;

    public RunController(LaunchRunUseCase launchRun, SimulationRunRepository repository,
                         ScenarioRepository scenarioRepository) {
        this.launchRun = launchRun;
        this.repository = repository;
        this.scenarioRepository = scenarioRepository;
    }

    /** Nom du scénario d'un run (null si run de dev ou scénario introuvable). */
    private String scenarioNameOf(SimulationRun run) {
        if (run.getScenarioId() == null) return null;
        return scenarioRepository.findById(run.getScenarioId())
                .map(Scenario::getName).orElse(null);
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
                .body(RunStatusResponse.from(run, scenarioNameOf(run)));
    }

    @GetMapping("/api/v1/projects/{projectId}/runs")
    public List<RunStatusResponse> listForProject(@PathVariable UUID projectId) {
        // Un seul chargement des scénarios du projet (évite N requêtes) pour porter le nom.
        Map<UUID, String> names = scenarioRepository.findByProject(projectId).stream()
                .collect(Collectors.toMap(Scenario::getId, Scenario::getName, (a, b) -> a));
        return repository.findByProject(projectId).stream()
                .map(run -> RunStatusResponse.from(run,
                        run.getScenarioId() == null ? null : names.get(run.getScenarioId())))
                .toList();
    }

    @GetMapping("/api/v1/runs/{id}")
    public ResponseEntity<RunStatusResponse> get(@PathVariable UUID id) {
        return repository.findById(id)
                .map(run -> ResponseEntity.ok(RunStatusResponse.from(run, scenarioNameOf(run))))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
