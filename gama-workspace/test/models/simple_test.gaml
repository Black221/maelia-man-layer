/**
 * simple_test.gaml — Modèle GAMA de TEST, totalement indépendant de MAELIA.
 *
 * But : valider la chaîne complète front ↔ back ↔ RabbitMQ ↔ GAMA ↔ résultat,
 * sans aucune donnée d'entrée (CSV/SHP) ni plugin compilé. Auto-suffisant.
 *
 * Principe : une population d'agents se déplace aléatoirement ; une « infection »
 * se propage par contact. Le modèle s'arrête de lui-même au cycle `nb_steps`.
 *
 * Arrêt headless : la condition d'arrêt est portée par le facet `until:` de
 * l'EXPÉRIENCE (GAML standard, indépendant de la version du serveur) ; quand elle
 * devient vraie, GAMA termine la simulation et émet `SimulationEnded`. On garde aussi
 * la globale `sim_termine` que le backend passe en `until` protocolaire (défense).
 *
 * Sorties observables côté front :
 *   - barre de progression (messages `status` = cycle courant),
 *   - journal GAMA en temps réel (les `write` ci-dessous),
 *   - cycle final (évalué via la commande `expression` à la fin du run).
 */
model SimpleTest

global {
    // --- Paramètres exposés par l'expérience ---
    int nb_people <- 200;              // taille de la population
    int nb_steps <- 100;               // nombre de pas avant arrêt automatique
    float infection_distance <- 2.0;   // rayon de contagion
    float infection_proba <- 0.05;     // probabilité de transmission par contact
    float people_speed <- 2.0;         // vitesse de déplacement des agents

    // --- Indicateurs suivis ---
    // ATTRIBUTS STOCKÉS (pas de fonction `->`) : recalculés une seule fois par pas
    // dans le reflex `suivi`. En gama-server, les monitors lisent ces valeurs depuis
    // un autre thread ; une fonction paresseuse `people count(...)` itérerait alors la
    // population pendant que la simulation la modifie → ConcurrentModificationException.
    int nb_infected <- 1;
    int nb_healthy  <- nb_people - 1;

    // --- Condition d'arrêt lue par le facet `until:` de l'expérience ---
    bool sim_termine <- false;

    init {
        // Throttle : ~50 ms/pas pour que la progression soit observable en temps réel
        // côté front (sinon la simulation se termine en quelques millisecondes).
        minimum_cycle_duration <- 0.05;

        create people number: nb_people;
        ask one_of(people) { infected <- true; }   // patient zéro
        // Echo des paramètres reçus : permet de vérifier dans le journal du run
        // que les valeurs saisies dans le front sont bien appliquées par GAMA.
        write "Init : " + nb_people + " agents créés, 1 infecté initial.";
        write "Paramètres : nb_steps=" + nb_steps + " distance=" + infection_distance
            + " proba=" + infection_proba + " vitesse=" + people_speed;
    }

    // Suivi + arrêt. Recalcule les compteurs (scope global, après le pas des agents :
    // pas d'itération concurrente) puis écrit une ligne streamée vers le front.
    reflex suivi {
        nb_infected <- people count (each.infected);
        nb_healthy  <- nb_people - nb_infected;
        write "cycle " + cycle + " — infectés : " + nb_infected + "/" + nb_people;
        if (cycle >= nb_steps) {
            sim_termine <- true;
            write "Simulation terminée au cycle " + cycle + " (infectés : " + nb_infected + ").";
        }
    }
}

species people skills: [moving] {
    bool infected <- false;
    float speed <- people_speed;

    reflex se_deplacer {
        do wander amplitude: 120.0;
    }

    reflex contaminer when: infected {
        ask (people at_distance infection_distance) where (!each.infected) {
            if (flip(infection_proba)) {
                infected <- true;
            }
        }
    }
}

// Expérience compatible serveur headless (pilotée par load/play en WebSocket).
// `until:` = condition d'arrêt évaluée par GAMA après chaque pas → SimulationEnded.
experiment test_simulation type: gui until: sim_termine {
    parameter "Taille de la population" var: nb_people min: 10 max: 2000;
    parameter "Nombre de pas (arrêt)"   var: nb_steps min: 10 max: 1000;
    parameter "Distance d'infection"    var: infection_distance min: 0.5 max: 10.0 category: "Infection";
    parameter "Probabilité d'infection" var: infection_proba min: 0.0 max: 1.0 category: "Infection";
    parameter "Vitesse des agents"      var: people_speed min: 0.5 max: 10.0;

    output {
        monitor "Cycle"    value: cycle;
        monitor "Infectés" value: nb_infected;
        monitor "Sains"    value: nb_healthy;
    }
}
