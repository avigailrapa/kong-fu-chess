# Running tests

`lib/` only has `junit-jupiter-api`/`junit-jupiter-engine`/`junit-platform-*` jars — there is no
`junit-platform-console-standalone.jar`, so tests can't be run with a single `java -jar` command.
Drive the `org.junit.platform.launcher.Launcher` API directly with a throwaway driver class:

```java
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
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

Compile it against the same classpath as the tests, then run `java -cp out;lib\*;. RunTests`.

Test packages don't have a `test.` prefix (`test/engine/GameEngineTest.java` declares
`package engine;`), so `selectPackage`/`selectClass` use the bare package name.

`selectPackage("integration")` is easy to leave out silently — nothing fails loudly when a
package is never discovered, and omitting it has previously skipped `test/integration/` without
any error.

Full-suite runs take several seconds, not milliseconds: `test/server/MatchTest.java` and the
`test/integration/` server/client tests exercise a real `ScheduledExecutorService` tick loop and,
in a few cases, a real bound socket, so they wait on real wall-clock timing (generous timeouts,
not tight sleeps) rather than an injected `ms` argument the way `GameEngine.waitMs`/
`EffectsController.tick` do.

The suite now requires a live Postgres: `test/server/auth/TestDatabase.java` (`freshUserStore()`/
`freshGameStore()`) connects to `TEST_POSTGRES_URL`, default
`jdbc:postgresql://localhost:5432/kongfu?user=kongfu&password=kongfu` — start it with
`docker compose up -d postgres` (and wait for `pg_isready`) before running tests, or the whole
suite fails at the first `UserStore`/`GameStore` construction, not just the tests that obviously
need a database. `test/integration/ClusterCrossNodeTest.java` additionally needs
`tools/nats-server.exe` fetched once by hand (see dependencies.md) — it forks/kills a real,
ephemeral-port NATS broker per run via `test/server/cluster/NatsServerProcess.java`, so unlike
Postgres it does *not* need anything pre-started, just the binary present on disk.

`javac`/`java` print `warning: unknown enum constant Status.STABLE` noise from JUnit's `@API`
annotations (the `apiguardian` jar isn't in `lib/`) — harmless, not a real error.
