-- Football Championship Simulator: database schema (core tables)
-- Run this first on an empty database, then seed.sql.
-- Safe to run again: it drops and recreates the tables.

DROP TABLE IF EXISTS players CASCADE;
DROP TABLE IF EXISTS coaches CASCADE;
DROP TABLE IF EXISTS referees CASCADE;
DROP TABLE IF EXISTS teams CASCADE;

CREATE TABLE teams (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    short_name  CHAR(3)      NOT NULL UNIQUE CHECK (short_name ~ '^[A-Z]{3}$'),
    city        VARCHAR(50)  NOT NULL,
    stadium     VARCHAR(60)  NOT NULL
);

CREATE TABLE coaches (
    id                   SERIAL PRIMARY KEY,
    team_id              INT          NOT NULL UNIQUE REFERENCES teams (id) ON DELETE CASCADE,
    first_name           VARCHAR(50)  NOT NULL DEFAULT '',
    last_name            VARCHAR(50)  NOT NULL,
    nationality          VARCHAR(50)  NOT NULL,
    age                  SMALLINT     NOT NULL CHECK (age BETWEEN 15 AND 80),
    preferred_formation  VARCHAR(20)  NOT NULL
        CHECK (preferred_formation IN ('FOUR_THREE_THREE', 'FOUR_FOUR_TWO', 'FOUR_TWO_THREE_ONE', 'THREE_FIVE_TWO'))
);

CREATE TABLE referees (
    id           SERIAL PRIMARY KEY,
    first_name   VARCHAR(50)  NOT NULL DEFAULT '',
    last_name    VARCHAR(50)  NOT NULL,
    nationality  VARCHAR(50)  NOT NULL,
    age          SMALLINT     NOT NULL CHECK (age BETWEEN 15 AND 80),
    strictness   SMALLINT     NOT NULL CHECK (strictness BETWEEN 1 AND 99)
);

CREATE TABLE players (
    id              SERIAL PRIMARY KEY,
    team_id         INT          NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    first_name      VARCHAR(50)  NOT NULL DEFAULT '',
    last_name       VARCHAR(50)  NOT NULL,
    nationality     VARCHAR(50)  NOT NULL,
    age             SMALLINT     NOT NULL CHECK (age BETWEEN 15 AND 80),
    shirt_number    SMALLINT     NOT NULL CHECK (shirt_number BETWEEN 1 AND 99),
    position        VARCHAR(3)   NOT NULL
        CHECK (position IN ('GK', 'CB', 'LB', 'RB', 'LWB', 'RWB', 'CDM', 'CM', 'CAM', 'LM', 'RM', 'LW', 'RW', 'CF', 'ST')),
    preferred_foot  VARCHAR(5)   NOT NULL DEFAULT 'RIGHT' CHECK (preferred_foot IN ('LEFT', 'RIGHT', 'BOTH')),

    pace        SMALLINT NOT NULL CHECK (pace       BETWEEN 1 AND 99),
    shooting    SMALLINT NOT NULL CHECK (shooting   BETWEEN 1 AND 99),
    passing     SMALLINT NOT NULL CHECK (passing    BETWEEN 1 AND 99),
    dribbling   SMALLINT NOT NULL CHECK (dribbling  BETWEEN 1 AND 99),
    defending   SMALLINT NOT NULL CHECK (defending  BETWEEN 1 AND 99),
    physical    SMALLINT NOT NULL CHECK (physical   BETWEEN 1 AND 99),
    heading     SMALLINT NOT NULL CHECK (heading    BETWEEN 1 AND 99),
    reflexes    SMALLINT NOT NULL CHECK (reflexes   BETWEEN 1 AND 99),
    handling    SMALLINT NOT NULL CHECK (handling   BETWEEN 1 AND 99),
    stamina     SMALLINT NOT NULL CHECK (stamina    BETWEEN 1 AND 99),
    aggression  SMALLINT NOT NULL CHECK (aggression BETWEEN 1 AND 99),

    CONSTRAINT uq_players_team_shirt UNIQUE (team_id, shirt_number)
);

CREATE INDEX idx_players_team ON players (team_id);
