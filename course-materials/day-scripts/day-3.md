# Day 3 — Budowa rdzenia aplikacji
### Temat: AI jako Twój Pair Programmer

**Goal of the day:** Design system created → backend API built → frontend connected → first working version.

---

## 09:00–10:30 — Moduł 3.1: AI UX Researcher — Design System & Tokens

**Key points:**
- Use Playwright (Skill or MCP) to analyze a reference website
- Extract design tokens: colors, fonts, spacing, button styles, shadows, radii
- Save `assets/design-tokens.json`, logo, favicon
- Generate design system doc → `docs/design-guidelines.md`
- Build first UI components from tokens (NO v0/Lovable yet — that's Day 5)

**Demo:** Live Playwright analysis of a reference site → token extraction → component generation

**Links:** `../Prompt examples/Design System reverse-engineering with Playwright.md`; `../prompts/day-3/day-3-prompt-pack.md`

**Outcome:** Design system + tokens + first components ready.

---

## 10:30–11:00 — Moduł 3.2 (start): Full-Stack Generation

**Key points:**
- Generate backend API with the agent (routes, models, services)
- TDD approach: agent writes tests first, then implementation
- Connect to SQLite (simple, no Docker/PostgreSQL needed for PoC)

---

## ☕ 11:00–11:15 — BREAK (15 min)

---

## 11:15–12:30 — Moduł 3.2 (cont): Multi-Agent Power

**Key points:**
- Delegate backend tasks to `be-developer` sub-agent
- Delegate frontend tasks to `fe-developer` sub-agent
- Parallel work: two agents working simultaneously (git worktrees or separate terminals)
- Analyze codebase with agent in CLI and Desktop App side-by-side

**Demo:** Two terminal windows — be-developer and fe-developer working in parallel

**Outcome:** Backend API connected to frontend.

---

## 12:30–13:00 — Moduł 3.3 (start): AI-Assisted Debugging

**Key points:**
- When something breaks: paste stack trace to the agent
- Agent diagnoses, suggests fix, you approve
- Feedback loop: fix → test → iterate

---

## 🍽️ 13:00–13:30 — LUNCH (30 min)

---

## 13:30–14:45 — Moduł 3.3 (cont): Refactoring & first working version

**Key points:**
- Refactor with the agent: improve code quality, extract components
- Manage context during long sessions (context rot awareness)
- Goal: a **first working version** of the app — form submission → AI analysis → chat response
- Use the Ralph Wiggum Bash Loop technique if sessions get long

**Demo:** Live debugging session — encounter a real bug, fix it with the agent

**Links:** `../how-to-ralph-wiggum/README.md` (context rot prevention); `../Research/Sandboxing-and-harness-in-Codex-vs-ClaudeCode.md`

**Outcome:** First working version of the app running locally.

---

## *15:00–15:10 — Optional short break*

---

## 15:10–16:00 — Day 3 wrap-up

**Key points:**
- Demo: show the working app to the group
- What worked well? What was hard?
- Preview Day 4: testing, code review, security, CI/CD
- Q&A

**Outcome:** Working app. Confidence that the agent-driven workflow produces results.
