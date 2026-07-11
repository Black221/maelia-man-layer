# Référence API REST

Tous les endpoints sont exposés sous le préfixe **`/api/v1`**. Les erreurs suivent le format
**RFC 7807** (`GlobalExceptionHandler`).

!!! info "Documentation interactive"
    Une **Swagger UI** est générée automatiquement (springdoc-openapi) :
    <http://localhost:8080/swagger-ui.html>.

## Santé

| Méthode | Endpoint | Description |
|---|---|---|
| `GET` | `/health` | Santé applicative (+ `/actuator/health`, `/actuator/prometheus`) |

## Catalogue de données

| Méthode | Endpoint | Description |
|---|---|---|
| `GET` | `/dataspecs` · `/dataspecs/{id}` | Liste / détail des types de données |
| `POST` | `/dataspecs/applicable` | Types applicables selon la config projet |
| `POST/PUT/DELETE` | `/admin/dataspecs[/{id}]` (`?force`) | CRUD admin (provenance `origin=USER`) |
| `POST` | `/admin/dataspecs/{id}/duplicate` | Duplication d'un type |
| `GET` | `/admin/dataspecs/{id}/usage` | Utilisation d'un type |
| `POST/PUT/DELETE` | `/admin/dataspecs/{id}/fields[/{fieldId}]` | CRUD des champs |
| `PUT` | `/admin/dataspecs/{id}/fields:reorder` | Réordonnancement des champs |

## Projets

| Méthode | Endpoint | Description |
|---|---|---|
| `GET/POST` | `/projects` | Liste / création |
| `GET/DELETE` | `/projects/{id}` | Détail / suppression |
| `PUT` | `/projects/{id}` | Nom + description |
| `PUT` | `/projects/{id}/modeling-configuration` | Configuration de modélisation |
| `GET` | `/projects/{id}/completion` | Tableau de complétude |

## Datasets

| Méthode | Endpoint | Description |
|---|---|---|
| `GET` | `/projects/{pid}/datasets` | Datasets du projet |
| `POST` | `/projects/{pid}/datasets/{dataSpecId}` | Créer / ouvrir un dataset |
| `GET` | `/datasets/{id}` | Détail |
| `PUT` | `/datasets/{id}/records` | Écriture des enregistrements |
| `POST` | `/projects/{pid}/datasets/{dataSpecId}/import` | Import CSV (multipart) |
| `POST` | `/projects/{pid}/datasets/{dataSpecId}/shp` | Upload zip shapefile |
| `GET` | `/projects/{pid}/datasets/{dataSpecId}/files` | Fichiers uploadés |
| `POST` | `/datasets/{id}/validate` | Validation |
| `POST` | `/projects/{pid}/materialize` | Matérialisation des includes |
| `POST` | `/projects/{pid}/datasets/import-zip` | Initialisation en masse (ZIP) |

## Paramètres de scénario

| Méthode | Endpoint | Description |
|---|---|---|
| `GET` | `/scenario-parameters` · `/scenario-parameters/groups` | Catalogue des paramètres / groupes |
| `PUT/DELETE` | `/admin/scenario-parameters/{gamlName}` | CRUD admin |

## Scénarios

| Méthode | Endpoint | Description |
|---|---|---|
| `GET/POST` | `/projects/{pid}/scenarios` | Liste / création |
| `GET/PUT/DELETE` | `/projects/{pid}/scenarios/{id}` | Détail / modification / suppression |

## Runs

| Méthode | Endpoint | Description |
|---|---|---|
| `POST` | `/projects/{pid}/runs` | Lancer un run |
| `GET` | `/projects/{pid}/runs` | Historique des runs du projet |
| `GET` | `/runs/{id}` | Détail d'un run |
| `POST` | `/dev/runs` · `/dev/runs/test` · `/dev/runs/maelia-test` | Runs de développement / test |
| `GET` | `/dev/runs/{id}` | Détail d'un run de dev |

## Prétraitement

| Méthode | Endpoint | Description |
|---|---|---|
| `GET` | `/preprocessing/dependency-graph` | Graphe global des dépendances (niveaux, cycles, références inconnues) |
| `GET` | `/projects/{pid}/preprocessing/plan` | Plan ordonné (statut DONE / READY / BLOCKED) |

## Résultats

| Méthode | Endpoint | Description |
|---|---|---|
| `GET` | `/runs/{runId}/results` | Séries journalières + agrégat annuel + artefacts |
| `GET` | `/runs/{runId}/artifacts/{artifactId}` | Octets d'un artefact |
| `POST` | `/runs/{runId}/ingest` | Ré-ingestion des sorties |

## Temps réel (STOMP)

Connexion STOMP sur **`/ws`**, abonnement à **`/topic/runs/{runId}`**.

```json
{
  "runId": "…",
  "type": "EN_COURS | PROGRESS | LOG | ENDED | ERROR | TERMINE | ECHEC",
  "cycle": 0,
  "message": "…",
  "error": null
}
```
