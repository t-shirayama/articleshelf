---
name: articleshelf-ui-polish
description: Apply ArticleShelf-specific UI design rules when changing Vue components, Vuetify usage, CSS, layout, visual states, forms, modals, article cards, detail screens, sidebar, search/sort controls, or any user-facing interaction. Use this before implementing UI or visual changes, and when reviewing whether a ArticleShelf screen feels aligned, stable, and understandable.
---

# ArticleShelf UI Polish

Use this skill to keep ArticleShelf UI changes intentional instead of one-off. Always read `docs/designs/README.md` and the relevant detailed design doc before changing user-facing UI.

## Workflow

1. Identify the affected surface: article list, article detail, add modal, sidebar, search/sort controls, common buttons, forms, cards, or responsive layout.
2. Check `docs/designs/README.md`, then `docs/designs/components/README.md`, `docs/designs/responsive/README.md`, or `docs/designs/responsive/mobile.md` as relevant for the current design rule and update it if the intended UI behavior changes.
3. Apply the four design principles:
   - Proximity: keep helper text close to the field it explains; separate unrelated groups with clear spacing or dividers.
   - Alignment: preserve shared left edges, fixed slots, and consistent control widths.
   - Repetition: reuse existing components and class patterns before adding one-off styling.
   - Contrast: make interactive, disabled, error, and selected states visible without changing layout.
4. Preserve layout stability. Viewing and editing states should not change control size, card height, sidebar count positions, or meta panel alignment unless explicitly requested.
5. Prefer shared UI patterns for buttons, date fields, rating stars, dialogs, and card actions.
6. Verify the result with `npm run build` from `frontend` when Vue or CSS changed.

## ArticleShelf Preferences

- Avoid decorative wrappers that shift content start positions between states.
- Disabled states should feel inactive through tone, cursor, and contrast, not through size changes.
- Star ratings should keep the same start position and gap in view/edit modes; hover/focus should preview the selected range.
- Article cards should keep stable internal regions; long text should clamp or scroll rather than overlap adjacent controls.
- Modal spacing should be uniform, with actions aligned to the form body.
- Sidebar counts should communicate relationships clearly, such as unread + read equaling all articles.
