package cr0s.warpdrive.compat;

import blusunrize.immersiveengineering.api.energy.IImmersiveConnectable;
import blusunrize.immersiveengineering.api.energy.ImmersiveNetHandler;
import blusunrize.immersiveengineering.api.energy.ImmersiveNetHandler.Connection;
import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.config.WarpDriveConfig;

import java.util.Collection;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import cpw.mods.fml.common.Optional;

public class CompatImmersiveEngineering implements IBlockTransformer {
	
	private static Class<?> classTileEntityIEBase;
	
	public static void register() {
		try {
			classTileEntityIEBase = Class.forName("blusunrize.immersiveengineering.common.blocks.TileEntityIEBase");
			WarpDriveConfig.registerBlockTransformer("ImmersiveEngineering", new CompatImmersiveEngineering());
		} catch(ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return tileEntity instanceof IImmersiveConnectable || classTileEntityIEBase.isInstance(tileEntity);
	}
	
	@Override
	public boolean isJumpReady(final Block block, final int metadata, final TileEntity tileEntity, StringBuilder reason) {
		return true;
	}
	
	@Override
	@Optional.Method(modid = "ImmersiveEngineering")
	public NBTBase saveExternals(final World world, final int x, final int y, final int z, final Block block, final int blockMeta, final TileEntity tileEntity) {
		if (tileEntity instanceof IImmersiveConnectable) {
			ChunkCoordinates node = new ChunkCoordinates(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
			Collection<Connection> connections = ImmersiveNetHandler.INSTANCE.getConnections(tileEntity.getWorldObj(), node);
			if (connections != null) {
				NBTTagList nbtImmersiveEngineering = new NBTTagList();
				for (Connection connection : connections) {
					nbtImmersiveEngineering.appendTag(connection.writeToNBT());
				}
				ImmersiveNetHandler.INSTANCE.clearConnectionsOriginatingFrom(node, tileEntity.getWorldObj());
				return nbtImmersiveEngineering;
			}
		}
		return null;
	}
	
	@Override
	public void removeExternals(final World world, final int x, final int y, final int z,
	                            final Block block, final int blockMeta, final TileEntity tileEntity) {
		// nothing to do
	}
	
	@Override
	public int rotate(final Block block, final int metadata, NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0 || !nbtTileEntity.hasKey("facing")) {
			return metadata;
		}
		
		int facing = nbtTileEntity.getInteger("facing");
		final int[] mrot = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
		switch (rotationSteps) {
		case 1:
			nbtTileEntity.setInteger("facing", mrot[facing]);
			return metadata;
		case 2:
			nbtTileEntity.setInteger("facing", mrot[mrot[facing]]);
			return metadata;
		case 3:
			nbtTileEntity.setInteger("facing", mrot[mrot[mrot[facing]]]);
			return metadata;
		default:
			return metadata;
		}
	}
	
	@Override
	@Optional.Method(modid = "ImmersiveEngineering")
	public void restoreExternals(final World world, final int x, final int y, final int z,
	                             final Block block, final int blockMeta, final TileEntity tileEntity,
	                             final ITransformation transformation, final NBTBase nbtBase) {
		NBTTagList nbtImmersiveEngineering = (NBTTagList) nbtBase;
		if (nbtImmersiveEngineering == null) {
			return;
		}
		World targetWorld = transformation.getTargetWorld();
		
		// powerPathList
		for (int indexConnectionToAdd = 0; indexConnectionToAdd < nbtImmersiveEngineering.tagCount(); indexConnectionToAdd++) {
			Connection connectionToAdd = Connection.readFromNBT(nbtImmersiveEngineering.getCompoundTagAt(indexConnectionToAdd));
			connectionToAdd.start = transformation.apply(connectionToAdd.start);
			connectionToAdd.end = transformation.apply(connectionToAdd.end);
			ChunkCoordinates node = new ChunkCoordinates(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
			Collection<Connection> connectionActuals = ImmersiveNetHandler.INSTANCE.getConnections(tileEntity.getWorldObj(), node);
			boolean existing = false;
			if (connectionActuals != null) {
				for (Connection connectionActual : connectionActuals) {
					if ( connectionActual.start.posX == connectionToAdd.start.posX
					  && connectionActual.start.posY == connectionToAdd.start.posY
					  && connectionActual.start.posZ == connectionToAdd.start.posZ
					  && connectionActual.end.posX == connectionToAdd.end.posX
					  && connectionActual.end.posY == connectionToAdd.end.posY
					  && connectionActual.end.posZ == connectionToAdd.end.posZ) {
						existing = true;
						break;
					} else if (
					     connectionActual.start.posX == connectionToAdd.end.posX
					  && connectionActual.start.posY == connectionToAdd.end.posY
					  && connectionActual.start.posZ == connectionToAdd.end.posZ
					  && connectionActual.end.posX == connectionToAdd.start.posX
					  && connectionActual.end.posY == connectionToAdd.start.posY
					  && connectionActual.end.posZ == connectionToAdd.start.posZ) {
						existing = true;
						break;
					}
				}
			}
			if (!existing) {
				ImmersiveNetHandler.INSTANCE.addConnection(targetWorld, new ChunkCoordinates(connectionToAdd.start.posX, connectionToAdd.start.posY, connectionToAdd.start.posZ), connectionToAdd);
			}
		}
	}
}
