-- ===========================================================================
-- 91_verify.sql
--
-- The correctness gates, as SQL. Run after EVERY experiment.
--
-- A throughput number from a run that violated an invariant does not appear in
-- the report except labelled INVALID. These functions are what decides that.
--
-- Two severities:
--   FAIL    -- invariant violation. The run's numbers are void.
--   REPORT  -- a known defect being measured on purpose. Recorded in the
--             report, does not void the run. Exactly one check uses this, and
--             only on the baseline: duplicate unique_join_id. That duplicate
--             IS the finding; failing the run on it would delete the evidence.
--
-- Manual use:
--   psql -d rowlock_bench -c "SELECT * FROM bench.verify_all(500000,'sharded')"
--   psql -d rowlock_bench -c "SELECT bench.verify_gate(500000,'sharded')"
-- ===========================================================================

\set ON_ERROR_STOP on

CREATE OR REPLACE FUNCTION bench.verify_all(
    _contest_id BIGINT,
    _mode       TEXT
)
RETURNS TABLE (
    check_name TEXT,
    severity   TEXT,   -- FAIL | REPORT
    status     TEXT,   -- PASS | FAIL | SKIP
    expected   TEXT,
    actual     TEXT,
    detail     TEXT
)
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_inst_id        BIGINT;
    v_capacity       INT;
    v_max_per_user   INT;
    v_rows           BIGINT;
    v_shard_used     BIGINT;
    v_shard_cap      BIGINT;
    v_counter        BIGINT;
    v_dupes          BIGINT;
    v_dupe_detail    TEXT;
    v_outcomes       BIGINT;
    v_bad            BIGINT;
    v_entries        BIGINT;
BEGIN
    SELECT c.max_no_of_entries, c.max_entries_per_user
      INTO v_capacity, v_max_per_user
      FROM contest.contest c WHERE c.id = _contest_id;

    IF NOT FOUND THEN
        RETURN QUERY SELECT 'fixture_exists', 'FAIL', 'FAIL',
                            'contest ' || _contest_id, 'missing',
                            'No contest row. Run 90_reset.sql.';
        RETURN;
    END IF;

    SELECT ci.id INTO v_inst_id
      FROM contest.contest_inst ci WHERE ci.contest_id = _contest_id
     ORDER BY ci.id LIMIT 1;

    SELECT COUNT(*) INTO v_rows
      FROM contest.contest_inst_team cit WHERE cit.contest_inst_id = v_inst_id;

    SELECT COALESCE(SUM(s.used), 0), COALESCE(SUM(s.cap), 0)
      INTO v_shard_used, v_shard_cap
      FROM contest.contest_inst_shard s WHERE s.contest_inst_id = v_inst_id;

    -- =====================================================================
    -- I1 -- never oversell
    -- =====================================================================

    -- The headline sharded invariant. SUM(used) is the ONLY participant count
    -- the sharded design keeps, so if it drifts from the number of actual join
    -- rows then the design has either lost seats or sold seats it did not
    -- record, and every throughput number from the run is meaningless.
    IF _mode = 'sharded' THEN
        RETURN QUERY SELECT
            'shard_used_equals_join_rows', 'FAIL',
            CASE WHEN v_shard_used = v_rows THEN 'PASS' ELSE 'FAIL' END,
            v_rows::TEXT, v_shard_used::TEXT,
            'SUM(contest_inst_shard.used) must equal COUNT(*) contest_inst_team';
    ELSE
        RETURN QUERY SELECT
            'shard_used_equals_join_rows', 'FAIL', 'SKIP',
            v_rows::TEXT, v_shard_used::TEXT,
            'baseline does not use shards';
    END IF;

    IF _mode = 'sharded' THEN
        RETURN QUERY SELECT
            'shard_used_within_capacity', 'FAIL',
            CASE WHEN v_shard_used <= v_capacity THEN 'PASS' ELSE 'FAIL' END,
            '<= ' || v_capacity::TEXT, v_shard_used::TEXT,
            'SUM(used) must never exceed max_no_of_entries';
    ELSE
        RETURN QUERY SELECT
            'shard_used_within_capacity', 'FAIL', 'SKIP',
            '<= ' || v_capacity::TEXT, v_shard_used::TEXT, 'baseline';
    END IF;

    -- Checked on BOTH paths. On the baseline the shards are provisioned but
    -- unused, so this is really a check that provisioning itself is sound --
    -- integer division silently strands capacity, and this is what catches it.
    RETURN QUERY SELECT
        'shard_cap_equals_capacity', 'FAIL',
        CASE WHEN v_shard_cap = v_capacity THEN 'PASS' ELSE 'FAIL' END,
        v_capacity::TEXT, v_shard_cap::TEXT,
        'SUM(contest_inst_shard.cap) must equal max_no_of_entries exactly';

    -- The invariant as a user experiences it, independent of which mechanism
    -- was supposed to enforce it.
    RETURN QUERY SELECT
        'no_oversell', 'FAIL',
        CASE WHEN v_rows <= v_capacity THEN 'PASS' ELSE 'FAIL' END,
        '<= ' || v_capacity::TEXT, v_rows::TEXT,
        'COUNT(*) contest_inst_team must not exceed max_no_of_entries';

    -- Baseline-only: its global counter must agree with reality. A drift here
    -- means the counter and the rows were updated in different transactions or
    -- one of them was lost.
    IF _mode = 'baseline' THEN
        SELECT ci.joined_participants_count INTO v_counter
          FROM contest.contest_inst ci WHERE ci.id = v_inst_id;
        RETURN QUERY SELECT
            'baseline_counter_equals_join_rows', 'FAIL',
            CASE WHEN v_counter = v_rows THEN 'PASS' ELSE 'FAIL' END,
            v_rows::TEXT, v_counter::TEXT,
            'contest_inst.joined_participants_count must equal COUNT(*) contest_inst_team';
    END IF;

    -- =====================================================================
    -- I4 -- a unique_join_id produces at most one row
    -- =====================================================================
    -- SEVERITY IS DELIBERATELY DIFFERENT PER PATH.
    --
    -- sharded : FAIL. I4 is enforced by a unique index; a duplicate means the
    --           index is missing or the design is broken.
    -- baseline: REPORT. The baseline's idempotency pre-check runs before any
    --           lock, so concurrent retries of one id both pass it. When the
    --           production unique index is absent (the configuration this
    --           benchmark specifies) that produces genuine duplicate rows --
    --           a double charge. That is the deliverable of Experiment C1.
    --           Failing the run on it would throw away the evidence.
    SELECT COUNT(*), string_agg(d.unique_join_id || ' x' || d.n, ', ')
      INTO v_dupes, v_dupe_detail
      FROM (
        SELECT cit.unique_join_id, COUNT(*) AS n
          FROM contest.contest_inst_team cit
         WHERE cit.contest_inst_id = v_inst_id
         GROUP BY cit.unique_join_id
        HAVING COUNT(*) > 1
         LIMIT 20
      ) d;

    RETURN QUERY SELECT
        'no_duplicate_unique_join_id',
        CASE WHEN _mode = 'baseline' THEN 'REPORT' ELSE 'FAIL' END,
        CASE WHEN v_dupes = 0 THEN 'PASS' ELSE 'FAIL' END,
        '0', v_dupes::TEXT,
        COALESCE('duplicated ids (first 20): ' || v_dupe_detail,
                 'distinct unique_join_id values with more than one row');

    -- =====================================================================
    -- I2 -- a team joins an instance at most once
    -- =====================================================================
    -- FAIL on both paths. Production has UNIQUE (contest_inst_id, team_id) and
    -- so does this schema, so a violation here would mean the constraint was
    -- dropped rather than that the join logic is weak.
    SELECT COUNT(*) INTO v_dupes
      FROM (
        SELECT 1 FROM contest.contest_inst_team cit
         WHERE cit.contest_inst_id = v_inst_id
         GROUP BY cit.contest_inst_id, cit.team_id
        HAVING COUNT(*) > 1
      ) d;

    RETURN QUERY SELECT
        'no_duplicate_team', 'FAIL',
        CASE WHEN v_dupes = 0 THEN 'PASS' ELSE 'FAIL' END,
        '0', v_dupes::TEXT,
        '(contest_inst_id, team_id) pairs appearing more than once';

    -- =====================================================================
    -- I3 -- no user exceeds max_entries_per_user
    -- =====================================================================
    -- Counted from the join rows themselves, NOT from
    -- contest_inst_user_entry. Verifying the sharded design's counter against
    -- its own counter would prove nothing; the counter is what is on trial.
    IF v_max_per_user IS NULL THEN
        RETURN QUERY SELECT
            'no_user_over_entry_cap', 'FAIL', 'SKIP',
            'unlimited', '-', 'contest.max_entries_per_user IS NULL';
    ELSE
        SELECT COUNT(*), MAX(d.n)
          INTO v_dupes, v_bad
          FROM (
            SELECT cpt.user_id, COUNT(*) AS n
              FROM contest.contest_inst_team cit
              JOIN contest.contest_participant_team cpt ON cpt.id = cit.team_id
             WHERE cit.contest_inst_id = v_inst_id
             GROUP BY cpt.user_id
            HAVING COUNT(*) > v_max_per_user
          ) d;

        RETURN QUERY SELECT
            'no_user_over_entry_cap', 'FAIL',
            CASE WHEN v_dupes = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0 users over ' || v_max_per_user::TEXT,
            v_dupes::TEXT || ' users, worst ' || COALESCE(v_bad::TEXT, '-'),
            'users holding more than max_entries_per_user rows in this instance';
    END IF;

    -- Sharded-only cross-check: the per-user counter must agree with the join
    -- rows. This is the I3 analogue of shard_used_equals_join_rows.
    IF _mode = 'sharded' THEN
        SELECT COALESCE(SUM(ue.entries_used), 0) INTO v_entries
          FROM contest.contest_inst_user_entry ue
         WHERE ue.contest_inst_id = v_inst_id;

        RETURN QUERY SELECT
            'user_entry_sum_equals_join_rows', 'FAIL',
            CASE WHEN v_entries = v_rows THEN 'PASS' ELSE 'FAIL' END,
            v_rows::TEXT, v_entries::TEXT,
            'SUM(contest_inst_user_entry.entries_used) must equal COUNT(*) contest_inst_team';
    END IF;

    -- =====================================================================
    -- Client/database agreement -- needs bench.run_outcome from the harness
    -- =====================================================================
    SELECT COUNT(*) INTO v_outcomes FROM bench.run_outcome;

    IF v_outcomes = 0 THEN
        RETURN QUERY SELECT 'success_has_row', 'FAIL', 'SKIP', '0', '-',
                            'bench.run_outcome is empty; harness did not upload outcomes';
        RETURN QUERY SELECT 'failure_has_no_row', 'FAIL', 'SKIP', '0', '-',
                            'bench.run_outcome is empty; harness did not upload outcomes';
    ELSE
        -- Every request the CLIENT was told succeeded must have a row. A
        -- violation means the harness counted a success the database did not
        -- keep -- e.g. a commit the client believed in that was rolled back.
        SELECT COUNT(*) INTO v_bad
          FROM bench.run_outcome o
         WHERE o.outcome IN ('success', 'idempotent')
           AND NOT EXISTS (SELECT 1 FROM contest.contest_inst_team cit
                            WHERE cit.unique_join_id = o.unique_join_id);

        RETURN QUERY SELECT
            'success_has_row', 'FAIL',
            CASE WHEN v_bad = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', v_bad::TEXT,
            'reported successes with no corresponding contest_inst_team row';

        -- PHANTOM WRITES. Every request the client was told FAILED must have
        -- left nothing behind. This is the check that catches a partially
        -- applied join -- a seat consumed, or a row written, under an error the
        -- caller will treat as "nothing happened" and retry.
        SELECT COUNT(*) INTO v_bad
          FROM bench.run_outcome o
         WHERE o.outcome = 'failure'
           AND EXISTS (SELECT 1 FROM contest.contest_inst_team cit
                        WHERE cit.unique_join_id = o.unique_join_id);

        RETURN QUERY SELECT
            'failure_has_no_row', 'FAIL',
            CASE WHEN v_bad = 0 THEN 'PASS' ELSE 'FAIL' END,
            '0', v_bad::TEXT,
            'reported failures that nevertheless left a contest_inst_team row';
    END IF;
END;
$$;

-- ---------------------------------------------------------------------------
-- bench.verify_gate
-- ---------------------------------------------------------------------------
-- The gate itself: raises if any FAIL-severity check did not pass, so a caller
-- (Makefile, Go harness, CI) can treat a violated run as an error rather than
-- having to interpret a result set. REPORT-severity failures are returned in
-- the message but do not raise.
CREATE OR REPLACE FUNCTION bench.verify_gate(
    _contest_id BIGINT,
    _mode       TEXT
)
RETURNS TEXT
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_failed   TEXT;
    v_reported TEXT;
BEGIN
    SELECT string_agg(v.check_name || ' (expected ' || v.expected ||
                      ', got ' || v.actual || ')', E'\n  ')
      INTO v_failed
      FROM bench.verify_all(_contest_id, _mode) v
     WHERE v.severity = 'FAIL' AND v.status = 'FAIL';

    SELECT string_agg(v.check_name || ' (' || v.actual || ')', ', ')
      INTO v_reported
      FROM bench.verify_all(_contest_id, _mode) v
     WHERE v.severity = 'REPORT' AND v.status = 'FAIL';

    IF v_failed IS NOT NULL THEN
        RAISE EXCEPTION
            E'CORRECTNESS GATE FAILED (%). This run''s numbers are INVALID:\n  %',
            _mode, v_failed;
    END IF;

    RETURN 'gate passed' ||
           COALESCE('; reported defects: ' || v_reported, '');
END;
$$;
