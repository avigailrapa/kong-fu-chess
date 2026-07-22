# Architecture detail

Full per-package contracts. See CLAUDE.md for the layering diagram; this file has the "why" and
class-level detail behind each box.

- **`src/model/`** — `Position`, `Piece`, `Board`/`IBoard`, `GameState`. Zero dependencies on
  anything else in the project. `Board` is the only mutable piece-position store; `Piece.State`
  carries both logical state (`IDLE`/`MOVING`/`JUMPING`/`CAPTURED`) and rest states
  (`SHORT_REST`/`LONG_REST`).
- **`src/bus/`** — `EventBus`, a small generic type-keyed pub/sub (`subscribe(Class<T>,
  Consumer<? super T>)`, `publish(Object)`). Zero dependencies, same as `model`. `GameEngine` owns
  one `EventBus` per game and publishes `MoveEvent`/`GameOverEvent`/`ScoreChangedEvent` onto it;
  coexists with the older `MoveObserver`/`MoveLogger` mechanism rather than replacing it.
- **`src/rules/`** — `PieceRules` is a Strategy interface (`legalDestinations(board, piece)`), one
  implementation per piece kind, sharing `SlidingRule`/`FixedOffsetRule` base classes.
  `RuleEngine` is the read-only validation service (`validateMove`, `legalDestinations`) — never
  mutates `Board`, knows nothing about game-over.
- **`src/realtime/`** — `RealTimeArbiter` owns all in-flight `Motion`s, jumps, and rest timers,
  and is the only thing that resolves arrivals/captures/races. `MotionResolver`/`JumpResolver`/
  `CollisionResolver`/`PathCrossingResolver` are its internal collaborators (collision = two
  pieces targeting the same square; path-crossing = two pieces' straight-line paths intersect
  mid-flight). Depends on `IBoard` (not concrete `Board`) plus `view.AnimationConfig` (accepted
  exception, for per-piece speed from the asset JSON). Zero knowledge of `GameState` — king
  capture is only *reported* via `ArrivalEvent`, never acted on here.
- **`src/engine/`** — `GameEngine` is the application service and only public command boundary
  (`requestMove`, `requestJump`, `waitMs`, `snapshot`). Composes `Board`+`GameState`+`RuleEngine`+
  `RealTimeArbiter`; the one place allowed to construct `view` DTOs. `MoveObserver`/`MoveLogger`
  give a decoupled way to react to completed moves without `GameEngine` knowing about rendering;
  `GameEngine.eventBus()` is the newer, more general alternative (see `src/bus/`). `GameCommands`
  is the 3-method interface (`isOccupied`, `requestMove`, `requestJump`) `GameEngine implements`,
  extracted so `src/input/ClickHandler` depends on the interface, not the concrete class.
  `AlgebraicNotation` converts `Position` to/from a 2-char algebraic square string (`"e7"`) both
  ways; `MoveEvent.algebraicMove()` delegates to it.
- **`src/input/`** — `ClickHandler` (GUI path: pixel click → `BoardMapper.pixelToCell` →
  `GameCommands` call, implemented by `GameEngine`) and `CommandParser`/`CommandRunner`/
  `ConsoleRunner` (text-DSL path, used by both `app/Main.java` and `test/integration/` — see
  text-dsl.md). Neither depends on `RuleEngine`/`RealTimeArbiter`/`Board` directly; both only call
  through `GameEngine`/`GameCommands`.
- **`src/net/`** — wire protocol shared by client and server, plus the client-side network
  adapter. `WireMessage` is a sealed interface (`MoveCommand`/`JumpCommand`/`MoveAccepted`/
  `MoveRejected`/`StateMessage`/`LoginCommand`/`Welcome`/`SelectCommand`/`MoveOccurred`/
  `GameOverMessage`/`NewGameCommand`/`RatingChanged`). `Protocol.parse(String)`/`encode(WireMessage)`
  convert to/from the text sent over a WebSocket text frame: a bare 6-char move token (e.g.
  `WQe2e5`, no verb prefix), `JUMP <token>`, `OK`, `REJECT <reason>`, `NEWGAME` (client asks the
  server for a fresh game — only accepted once the current one is over), a multi-line
  `STATE`/`PIECE`/`SELECT`/`LEGAL`/`WLOG`/`BLOG`/`ENDSTATE` block that flattens/reconstructs a
  whole `GameSnapshot`, or the server-to-client-only `EVENT_MOVE
  <color><kind><from><to> <capture:0/1> <kingCapture:0/1> <requestTimestampMs>` /
  `EVENT_GAMEOVER <color|->` / `RATING <newRating>` — the first two wrap the engine's own
  `MoveEvent`/`GameOverEvent` records, same pattern `StateMessage` uses for `GameSnapshot`;
  `RatingChanged` is sent to one connection at a time, not broadcast, since the two players' post-
  game ratings differ. `LOGIN <username> <password>` / `WELCOME <color> <rating>` round out the
  login exchange. `MalformedMessageException` is what `parse` throws on any bad input; never lets
  any other exception type escape. `NetworkGameProxy` (`extends
  org.java_websocket.client.WebSocketClient`, `implements GameCommands`) is the client-side stand-
  in for `GameEngine`: `isOccupied` and the color/kind needed for a move token are answered from a
  locally-cached `GameSnapshot` (zero round-trip); `requestMove` blocks the calling thread on a
  `CompletableFuture` up to a timeout, matched to its reply via a FIFO queue (not a single shared
  slot) so a late reply to an abandoned/timed-out request can't be misdelivered — relies on one
  WebSocket connection delivering frames in send order both ways, and `GameServer` replying to a
  connection's messages in the order received. `requestJump`/`newGame` stay fire-and-forget,
  matching `GameCommands`' existing asymmetry. `NetworkGameProxy` also owns its own `EventBus`
  (`eventBus()`) — `onMessage` republishes `MoveEvent`/`GameOverEvent` unwrapped from
  `MoveOccurred`/`GameOverMessage`, so `EffectsController` can subscribe over the network exactly
  as it does to a local `GameEngine`'s bus; also tracks the latest `RatingChanged`/`Welcome` value
  via `latestRating()`.
- **`src/server/`** — `Match` owns a `GameEngine` + a `MoveLogger` wired to it, on its own
  single-threaded `ScheduledExecutorService`; `start(Runnable onTick)` schedules a periodic
  `engine.waitMs(tickIntervalMs)` followed by the callback, `submit(Runnable)` funnels any other
  work (incoming messages) onto that same thread — `GameEngine`/`RealTimeArbiter` have no internal
  synchronization and must never be touched concurrently. `engine`/`moveLogger` are replaceable,
  not `final`: `newGame(GameEngine)` swaps in a fresh engine and reruns any registered
  `onNewGame(Runnable)` listener, since anything subscribed to the *old* engine's `EventBus` would
  otherwise keep listening to a bus nothing publishes to anymore. `GameServer extends
  org.java_websocket.server.WebSocketServer`, composing one `Match`, one `UserStore`, and
  `Protocol`; its constructor subscribes to `match.engine().eventBus()` for
  `MoveEvent`/`GameOverEvent` (via `subscribeToEngineEvents()`, also re-run through `onNewGame`)
  and broadcasts each as `MoveOccurred`/`GameOverMessage` to every connection the instant it fires,
  independent of the tick/state cycle; a second `GameOverEvent` subscriber,
  `updateRatingsAfterGameOver`, looks up both seated `Session`s, runs `EloCalculator.updatedRating`
  for each (K=32, draws split 0.5/0.5 when `GameOverEvent.winner()` is `null`), persists both via
  `UserStore.updateRating`, updates each in-memory `Session.rating()` in place (so a same-session
  rematch after `NEWGAME` starts from the just-updated rating), and sends each player their own
  `RatingChanged`. `onMessage` funnels the request through `match.submit(...)`, cross-checks the
  move/jump's declared color+kind against the board before forwarding to `GameEngine` (rejecting a
  mismatch with `REJECT token_mismatch`), then calls `broadcastState()`; `onStart` calls
  `match.start(this::broadcastState)` so the periodic tick also calls it, not just each reactive
  update. `handleLogin` calls `match.assignSeat()` first (rejecting `table_full` before touching
  the database), then `UserStore.find`/`createUser`/`checkPassword` — a brand-new username is
  created at the default rating (1200), an existing one must pass `checkPassword` or the login is
  rejected `bad_credentials`; either way the reply is `WELCOME <color> <rating>` and a `Session`
  (carrying that rating, mutable, alongside the existing mutable `selectedCell`) is seated.
  `handleNewGame` requires an existing session (`not_logged_in` otherwise) and the match to
  actually be over (`game_in_progress` otherwise). `broadcastState()` sends each connection its
  *own* `StateMessage`: for each `Session`, it looks up whatever piece sits on that session's
  `selectedCell()` and only passes the selection through to `GameEngine.snapshot(...)` (populating
  `LEGAL` squares) if the piece's color matches `session.assignedColor()` — reselected fresh every
  broadcast off the live board, so a player can never see legal-destination highlights for a piece
  they don't own, including one that only became the opponent's mid-flight. `UserStore`
  (`jdbc:sqlite:<url>`, one `Connection` held open for the store's whole lifetime — required for
  `jdbc:sqlite::memory:` to work at all across multiple calls, since a fresh connection to
  `:memory:` gets its own empty database) owns the `users` table (`username` primary key,
  `password_hash`/`password_salt`, `rating` defaulting to 1200) and creates it if missing on
  construction; `ServerMain` points it at `server-data/kongfu.db`, creating that directory first
  since the SQLite driver creates the database file but not missing parent directories.
  `PasswordHasher` is SHA-256 salted with `SecureRandom` (JDK-only — course project, not handling
  real user data). `EloCalculator.updatedRating` is the standard logistic-expectation formula,
  `K=32`.
- **`src/io/`** — `BoardParser`/`BoardPrinter`, plain-text board serialization, model-only
  dependency.
- **`src/view/`** — `GameSnapshot`/`PieceSnapshot`/`SelectionSnapshot` are passive, read-only DTOs
  built by `GameEngine.snapshot(...)` (pre-computed pixel positions, move-log text, legal-
  destination set — nothing in `view` re-derives game logic). `Renderer.render(GameSnapshot)` is a
  pure snapshot-to-`BufferedImage` function and must never import anything from
  `src.engine`/`src.model`/`src.realtime` beyond value types like `Piece.Color`/`Position`.
  `GameWindow` is the Swing shell (JFrame + mouse input + repaint loop) and must never import
  `GameEngine` either — it only holds a `Supplier<GameSnapshot>`, a `ClickHandler`, a `Renderer`, a
  `GameLoop`, and an `EffectsController`. `GameLoop` and `EffectsController` are the two classes in
  `view` allowed to reach into `engine`. `GameLoop` holds a `GameEngine` reference directly; its
  only job is advancing simulated time each tick (`engine.waitMs`) and reporting whether anything
  changed. `EffectsController` doesn't hold a `GameEngine` reference — it subscribes to `engine`-
  defined event records (`MoveEvent`, `GameOverEvent`) published on an `EventBus` to trigger one-
  shot sounds and a short "GAME START!" banner; `GuiMain` hands it `GameEngine`'s own bus directly,
  `ClientMain` hands it `NetworkGameProxy.eventBus()` — `EffectsController` doesn't know or care
  which mode it's in. `EffectsController`, its `SoundPlayer` interface, and the real
  `ClipSoundPlayer` implementation (tries `<root>/<name>.wav` via `javax.sound.sampled`, falls back
  to `Toolkit.beep()`) live in `src/view/sound/` — the one nested package in this project, kept
  separate so `EffectsController`'s tests can inject a fake `SoundPlayer` without touching real
  audio. `Renderer.drawBanner(BufferedImage, String)` paints the banner text. `Img` is a thin
  `BufferedImage` wrapper (load/resize/draw/text) — all pixel drawing goes through it, never raw
  `Graphics2D` calls scattered elsewhere. `AnimationConfig` loads a piece's per-state JSON
  (`speed_m_per_sec`, `next_state_when_finished`, `frames_per_sec`, `is_loop`) via regex, not a
  JSON library.

Because `Renderer`/`GameWindow` only ever see `GameSnapshot`, a rendering bug can be reproduced/
tested with a hand-built fake `GameSnapshot` — no real `Board`/`GameEngine`/`ClickHandler` needed.
