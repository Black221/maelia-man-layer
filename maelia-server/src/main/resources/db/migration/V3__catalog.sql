-- M2 : catalogue DataSpec / FieldSpec (immuable côté utilisateur, chargé depuis le seed JSON)

CREATE TABLE data_spec (
    id                  VARCHAR(120)  PRIMARY KEY,
    module              VARCHAR(30)   NOT NULL,
    folder              VARCHAR(200)  NOT NULL,
    file_name           VARCHAR(200)  NOT NULL,
    file_type           VARCHAR(10)   NOT NULL,
    csv_format          VARCHAR(20),
    generation          VARCHAR(10)   NOT NULL,
    required            BOOLEAN       NOT NULL DEFAULT TRUE,
    required_if         TEXT,
    temporal_resolution VARCHAR(10)   NOT NULL DEFAULT 'NONE',
    multi_instance      BOOLEAN       NOT NULL DEFAULT FALSE,
    instance_pattern    TEXT,
    saisie_mode         VARCHAR(10)   NOT NULL,
    description         TEXT,
    fields_status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
);

CREATE TABLE field_spec (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    data_spec_id        VARCHAR(120)  NOT NULL REFERENCES data_spec(id) ON DELETE CASCADE,
    position            INTEGER,
    label               VARCHAR(100)  NOT NULL,
    info_type           VARCHAR(20)   NOT NULL,
    unit                VARCHAR(50),
    required            BOOLEAN       NOT NULL DEFAULT TRUE,
    required_if         TEXT,
    references_data_spec VARCHAR(120) REFERENCES data_spec(id),
    description         TEXT,
    list_separator      VARCHAR(10),
    allowed_values      TEXT,
    sort_order          INTEGER       NOT NULL DEFAULT 0
);

CREATE INDEX idx_data_spec_module ON data_spec(module);
CREATE INDEX idx_field_spec_data_spec ON field_spec(data_spec_id);
