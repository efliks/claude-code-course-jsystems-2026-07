---
name: project-hsdc-backend-stack
description: HSDC course project backend is Java 21 + Spring Boot 3.5.x + Maven under app/backend, not the Python default implied by root AGENTS.md.
metadata:
  type: project
---

The "Hardware Service Decision Copilot" (HSDC) course project's backend
(`app/backend`) is **Java 21 + Spring Boot 3.5.16 + Maven**, package
`com.jsystems.hsdc`, built via the Maven wrapper (`./mvnw` / `.\mvnw.cmd`).
Persistence is hand-written SQL over Spring `JdbcClient` against a single
**SQLite** file (`HSDC_DB_PATH`, default `./data/hsdc.db`) — no JPA, no
Flyway, no H2 in tests. All backend text is **English** (this overrides the
otherwise Polish-first project default — confirmed by an explicit task
instruction, not just inferred).

**Why:** root `AGENTS.md` says "primary demo stack: Python" because the
language/stack is chosen live per course group; this particular group
picked Java/Spring for the backend, documented in `docs/ADR/004-persistence-sqlite.md`
and `docs/ADR/000-main-architecture.md`.

**How to apply:** before touching `app/backend`, read the relevant
`docs/ADR/00X-*.md` file — do not assume Python/FastAPI/Django patterns.
Commits under this area use the `Backend:` prefix per `AGENTS.md`. See also
[[feedback_scoped_git_add_shared_repo]] — this repo is shared with
concurrent frontend/QA agents, so stage only `app/backend/**` explicitly.

LLM integration (B4, `com.jsystems.hsdc.llm`) uses `com.openai:openai-java:4.43.0`
(Chat Completions API, not Responses) via OpenRouter, sole owner
`LlmService`. Default models were corrected as of 2026-07-15: OpenAI
retired `gpt-4o`/`gpt-4o-mini` from OpenRouter; current defaults are
`openai/gpt-5.6-luna` (vision, `HSDC_VISION_MODEL`) and
`openai/gpt-5.6-terra` (decision/chat, `HSDC_DECISION_MODEL`) — see
[[feedback_verify_llm_apis_via_jar_decompile]]. Don't reintroduce the old
gpt-4o defaults from memory/training data; re-verify against the live
OpenRouter catalog if picking new models. WireMock tests need
[[feedback_wiremock_3_13_needs_jetty12_module]].
