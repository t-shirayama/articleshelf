---
name: articleshelf-change-sync
description: Keep ArticleShelf implementation, documentation, and verification aligned when changing frontend, backend, API contracts, persistence, UI behavior, project setup, or operational rules. Use this for any code change that may require updates to docs/designs/README.md, docs/specs/README.md, docs/specs/*, docs/architecture/README.md, docs/architecture/*, docs/requirements/backlog/README.md, README.md, or AGENTS.md.
---

# ArticleShelf Change Sync

Use this skill to avoid implementation/documentation drift in ArticleShelf.

## Workflow

1. Scope the change and avoid unrelated cleanup.
2. Check `AGENTS.md` for project rules before editing.
3. Update only directly affected documentation:
   - Requirements or purpose: `docs/requirements/README.md` or `docs/requirements/*`
   - Feature or API behavior: `docs/specs/features/README.md`, `docs/specs/api/README.md`, or `docs/specs/auth/account-api.md`
   - UI behavior or visual rules: `docs/designs/README.md` and usually `docs/specs/ui/README.md`
   - Architecture, persistence, or responsibility boundaries: `docs/architecture/README.md` or `docs/architecture/*`
   - Future tasks, known gaps, temporary workarounds, or technical debt: `docs/requirements/backlog/README.md`
   - Setup, scripts, or onboarding: `README.md`
   - Agent or workflow rules: `AGENTS.md`
   Keep source-of-truth ownership clear: requirements describe what must be true, specs describe current behavior and contracts, architecture describes structure and responsibility boundaries, designs describe visual layout, testing describes verification, and Backlog keeps future work.
4. Remove obsolete code, config, or docs references made unnecessary by the change.
5. Choose verification based on risk:
   - Frontend Vue/CSS changes: run `npm run build` in `frontend`.
   - Backend Java/API changes: use Docker-based Maven, such as `docker compose run --rm backend mvn test`.
   - Hook or script changes: run the script directly with a representative staged or local diff when practical.
6. Report changed behavior, updated docs, verification result, and any remaining follow-up.

## Guardrails

- Do not run local `mvn`; Maven checks should go through Docker.
- Do not add tests automatically during MVP iteration unless the user asks or the risk is high.
- Keep `docs/<area>/` direct children to `README.md` plus responsibility folders, except asset-only folders such as images and generated screenshots. In responsibility folders, keep small topics in `README.md`; split detailed `.md` files only when readers, update reasons, or source-of-truth boundaries differ.
- For docs integrity audits, old path checks, structure checks, and responsibility overlap cleanup, use `.codex/skills/articleshelf-docs-audit/SKILL.md`.
- If docs and implementation disagree, either ask which is authoritative or update the relevant specs, design, or architecture doc. Future follow-up work belongs in `docs/requirements/backlog/README.md`.
- Keep final reports concise, but always mention documentation updates and verification.
