-- ===========================================================================
-- 02_sharded_schema.sql
--
-- ROW-LEVEL COUNTER SHARDING.
--
-- This is NOT table partitioning and NOT database sharding. One table, N rows,
-- one database, one PostgreSQL instance.
--
-- THE MECHANISM
-- -------------
-- A PostgreSQL row lock is not held in a lock table. It is written into the
-- tuple header: t_xmax carries the locking transaction id (or a multixact id),
-- with t_infomask bits saying what kind of lock it is. There is no shared
-- memory entry and no lock manager partition involved for an uncontended row
-- lock -- it is a heap page write.
--
-- The consequence is the whole design: N distinct tuples have N distinct
-- t_xmax fields, therefore N independent locks, therefore N concurrent lanes.
-- Splitting one counter row into N counter rows converts a serialisation
-- problem into a fan-out problem, without introducing a single new moving
-- part outside the database.
--
-- WHAT IS BEING REPLACED
-- ----------------------
--   contest_inst.joined_participants_count  (1 row, all joins)   -> contest_inst_shard  (N rows)
--   COUNT(*) over contest_inst_team          (O(contest size))   -> contest_inst_user_entry (O(1))
--   contest.teams_joined_count               (1 row, all joins)  -> deleted
--   pg_advisory_xact_lock x2                                     -> deleted
--
-- Run as:  psql -v ON_ERROR_STOP=1 -d rowlock_bench -f sql/02_sharded_schema.sql
-- ===========================================================================

\set ON_ERROR_STOP on

-- ---------------------------------------------------------------------------
-- contest.contest_inst_shard
-- ---------------------------------------------------------------------------
-- One row per (contest instance, shard). `used` is the counter, `cap` is this
-- shard's slice of max_no_of_entries, `state` is a coarse "is there any point
-- probing me" flag.
--
-- ***********************************************************************
-- *** CRITICAL INVARIANT: NO INDEX MAY REFERENCE `used`.              ***
-- ***********************************************************************
-- Not as a key column, not as an INCLUDE column, and NOT AS A PARTIAL INDEX
-- PREDICATE. PostgreSQL builds the set of attributes that block HOT
-- (RelationGetIndexAttrBitmap with INDEX_ATTR_BITMAP_HOT_BLOCKING) from every
-- column any index mentions -- and predicate columns are pulled in via
-- pull_varattnos over the index predicate, exactly like key columns. An index
-- such as
--        CREATE INDEX ... ON contest_inst_shard (shard_no) WHERE used < cap
-- would therefore put `used` in the HOT-blocking set, and since EVERY join
-- updates `used`, EVERY update would become a non-HOT update: a new index
-- tuple in every index, a broken HOT chain, and index bloat proportional to
-- the join count. The benchmark would then be measuring index write
-- amplification instead of lock contention, and the sharded numbers would be
-- wrong in the direction that makes the design look bad.
--
-- The only permitted index is the primary key plus the (contest_inst_id,
-- shard_no) WHERE state = 'O' partial index below. `state` IS in the
-- HOT-blocking set as a result -- that is acceptable because state changes at
-- most once per shard over the life of a contest (O -> F when the shard
-- exhausts), i.e. N times total, versus `used` which changes once per join.
--
-- 92_observe.sql asserts hot_pct >= 95 after every sharded run and fails the
-- run if this invariant has been broken.
--
-- STORAGE PARAMETERS, and why each is not optional:
--
--   fillfactor = 70
--     HOT requires free space ON THE SAME PAGE to write the new tuple version.
--     At the default fillfactor of 100 the page is packed at load time, the
--     first few updates consume what little slack exists, and then every
--     update migrates to a new page and breaks the HOT chain. 70 reserves 30%
--     of each page as an update arena. With N <= 128 rows the whole table is
--     one or two pages, so this costs nothing.
--
--   autovacuum_vacuum_scale_factor = 0, autovacuum_vacuum_threshold = 500
--     The global default is "vacuum after 50 + 0.2 * n_live_tup dead tuples".
--     For a 128-row table that is 75 dead tuples -- but this table takes ~1M
--     updates, so it generates dead tuples at thousands per second while
--     n_live_tup stays at 128. Scale factor 0 plus a flat threshold of 500
--     converts the rule to "vacuum every 500 dead tuples", which is what a
--     tiny-and-extremely-hot table needs. (HOT pruning reclaims most of this
--     opportunistically on page access, but pruning cannot remove the line
--     pointers; only vacuum can.)
--
--   autovacuum_vacuum_cost_delay = 0
--     Do not throttle the worker on this table. If vacuum falls behind, free
--     space runs out, HOT stops, and the run fails the hot_pct gate for a
--     reason that has nothing to do with the design under test.
CREATE TABLE IF NOT EXISTS contest.contest_inst_shard (
    contest_inst_id BIGINT   NOT NULL,
    shard_no        SMALLINT NOT NULL,
    used            INT      NOT NULL DEFAULT 0 CHECK (used >= 0),
    cap             INT      NOT NULL CHECK (cap >= 0),

    -- "char" (quoted) is the 1-byte internal type, not char(1). It keeps the
    -- row narrow enough that the whole shard set stays on one heap page for
    -- N <= 128, which in turn keeps every probe a single buffer hit.
    -- 'O' = open, 'F' = full.
    state           "char"   NOT NULL DEFAULT 'O',

    PRIMARY KEY (contest_inst_id, shard_no)
)
WITH (
    fillfactor = 70,
    autovacuum_vacuum_scale_factor = 0,
    autovacuum_vacuum_threshold = 500,
    autovacuum_vacuum_cost_delay = 0
);

-- The ONE permitted secondary index. Used by rung 2 of the probe ladder to
-- enumerate shards still worth probing once random probing starts missing.
-- Columns referenced: contest_inst_id, shard_no (key) and state (predicate).
-- `used` appears nowhere. Do not add it.
CREATE INDEX IF NOT EXISTS contest_inst_shard_open_idx
    ON contest.contest_inst_shard (contest_inst_id, shard_no)
    WHERE state = 'O';

-- ---------------------------------------------------------------------------
-- contest.contest_inst_user_entry
-- ---------------------------------------------------------------------------
-- Replaces the baseline's per-user COUNT(*).
--
-- Invariant I3 ("a user joins at most max_entries_per_user times per instance")
-- is KEY-SCOPED, not a shared resource: it constrains one user at a time. It
-- therefore belongs on a row keyed by that user, where the row lock only
-- conflicts with that same user's other in-flight joins. In the mega-contest
-- workload, one user's concurrent joins are a handful; the contest's are a
-- million. Moving I3 off the global lock and onto a per-user row is a
-- reduction in lock scope of five orders of magnitude, and it turns an
-- O(contest size) COUNT(*) into an O(1) index lookup.
--
-- fillfactor 70 for the same reason as the shard table: entries_used is
-- updated in place and we want those updates to stay HOT. No index references
-- entries_used.
CREATE TABLE IF NOT EXISTS contest.contest_inst_user_entry (
    contest_inst_id BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    entries_used    INT    NOT NULL DEFAULT 0 CHECK (entries_used >= 0),

    PRIMARY KEY (contest_inst_id, user_id)
)
WITH (
    fillfactor = 70,
    autovacuum_vacuum_scale_factor = 0,
    autovacuum_vacuum_threshold = 1000,
    autovacuum_vacuum_cost_delay = 0
);

-- ---------------------------------------------------------------------------
-- Uniqueness for I2 and I4
-- ---------------------------------------------------------------------------
-- I2 (a team joins an instance at most once) and I4 (a unique_join_id produces
-- at most one row) are both key-scoped. Neither needs a lock held by the
-- application: a unique index already provides exactly the right guarantee at
-- exactly the right scope, enforced by the btree insertion path, which blocks
-- only the concurrent inserter of the SAME key.
--
-- I2's index is a table constraint declared in 00_baseline_schema.sql
-- (contest_inst_team_inst_team_key) because production has it and both paths
-- need it. Assert it rather than redefine it.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'contest_inst_team_inst_team_key'
          AND conrelid = 'contest.contest_inst_team'::regclass
    ) THEN
        RAISE EXCEPTION
            'I2 index missing: contest_inst_team_inst_team_key not found. Run 00_baseline_schema.sql first.';
    END IF;
END
$$;

-- I4's index is per-run, not permanent -- see the long note in
-- 00_baseline_schema.sql. The sharded path REQUIRES it; the baseline runs both
-- with and without it so Experiment C1 can report both failure modes. This
-- helper is the single owner of that index; 90_reset.sql calls it.
CREATE OR REPLACE FUNCTION bench.set_unique_join_id_index(_enabled BOOLEAN)
RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
    IF _enabled THEN
        -- Not CONCURRENTLY: this only ever runs between runs, on an empty
        -- table, and CONCURRENTLY cannot run inside a transaction block.
        CREATE UNIQUE INDEX IF NOT EXISTS contest_inst_team_unique_join_id_uidx
            ON contest.contest_inst_team (unique_join_id);
    ELSE
        DROP INDEX IF EXISTS contest.contest_inst_team_unique_join_id_uidx;
    END IF;
END;
$$;

-- ---------------------------------------------------------------------------
-- bench.provision_shards
-- ---------------------------------------------------------------------------
-- Splits _capacity across _n_shards so that SUM(cap) == _capacity EXACTLY.
--
-- WHY EXACTLY MATTERS: SUM(cap) is the ONLY thing enforcing invariant I1
-- (never oversell) in the sharded design. There is no global counter left to
-- cross-check against. Integer division alone loses `_capacity % _n_shards`
-- seats -- a 100,000-seat contest across 128 shards would silently sell 99,968
-- and strand 32 paid-for seats. Rounding up instead would oversell by up to
-- 127. Neither is acceptable, so the remainder is distributed one seat at a
-- time to the lowest-numbered shards:
--
--     shard i gets  floor(capacity / n) + (1 if i < capacity mod n else 0)
--
-- The uneven caps this produces (some shards one seat larger) are harmless:
-- shard selection is random, so a one-seat difference across 128 shards is far
-- below the variance of random assignment itself.
--
-- The assertion at the end is not defensive decoration. If it ever fires, every
-- throughput number from the run is meaningless because the capacity invariant
-- the run was verifying did not hold at t=0.
CREATE OR REPLACE FUNCTION bench.provision_shards(
    _contest_inst_id BIGINT,
    _n_shards        INT,
    _capacity        INT
)
RETURNS INT
LANGUAGE plpgsql
AS $$
DECLARE
    v_base      INT;
    v_remainder INT;
    v_sum       BIGINT;
BEGIN
    IF _n_shards < 1 THEN
        RAISE EXCEPTION 'provision_shards: _n_shards must be >= 1, got %', _n_shards;
    END IF;
    IF _n_shards > 32767 THEN
        -- shard_no is SMALLINT; the probe ladder casts into it.
        RAISE EXCEPTION 'provision_shards: _n_shards must fit in SMALLINT, got %', _n_shards;
    END IF;
    IF _capacity < 0 THEN
        RAISE EXCEPTION 'provision_shards: _capacity must be >= 0, got %', _capacity;
    END IF;

    v_base      := _capacity / _n_shards;    -- integer division
    v_remainder := _capacity % _n_shards;

    DELETE FROM contest.contest_inst_shard WHERE contest_inst_id = _contest_inst_id;

    INSERT INTO contest.contest_inst_shard (contest_inst_id, shard_no, used, cap, state)
    SELECT
        _contest_inst_id,
        i::SMALLINT,
        0,
        v_base + CASE WHEN i < v_remainder THEN 1 ELSE 0 END,
        -- A zero-capacity shard is born full. Marking it 'F' keeps it out of
        -- the partial index so the probe ladder never wastes a rung on it.
        -- This happens when _capacity < _n_shards, e.g. the C2 oversell test
        -- at capacity 1000 with 128 shards is fine, but capacity 50 with 128
        -- shards would produce 78 of them.
        CASE WHEN v_base + CASE WHEN i < v_remainder THEN 1 ELSE 0 END = 0
             THEN 'F'::"char" ELSE 'O'::"char" END
    FROM generate_series(0, _n_shards - 1) AS g(i);

    SELECT SUM(cap) INTO v_sum
    FROM contest.contest_inst_shard
    WHERE contest_inst_id = _contest_inst_id;

    IF v_sum IS DISTINCT FROM _capacity::BIGINT THEN
        RAISE EXCEPTION
            'provision_shards INVARIANT VIOLATION: SUM(cap)=% but capacity=% for contest_inst %',
            v_sum, _capacity, _contest_inst_id;
    END IF;

    RETURN _n_shards;
END;
$$;

-- ---------------------------------------------------------------------------
-- bench.assert_no_index_on_used
-- ---------------------------------------------------------------------------
-- Guards the critical invariant at the top of this file. Called by 90_reset.sql
-- before every run so that a well-meaning index added later fails the harness
-- immediately, rather than quietly turning the sharded results into a
-- measurement of index bloat.
--
-- pg_index.indkey covers key + INCLUDE columns; pg_index.indpred is the
-- predicate expression tree. Checking indkey alone would miss exactly the
-- partial-index case that is easiest to get wrong.
CREATE OR REPLACE FUNCTION bench.assert_no_index_on_used()
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    v_attnum SMALLINT;
    v_bad    TEXT;
BEGIN
    SELECT attnum INTO v_attnum
    FROM pg_attribute
    WHERE attrelid = 'contest.contest_inst_shard'::regclass AND attname = 'used';

    SELECT string_agg(c.relname, ', ') INTO v_bad
    FROM pg_index i
    JOIN pg_class c ON c.oid = i.indexrelid
    WHERE i.indrelid = 'contest.contest_inst_shard'::regclass
      AND (
            -- key or INCLUDE column
            v_attnum = ANY (i.indkey::SMALLINT[])
            -- or referenced anywhere in the partial-index predicate / an
            -- expression index. pg_get_expr renders the tree; matching the
            -- column name in it is coarse but it is a tripwire, and a false
            -- positive here costs one comment while a false negative costs the
            -- whole experiment.
         OR (i.indpred IS NOT NULL
             AND pg_get_expr(i.indpred, i.indrelid) ~ '\yused\y')
         OR (i.indexprs IS NOT NULL
             AND pg_get_expr(i.indexprs, i.indrelid) ~ '\yused\y')
          );

    IF v_bad IS NOT NULL THEN
        RAISE EXCEPTION
            E'HOT INVARIANT VIOLATION: index(es) [%] reference contest_inst_shard.used.\n'
            'PostgreSQL includes partial-index predicate columns in the HOT-blocking\n'
            'attribute set, so every UPDATE of `used` would write index entries and the\n'
            'benchmark would measure index bloat instead of lock contention.\n'
            'Drop the index or exclude `used` from it. See sql/02_sharded_schema.sql.',
            v_bad;
    END IF;
END;
$$;
