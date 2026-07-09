-- M1 : Table de persistance des runs de simulation.

CREATE TABLE simulation_run (
    id                 UUID         NOT NULL PRIMARY KEY,
    model_path         VARCHAR(512) NOT NULL,
    experiment_name    VARCHAR(256) NOT NULL,
    status             VARCHAR(32)  NOT NULL DEFAULT 'EN_FILE',
    created_at         TIMESTAMPTZ  NOT NULL,
    started_at         TIMESTAMPTZ,
    finished_at        TIMESTAMPTZ,
    gama_experiment_id VARCHAR(128),
    final_cycle        INTEGER      NOT NULL DEFAULT 0,
    error_message      VARCHAR(2048)
);

CREATE INDEX idx_simulation_run_status ON simulation_run(status);
