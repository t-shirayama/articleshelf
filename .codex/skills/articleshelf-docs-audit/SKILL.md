---
name: articleshelf-docs-audit
description: Audit ArticleShelf documentation consistency. Use when Codex needs to check or fix docs links, old docs paths, docs directory structure rules, source-of-truth ownership, responsibility overlap, or whether AGENTS.md and ArticleShelf skills are aligned with docs changes.
---

# ArticleShelf Docs Audit

Use this skill for documentation integrity checks and responsibility cleanup.
After the initial release, treat `AGENTS.md` as the worker handbook for Codex / AI agents / maintainers, and `docs/README.md` as the human-facing docs entrypoint.

## Workflow

1. Read `AGENTS.md`, `docs/README.md`, and `.codex/skills/articleshelf-change-sync/SKILL.md`.
2. Check link and path integrity:
   - Run a Markdown relative link existence check across `README.md`, `docs`, `AGENTS.md`, and `.codex`.
   - Run old-path searches for the legacy specification folder, the old Backlog file path, moved architecture direct `.md` paths, moved product direct `.md` paths, and old phase wording.
   - Run `git diff --check`.
3. Check docs structure:
   - `docs/` direct children should be `README.md` plus major folders.
   - Each `docs/<area>/` direct child should be `README.md`, a responsibility folder, or an asset-only folder such as images or generated screenshots.
   - Non-asset responsibility folders should have a `README.md`.
4. Check source-of-truth ownership:
   - Requirements: what must be true.
   - Specs: current behavior, API contracts, data model, UI behavior, security behavior.
   - Architecture: structure, responsibility boundaries, runtime and persistence design.
   - Designs: visual layout, component appearance, responsive details, screenshots.
   - Testing: verification strategy, cases, commands, CI test shape.
   - Backlog: future tasks, gaps, ideas, TODOs, and technical debt. Backlog uses one file per task under `pending/` or `in-progress/`; completed task summaries go to `archive/YYYY-MM-DD.md`.
5. If overlap exists, preserve information by moving details to the proper source of truth, then replace duplicates with concise links.
6. Report updated docs, checks run, and any remaining intentional overlap.

## Commands

Use these checks as the default audit set:

```powershell
$legacy = @(
  'docs' + '/specification',
  'specification' + '/',
  'requirements/backlog' + '.md',
  'architecture/(technology|frontend|backend|data-model|api-flow|runtime|ci-cd)' + '\.md',
  'product/(vision|glossary)' + '\.md',
  'MVP' + ' iteration',
  ('ま' + 'ずは' + '基本'),
  '画像' + '貼り付け'
) -join '|'
rg $legacy README.md docs AGENTS.md .codex
git diff --check
```

For Markdown link validation, use a local script or shell snippet that resolves relative `.md` links from each source file and reports missing targets.

## Guardrails

- Do not turn requirements docs into implementation specs.
- Do not duplicate detailed data model, API, UI, security, or test rules in multiple places.
- Keep README files as indexes or short source-of-truth summaries.
- Keep `AGENTS.md` focused on operating rules and review lenses, not project roadmap or speculative product ideas.
- For Backlog, verify `docs/requirements/backlog/README.md` and each state folder README stay updated, task filenames use lowercase kebab-case, task files use the standard headings, and priorities use `P0`-`P4`.
- Build / unit / integration / E2E are not required for docs-only audits unless scripts or executable behavior changed.
