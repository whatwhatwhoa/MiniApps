# Architecture

## Goals
The owner wants a phone app to compare display sizes, like displaywars.com: how much bigger a
34″ ultrawide is than a 27″, what a TV looks like next to a monitor, a phone next to a tablet.
It answers with a drawing to scale and a few honest numbers, in the series' Stack look. It
leaves out bezels, stands, curvature, prices and product databases.

## The model (`Display.kt`)
A `Display` is a diagonal in inches (displays are sold in inches everywhere), a shape and an
optional resolution, a portrait flag, a slot and an optional preset name.
- **Shape**: one of the named ratios (16:9, 16:10, 3:2, 4:3, 21:9, 32:9, 19.5:9 for iPhones,
  20:9 for most Android phones) or **Pixels**, the resolution's own ratio. Typing a full
  resolution switches the shape to Pixels; clearing it falls back to the nearest named shape.
  A named shape that the pixels don't match (> 1 % apart) shows a warn pill with the pixels'
  ratio. A pixel ratio is shown by name when it is within 0.5 % of one, else as "2.39:1".
- **Size**: long side = d·r/√(r²+1), short side = d/√(r²+1) for ratio r; portrait swaps width
  and height. Area is the product; pixel density is √(w²+h²) px ÷ the diagonal.
- **Slot** 0–3 fixes the letter (A–D) and the series color (s0–s3) for as long as the display
  exists; removing B doesn't recolor C.
- **Name**: set by a preset, dropped by any edit of size, shape or pixels (it no longer is that
  preset), kept by turning portrait.

## Presets (`Presets`)
Five families, a tab each in the Presets sheet: Phones (portrait), Tablets, Laptops, Monitors,
TVs. Monitors and TVs use the viewable diagonal (a 24″ class monitor is 23.8″, a 65″ class TV
64.5″); phones, tablets and laptops the maker's figure. Each row shows its pixels, their shape
and density. **This phone** heads Phones: the full panel's pixels ÷ the panel's dpi, held
upright, rounded to 0.1″ (the Pixel 11 Pro XL reads 6.7″; makers quote the uncut rectangle).
**Custom** keeps the numbers and drops the name.

## The mat (`ui/DisplayMat.kt`)
Every display at one scale (dp per inch = the smaller of width and height fit), outlined in its
series color over a 12 % fill, the biggest drawn first so smaller ones sit on top. *Line up*:
Bottom (centered, standing on one line, as on a desk), Center, or Corner (bottom-left). The
grid is in real units from the displays' common bottom-left corner, its step the smallest of
1/2/5/10/20/50 in (2/5/10/20/50/100 cm) that leaves at most ten squares across, named in the
corner ("Grid 5 in"). Each display's label ("A 27″") sits inside its top-left corner, moved down
past any label already placed. The mat is as tall as the displays need, up to 250 dp on a
phone and 440 dp on a tablet.

## The screen (`ui/CompareScreen.kt`, `ui/DisplayCard.kt`)
- **Phone**: the header and the mat stay put; below them scroll the update note (only while a
  newer release is out), the controls (Units in/cm, Line up), the four tiles, a card per
  display, Add display (up to four), the table, the notes, the Updates card (series *Updates*).
- **From 600 dp**: the header on top; the mat, controls and tiles on the left, the cards, table
  and notes on the right (the update note on top, the Updates card last), each column
  scrolling; four tiles in a row from 900 dp.
- **Tiles**: one display against another (B with A, or chosen with two segments when there
  are more than two): area ×, width ±, height ±, pixel density ±; each with an (i) definition.
- **A display's card**: a stripe in its color; its letter and title; Presets; diagonal with
  − / + (0.5″ below 15″, else 1″); shape; resolution; pills; a mono summary line
  ("23.5 × 13.2 in · 312 in² · 109 ppi"); Turn portrait / landscape; Remove.
- **Table**: a column per display, a row per measure, scrolling sideways when it doesn't fit.
- Diagonals are typed in inches in either unit; cm mode shows the cm beside them.

## Saving (`CompareState.kt`)
The displays, the compared pair, units and line-up are one JSON string in the "compare"
preferences, written on every change and read at start; first start shows a 27″ 1440p (A)
and a 34″ ultrawide (B). Add display picks the first of 27″ 1440p, 34″ ultrawide, 32″ class 4K,
This phone, 65″ class 4K TV that is not on the mat.
