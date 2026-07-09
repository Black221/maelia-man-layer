package sn.lhacksrt.maeliaserver.preprocessing.domain.model;

import java.util.List;

/**
 * Nœud du graphe de dépendances = un fichier du catalogue.
 * {@code level} = profondeur topologique (0 = racine sans dépendance,
 * n = 1 + max(level des dépendances), -1 = impliqué dans un cycle).
 */
public record DependencyNode(
        String dataSpecId,
        String module,
        String fileName,
        String fileType,
        String generation,
        int level,
        List<String> dependsOn,
        List<String> requiredBy
) {}
