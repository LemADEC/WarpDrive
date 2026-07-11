# Navigation and Jump Performance

This update reduces server work around ship navigation, jump preparation, and recurring world lookups.

## Navigation Snapshots

Ship navigation data is split into two categories:

- live status: energy, cooldown, warmup, command, jump state, autopilot state, notices, and current movement preview
- cached heavy data: route data, destinations, celestial map data, and destination metrics that do not change every tick

Heavy navigation snapshots are invalidated when route, ship scan, ship mass, celestial map version, tier, configuration, or explicit refresh state changes.

## Jump Safety

Destination chunk reads are guarded by the existing batched chunk loading path. Collision and protection checks wait for the candidate target area to be fully loaded before reading target block states.

When jump adjustment changes the movement vector, the target chunk list is rebuilt before the final collision check.

## Sequencer Chunk Loading

The former sequencer forced every source or target chunk in one server tick. In ungenerated or heavily modded terrain, that made one jump-preparation tick responsible for all chunk I/O and generation, increasing watchdog risk.

The sequencer now:

- builds the required source and target chunk lists once and processes at most `general.chunks_per_tick` entries per tick (default `4`)
- orders chunks from the ship core outward so the area nearest the moving ship becomes available first
- remains in its loading state until all target chunks report loaded through `isBlockLoaded(..., false)`, so collision and protection reads cannot generate chunks accidentally
- batches release work as well as force-loading work
- regenerates skylight only for chunks already present in `ChunkProviderServer`, instead of calling `World.getChunk(...)` and loading a chunk solely during cleanup

The old target-anchor ticket forced chunk `(0, 0)` independently of the destination. It is no longer needed because the actual target chunks are ticketed directly and their loaded state is checked before the sequencer advances.

## Movement Range

Movement legs are clamped by calculated ship movement costs as a vector magnitude. This keeps route previews, destination estimates, and executed movement aligned with the ship's actual range.

## Air Simulation

Chunk air data keeps derived `cache_` counters for non-empty blocks, pressurized blocks, and scheduled update buckets. Empty segments and segments with no due work can therefore be skipped without scanning all 4,096 entries.

Every differential update captures the stored data and tick before mutation. When a segment is already due for a full scan, a read-only recount first validates all three caches; drift is logged with throttling and the cache is rebuilt before air spreading changes neighboring state.

## Celestial and Region Lookups

The celestial registry's immutable `byDimensionId` index scopes position lookups to objects in the requested dimension. The redundant per-entity lookup cache was removed. Global-region spatial indexes reduce repeated searches for containment checks, block updates, chat handling, and nearest-region queries.
