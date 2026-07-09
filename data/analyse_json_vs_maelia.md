# Analyse comparative : `maelia-database.json` vs Organigrammes & Schéma Excel MAELIA

> **Références :** Organigrammes (OrganigrammeAll, ModeleAgricole, ModeleCommun, ModeleHydrographique, ModeleNormatif) + `MAELIA_Schema_Donnees.xlsx`  
> **Source analysée :** `maelia-database.json` — généré le 2026-06-28, zone d'étude : `garonne-amont`, basé sur SASSEME V1

---

## Résumé global

| Dimension | Valeur |
|-----------|--------|
| Total dataSpecs dans le JSON | **71** |
| Modules couverts | COMMUN (12), AGRICOLE (26), NORMATIF (10), HYDROGRAPHIQUE (23) |
| Fichiers grounded (vérifiés sur données réelles) | **14** |
| Fichiers schema only (documentés, pas encore vérifiés) | **43** |
| Fichiers pending (champs non encore documentés) | **14** |

### Légende des statuts

| Symbole | Signification |
|---------|--------------|
| ✅ GROUNDED | Fichier vérifié sur données réelles (ZIP SASSEME V1) — colonnes + échantillons confirmés |
| 📋 SCHEMA ONLY | Documenté dans le JSON (champs définis), mais pas de données réelles disponibles |
| ⏳ PENDING | Fichier référencé mais champs non encore documentés — à compléter |
| `REQ` | Fichier obligatoire pour toute simulation |
| `COND` | Obligatoire sous condition (option de modélisation activée) |
| `OPT` | Optionnel |

### Comparaison JSON vs Organigrammes/Excel — vue d'ensemble

| Aspect | Organigrammes / Excel (référence) | JSON (`maelia-database.json`) |
|--------|-----------------------------------|-------------------------------|
| Nombre total de fichiers décrits | ~68 (estimation organigrammes) | **71** |
| Modèle normatif | Présent dans organigramme, **absent du ZIP** | Présent et documenté (10 specs) |
| Nommage `pplrr.shp` / `pplInd.shp` | Organigramme : `pplrr.shp`, `pplInd.shp` | JSON : **`ppIrr.shp`**, **`ppInd.shp`** (renommés) |
| Nommage `N x prixVentes(ID).csv` | Organigramme : `N x prixVentes(ID).csv` | JSON : **`prixVentes(ID).csv`** (simplifié) |
| Nommage `N x NomCanal.csv` | Organigramme : `N x NomCanal.csv` | JSON : **`NomCanal.csv`** (simplifié) |
| Fichiers hydro ZH extra | Non visibles dans organigrammes | `debitEntre.csv`, `debitEntreObs.csv`, `zhDebitForce.csv` (**3 nouveaux**) |
| `reglesDeDecisions.csv` | Excel : structure vague | JSON : **matrice complète** (200+ paramètres, 8 ITK) |
| `Engrais.csv` | Non prévu dans organigrammes | JSON : `REQ`, 33 engrais, 33 paramètres (ETM, coûts, CO₂…) |
| `reglesDeDecisions_fertilisation.csv` | Non prévu dans organigrammes | JSON : `REQ`, matrice 36 paramètres |
| `residence_principale_maelia-complet.csv` | Tronqué dans organigramme (`nce_principale…`) | JSON : nom complet rétabli |
| `polygonesMeteoProjettes.shp` | `polygonesMeteoProjecttes.shp` (typo) | JSON : **`polygonesMeteoProjettes.shp`** (corrigé) |
| Champs non encore documentés | N/A | 14 fichiers `PENDING` à compléter |

---

---

## Module : COMMUN

**12 fichiers décrits** — 4 obligatoires, 4 groundés, 1 en attente de documentation.

### `typesDeSol`

#### `typeDeSolParZH.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `commun.typesDeSol.typeDeSolParZH` |
| Type | `SHP` |
| Statut données | ✅ GROUNDED |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `REQ` |
| Nb enregistrements | 8 |
| Géométrie | `True` |
| Projection | `WGS_1984_UTM_Zone_28N` |
| Génération | `AUTO` |

**Description :** Type de sol par BVe (parametres SWAT et AqYield). Sol commun a tous les modules.

**Champs (33) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ZONE_PEDO` | String |  |  |  | Identifiant du zonage pédologique (par exemple ‘ctx_arg’ pour coteaux argileux) _(ex: `dekkMbel_cb_avec_arbr`, `dekkMbel_cb_sans_arbr`, `dekk_cb_avec_arbr`)_ |
| `ID_SOL` | String |  | ✅ |  | Identifiant unique du type de sol par BVe (actuellement composé de l’ID du BVe x _(ex: `SSM1-argileux-dekkMbel_cb_avec_arbr`, `SSM1-argileux-dekkMbel_cb_sans_arbr`, `SSM1-argileux-dekk_cb_avec_arbr`)_ |
| `ID_ZH` | String |  | ✅ | hydro.zonesHydrographiques.ZH | Identifiant du BVe _(ex: `SSM1`, `SSM1`, `SSM1`)_ |
| `STU_DOM` | String |  |  |  | Identifiant du sol SWAT _(ex: `argileux`, `argileux`, `argileux`)_ |
| `PIRM` | Double | mm |  |  | Perméabilité du sol pour le modèle sol-plante _(ex: `420.240000000000009`, `348.000000000000000`, `420.240000000000009`)_ |
| `CSTRU` | Double |  |  |  | Note sur la qualité de structure du sol pour le modèle sol-plante _(ex: `0.500000000000000`, `0.500000000000000`, `0.500000000000000`)_ |
| `PRO` | Double |  |  |  |  _(ex: `60`, `60`, `60`)_ |
| `P1` | Double |  |  |  |  _(ex: `30`, `30`, `30`)_ |
| `P2` | Double |  |  |  |  _(ex: `60`, `60`, `60`)_ |
| `ARG1` | Double |  |  |  |  _(ex: `18.066666666666663`, `18.100000000000001`, `17.492063492063490`)_ |
| `ARG2` | Double |  |  |  |  _(ex: `18.800000000000001`, `18.699999999999999`, `18.365079365079364`)_ |
| `SAB1` | Double |  |  |  |  _(ex: `64.400000000000006`, `64.450000000000003`, `66.047619047619051`)_ |
| `SAB2` | Double |  |  |  |  _(ex: `64.733333333333334`, `64.849999999999994`, `65.984126984126988`)_ |
| `DAH1` | Double |  |  |  |  _(ex: `1.557333333333333`, `1.556500000000000`, `1.546031746031746`)_ |
| `DAH2` | Double |  |  |  |  _(ex: `1.514666666666667`, `1.509000000000000`, `1.508888888888889`)_ |
| `MO1` | Double |  |  |  |  _(ex: `0.807981333333333`, `0.818038000000000`, `0.753360634920635`)_ |
| `MO2` | Double |  |  |  |  _(ex: `0.557426666666667`, `0.561162000000000`, `0.523767619047619`)_ |
| `PH1` | Double |  |  |  |  _(ex: `5.864444444444445`, `5.868421052631579`, `5.841798941798941`)_ |
| `PH2` | Double |  |  |  |  _(ex: `5.806666666666667`, `5.815789473684211`, `5.766666666666667`)_ |
| `CN1` | Double |  |  |  |  _(ex: `13.107424360816616`, `13.494923255783169`, `13.057301296621487`)_ |
| `CN2` | Double |  |  |  |  _(ex: `10.096398524302076`, `10.308825718844409`, `9.962350778232222`)_ |
| `HCC1` | Double |  |  |  |  _(ex: `19.926366486388961`, `19.618432047215883`, `18.820873287626100`)_ |
| `HCC2` | Double |  |  |  |  _(ex: `20.082566613989400`, `20.266224473757472`, `19.060624978924487`)_ |
| `HPFP1` | Double |  |  |  |  _(ex: `9.113580874856686`, `8.976414785115804`, `8.657768728491996`)_ |
| `HPFP2` | Double |  |  |  |  _(ex: `9.235676702409425`, `9.353365477423690`, `8.797480478030915`)_ |
| `RUPRH1` | Double |  |  |  |  _(ex: `32.438356834596810`, `31.926051786300238`, `30.489313677402308`)_ |
| `RUPRH2` | Double |  |  |  |  _(ex: `32.540669734739907`, `32.738576989001338`, `30.789433502680723`)_ |
| `KSAT1` | Double |  |  |  |  _(ex: `322.740000000000009`, `296.279999999999973`, `322.740000000000009`)_ |
| `KSAT2` | Double |  |  |  |  _(ex: `242.055000000000007`, `222.210000000000008`, `242.055000000000007`)_ |
| `EG1` | Double |  |  |  |  _(ex: `0`, `0`, `0`)_ |
| `EG2` | Double |  |  |  |  _(ex: `0`, `0`, `0`)_ |
| `CAL1` | Double |  |  |  |  _(ex: `0`, `0`, `0`)_ |
| `CAL2` | Double |  |  |  |  _(ex: `0`, `0`, `0`)_ |

**Comparaison vs références :**

- 📊 **Divergence / enrichissement vs schéma Excel :** Schéma Excel utilise `PRO_OC`, `ARG_OC`, `DAH_OC` → JSON confirme noms courts `PRO`, `ARG1/2`, `DAH1/2` issus du DBF réel.

### `meteo/observee`

#### `AAAA.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `commun.meteo.serieClimatique` |
| Type | `CSV` |
| Statut données | ✅ GROUNDED |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `REQ` |
| Nb enregistrements | 365 |
| Résolution temporelle | `DAY` |
| Multi-instance | Oui (un fichier par année/polygone) |
| Génération | `AUTO` |

**Description :** Serie climatique journaliere (SAFRAN observe 1970-2013 ou Arpege simule). Forcage.

**Champs (7) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `idPolygone` | String |  | ✅ | commun.meteo.polygonesMeteoFrance | Identifiant du polygone meteo |
| `date` | Date | j/m/aaaa | ✅ |  | Date |
| `precipitation` | Double | mm | ✅ |  | Precipitation |
| `tmin` | Double | C | ✅ |  | Temperature minimale |
| `tmax` | Double | C | ✅ |  | Temperature maximale |
| `etp` | Double | mm | ✅ |  | Evapotranspiration potentielle |
| `rayonnement` | Double | MJ/m2/j |  |  | Rayonnement (requis si modele HerbSim) |

### `meteo`

#### `polygonesMeteoFrance.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `commun.meteo.polygonesMeteoFrance` |
| Type | `SHP` |
| Statut données | ✅ GROUNDED |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `REQ` |
| Nb enregistrements | 1 |
| Géométrie | `True` |
| Projection | `WGS_1984_UTM_Zone_28N` |
| Génération | `AUTO` |

**Description :** Polygones meteo servant de jointure avec la serie climatique.

**Champs (4) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_PDG` | String |  | ✅ |  |  _(ex: `0001`)_ |
| `POSX` | Double |  |  |  |  _(ex: `336039.975838058919180`)_ |
| `POSY` | Double |  |  |  |  _(ex: `1603304.477980490308255`)_ |
| `ALTI_MOY` | Double | m |  |  | Altitude moyenne de la bande d’élévation. Sept valeurs possibles : 250, 750, 1 _(ex: `0.000000000000000`)_ |

#### `polygonesMeteoProjettes.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `commun.meteo.polygonesMeteoProjettes` |
| Type | `SHP` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ⚠️ VERIFIED_PARTIAL |
| Obligatoire | `COND` _(si scenarioClimatique != null)_ |
| Génération | `AUTO` |

**Description :** Polygones meteo projetes (scenarios climatiques).

**Champs (4) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_PDG` | String |  | ✅ |  | Identifiant du polygone meteo projete |
| `POSX` | Double | m |  |  | Coordonnee X du centroide (UTM 28N) |
| `POSY` | Double | m |  |  | Coordonnee Y du centroide (UTM 28N) |
| `ALTI_MOY` | Double | m |  |  | Altitude moyenne du polygone |

**Comparaison vs références :**

- ⚠️ **Renommage vs organigramme :** était `polygonesMeteoProjecttes.shp (organigramme/typo)`

### `date`

#### `joursParMois.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `commun.date.joursParMois` |
| Type | `CSV` |
| Statut données | ✅ GROUNDED |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `REQ` |
| Nb enregistrements | 13 |
| Génération | `AUTO` |

**Description :** Calendrier : nombre de jours par mois.

**Champs (3) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `numeroMois` | Integer |  | ✅ |  |  _(ex: `1`, `2`, `3`)_ |
| `anneeBessextile` | Integer |  |  |  |  _(ex: `0`, `31`, `60`)_ |
| `anneeNonBissextile` | Integer |  |  |  |  _(ex: `0`, `31`, `59`)_ |

**Comparaison vs références :**

- 📊 **Divergence / enrichissement vs schéma Excel :** Schéma Excel orthographie `anneeBissextile` → JSON (et fichier réel) utilisent `anneeBessextile` (faute de frappe à corriger).

### `communes`

#### `communes-trimUG.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `commun.communes.communesTrimUG` |
| Type | `SHP` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si module.usages == true)_ |
| Génération | `AUTO` |

**Description :** Communes (source INSEE).

**Champs (2) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `CODE_INSEE` | String |  | ✅ |  | Code INSEE de la commune |
| `NOM` | String |  | ✅ |  | Nom de la commune |

#### `departement.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `commun.communes.departement` |
| Type | `SHP` |
| Statut données | ⏳ PENDING |
| Statut champs | ⏳ PENDING |
| Obligatoire | `COND` _(si module.usages == true)_ |
| Génération | `AUTO` |

**Description :** Departements.

> ⏳ **Champs non encore documentés** — à compléter.

#### `resultatsEDEM.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `commun.communes.resultatsEDEM` |
| Type | `CSV` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si module.usages == true)_ |
| Résolution temporelle | `YEAR` |
| Génération | `MANUAL` |

**Description :** Evolution demographique par commune (scenario central OMPHALE/INSEE). Matrice : annees en ligne 0, code INSEE en colonne 0.

**Champs (2) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `codeInsee` | Integer |  | ✅ |  | Code INSEE (sans 0 initial) |
| `population[annee]` | Double | habitants | ✅ |  | Population par annee (colonnes 2000..2032) |

#### `prix_eau_maelia_complet.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `commun.communes.prixEau` |
| Type | `CSV` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si module.usages == true)_ |
| Génération | `MANUAL` |

**Description :** Prix de l'eau potable (equation AEP MOGIRE).

**Champs (2) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `codeInsee` | Integer |  | ✅ |  | Code INSEE de la commune |
| `prixM3[annee]` | Double | euros/m3 | ✅ |  | Prix du m3 d'eau potable par annee (1 colonne par annee) |

#### `salaires_maelia-complet.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `commun.communes.salaires` |
| Type | `CSV` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si module.usages == true)_ |
| Génération | `MANUAL` |

**Description :** Salaires (equation AEP MOGIRE).

**Champs (2) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `codeInsee` | Integer |  | ✅ |  | Code INSEE de la commune |
| `salaire` | Double | euros | ✅ |  | Salaire net horaire moyen |

#### `residence_principale_maelia-complet.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `commun.communes.residencePrincipale` |
| Type | `CSV` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si module.usages == true)_ |
| Génération | `MANUAL` |

**Description :** Taux de residences principales.

**Champs (2) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `codeInsee` | Integer |  | ✅ |  | Code INSEE de la commune |
| `tauxResidencePrincipale[annee]` | Double | fraction | ✅ |  | Taux de residences principales par annee |

**Comparaison vs références :**

- ⚠️ **Renommage vs organigramme :** était `nce_principale_maelia-compl… (tronqué dans organigramme)`

### `altitude`

#### `altitudeAgregeesParZH.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `commun.altitude.altitudeAgregeesParZH` |
| Type | `SHP` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si module.hydrographique == true)_ |
| Génération | `AUTO` |

**Description :** Altitude moyenne agregee par BVe.

**Champs (3) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_ALTI` | String |  | ✅ |  | Identifiant de la bande d'elevation |
| `ID_ZH` | String |  | ✅ | hydro.zonesHydrographiques.ZH | Identifiant du BVe associe |
| `ALTI_MOY` | Double | m |  |  | Altitude moyenne de la bande (250, 750, 1250, 1750, 2250, 2750, 3250) |


---

## Module : AGRICOLE

**26 fichiers décrits** — 8 obligatoires, 7 groundés, 4 en attente de documentation.

### `agriculteurs`

#### `exploitations.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `agri.agriculteurs.exploitations` |
| Type | `CSV` |
| Statut données | ✅ GROUNDED |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `REQ` |
| Nb enregistrements | 44 |
| Génération | `MANUAL` |

**Description :** Liste des exploitations agricoles du territoire et leur type.

**Champs (2) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_EXPL` | String |  | ✅ |  | Identifiant de l'exploitation _(ex: `SSM1-0001`, `SSM1-0002`, `SSM1-0003`)_ |
| `TYPE_EXPL` | String |  |  |  | Type d'exploitation (avec/sans unite de travail liee) _(ex: `sans_UTL`, `avec_UTL`)_ |

**Comparaison vs références :**

- ⊕ **Nouveau / non référencé dans les organigrammes :** Organigramme référence `profilesAgriculteurs.csv` — JSON confirme `exploitations.csv` comme fichier réel

#### `materiel.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `agri.agriculteurs.materiel` |
| Type | `CSV` |
| Statut données | ✅ GROUNDED |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `REQ` |
| Nb enregistrements | 0 |
| Génération | `MANUAL` |

**Description :** Materiels d'irrigation (dire d'expert).

**Champs (3) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `idMateriel` | String |  | ✅ |  | Identifiant du materiel |
| `surfaceIrrigableJour` | Double | ha/jr | ✅ |  | Surface irrigable par jour (SIJ) |
| `tempsTravailJour` | Double | h/jr | ✅ |  | Temps de travail par jour |

**Comparaison vs références :**

- 📊 **Divergence / enrichissement vs schéma Excel :** Schéma Excel : colonnes `SIJ (ha/jr)` et `travail (h/jr)`. JSON normalise en `surfaceIrrigableJour` et `tempsTravailJour`. `rowCount=0` : fichier vide dans SASSEME V1.

#### `profilesAgriculteurs.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `agri.agriculteurs.profilesAgriculteurs` |
| Type | `CSV` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si assolementMethod == FONCTIONS_DE_CROYANCE)_ |
| Génération | `MANUAL` |

**Description :** Profils d'agriculteurs (utile uniquement si assolement par fonctions de croyance).

**Champs (2) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `nomProfil` | String |  | ✅ |  | Nom du profil agriculteur |
| `caracteristiques` | String |  |  |  | Caracteristiques du profil (colonnes variables) |

#### `perceptionAgriculteurs.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `agri.agriculteurs.perceptionAgriculteurs` |
| Type | `CSV` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si assolementMethod == FONCTIONS_DE_CROYANCE)_ |
| Génération | `MANUAL` |

**Description :** Biais de perception des agriculteurs (fonctions de croyance).

**Champs (4) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `idAgriculteur` | String |  | ✅ |  | Identifiant de l'agriculteur |
| `biaisFenetresTemporelles` | Double | jour |  |  | Biais de perception des fenetres temporelles |
| `biaisVegetation` | Double |  |  |  | Biais de perception vegetation |
| `biaisPrecipitationsTeneurEau` | Double |  |  |  | Biais de perception des precipitations et teneur en eau du sol |

### `culture`

#### `especesCultivees.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `agri.culture.especesCultivees` |
| Type | `CSV` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ⚠️ VERIFIED_PARTIAL |
| Obligatoire | `REQ` |
| Génération | `MANUAL` |

**Description :** Especes cultivees : rendements, phenologie, parametres AqYield. Lecture par numero de ligne.

**Champs (21) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_ESPECE` | String |  | ✅ |  | Identifiant de l'espece |
| `RENDEMENT_MOYEN` | Double | t/ha | ✅ |  | Rendement moyen (module Simple) |
| `RENDEMENT_MIN` | Double | t/ha | ✅ |  | Rendement minimal (module Simple) |
| `RENDEMENT_OPTIMAL` | Double | t/ha | ✅ |  | Rendement potentiel |
| `COULEUR_R` | Integer |  |  |  | Composante rouge d'affichage |
| `COULEUR_V` | Integer |  |  |  | Composante verte d'affichage |
| `COULEUR_B` | Integer |  |  |  | Composante bleue d'affichage |
| `Tbase` | Double | C | ✅ |  | Temperature de base (echelle de vegetation) |
| `Tmax` | Double | C | ✅ |  | Temperature maximale (echelle de vegetation) |
| `DEGRES_J_LevTbase` | Double | C | ✅ |  | Somme degres-jours a la levee |
| `DEGRES_J_Flor` | Double | C | ✅ |  | Somme degres-jours a la floraison |
| `DEGRES_J_matPhyTbase` | Double | C | ✅ |  | Somme degres-jours a la maturite physiologique |
| `FREIN` | Double |  |  |  | Fraction de somme de temperature en periode hivernale (0-1, 1=absence de frein) |
| `CRACINE` | Double | C/mm |  |  | Somme degres-jours pour 1 mm de croissance racinaire |
| `CVIG` | Double |  |  |  | Potentiel de croissance de la plante |
| `KMAX` | Double |  |  |  | Kc maximum de la culture |
| `CSTO` | Integer |  |  |  | Effet de fermeture des stomates sur le stress hydrique |
| `coeff_Fonction_Prod` | Double |  |  |  | Coefficient de forme de la fonction de production (a dans AqYield) |
| `ALPHA*` | Double | variable |  |  | Colonnes 19-28 : courbe de Kc (modele Simple) |
| `KC*` | Double | variable |  |  | Colonnes 29-44 : courbe de Kc (modele Simple) |
| `ZonesClimatiques` | String |  |  |  | Zones climatiques pouvant accueillir cette culture |

**Comparaison vs références :**

- 📊 **Divergence / enrichissement vs schéma Excel :** Schéma Excel décrit 45 colonnes regroupées, JSON liste 21 labels canoniques (`ALPHA*`, `KC*` regroupant plusieurs colonnes).

#### `reglesDeDecisions.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `agri.culture.reglesDeDecisions` |
| Type | `CSV` |
| Statut données | ✅ GROUNDED |
| Statut champs | ⚠️ VERIFIED_PARTIAL |
| Obligatoire | `REQ` |
| Nb enregistrements | 272 |
| Génération | `MANUAL` |

**Description :** Itineraires techniques (ITK) / regles de decision. Matrice : col 1 = id parametre, col 2 = unite, col 3..N = valeur par ITK.

**Champs (8) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `NOM_ITK_AFFICHAGE` | String |  | ✅ |  | Nom d'affichage de l'ITK |
| `ID_ITK` | String |  | ✅ |  | Identifiant unique de l'ITK |
| `ID_ESPECE` | String |  | ✅ | agri.culture.especesCultivees | Espece concernee |
| `IDS_SDCS` | String |  | ✅ |  | Liste des SDC associes |
| `MATERIEL` | String |  |  | agri.agriculteurs.materiel | Id materiel d'irrigation ou NA |
| `ZONE_PEDO` | String |  | ✅ |  | Zones pedologiques |
| `IS_CULTURE_HIVER` | Boolean |  | ✅ |  | Culture d'hiver |
| `OT_*` | String | variable |  |  | Bloc de parametres par operation technique (semis, travail du sol, binage...) :  |

**Format matrice transposée** _(colonne pivot : `NOM_ITK_AFFICHAGE`)_ :

- **Entités (8) :** `X.`, `gel`, `arachide_precMil`, `arachide_precJachere`, `jachere_precMil`, `jachere_precArachide`, `mil_precArachide`, `mil_precMil`
- **Paramètres (272) :** `ID_ITK`, `IDS_SDCS`, `IDS_SDCS_CLASS`, `ID_ESPECE`, `MATERIEL`, `ID_PREC`, `ZONE_PEDO`, `ZONE_PEDO_CLASS`, `TYPE_EXPL`, `CLIMAT`, `IS_CULTURE_HIVER`, `IS_PREPA`, `PREPA_PASSAGES`, `PREPA_OUTIL`, `PREPA_AGRIW` …

#### `reglesDeDecisions_fertilisation.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `agri.culture.reglesDeDecisionsFertilisation` |
| Type | `CSV` |
| Statut données | ✅ GROUNDED |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `REQ` |
| Nb enregistrements | 36 |
| Génération | `MANUAL` |

**Description :** Regles de decision de fertilisation par ITK (matrice : parametres en lignes, ITK en colonnes).

**Champs (3) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `FERTIALT_NOM_ITK` | String |  | ✅ | agri.culture.reglesDeDecisions | Identifiant de l'ITK (colonne parametre) |
| `sousParametre` | String |  |  |  | Sous-parametre (DOSE, DOSE_P, DOSE_K, ...) |
| `valeurParITK` | String |  |  |  | Valeur par ITK (1 colonne par ITK) |

**Format matrice transposée** _(colonne pivot : `FERTIALT_NOM_ITK`)_ :

- **Entités (6) :** `arachide_precMil`, `arachide_precJachere`, `jachere_precMil`, `jachere_precArachide`, `mil_precArachide`, `mil_precMil`
- **Paramètres (36) :** `FERTIALT_NOM_ALTERNATIVE`, `FERTIALT_ORDRE_ALTERNATIVE`, `FERTIALT_ORDRE_APPORT`, `FERTIALT_NOM_PRODUIT`, `FERTIALT_DOSE`, `FERTIALT_DOSE_P`, `FERTIALT_DOSE_K`, `FERTIALT_PROF_WSOL`, `FERTIALT_AGRIW`, `FERTIALT_OUTIL`, `FERTIALT_TPS_TRAVAIL`, `FERTIALT_N_PASSAGES`, `FERTIALT_OT_SIMULTANEE`, `FERTIALT_N_SOUS_PERIODES`, `FERTIALT_DEBUT` …

**Comparaison vs références :**

- ⊕ **Nouveau / non référencé dans les organigrammes :** Non prévu dans les organigrammes — ajout V1

#### `systemesDeCultureDeReference.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `agri.culture.systemesDeCultureDeReference` |
| Type | `CSV` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si assolementMethod == FONCTIONS_DE_CROYANCE)_ |
| Génération | `MANUAL` |

**Description :** Systemes de culture de reference (rotations).

**Champs (2) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `id_sdc` | String |  | ✅ |  | Identifiant du SDC |
| `rotation` | String |  | ✅ |  | Cultures de la rotation |

#### `matriceDistanceCulturale.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `agri.culture.matriceDistanceCulturale` |
| Type | `CSV` |
| Statut données | ⏳ PENDING |
| Statut champs | ⏳ PENDING |
| Obligatoire | `COND` _(si assolementMethod == FONCTIONS_DE_CROYANCE)_ |
| Génération | `MANUAL` |

**Description :** Matrice de distance entre cultures.

> ⏳ **Champs non encore documentés** — à compléter.

### `Engrais`

#### `Engrais.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `agri.engrais.engrais` |
| Type | `CSV` |
| Statut données | ✅ GROUNDED |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `REQ` |
| Nb enregistrements | 33 |
| Génération | `MANUAL` |

**Description :** Caracteristiques des engrais (matrice : proprietes en lignes, type d'engrais en colonnes).

**Champs (2) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `nom` | String |  | ✅ |  | Propriete de l'engrais (C, N, Norg, ...) |
| `valeurParEngrais` | Double |  |  |  | Valeur par type d'engrais (1 colonne par engrais) |

**Format matrice transposée** _(colonne pivot : `nom`)_ :

- **Entités (35) :** `boue urbaine liquide`, `boue urbaine epaissie chaulee`, `boue papeterie epaissies deshydratees`, `digestat brut`, `digestat liquide`, `digestat solide`, `fumier bovin`, `fumier ovin`, `fumier porc`, `fumier de cheval` …
- **Paramètres (33) :** `C`, `N`, `Norg`, `Nmin`, `CNorg`, `hum`, `K1`, `C2`, `kres1`, `kres2`, `kbio`, `CNbio`, `aCN1`, `Y`, `H` …

**Comparaison vs références :**

- ⊕ **Nouveau / non référencé dans les organigrammes :** Non prévu dans les organigrammes — ajout V1

### `ilots/dansZone`

#### `ilots.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `agri.ilots.ilots` |
| Type | `SHP` |
| Statut données | ✅ GROUNDED |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `REQ` |
| Nb enregistrements | 749 |
| Géométrie | `True` |
| Projection | `WGS_1984_UTM_Zone_28N` |
| Génération | `AUTO` |

**Description :** Ilots dans la zone d'etude (issus du RPG anonymise).

**Champs (12) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_ILOT` | Double |  | ✅ |  | Identifiant de l’ilot _(ex: `1`, `2`, `3`)_ |
| `ID_EXPL` | String |  | ✅ | agri.agriculteurs.exploitations |  _(ex: `SSM1-0001`, `SSM1-0001`, `SSM1-0001`)_ |
| `ID_SOL` | String |  | ✅ | commun.typesDeSol.typeDeSolParZH | Identifiant unique du type de sol par BVe (actuellement composé de l’ID du BVe x _(ex: `SSM1-sableux-dior_cc_avec_arbr`, `SSM1-sableux-dior_cc_sans_arbr`, `SSM1-argileux-dekk_cb_avec_arbr`)_ |
| `ID_ZH` | String |  | ✅ | hydro.zonesHydrographiques.ZH | Identifiant du BVe _(ex: `SSM1`, `SSM1`, `SSM1`)_ |
| `CARACT_IRR` | String |  |  |  | Indique si l’ilot est irrigable (N/O) _(ex: `N`, `N`, `N`)_ |
| `MATERIEL` | Double |  |  |  | Type de matériel d’irrigation affecté à l’ilot _(ex: `0`, `0`, `0`)_ |
| `LISTE_EQUI` | String |  |  |  |  |
| `PENTE_MOY` | Double |  |  |  |  _(ex: `0`, `0`, `0`)_ |
| `PENTE_SWAT` | Double |  |  |  | Pente utilisée côté modèle hydrographique (5 valeurs possibles) _(ex: `0`, `0`, `0`)_ |
| `EQU_0` | String |  |  |  |  |
| `EQU_1` | String |  |  |  |  |
| `EQU_2` | String |  |  |  |  |

**Comparaison vs références :**

- 📊 **Divergence / enrichissement vs schéma Excel :** Schéma Excel nomme `PAE_ID_EXP` → JSON confirme `ID_EXPL`. Colonne `LISTE_EQUS` → JSON confirme `LISTE_EQUI` (tronqué dBASE). Colonnes `EQU_0/1/2` : extra dans JSON/réel, absentes du schéma Excel.

#### `parcelles.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `agri.ilots.parcelles` |
| Type | `SHP` |
| Statut données | ✅ GROUNDED |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `REQ` |
| Nb enregistrements | 749 |
| Géométrie | `True` |
| Projection | `WGS_1984_UTM_Zone_28N` |
| Génération | `AUTO` |

**Description :** Parcelles dans la zone d'etude.

**Champs (9) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_PARCELL` | String |  | ✅ |  | Identifiant de la parcelle, donne également l’indication de l’ilot auquel elle a _(ex: `1_001`, `2_001`, `3_001`)_ |
| `ID_ILOT` | Double |  | ✅ | agri.ilots.ilots | Identifiant de l’ilot _(ex: `1`, `2`, `3`)_ |
| `ID_EXPL` | String |  | ✅ | agri.agriculteurs.exploitations |  _(ex: `SSM1-0001`, `SSM1-0001`, `SSM1-0001`)_ |
| `SEQUENCE` | String | Ha |  |  | Rotation initiale de parcelle _(ex: `mil_arachide_jachere_arachide`, `arachide_mil_jachere`, `arachide_mil_jachere`)_ |
| `POURCENTAG` | Double |  |  |  |  _(ex: `1.000000000000000`, `1.000000000000000`, `1.000000000000000`)_ |
| `INDEX_DEP` | String |  |  |  | Indice de départ dans la SEQUENCE _(ex: `NA`, `NA`, `NA`)_ |
| `CULT_REF` | String |  |  |  | Culture initiale de la parcelle |
| `SURFACE` | Double | Ha |  |  | Surface de la parcelle _(ex: `0.318122421035234`, `0.586761860992035`, `0.104551693142694`)_ |
| `EXPREST` | String |  |  |  |  _(ex: `NA`, `NA`, `NA`)_ |

**Comparaison vs références :**

- 📊 **Divergence / enrichissement vs schéma Excel :** Schéma Excel mentionne `ID_SDC` → JSON confirme `EXPREST` à la place. `CARACT_IRR` : Excel le met dans parcelles, JSON le met dans `ilots.shp`.

### `ilots/horsZone`

#### `ilotsHZ.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `agri.ilots.ilotsHZ` |
| Type | `SHP` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si module.agricole == true)_ |
| Génération | `AUTO` |

**Description :** Ilots hors zone (coherence du modele agriculteur).

**Champs (9) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_ILOT` | Integer |  | ✅ |  | Identifiant de l'ilot |
| `ID_EXPL` | String |  | ✅ | agri.agriculteurs.exploitations | Identifiant de l'exploitation |
| `ID_SOL` | String |  | ✅ | commun.typesDeSol.typeDeSolParZH | Identifiant du type de sol associe |
| `ID_ZH` | String |  | ✅ | hydro.zonesHydrographiques.ZH | Identifiant du BVe associe |
| `CARACT_IRR` | String |  |  |  | Indique si l'ilot est irrigable |
| `MATERIEL` | Integer |  |  | agri.agriculteurs.materiel | Type de materiel d'irrigation affecte a l'ilot |
| `LISTE_EQUI` | String |  |  |  | Identifiants des equipements associes |
| `PENTE_MOY` | Double |  |  |  | Pente moyenne de l'ilot |
| `PENTE_SWAT` | Double |  |  |  | Pente utilisee cote modele hydrographique (5 valeurs possibles) |

#### `parcellesHZ.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `agri.ilots.parcellesHZ` |
| Type | `SHP` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si module.agricole == true)_ |
| Génération | `AUTO` |

**Description :** Parcelles hors zone.

**Champs (8) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_PARCELL` | String |  | ✅ |  | Identifiant de la parcelle (forme id_ilot_xx) |
| `ID_ILOT` | Integer |  | ✅ | agri.ilots.ilots | Ilot d'appartenance |
| `ID_EXPL` | String |  | ✅ | agri.agriculteurs.exploitations | Exploitation d'appartenance |
| `SEQUENCE` | String |  |  |  | Rotation initiale de la parcelle |
| `POURCENTAG` | Double | fraction |  |  | Part de la parcelle dans l'ilot |
| `INDEX_DEP` | Integer |  |  |  | Indice de depart dans la SEQUENCE |
| `CULT_REF` | String |  |  | agri.culture.especesCultivees | Culture initiale de la parcelle |
| `SURFACE` | Double | ha |  |  | Surface de la parcelle |

### `marcheAgricole`

#### `prixVentes(ID).csv`

| Attribut | Valeur |
|----------|--------|
| ID | `agri.marcheAgricole.prixVentes` |
| Type | `CSV` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si module.agricole == true)_ |
| Résolution temporelle | `YEAR` |
| Multi-instance | Oui (un fichier par année/polygone) |
| Génération | `MANUAL` |

**Description :** Prix de vente des cultures par annee.

**Champs (3) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `idEspece` | String |  | ✅ | agri.culture.especesCultivees | Espece |
| `nomIndicatif` | String |  |  |  | Nom indicatif (non utilise) |
| `prix[annee]` | Double | EUR/t | ✅ |  | Prix par annee (colonnes suivantes) |

**Comparaison vs références :**

- ⚠️ **Renommage vs organigramme :** était `N x prixVentes(ID).csv (organigramme)`

#### `chargesOp.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `agri.marcheAgricole.chargesOp` |
| Type | `CSV` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si module.agricole == true)_ |
| Génération | `MANUAL` |

**Description :** Charges operationnelles.

**Champs (3) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_ITK` | String |  | ✅ | agri.culture.reglesDeDecisions | Identifiant unique de l'ITK |
| `nomAffichage` | String |  |  |  | Nom d'affichage de l'ITK (indicatif) |
| `chargesOp[annee]` | Double | euros/ha | ✅ |  | Charges operationnelles hors irrigation par annee |

#### `primes.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `agri.marcheAgricole.primes` |
| Type | `CSV` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si module.agricole == true)_ |
| Génération | `MANUAL` |

**Description :** Primes couplees.

**Champs (4) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `idEspece` | String |  | ✅ | agri.culture.especesCultivees | Identifiant de l'espece cultivee |
| `nomEspece` | String |  |  |  | Nom de l'espece (indicatif) |
| `idDepartement` | String |  |  |  | Identifiant du departement |
| `prime[annee]` | Double | euros/ha | ✅ |  | Prime couplee par annee |

#### `prixEau.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `agri.marcheAgricole.prixEau` |
| Type | `CSV` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si module.agricole == true)_ |
| Génération | `MANUAL` |

**Description :** Prix de l'eau d'irrigation.

**Champs (2) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `natureRessource` | String |  | ✅ |  | Nature de la ressource |
| `prixEau[annee]` | Double | euros/m3 | ✅ |  | Prix de l'eau d'irrigation par annee |

#### `redevanceEau.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `agri.marcheAgricole.redevanceEau` |
| Type | `CSV` |
| Statut données | ⏳ PENDING |
| Statut champs | ⏳ PENDING |
| Obligatoire | `COND` _(si module.agricole == true)_ |
| Génération | `MANUAL` |

**Description :** Redevance sur l'eau.

> ⏳ **Champs non encore documentés** — à compléter.

#### `chargesDePassage.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `agri.marcheAgricole.chargesDePassage` |
| Type | `CSV` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si module.agricole == true)_ |
| Génération | `MANUAL` |

**Description :** Charges fixes de passage.

**Champs (3) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_ITK` | String |  | ✅ | agri.culture.reglesDeDecisions | Identifiant unique de l'ITK |
| `nomAffichage` | String |  |  |  | Nom d'affichage de l'ITK (indicatif) |
| `chargesPassage[annee]` | Double | euros/ha | ✅ |  | Charges de passage hors irrigation par annee |

#### `chargesFixesAccesRessourceIrrigation.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `agri.marcheAgricole.chargesFixesAccesRessourceIrrigation` |
| Type | `CSV` |
| Statut données | ⏳ PENDING |
| Statut champs | ⏳ PENDING |
| Obligatoire | `COND` _(si module.agricole == true && irrigationMode == BLOC)_ |
| Génération | `MANUAL` |

**Description :** Charges fixes d'acces a la ressource d'irrigation (collectif).

> ⏳ **Champs non encore documentés** — à compléter.

#### `chargesFixesMaterielIrrigation.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `agri.marcheAgricole.chargesFixesMaterielIrrigation` |
| Type | `CSV` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si module.agricole == true)_ |
| Génération | `MANUAL` |

**Description :** Charges fixes de materiel d'irrigation.

**Champs (2) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `idMateriel` | String |  | ✅ | agri.agriculteurs.materiel | Identifiant du materiel d'irrigation |
| `chargesFixes[annee]` | Double | euros/materiel | ✅ |  | Charges fixes par materiel d'irrigation par annee |

#### `ASAForfaitDebit.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `agri.marcheAgricole.ASAForfaitDebit` |
| Type | `CSV` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si module.agricole == true && irrigationMode == BLOC)_ |
| Génération | `MANUAL` |

**Description :** ASA - forfait debit.

**Champs (2) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `idASA` | String |  | ✅ |  | Identifiant de l'ASA (collectif d'irrigation) |
| `forfaitDebit[annee]` | Double | euros/(L/s) | ✅ |  | Forfait debit par annee |

#### `ASAForfaitSurface.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `agri.marcheAgricole.ASAForfaitSurface` |
| Type | `CSV` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si module.agricole == true && irrigationMode == BLOC)_ |
| Génération | `MANUAL` |

**Description :** ASA - forfait surface.

**Champs (2) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `idASA` | String |  | ✅ |  | Identifiant de l'ASA (collectif d'irrigation) |
| `forfaitSurface[annee]` | Double | euros/ha | ✅ |  | Forfait surface par annee |

#### `ASAPrixEau.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `agri.marcheAgricole.ASAPrixEau` |
| Type | `CSV` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si module.agricole == true && irrigationMode == BLOC)_ |
| Génération | `MANUAL` |

**Description :** ASA - prix de l'eau.

**Champs (2) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `idASA` | String |  | ✅ |  | Identifiant de l'ASA (collectif d'irrigation) |
| `prixEau[annee]` | Double | euros/m3 | ✅ |  | Prix de l'eau ASA par annee |

#### `rendementsObservesAnterieur.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `agri.marcheAgricole.rendementsObservesAnterieur` |
| Type | `CSV` |
| Statut données | ⏳ PENDING |
| Statut champs | ⏳ PENDING |
| Obligatoire | `COND` _(si module.agricole == true)_ |
| Génération | `MANUAL` |

**Description :** Rendements observes anterieurs (calibration).

> ⏳ **Champs non encore documentés** — à compléter.


---

## Module : NORMATIF

**10 fichiers décrits** — 4 obligatoires, 0 groundés, 3 en attente de documentation.

### `zonesAdministratives`

#### `zonesAdministratives.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `norm.zonesAdministratives.zonesAdministratives` |
| Type | `SHP` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `REQ` |
| Génération | `AUTO` |

**Description :** Zones administratives (rattachees a une station de mesure).

**Champs (2) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_ZA` | String |  | ✅ |  | Identifiant de la zone administrative |
| `ID_STH` | String |  | ✅ | norm.pointsDeReference.pointsDeReference | Station hydrographique |

#### `secteursAdministratifs.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `norm.zonesAdministratives.secteursAdministratifs` |
| Type | `SHP` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `REQ` |
| Génération | `AUTO` |

**Description :** Secteurs administratifs.

**Champs (3) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_SECTEU` | String |  | ✅ |  | Identifiant du secteur |
| `ID_ZA` | String |  | ✅ | norm.zonesAdministratives.zonesAdministratives | Zone administrative |
| `NATURE` | String |  | ✅ |  | Nature (combinable, ex. SURF_CAN) |

#### `seuilsDeRestriction.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `norm.zonesAdministratives.seuilsDeRestriction` |
| Type | `CSV` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si restrictionMethod == SIMPLE)_ |
| Génération | `MANUAL` |

**Description :** Seuils de restriction (methode zones simples).

**Champs (4) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_ZA` | String |  | ✅ | norm.zonesAdministratives.zonesAdministratives | Zone administrative |
| `NomAffichageNiveau` | String |  | ✅ |  | Nom d'affichage du niveau |
| `NiveauRestriction` | Integer |  | ✅ |  | Niveau de restriction (1..N) |
| `Seuil` | Double | m3/s | ✅ |  | Debit seuil |

#### `joursRestrictionSecteurs.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `norm.zonesAdministratives.joursRestrictionSecteurs` |
| Type | `CSV` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si restrictionMethod == COMPLEXE)_ |
| Génération | `MANUAL` |

**Description :** Jours de restriction par secteur et type de materiel.

**Champs (2) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_SECTEU` | String |  | ✅ | norm.zonesAdministratives.secteursAdministratifs | Secteur |
| `joursParMateriel` | String |  |  |  | Colonnes optionnelles par type de materiel (listes de jours) |

### `uniteDeGestion`

#### `UG_region_L93_BGA.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `norm.uniteDeGestion.UG` |
| Type | `SHP` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `REQ` |
| Génération | `AUTO` |

**Description :** Unites de gestion (affectation des volumes prelevables).

**Champs (1) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_UG` | String |  | ✅ |  | Identifiant de l'unite de gestion |

#### `VP_historique.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `norm.uniteDeGestion.VP_historique` |
| Type | `CSV` |
| Statut données | ⏳ PENDING |
| Statut champs | ⏳ PENDING |
| Obligatoire | `COND` _(si module.normatif == true)_ |
| Résolution temporelle | `YEAR` |
| Génération | `MANUAL` |

**Description :** Volume prelevable historique par unite de gestion (genere via SIMULTEAU possible).

> ⏳ **Champs non encore documentés** — à compléter.

### `pointsDeReference`

#### `pointsDeReference.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `norm.pointsDeReference.pointsDeReference` |
| Type | `SHP` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `REQ` |
| Génération | `AUTO` |

**Description :** Stations de mesure (source DREAL).

**Champs (5) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_STH` | String |  | ✅ |  | Identifiant de la station hydrographique |
| `ID_ZH` | String |  | ✅ | hydro.zonesHydrographiques.ZH | BVe associe |
| `DOE` | Double | m3/s | ✅ |  | Debit objectif d'etiage |
| `DCR` | Double | m3/s | ✅ |  | Debit de crise |
| `IS_NODAL` | Boolean |  | ✅ |  | Point nodal |

#### `matriceDebitReelPointDOE.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `norm.pointsDeReference.matriceDebitReelPointDOE` |
| Type | `CSV` |
| Statut données | ⏳ PENDING |
| Statut champs | ⏳ PENDING |
| Obligatoire | `COND` _(si module.normatif == true)_ |
| Génération | `AUTO` |

**Description :** Matrice debit reel vers point DOE.

> ⏳ **Champs non encore documentés** — à compléter.

### `barrages`

#### `barrages.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `norm.barrages.barrages` |
| Type | `CSV` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si module.barrage == true)_ |
| Génération | `MANUAL` |

**Description :** Barrages : priorite, volumes, regles de lacher. Reference par les modules normatif et hydrographique. Lecture par numero de ligne.

**Champs (17) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `Nom` | String |  | ✅ |  | Identifiant unique du barrage |
| `ID_STATION_REF` | String |  | ✅ | norm.pointsDeReference.pointsDeReference | Station de mesure realimentee par le barrage |
| `NOM_Station_ref` | String |  |  |  | Nom de la station (indicatif) |
| `PRIORITE` | Integer |  |  |  | Priorite du barrage (plus petit = prioritaire) |
| `ZH_ENTREE` | String |  | ✅ | hydro.zonesHydrographiques.ZH | Premier BVe recevant le debit du barrage |
| `DATE_LACHER_POSSIBLE` | Integer | jour julien |  |  | Jour de l'annee a partir duquel le premier lacher est possible |
| `JOURS_DECISION_LACHER` | String | jour julien |  |  | Jour(s) de la semaine de decision du soutien d'etiage |
| `V_total` | Double | hm3 |  |  | Volume total du barrage |
| `V_soutien` | Double | hm3 |  |  | Volume maximal alloue au soutien d'etiage |
| `V_critique` | Double | hm3 |  |  | Volume seuil de diminution du debit max de lacher |
| `Q_reserve` | Double | m3/s |  |  | Debit reserve |
| `Qse_max` | Double | m3/s |  |  | Debit maximal de lacher |
| `Qse_critique` | Double | fraction |  |  | Fraction du debit max de lacher sous volume critique |
| `EfficienceEntreeTerritoire` | Double | fraction |  |  | Efficience d'un transfert en entree de territoire |
| `EfficienceDeGestion` | Double | fraction |  |  | Fraction du debit lache atteignant le point DOE |
| `tps_transfert` | Integer | jours |  |  | Temps de transfert jusqu'a l'entree de la zone (min 1 jour) |
| `ID_RETENUES` | String |  |  | hydro.retenuesCollinaires.retenuesParZH | Retenue associee, si sur le territoire |

**Comparaison vs références :**

- 📊 **Divergence / enrichissement vs schéma Excel :** Schéma Excel non détaillé pour ce fichier. JSON documente 17 colonnes complètes (volumes, débits, efficiences, transfert).

### `canaux`

#### `RestrictionsDebitCanaux.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `norm.canaux.restrictionsDebitCanaux` |
| Type | `CSV` |
| Statut données | ⏳ PENDING |
| Statut champs | ⏳ PENDING |
| Obligatoire | `COND` _(si module.normatif == true && hasCanaux == true)_ |
| Génération | `MANUAL` |

**Description :** Restrictions de debit specifiques aux canaux.

> ⏳ **Champs non encore documentés** — à compléter.


---

## Module : HYDROGRAPHIQUE

**23 fichiers décrits** — 10 obligatoires, 3 groundés, 6 en attente de documentation.

### `zonesHydrographiques`

#### `ZH.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `hydro.zonesHydrographiques.ZH` |
| Type | `SHP` |
| Statut données | ✅ GROUNDED |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `REQ` |
| Nb enregistrements | 1 |
| Géométrie | `True` |
| Projection | `WGS_1984_UTM_Zone_28N` |
| Génération | `AUTO` |

**Description :** Bassins versants elementaires (BVe).

**Champs (1) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_ZH` | Double |  | ✅ |  | Identifiant du BVe _(ex: `1`)_ |

#### `contourZH.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `hydro.zonesHydrographiques.contourZH` |
| Type | `SHP` |
| Statut données | ✅ GROUNDED |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `REQ` |
| Nb enregistrements | 1 |
| Géométrie | `True` |
| Projection | `WGS_1984_UTM_Zone_28N` |
| Génération | `AUTO` |

**Description :** Contour des BVe.

**Champs (3) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `Code_Zone` | String |  | ✅ |  |  _(ex: `SSM1`)_ |
| `Surface` | Double | Ha |  |  | Surface de la parcelle _(ex: `2254072.350763372611254`)_ |
| `Area_ha` | Double |  |  |  |  _(ex: `225.407235076337258`)_ |

#### `donneesMNT_ZH.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `hydro.zonesHydrographiques.donneesMNT_ZH` |
| Type | `CSV` |
| Statut données | ✅ GROUNDED |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `REQ` |
| Nb enregistrements | 1 |
| Génération | `MANUAL` |

**Description :** Caracteristiques du cours d'eau / MNT par BVe (phase routage SWAT).

**Champs (9) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `No. Subbasin SWAT` | String |  | ✅ |  |  |
| `No. ZH MAELIA` | Integer |  |  |  |  _(ex: `1`)_ |
| `W_bnkful (CH_W2)` | String |  |  |  |  |
| `depth_bnkful (CH_D)` | String |  |  |  |  |
| `slp_ch (CH_S2)` | String |  |  |  |  |
| `L_Ch (CH_L2)` | String |  |  |  |  |
| `L_slp.zh (SLSUBBSN)` | String |  |  |  |  |
| `CH_S1` | String | m/m |  |  | Average slope of tributary channels |
| `CH_W1` | String | m |  |  | Average width of tributary channel |

**Comparaison vs références :**

- 📊 **Divergence / enrichissement vs schéma Excel :** JSON confirme la structure SWAT (9 colonnes). `rowCount=1` mais toutes valeurs NA dans SASSEME V1 — paramètres SWAT à calibrer.

#### `debitEntre.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `hydro.zonesHydrographiques.debitEntre` |
| Type | `CSV` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si module.barrage == false)_ |
| Résolution temporelle | `DAY` |
| Génération | `MANUAL` |

**Description :** Debit d'entree aux points amont (source Banque HYDRO).

**Champs (2) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `date` | Date | j/m/aaaa | ✅ |  | Date |
| `debit[point]` | Double | L/s | ✅ |  | Debit par point d'entree (colonnes suivantes) |

**Comparaison vs références :**

- ⊕ **Nouveau / non référencé dans les organigrammes :** Non visible dans les organigrammes fournis

#### `debitEntreObs.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `hydro.zonesHydrographiques.debitEntreObs` |
| Type | `CSV` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si module.barrage == false)_ |
| Résolution temporelle | `DAY` |
| Génération | `MANUAL` |

**Description :** Debit d'entree observe (utilise s'il existe et si module barrage non active).

**Champs (2) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `date` | Date | j/m/aaaa | ✅ |  | Date du jour |
| `debit[pointEntree]` | Double | L/s | ✅ |  | Debit observe par point d'entree (1 colonne par point) |

**Comparaison vs références :**

- ⊕ **Nouveau / non référencé dans les organigrammes :** Non visible dans les organigrammes fournis

#### `zhDebitForce.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `hydro.zonesHydrographiques.zhDebitForce` |
| Type | `CSV` |
| Statut données | ⏳ PENDING |
| Statut champs | ⏳ PENDING |
| Obligatoire | `COND` _(si module.hydrographique == true)_ |
| Résolution temporelle | `DAY` |
| Génération | `MANUAL` |

**Description :** Debit force par BVe.

> ⏳ **Champs non encore documentés** — à compléter.

**Comparaison vs références :**

- ⊕ **Nouveau / non référencé dans les organigrammes :** Non visible dans les organigrammes fournis

### `troncons`

#### `tronconsParZH.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `hydro.troncons.tronconsParZH` |
| Type | `SHP` |
| Statut données | ⏳ PENDING |
| Statut champs | ⏳ PENDING |
| Obligatoire | `REQ` |
| Génération | `AUTO` |

**Description :** Tronçons hydrographiques par BVe (BD CARTHAGE).

> ⏳ **Champs non encore documentés** — à compléter.

#### `tronconsPrincipauxParZH.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `hydro.troncons.tronconsPrincipauxParZH` |
| Type | `SHP` |
| Statut données | ⏳ PENDING |
| Statut champs | ⏳ PENDING |
| Obligatoire | `REQ` |
| Génération | `AUTO` |

**Description :** Tronçon principal par BVe (routage SWAT).

> ⏳ **Champs non encore documentés** — à compléter.

### `hru`

#### `hru_0.25.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `hydro.hru.hru_025` |
| Type | `SHP` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `REQ` |
| Génération | `AUTO` |

**Description :** HRU (hydrologie seule, couvre tout le territoire).

**Champs (7) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_HRU` | String |  | ✅ |  | Identifiant de la HRU |
| `ID_ZH` | String |  | ✅ | hydro.zonesHydrographiques.ZH | BVe |
| `ID_CLC` | String |  | ✅ |  | Couvert (CLC) |
| `ID_SOL` | String |  | ✅ | commun.typesDeSol.typeDeSolParZH | Sol |
| `ID_PENTE` | Double | % | ✅ |  | Classe de pente |
| `SURFACE` | Double | m2 | ✅ |  | Surface |
| `FRACTION` | Double |  | ✅ |  | Fraction de la HRU dans son BVe |

#### `hruSansIlots_0.25.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `hydro.hru.hruSansIlots_025` |
| Type | `SHP` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ⚠️ VERIFIED_PARTIAL |
| Obligatoire | `COND` _(si module.agricole == true)_ |
| Génération | `AUTO` |

**Description :** HRU avec trous aux emplacements des ilots (couplage hydrologie + agricole).

**Champs (7) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_HRU` | String |  | ✅ |  | Identifiant de la HRU |
| `ID_ZH` | String |  | ✅ | hydro.zonesHydrographiques.ZH | BVe |
| `ID_CLC` | String |  | ✅ |  | Couvert (CLC) |
| `ID_SOL` | String |  | ✅ | commun.typesDeSol.typeDeSolParZH | Sol |
| `ID_PENTE` | Double | % | ✅ |  | Classe de pente |
| `SURFACE` | Double | m2 | ✅ |  | Surface |
| `FRACTION` | Double |  | ✅ |  | Fraction de la HRU dans son BVe |

### `nappes`

#### `nappeParZH.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `hydro.nappes.nappeParZH` |
| Type | `SHP` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ⚠️ VERIFIED_PARTIAL |
| Obligatoire | `REQ` |
| Génération | `AUTO` |

**Description :** Nappes d'accompagnement par BVe (BDRHF/BRGM).

**Champs (2) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_RESSOUR` | String |  | ✅ |  | Identifiant de la ressource en eau |
| `ID_ZH` | String |  | ✅ | hydro.zonesHydrographiques.ZH | Identifiant du BVe associe |

### `retenuesCollinaires`

#### `retenuesParZH.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `hydro.retenuesCollinaires.retenuesParZH` |
| Type | `SHP` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `REQ` |
| Génération | `AUTO` |

**Description :** Retenues collinaires par BVe (BD TOPO/IGN).

**Champs (9) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_RESSOUR` | String |  | ✅ |  | Identifiant de la ressource en eau |
| `ID_ZH` | String |  | ✅ | hydro.zonesHydrographiques.ZH | Identifiant du BVe associe |
| `FRACTIONDRAIN` | Double | [0-1] |  |  | Fraction de l'impluvium couvrant le BVe |
| `VOLMAX` | Double | m3 |  |  | Volume maximal de la retenue |
| `SURFACERET` | Double | m2 |  |  | Surface de la retenue |
| `TYPEOFRET` | String |  |  |  | Type de la retenue |
| `TYPEDEDRAI` | String |  |  |  | Type de drain |
| `Q_RESERVE` | Double | m3/s |  |  | Debit reserve |
| `ORDREDRAIN` | Integer |  |  |  | Numero d'ordre de la retenue (cours d'eau principal) |

### `clc`

#### `clcParZH.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `hydro.clc.clcParZH` |
| Type | `SHP` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `REQ` |
| Génération | `AUTO` |

**Description :** Corine Land Cover par BVe.

**Champs (4) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_CLC` | String |  | ✅ |  | Identifiant CLC |
| `ID_ZH` | String |  | ✅ | hydro.zonesHydrographiques.ZH | BVe |
| `TYPE_CLC` | String |  | ✅ |  | Type (agricole/bati/foret...) |
| `INDICE_CLC` | String |  | ✅ |  | Indice CLC |

#### `clcRPGParZH.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `hydro.clc.clcRPGParZH` |
| Type | `SHP` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si module.agricole == true)_ |
| Génération | `AUTO` |

**Description :** CLC croise RPG par BVe.

**Champs (4) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_CLC` | String |  | ✅ |  | Identifiant du couvert par BVe |
| `ID_ZH` | String |  | ✅ | hydro.zonesHydrographiques.ZH | BVe de rattachement |
| `TYPE_CLC` | String |  |  |  | Type du couvert (agricole, bati, foret, etc.) |
| `INDICE_CLC` | String |  |  |  | Indice du couvert |

### `canaux`

#### `canaux.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `hydro.canaux.canaux_shp` |
| Type | `SHP` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ⚠️ VERIFIED_PARTIAL |
| Obligatoire | `COND` _(si hasCanaux == true)_ |
| Génération | `AUTO` |

**Description :** Canaux - geometrie (Saint-Martory, Neste pour Garonne-Amont).

**Champs (3) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `Nom_Canal` | String |  |  |  | Nom du canal (affichage des sorties) |
| `ID_Canal` | String |  | ✅ |  | Identifiant de la ressource (canal) |
| `BVe_origine` | String |  |  | hydro.zonesHydrographiques.ZH | BVe de depart du canal |

#### `canaux.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `hydro.canaux.canaux_csv` |
| Type | `CSV` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si hasCanaux == true)_ |
| Génération | `MANUAL` |

**Description :** Canaux - caracteristiques hydrauliques.

**Champs (11) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `Nom_Canal` | String |  |  |  | Nom du canal (affichage des sorties) |
| `ID_Canal` | String |  | ✅ |  | Identifiant de la ressource (canal) |
| `BVe_origine` | String |  | ✅ | hydro.zonesHydrographiques.ZH | BVe de depart et de prelevement |
| `dataObs` | String |  |  |  | Fichier des prelevements journaliers observes (m3/s) |
| `NoDataHiver` | Double | m3/s |  |  | Debit de prelevement en l'absence de donnee |
| `idPointRef` | String |  |  |  | Station de mesure pour la periode de predictions |
| `BVeAvantObs` | String |  |  | hydro.zonesHydrographiques.ZH | BVe pour estimer la moyenne des prelevements |
| `debitmax` | Double | m3/s |  |  | Debit maximum autorise du canal |
| `BVe_retourX` | String |  |  | hydro.zonesHydrographiques.ZH | BVe du/des point(s) de rejet |
| `fractionEteX` | Double |  |  |  | Fraction du prelevement relachee en ete (1 juin-31 oct) |
| `fractionHiverX` | Double |  |  |  | Fraction du prelevement relachee en hiver/sans donnee (1 nov-31 mai) |

#### `NomCanal.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `hydro.canaux.nomCanal` |
| Type | `CSV` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si hasCanaux == true)_ |
| Résolution temporelle | `DAY` |
| Multi-instance | Oui (un fichier par année/polygone) |
| Génération | `MANUAL` |

**Description :** Forcage journalier par canal.

**Champs (2) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_Canal` | String |  | ✅ | hydro.canaux.canaux_csv | Identifiant du canal |
| `Nom_Canal` | String |  |  |  | Nom du canal (affichage) |

**Comparaison vs références :**

- ⚠️ **Renommage vs organigramme :** était `N x NomCanal.csv (organigramme)`

### `equipements/pointsDePrelevement/irr`

#### `ppIrr.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `hydro.equipements.ppIrr` |
| Type | `SHP` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `REQ` |
| Génération | `AUTO` |

**Description :** Points de prelevement irrigation.

**Champs (3) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_EQU` | String |  | ✅ |  | Identifiant de l'equipement |
| `CODE_INSEE` | String |  | ✅ | commun.communes.communesTrimUG | Commune de rattachement |
| `TAUX` | Double |  | ✅ |  | Importance du point dans la zone |

**Comparaison vs références :**

- ⚠️ **Renommage vs organigramme :** était `pplrr.shp (organigramme)`

### `equipements/pointsDePrelevement/aep`

#### `ppAep.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `hydro.equipements.ppAep` |
| Type | `SHP` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si module.usages == true)_ |
| Génération | `AUTO` |

**Description :** Points de prelevement AEP.

**Champs (6) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_EQU` | String |  | ✅ |  | Identifiant de l'equipement |
| `CODE_INSEE` | String |  |  |  | Commune rattachee (code INSEE) |
| `TAUX` | Double | % |  |  | Importance du point dans la zone Garonne-Amont |
| `ID_RESS_ZH` | String |  |  | hydro.zonesHydrographiques.ZH | Identifiant de la ressource prelevee (BVe) |
| `ID_RESSOUR` | String |  |  |  | Identifiant precis de la ressource prelevee (non utilise) |
| `NATURE` | String |  |  |  | Nature de la ressource prelevee |

### `equipements/pointsDePrelevement/ind`

#### `ppInd.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `hydro.equipements.ppInd` |
| Type | `SHP` |
| Statut données | 📋 SCHEMA ONLY |
| Statut champs | ✅ VERIFIED |
| Obligatoire | `COND` _(si module.usages == true)_ |
| Génération | `AUTO` |

**Description :** Points de prelevement industriel.

**Champs (6) :**

| Label | Type | Unité | Requis | Référence | Description |
|-------|------|-------|--------|-----------|-------------|
| `ID_EQU` | String |  | ✅ |  | Identifiant de l'equipement |
| `CODE_INSEE` | String |  |  |  | Commune rattachee (code INSEE) |
| `TAUX` | Double | % |  |  | Importance du point dans la zone Garonne-Amont |
| `ID_RESS_ZH` | String |  |  | hydro.zonesHydrographiques.ZH | Identifiant de la ressource prelevee (BVe) |
| `ID_RESSOUR` | String |  |  |  | Identifiant precis de la ressource prelevee (non utilise) |
| `NATURE` | String |  |  |  | Nature de la ressource prelevee |

**Comparaison vs références :**

- ⚠️ **Renommage vs organigramme :** était `pplInd.shp (organigramme)`

#### `volumeRefAnnuelIND.csv`

| Attribut | Valeur |
|----------|--------|
| ID | `hydro.equipements.volumeRefAnnuelIND` |
| Type | `CSV` |
| Statut données | ⏳ PENDING |
| Statut champs | ⏳ PENDING |
| Obligatoire | `COND` _(si module.usages == true)_ |
| Résolution temporelle | `YEAR` |
| Génération | `AUTO` |

**Description :** Volume de reference annuel industriel.

> ⏳ **Champs non encore documentés** — à compléter.

### `equipements/pointsDeRejet/aep`

#### `rjAEP.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `hydro.equipements.rjAEP` |
| Type | `SHP` |
| Statut données | ⏳ PENDING |
| Statut champs | ⏳ PENDING |
| Obligatoire | `COND` _(si module.usages == true)_ |
| Génération | `AUTO` |

**Description :** Points de rejet AEP.

> ⏳ **Champs non encore documentés** — à compléter.

### `equipements/pointsDeRejet/ind`

#### `rjI.shp`

| Attribut | Valeur |
|----------|--------|
| ID | `hydro.equipements.rjI` |
| Type | `SHP` |
| Statut données | ⏳ PENDING |
| Statut champs | ⏳ PENDING |
| Obligatoire | `COND` _(si module.usages == true)_ |
| Génération | `AUTO` |

**Description :** Points de rejet industriel.

> ⏳ **Champs non encore documentés** — à compléter.


---

## Synthèse des divergences JSON vs références

### Renommages confirmés par le JSON

| Nom dans organigramme/Excel | Nom canonique dans JSON | Nature |
|----------------------------|------------------------|--------|
| `N x prixVentes(ID).csv` | `prixVentes(ID).csv` | Simplification du préfixe `N x` |
| `N x NomCanal.csv` | `NomCanal.csv` | Idem |
| `pplrr.shp` | `ppIrr.shp` | Harmonisation de la convention `pp` |
| `pplInd.shp` | `ppInd.shp` | Idem |
| `polygonesMeteoProjecttes.shp` (typo) | `polygonesMeteoProjettes.shp` | Correction orthographique |
| `nce_principale_maelia-compl…` (tronqué) | `residence_principale_maelia-complet.csv` | Nom complet rétabli |
| `PAE_ID_EXP` (schéma Excel `ilots`) | `ID_EXPL` (DBF + JSON) | Harmonisation colonne |
| `LISTE_EQUS` (schéma Excel) | `LISTE_EQUI` (DBF + JSON) | Tronqué à 10 car. dBASE |
| `anneeBissextile` (schéma Excel) | `anneeBessextile` (JSON + fichier) | Faute de frappe — à corriger |
| `SIJ (ha/jr)` / `travail (h/jr)` (Excel) | `surfaceIrrigableJour` / `tempsTravailJour` | Normalisation noms de colonnes |

### Fichiers présents dans le JSON, absents des organigrammes

| Fichier | Module | Statut | Remarque |
|---------|--------|--------|----------|
| `debitEntre.csv` | HYDROGRAPHIQUE | 📋 SCHEMA ONLY | Débit entrant simulé par date et point |
| `debitEntreObs.csv` | HYDROGRAPHIQUE | 📋 SCHEMA ONLY | Débit entrant observé |
| `zhDebitForce.csv` | HYDROGRAPHIQUE | ⏳ PENDING | Débit forcé — champs à documenter |
| `Engrais.csv` | AGRICOLE | ✅ GROUNDED | Ajout V1 — 35 engrais, 33 paramètres |
| `reglesDeDecisions_fertilisation.csv` | AGRICOLE | ✅ GROUNDED | Ajout V1 — matrice fertilisation 36 paramètres |

### Fichiers avec champs PENDING (à documenter en priorité)

| Fichier | Module | Requis | Impact |
|---------|--------|--------|--------|
| `tronconsParZH.shp` | HYDROGRAPHIQUE | `REQ` | 🔴 Bloquant — réseau hydrographique principal |
| `tronconsPrincipauxParZH.shp` | HYDROGRAPHIQUE | `REQ` | 🔴 Bloquant |
| `departement.shp` | COMMUN | `COND` | 🟡 Requis si module communes activé |
| `VP_historique.csv` | NORMATIF | `COND` | 🟡 Historique volumes prélevés |
| `matriceDebitReelPointDOE.csv` | NORMATIF | `COND` | 🟡 Débits réels aux points DOE |
| `RestrictionsDebitCanaux.csv` | NORMATIF | `COND` | 🟡 Restrictions canaux |
| `zhDebitForce.csv` | HYDROGRAPHIQUE | `COND` | 🟡 Débit forcé ZH |
| `volumeRefAnnuelIND.csv` | HYDROGRAPHIQUE | `COND` | 🟡 Volumes ref industrie |
| `rjAEP.shp` | HYDROGRAPHIQUE | `COND` | 🟡 Points rejet AEP |
| `rjI.shp` | HYDROGRAPHIQUE | `COND` | 🟡 Points rejet industrie |
| `matriceDistanceCulturale.csv` | AGRICOLE | `COND` | 🟡 Distance culturale entre rotations |
| `redevanceEau.csv` | AGRICOLE | `COND` | 🟡 Redevance eau par ressource |
| `chargesFixesAccesRessourceIrrigation.csv` | AGRICOLE | `COND` | 🟡 Charges fixes accès ressource |
| `rendementsObservesAnterieur.csv` | AGRICOLE | `COND` | 🟡 Rendements historiques |

---

## Conclusions

### Ce que le JSON apporte par rapport aux références (organigrammes + Excel)

1. **Schéma exhaustif et structuré** : 71 fichiers décrits avec id, type, champs typés, unités, références croisées (`referencesDataSpec`) et exemples de valeurs (`samples`) — le schéma Excel n'offre pas ce niveau de détail.
2. **Grounding vérifié** : 14 fichiers ont leur structure confirmée par les données réelles du ZIP SASSEME V1, avec `rowCount` et échantillons.
3. **Matrices transposées documentées** : `reglesDeDecisions.csv` (200+ paramètres, 8 ITK), `reglesDeDecisions_fertilisation.csv` (36 paramètres), `Engrais.csv` (33 paramètres) — absentes du schéma Excel.
4. **Renommages confirmés** : le JSON tranche les ambiguïtés de nommage entre organigrammes et données réelles (`ppIrr`, `ppInd`, noms simplifiés).
5. **Fichiers nouveaux identifiés** : 5 fichiers non visibles dans les organigrammes sont maintenant documentés.
6. **Conditions d'activation explicites** : le champ `requiredIf` indique clairement sous quelle option de modélisation chaque fichier devient obligatoire.

### Points d'action restants

1. **Documenter les 14 fichiers PENDING** — en priorité les tronçons hydrographiques (`REQ`) bloquants pour la simulation SWAT.
2. **Corriger la faute de frappe** `anneeBessextile` → `anneeBissextile` dans `joursParMois.csv` et le JSON.
3. **Mettre à jour les organigrammes** pour intégrer les 5 nouveaux fichiers et corriger les renommages.
4. **Renseigner `donneesMNT_ZH.csv`** avec les paramètres SWAT calibrés (actuellement toutes valeurs NA).
5. **Vérifier la colonne `EXPREST`** dans `parcelles.dbf` — son rôle par rapport à l'ancien `ID_SDC` n'est pas encore documenté.
6. **Ajouter des données dans `materiel.csv`** — le fichier est vide (`rowCount=0`) dans SASSEME V1.

---

_Analyse générée sur la base de : `maelia-database.json` (2026-06-28), `MAELIA_Schema_Donnees.xlsx`, et les 5 organigrammes fournis._