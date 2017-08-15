package cr0s.warpdrive.api;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public interface IBlockTransformer {
	// Return true if this transformer is applicable to that block.
	boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity);
	
	// Called when preparing to save a ship structure.
	// Use this to prevent jump during critical events/animations.
	boolean isJumpReady(final Block block, final int metadata, final TileEntity tileEntity, StringBuilder reason);
	
	// Called when saving a ship structure.
	// Use this to save external data in the ship schematic.
	// You don't need to save Block and TileEntity data here, it's already covered.
	// Warning: do NOT assume that the ship will be removed!
	NBTBase saveExternals(final World world, final int x, final int y, final int z,
	                      final Block block, final int blockMeta, final TileEntity tileEntity);
	
	// Called when removing the original ship structure, if saveExternals() returned non-null for that block.
	// Use this to prevents drops, clear energy networks, etc.
	// Block and TileEntity will be removed right after this call. 
	// When moving, the new ship is placed first.
	void removeExternals(final World world, final int x, final int y, final int z,
	                     final Block block, final int blockMeta, final TileEntity tileEntity);
	
	// Called when restoring a ship in the world.
	// Use this to apply metadata & NBT rotation, right before block & tile entity placement.
	// Use priority placement to ensure dependent blocks are placed first.
	// Warning: do NOT place the block or tile entity!
	int rotate(final Block block, int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation);
	
	// Called when placing back a ship in the world, if saveExternals() returned non-null for that block.
	// Use this to restore external data from the ship schematic, right after block & tile entity placement.
	// Use priority placement to ensure dependent blocks are placed first.
	// This is will be called whether saveExternals returned null or not.
	void restoreExternals(final World world, final int x, final int y, final int z,
	                      final Block block, final int blockMeta, final TileEntity tileEntity,
	                      final ITransformation transformation, final NBTBase nbtBase);
}