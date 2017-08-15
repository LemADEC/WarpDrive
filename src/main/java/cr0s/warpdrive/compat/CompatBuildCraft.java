package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.config.WarpDriveConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class CompatBuildCraft implements IBlockTransformer {
	
	private static Class<?> classTileEntityBuildCraft;
	private static Class<?> classTileEntityGenericPipe;
	private static Class<?> classTileEntityQuarry;
	private static Class<?> classTileEntityFiller;
	private static Class<?> classTileEntityZonePlanner;
	
	public static void register() {
		try {
			classTileEntityBuildCraft = Class.forName("buildcraft.core.lib.block.TileBuildCraft");
			classTileEntityGenericPipe = Class.forName("buildcraft.transport.TileGenericPipe");
			classTileEntityQuarry = Class.forName("buildcraft.builders.TileQuarry"); // id is Machine
			classTileEntityFiller = Class.forName("buildcraft.builders.TileFiller"); // id is Filler
			classTileEntityZonePlanner = Class.forName("buildcraft.robotics.TileZonePlan"); // id is net.minecraft.src.buildcraft.commander.TileZonePlan
			WarpDriveConfig.registerBlockTransformer("BuildCraft", new CompatBuildCraft());
		} catch(ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classTileEntityBuildCraft.isInstance(tileEntity)
		    || classTileEntityGenericPipe.isInstance(tileEntity);
	}
	
	@Override
	public boolean isJumpReady(final Block block, final int metadata, final TileEntity tileEntity, final StringBuilder reason) {
		if (classTileEntityQuarry.isInstance(tileEntity)) {
			reason.append("Quarry detected on board!");
			return false;
		}
		if (classTileEntityFiller.isInstance(tileEntity)) {
			reason.append("Filler detected on board!");
			return false;
		}
		if (classTileEntityZonePlanner.isInstance(tileEntity)) {
			reason.append("Zone Planner detected on board!");
			return false;
		}
		return true;
	}
	
	@Override
	public NBTBase saveExternals(final World world, final int x, final int y, final int z, final Block block, final int blockMeta, final TileEntity tileEntity) {
		// nothing to do
		return null;
	}
	
	@Override
	public void removeExternals(final World world, final int x, final int y, final int z,
	                            final Block block, final int blockMeta, final TileEntity tileEntity) {
		// nothing to do
	}
	
	private static final byte[]  mrotByte  = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final short[] mrotShort = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final Map<String, String> rotPipeNames;
	static {
		Map<String, String> map = new HashMap<>();
		map.put("redstoneInputSide[0]", "redstoneInputSide[0]");
		map.put("redstoneInputSide[1]", "redstoneInputSide[1]");
		map.put("redstoneInputSide[2]", "redstoneInputSide[5]");
		map.put("redstoneInputSide[3]", "redstoneInputSide[4]");
		map.put("redstoneInputSide[4]", "redstoneInputSide[2]");
		map.put("redstoneInputSide[5]", "redstoneInputSide[3]");
		map.put("pluggable[0]", "pluggable[0]");
		map.put("pluggable[1]", "pluggable[1]");
		map.put("pluggable[2]", "pluggable[5]");
		map.put("pluggable[3]", "pluggable[4]");
		map.put("pluggable[4]", "pluggable[2]");
		map.put("pluggable[5]", "pluggable[3]");
		map.put("Gate[0]", "Gate[0]");
		map.put("Gate[1]", "Gate[1]");
		map.put("Gate[2]", "Gate[5]");
		map.put("Gate[3]", "Gate[4]");
		map.put("Gate[4]", "Gate[2]");
		map.put("Gate[5]", "Gate[3]");
		rotPipeNames = Collections.unmodifiableMap(map);
	}
	
	@Override
	public int rotate(final Block block, final int metadata, NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		
		if (nbtTileEntity == null) {
			return metadata;
		}
		
		final String idTileEntity = nbtTileEntity.getString("id");
		switch(idTileEntity) {
		case "net.minecraft.src.buildcraft.energy.TileEngineWood" :
		case "net.minecraft.src.buildcraft.energy.TileEngineStone" :
		case "net.minecraft.src.buildcraft.energy.TileEngineIron" :
		case "net.minecraft.src.buildcraft.energy.TileEngineCreative" :
			if (nbtTileEntity.hasKey("orientation")) {
				final byte facing = nbtTileEntity.getByte("orientation");
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setByte("orientation", mrotByte[facing]);
					return metadata;
				case 2:
					nbtTileEntity.setByte("orientation", mrotByte[mrotByte[facing]]);
					return metadata;
				case 3:
					nbtTileEntity.setByte("orientation", mrotByte[mrotByte[mrotByte[facing]]]);
					return metadata;
				default:
					return metadata;
				}
			}
			return metadata;
			
		case "net.minecraft.src.buildcraft.factory.Refinery" :
		case "MiningWell" :
		case "net.minecraft.src.buildcraft.commander.TileZonePlan" :
		case "net.minecraft.src.buildcraft.commander.TileRequester" :
		case "net.minecraft.src.buildcraft.factory.TileLaser" :
		case "net.minecraft.src.builders.TileBuilder" :
		case "net.minecraft.src.builders.TileBlueprintLibrary" :
		case "net.minecraft.src.builders.TileTemplate" :
		case "Marker" :
		case "net.minecraft.src.builders.TilePathMarker" :
			switch (rotationSteps) {
			case 1:
				return mrotShort[metadata];
			case 2:
				return mrotShort[mrotShort[metadata]];
			case 3:
				return mrotShort[mrotShort[mrotShort[metadata]]];
			default:
				return metadata;
			}
			
		case "net.minecraft.src.builders.TileConstructionMarker" :
			final int directionMarker = nbtTileEntity.getByte("direction");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setByte("direction", mrotByte[directionMarker]);
				break;
			case 2:
				nbtTileEntity.setByte("direction", mrotByte[mrotByte[directionMarker]]);
				break;
			case 3:
				nbtTileEntity.setByte("direction", mrotByte[mrotByte[mrotByte[directionMarker]]]);
				break;
			default:
				break;
			}
			
			switch (rotationSteps) {
			case 1:
				return mrotShort[metadata];
			case 2:
				return mrotShort[mrotShort[metadata]];
			case 3:
				return mrotShort[mrotShort[mrotShort[metadata]]];
			default:
				return metadata;
			}
			
		case "net.minecraft.src.buildcraft.transport.GenericPipe" :
			final NBTTagCompound tagsNew = new NBTTagCompound();
			@SuppressWarnings("unchecked")
			final Object[] objectKeys = ((Set<Object>) nbtTileEntity.func_150296_c()).toArray();
			for (final Object objectKey : objectKeys) {
				if (!(objectKey instanceof String)) {
					continue;
				}
				final String keyOld = (String) objectKey;
				final String keyNew = rotPipeNames.get(keyOld);
				if (keyNew != null) {
					final NBTBase nbtBase = nbtTileEntity.getTag(keyOld);
					tagsNew.setTag(keyNew, nbtBase);
					nbtTileEntity.removeTag(keyOld);
					
					if (nbtBase instanceof NBTTagCompound) {
						final int direction = ((NBTTagCompound) nbtBase).getInteger("direction");
						if (direction != 0) {
							switch (rotationSteps) {
							case 1:
								((NBTTagCompound) nbtBase).setInteger("direction", mrotByte[direction]);
								break;
							case 2:
								((NBTTagCompound) nbtBase).setInteger("direction", mrotByte[mrotByte[direction]]);
								break;
							case 3:
								((NBTTagCompound) nbtBase).setInteger("direction", mrotByte[mrotByte[mrotByte[direction]]]);
								break;
							default:
								break;
							}
						}
					}
				}
			}
			@SuppressWarnings("unchecked")
			final Set<String> keysNew = tagsNew.func_150296_c();
			for (final String keyNew : keysNew) {
				nbtTileEntity.setTag(keyNew, tagsNew.getTag(keyNew));
			}
			
			switch (rotationSteps) {
			case 1:
				return mrotShort[metadata];
			case 2:
				return mrotShort[mrotShort[metadata]];
			case 3:
				return mrotShort[mrotShort[mrotShort[metadata]]];
			default:
				return metadata;
			}
		
		case "net.minecraft.src.buildcraft.factory.TilePump" :  // BuildCraft 7.1.22
			nbtTileEntity.setInteger("aimY", 0);
			nbtTileEntity.setFloat("tubeY", Float.NaN);
			return metadata;
			
		default:
			break;
		}
		
		return metadata;
	}
	
	@Override
	public void restoreExternals(final World world, final int x, final int y, final int z,
	                             final Block block, final int blockMeta, final TileEntity tileEntity,
	                             final ITransformation transformation, final NBTBase nbtBase) {
		// nothing to do
	}
}
