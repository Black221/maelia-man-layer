-- V21 : sélecteurs de valeurs depuis les données, colonne configurable.
-- options_column  : label de la colonne du DataSpec dont on propose les valeurs (null = 1er champ).
--                   Permet de proposer autre chose que l'ID (nom, type de sol ZONE_PEDO…).
-- options_source  : COLUMN (défaut) | COLUMN_HEADERS (noms de colonnes) | INSTANCE_KEYS
--                   (clés d'instance d'un DataSpec multi-instance, ex. prixVentesXX).

ALTER TABLE parameter_spec ADD COLUMN IF NOT EXISTS options_column varchar(120);
ALTER TABLE parameter_spec ADD COLUMN IF NOT EXISTS options_source varchar(20);

-- Colonne d'ID explicite pour les 5 paramètres déjà câblés (robustesse si l'ordre des champs change).
UPDATE parameter_spec SET options_column = 'ID_ZH'       WHERE gaml_name = 'listNomsZHsDecoupageZone';
UPDATE parameter_spec SET options_column = 'ID_EXPL'     WHERE gaml_name = 'idExploitationAexecuter';
UPDATE parameter_spec SET options_column = 'ID_EXPL'     WHERE gaml_name = 'listIdExploitationAexecuter';
UPDATE parameter_spec SET options_column = 'ID_PARCELL'  WHERE gaml_name = 'nomParcelleAffichee';
UPDATE parameter_spec SET options_column = 'ID_PDG'      WHERE gaml_name = 'idPointMeteoUnique';

-- Lot B : liaisons manquantes (paramètres de sortie/suivi qui listent des parcelles/exploitations).
UPDATE parameter_spec SET options_data_spec = 'agri.ilots.parcelles', options_column = 'ID_PARCELL'
 WHERE gaml_name IN ('listParcellesASuivre', 'listParcellesPourSortiesAqYield');

UPDATE parameter_spec SET options_data_spec = 'agri.agriculteurs.exploitations', options_column = 'ID_EXPL'
 WHERE gaml_name = 'listAgriASuivre';

UPDATE parameter_spec SET options_data_spec = 'commun.typesDeSol.typeDeSolParZH', options_column = 'ZONE_PEDO',
       enabled_if = 'executerParcelleVirtuelle == true'
 WHERE gaml_name = 'typeDeSolForceParcelle';

-- Lot C : scénarios de prix = clés d'instance des fichiers prixVentesXX.csv.
UPDATE parameter_spec SET options_data_spec = 'agri.marcheAgricole.prixVentes', options_source = 'INSTANCE_KEYS'
 WHERE gaml_name IN ('scenarioDePrixPrincipal', 'listScenarioPrix');

-- Paramètres du launcher jusque-là absents du catalogue (couverture complète). Le seeder ne
-- réinsère pas sur base existante : on les ajoute ici (idempotent).
INSERT INTO parameter_spec (gaml_name, label, group_id, type, default_value, advanced, sort_order)
VALUES ('Canaux', 'Sortie Prelevements pour alimenter les canaux', 'sorties.prelevements', 'BOOLEAN', 'false', true, 110)
ON CONFLICT (gaml_name) DO NOTHING;

INSERT INTO parameter_spec (gaml_name, label, group_id, type, default_value, advanced, sort_order)
VALUES ('sequences_a_optimiser', 'Séquence(s) à optimiser (agricole)', 'agricole', 'STRING', '', true, 400)
ON CONFLICT (gaml_name) DO NOTHING;
