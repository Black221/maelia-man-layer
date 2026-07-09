import json

with open('maelia-database.json', "r", encoding='utf-8') as f:
    data = json.load(f)

ds = data['dataSpecs']
grounded_ids = set(data.get('groundedIds', []))
pending_ids   = set(data.get('pendingIds', []))

# Build per-module groups
from collections import defaultdict
by_module = defaultdict(list)
for item in ds:
    entry = dict(item)
    entry['is_grounded'] = item['id'] in grounded_ids
    entry['is_pending']  = item['id'] in pending_ids
    by_module[item['module']].append(entry)

# Helper: format fields list
def fmt_fields(fields):
    if not fields:
        return '_aucun champ documenté_'
    return ', '.join(f'`{f.get("label","?")}`' for f in fields)

def req_tag(item):
    if item.get('required'):
        return '`REQ`'
    elif item.get('requiredIf'):
        return f'`COND` _(si {item["requiredIf"]})_'
    return '`OPT`'

def status_tag(item):
    if item['is_grounded']:
        return '✅ GROUNDED'
    elif item['is_pending']:
        return '⏳ PENDING'
    return '📋 SCHEMA ONLY'

def fields_status_tag(fs):
    if fs == 'VERIFIED':
        return '✅ VERIFIED'
    elif fs == 'VERIFIED_PARTIAL':
        return '⚠️ VERIFIED_PARTIAL'
    elif fs == 'PENDING':
        return '⏳ PENDING'
    return fs or '—'

md = []

md.append("# Analyse comparative : `maelia-database.json` vs Organigrammes & Schéma Excel MAELIA")
md.append("")
md.append("> **Références :** Organigrammes (OrganigrammeAll, ModeleAgricole, ModeleCommun, ModeleHydrographique, ModeleNormatif) + `MAELIA_Schema_Donnees.xlsx`  ")
md.append("> **Source analysée :** `maelia-database.json` — généré le 2026-06-28, zone d'étude : `garonne-amont`, basé sur SASSEME V1")
md.append("")

md.append("---")
md.append("")
md.append("## Résumé global")
md.append("")
md.append("| Dimension | Valeur |")
md.append("|-----------|--------|")
md.append(f"| Total dataSpecs dans le JSON | **71** |")
md.append(f"| Modules couverts | COMMUN (12), AGRICOLE (26), NORMATIF (10), HYDROGRAPHIQUE (23) |")
md.append(f"| Fichiers grounded (vérifiés sur données réelles) | **14** |")
md.append(f"| Fichiers schema only (documentés, pas encore vérifiés) | **43** |")
md.append(f"| Fichiers pending (champs non encore documentés) | **14** |")
md.append("")
md.append("### Légende des statuts")
md.append("")
md.append("| Symbole | Signification |")
md.append("|---------|--------------|")
md.append("| ✅ GROUNDED | Fichier vérifié sur données réelles (ZIP SASSEME V1) — colonnes + échantillons confirmés |")
md.append("| 📋 SCHEMA ONLY | Documenté dans le JSON (champs définis), mais pas de données réelles disponibles |")
md.append("| ⏳ PENDING | Fichier référencé mais champs non encore documentés — à compléter |")
md.append("| `REQ` | Fichier obligatoire pour toute simulation |")
md.append("| `COND` | Obligatoire sous condition (option de modélisation activée) |")
md.append("| `OPT` | Optionnel |")
md.append("")
md.append("### Comparaison JSON vs Organigrammes/Excel — vue d'ensemble")
md.append("")
md.append("| Aspect | Organigrammes / Excel (référence) | JSON (`maelia-database.json`) |")
md.append("|--------|-----------------------------------|-------------------------------|")
md.append("| Nombre total de fichiers décrits | ~68 (estimation organigrammes) | **71** |")
md.append("| Modèle normatif | Présent dans organigramme, **absent du ZIP** | Présent et documenté (10 specs) |")
md.append("| Nommage `pplrr.shp` / `pplInd.shp` | Organigramme : `pplrr.shp`, `pplInd.shp` | JSON : **`ppIrr.shp`**, **`ppInd.shp`** (renommés) |")
md.append("| Nommage `N x prixVentes(ID).csv` | Organigramme : `N x prixVentes(ID).csv` | JSON : **`prixVentes(ID).csv`** (simplifié) |")
md.append("| Nommage `N x NomCanal.csv` | Organigramme : `N x NomCanal.csv` | JSON : **`NomCanal.csv`** (simplifié) |")
md.append("| Fichiers hydro ZH extra | Non visibles dans organigrammes | `debitEntre.csv`, `debitEntreObs.csv`, `zhDebitForce.csv` (**3 nouveaux**) |")
md.append("| `reglesDeDecisions.csv` | Excel : structure vague | JSON : **matrice complète** (200+ paramètres, 8 ITK) |")
md.append("| `Engrais.csv` | Non prévu dans organigrammes | JSON : `REQ`, 33 engrais, 33 paramètres (ETM, coûts, CO₂…) |")
md.append("| `reglesDeDecisions_fertilisation.csv` | Non prévu dans organigrammes | JSON : `REQ`, matrice 36 paramètres |")
md.append("| `residence_principale_maelia-complet.csv` | Tronqué dans organigramme (`nce_principale…`) | JSON : nom complet rétabli |")
md.append("| `polygonesMeteoProjettes.shp` | `polygonesMeteoProjecttes.shp` (typo) | JSON : **`polygonesMeteoProjettes.shp`** (corrigé) |")
md.append("| Champs non encore documentés | N/A | 14 fichiers `PENDING` à compléter |")
md.append("")
md.append("---")
md.append("")

# MODULES
for mod_name in ['COMMUN', 'AGRICOLE', 'NORMATIF', 'HYDROGRAPHIQUE']:
    items = by_module[mod_name]
    req_count  = sum(1 for i in items if i.get('required'))
    grnd_count = sum(1 for i in items if i['is_grounded'])
    pend_count = sum(1 for i in items if i['is_pending'])

    md.append(f"---")
    md.append("")
    md.append(f"## Module : {mod_name}")
    md.append("")
    md.append(f"**{len(items)} fichiers décrits** — {req_count} obligatoires, {grnd_count} groundés, {pend_count} en attente de documentation.")
    md.append("")

    # Group by folder
    folder_groups = defaultdict(list)
    for item in items:
        folder_groups[item['folder']].append(item)

    for folder, fitems in folder_groups.items():
        short_folder = folder.replace(f'modele{mod_name.capitalize()}/', '').replace('modeleCommun/', '')
        md.append(f"### `{short_folder}`")
        md.append("")
        for item in fitems:
            fname = item['fileName']
            ftype = item['fileType']
            desc  = item.get('description', '')
            fields = item.get('fields', [])
            matrix = item.get('matrix')
            geom  = item.get('geometry', '')
            proj  = item.get('projection', '')
            row_count = item.get('rowCount')
            temporal  = item.get('temporalResolution', '')
            gen       = item.get('generation', '')
            multi     = item.get('multiInstance', False)
            req_if    = item.get('requiredIf')

            md.append(f"#### `{fname}`")
            md.append("")
            md.append(f"| Attribut | Valeur |")
            md.append(f"|----------|--------|")
            md.append(f"| ID | `{item['id']}` |")
            md.append(f"| Type | `{ftype}` |")
            md.append(f"| Statut données | {status_tag(item)} |")
            md.append(f"| Statut champs | {fields_status_tag(item.get('fieldsStatus',''))} |")
            md.append(f"| Obligatoire | {req_tag(item)} |")
            if row_count is not None:
                md.append(f"| Nb enregistrements | {row_count} |")
            if temporal and temporal != 'NONE':
                md.append(f"| Résolution temporelle | `{temporal}` |")
            if multi:
                md.append(f"| Multi-instance | Oui (un fichier par année/polygone) |")
            if geom:
                md.append(f"| Géométrie | `{geom}` |")
            if proj:
                md.append(f"| Projection | `{proj}` |")
            if gen:
                md.append(f"| Génération | `{gen}` |")
            md.append("")

            if desc:
                md.append(f"**Description :** {desc}")
                md.append("")

            # Fields table
            if fields:
                md.append(f"**Champs ({len(fields)}) :**")
                md.append("")
                md.append("| Label | Type | Unité | Requis | Référence | Description |")
                md.append("|-------|------|-------|--------|-----------|-------------|")
                for f in fields:
                    lbl  = f.get('label', '?')
                    typ  = f.get('infoType', '')
                    unit = f.get('unit', '') or ''
                    req  = '✅' if f.get('required') else ''
                    ref  = f.get('referencesDataSpec', '') or ''
                    dsc  = (f.get('description', '') or '').replace('|', '\\|')[:80]
                    samples = f.get('samples', [])
                    samp_str = ', '.join(f'`{s}`' for s in samples[:3]) if samples else ''
                    desc_cell = dsc + (f' _(ex: {samp_str})_' if samp_str else '')
                    md.append(f"| `{lbl}` | {typ} | {unit} | {req} | {ref} | {desc_cell} |")
                md.append("")
            elif item.get('fieldsStatus') == 'PENDING':
                md.append(f"> ⏳ **Champs non encore documentés** — à compléter.")
                md.append("")

            # Matrix
            if matrix:
                params = matrix.get('parameters', [])
                entities = matrix.get('entities', [])
                param_col = matrix.get('parameterColumn', '')
                md.append(f"**Format matrice transposée** _(colonne pivot : `{param_col}`)_ :")
                md.append("")
                md.append(f"- **Entités ({matrix.get('entityCount', len(entities))}) :** {', '.join(f'`{e}`' for e in entities[:10])}" + (' …' if len(entities) > 10 else ''))
                md.append(f"- **Paramètres ({len(params)}) :** {', '.join(f'`{p}`' for p in params[:15])}" + (' …' if len(params) > 15 else ''))
                md.append("")

            # Comparaison vs référence
            # Build comparison notes
            notes = []

            # Organigramme vs JSON name checks
            organi_renames = {
                'prixVentes(ID).csv': 'N x prixVentes(ID).csv (organigramme)',
                'NomCanal.csv': 'N x NomCanal.csv (organigramme)',
                'ppIrr.shp': 'pplrr.shp (organigramme)',
                'ppInd.shp': 'pplInd.shp (organigramme)',
                'polygonesMeteoProjettes.shp': 'polygonesMeteoProjecttes.shp (organigramme/typo)',
                'residence_principale_maelia-complet.csv': 'nce_principale_maelia-compl… (tronqué dans organigramme)',
            }
            if fname in organi_renames:
                notes.append(f"⚠️ **Renommage vs organigramme :** était `{organi_renames[fname]}`")

            new_in_json = {
                'debitEntre.csv': 'Non visible dans les organigrammes fournis',
                'debitEntreObs.csv': 'Non visible dans les organigrammes fournis',
                'zhDebitForce.csv': 'Non visible dans les organigrammes fournis',
                'Engrais.csv': 'Non prévu dans les organigrammes — ajout V1',
                'reglesDeDecisions_fertilisation.csv': 'Non prévu dans les organigrammes — ajout V1',
                'exploitations.csv': "Organigramme référence `profilesAgriculteurs.csv` — JSON confirme `exploitations.csv` comme fichier réel",
            }
            if fname in new_in_json:
                notes.append(f"⊕ **Nouveau / non référencé dans les organigrammes :** {new_in_json[fname]}")

            # Excel schema discrepancies
            excel_notes = {
                'ilots.shp': "Schéma Excel nomme `PAE_ID_EXP` → JSON confirme `ID_EXPL`. Colonne `LISTE_EQUS` → JSON confirme `LISTE_EQUI` (tronqué dBASE). Colonnes `EQU_0/1/2` : extra dans JSON/réel, absentes du schéma Excel.",
                'parcelles.shp': "Schéma Excel mentionne `ID_SDC` → JSON confirme `EXPREST` à la place. `CARACT_IRR` : Excel le met dans parcelles, JSON le met dans `ilots.shp`.",
                'materiel.csv': "Schéma Excel : colonnes `SIJ (ha/jr)` et `travail (h/jr)`. JSON normalise en `surfaceIrrigableJour` et `tempsTravailJour`. `rowCount=0` : fichier vide dans SASSEME V1.",
                'donneesMNT_ZH.csv': "JSON confirme la structure SWAT (9 colonnes). `rowCount=1` mais toutes valeurs NA dans SASSEME V1 — paramètres SWAT à calibrer.",
                'typeDeSolParZH.shp': "Schéma Excel utilise `PRO_OC`, `ARG_OC`, `DAH_OC` → JSON confirme noms courts `PRO`, `ARG1/2`, `DAH1/2` issus du DBF réel.",
                'joursParMois.csv': "Schéma Excel orthographie `anneeBissextile` → JSON (et fichier réel) utilisent `anneeBessextile` (faute de frappe à corriger).",
                'especesCultivees.csv': "Schéma Excel décrit 45 colonnes regroupées, JSON liste 21 labels canoniques (`ALPHA*`, `KC*` regroupant plusieurs colonnes).",
                'barrages.csv': "Schéma Excel non détaillé pour ce fichier. JSON documente 17 colonnes complètes (volumes, débits, efficiences, transfert).",
            }
            if fname in excel_notes:
                notes.append(f"📊 **Divergence / enrichissement vs schéma Excel :** {excel_notes[fname]}")

            if notes:
                md.append("**Comparaison vs références :**")
                md.append("")
                for n in notes:
                    md.append(f"- {n}")
                md.append("")

    md.append("")

# Final section
md.append("---")
md.append("")
md.append("## Synthèse des divergences JSON vs références")
md.append("")
md.append("### Renommages confirmés par le JSON")
md.append("")
md.append("| Nom dans organigramme/Excel | Nom canonique dans JSON | Nature |")
md.append("|----------------------------|------------------------|--------|")
md.append("| `N x prixVentes(ID).csv` | `prixVentes(ID).csv` | Simplification du préfixe `N x` |")
md.append("| `N x NomCanal.csv` | `NomCanal.csv` | Idem |")
md.append("| `pplrr.shp` | `ppIrr.shp` | Harmonisation de la convention `pp` |")
md.append("| `pplInd.shp` | `ppInd.shp` | Idem |")
md.append("| `polygonesMeteoProjecttes.shp` (typo) | `polygonesMeteoProjettes.shp` | Correction orthographique |")
md.append("| `nce_principale_maelia-compl…` (tronqué) | `residence_principale_maelia-complet.csv` | Nom complet rétabli |")
md.append("| `PAE_ID_EXP` (schéma Excel `ilots`) | `ID_EXPL` (DBF + JSON) | Harmonisation colonne |")
md.append("| `LISTE_EQUS` (schéma Excel) | `LISTE_EQUI` (DBF + JSON) | Tronqué à 10 car. dBASE |")
md.append("| `anneeBissextile` (schéma Excel) | `anneeBessextile` (JSON + fichier) | Faute de frappe — à corriger |")
md.append("| `SIJ (ha/jr)` / `travail (h/jr)` (Excel) | `surfaceIrrigableJour` / `tempsTravailJour` | Normalisation noms de colonnes |")
md.append("")
md.append("### Fichiers présents dans le JSON, absents des organigrammes")
md.append("")
md.append("| Fichier | Module | Statut | Remarque |")
md.append("|---------|--------|--------|----------|")
md.append("| `debitEntre.csv` | HYDROGRAPHIQUE | 📋 SCHEMA ONLY | Débit entrant simulé par date et point |")
md.append("| `debitEntreObs.csv` | HYDROGRAPHIQUE | 📋 SCHEMA ONLY | Débit entrant observé |")
md.append("| `zhDebitForce.csv` | HYDROGRAPHIQUE | ⏳ PENDING | Débit forcé — champs à documenter |")
md.append("| `Engrais.csv` | AGRICOLE | ✅ GROUNDED | Ajout V1 — 35 engrais, 33 paramètres |")
md.append("| `reglesDeDecisions_fertilisation.csv` | AGRICOLE | ✅ GROUNDED | Ajout V1 — matrice fertilisation 36 paramètres |")
md.append("")
md.append("### Fichiers avec champs PENDING (à documenter en priorité)")
md.append("")
md.append("| Fichier | Module | Requis | Impact |")
md.append("|---------|--------|--------|--------|")
md.append("| `tronconsParZH.shp` | HYDROGRAPHIQUE | `REQ` | 🔴 Bloquant — réseau hydrographique principal |")
md.append("| `tronconsPrincipauxParZH.shp` | HYDROGRAPHIQUE | `REQ` | 🔴 Bloquant |")
md.append("| `departement.shp` | COMMUN | `COND` | 🟡 Requis si module communes activé |")
md.append("| `VP_historique.csv` | NORMATIF | `COND` | 🟡 Historique volumes prélevés |")
md.append("| `matriceDebitReelPointDOE.csv` | NORMATIF | `COND` | 🟡 Débits réels aux points DOE |")
md.append("| `RestrictionsDebitCanaux.csv` | NORMATIF | `COND` | 🟡 Restrictions canaux |")
md.append("| `zhDebitForce.csv` | HYDROGRAPHIQUE | `COND` | 🟡 Débit forcé ZH |")
md.append("| `volumeRefAnnuelIND.csv` | HYDROGRAPHIQUE | `COND` | 🟡 Volumes ref industrie |")
md.append("| `rjAEP.shp` | HYDROGRAPHIQUE | `COND` | 🟡 Points rejet AEP |")
md.append("| `rjI.shp` | HYDROGRAPHIQUE | `COND` | 🟡 Points rejet industrie |")
md.append("| `matriceDistanceCulturale.csv` | AGRICOLE | `COND` | 🟡 Distance culturale entre rotations |")
md.append("| `redevanceEau.csv` | AGRICOLE | `COND` | 🟡 Redevance eau par ressource |")
md.append("| `chargesFixesAccesRessourceIrrigation.csv` | AGRICOLE | `COND` | 🟡 Charges fixes accès ressource |")
md.append("| `rendementsObservesAnterieur.csv` | AGRICOLE | `COND` | 🟡 Rendements historiques |")
md.append("")
md.append("---")
md.append("")
md.append("## Conclusions")
md.append("")
md.append("### Ce que le JSON apporte par rapport aux références (organigrammes + Excel)")
md.append("")
md.append("1. **Schéma exhaustif et structuré** : 71 fichiers décrits avec id, type, champs typés, unités, références croisées (`referencesDataSpec`) et exemples de valeurs (`samples`) — le schéma Excel n'offre pas ce niveau de détail.")
md.append("2. **Grounding vérifié** : 14 fichiers ont leur structure confirmée par les données réelles du ZIP SASSEME V1, avec `rowCount` et échantillons.")
md.append("3. **Matrices transposées documentées** : `reglesDeDecisions.csv` (200+ paramètres, 8 ITK), `reglesDeDecisions_fertilisation.csv` (36 paramètres), `Engrais.csv` (33 paramètres) — absentes du schéma Excel.")
md.append("4. **Renommages confirmés** : le JSON tranche les ambiguïtés de nommage entre organigrammes et données réelles (`ppIrr`, `ppInd`, noms simplifiés).")
md.append("5. **Fichiers nouveaux identifiés** : 5 fichiers non visibles dans les organigrammes sont maintenant documentés.")
md.append("6. **Conditions d'activation explicites** : le champ `requiredIf` indique clairement sous quelle option de modélisation chaque fichier devient obligatoire.")
md.append("")
md.append("### Points d'action restants")
md.append("")
md.append("1. **Documenter les 14 fichiers PENDING** — en priorité les tronçons hydrographiques (`REQ`) bloquants pour la simulation SWAT.")
md.append("2. **Corriger la faute de frappe** `anneeBessextile` → `anneeBissextile` dans `joursParMois.csv` et le JSON.")
md.append("3. **Mettre à jour les organigrammes** pour intégrer les 5 nouveaux fichiers et corriger les renommages.")
md.append("4. **Renseigner `donneesMNT_ZH.csv`** avec les paramètres SWAT calibrés (actuellement toutes valeurs NA).")
md.append("5. **Vérifier la colonne `EXPREST`** dans `parcelles.dbf` — son rôle par rapport à l'ancien `ID_SDC` n'est pas encore documenté.")
md.append("6. **Ajouter des données dans `materiel.csv`** — le fichier est vide (`rowCount=0`) dans SASSEME V1.")
md.append("")
md.append("---")
md.append("")
md.append("_Analyse générée sur la base de : `maelia-database.json` (2026-06-28), `MAELIA_Schema_Donnees.xlsx`, et les 5 organigrammes fournis._")

output = '\n'.join(md)
with open('./analyse_json_vs_maelia.md', 'w', encoding='utf-8') as f:
    f.write(output)

print(f"Fichier généré — {len(md)} lignes, {len(output)} caractères")