# Installation & démarrage

La plateforme se déploie intégralement avec **Docker Compose**. Le fichier `docker-compose.yml`
décrit l'ensemble des services (base de données, messagerie, stockage objet, moteur GAMA,
backend et frontend).

## Prérequis

- **Docker** et **Docker Compose** (v2)
- Le modèle MAELIA (`.gaml` + `includes/`) placé dans `./gama-workspace` (monté dans les services `gama-headless`, `api` et `worker`)

## Démarrage

```bash
docker compose build && docker compose up -d
docker compose logs -f api worker gama-headless
```

## Services et ports

| Service | Image | Ports | Rôle |
|---|---|---|---|
| `db` | `postgis/postgis:16-3.4` | 5432 | Base `maelia` (user/pass `maelia`), volume `db-data` |
| `rabbitmq` | `rabbitmq:3.13-management` | 5672, 15672 | File des runs + console de gestion |
| `minio` | `minio/minio` | 9000, 9001 | Stockage objet (shapefiles uploadés) + console |
| `gama-headless` | `maelia/gama:local` (buildée depuis `gamaplatform/gama`) | 6868 | Serveur GAMA WebSocket |
| `api` | `maelia/backend:local` | 8080 | Profil Spring `api` : REST + STOMP |
| `worker` | même image que `api` | — | Profil Spring `worker` : consomme la file, pilote GAMA |
| `frontend` | `maelia/frontend:local` | 8081 | SPA React (nginx) |
| `docs` | `squidfunk/mkdocs-material` | 8082 | Cette documentation |

!!! info "api et worker partagent la même image"
    Seule la variable `SPRING_PROFILES_ACTIVE` change. Le worker n'a pas de serveur web
    (`web-application-type: none`).

## URLs utiles

| Ressource | URL |
|---|---|
| Frontend (SPA) | <http://localhost:8081> |
| API REST | <http://localhost:8080/api/v1> |
| Swagger UI | <http://localhost:8080/swagger-ui.html> |
| Console RabbitMQ | <http://localhost:15672> (`maelia` / `maelia`) |
| Console MinIO | <http://localhost:9001> (`maelia` / `maelia12345`) |
| GAMA (WebSocket) | `ws://localhost:6868` |
| Documentation | <http://localhost:8082> |

## Le volume `gama-workspace`

Le volume `./gama-workspace` est le **canal d'échange de fichiers** entre le worker et GAMA
(mode `SHARED_VOLUME`) :

- le worker y **matérialise les includes** (régénère les fichiers d'entrée à partir de la base) ;
- GAMA y **lit le modèle** et y **écrit ses sorties**.

!!! warning "Droits d'écriture"
    Le répertoire doit être accessible en **écriture** : GAMA y crée des workspaces Eclipse à la
    volée (`.workspace*`). Le même chemin `/workspace` est vu à l'identique par `api`, `worker` et
    `gama-headless`, ce qui garantit un référentiel de chemins unique partout.

## Servir la documentation

Le service `docs` monte ce dossier (`./docs`) dans `/docs` et lance `mkdocs serve`. En local,
sans Docker, on peut aussi la servir directement :

```bash
cd docs
docker run --rm -it -p 8082:8000 -v "${PWD}:/docs" squidfunk/mkdocs-material serve --dev-addr=0.0.0.0:8000
```
