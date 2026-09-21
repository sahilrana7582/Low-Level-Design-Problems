# LLD PROGRESS LOG

> Read at the start of every session. Append the Phase-8 card at the end of every session.
> Profile & curriculum: `LLD_TUTOR_PROMPT.md`

---

## PARKED

- **Library Management System (parked 2026-09-21 after S6, at 60/100, raw 66).** Sahil closed the build
  and moved on. He asked for an **end-of-day session (2026-09-21) to find "the absolutely best way to
  solve this LLD"** = Phase 7 for Library: reference design + `Decision | Mine | Yours | Why` diff table +
  one deliberately over-engineered version. Code state at handoff: B22 (`Booking.returnDate` not
  `volatile` — triggers the §6 cap) and B23 (return-side partial failure) still OPEN, no JUnit (G16),
  `Main` = 699 lines of println. Open with a recall on B22 before showing any reference design.
- **Session 7 · Subscription Billing (closed 2026-09-21 — see the S7 card).** Phase 0 recall 3/10
  (see recall cards 3 and 6). Phase 2: first live data for dim 1 = **4/10 provisional** (round 1). **Locked scope —
  In:** plan catalog, subscribing a customer to a plan, per-plan configurable trials (whole days), monthly
  recurring billing decisions ("what is owed, when"), a query for a customer's current standing. **Non-goals:**
  the product itself, payment collection (no cards/gateways), customers/auth, access enforcement, UI.
  Everything not stated above is open; anything he doesn't close, I decide in Phase 5. Rule for Phase 5:
  JUnit + Clock-injected time are required (G15/G16).
  Phase 3 (paper design, 2026-09-21): **29/100 provisional** (23/80; dim 5 has nothing to grade, dim 9 waits
  for the gauntlet). Answered ~1.5 of 9 Phase-3 questions; design re-added two locked non-goals
  (`consumeService`, `type: Once`); nothing computes an amount owed. Phase 4 gauntlet issued (7 items).
  Phase 4 answers: 4/7 answered, none complete; SKIPPED #4 (renewal vs cancel vs duplicate job), #5 (job down 3
  days), #6 (delete `Once`/`consumeService`). Dim 9 = 3/10 (first live data point); provisional design 30/100.
  Owed before Phase 5: #4/#5/#6 + a v2 design (state machine, where "owes" is computed, what runs when time
  passes, the atomic unit, source of "now"). Transfer nudge given: double renewal = Library's double `returnBook`.
- **Coaching mode change (2026-09-21).** Sahil said the sessions were too advanced and overwhelming ("our goal
  is to master LLD, not overwhelm me"). New rule: **one level per session, decent-first, same problem revisited**
  (ONCE model → abstractions/patterns → Recurring → concurrency and testability later). Sensei proposed a Level 1–6
  roadmap (L1 decent first solution · L2 clean structure & abstractions · L3 extend to Recurring · L4 testable: Clock +
  JUnit · L5 concurrency · L6 timed interview on a NEW problem); a level passes at 70/100 with every criterion ≥ 5;
  the Phase 4 gauntlet only from L3. **Awaiting his approval** (he sent "Rate Now" without answering the two decisions; S8 treated it as "close Level 1 first"). Until he approves otherwise, do NOT apply the old
  10-dimension bar, the data-race cap, or unprompted advanced attacks to a first attempt.

---

## SESSION LOG

```
SESSION 1 · Thread-safe KV Store w/ TTL · 2026-09-09 – 2026-09-10
Target gaps: G1, G15, G16, G12
Score: 50/100 (partial — dims 1 Requirements/Scoping and 5 Pattern Selection not
  exercised this session; 9 Extensibility not yet attacked) → Verdict: No Hire
Won:  Reached for PriorityQueue<ExpireKey> + a version tag to solve "no O(log n)
        arbitrary removal," unprompted, before any unlock — real instinct.
      Correctly identified ReentrantReadWriteLock as the primitive to reach for
        at all — first lock in ~3,700+ lines of code, ever.
Lost: Could not produce the lock scoping, the read→write upgrade hazard in
        get(), or the sweeper's wait/signal protocol without two separate
        "unlock it" requests — the P0 target gap (G1) was received, not
        produced.
      G12 (domain exceptions), G15 (Clock injection), G16 (tests) — all three
        other assigned gaps — were never attempted at all.
TRANSFERABLE RULE: When a thread must block for a variable, data-driven
  duration, pair a lock with a Condition instead of Thread.sleep() — sleep
  can't be woken early by a nearer deadline arriving mid-wait, but
  Condition.await(timeout) can be signal()-ed awake and re-evaluated.
RECALL CARD → Q: Why can't you call writeLock().lock() while already holding
  readLock() on the same ReentrantReadWriteLock?
  A: Not a supported upgrade — the write lock requires no readers hold the
  lock, so a thread holding the read lock blocks indefinitely waiting on the
  write lock. Release the read lock fully first, then re-check state from
  scratch since it wasn't held continuously.
Next session: Concurrent Seat/Inventory Reservation Service with lock expiry
  targeting G1 (re-test cold, no unlock), G10 (lock expiry/reaper).
```

```
SESSION 2 · Concurrent Hotel-Room Hold Reservation (UnitHoldingTemporary) · 2026-09-10
Target gaps: G1 (retest cold), G10
Score: 54/100 (partial — dims 1/5/9 not exercised) → Verdict: No Hire on aggregate,
  but the actually-targeted dimension (6, Concurrency) went 3→8 and was
  self-produced with zero unlocks. Aggregate is dragged down by dims 3/4/8/10,
  which were out of scope this round, not regressed.
Won:  G1 and G10 independently produced, no unlock, verified correct under a
        real 5-thread race (exactly 2/2 available rooms won, 3/5 correctly
        rejected) — first time a concurrency gap closed the same session it
        was cold-assigned.
      Independently re-fixed B1 (atomic check-and-commit under one lock) and
        B2 (TreeSet comparator now tie-breaks on a unique id) from the
        original 4-problem audit, unprompted, in a brand-new domain — direct
        evidence the meta-gap (root cause #5, no cross-problem transfer) is
        moving.
Lost: holdId is never checked for uniqueness (HotelInventory.holdRoom), and
        findHoldInternal returns a nondeterministic first match on collision
        — a real correctness bug with a concrete break case.
      confirmHold/cancelHold both do pendingHolds.remove(hold), an O(n) PQ
        scan — the exact "stale entry, skip lazily via a version tag instead
        of eager removal" lesson from KV_Store_TTL was not applied here.
        Confirms the transfer isn't automatic yet; it has to be deliberately
        checked for on every new problem.
TRANSFERABLE RULE: A lesson learned via "unlock it" isn't transferred until
  it's reproduced unprompted in a different problem — track each taught
  concept (not just each gap id) across problems until it shows up on its own.
RECALL CARD → Q: What specifically closes a gap in this ledger (vs. just
  "improving")?
  A: Solving it correctly, unprompted, in a DIFFERENT problem than the one
  where it was taught/unlocked — which is what happened to G1 and G10 this
  session.
Next session: TBD — candidate: fix the holdId-uniqueness bug + the PQ lazy-
  skip pattern in THIS codebase first (quick, closes the loop cleanly), or
  move to a fresh problem (Rate Limiter / Bounded Blocking Queue) to test
  whether the Condition/wait-signal mechanic itself (not just "add a lock")
  transfers next.
```

```
SESSION 3 · In-Process Pub/Sub Event Bus (MPSC ring buffer + Observer) · 2026-09-18
Target gaps: informal — no Phase 0-2 run, this was ad-hoc build-then-review,
  not a full engine session. In hindsight it exercised G1 (concurrency, this
  time via CAS/atomics instead of locks) and G9 (Observer, first use ever).
Score: ~68/100 (partial — dims 1 Requirements/Scoping and 9 Extensibility
  not exercised; no Phase 1/2 vague-prompt or Phase 4 gauntlet was run this
  session) → Verdict: Lean Hire on the exercised dimensions.
Won:  Built a real, previously-never-used Observer pattern with a genuinely
        advanced lock-free single-active-drainer CAS handoff for dispatch
        (Topic.publish() self-elects a drainer thread, drains to empty,
        re-checks after releasing the flag) — self-designed, materially more
        sophisticated than the locking in S1/S2.
      First submission with an actual automated correctness+throughput
        verification harness shipped as part of the deliverable (Main.java
        simulation, PASS/FAIL assertions on every subscriber/topic pair)
        instead of "read the console and eyeball it" — real move on G16.
Lost: The core MpscRingBuffer shipped with a genuine data race — producer
        bumped the shared sequence (publishing "data ready" to the consumer)
        BEFORE writing the payload, so a descheduled producer left a gap the
        consumer could poll as empty/stale while the slot was permanently
        skipped. Walked through the exact breaking interleaving and the JMM
        happens-before question directly; still required "I'm stuck, unlock
        it" rather than being self-diagnosed. Same shape as G1 in S1: the
        concurrency instinct to reach for CAS is there, but tracing WHY a
        specific interleaving breaks it, unprompted, is not yet.
      Independently-written broadcast() had its own new bug (unbounded
        recursion in the re-acquire path -> StackOverflow under sustained
        multi-producer load) — caught and fixed before it could crash the
        very simulation being built. Also reintroduced G5 in new code
        (System.out.println inside Topic.handleSubscriberFailure).
      Dims 1 (Requirements & scoping) and 9 (Extensibility, proven via
        attack) still have zero data points after 3 sessions — this session
        skipped Phase 1/2/4 entirely by going straight to implementation.
TRANSFERABLE RULE: In any lock-free structure, the "this slot is ready"
  signal the consumer checks must be raised strictly AFTER the payload
  write, using a PER-SLOT publish marker — bumping one shared cursor before
  the data write lands is backwards regardless of producer count, and it's
  a real JMM data race (no happens-before edge), not just a scheduling
  problem that only shows up when you're unlucky.
RECALL CARD → Q: Why is "CAS the shared producer cursor forward, then write
  the payload" unsafe even with a single producer thread and zero
  preemption?
  A: The JMM gives no happens-before edge from a plain write that comes
  AFTER a volatile/atomic write in program order to a later read of that
  location by another thread — the consumer's read of the cursor only
  happens-before what preceded that write in the producer's program order,
  not what follows it. The payload write must happen-before the publish
  signal, never the other way around.
Next session: run a FULL Phase 1-8 session, no shortcuts, on a
  layering-shaped (not concurrency-shaped) problem — dims 1/3/9 need their
  first real data point. Library Management System targeting G3, G4, G12,
  G16 (JUnit this time, not a bespoke harness).
```

```
SESSION 4 · Library Management System (self-built implementation, reviewed; no Phase 1–4 run) · 2026-09-21
Target gaps: assigned G3, G4, G12, G16 (S3's "next session") — user asked to be scored
  only on what was built, so none were addressed in the build. Actually exercised:
  G1 (transfer into a layering-shaped problem), G5, G13, G15.
Score: 43/100 (partial — dim 1 not gradeable from code, dim 5 nothing to grade; dim 9 scored
  by paper gauntlet, not a live Phase 4) → Verdict: No Hire. Data-race cap (60) triggered
  but non-binding at 43 raw.
Won:  Critical section is correct and verified: borrowBook does check + decrement + create
        under one write lock, no read→write upgrade; 400 threads racing for 50 copies →
        exactly 50 succeeded. G1 transferred into a new problem shape without prompting.
      Built the O(1) inverted indexes (bookByTags/bookByAuthor) that B10 said the Movie
        system lacked, and kept every domain class free of System.out (G5 — presentation
        lives only in Main).
Lost: The lock guards the maps, not what escapes them: getUserBooking/getBookByTag/
        getBookByAuthor return live ArrayLists from inside the read lock; a reader iterating
        while borrowBook appended threw 16,010 ConcurrentModificationExceptions in 1.5s
        (95 clean iterations). Same "builds it, can't trace it under attack" gap as S3,
        second shape.
      Entities regressed to mutable JavaBeans (public setters incl. setId, zero `final` in
        Book/User/Booking — S9 lost): setTags/setId silently desync the indexes; addBook on
        an existing id overwrote stock and doubled index entries (3 copies in the world for
        a 2-copy catalogue); 3 failure conventions in one class (Optional / silent void /
        null) — G12 untouched for 4 sessions; verification fell back from S3's PASS/FAIL
        harness to 460 lines of println (G16).
TRANSFERABLE RULE: A lock guards only what happens while it is held — anything that escapes
  it (a live list, a mutable entity, a map value) is unprotected the instant the lock
  releases, so return snapshots or immutable values, never internals.
RECALL CARD → Q: A getter takes the read lock, does map.get(key), and returns the List it
  found. Is the caller's for-loop over that list safe against concurrent writers?
  A: No. The lock is released on return, so the caller iterates the live list unlocked
  while a writer mutates it → ConcurrentModificationException / data race. Snapshot inside
  the lock (List.copyOf) or store only immutable lists.
Next session: REDO Library under the refactor plan (immutable entities, snapshot getters, one
  failure convention with domain exceptions, JUnit from the 10 probes, then carve
  catalog/stock/loan behind repository seams) targeting G3, G4, G12, G16, B14, B16–B18.
  Then a full, no-shortcut Phase 1–8 on a fresh problem — dims 1 and 9 still have no live
  data point after 4 sessions.
```

```
SESSION 5 · Library Management System — REDO after the S4 review · 2026-09-21
Target gaps: G3, G4, G12, G13 exercised (assigned S3/S4); G15, G16 assigned again — not addressed.
Score: 60/100 (raw 63, CAPPED at 60 — confirmed data race in a shared-mutable design). Prev S4: 43.
  Dim 5 was ungradeable in S4 (no pattern present) and is 7 now; like-for-like on the original
  8 dims it is 50/80 = 63 raw either way, so the delta is real, not a denominator effect.
  → Verdict: Lean Hire (at the threshold; the cap, not design quality, is what pins it at 60).
Won:  Steps 1–3 of the S4 refactor plan landed the same day and verified by probe: 0 CMEs (was
        16,010 / 318,373 in 1.5s), zero setters and final entities (Book fully immutable via
        List.copyOf), 4 distinct domain exceptions + an id-based borrow/return API. Fastest
        review→fix turn so far.
      First-ever Repository: 3 interfaces + in-memory impls, constructor-injected; manager
        270 → 168 lines, orchestration only. The seam paid off immediately — a 5-line failing
        BookingRepository fake reproduced a partial-failure bug with no change to LibraryManager.
Lost: Decomposition dissolved the atomic section. LibraryManager.returnBook does
        booking.isReturned() → inventoryRepository.increment() → booking.markReturned() across
        independent locks, with an unsynchronized check-then-act on Booking.returnDate:
        concurrent double return inflated stock to a phantom 2nd copy in 39/30,000 rounds
        (22 both-calls-succeeded, 17 leaked a raw IllegalStateException). borrowBook does
        decrement → save with no compensation: a failing save leaks a copy permanently.
        The S4 single lock was silently protecting both.
      G16 untouched again: Main grew 460 → 694 lines of println, no JUnit, no assertions, no
        concurrent path — both concurrency bugs above were found by my probes, not by his own
        verification. Also: InMemoryBookRepository.save still non-idempotent (index dups + stale
        entity), getBook/getBooking still return null, UserNotFoundException.java is a 0-byte
        file, LocalDateTime.now() now also inside Booking.markReturned.
TRANSFERABLE RULE: Splitting a class splits its lock's guarantees too — before decomposing, list
  every check-then-act the old lock made atomic, then make each one either a single atomic call
  on one collaborator (the loser of a race must learn it lost, and only the winner proceeds) or
  an explicitly locked sequence in the orchestrator, with compensation for any step that can
  fail after an earlier one committed.
RECALL CARD → Q: Two threads call returnBook(sameLoanId). The manager does isReturned() check →
  stock.increment() → loan.markReturned(). What goes wrong, and what is the smallest fix?
  A: Both pass the check, both increment → phantom copy; check-then-act on returnDate isn't
  atomic. Make the state transition itself atomic (CAS / synchronized, returning whether THIS
  caller won) and increment stock only if you won.
Next session: Close B19/B20 (atomic transition + compensation) and write the JUnit for G16 from
  the behaviour list — then a fresh, full Phase 1–8 problem; dims 1 and 9 still have no live data
  point after 5 sessions.
```

```
SESSION 6 · Library Management System — fix round 2 (same-day rescore) · 2026-09-21
Target gaps: B19, B20, B21 (from the S5 review) fixed; G16 assigned again — not addressed.
Score: 60/100 (raw 66, CAPPED at 60 — an observable data race remains). Prev S5: 60 (raw 63); S4: 43.
  Dims: 2:7 3:7 4:6 5:7 6:7 7:7 8:7 9:6 10:5 = 59/90 (8-dim like-for-like: 52/80 = 65).
  → Verdict: Lean Hire. The cap now costs 6 points and is one keyword (`volatile`) away.
Won:  All three S5 findings fixed and verified by probe: Booking.markReturned() is now an atomic
        compare-and-set returning whether THIS caller won, and only the winner restores stock
        (0 phantom copies in 30,000 double-return rounds, was 39); borrowBook compensates and
        rethrows the original exception (16-thread churn with 12,292 injected save failures
        still ended at exactly the right stock); InMemoryBookRepository.save de-indexes the
        old entity (1 index hit, no stale entity). Feedback-to-fix loop is fast and precise.
      Compensation scoped correctly: the try starts AFTER the decrement. The older draft in
        the editor wrapped the whole method (did not compile; patched to compile it handed
        back a copy on every failed borrow and let the next caller borrow a nonexistent copy)
        — the saved version avoids it.
Lost: Same happens-before gap as S3, third shape: markReturned() writes returnDate under
        synchronized(this) but isReturned()/getReturnDate()/toString() read it unsynchronized
        and the field is not volatile — a spin-reader never saw the write in 4s (with
        volatile it did). Writer synchronized, reader forgotten.
      Return-side partial failure is the mirror of the borrow fix and was not applied:
        returnBook commits markReturned() then calls increment(); if increment throws, the
        loan is closed, stock isn't restored, and a retry gets AlreadyReturnedException — the
        copy is unrecoverable (verified with a flaky inventory fake). G16 still untouched:
        three concurrency bugs fixed across S5–S6 with no test proving any of them; Main is
        still 694 lines of println.
TRANSFERABLE RULE: Synchronizing the write is half the contract — every read of the same shared
  field needs the same lock (or the field must be volatile), otherwise the reader has no
  happens-before edge and may never see the update; and whenever you compensate one side of a
  two-step operation, mechanically check its mirror operation for the same failure window.
RECALL CARD → Q: Booking.markReturned() writes returnDate inside synchronized(this); isReturned()
  reads it with no lock and no volatile. What can a polling reader observe, and what is the
  smallest fix?
  A: It may never see the write — no happens-before edge, so the JIT can hoist the read out of
  the loop. Make the field volatile (or synchronize the readers too).
Next session: volatile + return-side compensation (or one per-loan critical section around
  check→increment→set), then JUnit for G16 — double-return race, failing-save compensation,
  failing-increment compensation, spin-reader visibility — then a fresh full Phase 1–8 problem;
  dims 1 and 9 have no live data after 6 sessions.
```

```
SESSION 7 · Subscription Billing (ONCE model) — LEVEL-1 lens · 2026-09-21
Target gaps: G15/G16/G8/G11 were assigned; Sahil re-scoped the programme mid-session (too advanced, overwhelming),
  so this session is scored on a new Level-1 "Decent LLD" bar only. Phases 0–4 ran on the old bar (recall 3/10,
  dim 1 = 4, paper design 29 → 30) and are NOT comparable with this score.
Score: 60/100 on the Level-1 lens (36/60: requirements covered 5, domain 7, responsibilities 6, data structures 8,
  API & errors 4, code quality 6). Pass mark for a level = 70 with every criterion ≥ 5. Not graded at Level 1:
  seams/abstractions, patterns, concurrency, Clock/tests, extensibility. → Not yet decent — three fixes from passing.
Won:  Kept the Plan/Subscription split and separated current vs history maps — the history question from the
        gauntlet was answered correctly in code; cancel closes the subscription and keeps its history (B3).
      Applied "don't leak internals" unprompted in showAllPlans() (returns a copy), returned a boolean instead of
        UI text from canConsumeService, BigDecimal price, enums for Period and State.
Lost: Subscription points at the live mutable Plan and records nothing about what was agreed: demoDays is never
        read (no trial), no amount owed, and editing the plan rewrites existing subscriptions (price 999 / WEEKLY
        reported against a one-month expiry).
      No validation: newSubscription accepts never-added and removed plans (Main's own "expected failure" step prints
        a success), getHistory leaks the live queue, and `state` can disagree with the clock (expired yesterday
        still Active; re-subscribing records it as Cancelled).
TRANSFERABLE RULE: A contract records what was agreed at the moment of agreement — copy the terms (price, period,
  trial end, due date) into the subscription instead of pointing at a catalogue entry that can change later.
RECALL CARD → Q: A customer subscribes at ₹299; marketing then changes the plan's price to ₹999. What should the
  customer's subscription say, and how do you guarantee it?
  A: ₹299 — copy the agreed terms into the subscription at subscribe time; never let a contract read its terms
  from a mutable catalogue entry.
Next session: Close Level 1 (3 fixes, then re-score), then Level 2 (clean structure & abstractions) — pending
  Sahil's approval of the level roadmap.
```

```
SESSION 8 · Subscription Billing (ONCE model) — Level-1 RE-SCORE · 2026-09-21
Target: close Level 1 (the three fixes from S7). He also started Level-2 material early (factories, split classes).
Score: 52/100 on the Level-1 lens (31/60: requirements covered 4, domain 7, responsibilities 5, data structures 8,
  API & errors 3, code quality 4). Was 60 in S7. Pass mark 70 with every criterion ≥ 5 → not yet.
  Dropped because four flows that worked in S7 broke while adding structure. Tests not graded (Level 4).
Won:  Applied the S7 rule: Subscription now copies the Plan at subscribe time — a catalogue price edit no longer
        rewrites an existing subscription (verified: still 299.00 / MONTHLY after the plan was set to 999 / WEEKLY).
      History Queue → List, Optional return types, and a scope comment on the "same plan again" decision — good
        instincts, right direction.
Lost: Regressions: changing plan no longer cancels the old subscription (history: Weekly:Active, Monthly:Active);
        cancel/expiry append the subscription to history a second time (same object twice); an expired subscription
        makes canConsumeService throw NullPointerException (getCurrentSubscription returns null from an Optional
        method); "same plan" compares the period, not the plan (Weekly Basic → Weekly Premium is silently ignored)
        and prints from the domain (System.out in SubscriptionManager.newSubscription — G5 relapse).
      Early abstraction without variation: MonthlySubscriptionFactory calls WeeklySubscription (copy-paste), so
        MonthlySubscription is dead code; both classes compute expiry with the same `switch`; three factories + two
        classes do a one-line job. Still open from S7: no validation (unknown/removed plan, null user), getHistory
        leaks the live list, demoDays never read (no trial), no amount/due date recorded.
TRANSFERABLE RULE: A refactor must leave every flow that worked still working — after each structural change, run
  the demo and read its output like a checklist (his own output showed the old plan still Active and the new one
  listed twice).
RECALL CARD → Q: What is the first thing you do after any structural change (splitting a class, adding a factory)?
  A: Re-run every flow that worked before and compare — a refactor must not change behaviour.
Next session: Bring the flows back and close the remaining Level-1 items (validation, trial/owed); park the factories
  for Level 2, where the lesson is that weekly vs monthly differs only in DATA (how long a period lasts).
```

| # | Date | Problem | Target gaps | Score | Verdict |
|---|------|---------|-------------|-------|---------|
| 1 | 2026-09-09 – 2026-09-10 | Thread-safe KV Store w/ TTL | G1, G15, G16, G12 | 50/100 (partial) | No Hire — assisted |
| 2 | 2026-09-10 | Concurrent Seat/Inventory Reservation w/ lock expiry | G1, G10 | 54/100 (partial) | No Hire on aggregate — but G1 target: Hire |
| 3 | 2026-09-18 | In-Process Pub/Sub Event Bus (MPSC ring buffer + Observer) | G1 (retest), G9 | 68/100 (partial, informal session) | Lean Hire |
| 4 | 2026-09-21 | Library Management System (self-built, reviewed) | G1 (transfer), G5, G13, G15 exercised; G3/G4/G12/G16 assigned, not addressed | 43/100 (partial) | No Hire |
| 5 | 2026-09-21 | Library Management System REDO (post-review) | G3, G4, G12, G13 exercised; G15, G16 not addressed | 60/100 (raw 63, capped — data race) | Lean Hire (at threshold) |
| 6 | 2026-09-21 | Library Management System — fix round 2 (rescore) | B19, B20, B21 fixed; G16 not addressed | 60/100 (raw 66, capped — data race) | Lean Hire |
| 7 | 2026-09-21 | Subscription Billing (ONCE model) — Level-1 lens | Level-1 bar (new); G15/G16 deferred | 60/100 (Level-1 lens, not comparable) | Not yet decent |
| 8 | 2026-09-21 | Subscription Billing (ONCE model) — Level-1 re-score | Close Level 1; 4 regressions found | 52/100 (Level-1 lens) | Not yet decent |

---

## RUBRIC SCORES OVER TIME

| Dimension | S1 | S2 | S3 | S4 | S5 | S6 | S7 | S8 | S9 | S10 | S11 | S12 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 Requirements & scoping | — | — | — | — | — | — | 5 | 4 | | | | |
| 2 Domain modeling | 7 | 8 | 7 | 5 | 7 | 7 | 7 | 7 | | | | |
| 3 Abstraction & seams | 5 | 4 | 6 | 3 | 7 | 7 | — | — | | | | |
| 4 SOLID | 5 | 5 | 6 | 4 | 6 | 6 | 6 | 5 | | | | |
| 5 Pattern selection | — | — | 8 | — | 7 | 7 | — | — | | | | |
| 6 Concurrency & correctness | 3 | 8 | 6 | 5 | 5 | 7 | — | — | | | | |
| 7 Data structures & complexity | 8 | 6 | 8 | 6 | 7 | 7 | 8 | 8 | | | | |
| 8 API & error design | 4 | 4 | 6 | 4 | 7 | 7 | 4 | 3 | | | | |
| 9 Extensibility (proven) | — | — | — | 3 | 6 | 6 | — | — | | | | |
| 10 Testability & hygiene | 3 | 3 | 7 | 4 | 5 | 5 | 6 | 4 | | | | |

_Flat-dimension watch (§10): dim 6 went 3, 8, 6, 5, 5, 7 — S6 fixes verified under fault injection, but the `returnDate` visibility race is S3's happens-before gap recurring (third shape). Dim 10 is flat at ≤5 for three rounds (3, 3, 7, 4, 5, 5): G16 has been assigned in S3, S4, S5 and S6 and never attempted. Dim 1 has zero data points after 6 sessions; dim 9 is paper-only (3 → 6), never attacked live. S7 column = the Level-1 lens (covered→1, domain→2, responsibilities→4, data structures→7, API/errors→8, quality→10; the rest not graded) — not comparable with S1–S6._

---

## GAP LEDGER

`open` → `improving` → `closed` (closed = solved correctly in a **different** problem than where it was taught)

### P0
| ID | Gap | Status |
|---|---|---|
| G1 | Zero concurrency thinking (TOCTOU in `ShowSeat.lock()`) | closed (S2: `HotelInventory` — atomic check-and-commit under one lock, verified correct under a real 5-thread race, no unlock used) |
| G2 | No thread of execution / no simulation loop | closed (S2: `runHoldExpirationWorker` is a real, self-built daemon thread — releases the lock before sleeping, verified sweeping hold-3 within its TTL window) |
| G3 | God controllers, no layering vocabulary | improving (S5: `LibraryManager` cut 270 → 168 lines to orchestration only, storage behind 3 repositories, new `exceptions/` + `repository/` packages. Still open: the manager lives in `entities`, no `service` package. S4: was 6 maps + search + stock + lending in one class) |
| G4 | No repository abstraction | improving (S5: `BookRepository`/`InventoryRepository`/`BookingRepository` + in-memory impls, constructor-injected — first Repository use. Caveat: `InventoryRepository` carries a business invariant (non-negative stock, throws `NoCopiesAvailableException`) and splits `isAvailable`/`decrement`. Closes when repeated unprompted in a different problem) |
| G5 | Presentation fused into domain (`System.out` in entities) | improving (S4 + S5: 0 `System.out` in the domain in both builds. S3 relapsed in `Topic.handleSubscriberFailure`, so it closes only when it holds in a different problem) — S8: relapsed in `SubscriptionManager.newSubscription` (`System.out.println("Already have the same plan")`) |
| G6 | Inheritance for reuse, not IS-A (LSP violations) | open |

### P1
| ID | Gap | Status |
|---|---|---|
| G7 | Only 4 patterns ever used | open |
| G8 | Business policy hardcoded in logic | open |
| G9 | No events / no Observer | closed* (S3: `Topic`/`Subscriber`/`Publisher` — real fan-out, correct, not over-engineered. *First-ever use, not a taught-then-transferred case like G1/G2/G10 — no prior session to transfer from, so this is "gap no longer true" rather than proven transfer. Re-verify it shows up unprompted in a later problem before treating the *pattern-selection* instinct itself as trusted.) — **S7 Phase 4: not reached for unprompted** (audit + email proposed as two separate mechanisms); pattern-selection instinct still unproven |
| G10 | No lock expiry / TTL / reaper | closed (S2: hold expiry + background sweep self-built in `HotelInventory`, no unlock — closes the loop from the *original* Movie Ticket Booking gap where a `SEATS_LOCKED` booking never expired) |
| G11 | Idempotency modeled but never used | open |
| G12 | No custom domain exceptions | improving (S5: 4 typed exceptions — `BookNotFound`, `NoCopiesAvailable`, `LoanNotFound`, `AlreadyReturned` — thrown from borrow/return. Still open: `getBook`/`getBooking` return null, `Booking.markReturned` leaks a raw `IllegalStateException`, no common base type, `UserNotFoundException.java` is a 0-byte file, user never validated) — S7: no validation or typed failures again in Subscription Billing (null returns, unknown/removed plan accepted); now a Level-1 item. |
| G13 | Encapsulation leaks (live collections returned) | improving (S5: all 3 repository finders return `List.copyOf` snapshots — 0 CME in 1.5s vs 16,010 in S4; `Book`/`User` defensively copy. Residual: snapshots still hold the shared mutable `Booking` objects) |
| G14 | Telescoping constructors, no Builder | open |
| G15 | No `Clock` abstraction | open (S4/S5: `LocalDateTime.now()` x2 — S5 moved one *inside* `Booking.markReturned`, so the entity now reaches for the clock; `UUID.randomUUID()` also inline) — S7: `LocalDateTime.now()` inside `MonthlySubscription`; deferred by the level roadmap to Level 4 (parked, not dropped). |
| G16 | Zero tests | open (S4 regressed vs S3; S5/S6 no change — `Main` is 694 lines of println, no JUnit, no assertions, no concurrent path. Assigned in S3, S4, S5 and S6; three concurrency bugs were fixed in S5–S6 with no test proving any of them) — S7: deferred by the level roadmap to Level 4 (parked, not dropped). |

### P2 — correctness bugs
| ID | Bug | Status |
|---|---|---|
| B1 | Double-booking hole (Rental): check and commit not atomic | closed (S2: `HotelInventory.holdRoom` wraps find-available + create-hold in one `lock`, verified by the 5-thread race) |
| B2 | `TreeSet` comparator defines equality (Rental) | closed (S2: `holdsByRoomId`/`bookingsByRoomId` comparators end in `.thenComparing(...Id)`, a real tie-breaker) |
| B3 | Return ≠ cancel (Rental) | closed (S4: `returnBook` closed the loan via `returnDate` unprompted, in a different problem; S5: no longer evicts it — `getBooking(id)` returns the returned loan, verified) |
| B4 | Entity as `HashMap` key without equals/hashCode (Airline) | improving (S4: all six maps keyed by String id/tag/author, no entity keys — one data point where the trap was avoidable) |
| B5 | `TreeMap` used as a `HashMap` (Airline) | open |
| B6 | Control flow via side-effect inspection (Airline) | open |
| B7 | Hall-call direction discarded; no SCAN/LOOK (Elevator) | open |
| B8 | Idle elevators penalized by scoring (Elevator) | open |
| B9 | No overlap check where he already knew how (Movie) — **meta-gap: transfer failure** | open |
| B10 | `Show` has no id / no back-reference; linear scans | closed (S4: `bookByTags`/`bookByAuthor` inverted indexes built unprompted; S5: `returnBook(bookingId)` is an O(1) id lookup instead of a per-user scan) |
| B11 | Name collisions & package naming | open |
| B12 | `RentalSystem` is an empty class | open |

### Sensei-added findings (verified 2026-09-09, not in original audit)
| ID | Bug | Status |
|---|---|---|
| B13 | `BookingController.confirmBooking` takes payment, *then* calls `seat.book()`. A double-click on Pay charges twice — the second call's guard throws only **after** the money moved. Idempotency must gate **before** the side effect. | open |
| B14 | `BookingController.addTheater` does `theaterTreeMap.put(location, theater)` — two theaters at the same `Location` silently overwrite. A `Map` keyed by a non-unique attribute is a data-loss bug. | improving (recurred S4 in `addBook`; S6: `InMemoryBookRepository.save` is now a correct upsert — old entity de-indexed, verified. Open design question: a collision between two *different* books with one id silently replaces the metadata while stock adds) |
| B15 | `BookingController.createShow` constructs `new Show(movie, showSeats, time)` with an **empty** list, then fills that same list afterwards. `Show` aliases a collection the controller still mutates. | open |
| B16 | Lock released on return: the finders returned the live `ArrayList` found inside the read lock; the caller iterates it unlocked while writers append → CME / data race (S4 verified: 16,010 CME in 1.5s). | improving (S5: `List.copyOf` in all three finders; 0 CME under the same stress. Same-problem fix — closes when unprompted elsewhere) |
| B17 | Caller-supplied entities trusted: `borrowBook(User, Book)` stored the *caller's* `Book` in the `Booking` and never validated the `User` — a forged `Book("B1", "OTHER TITLE", …)` borrowed the real B1's copy (S4 verified). | improving (S5: `borrowBook` takes a bookId and stores the catalogued `Book` — forgery closed. Still open: the `User` is never validated — `new User(null, …)` borrows successfully) |
| B18 | Entities behind an index are mutable: `Book.setTags/setId`, `Booking.setId` desynced `bookByTags`/`bookMap`/`bookingMap` (S4 verified: after `setId("B9")`, `getBook("B1").getId()` returned "B9"). | improving (S5 verified by reflection: zero setters on Book/Booking/User; `Book` fully final with `List.copyOf(tags)`; only `Booking.returnDate` and `User.debt` are mutable) |
| B19 | Decomposition dissolved atomicity: `LibraryManager.returnBook` did `booking.isReturned()` → `inventoryRepository.increment()` → `booking.markReturned()` with no synchronization (S5 verified: 39/30,000 double-return rounds inflated stock to 2; 22 both succeeded; 17 leaked a raw `IllegalStateException`). | improving (S6: `Booking.markReturned()` is a synchronized compare-and-set returning whether THIS caller won; only the winner increments — 0/30,000 phantom copies, 0 raw ISE. Same-problem fix; closes when unprompted elsewhere) |
| B20 | No compensation on partial failure: `LibraryManager.borrowBook` did `decrement` then `save`; a failing `save` leaked the copy (S5 verified with an injected failing `BookingRepository`). | improving (S6: try scoped after the decrement, compensates with `increment`, rethrows the original — verified single failure and a 16-thread churn with 12,292 injected save failures ending at exactly the right stock. Residual: if the compensating `increment` also throws, the original cause is lost — no `addSuppressed`; the return-side mirror is B23) |
| B21 | Non-idempotent index writes: `InMemoryBookRepository.save` appended to `bookByAuthor`/`bookByTags` on every call — re-saving an id duplicated hits and left the old `Book` indexed under stale tags/author (S5 verified). | improving (S6 verified: re-save leaves exactly 1 index hit and no stale entity; residual NITs: author lists never pruned when empty, `Book` has no `equals`/`hashCode` so `List.remove` relies on identity) |
| B22 | No happens-before for readers of a synchronized write: `Booking.markReturned()` writes `returnDate` under `synchronized(this)`, but `isReturned()`/`getReturnDate()`/`toString()` read it unsynchronized and the field is not `volatile` (verified 2026-09-21: a spin-reader on `isReturned()` never saw the write in 4s; with `volatile` it did). Same lesson as S3's recall card 3 (HB edge), third shape. **Triggers the §6 cap.** | open |
| B23 | Return-side partial failure (mirror of B20): `LibraryManager.returnBook` commits `markReturned()` and only then calls `inventoryRepository.increment`; if `increment` throws, the loan is closed and stock is never restored — a retry gets `AlreadyReturnedException`, so the copy is unrecoverable through the API (verified 2026-09-21 with a flaky inventory fake: loan returned, stock 0). | open |

---

## PATTERN LEDGER

| Pattern | Used correctly | Misused | Never used |
|---|---|---|---|
| Strategy | ✓ (Elevator dispatch) | | |
| Factory | ✓ (PaymentMethodFactory) | | |
| Static Factory Method | ✓ (PaymentResult) | | |
| Adapter | ✓ thin (PaymentGateway) | | |
| Observer | ✓ (S3: Topic/Subscriber fan-out, self-driving publish-triggers-broadcast) | | |
| Decorator | | | ✗ |
| Command | | | ✗ |
| State (as objects) | | | ✗ |
| Builder | | | ✗ |
| Chain of Responsibility | | | ✗ |
| Composite | | | ✗ |
| Template Method | | | ✗ |
| Proxy | | | ✗ |
| Mediator | | | ✗ |
| Memento | | | ✗ |
| Flyweight | | | ✗ |
| Visitor | | | ✗ |
| Iterator | | | ✗ |
| Repository | ✓ (S5: Book/Booking; `InventoryRepository` is borderline — it carries a business invariant) | | |
| Specification | | | ✗ |
| Null Object | | | ✗ |
| Registry | | | ✗ |
| Prototype | | | ✗ |

---

## RECALL CARDS

_(one added per session; ★ = failed on re-test at least once)_

| # | Q | A | Failed re-tests |
|---|---|---|---|
| 1 | Why can't you call `writeLock().lock()` while already holding `readLock()` on the same `ReentrantReadWriteLock`? | Not a supported upgrade — the write lock requires no readers hold the lock, so the holding thread blocks indefinitely. Release the read lock fully first, then re-check state from scratch since it wasn't held continuously. | |
| 2 | What specifically moves a gap from `open`/`improving` to `closed` in this ledger? | Solved correctly, unprompted, in a DIFFERENT problem than the one where it was taught or unlocked — not just "worked once where I showed you." | |
| 3 | Why is "CAS the shared producer cursor forward, then write the payload" unsafe even with a single producer and zero preemption? | The JMM gives no happens-before edge from a plain write that follows a volatile/atomic write in program order to a later read by another thread. The payload write must happen-before the publish signal, never after it. | ★ S7 Phase 0 (2026-09-21): answered "not sure"; the same rule also failed in application in S6 (B22) |
| 4 | A getter takes the read lock, does `map.get(key)` and returns the `List` it found. Is the caller's `for`-loop over it safe against concurrent writers? | No — the lock releases on return, so the caller iterates the live list unlocked while a writer mutates it → CME / data race. Return a snapshot (`List.copyOf`) from inside the lock, or store only immutable lists. | |
| 5 | Two threads call `returnBook(sameLoanId)`. The manager does `isReturned()` check → `stock.increment()` → `loan.markReturned()`. What goes wrong, and what's the smallest fix? | Both pass the check and both increment → a phantom copy; the check-then-act on `returnDate` isn't atomic. Make the state transition itself atomic (CAS / `synchronized`, returning whether THIS caller won) and increment stock only if you won. | |
| 6 | `Booking.markReturned()` writes `returnDate` inside `synchronized(this)`; `isReturned()` reads it with no lock and no `volatile`. What can a polling reader observe, and what's the smallest fix? | It may never see the write — no happens-before edge, so the JIT can hoist the read out of the loop. Make the field `volatile` (or synchronize the readers too). | partial, S7 Phase 0: named volatile/synchronized (said "volatile isReturn" — it is the field, and synchronized only works if EVERY reader takes the same monitor); never said what the reader observes (may never see the write) |
| 7 | A customer subscribes at ₹299; marketing then changes the plan's price to ₹999. What should the customer's subscription say, and how do you guarantee it? | ₹299 — copy the agreed terms into the subscription at subscribe time; never let a contract read its terms from a mutable catalogue entry. | |
