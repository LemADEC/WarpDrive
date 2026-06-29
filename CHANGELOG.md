# Changelog

## 1.5.29 - 2026-06-28

### Added

- Added the Ship Navigation GUI with map, destination, ship status, dimensions, movement, and drive tabs.
- Added route planning for takeoff, orbital approach, landing, hyperspace entry, hyperspace cruise, and hyperspace exit legs.
- Added autopilot modes: off, assisted, safety stops, and full auto.
- Added navigation packets for live ship status, cached celestial map data, and ship navigation actions.
- Added computer validation APIs for movement and navigation previews.
- Added per-segment air occupancy tracking to reduce air simulation scans.
- Added celestial lookup caching and global-region spatial indexes for lower server tick cost.

### Changed

- Navigation snapshots now separate cheap live state from heavier cached map and destination data.
- Ship dimensions are applied automatically when valid dimension text is entered.
- Ship scan data, dimensions, navigation targets, and autopilot state now persist across jumps.
- Movement preview and execution now clamp to the calculated ship movement range as a vector magnitude.
- Space-map rendering now treats space as the backdrop region and shows celestial bodies by their actual parent coordinates.
- Destination lists and map tooltips now include celestial body X/Z coordinates.
- Jump target chunk loading now happens before destination collision reads to avoid accidental generation of unloaded target chunks.
- Jump collision adjustment now rechecks target chunks after movement changes and keeps completion callbacks tied to the moved core.
- Ship navigation route estimates now use the same effective movement calculation as the executed leg.
- Autopilot now treats target orbit as arrival when the ship is too large or heavy to land on that body.

### Fixed

- Fixed full-auto navigation stopping after every completed leg.
- Fixed ship dimensions resetting after load or jump due to moved core NBT restoration issues.
- Fixed stale or missing navigation engaged-target state after a ship jump.
- Fixed route range display being limited by a hard input cap instead of the calculated ship range.
- Fixed Pluto and other nested bodies being difficult to diagnose by adding real map and destination coordinates.
- Fixed jump aborts caused by target collision checks touching chunks before the batched loading path completed.
- Fixed ship core orientation access during moved-core restoration.
- Fixed stale tile entities crashing when their saved tile entity outlives the block state at the same position.
- Fixed navigation map body icons being hidden behind opaque fallback markers.

### Removed

- Removed obsolete jump gate block, tile entity, scanner, and generator classes from the movement registration path.
