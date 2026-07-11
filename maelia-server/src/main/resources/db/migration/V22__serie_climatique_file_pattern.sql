-- V22 : le catalogue en base (seedé depuis une version antérieure de maelia-database.json)
-- n'avait pas le motif d'instance de la série climatique météo -> l'upload d'un AAAA.csv
-- (2018.csv, 2019.csv…) échouait : « Aucun fichier du catalogue ne porte ce nom ni ne
-- correspond à un motif d'instance ». Le seed JSON le contient déjà (installs neuves OK) ;
-- ici on corrige les bases existantes. Idempotent (ne touche que si vide/nul).
-- NB : data_spec est peuplée par DataSpecSeeder (ApplicationReadyEvent, après Flyway) ;
-- sur une base neuve cet UPDATE ne matche rien puis le seeder insère le motif -> OK.

UPDATE data_spec
   SET file_name_pattern = '\d{4}\.csv'
 WHERE id = 'commun.meteo.serieClimatique'
   AND (file_name_pattern IS NULL OR file_name_pattern = '');
