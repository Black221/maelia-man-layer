package sn.lhacksrt.maeliaserver.paramcatalog.domain.model;

import java.util.List;

/**
 * Spécification d'un paramètre de simulation, extraite de launcherBase.gaml.
 * Catalogue immuable côté utilisateur (analogue de FieldSpec pour les données).
 *
 * {@code defaultValue} est conservé sous forme textuelle (les listes sont jointes par '|') ;
 * {@code type} indique comment le coercer.
 */
public record ParameterSpec(
        String gamlName,
        String label,
        String group,
        ParamType type,
        String defaultValue,
        String unit,
        List<String> allowedValues,
        String visibleIf,
        /** Condition d'ACTIVATION (même grammaire que visibleIf) : si fausse, le champ est
         *  affiché mais désactivé. Sert aux dépendances entre paramètres (ex. un id n'est
         *  saisissable que si la case « simulationSurX » est cochée). */
        String enabledIf,
        /** Id d'un DataSpec dont le dataset projet alimente les valeurs proposées (select issu
         *  des données ; valeurs distinctes du champ clé, comme les sélecteurs référentiels). */
        String optionsDataSpec,
        boolean advanced,
        int sortOrder
) {}
