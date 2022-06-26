package cr0s.warpdrive.compat;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CompatAppliedEnergistics2 implements IBlockTransformer {
	
	private static Class<?> classAEBaseBlock;
	private static Class<?> classBlockQuartzFixture;
	private static Class<?> classBlockCableBus;
	private static Class<?> classBlockQuantumLinkChamber;
	private static Class<?> classTileQuantumBridge;
	private static Method methodTileQuantumBridge_getQEFrequency;
	
	public static void register() {
		try {
			classAEBaseBlock = Class.forName("appeng.block.AEBaseBlock");
			classBlockQuartzFixture = Class.forName("appeng.block.misc.BlockQuartzFixture");
			classBlockCableBus = Class.forName("appeng.block.networking.BlockCableBus");
			classBlockQuantumLinkChamber = Class.forName("appeng.block.qnb.BlockQuantumLinkChamber");
			classTileQuantumBridge = Class.forName("appeng.tile.qnb.TileQuantumBridge");
			methodTileQuantumBridge_getQEFrequency = classTileQuantumBridge.getMethod("getQEFrequency");
			WarpDriveConfig.registerBlockTransformer("appliedenergistics2", new CompatAppliedEnergistics2());
		} catch(final ClassNotFoundException | NoSuchMethodException | SecurityException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final BlockState blockState, final TileEntity tileEntity) {
		return classAEBaseBlock.isInstance(blockState.getBlock());
	}
	
	@Override
	public boolean isJumpReady(final BlockState blockState, final TileEntity tileEntity, final WarpDriveText reason) {
		if (classBlockQuantumLinkChamber.isInstance(blockState.getBlock())) {
			if (classTileQuantumBridge.isInstance(tileEntity)) {
				try {
					final Object object = methodTileQuantumBridge_getQEFrequency.invoke(tileEntity);
					if (((Long)object) != 0L) {
						reason.append(Commons.getStyleWarning(), "warpdrive.compat.guide.quantum_field_interference");
						return false;
					} else {
						return true;
					}
				} catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
					exception.printStackTrace();
				}
			}
			return false;
		}
		return true;
	}
	
	@Override
	public INBT saveExternals(final World world, final int x, final int y, final int z,
	                          final BlockState blockState, final TileEntity tileEntity) {
		// nothing to do
		return null;
	}
	
	@Override
	public void removeExternals(final World world, final int x, final int y, final int z,
	                            final BlockState blockState, final TileEntity tileEntity) {
		// nothing to do
	}
	
	private static final byte[] mrotQuartzFixture = { 0, 1, 5, 4, 2, 3, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
	private static final Map<String, String> rotSideNames;
	private static final Map<String, String> rotTagSuffix;
	static {
		Map<String, String> map = new HashMap<>();
		map.put("EAST", "SOUTH");
		map.put("SOUTH", "WEST");
		map.put("WEST", "NORTH");
		map.put("NORTH", "EAST");
		map.put("UNKNOWN", "UNKNOWN");
		rotSideNames = Collections.unmodifiableMap(map);
		map = new HashMap<>();
		map.put("2", "5");
		map.put("5", "3");
		map.put("3", "4");
		map.put("4", "2");
		rotTagSuffix = Collections.unmodifiableMap(map);
	}
	
	@Override
	public BlockState rotate(final BlockState blockState, final CompoundNBT nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0) {
			return blockState;
		}
		
		if (classBlockQuartzFixture.isInstance(blockState.getBlock())) {
		/*
			switch (rotationSteps) {
			case 1:
				return mrotQuartzFixture[metadata];
			case 2:
				return mrotQuartzFixture[mrotQuartzFixture[metadata]];
			case 3:
				return mrotQuartzFixture[mrotQuartzFixture[mrotQuartzFixture[metadata]]];
			default:
				return blockState;
			}
		*/
		}
		
		if (nbtTileEntity.contains("up") && nbtTileEntity.contains("forward")) {
			final String orientation_forward = nbtTileEntity.getString("forward");
			final String orientation_up = nbtTileEntity.getString("up");
			if (orientation_forward.equals("UP") || orientation_forward.equals("DOWN")) {
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.putString("up", rotSideNames.get(orientation_up));
					break;
				case 2:
					nbtTileEntity.putString("up", rotSideNames.get(rotSideNames.get(orientation_up)));
					break;
				case 3:
					nbtTileEntity.putString("up", rotSideNames.get(rotSideNames.get(rotSideNames.get(orientation_up))));
					break;
				default:
					break;
				}
			} else {
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.putString("forward", rotSideNames.get(orientation_forward));
					break;
				case 2:
					nbtTileEntity.putString("forward", rotSideNames.get(rotSideNames.get(orientation_forward)));
					break;
				case 3:
					nbtTileEntity.putString("forward", rotSideNames.get(rotSideNames.get(rotSideNames.get(orientation_forward))));
					break;
				default:
					break;
				}
			}
		} else if (classBlockCableBus.isInstance(blockState.getBlock())) {
			final HashMap<String, CompoundNBT> tagsRotated = new HashMap<>(7);
			final ArrayList<String> keys = new ArrayList<>();
			keys.addAll(nbtTileEntity.keySet());
			for (final String key : keys) {
				if ( (key.startsWith("def:") && !key.equals("def:6"))
				  || (key.startsWith("extra:") && !key.equals("extra:6"))) {
					final CompoundNBT tagCompound = nbtTileEntity.getCompound(key).copy();
					final String[] parts = key.split(":");
					if ( parts.length == 2
					  && rotTagSuffix.containsKey(parts[1]) ) {
						switch (rotationSteps) {
						case 1:
							tagsRotated.put(parts[0] + ":" + rotTagSuffix.get(parts[1]), tagCompound);
							break;
						case 2:
							tagsRotated.put(parts[0] + ":" + rotTagSuffix.get(rotTagSuffix.get(parts[1])), tagCompound);
							break;
						case 3:
							tagsRotated.put(parts[0] + ":" + rotTagSuffix.get(rotTagSuffix.get(rotTagSuffix.get(parts[1]))), tagCompound);
							break;
						default:
							tagsRotated.put(key, tagCompound);
							break;
						}
						nbtTileEntity.remove(key);
					}
				}
			}
			for (final Entry<String, CompoundNBT> entry : tagsRotated.entrySet()) {
				nbtTileEntity.put(entry.getKey(), entry.getValue());
			}
		}
		return blockState;
	}
	
	@Override
	public void restoreExternals(final World world, final BlockPos blockPos,
	                             final BlockState blockState, final TileEntity tileEntity,
	                             final ITransformation transformation, final INBT nbtBase) {
		// nothing to do
	}
}
