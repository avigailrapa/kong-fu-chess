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
  exception, for per-piece speed from the asset JSON). Its own `DEFAULT_PIECES_ROOT` constant
  (`"assets/pieces"`, used by the convenience one-`IBoard`-arg constructor) is a private literal,
  not sourced from `view.Renderer` — the design PDF's layer table forbids `RealTimeArbiter` from
  owning rendering, and the dependency direction must never point from a core layer up to `view`,
  even for a single default-path string. Zero knowledge of `GameState` — king capture is only
  *reported* via `ArrivalEvent`, never acted on here.
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
  `GameCommands` call, implemented by `GameEngine`). Never depends on
  `RuleEngine`/`RealTimeArbiter`/`Board` directly; only calls through `GameEngine`/`GameCommands`.
- **`src/net/`** — wire protocol shared by client and server, plus the client-side network
  adapter, split into two subpackages: `src/net/messages/` holds `WireMessage` and every class it
  `permits` (a sealed interface's permitted subclasses must share its package in an unnamed
  module, so these can't be split further), `src/net/client/` holds the client-only
  `NetworkGameProxy`/`LoginResult`/`RoomCreateResult`/`RoomJoinResult`;
  `Protocol.java` and `MalformedMessageException.java` stay directly under `src/net/` since both
  client and server import them. `WireMessage` is a sealed interface (`MoveCommand`/`JumpCommand`/`MoveAccepted`/
  `MoveRejected`/`StateMessage`/`LoginCommand`/`Welcome`/`SelectCommand`/`MoveOccurred`/
  `GameOverMessage`/`NewGameCommand`/`RatingChanged`/`PlayCommand`/`CancelPlayCommand`/
  `MatchFound`/`MatchTimeout`/`DisconnectCountdown`/`RoomCreateCommand`/`RoomJoinCommand`/
  `RoomId`/`Spectating`/`WelcomeBack`/`OpponentReconnected`/`AuthCommand`). `Protocol.parse(String)`/
  `encode(WireMessage)` convert to/from the text
  sent over a WebSocket text frame: a bare 6-char move token (e.g. `WQe2e5`, no verb prefix),
  `JUMP <token>`, `OK`, `REJECT <reason>`, `NEWGAME` (client asks the server for a fresh game —
  only accepted once the current one is over), `PLAY`/`CANCEL_PLAY` (join/leave the matchmaking
  queue), `ROOM_CREATE`/`ROOM_JOIN <roomId>` (create or join a private room instead of
  matchmaking), a multi-line `STATE`/`PIECE`/`SELECT`/`LEGAL`/`WLOG`/`BLOG`/`ENDSTATE` block that
  flattens/reconstructs a whole `GameSnapshot`, or the server-to-client-only `EVENT_MOVE
  <color><kind><from><to> <capture:0/1> <kingCapture:0/1> <promotion:0/1> <requestTimestampMs>` /
  `EVENT_GAMEOVER <color|->` / `RATING <newRating>` / `MATCH_FOUND <opponentUsername> <color>
  <opponentRating>` / `MATCH_TIMEOUT` / `DISCONNECT_COUNTDOWN <secondsRemaining>` / `ROOM_ID
  <roomId>` / `SPECTATING` / `WELCOME_BACK <rating>` / `OPPONENT_RECONNECTED` — the first two wrap
  the engine's own `MoveEvent`/`GameOverEvent` records, same pattern `StateMessage` uses for
  `GameSnapshot`; `RatingChanged` is sent to one connection at a time, not broadcast, since the two
  players' post-game ratings differ. `LOGIN <username> <password>` / `WELCOME <rating>` round out
  the ordinary login exchange — `WELCOME` carries no color because login no longer seats a player
  (see `src/server/` below); color is only learned once `MATCH_FOUND` arrives, whether pairing came
  from matchmaking or a filled room. `AUTH <token>` is an additive sibling to `LOGIN` (never
  replaces it — `LoginCommand` keeps working exactly as before, for the monolithic path and any
  client that still wants to send a password directly over the wire): a client that already holds
  a token from `app.ApiGatewayMain`'s `POST /login` sends `AUTH <token>` instead of resending its
  password, and gets back the same `WELCOME`/`WELCOME_BACK` reply `LOGIN` would have produced.
  Only `Matchmaker` handles `AUTH` — the monolithic `Lobby`/`SessionAuthHandler` never learned it,
  by design (no `ApiGatewayMain` exists for that deployment shape). If the same username has a pending disconnect countdown (see
  `ReconnectManager` below), `LOGIN` resolves to `WELCOME_BACK <rating>` instead and the
  *opponent's* connection gets an unsolicited `OPPONENT_RECONNECTED` so its client can clear any
  "waiting for reconnect" UI. Rejection
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
  the server (`GameServer` delegating to `Lobby`, see `src/server/` below) replying to a
  connection's messages in the order received. `requestJump`/`newGame`/
  `play`/`cancelPlay`/`updateSelection` stay fire-and-forget, matching `GameCommands`' existing
  asymmetry. `NetworkGameProxy` also owns its own `EventBus` (`eventBus()`) — `onMessage`
  republishes `MoveEvent`/`GameOverEvent` unwrapped from `MoveOccurred`/`GameOverMessage`, plus
  `RatingChanged`/`MatchFound`/`MatchTimeout`/`DisconnectCountdown` as-is, so `EffectsController`
  and `ClientMain` can subscribe over the network exactly as they would to a local `GameEngine`'s
  bus; also tracks the latest `RatingChanged`/`Welcome` value via `latestRating()`. `RoomId`/
  `Spectating` are *not* published on the bus — they complete the pending `createRoom`/`joinRoom`
  future directly, since (unlike `MatchFound`) they're always a direct reply to a request the same
  client just made.
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
  across matchmaking/room flows, never rebuilt. `Session.connection()` is typed as
  `ClientConnection` (a one-method `send(String)` interface), not the raw WebSocket `WebSocket` —
  the indirection exists so `reconnectSession` can swap a session's underlying transport
  (`session.connection(conn::send)`) when a player's client opens a *new* socket after a drop,
  and so tests can hand a `Session` a fake sink without a real socket. `GameServer extends
  org.java_websocket.server.WebSocketServer` is now a thin adapter: its `onOpen`/`onClose`/
  `onMessage`/`onError` overrides do nothing but delegate to one composed `Lobby`
  (`onClose` → `lobby.disconnect(conn)`, `onMessage` → `lobby.receive(conn, message)`); it holds no
  session/match state itself.

  `Lobby` is where the composition unlike Level 2-3 actually lives: it owns the single `UserStore`,
  `MatchmakingQueue`, `RoomRegistry`, and `ReconnectManager`, plus
  `Map<WebSocket, Session> sessionsByConnection` and `Map<Session, Match> matchBySession` — the
  latter is what lets arbitrarily many matches run concurrently, each ticking on its own executor.
  `Lobby.receive` logs the raw inbound text, resolves the connection's current `Match` (if any) via
  `matchFor`, and routes the actual `handleMessage` dispatch through `match.submit(...)` when one
  exists so `GameEngine`/`RealTimeArbiter` are never touched concurrently, or runs it inline
  otherwise (still-in-matchmaking, or the very `PLAY`/`ROOM_JOIN` message about to create the
  match); it calls `broadcastState(match)` afterward only if a match existed *before* the message
  was handled — a freshly-paired/freshly-joined match gets its first `STATE` from the next
  scheduled tick, not synchronously. `handleLogin` only authenticates/creates the `UserStore`
  record and constructs a `Session` — it no longer seats anyone or assigns a color, so
  `WELCOME <rating>` is all it can report at that point — *unless* `ReconnectManager.pendingFor`
  finds a live disconnect countdown for that username, in which case it re-validates the password,
  cancels the countdown, and calls `reconnectSession` (rebinds `session.connection()` to the new
  socket, notifies the opponent with `OpponentReconnected`, replies `WelcomeBack`) instead of
  creating a fresh `Session`. Pairing happens two ways, both funneling into the same private
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
  legal-move highlighting, since they own no color). `Lobby.disconnect` (called from `onClose`)
  looks up the disconnected session's match and, if it's a seated player (not a spectator — the
  countdown only fires when `match.seated()` contains them), calls
  `startDisconnectCountdown`, which delegates the actual timing to `ReconnectManager.startCountdown`
  — a small standalone class (its own single-thread `ScheduledExecutorService`, keyed by
  username in a `Map<String, Entry>`) that schedules one callback per elapsed second up to the
  configured countdown length: `onTick` broadcasts `DisconnectCountdown` to the opponent each
  second, and `onExpire` fires once with no remaining reconnect window. `Lobby` wires `onExpire` to
  `match.submit(() -> match.engine().resign(disconnectedColor))` (never called directly, always
  through the match's own executor); if the same username logs back in before expiry,
  `ReconnectManager.cancelCountdown` cancels every remaining scheduled tick and the reconnect path
  above runs instead of a resignation. `ReconnectManager` knows nothing about `WireMessage`/
  `Protocol` — `Lobby` is the only place that translates its ticks/expiry into wire messages, so
  the countdown logic itself stays unit-testable without a socket. A `GameOverEvent` subscriber,
  `updateRatingsAfterGameOver`, looks up both seated `Session`s, runs `EloCalculator.updatedRating`
  for each (K=32, draws split 0.5/0.5 when `GameOverEvent.winner()` is `null`), persists both via
  `UserStore.updateRating`, updates each in-memory `Session.rating()` in place (so a same-session
  rematch after `NEWGAME` starts from the just-updated rating), and sends each player their own
  `RatingChanged`. A second `GameOverEvent` subscriber on the same `subscribeToEngineEvents`,
  `GameHistoryService.recordGameOver` (in `src/server/handlers/`, alongside `RatingService` —
  see `src/server/history/` below), fires right next to `RatingService` and persists the completed
  game; it's a fire-and-forget subscriber, so tests that don't care about history are unaffected by
  its presence. `handleNewGame`
  requires an existing session (`not_logged_in` otherwise), rejects spectators, and requires the
  match to actually be over (`game_in_progress` otherwise). `UserStore` (in `src/server/auth/`, plain `jdbc:postgresql:...` URL —
  originally SQLite, migrated to Postgres for the reasons in Server_Design.md's SQLite section; one
  `Connection` held open for the store's whole lifetime, no pooling) owns the `users` table
  (`username` primary key, `password_hash`/`password_salt`, `rating` defaulting to 1200) and
  creates it if missing on construction; `ServerMain` reads the Postgres URL from
  `server.properties` (`postgresUrl` key), `app.GameNodeMain`/`app.MatchmakerMain`/
  `app.ApiGatewayMain` from the `POSTGRES_URL` env var. `PasswordHasher` is SHA-256 salted with
  `SecureRandom` (JDK-only — course project, not handling real user data). `EloCalculator.updatedRating`
  is the standard logistic-expectation formula, `K=32`. Note `Lobby` (and therefore
  `RatingService`/`GameHistoryService`/`ReconnectManager`/`EloCalculator`) is not
  monolithic-`GameServer`-only — `app.GameNodeMain` constructs the identical `Lobby` class per
  Game Node in the clustered deployment (see `src/server/cluster/` below), so all of this applies
  equally to both deployment shapes. `src/server/auth/` also holds `TokenStore` (interface:
  `mint(username) -> token`, `resolve(token) -> Optional<username>`) with `RedisTokenStore` (real,
  `SETEX`-backed with a TTL — same key-prefix/TTL shape as `RedisConnectionDirectory` below) and
  `InMemoryTokenStore` (test fake) implementations — minted by `app.ApiGatewayMain`'s `POST
  /login`, resolved by `Matchmaker`'s `AUTH <token>` handler (see `src/net/` and
  `src/server/matchmaker/` below). Unlike `UserStore`, `TokenStore` is never touched by the
  monolithic `Lobby`/`GameServer` path — only `Matchmaker` and `ApiGatewayMain` use it.
- **`src/server/cluster/`** — the NATS wire bridge that makes the monolithic `Lobby`/`GameServer`
  shape into several independently-deployable processes (`app.WsGatewayMain`/`MatchmakerMain`/
  `GameAllocatorMain`/`GameNodeMain`/`ApiGatewayMain`, wired up in `docker-compose.yml` and
  `k8s/`) without changing `Lobby`/`MatchOrchestrator`/`GameActionHandler` themselves — every
  cluster process still ends up calling the same `Lobby.receive`/`disconnect` methods the
  monolithic `GameServer` calls directly. `ClusterProtocol` is the subject-naming/encoding
  authority: `ws.in.lobby`/`ws.disconnect.lobby` (pre-match traffic → `Matchmaker`),
  `ws.in.node.<nodeId>`/`ws.disconnect.node.<nodeId>` (in-match traffic → that Game Node's
  `Lobby`), `game-allocator.assign` (`Matchmaker` → `GameAllocator`, no node id yet — allocation
  hasn't happened), `gamenode.create-match.<nodeId>`/`gamenode.reconnect.<nodeId>`
  (`GameAllocator`/`Matchmaker` → a specific Game Node — named `gamenode.*` not `coordinator.*`
  since `GameNodeBridge` is the actual subscriber regardless of who publishes),
  `gamenode.match-ended` (a Game Node → `Matchmaker`, so `Matchmaker` can
  `ConnectionDirectory.clear(...)` both players), `ws.out.<connId>` (any process → `WsGateway`,
  the only subject a WS client's replies ever travel on). `WsGateway extends
  org.java_websocket.server.WebSocketServer`: classifies each inbound frame with a cheap prefix
  check (`LOGIN`/`AUTH`/`PLAY`/`CANCEL_PLAY`/`ROOM_CREATE`/`ROOM_JOIN` → `ws.in.lobby`; anything
  else → `ConnectionDirectory.nodeForConnection` → that node's subject, or a direct
  `REJECT not_in_match` with zero NATS round-trip if the connection isn't routed to a match yet)
  — it never imports `RuleEngine`/`GameEngine`/`Lobby`, matching the design PDF's "Gateway holds
  no game state" rule from Server_Design.md. `GameNodeBridge` (owned by each `app.GameNodeMain`)
  subscribes only its own node's four subjects, builds a fresh `Match`+`Session`s via
  `Lobby.createAssignedMatch` on `gamenode.create-match.<nodeId>`, and publishes
  `gamenode.match-ended` from a `GameOverEvent` subscriber re-registered on every `newGame()` (the
  same re-registration pattern `MatchOrchestrator.subscribeToEngineEvents` already uses).
  `MatchmakerBridge` is the equivalent for `Matchmaker` (subscribes the three lobby-facing
  subjects). `NatsMatchDispatcher implements Matchmaker.MatchDispatcher` — `assign(matchId,
  players)` publishes to `game-allocator.assign`, `reconnect(nodeId, username, connectionId)`
  publishes straight to that node's `gamenode.reconnect.<nodeId>` (a lookup, not an allocation
  decision, so it skips `GameAllocator` entirely). `NatsClientConnection implements
  ClientConnection` — `send(text)` just publishes to `ClusterProtocol.outboundSubject(connectionId)`;
  every cluster `Session` holds one of these instead of a raw `WebSocket`, which is what lets the
  exact same `Lobby`/`RatingService`/`GameHistoryService` code path work identically whether a
  reply is going out over a real socket (`GameServer`) or over NATS to whichever `WsGateway`
  happens to hold that connection.
- **`src/server/matchmaker/`** — `Matchmaker` is the pre-match half of what used to be the
  single-process `Coordinator` (Phase 1; superseded, package removed): owns `SessionRegistry`,
  `MatchmakingQueue`, `RoomRegistry<RoomState>` (all reused unchanged from `src/server/matchmaking/`
  — the same classes the monolithic `MatchOrchestrator` uses), and a **read-only**
  `ConnectionDirectory` (`nodeForUser` for reconnect detection, `clearConnection`/`clearUser` on
  disconnect/match-end). Handles `LOGIN`/`AUTH`/`PLAY`/`CANCEL_PLAY`/`ROOM_CREATE`/`ROOM_JOIN`;
  `handleLogin`/`handleAuth` both funnel into a shared private `establishSession` (reconnect check,
  then either dispatch a reconnect or register a fresh `Session` and reply `WELCOME`) so the two
  entry points can't drift. Unlike the old `Coordinator`, `Matchmaker` does **not** decide which
  Game Node a match runs on and does **not** write to `ConnectionDirectory` — `dispatchNewMatch`
  builds the colored `PlayerAssignment` list exactly as before, then hands it to
  `MatchDispatcher.assign(matchId, players)` (no `nodeId` parameter — that decision belongs to
  `GameAllocator` now) instead of picking a node and dispatching itself.
- **`src/server/allocator/`** — the node-placement half of the old `Coordinator`, now its own
  service. `Allocator.pickNode()` is unchanged (round-robin over the `GAME_NODE_IDS` env var).
  `ConnectionDirectory` (interface: `assign`/`nodeFor`/`clear`, both connection- and
  username-keyed) moved here from the removed `coordinator` package since `GameAllocator` is now
  the sole **writer** (`Matchmaker` only reads it) — `RedisConnectionDirectory` (`kongfu:conn:*`/
  `kongfu:user:*` keys, 24h TTL via `JedisPooled`) is the real implementation, shared by
  `WsGateway`/`Matchmaker`/`GameAllocator`; `InMemoryConnectionDirectory` is the test fake.
  `GameAllocator` subscribes `game-allocator.assign`, decodes the
  `ClusterProtocol.CreateMatchCommand` payload `Matchmaker` published, calls `allocator.pickNode()`,
  writes both players into `ConnectionDirectory`, then **republishes the identical payload bytes**
  (no re-encoding) to `ClusterProtocol.createMatchSubject(nodeId)` — `GameNodeBridge` doesn't
  change at all, it never knew or cared that the publisher used to be `Coordinator` and is now
  `GameAllocator`. Fire-and-forget by design, same as the dispatch it replaced; a `log.warn` on the
  decode-failure path is the only defense against a malformed message, no retry/timeout — matches
  this project's course-scope tolerance for "best effort" cluster messaging.
- **`src/server/history/`** — `GameStore` (mirrors `UserStore`'s exact JDBC shape: one long-lived
  `Connection`, `CREATE TABLE IF NOT EXISTS` on construct, try-with-resources `PreparedStatement`
  per call) owns the `games` table (`game_id` primary key — a fresh `UUID` per completed game, not
  `Match.matchId()`, since the same `Match`/room can be replayed via `NEWGAME` and each play
  deserves its own history row; `white`/`black`/`winner` — `Piece.Color.letter()` or `"-"` for a
  draw, same convention `Protocol`'s `EVENT_GAMEOVER`/`STATE` encoding already uses;
  `white_moves`/`black_moves` — `;`-joined `MoveEvent.algebraicMove()` strings, a plain TEXT
  column rather than a JSON library, matching this project's established
  regex-over-JSON-library convention (`view.AnimationConfig`); `ended_at`). `GameHistoryService`
  (in `src/server/handlers/`, not here — mirrors `RatingService`'s shape and lives beside it)
  is the actual `GameOverEvent` subscriber that calls `GameStore.save(...)`; `GameStore` itself
  has no engine-event knowledge. `GET /history/{username}` on `app.ApiGatewayMain` is the only
  reader, via `GameStore.forUser` (a small `GameRecord` list — move history intentionally left out
  of that endpoint, would need a `forMatch`-style lookup to add later if wanted).
- **`src/server/health/`** — `HealthServer` wraps a plain JDK `com.sun.net.httpserver.HttpServer`
  (zero new dependency): ctor takes a port and a `Supplier<Map<String, Object>>` for one
  service-specific gauge, `GET /health` hand-builds `{"status":"ok","uptimeMs":N,...gauges}` (same
  no-JSON-library convention as `src/server/history/` above). Every `app.*Main` for the clustered
  deployment starts one on a `HEALTH_PORT` env var right before blocking on
  `Thread.currentThread().join()`: `WsGatewayMain` reports `connections`
  (`WsGateway.connectionCount()`), `GameNodeMain` reports `activeMatches`
  (`Lobby.activeMatchCount()`, via `SessionRegistry.activeMatchCount()` — distinct `Match`
  objects across all bound sessions), `MatchmakerMain` reports `queuedPlayers`
  (`Matchmaker.queuedPlayerCount()`, via `MatchmakingQueue.size()`), `GameAllocatorMain` reports
  `matchesAllocated` (a plain `AtomicLong` on `GameAllocator`), `ApiGatewayMain` reports no extra
  gauge. `docker-compose.yml`'s `healthcheck:` blocks and `k8s/`'s `readinessProbe`/`livenessProbe`
  both point at this same endpoint — deliberately lightweight (logs + health checks only, no
  metrics/tracing/alerting stack) per explicit user decision, not a partial implementation of a
  bigger observability plan.
- **`src/io/`** — `BoardParser`, plain-text board serialization, model-only dependency.
- **`src/view/`** — `GameSnapshot`/`PieceSnapshot`/`SelectionSnapshot` are passive, read-only DTOs
  built by `GameEngine.snapshot(...)` (pre-computed pixel positions, move-log text, legal-
  destination set — nothing in `view` re-derives game logic). `Renderer.render(GameSnapshot)` is a
  pure snapshot-to-`BufferedImage` function and must never import anything from
  `src.engine`/`src.model`/`src.realtime` beyond value types like `Piece.Color`/`Position`.
  `GameWindow` is the Swing shell (JFrame + mouse input + repaint loop) and must never import
  `GameEngine` either — it only holds a `Supplier<GameSnapshot>`, a `ClickHandler`, a `Renderer`, a
  `LongPredicate tickSource`, and an `EffectsController`. `tickSource` is what actually reaches
  into `engine` on each repaint tick; `app/ClientMain.java` supplies an inline lambda that just
  pushes the current click selection to `NetworkGameProxy` and always returns `true` —
  `GameWindow` itself never touches `engine` directly. `EffectsController` is the one class in
  `view` allowed to reach into `engine` — it doesn't hold a `GameEngine` reference, it subscribes
  to `engine`-defined event records (`MoveEvent`, `GameOverEvent`) published on an `EventBus` to
  trigger one-shot sounds and a short "GAME START!" banner; `ClientMain` hands it
  `NetworkGameProxy.eventBus()`. `EffectsController`, its `SoundPlayer` interface, and the real
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
