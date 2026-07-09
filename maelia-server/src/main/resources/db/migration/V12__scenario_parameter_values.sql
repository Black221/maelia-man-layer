-- M8 : le scénario devient piloté par le catalogue de paramètres.
-- Les champs fixes sont migrés vers une map JSONB parameter_values (clé = gamlName).

ALTER TABLE scenario ADD COLUMN parameter_values JSONB NOT NULL DEFAULT '{}'::jsonb;

-- Migration des colonnes existantes vers la map (climat ACTUEL/RCP -> nomScenarioClimatique, etc.)
UPDATE scenario SET parameter_values =
    jsonb_strip_nulls(
        jsonb_build_object(
            'anneeDebutSimulation', annee_debut_simulation,
            'nbAnneesSimulation',   nb_annees_simulation,
            'nomDecoupageZonePourLectureFichiers', nom_territoire,
            'seed', graine,
            'nomScenarioClimatique',
                CASE scenario_climatique
                    WHEN 'RCP45' THEN 'rcp4.5'
                    WHEN 'RCP85' THEN 'rcp8.5'
                    WHEN 'ACTUEL' THEN ''
                    ELSE NULL
                END
        )
    );

ALTER TABLE scenario
    DROP COLUMN scenario_climatique,
    DROP COLUMN prix_eau,
    DROP COLUMN prix_culture,
    DROP COLUMN annee_debut_simulation,
    DROP COLUMN nb_annees_simulation,
    DROP COLUMN nom_territoire,
    DROP COLUMN graine;
