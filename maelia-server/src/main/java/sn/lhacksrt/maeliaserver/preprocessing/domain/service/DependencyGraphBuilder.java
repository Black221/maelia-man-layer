package sn.lhacksrt.maeliaserver.preprocessing.domain.service;

import sn.lhacksrt.maeliaserver.catalog.domain.model.DataSpec;
import sn.lhacksrt.maeliaserver.catalog.domain.model.FieldSpec;
import sn.lhacksrt.maeliaserver.preprocessing.domain.model.DependencyEdge;
import sn.lhacksrt.maeliaserver.preprocessing.domain.model.DependencyGraph;
import sn.lhacksrt.maeliaserver.preprocessing.domain.model.DependencyKind;
import sn.lhacksrt.maeliaserver.preprocessing.domain.model.DependencyNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Construit le graphe de dépendances entre fichiers du catalogue à partir :
 * 1) des références par identifiant portées par les colonnes (FieldSpec.referencesDataSpec) ;
 * 2) des dépendances implicites "par construction" (DataSpec.dependsOn).
 * Calcule ensuite les niveaux topologiques (ordre de génération du prétraitement)
 * et détecte les cycles. Domaine pur, sans dépendance framework.
 */
public final class DependencyGraphBuilder {

    private DependencyGraphBuilder() {}

    public static DependencyGraph build(List<DataSpec> specs) {
        Set<String> knownIds = new LinkedHashSet<>();
        for (DataSpec s : specs) knownIds.add(s.id());

        List<DependencyEdge> edges = new ArrayList<>();
        Set<String> edgeKeys = new LinkedHashSet<>();
        Set<String> unknownRefs = new TreeSet<>();
        // dépendances par nœud (targetId -> sources), et inverse
        Map<String, Set<String>> depsOf = new TreeMap<>();
        Map<String, Set<String>> requiredBy = new TreeMap<>();
        for (String id : knownIds) {
            depsOf.put(id, new TreeSet<>());
            requiredBy.put(id, new TreeSet<>());
        }

        for (DataSpec spec : specs) {
            // 1) Références explicites (colonnes FK)
            if (spec.fields() != null) {
                for (FieldSpec f : spec.fields()) {
                    String ref = f.referencesDataSpec();
                    if (ref == null || ref.isBlank() || ref.equals(spec.id())) continue;
                    if (!knownIds.contains(ref)) {
                        unknownRefs.add(spec.id() + " -> " + ref);
                        continue;
                    }
                    addEdge(edges, edgeKeys, depsOf, requiredBy,
                            new DependencyEdge(ref, spec.id(), f.label(), DependencyKind.EXPLICIT));
                }
            }
            // 2) Dépendances implicites (par construction)
            if (spec.dependsOn() != null) {
                for (String dep : spec.dependsOn()) {
                    if (dep == null || dep.isBlank() || dep.equals(spec.id())) continue;
                    if (!knownIds.contains(dep)) {
                        unknownRefs.add(spec.id() + " -> " + dep);
                        continue;
                    }
                    addEdge(edges, edgeKeys, depsOf, requiredBy,
                            new DependencyEdge(dep, spec.id(), null, DependencyKind.IMPLICIT));
                }
            }
        }

        Map<String, Integer> levels = computeLevels(knownIds, depsOf);
        List<String> cycleIds = levels.entrySet().stream()
                .filter(e -> e.getValue() == -1)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();

        List<DependencyNode> nodes = new ArrayList<>();
        for (DataSpec s : specs) {
            nodes.add(new DependencyNode(
                    s.id(), s.module(), s.fileName(), s.fileType(), s.generation(),
                    levels.getOrDefault(s.id(), 0),
                    List.copyOf(depsOf.get(s.id())),
                    List.copyOf(requiredBy.get(s.id()))));
        }
        nodes.sort(Comparator
                .comparingInt((DependencyNode n) -> n.level() == -1 ? Integer.MAX_VALUE : n.level())
                .thenComparing(DependencyNode::dataSpecId));

        return new DependencyGraph(List.copyOf(nodes), List.copyOf(edges),
                !cycleIds.isEmpty(), cycleIds, List.copyOf(unknownRefs));
    }

    private static void addEdge(List<DependencyEdge> edges, Set<String> edgeKeys,
                                Map<String, Set<String>> depsOf, Map<String, Set<String>> requiredBy,
                                DependencyEdge edge) {
        String key = edge.sourceId() + "|" + edge.targetId() + "|"
                + (edge.viaField() == null ? "" : edge.viaField());
        if (!edgeKeys.add(key)) return;
        edges.add(edge);
        depsOf.get(edge.targetId()).add(edge.sourceId());
        requiredBy.get(edge.sourceId()).add(edge.targetId());
    }

    /**
     * Niveau topologique par point fixe : 0 = aucune dépendance,
     * sinon 1 + max(niveau des dépendances). Les nœuds jamais résolus
     * (cycle ou dépendant d'un cycle) restent à -1.
     */
    private static Map<String, Integer> computeLevels(Set<String> ids, Map<String, Set<String>> depsOf) {
        Map<String, Integer> levels = new HashMap<>();
        for (String id : ids) {
            if (depsOf.get(id).isEmpty()) levels.put(id, 0);
        }
        boolean progressed = true;
        while (progressed) {
            progressed = false;
            for (String id : ids) {
                if (levels.containsKey(id)) continue;
                Set<String> deps = depsOf.get(id);
                if (deps.stream().allMatch(levels::containsKey)) {
                    int max = deps.stream().mapToInt(levels::get).max().orElse(-1);
                    levels.put(id, max + 1);
                    progressed = true;
                }
            }
        }
        for (String id : ids) levels.putIfAbsent(id, -1);
        return levels;
    }
}
