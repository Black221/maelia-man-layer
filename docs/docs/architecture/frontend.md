# Architecture frontend

Le frontend (`maelia-front/`) est une **SPA React 19 / TypeScript / Vite** organisée selon
**Feature-Sliced Design (FSD)**.

## Stack technique

| Préoccupation | Choix |
|---|---|
| Langage / framework | TypeScript strict · React 19 |
| Build / dev | Vite 8 (React Compiler) |
| Routing | React Router v7 (data router) |
| État serveur | TanStack Query v5 (cache, mutations, invalidation) |
| État client | Zustand |
| Temps réel | STOMP over WebSocket (`@stomp/stompjs`) |
| Styles | Tailwind CSS v4 + design tokens (`src/index.css`) |
| Icônes | lucide-react |
| HTTP | axios (instance unique) |

## Découpage en couches

FSD impose une hiérarchie de couches avec un sens de dépendance **unique** — une couche
n'importe que des couches situées en dessous d'elle :

```text
src/
├── app/        → bootstrap : providers, router, thème, configuration globale
├── pages/      → assemblage d'écrans (une route = une page)
├── widgets/    → blocs d'UI composites réutilisables
├── features/   → unités fonctionnelles avec interaction utilisateur
├── entities/   → modèles métier et leur UI de base (project, dataset, scenario, run, result)
└── shared/     → socle réutilisable sans logique métier (ui kit, api client, lib, config)
```

!!! note "Règle de dépendance FSD"
    `app → pages → widgets → features → entities → shared`. Un module n'importe **jamais** depuis
    une couche au-dessus de lui, ni depuis une autre tranche de même niveau. Chaque tranche expose
    une **API publique** via un `index.ts` (barrel) : l'extérieur n'importe que ce qui est exporté.

### Structure interne d'une tranche

```text
features/manage-dataset/
├── ui/         → composants React de la feature
├── model/      → hooks, logique d'état (use-cases côté front)
├── api/        → appels réseau propres à la feature (queries, mutations)
├── lib/        → helpers purs spécifiques
└── index.ts    → API publique de la feature
```

## Le moteur de formulaires piloté par schéma

C'est la **pièce maîtresse** du frontend. Un seul moteur, piloté par les métadonnées du
catalogue, couvre les ~70 types de données **et** les 142 paramètres de scénario.

- **Source** — le backend sert le `DataSpec` (avec ses `FieldSpec` : label, type, unité, `required`,
  `allowedValues`, `referencesDataSpec`) et un mode de saisie.
- **Rendu par mode** — grille éditable (saisie CSV, cœur de la saisie facile), carte (édition
  d'entités SHP), ou import (upload + mapping de colonnes).
- **Champs référentiels** — un `FieldSpec.referencesDataSpec` devient un `<Select>` alimenté par les
  valeurs du dataset référencé, garantissant l'intégrité.

!!! tip "Zéro code par type"
    Ajouter un type de donnée ou un paramètre côté backend ne demande **aucune ligne de front** :
    tout est dérivé du schéma servi par l'API.

## Couche d'accès aux données

- **Client HTTP** dans `shared/api` : instance axios unique, `baseURL`, intercepteurs (normalisation
  des erreurs RFC 7807, gestion des statuts).
- **Hooks par entité/feature** avec TanStack Query : clés de query structurées
  (`shared/api/queryKeys.ts`), invalidation ciblée après mutation, états `isLoading`/`isError`
  exposés à l'UI.
- **Aucun appel réseau dans le JSX** — les composants consomment des hooks.

## Temps réel

`features/run-progress` ouvre un abonnement **STOMP** sur `/topic/runs/{runId}`. Le hook
`useRunProgress(runId)` expose le statut, la progression et les logs, et met à jour le cache
TanStack Query du run (**pas de polling**). La connexion s'ouvre à l'entrée du moniteur de run et
se ferme à la sortie ; la reconnexion est automatique.

## Routing

Deux enveloppes de mise en page : `TopNavLayout` (barre supérieure seule) et `ProjectShell`
(barre + sidebar de l'espace projet).

| Route | Écran |
|---|---|
| `/projects` | Liste des projets |
| `/catalog` | Catalogue de données (gestion globale) |
| `/scenario-parameters` | Catalogue des paramètres de scénario |
| `/test` | Simulation de test (validation de la communication GAMA) |
| `/projects/:id/config` | Configuration de modélisation |
| `/projects/:id/data` · `/data/:datasetId` | Éditeur de donnée |
| `/projects/:id/preprocessing` | Plan de prétraitement |
| `/projects/:id/scenarios` · `/scenarios/new` · `/scenarios/:id/edit` | Scénarios |
| `/projects/:id/results` | Tableau de bord des résultats |
| `/projects/:id/runs/:runId` · `/runs/:id` | Moniteur de run (temps réel) |

## Design tokens

Les tokens de la charte graphique vivent dans `src/index.css` (`@theme` Tailwind v4) : palette
teal « eau » (`primary-600 #0E7C86`), neutres slate, typographie Inter + JetBrains Mono, rayons et
ombres. Cette documentation en reprend fidèlement les valeurs — voir la
[charte graphique](../reference/charte-graphique.md).
