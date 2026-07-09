package sn.lhacksrt.maeliaserver.preprocessing.domain.model;

import java.util.List;

/**
 * Graphe complet des dépendances entre fichiers du catalogue.
 * {@code unknownReferences} = références vers des ids absents du catalogue
 * (ignorées dans le graphe, remontées pour diagnostic).
 */
public record DependencyGraph(
        List<DependencyNode> nodes,
        List<DependencyEdge> edges,
        boolean hasCycle,
        List<String> cycleIds,
        List<String> unknownReferences
) {}
