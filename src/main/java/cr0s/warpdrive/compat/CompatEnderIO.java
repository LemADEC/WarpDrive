package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.config.WarpDriveConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import net.minecraftforge.common.util.Constants;

public class CompatEnderIO implements IBlockTransformer {
	
	private static Class<?> classTileEntityEIO;
	private static Class<?> classBlockReservoir;
	
	public static void register() {
		try {
			classTileEntityEIO = Class.forName("crazypants.enderio.TileEntityEio");
			classBlockReservoir = Class.forName("crazypants.enderio.machine.reservoir.BlockReservoir");
			WarpDriveConfig.registerBlockTransformer("EnderIO", new CompatEnderIO());
		} catch(ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classTileEntityEIO.isInstance(tileEntity);
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
	
	private static final short[] mrot = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final Map<String, String> rotSideNames;
	private static final Map<String, String> rotFaceNames;
	static {
		Map<String, String> map = new HashMap<>();
		map.put("EAST", "SOUTH");
		map.put("SOUTH", "WEST");
		map.put("WEST", "NORTH");
		map.put("NORTH", "EAST");
		rotSideNames = Collections.unmodifiableMap(map);
		map = new HashMap<>();
		map.put("face2", "face5");
		map.put("face5", "face3");
		map.put("face3", "face4");
		map.put("face4", "face2");
		map.put("faceDisplay2", "faceDisplay5");
		map.put("faceDisplay5", "faceDisplay3");
		map.put("faceDisplay3", "faceDisplay4");
		map.put("faceDisplay4", "faceDisplay2");
		rotFaceNames = Collections.unmodifiableMap(map);
	}
	private static final short[] rotFront         = {  0,  1,  5,  3,  4,  2,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final short[] rotRight         = {  0,  1,  4,  3,  2,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final short[] rotPosHorizontal = {  1,  3,  0,  2,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	private byte[] rotate_byteArray(final byte rotationSteps, final byte[] data) {
		byte[] newData = data.clone();
		for (int index = 0; index < data.length; index++) {
			switch (rotationSteps) {
			case 1:
				newData[mrot[index]] = data[index];
				break;
			case 2:
				newData[mrot[mrot[index]]] = data[index];
				break;
			case 3:
				newData[mrot[mrot[mrot[index]]]] = data[index];
				break;
			default:
				break;
			}
		}
		return newData;
	}
	private NBTTagCompound rotate_conduit(final byte rotationSteps, NBTTagCompound nbtConduit) {
		NBTTagCompound nbtNewConduit = new NBTTagCompound();
		Set<String> keys = nbtConduit.func_150296_c();
		for (String key : keys) {
			NBTBase base = nbtConduit.getTag(key);
			switch(base.getId()) {
			case Constants.NBT.TAG_INT_ARRAY:	// "connections", "externalConnections"
				int[] data = nbtConduit.getIntArray(key);
				int[] newData = data.clone();
				for (int index = 0; index < data.length; index++) {
					switch (rotationSteps) {
					case 1:
						newData[index] = mrot[data[index]];
						break;
					case 2:
						newData[index] = mrot[mrot[data[index]]];
						break;
					case 3:
						newData[index] = mrot[mrot[mrot[data[index]]]];
						break;
					default:
						break;
					}
				}
				nbtNewConduit.setIntArray(key, newData);
				break;
				
			case Constants.NBT.TAG_BYTE_ARRAY:	// "conModes", "signalColors", "forcedConnections", "signalStrengths"
				nbtNewConduit.setByteArray(key, rotate_byteArray(rotationSteps, nbtConduit.getByteArray(key)));
				break;
				
			default:
				String[] parts = key.split("\\.");
				if (parts.length != 2 || !rotSideNames.containsKey(parts[1])) {
					nbtNewConduit.setTag(key, base);
				} else {
					switch (rotationSteps) {
					case 1:
						nbtNewConduit.setTag(parts[0] + "." + rotSideNames.get(parts[1]), base);
						break;
					case 2:
						nbtNewConduit.setTag(parts[0] + "." + rotSideNames.get(rotSideNames.get(parts[1])), base);
						break;
					case 3:
						nbtNewConduit.setTag(parts[0] + "." + rotSideNames.get(rotSideNames.get(rotSideNames.get(parts[1]))), base);
						break;
					default:
						nbtNewConduit.setTag(key, base);
						break;
					}
				}
				break;
			}
		}
		return nbtNewConduit;
	}
	
	private void rotateReservoir(NBTTagCompound nbtTileEntity, final ITransformation transformation, final byte rotationSteps) {
		if (nbtTileEntity.hasKey("front")) {
			short front = nbtTileEntity.getShort("front");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setShort("front", rotFront[front]);
				break;
			case 2:
				nbtTileEntity.setShort("front", rotFront[rotFront[front]]);
				break;
			case 3:
				nbtTileEntity.setShort("front", rotFront[rotFront[rotFront[front]]]);
				break;
			default:
				break;
			}
		}
		if (nbtTileEntity.hasKey("right")) {
			short right = nbtTileEntity.getShort("right");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setShort("right", rotRight[right]);
				break;
			case 2:
				nbtTileEntity.setShort("right", rotRight[rotRight[right]]);
				break;
			case 3:
				nbtTileEntity.setShort("right", rotRight[rotRight[rotRight[right]]]);
				break;
			default:
				break;
			}
		}
		
		// Multiblock
		if (nbtTileEntity.hasKey("multiblock") && nbtTileEntity.hasKey("pos")) {
			int[] oldCoords = nbtTileEntity.getIntArray("multiblock");
			ChunkCoordinates[] targets = new ChunkCoordinates[oldCoords.length / 3];
			for (int index = 0; index < oldCoords.length / 3; index++) {
				targets[index] = transformation.apply(oldCoords[3 * index], oldCoords[3 * index + 1], oldCoords[3 * index + 2]);
			}
			if (targets[0].posY == targets[1].posY && targets[1].posY == targets[2].posY && targets[2].posY == targets[3].posY) {
				short pos = nbtTileEntity.getShort("pos");
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setShort("pos", rotPosHorizontal[pos]);
					break;
				case 2:
					nbtTileEntity.setShort("pos", rotPosHorizontal[rotPosHorizontal[pos]]);
					break;
				case 3:
					nbtTileEntity.setShort("pos", rotPosHorizontal[rotPosHorizontal[rotPosHorizontal[pos]]]);
					break;
				default:
					break;
				}
			} else {
				ChunkCoordinates newPos = transformation.apply(nbtTileEntity.getInteger("x"), nbtTileEntity.getInteger("y"), nbtTileEntity.getInteger("z"));
				if (targets[0].posX == targets[1].posX && targets[1].posX == targets[2].posX && targets[2].posX == targets[3].posX) {
					int minZ = Math.min(targets[0].posZ, Math.min(targets[1].posZ, targets[2].posZ));
					short pos = (short) ((nbtTileEntity.getShort("pos") & 2) + (newPos.posZ == minZ ? 1 : 0));	// 2 & 3 are for bottom
					nbtTileEntity.setShort("pos", pos);
				} else {
					int minX = Math.min(targets[0].posX, Math.min(targets[1].posX, targets[2].posX));
					short pos = (short) ((nbtTileEntity.getShort("pos") & 2) + (newPos.posX == minX ? 1 : 0));	// 2 & 3 are for bottom
					nbtTileEntity.setShort("pos", pos);
				}
			}
			
			int[] newCoords = new int[oldCoords.length];
			for (int index = 0; index < oldCoords.length / 3; index++) {
				newCoords[3 * index    ] = targets[index].posX;
				newCoords[3 * index + 1] = targets[index].posY;
				newCoords[3 * index + 2] = targets[index].posZ;
			}
			nbtTileEntity.setIntArray("multiblock", newCoords);
		}
	}
	
	@Override
	public int rotate(final Block block, final int metadata, NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		byte rotationSteps = transformation.getRotationSteps();
		
		if (nbtTileEntity.hasKey("facing")) {
			short facing = nbtTileEntity.getShort("facing");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setShort("facing", mrot[facing]);
				return metadata;
			case 2:
				nbtTileEntity.setShort("facing", mrot[mrot[facing]]);
				return metadata;
			case 3:
				nbtTileEntity.setShort("facing", mrot[mrot[mrot[facing]]]);
				return metadata;
			default:
				return metadata;
			}
		}
		
		// Reservoir
		if (classBlockReservoir.isInstance(block)) {
			rotateReservoir(nbtTileEntity, transformation, rotationSteps);
		}
		
		// Faces
		Map<String, Short> map = new HashMap<>();
		for (String key : rotFaceNames.keySet()) {
			if (nbtTileEntity.hasKey(key)) {
				short face = nbtTileEntity.getShort(key);
				switch (rotationSteps) {
				case 1:
					map.put(rotFaceNames.get(key), face);
					break;
				case 2:
					map.put(rotFaceNames.get(rotFaceNames.get(key)), face);
					break;
				case 3:
					map.put(rotFaceNames.get(rotFaceNames.get(rotFaceNames.get(key))), face);
					break;
				default:
					map.put(key, face);
					break;
				}
				nbtTileEntity.removeTag(key);
			}
		}
		if (!map.isEmpty()) {
			for (Entry<String, Short> entry : map.entrySet()) {
				nbtTileEntity.setShort(entry.getKey(), entry.getValue());
			}
		}
		
		// Conduits
		if (nbtTileEntity.hasKey("conduits")) {
			NBTTagList nbtConduits = nbtTileEntity.getTagList("conduits", Constants.NBT.TAG_COMPOUND);
			NBTTagList nbtNewConduits = new NBTTagList(); 
			for (int index = 0; index < nbtConduits.tagCount(); index++) {
				NBTTagCompound conduitTypeAndContent = nbtConduits.getCompoundTagAt(index);
				NBTTagCompound newConduitTypeAndContent = new NBTTagCompound();
				newConduitTypeAndContent.setString("conduitType", conduitTypeAndContent.getString("conduitType"));
				newConduitTypeAndContent.setTag("conduit", rotate_conduit(rotationSteps, conduitTypeAndContent.getCompoundTag("conduit")));
				nbtNewConduits.appendTag(newConduitTypeAndContent);
			}
			nbtTileEntity.setTag("conduits", nbtNewConduits);
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
