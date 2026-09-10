# LLD PROGRESS LOG

> Read at the start of every session. Append the Phase-8 card at the end of every session.
> Profile & curriculum: `LLD_TUTOR_PROMPT.md`

---

## SESSION LOG

_(none yet — Session 1 in progress)_

| # | Date | Problem | Target gaps | Score | Verdict |
|---|------|---------|-------------|-------|---------|
| 1 | 2026-09-09 | Thread-safe KV Store w/ TTL | G1, G15, G16, G12 | — | in progress |

---

## RUBRIC SCORES OVER TIME

| Dimension | S1 | S2 | S3 | S4 | S5 | S6 | S7 | S8 | S9 | S10 | S11 | S12 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 Requirements & scoping | | | | | | | | | | | | |
| 2 Domain modeling | | | | | | | | | | | | |
| 3 Abstraction & seams | | | | | | | | | | | | |
| 4 SOLID | | | | | | | | | | | | |
| 5 Pattern selection | | | | | | | | | | | | |
| 6 Concurrency & correctness | | | | | | | | | | | | |
| 7 Data structures & complexity | | | | | | | | | | | | |
| 8 API & error design | | | | | | | | | | | | |
| 9 Extensibility (proven) | | | | | | | | | | | | |
| 10 Testability & hygiene | | | | | | | | | | | | |

---

## GAP LEDGER

`open` → `improving` → `closed` (closed = solved correctly in a **different** problem than where it was taught)

### P0
| ID | Gap | Status |
|---|---|---|
| G1 | Zero concurrency thinking (TOCTOU in `ShowSeat.lock()`) | open |
| G2 | No thread of execution / no simulation loop | open |
| G3 | God controllers, no layering vocabulary | open |
| G4 | No repository abstraction | open |
| G5 | Presentation fused into domain (`System.out` in entities) | open |
| G6 | Inheritance for reuse, not IS-A (LSP violations) | open |

### P1
| ID | Gap | Status |
|---|---|---|
| G7 | Only 4 patterns ever used | open |
| G8 | Business policy hardcoded in logic | open |
| G9 | No events / no Observer | open |
| G10 | No lock expiry / TTL / reaper | open |
| G11 | Idempotency modeled but never used | open |
| G12 | No custom domain exceptions | open |
| G13 | Encapsulation leaks (live collections returned) | open |
| G14 | Telescoping constructors, no Builder | open |
| G15 | No `Clock` abstraction | open |
| G16 | Zero tests | open |

### P2 — correctness bugs
| ID | Bug | Status |
|---|---|---|
| B1 | Double-booking hole (Rental): check and commit not atomic | open |
| B2 | `TreeSet` comparator defines equality (Rental) | open |
| B3 | Return ≠ cancel (Rental) | open |
| B4 | Entity as `HashMap` key without equals/hashCode (Airline) | open |
| B5 | `TreeMap` used as a `HashMap` (Airline) | open |
| B6 | Control flow via side-effect inspection (Airline) | open |
| B7 | Hall-call direction discarded; no SCAN/LOOK (Elevator) | open |
| B8 | Idle elevators penalized by scoring (Elevator) | open |
| B9 | No overlap check where he already knew how (Movie) — **meta-gap: transfer failure** | open |
| B10 | `Show` has no id / no back-reference; linear scans | open |
| B11 | Name collisions & package naming | open |
| B12 | `RentalSystem` is an empty class | open |

### Sensei-added findings (verified 2026-09-09, not in original audit)
| ID | Bug | Status |
|---|---|---|
| B13 | `BookingController.confirmBooking` takes payment, *then* calls `seat.book()`. A double-click on Pay charges twice — the second call's guard throws only **after** the money moved. Idempotency must gate **before** the side effect. | open |
| B14 | `BookingController.addTheater` does `theaterTreeMap.put(location, theater)` — two theaters at the same `Location` silently overwrite. A `Map` keyed by a non-unique attribute is a data-loss bug. | open |
| B15 | `BookingController.createShow` constructs `new Show(movie, showSeats, time)` with an **empty** list, then fills that same list afterwards. `Show` aliases a collection the controller still mutates. | open |

---

## PATTERN LEDGER

| Pattern | Used correctly | Misused | Never used |
|---|---|---|---|
| Strategy | ✓ (Elevator dispatch) | | |
| Factory | ✓ (PaymentMethodFactory) | | |
| Static Factory Method | ✓ (PaymentResult) | | |
| Adapter | ✓ thin (PaymentGateway) | | |
| Observer | | | ✗ |
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
| Repository | | | ✗ |
| Specification | | | ✗ |
| Null Object | | | ✗ |
| Registry | | | ✗ |
| Prototype | | | ✗ |

---

## RECALL CARDS

_(one added per session; ★ = failed on re-test at least once)_

| # | Q | A | Failed re-tests |
|---|---|---|---|
