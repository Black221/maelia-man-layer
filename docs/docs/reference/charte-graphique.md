# Charte graphique

Cette documentation reprend fidèlement le **Design System** du frontend, défini dans
`maelia-front/src/index.css` (`@theme` Tailwind v4). Direction visuelle : **claire, sobre,
« eau »** — un teal évoquant la ressource hydrique au cœur du modèle MAELIA, sur des neutres slate.

## Typographie

| Usage | Police |
|---|---|
| Texte | **Inter** (`--font-sans`) |
| Code / valeurs | **JetBrains Mono** (`--font-mono`) |

## Palette primaire — « eau / teal »

<div class="grid" markdown>

| Token | Hex |
|---|---|
| `primary-50` | `#ECFEFF` |
| `primary-100` | `#CFFAFE` |
| `primary-200` | `#A5F0F5` |
| `primary-300` | `#67E0E8` |
| `primary-400` | `#2DC4D1` |
| `primary-500` | `#0E9CA8` |
| `primary-600` | `#0E7C86` **(action)** |
| `primary-700` | `#115E67` **(hover)** |
| `primary-800` | `#134E55` |
| `primary-900` | `#134048` |

</div>

## Neutres — slate

| Token | Hex |
|---|---|
| `neutral-50` | `#F8FAFC` |
| `neutral-100` | `#F1F5F9` |
| `neutral-200` | `#E2E8F0` |
| `neutral-300` | `#CBD5E1` |
| `neutral-400` | `#94A3B8` |
| `neutral-500` | `#64748B` |
| `neutral-700` | `#334155` |
| `neutral-900` | `#0F172A` |

## Couleurs sémantiques

| Rôle | Hex | Exemple |
|---|---|---|
| Succès | `#16A34A` | <span class="maelia-badge maelia-badge--ok">TERMINE</span> |
| Avertissement | `#D97706` | <span class="maelia-badge maelia-badge--wip">EN COURS</span> |
| Danger | `#DC2626` | <span class="maelia-badge maelia-badge--todo">ECHEC</span> |
| Info | `#2563EB` | — |

## Rayons & ombres

| Token | Valeur |
|---|---|
| `radius-sm` | `6px` |
| `radius-md` | `10px` |
| `radius-lg` | `16px` |
| `shadow-sm` | `0 1px 2px rgba(15, 23, 42, 0.06)` |
| `shadow-md` | `0 4px 12px rgba(15, 23, 42, 0.08)` |
| `shadow-lg` | `0 12px 32px rgba(15, 23, 42, 0.12)` |

## Espacement

Base **4px** : `4 · 8 · 12 · 16 · 20 · 24 · 32 · 40 · 48 · 64`.

!!! tip "Source unique"
    Toute évolution de la charte doit se faire dans `maelia-front/src/index.css`, puis être
    répercutée ici (`docs/docs/stylesheets/extra.css`) pour garder documentation et application
    visuellement alignées.
