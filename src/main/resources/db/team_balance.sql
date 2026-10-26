-- Makes Real Madrid, Barcelona and Bayern München the usual title contenders.
-- Barcelona +2 and Bayern +1 on every attribute, AC Milan and Manchester United -1.
-- (Tested with CalibrationRunner over 30 seasons: the three finish on about 77 points, 2nd–3rd on average.)
-- Run it once after seed.sql. Running it again changes nothing: it remembers that it was applied.

CREATE TABLE IF NOT EXISTS applied_changes (
    name       VARCHAR(50) PRIMARY KEY,
    applied_at TIMESTAMP NOT NULL DEFAULT now()
);

DO $$
DECLARE
    change RECORD;
BEGIN
    IF EXISTS (SELECT 1 FROM applied_changes WHERE name = 'team-balance-1') THEN
        RAISE NOTICE 'team-balance-1 was already applied';
        RETURN;
    END IF;

    FOR change IN
        SELECT * FROM (VALUES ('BAR', 2), ('BAY', 1), ('MIL', -1), ('MUN', -1)) AS c(short_name, delta)
    LOOP
        UPDATE players
        SET pace      = LEAST(99, GREATEST(1, pace + change.delta)),
            shooting  = LEAST(99, GREATEST(1, shooting + change.delta)),
            passing   = LEAST(99, GREATEST(1, passing + change.delta)),
            dribbling = LEAST(99, GREATEST(1, dribbling + change.delta)),
            defending = LEAST(99, GREATEST(1, defending + change.delta)),
            physical  = LEAST(99, GREATEST(1, physical + change.delta)),
            heading   = LEAST(99, GREATEST(1, heading + change.delta)),
            reflexes  = LEAST(99, GREATEST(1, reflexes + change.delta)),
            handling  = LEAST(99, GREATEST(1, handling + change.delta))
        WHERE team_id = (SELECT id FROM teams WHERE short_name = change.short_name);
    END LOOP;

    INSERT INTO applied_changes (name) VALUES ('team-balance-1');
END $$;
