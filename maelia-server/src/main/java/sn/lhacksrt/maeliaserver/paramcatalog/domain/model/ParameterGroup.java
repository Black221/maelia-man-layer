package sn.lhacksrt.maeliaserver.paramcatalog.domain.model;

/** Regroupement de paramètres (section du launcher : général, hydro, agricole, sorties…). */
public record ParameterGroup(
        String id,
        String label,
        int sortOrder,
        String parentId
) {}
