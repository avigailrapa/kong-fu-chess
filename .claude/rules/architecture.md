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
  adapter, split into two subpackages: `src/net/messages/` holds `WireMessage` and every class it
  `permits` (a sealed interface's permitted subclasses must share its package in an unnamed
  module, so these can't be split further), `src/net/client/` holds the client-only
  `NetworkGameProxy`/`ClientActivityLog`/`LoginResult`/`RoomCreateResult`/`RoomJoinResult`;
  `Protocol.java` and `MalformedMessageException.java` stay directly under `src/net/` since both
  client and server import them. `WireMessage` is a sealed interface (`MoveCommand`/`JumpCommand`/`MoveAccepted`/
  `MoveRejected`/`StateMessage`/`LoginCommand`/`Welcome`/`SelectCommand`/`MoveOccurred`/
  `GameOverMessage`/`NewGameCommand`/`RatingChanged`/`PlayCommand`/`CancelPlayCommand`/
  `MatchFound`/`MatchTimeout`/`DisconnectCountdown`/`RoomCreateCommand`/`RoomJoinCommand`/
  `RoomId`/`Spectating`). `Protocol.parse(String)`/`encode(WireMessage)` convert to/from the text
  sent over a WebSocket text frame: a bare 6-char move token (e.g. `WQe2e5`, no verb prefix),
  `JUMP <token>`, `OK`, `REJECT <reason>`, `NEWGAME` (client asks the server for a fresh game —
  only accepted once the current one is over), `PLAY`/`CANCEL_PLAY` (join/leave the matchmaking
  queue), `ROOM_CREATE`/`ROOM_JOIN <roomId>` (create or join a private room instead of
  matchmaking), a multi-line `STATE`/`PIECE`/`SELECT`/`LEGAL`/`WLOG`/`BLOG`/`ENDSTATE` block that
  flattens/reconstructs a whole `GameSnapshot`, or the server-to-client-only `EVENT_MOVE
  <color><kind><from><to> <capture:0/1> <kingCapture:0/1> <promotion:0/1> <requestTimestampMs>` /
  `EVENT_GAMEOVER <color|->` / `RATING <newRating>` / `MATCH_FOUND <opponentUsername> <color>
  <opponentRating>` / `MATCH_TIMEOUT` / `DISCONNECT_COUNTDOWN <secondsRemaining>` / `ROOM_ID
  <roomId>` / `SPECTATING` — the first two wrap the engine's own `MoveEvent`/`GameOverEvent`
  records, same pattern `StateMessage` uses for `GameSnapshot`; `RatingChanged` is sent to one
  connection at a time, not broadcast, since the two players' post-game ratings differ. `LOGIN
  <username> <password>` / `WELCOME <rating>` round out the login exchange — `WELCOME` carries no
  color because login no longer seats a player (see `src/server/` below); color is only learned
  once `MATCH_FOUND` arrives, whether pairing came from matchmaking or a filled room. Rejection
  reasons beyond the engine's own (`game_over`/`motion_in_progress`/etc.) include
  `not_your_piece`/`not_logged_in`/`already_in_match`/`bad_credentials`/`token_mismatch`/
  `not_in_match`/`game_in_progress`/`room_not_found`/`spectator` (the last one specifically for a
  spectator attempting `MOVE`/`JUMP`/`NEWGAME`). `MalformedMessageException` is what `parse` throws
  on any bad input; never lets any other exception type escape. `NetworkGameProxy` (`extends
  org.java_websocket.client.WebSocketClient`, `implements GameCommands`) is the client-side stand-
  in for `GameEngine`: `isOccupied` and the color/kind needed for a move token are answered from a
  locally-cached `GameSnapshot` (zero round-trip); `requestMove`/`login`/`createRoom`/`joinRoom`
  block the calling thread on a `CompletableFuture` up to a timeout, matched to its reply via a
  FIFO queue (not a single shared slot) so a late reply to an abandoned/timed-out request can't be
  misdelivered — relies on one WebSocket connection delivering frames in send order both ways, and
  `GameServer` replying to a connection's messages in the order received. `requestJump`/`newGame`/
  `play`/`cancelPlay`/`updateSelection` stay fire-and-forget, matching `GameCommands`' existing
  asymmetry. `NetworkGameProxy` also owns its own `EventBus` (`eventBus()`) — `onMessage`
  republishes `MoveEvent`/`GameOverEvent` unwrapped from `MoveOccurred`/`GameOverMessage`, plus
  `RatingChanged`/`MatchFound`/`MatchTimeout`/`DisconnectCountdown` as-is, so `EffectsController`
  and `ClientMain` can subscribe over the network exactly as they would to a local `GameEngine`'s
  bus; also tracks the latest `RatingChanged`/`Welcome` value via `latestRating()`. `RoomId`/
  `Spectating` are *not* published on the bus — they complete the pending `createRoom`/`joinRoom`
  future directly, since (unlike `MatchFound`) they're always a direct reply to a request the same
  client just made. `ClientActivityLog` (append-only timestamped log to a file + stdout) lives here
  rather than in `src/server/` purely so the client doesn't have to depend on the server package
  for it — it's a near-duplicate of `src/server/ActivityLog.java`, kept as two small classes rather
  than one shared one to respect the one-directional layering.
- **`src/server/`** — `Match` owns a `GameEngine` + a `MoveLogger` wired to it, on its own
  single-threaded `ScheduledExecutorService`; `start(Runnable onTick)` schedules a periodic
  `engine.waitMs(tickIntervalMs)` followed by the callback, `submit(Runnable)` funnels any other
  work (incoming messages) onto that same thread — `GameEngine`/`RealTimeArbiter` have no internal
  synchronization and must never be touched concurrently. `engine`/`moveLogger` are replaceable,
  not `final`: `newGame(GameEngine)` swaps in a fresh engine and reruns any registered
  `onNewGame(Runnable)` listener, since anything subscribed to the *old* engine's `EventBus` would
  otherwise keep listening to a bus nothing publishes to anymore. `Match` tracks two independent
  `Session` lists: `seated` (the two players, `addSession`/`seated()`) and `spectators`
  (`addSpectator`/`spectators()`) — kept separate because ELO updates, disconnect handling, and
  `assignSeat()`'s two-seat cap only ever care about `seated`, while state/move/game-over
  broadcasts go to both. `Session` carries `assignedColor` (nullable until seated),
  `role` (`Role.WHITE`/`BLACK`/`SPECTATOR`, nullable until seated or registered as a spectator),
  `rating`, and `selectedCell`, all mutable — a `Session` is created once at login and reused
  across matchmaking/room flows, never rebuilt. `GameServer extends
  org.java_websocket.server.WebSocketServer`, composing one `UserStore`, one `ActivityLog`, one
  `MatchmakingQueue`, one `RoomRegistry`, and `Protocol` — unlike Level 2-3, it does **not** own a
  single `Match`; instead `Map<Session, Match> matchBySession` (plus `Map<WebSocket, Session>
  sessionsByConnection`) lets arbitrarily many matches run concurrently, each ticking on its own
  executor. `handleLogin` only authenticates/creates the `UserStore` record and constructs a
  `Session` — it no longer seats anyone or assigns a color, so `WELCOME <rating>` is all it can
  report at that point. Pairing happens two ways, both funneling into the same private
  `wireAndStartMatch(Match)` (subscribes `MoveEvent`/`GameOverEvent` via
  `subscribeToEngineEvents()`, calls `match.start(() -> broadcastState(match))`, logs "`<white> vs
  <black> - match started`", sends each seated player their own `MatchFound` with the *opponent's*
  username/rating and their *own* assigned color): (1) `PLAY` enqueues the session into
  `MatchmakingQueue` (constructor-injected `onPaired`/`onTimeout` callbacks, pairs any two waiting
  sessions within a ±100 rating window, 60s wait before `MatchTimeout`); its `onPaired` builds a
  fresh `Match` off the standard starting position, seats both via the shared `seat(Match,
  Session)` helper (`match.assignSeat()` for color, sets `session.role()` to `WHITE`/`BLACK`,
  registers `matchBySession`), then calls `wireAndStartMatch`. (2) `ROOM_CREATE`/`ROOM_JOIN
  <roomId>` go through `RoomRegistry` (also constructor-injected: a `Supplier<Match> matchFactory`,
  the same `seat` callback as matchmaking, `wireAndStartMatch` itself as the `onMatchReady`
  callback, and an `addSpectator` callback) — `createRoom(Session)` seats the creator as white and
  returns a random 6-char room ID (`[A-Z0-9]`, retried against a uniqueness check, no external
  dependency), `joinRoom(roomId, Session)` returns `JoinOutcome.SEATED_BLACK` (seats the joiner,
  triggers `onMatchReady`) if the room still has an open seat, `SPECTATING` (registers the joiner
  as a read-only spectator, no `MatchFound` since spectators aren't in `match.seated()`) once it's
  full, or `NOT_FOUND` for an unknown ID; both `RoomRegistry`'s public methods are `synchronized`
  so two simultaneous joins to the same room can't double-seat. A spectator's `MOVE`/`JUMP`/
  `NEWGAME` is rejected with `REJECT spectator` *before* any of the normal ownership/token checks
  run (mirrors the existing `not_your_piece` pattern) — `broadcastState`/`broadcastToMatch` still
  send every seated player's *and* every spectator's connection its own `StateMessage`/
  `MoveOccurred`/`GameOverMessage`, just with spectators always getting a `null` selection (no
  legal-move highlighting, since they own no color). `onClose` (a connection dropping) looks up the
  disconnected session's match and, if it's a seated player (not a spectator — the countdown only
  fires when `match.seated()` contains them), starts a per-second `DisconnectCountdown` broadcast
  to the opponent via a dedicated `disconnectTimers` executor, ending in
  `engine.resign(disconnectedColor)` (submitted through the match's own executor, never called
  directly) if the countdown reaches zero; a `GameOverEvent` subscriber,
  `updateRatingsAfterGameOver`, looks up both seated `Session`s, runs `EloCalculator.updatedRating`
  for each (K=32, draws split 0.5/0.5 when `GameOverEvent.winner()` is `null`), persists both via
  `UserStore.updateRating`, updates each in-memory `Session.rating()` in place (so a same-session
  rematch after `NEWGAME` starts from the just-updated rating), and sends each player their own
  `RatingChanged`. `onMessage` funnels the request through `match.submit(...)` when the connection
  is already in a match (`null` `matchFor(conn)` — e.g. still in matchmaking, or the very `PLAY`/
  `ROOM_JOIN` message that's about to create the match — just runs inline instead), cross-checks a
  move/jump's declared color+kind against the board before forwarding to `GameEngine` (rejecting a
  mismatch with `REJECT token_mismatch`), then calls `broadcastState()` if a match existed *before*
  the message was handled — a freshly-paired/freshly-joined match gets its first `STATE` from the
  next scheduled tick, not synchronously, same as the existing matchmaking behavior. `handleNewGame`
  requires an existing session (`not_logged_in` otherwise), rejects spectators, and requires the
  match to actually be over (`game_in_progress` otherwise). `ActivityLog` (`log(String)` — appends
  an ISO-8601-timestamped line to a file and echoes it to stdout, one `PrintWriter` held open for
  the instance's lifetime) records logins (including rejected ones), room create/join/spectate,
  match start, game over, and disconnect/auto-resign; `ServerMain` points it at
  `server-data/activity.log`. `UserStore` (`jdbc:sqlite:<url>`, one `Connection` held open for the
  store's whole lifetime — required for `jdbc:sqlite::memory:` to work at all across multiple
  calls, since a fresh connection to `:memory:` gets its own empty database) owns the `users` table
  (`username` primary key, `password_hash`/`password_salt`, `rating` defaulting to 1200) and
  creates it if missing on construction; `ServerMain` points it at `server-data/kongfu.db`,
  creating that directory first since the SQLite driver creates the database file but not missing
  parent directories. `PasswordHasher` is SHA-256 salted with `SecureRandom` (JDK-only — course
  project, not handling real user data). `EloCalculator.updatedRating` is the standard
  logistic-expectation formula, `K=32`.
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
  JSON library. `HomeScreen` is the pre-game Swing screen `ClientMain` shows before any
  `GameWindow` exists (Play/Cancel for matchmaking, a Room button opening a modal `JDialog` with
  Create/Join/Close) — like `GameWindow`, it never imports `engine`/`net` itself; its constructor
  takes four callbacks (`onPlayClicked`, `onCancelClicked`, `onRoomCreate`, `onRoomJoin`) and
  `ClientMain` is the only place that wires those to `NetworkGameProxy` calls, so `HomeScreen`
  stays reusable regardless of what's on the other end of the callback.

Because `Renderer`/`GameWindow` only ever see `GameSnapshot`, a rendering bug can be reproduced/
tested with a hand-built fake `GameSnapshot` — no real `Board`/`GameEngine`/`ClickHandler` needed.
