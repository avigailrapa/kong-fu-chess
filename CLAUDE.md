# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

"Kung Fu Chess" — a real-time chess variant: moves travel over simulated time instead of
teleporting, pieces rest/cool down after moving, and two pieces can race for a square or cross
paths mid-flight. Two front ends over one engine: a Swing GUI (`app/GuiMain.java`) and a
text-command console runner (`app/Main.java`) reading a small DSL from stdin
(@.claude/rules/text-dsl.md).

Plain `javac`-compiled — no Maven/Gradle/build.xml. `KongFu.iml` is the IntelliJ project file
(source roots: project root, `src/`, `test/`). Dependency jars live in `lib/`, fetched by hand, not
resolved by any tool (@.claude/rules/dependencies.md).

Top-level layout: `app/` (entry points, `package app`), `src/` (layered engine, packages prefixed
`src.*` since the project root — not `src/` itself — is the source root), `test/` (mirrors `src/`
and `app/` package-for-package, minus the `src.` prefix), `lib/` (jars), `assets/` (piece
sprites/board image).

## Commands

Use PowerShell, not Git Bash — Git Bash mangles the `;` classpath separator and POSIX-converts
paths passed to `javac`/`java`, silently breaking multi-jar classpaths and multi-file compiles.

**Compile main** (outputs to `out/`; needs `-processorpath` for Lombok — plain `-cp` silently
skips annotation processing on this project's JDK):
```powershell
$cp = (Get-ChildItem lib\*.jar | ForEach-Object { $_.FullName }) -join ";"
$proc = (Get-ChildItem lib\lombok*.jar).FullName
$files = Get-ChildItem -Recurse src\*.java, app\*.java | ForEach-Object { $_.FullName }
javac -d out -encoding UTF-8 -cp $cp -processorpath $proc $files
```

**Compile tests** (`out/` + JUnit jars from `lib/` on classpath):
```powershell
$cp = "out;" + ((Get-ChildItem lib\*.jar | ForEach-Object { $_.FullName }) -join ";")
$files = Get-ChildItem -Recurse test\*.java | ForEach-Object { $_.FullName }
javac -d out -cp $cp $files
```

**Run** (entry points live in the `app` package, so classes are referenced fully-qualified):
- GUI: `javaw -cp out app.GuiMain`
- Console/DSL: `java -cp out app.Main < path\to\script.kfc`
- Server: `java -cp "out;lib\*" app.ServerMain`
- Client: `java -cp "out;lib\*" app.ClientMain [wsUrl]` (defaults to `ws://localhost:8887`)

GUI/console stay `-cp out` (no jars — neither imports `src/net`/`src/server`); server/client need
`lib\*` since they pull in the WebSocket jar transitively.

**Test:** no `junit-platform-console-standalone.jar` in `lib/` — run tests via a hand-written
JUnit `Launcher` driver class, not a single `java -jar` command (@.claude/rules/testing.md).

## Code Style

- Never add comments — not even "why"-only ones. Write self-explanatory names/structure instead.
- Fluent, no-`get`-prefix accessors everywhere (`Position.row()`, `Piece.color()`), matched by
  Lombok's `@Accessors(fluent = true)`. Never write JavaBean-style `getX()`/`setX()`.

## Architecture

Strict one-directional layering, enforced by convention only (no build-tool check): each layer
depends only on the ones below it, never sideways or up. Full per-class contracts:
@.claude/rules/architecture.md.

```
model  →  rules  →  realtime  →  engine  →  view (DTOs only)
                                   ↑  ↑  ↑
                                bus │  │  input (Controller / CommandParser+Runner)
                                    │  net (wire protocol, NetworkGameProxy) / server (GameServer, Match)
                                    engine also publishes to bus; view/sound subscribes
```

- `model` / `bus` — zero project dependencies.
- `rules` — read-only move legality (`PieceRules`, `RuleEngine`); never mutates `Board`.
- `realtime` — `RealTimeArbiter` resolves all in-flight motion, jumps, rests, arrivals, races.
- `engine` — `GameEngine`, the one public command boundary; only place allowed to build `view` DTOs.
- `input` — GUI click path and text-DSL path; both call through `GameEngine`/`GameCommands` only.
- `net` / `server` — WebSocket wire protocol, client proxy, match/session/rating server.
- `view` — `Renderer`/`GameWindow` see only `GameSnapshot` DTOs, never live model/engine objects.
- `io` — plain-text board serialization, model-only dependency.

Piece asset layout (`assets/pieces/...config.json`): @.claude/rules/text-dsl.md.

## Workflow

- Cross-check design/model decisions against the course design PDF before writing code; treat it
  as authoritative over earlier informal or pasted instructions.
- Don't build interim scaffolding (stub interfaces, fake seams) to make a blocked component
  compile — defer the component until its real dependency actually exists.
- Ask before every `git commit` and before editing any document, including memory files — one
  approval doesn't carry over to the next commit/edit.
- Never cite design-doc page numbers ("p.X") or iteration numbers in commit messages.
