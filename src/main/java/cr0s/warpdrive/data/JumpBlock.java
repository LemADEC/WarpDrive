package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.FastSetBlockState;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.computer.ICoreSignature;
import cr0s.warpdrive.block.energy.BlockCapacitor;
import cr0s.warpdrive.block.movement.BlockShipCore;
import cr0s.warpdrive.compat.CompatForgeMultipart;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.config.Filler;
import cr0s.warpdrive.config.WarpDriveDataFixer;
import cr0s.warpdrive.network.PacketHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockEndPortalFrame;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockHugeMushroom;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockPistonExtension;
import net.minecraft.block.BlockPistonMoving;
import net.minecraft.block.BlockPortal;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockTripWireHook;
import net.minecraft.block.BlockVine;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;

import ic2.api.network.INetworkDataProvider;
import ic2.api.network.INetworkManager;
import ic2.api.network.NetworkHelper;

public class JumpBlock {
	
	public Block block;
	public int blockMeta;
	public boolean hasTileEntity;
	public WeakReference<TileEntity> weakTileEntity;
	public NBTTagCompound blockNBT;
	public int x;
	public int y;
	public int z;
	public HashMap<String, NBTBase> externals;
	
	public JumpBlock() {
	}
	
	public JumpBlock(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState, @Nullable final TileEntity tileEntity) {
		this.x = blockPos.getX();
		this.y = blockPos.getY();
		this.z = blockPos.getZ();
		this.block = blockState.getBlock();
		this.blockMeta = blockState.getBlock().getMetaFromState(blockState);
		if (tileEntity == null) {
			hasTileEntity = false;
			weakTileEntity = null;
			blockNBT = null;
		} else {
			hasTileEntity = true;
			weakTileEntity = new WeakReference<>(tileEntity);
			blockNBT = new NBTTagCompound();
			tileEntity.writeToNBT(blockNBT);
			if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
				WarpDrive.logger.info(String.format("Saving from (%d %d %d) with TileEntity %s",
				                                    x, y, z, blockNBT));
			}
		}
		
		// save externals
		for (final Entry<String, IBlockTransformer> entryBlockTransformer : WarpDriveConfig.blockTransformers.entrySet()) {
			if (entryBlockTransformer.getValue().isApplicable(block, blockMeta, tileEntity)) {
				final NBTBase nbtBase = entryBlockTransformer.getValue().saveExternals(world, x, y, z, block, blockMeta, tileEntity);
				// (we always save, even if null as a reminder on which transformer applies to this block)
				setExternal(entryBlockTransformer.getKey(), nbtBase);
			}
		}
	}
	
	public JumpBlock(@Nonnull final Filler filler, final int x, final int y, final int z) {
		if (filler.block == null) {
			WarpDrive.logger.info(String.format("Forcing glass for invalid filler with null block at (%d %d %d)",
			                                    x, y, z));
			filler.block = Blocks.GLASS;
		}
		block = filler.block;
		blockMeta = filler.metadata;
		hasTileEntity = false;
		weakTileEntity = null;
		blockNBT = (filler.tagCompound != null) ? filler.tagCompound.copy() : null;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void refreshSource(@Nonnull final World worldSource) {
		final BlockPos blockPos = new BlockPos(x, y, z);
		final IBlockState blockState = worldSource.getBlockState(blockPos);
		if (blockState.getBlock() != block) {
			WarpDrive.logger.error(String.format("Source block has changed to %s, updating in %s",
			                                     blockState, this ));
			block = blockState.getBlock();
			blockMeta = block.getMetaFromState(blockState);
			hasTileEntity = false;
			weakTileEntity = null;
			blockNBT = null;
			return;
		}
		if (block.getMetaFromState(blockState) != blockMeta) {
			WarpDrive.logger.error(String.format("Source block variation has changed to %s, updating in %s",
			                                     blockState, this ));
			blockMeta = block.getMetaFromState(blockState);
		}
		final TileEntity tileEntity = worldSource.getTileEntity(blockPos);
		if ( (hasTileEntity && tileEntity == null)
		  || (!hasTileEntity && tileEntity != null) ) {
			WarpDrive.logger.error(String.format("Tile entity has changed, refreshing in %s",
			                                     this));
		}
		hasTileEntity = tileEntity != null;
		if (hasTileEntity) {
			weakTileEntity = new WeakReference<>(tileEntity);
		} else {
			weakTileEntity = null;
		}
		blockNBT = null;
	}
	
	public TileEntity getTileEntity(@Nonnull final World worldSource) {
		if (!hasTileEntity) {
			return null;
		}
		TileEntity tileEntity = weakTileEntity.get();
		if (tileEntity != null) {
			return tileEntity;
		}
		WarpDrive.logger.error(String.format("Tile entity lost in %s",
		                                     this));
		tileEntity = worldSource.getTileEntity(new BlockPos(x, y, z));
		weakTileEntity = new WeakReference<>(tileEntity);
		return tileEntity;
	}
	
	@Nullable
	private NBTTagCompound getBlockNBT(@Nonnull final World worldSource) {
		if (blockNBT != null) {
			return blockNBT.copy();
		}
		if (!hasTileEntity) {
			return null;
		}
		final TileEntity tileEntity = getTileEntity(worldSource);
		if (tileEntity == null) {
			WarpDrive.logger.error(String.format("No more tile entity in %s",
			                                     this ));
			return null;
		}
		final NBTTagCompound tagCompound = new NBTTagCompound();
		tileEntity.writeToNBT(tagCompound);
		return tagCompound;
	}
	
	public NBTBase getExternal(final String modId) {
		if (externals == null) {
			return null;
		}
		final NBTBase nbtExternal = externals.get(modId);
		if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
			WarpDrive.logger.info(String.format("Returning externals from (%d %d %d) of %s: %s",
			                                    x, y, z, modId, nbtExternal));
		}
		if (nbtExternal == null) {
			return null;
		}
		return nbtExternal.copy();
	}
	
	private void setExternal(final String modId, final NBTBase nbtExternal) {
		if (externals == null) {
			externals = new HashMap<>();
		}
		externals.put(modId, nbtExternal);
		if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
			WarpDrive.logger.info(String.format("Saved externals from (%d %d %d) of %s: %s",
			                                    x, y, z, modId, nbtExternal));
		}
	}
	
	private static final byte[] mrotNone           = {  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final byte[] mrotRail           = {  1,  0,  5,  4,  2,  3,  7,  8,  9,  6, 10, 11, 12, 13, 14, 15 };
	private static final byte[] mrotAnvil          = {  1,  2,  3,  0,  5,  6,  7,  4,  9, 10, 11,  8, 12, 13, 14, 15 };
	private static final byte[] mrotFenceGate      = {  1,  2,  3,  0,  5,  6,  7,  4,  9, 10, 11,  8, 13, 14, 15, 12 };
	private static final byte[] mrotPumpkin        = {  1,  2,  3,  0,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };	// Tripwire hook, Pumpkin, Jack-o-lantern
	private static final byte[] mrotEndPortalFrame = {  1,  2,  3,  0,  5,  6,  7,  4,  8,  9, 10, 11, 12, 13, 14, 15 };	// EndPortal, doors (open/closed, base/top)
	private static final byte[] mrotCocoa          = {  1,  2,  3,  0,  5,  6,  7,  4,  9, 10, 11,  8, 12, 13, 14, 15 };
	private static final byte[] mrotRepeater       = {  1,  2,  3,  0,  5,  6,  7,  4,  9, 10, 11,  8, 13, 14, 15, 12 };	// Repeater (normal/lit), Comparator
	private static final byte[] mrotBed            = {  1,  2,  3,  0,  4,  5,  6,  7,  9, 10, 11,  8, 12, 13, 14, 15 };
	private static final byte[] mrotStair          = {  2,  3,  1,  0,  6,  7,  5,  4,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final byte[] mrotSign           = {  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15,  0,  1,  2,  3 };	// Sign, Skull
	private static final byte[] mrotTrapDoor       = {  3,  2,  0,  1,  7,  6,  4,  5, 11, 10,  8,  9, 15, 14, 12, 13 };
	private static final byte[] mrotLever          = {  7,  3,  4,  2,  1,  6,  5,  0, 15, 11, 12, 10,  9, 14, 13,  8 };
	private static final byte[] mrotNetherPortal   = {  0,  2,  1,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final byte[] mrotVine           = {  0,  2,  4,  6,  8, 10, 12, 14,  1,  3,  5,  7,  9, 11, 13, 15 };
	private static final byte[] mrotButton         = {  0,  3,  4,  2,  1,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };	// Button, torch (normal, redstone lit/unlit)
	private static final byte[] mrotMushroom       = {  0,  3,  6,  9,  2,  5,  8,  1,  4,  7, 10, 11, 12, 13, 14, 15 };	// Red/brown mushroom block
	private static final byte[] mrotForgeDirection = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };	// Furnace (lit/normal), Dispenser/Dropper, Enderchest, Chest (normal/trapped), Hopper, Ladder, Wall sign
	private static final byte[] mrotPiston         = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 13, 12, 10, 11, 14, 15 };	// Pistons (sticky/normal, base/head)
	private static final byte[] mrotWoodLog        = {  0,  1,  2,  3,  8,  9, 10, 11,  4,  5,  6,  7, 12, 13, 14, 15 };
	
	// Return updated metadata from rotating a vanilla block
	private int getMetadataRotation(final NBTTagCompound nbtTileEntity, final byte rotationSteps) {
		if (rotationSteps == 0) {
			return blockMeta;
		}
		
		byte[] mrot = mrotNone;
		if (block instanceof BlockRailBase) {
			mrot = mrotRail;
		} else if (block instanceof BlockAnvil) {
			mrot = mrotAnvil;
		} else if (block instanceof BlockFenceGate) {
			mrot = mrotFenceGate;
		} else if (block instanceof BlockPumpkin || block instanceof BlockTripWireHook) {
			mrot = mrotPumpkin;
		} else if (block instanceof BlockEndPortalFrame || block instanceof BlockDoor) {
			mrot = mrotEndPortalFrame;
		} else if (block instanceof BlockCocoa) {
			mrot = mrotCocoa;
		} else if (block instanceof BlockRedstoneDiode) {
			mrot = mrotRepeater;
		} else if (block instanceof BlockBed) {
			mrot = mrotBed;
		} else if (block instanceof BlockStairs) {
			mrot = mrotStair;
		} else if (block instanceof BlockSign) {
			if (block == Blocks.WALL_SIGN) {
				mrot = mrotForgeDirection;
			} else {
				mrot = mrotSign;
			}
		} else if (block instanceof BlockTrapDoor) {
			mrot = mrotTrapDoor;
		} else if (block instanceof BlockLever) {
			mrot = mrotLever;
		} else if (block instanceof BlockPortal) {
			mrot = mrotNetherPortal;
		} else if (block instanceof BlockVine) {
			mrot = mrotVine;
		} else if (block instanceof BlockButton || block instanceof BlockTorch) {
			mrot = mrotButton;
		} else if (block instanceof BlockHugeMushroom) {
			mrot = mrotMushroom;
		} else if (block instanceof BlockFurnace || block instanceof BlockDispenser || block instanceof BlockHopper
				|| block instanceof BlockChest || block instanceof BlockEnderChest || block instanceof BlockLadder) {
			mrot = mrotForgeDirection;
		} else if (block instanceof BlockPistonBase || block instanceof BlockPistonExtension || block instanceof BlockPistonMoving) {
			mrot = mrotPiston;
		} else if (block instanceof BlockLog) {
			mrot = mrotWoodLog;
		} else if (block instanceof BlockSkull) {
			// mrot = mrotNone;
			final byte facing = nbtTileEntity.getByte("Rot");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setByte("Rot", mrotSign[facing]);
				break;
			case 2:
				nbtTileEntity.setByte("Rot", mrotSign[mrotSign[facing]]);
				break;
			case 3:
				nbtTileEntity.setByte("Rot", mrotSign[mrotSign[mrotSign[facing]]]);
				break;
			default:
				break;
			}
		} else {
			// apply default transformer
			return IBlockTransformer.rotateFirstEnumFacingProperty(block, blockMeta, rotationSteps);
		}
		
		switch (rotationSteps) {
		case 1:
			return mrot[blockMeta];
		case 2:
			return mrot[mrot[blockMeta]];
		case 3:
			return mrot[mrot[mrot[blockMeta]]];
		default:
			return blockMeta;
		}
	}
	
	@Nullable
	public BlockPos deploy(final World worldSource, final World worldTarget, final ITransformation transformation) {
		try {
			final NBTTagCompound nbtToDeploy = getBlockNBT(worldSource);
			int newBlockMeta = blockMeta;
			if (externals != null) {
				for (final Entry<String, NBTBase> external : externals.entrySet()) {
					final IBlockTransformer blockTransformer = WarpDriveConfig.blockTransformers.get(external.getKey());
					if (blockTransformer != null) {
						newBlockMeta = blockTransformer.rotate(block, blockMeta, nbtToDeploy, transformation);
					}
				}
			} else {
				newBlockMeta = getMetadataRotation(nbtToDeploy, transformation.getRotationSteps());
			}
			final BlockPos target = transformation.apply(x, y, z);
			if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
				WarpDrive.logger.info(String.format("Deploying to (%d %d %d) of %s@%d: %s",
				                                    target.getX(), target.getY(), target.getZ(),
				                                    block, newBlockMeta, nbtToDeploy));
			}
			final IBlockState blockState = block.getStateFromMeta(newBlockMeta);
			FastSetBlockState.setBlockStateNoLight(worldTarget, target, blockState, 2);
			
			if (nbtToDeploy != null) {
				nbtToDeploy.setInteger("x", target.getX());
				nbtToDeploy.setInteger("y", target.getY());
				nbtToDeploy.setInteger("z", target.getZ());
				
				TileEntity newTileEntity = null;
				boolean isForgeMultipart = false;
				if ( WarpDriveConfig.isForgeMultipartLoaded
				  && nbtToDeploy.hasKey("id")
				  && nbtToDeploy.getString("id").equals("savedMultipart") ) {
					isForgeMultipart = true;
					if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
						WarpDrive.logger.info(String.format("%s deploy: TileEntity is ForgeMultipart",
						                                    this ));
					}
					newTileEntity = (TileEntity) CompatForgeMultipart.methodMultipartHelper_createTileFromNBT.invoke(null, worldTarget, nbtToDeploy);
				}
				
				if (newTileEntity == null) {
					newTileEntity = TileEntity.create(worldTarget, nbtToDeploy);
					if (newTileEntity == null) {
						WarpDrive.logger.error(String.format("%s deploy failed to create new tile entity %s block %s:%d",
						                                     this, Commons.format(worldTarget, x, y, z), block, blockMeta ));
						WarpDrive.logger.error(String.format("NBT data was %s",
						                                     nbtToDeploy ));
					}
				}
				
				if (newTileEntity != null) {
					worldTarget.setTileEntity(target, newTileEntity);
					if (isForgeMultipart) {
						CompatForgeMultipart.methodTileMultipart_onChunkLoad.invoke(newTileEntity);
						CompatForgeMultipart.methodMultipartHelper_sendDescPacket.invoke(null, worldTarget, newTileEntity);
					}
					
					// see https://github.com/MinecraftForge/MinecraftForge/issues/5061
					newTileEntity.onLoad();
					
					newTileEntity.markDirty();
				}
			}
			return target;
			
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
			WarpDrive.logger.error(String.format("Deploy failed from (%d %d %d) of %s:%d",
			                                     x, y, z, block, blockMeta));
		}
		return null;
	}
	
	public static void refreshBlockStateOnClient(@Nonnull final World world, @Nonnull final BlockPos blockPos) {
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (tileEntity != null) {
			final Class<?> teClass = tileEntity.getClass();
			if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
				WarpDrive.logger.info(String.format("Refreshing clients %s with %s derived from %s",
				                                    Commons.format(world, blockPos),
				                                    teClass,
				                                    teClass.getSuperclass()));
			}
			
			// is it required?
			tileEntity.updateContainingBlockInfo();
			
			// Forces a full client re-sync (getUpdateTag -> handleUpdateTag), like a chunk reload would do:
			// the vanilla per-block update only carries getUpdatePacket(), which some mods (e.g. GregTech pipes/
			// cables) leave empty after a bulk placement.
			PacketHandler.sendTileEntitySyncToClients(world, blockPos, tileEntity.getUpdateTag());
			
			final String className = teClass.getName();
			try {
				if (WarpDriveConfig.isIndustrialCraft2Loaded) {
					if (tileEntity instanceof INetworkDataProvider) {
						final List<String> fields = ((INetworkDataProvider) tileEntity).getNetworkedFields();
						if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
							WarpDrive.logger.info(String.format("Tile has %d networked fields: %s",
							                                    fields.size(), fields ));
						}
						final INetworkManager networkManager = NetworkHelper.getNetworkManager(Side.SERVER);
						for (final String field : fields) {
							try {
								networkManager.updateTileEntityField(tileEntity, field);
							} catch (final Exception exception) {
								throw new RuntimeException(exception);
							}
						}
					}
				}
			} catch (final Exception exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.info(String.format("Exception involving TileEntity %s %s",
				                                    className, Commons.format(world, blockPos)));
			}
		}
	}
	
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		final String blockName = tagCompound.getString("block");
		blockMeta = tagCompound.getByte("blockMeta");
		final String stringBlockState = String.format("%s@%d", blockName, blockMeta);
		final IBlockState blockState = WarpDriveDataFixer.getBlockState(stringBlockState);
		if (blockState == null) {
			if (WarpDriveConfig.LOGGING_BUILDING) {
				WarpDrive.logger.warn(String.format("Ignoring unknown blockstate %s from tag %s, consider updating your warpdrive/dataFixer.yml",
				                                    stringBlockState, tagCompound));
			}
			block = Blocks.AIR;
			return;
		}
		block = blockState.getBlock();
		blockMeta = blockState.getBlock().getMetaFromState(blockState);
		
		hasTileEntity = false;
		weakTileEntity = null;
		if (tagCompound.hasKey("blockNBT")) {
			blockNBT = tagCompound.getCompoundTag("blockNBT");
			
			// Clear computer IDs
			if (blockNBT.hasKey("computerID")) {
				blockNBT.removeTag("computerID");
			}
			if (blockNBT.hasKey("oc:computer")) {
				final NBTTagCompound tagComputer = blockNBT.getCompoundTag("oc:computer");
				tagComputer.removeTag("components");
				tagComputer.removeTag("node");
				blockNBT.setTag("oc:computer", tagComputer);
			}
		} else {
			blockNBT = null;
		}
		x = tagCompound.getInteger("x");
		y = tagCompound.getInteger("y");
		z = tagCompound.getInteger("z");
		if (tagCompound.hasKey("externals")) {
			final NBTTagCompound tagCompoundExternals = tagCompound.getCompoundTag("externals");
			externals = new HashMap<>();
			for (final Object key : tagCompoundExternals.getKeySet()) {
				assert key instanceof String;
				externals.put((String) key, tagCompoundExternals.getTag((String) key));
			}
		} else {
			externals = null;
		}
	}
	
	public void writeToNBT(@Nonnull final World worldSource, @Nonnull final NBTTagCompound tagCompound) {
		tagCompound.setString("block", Block.REGISTRY.getNameForObject(block).toString());
		tagCompound.setByte("blockMeta", (byte) blockMeta);
		final NBTTagCompound nbtTileEntity = getBlockNBT(worldSource);
		if (nbtTileEntity != null) {
			tagCompound.setTag("blockNBT", nbtTileEntity);
		}
		tagCompound.setInteger("x", x);
		tagCompound.setInteger("y", y);
		tagCompound.setInteger("z", z);
		if (externals != null && !externals.isEmpty()) {
			final NBTTagCompound tagCompoundExternals = new NBTTagCompound();
			for (final Entry<String, NBTBase> entry : externals.entrySet()) {
				if (entry.getValue() == null) {
					tagCompoundExternals.setString(entry.getKey(), "");
				} else {
					tagCompoundExternals.setTag(entry.getKey(), entry.getValue());
				}
			}
			tagCompound.setTag("externals", tagCompoundExternals);
		}
	}
	
	public void removeUniqueIDs() {
		removeUniqueIDs(blockNBT);
	}
	
	public static void removeUniqueIDs(final NBTTagCompound tagCompound) {
		if (tagCompound == null) {
			return;
		}
		
		// ComputerCraft computer
		if (tagCompound.hasKey("computerID")) {
			tagCompound.removeTag("computerID");
			tagCompound.removeTag("label");
		}
		
		// WarpDrive machine signature UUID
		if (tagCompound.hasKey(ICoreSignature.UUID_MOST_TAG)) {
			tagCompound.removeTag(ICoreSignature.UUID_MOST_TAG);
			tagCompound.removeTag(ICoreSignature.UUID_LEAST_TAG);
		}
		
		// WarpDrive any OC connected tile
		if (tagCompound.hasKey("oc:node")) {
			tagCompound.removeTag("oc:node");
		}
		
		// WarpDrive ship jump count
		if (tagCompound.hasKey("jumpCount")) {
			tagCompound.removeTag("jumpCount");
		}
		
		// OpenComputers case
		if (tagCompound.hasKey("oc:computer")) {
			final NBTTagCompound tagComputer = tagCompound.getCompoundTag("oc:computer");
			tagComputer.removeTag("chunkX");
			tagComputer.removeTag("chunkZ");
			tagComputer.removeTag("components");
			tagComputer.removeTag("dimension");
			tagComputer.removeTag("node");
			tagCompound.setTag("oc:computer", tagComputer);
		}
		
		// OpenComputers case
		if (tagCompound.hasKey("oc:items")) {
			final NBTTagList tagListItems = tagCompound.getTagList("oc:items", Constants.NBT.TAG_COMPOUND);
			for (int indexItemSlot = 0; indexItemSlot < tagListItems.tagCount(); indexItemSlot++) {
				final NBTTagCompound tagCompoundItemSlot = tagListItems.getCompoundTagAt(indexItemSlot);
				final NBTTagCompound tagCompoundItem = tagCompoundItemSlot.getCompoundTag("item");
				final NBTTagCompound tagCompoundTag = tagCompoundItem.getCompoundTag("tag");
				final NBTTagCompound tagCompoundOCData = tagCompoundTag.getCompoundTag("oc:data");
				final NBTTagCompound tagCompoundNode = tagCompoundOCData.getCompoundTag("node");
				if (tagCompoundNode.hasKey("address")) {
					tagCompoundNode.removeTag("address");
				}
			}
		}
		
		// OpenComputers keyboard
		if (tagCompound.hasKey("oc:keyboard")) {
			final NBTTagCompound tagCompoundKeyboard = tagCompound.getCompoundTag("oc:keyboard");
			tagCompoundKeyboard.removeTag("node");
		}
		
		// OpenComputers screen
		if (tagCompound.hasKey("oc:hasPower")) {
			tagCompound.removeTag("node");
		}
		
		// Immersive Engineering & Thermal Expansion
		if (tagCompound.hasKey("Owner")) {
			tagCompound.setString("Owner", "None");
		}
		if (tagCompound.hasKey("OwnerUUID")) {
			tagCompound.removeTag("OwnerUUID");
		}
		
		// Mekanism
		if (tagCompound.hasKey("owner")) {
			tagCompound.setString("owner", "None");
		}
		if (tagCompound.hasKey("ownerUUID")) {
			tagCompound.removeTag("ownerUUID");
		}
	}
	
	public static void emptyEnergyStorage(@Nonnull final NBTTagCompound tagCompound) {
		// BuildCraft
		if (tagCompound.hasKey("battery", NBT.TAG_COMPOUND)) {
			final NBTTagCompound tagCompoundBattery = tagCompound.getCompoundTag("battery");
			if (tagCompoundBattery.hasKey("energy", NBT.TAG_INT)) {
				tagCompoundBattery.setInteger("energy", 0);
			}
		}
		
		// Gregtech
		if (tagCompound.hasKey("mStoredEnergy", NBT.TAG_INT)) {
			tagCompound.setInteger("mStoredEnergy", 0);
		}
		
		// IC2
		if (tagCompound.hasKey("energy", NBT.TAG_DOUBLE)) {
			// energy_consume((int)Math.round(blockNBT.getDouble("energy")), true);
			tagCompound.setDouble("energy", 0);
		}
		
		// Immersive Engineering & Thermal Expansion
		if (tagCompound.hasKey("Energy", NBT.TAG_INT)) {
			// energy_consume(blockNBT.getInteger("Energy"), true);
			tagCompound.setInteger("Energy", 0);
		}
		
		// Mekanism
		if (tagCompound.hasKey("electricityStored", NBT.TAG_DOUBLE)) {
			tagCompound.setDouble("electricityStored", 0);
		}
		
		// WarpDrive
		if (tagCompound.hasKey("energy", NBT.TAG_LONG)) {
			tagCompound.setLong("energy", 0L);
		}
	}
	
	public void fillEnergyStorage() {
		if (block instanceof IBlockBase) {
			final EnumTier enumTier = ((IBlockBase) block).getTier(null);
			if (enumTier != EnumTier.CREATIVE) {
				if (block instanceof BlockShipCore) {
					blockNBT.setLong(EnergyWrapper.TAG_ENERGY, WarpDriveConfig.SHIP_MAX_ENERGY_STORED_BY_TIER[enumTier.getIndex()]);
				}
				if (block instanceof BlockCapacitor) {
					blockNBT.setLong(EnergyWrapper.TAG_ENERGY, WarpDriveConfig.CAPACITOR_MAX_ENERGY_STORED_BY_TIER[enumTier.getIndex()]);
				}
			}
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s @ (%d %d %d) %s:%d %s nbt %s",
		                     getClass().getSimpleName(),
		                     x, y, z,
		                     block.getRegistryName(), blockMeta,
		                     weakTileEntity == null ? null : weakTileEntity.get(),
		                     blockNBT);
	}
}
