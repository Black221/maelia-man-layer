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
        /** Colonne (label de champ) du DataSpec dont on propose les valeurs. Null = 1er champ.
         *  Permet de proposer autre chose que l'ID (ex. un nom, un type de sol ZONE_PEDO…). */
        String optionsColumn,
        /** Source des valeurs proposées : {@code COLUMN} (défaut, valeurs distinctes d'une colonne),
         *  {@code COLUMN_HEADERS} (noms de colonnes, hors 1re — ex. cultures d'especesCultivees),
         *  {@code INSTANCE_KEYS} (clés d'instance d'un DataSpec multi-instance — ex. prixVentesXX). */
        String optionsSource,
        boolean advanced,
        int sortOrder
) {}
