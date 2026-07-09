-- M2 : projets de simulation (sans authentification jusqu'à M7)

CREATE TABLE project (
    id                      UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    VARCHAR(200)  NOT NULL,
    description             TEXT,
    study_area              VARCHAR(100)  NOT NULL DEFAULT 'garonne-amont',
    -- ModelingConfiguration sérialisée en JSONB
    modeling_configuration  JSONB         NOT NULL DEFAULT '{}',
    status                  VARCHAR(20)   NOT NULL DEFAULT 'ACTIF',
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_project_status ON project(status);
