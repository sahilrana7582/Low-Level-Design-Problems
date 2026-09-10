-- ===========================================================================
-- 01_baseline_fn.sql
--
-- The BASELINE join path. Faithful port of production:
--   migrations/50_func_join_contest_get_contest_inst.up.sql
--   migrations/72_fix_join_contest_full_status.up.sql   (latest join_contest)
--
-- This is the system under test, not a strawman. Everything below is what
-- runs in production today, warts included. Read the WARNING comments: each
-- one marks something that a reviewer would flag in code review and that is
-- nevertheless deployed.
--
-- WHY THE ADVISORY LOCK CAPS THROUGHPUT
-- -------------------------------------
-- pg_advisory_xact_lock() is transaction-scoped: it is released by the commit
-- record, not by the last statement. So the lock is still held while the
-- backend is inside CommitTransaction() waiting for the WAL fsync. The
-- serialised critical section is therefore
--
--     lock acquire -> reads -> COUNT(*) -> insert -> 2 updates -> COMMIT fsync
--
-- and the ceiling is 1 / (critical_section + fsync) joins per second FOR THE
-- WHOLE CONTEST. Adding connections, replicas or CPU cannot move it; they only
-- lengthen the queue in front of it. That is the claim this benchmark tests.
--
-- Run as:  psql -v ON_ERROR_STOP=1 -d rowlock_bench -f sql/01_baseline_fn.sql
-- ===========================================================================

\set ON_ERROR_STOP on

-- ---------------------------------------------------------------------------
-- contest.join_contest_get_contest_inst  (production migration 50, verbatim)
-- ---------------------------------------------------------------------------
-- Custom SQLSTATEs raised:
--   01S01  invalid published_status
--   01S02  invalid contest status
--   01S03  contest is full
--   01S04  wrong contest type (H2H)
--   01S07  join close time passed
--
-- WARNING (production bug, preserved): this function is declared PARALLEL SAFE
-- while it performs INSERT and UPDATE. A parallel-unsafe body labelled safe is
-- a correctness hazard -- if the planner ever did place it in a parallel
-- worker, execution would fail with "cannot execute INSERT during a parallel
-- operation". It is latent today only because its caller (join_contest) is
-- plpgsql and therefore defaults to PARALLEL UNSAFE, which disables
-- parallelism for the whole statement. Preserved verbatim; do not "fix" it.
--
-- WARNING (the second lock): pg_advisory_xact_lock(_contest_id << 3) is a
-- SEPARATE advisory lock from the one join_contest takes on _contest_id. Both
-- are transaction-scoped, so a join holds TWO cluster-wide locks across its
-- own commit fsync. The << 3 exists only to keep the two key spaces from
-- colliding.
CREATE OR REPLACE FUNCTION contest.join_contest_get_contest_inst (
    _contest_id BIGINT
)
  RETURNS BIGINT
  PARALLEL SAFE   -- see WARNING above: this label is wrong in production
  COST 200
AS
$func$
DECLARE
    -- contest vars
    contest_published_status VARCHAR(20);
    contest_status VARCHAR(20);
    v_max_teams_allowed INT;
    auto_generate_flg BOOL;
    v_join_close_time TIMESTAMPTZ;

    -- contest inst vars
    contest_inst_id BIGINT;
    current_participants_count INT;
    return_contest_inst_id BIGINT;
    v_contest_type INT8;

BEGIN
    -- Create lock for these SQL commands. This is using a bitwise shift so that
    -- the lock ID is different than the lock in contest.join_contest.
    PERFORM pg_advisory_xact_lock(_contest_id << 3);

    -- Retrieve Contest data and validate contest status
    SELECT published_status, status, max_no_of_entries, auto_generate, contest_type,
           COALESCE(delayed_join_close_time, join_close_time) AS join_close_time
        FROM contest.contest
        WHERE id = _contest_id
    INTO contest_published_status, contest_status, v_max_teams_allowed,
         auto_generate_flg, v_contest_type, v_join_close_time;

    -- Validations
    IF v_contest_type = 1 THEN
        RAISE 'contest % has a contest_type of %. For H2H contests, use contest.join_contest_H2H.',
            _contest_id, v_contest_type USING ERRCODE = '01S04';
    END IF;

    IF contest_published_status != 'ACTIVE' THEN
        RAISE 'Contest published state must be "ACTIVE", currently: %',
            contest_published_status USING ERRCODE = '01S01';
    END IF;

    IF contest_status != 'CREATED' THEN
        RAISE 'Contest state must be "CREATED", currently: %',
            contest_status USING ERRCODE = '01S02';
    END IF;

    IF v_join_close_time < NOW() THEN
        RAISE 'Contest join close time has passed.' USING ERRCODE = '01S07';
    END IF;

    -- Contest status valid -- ensure at least one contest instance exists for
    -- contest. otherwise, create it.
    PERFORM FROM contest.contest_inst WHERE contest_id = _contest_id FOR KEY SHARE SKIP LOCKED;

    -- Scenario 1: Contest Inst does not exist.
    IF NOT FOUND THEN
        SELECT public.id_generator() INTO return_contest_inst_id;

        INSERT INTO contest.contest_inst (id, contest_id, status)
        VALUES (return_contest_inst_id, _contest_id, 'OPEN');

        RETURN return_contest_inst_id;
    END IF;

    -- Retrieve Contest Inst data
    SELECT id, joined_participants_count
        FROM contest.contest_inst
        WHERE contest_id = _contest_id AND status = 'OPEN'
        INTO contest_inst_id, current_participants_count;

    -- Scenario 2: OPEN instance exists and has available slots.
    IF contest_inst_id IS NOT NULL AND current_participants_count < v_max_teams_allowed THEN
        -- WARNING (production bug, preserved): this fires a NoticeResponse
        -- message to the client on EVERY successful join. At 5,000 joins/sec
        -- that is 5,000 extra protocol messages per second, each of which the
        -- backend formats and writes and the driver allocates for. It is
        -- obviously a debug leftover. It is measured, not removed -- the whole
        -- point of the N=1 shard control is to separate gains like "we deleted
        -- a stray RAISE NOTICE" from the gain that actually comes from
        -- sharding the counter.
        RAISE NOTICE '1=1';
        SELECT contest_inst_id INTO return_contest_inst_id;

    -- Scenario 3: OPEN instance exists but is full, and auto_generate = TRUE.
    ELSIF contest_inst_id IS NOT NULL
          AND current_participants_count >= v_max_teams_allowed
          AND auto_generate_flg = TRUE THEN
        SELECT public.id_generator() INTO return_contest_inst_id;

        UPDATE contest.contest_inst SET STATUS = 'FULL' WHERE id = contest_inst_id;
        INSERT INTO contest.contest_inst (id, contest_id, status)
        VALUES (return_contest_inst_id, _contest_id, 'OPEN');
        UPDATE contest.contest SET teams_joined_count = 0 WHERE id = _contest_id;

    ELSIF contest_inst_id IS NULL AND auto_generate_flg = TRUE THEN
        SELECT public.id_generator() INTO return_contest_inst_id;
        INSERT INTO contest.contest_inst (id, contest_id, status)
        VALUES (return_contest_inst_id, _contest_id, 'OPEN');
        UPDATE contest.contest SET teams_joined_count = 0 WHERE id = _contest_id;

    ELSE
        -- No OPEN instance and auto_generate = FALSE -> contest is full.
        RAISE 'Contest is full' USING ERRCODE = '01S03';
    END IF;

    RETURN return_contest_inst_id;
END
$func$ LANGUAGE plpgsql;


-- ---------------------------------------------------------------------------
-- contest.join_contest  (production migration 72, verbatim)
-- ---------------------------------------------------------------------------
-- Custom SQLSTATEs raised:
--   01S12  team has already joined
--   01R11  contest inst is full
--   01R22  user has hit max_entries_per_user
--   (plus everything join_contest_get_contest_inst can raise)
--
-- WARNING (parameter typmod): _unique_join_id is declared VARCHAR(20) while
-- contest_inst_team.unique_join_id is VARCHAR(24). PostgreSQL IGNORES length
-- modifiers on function parameters -- the argument is passed as unconstrained
-- varchar and is only length-checked on INSERT. So a 22-character id is
-- accepted by the signature and stored fine, and a 25-character id passes the
-- signature and then fails at the INSERT with 22001. Preserved as-is.
CREATE OR REPLACE FUNCTION contest.join_contest(
    _contest_id BIGINT,
    _contest_participant_team_id BIGINT,
    _unique_join_id VARCHAR(20),
    _user_id BIGINT,
    _saga_id BIGINT
)
RETURNS TABLE (
    saga_id BIGINT,
    contest_inst_id BIGINT,
    team_id BIGINT,
    contest_name TEXT,
    match_group_name TEXT,
    currency TEXT,
    entry_fee NUMERIC,
    is_unique_request BOOLEAN
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_contest_inst_id BIGINT;
    v_max_teams_allowed INT;
    v_max_entries_per_user INT;
    v_current_participants_count INT;
    v_is_contest_free BOOLEAN;
    v_auto_generate BOOLEAN;
    v_user_curr_join_count INT;
BEGIN
    -- ****************************************************************
    -- IDEMPOTENCY CHECK: Check if this unique_join_id has already been processed
    -- ****************************************************************
    --
    -- WARNING (production bug, and the subject of Experiment C1):
    -- This EXISTS runs BEFORE pg_advisory_xact_lock is taken, and it reads the
    -- transaction's own MVCC snapshot. Two concurrent retries of the same
    -- unique_join_id therefore BOTH see "not found" and BOTH proceed. Nothing
    -- in this function serialises them.
    --
    -- What happens next depends on whether the UNIQUE index on unique_join_id
    -- exists (owned by 90_reset.sql, see 00_baseline_schema.sql):
    --   index present -> one commits, the others get SQLSTATE 23505. The
    --                    caller sees a hard error where the contract promised
    --                    an idempotent replay.
    --   index absent  -> N rows are written. That is a double charge.
    -- Either way the guard below does not do the job its comment claims.
    IF EXISTS (SELECT 1 FROM contest.contest_inst_team WHERE unique_join_id = _unique_join_id) THEN
        RETURN QUERY
            SELECT
                cit.saga_id,
                cit.contest_inst_id,
                cit.team_id,
                c.name::TEXT AS contest_name,
                cmg.name::TEXT AS match_group_name,
                c.currency_code::TEXT AS currency,
                c.entry_fee,
                false AS is_unique_request
            FROM contest.contest_inst_team cit
            INNER JOIN contest.contest_inst ci ON ci.id = cit.contest_inst_id
            INNER JOIN contest.contest c ON c.id = ci.contest_id
            INNER JOIN contest.contest_match_group cmg ON cmg.id = c.match_group_id
            WHERE cit.unique_join_id = _unique_join_id;
        RETURN;
    END IF;

    -- ****************************************************************
    -- GET CONTEST INSTANCE   (takes advisory lock #1 on _contest_id << 3)
    -- ****************************************************************
    SELECT contest.join_contest_get_contest_inst(_contest_id) INTO v_contest_inst_id;

    -- ****************************************************************
    -- LOCK   (advisory lock #2 on _contest_id)
    -- ****************************************************************
    -- Everything from here to COMMIT is strictly serialised per contest,
    -- INCLUDING the commit fsync, because xact-scoped advisory locks are
    -- released by the commit record.
    PERFORM pg_advisory_xact_lock(_contest_id);

    -- Ensure this team has not already joined this Contest/Contest Instance
    --
    -- WARNING (production bug, preserved): SKIP LOCKED means "if another
    -- transaction currently holds a lock on that row, pretend it isn't there".
    -- For a duplicate check that is backwards -- a concurrent in-flight insert
    -- of the same team is exactly the case you want to see. In practice the
    -- UNIQUE (contest_inst_id, team_id) constraint is what actually enforces
    -- I2; this check only converts some of those into a friendlier error code.
    PERFORM 1 FROM contest.contest_inst_team cit
     WHERE cit.contest_inst_id = v_contest_inst_id
       AND cit.team_id = _contest_participant_team_id
     FOR KEY SHARE SKIP LOCKED;
    IF FOUND THEN
        RAISE 'team % has already joined this contest',
            _contest_participant_team_id USING ERRCODE = '01S12';
    END IF;

    -- Ensure total spot count for that particular contest_inst must be less
    -- than max_no_of_entries.
    SELECT c.max_no_of_entries, c.max_entries_per_user, ci.joined_participants_count,
           c.is_free, c.auto_generate
    FROM contest.contest c
    INNER JOIN contest.contest_inst ci ON c.id = ci.contest_id
    WHERE ci.id = v_contest_inst_id
    INTO v_max_teams_allowed, v_max_entries_per_user, v_current_participants_count,
         v_is_contest_free, v_auto_generate;

    IF v_current_participants_count >= v_max_teams_allowed THEN
        RAISE 'contest inst is full. If contest.auto_generate=true, try again'
            USING ERRCODE = '01R11';
    END IF;

    -- Get user joined count for that particular contest_inst.
    --
    -- WARNING (the second-biggest cost in the critical section): this is a
    -- COUNT(*) over contest_inst_team joined to contest_participant_team,
    -- executed WHILE HOLDING both advisory locks. Its cost grows with the size
    -- of the contest -- at 900k rows it is scanning an index range of ~900k
    -- entries per join to answer "has this user joined 3 times?". The sharded
    -- path replaces it with a single-row conditional UPSERT on
    -- contest_inst_user_entry, which is O(1) and key-scoped.
    SELECT COUNT(*)
    INTO v_user_curr_join_count
    FROM contest.contest_inst_team cit
    INNER JOIN contest.contest_participant_team cpt ON cpt.id = cit.team_id
    WHERE cit.contest_inst_id = v_contest_inst_id
      AND cpt.user_id = _user_id;

    -- Check max_entries_per_user only if it's not NULL
    IF v_max_entries_per_user IS NOT NULL AND v_user_curr_join_count >= v_max_entries_per_user THEN
        RAISE 'User % has already joined max times (%) for this contest.',
            _user_id, v_max_entries_per_user USING ERRCODE = '01R22';
    END IF;

    -- Add the contest team
    IF v_is_contest_free THEN
        INSERT INTO contest.contest_inst_team
            (contest_inst_id, team_id, saga_id, saga_status, unique_join_id)
        VALUES
            (v_contest_inst_id, _contest_participant_team_id, _saga_id, 'C', _unique_join_id);
    ELSE
        INSERT INTO contest.contest_inst_team
            (contest_inst_id, team_id, saga_id, unique_join_id)
        VALUES
            (v_contest_inst_id, _contest_participant_team_id, _saga_id, _unique_join_id);
    END IF;

    -- Update the joined_participants_count AND set FULL when count reaches max.
    --
    -- THIS IS THE CONTENDED ROW. One tuple, one t_xmax, one lane. Note that the
    -- status column IS indexed (contest_contest_inst_status_idx), so on the
    -- join that flips OPEN -> FULL this update cannot be HOT and must write an
    -- index entry. On every other join only joined_participants_count changes,
    -- which is HOT-eligible -- but contest_inst has no fillfactor override, so
    -- the page's free space runs out and HOT stops happening. Measured in
    -- 92_observe.sql.
    UPDATE contest.contest_inst SET
        status = CASE WHEN joined_participants_count + 1 >= v_max_teams_allowed
                      THEN 'FULL'::contest_inst_status
                      ELSE status
                 END,
        joined_participants_count = joined_participants_count + 1
    WHERE id = v_contest_inst_id;

    -- A SECOND global hot row: contest.teams_joined_count. Every join in the
    -- entire contest updates this one tuple too, on a table carrying 10
    -- indexes. Even if the advisory lock were removed tomorrow, this row would
    -- serialise the workload on its own via row locks.
    UPDATE contest.contest SET
        teams_joined_count = CASE
            WHEN v_auto_generate AND teams_joined_count + 1 >= v_max_teams_allowed
                THEN 0
                ELSE teams_joined_count + 1
            END
    WHERE id = _contest_id;

    RETURN QUERY
        SELECT
            _saga_id                            AS saga_id,
            v_contest_inst_id                   AS contest_inst_id,
            _contest_participant_team_id        AS team_id,
            c.name::TEXT                        AS contest_name,
            cmg.name::TEXT                      AS match_group_name,
            c.currency_code::TEXT               AS currency,
            c.entry_fee,
            true                                AS is_unique_request
        FROM contest.contest c
            INNER JOIN contest.contest_match_group cmg ON cmg.id = c.match_group_id
        WHERE c.id = _contest_id;
END;
$$;
