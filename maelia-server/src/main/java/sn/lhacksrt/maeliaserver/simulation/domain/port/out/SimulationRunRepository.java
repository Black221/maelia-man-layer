package sn.lhacksrt.maeliaserver.simulation.domain.port.out;

import sn.lhacksrt.maeliaserver.simulation.domain.model.SimulationRun;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SimulationRunRepository {

    SimulationRun save(SimulationRun run);

    Optional<SimulationRun> findById(UUID id);

    List<SimulationRun> findByProject(UUID projectId);
}
