# lib/ dependency notes

No build tool resolves these — jars are hand-fetched from Maven Central and dropped into `lib/`.
`KongFu.iml`'s `jarDirectory` entry for `lib/` (non-recursive) is what makes IntelliJ pick up
anything placed there automatically.

- **Java-WebSocket-1.6.0.jar** — used by `src/net/`/`src/server/`.
- **slf4j-api-2.0.13.jar** — a real runtime dependency of Java-WebSocket despite its own docs
  calling it dependency-free. Without it, anything using `GameServer`/`NetworkGameProxy` throws
  `NoClassDefFoundError` the first time the library logs.
- **logback-classic-1.5.13.jar** / **logback-core-1.5.13.jar** — the logging backend for
  `slf4j-api`, replacing the earlier `slf4j-simple` (removed — it can only write to one target at
  a time, not console *and* file simultaneously). Configured by the single `logback.xml` at the
  project root, which always writes to the console and, when `${LOG_FILE}` is set, also to a named
  file (falls back to a bare `app.log` in the working directory otherwise). `logback.xml` is only
  found if the current directory is on the classpath (see the `.;` in CLAUDE.md's Server/Client run
  commands) — without it, Logback falls back to its built-in console-only default config, silently
  ignoring `logback.xml` and `LOG_FILE`. Whether an `app.*Main` entry point sets
  `System.setProperty("LOG_FILE", ...)` before its first logger call depends on whether it has
  somewhere durable to put the file: `ServerMain`/`ClientMain` (local processes, no container) and
  `GameNodeMain` (its `DATA_DIR` is volume-mounted per node in `docker-compose.yml`/`k8s`, e.g.
  `./data/server/node-1:/kongfu/data/server`) all set it, to `server.log`/`client.log`/
  `game-node.log` under their respective data directory. `MatchmakerMain`/`GameAllocatorMain`/
  `WsGatewayMain`/`ApiGatewayMain` deliberately do **not** — they're stateless cluster services with
  no volume mount backing them, so a log file written inside their container would just vanish on
  restart/reschedule while wasting I/O; per the standard container-logging convention (write to
  stdout, let the platform capture it), they rely on the always-on console appender alone, collected
  via `docker-compose logs`/`kubectl logs`. This is intentional, not an oversight — don't add
  `LOG_FILE` to those four without first giving them a real persistent volume to write into.
- **lombok-1.18.46.jar** — compile-time only (an annotation processor, not a runtime dependency),
  but must be on `-processorpath`, not just `-cp`. On this project's JDK (26), implicit
  annotation-processor discovery via plain `-cp` does not run Lombok at all — no warning, it
  silently skips code generation, which then surfaces as a confusing "variable not initialized in
  the default constructor" error on any `@RequiredArgsConstructor` class, with no mention of
  Lombok anywhere in the output.
- **postgresql-42.7.13.jar** — used by `src/server/auth/UserStore.java` and
  `src/server/history/GameStore.java` (replaced `sqlite-jdbc`, removed — see
  Server_Design.md's SQLite section for why: no cluster-wide write capacity, no
  replication/sharding). Registers itself as a JDBC driver automatically via `META-INF/services`,
  so `DriverManager.getConnection("jdbc:postgresql:...")` works with no explicit `Class.forName`
  call. `docker-compose.yml`'s `postgres` service is the dev/test target
  (`jdbc:postgresql://localhost:5432/kongfu?user=kongfu&password=kongfu`); tests read
  `TEST_POSTGRES_URL` via `test/server/auth/TestDatabase.java` (see testing.md) and require it
  running (`docker compose up -d postgres`) before the suite works at all.
- **jnats-2.20.4.jar** — the NATS Java client, used throughout `src/server/cluster/` (the wire
  bridge between `WsGateway`/`Matchmaker`/`GameAllocator`/each Game Node — see architecture.md).
  Pulls in **eddsa-0.3.0.jar** as a real transitive runtime dependency (NKey/Ed25519 auth support)
  even though most of this project's usage never touches NATS auth — same "the docs undersell a
  real transitive dependency" pattern as `slf4j-api` below; omitting it throws
  `NoClassDefFoundError` the first time a `Connection` is created.
- **jedis-5.2.0.jar** — the Redis client, used by `src/server/allocator/RedisConnectionDirectory.java`
  (connection/user → node routing, written by `GameAllocator`, read by `Matchmaker`/`WsGateway`)
  and `src/server/auth/RedisTokenStore.java` (REST-login-minted tokens, written by
  `app.ApiGatewayMain`, read by `Matchmaker`'s `AUTH <token>` handler). Pulls in
  **commons-pool2-2.12.0.jar** as a real transitive runtime dependency (connection pooling for
  `JedisPooled`) even for the simple unpooled-looking constructor call sites in this project —
  omitting it throws `NoClassDefFoundError` on first use, same pattern as `slf4j-api`/`eddsa`
  above.
- **`tools/nats-server.exe`** — *not* a jar, not in `lib/`, not checked into git (gitignored,
  platform-specific native binary). Fetched once by hand from
  `https://github.com/nats-io/nats-server/releases` into `tools/nats-server.exe`. Used only by
  `test/server/cluster/NatsServerProcess.java`, which forks/kills it per test run (real NATS
  protocol, no Docker needed) for `test/integration/ClusterCrossNodeTest.java`. `jnats` itself
  does **not** bundle an embeddable test-server helper despite some of its own documentation
  implying otherwise — `NatsServerRunner`-style helpers just fork this same real binary, so there
  was no way around fetching it. Missing the file fails loudly with a message pointing at the
  release URL, rather than silently skipping the test.
