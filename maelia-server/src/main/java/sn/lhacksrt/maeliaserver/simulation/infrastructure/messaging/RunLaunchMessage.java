package sn.lhacksrt.maeliaserver.simulation.infrastructure.messaging;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Message AMQP transmis à RunWorker pour exécuter un run GAMA.
 *
 * until : expression GAML évaluée après chaque pas pour détecter la fin.
 *         Pour MAELIA : "simulationTerminee" (variable globale dans main.gaml).
 *         Pour un run de dev sans condition : "" (joue jusqu'à maxCycles).
 */
public record RunLaunchMessage(
        UUID runId,
        String modelPath,
        String experimentName,
        String until,
        UUID projectId,
        UUID scenarioId,
        List<Map<String, Object>> gamaParameters
) implements Serializable {}
