# QA Test Plan — Hardware Service Decision Copilot (MVP)

> **MANUAL VERIFICATION ONLY. NO AUTOMATED E2E.**
> Per project decision (2026-07-15) and `docs/ADR/000-main-architecture.md` §10 ("Scope decision (user): unit + integration tests only — no automated E2E in the MVP"), this plan is executed by hand against the running app. Every "happy path" and "AI decision" round in this plan makes **real calls to OpenRouter** (`HSDC_VISION_MODEL` / `HSDC_DECISION_MODEL`) — no LLM mocking. Expect live-model variance (wording, exact justification phrasing); assert on the *structure and content requirements* in the Acceptance Criteria, not on exact text. All UI text and agent output must be in **English** (AC-31).

**Scope:** `docs/PRD.md` flows 4.1–4.6, Acceptance Criteria AC-01..AC-32; `docs/ADR/000-main-architecture.md` §10 TAC-01..06; ADR-001..004 TAC-00x series; visual/brand fidelity against `docs/design-guidelines.md` and `assets/homepage.png`.

**Fixtures:** `docs/qa/assets/` — see `docs/qa/assets/README.md` for exact byte sizes and purpose of each file.

**Preconditions common to all sections:**
- Backend running (`mvnw spring-boot:run` in `app/backend`), `GET /api/health` returns UP.
- Frontend running (`ng serve` in `app/frontend`), reachable at the dev-server URL, proxying `/api` to the backend.
- `OPENROUTER_API_KEY` set in the environment (never print/log/commit its value).
- Fresh browser tab, cache/local-state cleared, DevTools Network + Console tabs open for inspection where noted.

---

## 1. Flow Procedures

### 1.1 Flow 4.1 — Happy path: Return request approved

**Preconditions:** App loaded at `/`, empty form (Screen 1) visible.

| # | Action | Expected result | AC(s) |
|---|---|---|---|
| 1 | Observe the initial form | Title with product name + one-line explanation; fields present top to bottom: request type, equipment category, model, purchase date, order number (optional), reason, image upload; Submit button visible and disabled | AC-01 |
| 2 | Select request type = **Return** | Reason field label/marker updates to show it is **optional** | AC-03 |
| 3 | Select equipment category from the dropdown | List contains at least: Smartphone, Laptop, Tablet, TV / Monitor, Audio, Peripherals, Gaming console, Smart home, Other | AC-08 |
| 4 | Enter equipment model, e.g. "Sony WH-1000XM5" | Value accepted | AC-01 |
| 5 | Open the date picker and try to pick tomorrow's date | Future dates are disabled/blocked; inline message if attempted via manual entry | AC-04 |
| 6 | Pick a valid past purchase date (e.g. 5 days ago) | Value accepted | AC-01, AC-02 |
| 7 | Enter a **known seeded order number** (see seed data once implemented, e.g. `data.sql` comments) | Value accepted | AC-01 |
| 8 | Leave reason empty (optional for Return) | Submit remains enabled once other required fields are filled — reason not blocking | AC-03 |
| 9 | Upload `docs/qa/assets/valid-photo.jpg` via the file picker or drag-and-drop | Thumbnail preview appears with file name and a remove (×) control | AC-01 |
| 10 | Click Submit | Form is replaced by a full-area progress indicator with staged text ("Uploading photo…", "Analyzing image…", "Making decision…"); no input possible during this | PRD §9 Screen 1 loading state |
| 11 | Wait for processing to complete (live OpenRouter call, may take up to ~30 s) | UI switches to the Chat view (Screen 2) | — |
| 12 | Inspect the DevTools Network tab for the `POST /api/cases` request payload | Payload size for the compressed image is smaller than the original 45 KB source is already small — repeat this check with a larger real photo if available, or trust backend unit tests for AC-09; at minimum confirm the request completed with 201 | AC-09 |
| 13 | Read the first chat bubble | From the agent; contains: greeting, decision (visually distinguished badge/heading), justification section referencing a concrete return-policy rule, next-steps section, and the mandatory disclaimer sentence verbatim: *"This is an automated recommendation based on our published policies. It does not limit your statutory consumer rights."* | AC-16, AC-18, AC-32 |
| 14 | Confirm the decision shown is exactly one of Approve / Reject / Needs more info | For this clean happy-path case, expect **Approve** | AC-12 |
| 15 | Confirm the message references the order number as verified (no "could not be verified" note) | Purchase history was found and used | AC-14, AC-15 |
| 16 | Type a follow-up question in the chat input, e.g. "How do I ship the item back?" and Send | Agent reply is generated with full context (references the case's model/category correctly); reply streams in progressively | AC-19 |
| 17 | While the reply is streaming, observe the input row | Visible pending/typing indicator shown; Send is disabled/blocked until the reply completes or fails | AC-21 |
| 18 | Click **New case** | Confirmation prompt appears ("This will end the current case") | PRD §9 Screen 2 |
| 19 | Confirm the prompt | Returns to an empty Screen 1 form; the previous session's record remains stored (verified in §1.7 persistence checks) | AC-24 |

### 1.2 Flow 4.2 — Complaint request — rejected

**Preconditions:** Empty form.

| # | Action | Expected result | AC(s) |
|---|---|---|---|
| 1 | Select request type = **Complaint** | Reason field label/marker updates to show it is **required** | AC-03 |
| 2 | Fill category, model, a purchase date well within any warranty window, leave order number empty | Values accepted | AC-01, AC-02 |
| 3 | Try to submit with reason left empty | Submission blocked; inline required-field message for reason | AC-02, AC-03 |
| 4 | Fill reason describing damage plausibly caused by a drop (e.g. "Screen is cracked, phone was dropped") | Value accepted | AC-01 |
| 5 | Upload `docs/qa/assets/valid-photo.jpg` (stand-in for a damage photo) | Thumbnail preview shown | AC-01 |
| 6 | Submit | Staged progress indicator, then Chat view | — |
| 7 | Read the first chat bubble | Decision is **Reject** (given the drop-damage narrative and policy exclusion), justification cites a specific complaint-policy rule, next steps mention an alternative (e.g. paid repair), disclaimer present | AC-12, AC-16, AC-18, AC-32 |
| 8 | Confirm the analysis used the complaint-specific prompt path (behaviorally: decision discusses damage type/cause, not resale condition) | Wording is damage/cause-oriented, not "signs of usage/resellable" | AC-10, AC-13 |

### 1.3 Flow 4.3 — Ambiguous case — needs more info, revised in chat

**Preconditions:** Empty form.

| # | Action | Expected result | AC(s) |
|---|---|---|---|
| 1 | Fill a Complaint form (category, model, past date, reason e.g. "Cracked by itself") | Values accepted | AC-01, AC-02, AC-03 |
| 2 | Upload `docs/qa/assets/tiny-unusable.jpg` (near-black, low-detail) | Thumbnail preview shown | AC-01 |
| 3 | Submit | Staged progress, then Chat view | — |
| 4 | Read the first chat bubble | Decision is **Needs more info**; message states exactly what is missing (e.g. a clearer photo of the damage/charging port) rather than guessing | AC-12, AC-17 |
| 5 | In chat, provide the missing information as text (e.g. "The crack is on the top-left corner, no signs of impact elsewhere, screen still works") | Agent processes the new info | AC-19 |
| 6 | Observe the agent's reply | Agent posts a **revised decision** (Approve or Reject) in the same rich format as the first message (badge, justification, next steps, disclaimer) | AC-20, AC-16, AC-32 |
| 7 | Confirm which decision is "current" | The most recent decision message is understood as the valid decision of the case (no earlier "Needs more info" badge overrides it) | AC-20 |

### 1.4 Flow 4.4 — Error path: unusable image

**Preconditions:** Empty form.

| # | Action | Expected result | AC(s) |
|---|---|---|---|
| 1 | Fill a valid Return or Complaint form | Values accepted | AC-01, AC-02 |
| 2 | Upload `docs/qa/assets/tiny-unusable.jpg` | Preview shown (client accepts it — it's a valid JPEG under 5 MB, just poor content) | AC-05, AC-06 |
| 3 | Submit | Staged progress, then Chat view | — |
| 4 | Read the first chat bubble | Vision model reports the image as unusable/not showing electronics equipment; decision is **Needs more info**; message explicitly asks for a better photo or description of the item's condition | AC-17 |
| 5 | Continue in chat as in flow 4.3 (provide a text description) | Same revision behavior as §1.3 | AC-19, AC-20 |

*Note: model behavior on the "unusable" judgment is probabilistic (live LLM). If the model instead analyzes the tiny image successfully, retry with an even more degraded fixture or document the observed behavior — do not treat as an automatic fail without one retry.*

### 1.5 Flow 4.5 — Error path: service failure

**Preconditions:** A way to force an LLM failure — e.g. temporarily set `OPENROUTER_API_KEY` to an invalid value, or set `HSDC_VISION_MODEL`/`HSDC_DECISION_MODEL` to a non-existent model ID, then restart the backend. **Restore the correct key/model and restart before any other section.**

| # | Action | Expected result | AC(s) |
|---|---|---|---|
| 1 | Fill a valid form completely, including `docs/qa/assets/valid-photo.png` | Values accepted | AC-01 |
| 2 | Submit | Staged progress begins | — |
| 3 | Wait for the forced failure to surface | UI shows an error panel: "We couldn't process your request. Please try again." with a **Retry** action; a secondary "Back to form" link is available | AC-22 |
| 4 | Inspect the form/image state (via "Back to form" or directly) | All previously entered field values and the uploaded image are preserved — nothing needs re-entry | AC-22, AC-07 |
| 5 | Restore valid `OPENROUTER_API_KEY`/model config, restart backend, click **Retry** | Processing re-runs from the failed step using the same payload and completes normally (Chat view reached) | AC-22 |

### 1.6 Flow 4.6 — Validation error path

**Preconditions:** Empty form.

| # | Action | Expected result | AC(s) |
|---|---|---|---|
| 1 | Click Submit with all fields empty | Submit is disabled (cannot even attempt) or, if enabled prematurely, submission is blocked with inline messages under each empty required field | AC-02 |
| 2 | Fill all required fields except leave category unselected, attempt submit | Blocked; inline message under category field | AC-02 |
| 3 | Fill all fields; upload `docs/qa/assets/wrong-format.gif` | Client-side rejection immediately on file selection: message names the allowed formats (JPEG/PNG) | AC-05 |
| 4 | Replace with `docs/qa/assets/oversized.jpg` (5.68 MB) | Client-side rejection: message names the 5 MB limit | AC-06 |
| 5 | If client-side validation is somehow bypassed (e.g. via direct API call/DevTools), send the same oversized/wrong-format payload to `POST /api/cases` | Server-side rejection with the same rule, HTTP 400, machine-readable error code (`IMAGE_TOO_LARGE` / `VALIDATION_ERROR`) | AC-05, AC-06 |
| 6 | After any of the above validation failures, check all other previously entered field values | All preserved, including the (rejected) file selection state where applicable; first invalid field scrolled into view | AC-07 |
| 7 | Select a future purchase date (bypass the date-picker's disabled state if possible, e.g. via keyboard entry) | Submission blocked with an inline message | AC-04 |

---

## 2. Cross-Cutting Checks (not tied to one flow)

| # | Check | Steps | AC(s) |
|---|---|---|---|
| 1 | Order number miss doesn't block | Submit a valid form with an order number that matches no seeded record (e.g. `ORD-UNKNOWN-999`) | Case still processes to a decision; first chat message states the order could not be verified | AC-15 |
| 2 | Decision categories never exceed the 3-value set | Across all flows executed above, note every decision badge seen | Always exactly Approve / Reject / Needs more info — never any other label | AC-12 |
| 3 | Chat context correctness | In any active chat, ask "What device/model is this case about?" | Agent answers correctly from form data without the customer repeating it | AC-19 |
| 4 | Chat message failure + resend | Force a chat-turn failure (e.g. invalid API key temporarily, as in §1.5, but trigger it from an already-open chat) | The failed customer message is marked as failed with a Resend affordance; prior conversation history is not lost; after restoring config, Resend succeeds | AC-23 |
| 5 | Reload does not restore session | After reaching the Chat view, reload the browser tab | Returns to an empty Screen 1 form; no chat/form data restored client-side | AC-25 |
| 6 | Language check | Skim every screen, every decision message, every error message, every validation message produced during this plan | 100% English text, no other language leaking in | AC-31 |

---

## 3. Acceptance Criteria Coverage Matrix

| AC | Description (short) | Verified in |
|---|---|---|
| AC-01 | Exact field set on the form | §1.1 steps 1–9 |
| AC-02 | Required fields block submission | §1.1 step 1; §1.6 steps 1–2 |
| AC-03 | Reason required only for Complaint | §1.1 steps 2, 8; §1.2 steps 1, 3 |
| AC-04 | Purchase date cannot be future | §1.1 step 5; §1.6 step 7 |
| AC-05 | Non-JPEG/PNG rejected client+server | §1.6 steps 3, 5 |
| AC-06 | > 5 MB rejected client+server | §1.6 steps 4, 5 |
| AC-07 | Field values preserved after validation error | §1.6 step 6; §1.5 step 4 |
| AC-08 | Category list contains required options | §1.1 step 3 |
| AC-09 | Image compressed before LLM call | §1.1 step 12 |
| AC-10 | Complaint uses complaint analysis prompt | §1.2 step 8 |
| AC-11 | Return uses return analysis prompt | §1.1 step 13 (behavioral) |
| AC-12 | Decision is exactly one of 3 categories | §1.1 step 14; §2 check 2 |
| AC-13 | Separate decision prompts + policy docs per type | §1.2 step 8 |
| AC-14 | Decision context includes form+analysis+purchase history | §1.1 steps 7, 15 |
| AC-15 | Unverified order doesn't block, is noted | §2 check 1 |
| AC-16 | Justification + next-steps sections present | §1.1 step 13; §1.2 step 7 |
| AC-17 | Unusable image → Needs more info with explanation | §1.3 step 4; §1.4 step 4 |
| AC-18 | First chat message format | §1.1 step 13 |
| AC-19 | Follow-up messages use full context | §1.1 step 16; §2 check 3 |
| AC-20 | Revised decision on new info, latest is valid | §1.3 steps 6–7 |
| AC-21 | Pending indicator, second send blocked | §1.1 step 17 |
| AC-22 | Service failure → error panel + Retry, data kept | §1.5 |
| AC-23 | Chat failure → marked failed + Resend, history kept | §2 check 4 |
| AC-24 | New case action available and works | §1.1 steps 18–19 |
| AC-25 | Reload starts fresh UI session | §2 check 5 |
| AC-26 | Successful submission persists full session record | §4 step 1 |
| AC-27 | Every decision persisted with category/justification/timestamp | §4 step 2 |
| AC-28 | Every chat message persisted with sender/content/timestamp | §4 step 3 |
| AC-29 | Persistence failure doesn't block customer flow | §4 step 5 |
| AC-30 | Records survive app restart | §4 step 4 |
| AC-31 | All UI/agent text is English | §2 check 6 |
| AC-32 | Disclaimer on every decision message | §1.1 step 13; §1.2 step 7; §1.3 step 6 |

**Coverage: 32 / 32 ACs mapped to at least one step.**

---

## 4. Session Persistence Checks (SQLite)

Requires shell access to the SQLite file at `HSDC_DB_PATH` (default `./data/hsdc.db`), e.g. via `sqlite3` CLI or a JDBC browser tool. Run after completing §1.1–§1.3 so there is data to inspect.

| # | Check | How | AC(s) |
|---|---|---|---|
| 1 | Case session persisted | `SELECT * FROM case_session ORDER BY created_at DESC LIMIT 1;` | Row has all form field values, non-empty `image_analysis`, non-null `created_at` | AC-26 |
| 2 | Decision(s) persisted | `SELECT * FROM decision WHERE case_id = '<id>' ORDER BY created_at;` | One row per decision issued (initial + any revision), each with category, justification, timestamp | AC-27 |
| 3 | Chat messages persisted | `SELECT * FROM chat_message WHERE case_id = '<id>' ORDER BY created_at;` | One row per customer and agent message, correct `sender`, `content`, timestamp | AC-28 |
| 4 | Restart durability | Stop the backend, restart it (`mvnw spring-boot:run`), re-run the same `SELECT` queries against the same DB file | All rows from before restart still present; second-start schema init doesn't duplicate seed rows | AC-30, TAC-004-01 |
| 5 | Persistence failure tolerance | Best-effort: temporarily rename/lock the DB file (or otherwise force a write failure) mid-session, submit a case, then restore the file | The customer-facing flow still completes (chat view reached, decision shown) even though persistence failed; check backend logs for an ERROR entry referencing the case id — no exception surfaces to the customer | AC-29 |

---

## 5. Technical Acceptance Criteria (TAC) Checklist

### Main architecture (`ADR-000` §10)

- [ ] **TAC-01** — `mvnw verify` (backend, in `app/backend`) and `ng test` (frontend, in `app/frontend`) both exit 0 with zero failures.
- [ ] **TAC-02** — Backend integration tests make no real network calls: grep test config/source for the WireMock base-URL override (e.g. `wiremock`/stub base URL) and confirm no test references `openrouter.ai` directly; optionally run tests with network disabled and confirm they still pass.
- [ ] **TAC-03** — With live OpenRouter models, time `POST /api/cases` from Submit click to Chat view render across a few of the manual runs above; p95 informally < 30 s (demo-grade, not a load test).
- [ ] **TAC-04** — Delete `./data/hsdc.db` (backend stopped), start the backend, confirm the file is created automatically with schema + seed rows; restart again and confirm row counts in `customer`/`purchase` are unchanged (no duplicate seeding).
- [ ] **TAC-05** — Stop the backend while the frontend is running; attempt to submit the form; confirm the PRD's service-failure UI state appears (not a raw browser error) and the browser DevTools Console shows no unhandled/uncaught JS errors.
- [ ] **TAC-06** — Search backend logs (all levels enabled) and any HTTP response bodies produced during this test session for the literal `OPENROUTER_API_KEY` value; confirm it never appears; confirm it is not present in any file under version control (`git grep` for the key value, or for `sk-or-` prefix, should return nothing outside `.env`/local config that's gitignored).

### Backend (`ADR-001`)

- [ ] **TAC-001-01** — `mvnw verify` passes; confirm (via test source inspection) every integration test's SDK client is configured against the WireMock base URL, not a real host.
- [ ] **TAC-001-02** — Every `ErrorResponse.code` value the handler can emit (`VALIDATION_ERROR`, `IMAGE_TOO_LARGE`, `LLM_UNAVAILABLE`, `CASE_NOT_FOUND`, plus any others) is covered by at least one test asserting status + code; cross-check against `GlobalExceptionHandler` source.
- [ ] **TAC-001-03** — Start the backend with only `OPENROUTER_API_KEY` set (unset all `HSDC_*` overrides); confirm `GET /api/health` returns UP.
- [ ] **TAC-001-04** — Temporarily point `HSDC_POLICIES_DIR` at a non-existent directory, start the backend; confirm it fails fast at startup with a readable error message (not a silent partial start).
- [ ] **TAC-001-05** — Upload a 5.00 MB JPEG (accepted) and a 5.01 MB JPEG (rejected with `IMAGE_TOO_LARGE`) — boundary test; can reuse/derive from `oversized.jpg` by trimming, or generate two boundary-sized fixtures ad hoc.

### LLM integration (`ADR-002`)

- [ ] **TAC-002-01** — During a full test run (this plan), confirm via a network monitor (e.g. Wireshark, or backend outbound-connection logging) that the only non-localhost host contacted is `openrouter.ai` — no test run opens sockets elsewhere. (In automated integration tests specifically: no host but WireMock/localhost.)
- [ ] **TAC-002-02** — Inspect the 4 prompt classpath resources (`complaint-analysis`, `return-analysis`, `complaint-decision`, `return-decision`); confirm all exist, are non-empty, and each of the two decision prompts contains the disclaimer sentence exactly once, verbatim: *"This is an automated recommendation based on our published policies. It does not limit your statutory consumer rights."*
- [ ] **TAC-002-03** — With all `HSDC_*_MODEL`/`OPENROUTER_BASE_URL` env vars unset, confirm effective config equals the documented defaults (`openai/gpt-4o-mini` vision, `openai/gpt-4o` decision, `https://openrouter.ai/api/v1`); set overrides and confirm they take effect (e.g. via a debug/actuator config endpoint or log line at startup).
- [ ] **TAC-002-04** — Enable DEBUG logging for a full pipeline run (form submit + chat); grep the log output for the API key value; confirm zero occurrences.
- [ ] **TAC-002-05** — Confirm (code inspection + the manual runs above) that `DecisionResult.category` is backed by a real enum type — no case observed or reachable in code where an out-of-schema string could reach the UI as a "decision".

### Frontend (`ADR-003`)

- [ ] **TAC-003-01** — `ng test` passes headless (no watch mode) on the course VM.
- [ ] **TAC-003-02** — `ng build` (production) succeeds with zero errors; `ng lint` reports no errors.
- [ ] **TAC-003-03** — With the backend running, exercise the full flow via `ng serve` + proxy; confirm no CORS errors in DevTools Console and `grep -r "http://localhost:8080" app/frontend/src` returns nothing.
- [ ] **TAC-003-04** — Resize the browser (or DevTools device toolbar) to 375 px width and to 1280 px width; confirm both Screen 1 and Screen 2 render usably (no overlapping/cut-off content, controls reachable) at both widths.
- [ ] **TAC-003-05** — Cross-check every backend `code` value from TAC-001-02 against `ErrorMapper` source/tests; confirm each has a mapped UI behavior (inline / error panel+Retry / failed-bubble+Resend / redirect+snackbar as appropriate).

### Persistence (`ADR-004`)

- [ ] **TAC-004-01** — Fresh checkout, `mvnw spring-boot:run`: confirm `./data/hsdc.db` is created with all 5 tables (`case_session`, `decision`, `chat_message`, `customer`, `purchase`) and seed rows present; restart and confirm row counts in `customer`/`purchase` are identical.
- [ ] **TAC-004-02** — Confirm (test source inspection) all repository integration tests run against real SQLite temp files — no H2 or other in-memory-DB substitute anywhere in backend test config.
- [ ] **TAC-004-03** — Inspect `data.sql` seed comments/dates; confirm seed purchases exist covering: within 14 days, outside 14 days, within 24 months, outside 24 months relative to the documented fixed reference date.
- [ ] **TAC-004-04** — Stop the app, delete the DB file, restart; confirm the app comes up working with fresh seeds (documented recovery path holds).
- [ ] **TAC-004-05** — `git grep` / code review over repository source for string-concatenated SQL (e.g. `"SELECT * FROM " + table` patterns); confirm all `JdbcClient` calls use bound parameters, not concatenation.

---

## 6. Visual / Brand Comparison Checklist

Reference: `docs/design-guidelines.md` (Colors + Components tables) and `assets/homepage.png` (Play visual reference). Compare the running app side-by-side with the screenshot.

- [ ] **Primary purple `#6C43BF`** used on the Submit button and any primary CTA (New case, Retry, Send) — sample the rendered color via DevTools "Inspect" / color picker and confirm it matches `#6C43BF` (or its documented hover-darken toward `#48227C`).
- [ ] **Typography base weight 500** — confirm body text and button labels use Manrope at weight 500 (not the browser-default 400); check computed `font-family` includes `Manrope` and computed `font-weight` is `500` for body copy via DevTools.
- [ ] **Button radius 7px** — inspect Submit / Send / New case / Retry buttons; computed `border-radius` should be `7px` (the "signature radius" per design-guidelines.md).
- [ ] **White canvas (`#FFFFFF`) background** — confirm the page background is white (or `#FAFAFA`/`#F5F5F5` for raised sections/cards), not a default Material theme color.
- [ ] **Text colors** — primary text approximates `#1F1F1F`, secondary/hint text approximates `#707070`/`#999999` — check form field hints, disclaimer text, timestamps.
- [ ] **Correct logo usage** — if the app header/branding includes the Play logo (`assets/logo.svg`), confirm it's used per guidelines: white wordmark on the `#2D0066` plate on light backgrounds (or recolored glyph variant), correct aspect ratio (125:40), adequate clear space, not stretched.
- [ ] **Secondary buttons** (e.g. "Back to form") — white background, `#6C43BF` text, same 7px radius, no border unless on a colored background.
- [ ] **No stray accent colors** — magenta `#E6144B` and yellow `#FFF200` should not appear as body-text colors; if used at all, only as small decorative/promo accents (unlikely to appear in this app's flows — flag if seen).
- [ ] **Overall gestalt vs `assets/homepage.png`** — softly rounded purple CTAs, generous whitespace, near-black Manrope text, confident-but-not-flashy retail feel; note any stock Angular Material blue/default-theme leakage as a bug.
