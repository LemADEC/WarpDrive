# Tile-entity block-state guards

## Why the guards exist

During navigation testing on the `wowid401` server, the server entered a `Ticking block entity` crash on 2026-06-28. The crash report was `crash-reports/crash-2026-06-28_23.54.18-server.txt`.

The saved tile entity was a `warpdrive:lift` at `(9349, 127, -94997)`, while the block state at that position was `minecraft:air`. The unguarded update path attempted to read `BlockLift.MODE` from that air block state and failed:

```text
java.lang.IllegalArgumentException: Cannot get property PropertyEnum{name=mode,
clazz=class cr0s.warpdrive.data.EnumLiftMode,
values=[INACTIVE, UP, DOWN, REDSTONE]} as it does not exist in
BlockStateContainer{block=minecraft:air, properties=[]}
    at cr0s.warpdrive.block.movement.TileEntityLift.update(TileEntityLift.java:116)
```

The tile entity and its NBT were already persisted in the chunk. Reloading that chunk recreated the mismatch and repeated the crash, so recovery otherwise required editing the affected region data.

The test server is heavily modded, and the evidence does not establish what originally produced the mismatched saved state. A concurrent server optimization, an interrupted movement, or another mod remain possible causes. The guard addresses the recoverability problem: a malformed persisted pairing should be logged and removed instead of crash-looping the server.

## Implementation shape

`TileEntityAbstractBase.isInvalidBlockState(...)` verifies the expected block class, tile-entity capability, and required block-state properties before a subclass reads or writes them. The checks remain at the affected subclass entry points because `super.update()` cannot return from the caller's update frame; centralizing the check would require an invasive template-method rewrite of every ticking WarpDrive tile entity.

On the server, the guard uses `Chunk.EnumCreateEntityType.CHECK` so the diagnostic lookup cannot create a tile entity. If the chunk still maps the position to the ticking instance, the orphan is removed. If another tile entity has already replaced it, only the detached stale instance is invalidated. Forge's invalid-tile sweep identity-checks the chunk entry before removing it, so this cannot delete the replacement instance.

This extends an existing upstream precedent: `TileEntityAbstractBase.updateBlockState(...)` already checks for missing properties and refuses an invalid block-state write. The guards cover equivalent property reads and casts in tick/update paths.
