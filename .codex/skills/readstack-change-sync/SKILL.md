---
name: readstack-change-sync
description: Keep ReadStack implementation, documentation, and verification aligned when changing frontend, backend, API contracts, persistence, UI behavior, project setup, or operational rules. Use this for any code change that may require updates to docs/design/README.md, docs/specification/README.md, docs/specification/*, docs/architecture/README.md, docs/status/project-status.md, README.md, or AGENTS.md.
---

# ReadStack Change Sync

Use this skill to avoid implementation/documentation drift in ReadStack.

## Workflow

1. Scope the change and avoid unrelated cleanup.
2. Check `AGENTS.md` for project rules before editing.
3. Update only directly affected documentation:
   - Requirements or purpose: `docs/requirements/README.md` or `docs/requirements/*`
   - Feature or API behavior: `docs/specification/README.md` or `docs/specification/*`
   - UI behavior or visual rules: `docs/design/README.md` and usually `docs/specification/ui.md`
   - Architecture, persistence, or responsibility boundaries: `docs/architecture/README.md`
   - Known gaps, temporary workarounds, or technical debt: `docs/status/project-status.md`
   - Setup, scripts, or onboarding: `README.md`
   - Agent or workflow rules: `AGENTS.md`
4. Remove obsolete code, config, or docs references made unnecessary by the change.
5. Choose verification based on risk:
   - Frontend Vue/CSS changes: run `npm run build` in `frontend`.
   - Backend Java/API changes: use Docker-based Maven, such as `docker compose run --rm backend mvn test`.
   - Hook or script changes: run the script directly with a representative staged or local diff when practical.
6. Report changed behavior, updated docs, verification result, and any remaining follow-up.

## Guardrails

- Do not run local `mvn`; Maven checks should go through Docker.
- Do not add tests automatically during MVP iteration unless the user asks or the risk is high.
- If docs and implementation disagree, either ask which is authoritative or record the mismatch in `docs/status/project-status.md`.
- Keep final reports concise, but always mention documentation updates and verification.
