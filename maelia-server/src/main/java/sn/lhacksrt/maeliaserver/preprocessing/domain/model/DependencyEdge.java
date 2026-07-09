package sn.lhacksrt.maeliaserver.preprocessing.domain.model;

/**
 * Arc du graphe de dépendances : {@code sourceId} doit exister pour produire/valider
 * {@code targetId}. {@code viaField} = label de la colonne qui porte la référence
 * (null pour une dépendance implicite).
 */
public record DependencyEdge(
        String sourceId,
        String targetId,
        String viaField,
        DependencyKind kind
) {}
