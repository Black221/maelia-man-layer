package sn.lhacksrt.maeliaserver.simulation.infrastructure.messaging;

import java.util.UUID;

/**
 * Mise à jour de progression d'un run, publiée par le worker sur l'échange fanout
 * {@code maelia.run.updates} et relayée vers STOMP (/topic/runs/{runId}) par l'API.
 *
 * Permet la progression temps réel dans un déploiement api/worker séparé : le broker
 * STOMP (en mémoire) vit dans l'API, le worker ne peut donc pas y publier directement.
 */
public record RunUpdateMessage(
        UUID runId,
        String type,      // PROGRESS | LOG | ENDED | ERROR | EN_COURS | TERMINE | ECHEC
        int cycle,
        String message,
        String error
) {}
