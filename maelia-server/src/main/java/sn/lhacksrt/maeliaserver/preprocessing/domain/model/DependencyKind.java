package sn.lhacksrt.maeliaserver.preprocessing.domain.model;

/**
 * Nature d'une dépendance entre deux fichiers du catalogue :
 * EXPLICIT = portée par une colonne (FieldSpec.referencesDataSpec, clé étrangère),
 * IMPLICIT = par construction (DataSpec.dependsOn, ex. contourZH.shp dérivé de ZH.shp).
 */
public enum DependencyKind {
    EXPLICIT,
    IMPLICIT
}
