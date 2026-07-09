package sn.lhacksrt.maeliaserver.scenario.domain.port.out;

import sn.lhacksrt.maeliaserver.scenario.domain.model.Scenario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScenarioRepository {

    Scenario save(Scenario scenario);

    Optional<Scenario> findById(UUID id);

    List<Scenario> findByProject(UUID projectId);
}
