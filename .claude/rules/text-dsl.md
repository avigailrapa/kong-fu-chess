# Text DSL (`.kfc` scripts)

Used by `app/Main.java` and `test/integration/scripts/*.kfc`.

```
Board:
bR bN bB bQ bK bB bN bR
.  .  .  .  .  .  .  .
...
Commands:
click <x> <y>
wait <ms>
jump <x> <y>
print board
```

Board tokens: `.` for empty, `<w|b><K|Q|R|B|N|P>` for a piece (e.g. `wK`). `print board` is the
only assertion mechanism in scripts — `TextScriptsTest` runs each `.kfc` file and diffs the
captured stdout.

# Piece assets

`assets/pieces/<KindLetter><ColorLetter>/states/{idle,move,jump,short_rest,long_rest}/config.json`
+ `sprites/*.png` — e.g. `assets/pieces/PW/` is the white pawn (kind letter first, then color
letter — the opposite order from the board-token format `wP` used in `.kfc` files and
`BoardParser`). Each state's `config.json` carries that state's `speed_m_per_sec` (0 for non-
moving states), `next_state_when_finished` (`"idle"`, `"short_rest"`, or `"long_rest"` — this is
what actually drives whether/how long a piece rests after moving or jumping, not a hardcoded
rule), `frames_per_sec`, and `is_loop`. `assets/board.png` is the board background image;
`Renderer` requires it to exist (no procedural fallback).
