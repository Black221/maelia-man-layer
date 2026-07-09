package sn.lhacksrt.maeliaserver.simulation.domain.model;

public enum RunStatus {
    EN_FILE,    // publié dans la file, en attente du worker
    EN_COURS,   // GAMA en train d'exécuter
    TERMINE,    // simulation terminée avec succès
    ECHEC,      // erreur GAMA ou timeout
    ANNULE      // annulé par l'utilisateur
}
