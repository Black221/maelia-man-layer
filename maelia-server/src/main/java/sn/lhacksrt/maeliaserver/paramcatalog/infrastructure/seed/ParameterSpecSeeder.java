package sn.lhacksrt.maeliaserver.paramcatalog.infrastructure.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.model.ParamType;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.model.ParameterGroup;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.model.ParameterSpec;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.port.out.ParameterCatalogRepository;

import java.util.ArrayList;
import java.util.List;

/** Charge le catalogue de paramètres de simulation (seed extrait de launcherBase.gaml) au démarrage. */
@Component
public class ParameterSpecSeeder {

    private static final Logger log = LoggerFactory.getLogger(ParameterSpecSeeder.class);

    private final ParameterCatalogRepository repository;
    private final ObjectMapper objectMapper;

    @Value("${maelia.catalog.scenario-params-seed-location:classpath:catalog/scenario-parameters-seed.json}")
    private Resource seedResource;

    public ParameterSpecSeeder(ParameterCatalogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (repository.countSpecs() > 0) {
            log.debug("Catalogue de paramètres déjà chargé ({} entrées), seed ignoré.", repository.countSpecs());
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(seedResource.getInputStream());

            List<ParameterGroup> groups = new ArrayList<>();
            for (JsonNode g : root.path("groups")) {
                groups.add(new ParameterGroup(
                        g.path("id").asText(),
                        g.path("label").asText(""),
                        g.path("order").asInt(0),
                        nullable(g, "parentId")));
            }

            List<ParameterSpec> specs = new ArrayList<>();
            for (JsonNode p : root.path("parameters")) {
                specs.add(new ParameterSpec(
                        p.path("gamlName").asText(),
                        p.path("label").asText(""),
                        p.path("group").asText("general"),
                        ParamType.fromString(p.path("type").asText("STRING")),
                        defaultValueToText(p.path("defaultValue")),
                        nullable(p, "unit"),
                        toStringList(p.path("allowedValues")),
                        nullable(p, "visibleIf"),
                        nullable(p, "enabledIf"),
                        nullable(p, "optionsDataSpec"),
                        p.path("advanced").asBoolean(false),
                        p.path("order").asInt(0)));
            }

            repository.saveGroups(groups);
            repository.saveSpecs(specs);
            log.info("Catalogue de paramètres chargé : {} groupe(s), {} paramètre(s).",
                    groups.size(), specs.size());
        } catch (Exception e) {
            log.error("Échec du chargement du catalogue de paramètres : {}", e.getMessage(), e);
        }
    }

    /** Conserve la valeur par défaut sous forme textuelle (les listes sont jointes par '|'). */
    private String defaultValueToText(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) return null;
        if (node.isArray()) {
            List<String> items = new ArrayList<>();
            node.forEach(n -> items.add(n.asText()));
            return String.join("|", items);
        }
        return node.asText();
    }

    private List<String> toStringList(JsonNode node) {
        if (node == null || !node.isArray() || node.isEmpty()) return null;
        List<String> out = new ArrayList<>();
        node.forEach(n -> out.add(n.asText()));
        return out;
    }

    private String nullable(JsonNode node, String field) {
        JsonNode n = node.path(field);
        return (n.isNull() || n.isMissingNode() || n.asText().isBlank()) ? null : n.asText();
    }
}
