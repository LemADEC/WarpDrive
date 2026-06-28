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

## Movement Range

Movement legs are clamped by calculated ship movement costs as a vector magnitude. This keeps route previews, destination estimates, and executed movement aligned with the ship's actual range.

## Air Simulation

Chunk air data tracks non-empty segment counters, allowing empty segments to be skipped without scanning every block. Pending air updates are also tracked per segment so tick processing can skip segments with no due blocks.

## Celestial and Region Lookups

Celestial object lookup caches and global-region spatial indexes reduce repeated searches for entity checks, containment checks, block updates, chat handling, and nearest-region queries.
