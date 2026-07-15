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
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.config.Filler;
import cr0s.warpdrive.config.WarpDriveDataFixer;
import cr0s.warpdrive.network.PacketHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map.Entry;

import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.CocoaBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.EndPortalFrameBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.FurnaceBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.HugeMushroomBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.LogBlock;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.PistonHeadBlock;
import net.minecraft.block.MovingPistonBlock;
import net.minecraft.block.PumpkinBlock;
import net.minecraft.block.RedstoneDiodeBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TorchBlock;
import net.minecraft.block.TrapDoorBlock;
import net.minecraft.block.TripWireHookBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.Constants.NBT;

import com.mojang.datafixers.Dynamic;

public class JumpBlock {
	
	public BlockState blockState;
	public boolean hasTileEntity;
	public WeakReference<TileEntity> weakTileEntity;
	public CompoundNBT blockNBT;
	public int x;
	public int y;
	public int z;
	public HashMap<String, INBT> externals;
	
	public JumpBlock() {
	}
	
	public JumpBlock(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final BlockState blockState, @Nullable final TileEntity tileEntity) {
		this.x = blockPos.getX();
		this.y = blockPos.getY();
		this.z = blockPos.getZ();
		this.blockState = blockState;
		if (tileEntity == null) {
			hasTileEntity = false;
			weakTileEntity = null;
			blockNBT = null;
		} else {
			hasTileEntity = true;
			weakTileEntity = new WeakReference<>(tileEntity);
			blockNBT = new CompoundNBT();
			tileEntity.write(blockNBT);
			if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
				WarpDrive.logger.info(String.format("Saving from (%d %d %d) with TileEntity %s",
				                                    x, y, z, blockNBT));
			}
		}
		
		// save externals
		for (final Entry<String, IBlockTransformer> entryBlockTransformer : WarpDriveConfig.blockTransformers.entrySet()) {
			if (entryBlockTransformer.getValue().isApplicable(blockState, tileEntity)) {
				final INBT nbtBase = entryBlockTransformer.getValue().saveExternals(world, x, y, z, blockState, tileEntity);
				// (we always save, even if null as a reminder on which transformer applies to this block)
				setExternal(entryBlockTransformer.getKey(), nbtBase);
			}
		}
	}
	
	public JumpBlock(@Nonnull final Filler filler, final int x, final int y, final int z) {
		if (filler.blockState == null) {
			WarpDrive.logger.warn(String.format("Forcing glass for invalid filler with null block at (%d %d %d)",
			                                    x, y, z ));
			filler.blockState = Blocks.GLASS.getDefaultState();
		}
		blockState = filler.blockState;
		hasTileEntity = false;
		weakTileEntity = null;
		blockNBT = (filler.tagCompound != null) ? filler.tagCompound.copy() : null;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void refreshSource(@Nonnull final World worldSource) {
		final BlockPos blockPos = new BlockPos(x, y, z);
		final BlockState blockState = worldSource.getBlockState(blockPos);
		if (blockState != this.blockState) {
			WarpDrive.logger.error(String.format("Source block has changed to %s, updating in %s",
			                                     blockState, this ));
			this.blockState = blockState;
			hasTileEntity = false;
			weakTileEntity = null;
			blockNBT = null;
			return;
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
	private CompoundNBT getBlockNBT(@Nonnull final World worldSource) {
		if (!hasTileEntity) {
			return blockNBT == null ? null : blockNBT.copy();
		}
		final TileEntity tileEntity = getTileEntity(worldSource);
		if (tileEntity == null) {
			WarpDrive.logger.error(String.format("No more tile entity in %s",
			                                     this ));
			return null;
		}
		final CompoundNBT tagCompound = new CompoundNBT();
		tileEntity.write(tagCompound);
		return tagCompound;
	}
	
	public INBT getExternal(final String modId) {
		if (externals == null) {
			return null;
		}
		final INBT nbtExternal = externals.get(modId);
		if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
			WarpDrive.logger.info(String.format("Returning externals from (%d %d %d) of %s: %s",
			                                    x, y, z, modId, nbtExternal));
		}
		if (nbtExternal == null) {
			return null;
		}
		return nbtExternal.copy();
	}
	
	private void setExternal(final String modId, final INBT nbtExternal) {
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
	private BlockState getRotatedBlockState(final CompoundNBT nbtTileEntity, final byte rotationSteps) {
		if (rotationSteps == 0) {
			return blockState;
		}
		
		return blockState;
		/* TODO MC1.15 vanilla rotation
		final Block block = blockState.getBlock();
		byte[] mrot = mrotNone;
		if (block instanceof AbstractRailBlock) {
			mrot = mrotRail;
		} else if (block instanceof AnvilBlock) {
			mrot = mrotAnvil;
		} else if (block instanceof FenceGateBlock) {
			mrot = mrotFenceGate;
		} else if (block instanceof PumpkinBlock || block instanceof TripWireHookBlock) {
			mrot = mrotPumpkin;
		} else if (block instanceof EndPortalFrameBlock || block instanceof DoorBlock) {
			mrot = mrotEndPortalFrame;
		} else if (block instanceof CocoaBlock) {
			mrot = mrotCocoa;
		} else if (block instanceof RedstoneDiodeBlock) {
			mrot = mrotRepeater;
		} else if (block instanceof BedBlock) {
			mrot = mrotBed;
		} else if (block instanceof StairsBlock) {
			mrot = mrotStair;
		} else if (block instanceof AbstractSignBlock) {
			if (block instanceof WallSignBlock) {
				mrot = mrotForgeDirection;
			} else {
				mrot = mrotSign;
			}
		} else if (block instanceof TrapDoorBlock) {
			mrot = mrotTrapDoor;
		} else if (block instanceof LeverBlock) {
			mrot = mrotLever;
		} else if (block instanceof NetherPortalBlock) {
			mrot = mrotNetherPortal;
		} else if (block instanceof VineBlock) {
			mrot = mrotVine;
		} else if (block instanceof AbstractButtonBlock || block instanceof TorchBlock) {
			mrot = mrotButton;
		} else if (block instanceof HugeMushroomBlock) {
			mrot = mrotMushroom;
		} else if (block instanceof FurnaceBlock || block instanceof DispenserBlock || block instanceof HopperBlock
		           || block instanceof ChestBlock || block instanceof EnderChestBlock || block instanceof LadderBlock) {
			mrot = mrotForgeDirection;
		} else if (block instanceof PistonBlock || block instanceof PistonHeadBlock || block instanceof MovingPistonBlock) {
			mrot = mrotPiston;
		} else if (block instanceof LogBlock) {
			mrot = mrotWoodLog;
		} else if (block instanceof SkullBlock) {
			// mrot = mrotNone;
			final byte facing = nbtTileEntity.getByte("Rot");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.putByte("Rot", mrotSign[facing]);
				break;
			case 2:
				nbtTileEntity.putByte("Rot", mrotSign[mrotSign[facing]]);
				break;
			case 3:
				nbtTileEntity.putByte("Rot", mrotSign[mrotSign[mrotSign[facing]]]);
				break;
			default:
				break;
			}
		} else {
			// apply default transformer
			return IBlockTransformer.rotateFirstDirectionProperty(blockState, rotationSteps);
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
		*/
	}
	
	@Nullable
	public BlockPos deploy(final World worldSource, final World worldTarget, final ITransformation transformation) {
		try {
			final CompoundNBT nbtToDeploy = getBlockNBT(worldSource);
			BlockState blockStateNew = blockState;
			if (externals != null) {
				for (final Entry<String, INBT> external : externals.entrySet()) {
					final IBlockTransformer blockTransformer = WarpDriveConfig.blockTransformers.get(external.getKey());
					if (blockTransformer != null) {
						blockStateNew = blockTransformer.rotate(blockState, nbtToDeploy, transformation);
					}
				}
			} else {
				blockStateNew = getRotatedBlockState(nbtToDeploy, transformation.getRotationSteps());
			}
			final BlockPos target = transformation.apply(x, y, z);
			if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
				WarpDrive.logger.info(String.format("Deploying to (%d %d %d) of %s with NBT %s",
				                                    target.getX(), target.getY(), target.getZ(),
				                                    blockStateNew, nbtToDeploy ));
			}
			FastSetBlockState.setBlockStateNoLight(worldTarget, target, blockStateNew, 2);
			
			if (nbtToDeploy != null) {
				nbtToDeploy.putInt("x", target.getX());
				nbtToDeploy.putInt("y", target.getY());
				nbtToDeploy.putInt("z", target.getZ());
				
				TileEntity newTileEntity = null;
				boolean isForgeMultipart = false;
				/* TODO MC1.15 compatibility classes
				if ( WarpDriveConfig.isForgeMultipartLoaded
				  && nbtToDeploy.contains("id")
				  && nbtToDeploy.getString("id").equals("savedMultipart") ) {
					isForgeMultipart = true;
					if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
						WarpDrive.logger.info(String.format("%s deploy: TileEntity is ForgeMultipart",
						                                    this ));
					}
					newTileEntity = (TileEntity) CompatForgeMultipart.methodMultipartHelper_createTileFromNBT.invoke(null, worldTarget, nbtToDeploy);
				}
				*/
				
				if (newTileEntity == null) {
					newTileEntity = TileEntity.create(nbtToDeploy);
					if (newTileEntity == null) {
						WarpDrive.logger.error(String.format("%s deploy failed to create new tile entity %s block %s",
						                                     this, Commons.format(worldTarget, x, y, z), blockStateNew ));
						WarpDrive.logger.error(String.format("NBT data was %s",
						                                     nbtToDeploy ));
					}
				}
				
				if (newTileEntity != null) {
					worldTarget.setTileEntity(target, newTileEntity);
					/* TODO MC1.15 compatibility classes
					if (isForgeMultipart) {
						CompatForgeMultipart.methodTileMultipart_onChunkLoad.invoke(newTileEntity);
						CompatForgeMultipart.methodMultipartHelper_sendDescPacket.invoke(null, worldTarget, newTileEntity);
					}
					*/
					
					// see https://github.com/MinecraftForge/MinecraftForge/issues/5061
					newTileEntity.onLoad();
					
					newTileEntity.markDirty();
				}
			}
			return target;
			
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
			WarpDrive.logger.error(String.format("Deploy failed from (%d %d %d) of %s",
			                                     x, y, z, blockState ));
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
			
			/* TODO MC1.15 enable IC2 support once it's updated
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
			*/
		}
	}
	
	public void read(@Nonnull final CompoundNBT tagCompound) {
		final String stringBlockState;
		if (tagCompound.contains("state")) {
			stringBlockState = tagCompound.getString("state");
			blockState = Commons.readBlockStateFromNBT(tagCompound.get("state"));
		} else {
			final String blockName = tagCompound.getString("block");
			final int blockMeta = tagCompound.getByte("blockMeta");
			stringBlockState = String.format("%s@%d", blockName, blockMeta);
		}
		blockState = WarpDriveDataFixer.getBlockState(stringBlockState);
		if (blockState == null) {
			if (WarpDriveConfig.LOGGING_BUILDING) {
				WarpDrive.logger.warn(String.format("Ignoring unknown blockstate %s from tag %s, consider updating your warpdrive/dataFixer.yml",
				                                    stringBlockState, tagCompound ));
			}
			blockState = Blocks.AIR.getDefaultState();
			return;
		}
		
		hasTileEntity = false;
		weakTileEntity = null;
		if (tagCompound.contains("blockNBT")) {
			blockNBT = tagCompound.getCompound("blockNBT");
			
			// Clear computer IDs
			if (blockNBT.contains("computerID")) {
				blockNBT.remove("computerID");
			}
			if (blockNBT.contains("oc:computer")) {
				final CompoundNBT tagComputer = blockNBT.getCompound("oc:computer");
				tagComputer.remove("components");
				tagComputer.remove("node");
				blockNBT.put("oc:computer", tagComputer);
			}
		} else {
			blockNBT = null;
		}
		x = tagCompound.getInt("x");
		y = tagCompound.getInt("y");
		z = tagCompound.getInt("z");
		if (tagCompound.contains("externals")) {
			final CompoundNBT tagCompoundExternals = tagCompound.getCompound("externals");
			externals = new HashMap<>();
			for (final Object key : tagCompoundExternals.keySet()) {
				assert key instanceof String;
				externals.put((String) key, tagCompoundExternals.get((String) key));
			}
		} else {
			externals = null;
		}
	}
	
	public void write(@Nonnull final World worldSource, @Nonnull final CompoundNBT tagCompound) {
		tagCompound.put("state", Commons.writeBlockStateToNBT(blockState));
		final CompoundNBT nbtTileEntity = getBlockNBT(worldSource);
		if (nbtTileEntity != null) {
			tagCompound.put("blockNBT", nbtTileEntity);
		}
		tagCompound.putInt("x", x);
		tagCompound.putInt("y", y);
		tagCompound.putInt("z", z);
		if (externals != null && !externals.isEmpty()) {
			final CompoundNBT tagCompoundExternals = new CompoundNBT();
			for (final Entry<String, INBT> entry : externals.entrySet()) {
				if (entry.getValue() == null) {
					tagCompoundExternals.putString(entry.getKey(), "");
				} else {
					tagCompoundExternals.put(entry.getKey(), entry.getValue());
				}
			}
			tagCompound.put("externals", tagCompoundExternals);
		}
	}
	
	public void removeUniqueIDs() {
		removeUniqueIDs(blockNBT);
	}
	
	public static void removeUniqueIDs(final CompoundNBT tagCompound) {
		if (tagCompound == null) {
			return;
		}
		
		// ComputerCraft computer
		if (tagCompound.contains("computerID")) {
			tagCompound.remove("computerID");
			tagCompound.remove("label");
		}
		
		// WarpDrive machine signature UUID
		if (tagCompound.contains(ICoreSignature.UUID_MOST_TAG)) {
			tagCompound.remove(ICoreSignature.UUID_MOST_TAG);
			tagCompound.remove(ICoreSignature.UUID_LEAST_TAG);
		}
		
		// WarpDrive any OC connected tile
		if (tagCompound.contains("oc:node")) {
			tagCompound.remove("oc:node");
		}
		
		// WarpDrive ship jump count
		if (tagCompound.contains("jumpCount")) {
			tagCompound.remove("jumpCount");
		}
		
		// OpenComputers case
		if (tagCompound.contains("oc:computer")) {
			final CompoundNBT tagComputer = tagCompound.getCompound("oc:computer");
			tagComputer.remove("chunkX");
			tagComputer.remove("chunkZ");
			tagComputer.remove("components");
			tagComputer.remove("dimension");
			tagComputer.remove("node");
			tagCompound.put("oc:computer", tagComputer);
		}
		
		// OpenComputers case
		if (tagCompound.contains("oc:items")) {
			final ListNBT tagListItems = tagCompound.getList("oc:items", Constants.NBT.TAG_COMPOUND);
			for (int indexItemSlot = 0; indexItemSlot < tagListItems.size(); indexItemSlot++) {
				final CompoundNBT tagCompoundItemSlot = tagListItems.getCompound(indexItemSlot);
				final CompoundNBT tagCompoundItem = tagCompoundItemSlot.getCompound("item");
				final CompoundNBT tagCompoundTag = tagCompoundItem.getCompound("tag");
				final CompoundNBT tagCompoundOCData = tagCompoundTag.getCompound("oc:data");
				final CompoundNBT tagCompoundNode = tagCompoundOCData.getCompound("node");
				if (tagCompoundNode.contains("address")) {
					tagCompoundNode.remove("address");
				}
			}
		}
		
		// OpenComputers keyboard
		if (tagCompound.contains("oc:keyboard")) {
			final CompoundNBT tagCompoundKeyboard = tagCompound.getCompound("oc:keyboard");
			tagCompoundKeyboard.remove("node");
		}
		
		// OpenComputers screen
		if (tagCompound.contains("oc:hasPower")) {
			tagCompound.remove("node");
		}
		
		// Immersive Engineering & Thermal Expansion
		if (tagCompound.contains("Owner")) {
			tagCompound.putString("Owner", "None");
		}
		if (tagCompound.contains("OwnerUUID")) {
			tagCompound.remove("OwnerUUID");
		}
		
		// Mekanism
		if (tagCompound.contains("owner")) {
			tagCompound.putString("owner", "None");
		}
		if (tagCompound.contains("ownerUUID")) {
			tagCompound.remove("ownerUUID");
		}
	}
	
	public static void emptyEnergyStorage(@Nonnull final CompoundNBT tagCompound) {
		// BuildCraft
		if (tagCompound.contains("battery", NBT.TAG_COMPOUND)) {
			final CompoundNBT tagCompoundBattery = tagCompound.getCompound("battery");
			if (tagCompoundBattery.contains("energy", NBT.TAG_INT)) {
				tagCompoundBattery.putInt("energy", 0);
			}
		}
		
		// Gregtech
		if (tagCompound.contains("mStoredEnergy", NBT.TAG_INT)) {
			tagCompound.putInt("mStoredEnergy", 0);
		}
		
		// IC2
		if (tagCompound.contains("energy", NBT.TAG_DOUBLE)) {
			// energy_consume((int)Math.round(blockNBT.getDouble("energy")), true);
			tagCompound.putDouble("energy", 0);
		}
		
		// Immersive Engineering & Thermal Expansion
		if (tagCompound.contains("Energy", NBT.TAG_INT)) {
			// energy_consume(blockNBT.getInt("Energy"), true);
			tagCompound.putInt("Energy", 0);
		}
		
		// Mekanism
		if (tagCompound.contains("electricityStored", NBT.TAG_DOUBLE)) {
			tagCompound.putDouble("electricityStored", 0);
		}
		
		// WarpDrive
		if (tagCompound.contains("energy", NBT.TAG_LONG)) {
			tagCompound.putLong("energy", 0L);
		}
	}
	
	public void fillEnergyStorage() {
		if (blockState.getBlock() instanceof IBlockBase) {
			final EnumTier enumTier = ((IBlockBase) blockState.getBlock()).getTier();
			if (enumTier != EnumTier.CREATIVE) {
				if (blockState.getBlock() instanceof BlockShipCore) {
					blockNBT.putLong(EnergyWrapper.TAG_ENERGY, WarpDriveConfig.SHIP_MAX_ENERGY_STORED_BY_TIER[enumTier.getIndex()]);
				}
				if (blockState.getBlock() instanceof BlockCapacitor) {
					blockNBT.putLong(EnergyWrapper.TAG_ENERGY, WarpDriveConfig.CAPACITOR_MAX_ENERGY_STORED_BY_TIER[enumTier.getIndex()]);
				}
			}
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s @ (%d %d %d) %s %s nbt %s",
		                     getClass().getSimpleName(),
		                     x, y, z,
		                     blockState,
		                     weakTileEntity == null ? null : weakTileEntity.get(),
		                     blockNBT );
	}
}
