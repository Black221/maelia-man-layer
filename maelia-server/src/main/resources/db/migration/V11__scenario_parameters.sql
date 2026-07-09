-- M7 : catalogue des paramètres de simulation (extrait de launcherBase.gaml)

CREATE TABLE parameter_group (
    id         VARCHAR(80)  PRIMARY KEY,
    label      VARCHAR(200) NOT NULL,
    sort_order INTEGER      NOT NULL DEFAULT 0,
    parent_id  VARCHAR(80)
);

CREATE TABLE parameter_spec (
    gaml_name      VARCHAR(120) PRIMARY KEY,   -- nom EXACT de la variable GAML (clé envoyée à GAMA)
    label          VARCHAR(255) NOT NULL,
    group_id       VARCHAR(80)  NOT NULL,
    type           VARCHAR(20)  NOT NULL,      -- BOOLEAN | INTEGER | FLOAT | STRING | ENUM | STRING_LIST
    default_value  TEXT,                       -- valeur par défaut (forme textuelle ; liste = valeurs séparées par |)
    unit           VARCHAR(50),
    allowed_values TEXT,                        -- ENUM : valeurs séparées par |
    visible_if     TEXT,                        -- dépendance d'affichage, ex. "executerModeleHydrographique == true"
    advanced       BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order     INTEGER      NOT NULL DEFAULT 0
);

CREATE INDEX idx_parameter_spec_group ON parameter_spec(group_id);
