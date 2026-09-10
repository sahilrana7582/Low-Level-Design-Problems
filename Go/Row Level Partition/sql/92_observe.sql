-- ===========================================================================
-- 92_observe.sql
--
-- Observability. The headline finding of this whole exercise lives here.
--
-- The number that matters is not "the sharded path is N times faster". It is
-- WHERE THE TIME WENT, before and after:
--
--   BASELINE   Lock / transactionid          software serialisation.
--                                            An architectural problem. No
--                                            amount of money fixes it: faster
--                                            disks, more replicas and bigger
--                                            instances all leave the ceiling
--                                            at 1 / (critical section + fsync).
--
--   SHARDED    IO / WALSync                  hardware.
--              LWLock / WALWrite             A buyable problem. Faster storage,
--                                            group commit, a closer replica,
--                                            or synchronous_commit tuning all
--                                            move it.
--
-- A throughput multiplier without this shift is an anecdote. With it, it is an
-- explanation.
--
-- Run as:  psql -v ON_ERROR_STOP=1 -d rowlock_bench -f sql/92_observe.sql
-- ===========================================================================

\set ON_ERROR_STOP on

-- ---------------------------------------------------------------------------
-- bench.wait_events  -- sampled every 100ms during a run
-- ---------------------------------------------------------------------------
-- The specification's query, with two exclusions, both of which make the
-- measurement more honest rather than less:
--
--   pid <> pg_backend_pid()
--     The sampler is itself an active backend. Including it adds one
--     ('', NULL) "running on CPU" row to every single sample -- a constant
--     offset that is pure instrument error, and one that matters most when the
--     real answer is small.
--
--   datname = current_database()
--     This PostgreSQL instance also hosts contest_db, wallet_db and a dozen
--     others. Their backends have nothing to do with this experiment.
--
-- NULL wait_event means the backend is on CPU, not idle -- state='active'
-- already excluded idle ones. It is rendered as 'CPU' so the stacked histogram
-- in the report reads as a partition of the active backends rather than
-- having a mystery blank band.
CREATE OR REPLACE FUNCTION bench.wait_events()
RETURNS TABLE (wait_event_type TEXT, wait_event TEXT, backends BIGINT)
LANGUAGE sql
VOLATILE
AS $$
    SELECT COALESCE(a.wait_event_type, 'CPU')::TEXT,
           COALESCE(a.wait_event, 'CPU')::TEXT,
           COUNT(*)::BIGINT
      FROM pg_stat_activity a
     WHERE a.state = 'active'
       AND a.pid <> pg_backend_pid()
       AND a.datname = current_database()
     GROUP BY 1, 2;
$$;

-- ---------------------------------------------------------------------------
-- bench.hot_health
-- ---------------------------------------------------------------------------
-- HOT (Heap-Only Tuple) updates write the new tuple version on the same page
-- as the old one and chain them, so NO index entry is written. That is the
-- entire reason the shard table can absorb a million updates against 128 rows
-- without its index turning into landfill.
--
-- HOT requires two things and both are easy to break by accident:
--   1. No indexed column changed. "Indexed" includes PARTIAL INDEX PREDICATE
--      columns -- see the long note in 02_sharded_schema.sql.
--   2. Free space on the same page, which is what fillfactor = 70 reserves.
CREATE OR REPLACE FUNCTION bench.hot_health()
RETURNS TABLE (
    relname       TEXT,
    n_tup_upd     BIGINT,
    n_tup_hot_upd BIGINT,
    hot_pct       NUMERIC
)
LANGUAGE sql
STABLE
AS $$
    SELECT t.relname::TEXT,
           t.n_tup_upd,
           t.n_tup_hot_upd,
           round(100.0 * t.n_tup_hot_upd / NULLIF(t.n_tup_upd, 0), 1)
      FROM pg_stat_user_tables t
     WHERE t.relname LIKE 'contest_inst_%'
     ORDER BY t.relname;
$$;

-- ---------------------------------------------------------------------------
-- bench.assert_hot_health  -- FAILS THE RUN
-- ---------------------------------------------------------------------------
-- Called after every sharded run. If HOT has broken on contest_inst_shard,
-- the run measured index write amplification and page splits, not lock
-- contention, and its throughput number is not a measurement of the design.
--
-- The diagnostic names the most likely cause, because when this fires it is
-- almost always someone having added a helpful-looking index.
CREATE OR REPLACE FUNCTION bench.assert_hot_health(_min_pct NUMERIC DEFAULT 95.0)
RETURNS TEXT
LANGUAGE plpgsql
AS $$
DECLARE
    v_upd BIGINT;
    v_hot BIGINT;
    v_pct NUMERIC;
    v_idx TEXT;
BEGIN
    SELECT t.n_tup_upd, t.n_tup_hot_upd
      INTO v_upd, v_hot
      FROM pg_stat_user_tables t
     WHERE t.schemaname = 'contest' AND t.relname = 'contest_inst_shard';

    IF v_upd IS NULL OR v_upd = 0 THEN
        -- No updates at all: a baseline run, or a run that did nothing. Not a
        -- HOT failure, and asserting on a zero denominator would just produce
        -- a confusing error.
        RETURN 'skipped: contest_inst_shard has no updates in this stats window';
    END IF;

    v_pct := round(100.0 * v_hot / v_upd, 1);

    IF v_pct < _min_pct THEN
        SELECT string_agg(c.relname || COALESCE(' WHERE ' ||
                          pg_get_expr(i.indpred, i.indrelid), ''), ', ')
          INTO v_idx
          FROM pg_index i
          JOIN pg_class c ON c.oid = i.indexrelid
         WHERE i.indrelid = 'contest.contest_inst_shard'::regclass;

        RAISE EXCEPTION
            E'HOT HEALTH GATE FAILED: contest_inst_shard hot_pct = % (need >= %).\n'
            'n_tup_upd = %, n_tup_hot_upd = %.\n'
            'This run measured index write amplification, not lock contention. Its\n'
            'throughput number is INVALID.\n'
            '\n'
            'DIAGNOSTIC -- check, in this order:\n'
            '  1. Does ANY index reference the `used` column, including as a partial\n'
            '     index PREDICATE? PostgreSQL puts predicate columns in the\n'
            '     HOT-blocking attribute set, so `WHERE used < cap` on an index is\n'
            '     just as fatal as indexing `used` directly.\n'
            '     Indexes currently on the table: %\n'
            '     bench.assert_no_index_on_used() checks this; call it.\n'
            '  2. Is fillfactor still 70? Without free space on the page there is\n'
            '     nowhere to put the new tuple version and HOT cannot happen.\n'
            '       SELECT reloptions FROM pg_class\n'
            '        WHERE oid = ''contest.contest_inst_shard''::regclass;\n'
            '  3. Is autovacuum keeping up? Free space is reclaimed by vacuum; if\n'
            '     the worker is starved the page fills and HOT degrades mid-run.\n'
            '       SELECT last_autovacuum, autovacuum_count FROM pg_stat_user_tables\n'
            '        WHERE relname = ''contest_inst_shard'';',
            v_pct, _min_pct, v_upd, v_hot, COALESCE(v_idx, '(none)');
    END IF;

    RETURN format('hot_pct = %s on %s updates', v_pct, v_upd);
END;
$$;

-- ---------------------------------------------------------------------------
-- bench.bloat  -- capture before AND after every run
-- ---------------------------------------------------------------------------
-- pgstattuple does a real scan and reports real dead-tuple bytes. Estimating
-- bloat from pg_class.reltuples and column widths is a well-known guess that
-- can be off by a factor of two, and "the sharded table bloated 2x less" is a
-- claim that has to survive scrutiny.
--
-- contest_inst_team is excluded from the exact scan and uses
-- pgstattuple_approx instead: at a million rows the exact version takes long
-- enough that running it between every run of a 2,500-run matrix would cost
-- more wall clock than the experiments. The small, hot, interesting tables get
-- the exact numbers.
CREATE OR REPLACE FUNCTION bench.bloat()
RETURNS TABLE (
    relname          TEXT,
    method           TEXT,
    table_bytes      BIGINT,
    index_bytes      BIGINT,
    live_tuples      BIGINT,
    dead_tuples      BIGINT,
    dead_pct         NUMERIC,
    free_pct         NUMERIC
)
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    r   RECORD;
    st  RECORD;
BEGIN
    FOR r IN
        SELECT c.oid, c.relname::TEXT AS rn
          FROM pg_class c
          JOIN pg_namespace n ON n.oid = c.relnamespace
         WHERE n.nspname = 'contest'
           AND c.relkind = 'r'
           AND c.relname IN ('contest', 'contest_inst', 'contest_inst_team',
                             'contest_inst_shard', 'contest_inst_user_entry')
         ORDER BY c.relname
    LOOP
        IF r.rn = 'contest_inst_team' THEN
            SELECT a.table_len, a.approx_tuple_count, a.dead_tuple_count,
                   a.dead_tuple_percent, a.approx_free_percent
              INTO st
              FROM pgstattuple_approx(r.oid) a;
            RETURN QUERY SELECT r.rn, 'approx',
                   st.table_len::BIGINT,
                   pg_indexes_size(r.oid),
                   st.approx_tuple_count::BIGINT,
                   st.dead_tuple_count::BIGINT,
                   round(st.dead_tuple_percent::NUMERIC, 2),
                   round(st.approx_free_percent::NUMERIC, 2);
        ELSE
            SELECT s.table_len, s.tuple_count, s.dead_tuple_count,
                   s.dead_tuple_percent, s.free_percent
              INTO st
              FROM pgstattuple(r.oid) s;
            RETURN QUERY SELECT r.rn, 'exact',
                   st.table_len::BIGINT,
                   pg_indexes_size(r.oid),
                   st.tuple_count::BIGINT,
                   st.dead_tuple_count::BIGINT,
                   round(st.dead_tuple_percent::NUMERIC, 2),
                   round(st.free_percent::NUMERIC, 2);
        END IF;
    END LOOP;
END;
$$;

-- ---------------------------------------------------------------------------
-- bench.pgss_top  -- top statements by total execution time
-- ---------------------------------------------------------------------------
-- pg_stat_statements.track = 'all' (conf/postgresql.bench.conf) is what makes
-- this useful. At the default 'top' every row would read
-- "SELECT contest.join_contest($1,$2,$3,$4,$5)" and attribute 100% of the time
-- to it -- true, and completely uninformative. With 'all' the statements
-- INSIDE the plpgsql body are tracked separately, so the COUNT(*), the two
-- UPDATEs and the INSERT each get their own line and the critical section can
-- be decomposed.
CREATE OR REPLACE FUNCTION bench.pgss_top(_limit INT DEFAULT 10)
RETURNS TABLE (
    calls            BIGINT,
    total_exec_ms    NUMERIC,
    mean_exec_ms     NUMERIC,
    -- Percentiles are unavailable here: pg_stat_statements keeps only sums,
    -- min, max, mean and stddev. The percentile numbers in the report come
    -- from the harness HDR histogram. These means are for ATTRIBUTION (which
    -- statement owns the time), never for latency reporting.
    stddev_exec_ms   NUMERIC,
    max_exec_ms      NUMERIC,
    rows_returned    BIGINT,
    wal_bytes        NUMERIC,
    query            TEXT
)
LANGUAGE sql
STABLE
AS $$
    SELECT s.calls,
           round(s.total_exec_time::NUMERIC, 2),
           round(s.mean_exec_time::NUMERIC, 4),
           round(s.stddev_exec_time::NUMERIC, 4),
           round(s.max_exec_time::NUMERIC, 2),
           s.rows,
           s.wal_bytes,
           left(regexp_replace(s.query, '\s+', ' ', 'g'), 200)
      FROM pg_stat_statements s
      JOIN pg_database d ON d.oid = s.dbid
     WHERE d.datname = current_database()
     ORDER BY s.total_exec_time DESC
     LIMIT _limit;
$$;

-- ---------------------------------------------------------------------------
-- bench.checkpoint_stats
-- ---------------------------------------------------------------------------
-- A checkpoint inside the measurement window shows up as a latency spike that
-- has nothing to do with row locks. Counters are reset at the start of every
-- run (bench.reset_stats), so a non-zero count here means a checkpoint DID
-- land in the window and the run's p99.9 must be read with that in mind --
-- or the run rerun.
--
-- PG16: the counters live in pg_stat_bgwriter. PG17 split them into
-- pg_stat_checkpointer; if this harness is ever moved forward, this is the
-- query that breaks.
CREATE OR REPLACE FUNCTION bench.checkpoint_stats()
RETURNS TABLE (
    checkpoints_timed     BIGINT,
    checkpoints_req       BIGINT,
    checkpoint_write_ms   DOUBLE PRECISION,
    checkpoint_sync_ms    DOUBLE PRECISION,
    buffers_checkpoint    BIGINT,
    buffers_clean         BIGINT,
    buffers_backend       BIGINT,
    buffers_backend_fsync BIGINT,
    stats_reset           TIMESTAMPTZ
)
LANGUAGE sql
STABLE
AS $$
    SELECT b.checkpoints_timed, b.checkpoints_req,
           b.checkpoint_write_time, b.checkpoint_sync_time,
           b.buffers_checkpoint, b.buffers_clean,
           b.buffers_backend, b.buffers_backend_fsync,
           b.stats_reset
      FROM pg_stat_bgwriter b;
$$;

-- ---------------------------------------------------------------------------
-- bench.environment  -- goes into every results file
-- ---------------------------------------------------------------------------
-- The database half of the environment capture. The Go harness adds CPU model,
-- core count, GOMAXPROCS and whether the generator shares cores with
-- PostgreSQL (on a laptop it always does -- see the CPU-contention caveat,
-- which belongs at the top of the report, not in a footnote).
--
-- source <> 'default' is what makes this a capture rather than a wish: it
-- reports what the server is ACTUALLY running with, including anything set by
-- ALTER SYSTEM or on the command line, rather than what the conf file was
-- believed to say.
CREATE OR REPLACE FUNCTION bench.environment()
RETURNS TABLE (name TEXT, setting TEXT, unit TEXT, source TEXT)
LANGUAGE sql
STABLE
AS $$
    SELECT 'server_version'::TEXT, version()::TEXT, NULL::TEXT, 'version()'::TEXT
    UNION ALL
    SELECT s.name::TEXT, s.setting::TEXT, s.unit::TEXT, s.source::TEXT
      FROM pg_settings s
     WHERE s.source <> 'default'
        -- Always report these even when left at the default: they are the ones
        -- a reader will want to check first, and "absent because default" is
        -- indistinguishable from "absent because nobody looked".
        OR s.name IN ('synchronous_commit', 'fsync', 'wal_level', 'max_connections',
                      'shared_buffers', 'max_wal_size', 'checkpoint_timeout',
                      'commit_delay', 'commit_siblings', 'wal_writer_delay',
                      'autovacuum_naptime', 'track_io_timing', 'track_wal_io_timing')
     ORDER BY 1;
$$;

-- ---------------------------------------------------------------------------
-- Ad-hoc views for eyeballing during a manual run
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW bench.v_wait_events AS SELECT * FROM bench.wait_events();
CREATE OR REPLACE VIEW bench.v_hot_health  AS SELECT * FROM bench.hot_health();
CREATE OR REPLACE VIEW bench.v_shard_fill  AS
    SELECT s.contest_inst_id, s.shard_no, s.used, s.cap, s.state,
           round(100.0 * s.used / NULLIF(s.cap, 0), 1) AS pct_full
      FROM contest.contest_inst_shard s
     ORDER BY s.contest_inst_id, s.shard_no;
