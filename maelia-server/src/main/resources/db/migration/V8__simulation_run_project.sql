-- M4 : Lier simulation_run à un projet et un scénario (nullable pour les runs dev M1).

ALTER TABLE simulation_run
    ADD COLUMN IF NOT EXISTS project_id  UUID REFERENCES project(id),
    ADD COLUMN IF NOT EXISTS scenario_id UUID REFERENCES scenario(id);

CREATE INDEX idx_simulation_run_project_id ON simulation_run(project_id);
