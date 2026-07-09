-- M3 : datasets (données d'entrée saisies ou importées par projet)

CREATE TABLE dataset (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID        NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    data_spec_id    VARCHAR(120) NOT NULL REFERENCES data_spec(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'VIDE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (project_id, data_spec_id)
);

-- Enregistrements CSV : chaque ligne = une Map<String,Object> en JSONB
CREATE TABLE dataset_record (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    dataset_id  UUID        NOT NULL REFERENCES dataset(id) ON DELETE CASCADE,
    row_index   INTEGER     NOT NULL,
    values      JSONB       NOT NULL DEFAULT '{}'
);

-- Problèmes de validation : persistés après chaque run de validation
CREATE TABLE validation_issue (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    dataset_id  UUID        NOT NULL REFERENCES dataset(id) ON DELETE CASCADE,
    field       VARCHAR(100),
    row_index   INTEGER,
    severity    VARCHAR(10) NOT NULL DEFAULT 'ERROR',
    message     TEXT        NOT NULL
);

CREATE INDEX idx_dataset_project ON dataset(project_id);
CREATE INDEX idx_dataset_record_dataset ON dataset_record(dataset_id, row_index);
CREATE INDEX idx_dataset_record_values ON dataset_record USING GIN (values);
CREATE INDEX idx_validation_issue_dataset ON validation_issue(dataset_id);
