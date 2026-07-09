-- V18 : réaligne les défauts du catalogue de paramètres de scénario sur launcherProjet.gaml
-- (copie du launcher de test SASSEME validé en headless — scénario minimal par défaut).
-- Le seeder ne tourne que sur catalogue vide : ces UPDATE couvrent les bases existantes.
-- Encodage default_value (cf. V11) : texte ; listes jointes par '|' ; booléens 'true'/'false'.

UPDATE parameter_spec SET default_value = '2018'   WHERE gaml_name = 'anneeDebutSimulation';
UPDATE parameter_spec SET default_value = '1'      WHERE gaml_name = 'nbAnneesSimulation';

-- Forcé par GamaParameterBuilder (includes matérialisés à plat) : géré par le système, plus éditable.
UPDATE parameter_spec SET default_value = '', group_id = 'system'
    WHERE gaml_name = 'nomDecoupageZonePourLectureFichiers';

UPDATE parameter_spec SET default_value = 'SSM1'                WHERE gaml_name = 'listNomsZHsDecoupageZone';
UPDATE parameter_spec SET default_value = 'SSM1-0001'           WHERE gaml_name = 'idExploitationAexecuter';
UPDATE parameter_spec SET default_value = 'SSM1-0001|SSM1-0002' WHERE gaml_name = 'listIdExploitationAexecuter';
UPDATE parameter_spec SET default_value = 'true'                WHERE gaml_name = 'executerUneSeuleParcelle';
UPDATE parameter_spec SET default_value = '19_001'              WHERE gaml_name = 'nomParcelleAffichee';
UPDATE parameter_spec SET default_value = ''                    WHERE gaml_name = 'nomScenarioClimatique';
UPDATE parameter_spec SET default_value = 'true'                WHERE gaml_name = 'verboseMode';
UPDATE parameter_spec SET default_value = 'false'               WHERE gaml_name = 'executionViaAPI';

UPDATE parameter_spec SET default_value = 'false'
    WHERE gaml_name IN ('avecContrainteDeMainOeuvre', 'plusieursTravauxDuSolParITK',
                        'plusieursFertilisationsParITK', 'plusieursTraitementsPhytoParITK');

UPDATE parameter_spec SET default_value = 'reliquat', allowed_values = '|corpen|reliquat'
    WHERE gaml_name = 'adaptationFertilisation';
UPDATE parameter_spec SET default_value = 'AqYield' WHERE gaml_name = 'nomChoixModeleCroissancePrairie';
UPDATE parameter_spec SET default_value = 'SC1'     WHERE gaml_name IN ('listScenarioPrix', 'scenarioDePrixPrincipal');
UPDATE parameter_spec SET default_value = 'ci-'     WHERE gaml_name = 'PREFIXE_CI';
UPDATE parameter_spec SET default_value = 'true'    WHERE gaml_name = 'executerBarrage';

-- Sorties : suivis réduits à l'échantillon, sorties azote/carbone activées comme dans le test.
UPDATE parameter_spec SET default_value = ''        WHERE gaml_name = 'listAgriASuivre';
UPDATE parameter_spec SET default_value = '21_001'  WHERE gaml_name = 'listParcellesASuivre';
UPDATE parameter_spec SET default_value = '20_001'  WHERE gaml_name = 'listParcellesPourSortiesAqYield';
UPDATE parameter_spec SET default_value = 'true'
    WHERE gaml_name IN ('sortiesAqYieldNC', 'N_Cstock_Parcelles', 'N_GES_Parcelles', 'N_lixi_Parcelles');
