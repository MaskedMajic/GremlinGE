# GremlinGE

GremlinGE is a Java-first OSRS Grand Exchange flipping assistant built to grow into a RuneLite-native helper.

## Goal
Help identify strong **high-volume GE flips** and track your **buy limits / live offer state** without automating gameplay.

## Current status
### v0.3 complete
GremlinGE currently supports:
- live OSRS market data from the OSRS Wiki price API
- top high-volume flip candidate ranking
- item buy limits from mapping data
- local buy-limit tracking
- reset timer calculation
- manual CLI purchase logging as a temporary bridge

## Current commands
### Run scanner
```bash
gradle -q run
```

### Log a purchase manually (temporary)
```bash
gradle -q run --args='buy Death_rune 5000'
```

## Current output
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

## Project direction
The real destination is a **RuneLite-integrated helper** that:
- reads your live GE offer state
- detects fills / partial fills / completed offers
- infers buy-limit usage automatically
- surfaces high-volume flips worth rotating into next

## Next milestone: v0.4
### Live GE state reader
Before GremlinGE feels truly good, it needs to stop relying on manual logging and instead read:
- active GE offer slots
- item / quantity / price / side
- fill progress
- completed / partial / cancelled state

That will allow it to:
- track your limits automatically
- know when slots are open
- know what filled
- know what is still active

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
