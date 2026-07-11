-- Lot 1 résultats : dimensions catégorielles pour l'analyse agronomique.
-- Le parseur générique ne portait qu'une dimension (zone). Pour « rendement par type de
-- culture au cours du temps » il faut une dimension catégorielle (culture/couvert) et un axe
-- annuel explicite (les sorties MAELIA portent une colonne 'annee', pas une date exploitable).

ALTER TABLE result_value ADD COLUMN category VARCHAR(255);   -- culture / couvert
ALTER TABLE result_value ADD COLUMN year      INTEGER;        -- annee de la sortie

CREATE INDEX idx_result_value_run_ind_cat_year
    ON result_value(run_id, indicator, category, year);
