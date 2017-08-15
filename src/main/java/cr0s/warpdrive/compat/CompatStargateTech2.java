package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.config.WarpDriveConfig;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

public class CompatStargateTech2 implements IBlockTransformer {
	
	private static Class<?> classBlockMachine;
	private static Class<?> classBlockShield;
	private static Class<?> classBlockTransportRing;
	private static Class<?> classTileTransportRing;
	private static Method   methodTileTransportRing_link;
	
	public static void register() {
		try {
			classBlockMachine = Class.forName("lordfokas.stargatetech2.core.machine.BlockMachine");
			classBlockShield = Class.forName("lordfokas.stargatetech2.enemy.BlockShield");
			classBlockTransportRing = Class.forName("lordfokas.stargatetech2.transport.BlockTransportRing");
			classTileTransportRing = Class.forName("lordfokas.stargatetech2.transport.TileTransportRing");
			methodTileTransportRing_link = classTileTransportRing.getDeclaredMethod("link");
			WarpDriveConfig.registerBlockTransformer("StargateTech2", new CompatStargateTech2());
		} catch(ClassNotFoundException | NoSuchMethodException | SecurityException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockMachine.isInstance(block)
			|| classBlockShield.isInstance(block)
			|| classBlockTransportRing.isInstance(block);
	}
	
	@Override
	public boolean isJumpReady(final Block block, final int metadata, final TileEntity tileEntity, StringBuilder reason) {
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
	
	private static final int[] mrot = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final Map<String, String> rotFacingcolors;
	static {
		Map<String, String> map = new HashMap<>();
		map.put("color0", "color0");
		map.put("color1", "color1");
		map.put("color2", "color5");
		map.put("color3", "color4");
		map.put("color4", "color2");
		map.put("color5", "color3");
		rotFacingcolors = Collections.unmodifiableMap(map);
	}
	
	private static NBTTagCompound rotateVector(ITransformation transformation, NBTTagCompound tag) {
		ChunkCoordinates target = transformation.apply(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z"));
		tag.setInteger("x", target.posX);
		tag.setInteger("y", target.posY);
		tag.setInteger("z", target.posZ);
		return tag;
	}
	
	private static NBTTagCompound rotateFacingColors(final Byte rotationSteps, final NBTTagCompound tag) {
		NBTTagCompound newFacing = new NBTTagCompound();
		Set<String> keys = tag.func_150296_c();
		for (String key : keys) {
			NBTBase base = tag.getTag(key);
			if (base instanceof NBTTagByte && rotFacingcolors.containsKey(key)) {
				switch (rotationSteps) {
				case 1:
					newFacing.setTag(rotFacingcolors.get(key), base);
					break;
				case 2:
					newFacing.setTag(rotFacingcolors.get(rotFacingcolors.get(key)), base);
					break;
				case 3:
					newFacing.setTag(rotFacingcolors.get(rotFacingcolors.get(rotFacingcolors.get(key))), base);
					break;
				default:
					newFacing.setTag(key, base);
					break;
				}
			} else {
				newFacing.setTag(key, base);
			}
		}
		return newFacing;
	}
	
	@Override
	public int rotate(final Block block, final int metadata, NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		byte rotationSteps = transformation.getRotationSteps();
		
		if (nbtTileEntity.hasKey("pairDn")) {
			nbtTileEntity.setTag("pairDn", rotateVector(transformation, nbtTileEntity.getCompoundTag("pairDn")));
		}
		if (nbtTileEntity.hasKey("pairUp")) {
			nbtTileEntity.setTag("pairUp", rotateVector(transformation, nbtTileEntity.getCompoundTag("pairUp")));
		}
		
		if (nbtTileEntity.hasKey("controller")) {
			nbtTileEntity.setTag("controller", rotateVector(transformation, nbtTileEntity.getCompoundTag("controller")));
		}
		if (nbtTileEntity.hasKey("emitter_0")) {
			nbtTileEntity.setTag("emitter_0", rotateVector(transformation, nbtTileEntity.getCompoundTag("emitter_0")));
		}
		if (nbtTileEntity.hasKey("facing")) {
			nbtTileEntity.setTag("facing", rotateFacingColors(rotationSteps, nbtTileEntity.getCompoundTag("facing")));
		}
		
		// Shield master (a.k.a. shield controller)
		if (nbtTileEntity.hasKey("master")) {
			nbtTileEntity.setTag("master", rotateVector(transformation, nbtTileEntity.getCompoundTag("master")));
		}
		
		switch (rotationSteps) {
		case 1:
			return mrot[metadata];
		case 2:
			return mrot[mrot[metadata]];
		case 3:
			return mrot[mrot[mrot[metadata]]];
		default:
			return metadata;
		}
	}
	
	@Override
	public void restoreExternals(final World world, final int x, final int y, final int z,
	                             final Block block, final int blockMeta, final TileEntity tileEntity,
	                             final ITransformation transformation, final NBTBase nbtBase) {
		if (classTileTransportRing.isInstance(tileEntity)) {
			try {
				methodTileTransportRing_link.invoke(tileEntity);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
				exception.printStackTrace();
			}
		}
	}
}
