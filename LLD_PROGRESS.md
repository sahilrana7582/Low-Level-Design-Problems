# LLD PROGRESS LOG

> Read at the start of every session. Append the Phase-8 card at the end of every session.
> Profile & curriculum: `LLD_TUTOR_PROMPT.md`

---

## PARKED

- **Session 14 · Expense Sharing (OPENED 2026-09-25, Level-1 lens).** Sahil asked for a new problem right after the S13 score
  (Meeting Room stays parked at 67, ~20-min finish). Level 1 is now unpassed on FOUR problems (52, 57/67, 67 — pass = 70).
  Recall cards 12 and 9 retired from the "owed" list (12 shown in practice by the expected-vs-actual demo; 9's idea is card 13).
  Phase 0 this session = card 13 (last session) + card 7 (S7, never re-tested). Chosen because the core lesson is "store
  facts, derive views" (balances derived from expenses) — the exact temptation behind B28/B29 — and it needs money + rounding.
  Phase 1 problem given vague; his Phase 2 must contain questions AND a scope statement (he skipped the statement in S13).
  **RULE (2026-09-25, from Sahil): the backlog is shown ONLY when he asks for it — never raise it, never gate a grade on it.**
  Recall answers for cards 13 and 7 were skipped again (4th time) and are now silent backlog.
  Phase 2 (2026-09-25): dim 1 = **5/10 provisional** (flat vs S13). Good: scope statement present this time; partial-settle
  question; per-expense participants question. Weak: "private expense" + "Personal Expenses" as a group (scope creep); asked
  about settle-up but left it out of his own scope; scope #3/#4 ambiguous; no Out list. Missed: WHO PAID, rounding (100/3),
  edit/delete an expense, pairwise netting. **Locked — In:** users (id, name) pre-registered; many groups, a user in several,
  balances per group only; creator + initial members, any member can add a member later, no leaving, a newcomer shares only
  expenses recorded after joining; one payer per expense (a member); participants chosen per expense from current members
  (payer may or may not be included; at least one participant other than the payer); equal split, rupees to 2 decimals,
  amount > 0, shares sum exactly to the amount, leftover paise go one each to participants in the order listed; settle = one
  member pays another in the same group, any amount > 0 up to what they currently owe; views: my groups, my net owe/owed in a
  group, per-member NETTED breakdown (zero omitted). **Out:** solo expenses, exact/percent splits, edit/delete expense, debt
  simplification, cross-group netting, currencies, expense-history listing, auth, removing members. Every rejection distinguishable.
  Phase 3 paper questions (Q1–Q5) were left unanswered; he built V1 directly. **V1 scored 53/100 Level-1 (S14 card).**

- **Session 13 · Meeting Room Booking (IN PROGRESS, opened 2026-09-24, Level-1 lens).** Phase 0 recall questions were not in
  his paste — asked after Phase 2 (cards 12 and 9). Phase 2: his 5 questions graded → dim 1 = **5/10 provisional**
  (good: per-employee overlap rule, history by room + employee, own-bookings query; noise: floors; missed: same-room overlap +
  back-to-back boundary, cancel, how a room is chosen / availability query, time shape; stated NO scope at all).
  **Locked scope — In:** rooms fixed at setup (id, floor label, capacity); book a specific room for start/end + attendee count
  (≤ capacity), same calendar day, start < end; no overlap per room; no overlap per employee; end exclusive (back-to-back OK);
  owner-only cancel that frees the slot and keeps the record; queries: employee's active bookings by start time, room history,
  employee history (staff; includes cancelled), free rooms for a slot with capacity ≥ N. **Out:** notifications, calendar
  sync, recurring, approvals, equipment, auth, room removal, edit/reschedule, office hours, time zones. **Parked by level:**
  "now" / past-booking rejection / ongoing state → L4 (Clock); simultaneous double-booking → L5; system auto-picks best room →
  L2. Note: `problems/MeetingRoomSchedular` (399 lines, committed 2026-09-21) already exists — asked if this is a redo; do NOT
  open it before his Phase 3.
  **Phase 3 (paper, 2026-09-24): 52/100 provisional** (26/50 on the 5 paper-gradable criteria: requirements 5, domain 6,
  responsibilities 5, data structures 6, API & errors 4; code quality waits for code). He skipped both recall answers twice and
  asked for "no more questions" after one Socratic round — so no gauntlet; Sensei issued rulings + a Phase-5 acceptance list
  instead. Findings: F1 each booking stored in 3 copies + state kept twice (field AND which list) — his defence "for future
  extension" is speculative (B28); F2 scope creep ×3: `removeBooking` (contradicts "nothing deleted"), COMPLETED/`completeBooking`
  (no clock → completing a future booking frees its slot), the `EmployeeService` copy; F3 API: employee conflict reported as
  RoomAlreadyBooked, cancel takes a caller-built Booking + state, `newBooking` trusts a caller-built Booking, raw
  InvalidArgumentException; F4 free-rooms query + headcount missing; F5 `List<Employee>`/`List<Room>` (same List-vs-Map slip as
  S10 B26, fixed in S11). Wins: overlap boundary correct (11–12 only), TreeSet O(log n), compensating-rollback instinct,
  `BookingService(roomService, employeeService)` constructor injection.
  **Build scored (S13 card): 67/100 Level-1, pass 70.** He kept F1/F2's targets on purpose (mirror copy, completeBooking) — cost graded.
  ~20-minute finish to cross 70: drop COMPLETED/completeBooking/removeBooking, delete or seal the EmployeeService mirror,
  cancel by (employeeId, bookingId). Recall answers (cards 12 and 9) still owed — third time skipped.
- **Student Management System (parked 2026-09-24 after S12, at 57/100 Level-1 lens — best 67 in S11).** Sahil asked for
  another new problem. Level 1 is now unpassed on THREE problems in a row (Subscription 52, SMS 57, best 67; pass = 70).
  Owed to cross 70, all small and concrete: make `Grade` immutable again (build a new one at `completeCourse`, no
  setter, no aliasing across snapshots), wire `totalMarksObtained` through (his own demo prints 0 after passing 85),
  decide whether a Grade belongs on a PROGRESSING enrollment at all, and the two raw `IllegalArgumentException`
  spots. ~20-minute finish whenever he wants it. Recurring root cause across S10–S12: the breaking evidence was already
  in his own console output (card 12).
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

```
SESSION 9 · Subscription Billing — Level-1 REFERENCE solution (written by Sensei at his request) · 2026-09-21
Score: NOT SCORED. Sahil was exhausted and asked for the fixed, best version to study and take notes from. His own
  attempt stays at 52/100 (S8) and is untouched in problems/SubscriptionBilling (a redundant copy of it sits in
  problems/SubscriptionBilling/_my-attempt-before-fix — Sensei-made, safe to delete). Overwriting/deleting his files was
  blocked by the auto-mode classifier, so the reference lives in problems/SubscriptionBillingReference. No ledger gap is
  credited: he has to reproduce it himself.
What it is: the best LEVEL-1 (ONCE model) solution — deliberately no factories, no interfaces, no concurrency, no recurring,
  no JUnit. Verified: a self-checking Main (8 sections, 28 checks, movable Clock) and 21 extra edge probes all pass.
Rules it demonstrates (his notes checklist): immutable Plan record (snapshot for free) · store facts, derive state from the
  clock · one structure per fact (single history list, current = last live entry) · take ids across the boundary and resolve
  inside · one failure convention (Optional for "maybe", domain exceptions for failures, no printing in the service) ·
  variation as data (BillingPeriod enum carries the period length) · inject the Clock · return copies · a demo that verifies.
TRANSFERABLE RULE: Store facts, derive states — keep only what happened (started, cancelled) and compute trial/active/expired
  from the clock, so two sources of truth can never disagree.
RECALL CARD → Q: Why derive "expired" from the clock instead of keeping a state flag on the subscription?
  A: A stored flag only changes when someone touches the object, so it can say ACTIVE while the clock says expired. Store the
  timestamps and compute the state from "now" — one source of truth.
Next session: Closed-book rebuild — he rebuilds the ONCE model from his own notes (30–40 min), scored on the Level-1 bar
  (pass = 70, every criterion ≥ 5). Then Level 2 (where abstraction earns its place). Library end-of-day session still parked.
```

```
SESSION 10 · Student Management System (new problem, Level 1) · 2026-09-22
Sahil chose to move to a new problem instead of the planned Subscription Billing closed-book rebuild — that rebuild
  and the Library end-of-day session both stay parked. Ran the full Level-1 loop: Phase 0 recall, Phase 2 scoping
  (graded his 4 questions, caught 2 misses — catalog-vs-instance for "course", verbs before nouns), Phase 3 design
  (multi-round Socratic — TreeSet→Map self-correction, registration-scope correction, Grade sentinel flagged,
  completedAt NPE trap caught before code), Phase 5 he implemented solo, Phase 6 scored.
Score: 63/100 on the Level-1 lens (38/60: requirements 5, domain 8, responsibilities 8, data structures 5,
  API & errors 7, code quality 5). Pass mark 70 with every criterion ≥ 5 → not yet, but first Level-1 attempt to
  clear the ≥5 floor on every criterion (S7 had API&errors=4, S8 had API&errors=3 and code quality=4). Best
  Level-1 score to date.
Won:  Enrollment modeled as an immutable, append-only snapshot per status transition — genuine unprompted transfer
        of last session's "store facts" lesson. getStudent/getCourse throw typed exceptions instead of returning
        null — directly fixes the exact NPE-class bug from S8 (Subscription's canConsumeService). Duplicate
        enrollment now throws instead of returning a UI string — resolved the open convention question correctly
        in code. CourseService correctly moved TreeSet → Map<id,Course> after Phase-3 pushback. Defensive copies
        on every list leaving a service (G13 held); zero System.out in the domain (G5 held, third clean data point).
Lost: Standing/grades — the feature he scoped himself — is unreachable: no path ever creates a COMPLETED
        enrollment or a real Grade; Grade.getPercentage() is dead code. Re-enrollment after a drop is silently
        allowed (hasActiveEnrollment only checks the latest snapshot's status == PROGRESSING), directly
        contradicting his own Phase-3 decision to block it. StudentService — built minutes after fixing Course's
        List→Map problem — still uses List + O(n) scan with no duplicate-id guard: a same-session, same-problem
        transfer failure, sharper than his usual cross-problem one. The promised two-map rollback ("ACID, whole
        or not at all") was never written — invisible today only because ArrayList.add can't fail. Main is still
        println-only; none of his 5 exception types are ever exercised in the demo, not even the capacity
        boundary he set up himself (maxSeats=2, exactly 2 enrolled, no 3rd attempt).
TRANSFERABLE RULE: A lesson learned for one class isn't applied until you check every sibling class doing the same
  job — fixing Course's lookup and leaving Student's the same mistake, minutes later, is a checklist failure, not
  a knowledge gap.
RECALL CARD → Q: You fixed Course's lookup from a TreeSet to a Map<id, Course> earlier in the session. StudentService,
  built minutes later, still uses a List + linear scan with no duplicate-id guard. What should you have done, and when?
  A: The moment a data-structure lesson lands for one entity, check every other service doing the same id-keyed job —
  it's a general rule, not a Course-specific one. Do it before writing the sibling class, not after review finds it.
Next session: fix the 3 MAJORs (wire up a real completeCourse/Grade path, decide + enforce re-enrollment-after-drop
  on purpose, fix StudentService's data structure + duplicate-id guard), re-score against the same Level-1 bar.
  Subscription Billing closed-book rebuild and Library end-of-day session remain parked.
```

```
SESSION 11 · Student Management System — Level-1 re-score · 2026-09-22
Target: close the 3 S10 MAJORs.
Score: 67/100 on the Level-1 lens (40/60: requirements 6, domain 8, responsibilities 6, data structures 9,
  API & errors 6, code quality 5). Was 63 in S10. Pass mark 70 → not yet, but closing in.
Won:  StudentService fully fixed — Map<id,Student>, O(1), duplicate-id guard (DuplicateStudentException). Clean,
        complete resolution of S10's sharpest finding (B26). Re-enrollment policy explicitly decided and documented
        (blocked on ANY history for a course, not just the latest snapshot — consistent with the locked "one
        offering" scope) — closes B24.
Lost: The headline gap survived the fix round: `completeCourse` now creates a real, Grade-bearing COMPLETED
        enrollment, but `getStudentStanding` still filters for latest-status == PROGRESSING only, so a student's
        actual grade stays invisible through the one method named for it. His own Main.java runs exactly this
        sequence (completeCourse then getStudentStanding) and prints an empty list right under a recorded 85/100 —
        he ran it and didn't catch it (B25 narrows, doesn't close). New regression: fixing "remove doesn't check
        active enrollments" made CourseService.remove take an EnrollmentService parameter — the catalog now
        depends on enrollment orchestration to do its job (B27, new). Exception convention regressed in two new
        spots (addCourse, completeCourse both throw raw IllegalArgumentException instead of a named exception,
        while he used named exceptions correctly elsewhere in the same round). getStanding still byte-identical
        to getEnrolledStudents, now just commented as intentional rather than fixed. Main still println-only —
        zero of 7 exception types ever exercised in the demo, no capacity boundary hit.
TRANSFERABLE RULE: Building the write path for a feature (completeCourse) doesn't mean the read path (getStudentStanding)
  automatically uses it — trace the fact all the way from where it's created to where it's displayed, especially
  when your own demo prints the broken result right in front of you.
RECALL CARD → Q: You added `completeCourse`, which creates a real Grade-bearing enrollment. Your own Main.java calls
  it and then immediately calls getStudentStanding, and prints an empty list. What does that output tell you, and
  why didn't code review catch it before I did?
  A: A method's name is a promise about what it returns — "standing" implied grades, but the implementation only
  ever looked at PROGRESSING status. Reading your own demo's output line-by-line against what you expected it to
  say is exactly the habit that catches this before someone else has to.
Next session: fix getStudentStanding to actually surface completed grades (decide what "standing" means, once,
  and make the method match), move the remove-with-active-enrollments check to whoever should own it, fix the
  two raw IllegalArgumentException spots. Subscription Billing rebuild and Library end-of-day session remain parked.
```

```
SESSION 12 · Student Management System — Level-1 re-score (regression) · 2026-09-22
Target: close S11's remaining MAJOR (getStudentStanding excludes grades) and B27 (CourseService/EnrollmentService
  dependency inversion).
Score: 57/100 on the Level-1 lens (34/60: requirements 6, domain 4, responsibilities 7, data structures 7,
  API & errors 5, code quality 5). Was 67 in S11 — a real regression, his lowest score on this problem yet, despite
  two genuine structural fixes landing correctly.
Won:  B27 cleanly closed — `removeCourse` moved into EnrollmentService (checks active enrollments, then delegates to
        a simplified single-arg CourseService.remove); dependency direction now correct. getStanding/getStudentStanding/
        getStudentCourseStanding all reworked to take an explicit EnrollmentStatus parameter instead of hardcoding
        PROGRESSING — the right shape of fix for S11's core MAJOR; the read path can now structurally surface
        COMPLETED records. Main's demo widened from 1 flow to a 17-section walkthrough covering every status and
        both courses.
Lost: Two new defects, both visible in his own printed output, neither caught before asking for a re-score.
        (1) completeCourse(studentId, courseId, totalMarksObtained) never uses totalMarksObtained — Grade.SetObtainerMark
        is defined but called nowhere in the codebase (grepped to confirm) — so a completed course's grade is always
        obtainedMarks=0 regardless of the score passed in; his own Main passes 85 and prints 0 four lines later.
        (2) Grade regressed from a fully immutable value object to a mutable one (public setter, non-final field),
        and the identical Grade instance is now carried by reference across every snapshot in an enrollment's history
        (Progressing -> Dropped/Completed all alias the same object) — breaking the one property (immutable
        append-only snapshots) that was this problem's standout win. Also: Grade now constructed with a hardcoded
        totalMarks=100 and a hand-picked id offset (+299) at enrollment time, before any grading exists.
TRANSFERABLE RULE: Fixing the bug you were shown doesn't verify itself — when the fix touches a new field, parameter,
  or method, trace that exact new path end-to-end (call it, print it, read the number) before calling it fixed and
  asking for a re-score. Three rounds running now where the breaking evidence was already sitting in his own console
  output.
RECALL CARD → Q: You reworked completeCourse to take a score and reworked the standing queries to surface COMPLETED
  records — genuinely the right fix. Your own demo then prints obtainedMarks=0 right after passing 85. What single
  habit would have caught this before asking for a re-score?
  A: Read your own demo's printed output line by line against what you expected each line to say — not just that it
  ran without throwing, but that the numbers in it are the numbers you put in.
Next session: make Grade immutable again (construct a new one at completeCourse time instead of mutating), actually
  wire the score through, decide whether Grade belongs on a PROGRESSING enrollment at all. Subscription Billing
  rebuild and Library end-of-day session remain parked.
```

```
SESSION 13 · Meeting Room Booking (new problem, Level 1) · 2026-09-24
Target: Level-1 bar on a fresh problem — dim 1 Phase 2 (5/10), paper design (52), then the build. Code in problems/EmployeRoomManager.
Score: 67/100 on the Level-1 lens (40/60: requirements 7, domain 7, responsibilities 5, data structures 8, API & errors 6,
  code quality 7). Pass mark 70 with every criterion >= 5 -> 3 short; every criterion clears 5 (ties his best, S11 = 67).
Won:  A self-checking Main (70/70, expected-vs-actual on every scenario) plus a fault-injecting rollback test; my randomized
        40,000-op comparison against brute force found 0 mismatches — the TreeSet + lower() overlap check with an id tie-break
        is correct, end-exclusive boundary included. First time his own harness proves a fix (G16 moves; JUnit still L4).
      Transferred lessons unprompted: registries are Map + duplicate guard from the start (B26), immutable Booking with
        withState (S10 snapshots), system-assigned id + returned booking, distinct EmployeeAlreadyBooked, no System.out in domain.
Lost: He kept two rulings' targets and wrote why — the cost is real. completeBooking lets a FUTURE booking be completed, which
        drops it from the set the overlap check reads, so E2 then books the same slot (probe: ok, two bookings, one slot).
        The EmployeeService mirror copy is a write-only ledger no rule reads, yet has public addBooking/removeBooking and a
        live Employee.getBookingData(): touching it makes the two answers disagree (0 vs 1), two employees can share one
        injected manager, null is accepted and the first booking dies with a raw NullPointerException.
      cancelBooking still takes a caller-built Booking (a wrong roomId inside it -> BookingNotFound for a booking that
        exists) and 5 failures are a raw IllegalArgumentException (end<=start, cross-day, headcount<1, duplicate room/employee).
TRANSFERABLE RULE: A feature added "for the future" still ships today — its bugs count today. completeBooking was outside the
  locked scope and is the only path that breaks the room's no-overlap rule.
RECALL CARD -> Q: You add completeBooking, which a caller triggers by hand, with no clock. Which invariant can it break, and
  what is the general rule? A: It can free a future slot (the booking leaves the BOOKED set that overlap checks read) so the
  room is double-booked; a hand-set state flag can disagree with time. Store facts, derive states from the clock (card 9).
Next session: the ~20-minute finish (drop COMPLETED/completeBooking/removeBooking, delete or seal the mirror, cancel by
  id + typed exceptions for bad input), re-score against the same bar. Then Level 2 on this problem (auto-pick best room).
  Parked: thread-safety of nextBookingNumber++ / check-then-act in newBooking (L5); Clock + JUnit (L4).
```

```
SESSION 14 · Expense Sharing (new problem, Level 1) · 2026-09-25
Target: Level-1 bar on a fresh problem — Phase 2 dim 1 = 5/10, paper design, then a V1 build (problems/ExpenseTracker). He built V1
  without answering the five Phase-3 questions; Q1, Q3, Q4 and Q5 each predicted a break that the probes then found.
Score: 53/100 on the Level-1 lens (32/60: requirements 4, domain 7, responsibilities 5, data structures 5, API & errors 5,
  code quality 6). Pass 70 with every criterion >= 5 -> requirements is under the floor.
Won:  Expense is now a real ledger — one shared expense with an immutable PAY transaction (payer, total) and SETTLE transactions —
        so the fact survives (Phase-3 Q1 answered in code). Arrows are clean: no cycle, User no longer holds a service, maps keyed by
        id not by User object. First idempotency he modelled AND used: caller-supplied expenseId, a double-click throws
        ExpenseAlreadyExist. BigDecimal, defensive copies, member-gated reads, self-checking Main (67/67, expected vs actual).
Lost: The two headline features are absent: no settle-up on any service (0 methods; only Expense.settle on an entity that
        getGroupExpenses hands out live) and no "total owe/owed" or per-member "who owes whom" view — B pays 90 for A,B,C and B's
        view is {v1: 0.00}. newExpense is unguarded: the comment says GroupService validates before calling in, but nothing calls
        in and Main calls it directly (phantom group, ghost payer, non-user participant all accepted). Money rules unmet: 100/3 sums
        to 99.99, amount -50 and 0 accepted, empty list -> raw ArithmeticException, settle 1000 on a 30.00 debt -> owes -970.00,
        settle -10 raises the debt. newGroup(A,B,A) fails after A and B were already indexed -> a ghost group in their lists.
        Three registries are List + linear scan again (S10's slip): 20,000 newExpense = 473 ms.
TRANSFERABLE RULE: A check you only describe in a comment does not exist — trace each rule to the exact line that enforces it,
  starting from the method your own demo calls.
RECALL CARD -> Q: A comment says "GroupService validates before calling in", yet Main calls ExpenseService.newExpense directly.
  What habit would have caught this? A: For every rule, find the line that enforces it and the call path from the public entry
  point your demo uses; grep for callers of the method that is supposed to be guarded.
Next: re-score after the requirements/guard/money fixes.
```

```
SESSION 15 · Music Playlist Manager (self-built alone, no Phase 1-3 run) · 2026-09-26
Target: none assigned — Sahil built it in his own time and asked to be scored (problems/MusicPlaylist, 810 lines). No scope was
  written, so "requirements covered" is provisional (checked only against the obvious core of a playlist manager).
Score: 55/100 on the Level-1 lens (33/60: requirements 5 provisional, domain 6, responsibilities 5, data structures 6,
  API & errors 5, code quality 6). Pass 70 with every criterion >= 5 -> 15 short; no criterion under the floor.
Won:  Transfers seen unprompted: every registry is a Map<UUID,...> (no List + linear scan — B26 held in a new problem), playlist
        mutators take (actingUserId, playlistId) and look the playlist up in the acting user's own list so a non-member gets
        PlaylistNotFound (Meeting Room's id-first API), 13 typed exceptions and no null returns, services return copies, clean
        constructor-injection arrows (Account -> User -> Artist), composition not inheritance. 80-check self-verifying Main.
Lost: Effort went to identity plumbing (3 entities, 3 services, 9 of 13 exceptions for accounts/users/artists) while the playlist
        itself can't be played (no next/previous/shuffle), deleted, renamed, and a song can't be found without its artist id.
      PlaylistService takes caller-built User/Playable objects and has no collaborator to check them: an unregistered user creates
        a playlist, a Playable in no catalogue (artist null) and a `new Song(..)` are accepted. Removing a song from the catalogue
        leaves it in playlists (still plays) and re-adding gives a second identity -> playlist [Two, Two]. User.stopSong() throws
        NPE with nothing playing, playSong never stops the current one, Song.play()/stop() print from the domain (his Main hijacks
        System.out to test it). Artist.getSong() returns the live list (add(dup) bypasses SongAlreadyExists, clear() wipes it);
        createAccount(name, null) poisons the next createAccount with an NPE; isEmailTaken scans all accounts (20,000 = 2.5 s).
TRANSFERABLE RULE: Write the scope before the first class — without it you cannot tell a missing feature from a choice, and the
  build drifts toward what is easy (identity plumbing) instead of what the problem is about (playing and managing playlists).
RECALL CARD -> Q: You start a problem alone. What is the first thing you write down, and what does it protect you from?
  A: A short scope — the verbs each actor performs, the questions they ask, and an Out list. It protects you from building the
  easy plumbing and calling the thin core "done", and it gives you something to test against.
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
| 9 | 2026-09-21 | Subscription Billing — Level-1 reference (Sensei-written) | — | not scored | — |
| 10 | 2026-09-22 | Student Management System (Level 1) | Requirements, domain, responsibilities, data structures, API & errors, code quality (Level-1 lens) | 63/100 (Level-1 lens) | Not yet decent — best L1 score, first to clear ≥5 on every criterion |
| 11 | 2026-09-22 | Student Management System — Level-1 re-score | Close S10's 3 MAJORs (B24, B25, B26) | 67/100 (Level-1 lens) | Not yet decent — closing in on 70 |
| 12 | 2026-09-22 | Student Management System — Level-1 re-score (regression) | Close remaining MAJOR + B27 | 57/100 (Level-1 lens) | Not yet decent — regressed, lowest score on this problem |
| 13 | 2026-09-24 | Meeting Room Booking (Level 1, new problem) | Level-1 bar; Phase 2 dim 1 = 5, paper 52 | 67/100 (Level-1 lens) | Not yet decent — 3 short of 70, every criterion >= 5 |
| 14 | 2026-09-25 | Expense Sharing (Level 1, new problem) | Level-1 bar; Phase 2 dim 1 = 5 | 53/100 (Level-1 lens) | Not yet decent — requirements under the floor (headline features missing) |
| 15 | 2026-09-26 | Music Playlist Manager (self-built, no coaching phases) | none assigned; requirements provisional | 55/100 (Level-1 lens) | Not yet decent — 15 short, no criterion under the floor |

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
| G5 | Presentation fused into domain (`System.out` in entities) | improving (S4 + S5: 0 `System.out` in the domain in both builds. S3 relapsed in `Topic.handleSubscriberFailure`, so it closes only when it holds in a different problem) — S8: relapsed in `SubscriptionManager.newSubscription` (`System.out.println("Already have the same plan")`) — S10: held clean again, 0 `System.out` in `CourseService`/`StudentService`/`EnrollmentService`, third clean data point but given the flip-flop history still not marked closed |
| G6 | Inheritance for reuse, not IS-A (LSP violations) | open |

### P1
| ID | Gap | Status |
|---|---|---|
| G7 | Only 4 patterns ever used | open |
| G8 | Business policy hardcoded in logic | open |
| G9 | No events / no Observer | closed* (S3: `Topic`/`Subscriber`/`Publisher` — real fan-out, correct, not over-engineered. *First-ever use, not a taught-then-transferred case like G1/G2/G10 — no prior session to transfer from, so this is "gap no longer true" rather than proven transfer. Re-verify it shows up unprompted in a later problem before treating the *pattern-selection* instinct itself as trusted.) — **S7 Phase 4: not reached for unprompted** (audit + email proposed as two separate mechanisms); pattern-selection instinct still unproven |
| G10 | No lock expiry / TTL / reaper | closed (S2: hold expiry + background sweep self-built in `HotelInventory`, no unlock — closes the loop from the *original* Movie Ticket Booking gap where a `SEATS_LOCKED` booking never expired) |
| G11 | Idempotency modeled but never used | improving (S14: caller-supplied `expenseId`, a duplicate submit throws `ExpenseAlreadyExistException` — first idempotency he modelled AND used; closes when repeated unprompted in another problem) |
| G12 | No custom domain exceptions | improving (S5: 4 typed exceptions — `BookNotFound`, `NoCopiesAvailable`, `LoanNotFound`, `AlreadyReturned` — thrown from borrow/return. Still open: `getBook`/`getBooking` return null, `Booking.markReturned` leaks a raw `IllegalStateException`, no common base type, `UserNotFoundException.java` is a 0-byte file, user never validated) — S7: no validation or typed failures again in Subscription Billing (null returns, unknown/removed plan accepted); now a Level-1 item. — S10: real progress — 5 typed exceptions, `getStudent`/`getCourse` throw instead of returning null (fixes the S8 NPE shape), duplicate-enrollment throws instead of returning a UI string. Still open: `StudentService.registerStudent` has no duplicate-id guard (B26). |
| G13 | Encapsulation leaks (live collections returned) | improving (S5: all 3 repository finders return `List.copyOf` snapshots — 0 CME in 1.5s vs 16,010 in S4; `Book`/`User` defensively copy. Residual: snapshots still hold the shared mutable `Booking` objects) — S10: held again, every list-returning method in `EnrollmentService`/`StudentService` copies before returning |
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
| B24 | Silent re-enrollment after drop (Student Mgmt): `EnrollmentService.hasActiveEnrollment` only checks whether the *latest* snapshot for (student, course) has status `PROGRESSING` — a `DROPPED` latest snapshot returns false, so `newEnrollment`'s duplicate guard never fires. Contradicts his own Phase-3 decision to block re-enrollment (2026-09-22 verified: enroll → drop → enroll again succeeds silently). | closed same-round (S11: `hasEnrollmentHistory` now checks the *entire* history, not just the latest snapshot — re-enrollment blocked on any prior record, decided and documented on purpose) |
| B25 | Standing/grades unreachable (Student Mgmt): no code path ever creates a `COMPLETED` enrollment or attaches a real `Grade` — `Grade` is always constructed `null`, `EnrollmentStatus.COMPLETED` and `Grade.getPercentage()` are dead code, despite "standing = grades" being his own Phase-2 scope choice (2026-09-22 verified by reading every call site). | improving (S11: `completeCourse` now creates a real Grade-bearing `COMPLETED` enrollment — the write side works. Still open: `getStudentStanding` filters for latest-status `PROGRESSING` only, so the grade just recorded is invisible through the one method named to show it — verified via his own `Main`, which prints an empty standing right after a recorded 85/100) |
| B26 | No duplicate-id guard on registration (Student Mgmt): `StudentService.registerStudent` appends to a `List` with no id check; `getStudent`/`exists` linear-scan and return the first match, so a second registration under an existing id becomes a permanently unreachable ghost record still present in `getRegisteredStudents()` (2026-09-22 verified). Same List-vs-Map mistake he'd just fixed for `Course` in the same session. | improving (S11: `Map<Integer,Student>` + `DuplicateStudentException` guard — complete, correct fix; same-problem fix, closes when unprompted elsewhere) |
| B27 | Dependency-direction inversion (Student Mgmt): fixing "`CourseService.remove` doesn't check active enrollments" (S10 NIT) made `remove` take an `EnrollmentService` as a method parameter — the catalog service now needs enrollment orchestration to do its own job, instead of the check living where enrollment state is owned (S11 verified by reading `CourseService.java`). | open |
| B28 | Unguarded mirror copy (Meeting Room): `EmployeeService` keeps a write-only copy of every employee's bookings that no rule reads, exposes public `addBooking`/`removeBooking` and a live `Employee.getBookingData()`; `BookingDataManager` is injected by the caller (2026-09-24 verified: shared manager -> E2 sees E1's booking; null manager accepted, first booking throws raw NPE; direct write makes 0 vs 1 disagree). Defended as "for the future" although the locked scope deletes nothing. | open (cost graded in S13) |
| B29 | State without a clock (Meeting Room): `BookingService.completeBooking` moves a BOOKED booking to COMPLETED by hand; COMPLETED bookings leave the `booked` TreeSet that `hasOverlap` reads, so completing a FUTURE booking lets another employee book the same slot (2026-09-24 verified). Same family as card 9. | open |
| B30 | Unguarded write path (Expense Sharing): `ExpenseService.newExpense` never checks the group, payer or participants; its comment says `GroupService` validates before calling in, but `GroupService` has no expense-creation method and `Main` calls `newExpense` directly (2026-09-25 verified: expense in a nonexistent group, ghost payer, non-user participant all accepted). Reads are member-gated, writes are not. | open |
| B31 | Money rules unenforced (Expense Sharing): `EqualSplit` drops the remainder (100/3 -> 99.99; 0.10/3 -> 0.09), `newExpense` accepts amount <= 0, empty list -> raw `ArithmeticException`, only-payer list; `Expense.settle` accepts <= 0 and > owed (owes -970.00 after paying 1000 on 30.00; a negative settle raises the debt) and is reachable through no service (2026-09-25 verified). | open |
| B32 | Caller-built objects trusted (Music Playlist): `PlaylistService.createPlaylist(name, User)`, `addNewUser(.., User)`, `addNewPlayable(.., Playable)` take entities and have no collaborator to validate them (2026-09-26 verified: unregistered user creates a playlist and is added to another; a `Playable` in no catalogue and a `new Song(..)` never registered with its artist are accepted). Same family as B17/B30. | open |
| B33 | Dangling song references (Music Playlist): `ArtistService.removeSong` never touches playlists, `Song` ids are per-instance UUIDs; a removed song stays in playlists and still plays, and re-adding the same name is accepted as a new identity -> playlist [Two, Two] (2026-09-26 verified). | open |

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
| 3 | Why is "CAS the shared producer cursor forward, then write the payload" unsafe even with a single producer and zero preemption? | The JMM gives no happens-before edge from a plain write that follows a volatile/atomic write in program order to a later read by another thread. The payload write must happen-before the publish signal, never after it. | ★ S7 Phase 0 (2026-09-21): answered "not sure"; the same rule also failed in application in S6 (B22); ★★ S10 Phase 0 (2026-09-22): answered "consumer can get the null" — a guessed symptom, no mechanism (publish-before-populate; no happens-before edge). Third failure → needs a dedicated drill before Level 5, not another recall card |
| 4 | A getter takes the read lock, does `map.get(key)` and returns the `List` it found. Is the caller's `for`-loop over it safe against concurrent writers? | No — the lock releases on return, so the caller iterates the live list unlocked while a writer mutates it → CME / data race. Return a snapshot (`List.copyOf`) from inside the lock, or store only immutable lists. | |
| 5 | Two threads call `returnBook(sameLoanId)`. The manager does `isReturned()` check → `stock.increment()` → `loan.markReturned()`. What goes wrong, and what's the smallest fix? | Both pass the check and both increment → a phantom copy; the check-then-act on `returnDate` isn't atomic. Make the state transition itself atomic (CAS / `synchronized`, returning whether THIS caller won) and increment stock only if you won. | |
| 6 | `Booking.markReturned()` writes `returnDate` inside `synchronized(this)`; `isReturned()` reads it with no lock and no `volatile`. What can a polling reader observe, and what's the smallest fix? | It may never see the write — no happens-before edge, so the JIT can hoist the read out of the loop. Make the field `volatile` (or synchronize the readers too). | partial, S7 Phase 0: named volatile/synchronized (said "volatile isReturn" — it is the field, and synchronized only works if EVERY reader takes the same monitor); never said what the reader observes (may never see the write) |
| 7 | A customer subscribes at ₹299; marketing then changes the plan's price to ₹999. What should the customer's subscription say, and how do you guarantee it? | ₹299 — copy the agreed terms into the subscription at subscribe time; never let a contract read its terms from a mutable catalogue entry. | |
| 8 | What is the first thing you do after any structural change (splitting a class, adding a factory)? | Re-run every flow that worked before and compare the output — a refactor must not change behaviour. | |
| 9 | Why derive "expired" from the clock instead of keeping a state flag on the subscription? | A stored flag only changes when someone touches the object, so it can say ACTIVE while the clock says expired. Store the timestamps and compute the state from "now" — one source of truth. | partial, S10 Phase 0 (2026-09-22): right conclusion (single source of truth, trust the clock) but blamed "if it broke in between"; missed the mechanism — a flag only changes when something touches the object, and time passing touches nothing |
| 10 | You fixed Course's lookup from a TreeSet to a Map<id, Course> earlier in a session. StudentService, built minutes later, still uses a List + linear scan with no duplicate-id guard. What should you have done, and when? | The moment a data-structure lesson lands for one entity, check every other service doing the same id-keyed job — it's a general rule, not a Course-specific one. Do it before writing the sibling class, not after review finds it. | |
| 11 | You added `completeCourse`, which creates a real Grade-bearing enrollment. Your own `Main` calls it and then immediately calls `getStudentStanding`, which prints an empty list. What does that output tell you, and why should code review not have to be the one to catch it? | A method's name is a promise about what it returns — "standing" implied grades, but the implementation only ever looked at `PROGRESSING` status. Reading your own demo's output line-by-line against what you expected it to say catches this before anyone else has to. | |
| 12 | You reworked `completeCourse` to take a score and the standing queries to surface COMPLETED records — the right fix. Your own demo then prints `obtainedMarks=0` right after passing 85. What single habit would have caught this before you asked for a re-score? | Read your own demo's printed output line by line against what you expected each line to say — not just that it ran without throwing, but that the numbers coming out are the numbers you put in. | |
| 13 | You add `completeBooking`, which a caller triggers by hand, with no clock. Which invariant can it break, and what is the general rule? | It can free a future slot (the booking leaves the BOOKED set overlap checks read) so the room is double-booked; a hand-set state flag can disagree with time. Store facts, derive states from the clock. | |
| 14 | A comment says "GroupService validates before calling in", yet `Main` calls `ExpenseService.newExpense` directly. What habit would have caught this? | For every rule, find the line that enforces it and the call path from the public entry point your demo uses; grep for callers of the method that is supposed to be the guard. | |
| 15 | You start a problem alone. What is the first thing you write down, and what does it protect you from? | A short scope — verbs each actor performs, the questions they ask, an Out list. It protects you from building the easy plumbing and calling a thin core done, and gives you something to test against. | |
