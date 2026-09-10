-- ===========================================================================
-- 00_baseline_schema.sql
--
-- Faithful mirror of the runtiva contest-svc production schema, restricted to
-- the objects the non-H2H join path actually touches.
--
-- Source of truth: runtiva/contest-svc/db-migration/migrations/
--   1_enum_sport, 4_enums_contest_types, 5_enums_contest_inst_types,
--   6_enums_contest_match_types, 20_contest_group_match, 21_contest,
--   25_contest_inst, 30_contest_participant_team, 31_contest_inst_team,
--   43_id_generator, 70_payout_saga_currency_and_team_constraint
--
-- RULE FOR THIS FILE: the baseline must reproduce production INCLUDING ITS
-- FLAWS. Anything that looks like it should be "fixed" here is either
-- (a) deliberately preserved because it is part of what we measure, or
-- (b) called out in a WHY comment. Do not tidy this file.
--
-- Run as:  psql -v ON_ERROR_STOP=1 -d rowlock_bench -f sql/00_baseline_schema.sql
-- ===========================================================================

\set ON_ERROR_STOP on

-- ---------------------------------------------------------------------------
-- Extensions
-- ---------------------------------------------------------------------------
-- pg_stat_statements needs shared_preload_libraries = 'pg_stat_statements'
-- (see conf/postgresql.bench.conf). CREATE EXTENSION here will succeed even
-- without the preload, but pg_stat_statements_reset() will then error at run
-- time. 90_reset.sql checks for this explicitly rather than letting a run
-- proceed with no statement attribution.
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- pgstattuple gives us real dead-tuple and free-space numbers for the bloat
-- capture in 92_observe.sql. Estimating bloat from pg_class.reltuples is
-- guesswork and this benchmark stands or falls on measurement honesty.
CREATE EXTENSION IF NOT EXISTS pgstattuple;

-- ---------------------------------------------------------------------------
-- Schemas
-- ---------------------------------------------------------------------------
-- contest : the production mirror. Nothing harness-specific goes in here.
-- bench   : everything the harness owns (fixtures, provisioning, verification,
--           observability). Keeping them apart means a reader can always tell
--           "is this production behaviour or benchmark scaffolding?".
CREATE SCHEMA IF NOT EXISTS contest;
CREATE SCHEMA IF NOT EXISTS bench;

-- ---------------------------------------------------------------------------
-- Enums (production: migrations 4, 5, 6)
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'contest_inst_status') THEN
        CREATE TYPE contest_inst_status AS ENUM (
            'OPEN', 'FULL',
            'IN_PROGRESS', 'SCORING', 'RANKED', 'COMPLETED',
            'CANCELLING', 'CANCELLED',
            'VOIDING', 'VOIDED',
            'COMPLIANCE_HOLD', 'DISPUTE', 'REPROCESSING',
            'ERROR', 'PARTIAL_ERROR'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'contest_published_status') THEN
        CREATE TYPE contest_published_status AS ENUM ('DRAFT', 'ACTIVE', 'INACTIVE');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'contest_status') THEN
        CREATE TYPE contest_status AS ENUM (
            'CREATED', 'IN_PROGRESS', 'POST_PROCESSING', 'COMPLETED',
            'VOIDED', 'CANCELLED', 'DISPUTED'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'contest_match_status') THEN
        CREATE TYPE contest_match_status AS ENUM ('SCHEDULED', 'LOCKED', 'LIVE', 'COMPLETED');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'contest_match_completed_state') THEN
        CREATE TYPE contest.contest_match_completed_state AS ENUM (
            'FINAL', 'POSTPONED', 'SUSPENDED', 'FORFEIT',
            'CANCELLED', 'VACATED', 'NO_CONTEST_FORFEIT'
        );
    END IF;
END
$$;

-- ---------------------------------------------------------------------------
-- public.id_generator  (production: migration 43)
-- ---------------------------------------------------------------------------
-- Snowflake-ish: 41 bits of ms since a custom epoch, 13 bits of shard id,
-- 10 bits of sequence. Reproduced verbatim because contest_inst ids in the
-- baseline are minted by it inside join_contest_get_contest_inst, and its
-- nextval() is itself a (very cheap) shared resource on the join path.
CREATE SEQUENCE IF NOT EXISTS public.global_id_seq;

CREATE OR REPLACE FUNCTION public.id_generator()
    RETURNS bigint
    LANGUAGE plpgsql
AS $BODY$
DECLARE
    our_epoch  bigint := 1314220021721;
    seq_id     bigint;
    now_millis bigint;
    shard_id   int := 1;
    result     bigint := 0;
BEGIN
    SELECT nextval('public.global_id_seq') % 1024 INTO seq_id;
    SELECT FLOOR(EXTRACT(EPOCH FROM clock_timestamp()) * 1000) INTO now_millis;
    result := (now_millis - our_epoch) << 23;
    result := result | (shard_id << 10);
    result := result | (seq_id);
    RETURN result;
END;
$BODY$;

-- ---------------------------------------------------------------------------
-- Reference-table stubs
-- ---------------------------------------------------------------------------
-- contest has FKs to team_combination / awards_schedule / scoring_rule /
-- booster_config. None of them are read on the join path, but the FK
-- CONSTRAINTS are kept real rather than dropped, because dropping a constraint
-- changes the plan and the row width of contest, and contest is a hot row in
-- the baseline (UPDATE ... teams_joined_count on every single join). We keep
-- the shape exact and stub out only the bodies of the parent tables.
CREATE TABLE IF NOT EXISTS contest.team_combination  (id BIGINT PRIMARY KEY);
CREATE TABLE IF NOT EXISTS contest.awards_schedule   (id BIGINT PRIMARY KEY);
CREATE TABLE IF NOT EXISTS contest.scoring_rule      (id BIGINT PRIMARY KEY);
CREATE TABLE IF NOT EXISTS contest.booster_config    (id BIGINT PRIMARY KEY);

-- ---------------------------------------------------------------------------
-- contest.contest_match_group  (production: migration 20)
-- ---------------------------------------------------------------------------
-- Read once per join by join_contest's RETURN QUERY (for match_group_name).
CREATE TABLE IF NOT EXISTS contest.contest_match_group (
    id                 BIGINT PRIMARY KEY,
    group_key          TEXT UNIQUE NOT NULL CHECK (char_length(group_key) <= 255),
    match_group_status contest_match_status NOT NULL DEFAULT 'SCHEDULED',
    sport_id           INT NOT NULL CHECK (sport_id > 0 AND sport_id <= 10),
    name               TEXT NOT NULL CHECK (char_length(name) <= 100),
    description        TEXT CHECK (char_length(description) <= 255),
    tags               JSONB,
    priority           INT NOT NULL DEFAULT 0 CHECK (priority >= 0 AND priority <= 3),
    weight             INT NOT NULL DEFAULT 0 CHECK (weight >= 0),
    contest_tags       JSONB,
    is_popular         BOOLEAN DEFAULT FALSE,
    is_multi_match     BOOLEAN NOT NULL DEFAULT FALSE,
    series_key         TEXT NOT NULL CHECK (char_length(series_key) <= 100),
    series_name        TEXT NOT NULL CHECK (char_length(series_name) <= 150),
    series_short_name  TEXT NOT NULL CHECK (char_length(series_short_name) <= 50),
    is_active          BOOLEAN DEFAULT TRUE NOT NULL,
    slate_key          TEXT NULL CHECK (slate_key IS NULL OR char_length(slate_key) <= 50),
    slate_id           BIGINT NULL,
    salary_cap         INT NULL CHECK (salary_cap IS NULL OR salary_cap > 0),
    lock_time          TIMESTAMPTZ NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------------
-- contest.contest_match_group_match  (production: migration 20, MODIFIED)
-- ---------------------------------------------------------------------------
-- DELIBERATE DEVIATION FROM PRODUCTION, at the operator's instruction.
--
-- In production this table stores only match_key, and the service then makes a
-- cross-database call to sports_db to hydrate the match. That cross-DB hop is
-- out of scope for a row-lock benchmark: it happens on the read path, not on
-- the join path, and modelling it would add network variance to numbers that
-- are supposed to isolate lock behaviour.
--
-- Instead the match detail is denormalised into this table. It is never read by
-- join_contest or join_contest_sharded -- it exists so the fixture is a
-- realistic contest, not a bare skeleton.
CREATE TABLE IF NOT EXISTS contest.contest_match_group_match (
    contest_match_group_id BIGINT NOT NULL,
    match_key              TEXT NOT NULL CHECK (char_length(match_key) <= 50),
    scheduled_start_time   TIMESTAMPTZ NOT NULL,
    delayed_start_time     TIMESTAMPTZ DEFAULT NULL,
    status                 contest_match_status NOT NULL DEFAULT 'SCHEDULED',
    completed_state        contest.contest_match_completed_state NULL,
    verified_at            TIMESTAMPTZ DEFAULT NULL,

    -- Denormalised match detail (replaces the sports_db lookup).
    -- Mirrors the Go struct the operator specified.
    match_id               BIGINT NOT NULL,
    match_name             TEXT   NOT NULL CHECK (char_length(match_name) <= 150),
    match_short_name       TEXT   NOT NULL CHECK (char_length(match_short_name) <= 50),
    sub_title              TEXT   NOT NULL CHECK (char_length(sub_title) <= 100),
    title                  TEXT   NULL CHECK (title IS NULL OR char_length(title) <= 150),
    sport_id               INT    NOT NULL CHECK (sport_id > 0 AND sport_id <= 10),
    series_key             TEXT   NOT NULL CHECK (char_length(series_key) <= 100),
    series_name            TEXT   NOT NULL CHECK (char_length(series_name) <= 150),
    series_short_name      TEXT   NOT NULL CHECK (char_length(series_short_name) <= 50),
    start_at               TIMESTAMPTZ NOT NULL,
    expected_start_at      TIMESTAMPTZ NOT NULL,

    PRIMARY KEY (contest_match_group_id, match_key),
    FOREIGN KEY (contest_match_group_id) REFERENCES contest.contest_match_group(id)
);

CREATE INDEX IF NOT EXISTS contest_contest_match_group_group_key_idx
    ON contest.contest_match_group(group_key);
CREATE INDEX IF NOT EXISTS contest_contest_match_group_sport_id_idx
    ON contest.contest_match_group(sport_id);
CREATE INDEX IF NOT EXISTS contest_contest_match_group_series_key_idx
    ON contest.contest_match_group(series_key);
CREATE INDEX IF NOT EXISTS contest_match_group_match_match_key_idx
    ON contest.contest_match_group_match(match_key);

-- ---------------------------------------------------------------------------
-- contest.contest  (production: migration 21)
-- ---------------------------------------------------------------------------
-- Every column and every index is preserved.
--
-- WHY THE INDEXES MATTER: the baseline runs
--     UPDATE contest.contest SET teams_joined_count = ... WHERE id = _contest_id
-- on EVERY join. teams_joined_count is not itself indexed, so that update is
-- HOT-eligible -- but only while the page has free space. contest is created
-- with the default fillfactor of 100 (production does not override it), so a
-- single-row-per-page table under a million updates exhausts its free space
-- almost immediately, HOT chains stop forming, and each update then writes
-- into all 10 indexes below. Removing the indexes "to keep the benchmark
-- focused" would delete the flaw we are trying to measure.
CREATE TABLE IF NOT EXISTS contest.contest (
    id                          BIGINT PRIMARY KEY,
    name                        VARCHAR(150) NOT NULL,
    short_name                  VARCHAR(50) NOT NULL,
    sport_id                    INT NOT NULL CHECK (sport_id > 0 AND sport_id <= 10),
    game_type                   SMALLINT NOT NULL CHECK (game_type > 0 AND game_type <= 2),
    contest_type                SMALLINT NOT NULL CHECK (contest_type > 0 AND contest_type <= 3),
    tags                        JSONB NOT NULL,
    is_free                     BOOLEAN DEFAULT FALSE NOT NULL,
    run_coins_allowed           BOOLEAN DEFAULT FALSE NOT NULL,
    currency_code               VARCHAR(3) NOT NULL,
    entry_fee                   NUMERIC NOT NULL CHECK (entry_fee >= 0),
    min_no_of_entries           INT NOT NULL CHECK (min_no_of_entries >= 0),
    max_no_of_entries           INT NOT NULL CHECK (max_no_of_entries >= min_no_of_entries),
    max_entries_per_user        INT NULL,   -- NULL = no limit
    prize_pool                  NUMERIC NOT NULL DEFAULT 0,
    number_of_winners           INT NOT NULL DEFAULT 0,
    is_private                  BOOLEAN DEFAULT FALSE NOT NULL,
    owner_id                    BIGINT DEFAULT NULL,
    is_cancelable               BOOLEAN DEFAULT TRUE NOT NULL,
    auto_generate               BOOLEAN DEFAULT FALSE NOT NULL,
    team_combination_id         BIGINT NOT NULL,
    awards_schedule_id          BIGINT NOT NULL,
    scoring_rule_id             BIGINT NOT NULL,
    booster_config_id           BIGINT NOT NULL,
    match_group_id              BIGINT NOT NULL,
    slate_key                   TEXT NULL CHECK (slate_key IS NULL OR char_length(slate_key) <= 50),
    slate_id                    BIGINT NULL,
    published_status            contest_published_status NOT NULL DEFAULT 'DRAFT',
    status                      contest_status NOT NULL DEFAULT 'CREATED',
    teams_joined_count          INT DEFAULT 0,
    join_close_time             TIMESTAMPTZ NOT NULL,
    delayed_join_close_time     TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    FOREIGN KEY (team_combination_id) REFERENCES contest.team_combination(id),
    FOREIGN KEY (awards_schedule_id)  REFERENCES contest.awards_schedule(id),
    FOREIGN KEY (scoring_rule_id)     REFERENCES contest.scoring_rule(id),
    FOREIGN KEY (booster_config_id)   REFERENCES contest.booster_config(id),
    FOREIGN KEY (match_group_id)      REFERENCES contest.contest_match_group(id)
);

CREATE INDEX IF NOT EXISTS contest_contest_contest_type_idx      ON contest.contest(contest_type);
CREATE INDEX IF NOT EXISTS contest_contest_published_status_idx  ON contest.contest(published_status);
CREATE INDEX IF NOT EXISTS contest_contest_status_idx            ON contest.contest(status);
CREATE INDEX IF NOT EXISTS contest_contest_sport_id_idx          ON contest.contest(sport_id);
CREATE INDEX IF NOT EXISTS contest_contest_owner_id_idx          ON contest.contest(owner_id);
CREATE INDEX IF NOT EXISTS contest_contest_awards_schedule_id_idx ON contest.contest(awards_schedule_id);
CREATE INDEX IF NOT EXISTS contest_contest_team_combination_id_idx ON contest.contest(team_combination_id);
CREATE INDEX IF NOT EXISTS contest_contest_scoring_rule_id_idx   ON contest.contest(scoring_rule_id);
CREATE INDEX IF NOT EXISTS contest_contest_booster_config_id_idx ON contest.contest(booster_config_id);
CREATE INDEX IF NOT EXISTS contest_contest_slate_key_idx
    ON contest.contest(slate_key) WHERE slate_key IS NOT NULL;

-- ---------------------------------------------------------------------------
-- contest.contest_inst  (production: migration 25)
-- ---------------------------------------------------------------------------
-- THE contended row in the baseline. joined_participants_count is the single
-- shared counter that row-level sharding replaces.
--
-- Note there is no fillfactor override in production either. A one-row table
-- taking a million updates on default fillfactor 100 is exactly the shape that
-- produces the bloat we want to observe.
CREATE TABLE IF NOT EXISTS contest.contest_inst (
    id                        BIGINT PRIMARY KEY,
    contest_id                BIGINT NOT NULL,
    joined_participants_count INT NOT NULL DEFAULT 0 CHECK (joined_participants_count >= 0),
    status                    contest_inst_status NOT NULL DEFAULT 'OPEN',
    FOREIGN KEY (contest_id) REFERENCES contest.contest(id)
);

CREATE INDEX IF NOT EXISTS contest_contest_inst_contest_id_idx ON contest.contest_inst(contest_id);
-- status IS indexed in production. That means the "set FULL on the last join"
-- update in join_contest changes an indexed column and can never be HOT.
CREATE INDEX IF NOT EXISTS contest_contest_inst_status_idx     ON contest.contest_inst(status);

-- ---------------------------------------------------------------------------
-- contest.contest_participant_team  (production: migration 30)
-- ---------------------------------------------------------------------------
-- The user's saved lineup. One row per (user, match_group, lineup). The
-- baseline's per-user cap check joins contest_inst_team against this table to
-- count how many entries a user already has -- a COUNT(*) over a growing table
-- executed while holding the advisory lock. That is the second-largest cost in
-- the critical section after the fsync.
CREATE TABLE IF NOT EXISTS contest.contest_participant_team (
    id                      BIGINT PRIMARY KEY,
    user_id                 BIGINT NOT NULL,
    match_group_id          BIGINT NOT NULL,
    team_name               VARCHAR(100) NOT NULL,
    players                 JSONB NOT NULL,
    players_hash            TEXT NULL,
    team_combination_id     BIGINT NOT NULL,
    booster_config_id       BIGINT NOT NULL,
    game_type               INT8 NOT NULL CHECK (game_type > 0 AND game_type <= 2),
    empty_team              BOOLEAN DEFAULT FALSE,

    FOREIGN KEY (booster_config_id)   REFERENCES contest.booster_config(id),
    FOREIGN KEY (team_combination_id) REFERENCES contest.team_combination(id)
);

CREATE INDEX IF NOT EXISTS contest_contest_participant_team_booster_config_id_idx
    ON contest.contest_participant_team(booster_config_id);
CREATE INDEX IF NOT EXISTS contest_contest_participant_team_user_id_idx
    ON contest.contest_participant_team(user_id);
CREATE INDEX IF NOT EXISTS contest_contest_participant_team_team_combination_id_idx
    ON contest.contest_participant_team(team_combination_id);
CREATE UNIQUE INDEX IF NOT EXISTS unique_team_hash_per_user_match_game
    ON contest.contest_participant_team(user_id, match_group_id, game_type, players_hash);

-- ---------------------------------------------------------------------------
-- contest.contest_inst_team  (production: migration 31, + 67/70/80/124)
-- ---------------------------------------------------------------------------
-- The join record. Shared by BOTH benchmark paths, deliberately: an identical
-- insert target on both sides means any cost it carries (FK locks, index
-- maintenance, WAL volume) cancels out of the A/B comparison instead of
-- biasing it.
--
-- >>> THE unique_join_id INDEX IS NOT DEFINED HERE. <<<
--
-- Production migration 31 declares UNIQUE (unique_join_id). The benchmark
-- specification calls for the baseline to run WITHOUT it, so that Experiment
-- C1 can observe a genuine duplicate row rather than a unique-violation error.
-- These two are in direct conflict, so the index is owned by 90_reset.sql and
-- created or dropped per run according to :uniq_join_id. Both modes are
-- reportable and they are different findings:
--
--   uniq_join_id = on  (production-faithful)
--       The pre-check "IF EXISTS (SELECT ... WHERE unique_join_id = ...)" in
--       join_contest runs BEFORE any lock is taken, so N concurrent retries of
--       the same unique_join_id all pass it. The unique index then serialises
--       them: one commits, the rest fail with SQLSTATE 23505. No double row --
--       but the caller gets a hard error where the contract promises an
--       idempotent replay. The pre-check is decorative.
--
--   uniq_join_id = off (as specified)
--       Nothing serialises the duplicates. N concurrent retries produce N rows.
--       That is a double-charge.
--
-- Run C1 both ways; see sql/91_verify.sql check 'no_duplicate_unique_join_id'.
--
-- The UNIQUE (contest_inst_id, team_id) constraint IS defined here because it
-- is present in production and required by both paths (invariant I2).
CREATE TABLE IF NOT EXISTS contest.contest_inst_team (
    contest_inst_id         BIGINT NOT NULL,
    team_id                 BIGINT NOT NULL,
    saga_id                 BIGINT NOT NULL,
    saga_status             VARCHAR(20) NOT NULL DEFAULT 'P' CHECK (saga_status IN ('P', 'C')),
    saga_pending_timestamp  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    points                  DECIMAL(10,2) DEFAULT 0 NOT NULL,
    rank                    INT DEFAULT 0 NOT NULL,
    currency_code           VARCHAR(3) DEFAULT NULL,
    winning_amount          DECIMAL(20,2) NOT NULL DEFAULT 0.0,
    run_coins_awarded       INT NOT NULL DEFAULT 0,
    unique_join_id          VARCHAR(24) NOT NULL,
    scoring_version         INT NOT NULL DEFAULT 1 CHECK (scoring_version >= 1),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- These FKs fire on every insert and take a FOR KEY SHARE lock on the
    -- parent row. contest_inst_id points at ONE row for the whole mega
    -- contest, so every join in flight tags the same tuple's t_xmax and a
    -- multixact is created. KEY SHARE locks do not conflict with each other,
    -- so this does not serialise -- but it is a shared cache line and it will
    -- appear in the wait-event data as MultiXact* / BufferContent once the
    -- advisory lock stops hiding it. Kept on both paths so it does not bias
    -- the comparison; isolating it is a candidate follow-up experiment.
    FOREIGN KEY (contest_inst_id) REFERENCES contest.contest_inst(id),
    FOREIGN KEY (team_id)         REFERENCES contest.contest_participant_team(id),

    CONSTRAINT contest_inst_team_inst_team_key UNIQUE (contest_inst_id, team_id),
    CONSTRAINT chk_contest_inst_team_currency_code
        CHECK (currency_code IS NULL OR currency_code IN ('USD', 'INR'))
);

CREATE INDEX IF NOT EXISTS contest_contest_inst_team_team_id_idx
    ON contest.contest_inst_team(team_id);
CREATE INDEX IF NOT EXISTS contest_contest_inst_team_contest_inst_id_idx
    ON contest.contest_inst_team(contest_inst_id);

-- ---------------------------------------------------------------------------
-- bench.run_outcome
-- ---------------------------------------------------------------------------
-- Harness scaffolding, NOT part of the production mirror.
--
-- Two of the mandated correctness gates cannot be expressed in SQL alone:
--   "every reported success has a corresponding row"
--   "every reported failure has NO row"  (no phantom writes)
-- Both compare what the CLIENT believes happened against what the database
-- actually holds. The Go harness COPYs one row per request in here after the
-- measurement window closes, and 91_verify.sql joins against it. If the table
-- is empty those two checks report SKIPPED rather than silently passing.
CREATE TABLE IF NOT EXISTS bench.run_outcome (
    unique_join_id  VARCHAR(24) PRIMARY KEY,
    outcome         TEXT NOT NULL CHECK (outcome IN ('success', 'idempotent', 'failure')),
    sqlstate        TEXT NULL,
    contest_inst_id BIGINT NULL,
    team_id         BIGINT NULL
);

-- ---------------------------------------------------------------------------
-- bench.run_meta
-- ---------------------------------------------------------------------------
-- One row per run, written by the harness at run start. Lets a results file be
-- traced back to the exact database state it was produced against.
CREATE TABLE IF NOT EXISTS bench.run_meta (
    run_id          TEXT PRIMARY KEY,
    mode            TEXT NOT NULL CHECK (mode IN ('baseline', 'sharded')),
    contest_id      BIGINT NOT NULL,
    contest_inst_id BIGINT NULL,
    n_shards        INT NULL,
    capacity        INT NOT NULL,
    uniq_join_id    BOOLEAN NOT NULL,
    started_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
