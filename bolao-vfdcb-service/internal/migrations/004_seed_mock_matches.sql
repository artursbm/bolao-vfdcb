-- +goose Up
INSERT INTO teams (name, code) VALUES
    ('Brazil', 'BRA'),
    ('Argentina', 'ARG'),
    ('France', 'FRA'),
    ('England', 'ENG'),
    ('Germany', 'GER'),
    ('Spain', 'ESP'),
    ('Portugal', 'POR'),
    ('Italy', 'ITA');

-- +goose StatementBegin
DO $$
DECLARE
    bra UUID;
    arg UUID;
    fra UUID;
    eng UUID;
    ger UUID;
    esp UUID;
    por UUID;
    ita UUID;
BEGIN
    SELECT id INTO bra FROM teams WHERE code = 'BRA';
    SELECT id INTO arg FROM teams WHERE code = 'ARG';
    SELECT id INTO fra FROM teams WHERE code = 'FRA';
    SELECT id INTO eng FROM teams WHERE code = 'ENG';
    SELECT id INTO ger FROM teams WHERE code = 'GER';
    SELECT id INTO esp FROM teams WHERE code = 'ESP';
    SELECT id INTO por FROM teams WHERE code = 'POR';
    SELECT id INTO ita FROM teams WHERE code = 'ITA';

    INSERT INTO matches (home_team_id, away_team_id, match_time) VALUES
        (bra, arg, '2026-06-11 18:00:00+00'),
        (fra, eng, '2026-06-12 15:00:00+00'),
        (ger, esp, '2026-06-13 18:00:00+00'),
        (por, ita, '2026-06-14 21:00:00+00'),
        (bra, fra, '2026-06-18 18:00:00+00'),
        (arg, ger, '2026-06-19 21:00:00+00');
END $$;
-- +goose StatementEnd

-- +goose Down
DELETE FROM matches;
DELETE FROM teams;
