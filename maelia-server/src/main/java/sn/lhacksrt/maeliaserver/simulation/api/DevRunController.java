package sn.lhacksrt.maeliaserver.simulation.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import sn.lhacksrt.maeliaserver.simulation.api.dto.LaunchRunRequest;
import sn.lhacksrt.maeliaserver.simulation.api.dto.RunStatusResponse;
import sn.lhacksrt.maeliaserver.simulation.domain.model.SimulationRun;
import sn.lhacksrt.maeliaserver.simulation.domain.port.in.LaunchRunUseCase;
import sn.lhacksrt.maeliaserver.simulation.domain.port.out.SimulationRunRepository;

import java.util.UUID;

/**
 * Endpoint de développement (M1) pour lancer une simulation sans passer par
 * la gestion de projet. Permet de valider le process front → back → GAMA.
 *
 * POST /api/v1/dev/runs          → lance un run générique (modelPath/experiment/until au choix)
 * POST /api/v1/dev/runs/test     → lance le MODÈLE DE TEST autonome (indépendant de MAELIA)
 * GET  /api/v1/dev/runs/{id}     → statut courant du run
 */
@RestController
@RequestMapping("/api/v1/dev/runs")
public class DevRunController {

    private final LaunchRunUseCase launchRun;
    private final SimulationRunRepository repository;

    @Value("${maelia.simulation.default-model-path:#{null}}")
    private String defaultModelPath;

    @Value("${maelia.simulation.default-experiment:TestExperiment}")
    private String defaultExperiment;

    @Value("${maelia.simulation.default-until:}")
    private String defaultUntil;

    // --- Modèle de test autonome (indépendant de MAELIA) ---
    @Value("${maelia.simulation.test-model-path:/workspace/test/models/simple_test.gaml}")
    private String testModelPath;

    @Value("${maelia.simulation.test-experiment:test_simulation}")
    private String testExperiment;

    @Value("${maelia.simulation.test-until:sim_termine}")
    private String testUntil;

    // --- Launcher de test du VRAI modèle MAELIA (includes de base SASSEME) ---
    @Value("${maelia.simulation.maelia-test-model-path:/workspace/maelia/models/main/launcherTest.gaml}")
    private String maeliaTestModelPath;

    @Value("${maelia.simulation.maelia-test-experiment:test_maelia}")
    private String maeliaTestExperiment;

    @Value("${maelia.simulation.maelia-test-until:simulationTerminee}")
    private String maeliaTestUntil;

    public DevRunController(LaunchRunUseCase launchRun, SimulationRunRepository repository) {
        this.launchRun = launchRun;
        this.repository = repository;
    }

    /** Lance un run générique. Le corps est optionnel : les valeurs par défaut viennent de application.yml. */
    @PostMapping
    public ResponseEntity<RunStatusResponse> launch(
            @RequestBody(required = false) LaunchRunRequest req,
            UriComponentsBuilder ucb) {

        String modelPath = (req != null && req.modelPath() != null) ? req.modelPath()
                : (defaultModelPath != null ? defaultModelPath : "/working_dir/model/maelia.gaml");
        String experiment = (req != null && req.experimentName() != null) ? req.experimentName() : defaultExperiment;
        String until = (req != null && req.until() != null) ? req.until() : defaultUntil;

        SimulationRun run = launchRun.launch(modelPath, experiment, until,
                req != null ? req.parameters() : null);
        return accepted(run, ucb);
    }

    /**
     * Lance le modèle de test autonome (gama-workspace/test/models/simple_test.gaml).
     * Aucune donnée d'entrée requise : sert à vérifier que la communication passe bien.
     * Corps optionnel : { "parameters": { "nb_people": 500, "nb_steps": 200, ... } }
     * (clé = nom de variable GAML de l'expérience test_simulation).
     */
    @PostMapping("/test")
    public ResponseEntity<RunStatusResponse> launchTest(
            @RequestBody(required = false) LaunchRunRequest req,
            UriComponentsBuilder ucb) {
        SimulationRun run = launchRun.launch(testModelPath, testExperiment, testUntil,
                req != null ? req.parameters() : null);
        return accepted(run, ucb);
    }

    /**
     * Lance le VRAI modèle MAELIA via launcherTest.gaml (includes de base SASSEME posés dans
     * gama-workspace/maelia/includes/). Valide la chaîne de bout en bout avec MAELIA réel,
     * sans gestion de projet ni matérialisation d'includes. Corps optionnel : { "parameters": {...} }.
     */
    @PostMapping("/maelia-test")
    public ResponseEntity<RunStatusResponse> launchMaeliaTest(
            @RequestBody(required = false) LaunchRunRequest req,
            UriComponentsBuilder ucb) {
        SimulationRun run = launchRun.launch(maeliaTestModelPath, maeliaTestExperiment, maeliaTestUntil,
                req != null ? req.parameters() : null);
        return accepted(run, ucb);
    }

    /** Statut courant d'un run (polling ou appel initial). */
    @GetMapping("/{id}")
    public ResponseEntity<RunStatusResponse> status(@PathVariable UUID id) {
        return repository.findById(id)
                .map(run -> ResponseEntity.ok(RunStatusResponse.from(run)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ResponseEntity<RunStatusResponse> accepted(SimulationRun run, UriComponentsBuilder ucb) {
        return ResponseEntity
                .accepted()
                .location(ucb.path("/api/v1/dev/runs/{id}").buildAndExpand(run.getId()).toUri())
                .body(RunStatusResponse.from(run));
    }
}
