# Analyse — Documentation officielle MAELIA (PDF) vs `maelia-database.json`

> **Source de référence :** `documentationMAELIA_04032024.pdf` (98 p., « Documentation de la plateforme
> MAELIA », équipe MAELIA / UMR LAE / MAELAB, 2024-03-04). C'est la **documentation officielle**.
> **Objet comparé :** `data/maelia-database.json` (catalogue seed, 71 dataSpecs, grounding SASSEME V1).
> **Croisé avec :** `DEPENDANCES_FICHIERS.md` (dépendances) et le concept de **module de prétraitement**.
>
> Le PDF fait autorité. Là où il diverge du JSON, c'est le JSON qu'il faut corriger/compléter.

---

## 1. Méthode

- Extraction texte des 98 pages du PDF. La comparaison porte sur le **Chapitre 2 « Données d'entrée »**
  (p. 23-59), qui est la spécification officielle fichier-par-fichier, plus le **Glossaire**, les
  **Éléments introductifs** et le **§1.2 (launcher)** pour le module de prétraitement et les dépendances.
- Chaque fichier du PDF ch.2 a été apparié à son `dataSpec` JSON (par `fileName`), puis les **champs**
  documentés dans le PDF ont été confrontés au tableau `fields` du JSON.
- Le PDF décrit **6 modules de données** : Commun (2.2), Agricole (2.3), Hydrologique (2.4),
  Normatif (2.5), **Filières (2.6)**, **Autres Usages (2.7)**.
  Le JSON n'en modélise que **4** : `COMMUN`, `AGRICOLE`, `NORMATIF`, `HYDROGRAPHIQUE`.

---

## 2. Vue d'ensemble

| | PDF (ch.2) | `maelia-database.json` |
|---|---|---|
| Modules de données | 6 | 4 |
| Fichiers spécifiés | ~55 (dont Filières + MNT bruts) | 71 dataSpecs |
| Statut champs | prose officielle | 57 VERIFIED/PARTIAL · 14 PENDING |

**Conclusion générale :** sur les 4 modules communs, la concordance est **très bonne** — la majorité des
fichiers et des champs du JSON sont confirmés mot pour mot par le PDF (souvent le JSON est *plus riche*
grâce au grounding SASSEME). Les écarts se concentrent sur (a) **un module entier manquant** (Filières),
(b) quelques **fichiers optionnels non modélisés**, et (c) des **sous-couvertures de champs** sur des
fichiers-clés, dont le **pivot `ZH.shp`**.

---

## 3. Concordances validées par le PDF (le JSON est correct)

Fichiers dont les champs JSON correspondent exactement (ou dépassent) la spec PDF :

- **`barrages.csv`** — 17 champs JSON = 17 champs PDF §2.5.2 (Nom, ID_STATION_REF, PRIORITE, ZH_ENTREE,
  V_total/V_soutien/V_critique, Qse_max/critique, Efficience*, tps_transfert, ID_RETENUES). ✅ exact.
- **`canaux.csv`** — 11 champs JSON = §2.4.5 (BVe_origine, dataObs, NoDataHiver, idPointRef,
  BVeAvantObs, debitmax, BVe_retourX, fractionEteX/HiverX). ✅ exact.
- **`donneesMNT_ZH.csv`** — champs SWAT (W_bnkful/CH_W2, depth_bnkful/CH_D, slp_ch/CH_S2, L_slp.zh/
  SLSUBBSN, CH_S1, CH_W1) = §2.4.2. ✅ exact (le PDF confirme l'estimation via ARCSWAT).
- **`pointsDeReference.shp`** — ID_STH, ID_ZH, IS_NODAL, DOE, DCR = §2.5.1. ✅ exact.
- **`ilots.shp`** (12 champs) et **`parcelles.shp`** (9 champs) = §2.3.1 / §2.3.2. ✅ exact.
- **`joursParMois.csv`**, **`polygonesMeteoFrance.shp`**, **`altitudeAgregeesParZH.shp`**,
  **`secteursAdministratifs.shp`**, **`seuilsDeRestriction.csv`**, **`zonesAdministratives.shp`**,
  **`retenuesParZH.shp`** (à un champ optionnel près, cf. §5) — concordants. ✅
- **`typeDeSolParZH.shp`** — le JSON *aplatit* les patterns `P[1-10]`, `ARG[1-10]`, etc. décrits en
  §2.2.3 en 33 champs concrets (grounding). ✅ conforme, plus exploitable que le PDF.

Le PDF confirme aussi les points structurants du JSON :
- **`reglesDeDecisions.csv`** : le PDF §2.3.4 indique que les champs sont saisis *« via une interface
  dédiée sur le web »* et renvoie aux supports de formation → cohérent avec `fieldsStatus: VERIFIED_PARTIAL`.
- **Nommage des instances** : §2.3.5 confirme le pattern `prixVentes + <scenario> + .csv` (⇒ `multiInstance`) ;
  §2.2.5 confirme la météo `année.csv` (⇒ JSON `AAAA.csv`, `multiInstance`).
- **Matrices transposées** : §2.5.2 (« chaque colonne représente un barrage, variables en ligne ») et
  §2.3.3 (« chaque culture est une colonne ») confirment le modèle MATRIX du JSON pour `barrages`,
  `especesCultivees`, `reglesDeDecisions`.

---

## 4. Écarts de périmètre (fichiers / modules)

### 4.1 ⛔ Module **Filières** (PDF §2.6) — entièrement absent du JSON
Cinq fichiers officiels non modélisés :

| Fichier PDF | § | Contenu |
|---|---|---|
| `produits.csv` | 2.6.1 | Produits entrants/sortants des unités de transfo (Prix, N/P/K/Mg…, partMS/MO, stockable) |
| `recettes.csv` | 2.6.2 | Recettes de transformation (PRO), taux d'abattement N/C, durée |
| `routes.shp` | 2.6.3 | Réseau routier (osm_id, type, maxspeed…) pour le transport |
| `UP.shp` | 2.6.4 | Unités de production (déchets verts, capacités, dates de prod) |
| `UT.shp` | 2.6.5 | Unités de transformation (compostage, capacités, flux max) |

C'est le **plus gros écart**. Ce module (bioéconomie / PRO) est fonctionnel dans le PDF mais totalement
hors du catalogue. À décider : hors-périmètre assumé, ou à ajouter (module `FILIERES`).

### 4.2 ⚠️ Module **Autres Usages** (PDF §2.7) — non spécifié
Le PDF renvoie au site (module « en cours de stabilisation », demande AEP par commune). Le JSON contient
en revanche les 4 fichiers COMMUN qui l'alimentent (`resultatsEDEM.csv`, `prix_eau_*`, `salaires_*`,
`residence_principale_*`) — donc le JSON **anticipe** ce module sans que le PDF le documente en détail.

### 4.3 Fichiers documentés dans le PDF mais absents du JSON

| Fichier PDF | § | Remarque |
|---|---|---|
| `reseaux_asa.shp` | 2.4.14 | Tracé des collectifs d'irrigation (optionnel, non simulé). |
| `CLC.shp` (générique) | 2.4.10 | Le JSON modélise à la place `clcParZH.shp` + `clcRPGParZH.shp` (variantes découpées par ZH). |
| MNT bruts : `alti25m_ascii.txt`, `MNTTIFN_Alti.tif`, `pente_percent_ascii.txt` | 2.4.9 | **Sources externes** du prétraitement — volontairement hors catalogue (cf. §7). |
| `communes.shp` (distinct) | 2.2.2 | JSON fusionne en `communes-trimUG.shp` (grounding SASSEME). |

### 4.4 Fichiers du JSON hors Chapitre 2 du PDF (extras — légitimes)
Le JSON va au-delà du ch.2 ; ces entrées sont justifiées ailleurs dans le PDF ou par l'outillage :
- **Coûts avancés** : `ASAForfaitDebit/Surface/PrixEau.csv`, `chargesFixesMaterielIrrigation.csv`,
  `chargesFixesAccesRessourceIrrigation.csv`, `redevanceEau.csv`, `rendementsObservesAnterieur.csv`
  → couverts par §2.3.10 « Autres coûts » (le PDF renvoie au site pour le détail).
- **Choix d'assolement** : `systemesDeCultureDeReference.csv`, `matriceDistanceCulturale.csv`,
  `profilesAgriculteurs.csv` → mode expert §2.3.12 (fonctions de croyance).
- **Fertilisation** : `Engrais.csv`, `reglesDeDecisions_fertilisation.csv` → interface web §2.3.4.
- **Forçage hydro** : `debitEntre.csv`, `debitEntreObs.csv`, `zhDebitForce.csv` → paramètres launcher
  §1.2.3.2 (`listNomsZHsDebitForce`, `listNomsZHsDebitComplement`), pas dans la section données.
- **Internes normatif/prétraitement** : `UG_region_L93_BGA.shp`, `matriceDebitReelPointDOE.csv`,
  `volumeRefAnnuelIND.csv`, `VP_historique.csv`, `NomCanal.csv` (le `dataObs` de §2.4.5).

---

## 5. Écarts au niveau des champs (fichiers présents des deux côtés)

### 5.1 🔴 `ZH.shp` — le pivot est sous-spécifié
- **JSON : 1 seul champ** (`ID_ZH`).
- **PDF §2.4.1 : 5 champs** — `ID_ZH`, `EU_CD`, `EU_CD_exut`, `PERCENTAGE`, **`ID_ND_EXUT`**.
- **`ID_ND_EXUT` est le champ qui décrit le chaînage amont→aval des BVe** (arbre d'écoulement).
  Son absence dans le JSON est notable : c'est *la* relation topologique du réseau hydro, et le
  `DEPENDANCES_FICHIERS.md` désigne `ZH.shp` comme pivot de 14 fichiers. Le grounding SASSEME n'a capté
  que `ID_ZH` (l'échantillon n'avait pas ces colonnes). **À compléter** (au moins `ID_ND_EXUT`).
- `EU_CD`, `EU_CD_exut`, `PERCENTAGE` sont marqués *Optionnel* dans le PDF (sorties par masse d'eau).

### 5.2 🟠 Points de prélèvement `ppAep.shp` / `ppInd.shp`
- JSON : 6 champs (`ID_EQU`, `CODE_INSEE`, `TAUX`, `ID_RESS_ZH`, `ID_RESSOUR`, `NATURE`).
- PDF §2.4.7 en documente ~11 : manquent **`ID_ZH`**, **`VOL_AGENCE`** (m³/an, utilisé), `TOPONYME`,
  `cours_eau`, `syst_aqui` (ces 3 derniers « non utilisés par MAELIA », optionnels). `VOL_AGENCE` et
  `ID_ZH` méritent d'être ajoutés.

### 5.3 🟠 `ppIrr.shp`
- JSON : 3 champs (`ID_EQU`, `CODE_INSEE`, `TAUX`).
- PDF §2.4.7 ajoute les champs irrigation : `NOM_ASA`/`ID_ASA`, `CODE_IRRIG`/`ID_IRRIG`, `VOLUME_AUT`,
  `rssce_NOM`. Sous-couverture forte, mais le PDF précise que ce fichier « n'est pas utilisé par le
  simulateur » (lien îlot↔ressource) → priorité basse.

### 5.4 🟠 `especesCultivees.csv` — patterns non expansés
- JSON : 21 champs, dont deux **placeholders** `ALPHA*` et `KC*`.
- PDF §2.3.3 : les paramètres vont jusqu'à la ligne 45 — `ALPHA*` = lignes 19-28 (**10** valeurs),
  `KC*` = lignes 29-44 (**16** valeurs), utilisés par le modèle de culture « simple ». À expanser si
  ce modèle est utilisé (pour AqYield ils ne sont pas nécessaires — cohérent avec `VERIFIED_PARTIAL`).

### 5.5 🟢 `retenuesParZH.shp` (mineur)
- JSON 9 champs vs PDF §2.4.6 10 champs : manque `TOPONYME` (optionnel, spatialisation). Le JSON a bien
  `FRACTIONDRAIN` (= `FRACTIONDR` du PDF), `ORDREDRAIN`, `VOLMAX`, `TYPEOFRET`, `Q_RESERVE`.

### 5.6 Divergences de nommage confirmées (déjà notées ailleurs)
`rjAep→rjAEP`, `rjInd→rjI`, `ppIrr` (vs `pplrr` des organigrammes), `année.csv→AAAA.csv`,
`communes.shp→communes-trimUG.shp`, `CLC.shp→clcParZH/clcRPGParZH`. Aucune n'est une erreur de fond.

---

## 6. Dépendances entre fichiers — validation par le PDF

Le PDF **corrobore** le graphe de `DEPENDANCES_FICHIERS.md` :

- **`ID_ND_EXUT` (ZH.shp)** = arbre d'écoulement amont-aval → confirme que `ZH.shp` est la racine hydro
  (§2.4.1). ⚠️ mais ce champ manque dans le JSON (§5.1).
- **Chaîne agricole** `RPG → ilots.shp → parcelles.shp` : §2.3.2 « SEQUENCE… peut venir de la base INRAE
  développée à partir de l'analyse du RPG » ; `ID_EXPL`, `ID_SOL`, `ID_ZH` comme FK dans `ilots` (§2.3.1). ✅
- **Chaîne normative** `ZH → pointsDeReference (ID_STH) → zonesAdministratives (ID_ZA) →
  secteursAdministratifs` : §2.5.1→2.5.5 (`ID_STH`, `ID_ZA`) ; `barrages.csv` référence `ID_STATION_REF`,
  `ZH_ENTREE`, `ID_RETENUES` (§2.5.2). ✅ exactement le graphe du .md.
- **Chaîne météo** `polygonesMeteoFrance (ID_PDG) → AAAA.csv` via `idPolygone`/`ID_PDG` (§2.2.4/2.2.5). ✅
- **Canaux** : `canaux.csv` référence des `BVe_origine`/`BVe_retourX` = `ID_ZH` (§2.4.5), et
  `RestrictionsDebitCanaux.csv` s'appuie sur `ID_Canal` (§2.5.7). ✅
- **Restrictions** : `seuilsDeRestriction` (`ID_ZA`) et `joursDeRestriction` (`ID_SECTEUR`) →
  §2.5.4/2.5.6, cohérent avec « saisie valide seulement après génération » (niveau 4 du tri topologique). ✅

> Aucune contradiction relevée entre le graphe de dépendances et le PDF. Le seul correctif induit est
> l'ajout de `ID_ND_EXUT` sur `ZH.shp` pour matérialiser explicitement l'arête de drainage.

---

## 7. Module de prétraitement — validation par le PDF

Le PDF **définit et valide** le concept de prétraitement sur lequel repose la classification `AUTO`/`MANUAL`
du JSON et le `DEPENDANCES_FICHIERS.md` :

- **Glossaire** : « dossiers *includes* … **générés par des prétraitements ou manuellement** » ; et
  « **implémentation d'un nouveau territoire** : mise en œuvre par un utilisateur expert de prétraitements
  permettant de mettre au format des données brutes … et de générer les fichiers includes ».
- **Éléments introductifs** (Figure 1) : « un ensemble [de] **prétraitements informatiques, qui transforment
  des données génériques et locales en une base de données unique par territoire (includes)** ». C'est
  exactement le rôle attribué aux fichiers verts `generation: AUTO`.

Le PDF confirme fichier par fichier les **sources externes** du prétraitement (grises dans le schéma .md) :

| Source externe (PDF) | Alimente (fichiers AUTO) | § |
|---|---|---|
| MNT (alti/pente ASCII+TIFF) | `altitudeAgregeesParZH`, pentes, bandes neige, `PENTE_MOY` | 2.2.7 / 2.4.8 / 2.4.9 / 2.3.1 |
| RPG (parcellaire PAC) | `ilots.shp`, `parcelles.shp`, `SEQUENCE` | 2.3.1 / 2.3.2 |
| Corine Land Cover | `clcParZH.shp` / HRU | 2.4.10 |
| SAFRAN / E-OBS (Copernicus) | météo `AAAA.csv`, `polygonesMeteoFrance` | 2.2.4 / 2.2.5 |
| INSEE / IGN | `communes-trimUG`, `departement`, AEP | 2.2.1 / 2.2.2 |
| ARCSWAT | `donneesMNT_ZH.csv` (géomorpho SWAT) | 2.4.2 |
| BDGSF (BD sols) | `typeDeSolParZH.shp` | 2.2.3 |
| BD TOPO / BD CARTHAGE | `retenuesParZH`, `canaux`, `ZH` | 2.4.6 / 2.4.12 / Glossaire |
| Agences de l'eau / OUGC | `ppAep/ppInd/ppIrr`, `rjAEP/rjI` | 2.4.7 |

Le PDF ne fournit **pas** de spécification du code de prétraitement lui-même (il précise qu'il « nécessite
une expertise importante » et un accompagnement de l'équipe MAELIA). Il n'existe donc pas de fichier de
plus dans le PDF pour enrichir le module ; la modélisation actuelle (sources externes en amont, non
cataloguées ; fichiers AUTO produits ; fichiers MANUAL saisis) est **conforme et complète** vis-à-vis du PDF.

---

## 8. Actions recommandées (priorisées)

| # | Action | Priorité | Justif. |
|---|---|---|---|
| 1 | Ajouter **`ID_ND_EXUT`** (+ `EU_CD`, `EU_CD_exut`, `PERCENTAGE` optionnels) à `ZH.shp` | **Haute** | pivot hydro, arête de drainage manquante (§5.1) |
| 2 | Décider du sort du **module Filières** (`produits`, `recettes`, `routes`, `UP`, `UT`) : hors-périmètre ou ajout d'un module `FILIERES` | **Haute** | seul module entier manquant (§4.1) |
| 3 | Compléter `ppAep`/`ppInd` avec **`ID_ZH`** et **`VOL_AGENCE`** | Moyenne | champs utilisés par le simulateur (§5.2) |
| 4 | Expanser `especesCultivees` `ALPHA*` (10) / `KC*` (16) si modèle culture « simple » utilisé | Moyenne | sinon garder PARTIAL (AqYield n'en a pas besoin) (§5.4) |
| 5 | Champs optionnels de confort : `retenues.TOPONYME`, `ppIrr.*` (ASA/IRRIG) | Basse | non utilisés par le simulateur (§5.3/5.5) |
| 6 | Renseigner les 14 `PENDING` restants à partir du PDF là où possible (ex. `departement.CODE_DEPT` §2.2.1) | Basse | plusieurs champs simples sont dans le PDF |

> Le PDF valide `departement.shp` (PENDING, 0 champ) = **1 champ `CODE_DEPT`** (§2.2.1) — correction immédiate possible.

---

## 9bis. Statut d'application (2026-07-10)

**Tous les correctifs ont été appliqués** à `data/maelia-database.json` (77 dataSpecs, `studyArea: ferlo-sine`)
et **synchronisés** vers `maelia-server/src/main/resources/catalog/maelia-database.json` (seed backend) :

- `ZH.shp` +`ID_ND_EXUT`,`EU_CD`,`EU_CD_exut`,`PERCENTAGE` · `departement.shp` +`CODE_DEPT`
- `ppAep`/`ppInd` +`ID_ZH`,`VOL_AGENCE`,`TOPONYME`,`cours_eau`,`syst_aqui` · `ppIrr` +8 champs (ASA/IRRIG/point)
- `especesCultivees` `ALPHA*`→`ALPHA1..10`, `KC*`→`KC1..16` · `retenuesParZH` +`TOPONYME`
- `redevanceEau`, `RestrictionsDebitCanaux`, `tronconsParZH`, `tronconsPrincipauxParZH`, `rjAEP`, `rjI` : PENDING → champs renseignés
- **Nouveau fichier** `IND_RJI.csv` (liens prélèvement↔rejet industriel, §2.4.7)
- **Nouveau module `FILIERES`** (5 fichiers : `produits`, `recettes`, `routes`, `UP`, `UT`)

**Côté backend** : seed synchronisé ; défaut `studyArea` aligné sur `ferlo-sine` (`Project.create`,
`ProjectJpaEntity`, migration `V19__project_study_area_default_ferlo_sine.sql`). Le seeder charge `module`,
`infoType`, `allowedValues` comme chaînes libres → `FILIERES` et les nouveaux champs passent sans changement
de schéma. La FK `references_data_spec` ayant été supprimée (V5), aucune contrainte d'ordre ne s'applique.

⚠️ Le `DataSpecSeeder` ne réensemence **que si le catalogue est vide** (`repository.count()==0`) : pour
appliquer le nouveau seed sur une base existante, vider `data_spec`/`field_spec` ou recréer la base.

**Restent PENDING (7)** — non spécifiés par le PDF officiel, donc non inventés : `matriceDistanceCulturale`,
`chargesFixesAccesRessourceIrrigation`, `rendementsObservesAnterieur`, `VP_historique`,
`matriceDebitReelPointDOE`, `zhDebitForce`, `volumeRefAnnuelIND`.

## 9. Synthèse

- **Concordance élevée** sur les 4 modules communs : fichiers et champs du JSON quasi tous confirmés par
  la doc officielle, souvent enrichis par le grounding SASSEME.
- **Écart majeur** : le **module Filières** (5 fichiers) est absent du catalogue.
- **Écart critique ponctuel** : le pivot **`ZH.shp`** ne porte que `ID_ZH` alors que le PDF documente
  `ID_ND_EXUT` (chaînage amont-aval) — à corriger.
- **Dépendances et module de prétraitement** : le PDF **confirme sans contradiction** le graphe de
  `DEPENDANCES_FICHIERS.md` et la classification `AUTO`/`MANUAL`, y compris toutes les sources externes.
