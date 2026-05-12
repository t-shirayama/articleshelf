---
name: articleshelf-change-sync
description: Keep ArticleShelf implementation, documentation, and verification aligned when changing frontend, backend, API contracts, persistence, UI behavior, project setup, or operational rules. Use this for any code change that may require updates to docs/designs/README.md, docs/specs/README.md, docs/specs/*, docs/architecture/README.md, docs/architecture/*, docs/requirements/backlog/README.md, docs/requirements/backlog/*, README.md, AGENTS.md, or ArticleShelf skills.
---

# ArticleShelf Change Sync

Use this skill to avoid implementation/documentation drift in ArticleShelf.
After the initial release, treat changes as maintenance, improvement, Backlog execution, or quality work unless the user explicitly says otherwise.

## Workflow

1. Scope the change and avoid unrelated cleanup.
2. Check `AGENTS.md` for project rules before editing.
3. Update only directly affected documentation:
   - Requirements or purpose: `docs/requirements/README.md` or `docs/requirements/*`
   - Feature or API behavior: `docs/specs/features/README.md`, `docs/specs/api/README.md`, or `docs/specs/auth/account-api.md`
   - UI behavior or visual rules: `docs/designs/README.md` and usually `docs/specs/ui/README.md`
   - Architecture, persistence, or responsibility boundaries: `docs/architecture/README.md` or `docs/architecture/*`
   - Future tasks, known gaps, temporary workarounds, TODOs, or technical debt: `docs/requirements/backlog/pending/` as one file per task with a `P0`-`P4` priority, plus the relevant backlog README indexes
   - Setup, scripts, or onboarding: `README.md`
   - Agent or workflow rules: `AGENTS.md` and, when the operating rule changes, the relevant `.codex/skills/` files
   Keep source-of-truth ownership clear: requirements describe what must be true, specs describe current behavior and contracts, architecture describes structure and responsibility boundaries, designs describe visual layout, testing describes verification, and Backlog keeps future work.
4. Remove obsolete code, config, or docs references made unnecessary by the change.
5. Choose verification based on risk:
   - Docs-only changes: run link/path checks and `git diff --check`; build, unit, integration, and E2E checks are not required.
   - Frontend Vue/CSS changes: run `npm run build` in `frontend` when practical.
   - Backend Java/API changes: use Docker-based Maven, such as `docker compose run --rm backend mvn test`.
   - Hook or script changes: run the script directly with a representative staged or local diff when practical.
   - Before running pre-push E2E smoke or Playwright checks that start the dedicated stack, ensure `http://localhost:4173` and `http://localhost:18080` are not already occupied by a dev server or stale E2E stack. Use `PLAYWRIGHT_USE_EXISTING_SERVER=1` only for an intentionally prepared E2E server.
6. Report changed behavior, updated docs, verification result, and any remaining follow-up.

## Guardrails

- Do not run local `mvn`; Maven checks should go through Docker.
- Do not add tests automatically for low-risk maintenance changes unless the user asks, existing coverage must be updated, or the risk is high.
- Keep `docs/<area>/` direct children to `README.md` plus responsibility folders, except asset-only folders such as images and generated screenshots. In responsibility folders, keep small topics in `README.md`; split detailed `.md` files only when readers, update reasons, or source-of-truth boundaries differ.
- For docs integrity audits, old path checks, structure checks, and responsibility overlap cleanup, use `.codex/skills/articleshelf-docs-audit/SKILL.md`.
- If docs and implementation disagree, either ask which is authoritative or update the relevant specs, design, or architecture doc. Future follow-up work belongs in `docs/requirements/backlog/pending/` as one file per task.
- Keep final reports concise, but always mention documentation updates and verification.
