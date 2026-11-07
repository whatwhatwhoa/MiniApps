# M3 — Readings on the chart (owner, 2026-11-07)

### D3.1 Read any point of a chart, by touch
Owner: "a scrolling readout. On the graph, you should be able to poll which part of the graph
and what the readout is." A tap or a sideways drag sets a cursor; a line, a dot, a bubble with
time and value at the finger, and the full reading in a line under the chart with Clear
(`architecture.md` *Reading a chart*). It stays after the finger lifts, so it can be read
without a hand over it. History's power and level charts share one cursor. Rejected: a cursor
only while touching (the finger hides it); panning the chart through time (the range chips
already pick the span). Status: as built 2026-11-07, shipped 0.2.0, owner to confirm.

### D3.2 A rolling average under the big number, 10 s by default
Owner: "a rolling average readout below the instantaneous. Make the rolling average default to
10 seconds, with settings for it." The plain mean of the readings in the window (5 s, 10 s,
30 s, 1 min, 5 min), signed, in the charge or drain color, with its window named beside it.
Plain, not time-weighted: on screen the readings come at the update rate, so the two barely
differ. Status: as built 2026-11-07, shipped 0.2.0, owner to confirm.

### D3.3 0.2.0 carries everything since 0.1.0
Owner: "bump it up to 0.2.0, the graphical changes were pretty large": the Stack look (series
D1.2), alerts (M2), the readout and the average (D3.1, D3.2) and updates from GitHub (series
D1.6). Released in the series repo, `powermeter-v0.2.0`. Status: done 2026-11-07.
