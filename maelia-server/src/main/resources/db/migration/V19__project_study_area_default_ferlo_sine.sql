-- Change la zone d'etude par defaut des projets : Garonne-Amont -> Ferlo-Sine (Senegal).
-- Aligne le defaut SQL sur les defauts applicatifs (Project.create / ProjectJpaEntity)
-- et sur le seed du catalogue (maelia-database.json : studyArea "ferlo-sine").
-- N'affecte que les insertions futures qui n'auraient pas de valeur explicite ; les
-- projets existants conservent leur study_area.

ALTER TABLE project ALTER COLUMN study_area SET DEFAULT 'ferlo-sine';
