-- M0 : Initialisation du schéma PostgreSQL/PostGIS pour MAELIA.
-- Ce script est exécuté automatiquement par Flyway au premier démarrage.

-- Extensions nécessaires
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS postgis;

-- Table de base pour valider la connexion (sera remplacée par le vrai schéma en M2)
CREATE TABLE IF NOT EXISTS maelia_schema_info (
    key   VARCHAR(64)  PRIMARY KEY,
    value VARCHAR(256) NOT NULL
);

INSERT INTO maelia_schema_info (key, value)
VALUES ('schema_version', 'M0-init')
ON CONFLICT (key) DO NOTHING;
