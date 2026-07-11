package sn.lhacksrt.maeliaserver.simulation;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import sn.lhacksrt.maeliaserver.dataset.application.materializer.IncludesMaterializer;
import sn.lhacksrt.maeliaserver.project.domain.model.Project;
import sn.lhacksrt.maeliaserver.project.domain.port.out.ProjectRepository;
import sn.lhacksrt.maeliaserver.scenario.application.service.GamaParameterBuilder;
import sn.lhacksrt.maeliaserver.scenario.domain.model.Scenario;
import sn.lhacksrt.maeliaserver.scenario.domain.port.out.ScenarioRepository;
import sn.lhacksrt.maeliaserver.simulation.application.service.LaunchRunService;
import sn.lhacksrt.maeliaserver.simulation.domain.model.SimulationRun;
import sn.lhacksrt.maeliaserver.simulation.domain.port.out.SimulationRunRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Garde-fou : un seul run actif par projet à la fois (évite la course sur les includes). */
class LaunchRunConcurrencyTest {

    private final SimulationRunRepository runRepo = mock(SimulationRunRepository.class);
    private final ProjectRepository projectRepo = mock(ProjectRepository.class);
    private final ScenarioRepository scenarioRepo = mock(ScenarioRepository.class);
    private final IncludesMaterializer materializer = mock(IncludesMaterializer.class);
    private final GamaParameterBuilder paramBuilder = mock(GamaParameterBuilder.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);

    private final LaunchRunService service =
            new LaunchRunService(runRepo, projectRepo, scenarioRepo, materializer, paramBuilder, rabbit);

    @Test
    void refuse_un_second_run_si_un_run_est_actif() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID scenarioId = UUID.randomUUID();
        when(projectRepo.findById(projectId)).thenReturn(Optional.of(mock(Project.class)));
        when(scenarioRepo.findById(scenarioId)).thenReturn(Optional.of(mock(Scenario.class)));

        // Un run déjà EN_COURS pour ce projet.
        SimulationRun running = SimulationRun.createForProject("m", "e", projectId, scenarioId);
        running.markStarted("42"); // -> EN_COURS
        when(runRepo.findByProject(projectId)).thenReturn(List.of(running));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.launchForProject(projectId, scenarioId));
        assertTrue(ex.getMessage().toLowerCase().contains("déjà") || ex.getMessage().toLowerCase().contains("deja")
                        || ex.getMessage().toLowerCase().contains("en cours"),
                "message explicite : " + ex.getMessage());

        // Aucun effet de bord : pas de matérialisation, pas de publication, pas de sauvegarde de run.
        verify(materializer, never()).materialize(any());
        verify(rabbit, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        verify(runRepo, never()).save(any());
    }
}
