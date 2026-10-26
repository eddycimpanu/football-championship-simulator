-- Football Championship Simulator: season tables
-- Run this after schema.sql and seed.sql. It only touches the season tables,
-- so teams, players, coaches and referees stay as they are.
-- Safe to run again: it drops and recreates the season tables (all saved seasons are lost).

DROP VIEW IF EXISTS v_standings;
DROP TABLE IF EXISTS player_season_state CASCADE;
DROP TABLE IF EXISTS team_match_stats CASCADE;
DROP TABLE IF EXISTS player_match_stats CASCADE;
DROP TABLE IF EXISTS match_events CASCADE;
DROP TABLE IF EXISTS matches CASCADE;
DROP TABLE IF EXISTS seasons CASCADE;

CREATE TABLE seasons (
    id             SERIAL PRIMARY KEY,
    name           VARCHAR(60)  NOT NULL,
    random_seed    BIGINT       NOT NULL,
    current_round  SMALLINT     NOT NULL DEFAULT 1,
    status         VARCHAR(12)  NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'FINISHED')),
    created_at     TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE matches (
    id            SERIAL PRIMARY KEY,
    season_id     INT       NOT NULL REFERENCES seasons (id) ON DELETE CASCADE,
    round         SMALLINT  NOT NULL CHECK (round >= 1),
    home_team_id  INT       NOT NULL REFERENCES teams (id),
    away_team_id  INT       NOT NULL REFERENCES teams (id),
    referee_id    INT       REFERENCES referees (id),
    home_goals    SMALLINT  CHECK (home_goals >= 0),
    away_goals    SMALLINT  CHECK (away_goals >= 0),
    status        VARCHAR(10) NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED', 'PLAYED')),
    CONSTRAINT chk_matches_two_teams CHECK (home_team_id <> away_team_id),
    CONSTRAINT uq_matches_pair UNIQUE (season_id, home_team_id, away_team_id),
    CONSTRAINT chk_matches_score_when_played CHECK (status = 'SCHEDULED' OR (home_goals IS NOT NULL AND away_goals IS NOT NULL))
);

CREATE INDEX idx_matches_season_round ON matches (season_id, round);

CREATE TABLE match_events (
    id                   SERIAL PRIMARY KEY,
    match_id             INT          NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    minute               SMALLINT     NOT NULL CHECK (minute BETWEEN 1 AND 90),
    added_time           SMALLINT     NOT NULL DEFAULT 0 CHECK (added_time >= 0),
    event_type           VARCHAR(20)  NOT NULL CHECK (event_type IN (
                             'KICK_OFF', 'HALF_TIME', 'SECOND_HALF', 'FULL_TIME', 'GOAL', 'OWN_GOAL',
                             'YELLOW_CARD', 'SECOND_YELLOW', 'RED_CARD', 'COACH_YELLOW_CARD', 'COACH_RED_CARD',
                             'SUBSTITUTION', 'INJURY', 'SHOT', 'SAVE', 'CORNER', 'FOUL', 'OFFSIDE')),
    team_id              INT          REFERENCES teams (id),
    player_id            INT          REFERENCES players (id),
    secondary_player_id  INT          REFERENCES players (id),
    is_key_event         BOOLEAN      NOT NULL,
    description          TEXT         NOT NULL
);

CREATE INDEX idx_match_events_match ON match_events (match_id);

CREATE TABLE player_match_stats (
    match_id          INT       NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    player_id         INT       NOT NULL REFERENCES players (id),
    team_id           INT       NOT NULL REFERENCES teams (id),
    is_starter        BOOLEAN   NOT NULL,
    minutes_played    SMALLINT  NOT NULL CHECK (minutes_played BETWEEN 0 AND 90),
    goals             SMALLINT  NOT NULL DEFAULT 0,
    assists           SMALLINT  NOT NULL DEFAULT 0,
    own_goals         SMALLINT  NOT NULL DEFAULT 0,
    shots             SMALLINT  NOT NULL DEFAULT 0,
    shots_on_target   SMALLINT  NOT NULL DEFAULT 0,
    key_passes        SMALLINT  NOT NULL DEFAULT 0,
    tackles           SMALLINT  NOT NULL DEFAULT 0,
    interceptions     SMALLINT  NOT NULL DEFAULT 0,
    fouls_committed   SMALLINT  NOT NULL DEFAULT 0,
    fouls_suffered    SMALLINT  NOT NULL DEFAULT 0,
    offsides          SMALLINT  NOT NULL DEFAULT 0,
    yellow_cards      SMALLINT  NOT NULL DEFAULT 0,
    red_cards         SMALLINT  NOT NULL DEFAULT 0,
    saves             SMALLINT  NOT NULL DEFAULT 0,
    goals_conceded    SMALLINT  NOT NULL DEFAULT 0,
    clean_sheet       BOOLEAN   NOT NULL DEFAULT FALSE,
    rating            NUMERIC(3, 1) CHECK (rating BETWEEN 3.0 AND 10.0),   -- NULL when he played too little
    PRIMARY KEY (match_id, player_id)
);

CREATE INDEX idx_player_match_stats_player ON player_match_stats (player_id);

CREATE TABLE team_match_stats (
    match_id            INT       NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    team_id             INT       NOT NULL REFERENCES teams (id),
    goals               SMALLINT  NOT NULL,
    shots               SMALLINT  NOT NULL,
    shots_on_target     SMALLINT  NOT NULL,
    corners             SMALLINT  NOT NULL,
    fouls               SMALLINT  NOT NULL,
    offsides            SMALLINT  NOT NULL,
    saves               SMALLINT  NOT NULL,
    yellow_cards        SMALLINT  NOT NULL,
    red_cards           SMALLINT  NOT NULL,
    possession_percent  SMALLINT  NOT NULL CHECK (possession_percent BETWEEN 0 AND 100),
    PRIMARY KEY (match_id, team_id)
);

-- Fitness and availability between rounds, so a season can be continued later
CREATE TABLE player_season_state (
    season_id                 INT       NOT NULL REFERENCES seasons (id) ON DELETE CASCADE,
    player_id                 INT       NOT NULL REFERENCES players (id),
    condition                 SMALLINT  NOT NULL CHECK (condition BETWEEN 0 AND 100),
    yellow_cards_accumulated  SMALLINT  NOT NULL DEFAULT 0,
    matches_suspended         SMALLINT  NOT NULL DEFAULT 0,
    matches_injured           SMALLINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (season_id, player_id)
);

-- The league table calculated in SQL from the played matches.
-- Every match counts twice: once for the home team and once for the away team.
-- Order: points, goal difference, goals scored (head-to-head is applied in Java).
CREATE VIEW v_standings AS
WITH results AS (
    SELECT season_id, home_team_id AS team_id, home_goals AS goals_for, away_goals AS goals_against
    FROM matches WHERE status = 'PLAYED'
    UNION ALL
    SELECT season_id, away_team_id, away_goals, home_goals
    FROM matches WHERE status = 'PLAYED'
)
SELECT r.season_id,
       t.id                                                    AS team_id,
       t.name                                                  AS team_name,
       COUNT(*)                                                AS played,
       COUNT(*) FILTER (WHERE r.goals_for > r.goals_against)   AS won,
       COUNT(*) FILTER (WHERE r.goals_for = r.goals_against)   AS drawn,
       COUNT(*) FILTER (WHERE r.goals_for < r.goals_against)   AS lost,
       SUM(r.goals_for)                                        AS goals_for,
       SUM(r.goals_against)                                    AS goals_against,
       SUM(r.goals_for) - SUM(r.goals_against)                 AS goal_difference,
       3 * COUNT(*) FILTER (WHERE r.goals_for > r.goals_against)
         + COUNT(*) FILTER (WHERE r.goals_for = r.goals_against) AS points
FROM results r
JOIN teams t ON t.id = r.team_id
GROUP BY r.season_id, t.id, t.name;
