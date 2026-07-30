# Server Design - Condensed

A real-time game, 100M registered users, 10M concurrent players, each match
lasts 30-90 seconds, one action per player every 2 seconds.

## Current implementation status

Everything below this line describes the **target** architecture at 100M-user scale. What
actually runs today is a small, single-machine version of the same shape, not the scaled-out
counts in the table:

| Target row | What runs today |
|---|---|
| Gateway (~200) | One `WsGateway` (WebSocket) + one `ApiGatewayMain` (REST: `/login`, `/history/{username}`) |
| Matchmaking (~50-100) | One `Matchmaker` service (`app.MatchmakerMain`) |
| Allocator (~10-20) | One `GameAllocator` service (`app.GameAllocatorMain`), round-robin only (no least-loaded/health-aware picking yet) |
| Game Node (~700-800) | Two (`game-node-1`/`game-node-2`, `app.GameNodeMain`) |
| Presence/Reconnect | Folded into `Matchmaker` (`ConnectionDirectory`, Redis-backed) + each Game Node's existing `ReconnectManager`/`DisconnectHandler`, not a separate service |
| Rating Worker | Runs inline on the Game Node that hosted the match (`RatingService`, a `GameOverEvent` subscriber), not a separate worker/queue |
| User DB (32-64 shards) | One unsharded, unreplicated PostgreSQL instance |
| Redis (16-32 shards) | One unsharded Redis instance, and only for connection routing + REST auth tokens — the matchmaking queue and room registry are still in-process memory inside `Matchmaker`, not in Redis |

Two things from the target design that **are** already true today, at small scale: (1) the
database is only touched once a match ends (`RatingService`/`GameHistoryService`, both
`GameOverEvent` subscribers — nothing queries Postgres mid-match), and (2) room/matchmaking
routing across processes already relies on shared state (`ConnectionDirectory` in Redis) rather
than any one process's local memory, so a Game Node crashing only takes down the matches it was
hosting, not global matchmaking state.

Not built at any scale yet: Kafka (rating updates go straight to Postgres, no queue), DB
sharding/replication, and the load-bearing traffic-shape argument in "Why this design meets the
requirements" below (events-only outbound, no snapshot-per-tick) — the current wire protocol still
sends full `STATE` snapshots on tick, `EVENT_MOVE` exists but hasn't replaced snapshot broadcasting
as the primary channel.

Deployable today two ways: `docker-compose.yml` (one process per service, single machine) and
`k8s/` (the same set of services as Kubernetes Deployments/Services, verified against Docker
Desktop's local Kubernetes — not a real multi-node cluster).

## Which servers are needed, and what each one does

| Server | Role | Rough count |
|---|---|---|
| **Gateway** | Connects the client, authenticates it, routes it onward | ~200 |
| **Matchmaking** | Pairs two players by rating | ~50-100 |
| **Allocator** | Picks which Game Node a new match goes to | ~10-20 |
| **Game Node** | Actually runs the live matches (logic, timing, collisions) | ~700-800 |
| **Presence/Reconnect** | Tracks who's connected, handles disconnects | ~10-20 |
| **Rating Worker** | Updates ELO after a match ends | ~10-20 |
| **User DB** | Username, password, rating - sharded with replication | ~32-64 shards |
| **Redis** | Queue, rooms, presence - transient data only | ~16-32 shards |

**Key point:** no match gets its own server. One **Game Node** holds
thousands of small matches in memory at once (the same way `Match` already
runs its own independent tick loop today) — a 30-90s match is far too short
to spin up a dedicated container for.

## Database: would SQLite work for 100M users?

**No.** SQLite is a single file with a single writer, and that's the whole
problem:

- **One write lock.** Every login and rating update funnels through one
  file-level lock — there's no way to add more write capacity, since it's
  not a cluster, it's a file.
- **No built-in replication or sharding.** It runs on one machine. There's
  no native way to split 100M users across multiple nodes.
- **Single point of failure.** That one file/machine going down is a full
  outage, not a degraded one.

It's fine for a small project with a handful of users, but it has no growth
path to 100M.

**Instead:** a **sharded, replicated** database (e.g. CockroachDB, Spanner,
or Vitess/MySQL) — users are split across ~32-64 shards by
`hash(username)`, each shard replicated 3x for automatic failover. This is
the `User DB` row in the table above.

## How the servers talk to each other

```
Client → Gateway → Matchmaking queue (Redis) → pairing → Allocator
       → gets a Game Node address ← connects directly to it for the match

Game Node → (only when the match ends) → Kafka → Rating Worker → User DB
```

The rule: **while a match is running, no server calls the database.** The
database is touched exactly once, after the match ends — so a slow DB write
can never stall a live match.

## Room management & player routing: can anyone play with anyone, and join any room?

Yes — because room and matchmaking data is **shared and global**, not tied
to whichever Gateway or Game Node a player happens to be connected through.

- **Random opponents:** every waiting player, regardless of which Gateway
  they connected to, is placed into the **same** matchmaking queue in
  Redis. Matchmaking Workers pull pairs from that one shared queue, so any
  two compatible players anywhere can be matched, not just two players on
  the same server.
- **Specific rooms:** a room code isn't tied to one machine's memory — it's
  registered in a shared Redis **room registry**: `roomId → which Game Node
  is hosting it`. When a second player enters that room code, their Gateway
  looks up the registry (not its own local state) and redirects them to the
  *same* Game Node the room creator is on, no matter which Gateway either
  player connected through.

So a player's Gateway is just their nearest entry point — it holds no game
state itself, which is exactly what lets any player reach any opponent or
any room regardless of physical server or region.

## What happens when a server crashes

- **Game Node crashes** — its matches (up to 5,000-8,000) are voided, both
  players get a message and are re-queued; no real data loss since a match
  only lasts about a minute anyway.
- **Gateway crashes** — it's stateless, the client just connects to another
  one.
- **Matchmaking crashes** — the queue itself lives separately in Redis, any
  other worker just keeps processing it.
- **DB shard crashes** — every shard is replicated 3x, a new leader is
  elected automatically — no downtime.

## Why this design meets the requirements

**Inbound traffic:** 10M players × one action every 2s = 5M actions/sec. A
small message (~60 bytes) → about 2.4 Gbps total — low for modern
infrastructure, and split across 200 gateways.

**Outbound traffic (the critical point):** because pieces move over time
instead of teleporting, the client needs animation updates — but the server
must **not** send a full state snapshot on every tick (which is what the
current code does today), since that alone would generate tens of millions
of messages per second for no reason. The fix: send **events only** (motion
started/finished) with speed and timing, and let the client draw the motion
itself — exactly how `EVENT_MOVE` already works. That keeps outbound traffic
tied to the actual action rate (~15 Gbps total) instead of an arbitrary tick
rate, spread across ~700-800 Game Nodes.

**Match rate:** 5M concurrent matches ÷ ~60s average = about 83,000 new
matches per second. That's exactly why a container per match is impossible
(spinning one up takes longer than the match itself) — and it's also the
basis for the ~700-800 Game Node estimate (~8,000 matches per node).

**Bottom line:** no server waits on another mid-match — the database is
decoupled from the live stream, messages to the client are limited to real
events rather than ticks, so nothing gets stuck even under full load.
