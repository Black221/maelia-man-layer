package sn.lhacksrt.maeliaserver.simulation.infrastructure.gama.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Parse les payloads JSON bruts du protocole GAMA en GamaMessage typés.
 * Les noms de champs proviennent de la doc officielle GAMA WebSocket Server.
 */
@Component
public class GamaMessageParser {

    private static final Logger log = LoggerFactory.getLogger(GamaMessageParser.class);
    private final ObjectMapper mapper = new ObjectMapper();

    public GamaMessage parse(String rawJson) {
        try {
            JsonNode root = mapper.readTree(rawJson);
            String type = root.path("type").asText("");

            return switch (type) {
                case "ConnectionSuccessful" ->
                        new GamaMessage.ConnectionSuccessful(root.path("content").asText(""));

                case "CommandExecutedSuccessfully" -> {
                    // Doc GAMA : pour "load", l'experiment_id est renvoyé dans `content`.
                    // Pour les autres commandes, `content` porte le résultat ; on retombe alors
                    // sur command.exp_id / exp_id pour identifier l'expérience.
                    String content = root.path("content").asText("");
                    String expId = firstNonBlank(
                            content,
                            root.path("command").path("exp_id").asText(""),
                            root.path("exp_id").asText(""));
                    yield new GamaMessage.CommandExecuted(expId, content);
                }

                case "SimulationStatusInform" ->
                        new GamaMessage.StatusInform(
                                root.path("exp_id").asText(""),
                                root.path("step").asInt(root.path("cycle").asInt(0)),
                                root.path("time").asDouble(0.0),
                                content(root));

                case "SimulationStatusError" ->
                        new GamaMessage.StatusError(
                                root.path("exp_id").asText(""),
                                content(root));

                case "SimulationStatusNeutral" ->
                        new GamaMessage.StatusNeutral(
                                root.path("exp_id").asText(""),
                                content(root));

                case "SimulationOutput" ->
                        new GamaMessage.SimulationOutput(
                                root.path("exp_id").asText(""),
                                content(root));

                case "SimulationDebug" ->
                        new GamaMessage.SimulationDebug(
                                root.path("exp_id").asText(""),
                                content(root));

                case "SimulationEnded" ->
                        new GamaMessage.SimulationEnded(root.path("exp_id").asText(""));

                case "SimulationError" ->
                        new GamaMessage.SimulationError(
                                root.path("exp_id").asText(""),
                                content(root));

                case "RuntimeError" ->
                        new GamaMessage.RuntimeError(
                                root.path("exp_id").asText(""),
                                content(root));

                case "GamaServerError" ->
                        new GamaMessage.GamaServerError(content(root));

                case "UnableToExecuteRequest" ->
                        new GamaMessage.UnableToExecute(content(root));

                case "MalformedRequest" ->
                        new GamaMessage.MalformedRequest(content(root));

                default -> {
                    log.warn("Unknown GAMA message type='{}': {}", type, rawJson);
                    yield new GamaMessage.Unknown(type, rawJson);
                }
            };
        } catch (Exception e) {
            log.error("Failed to parse GAMA message: {}", rawJson, e);
            return new GamaMessage.Unknown("parse-error", rawJson);
        }
    }

    /**
     * Extrait le texte du champ {@code content}, robuste aux variations de format GAMA :
     * selon la version/le type de message, {@code content} est soit une chaîne, soit un
     * objet {@code {"message": "...", "color": null}} (cas des SimulationOutput récents).
     * Sans cela, {@code asText()} sur un nœud objet renvoie une chaîne vide → journal vide.
     */
    private static String content(JsonNode root) {
        JsonNode node = root.path("content");
        if (node.isMissingNode() || node.isNull()) return "";
        if (node.isObject()) {
            JsonNode message = node.path("message");
            if (!message.isMissingNode() && !message.isNull()) return message.asText("");
            return node.toString();
        }
        return node.asText("");
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return "";
    }
}
