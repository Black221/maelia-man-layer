# BACKEND.md — Plateforme MAELIA : description & architecture du backend

> Document de référence unique du backend. Il remplace `analyse-backend.md`, `architecture-backend.md`, `architecture-catalogue.md`, `architecture-scenario.md` et l'ancien `CLAUDE.md`. Basé sur l'analyse du code réel de `maelia-server/`, du `docker-compose.yml` et de `gama-workspace/` (juillet 2026).

---

## 1. Description du projet

Application web de **gestion de projets de simulation MAELIA**. MAELIA est une plateforme multi-agents de l'INRAE (évaluation intégrée eau / agriculture / normes, cas de référence Garonne-Amont / échantillon SASSEME) qui s'exécute sur le moteur **GAMA**. L'application permet de :

1. **Créer un projet** sur un territoire et le configurer (`ModelingConfiguration`) ;
2. **Saisir ou uploader les données d'entrée** (~70 fichiers CSV/SHP décrits par un catalogue `DataSpec`) ;
3. **Configurer un scénario** (142 paramètres du launcher MAELIA, décrits par un catalogue `ParameterSpec`) ;
4. **Lancer une simulation** sur GAMA headless (piloté par WebSocket) et suivre sa progression en temps réel (STOMP) ;
5. **Visualiser les sorties** (séries temporelles CSV, artefacts PNG/CSV/XML).

**Principe directeur : tout est piloté par des schémas.** Aucune logique propre à un fichier de données ou à un paramètre du launcher n'est codée en dur : les données passent par le catalogue `DataSpec`/`FieldSpec` (seed `catalog/maelia-database.json`, 71 types), les paramètres de simulation par le catalogue `ParameterSpec`/`ParameterGroup` (seed `catalog/scenario-parameters-seed.json`, 142 paramètres extraits de `launcherBase.gaml`). La **base de données est la source de vérité** ; les fichiers d'entrée GAMA sont régénérés à la demande (« matérialisation des includes »).

L'authentification n'est **pas encore implémentée** (jalon final M9) : l'application tourne avec un utilisateur implicite unique (`maelia.security.enabled=false`).

## 2. Pile technique

| Couche | Technologie |
|---|---|
| Backend | Java 21, Spring Boot 3.3.6 (Web, Validation, WebSocket/STOMP, AMQP, Data JPA, Actuator), Hibernate Spatial, hypersistence-utils (JSONB), Flyway, MapStruct, Lombok, Apache Commons CSV, springdoc-openapi, client MinIO |
| Base de données | PostgreSQL 16 + PostGIS 3.4 |
| Messagerie | RabbitMQ 3.13 (file des runs + fanout des mises à jour) |
| Stockage objet | MinIO (bucket `maelia` créé au démarrage par `MinioConfig`, best-effort) : stocke les shapefiles uploadés par l'utilisateur (C8, clés `projects/{pid}/shp/{dataSpecId}/…`), relus à chaque matérialisation |
| Moteur de simulation | GAMA headless en mode serveur WebSocket (`-socket 6868`), image locale `maelia-man-layer-gama-headless` (Alpine + JDK21 + GAMA) |
| Frontend | React 18 / TypeScript / Vite (repo `maelia-front/`, architecture Feature-Sliced Design — hors périmètre de ce document) |

## 3. Topologie Docker Compose

`docker-compose.yml` décrit 7 services :

| Service | Image | Ports | Rôle |
|---|---|---|---|
| `db` | `postgis/postgis:16-3.4` | 5432 | Base `maelia` (user/pass `maelia`), volume `db-data`, healthcheck `pg_isready` |
| `rabbitmq` | `rabbitmq:3.13-management` | 5672, 15672 (console) | File des runs ; `consumer_timeout` relevé via `rabbitmq/conf.d/20-consumer-timeout.conf` pour les runs longs |
| `minio` | `minio/minio` | 9000, 9001 (console) | Stockage objet, volume `minio-data` |
| `gama-headless` | `maelia-man-layer-gama-headless` (image locale Alpine + JDK21 + GAMA) | 6868 | Serveur GAMA WebSocket ; monte `./gama-workspace` → `/workspace` (WORKDIR de l'image, où le launcher crée son workspace Eclipse `.workspace*`) |
| `api` | `maelia/backend:local` (build `maelia-server/`) | 8080 | Profil Spring `api` : REST + STOMP + relais des mises à jour ; monte **aussi** `./gama-workspace` (matérialisation des includes au lancement d'un run + lecture des artefacts) |
| `worker` | même image que `api` | — | Profil Spring `worker` : consomme la file RabbitMQ, pilote GAMA ; monte **le même volume** `./gama-workspace` que `gama-headless` |
| `frontend` | `maelia/frontend:local` (build `maelia-front/`) | 8081 (nginx) | SPA React |

Points clés :
- **`api` et `worker` partagent la même image** ; seule la variable `SPRING_PROFILES_ACTIVE` change. Le worker n'a pas de serveur web (`web-application-type: none`).
- Le volume `./gama-workspace` est le **canal d'échange de fichiers** worker ↔ GAMA (mode `SHARED_VOLUME`) : le worker y matérialise les includes, GAMA y lit le modèle et y écrit ses sorties. Le répertoire doit être accessible en écriture (GAMA crée des workspaces à la volée).
- URLs utiles : API `http://localhost:8080/api/v1`, Swagger `http://localhost:8080/swagger-ui.html`, front `http://localhost:8081`, GAMA `ws://localhost:6868`.

```bash
docker compose build && docker compose up -d
docker compose logs -f api worker gama-headless
```

## 4. Architecture backend (hexagonale, par contexte métier)

Package racine : `sn.lhacksrt.maeliaserver`. Le code est découpé **par contexte métier** (bounded context), pas par couche globale. Chaque contexte suit la même structure hexagonale :

```
<contexte>/
├── api/                    # Controllers REST + DTOs (adaptateur entrant)
├── application/            # Services applicatifs (use cases)
├── domain/
│   ├── model/              # Domaine pur, sans annotation framework
│   └── port/ (in / out)    # Interfaces : use cases entrants, dépendances sortantes
└── infrastructure/         # Adaptateurs sortants : persistence JPA, GAMA, messaging, seed
```

Règle : les dépendances pointent vers l'intérieur (le domaine ne connaît ni Spring ni JPA) ; la communication inter-contextes passe par les ports (ex. `RunWorker` appelle `IngestOutputsUseCase` du contexte `result`).

### 4.1 Les 9 contextes

| Contexte | Responsabilité | Éléments principaux |
|---|---|---|
| `bootstrap` | Socle transverse | `WebConfig` (CORS), `StompConfig` (WS `/ws`, topics `/topic/**`), `RabbitMqConfig`, `GlobalExceptionHandler` (RFC 7807), `HealthController` |
| `catalog` | Catalogue des types de données d'entrée | `DataSpec` / `FieldSpec` / `Orientation` (FIELDS_AS_COLUMNS ou FIELDS_AS_ROWS/transposé), `RequiredIfEvaluator` (applicabilité selon la config projet), `DataSpecSeeder` (charge `maelia-database.json` si table vide), CRUD admin complet (`CatalogAdminService`, provenance `origin=USER`). Liaisons entre fichiers : `FieldSpec.referencesDataSpec` (référence par identifiant, ex. `ID_ZH`) + `DataSpec.dependsOn` (dépendances implicites « par construction », V16, stockées `\|`-séparées) |
| `project` | Projets et configuration de modélisation | `Project`, `ModelingConfiguration` (JSONB), calcul de complétude (`CompletionEntry`) croisant DataSpecs applicables × datasets existants |
| `dataset` | Saisie / import / validation / matérialisation des données | `Dataset` + `DatasetRecord` (JSONB), `ValidationEngine` (niveau 1 présence, niveau 2 typage + allowedValues), `CsvImportService` + `CsvOrientationCodec` (lecture/écriture bi-orientation, délimiteur `;`), `ShpUploadService` (C8 : zip .shp/.shx/.dbf → renommage sur `DataSpec.fileName` → MinIO + `dataset_file`, dataset VALIDE), `BulkImportService` (initialisation en masse depuis un ZIP : appariement par nom de fichier, CSV importés + validés, shapefiles groupés par basename ; aucune entrée ne fait échouer l'ensemble), `IncludesMaterializer` (copie le socle `gama.base-includes` = `maelia/includes/`, écrase avec les CSV des datasets VALIDE puis avec les SHP uploadés relus depuis MinIO → `gama-workspace/maelia/projects/{id}/includes/`) |
| `paramcatalog` | Catalogue des paramètres de simulation | `ParameterSpec` (142, typés BOOLEAN/INTEGER/FLOAT/ENUM/STRING/STRING_LIST, `allowedValues`, `visibleIf`, `systemManaged`) / `ParameterGroup` (16), `ParameterSpecSeeder` idempotent, CRUD admin |
| `scenario` | Scénarios de simulation | `Scenario.parameterValues` (JSONB) = **uniquement les écarts aux défauts du launcher**, validés contre le catalogue (clés connues + ENUM) ; `GamaParameterBuilder` générique |
| `simulation` | Orchestration des runs | `SimulationRun` / `RunStatus`, `LaunchRunService`, adaptateur GAMA (`GamaServerGateway` / `GamaServerSession` / `GamaWebSocketHandler` / `GamaMessage` sealed + parser), `RunWorker` (@RabbitListener, profil worker), `RunUpdateRelay` (profil api), `RunController` + `DevRunController` |
| `result` | Ingestion & restitution des sorties | `OutputArtifact` / `ResultValue`, `OutputIngestionService` (scan best-effort du répertoire de sortie), `CsvSeriesParser` (streaming, plafond 50 000 valeurs/fichier, repli ISO-8859-1), `ArtifactStorage`, `ResultController` (séries + agrégation annuelle + artefacts) |
| `preprocessing` | Module de prétraitement : gestion des dépendances entre fichiers d'entrée | Les fichiers `generation=AUTO` ne sont **pas produits par MAELIA** mais par un module de prétraitement amont (cf. `data/DEPENDANCES_FICHIERS.md`). `DependencyGraphBuilder` (domaine pur : graphe construit depuis `referencesDataSpec` [EXPLICIT] + `dependsOn` [IMPLICIT], niveaux topologiques, détection de cycles, références inconnues remontées), `PreprocessingService` (graphe global + plan de génération par projet : fichiers applicables × datasets présents → statut DONE/READY/BLOCKED), `PreprocessingController`. La génération effective des fichiers (SIG) sera branchée ultérieurement sur ce plan |

Le contexte `iam` (authentification, rôles ADMIN / MODELISATEUR / OBSERVATEUR) est prévu mais **non implémenté** (M9, en dernier).

### 4.2 Profils Spring (`application.yml` multi-documents)

| Profil | Usage |
|---|---|
| *(base)* | Datasource Postgres, Flyway, RabbitMQ, MinIO, config `gama.*` et `maelia.*`, multipart 200 MB, logs avec MDC `run=/proj=`, Actuator (health, metrics, prometheus) |
| `api` | Serveur web 8080, REST + STOMP ; ne consomme **pas** la file des runs, mais consomme la file fanout des mises à jour pour les relayer en STOMP |
| `worker` | Pas de serveur web ; listener AMQP avec `concurrency: 2` / `max-concurrency: 4` / `prefetch: 1` (2 consommateurs pour qu'un run bloqué n'empêche pas d'en lancer un autre — 1 run = 1 connexion WebSocket GAMA persistante) |
| `dev` | Logs SQL verbeux |
| `test` | Désactive DataSource/JPA/Flyway/AMQP → les tests unitaires purs passent sans infra (conséquence : `contextLoads` échoue sans DB, comportement connu et assumé) |

Configuration clé (`gama.*` / `maelia.*`) :
- `gama.ws-url` (déf. `ws://localhost:6868`), `gama.workspace` (volume partagé), `gama.transfer-mode=SHARED_VOLUME`, `gama.command-timeout-seconds=120`, `gama.run-timeout-seconds=1800` (filet de sécurité d'un run complet), `gama.workspace-mount=/workspace` (préfixe des chemins envoyés à GAMA, tels que vus dans le conteneur gama-headless).
- `maelia.messaging.*` : échange `maelia.runs` / file `maelia.runs.queue` / clé `run.launch` ; fanout `maelia.run.updates`.
- `maelia.simulation.*` : modèle par défaut `maelia/models/main/launcherBase.gaml` (expérience `simulationBase`), modèle de test autonome `test/models/simple_test.gaml` (`test_simulation`, until `sim_termine`), launcher de test MAELIA réel `launcherTest.gaml` (`test_maelia`, until `simulationTerminee`).
- `maelia.catalog.seed-location` : seed du catalogue de données.

## 5. Flux d'exécution d'un run (séquence complète)

```
Front (POST /api/v1/projects/{id}/runs?scenarioId=…)
  → API : LaunchRunService.launchForProject
      1. vérifie projet + scénario
      2. IncludesMaterializer.materialize(projectId)   → gama-workspace/maelia/projects/{id}/includes/
         (copie du socle maelia/includes/ = jeu complet de fichiers, puis écrasement par les
          datasets VALIDE du projet ; BLOQUANT : échec → 409 RFC 7807, le run n'est pas mis en file)
      3. SimulationRun.createForProject → save (statut initial)
      4. GamaParameterBuilder.build(scenario, projectId, runId)
      5. publie RunLaunchMessage sur maelia.runs (routing run.launch)
  → Worker : RunWorker.consume (@RabbitListener, MDC runId/projectId, métriques maelia.runs)
      6. ouvre GamaSession (connexion WebSocket persistante vers gama-headless — si le socket
         ferme, GAMA détruit la simulation : la session reste ouverte tout le run)
      7. session.load(modelPath, experiment, until, parameters) → exp_id  → markStarted
      8. session.play(exp_id) puis waitForEnd(run-timeout 1800 s)
         — les messages GAMA (StatusInform / SimulationOutput / SimulationEnded / SimulationError)
           sont republiés sur l'échange fanout maelia.run.updates
         — le cycle courant est extrait des lignes console « cycle N » (best-effort)
         — en cas d'erreur/timeout : session.stop(exp_id) puis markFailed
      9. evaluate(exp_id, "cycle") → markFinished(finalCycle)
     10. OutputIngestionService.ingest(runId, projectId)  (best-effort, n'échoue jamais le run)
  → API : RunUpdateRelay (file anonyme liée au fanout) → STOMP /topic/runs/{runId}
  → Front : barre de progression, logs, statut TERMINE / ECHEC
```

**Paramètres envoyés à GAMA** (`GamaParameterBuilder`, générique) : les `parameterValues` non nuls du scénario (écarts aux défauts du launcher), puis les paramètres **pilotés par le système** qui priment : `executerSurCluster=false`, `cheminRacineMaelia={mount}/maelia/`, `cheminModeleVersDonnees={mount}/maelia/projects/{id}/includes/`, `idSimulationAPI={runId}`, `nomSimulation={runId[0..8]}`. Chaque entrée est sérialisée en `{type:"parameter", name, value}`.

**Protocole GAMA server** (JSON sur WebSocket) : `load {model, experiment, until, parameters, status:true, console:true, runtime:true}` → réponse portant l'`exp_id` ; `play {exp_id, sync:false}` ; `stop {exp_id}` ; `expression {exp_id, expr}` ; fin détectée par le message `SimulationEnded` (le launcher expose une expérience `until: simulationTerminee`).

## 6. API REST exposée (préfixe `/api/v1`)

| Contexte | Endpoints |
|---|---|
| Santé | `GET /health` (+ `/actuator/health`, `/actuator/prometheus`) |
| Catalogue données | `GET /dataspecs`, `GET /dataspecs/{id}`, `POST /dataspecs/applicable` ; admin : `POST/PUT/DELETE /admin/dataspecs[/{id}]` (`?force`), `POST /admin/dataspecs/{id}/duplicate`, `GET …/usage`, `POST/PUT/DELETE …/fields[/{fieldId}]`, `PUT …/fields:reorder` |
| Projets | `GET/POST /projects`, `GET/DELETE /projects/{id}`, `PUT /projects/{id}` (nom + description, page Initialisation), `PUT /projects/{id}/modeling-configuration`, `GET /projects/{id}/completion` |
| Datasets | `GET /projects/{pid}/datasets`, `POST /projects/{pid}/datasets/{dataSpecId}` (créer/ouvrir), `GET /datasets/{id}`, `PUT /datasets/{id}/records`, `POST /projects/{pid}/datasets/{dataSpecId}/import` (CSV multipart), `POST …/{dataSpecId}/shp` (upload zip shapefile, C8), `GET …/{dataSpecId}/files` (fichiers uploadés), `POST /datasets/{id}/validate`, `POST /projects/{pid}/materialize`, `POST /projects/{pid}/datasets/import-zip` (initialisation en masse : ZIP de CSV + shapefiles appariés au catalogue par nom de fichier, rapport par entrée VALIDE/INVALIDE/IGNORE/ERREUR) |
| Paramètres scénario | `GET /scenario-parameters`, `GET /scenario-parameters/groups` ; admin : `PUT/DELETE /admin/scenario-parameters/{gamlName}` |
| Scénarios | CRUD `GET/POST /projects/{pid}/scenarios`, `GET/PUT/DELETE …/{id}` |
| Runs | `POST /projects/{pid}/runs`, `GET /projects/{pid}/runs`, `GET /runs/{id}` ; dev : `POST /dev/runs` (modèle arbitraire), `POST /dev/runs/test` (simple_test autonome), `POST /dev/runs/maelia-test` (launcherTest MAELIA réel), `GET /dev/runs/{id}` |
| Prétraitement | `GET /preprocessing/dependency-graph` (graphe global : nœuds avec niveau topologique, arêtes EXPLICIT/IMPLICIT, cycles, références inconnues), `GET /projects/{pid}/preprocessing/plan` (plan ordonné : dépendances, manquantes, statut DONE/READY/BLOCKED) |
| Résultats | `GET /runs/{runId}/results` (séries journalières + agrégat annuel + artefacts), `GET /runs/{runId}/artifacts/{artifactId}` (octets), `POST /runs/{runId}/ingest` (ré-ingestion) |
| Temps réel | STOMP sur `/ws`, abonnement `/topic/runs/{runId}` (messages `{runId, type: EN_COURS|PROGRESS|LOG|ENDED|ERROR|TERMINE|ECHEC, cycle, message, error}`) |

Erreurs : format RFC 7807 (`GlobalExceptionHandler`).

## 7. Persistance (Flyway V1 → V16)

| Migration | Contenu |
|---|---|
| V1 | Extension PostGIS + socle |
| V2 | `simulation_run` |
| V3–V5 | `data_spec`, `field_spec`, ajustements FK |
| V4 | `project` (config de modélisation JSONB) |
| V6 | `dataset`, `dataset_record` (JSONB), `validation_issue` |
| V7–V9 | `scenario` + rattachement `project_id`/`scenario_id` sur `simulation_run` |
| V10 | `output_artifact`, `result_value` |
| V11 | `parameter_group`, `parameter_spec` |
| V12 | `scenario.parameter_values` (JSONB) — migration des anciens champs fixes vers la map |
| V13 | Orientation du catalogue (`orientation`, `matrix_value_start_index`, `delimiter`) + colonnes CRUD (`origin`) |
| V14 | Interactions `parameter_spec` |
| V15 | `dataset_file` (métadonnées des shapefiles uploadés — octets dans MinIO, C8) |
| V16 | `data_spec.depends_on` (dépendances implicites « par construction », `\|`-séparées) + remplissage des dépendances connues pour les bases déjà seedées |

Conventions : JPA uniquement dans `infrastructure/persistence` (entités `*JpaEntity` + adaptateurs `*RepositoryAdapter` implémentant les ports du domaine) ; JSONB via hypersistence-utils ; `ddl-auto: none` (Flyway fait foi).

## 8. gama-workspace/ (volume partagé)

```
gama-workspace/
├── maelia/                          # Modèle MAELIA complet (GAML, INRAE, GPL v3)
│   ├── models/
│   │   ├── main/                    # launcherBase.gaml (référentiel des ~200 paramètres),
│   │   │                            # launcherSasseme.gaml, launcherTest.gaml (headless, 1 an,
│   │   │                            # until: simulationTerminee, sans bloc output), main.gaml
│   │   ├── modeleAgricole/          # agriculteurs, cultures (AqYield, HerbSim), ITKs, îlots…
│   │   ├── modeleHydrographique/    # hydrologie
│   │   ├── modeleCommun/            # socle commun (contourZoneMaelia…)
│   │   ├── modeleNormatif/          # normes / restrictions d'eau
│   │   ├── output/ & processus/     # sorties et processus
│   ├── includes/                    # SOCLE : jeu complet des fichiers d'entrée (échantillon SASSEME)
│   │                                # copié au début de chaque matérialisation (gama.base-includes)
│   ├── projects/{projectId}/includes/  # Includes par projet = socle + fichiers saisis/modifiés (générés)
│   └── images/                      # palettes de couleurs pour les displays GUI
└── test/models/simple_test.gaml     # Modèle de test autonome (chaîne front↔back↔GAMA sans données)
```

Monté dans `gama-headless`, `api` **et** `worker` sous `/workspace` (WORKDIR de l'image GAMA — le launcher y crée aussi son workspace Eclipse `.workspace*`, qui apparaît donc dans `gama-workspace/` sur l'hôte). Les chemins de modèle passés à GAMA sont absolus dans le conteneur (ex. `/workspace/maelia/models/main/launcherBase.gaml`).

**Sorties MAELIA (découverte importante)** : le modèle écrit ses sorties sous `gama-workspace/maelia/models/main/log/<territoire>_<nomSimulation>_<horodatage>/<idSimulationAPI>/` — CSV délimités `;` avec un préambule `detailSimulation` (pas de XML). L'ingestion (`ArtifactStorage.findRunOutputDir`) scanne `main/log/*/{runId}/` avec repli sur l'ancien chemin `projects/{pid}/outputs/{runId}` ; le saut du préambule dans `CsvSeriesParser` reste à fiabiliser sur des sorties réelles.

## 9. Tests & observabilité

- **Tests unitaires purs** (sans Spring ni infra) : `CsvSeriesParserTest`, `CsvOrientationCodecTest`, `ValidationEngineNaTest`, `RequiredIfEvaluatorTest`, `GamaParameterBuilderTest`, `ScenarioParametersSeedTest` (intégrité du seed). `MaeliaApplicationTests.contextLoads` échoue sans base Postgres (profil `test` sans DataSource) — connu, environnemental.
- **Observabilité** : MDC `runId`/`projectId` dans chaque ligne de log du worker ; métriques Micrometer `maelia.runs{event=received|finished|failed}`, timer `maelia.run.duration`, compteur `maelia.result.values.ingested` ; exposition Prometheus via Actuator.
- Manquent encore : tests d'intégration Testcontainers (adaptateurs JPA/AMQP), e2e Playwright.

## 10. État d'avancement & points ouverts

Jalons M0→M8 : **code complet** (tranche verticale front→back→GAMA, catalogue de données + CRUD, saisie/validation/matérialisation, scénario piloté par le catalogue de paramètres, ingestion/restitution des résultats). Restent à faire, dans l'ordre :

1. **GATE GAMA bout-en-bout** : via `docker compose up`, créer un scénario, lancer un run réel MAELIA, vérifier la progression STOMP et l'ingestion des sorties (chemin `main/log/*/{runId}` + préambule CSV). La faisabilité headless a été prouvée par un prototype Python (`maelia-headless/`), pas encore par la chaîne complète.
2. **Durcissement (M6)** : Testcontainers, Playwright, annulation de run, cartes choroplèthes (MapLibre + géométries BVe). L'upload des SHP (C8) est fait ; reste l'édition cartographique (reportée).
3. **M9 — Authentification (EN DERNIER)** : contexte `iam`, Spring Security, rôles, cloisonnement des projets.

Risques connus : format réel des sorties GAMA à confirmer (ingestion best-effort) ; ~50 DataSpecs du seed avec `fieldsStatus: PENDING` (colonnes à compléter) ; couches normatives Garonne-Amont parfois incomplètes dans la doc source.

## 11. Conventions de travail

- Ne jamais coder en dur la logique d'un type de donnée ou d'un paramètre MAELIA : tout passe par `DataSpec` / `ParameterSpec`.
- Domaine pur sans framework ; dépendances vers l'intérieur ; communication inter-contextes par ports.
- Toute décision d'architecture nouvelle est répercutée dans ce document.
- Documents complémentaires conservés : `architecture-frontend.md` (FSD React), `ui-ux-specifications.md` (design system).
