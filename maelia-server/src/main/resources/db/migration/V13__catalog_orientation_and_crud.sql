-- Gestion du catalogue (orientation des fichiers + édition manuelle).
--
-- Orientation : un fichier MAELIA stocke les champs soit en COLONNES (entête en 1re ligne,
-- chaque enregistrement = une ligne ; cas standard), soit en LIGNES (transposé : chaque champ
-- = une ligne, chaque enregistrement = une colonne ; ex. reglesDeDecisions.csv). Cet aspect
-- n'était pas modélisé, ce qui faussait l'import, la génération CSV et l'affichage.

ALTER TABLE data_spec
    ADD COLUMN orientation              VARCHAR(20)  NOT NULL DEFAULT 'FIELDS_AS_COLUMNS',
    -- Index (0-based) de la 1re colonne de DONNÉES en mode transposé (FIELDS_AS_ROWS).
    -- Permet le cas « col0 = clé du champ, col1 = unité, col2..N = valeurs ». Défaut = 1.
    ADD COLUMN matrix_value_start_index INTEGER,
    -- Délimiteur CSV (MAELIA utilise ';').
    ADD COLUMN delimiter                CHAR(1)      NOT NULL DEFAULT ';',
    -- Provenance : SEED (chargé depuis le JSON) ou USER (créé/édité manuellement). Empêche
    -- le seed d'écraser les éditions manuelles.
    ADD COLUMN origin                   VARCHAR(10)  NOT NULL DEFAULT 'SEED',
    ADD COLUMN updated_at               TIMESTAMPTZ;

-- Migration des fichiers historiquement marqués MATRIX = orientation transposée.
UPDATE data_spec SET orientation = 'FIELDS_AS_ROWS', matrix_value_start_index = 1
    WHERE csv_format = 'MATRIX';

-- Pour les fichiers transposés, csv_format n'a plus de sens « entête/positionnel » :
-- on normalise vers COLUMN_HEADER (mode d'entête par défaut quand on rebascule en colonnes).
UPDATE data_spec SET csv_format = 'COLUMN_HEADER' WHERE csv_format = 'MATRIX';
