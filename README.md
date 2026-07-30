# Kung Fu Chess (KongFu)

A real-time chess variant: moves travel over simulated time instead of teleporting instantly,
pieces rest/cool down after moving, and two pieces can race for a square or cross paths mid-flight.

Played only through the server: a WebSocket client/server pair (`app/ServerMain.java` /
`app/ClientMain.java`, or the clustered deployment — see `.claude/rules/architecture.md`), with
login/rating, matchmaking, private rooms with spectating, and disconnect/reconnect handling.

## Requirements

- JDK 26
- No Maven/Gradle — plain `javac`, compiled by hand. Dependency jars live in `lib/` (see
  `.claude/rules/dependencies.md`).
- **Use PowerShell, not Git Bash** — Git Bash mangles the `;` classpath separator and
  POSIX-converts paths passed to `javac`/`java`, silently breaking multi-jar classpaths and
  multi-file compiles.

## Build

Compile the main sources (outputs to `out/`; needs `-processorpath` for Lombok — plain `-cp`
silently skips annotation processing on this project's JDK):

```powershell
$cp = (Get-ChildItem lib\*.jar | ForEach-Object { $_.FullName }) -join ";"
$proc = (Get-ChildItem lib\lombok*.jar).FullName
$files = Get-ChildItem -Recurse src\*.java, app\*.java | ForEach-Object { $_.FullName }
javac -d out -encoding UTF-8 -cp $cp -processorpath $proc $files
```

## Run

- **Server:**
  ```powershell
  java -cp "out;lib\*" app.ServerMain
  ```
  Reads `port`/`dataDir`/`postgresUrl` from `server.properties`; stores accounts/ratings/game
  history in Postgres (see `.claude/rules/dependencies.md`) and logs to `<dataDir>/server.log`.
- **Client:**
  ```powershell
  java -cp "out;lib\*" app.ClientMain [wsUrl]
  ```
  `wsUrl` defaults to `ws://localhost:8887`. Shows a home screen to log in, then play via
  matchmaking or create/join a private room.

Both need `lib\*` since they pull in the WebSocket jar transitively.

## Test

Compile tests against `out/` plus the JUnit jars in `lib/`:

```powershell
$cp = "out;" + ((Get-ChildItem lib\*.jar | ForEach-Object { $_.FullName }) -join ";")
$files = Get-ChildItem -Recurse test\*.java | ForEach-Object { $_.FullName }
javac -d out -cp $cp $files
```

There is no `junit-platform-console-standalone.jar` in `lib/`, so tests run via a hand-written
JUnit `Launcher` driver class rather than a single `java -jar` command — see
`.claude/rules/testing.md` for the driver source and the full list of test packages to select.

```powershell
javac -d out -cp "out;lib\*;." RunTests.java
java -cp "out;lib\*;." RunTests
```

## Project layout

- `app/` — entry points (`ServerMain`, `ClientMain`, and the clustered-deployment mains), package
  `app`.
- `src/` — the layered engine (`model` → `rules` → `realtime` → `engine` → `view`, plus
  `input`/`net`/`server`). See `.claude/rules/architecture.md` for full per-class contracts.
- `test/` — mirrors `src/` and `app/` package-for-package (minus the `src.` prefix).
- `lib/` — hand-fetched dependency jars.
- `assets/` — piece sprites and board image.
