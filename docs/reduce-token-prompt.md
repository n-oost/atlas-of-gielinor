Help me simplify and maintain my RuneLite plugin with the least custom code necessary.

Plugin:
C:\dev\runelite-plugin-better-map-2\better-map

Assets:
C:\dev\runelite-plugin-better-map-2\better-map-assets

Read AGENTS.md before working. Preserve existing uncommitted changes.

PRIORITIES

1. Check whether standard Java 11 or supported RuneLite APIs can replace custom code. Prefer the simplest option that preserves behavior, performance, cancellation, and validation. Verify RuneLite APIs against current source. Use plugin messages when another plugin exposes a suitable integration. Do not replace clear loops with streams merely to shorten the code.
2. Remove unused code after checking callers, resources, Gson fields, and generated getters.
3. Extract genuinely repeated logic into small shared components. Prefer composition. Avoid generic frameworks, unnecessary wrappers, or splitting files merely to make them look smaller.
4. Move changing game content, coordinate lists, and curated exceptions into bundled datasets. Keep implementation constants and format limits in code.
5. Simplify installation and maintenance. Automate repetitive asset preparation where practical.

HOW TO WORK WITH ME

I will share classes and ask questions.

For a review:
- Inspect the full implementation and relevant callers.
- Explain what can be removed, shared, replaced by RuneLite, or moved into data.
- Distinguish reductions in total code from code merely moved elsewhere.

When I say “do it,” implement the agreed change completely.
Ask only when a missing decision materially affects behavior.
Keep changes focused and preserve existing behavior unless I explicitly approve a change.
Keep responses short, concrete, and easy to act on.


GUARDRAILS

Preserve Java 11 compatibility, native menus and map-close input, asynchronous loading, cancellation, and complete pack validation. Follow all AGENTS.md restrictions.


Start by inspecting the class or task I provide next.

goal is to reduce token count of files under main/java towards 200k token use, not including comments.


start with largest file