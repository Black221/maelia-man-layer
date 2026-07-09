-- M8+ : interactions entre paramètres (enabled_if) et entre paramètre et données (options_data_spec).
-- enabled_if  : condition d'activation (un champ reste visible mais grisé tant qu'elle est fausse).
-- options_data_spec : id d'un DataSpec dont le dataset projet alimente les valeurs proposées.

ALTER TABLE parameter_spec ADD COLUMN IF NOT EXISTS enabled_if text;
ALTER TABLE parameter_spec ADD COLUMN IF NOT EXISTS options_data_spec varchar(120);

-- Dépendances déduites de launcherBase.gaml : chaque "id…ASimuler" n'est saisissable que si la
-- case "simulationSur…" correspondante est cochée ; ses valeurs proviennent du DataSpec adéquat.
UPDATE parameter_spec SET enabled_if = 'executerModeleSurUneZH == true',
       options_data_spec = 'hydro.zonesHydrographiques.ZH'
 WHERE gaml_name = 'listNomsZHsDecoupageZone';

UPDATE parameter_spec SET enabled_if = 'executerUnSeulAgriculteur == true',
       options_data_spec = 'agri.agriculteurs.exploitations'
 WHERE gaml_name = 'idExploitationAexecuter';

UPDATE parameter_spec SET enabled_if = 'executerSurEnsembleExploit == true',
       options_data_spec = 'agri.agriculteurs.exploitations'
 WHERE gaml_name = 'listIdExploitationAexecuter';

UPDATE parameter_spec SET enabled_if = 'executerUneSeuleParcelle == true',
       options_data_spec = 'agri.ilots.parcelles'
 WHERE gaml_name = 'nomParcelleAffichee';

UPDATE parameter_spec SET enabled_if = 'utiliserMemeDonnesMeteoPartout == true',
       options_data_spec = 'commun.meteo.polygonesMeteoFrance'
 WHERE gaml_name = 'idPointMeteoUnique';
