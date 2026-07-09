package sn.lhacksrt.maeliaserver.simulation.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sn.lhacksrt.maeliaserver.dataset.application.materializer.IncludesMaterializer;
import sn.lhacksrt.maeliaserver.project.domain.port.out.ProjectRepository;
import sn.lhacksrt.maeliaserver.scenario.application.service.GamaParameterBuilder;
import sn.lhacksrt.maeliaserver.scenario.domain.model.Scenario;
import sn.lhacksrt.maeliaserver.scenario.domain.port.out.ScenarioRepository;
import sn.lhacksrt.maeliaserver.simulation.domain.model.SimulationRun;
import sn.lhacksrt.maeliaserver.simulation.domain.port.in.LaunchRunUseCase;
import sn.lhacksrt.maeliaserver.simulation.domain.port.out.SimulationRunRepository;
import sn.lhacksrt.maeliaserver.simulation.infrastructure.messaging.RunLaunchMessage;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LaunchRunService implements LaunchRunUseCase {

    private static final Logger log = LoggerFactory.getLogger(LaunchRunService.class);

    private final SimulationRunRepository repository;
    private final ProjectRepository projectRepository;
    private final ScenarioRepository scenarioRepository;
    private final IncludesMaterializer materializer;
    private final GamaParameterBuilder parameterBuilder;
    private final RabbitTemplate rabbitTemplate;

    @Value("${maelia.messaging.run-exchange}")
    private String runExchange;

    @Value("${maelia.messaging.run-routing-key}")
    private String runRoutingKey;

    @Value("${maelia.simulation.default-model-path:/working_dir/model/maelia.gaml}")
    private String defaultModelPath;

    @Value("${maelia.simulation.default-experiment:TestExperiment}")
    private String defaultExperiment;

    public LaunchRunService(SimulationRunRepository repository,
                            ProjectRepository projectRepository,
                            ScenarioRepository scenarioRepository,
                            IncludesMaterializer materializer,
                            GamaParameterBuilder parameterBuilder,
                            RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.projectRepository = projectRepository;
        this.scenarioRepository = scenarioRepository;
        this.materializer = materializer;
        this.parameterBuilder = parameterBuilder;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public SimulationRun launch(String modelPath, String experimentName, String until,
                                Map<String, Object> parameters) {
        SimulationRun run = SimulationRun.create(modelPath, experimentName);
        repository.save(run);

        RunLaunchMessage msg = new RunLaunchMessage(
                run.getId(), modelPath, experimentName, until != null ? until : "",
                null, null, GamaParameterBuilder.toGamaParameters(parameters));
        rabbitTemplate.convertAndSend(runExchange, runRoutingKey, msg);

        log.info("Dev run {} queued: model={} experiment={} until={} params={}",
                run.getId(), modelPath, experimentName, until,
                parameters != null ? parameters.size() : 0);
        return run;
    }

    @Override
    public SimulationRun launchForProject(UUID projectId, UUID scenarioId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("Scenario not found: " + scenarioId));

        // Bloquant : un run lancé sans includes matérialisés échouerait silencieusement côté GAMA
        // (cheminModeleVersDonnees pointant vers un répertoire absent).
        try {
            materializer.materialize(projectId);
        } catch (Exception e) {
            log.error("Materialization failed for project={}: {}", projectId, e.getMessage(), e);
            throw new IllegalStateException(
                    "Échec de la matérialisation des includes du projet " + projectId
                            + " : " + e.getMessage(), e);
        }

        SimulationRun run = SimulationRun.createForProject(
                defaultModelPath, defaultExperiment, projectId, scenarioId);
        repository.save(run);

        List<Map<String, Object>> params = parameterBuilder.build(scenario, projectId, run.getId());

        RunLaunchMessage msg = new RunLaunchMessage(
                run.getId(), defaultModelPath, defaultExperiment,
                "simulationTerminee", projectId, scenarioId, params);
        rabbitTemplate.convertAndSend(runExchange, runRoutingKey, msg);

        log.info("Project run {} queued: project={} scenario={}", run.getId(), projectId, scenarioId);
        return run;
    }
}
