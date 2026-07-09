-- M5 : restitution des résultats (sorties d'un run de simulation)

-- Artefacts bruts produits par GAMA (snapshots PNG, CSV, XML, etc.)
CREATE TABLE output_artifact (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id        UUID          NOT NULL REFERENCES simulation_run(id) ON DELETE CASCADE,
    name          VARCHAR(255)  NOT NULL,
    artifact_type VARCHAR(20)   NOT NULL,            -- IMAGE | CSV | XML | OTHER
    content_type  VARCHAR(150),
    relative_path VARCHAR(1024) NOT NULL,            -- relatif au volume gama-workspace
    size_bytes    BIGINT        NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- Valeurs d'indicateurs extraites des sorties (séries temporelles, valeurs spatialisées)
CREATE TABLE result_value (
    id        UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id    UUID             NOT NULL REFERENCES simulation_run(id) ON DELETE CASCADE,
    indicator VARCHAR(255)     NOT NULL,
    zone      VARCHAR(255),                          -- BVe / territoire (NULL = global)
    obs_date  DATE,                                  -- pour les séries temporelles
    cycle     INTEGER,                               -- pas de simulation (si pas de date)
    value     DOUBLE PRECISION NOT NULL,
    unit      VARCHAR(50)
);

CREATE INDEX idx_output_artifact_run        ON output_artifact(run_id);
CREATE INDEX idx_result_value_run           ON result_value(run_id);
CREATE INDEX idx_result_value_run_indicator ON result_value(run_id, indicator);
