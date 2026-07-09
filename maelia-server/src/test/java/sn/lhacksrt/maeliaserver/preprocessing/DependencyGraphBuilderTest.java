package sn.lhacksrt.maeliaserver.preprocessing;

import org.junit.jupiter.api.Test;
import sn.lhacksrt.maeliaserver.catalog.domain.model.DataSpec;
import sn.lhacksrt.maeliaserver.catalog.domain.model.FieldSpec;
import sn.lhacksrt.maeliaserver.catalog.domain.model.Orientation;
import sn.lhacksrt.maeliaserver.preprocessing.domain.model.DependencyGraph;
import sn.lhacksrt.maeliaserver.preprocessing.domain.model.DependencyKind;
import sn.lhacksrt.maeliaserver.preprocessing.domain.model.DependencyNode;
import sn.lhacksrt.maeliaserver.preprocessing.domain.service.DependencyGraphBuilder;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class DependencyGraphBuilderTest {

    private FieldSpec ref(String label, String referencesDataSpec) {
        return new FieldSpec(null, label, null, "String", null, true, null,
                referencesDataSpec, null, null, List.of(), 0);
    }

    private DataSpec spec(String id, String generation, List<String> dependsOn, FieldSpec... fields) {
        return new DataSpec(id, "HYDROGRAPHIQUE", "f", id + ".shp", "SHP", null,
                Orientation.FIELDS_AS_COLUMNS, null, ";", generation, true, null, "NONE",
                false, null, null, "MAP", null, "VERIFIED", "SEED", dependsOn, List.of(fields));
    }

    @Test
    void chaineExpliciteDonneDesNiveauxTopologiques() {
        // ZH (racine) <- sol (ID_ZH) <- ilots (ID_SOL) <- parcelles (ID_ILOT)
        DataSpec zh = spec("zh", "AUTO", List.of());
        DataSpec sol = spec("sol", "AUTO", List.of(), ref("ID_ZH", "zh"));
        DataSpec ilots = spec("ilots", "AUTO", List.of(), ref("ID_SOL", "sol"), ref("ID_ZH", "zh"));
        DataSpec parcelles = spec("parcelles", "AUTO", List.of(), ref("ID_ILOT", "ilots"));

        DependencyGraph g = DependencyGraphBuilder.build(List.of(parcelles, ilots, sol, zh));

        Map<String, DependencyNode> byId = g.nodes().stream()
                .collect(Collectors.toMap(DependencyNode::dataSpecId, Function.identity()));
        assertEquals(0, byId.get("zh").level());
        assertEquals(1, byId.get("sol").level());
        assertEquals(2, byId.get("ilots").level());
        assertEquals(3, byId.get("parcelles").level());
        assertFalse(g.hasCycle());
        // nœuds triés par niveau croissant
        assertEquals("zh", g.nodes().get(0).dataSpecId());
        assertEquals(List.of("ilots", "sol"), byId.get("zh").requiredBy());
    }

    @Test
    void dependanceImpliciteEstPriseEnCompte() {
        DataSpec zh = spec("zh", "AUTO", List.of());
        DataSpec contour = spec("contour", "AUTO", List.of("zh")); // aucune colonne FK

        DependencyGraph g = DependencyGraphBuilder.build(List.of(contour, zh));

        assertEquals(1, g.edges().size());
        assertEquals(DependencyKind.IMPLICIT, g.edges().get(0).kind());
        assertEquals("zh", g.edges().get(0).sourceId());
        assertEquals("contour", g.edges().get(0).targetId());
        assertNull(g.edges().get(0).viaField());
    }

    @Test
    void doublonsEtAutoReferencesSontIgnores() {
        // dependsOn redondant avec la FK sur la même paire (via différent → 2 arêtes,
        // mais une seule dépendance) + auto-référence ignorée
        DataSpec zh = spec("zh", "AUTO", List.of("zh")); // auto-référence
        DataSpec sol = spec("sol", "AUTO", List.of("zh"), ref("ID_ZH", "zh"));

        DependencyGraph g = DependencyGraphBuilder.build(List.of(zh, sol));

        Map<String, DependencyNode> byId = g.nodes().stream()
                .collect(Collectors.toMap(DependencyNode::dataSpecId, Function.identity()));
        assertEquals(List.of("zh"), byId.get("sol").dependsOn());
        assertEquals(List.of(), byId.get("zh").dependsOn());
        assertEquals(0, byId.get("zh").level());
        assertEquals(1, byId.get("sol").level());
    }

    @Test
    void cycleEstDetecte() {
        DataSpec a = spec("a", "AUTO", List.of(), ref("ID_B", "b"));
        DataSpec b = spec("b", "AUTO", List.of(), ref("ID_A", "a"));
        DataSpec racine = spec("racine", "MANUAL", List.of());

        DependencyGraph g = DependencyGraphBuilder.build(List.of(a, b, racine));

        assertTrue(g.hasCycle());
        assertEquals(List.of("a", "b"), g.cycleIds());
        Map<String, DependencyNode> byId = g.nodes().stream()
                .collect(Collectors.toMap(DependencyNode::dataSpecId, Function.identity()));
        assertEquals(-1, byId.get("a").level());
        assertEquals(0, byId.get("racine").level());
    }

    @Test
    void referenceInconnueEstRemonteeSansCasserLeGraphe() {
        DataSpec sol = spec("sol", "AUTO", List.of(), ref("ID_ZH", "zh.absent"));

        DependencyGraph g = DependencyGraphBuilder.build(List.of(sol));

        assertTrue(g.edges().isEmpty());
        assertEquals(List.of("sol -> zh.absent"), g.unknownReferences());
        assertEquals(0, g.nodes().get(0).level());
    }
}
