# Board text format

`src/io/BoardParser` reads the same plain-text board format the old `.kfc` DSL used, now only as
`MatchOrchestrator`'s way of building the starting position:

```
bR bN bB bQ bK bB bN bR
.  .  .  .  .  .  .  .
...
```

Board tokens: `.` for empty, `<w|b><K|Q|R|B|N|P>` for a piece (e.g. `wK`).

# Piece assets

`assets/pieces/<KindLetter><ColorLetter>/states/{idle,move,jump,short_rest,long_rest}/config.json`
+ `sprites/*.png` — e.g. `assets/pieces/PW/` is the white pawn (kind letter first, then color
letter — the opposite order from the board-token format `wP` above). Each state's `config.json`
carries that state's `speed_m_per_sec` (0 for non-moving states), `next_state_when_finished`
(`"idle"`, `"short_rest"`, or `"long_rest"` — this is what actually drives whether/how long a
piece rests after moving or jumping, not a hardcoded rule), `frames_per_sec`, and `is_loop`.
`assets/board.png` is the board background image; `Renderer` requires it to exist (no procedural
fallback).
