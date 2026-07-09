-- C8 : fichiers binaires uploadés par l'utilisateur pour un DataSpec de type SHP.
-- Les octets vivent dans MinIO (object_key) ; cette table porte les métadonnées.
-- À la matérialisation, ces fichiers écrasent ceux du socle dans includes/{folder}/.

CREATE TABLE dataset_file (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID         NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    data_spec_id    VARCHAR(120) NOT NULL REFERENCES data_spec(id),
    file_name       VARCHAR(255) NOT NULL,   -- nom final attendu par le modèle (ex. ilots.shp)
    object_key      VARCHAR(512) NOT NULL,   -- clé de l'objet dans le bucket MinIO
    size_bytes      BIGINT       NOT NULL DEFAULT 0,
    content_type    VARCHAR(100),
    uploaded_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (project_id, data_spec_id, file_name)
);

CREATE INDEX idx_dataset_file_project ON dataset_file(project_id);
CREATE INDEX idx_dataset_file_project_spec ON dataset_file(project_id, data_spec_id);
