---
name: readstack-design-capture
description: Archive old files under `docs/designs/` and refresh them with screenshots from the current ReadStack implementation. Use when UI changes make design images stale, when `docs/design.md` or `docs/designs/README.md` need to match the live app, or when a user asks to replace mock images with current implementation screenshots.
---

# ReadStack Design Capture

Refresh `docs/designs/*.png` from the running app instead of hand-maintaining static mockups.
Prefer this skill over a git hook because screenshot capture depends on app state, viewport control, and a human decision about when the UI meaningfully changed.

## Workflow

1. Confirm the app can be launched locally.
   Use `docker compose up --build -d` from the repo root unless the user already has the app running another way.
   Expect the frontend at `http://localhost:5173` and the backend at `http://localhost:8080`.

2. Review the current `docs/designs/` files before replacing them.
   Move the existing PNG files into `docs/designs/archive/<YYYY-MM-DD>/` so prior captures remain available.
   Keep the current filenames for the refreshed captures: `desktop_article_list.png`, `desktop_article_detail_light.png`, `add_article_modal.png`, and `mobile_article_list.png`.

3. Capture fresh screenshots from the live implementation.
   Run `npm run capture:designs` in `frontend/`.
   This uses `frontend/scripts/capture-design-screenshots.mjs` and writes directly into `docs/designs/`.
   The script must create deterministic capture data through the API, then authenticate the browser with the normal login form before capturing authenticated screens.
   Do not rely on API request storage state or copied auth cookies to enter the app; cookie transfer can differ between Playwright API contexts and browser contexts.
   Keep the browser context locale as `ja-JP`, set the app locale to `ja`, and capture desktop images at `1920x1080`.
   If Playwright cannot launch Chromium, install it with `npx playwright install chromium` from `frontend/`.

4. Verify that the captures still represent the current product accurately.
   Check that the desktop list, detail view, add modal, and mobile list all render successfully.
   If the current implementation differs from the design docs, update `docs/design.md` and `docs/designs/README.md` in the same task.
   If there are meaningful UI gaps that should not silently replace the intended design, record them in `docs/project-status.md`.

5. Run verification appropriate to the touched files.
   When only the screenshot script or docs change, run `npm run build` in `frontend/` after script edits.
   When screenshot generation behavior changes, run the capture command once and confirm the PNGs were regenerated.

## Deterministic Auth/Data Pattern

Use this sequence for authenticated screenshot capture:

1. Create a unique capture user with `POST /api/auth/register`.
2. Create representative articles and standalone tags with authenticated API calls using the returned access token.
3. Start a fresh browser context with `locale: "ja-JP"`, `viewport: { width: 1920, height: 1080 }`, `deviceScaleFactor: 1`, and `reducedMotion: "reduce"`.
4. Set `localStorage["readstack.locale"] = "ja"` via `addInitScript`.
5. Navigate to the frontend login screen and log in through the visible form using the capture user's email/password.
6. Wait for `.article-list` and at least one `.article-card` before capturing authenticated screens.

This keeps screenshots independent from the developer's current browser state, existing local data, and Playwright API cookie behavior.

If the script captures new screens or dialogs, add the filenames to `docs/designs/README.md`, update `docs/design.md`, and include user-facing README images when appropriate.

## Notes

- Do not replace archived images in place; keep each refresh date as a separate folder.
- Prefer the skill plus screenshot script over a pre-commit hook. A hook can warn, but it cannot reliably decide whether the currently running UI and data are screenshot-worthy.
- If the user explicitly asks for different captured states, update the Playwright script rather than taking one-off manual screenshots.
- Avoid one-off local browser screenshots for README/design docs unless the user explicitly requests a temporary diagnostic image.

## Files To Touch Together

- `docs/designs/*.png`
- `docs/designs/archive/`
- `docs/designs/README.md`
- `docs/design.md`
- `docs/project-status.md` when there is an intentional mismatch or follow-up
- `frontend/scripts/capture-design-screenshots.mjs` when capture steps or selectors need to change
