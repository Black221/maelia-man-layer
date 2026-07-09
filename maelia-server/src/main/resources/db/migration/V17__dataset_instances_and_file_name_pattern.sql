-- V17 : instances de fichiers multi-instance (ex. série climatique AAAA.csv → 2018.csv, 2019.csv…)
-- + motif machine-readable d'appariement nom de fichier → DataSpec (import ZIP).

-- Regex (match complet, insensible à la casse) reconnaissant les instances d'un type multi-instance.
ALTER TABLE data_spec ADD COLUMN file_name_pattern VARCHAR(160);

-- Nom de fichier de l'instance ("2018.csv") ; NULL = dataset unique du type (cas standard).
ALTER TABLE dataset ADD COLUMN instance_key VARCHAR(160);

-- L'unicité passe de (projet, type) à (projet, type, instance).
ALTER TABLE dataset DROP CONSTRAINT IF EXISTS dataset_project_id_data_spec_id_key;
CREATE UNIQUE INDEX ux_dataset_project_spec_instance
    ON dataset (project_id, data_spec_id, COALESCE(instance_key, ''));

-- Le seeder ne tourne que sur catalogue vide : on aligne ici les bases déjà seedées
-- avec les corrections du seed JSON (l'expansion des champs matrice, elle, nécessite
-- un re-seed : vider data_spec ou recréer la base).
UPDATE data_spec SET file_name_pattern = '\d{4}\.csv'
    WHERE id = 'commun.meteo.serieClimatique';
UPDATE data_spec SET file_name_pattern = 'prixVentes.+\.csv'
    WHERE id = 'agri.marcheAgricole.prixVentes';

-- Fichiers transposés avec colonne méta entre la clé (col 0) et les valeurs (col 2..N).
UPDATE data_spec SET matrix_value_start_index = 2
    WHERE id IN ('agri.culture.reglesDeDecisions', 'agri.culture.reglesDeDecisionsFertilisation');

-- resultatsEDEM n'est pas transposé : lignes = communes, colonnes = années (format large).
UPDATE data_spec SET orientation = 'FIELDS_AS_COLUMNS', csv_format = 'COLUMN_HEADER', matrix_value_start_index = NULL
    WHERE id = 'commun.communes.resultatsEDEM';
