package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.common.util.Constants;

public class CompatEnergyControl implements IBlockTransformer {

	// only require rotation
	private static Class<?> classThermalMonitor;            // Thermal Monitor
	private static Class<?> classRemoteThermalMonitor;      // Remote Thermal Monitor

	// rotation and coordinate
	private static Class<?> classInfoPanel;                 // (Advanced) Info Panel
	private static Class<?> classInfoPanelExtender;         // (Advanced) Info Panel Extender
	private static Class<?> classHoloPanel;                 // Holographic Panel
	private static Class<?> classHoloPanelExtender;         // Holo Extender

	private static Class<?> classRangeTrigger;              // Range Trigger

	public static void register() {
		try {
			classThermalMonitor = Class.forName("com.zuxelus.energycontrol.blocks.ThermalMonitor");
			classRemoteThermalMonitor = Class.forName("com.zuxelus.energycontrol.blocks.RemoteThermalMonitor");
			classInfoPanel = Class.forName("com.zuxelus.energycontrol.blocks.InfoPanel");
			classInfoPanelExtender = Class.forName("com.zuxelus.energycontrol.blocks.InfoPanelExtender");
			classHoloPanel = Class.forName("com.zuxelus.energycontrol.blocks.HoloPanel");
			classHoloPanelExtender = Class.forName("com.zuxelus.energycontrol.blocks.HoloPanelExtender");
			classRangeTrigger = Class.forName("com.zuxelus.energycontrol.blocks.RangeTrigger");
			WarpDriveConfig.registerBlockTransformer("energycontrol", new CompatEnergyControl());
		} catch (final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}

	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classThermalMonitor.isInstance(block)
		    || classRemoteThermalMonitor.isInstance(block)
		    || classInfoPanel.isInstance(block)
		    || classInfoPanelExtender.isInstance(block)
		    || classHoloPanel.isInstance(block)
		    || classHoloPanelExtender.isInstance(block);
	}

	@Override
	public boolean isJumpReady(final Block block, final int metadata, final TileEntity tileEntity, final WarpDriveText reason) {
		// nothing to do
		return true;
	}

	@Override
	public NBTBase saveExternals(final World world, final int x, final int y, final int z, final Block block, final int blockMeta, final TileEntity tileEntity) {
		// nothing to do
		return null;
	}

	@Override
	public void removeExternals(final World world, final int x, final int y, final int z, final Block block, final int blockMeta, final TileEntity tileEntity) {
		// nothing to do
	}

	// Rotation ID/metadata                                     0   1   2   3   4   5   6   7   8   9   10  11  12  13  14  15
	private static final byte[] panelRotFacing      = { 0, 1, 5, 4, 2, 3, 6, 7, 11, 10, 8, 9, 12, 13, 14, 15 };
	private static final byte[] nbtRotFacing        = { 0, 1, 5, 4, 2, 3, 0, 1, 2, 3, 4, 5, 12, 13, 14, 15 };
	private static final byte[] horizontalRotFacing = { 1, 2, 3, 0, 5, 6, 7, 4, 8, 9, 10, 11, 12, 13, 14, 15 };

	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		int metadataRotated = metadata;

		// Fix screen data for display panels
		if ( classInfoPanel.isInstance(block)
		  || classHoloPanel.isInstance(block) ) {
			final NBTTagCompound screenData = nbtTileEntity.getCompoundTag("screenData");
			final int maxX = screenData.getInteger("maxX");
			final int maxY = screenData.getInteger("maxY");
			final int maxZ = screenData.getInteger("maxZ");
			final int minX = screenData.getInteger("minX");
			final int minY = screenData.getInteger("minY");
			final int minZ = screenData.getInteger("minZ");
			final BlockPos newMax = transformation.apply(maxX, maxY, maxZ);
			final BlockPos newMin = transformation.apply(minX, minY, minZ);
			screenData.setInteger("maxX", Math.max(newMax.getX(), newMin.getX()));
			screenData.setInteger("maxY", Math.max(newMax.getY(), newMin.getY()));
			screenData.setInteger("maxZ", Math.max(newMax.getZ(), newMin.getZ()));
			screenData.setInteger("minX", Math.min(newMax.getX(), newMin.getX()));
			screenData.setInteger("minY", Math.min(newMax.getY(), newMin.getY()));
			screenData.setInteger("minZ", Math.min(newMax.getZ(), newMin.getZ()));
			nbtTileEntity.setTag("screenData", screenData);
		}

		// handle data cards (transform absolute coordinates)
		// note: getTagList / getCompoundTagAt return live references, so the items are updated in place.
		if ( classHoloPanel.isInstance(block)
		  || classInfoPanel.isInstance(block)
		  || classRemoteThermalMonitor.isInstance(block)
		  || classRangeTrigger.isInstance(block) ) {
			final NBTTagList items = nbtTileEntity.getTagList("Items", Constants.NBT.TAG_COMPOUND);
			for (int index = 0; index < items.tagCount(); index++) {
				final NBTTagCompound item = items.getCompoundTagAt(index);
				if (!item.hasKey("tag")) {// must have a tag
					continue;
				}
				final NBTTagCompound itemTag = item.getCompoundTag("tag");
				if ( !itemTag.hasKey("x")
				  || !itemTag.hasKey("y")
				  || !itemTag.hasKey("z") ) {// must have the coordinate data
					continue;
				}

				final int x = itemTag.getInteger("x");
				final int y = itemTag.getInteger("y");
				final int z = itemTag.getInteger("z");
				if (!transformation.isInside(x, y, z)) {// only convert if inside the ship
					continue;
				}

				final BlockPos result = transformation.apply(x, y, z);
				itemTag.setInteger("x", result.getX());
				itemTag.setInteger("y", result.getY());
				itemTag.setInteger("z", result.getZ());
			}
		}

		// Redirect core x/y/z for extended screens
		if ( classInfoPanelExtender.isInstance(block)
		  || classHoloPanelExtender.isInstance(block) ) {
			final byte partOfScreen = nbtTileEntity.getByte("partOfScreen");
			// only do the modification if it is part of a screen
			if (partOfScreen == 1) {
				final int x = nbtTileEntity.getInteger("coreX");
				final int y = nbtTileEntity.getInteger("coreY");
				final int z = nbtTileEntity.getInteger("coreZ");
				final BlockPos result = transformation.apply(x, y, z);
				nbtTileEntity.setInteger("coreX", result.getX());
				nbtTileEntity.setInteger("coreY", result.getY());
				nbtTileEntity.setInteger("coreZ", result.getZ());
			}
		}

		// handle rotation in NBT
		// 6 sided type (block screens and monitors) - both facing and rotation
		if ( classThermalMonitor.isInstance(block)
		  || classRemoteThermalMonitor.isInstance(block)
		  || classInfoPanel.isInstance(block)
		  || classInfoPanelExtender.isInstance(block) ) {
			int facing = nbtTileEntity.getInteger("facing");
			int rotation = 15;
			// only panels have rotation
			// NOTE there is no rotation of 15, so we use it as a marker of "Not exist"
			if (nbtTileEntity.hasKey("rotation")) {
				rotation = nbtTileEntity.getInteger("rotation");
			}
			switch (rotationSteps) {
			// without break, so it rotates an additional time for each step.
			case 3:
				metadataRotated = panelRotFacing[metadataRotated];
				facing = nbtRotFacing[facing];
				rotation = nbtRotFacing[rotation];
			case 2:
				metadataRotated = panelRotFacing[metadataRotated];
				facing = nbtRotFacing[facing];
				rotation = nbtRotFacing[rotation];
			case 1:
				metadataRotated = panelRotFacing[metadataRotated];
				facing = nbtRotFacing[facing];
				rotation = nbtRotFacing[rotation];
			default:
				break;
			}
			nbtTileEntity.setInteger("facing", facing);
			if (rotation != 15) {
				nbtTileEntity.setInteger("rotation", rotation);
			}
		}

		// 4 sided type (holo displays and range trigger) - facing only
		if ( classHoloPanel.isInstance(block)
		  || classHoloPanelExtender.isInstance(block)
		  || classRangeTrigger.isInstance(block) ) {
			int facing = nbtTileEntity.getInteger("facing");
			switch (rotationSteps) {
			// without break, so it rotates an additional time for each step.
			case 3:
				metadataRotated = horizontalRotFacing[metadataRotated];
				facing = nbtRotFacing[facing];
			case 2:
				metadataRotated = horizontalRotFacing[metadataRotated];
				facing = nbtRotFacing[facing];
			case 1:
				metadataRotated = horizontalRotFacing[metadataRotated];
				facing = nbtRotFacing[facing];
			default:
				break;
			}
			nbtTileEntity.setInteger("facing", facing);
		}

		return metadataRotated;
	}

	@Override
	public void restoreExternals(final World world, final BlockPos blockPos, final IBlockState blockState, final TileEntity tileEntity, final ITransformation transformation, final NBTBase nbtBase) {
		// nothing to do
	}
}
