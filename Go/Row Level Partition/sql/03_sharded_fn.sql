-- ===========================================================================
-- 03_sharded_fn.sql
--
-- The SHARDED join path: contest.join_contest_sharded.
--
-- No advisory locks. No global counter. No COUNT(*). The four invariants are
-- each enforced at their natural scope:
--
--   I1  never oversell            -> N shard rows, single-statement conditional
--                                    UPDATE, SUM(cap) == max_no_of_entries
--   I2  one join per team         -> UNIQUE (contest_inst_id, team_id)
--   I3  per-user entry cap        -> one row per (instance, user), conditional
--                                    UPSERT
--   I4  one row per join id       -> UNIQUE (unique_join_id)
--
-- Only I1 is a shared-resource problem. I2/I3/I4 are key-scoped and were never
-- the global lock's business.
--
-- Error codes are kept identical to the baseline so the harness error taxonomy
-- compares like for like:
--   01S03  no OPEN instance
--   01S12  team has already joined
--   01R11  instance is full (all shards exhausted)
--   01R22  user has hit max_entries_per_user
--
-- Run as:  psql -v ON_ERROR_STOP=1 -d rowlock_bench -f sql/03_sharded_fn.sql
-- ===========================================================================

\set ON_ERROR_STOP on

-- ---------------------------------------------------------------------------
-- WHY EVERY PROBE USES  FOR NO KEY UPDATE ... SKIP LOCKED
-- ---------------------------------------------------------------------------
-- The obvious implementation of a probe is one statement:
--
--     UPDATE contest_inst_shard SET used = used + 1
--      WHERE contest_inst_id = ? AND shard_no = ? AND used < cap
--
-- and if the shard is uncontended that is exactly what happens. The problem is
-- what it does when the shard IS contended: under READ COMMITTED the UPDATE
-- BLOCKS on the other transaction's row lock. That other transaction is on its
-- way to COMMIT, and its row lock is not released until the commit record is
-- flushed. So a blocking probe waits across somebody else's fsync -- which is
-- precisely the failure mode row-level sharding exists to remove, reproduced
-- in miniature on one lane. Waiting for a busy shard is never the right answer
-- when N-1 other lanes are idle.
--
-- SKIP LOCKED converts "queue behind another transaction's fsync" into "pick a
-- different lane", which is what the design is actually for.
--
-- It also makes the function deadlock-free, which the blocking version is not.
-- When a blocking UPDATE waits and then finds its qual no longer true (the
-- shard filled up while it waited), PostgreSQL has already taken a tuple lock
-- via the EvalPlanQual path and that lock is held until end of transaction
-- even though the row was skipped. A transaction can therefore accumulate
-- locks on shards it did not reserve, and two transactions probing shards in
-- different orders deadlock. SKIP LOCKED never waits, so it never strands a
-- lock, so no probe can be one half of a cycle.
--
-- FOR NO KEY UPDATE rather than FOR UPDATE: the subsequent UPDATE touches only
-- `used` and `state`, never the primary key, so the weaker mode is the honest
-- one. It still conflicts with itself, which is all the mutual exclusion a
-- reserver needs.
--
-- Note carefully: `used` appears in the SELECT's WHERE clause. That is a QUERY
-- predicate and has nothing to do with the "no index may reference used" rule
-- in 02_sharded_schema.sql, which is about INDEX predicates. Query predicates
-- are free; index predicates cost HOT.
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION contest.join_contest_sharded(
    _contest_id                  BIGINT,
    _contest_participant_team_id BIGINT,
    _unique_join_id              VARCHAR(24),
    _user_id                     BIGINT,
    _saga_id                     BIGINT,
    -- Shard count is CONFIGURATION, passed in by the caller, not discovered by
    -- querying the shard table. Deriving it per join would add a scan to the
    -- hot path to learn something the application already knows. If it is
    -- wrong, probes simply miss (an out-of-range shard_no matches no row) and
    -- rung 2 recovers -- it degrades, it does not corrupt.
    _n_shards                    INT,
    -- Rung 1 budget. 4 uniform random probes over N shards.
    _max_random_probes           INT DEFAULT 4,
    -- Rung 2 budget: how many still-open shards to try before falling through
    -- to the blocking last resort.
    _max_open_probes             INT DEFAULT 32
)
RETURNS TABLE (
    saga_id           BIGINT,
    contest_inst_id   BIGINT,
    team_id           BIGINT,
    contest_name      TEXT,
    match_group_name  TEXT,
    currency          TEXT,
    entry_fee         NUMERIC,
    is_unique_request BOOLEAN,
    shard_no          SMALLINT,
    probe_attempts    INT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_inst_id            BIGINT;
    v_max_entries        INT;
    v_max_per_user       INT;
    v_is_free            BOOLEAN;
    v_contest_name       TEXT;
    v_match_group_name   TEXT;
    v_currency           TEXT;
    v_entry_fee          NUMERIC;
    v_published_status   contest_published_status;
    v_contest_status     contest_status;
    v_contest_type       SMALLINT;
    v_join_close_time    TIMESTAMPTZ;

    v_rows               INT;
    v_shard              SMALLINT;
    v_probes             INT := 0;
    v_reserved           BOOLEAN := FALSE;
    v_pick               SMALLINT;
    i                    INT;

    v_existing_saga      BIGINT;
    v_existing_inst      BIGINT;
    v_existing_team      BIGINT;
BEGIN
    -- =====================================================================
    -- STEP 0 -- resolve instance and read contest configuration.
    -- =====================================================================
    -- One query, one snapshot, NO LOCK OF ANY KIND.
    --
    -- The baseline needs advisory lock #1 here only because it may CREATE a
    -- contest_inst on the join path. That is an administrative act on the hot
    -- path: it happens at most a handful of times over a contest's life and it
    -- makes every one of the other million joins pay for a cluster-wide lock.
    -- In the sharded design instance creation and shard provisioning are
    -- provisioning-time operations (bench.provision_shards), so the join path
    -- only ever reads. 
    SELECT ci.id, c.max_no_of_entries, c.max_entries_per_user, c.is_free,
           c.name::TEXT, cmg.name::TEXT, c.currency_code::TEXT, c.entry_fee,
           c.published_status, c.status, c.contest_type,
           COALESCE(c.delayed_join_close_time, c.join_close_time)
      INTO v_inst_id, v_max_entries, v_max_per_user, v_is_free,
           v_contest_name, v_match_group_name, v_currency, v_entry_fee,
           v_published_status, v_contest_status, v_contest_type,
           v_join_close_time
      FROM contest.contest c
      JOIN contest.contest_match_group cmg ON cmg.id = c.match_group_id
      LEFT JOIN contest.contest_inst ci
             ON ci.contest_id = c.id AND ci.status = 'OPEN'
     WHERE c.id = _contest_id;

    IF NOT FOUND THEN
        RAISE 'Contest % not found', _contest_id USING ERRCODE = '01S03';
    END IF;

    -- Same validations, same SQLSTATEs as the baseline, so the error taxonomy
    -- in the results is directly comparable.
    IF v_contest_type = 1 THEN
        RAISE 'contest % has a contest_type of %. H2H is out of scope.',
            _contest_id, v_contest_type USING ERRCODE = '01S04';
    END IF;
    IF v_published_status != 'ACTIVE' THEN
        RAISE 'Contest published state must be "ACTIVE", currently: %',
            v_published_status USING ERRCODE = '01S01';
    END IF;
    IF v_contest_status != 'CREATED' THEN
        RAISE 'Contest state must be "CREATED", currently: %',
            v_contest_status USING ERRCODE = '01S02';
    END IF;
    IF v_join_close_time < NOW() THEN
        RAISE 'Contest join close time has passed.' USING ERRCODE = '01S07';
    END IF;
    IF v_inst_id IS NULL THEN
        RAISE 'Contest is full' USING ERRCODE = '01S03';
    END IF;

    -- =====================================================================
    -- STEP 1 -- I2 and I4: write the join row FIRST.
    -- =====================================================================
    -- ORDERING IS DELIBERATE. This is the cheapest step, it is entirely
    -- key-scoped, and it is the one that detects an idempotent replay. Doing it
    -- first means a replay is answered without consuming a seat or a user
    -- entry. If it ran last, every duplicate would have to be compensated with
    -- a decrement, and a compensating write is a second chance to get the
    -- accounting wrong.
    --
    -- ON CONFLICT DO NOTHING with no conflict target covers BOTH unique
    -- indexes: contest_inst_team_unique_join_id_uidx (I4) and
    -- contest_inst_team_inst_team_key (I2).
    --
    -- Contrast with the baseline's "IF EXISTS (...)" pre-check, which reads a
    -- snapshot taken before any lock and therefore lets concurrent duplicates
    -- straight through. Here the btree insertion path itself is the
    -- serialisation point: a concurrent inserter of the SAME key waits for this
    -- transaction and then re-checks. The scope of that wait is one key, not
    -- one contest.
    IF v_is_free THEN
        INSERT INTO contest.contest_inst_team
            (contest_inst_id, team_id, saga_id, saga_status, unique_join_id)
        VALUES
            (v_inst_id, _contest_participant_team_id, _saga_id, 'C', _unique_join_id)
        ON CONFLICT DO NOTHING;
    ELSE
        INSERT INTO contest.contest_inst_team
            (contest_inst_id, team_id, saga_id, unique_join_id)
        VALUES
            (v_inst_id, _contest_participant_team_id, _saga_id, _unique_join_id)
        ON CONFLICT DO NOTHING;
    END IF;
    GET DIAGNOSTICS v_rows = ROW_COUNT;

    IF v_rows = 0 THEN
        -- Something conflicted. Which one? Under READ COMMITTED this SELECT
        -- takes a fresh snapshot, so the row the conflicting transaction
        -- committed is visible now.
        SELECT cit.saga_id, cit.contest_inst_id, cit.team_id
          INTO v_existing_saga, v_existing_inst, v_existing_team
          FROM contest.contest_inst_team cit
         WHERE cit.unique_join_id = _unique_join_id;

        IF FOUND THEN
            -- I4: idempotent replay. Return the original result, no seat
            -- consumed, no counter touched. This is the contract the baseline
            -- promises and does not keep.
            RETURN QUERY SELECT
                v_existing_saga, v_existing_inst, v_existing_team,
                v_contest_name, v_match_group_name, v_currency, v_entry_fee,
                FALSE, NULL::SMALLINT, 0;
            RETURN;
        END IF;

        -- I2: same team, different join id.
        RAISE 'team % has already joined this contest',
            _contest_participant_team_id USING ERRCODE = '01S12';
    END IF;

    -- =====================================================================
    -- STEP 2 -- I3: per-user entry cap, as a single conditional UPSERT.
    -- =====================================================================
    -- Replaces the baseline's COUNT(*) over contest_inst_team JOIN
    -- contest_participant_team, which was O(rows for this user in this
    -- instance) index work executed while holding a contest-wide lock.
    --
    -- The WHERE on the DO UPDATE is what makes this safe under concurrency.
    -- When two of the same user's joins collide, the second one blocks on the
    -- first's row lock; when the first commits, PostgreSQL re-reads the LATEST
    -- version of the row and re-evaluates this WHERE against it. If the cap is
    -- now reached the update matches nothing and ROW_COUNT is 0. There is no
    -- window in which both transactions see the pre-increment value -- which
    -- is exactly the window the baseline's read-then-check leaves open.
    --
    -- NULL max_entries_per_user means "no limit"; COALESCE to INT_MAX rather
    -- than branching keeps this to one statement and one plan.
    --
    -- ON CONFLICT ON CONSTRAINT, not ON CONFLICT (contest_inst_id, user_id):
    -- this function's RETURNS TABLE declares an output column named
    -- contest_inst_id, and every RETURNS TABLE output name is also a plpgsql
    -- variable. Inside the function body that variable shadows the column of
    -- the same name, and a conflict-target column list is one of the few
    -- places in SQL that cannot be table-qualified, so there is no way to
    -- disambiguate it -- PostgreSQL rejects the statement outright with
    -- "column reference contest_inst_id is ambiguous". Naming the constraint
    -- sidesteps the shadowing entirely and is more precise about intent
    -- besides: it says which uniqueness rule this UPSERT is negotiating with,
    -- rather than hoping a column list resolves to it.
    INSERT INTO contest.contest_inst_user_entry AS ue
        (contest_inst_id, user_id, entries_used)
    VALUES (v_inst_id, _user_id, 1)
    ON CONFLICT ON CONSTRAINT contest_inst_user_entry_pkey DO UPDATE
        SET entries_used = ue.entries_used + 1
        WHERE ue.entries_used < COALESCE(v_max_per_user, 2147483647);
    GET DIAGNOSTICS v_rows = ROW_COUNT;

    IF v_rows = 0 THEN
        -- Raising here rolls the whole function back, including the
        -- contest_inst_team row inserted in STEP 1. That is what keeps the
        -- "every reported failure has NO row" gate honest: there is exactly one
        -- transaction and it is all-or-nothing.
        RAISE 'User % has already joined max times (%) for this contest.',
            _user_id, v_max_per_user USING ERRCODE = '01R22';
    END IF;

    -- =====================================================================
    -- STEP 3 -- I1: reserve capacity. THE PROBE LADDER.
    -- =====================================================================

    -- --- Rung 1: uniform random probes ----------------------------------
    -- Random, NOT hash(user_id). Hashing looks attractive (no probing, perfect
    -- cache locality) and is wrong: it pins a user to one shard for life, so
    -- when that shard fills, that user is refused while capacity sits unused on
    -- the other N-1 shards. Capacity that exists but cannot be sold is worse
    -- than no capacity at all, because the contest is not full and the user is
    -- told it is. Random selection has no such stranding: any join can land on
    -- any shard with capacity.
    --
    -- With N shards f fraction full, one random probe succeeds with
    -- probability ~(1-f), so k probes reach 1-f^k. That is why the tail of
    -- Experiment B is the interesting part and the first 95% is not.
    FOR i IN 1.._max_random_probes LOOP  
        v_probes := v_probes + 1;
        v_pick := floor(random() * _n_shards)::SMALLINT; 

        SELECT s.shard_no INTO v_shard
          FROM contest.contest_inst_shard s
         WHERE s.contest_inst_id = v_inst_id
           AND s.shard_no = v_pick
           AND s.used < s.cap
           FOR NO KEY UPDATE SKIP LOCKED;

        IF FOUND THEN
            v_reserved := TRUE;
            EXIT;
        END IF;
    END LOOP;

    -- --- Rung 2: enumerate shards still marked open ---------------------
    -- Reached once random probing starts missing, i.e. the contest is mostly
    -- full. state = 'O' is served by contest_inst_shard_open_idx, so this reads
    -- only the shards worth trying rather than all N.
    --
    -- ORDER BY random() and not ORDER BY shard_no: a deterministic order would
    -- funnel every transaction at the lowest-numbered open shard and
    -- re-serialise the tail on one row. The set being ordered is at most N
    -- (<= 128) rows on one page, so the sort is free.
    --
    -- `state` can lag reality -- a shard can be full while still marked 'O' if
    -- the transaction that filled it has not committed. That is harmless: the
    -- `used < s.cap` predicate below is the authority, `state` is only a hint
    -- about where to look.
    IF NOT v_reserved THEN 
        FOR v_pick IN 
            SELECT s.shard_no
              FROM contest.contest_inst_shard s
             WHERE s.contest_inst_id = v_inst_id  
               AND s.state = 'O'
             ORDER BY random()
             LIMIT _max_open_probes
        LOOP
            v_probes := v_probes + 1;

            SELECT s.shard_no INTO v_shard
              FROM contest.contest_inst_shard s
             WHERE s.contest_inst_id = v_inst_id
               AND s.shard_no = v_pick
               AND s.used < s.cap
               FOR NO KEY UPDATE SKIP LOCKED;

            IF FOUND THEN
                v_reserved := TRUE;
                EXIT;
            END IF;
        END LOOP;
    END IF;

    -- --- Rung 3: blocking last resort, STRICTLY ASCENDING ---------------
    -- Every probe above is non-blocking, so all of them together can report
    -- "no capacity" while capacity actually exists and is merely locked by
    -- in-flight transactions. Declaring the contest full on that basis would
    -- refuse a paying user for a seat that is available -- an I1 violation in
    -- the other direction. So the ladder ends with one pass that is willing to
    -- wait.
    --
    -- ORDER BY shard_no ASC is load-bearing, not cosmetic. This is the only
    -- place a transaction can hold one shard's tuple lock while waiting for
    -- another's (the EvalPlanQual retention described at the top of this file).
    -- Because every transaction that gets here holds no shard locks yet (rungs
    -- 1 and 2 either succeeded and exited, or took nothing) and then acquires
    -- in one globally agreed order, no cycle can form. Reorder this and you
    -- introduce deadlocks that appear only under the tail of Experiment B.
    IF NOT v_reserved THEN
        FOR v_pick IN
            SELECT s.shard_no
              FROM contest.contest_inst_shard s
             WHERE s.contest_inst_id = v_inst_id
               AND s.used < s.cap
             ORDER BY s.shard_no ASC
        LOOP
            v_probes := v_probes + 1;

            -- Single-statement conditional increment. NOT a read in plpgsql,
            -- an increment, and a write of an absolute value -- that pattern
            -- cannot be protected by EvalPlanQual and WILL oversell. `used < cap`
            -- has to be inside the statement for the re-check to have anything
            -- to re-check.
            UPDATE contest.contest_inst_shard s
               SET used  = s.used + 1,
                   state = CASE WHEN s.used + 1 >= s.cap THEN 'F'::"char"
                                ELSE s.state END
             WHERE s.contest_inst_id = v_inst_id
               AND s.shard_no = v_pick
               AND s.used < s.cap;
            GET DIAGNOSTICS v_rows = ROW_COUNT;

            IF v_rows = 1 THEN
                v_shard    := v_pick;
                v_reserved := TRUE;
                EXIT;
            END IF;
        END LOOP;

        IF v_reserved THEN
            -- Rung 3 already performed the increment; skip the shared one.
            RETURN QUERY SELECT
                _saga_id, v_inst_id, _contest_participant_team_id,
                v_contest_name, v_match_group_name, v_currency, v_entry_fee,
                TRUE, v_shard, v_probes;
            RETURN;
        END IF;
    END IF;

    IF NOT v_reserved THEN
        -- Genuinely out of capacity. Rolls back STEP 1 and STEP 2.
        RAISE 'contest inst is full' USING ERRCODE = '01R11';
    END IF;

    -- --- Commit the reservation for rungs 1 and 2 -----------------------
    -- We hold FOR NO KEY UPDATE on v_shard, so nobody else can have touched it
    -- and `used < cap` is still true. The predicate is repeated anyway: it costs
    -- nothing, and it keeps this statement correct in isolation rather than
    -- correct only because of what happened two statements ago.
    --
    -- THE `state` ASSIGNMENT AND HOT: this writes the `state` column, which IS
    -- in the HOT-blocking attribute set (it appears in
    -- contest_inst_shard_open_idx's predicate). That does not break HOT,
    -- because PostgreSQL decides HOT eligibility by COMPARING the old and new
    -- values of the interesting attributes (heap_update ->
    -- HeapDetermineColumnsInfo), not by looking at which columns appear in the
    -- SET list. On the ~cap-1 joins where the CASE yields the shard's existing
    -- state, `state` is unmodified and the update stays HOT. Only the single
    -- 'O' -> 'F' transition per shard -- N writes over the whole contest, not
    -- one per join -- is non-HOT. 92_observe.sql asserts hot_pct >= 95 to prove
    -- this reasoning held in practice.
    UPDATE contest.contest_inst_shard s
       SET used  = s.used + 1,
           state = CASE WHEN s.used + 1 >= s.cap THEN 'F'::"char"
                        ELSE s.state END
     WHERE s.contest_inst_id = v_inst_id
       AND s.shard_no = v_shard
       AND s.used < s.cap;
    GET DIAGNOSTICS v_rows = ROW_COUNT;

    IF v_rows <> 1 THEN
        -- Unreachable while we hold the row lock. If it ever fires, something
        -- about the locking model is not what this function believes, and the
        -- run must fail loudly rather than quietly under-count.
        RAISE 'shard reservation lost after lock: inst=% shard=% rows=%',
            v_inst_id, v_shard, v_rows USING ERRCODE = 'XX000';
    END IF;

    -- NOTE what is NOT here:
    --   no UPDATE contest.contest_inst SET joined_participants_count = ...
    --   no UPDATE contest.contest SET teams_joined_count = ...
    -- Both were single global rows updated by every join. SUM(shard.used) is
    -- the participant count now; 91_verify.sql asserts it equals
    -- COUNT(*) FROM contest_inst_team after every run. A contest is full when
    -- the ladder is exhausted, not when a status column says so.

    RETURN QUERY SELECT
        _saga_id, v_inst_id, _contest_participant_team_id,
        v_contest_name, v_match_group_name, v_currency, v_entry_fee,
        TRUE, v_shard, v_probes;
END;
$$;
