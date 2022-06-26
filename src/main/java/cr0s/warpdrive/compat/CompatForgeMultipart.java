package cr0s.warpdrive.compat;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;

import java.lang.reflect.Method;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.common.util.Constants;

public class CompatForgeMultipart implements IBlockTransformer {
	
	public static Method methodMultipartHelper_createTileFromNBT = null;
	public static Method methodMultipartHelper_sendDescPacket = null;
	public static Method methodTileMultipart_onChunkLoad = null;
	
	private static Class<?> classBlockMultipart;
	
	public static boolean register() {
		try {
			/*
			helper no longer exist on 1.12.2, not sure if the underlying issue is still applicable (hidden block in multiplayer after jump)
			=> keep in comments for now
			
			final Class<?> forgeMultipart_helper = Class.forName("codechicken.multipart.MultipartHelper");
			methodMultipartHelper_createTileFromNBT = forgeMultipart_helper.getDeclaredMethod("createTileFromNBT", World.class, CompoundNBT.class);
			methodMultipartHelper_sendDescPacket = forgeMultipart_helper.getDeclaredMethod("sendDescPacket", World.class, TileEntity.class);
			final Class<?> forgeMultipart_tileMultipart = Class.forName("codechicken.multipart.TileMultipart");
			methodTileMultipart_onChunkLoad = forgeMultipart_tileMultipart.getDeclaredMethod("onChunkLoad");
			*/
			
			classBlockMultipart = Class.forName("codechicken.multipart.BlockMultipart");
			
			WarpDriveConfig.registerBlockTransformer("ForgeMultipart", new CompatForgeMultipart());
		} catch (final ClassNotFoundException | SecurityException /* | NoSuchMethodException */ exception) {
			exception.printStackTrace();
			return false;
		}
		return true;
	}
	
	@Override
	public boolean isApplicable(final BlockState blockState, final TileEntity tileEntity) {
		return classBlockMultipart.isInstance(blockState.getBlock());
	}
	
	@Override
	public boolean isJumpReady(final BlockState blockState, final TileEntity tileEntity, final WarpDriveText reason) {
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
	
	// Microblocks shape is bits 0-3 for shape/slot, bits 4-7 for size:
	// - mcr_cnr (nook, corner, notch): 16 20 22 18 / 17 21 23 19                  => shape/slot: 0 4 6 2 / 1 5 7 3 
	// - mcr_face (cover, panel, slab): 16 / 17 / 18 21 19 20                      => shape/slot: 0 / 1 / 2 5 3 4
	// - mcr_hllw (hollow cover, panel, slab): same as mcr_face
	// - mcr_edge (strip, post, pillar): 16 18 19 17 / 20 24 21 26 / 22 25 23 27   => shape/slot: 0 2 3 1 / 4 8 5 10 / 6 9 7 11
	// - mcr_post (post, pillar):  32 / 33 34                                      => shape/slot: 0 / 1 2
	
	//                                              = {  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final byte[] rotMicroblockCorner = {  4,  5,  0,  1,  6,  7,  2,  3,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final byte[] rotMicroblockFace   = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final byte[] rotMicroblockEdge   = {  2,  0,  3,  1,  8, 10,  9, 11,  5,  7,  4,  6, 12, 13, 14, 15 };
	private static final byte[] rotMicroblockPost   = {  0,  2,  1,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	// WRCBE state is bits 0-1 for rotation, bits 2-4 for face:
	// - wrcbe-recv, wrcbe-tran, wrcbe-jamm: 0 1 2 3 / 4 7 6 5 / 8 20 12 16 / 9 21 13 17 / 10 22 14 18 / 11 23 15 19
	private static final byte[] rotWRCBEstate = {  1,  2,  3,  0,  7,  4,  5,  6,
		                                          20, 21, 22, 23, 16, 17, 18, 19,  8,  9, 10, 11, 12, 13, 14, 15,
		                                          24, 25, 26, 27, 28, 29, 30, 31 };
	
	private CompoundNBT rotate_part(final byte rotationSteps, final CompoundNBT nbtPart) {
		final CompoundNBT nbtNewPart = nbtPart.copy();
		
		if (!nbtNewPart.contains("id")) {
			WarpDrive.logger.error(String.format("Ignoring ForgeMultipart with missing id: %s", nbtPart));
		} else {
			final String id = nbtPart.getString("id");
			String propertyName = null;
			byte mask = (byte) 0xFF;
			byte[] rot = null;
			
			// microblocks
			switch (id) {
			case "ccmb:mcr_cnr":
				propertyName = "shape";
				mask = (byte) 0x0F;
				rot = rotMicroblockCorner;
				break;
				
			case "ccmb:mcr_face":
			case "ccmb:mcr_hllw":
				propertyName = "shape";
				mask = (byte) 0x0F;
				rot = rotMicroblockFace;
				break;
				
			case "ccmb:mcr_edge":
				propertyName = "shape";
				mask = (byte) 0x0F;
				rot = rotMicroblockEdge;
				break;
				
			case "ccmb:mcr_post":
				propertyName = "shape";
				mask = (byte) 0x0F;
				rot = rotMicroblockPost;
				break;
			
			// wireless redstone
			case "wrcbe:receiver":
			case "wrcbe:transmitter":
			case "wrcbe:jammer":
				propertyName = "state";
				mask = (byte) 0x1F;
				rot = rotWRCBEstate;
				break;
				
			default:
				WarpDrive.logger.error(String.format("Ignoring part of ForgeMultipart with unknown id: %s",
				                                     nbtPart));
				break;
			}
			
			// actual rotation
			if (propertyName != null && rot != null) {
				if (nbtPart.contains(propertyName)) {
					final byte value = nbtPart.getByte(propertyName);
					final byte masked = (byte) (value & mask);
					final byte notmasked = (byte) (value - masked);
					switch (rotationSteps) {
					case 1:
						nbtNewPart.putByte(propertyName, (byte) (notmasked | rot[masked]));
						break;
					case 2:
						nbtNewPart.putByte(propertyName, (byte) (notmasked | rot[rot[masked]]));
						break;
					case 3:
						nbtNewPart.putByte(propertyName, (byte) (notmasked | rot[rot[rot[masked]]]));
						break;
					default:
						break;
					}
				}
			}
		}
		return nbtNewPart;
	}
	
	@Override
	public BlockState rotate(final BlockState blockState, final CompoundNBT nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0 || nbtTileEntity == null) {
			return blockState;
		}
		
		// Parts
		if (nbtTileEntity.contains("parts")) {
			final ListNBT nbtParts = nbtTileEntity.getList("parts", Constants.NBT.TAG_COMPOUND);
			final ListNBT nbtNewParts = new ListNBT();
			for (int index = 0; index < nbtParts.size(); index++) {
				final CompoundNBT nbtPart = nbtParts.getCompound(index);
				final CompoundNBT nbtNewPart = rotate_part(rotationSteps, nbtPart);
				nbtNewParts.add(nbtNewPart);
			}
			nbtTileEntity.put("parts", nbtNewParts);
		} else {
			WarpDrive.logger.error(String.format("Ignoring ForgeMultipart with no 'parts': %s",
			                                     nbtTileEntity));
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
