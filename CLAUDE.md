# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

"Kung Fu Chess" — a real-time chess variant where moves play out over simulated time (a piece travels
at a fixed speed instead of teleporting instantly), pieces have a rest/cooldown period after moving, and
two pieces can race for the same square or cross paths mid-flight. There are two front ends over the same
engine: a Swing GUI (`GuiMain.java`) and a text-command console runner (`Main.java`) that reads a small DSL
from stdin.

There is no build tool (no Maven/Gradle/build.xml) — this is a plain `javac`-compiled project. `KongFu.iml`
is the IntelliJ project file (source roots: project root, `src/`, `test/`; excludes `out/` and `legacy/`).
Dependency jars are just files dropped into `lib/` (fetched by hand from Maven Central, not resolved by any
tool) — `KongFu.iml`'s `jarDirectory` entry for `lib/` (non-recursive) means IntelliJ picks up anything
placed there automatically. Besides the JUnit 5 jars, `lib/` has `Java-WebSocket-1.6.0.jar` (used by
`src/net/`/`src/server/`), `slf4j-api-2.0.13.jar` (a real runtime dependency of Java-WebSocket, despite
its own docs describing it as dependency-free — without it, anything using `GameServer`/`NetworkGameProxy`
throws `NoClassDefFoundError` the first time the library logs anything; with just the API jar and no
logging backend, SLF4J 2.x prints one harmless one-time "no providers found" warning instead of failing),
and `lombok-1.18.46.jar` (`@Getter`/`@Setter`/`@RequiredArgsConstructor`, configured fluent/no-prefix via
`@Accessors(fluent = true)` to match this codebase's existing no-`get`-prefix accessor style everywhere
else — e.g. `Position.row()`, `Piece.color()` — rather than Lombok's JavaBean-style default; fluent
setters return the owning instance instead of `void`). Lombok is compile-time-only (an annotation
processor, not a runtime dependency) but still needs to be on the compiler's `-processorpath`, not just
`-cp` — see the compile command below.

## Build & test commands

Use PowerShell for these, not Git Bash — Git Bash mangles the `;` classpath separator and does POSIX-path
conversion on arguments passed to the native `javac`/`java` binaries, which silently breaks multi-jar
classpaths and multi-file compiles. PowerShell does not have this problem.

**Compile main sources** (outputs to `out/`, matching the IntelliJ module's output folder). Needs `-cp`
pointing at `lib/` now that `src/net/`/`src/server/` use the WebSocket jar — main sources had zero external
dependencies before that, so this flag is new as of those packages existing. Also needs `-processorpath`
pointing at the Lombok jar specifically: on this project's JDK (26), implicit annotation-processor
discovery via plain `-cp` does not run Lombok at all — not even a warning, it silently skips code
generation, which then surfaces as a confusing "variable not initialized in the default constructor"
error on whatever class used `@RequiredArgsConstructor`, with no mention of Lombok anywhere in the output:
```powershell
$cp = (Get-ChildItem lib\*.jar | ForEach-Object { $_.FullName }) -join ";"
$proc = (Get-ChildItem lib\lombok*.jar).FullName
$files = Get-ChildItem -Recurse src\*.java, GuiMain.java, Main.java, ServerMain.java, ClientMain.java | ForEach-Object { $_.FullName }
javac -d out -encoding UTF-8 -cp $cp -processorpath $proc $files
```

**Compile tests** (needs `out/` on the classpath plus the JUnit jars in `lib/`):
```powershell
$cp = "out;" + ((Get-ChildItem lib\*.jar | ForEach-Object { $_.FullName }) -join ";")
$files = Get-ChildItem -Recurse test\*.java | ForEach-Object { $_.FullName }
javac -d out -cp $cp $files
```

**Run the GUI:**
```powershell
javaw -cp out GuiMain
```

**Run the console/text-DSL mode** (reads a script from stdin — see "Text DSL" below):
```powershell
java -cp out Main < path\to\script.kfc
```
Both of the above stay exactly `-cp out` (no jars needed) — neither `GuiMain` nor `Main` imports anything
from `src/net/`/`src/server/`, so the WebSocket jar is never resolved at their runtime. Only the main
*compile* command needs `-cp` now, because it compiles every `.java` file under `src/` in one pass, `net`/
`server` included.

**Run the server** (binds a WebSocket port and hosts one `Match`):
```powershell
java -cp "out;lib\*" ServerMain
```

**Run the client** (connects to a running server, defaults to `ws://localhost:8887`; pass a URL as the
first arg to point at a different host):
```powershell
java -cp "out;lib\*" ClientMain
```
Both need `lib\*` on the runtime classpath (unlike `GuiMain`/`Main` above) since `ServerMain`/`ClientMain`
pull in the WebSocket jar transitively through `src/net/`/`src/server/`.

**Run tests:** `lib/` only has `junit-jupiter-api`/`junit-jupiter-engine`/`junit-platform-*` jars — there is
no `junit-platform-console-standalone.jar`, so tests can't be run with a single `java -jar` command. Drive
the `org.junit.platform.launcher.Launcher` API directly with a throwaway driver class instead:
```java
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
// or: selectClass("engine.GameEngineTest") for a single test class
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import java.io.PrintWriter;

public class RunTests {
    public static void main(String[] args) {
        LauncherDiscoveryRequest request = request()
                .selectors(selectPackage("engine"), selectPackage("input"), selectPackage("realtime"),
                        selectPackage("model"), selectPackage("rules"), selectPackage("io"), selectPackage("view"),
                        selectPackage("bus"), selectPackage("net"), selectPackage("server"),
                        selectPackage("integration"))
                .build();
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);
        TestExecutionSummary summary = listener.getSummary();
        summary.printTo(new PrintWriter(System.out));
        summary.printFailuresTo(new PrintWriter(System.out));
        System.exit(summary.getTotalFailureCount() > 0 ? 1 : 0);
    }
}
```
Compile it against the same classpath as the tests, then `java -cp out;lib\*;. RunTests`. Note that test
packages don't have a `test.` prefix (`test/engine/GameEngineTest.java` declares `package engine;`), so
`selectPackage`/`selectClass` use the bare package name. Make sure `selectPackage("integration")` is
actually included — it's easy to leave out (the very first version of this snippet did, silently skipping
`test/integration/` — including `TextScriptsTest` — for a while) since nothing fails loudly when a package
is just never discovered.

Full-suite runs now take several seconds, not milliseconds: `test/server/MatchTest.java` and the
`test/integration/` server/client tests exercise a real `ScheduledExecutorService` tick loop and, in a few
cases, a real bound socket, so they wait on real wall-clock timing (with generous timeouts, not tight
sleeps) rather than an injected `ms` argument the way `GameEngine.waitMs`/`EffectsController.tick` do.

`javac`/`java` print `warning: unknown enum constant Status.STABLE` noise from JUnit's `@API` annotations
(the `apiguardian` jar isn't in `lib/`) — harmless, not a real error.

## Architecture

Strict one-directional layering; each layer only depends on the ones below it, never sideways or up. This
is enforced by convention (not a build tool), so when adding code, check which package it belongs in by
what it's allowed to depend on:

```
model  →  rules  →  realtime  →  engine  →  view (DTOs only: GameSnapshot/PieceSnapshot/SelectionSnapshot)
                                   ↑  ↑  ↑
                                bus │  │  input (Controller depends on GameCommands, GameEngine's narrow
                                    │  │         3-method interface; BoardMapper depends on GameSnapshot
                                    │  │         only for its CELL_WIDTH/CELL_HEIGHT constants)
                                    │  net (wire protocol + NetworkGameProxy, a second GameCommands
                                    │       implementation — see below) / server (GameServer + Match,
                                    │       depends on net for the wire protocol)
                                    engine also publishes to its EventBus (src/bus/); view/sound
                                    subscribes to that same bus (see src/view/ and src/bus/ below)
```

- **`src/model/`** — `Position`, `Piece`, `Board`/`IBoard`, `GameState`. Zero dependencies on anything else
  in the project. `Board` is the only mutable piece-position store; `Piece.State` carries both logical
  state (`IDLE`/`MOVING`/`JUMPING`/`CAPTURED`) and the rest states (`SHORT_REST`/`LONG_REST`).
- **`src/bus/`** — `EventBus`, a small generic type-keyed pub/sub (`subscribe(Class<T>, Consumer<? super T>)`,
  `publish(Object)`). Zero dependencies on anything else in the project, same as `model`. `GameEngine` owns
  one `EventBus` instance per game and publishes `MoveEvent`/`GameOverEvent`/`ScoreChangedEvent` (all in
  `src/engine/`) onto it; this coexists with the older `MoveObserver`/`MoveLogger` mechanism rather than
  replacing it.
- **`src/rules/`** — `PieceRules` is a Strategy interface (`legalDestinations(board, piece)`), one
  implementation per piece kind (`RookRule`, `BishopRule`, etc.), sharing `SlidingRule`/`FixedOffsetRule`
  base classes. `RuleEngine` is the read-only validation service (`validateMove`, `legalDestinations`) —
  it never mutates `Board` and knows nothing about game-over.
- **`src/realtime/`** — `RealTimeArbiter` owns all in-flight `Motion`s, jumps, and rest timers, and is the
  only thing that resolves arrivals/captures/races. `MotionResolver`/`JumpResolver`/`CollisionResolver`/
  `PathCrossingResolver` are its internal collaborators (collision when two pieces target the same square;
  path-crossing when two pieces' straight-line paths intersect mid-flight). Depends on `IBoard` (not the
  concrete `Board`) plus `view.AnimationConfig` (an accepted exception, for reading per-piece speed from
  the asset JSON). Has zero knowledge of `GameState` — king-capture is only *reported* via `ArrivalEvent`,
  never acted on here.
- **`src/engine/`** — `GameEngine` is the application service and the only public command boundary
  (`requestMove`, `requestJump`, `waitMs`, `snapshot`). It composes `Board`+`GameState`+`RuleEngine`+
  `RealTimeArbiter` and is the one place allowed to construct `view` DTOs. `MoveObserver`/`MoveLogger`
  give a decoupled way to react to completed moves (e.g. for the on-screen move log) without `GameEngine`
  knowing anything about rendering; `GameEngine.eventBus()` is the newer, more general alternative to that
  same idea (see `src/bus/` above). `GameCommands` is the 3-method interface (`isOccupied`, `requestMove`,
  `requestJump`) that `GameEngine implements`, extracted so `src/input/Controller` can depend on the
  interface instead of the concrete class. `AlgebraicNotation` converts `Position` to/from a 2-character
  algebraic square string (`"e7"`) in both directions, with input validation — `MoveEvent.algebraicMove()`
  delegates to it rather than duplicating the conversion.
- **`src/input/`** — `Controller` (GUI path: pixel click → `BoardMapper.pixelToCell` → `GameCommands` call,
  implemented by `GameEngine`) and `CommandParser`/`CommandRunner`/`ConsoleRunner` (text-DSL path, used by
  both `Main.java` and the `test/integration/` script tests — see below). Neither depends on
  `RuleEngine`/`RealTimeArbiter`/`Board` directly; both only ever call through `GameEngine`/`GameCommands`.
- **`src/net/`** — the wire protocol shared by client and server, plus the client-side network adapter.
  `WireMessage` is a sealed interface (`MoveCommand`/`JumpCommand`/`MoveAccepted`/`MoveRejected`/
  `StateMessage`); `Protocol.parse(String)`/`encode(WireMessage)` convert to/from the actual text sent over
  a WebSocket text frame — a bare 6-char move token (e.g. `WQe2e5`, matching the course slide's literal
  example, no verb prefix), `JUMP <token>`, `OK`, `REJECT <reason>`, or a multi-line
  `STATE`/`PIECE`/`SELECT`/`LEGAL`/`WLOG`/`BLOG`/`ENDSTATE` block that flattens/reconstructs a whole
  `GameSnapshot`. `MalformedMessageException` is what `parse` throws on any bad input; it never lets any
  other exception type escape. `NetworkGameProxy` (`extends org.java_websocket.client.WebSocketClient`,
  `implements GameCommands`) is the client-side stand-in for `GameEngine` wherever `Controller` is used —
  `isOccupied` and the color/kind needed to build a move token are answered from a locally-cached
  `GameSnapshot` (zero round-trip); `requestMove` blocks the calling thread on a `CompletableFuture` up to
  a timeout, matched to its reply via a FIFO queue (not a single shared slot) so a late reply to an
  abandoned/timed-out request can't be misdelivered to whatever request is sent next — the ordering
  guarantee this relies on is that one WebSocket connection delivers frames in send order both ways, and
  `GameServer` replies to a connection's messages in the order it received them (see `src/server/` below).
  `requestJump` stays fire-and-forget, matching `GameCommands`' existing asymmetry.
- **`src/server/`** — `Match` owns one `GameEngine` + a fresh `MoveLogger` wired to it, on its own
  single-threaded `ScheduledExecutorService`; `start(Runnable onTick)` schedules a periodic
  `engine.waitMs(tickIntervalMs)` followed by the given callback, and `submit(Runnable)` funnels any other
  work (incoming messages) onto that same thread — the whole point being `GameEngine`/`RealTimeArbiter`
  have no internal synchronization and must never be touched concurrently. `GameServer extends
  org.java_websocket.server.WebSocketServer`, composing one `Match` with `Protocol`: `onOpen` sends a
  fresh client the current `StateMessage`; `onMessage` funnels the request through `match.submit(...)`,
  cross-checks the move/jump's declared color+kind against what's actually on the board before forwarding
  to `GameEngine` (rejecting a mismatch with `REJECT token_mismatch`), and broadcasts a `StateMessage` to
  every connection after handling it; `onStart` calls `match.start(this::broadcastState)` so the periodic
  tick also broadcasts, not just each reactive per-message update.
- **`src/io/`** — `BoardParser`/`BoardPrinter`, plain-text board serialization, model-only dependency.
- **`src/view/`** — `GameSnapshot`/`PieceSnapshot`/`SelectionSnapshot` are passive, read-only DTOs built by
  `GameEngine.snapshot(...)` (already carrying pre-computed pixel positions, move-log text, legal-destination
  set, etc. — nothing in `view` re-derives game logic). `Renderer.render(GameSnapshot)` is a pure
  snapshot-to-`BufferedImage` function and must never import anything from `src.engine`/`src.model`/
  `src.realtime` beyond value types like `Piece.Color`/`Position`. `GameWindow` is the Swing shell (JFrame +
  mouse input + repaint loop) and must never import `GameEngine` either — it only holds a
  `Supplier<GameSnapshot>`, a `Controller`, a `Renderer`, a `GameLoop`, and an `EffectsController`. `GameLoop` and `EffectsController`
  are the two classes in `view` allowed to reach into `engine`. `GameLoop` holds a `GameEngine` reference
  directly; its only job is advancing simulated time each tick (`engine.waitMs`) and reporting whether
  anything changed, so the window doesn't have to know the engine's API. `EffectsController` doesn't hold a
  `GameEngine` reference — it subscribes to `engine`-defined event records (`MoveEvent`, `GameOverEvent`)
  published on a `GameEngine`'s `EventBus` (see `src/bus/`, a dependency-free pub/sub package) to trigger
  one-shot sounds and a short "GAME START!" banner, so those don't have to be re-derived by polling
  `GameSnapshot` every frame. `EffectsController`, its `SoundPlayer` interface, and the real
  `ClipSoundPlayer` implementation (tries `<root>/<name>.wav` via `javax.sound.sampled`, falls back to
  `Toolkit.beep()`) live in the `src/view/sound/` subpackage — the one place in this project with a nested
  package under one of the 7 top-level ones, kept separate so `EffectsController`'s tests can inject a fake
  `SoundPlayer` without touching real audio. `Renderer.drawBanner(BufferedImage, String)` is what
  `EffectsController`'s banner text is actually painted with. `Img` is a thin `BufferedImage` wrapper (load/resize/draw/text) — all pixel drawing goes through it,
  never raw `Graphics2D` calls scattered elsewhere. `AnimationConfig` loads a piece's per-state JSON
  (`speed_m_per_sec`, `next_state_when_finished`, `frames_per_sec`, `is_loop`) via regex, not a JSON library.

Because `Renderer`/`GameWindow` only ever see `GameSnapshot`, a rendering bug can be reproduced/tested with
a hand-built fake `GameSnapshot` — no real `Board`/`GameEngine`/`Controller` needed.

### Text DSL (`.kfc` scripts, used by `Main.java` and `test/integration/scripts/*.kfc`)

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
Board tokens: `.` for empty, `<w|b><K|Q|R|B|N|P>` for a piece (e.g. `wK`). `print board` is the only
assertion mechanism in scripts — `TextScriptsTest` runs each `.kfc` file and diffs the captured stdout.

### Piece assets

`assets/pieces/<KindLetter><ColorLetter>/states/{idle,move,jump,short_rest,long_rest}/config.json` +
`sprites/*.png` — e.g. `assets/pieces/PW/` is the white pawn (kind letter first, then color letter — the
opposite order from the board-token format `wP` used in `.kfc` files and `BoardParser`). Each state's
`config.json` carries that state's `speed_m_per_sec` (0 for non-moving states), `next_state_when_finished`
(`"idle"`, `"short_rest"`, or `"long_rest"` — this is what actually drives whether/how long a piece rests
after moving or jumping, not a hardcoded rule), `frames_per_sec`, and `is_loop`. `assets/board.png` is the
board background image; `Renderer` requires it to exist (no procedural fallback).
