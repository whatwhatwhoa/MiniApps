# The Stack look on Android (`:stack`)

The owner's Stack Design Guide (v1.1, kept in the NZFieldDocs project at
`design/stack/Stack_Design_Guide.html`) defines the look of their Stack tools: a calibrated
bench tool, cool grey-blue neutrals, one deep teal accent, one amber "handle" for what you grab
or watch, a dark cutting mat with a grid, Instrument Sans for the UI and JetBrains Mono for
figures. `:stack` is that guide for Compose. Read this before any screen work.

## 1. What carries over, what changes
- **Same**: every color token (copied value for value into `Colors.kt`, light and dark), the
  type scale, the radii, the components' shapes, the principles (*Summary first*, *State in
  form*, *Honest data*, *Restraint*), the copy rules (plain words, units, no emoji, buttons say
  what happens, every metric explains itself).
- **Different on a phone**: fonts are bundled, not fetched; touch targets are larger (buttons
  ≥ 40 dp, small ones and segments ≥ 34 dp, tabs ≥ 44 dp); a tooltip opens on tap; a phone
  screen may keep its header and main picture fixed while the rest scrolls; the system bars
  follow the app's theme.
- **Not used**: the web-only parts (file drop zones, the library back link, PDF export, the
  `nzfd-*` metadata).

## 2. Theme
`StackTheme { … }` wraps an app's content. It provides `Stack.colors` (the tokens), maps
Material 3 onto them (primary = accent, surface = panel, background = bg, outline = line,
error = danger, inverse = ink/panel for toasts), sets the Stack type on Material's roles, and
keeps Auto / Light / Dark per app (a "stack" preferences file). `ThemeIconButton()` cycles it
and sits in the header by default (series D1.8). `StackTheme(systemBars = false)` leaves the bars to the app.

Token roles: `accent` selected, primary, focus · `handle` warn pill, callout bar, things to
watch · `danger` / `good` status · `mat` / `matLine` the cutting mat · `s0`…`s4` series told
apart by color, always beside a label, fixed per item · `muted` secondary text · `line`
borders · `grid` faint fills. A new token goes into both `LightStack` and `DarkStack`.

## 3. Type (`StackType`)
| Style | Use |
|---|---|
| `h1` 19 sp / 700 | the header's title |
| `h2` 15 sp / 600 | card titles |
| `body` 15 sp, `note` 13 sp, `sub` 12.5 sp | text, notes and captions, context lines |
| `label` 12 sp / 600, +0.07 em | field labels and tile labels, uppercased (`FLabel` does it) |
| `tileValue` 30 sp / 700 (`tileValuePhone` 24 sp) + `tileUnit` 15 sp | a tile's figure and unit |
| `table` 13 sp, `th` 11.5 sp / 600 | tables; headers uppercased |
| `mono` 12 sp, `monoBody` 15 sp | counts, codes, resolutions, axis labels, typed numbers |

Digits are tabular wherever they line up (`TNUM`, already in the figure styles).

## 4. Components
| Compose | Web | Use |
|---|---|---|
| `StackHeader(title, count)` | `header .brand` | title + mono count left, actions right (the Theme icon by default); the count wraps under the title at large fonts |
| `StackTabs(labels, selected, …)` | `.tabs` | views of one app, under the header; `compact` narrows them to share a row; `brand` sets the first (the app's name, in a one-row app) in the bold `h1` |
| `ThemeIconButton()` | — | the Theme button, an icon in every app (Auto half circle, Light sun, Dark moon) |
| `SettingsIconButton(selected, onClick)` | — | Settings as a gear beside the Theme icon, in a one-row header; soft accent fill and accent edge while open |
| `StackCard(title, info, stripe, action)` | `.card` | a separate object; `stripe` = a 4 dp category or series edge |
| `Tile` + `TileGrid(columns, …)` | `.tiles` | exactly four KPIs: label + (i), value + unit, a context line |
| `Seg(options, selected, …)` | `.seg` | one choice of a few; wraps when it doesn't fit |
| `StackButton(text, primary, small)` | `button` | an action; one primary per view |
| `StackField(value, suffix, …)` | `input` | a typed value; mono by default |
| `FLabel`, `Pill(kind)`, `Chip`, `Swatch` | `.flabel`, `.pill`, `.chip`, `.sw` | labels, state in form, legends |
| `Callout`, `Notes`, `KeyValue` | `.callout`, `.notes`, `dl.kv` | a caveat, definitions one per line, label/value rows |
| `InfoDot(tip)` | `.info` + `.ftip` | the (i) on every metric and card title |
| `UpdateCard()`, `UpdateBanner()` (in `:update`) | — | Settings → Updates, and the note at the top of a page while a newer release is out |
| `Mat(grid)`, `MatInk` | `.mat` | the dark board a tool shows its work on; `grid = null` when the content draws a grid in real units |

## 5. Layout
- The page column: `Modifier.stackColumn()` (max 1240 dp, 16 dp gutters); blocks
  `BlockGap` (14 dp) apart, spaced by `Arrangement.spacedBy`, never by margins on each block.
- Order a view as the guide does: header, tabs, controls, four tiles, the picture or chart,
  details and tables, then notes. A phone may pin the header and the picture.
- Tiles: two columns on a phone, four from 900 dp. From 600 dp an app goes to two columns.
- Radii: cards and tiles 12, inner panels 10, buttons and fields 8, segments 6, pills round.
- Wide tables scroll sideways inside their card; the page never does.

## 6. Charts and drawings
Canvas, colors read from `Stack.colors` at draw time (so they follow the theme), axis labels in
`StackType.mono` at 11 sp, gridlines in `line` (or `matLine` on the mat), series colors in slot
order. Low-sample or estimated values look different from measured ones, and say so.
