-- Dépendances entre fichiers au niveau DataSpec.
-- Les liaisons par identifiant (clé étrangère de colonne) sont déjà portées par
-- field_spec.references_data_spec. depends_on complète le modèle avec les dépendances
-- IMPLICITES "par construction" (fichier découpé/dérivé d'un autre sans colonne FK
-- documentée, ex. contourZH.shp dérivé de ZH.shp). Liste d'ids data_spec séparés par '|'.
ALTER TABLE data_spec ADD COLUMN IF NOT EXISTS depends_on TEXT;

-- Remplissage des dépendances implicites connues (cf. data/DEPENDANCES_FICHIERS.md).
-- Le seeder ne rejoue pas sur une base déjà remplie : on aligne ici les bases existantes.
UPDATE data_spec SET depends_on = 'hydro.zonesHydrographiques.ZH'
 WHERE id IN ('hydro.zonesHydrographiques.contourZH',
              'hydro.troncons.tronconsParZH',
              'hydro.zonesHydrographiques.donneesMNT_ZH',
              'hydro.zonesHydrographiques.debitEntre',
              'hydro.zonesHydrographiques.debitEntreObs',
              'hydro.zonesHydrographiques.zhDebitForce',
              'hydro.equipements.rjAEP',
              'hydro.equipements.rjI')
   AND depends_on IS NULL;

UPDATE data_spec SET depends_on = 'hydro.troncons.tronconsParZH'
 WHERE id = 'hydro.troncons.tronconsPrincipauxParZH' AND depends_on IS NULL;

UPDATE data_spec SET depends_on = 'agri.ilots.ilots'
 WHERE id = 'hydro.hru.hruSansIlots_025' AND depends_on IS NULL;

UPDATE data_spec SET depends_on = 'hydro.equipements.ppInd'
 WHERE id = 'hydro.equipements.volumeRefAnnuelIND' AND depends_on IS NULL;

UPDATE data_spec SET depends_on = 'norm.pointsDeReference.pointsDeReference'
 WHERE id = 'norm.pointsDeReference.matriceDebitReelPointDOE' AND depends_on IS NULL;

UPDATE data_spec SET depends_on = 'norm.uniteDeGestion.UG'
 WHERE id IN ('commun.communes.communesTrimUG', 'norm.uniteDeGestion.VP_historique')
   AND depends_on IS NULL;

UPDATE data_spec SET depends_on = 'hydro.canaux.canaux_csv'
 WHERE id IN ('hydro.canaux.canaux_shp', 'norm.canaux.restrictionsDebitCanaux')
   AND depends_on IS NULL;

UPDATE data_spec SET depends_on = 'commun.typesDeSol.typeDeSolParZH'
 WHERE id = 'agri.culture.reglesDeDecisions' AND depends_on IS NULL;

UPDATE data_spec SET depends_on = 'commun.communes.communesTrimUG'
 WHERE id IN ('commun.communes.resultatsEDEM',
              'commun.communes.prixEau',
              'commun.communes.salaires',
              'commun.communes.residencePrincipale')
   AND depends_on IS NULL;
