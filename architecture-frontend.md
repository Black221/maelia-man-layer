# Architecture Frontend — Plateforme MAELIA (React, Clean Architecture)

> Document d'implémentation destiné à Claude Code. Décrit l'architecture cible du frontend React/TypeScript : organisation en couches et par features (Feature-Sliced Design), moteur de formulaires piloté par schéma, client temps réel pour la progression des simulations, et règles de segmentation du code. À lire avec `architecture-backend.md` (contrats d'API) et `ui-ux-specifications.md` (design system et specs d'écrans).

## 1. Objectif et principes

L'application web permet de gérer des projets MAELIA : saisie assistée et upload des données d'entrée, configuration de modélisation, lancement et suivi des simulations, restitution cartographique et graphique des résultats. La priorité produit est la **saisie facile** ; l'upload est un cas d'usage de premier rang.

Principes d'architecture :

1. **Séparation stricte UI / logique / accès données.** Les composants de présentation ne parlent jamais directement à `fetch`/`axios` : ils passent par une couche `api` + des hooks de données. La logique métier d'un écran vit dans des hooks, pas dans le JSX.
2. **Organisation par feature, pas par type technique.** Le code est découpé par domaine fonctionnel (`projects`, `datasets`, `catalog`, `scenarios`, `runs`, `results`), chaque feature étant autonome. On évite les dossiers géants `components/`, `hooks/`, `utils/` transverses.
3. **Le schéma pilote l'UI.** Les formulaires de saisie et les validateurs sont générés à partir du `DataSpec` servi par le backend. Aucun formulaire MAELIA n'est codé champ par champ en dur.
4. **État serveur ≠ état client.** L'état serveur (données distantes, cache, invalidation) est géré par TanStack Query ; l'état d'interface local par Zustand. On ne stocke jamais des données serveur dans un store global.
5. **Typage de bout en bout.** TypeScript strict ; les types d'API sont dérivés du contrat OpenAPI du backend.

## 2. Stack technique

| Préoccupation | Choix | Notes |
|---|---|---|
| Langage | TypeScript (strict) | |
| Framework | React 18 | |
| Build / dev | Vite | |
| Routing | React Router v6 (data routers) | |
| État serveur | TanStack Query (React Query) v5 | cache, mutations, invalidation |
| État client | Zustand | UI state léger, pas de boilerplate |
| Formulaires | React Hook Form + Zod | validation client dérivée du schéma |
| Tableur (saisie) | AG Grid Community (ou TanStack Table + virtualisation) | édition en grille des CSV |
| Cartographie | MapLibre GL JS (+ react-map-gl) | couches SHP, édition d'entités |
| Édition géométrique | Mapbox Draw / Terra Draw | dessin de parcelles/points |
| Graphes | Recharts (ou Plotly pour les vues riches) | séries temporelles, indicateurs |
| Temps réel | STOMP over WebSocket (@stomp/stompjs) | progression des runs |
| UI kit | shadcn/ui (Radix + Tailwind) | composants accessibles, design épuré |
| Styles | Tailwind CSS + design tokens | voir `ui-ux-specifications.md` |
| Icônes | lucide-react | |
| HTTP | axios (instance unique) ou fetch wrapper | intercepteurs auth/erreurs |
| i18n | react-i18next | FR par défaut |
| Tests | Vitest + Testing Library + MSW + Playwright | unitaires, intégration, e2e |
| Qualité | ESLint + Prettier + TypeScript strict | |

## 3. Architecture en couches (Feature-Sliced Design)

On adopte une variante de **Feature-Sliced Design (FSD)**, qui impose une hiérarchie de couches avec un sens de dépendance unique (une couche n'importe que des couches situées en dessous d'elle).

```
src/
├── app/        → bootstrap : providers, router, thème, configuration globale
├── pages/      → assemblage d'écrans (une route = une page), orchestration de features
├── widgets/    → blocs d'UI composites réutilisables (ex. CompletionDashboard, RunMonitorPanel)
├── features/   → unités fonctionnelles avec interaction utilisateur (ex. dataset-grid-edit, launch-run)
├── entities/   → modèles métier et leur UI de base (project, dataset, dataSpec, scenario, run, result)
├── shared/     → socle réutilisable sans logique métier (ui kit, api client, lib, config, types)
```

Règle de dépendance FSD : `app → pages → widgets → features → entities → shared`. Un module n'importe **jamais** depuis une couche au-dessus de lui, ni depuis une autre tranche de même niveau (deux features ne s'importent pas mutuellement ; elles communiquent via `entities` ou via les pages qui les composent).

Chaque tranche (slice) expose une **API publique** via un `index.ts` (barrel) : l'extérieur n'importe que ce que la tranche exporte, jamais ses fichiers internes.

### Structure interne d'une tranche

```
features/dataset-grid-edit/
├── ui/         → composants React de la feature
├── model/      → hooks, logique d'état, machines (use-cases côté front)
├── api/        → appels réseau propres à la feature (mutations, queries)
├── lib/        → helpers purs spécifiques
└── index.ts    → API publique de la feature
```

## 4. Cartographie features ↔ écrans

| Feature / widget | Rôle | Couche |
|---|---|---|
| `entities/project` | Modèle Project + cartes/résumés | entities |
| `entities/data-spec` | Modèle DataSpec/FieldSpec + rendu d'un champ | entities |
| `entities/dataset` | Modèle Dataset + statut | entities |
| `entities/run` | Modèle SimulationRun + badge de statut | entities |
| `features/project-create` | Création/duplication de projet | features |
| `features/modeling-config` | Édition de la config de modélisation | features |
| `features/dataset-grid-edit` | Saisie en grille (CSV) pilotée par schéma | features |
| `features/dataset-map-edit` | Édition cartographique (SHP) | features |
| `features/dataset-upload` | Upload + mapping de colonnes + validation | features |
| `features/launch-run` | Lancement d'une simulation | features |
| `features/run-progress` | Abonnement STOMP + progression | features |
| `widgets/completion-dashboard` | Tableau de complétude par module | widgets |
| `widgets/results-dashboard` | Cartes + graphes + galerie de sorties | widgets |
| `pages/*` | Projets, Espace projet, Éditeur de donnée, Scénario, Run, Résultats | pages |

## 5. Le moteur de formulaires piloté par schéma (pièce maîtresse)

Vit dans `entities/data-spec` (rendu d'un champ) + `features/dataset-grid-edit` et `features/dataset-map-edit` (rendu d'un dataset complet).

- **Source** : le backend sert le `DataSpec` (`GET /api/v1/dataspecs/{id}`) avec ses `FieldSpec` (label, infoType, unit, required, allowedValues, listSeparator, referencesDataSpec) et un `saisieMode` (`GRID` | `MAP` | `IMPORT`).
- **Génération du schéma de validation** : un mapper `fieldSpecToZod(field)` construit dynamiquement un schéma **Zod** (number borné + message d'unité, enum pour `allowedValues`, array pour multivalué, etc.). React Hook Form utilise ce schéma pour la validation à la volée.
- **Rendu** :
  - `saisieMode === 'GRID'` → composant `DatasetGrid` (AG Grid) : une colonne par `FieldSpec`, éditeur de cellule typé, validation par cellule, copier-coller depuis Excel, ajout/duplication de ligne. **C'est le cœur de la saisie facile.**
  - `saisieMode === 'MAP'` → composant `DatasetMap` (MapLibre) : couche des `SpatialFeature`, sélection → panneau d'attributs (formulaire généré), outils de dessin.
  - `saisieMode === 'IMPORT'` → vue lecture seule / import only.
- **Champs référentiels** : un `FieldSpec.referencesDataSpec` devient un `<Select>` alimenté par les valeurs du dataset référencé (query dédiée). Garantit l'intégrité et assiste la saisie.
- **Upload** : `features/dataset-upload` réutilise le même schéma pour valider le fichier importé et propose le mapping colonnes → `FieldSpec`.

Un seul moteur, piloté par les métadonnées, couvre les ~70 types de données. Ajouter un type côté backend ne demande aucune ligne de front.

## 6. Couche d'accès aux données

- **Client HTTP** dans `shared/api` : instance axios unique, `baseURL`, intercepteurs (injection du token, normalisation des erreurs RFC 7807 en objet `ApiError`, gestion 401).
- **Types d'API générés** depuis l'OpenAPI du backend (`openapi-typescript`) dans `shared/api/types` — source de vérité des contrats, jamais réécrits à la main.
- **Hooks de données par entité/feature** avec TanStack Query : `useProject(id)`, `useDataSpecs(config)`, `useDataset(id)`, `useUpsertRecords()`, `useLaunchRun()`, `useRunResults(id)`. Conventions : clés de query structurées (`['dataset', id]`), invalidation ciblée après mutation, `select` pour dériver, états `isLoading`/`isError` exposés à l'UI.
- **Aucun appel réseau dans le JSX** : les composants consomment des hooks.

## 7. Temps réel (progression des runs)

`features/run-progress` ouvre un abonnement **STOMP** sur `/topic/runs/{id}` via un client partagé (`shared/api/stomp`). Un hook `useRunProgress(runId)` expose `{ status, progress, logs }` et met à jour le cache TanStack Query du run (pas de polling). La connexion est ouverte à l'entrée sur le moniteur de run et fermée à la sortie. Reconnection automatique gérée par le client STOMP.

## 8. Routing et structure des pages

React Router v6 en data router. Arborescence cible :

```
/projects                         → liste des projets
/projects/:id                     → espace projet (tableau de complétude)
/projects/:id/config              → configuration de modélisation
/projects/:id/data/:specId        → éditeur de donnée (grille ou carte selon saisieMode)
/projects/:id/scenarios           → liste / création de scénarios
/scenarios/:id                    → constructeur de scénario + lancement
/runs/:id                         → moniteur de run (progression temps réel)
/runs/:id/results                 → tableau de bord des résultats
```

Chaque route correspond à une `page` qui compose des `widgets` et `features`. Les `loader`/`action` de React Router peuvent précharger via TanStack Query (`queryClient.ensureQueryData`).

## 9. Gestion d'état

- **Serveur** : TanStack Query, exclusivement pour les données distantes.
- **Client global** : Zustand, pour l'état d'UI transverse (thème, panneau latéral, projet courant, préférences). Stores petits et ciblés (`useUiStore`, `useEditorStore`), jamais un store monolithique.
- **Local** : `useState`/`useReducer` dans les composants pour l'état purement local.
- **Formulaire** : React Hook Form gère l'état du formulaire ; ne pas dupliquer dans Zustand.

## 10. Gestion des erreurs et états d'UI

Tout écran de données gère explicitement quatre états : **chargement** (squelettes, pas de spinners pleine page), **vide** (empty state avec action), **erreur** (message + retry, via Error Boundary par zone), **succès**. Les erreurs réseau normalisées (`ApiError`) sont affichées en toasts non bloquants ou en encarts contextuels. Une `ErrorBoundary` par page évite l'écran blanc global.

## 11. Conventions de code et de segmentation

- **Respect strict des couches FSD** : import descendant uniquement ; pas d'import entre tranches de même niveau. Un lint personnalisé (`eslint-plugin-boundaries` ou `@feature-sliced/eslint-config`) verrouille la règle.
- **API publique par tranche** : tout passe par `index.ts` ; pas d'import en profondeur (`features/x/ui/Internal`).
- **Composants** : présentational vs container. Les composants de présentation sont purs (props in, JSX out) ; la logique va dans des hooks `model/`.
- **Un composant = un fichier**, nommé en PascalCase ; un hook par fichier, préfixe `use`. Fichiers ≤ ~200 lignes ; au-delà, extraire.
- **Pas de logique réseau ni de logique métier dans le JSX.** Le JSX orchestre des hooks et compose des composants.
- **Styles** : Tailwind + composants shadcn/ui ; pas de CSS inline dispersé ; les tokens de design (couleurs, espacements) viennent du thème défini dans `ui-ux-specifications.md`.
- **Types** : pas de `any` ; types d'API générés ; types de domaine front dans `entities/*/model`.
- **Accessibilité** : composants Radix/shadcn (focus, ARIA) ; labels explicites sur tous les champs générés.
- **Tests** : unitaires sur les hooks `model/` et les mappers (Vitest) ; intégration des features avec MSW (mock du backend) ; e2e des parcours critiques (saisie → run → résultats) avec Playwright.

## 12. Structure de dossiers complète (cible)

```
src/
├── app/
│   ├── providers/        (QueryClient, Router, Theme, i18n, Auth)
│   ├── router.tsx
│   └── App.tsx
├── pages/
│   ├── projects/
│   ├── project-workspace/
│   ├── dataset-editor/
│   ├── scenario-builder/
│   ├── run-monitor/
│   └── results/
├── widgets/
│   ├── completion-dashboard/
│   ├── results-dashboard/
│   └── app-shell/        (header, sidebar, layout)
├── features/
│   ├── project-create/
│   ├── modeling-config/
│   ├── dataset-grid-edit/
│   ├── dataset-map-edit/
│   ├── dataset-upload/
│   ├── launch-run/
│   └── run-progress/
├── entities/
│   ├── project/
│   ├── data-spec/
│   ├── dataset/
│   ├── scenario/
│   ├── run/
│   └── result/
└── shared/
    ├── ui/               (design system : Button, Input, Select, Dialog, DataGrid, MapView…)
    ├── api/              (client http, stomp, types générés, query keys)
    ├── lib/              (helpers purs : zod-from-fieldspec, formatters, geo)
    ├── config/           (env, constantes)
    └── i18n/
```

## 13. Ordre d'implémentation conseillé pour Claude Code

1. `shared` : design system (shadcn/ui + tokens), client API, types générés, providers, app shell.
2. `entities` : modèles + UI de base (project, data-spec avec le rendu d'un champ, dataset, run).
3. `pages/projects` + `features/project-create` + `features/modeling-config` : créer un projet et le configurer.
4. `widgets/completion-dashboard` : tableau de complétude piloté par les `DataSpec` applicables.
5. `features/dataset-grid-edit` (priorité saisie facile) puis `dataset-upload` puis `dataset-map-edit`.
6. `features/launch-run` + `run-monitor` + `features/run-progress` (STOMP).
7. `widgets/results-dashboard` : cartes, graphes, galerie.
8. Tests e2e du parcours complet, accessibilité, polissage.
