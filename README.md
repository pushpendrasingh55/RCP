# Road Cutting Permission — API and Portal

A Spring Boot API and a React portal implementing the Road Cutting Permission take-home assignment
(spec v3.0 + Addendum Revision 3.1).

## How to run it from a clean machine

### 1. Database

```bash
docker compose up -d          # starts Postgres on localhost:5432 (db=rcp, user=rcp, password=rcp)
```

(No Docker? Any local PostgreSQL 14+ works — create a database/user matching the values above, or
override them with the `SPRING_DATASOURCE_*` environment variables described in
`backend/src/main/resources/application.yml`.)

### 2. Backend

```bash
cd backend
mvn spring-boot:run           # runs on http://localhost:8080
```

Flyway migrates the schema automatically on startup — there is no manual migration step.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev                   # runs on http://localhost:5173, proxying /rcp/** to :8080
```

Open `http://localhost:5173`. There is no login screen (auth is out of scope per the spec) — use the
amber "Acting as" bar at the top of the page to pick a tenant, role, mobile number and uuid; every
API call is built from that identity, exactly as `RequestInfo.userInfo` requires.

### Tests

```bash
cd backend
mvn test
```

All backend tests are plain unit tests (JUnit 5 + Mockito, no Spring context, no database) — they
run on a clean machine with nothing but the JDK and Maven, no Postgres required.

---

## What we built

- **Backend**: Spring Boot 3 / Java 17, all four endpoints (`_calculate`, `_create`, `_action`,
  `_search`), PostgreSQL + Flyway, config-driven fee calculation and workflow engine, server-side
  role and tenant enforcement, optimistic + pessimistic concurrency control, a concurrency-safe
  application-number generator, and unit tests covering both worked examples, the fee-rule
  boundaries, one illegal-transition case, and one tenant-isolation case. Every request/response
  envelope's root keys (`RequestInfo`, `Calculation`, `ResponseInfo`, `Errors`, etc.) are
  capitalized via `@JsonProperty` to match the spec's contract precisely, while everything nested
  inside them (`userInfo`, `tenantId`, `roadType`, ...) stays lowercase camelCase, exactly as the
  spec's own worked example shows — the frontend's `api/client.ts` builds and reads the same casing.
- **Frontend**: React 18 + TypeScript + Vite + Tailwind. An applicant flow (form with per-field
  validation, debounced live fee preview, "my applications" list + detail with a timeline built
  entirely from the API's transition history) and an officer flow (a status-filterable queue whose
  action buttons come from `availableActions` in the API response, never a hardcoded switch).

## What we deliberately did not build

- **No stretch item.** The Core took the full time budget to do properly; per the spec's own
  scoring note ("doing three badly, at the expense of the Core, counts against you") we chose not to
  attempt idempotent create, draft survival, localisation, one-command startup, or permit expiry as
  full features. (A *minimal* idempotency hook — an optional `requestReferenceId` on `_create` that
  returns the existing application instead of creating a duplicate — was cheap enough to include
  alongside the Core and is not claimed as the stretch pick.)
- **No login/authentication UI**, per the spec ("do not build a login"). The frontend's identity
  switcher is a deliberate, visible stand-in — see Assumptions.
- **No single "get application by number" endpoint** — the spec's API contract only lists
  `_calculate`, `_create`, `_action`, `_search`. The detail view re-uses `_search` filtered to one
  `applicationNumber`, which stays within the given contract and inherits its tenant/role scoping for free.
- **No road-type catalogue endpoint** — the frontend's road-type dropdown hardcodes the three active
  codes from the published rate table; the backend remains authoritative (an inactive/unknown code
  is still rejected server-side regardless of what the dropdown offers).

## Assumptions

- **Percentage-based monetary sub-components are rounded half-up to a whole rupee at the point they
  are computed** (the urgency surcharge, and the percentage-based leg of the security deposit before
  the `max()`), not only at the final total. The spec's worked examples never exercise a case where
  this matters (all intermediate figures land on whole rupees already), so this is a forward-looking
  choice for inputs that don't; it is documented here rather than left implicit.
- **CANCEL is restricted to the application's own applicant**, not merely to "some caller carrying
  the APPLICANT role" — enforced as an extra check in `ApplicationService`, on top of the
  role check the workflow engine performs. This seemed like the obviously-intended reading of
  "Create, view own applications, cancel [own] application" in the actor table.
  Sending SEND_BACK back to APPLIED does not automatically clear the record's editable-state fields
  for GET-side editing, because the spec's Core API contract has no "update application" endpoint at
  all — resubmission after SEND_BACK would need a `_update` endpoint that isn't in the Core contract's
  four endpoints, so it wasn't built. This is the largest single Core gap; see Written Answer 3.
- **An unknown `tenantId`** (one with no entry in `rcp.tenant-prefixes`) is rejected as a 400
  `VALIDATION_ERROR` before any other processing, so that onboarding a new city is "add one config
  line", not "silently misbehave until someone notices".
- **`_search`'s `offset`/`limit` is translated onto Spring Data's page-based `Pageable`** by
  `page = offset / limit`. This is exact for normal "next page" navigation (where `offset` is always a
  multiple of the current `limit`) but not for an arbitrary, non-aligned offset — a reasonable
  trade-off for a take-home, called out explicitly rather than silently accepted.
- **`GOVERNMENT_AGENCY` zeroes the permission fee before the urgency-surcharge percentage is taken**,
  per the spec's own rule ordering (rule 4 before rule 5) — so a government applicant's surcharge is
  always zero too, without a separate special case.

## AI usage

AI assistance was used throughout, for drafting boilerplate (DTOs, entities, repetitive Tailwind
markup) and for cross-checking the fee-calculation arithmetic against the two worked examples by
hand. It helped most on the JSON-merge logic for tenant rate overrides (getting the "only override
named fields, fall back to defaults for the rest" behaviour right on the first pass, including the
null-vs-zero distinction that motivated boxed types in `RawRoadType`). It was least reliable on the
`_search` pagination semantics — its first instinct was to silently clamp any `offset` to the nearest
page boundary without saying so; that got caught and turned into the explicit, documented assumption above.

## Roughly how long it took

Building this as a single, careful pass (backend, frontend, tests, README) took the better part of a
working day's worth of effort, in line with the spec's ~7-hour time box for a from-scratch build of
this shape.

---

## Design notes

### Workflow configuration

`backend/src/main/resources/workflow-config.json` is a flat list of
`{ fromState, action, toState, allowedRoles }` rows, loaded once at startup by `WorkflowService`.
`WorkflowService.applyTransition(fromState, action, role)` does a single linear lookup against that
list and either returns the target state or throws `IllegalTransitionException` — there is no
`switch`/`if-else` on `ApplicationStatus` anywhere in the codebase. `WorkflowService.availableActions`
uses the same table to answer "what can this role do from this state", which is exactly what the
officer UI's action buttons are built from. Changing the lifecycle — a new state, a new role on an
existing transition, a brand-new action — is a JSON edit, not a Java change.

### Tenant isolation

`RequestInfo.userInfo.tenantId` is the single server-side-authoritative tenant identity for a
request. `TenantValidator` compares it against any `tenantId` the request body also carries
(`Calculation.tenantId`) and rejects a mismatch outright with `TENANT_MISMATCH` (403) — the server
never "picks one" of two disagreeing tenant claims.

Every repository method that can return an `Application` takes `tenantId` as an explicit parameter
and filters by it inside the query itself (`ApplicationRepository.findByTenantIdAndApplicationNumber`,
`findForUpdate`, and the `_search` path via `ApplicationSpecifications.tenantId(...)`, which is always
the first predicate composed, before any caller-supplied filter). There is deliberately no repository
method that looks up an application by number alone. A Haridwar caller asking for a Dehradun
application number gets the same `APPLICATION_NOT_FOUND` (404) they would get for a number that
doesn't exist at all — the API never confirms "that number exists, just not for you", which would
itself leak information across tenants.

### Concurrency

Two guards, deliberately overlapping:

1. **Pessimistic row lock on read.** `_action` loads the application via `findForUpdate` (`SELECT ...
   FOR UPDATE`), inside the same transaction as the eventual `UPDATE`. A second, concurrent `_action`
   call on the same row blocks at the `SELECT ... FOR UPDATE` until the first transaction commits or
   rolls back — it never reads a state that is about to become stale.
2. **JPA `@Version` optimistic lock, as a second, independent guard.** If the two guards were ever out
   of sync (e.g. a future code path that reads without `findForUpdate`), the `UPDATE ... WHERE id = ?
   AND version = ?` from the losing transaction affects zero rows, Hibernate throws
   `OptimisticLockException`, and `GlobalExceptionHandler` turns that into a 409 `CONCURRENT_UPDATE` —
   never a silent overwrite, never a 500.

Net effect for the spec's exact scenario (two officers act on the same application within the same
second): the first request's transaction commits; the second either blocks briefly and then fails its
*workflow* check (because the state it sees, post-commit, is no longer the one its action is legal
from — e.g. it's no longer `PENDING_APPROVAL`), or, in the rarer case where the second action would
still have been legal in principle, its own version check fails and it gets a 409 asking it to reload.
Either way, only one transition wins, and the other is told clearly, not silently dropped.

### Application numbers

`ApplicationNumberGenerator` uses a single atomic SQL statement —

```sql
INSERT INTO application_sequence (tenant_id, financial_year, last_value)
VALUES (?, ?, 1)
ON CONFLICT (tenant_id, financial_year)
DO UPDATE SET last_value = application_sequence.last_value + 1
RETURNING last_value
```

— run in its own `REQUIRES_NEW` transaction. PostgreSQL executes the whole `INSERT ... ON CONFLICT
... DO UPDATE` as one atomic, row-locked operation: two concurrent transactions racing on the same
`(tenant_id, financial_year)` are serialised by Postgres itself, not by application code, so each gets
a distinct, strictly-increasing `last_value`. This is why the implementation never does `SELECT
max(...)` followed by a separate `INSERT` — that shape has a race window between the read and the
write where two transactions can compute the same "next" number.

---

## Written answers (spec §4)

### 1. Rate versioning

Rates live in a single `rates.json`, loaded once at startup into an in-memory table with no
notion of "as of when". If a government order changes the Haridwar BT day-rate mid-year, restarting
the service with an edited `rates.json` recomputes nothing retroactively — existing applications
already store their own frozen `totalAmount` and breakdown at creation time, so *already-issued
permits are safe*. What breaks is anything that needs a *point-in-time* rate lookup that isn't "the
rate as of right now": a permit renewal quoted against its original terms, an audit report that asks
"what would this have cost under last April's rates", or simply confidence that a config reload
mid-request doesn't produce two different quotes for the same form session. The fix is to make rate
configuration *versioned and dated* rather than a single current snapshot — e.g. each `RoadTypeRate`
gains an `effectiveFrom` (and optional `effectiveTo`), `RateConfigurationProvider` gains a
`getRoadTypeRate(tenantId, code, LocalDate)` overload, and `_calculate`/`_create` always pass "today"
explicitly (they already compute `applicationDate` server-side, so this is a small addition, not a
rearchitecture) rather than implicitly reading "whatever's loaded right now".

### 2. Concurrency

Covered in the design notes above in mechanism; restated here at the level of *what happens vs. what
should happen*: what happens is that the first action to reach Postgres's row lock wins outright, and
the second either fails a workflow check against the now-current state or fails its optimistic-lock
check — in both cases it gets a clear 4xx, not a silent overwrite and not a 500. What *should* happen,
and does, is exactly that: the system should never let two conflicting decisions on the same
application both "succeed" from the two officers' point of view. The one gap worth naming: the losing
officer's UI currently has to re-fetch and re-read the application to understand what happened next
(the 409/400 message says what went wrong, but doesn't push the new state to them) — a nicer version
would have the frontend auto-refresh the detail view on a 409/ILLEGAL_TRANSITION response rather than
just showing the error banner.

### 3. The decision least happy with

Not building a `_update` endpoint for the post-`SEND_BACK` edit-and-resubmit flow. The spec is clear
that "after SEND_BACK, the applicant may edit and resubmit according to the application's
editable-state rules" — but the Core API contract's four endpoints (`_calculate`, `_create`,
`_action`, `_search`) don't include an update operation, and inventing a fifth endpoint outside that
contract felt like a bigger unstated-requirement risk than leaving it out and documenting the gap.
Given two more days, I'd add a `POST /rcp/v1/_update` restricted to `APPLIED`-state applications
owned by the calling applicant, reusing the same server-side fee recalculation as `_create` (never
trusting a client-sent total there either) and recording the edit as its own transition-history entry
so the timeline shows *what* was resubmitted, not just that a `SEND_BACK` happened. The risk of adding
that once the service is live rather than now is mostly about the history/timeline shape: if
`_update` records changed a submitted `Calculation` in place, any *external* system already keyed off
an application's original dimensions/fee (a downstream billing export, say) would need to cope with
those changing after the fact — which argues for making an update produce a superseding record rather
than mutating the existing row, and that's a schema decision better made once, deliberately, than
retrofitted.

---

Spec revision: 3.1-KESTREL
