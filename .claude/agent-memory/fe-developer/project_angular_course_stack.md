---
name: project-angular-course-stack
description: This course project's actual frontend stack is Angular 22 + Material (not React/TS), English UI text — overrides the generic fe-developer agent defaults.
metadata:
  type: project
---

For `claude-code-course-jsystems-2026-07`, the ADR-driven decision (see `docs/ADR/003-frontend-angular.md`) is **Angular 22 + Angular Material**, standalone components, SCSS, signals-based state (no NgRx), `*.spec.ts` (Jasmine/Angular-CLI convention, run via `ng test`), and **all user-facing text in English** (PRD AC-31) — not Polish.

**Why:** The generic fe-developer agent definition/system prompt defaults to React/TypeScript + Polish UI text, which is wrong for this specific project. The project's own CLAUDE.md/AGENTS.md and task briefs explicitly override this per-conversation, but it's easy to default back to the generic assumption if the override text is skimmed.

**How to apply:** Before starting any frontend task in this repo, re-check `docs/ADR/003-frontend-angular.md` §3 (structure) and §6 (technical decisions: ngx-sse-client for SSE-over-POST, ngx-markdown + Material primitives for chat UI, signals not NgRx) rather than assuming the generic stack. See [[env-node-angular22-engine-mismatch]] and [[fleet-concurrent-agent-writes]] for environment-specific gotchas hit during F0 (scaffold).
