-- M4 adaptation MAELIA : paramètres de simulation réels sur le scénario.
-- annee_debut_simulation → GAMA var: anneeDebutSimulation
-- nb_annees_simulation   → GAMA var: nbAnneesSimulation
-- nom_territoire         → GAMA var: nomDecoupageZonePourLectureFichiers (sous-répertoire dans includes/)

ALTER TABLE scenario
    ADD COLUMN IF NOT EXISTS annee_debut_simulation INTEGER NOT NULL DEFAULT 2019,
    ADD COLUMN IF NOT EXISTS nb_annees_simulation   INTEGER NOT NULL DEFAULT 3,
    ADD COLUMN IF NOT EXISTS nom_territoire         VARCHAR(255) NOT NULL DEFAULT 'terrainTest';
