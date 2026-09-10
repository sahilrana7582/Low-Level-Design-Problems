-- ===========================================================================
-- 90_reset.sql
--
-- Fresh state for every run. Defines the reset functions, then performs one
-- reset using psql variables so a human can drive it by hand.
--
-- Manual use:
--   psql -v ON_ERROR_STOP=1 -d rowlock_bench \
--        -v mode=sharded -v n_shards=32 -v capacity=100000 \
--        -f sql/90_reset.sql
--
-- Variables (all optional, defaults below):
--   mode                 baseline | sharded          default baseline
--   contest_id           BIGINT                      default 500000
--   match_group_id       BIGINT                      default 900001
--   capacity             INT                         default 2000000
--   n_shards             INT                         default 32
--   max_entries_per_user INT or -1 for NULL          default -1
--   n_teams              INT                         default 1000000
--   teams_per_user       INT                         default 1
--   uniq_join_id         on | off | auto             default auto
--
-- THE RESET IS THREE PHASES AND THE ORDER IS LOAD-BEARING:
--   1. bench.reset_prepare  truncate, reseed, provision
--   2. VACUUM (ANALYZE)     cannot run inside a function or a transaction
--                           block, so it has to be a top-level statement
--   3. bench.reset_stats    LAST, so that the vacuum's own work and the
--                           reseed's million inserts are not sitting in the
--                           counters the run is about to measure
-- ===========================================================================

\set ON_ERROR_STOP on

-- ---------------------------------------------------------------------------
-- bench.assert_environment
-- ---------------------------------------------------------------------------
-- Fails loudly rather than letting a run produce a number that cannot mean
-- what it appears to mean.
CREATE OR REPLACE FUNCTION bench.assert_environment(_max_pool INT DEFAULT 256)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    v_max_conn INT;
    v_needed   INT;
    v_fsync    TEXT;
BEGIN
    -- pg_stat_statements: CREATE EXTENSION succeeds without the preload, but
    -- every call then fails. Probe it here, once, instead of discovering it
    -- mid-run when the stats reset blows up.
    BEGIN
        PERFORM pg_stat_statements_reset();
    EXCEPTION WHEN OTHERS THEN
        RAISE EXCEPTION
            E'pg_stat_statements is not loaded (%).\n'
            'Add to postgresql.conf and restart:\n'
            '    shared_preload_libraries = ''pg_stat_statements''\n'
            'See conf/postgresql.bench.conf.', SQLERRM;
    END;

    -- Pool sweep headroom. The sweep goes to 256; without spare connections
    -- the 128 and 256 steps fail on "too many clients already" and we would be
    -- measuring max_connections instead of lock contention.
    v_needed := _max_pool + 20;   -- + sampler, psql, autovacuum workers, slack
    SELECT setting::INT INTO v_max_conn FROM pg_settings WHERE name = 'max_connections';
    IF v_max_conn < v_needed THEN
        RAISE EXCEPTION
            E'max_connections = % but the pool sweep needs at least % (max pool % + 20 for the\n'
            'sampler, psql and autovacuum workers). Raise it in postgresql.conf and restart.\n'
            'See conf/postgresql.bench.conf.', v_max_conn, v_needed, _max_pool;
    END IF;

    -- fsync = off would make the baseline look far better than it is, because
    -- the baseline's entire problem is holding a lock across the commit flush.
    -- synchronous_commit is deliberately NOT checked: it is a swept variable.
    SELECT setting INTO v_fsync FROM pg_settings WHERE name = 'fsync';
    IF v_fsync <> 'on' THEN
        RAISE EXCEPTION
            'fsync = % . The baseline''s cost is the commit flush; disabling fsync deletes '
            'the thing under test. Turn it back on.', v_fsync;
    END IF;
END;
$$;

-- ---------------------------------------------------------------------------
-- bench.reset_prepare
-- ---------------------------------------------------------------------------
-- Returns the contest_inst_id the run should target.
--
-- WHY contest_participant_team IS NOT UNCONDITIONALLY TRUNCATED
-- ------------------------------------------------------------
-- Every other table here is truncated on every run, because every other table
-- is WRITTEN by the join path and therefore accumulates the dead tuples and
-- index bloat that must not leak from one run into the next.
--
-- contest_participant_team is different: neither join path ever writes to it.
-- It is read-only reference data. Rebuilding a million identical rows before
-- each run would add 10-15s to every one of the ~2,500 runs in the full
-- matrix -- roughly seven hours of pure reseeding -- to restore a table to a
-- state it never left.
--
-- So it is rebuilt only when its shape does not match what the run asked for
-- (bench.fixture_fingerprint), or when _force_reseed_teams is set. The
-- VACUUM (ANALYZE) in phase 2 still covers it, so its visibility map and
-- statistics are refreshed every run regardless. If you distrust this, pass
-- _force_reseed_teams => TRUE; the results should be identical, and if they
-- are not, that itself is a finding worth chasing.
CREATE OR REPLACE FUNCTION bench.reset_prepare(
    _mode                 TEXT,
    _contest_id           BIGINT,
    _match_group_id       BIGINT,
    _capacity             INT,
    _n_shards             INT,
    _max_entries_per_user INT     DEFAULT NULL,
    _n_teams              INT     DEFAULT 1000000,
    _teams_per_user       INT     DEFAULT 1,
    _uniq_join_id         BOOLEAN DEFAULT NULL,   -- NULL = auto (see below)
    _is_free              BOOLEAN DEFAULT TRUE,
    _auto_generate        BOOLEAN DEFAULT FALSE,
    _force_reseed_teams   BOOLEAN DEFAULT FALSE
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_inst_id  BIGINT;
    v_uniq     BOOLEAN;
    v_fp       RECORD;
    v_reseed   BOOLEAN := _force_reseed_teams;
BEGIN
    IF _mode NOT IN ('baseline', 'sharded') THEN
        RAISE EXCEPTION 'reset_prepare: _mode must be baseline or sharded, got %', _mode;
    END IF;

    -- unique_join_id index policy.
    --   sharded  -> always ON. I4 is enforced BY that index; without it the
    --               sharded path has no idempotency guarantee at all and its
    --               correctness gates are meaningless.
    --   baseline -> defaults OFF, matching the benchmark specification, so
    --               Experiment C1 observes real duplicate rows. Pass TRUE to
    --               run the production-faithful variant instead, in which the
    --               duplicates become SQLSTATE 23505 errors. Both are findings;
    --               see the long note in 00_baseline_schema.sql.
    v_uniq := COALESCE(_uniq_join_id, _mode = 'sharded');
    IF _mode = 'sharded' AND NOT v_uniq THEN
        RAISE EXCEPTION
            'reset_prepare: the sharded path enforces I4 with the unique index on '
            'unique_join_id. Running it with _uniq_join_id => FALSE would test a '
            'design nobody proposed.';
    END IF;

    -- 1. Truncate everything the run writes.
    -- contest is included: the baseline updates contest.teams_joined_count once
    -- per join on a table with 10 indexes, so it bloats as hard as
    -- contest_inst does. Leaving it in place would carry that bloat forward.
    TRUNCATE
        contest.contest_inst_team,
        contest.contest_inst_shard,
        contest.contest_inst_user_entry,
        contest.contest_inst,
        contest.contest,
        bench.run_outcome,
        bench.run_meta;

    IF _force_reseed_teams THEN
        -- contest_inst_team (its only FK child) was truncated above.
        TRUNCATE contest.contest_participant_team CASCADE;
    END IF;

    -- 2. Index policy must be applied while the table is empty. Creating a
    --    unique index on a populated table would either take minutes or fail.
    PERFORM bench.set_unique_join_id_index(v_uniq);

    -- 3. Rebuild the contest and its single OPEN instance.
    v_inst_id := bench.seed_contest(
        _contest_id           => _contest_id,
        _match_group_id       => _match_group_id,
        _capacity             => _capacity,
        _max_entries_per_user => _max_entries_per_user,
        _is_free              => _is_free,
        _auto_generate        => _auto_generate
    );

    -- 4. Team pool, only if its shape is wrong.
    IF NOT v_reseed THEN
        SELECT * INTO v_fp FROM bench.fixture_fingerprint(_match_group_id);
        v_reseed := (v_fp.n_teams IS DISTINCT FROM _n_teams::BIGINT)
                 OR (v_fp.n_users IS DISTINCT FROM
                        CEIL(_n_teams::NUMERIC / _teams_per_user)::BIGINT);
    END IF;

    IF v_reseed THEN
        TRUNCATE contest.contest_participant_team CASCADE;
        PERFORM bench.seed_teams(
            _match_group_id => _match_group_id,
            _n_teams        => _n_teams,
            _teams_per_user => _teams_per_user
        );
    END IF;

    -- 5. Shards. Provisioned for BOTH modes.
    --    In baseline mode the shard rows are never read or written -- they are
    --    provisioned anyway so that 91_verify.sql can run the identical
    --    SUM(cap) == max_no_of_entries assertion against both paths, and so
    --    that the two modes present the same physical layout to autovacuum.
    PERFORM bench.assert_no_index_on_used();
    PERFORM bench.provision_shards(v_inst_id, _n_shards, _capacity);

    RETURN v_inst_id;
END;
$$;

-- ---------------------------------------------------------------------------
-- bench.reset_stats
-- ---------------------------------------------------------------------------
-- Phase 3. Must run AFTER the VACUUM (ANALYZE), otherwise the run starts with
-- the reseed's inserts and the vacuum's page work already in the counters that
-- 92_observe.sql is about to read.
-- The pg_stat_statements branch WARNs instead of raising, deliberately. The
-- loud failure for a missing preload belongs in bench.assert_environment(),
-- which every real run calls before it starts; making this utility hard-fail
-- as well would block the Phase 1 smoke test on a postmaster restart it does
-- not need. A run that skips the statement reset is caught by the gate, not
-- by chance.
CREATE OR REPLACE FUNCTION bench.reset_stats()
RETURNS TEXT
LANGUAGE plpgsql
AS $$
DECLARE
    v_note TEXT := 'ok';
BEGIN
    BEGIN
        PERFORM pg_stat_statements_reset();
    EXCEPTION WHEN OTHERS THEN
        v_note := 'pg_stat_statements NOT reset (' || SQLERRM || ')';
        RAISE WARNING
            'pg_stat_statements is not loaded; per-statement attribution will be '
            'unavailable for this run. Add shared_preload_libraries = '
            '''pg_stat_statements'' and restart. See conf/postgresql.bench.conf.';
    END;

    PERFORM pg_stat_reset();                      -- per-table / per-function counters
    PERFORM pg_stat_reset_shared('bgwriter');     -- checkpoint counts (PG16)
    PERFORM pg_stat_reset_shared('io');           -- pg_stat_io, PG16+
    RETURN v_note;
END;
$$;

-- ===========================================================================
-- Execute one reset from psql variables.
--
-- Skipped when loading definitions into a fresh database:
--     psql -v defs_only=1 -f sql/90_reset.sql
-- because bench.assert_environment() is meant to refuse to run on a server
-- that is not configured for the experiment, and that refusal should not stop
-- the schema from loading.
-- ===========================================================================

\if :{?defs_only}
  \echo '  90_reset.sql: definitions only, reset skipped'
  \quit
\endif

\if :{?mode}
\else
  \set mode 'baseline'
\endif
\if :{?contest_id}
\else
  \set contest_id 500000
\endif
\if :{?match_group_id}
\else
  \set match_group_id 900001
\endif
\if :{?capacity}
\else
  \set capacity 2000000
\endif
\if :{?n_shards}
\else
  \set n_shards 32
\endif
-- -1 encodes SQL NULL ("no per-user limit"), because psql variables have no
-- NULL of their own.
\if :{?max_entries_per_user}
\else
  \set max_entries_per_user -1
\endif
\if :{?n_teams}
\else
  \set n_teams 1000000
\endif
\if :{?teams_per_user}
\else
  \set teams_per_user 1
\endif
\if :{?uniq_join_id}
\else
  \set uniq_join_id 'auto'
\endif

SELECT bench.assert_environment(256);

SELECT bench.reset_prepare(
    _mode                 => :'mode',
    _contest_id           => :contest_id,
    _match_group_id       => :match_group_id,
    _capacity             => :capacity,
    _n_shards             => :n_shards,
    _max_entries_per_user => NULLIF(:max_entries_per_user, -1),
    _n_teams              => :n_teams,
    _teams_per_user       => :teams_per_user,
    _uniq_join_id         => CASE :'uniq_join_id'
                                  WHEN 'on'  THEN TRUE
                                  WHEN 'off' THEN FALSE
                                  ELSE NULL
                             END
) AS contest_inst_id \gset reset_

\echo '  reset: contest_inst_id =' :reset_contest_inst_id

-- Phase 2. Top level, outside any transaction block.
--
-- ANALYZE and not just VACUUM: the planner's row estimates for
-- contest_inst_team change by six orders of magnitude between an empty table
-- and a filled one, and a stale estimate can flip the baseline's per-user
-- COUNT(*) between an index scan and a sequential scan mid-experiment. That
-- would look exactly like a saturation knee and would not be one.
VACUUM (ANALYZE)
    contest.contest,
    contest.contest_inst,
    contest.contest_inst_team,
    contest.contest_inst_shard,
    contest.contest_inst_user_entry,
    contest.contest_participant_team,
    contest.contest_match_group,
    contest.contest_match_group_match;

-- Phase 3.
SELECT bench.reset_stats();
