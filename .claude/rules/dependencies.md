# lib/ dependency notes

No build tool resolves these — jars are hand-fetched from Maven Central and dropped into `lib/`.
`KongFu.iml`'s `jarDirectory` entry for `lib/` (non-recursive) is what makes IntelliJ pick up
anything placed there automatically.

- **Java-WebSocket-1.6.0.jar** — used by `src/net/`/`src/server/`.
- **slf4j-api-2.0.13.jar** — a real runtime dependency of Java-WebSocket despite its own docs
  calling it dependency-free. Without it, anything using `GameServer`/`NetworkGameProxy` throws
  `NoClassDefFoundError` the first time the library logs. With just the API jar and no logging
  backend, SLF4J 2.x prints one harmless one-time "no providers found" warning instead of failing.
- **lombok-1.18.46.jar** — compile-time only (an annotation processor, not a runtime dependency),
  but must be on `-processorpath`, not just `-cp`. On this project's JDK (26), implicit
  annotation-processor discovery via plain `-cp` does not run Lombok at all — no warning, it
  silently skips code generation, which then surfaces as a confusing "variable not initialized in
  the default constructor" error on any `@RequiredArgsConstructor` class, with no mention of
  Lombok anywhere in the output.
- **sqlite-jdbc-3.53.2.0.jar** — used by `src/server/UserStore.java`. No classifier needed;
  `org.xerial:sqlite-jdbc`'s default artifact bundles native binaries for every supported
  platform. Registers itself as a JDBC driver automatically via `META-INF/services`, so
  `DriverManager.getConnection("jdbc:sqlite:...")` works with no explicit `Class.forName` call.
