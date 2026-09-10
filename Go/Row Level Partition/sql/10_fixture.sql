-- ===========================================================================
-- 10_fixture.sql
--
-- Fixture builders. Harness scaffolding -- none of this is production code.
--
-- The fixture is one mega contest with one OPEN instance and a pool of
-- pre-created participant teams. Team ids are DENSE AND DETERMINISTIC
-- (_first_team_id .. _first_team_id + n - 1) so the Go load generator can pick
-- a team by arithmetic instead of querying for one. A generator that has to
-- SELECT a team before every join is measuring its own SELECT.
--
-- Run as:  psql -v ON_ERROR_STOP=1 -d rowlock_bench -f sql/10_fixture.sql
-- ===========================================================================

\set ON_ERROR_STOP on

-- Canonical fixture identifiers. Anything referencing them (Makefile, Go
-- config, smoke test) should use these values.
--   contest_id       500000
--   match_group_id   900001
--   first_team_id    1000000

-- ---------------------------------------------------------------------------
-- bench.seed_reference
-- ---------------------------------------------------------------------------
-- The four stub parents that contest has FKs to, plus the match group and its
-- matches. Idempotent.
CREATE OR REPLACE FUNCTION bench.seed_reference(_match_group_id BIGINT)
RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO contest.team_combination  (id) VALUES (1) ON CONFLICT DO NOTHING;
    INSERT INTO contest.awards_schedule   (id) VALUES (1) ON CONFLICT DO NOTHING;
    INSERT INTO contest.scoring_rule      (id) VALUES (1) ON CONFLICT DO NOTHING;
    INSERT INTO contest.booster_config    (id) VALUES (1) ON CONFLICT DO NOTHING;

    INSERT INTO contest.contest_match_group (
        id, group_key, match_group_status, sport_id, name, description,
        tags, priority, weight, contest_tags, is_popular, is_multi_match,
        series_key, series_name, series_short_name, is_active
    ) VALUES (
        _match_group_id,
        'mg_' || _match_group_id,
        'SCHEDULED',
        1,                                   -- cricket
        'IND vs AUS - Final',
        'World Cup Final',
        '{"mm_name":"world_cup_final"}'::jsonb,
        3, 0,
        '{"tags":"Mega 25K"}'::jsonb,
        TRUE, FALSE,
        'wc_2026', 'ICC World Cup 2026', 'WC26',
        TRUE
    ) ON CONFLICT (id) DO NOTHING;

    -- Single-match group. Match detail is denormalised here rather than fetched
    -- from sports_db (see the note in 00_baseline_schema.sql). Nothing on the
    -- join path reads it; it exists so the fixture is a real contest.
    INSERT INTO contest.contest_match_group_match (
        contest_match_group_id, match_key, scheduled_start_time, status,
        match_id, match_name, match_short_name, sub_title, title,
        sport_id, series_key, series_name, series_short_name,
        start_at, expected_start_at
    ) VALUES (
        _match_group_id,
        'wc_2026_final',
        NOW() + INTERVAL '4 hours',
        'SCHEDULED',
        7000001,
        'India vs Australia',
        'IND v AUS',
        'Final',
        'ICC World Cup 2026 Final',
        1, 'wc_2026', 'ICC World Cup 2026', 'WC26',
        NOW() + INTERVAL '4 hours',
        NOW() + INTERVAL '4 hours'
    ) ON CONFLICT (contest_match_group_id, match_key) DO NOTHING;
END;
$$;

-- ---------------------------------------------------------------------------
-- bench.seed_contest
-- ---------------------------------------------------------------------------
-- Creates (or resets) the mega contest and its single OPEN instance, and
-- returns the contest_inst_id.
--
-- auto_generate defaults to FALSE on purpose. With auto_generate = TRUE the
-- baseline spawns a fresh contest_inst the moment the current one fills, which
-- would make Experiment B measure instance churn instead of the tail of a
-- single counter. It also makes invariant I1 ambiguous -- "never oversell" is
-- per instance, and a path that keeps inventing instances can never be caught
-- overselling. One instance, fixed capacity, no escape hatch.
CREATE OR REPLACE FUNCTION bench.seed_contest(
    _contest_id           BIGINT,
    _match_group_id       BIGINT,
    _capacity             INT,
    _max_entries_per_user INT      DEFAULT NULL,   -- NULL = no per-user limit
    _is_free              BOOLEAN  DEFAULT TRUE,
    _auto_generate        BOOLEAN  DEFAULT FALSE
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_inst_id BIGINT;
BEGIN
    PERFORM bench.seed_reference(_match_group_id);

    INSERT INTO contest.contest (
        id, name, short_name, sport_id, game_type, contest_type, tags,
        is_free, run_coins_allowed, currency_code, entry_fee,
        min_no_of_entries, max_no_of_entries, max_entries_per_user,
        prize_pool, number_of_winners, is_private, owner_id, is_cancelable,
        auto_generate, team_combination_id, awards_schedule_id,
        scoring_rule_id, booster_config_id, match_group_id,
        published_status, status, teams_joined_count, join_close_time
    ) VALUES (
        _contest_id, 'Mega Contest 25K', 'Mega25K', 1, 1,
        2,                                    -- contest_type 2 = non-H2H
        '{"tier":"mega"}'::jsonb,
        _is_free, FALSE, 'INR',
        CASE WHEN _is_free THEN 0 ELSE 49 END,
        2, _capacity, _max_entries_per_user,
        2500000, 10000, FALSE, NULL, TRUE,
        _auto_generate, 1, 1, 1, 1, _match_group_id,
        'ACTIVE', 'CREATED', 0,
        NOW() + INTERVAL '4 hours'
    )
    ON CONFLICT (id) DO UPDATE SET
        max_no_of_entries    = EXCLUDED.max_no_of_entries,
        max_entries_per_user = EXCLUDED.max_entries_per_user,
        is_free              = EXCLUDED.is_free,
        auto_generate        = EXCLUDED.auto_generate,
        entry_fee            = EXCLUDED.entry_fee,
        published_status     = 'ACTIVE',
        status               = 'CREATED',
        teams_joined_count   = 0,
        -- Push the close time forward on every reset. A stale fixture whose
        -- join_close_time has passed makes every request fail with 01S07 and
        -- produces a beautiful, meaningless throughput number.
        join_close_time      = NOW() + INTERVAL '4 hours',
        delayed_join_close_time = NULL;

    -- Exactly one instance, id derived from the contest id rather than
    -- id_generator(), so a reset is reproducible and the Go harness can assert
    -- on a known value.
    v_inst_id := _contest_id * 1000 + 1;

    INSERT INTO contest.contest_inst (id, contest_id, joined_participants_count, status)
    VALUES (v_inst_id, _contest_id, 0, 'OPEN')
    ON CONFLICT (id) DO UPDATE SET
        joined_participants_count = 0,
        status                    = 'OPEN';

    RETURN v_inst_id;
END;
$$;

-- ---------------------------------------------------------------------------
-- bench.seed_teams
-- ---------------------------------------------------------------------------
-- Bulk-creates the participant-team pool.
--
--   team_id = _first_team_id + n          for n in [0, _n_teams)
--   user_id = _first_user_id + n / _teams_per_user
--
-- _teams_per_user controls how the offered load maps onto users, which is what
-- Experiment C3 (per-user cap) and the per-user branch of both join paths
-- actually exercise. At _teams_per_user = 1 every join is a different user and
-- the per-user check never contends; at 200 you get the hot-user shape a real
-- mega contest has.
--
-- Returns the number of rows inserted.
CREATE OR REPLACE FUNCTION bench.seed_teams(
    _match_group_id  BIGINT,
    _n_teams         INT,
    _teams_per_user  INT    DEFAULT 1,
    _first_team_id   BIGINT DEFAULT 1000000,
    _first_user_id   BIGINT DEFAULT 1
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_count BIGINT;
BEGIN
    IF _teams_per_user < 1 THEN
        RAISE EXCEPTION 'seed_teams: _teams_per_user must be >= 1, got %', _teams_per_user;
    END IF;

    INSERT INTO contest.contest_participant_team (
        id, user_id, match_group_id, team_name, players, players_hash,
        team_combination_id, booster_config_id, game_type, empty_team
    )
    SELECT
        _first_team_id + n,
        _first_user_id + (n / _teams_per_user),
        _match_group_id,
        'Team ' || n,
        -- A realistic-width lineup. Row width matters: contest_inst_team's FK
        -- check reads this row on every join, so an artificially narrow row
        -- would make the join path look cheaper than production.
        jsonb_build_object('players',
            (SELECT jsonb_agg(1000 + ((n * 11 + p) % 500)) FROM generate_series(0, 10) AS p)),
        -- Must be unique per (user_id, match_group_id, game_type, players_hash)
        -- because of unique_team_hash_per_user_match_game. Deriving it from n
        -- guarantees that without a collision retry loop.
        md5('team' || n),
        1, 1, 1, FALSE
    FROM generate_series(0, _n_teams - 1) AS g(n)
    ON CONFLICT (id) DO NOTHING;

    GET DIAGNOSTICS v_count = ROW_COUNT;
    RETURN v_count;
END;
$$;

-- ---------------------------------------------------------------------------
-- bench.fixture_fingerprint
-- ---------------------------------------------------------------------------
-- Cheap check for "is the existing team pool the one this run wants?".
--
-- Used by bench.reset_run to decide whether the participant-team pool must be
-- rebuilt. See the long justification in 90_reset.sql for why that table is
-- treated differently from every other table in the reset.
CREATE OR REPLACE FUNCTION bench.fixture_fingerprint(_match_group_id BIGINT)
RETURNS TABLE (n_teams BIGINT, n_users BIGINT, min_team_id BIGINT, max_team_id BIGINT)
LANGUAGE sql
STABLE
AS $$
    SELECT COUNT(*)::BIGINT,
           COUNT(DISTINCT user_id)::BIGINT,
           MIN(id),
           MAX(id)
    FROM contest.contest_participant_team
    WHERE match_group_id = _match_group_id;
$$;
