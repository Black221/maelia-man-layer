package sn.lhacksrt.maeliaserver.preprocessing.domain.model;

/**
 * Statut d'un fichier dans le plan de prétraitement d'un projet :
 * DONE = le dataset existe déjà, READY = toutes les dépendances sont satisfaites,
 * BLOCKED = au moins une dépendance manque.
 */
public enum PlanEntryStatus {
    DONE,
    READY,
    BLOCKED
}
