package sn.lhacksrt.maeliaserver.preprocessing.api.dto;

import sn.lhacksrt.maeliaserver.preprocessing.domain.model.DependencyEdge;
import sn.lhacksrt.maeliaserver.preprocessing.domain.model.DependencyGraph;
import sn.lhacksrt.maeliaserver.preprocessing.domain.model.DependencyNode;

import java.util.List;

public record DependencyGraphDto(
        List<NodeDto> nodes,
        List<EdgeDto> edges,
        boolean hasCycle,
        List<String> cycleIds,
        List<String> unknownReferences
) {
    public record NodeDto(
            String dataSpecId,
            String module,
            String fileName,
            String fileType,
            String generation,
            int level,
            List<String> dependsOn,
            List<String> requiredBy
    ) {
        static NodeDto from(DependencyNode n) {
            return new NodeDto(n.dataSpecId(), n.module(), n.fileName(), n.fileType(),
                    n.generation(), n.level(), n.dependsOn(), n.requiredBy());
        }
    }

    public record EdgeDto(String sourceId, String targetId, String viaField, String kind) {
        static EdgeDto from(DependencyEdge e) {
            return new EdgeDto(e.sourceId(), e.targetId(), e.viaField(), e.kind().name());
        }
    }

    public static DependencyGraphDto from(DependencyGraph g) {
        return new DependencyGraphDto(
                g.nodes().stream().map(NodeDto::from).toList(),
                g.edges().stream().map(EdgeDto::from).toList(),
                g.hasCycle(), g.cycleIds(), g.unknownReferences());
    }
}
