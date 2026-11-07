# Handoff

Current state only. **Rewrite this file at the end of every session**: carry forward what is
open, delete what is resolved, never append history (git and `docs/decisions/` hold it).

**State (2026-11-06):** 0.1.0 is the release on GitHub. M2 Alerts is built and still
uncommitted (Settings → Alerts: level, charging power, drain or temperature, at or above /
below a value, for at least a time; D2.1–D2.5). Since 2026-11-06 the app lives in the series
(`C:\Projects\MiniApps\apps\powermeter`) and wears the Stack look: Stack palette and fonts, a
header with "monitoring on/off" and the Theme button, tabs on top instead of the bottom bar and
rail, Stack cards, segments and buttons (series D1.2); text without its own color was black
on dark cards until the evening's `:stack` fix. That build is on the owner's Pixel
(release-signed debug; history kept). Lint: UseKtx, DataExtractionRules. Release APK 2.38 MB.
Not yet seen by the owner: the new look; an alert actually firing.

## Next
2. The owner looks at the Stack look (series `VERIFY.md` *V-stack-look*).
4. New README screenshots: `docs/screenshots/` still show the look before the Stack one.
5. A release (0.2.0) only when the owner asks; its download link moves to the series repo.

## Waiting on the owner
- Nothing (V-M1, V-M2 and the decisions they held were settled 2026-11-07).

## Uncommitted
Nothing (M2 and the Stack look committed and pushed to the series repo 2026-11-06).
