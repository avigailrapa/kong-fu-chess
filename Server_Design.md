# Server Design - Condensed

A real-time game, 100M registered users, 10M concurrent players, each match
lasts 30-90 seconds, one action per player every 2 seconds.

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
