---
name: fleet-concurrent-agent-writes
description: In this course's multi-agent fleet sessions, another fe-developer instance can be completing the same task concurrently on the same working directory — always re-check git log/status before assuming work is still needed.
metadata:
  type: feedback
---

During F0 (scaffold Angular frontend into `app/frontend`), while this agent was mid-task (fixing a corrupted `@angular/cli` install / stray `find-up` dependency), a parallel process (visible via `git log` as commits from `be-developer` and `qa-engineer` interleaved with frontend commits, all under the same git author) independently completed and committed the exact same two commits this agent was about to make — same messages, same scope, including this agent's in-progress `find-up` fix (the working tree had picked up the concurrent process's `npm install` runs).

**Why:** This project runs as a multi-agent fleet (be-developer, qa-engineer, fe-developer working the same repo/branch in the same session). Task briefs can be dispatched more than once, or a coordinator can run the same step redundantly. Discovering this only via `git status` showing "already clean" after finishing local edits was the tell — a naive agent would have proceeded to `git add && git commit` and produced a duplicate/conflicting commit.

**How to apply:** Before staging/committing at the end of any task in this repo, run `git log --oneline -N -- <scope>` and `git status --porcelain <scope>` first. If the exact expected commit(s) already exist with matching content, do not re-commit — just verify (build/test) at HEAD and report that the work was already landed. Don't assume tool-call history is the only source of truth for repo state; another concurrent agent's writes are real and can appear between your own tool calls.
