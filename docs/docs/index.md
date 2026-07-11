---
hide:
  - navigation
---

<div class="maelia-hero" markdown>

# Plateforme MAELIA

Application web de **gestion de projets de simulation MAELIA** — évaluation intégrée
**eau / agriculture / normes** — pilotant le moteur multi-agents **GAMA** de bout en bout,
de la saisie des données à la visualisation des sorties.

</div>

## Qu'est-ce que MAELIA ?

**MAELIA** est une plateforme de simulation multi-agents de l'INRAE (cas de référence
Garonne-Amont / échantillon SASSEME) qui s'exécute sur le moteur **GAMA**. Cette application
web l'enveloppe d'une couche de gestion permettant de :

1. **Créer un projet** sur un territoire et le configurer (`ModelingConfiguration`) ;
2. **Saisir ou uploader les données d'entrée** (~70 fichiers CSV/SHP décrits par un catalogue `DataSpec`) ;
3. **Configurer un scénario** (142 paramètres du launcher MAELIA, décrits par un catalogue `ParameterSpec`) ;
4. **Lancer une simulation** sur GAMA headless (piloté par WebSocket) et suivre sa progression en temps réel (STOMP) ;
5. **Visualiser les sorties** (séries temporelles CSV, artefacts PNG/CSV/XML).

!!! tip "Principe directeur — tout est piloté par des schémas"
    Aucune logique propre à un fichier de données ou à un paramètre du launcher n'est codée en
    dur. Les données passent par le catalogue `DataSpec`/`FieldSpec` (71 types), les paramètres de
    simulation par le catalogue `ParameterSpec` (142 paramètres extraits de `launcherBase.gaml`).
    **La base de données est la source de vérité** ; les fichiers d'entrée GAMA sont régénérés à la
    demande (« matérialisation des includes »).

## Par où commencer

<div class="grid cards" markdown>

-   :material-rocket-launch:{ .lg .middle } __Installation & démarrage__

    ---

    Lancer toute la plateforme avec Docker Compose en quelques commandes.

    [:octicons-arrow-right-24: Démarrer](guides/installation.md)

-   :material-book-open-variant:{ .lg .middle } __Guide d'utilisation__

    ---

    Le parcours complet : projet → données → scénario → run → résultats.

    [:octicons-arrow-right-24: Utiliser la plateforme](guides/utilisation.md)

-   :material-sitemap:{ .lg .middle } __Architecture__

    ---

    Backend hexagonal par contexte métier et frontend Feature-Sliced Design.

    [:octicons-arrow-right-24: Comprendre l'architecture](architecture/index.md)

-   :material-api:{ .lg .middle } __Référence API__

    ---

    Tous les endpoints REST sous le préfixe `/api/v1` et le canal temps réel STOMP.

    [:octicons-arrow-right-24: Consulter l'API](reference/api.md)

</div>

## Pile technique

| Couche | Technologie |
|---|---|
| Backend | Java 21, Spring Boot 3.3.6 (Web, Validation, WebSocket/STOMP, AMQP, Data JPA, Actuator), Hibernate Spatial, Flyway, MapStruct, Lombok, springdoc-openapi, client MinIO |
| Base de données | PostgreSQL 16 + PostGIS 3.4 |
| Messagerie | RabbitMQ 3.13 (file des runs + fanout des mises à jour) |
| Stockage objet | MinIO (bucket `maelia`) |
| Moteur de simulation | GAMA headless en mode serveur WebSocket (`-socket 6868`) |
| Frontend | React 19 / TypeScript / Vite, architecture Feature-Sliced Design |

!!! note "Authentification"
    L'authentification n'est **pas encore implémentée** (jalon final M9) : l'application tourne
    avec un utilisateur implicite unique (`maelia.security.enabled=false`).
