-- M4 : Scénarios de simulation liés à un projet.

CREATE TABLE scenario (
    id                  UUID         NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id          UUID         NOT NULL REFERENCES project(id),
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    scenario_climatique VARCHAR(100) NOT NULL DEFAULT 'ACTUEL',
    prix_eau            DOUBLE PRECISION,
    prix_culture        DOUBLE PRECISION,
    date_debut          DATE,
    date_fin            DATE,
    graine              INTEGER,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    archived_at         TIMESTAMPTZ
);

CREATE INDEX idx_scenario_project_id ON scenario(project_id);
