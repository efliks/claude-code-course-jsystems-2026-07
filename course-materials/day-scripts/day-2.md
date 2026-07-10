# Day 2 — Od pomysłu do projektu
### Temat: AI jako Twój Product Manager i UX Designer

**Goal of the day:** Decide the project → generate PRD → choose stack (ADR) → configure agents (MCP, skills, sub-agents).

---

## 09:00–10:30 — Moduł 2.1: AI pomaga doprecyzować wymagania

**Key points:**
- Brainstorm: what app does the group want? (electronics returns agent is the default)
- Use the agent as Product Manager: it asks YOU questions to clarify requirements
- Generate a PRD (Product Requirements Document) with user stories
- Validate assumptions with Deep Research (ChatGPT, Perplexity, Gemini)

**Demo:** Live PRD generation with the agent — use a prompt like `../prompts/day-2/PRD-generation-prompt-promtcowboy.md`

**Outcome:** A PRD saved to `docs/PRD-Product-Requirements-Document.md`.

---

## 10:30–11:00 — Moduł 2.2 (start): Advanced Research & planowanie

**Key points:**
- Research libraries/frameworks with the agent (Context7 MCP for docs)
- Project structure, data model, app flow
- Architecture diagrams, view descriptions, wireframes (generate images with ChatGPT/Gemini)

---

## ☕ 11:00–11:15 — BREAK (15 min)

---

## 11:15–12:30 — Moduł 2.2 (cont): Tech stack decision via ADR

**Key points:**
- Compare tech stack options (TS/Next.js vs Java/Spring Boot vs other)
- Generate ADR (Architecture Decision Record) — the agent researches and recommends
- Choose boilerplate (create-next-app, AI SDK starter, etc.)
- Initialize project in `/app`

**Demo:** Live ADR generation — agent compares options, you decide together

**Outcome:** Tech stack chosen, recorded in `docs/ADR/`, project scaffolded.

---

## 12:30–13:00 — Moduł 2.3 (start): MCP — Model Context Protocol

**Key points:**
- What is MCP? Servers and tools for agents
- Context7 MCP: docs-aware coding (resolve library IDs, query docs)
- IntelliJ MCP (if Java participants) or other MCP servers
- Configure MCP in `.mcp.json` / Claude settings

**Demo:** Show Context7 in action — ask about a library, get real docs

**Links:** `../references/handy-computer-mini-module.md` (optional); `../AGENTS.md/microsoft-mcp-AGENTS.md`

---

## 🍽️ 13:00–13:30 — LUNCH (30 min)

---

## 13:30–14:45 — Moduł 2.3 (cont): Skills, Sub-Agents, AGENTS.md

**Key points:**
- **Skills:** agent capabilities (e.g. Playwright browser automation skill)
- **Sub-Agents:** configure specialists (be-developer, fe-developer, qa-engineer)
  - Show `.claude/agents/*.md` definitions
  - Delegate a task to a sub-agent live
- **AGENTS.md / CLAUDE.md:** rules, conventions, coding standards
  - How to write effective AGENTS.md (TDD rules, verification, commit rules)
  - Compare: weak AGENTS.md vs strong AGENTS.md → see the difference in agent output

**Demo:** Sub-agent delegation — ask be-developer to scaffold an API route

**Links:** `../prompts/day-2/d2-e3-task-slicing-starter.md` (task decomposition exercise); `../Prompt examples/Plan-SubAgents-matrix-dependency-map.md`

**Outcome:** Agents configured with MCP, skills, sub-agents. Ready to build.

---

## *15:00–15:10 — Optional short break*

---

## 15:10–16:00 — Day 2 wrap-up

**Key points:**
- Recap: PRD → research → ADR → scaffold → agent config
- Preview Day 3: building the core app (design system, full-stack generation, debugging)
- Q&A
- Homework (optional): review the PRD and ADR, think about UI design

**Outcome:** Solid foundation to start building tomorrow.
