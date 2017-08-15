package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.config.WarpDriveConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class CompatMekanism implements IBlockTransformer {
	
	private static Class<?> tileEntityBasicBlock;
	
	public static void register() {
		try {
			tileEntityBasicBlock = Class.forName("mekanism.common.tile.TileEntityBasicBlock");
			
			WarpDriveConfig.registerBlockTransformer("Mekanism", new CompatMekanism());
		} catch(ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return tileEntityBasicBlock.isInstance(tileEntity);
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
	
	private static final int[] rotFacing           = {  0,  1,  5,  4,  2,  3 };
	
	private static final Map<String, String> rotConAttachmentNames;
	static {
		Map<String, String> map = new HashMap<>();
		map.put("conTypes2", "conTypes5");
		map.put("conTypes5", "conTypes3");
		map.put("conTypes3", "conTypes4");
		map.put("conTypes4", "conTypes2");
		map.put("attachment2", "attachment5");
		map.put("attachment5", "attachment3");
		map.put("attachment3", "attachment4");
		map.put("attachment4", "attachment2");
		rotConAttachmentNames = Collections.unmodifiableMap(map);
	}
	
	@Override
	public int rotate(final Block block, final int metadata, NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0 || nbtTileEntity == null) {
			return metadata;
		}
		
		// machines
		if (nbtTileEntity.hasKey("facing")) {
			int facing = nbtTileEntity.getInteger("facing");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("facing", rotFacing[facing]);
				break;
			case 2:
				nbtTileEntity.setInteger("facing", rotFacing[rotFacing[facing]]);
				break;
			case 3:
				nbtTileEntity.setInteger("facing", rotFacing[rotFacing[rotFacing[facing]]]);
				break;
			default:
				break;
			}
		}
		
		// ducts
		HashMap<String, NBTBase> mapRotated = new HashMap<>(9);
		for (String key : rotConAttachmentNames.keySet()) {
			if (nbtTileEntity.hasKey(key)) {
				NBTBase nbtBase = nbtTileEntity.getTag(key);
				nbtTileEntity.removeTag(key);
				switch (rotationSteps) {
				case 1:
					mapRotated.put(rotConAttachmentNames.get(key), nbtBase);
					break;
				case 2:
					mapRotated.put(rotConAttachmentNames.get(rotConAttachmentNames.get(key)), nbtBase);
					break;
				case 3:
					mapRotated.put(rotConAttachmentNames.get(rotConAttachmentNames.get(rotConAttachmentNames.get(key))), nbtBase);
					break;
				default:
					mapRotated.put(key, nbtBase);
					break;
				}
			}
		}
		for (Map.Entry<String, NBTBase> entry : mapRotated.entrySet()) {
			nbtTileEntity.setTag(entry.getKey(), entry.getValue());
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
