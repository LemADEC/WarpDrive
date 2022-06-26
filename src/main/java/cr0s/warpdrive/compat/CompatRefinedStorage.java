package cr0s.warpdrive.compat;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.common.util.Constants;

import com.raoulvdberge.refinedstorage.api.IRSAPI;
import com.raoulvdberge.refinedstorage.api.network.node.INetworkNode;
import com.raoulvdberge.refinedstorage.api.network.node.INetworkNodeFactory;
import com.raoulvdberge.refinedstorage.api.network.node.INetworkNodeManager;
import com.raoulvdberge.refinedstorage.apiimpl.API;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNode;
import com.raoulvdberge.refinedstorage.tile.TileBase;

public class CompatRefinedStorage implements IBlockTransformer {
	
	private static final String NBT_DIRECTION = "Direction";
	private static final String NBT_NODE = "Node";
	private static final String NBT_NODE_ID = "NodeID";
	
	private static Class<?> classBlockBase;
	
	public static void register() {
		try {
			classBlockBase = Class.forName("com.raoulvdberge.refinedstorage.block.BlockBase");
			
			WarpDriveConfig.registerBlockTransformer("RefinedStorage", new CompatRefinedStorage());
		} catch (final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final BlockState blockState, final TileEntity tileEntity) {
		return classBlockBase.isInstance(blockState.getBlock());
	}
	
	@Override
	public boolean isJumpReady(final BlockState blockState, final TileEntity tileEntity, final WarpDriveText reason) {
		return true;
	}
	
	@Override
	public INBT saveExternals(final World world, final int x, final int y, final int z,
	                          final BlockState blockState, final TileEntity tileEntity) {
		if (!(tileEntity instanceof TileBase)) {
			return null;
		}
		
		final IRSAPI refinedStorageAPI = API.instance();
		final INetworkNode networkNode;
		if (refinedStorageAPI == null) {
			WarpDrive.logger.error("Invalid API instance while saving externals for RefinedStorage, please report to mod author");
			networkNode = null;
		} else {
			final INetworkNodeManager networkNodeManager = refinedStorageAPI.getNetworkNodeManager(world);
			
			networkNode = networkNodeManager.getNode(tileEntity.getPos());
			if (networkNode == null) {
				WarpDrive.logger.debug(String.format("No network node, nothing to save for %s %s",
				                                     tileEntity, Commons.format(world, x, y, z)) );
			}
		}
		
		final TileBase tileBase = (TileBase) tileEntity;
		
		final CompoundNBT tagCompound = new CompoundNBT();
		
		tagCompound.putInt(NBT_DIRECTION, tileBase.getDirection().ordinal());
		if (networkNode != null) {
		    tagCompound.put(NBT_NODE, networkNode.write(new CompoundNBT()));
		    tagCompound.putString(NBT_NODE_ID, networkNode.getId());
		}
		
		return tagCompound;
	}
	
	@Override
	public void removeExternals(final World world, final int x, final int y, final int z,
	                            final BlockState blockState, final TileEntity tileEntity) {
		if (!(tileEntity instanceof TileBase)) {
			return;
		}
		
		final IRSAPI refinedStorageAPI = API.instance();
		if (refinedStorageAPI == null) {
			WarpDrive.logger.error("Invalid API instance while removing externals for RefinedStorage, please report to mod author");
			return;
		}
		final INetworkNodeManager networkNodeManager = refinedStorageAPI.getNetworkNodeManager(world);
		
		// Avoid inventory dropping
		networkNodeManager.removeNode(tileEntity.getPos());
		networkNodeManager.markForSaving();
	}
	
	/*
	refinedstorage:controller
		Direction int 2 ?
	cables connections & facades
	crafter/manager/etc.
		nothing in metadata, nor TileEntities => custom world data structure are used, refer to com.raoulvdberge.refinedstorage.integration.funkylocomotion.MoveFactoryNetworkNode

	Example of Saved externals with covers:
	{
		Node:{
			OwnerLeast:-5712523905692608947L,
			RedstoneMode:0,
			Covers:[{
				Item:{id:"minecraft:emerald_block",Count:1b,Damage:0s},
				Type:0,
				Direction:2
			}],
			Version:"1.6.12",
			OwnerMost:-7878229554721438009L,
			Direction:2
		},
		NodeID:"cable",
		Direction:2
	}
	 */
	
	//                                                   0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15
	private static final int[]  rotFacing           = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	@Override
	public BlockState rotate(final BlockState blockState, final CompoundNBT nbtTileEntity, final ITransformation transformation) {
		// nothing to do as it's all stored externally
		return blockState;
	}
	
	@Override
	@Method(modid = "refinedstorage")
	public void restoreExternals(final World world, final BlockPos blockPos,
	                             final BlockState blockState, final TileEntity tileEntity,
	                             final ITransformation transformation, final INBT nbtBase) {
		if (!(tileEntity instanceof TileBase)) {
			return;
		}
		
		if (!(nbtBase instanceof CompoundNBT)) {
			WarpDrive.logger.error(String.format("Unexpected external NBT while restoring RefinedStorage, please report to mod author: %s %s %s",
			                                     blockState, nbtBase, Commons.format(world, blockPos) ));
			return;
		}
		
		final byte rotationSteps = transformation.getRotationSteps();
		final CompoundNBT tagCompound = (CompoundNBT) nbtBase;
		final CompoundNBT tagCompoundNode = tagCompound.getCompound(NBT_NODE);
		
		// transform node
		if (tagCompoundNode.contains("Covers", Constants.NBT.TAG_LIST)) {
			final ListNBT tagListCovers = tagCompoundNode.getList("Covers", Constants.NBT.TAG_COMPOUND);
			for (int index = 0; index < tagListCovers.size(); index++) {
				final CompoundNBT compoundTagCover = tagListCovers.getCompound(index);
				final int directionCoverOld = compoundTagCover.getInt("Direction");
				final int directionCoverNew;
				
				switch (rotationSteps) {
				case 1:
					directionCoverNew = rotFacing[directionCoverOld];
					break;
				case 2:
					directionCoverNew = rotFacing[rotFacing[directionCoverOld]];
					break;
				case 3:
					directionCoverNew = rotFacing[rotFacing[rotFacing[directionCoverOld]]];
					break;
				default:
					directionCoverNew = directionCoverOld;
					break;
				}
				compoundTagCover.putInt("Direction", directionCoverNew);
			}
		}
		
		// restore node
		final IRSAPI refinedStorageAPI = API.instance();
		if (refinedStorageAPI == null) {
			WarpDrive.logger.error(String.format("Invalid API instance while restoring RefinedStorage, please report to mod author: %s",
			                                     nbtBase ));
			return;
		}
		final INetworkNodeManager manager = refinedStorageAPI.getNetworkNodeManager(world);
		if (tagCompound.contains(NBT_NODE_ID, NBT.TAG_STRING)) {
			final INetworkNodeFactory networkNodeFactory = refinedStorageAPI.getNetworkNodeRegistry().get(tagCompound.getString(NBT_NODE_ID));
			if (networkNodeFactory == null) {
				WarpDrive.logger.error(String.format("Invalid NodeId in external NBT while restoring externals for RefinedStorage, please report to mod author: %s",
				                                     nbtBase ));
				return;
			}
			final NetworkNode networkNode = (NetworkNode) networkNodeFactory.create(tagCompoundNode, world, blockPos);
			networkNode.setThrottlingDisabled();
			
			manager.setNode(blockPos, networkNode);
		}
		manager.markForSaving();
		
		// restore direction
		if (!tagCompound.contains(NBT_DIRECTION, NBT.TAG_ANY_NUMERIC)) {
			WarpDrive.logger.error(String.format("Missing direction in external NBT while restoring externals for RefinedStorage, please report to mod author: %s",
			                                     nbtBase ));
		} else {
			final int directionOld = tagCompound.getInt(NBT_DIRECTION);
			final int directionNew;
			
			switch (rotationSteps) {
			case 1:
				directionNew = rotFacing[directionOld];
				break;
			case 2:
				directionNew = rotFacing[rotFacing[directionOld]];
				break;
			case 3:
				directionNew = rotFacing[rotFacing[rotFacing[directionOld]]];
				break;
			default:
				directionNew = directionOld;
				break;
			}
			
			((TileBase) tileEntity).setDirection(Direction.byIndex(directionNew));
		}
		
		tileEntity.markDirty();
		
		world.notifyBlockUpdate(blockPos, blockState, blockState, 4);
	}
}
