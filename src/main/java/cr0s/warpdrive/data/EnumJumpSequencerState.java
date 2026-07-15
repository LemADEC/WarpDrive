package cr0s.warpdrive.data;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;

public enum EnumJumpSequencerState implements IStringSerializable {
	
	IDLE                  ("idle"),
	LOAD_SOURCE_CHUNKS    ("load_source_chunks"),
	SAVE_TO_MEMORY        ("save_to_memory"),
	CHECK_BORDERS         ("check_borders"),
	SAVE_TO_DISK          ("save_to_disk"),
	GET_INITIAL_VECTOR    ("get_initial_vector"),
	PRELOAD_TARGET_CHUNKS ("preload_target_chunks"),
	ADJUST_JUMP_VECTOR    ("adjust_jump_vector"),
	LOAD_TARGET_CHUNKS    ("load_target_chunks"),
	SAVE_ENTITIES         ("save_entities"),
	MOVE_BLOCKS           ("move_blocks"),
	MOVE_EXTERNALS        ("move_externals"),
	MOVE_ENTITIES         ("move_entities"),
	REMOVING              ("removing"),
	CHUNK_UNLOADING       ("chunk_unloading"),
	FINISHING             ("finishing");
	
	private final String name;
	
	EnumJumpSequencerState(final String name) {
		this.name = name;
	}
	
	@Nonnull
	@Override
	public String getName() { return name; }
}
