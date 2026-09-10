-- ===========================================================================
-- 99_smoke.sql
--
-- Phase 1 acceptance: prove BOTH paths work correctly for a single join, and
-- for the obvious single-threaded edge cases, before any Go is written.
--
-- This is NOT a concurrency test. Nothing here can catch a race -- races are
-- Experiment C's job and they need real goroutines. What this proves is that
-- the schema, the functions, the reset and the verification gates are wired up
-- and self-consistent. If this fails, nothing downstream is worth running.
--
-- Every assertion RAISEs on mismatch, so:
--   psql -v ON_ERROR_STOP=1 -d rowlock_bench -f sql/99_smoke.sql
-- exits non-zero on any failure.
-- ===========================================================================

\set ON_ERROR_STOP on
\timing off

\set contest_id     500000
\set match_group_id 900001
\set capacity       10
\set n_shards       4
\set n_teams        50

\echo ''
\echo '==========================================================='
\echo ' PHASE 1 SMOKE TEST'
\echo '==========================================================='

-- ===========================================================================
-- PART A -- BASELINE
-- ===========================================================================
\echo ''
\echo '--- A. BASELINE (advisory lock) ------------------------------'

-- Baseline defaults to uniq_join_id = off, matching the benchmark
-- specification. Part C re-runs the idempotency case with it on.
SELECT bench.reset_prepare(
    _mode                 => 'baseline',
    _contest_id           => :contest_id,
    _match_group_id       => :match_group_id,
    _capacity             => :capacity,
    _n_shards             => :n_shards,
    _max_entries_per_user => 3,
    _n_teams              => :n_teams,
    _teams_per_user       => 10        -- 5 users x 10 teams
) AS contest_inst_id \gset a_

VACUUM (ANALYZE) contest.contest, contest.contest_inst, contest.contest_inst_team,
                 contest.contest_inst_shard, contest.contest_inst_user_entry,
                 contest.contest_participant_team;

\echo '  fixture ready, contest_inst_id =' :a_contest_inst_id

-- A1 -- one successful join ------------------------------------------------
DO $$
DECLARE r RECORD; n BIGINT; c INT;
BEGIN
    SELECT * INTO r FROM contest.join_contest(500000, 1000000, 'smoke-b-001', 1, 90001);

    IF r.is_unique_request IS NOT TRUE THEN
        RAISE EXCEPTION 'A1: expected is_unique_request = true, got %', r.is_unique_request;
    END IF;
    IF r.contest_inst_id IS NULL THEN
        RAISE EXCEPTION 'A1: contest_inst_id is NULL';
    END IF;

    SELECT COUNT(*) INTO n FROM contest.contest_inst_team;
    IF n <> 1 THEN RAISE EXCEPTION 'A1: expected 1 join row, got %', n; END IF;

    SELECT joined_participants_count INTO c
      FROM contest.contest_inst WHERE id = r.contest_inst_id;
    IF c <> 1 THEN RAISE EXCEPTION 'A1: expected counter 1, got %', c; END IF;

    -- The baseline's second hot row.
    SELECT teams_joined_count INTO c FROM contest.contest WHERE id = 500000;
    IF c <> 1 THEN RAISE EXCEPTION 'A1: expected teams_joined_count 1, got %', c; END IF;

    RAISE NOTICE '  A1 PASS  single join -> 1 row, counter 1, teams_joined_count 1';
END $$;

-- A2 -- idempotent replay (single threaded, so the pre-check works) ---------
DO $$
DECLARE r RECORD; n BIGINT; c INT;
BEGIN
    SELECT * INTO r FROM contest.join_contest(500000, 1000000, 'smoke-b-001', 1, 90002);

    IF r.is_unique_request IS NOT FALSE THEN
        RAISE EXCEPTION 'A2: expected is_unique_request = false, got %', r.is_unique_request;
    END IF;

    SELECT COUNT(*) INTO n FROM contest.contest_inst_team;
    IF n <> 1 THEN RAISE EXCEPTION 'A2: replay wrote a row, count = %', n; END IF;

    SELECT joined_participants_count INTO c FROM contest.contest_inst WHERE id = r.contest_inst_id;
    IF c <> 1 THEN RAISE EXCEPTION 'A2: replay moved the counter to %', c; END IF;

    -- NOTE FOR THE READER: this passes only because the two calls are
    -- SEQUENTIAL. The EXISTS pre-check reads a snapshot taken before any lock,
    -- so it is correct exactly when there is no concurrency to be correct
    -- about. Experiment C1 runs the same two calls at the same time and gets a
    -- different answer.
    RAISE NOTICE '  A2 PASS  sequential replay -> is_unique_request=false, still 1 row';
END $$;

-- A3 -- duplicate team, different join id ----------------------------------
DO $$
DECLARE r RECORD; v_state TEXT;
BEGIN
    BEGIN
        SELECT * INTO r FROM contest.join_contest(500000, 1000000, 'smoke-b-002', 1, 90003);
        RAISE EXCEPTION 'A3: expected 01S12, call succeeded';
    EXCEPTION WHEN SQLSTATE '01S12' THEN
        RAISE NOTICE '  A3 PASS  duplicate team -> 01S12';
    END;
END $$;

-- A4 -- per-user cap (max_entries_per_user = 3, user 1 owns teams 0..9) -----
DO $$
DECLARE n BIGINT;
BEGIN
    PERFORM contest.join_contest(500000, 1000001, 'smoke-b-003', 1, 90004);
    PERFORM contest.join_contest(500000, 1000002, 'smoke-b-004', 1, 90005);

    BEGIN
        PERFORM contest.join_contest(500000, 1000003, 'smoke-b-005', 1, 90006);
        RAISE EXCEPTION 'A4: expected 01R22 on the 4th entry, call succeeded';
    EXCEPTION WHEN SQLSTATE '01R22' THEN
        NULL;
    END;

    SELECT COUNT(*) INTO n FROM contest.contest_inst_team;
    IF n <> 3 THEN RAISE EXCEPTION 'A4: expected 3 rows, got %', n; END IF;
    RAISE NOTICE '  A4 PASS  per-user cap -> 3 accepted, 4th 01R22, no phantom row';
END $$;

-- A5 -- fill to capacity, then refuse --------------------------------------
-- users 2..4 own teams 10..49; capacity is 10 and 3 seats are taken.
DO $$
DECLARE i INT; n BIGINT; v_status TEXT;
BEGIN
    -- 7 more joins: user 2 (teams 1000010..12), user 3 (1000020..22),
    -- user 4 (1000030) -- each user stays within the cap of 3.
    PERFORM contest.join_contest(500000, 1000010, 'smoke-b-010', 2, 90010);
    PERFORM contest.join_contest(500000, 1000011, 'smoke-b-011', 2, 90011);
    PERFORM contest.join_contest(500000, 1000012, 'smoke-b-012', 2, 90012);
    PERFORM contest.join_contest(500000, 1000020, 'smoke-b-020', 3, 90020);
    PERFORM contest.join_contest(500000, 1000021, 'smoke-b-021', 3, 90021);
    PERFORM contest.join_contest(500000, 1000022, 'smoke-b-022', 3, 90022);
    PERFORM contest.join_contest(500000, 1000030, 'smoke-b-030', 4, 90030);

    SELECT COUNT(*) INTO n FROM contest.contest_inst_team;
    IF n <> 10 THEN RAISE EXCEPTION 'A5: expected 10 rows at capacity, got %', n; END IF;

    SELECT status::TEXT INTO v_status FROM contest.contest_inst WHERE contest_id = 500000;
    IF v_status <> 'FULL' THEN
        RAISE EXCEPTION 'A5: expected instance status FULL, got % (migration 72 behaviour)', v_status;
    END IF;

    -- auto_generate = FALSE, so there is no OPEN instance left and
    -- join_contest_get_contest_inst raises 01S03 before join_contest's own
    -- 01R11 can fire.
    BEGIN
        PERFORM contest.join_contest(500000, 1000031, 'smoke-b-031', 4, 90031);
        RAISE EXCEPTION 'A5: expected 01S03 past capacity, call succeeded';
    EXCEPTION WHEN SQLSTATE '01S03' THEN
        NULL;
    END;

    SELECT COUNT(*) INTO n FROM contest.contest_inst_team;
    IF n <> 10 THEN RAISE EXCEPTION 'A5: oversold, % rows', n; END IF;
    RAISE NOTICE '  A5 PASS  filled to 10/10, status FULL, 11th -> 01S03, no oversell';
END $$;

\echo ''
\echo '  baseline correctness gates:'
SELECT check_name, severity, status, expected, actual
  FROM bench.verify_all(:contest_id, 'baseline');
SELECT bench.verify_gate(:contest_id, 'baseline') AS gate;


-- ===========================================================================
-- PART B -- SHARDED
-- ===========================================================================
\echo ''
\echo '--- B. SHARDED (row-level counter sharding) ------------------'

SELECT bench.reset_prepare(
    _mode                 => 'sharded',
    _contest_id           => :contest_id,
    _match_group_id       => :match_group_id,
    _capacity             => :capacity,
    _n_shards             => :n_shards,
    _max_entries_per_user => 3,
    _n_teams              => :n_teams,
    _teams_per_user       => 10
) AS contest_inst_id \gset b_

VACUUM (ANALYZE) contest.contest, contest.contest_inst, contest.contest_inst_team,
                 contest.contest_inst_shard, contest.contest_inst_user_entry,
                 contest.contest_participant_team;
SELECT bench.reset_stats();

\echo '  fixture ready, contest_inst_id =' :b_contest_inst_id

-- B0 -- provisioning: SUM(cap) must be EXACT --------------------------------
-- capacity 10 over 4 shards = 3,3,2,2. Integer division alone would give
-- 2,2,2,2 and silently strand two seats.
DO $$
DECLARE v_sum BIGINT; v_caps TEXT;
BEGIN
    SELECT SUM(cap), string_agg(cap::TEXT, ',' ORDER BY shard_no)
      INTO v_sum, v_caps
      FROM contest.contest_inst_shard;

    IF v_sum <> 10 THEN
        RAISE EXCEPTION 'B0: SUM(cap) = % expected 10 (caps: %)', v_sum, v_caps;
    END IF;
    IF v_caps <> '3,3,2,2' THEN
        RAISE EXCEPTION 'B0: remainder distribution wrong, caps = % expected 3,3,2,2', v_caps;
    END IF;
    RAISE NOTICE '  B0 PASS  4 shards, caps = %, SUM = 10 exactly', v_caps;
END $$;

-- B1 -- one successful join -------------------------------------------------
DO $$
DECLARE r RECORD; n BIGINT; u BIGINT; c INT;
BEGIN
    SELECT * INTO r FROM contest.join_contest_sharded(500000, 1000000, 'smoke-s-001', 1, 90001, 4);

    IF r.is_unique_request IS NOT TRUE THEN
        RAISE EXCEPTION 'B1: expected is_unique_request = true, got %', r.is_unique_request;
    END IF;
    IF r.shard_no IS NULL THEN RAISE EXCEPTION 'B1: no shard returned'; END IF;
    IF r.probe_attempts <> 1 THEN
        RAISE EXCEPTION 'B1: expected 1 probe on an empty contest, got %', r.probe_attempts;
    END IF;

    SELECT COUNT(*) INTO n FROM contest.contest_inst_team;
    IF n <> 1 THEN RAISE EXCEPTION 'B1: expected 1 join row, got %', n; END IF;

    SELECT SUM(used) INTO u FROM contest.contest_inst_shard;
    IF u <> 1 THEN RAISE EXCEPTION 'B1: expected SUM(used) = 1, got %', u; END IF;

    SELECT entries_used INTO c FROM contest.contest_inst_user_entry
     WHERE contest_inst_id = r.contest_inst_id AND user_id = 1;
    IF c <> 1 THEN RAISE EXCEPTION 'B1: expected entries_used 1, got %', c; END IF;

    -- The global counters must NOT have moved: the sharded path does not touch
    -- them, and that is the whole point.
    SELECT joined_participants_count INTO c FROM contest.contest_inst WHERE id = r.contest_inst_id;
    IF c <> 0 THEN
        RAISE EXCEPTION 'B1: sharded path touched contest_inst.joined_participants_count (= %)', c;
    END IF;
    SELECT teams_joined_count INTO c FROM contest.contest WHERE id = 500000;
    IF c <> 0 THEN
        RAISE EXCEPTION 'B1: sharded path touched contest.teams_joined_count (= %)', c;
    END IF;

    RAISE NOTICE '  B1 PASS  single join -> shard %, 1 probe, SUM(used)=1, global counters untouched', r.shard_no;
END $$;

-- B2 -- idempotent replay ---------------------------------------------------
DO $$
DECLARE r RECORD; n BIGINT; u BIGINT;
BEGIN
    SELECT * INTO r FROM contest.join_contest_sharded(500000, 1000000, 'smoke-s-001', 1, 90002, 4);

    IF r.is_unique_request IS NOT FALSE THEN
        RAISE EXCEPTION 'B2: expected is_unique_request = false, got %', r.is_unique_request;
    END IF;
    IF r.probe_attempts <> 0 THEN
        RAISE EXCEPTION 'B2: replay consumed % probes, expected 0', r.probe_attempts;
    END IF;

    SELECT COUNT(*) INTO n FROM contest.contest_inst_team;
    SELECT SUM(used) INTO u FROM contest.contest_inst_shard;
    IF n <> 1 OR u <> 1 THEN
        RAISE EXCEPTION 'B2: replay consumed capacity: rows=% used=%', n, u;
    END IF;
    RAISE NOTICE '  B2 PASS  replay -> is_unique_request=false, no seat consumed';
END $$;

-- B3 -- duplicate team ------------------------------------------------------
DO $$
DECLARE r RECORD; u BIGINT; e INT;
BEGIN
    BEGIN
        SELECT * INTO r FROM contest.join_contest_sharded(500000, 1000000, 'smoke-s-002', 1, 90003, 4);
        RAISE EXCEPTION 'B3: expected 01S12, call succeeded';
    EXCEPTION WHEN SQLSTATE '01S12' THEN
        NULL;
    END;

    -- No phantom writes: the failed call must not have consumed a seat or a
    -- user entry. This is the single-threaded proof that the all-or-nothing
    -- ordering in STEP 1/2/3 of join_contest_sharded actually rolls back.
    SELECT SUM(used) INTO u FROM contest.contest_inst_shard;
    SELECT entries_used INTO e FROM contest.contest_inst_user_entry WHERE user_id = 1;
    IF u <> 1 OR e <> 1 THEN
        RAISE EXCEPTION 'B3: phantom write after 01S12: used=% entries_used=%', u, e;
    END IF;
    RAISE NOTICE '  B3 PASS  duplicate team -> 01S12, no seat or entry consumed';
END $$;

-- B4 -- per-user cap --------------------------------------------------------
DO $$
DECLARE n BIGINT; u BIGINT; e INT;
BEGIN
    PERFORM contest.join_contest_sharded(500000, 1000001, 'smoke-s-003', 1, 90004, 4);
    PERFORM contest.join_contest_sharded(500000, 1000002, 'smoke-s-004', 1, 90005, 4);

    BEGIN
        PERFORM contest.join_contest_sharded(500000, 1000003, 'smoke-s-005', 1, 90006, 4);
        RAISE EXCEPTION 'B4: expected 01R22 on the 4th entry, call succeeded';
    EXCEPTION WHEN SQLSTATE '01R22' THEN
        NULL;
    END;

    SELECT COUNT(*) INTO n FROM contest.contest_inst_team;
    SELECT SUM(used) INTO u FROM contest.contest_inst_shard;
    SELECT entries_used INTO e FROM contest.contest_inst_user_entry WHERE user_id = 1;
    IF n <> 3 OR u <> 3 OR e <> 3 THEN
        RAISE EXCEPTION 'B4: rows=% used=% entries_used=% (all should be 3)', n, u, e;
    END IF;
    RAISE NOTICE '  B4 PASS  per-user cap -> 3 accepted, 4th 01R22, counters consistent';
END $$;

-- B5 -- fill to capacity, then refuse; probe ladder engages ------------------
DO $$
DECLARE i INT; n BIGINT; u BIGINT; r RECORD; v_max_probes INT := 0;
BEGIN
    PERFORM contest.join_contest_sharded(500000, 1000010, 'smoke-s-010', 2, 90010, 4);
    PERFORM contest.join_contest_sharded(500000, 1000011, 'smoke-s-011', 2, 90011, 4);
    PERFORM contest.join_contest_sharded(500000, 1000012, 'smoke-s-012', 2, 90012, 4);
    PERFORM contest.join_contest_sharded(500000, 1000020, 'smoke-s-020', 3, 90020, 4);
    PERFORM contest.join_contest_sharded(500000, 1000021, 'smoke-s-021', 3, 90021, 4);
    PERFORM contest.join_contest_sharded(500000, 1000022, 'smoke-s-022', 3, 90022, 4);

    -- The last seat. By now three of four shards are full, so this one is very
    -- likely to need more than one probe -- which is exactly the tail cost
    -- Experiment B is built to measure.
    SELECT * INTO r FROM contest.join_contest_sharded(500000, 1000030, 'smoke-s-030', 4, 90030, 4);
    RAISE NOTICE '  B5 .... last seat took % probe(s)', r.probe_attempts;

    SELECT COUNT(*), (SELECT SUM(used) FROM contest.contest_inst_shard)
      INTO n, u FROM contest.contest_inst_team;
    IF n <> 10 OR u <> 10 THEN
        RAISE EXCEPTION 'B5: expected 10/10, got rows=% used=%', n, u;
    END IF;

    -- Every shard must now be marked full. If any is still 'O' the state flip
    -- in the reservation UPDATE is not firing.
    IF EXISTS (SELECT 1 FROM contest.contest_inst_shard WHERE state = 'O') THEN
        RAISE EXCEPTION 'B5: shards still marked open at capacity: %',
            (SELECT string_agg(shard_no || '=' || used || '/' || cap, ' ')
               FROM contest.contest_inst_shard WHERE state = 'O');
    END IF;

    BEGIN
        PERFORM contest.join_contest_sharded(500000, 1000031, 'smoke-s-031', 4, 90031, 4);
        RAISE EXCEPTION 'B5: expected 01R11 past capacity, call succeeded';
    EXCEPTION WHEN SQLSTATE '01R11' THEN
        NULL;
    END;

    SELECT COUNT(*), (SELECT SUM(used) FROM contest.contest_inst_shard)
      INTO n, u FROM contest.contest_inst_team;
    IF n <> 10 OR u <> 10 THEN RAISE EXCEPTION 'B5: oversold: rows=% used=%', n, u; END IF;
    RAISE NOTICE '  B5 PASS  filled 10/10, all shards F, 11th -> 01R11, no oversell';
END $$;

\echo ''
\echo '  shard fill:'
SELECT shard_no, used, cap, state FROM contest.contest_inst_shard ORDER BY shard_no;

\echo ''
\echo '  sharded correctness gates:'
SELECT check_name, severity, status, expected, actual
  FROM bench.verify_all(:contest_id, 'sharded');
SELECT bench.verify_gate(:contest_id, 'sharded') AS gate;

\echo ''
\echo '  no index references contest_inst_shard.used (raises if one does):'
SELECT 'assert_no_index_on_used: ok' AS check
  FROM (SELECT bench.assert_no_index_on_used()) _;


-- ===========================================================================
-- PART D -- HOT chain formation, at a scale where it means something
-- ===========================================================================
-- The claim being tested is the one in the long comment at the end of
-- join_contest_sharded: writing `state` in the reservation UPDATE does NOT
-- break HOT, because PostgreSQL decides HOT eligibility by comparing the old
-- and new VALUES of the indexed attributes, not by looking at which columns
-- appear in the SET list. On every join except the one that fills a shard, the
-- CASE yields the state the row already has, so `state` is unmodified and the
-- update stays heap-only.
--
-- Part B could not test this. With capacity 10 across 4 shards, the four
-- 'O' -> 'F' transitions ARE 40% of all updates, so hot_pct lands around 60%
-- and says nothing about the design. Non-HOT writes scale with N (the shard
-- count), not with the join count: at 1M joins across 128 shards the same
-- effect is 0.013% of updates. The ratio is only meaningful once
-- joins >> shards, which is the regime the real experiments run in.
--
-- So: 8 shards, capacity far above the join count, 2,000 joins, expect ~100%.
\echo ''
\echo '--- D. HOT chain formation (2,000 joins, 8 shards) -----------'

SELECT bench.reset_prepare(
    _mode                 => 'sharded',
    _contest_id           => :contest_id,
    _match_group_id       => :match_group_id,
    _capacity             => 100000,   -- far above 2,000: no shard ever fills
    _n_shards             => 8,
    _max_entries_per_user => NULL,
    _n_teams              => 2000,
    _teams_per_user       => 1
) AS contest_inst_id \gset d_

VACUUM (ANALYZE) contest.contest, contest.contest_inst, contest.contest_inst_team,
                 contest.contest_inst_shard, contest.contest_inst_user_entry,
                 contest.contest_participant_team;
SELECT bench.reset_stats();

-- EACH JOIN MUST BE ITS OWN TRANSACTION. Running all 2,000 inside one DO block
-- would be far faster and would measure the wrong thing: a tuple superseded by
-- a later command of the SAME still-running transaction is
-- HEAPTUPLE_DELETE_IN_PROGRESS to HeapTupleSatisfiesVacuum, so opportunistic
-- HOT pruning cannot reclaim it. The page fills with unprunable versions, the
-- HOT chain breaks, and hot_pct collapses -- an artefact of the test harness,
-- not of the design. \gexec generates one statement per join, and psql's
-- autocommit makes each one its own transaction, which is also what the Go
-- load generator will do.
--
-- synchronous_commit = off for this section only. What is under test here is
-- whether HOT chains form, not what a commit costs; leaving it on would add
-- 2,000 fsyncs to a check that does not care about them. Every actual
-- experiment sweeps this setting deliberately (Methodology item 8).
SET synchronous_commit = off;

\o /dev/null
SELECT format(
    'SELECT contest.join_contest_sharded(%s, %s, %L, %s, %s, 8);',
    :contest_id, 1000000 + n, 'hot-' || n, 700000 + n, 800000 + n)
  FROM generate_series(0, 1999) AS g(n) \gexec
\o

RESET synchronous_commit;

-- Cumulative table statistics are flushed to shared memory at transaction end
-- but no more often than once per second per backend (PGSTAT_MIN_INTERVAL), so
-- a read immediately after the last join sees stale or zero counters. This is
-- not a race the harness can win by retrying -- it has to wait. The Go
-- observability code must do the same before reading HOT or bloat numbers.
SELECT pg_sleep(1.5);

\echo ''
\echo '  HOT health after 2,000 single-transaction joins:'
SELECT * FROM bench.hot_health();

DO $$
DECLARE v_res TEXT; n BIGINT; u BIGINT;
BEGIN
    SELECT COUNT(*), (SELECT SUM(used) FROM contest.contest_inst_shard)
      INTO n, u FROM contest.contest_inst_team;
    IF n <> 2000 OR u <> 2000 THEN
        RAISE EXCEPTION 'D: expected 2000/2000, got rows=% used=%', n, u;
    END IF;

    v_res := bench.assert_hot_health(95.0);
    RAISE NOTICE '  D1 PASS  2000 joins, SUM(used)=2000, %', v_res;
    RAISE NOTICE '           -> writing `state` in the reservation UPDATE does not';
    RAISE NOTICE '              break HOT, because the value is unchanged.';
END $$;

\echo ''
\echo '  gates after the HOT run:'
SELECT bench.verify_gate(:contest_id, 'sharded') AS gate;


-- ===========================================================================
-- PART C -- the unique_join_id index toggle
-- ===========================================================================
-- Both configurations must load and run. Experiment C1 is what makes them
-- differ; here we only prove the toggle works and that the sharded path
-- refuses to run without it.
\echo ''
\echo '--- C. unique_join_id index toggle ---------------------------'

DO $$
DECLARE v_has BOOLEAN;
BEGIN
    -- sharded reset must leave the index in place
    PERFORM bench.set_unique_join_id_index(TRUE);
    SELECT EXISTS (SELECT 1 FROM pg_indexes
                    WHERE schemaname = 'contest'
                      AND indexname = 'contest_inst_team_unique_join_id_uidx') INTO v_has;
    IF NOT v_has THEN RAISE EXCEPTION 'C: set_unique_join_id_index(TRUE) did not create the index'; END IF;

    PERFORM bench.set_unique_join_id_index(FALSE);
    SELECT EXISTS (SELECT 1 FROM pg_indexes
                    WHERE schemaname = 'contest'
                      AND indexname = 'contest_inst_team_unique_join_id_uidx') INTO v_has;
    IF v_has THEN RAISE EXCEPTION 'C: set_unique_join_id_index(FALSE) did not drop the index'; END IF;

    RAISE NOTICE '  C1 PASS  index toggle works in both directions';
END $$;

DO $$
BEGIN
    BEGIN
        PERFORM bench.reset_prepare(
            _mode => 'sharded', _contest_id => 500000, _match_group_id => 900001,
            _capacity => 10, _n_shards => 4, _uniq_join_id => FALSE);
        RAISE EXCEPTION 'C2: sharded reset accepted _uniq_join_id => FALSE';
    EXCEPTION WHEN SQLSTATE 'P0001' THEN
        IF SQLERRM LIKE '%design nobody proposed%' THEN
            RAISE NOTICE '  C2 PASS  sharded mode refuses to run without the I4 index';
        ELSE
            RAISE;
        END IF;
    END;
END $$;

\echo ''
\echo '==========================================================='
\echo ' PHASE 1 SMOKE TEST COMPLETE -- all assertions passed'
\echo '==========================================================='
\echo ''
