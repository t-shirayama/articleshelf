---
name: articleshelf-design-capture
description: Refresh screenshots under `docs/designs/screenshots/` from the current ArticleShelf implementation. Use when UI changes make design images stale, when `docs/designs/screenshots/README.md` needs to match the live app, or when a user asks to replace mock images with current implementation screenshots.
---

# ArticleShelf Design Capture

Refresh `docs/designs/screenshots/**/*.png` from the running app instead of hand-maintaining static mockups.
Prefer this skill over a git hook because screenshot capture depends on app state, viewport control, and a human decision about when the UI meaningfully changed.

## Workflow

1. Confirm the app can be launched locally.
   Prefer `docker compose -f docker-compose.e2e.yml up --build -d` from the repo root for screenshot work.
   This stack avoids the common failure modes seen with the dev stack: host `5432` conflicts, dev-only restart behavior, and unstable screenshot setup.
   Expect the frontend at `http://localhost:5173` and the backend at `http://localhost:8080`.
   Prefer `localhost` over `127.0.0.1` for screenshot capture. The capture flow and backend `FRONTEND_ORIGIN` are expected to match `http://localhost:5173`.
   If the user already has the app running another way, verify both URLs respond before capturing.

2. Review the current `docs/designs/screenshots/` files before replacing them.
   Release-ready docs keep only the current screenshots; do not maintain an archive folder.
   Keep the current filenames for the refreshed captures, including `auth_login.png`, `account_settings_dialog.png`, `desktop_article_list.png`, `desktop_article_detail_view.png`, `desktop_article_detail_edit.png`, `add_article_modal.png`, and `mobile_article_list.png`.

3. Capture fresh screenshots from the live implementation.
   Run `npm run capture:designs` in `frontend/`.
   This uses `frontend/scripts/capture-design-screenshots.mjs` and writes directly into `docs/designs/screenshots/`.
   If the user explicitly asks to update only one screenshot, use `ARTICLESHELF_SCREENSHOT_TARGET=<target> npm run capture:designs` when the script supports that target, so unrelated images are not regenerated. For example, use `ARTICLESHELF_SCREENSHOT_TARGET=account-settings-dialog` for `account_settings_dialog.png`.
   The script should prefer a stable existing capture account over ad hoc user creation when that is more reliable for the current stack.
   In this repo, the default stable path is the e2e stack's initial admin account: `owner` / `password123`, unless the script is intentionally testing registration.
   The script may still seed representative articles and standalone tags through the API before capture.
   The script must authenticate the browser with the normal login form before capturing authenticated screens.
   Do not rely on API request storage state or copied auth cookies to enter the app; cookie transfer can differ between Playwright API contexts and browser contexts.
   Keep the browser context locale as `ja-JP`, set the app locale to `ja`, and capture desktop images at `1920x1080`.
   Prefer running Playwright from the local host environment, targeting the running app on `localhost`.
   Do not default to running the capture script inside the frontend container. Browser binaries and OS dependencies are more likely to be incomplete there.
   If Playwright cannot launch Chromium locally, install it with `npx playwright install chromium` from `frontend/`.
   If the local sandbox blocks headless Chromium startup, rerun the capture command with escalated permissions instead of rewriting the script around the issue.

4. Verify that the captures still represent the current product accurately.
   Check that the desktop list, account settings dialog, detail view mode, detail edit mode, add modal, and mobile list all render successfully.
   If the current implementation differs from the design docs, update `docs/designs/screenshots/README.md` and the relevant design doc in the same task.
   If there are meaningful UI gaps that should not silently replace the intended design, record follow-up work in `docs/requirements/backlog/README.md`.

5. Run verification appropriate to the touched files.
   When only the screenshot script or docs change, run `npm run build` in `frontend/` after script edits.
   When screenshot generation behavior changes, run the capture command once and confirm the PNGs were regenerated.

## Deterministic Auth/Data Pattern

Use this sequence for authenticated screenshot capture:

1. Start the screenshot stack with `docker-compose.e2e.yml` unless there is a strong reason not to.
2. Log in through the API using the stable capture account `owner` / `password123`, or another explicitly configured capture user.
3. Create representative articles and standalone tags with authenticated API calls. Allow `409` conflicts for idempotent seed data when rerunning captures.
4. Start a fresh browser context with `locale: "ja-JP"`, `viewport: { width: 1920, height: 1080 }`, `deviceScaleFactor: 1`, and `reducedMotion: "reduce"`.
5. Set `localStorage["articleshelf.locale"] = "ja"` via `addInitScript`.
6. Navigate to the frontend login screen on `http://localhost:5173` and log in through the visible form using the same capture credentials.
7. Wait for `.article-list` and at least one `.article-card` before capturing authenticated screens.

Avoid this failed pattern unless the script has been explicitly updated and verified:

1. Starting from the dev `docker compose.yml` stack when a local PostgreSQL port conflict is possible.
2. Mixing `127.0.0.1` for capture URLs with `localhost`-based backend origin settings.
3. Running the screenshot script inside the frontend container before verifying Playwright browser dependencies there.

This keeps screenshots independent from the developer's current browser state, existing local data, and Playwright API cookie behavior.

If the script captures new screens or dialogs, add the filenames to `docs/designs/screenshots/README.md` and include user-facing README images when appropriate.

## Notes

- Keep only current release-facing screenshots; this project no longer maintains screenshot archives.
- Prefer the skill plus screenshot script over a pre-commit hook. A hook can warn, but it cannot reliably decide whether the currently running UI and data are screenshot-worthy.
- If the user explicitly asks for different captured states, update the Playwright script rather than taking one-off manual screenshots.
- Avoid one-off local browser screenshots for README/design docs unless the user explicitly requests a temporary diagnostic image.
- When capture fails, check these in order before changing app code:
  1. `docker compose -f docker-compose.e2e.yml ps`
  2. `curl -sS http://localhost:8080/actuator/health`
  3. `curl -sI http://localhost:5173`
  4. `npx playwright install chromium`
  5. rerun `npm run capture:designs` against `localhost`

## Files To Touch Together

- `docs/designs/screenshots/**/*.png`
- `docs/designs/screenshots/README.md`
- the relevant `docs/designs/**/README.md` or detailed design file when there is an intentional UI spec change
- `docs/requirements/backlog/README.md` when there is an intentional mismatch or follow-up
- `frontend/scripts/capture-design-screenshots.mjs` when capture steps or selectors need to change
