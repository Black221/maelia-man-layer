# Spécifications UI/UX — Plateforme MAELIA

> Document de design destiné à Claude Code. Définit la direction visuelle (épurée, moderne, userfriendly), le design system (tokens, composants), les spécifications des écrans clés, les patterns d'interaction, et les règles de segmentation/architecture du code de présentation. À lire avec `architecture-frontend.md` (architecture applicative React) et `catalogue-donnees-entree-maelia.md` (logique des données).

## 1. Principes de design

L'application sert des utilisateurs experts (modélisateurs, gestionnaires de l'eau, agronomes) face à une matière intrinsèquement complexe (près de 70 types de données, simulations longues, sorties multidimensionnelles). Le rôle du design est de **rendre cette complexité maîtrisable**, pas de la masquer. Cinq principes :

1. **Clarté avant densité.** Beaucoup d'espace blanc, une hiérarchie typographique nette, une information à la fois. On ne remplit pas l'écran parce qu'on peut.
2. **Guider, ne pas submerger.** L'utilisateur sait toujours où il en est (tableau de complétude, fil d'Ariane, statuts explicites) et quelle est la prochaine action utile.
3. **La saisie d'abord.** L'édition de données est l'activité centrale : elle doit être aussi fluide qu'un tableur, avec validation immédiate et assistance contextuelle.
4. **Cohérence systémique.** Un seul jeu de composants, de couleurs et d'espacements. Tout écran semble appartenir à la même application.
5. **Calme visuel.** Palette sobre et naturelle (l'eau, l'agriculture, le territoire), couleur d'accent réservée aux actions et aux statuts. Pas de surcharge décorative.

## 2. Design tokens

Implémentables directement dans `tailwind.config.ts` (clé `theme.extend`) et exposés en variables CSS pour le thème clair/sombre.

### 2.1 Couleurs

**Neutres (base de l'interface)** — échelle slate :
```
--neutral-50  #F8FAFC   --neutral-400 #94A3B8
--neutral-100 #F1F5F9   --neutral-500 #64748B
--neutral-200 #E2E8F0   --neutral-600 #475569
--neutral-300 #CBD5E1   --neutral-700 #334155
                        --neutral-800 #1E293B
                        --neutral-900 #0F172A
```

**Primaire (eau / accent d'action)** — teal :
```
--primary-50  #ECFEFF   --primary-500 #0E9CA8
--primary-100 #CFFAFE   --primary-600 #0E7C86  (couleur d'action par défaut)
--primary-200 #A5F0F5   --primary-700 #115E67
--primary-300 #67E0E8   --primary-800 #134E55
--primary-400 #2DC4D1   --primary-900 #134048
```

**Sémantiques** :
```
success #16A34A   warning #D97706   danger #DC2626   info #2563EB
```

**Statuts métier (réutilisent la sémantique MAELIA des organigrammes)** :
```
Source MANUEL (saisie)      → amber  #D97706   (écho des cases orange)
Source AUTO (généré/SHP)    → green  #16A34A   (écho des cases vertes)
Dataset VIDE                → neutral-400
Dataset BROUILLON           → warning
Dataset VALIDE              → success
Dataset INVALIDE            → danger
Run EN_FILE / EN_COURS      → primary (animé)
Run TERMINE                 → success
Run ECHEC                   → danger
```

Mode sombre : inverser l'échelle neutre (fond `neutral-900`, surfaces `neutral-800`), conserver primaire et sémantiques avec luminosité ajustée.

### 2.2 Typographie

Police : **Inter** (ou Geist), system-ui en repli. Une seule famille, graisses 400/500/600/700.

```
Display   28px / 36  600   titres de page
H1        22px / 30  600
H2        18px / 26  600   titres de section
H3        16px / 24  600
Body      14px / 22  400   texte courant
Body-sm   13px / 20  400   tableaux denses, métadonnées
Caption   12px / 16  500   labels, badges, unités
Mono      13px       —     valeurs numériques, logs (JetBrains Mono)
```

Les valeurs numériques (débits, surfaces, coordonnées) en chiffres tabulaires (`font-variant-numeric: tabular-nums`) pour l'alignement en grille.

### 2.3 Espacement, rayons, ombres

Échelle d'espacement base 4px : `2, 4, 8, 12, 16, 24, 32, 48, 64`. Gouttières de page 24–32px, respiration interne des cartes 16–24px.

Rayons : `sm 6px` (champs, badges), `md 10px` (cartes, boutons), `lg 16px` (modales, panneaux), `full` (pastilles de statut).

Ombres (subtiles, jamais dures) :
```
shadow-sm   0 1px 2px rgba(15,23,42,.06)
shadow-md   0 4px 12px rgba(15,23,42,.08)
shadow-lg   0 12px 32px rgba(15,23,42,.12)   (modales, popovers)
```

Bordures : `1px solid neutral-200` (clair). Préférer bordure + ombre légère aux fonds colorés pour délimiter.

### 2.4 Iconographie & mouvement

Icônes **lucide-react**, trait 1.5px, taille 16–20px. Transitions courtes (150–200ms, `ease-out`) sur hover/focus/ouverture ; pas d'animation gratuite. Respecter `prefers-reduced-motion`.

## 3. Layout & shell applicatif

Structure persistante en trois zones :

- **Barre latérale gauche (240px, repliable en 64px)** — navigation principale. En contexte projet, elle liste les **modules MAELIA** (Commun, Agricole, Hydrographique, Normatif, Autres usages) avec, pour chacun, une pastille de complétude. Plus les entrées Configuration, Scénarios, Résultats.
- **En-tête (56px)** — fil d'Ariane (Projet › Module › Donnée), sélecteur de projet courant, état de validation global, menu utilisateur. Pas de couleur de fond saturée : `neutral-50` + bordure basse.
- **Zone de contenu** — fond `neutral-50`, contenu sur surfaces blanches `shadow-sm`. Largeur de lecture maîtrisée pour les écrans de formulaire ; pleine largeur pour grilles et cartes.

Grille responsive : confortable ≥1280px (cible métier desktop), utilisable jusqu'à 1024px ; en deçà, la barre latérale passe en drawer.

## 4. Spécifications des écrans clés

### 4.1 Liste des projets
Cartes ou tableau listant les projets (nom, cas d'application, statut, date, complétude globale en barre). Action primaire « Nouveau projet » en haut à droite. Création en modale : nom, description, cas d'application (Garonne-Amont par défaut). Empty state illustré si aucun projet.

### 4.2 Espace projet — tableau de complétude (écran central)
C'est le cockpit. Pour le projet courant et **sa configuration de modélisation**, il affiche les types de données **réellement attendus**, groupés par module. Chaque ligne : nom de la donnée, badge source (amber = saisie / green = généré), badge statut (vide/brouillon/valide/invalide), et un raccourci d'édition. En tête de chaque module, une jauge de progression (`x/y validés`). Un bandeau de synthèse en haut indique si le projet est « prêt à simuler » et, sinon, ce qui manque. Les données `Conditionnel` n'apparaissent que si l'option qui les déclenche est active. Filtre rapide : tout / requis manquants / à corriger.

### 4.3 Configuration de modélisation
Formulaire clair en sections : méthode de choix d'assolement, mode d'irrigation, modèle de culture, activation des modules (barrage, agricole, autres usages), méthode de restriction, scénario de prix. Chaque option affiche une aide contextuelle expliquant son impact sur les données requises. À la validation, le tableau de complétude se recompose. Visualisation immédiate de l'effet (« +3 données requises, -1 »).

### 4.4 Éditeur de donnée — grille (saisie facile, priorité absolue)
Pour un `saisieMode = GRID`. Plein écran utile, grille type tableur (AG Grid) : une colonne par champ du schéma, en-tête montrant le libellé + l'unité (ex. « Surface (ha) »), type d'éditeur adapté (nombre, texte, sélecteur pour `allowedValues`, sélecteur référentiel pour les clés étrangères). Fonctions attendues : saisie au clavier fluide, copier-coller depuis Excel, ajout/duplication/suppression de ligne, **validation par cellule en temps réel** (cellule en erreur bordée de rouge + infobulle du message), compteur d'erreurs, barre d'action (Valider, Enregistrer, Importer un fichier, Exporter). Panneau latéral repliable décrivant la donnée (description, chemin includes, conditions). Auto-sauvegarde en brouillon. Bandeau de rappel des dépendances (« nécessite que le sol soit saisi »).

### 4.5 Éditeur de donnée — carte
Pour un `saisieMode = MAP`. Carte MapLibre plein cadre, couche des entités du dataset, fond cartographique sobre (clair, peu contrasté). Sélection d'une entité → panneau d'attributs à droite (formulaire généré par le schéma). Outils : dessin de nouvelle entité, édition de géométrie, suppression. Légende et sélecteur de couches de référence (BVe, communes) pour le contexte. Validation topologique et référentielle signalée sur la carte (entité fautive surlignée).

### 4.6 Upload + mapping de colonnes
Zone de dépôt (drag & drop) acceptant CSV ou SHP zippé. Après dépôt : aperçu des premières lignes, **mapping assisté** colonne du fichier → champ du schéma (auto-détecté par nom, ajustable), puis rapport de validation détaillé (erreurs par ligne/colonne). Bouton « Importer » actif seulement si la validation passe (ou import avec avertissements explicitement acceptés). Le résultat devient un dataset éditable comme une saisie.

### 4.7 Constructeur de scénario
Formulaire : nom, scénario climatique (observé/simulé + horizon), scénario de prix, période de simulation (dates), graine. Encart de pré-vol : checklist des données requises validées ; bouton « Lancer la simulation » désactivé tant que des données manquent, avec lien direct vers ce qui manque.

### 4.8 Moniteur de run
État du run en grand (badge animé), **barre de progression** (pas courant / pas final) alimentée en temps réel, durée écoulée, journal déroulant (messages `write` de la simulation) en zone mono repliable, bouton Annuler. À la fin, bouton proéminent « Voir les résultats ». En cas d'échec, extrait d'erreur lisible + action « Relancer ».

### 4.9 Tableau de bord des résultats
Mise en page en sections par thème (Hydrologie, Agriculture, Normatif, Usages), chacune avec : sélecteur d'indicateur, **carte choroplèthe** (débits par BVe, restrictions par zone, rendements par parcelle) et **graphes de séries temporelles** (débits journaliers, niveaux de nappe, volumes de retenue), cartes d'indicateurs de synthèse (respect des DOE, marges, consommations AEP/IND). Galerie des snapshots PNG produits par GAMA. Sélecteur d'échelle temporelle (jour/an) et spatiale (BVe/territoire). Export (PNG, CSV, données brutes). Comparaison de scénarios en option (deux runs côte à côte).

## 5. Patterns d'interaction

- **États systématiques** : chaque zone de données gère chargement (squelettes, pas de spinner plein écran), vide (message + action), erreur (encart + réessayer), succès.
- **Feedback** : toasts non bloquants pour les succès/erreurs d'action ; validations inline pour les formulaires ; jamais d'alerte modale pour une simple confirmation de sauvegarde.
- **Actions destructrices** : confirmation explicite (modale) pour suppression de projet/dataset/scénario.
- **Sauvegarde** : auto-save en brouillon pour la saisie ; action explicite pour « Valider ».
- **Navigation contextuelle** : depuis le tableau de complétude et le pré-vol de scénario, liens directs vers la donnée à corriger.
- **Densité réglable** : la grille de saisie propose un mode confortable/compact.

## 6. Accessibilité

Cible WCAG 2.1 AA. Contraste texte ≥ 4.5:1 (la couleur primaire teal-600 est choisie pour passer sur blanc). Navigation clavier complète (la grille et la carte incluses), focus visibles, composants Radix/shadcn pour les rôles ARIA. Tous les champs générés portent un `<label>` lié et l'unité en texte (pas seulement par couleur). Le statut n'est jamais véhiculé par la seule couleur : toujours doublé d'un libellé ou d'une icône. Respect de `prefers-reduced-motion` et `prefers-color-scheme`.

## 7. Architecture & segmentation du code de présentation

Le design system vit dans `shared/ui` (couche la plus basse de la Feature-Sliced Design décrite dans `architecture-frontend.md`). Il est organisé en niveaux de composition, sans logique métier :

```
shared/ui/
├── tokens/        → tailwind preset + variables CSS (couleurs, typo, espacements, ombres)
├── primitives/    → composants atomiques sans métier : Button, Input, Select, Checkbox,
│                    Badge, Tag, Tooltip, Dialog, Popover, Tabs, Toast, Skeleton, Spinner
├── data/          → composants de données réutilisables : DataGrid (wrapper AG Grid),
│                    MapView (wrapper MapLibre), Chart (wrapper Recharts), ProgressBar
├── layout/        → AppShell, Sidebar, Header, PageHeader, Section, Card, EmptyState, ErrorState
└── index.ts       → API publique du design system
```

Règles de segmentation du front (complètent celles de `architecture-frontend.md`) :

- **Trois niveaux de composants.** *Primitives* (shared/ui, purs, stylés par tokens) → *composants métier* (entities/features, assemblent des primitives + données) → *écrans* (pages/widgets, orchestrent). Un composant ne saute pas de niveau pour réimplémenter une primitive.
- **Aucun style en dur hors tokens.** Couleurs, espacements, rayons, ombres proviennent exclusivement du preset Tailwind dérivé des tokens §2. Pas de hex en dur dans les composants ; pas de valeurs magiques d'espacement.
- **Présentational vs container.** Les composants de `shared/ui` et d'`entities/*/ui` sont présentationnels (props in, JSX out, aucun appel réseau). La logique (data fetching, état) est dans les hooks `model/` des features. Le JSX ne contient ni `fetch` ni règle métier.
- **Theming centralisé.** Un seul `ThemeProvider` ; clair/sombre via variables CSS. Aucun composant ne lit `window.matchMedia` directement.
- **Composants génériques, pas spécifiques à un fichier MAELIA.** Le rendu d'un champ (`FieldRenderer`) et d'une grille (`DatasetGrid`) est piloté par le `DataSpec` ; on n'écrit jamais un composant `EspecesCultiveesForm`. Cette généricité est une exigence d'architecture, pas une option.
- **Accessibilité par construction.** Les primitives encapsulent Radix (focus, ARIA) ; les features n'ont pas à re-gérer l'accessibilité de base.
- **Nommage & taille.** PascalCase, un composant par fichier, ≤ ~200 lignes ; au-delà, extraire des sous-composants ou des hooks. Les variantes via une API de props claire (ex. `<Button variant="primary|ghost|danger" size="sm|md">`), pas par duplication.
- **Tests visuels & d'interaction.** Stories (ou tests Testing Library) pour les primitives ; tests d'interaction pour la grille de saisie et la carte ; e2e Playwright sur le parcours saisie → run → résultats.

## 8. Synthèse de la direction visuelle

Une application sobre et professionnelle : fond clair, surfaces blanches délimitées par des bordures fines et des ombres légères, une seule couleur d'accent teal évoquant l'eau réservée aux actions et aux statuts, une typographie Inter nette avec chiffres tabulaires pour les données, beaucoup d'espace. La complexité de MAELIA est domptée par un cockpit de complétude qui guide pas à pas, une saisie aussi fluide qu'un tableur, et une restitution des résultats lisible mêlant cartes et graphes. Rien de décoratif : chaque élément sert la compréhension ou l'action.
