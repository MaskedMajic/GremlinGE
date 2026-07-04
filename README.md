# GremlinGE

GremlinGE is a Java-first OSRS Grand Exchange flipping assistant built to grow into a RuneLite-native helper.

## Goal
Help identify strong **high-volume GE flips** and track your **buy limits / live offer state** without automating gameplay.

## Current status
### v0.4 in progress
GremlinGE now has two tracks living side-by-side:

**CLI scanner (v0.3 foundation):**
- live OSRS market data from the OSRS Wiki price API
- top high-volume flip candidate ranking
- item buy limits from mapping data
- local buy-limit tracking
- reset timer calculation
- manual CLI purchase logging as a temporary bridge

**RuneLite helper (v0.4 work-in-progress):**
- live GE offer snapshot reading
- active slot / item / price / quantity / side capture
- fill progress tracking
- event detection for placed / partial fill / completed / cancelled / cleared offers
- local snapshot persistence (`data/ge_snapshot.json`)
- local recent event persistence (`data/ge_events.json`)
- automatic buy-limit tracking from detected buy fills
- panel summary for open slots, active offers, partials, completes, recent events, limit status, and basic profit scaffolding

## Current commands
### Run scanner
```bash
gradle -q run
```

### Log a purchase manually (temporary CLI bridge)
```bash
gradle -q run --args='buy Death_rune 5000'
```

### Build project
```bash
./gradlew build -x test
```

## Current output
### CLI scanner
GremlinGE prints:
- item name
- current buy / sell prices
- margin
- volume tag
- GE buy limit
- 5 minute volume
- score
- remaining limit
- reset ETA

### RuneLite helper
GremlinGE surfaces:
- live active offers
- recent GE state-change events
- open-slot / partial / completed summary
- auto-tracked limit usage
- early profit-tracking scaffolding

## Project direction
The real destination is a **RuneLite-integrated helper** that:
- reads your live GE offer state
- detects fills / partial fills / completed offers
- infers buy-limit usage automatically
- surfaces high-volume flips worth rotating into next

## Next milestone focus
### Harden v0.4 live GE state awareness
The current priority is not fancy automation or bloated UI. It is tightening the live state model so GremlinGE can reliably:
- understand slot transitions across updates and restarts
- detect fills and replacements cleanly
- avoid noisy or misleading event spam
- persist enough local state to stay context-aware
- feed cleaner downstream limit/profit logic

### Immediate next steps
- improve edge-case GE event accuracy further
- make restart/resume behavior more state-aware
- strengthen profit matching from fill history
- keep panel improvements lightweight and state-focused

## What GremlinGE is NOT
GremlinGE is not meant to:
- place GE offers for you
- click anything in the game
- log you in/out
- automate gameplay

It is meant to be a **flipping brain / state-aware helper**, not a bot.

## Repo notes
- Java-first so the core logic can later be reused in a RuneLite plugin
- local runtime data stays out of git (`data/*.json`)
- build artifacts are ignored
- prefer `./gradlew` for reproducible local builds
