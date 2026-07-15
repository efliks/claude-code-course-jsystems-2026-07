---
name: feedback-scoped-git-add-shared-repo
description: In this course repo, multiple agents (frontend/QA/backend) work concurrently on unrelated dirty files — always git add the specific path, never -A, and verify with git status before committing.
metadata:
  type: feedback
---

The HSDC course repo (`claude-code-course-jsystems-2026-07`) is worked on
concurrently by separate frontend/backend/QA agent runs, each with its own
in-progress dirty files outside the current task's scope (e.g. skill config
edits, other areas' uncommitted work). A task instruction explicitly
required: never `git add -A`, always `git add app/backend` (or whatever the
task's specific path is), and confirm scope with `git status` before
committing.

**Why:** an unscoped `git add -A` would have swept in unrelated concurrent
changes (seen in practice: dirty `.claude/skills/create-adr/SKILL.md`,
`skills-lock.json`, untracked `.agents/skills/sqlite-database-expert/`) and
attributed them to a commit that should only be about the backend
persistence layer.

**How to apply:** in this repo (and generally, whenever a task specifies a
sub-path), stage only that exact path explicitly. After staging, run
`git status --porcelain` and `git diff --cached --stat` to confirm nothing
outside the intended scope got picked up before running `git commit`. Also
watch for test-generated runtime artifacts (e.g. a real `data/hsdc.db` file
created by a `@SpringBootTest` that boots the full app context during
`mvnw verify`) — clean those with `git clean -fd -- <path>` (not `rm`, which
is denied in this environment) before staging.
