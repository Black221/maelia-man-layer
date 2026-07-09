package sn.lhacksrt.maeliaserver.catalog.application.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Évalue les expressions requiredIf du catalogue MAELIA contre une ModelingConfiguration.
 *
 * Patterns supportés (tirés du seed) :
 *   module.agricole == true
 *   assolementMethod == FONCTIONS_DE_CROYANCE
 *   cropModel == 'AQYIELD'
 *   scenarioClimatique != null
 */
@Component
public class RequiredIfEvaluator {

    @SuppressWarnings("unchecked")
    public boolean isRequired(String requiredIf, Map<String, Object> config) {
        if (requiredIf == null || requiredIf.isBlank()) return true;

        String expr = requiredIf.trim();

        // module.xxx == true
        if (expr.startsWith("module.")) {
            String moduleName = expr.replaceFirst("module\\.", "").replaceAll("\\s*==\\s*true", "").trim();
            Object modules = config.get("modules");
            if (modules instanceof List<?> list) {
                return list.contains(moduleName);
            }
            return false;
        }

        // field != null
        if (expr.contains("!= null")) {
            String field = expr.replace("!= null", "").trim();
            Object val = config.get(field);
            return val != null && !val.toString().isBlank();
        }

        // field == VALUE (with or without quotes)
        if (expr.contains("==")) {
            String[] parts = expr.split("==", 2);
            String field = parts[0].trim();
            String expected = parts[1].trim().replaceAll("^['\"]|['\"]$", "");
            Object actual = config.get(field);
            return actual != null && actual.toString().equals(expected);
        }

        return false;
    }
}
