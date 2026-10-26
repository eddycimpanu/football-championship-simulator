-- Football Championship Simulator: betting tables
-- Run this once on an existing database (after season_schema.sql). It only adds what is missing,
-- so your saved seasons stay as they are. Run it again after re-running season_schema.sql.

-- The odds of every match, calculated before its round: one row per selection
CREATE TABLE IF NOT EXISTS match_odds (
    match_id     INT           NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    selection    VARCHAR(16)   NOT NULL CHECK (selection IN (
                     'HOME', 'DRAW', 'AWAY', 'OVER_2_5', 'UNDER_2_5', 'BOTH_SCORE', 'NOT_BOTH_SCORE')),
    probability  NUMERIC(5, 4) NOT NULL CHECK (probability > 0 AND probability < 1),
    odds         NUMERIC(7, 2) NOT NULL CHECK (odds >= 1.01),
    simulations  INT           NOT NULL CHECK (simulations > 0),
    created_at   TIMESTAMP     NOT NULL DEFAULT now(),
    PRIMARY KEY (match_id, selection)
);

-- Everyone who bets in a season: you and, later, the AI agents. Everyone starts each season with the same credits.
CREATE TABLE IF NOT EXISTS bettors (
    id                SERIAL PRIMARY KEY,
    season_id         INT            NOT NULL REFERENCES seasons (id) ON DELETE CASCADE,
    name              VARCHAR(40)    NOT NULL,
    kind              VARCHAR(5)     NOT NULL CHECK (kind IN ('HUMAN', 'AGENT')),
    strategy          VARCHAR(30),
    starting_balance  NUMERIC(12, 2) NOT NULL CHECK (starting_balance > 0),
    balance           NUMERIC(12, 2) NOT NULL CHECK (balance >= 0),
    CONSTRAINT uq_bettors_season_name UNIQUE (season_id, name)
);

-- The first version had single bets in a table called bets. Tickets replaced it: if it is still there,
-- it is removed and the balances start again from the starting credits (runs only once).
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'bets') THEN
        DROP TABLE bets;
        UPDATE bettors SET balance = starting_balance;
    END IF;
END $$;

-- A ticket: one stake on one or more selections. It wins only if every selection wins;
-- then it pays stake x total odds (the odds of the selections multiplied together).
CREATE TABLE IF NOT EXISTS tickets (
    id          SERIAL PRIMARY KEY,
    bettor_id   INT            NOT NULL REFERENCES bettors (id) ON DELETE CASCADE,
    round       SMALLINT       NOT NULL CHECK (round >= 1),
    stake       NUMERIC(12, 2) NOT NULL CHECK (stake > 0),
    total_odds  NUMERIC(14, 2) NOT NULL CHECK (total_odds >= 1.01),
    status      VARCHAR(4)     NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'WON', 'LOST')),
    payout      NUMERIC(14, 2),
    placed_at   TIMESTAMP      NOT NULL DEFAULT now(),
    settled_at  TIMESTAMP,
    CONSTRAINT chk_tickets_payout_when_settled CHECK (status = 'OPEN' OR payout IS NOT NULL)
);

-- The selections on a ticket: at most one per match
CREATE TABLE IF NOT EXISTS ticket_selections (
    id          SERIAL PRIMARY KEY,
    ticket_id   INT           NOT NULL REFERENCES tickets (id) ON DELETE CASCADE,
    match_id    INT           NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    selection   VARCHAR(16)   NOT NULL CHECK (selection IN (
                    'HOME', 'DRAW', 'AWAY', 'OVER_2_5', 'UNDER_2_5', 'BOTH_SCORE', 'NOT_BOTH_SCORE')),
    odds        NUMERIC(7, 2) NOT NULL CHECK (odds >= 1.01),
    status      VARCHAR(4)    NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'WON', 'LOST')),
    CONSTRAINT uq_ticket_selections_match UNIQUE (ticket_id, match_id)
);

CREATE INDEX IF NOT EXISTS idx_tickets_bettor ON tickets (bettor_id);
CREATE INDEX IF NOT EXISTS idx_ticket_selections_match ON ticket_selections (match_id);
