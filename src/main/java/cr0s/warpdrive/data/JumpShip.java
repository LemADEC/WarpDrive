package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.block.movement.TileEntityShipCore;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.config.WarpDriveDataFixer;
import cr0s.warpdrive.network.PacketHandler;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.mojang.datafixers.Dynamic;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.datafix.fixes.BlockStateFlatteningMap;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.Explosion.Mode;
import net.minecraft.world.World;

import net.minecraftforge.common.util.Constants;

public class JumpShip {
	
	public World world;
	public BlockPos core;
	public int dx;
	public int dz;
	public int maxX;
	public int maxZ;
	public int maxY;
	public int minX;
	public int minZ;
	public int minY;
	public JumpBlock[] jumpBlocks;
	public int actualMass;
	public TileEntityShipCore shipCore;
	public List<MovingEntity> entitiesOnShip;
	
	public JumpShip() {
	}
	
	public static JumpShip createFromFile(final String fileName, final WarpDriveText reason) {
		final CompoundNBT schematic = Commons.readNBTFromFile(WarpDriveConfig.G_SCHEMATICS_LOCATION + "/" + fileName + ".schematic");
		if (schematic == null) {
			reason.append(Commons.getStyleWarning(), "warpdrive.ship.guide.schematic_not_found",
			              fileName + ".schematic");
			return null;
		}
		
		final JumpShip jumpShip = new JumpShip();
		
		// Compute geometry
		// int shipMass = schematic.getInt("shipMass");
		// String shipName = schematic.getString("shipName");
		// int shipVolume = schematic.getInt("shipVolume");
		if (schematic.contains("ship")) {
			jumpShip.read(schematic.getCompound("ship"));
			
		} else {
			// Read dimensions
			// note: WorldEdit uses shorts. Sponge uses integers.
			final int width = schematic.getInt("Width");
			final int height = schematic.getInt("Height");
			final int length = schematic.getInt("Length");
			if (width <= 0 || height <= 0 || length <= 0) {
				reason.append(new WarpDriveText(Commons.getStyleWarning(), "warpdrive.ship.guide.schematic_invalid_format",
				                                String.format("Invalid size values: expecting strictly positive integers, found %1d %2d %3d.",
				                                              width, height, length) ));
				return null;
			}
			
			// Read core offset & original position
			final boolean isWorldEdit = schematic.contains("WEOffsetX");
			final VectorI vCore;
			final VectorI vOrigin;
			if (isWorldEdit) {
				// WEOffset is origin position relative to the core
				vCore = new VectorI(-schematic.getInt("WEOffsetX"),
				                    -schematic.getInt("WEOffsetY"),
				                    -schematic.getInt("WEOffsetZ") );
				
				// WEOrigin is offset on tile entity coordinates (position of the first block in original world)
				vOrigin = new VectorI(schematic.getInt("WEOriginX"),
				                      schematic.getInt("WEOriginY"),
				                      schematic.getInt("WEOriginZ") );
				
			} else if (schematic.contains("Offset")){
				// Offset is core position relative to origin
				final int[] intOffset = schematic.getIntArray("Offset");
				if (intOffset.length != 3) {
					reason.append(new WarpDriveText(Commons.getStyleWarning(), "warpdrive.ship.guide.schematic_invalid_format",
					                                String.format("Invalid offset format: expecting 3 integers, found %1d",
					                                              intOffset.length) ));
					return null;
				}
				vCore = new VectorI(intOffset[0],
				                    intOffset[1],
				                    intOffset[2] );
				
				// Origin is unknown with Sponge, defaulting to 0
				vOrigin = new VectorI(0, 0, 0 );
				
			} else {
				reason.append(new WarpDriveText(Commons.getStyleWarning(), "warpdrive.ship.guide.schematic_invalid_format",
				                                "Unknown offset format"));
				return null;
			}
			
			// Read registry name's palette if defined (as introduced by MC1.13)
			final HashMap<Integer, BlockState> blockStatePalette;
			if (schematic.contains("Palette")) {
				final CompoundNBT tagCompoundPalette = schematic.getCompound("Palette");
				blockStatePalette = new HashMap<>(tagCompoundPalette.keySet().size());
				for (final String stringBlockstate : tagCompoundPalette.keySet()) {
					final BlockState blockState = WarpDriveDataFixer.getBlockState(stringBlockstate);
					if (blockState != null) {
						blockStatePalette.put(tagCompoundPalette.getInt(stringBlockstate), blockState);
					} else {
						WarpDrive.logger.warn(String.format("Ignoring missing BlockState %s, consider updating your warpdrive/dataFixer.yml",
						                                    stringBlockstate));
					}
				}
			} else {
				blockStatePalette = null;
			}
			
			// Compute ship properties
			jumpShip.core = new BlockPos(vOrigin.x + vCore.x,
			                             vOrigin.y + vCore.y,
			                             vOrigin.z + vCore.z );
			jumpShip.minX = vOrigin.x;
			jumpShip.maxX = vOrigin.x + width - 1;
			jumpShip.minY = vOrigin.y;
			jumpShip.maxY = vOrigin.y + height - 1;
			jumpShip.minZ = vOrigin.z;
			jumpShip.maxZ = vOrigin.z + length - 1;
			jumpShip.jumpBlocks = new JumpBlock[width * height * length];
			
			// Read blocks from NBT to internal storage array
			// Before 1.13, WorldEdit uses Blocks for LSB, an optional AddBlocks for MSB.
			// From 1.13, WorldEdit uses ???.
			// Sponge uses BlockData with either a Palette or some custom encoding
			final byte[] localBlocks = schematic.contains("Blocks") ? schematic.getByteArray("Blocks") : schematic.getByteArray("BlockData");
			final byte[] localAddBlocks = schematic.contains("AddBlocks") ? schematic.getByteArray("AddBlocks") : null;
			final byte[] localMetadata = blockStatePalette == null ? schematic.getByteArray("Data") : null;
			if (localBlocks.length != jumpShip.jumpBlocks.length) {
				reason.append(new WarpDriveText(Commons.getStyleWarning(), "warpdrive.ship.guide.schematic_invalid_format",
				                                String.format("Invalid array size for Blocks: expecting %d (%d x %d x %d), found %d",
				                                              width, height, length,
				                                              jumpShip.jumpBlocks.length,
				                                              localBlocks.length ) ));
				return null;
			}
			final int sizeAddBlocks = (int) Math.ceil((jumpShip.jumpBlocks.length + 1.0F) / 2);
			if (localAddBlocks != null && localAddBlocks.length != sizeAddBlocks) {
				reason.append(new WarpDriveText(Commons.getStyleWarning(), "warpdrive.ship.guide.schematic_invalid_format",
				                                String.format("Invalid array size for AddBlocks: expecting %d (%d x %d x %d), found %d",
				                                              width, height, length,
				                                              jumpShip.jumpBlocks.length,
				                                              localAddBlocks.length ) ));
				return null;
			}
			if (localMetadata != null && localMetadata.length != jumpShip.jumpBlocks.length) {
				reason.append(new WarpDriveText(Commons.getStyleWarning(), "warpdrive.ship.guide.schematic_invalid_format",
				                                String.format("Invalid array size for Metadata: expecting %d (%d x %d x %d), found %d",
				                                              width, height, length,
				                                              jumpShip.jumpBlocks.length,
				                                              localMetadata.length ) ));
				return null;
			}
			
			// Read Tile Entities
			final CompoundNBT[] tileEntities = new CompoundNBT[jumpShip.jumpBlocks.length];
			final ListNBT tagListTileEntities = schematic.getList("TileEntities", Constants.NBT.TAG_COMPOUND);
			
			for (int index = 0; index < tagListTileEntities.size(); index++) {
				final CompoundNBT tagCompoundTileEntity = tagListTileEntities.getCompound(index);
				final int xTileEntity;
				final int yTileEntity;
				final int zTileEntity;
				if (isWorldEdit) {
					xTileEntity = tagCompoundTileEntity.getInt("x");
					yTileEntity = tagCompoundTileEntity.getInt("y");
					zTileEntity = tagCompoundTileEntity.getInt("z");
				} else if (tagCompoundTileEntity.contains("Pos")) {
					final int[] intPosition = tagCompoundTileEntity.getIntArray("Pos");
					if (intPosition.length != 3) {
						reason.append(new WarpDriveText(Commons.getStyleWarning(), "warpdrive.ship.guide.schematic_invalid_format",
						                                String.format("Invalid array size for TileEntity Pos: expecting 3, found %d in %s",
						                                              intPosition.length, tagCompoundTileEntity ) ));
						return null;
					}
					xTileEntity = intPosition[0];
					yTileEntity = intPosition[1];
					zTileEntity = intPosition[2];
				} else {
					reason.append(new WarpDriveText(Commons.getStyleWarning(), "warpdrive.ship.guide.schematic_invalid_format",
					                                String.format("Missing position for TileEntity %s",
					                                              tagCompoundTileEntity ) ));
					return null;
				}
				tagCompoundTileEntity.putInt("x", vOrigin.x + xTileEntity);
				tagCompoundTileEntity.putInt("y", vOrigin.y + yTileEntity);
				tagCompoundTileEntity.putInt("z", vOrigin.z + zTileEntity);
				
				tileEntities[xTileEntity + (yTileEntity * length + zTileEntity) * width] = tagCompoundTileEntity;
			}
			
			// Create list of blocks to deploy
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					for (int z = 0; z < length; z++) {
						final int index = x + (y * length + z) * width;
						JumpBlock jumpBlock = new JumpBlock();
						
						jumpBlock.x = vOrigin.x + x;
						jumpBlock.y = vOrigin.y + y;
						jumpBlock.z = vOrigin.z + z;
						
						// rebuild block id from signed byte + nibble tables
						int blockId = localBlocks[index];
						if (blockId < 0) {
							blockId += 256;
						}
						if (localAddBlocks != null) {
							int MSB = localAddBlocks[index / 2];
							if (MSB < 0) {
								MSB += 256;
							}
							if (index % 2 == 0) {
								blockId += (MSB & 0x0F) << 8;
							} else {
								blockId += (MSB & 0xF0) << 4;
							}
						}
						
						if (blockStatePalette == null) {
							// Legacy pre-1.13 numeric block ids (WorldEdit "Alpha" format): convert with vanilla's flattening
							// map. It's indexed by (id << 4 | metadata) and returns the fixed { Name, Properties } NBT, which
							// NBTUtil parses into a BlockState. Covers vanilla ids 0-255; modded ids (>255 via AddBlocks) fall
							// out of the 4096-entry table and resolve to air (see the removed-mod-block placeholder backlog item).
							final int metadata = localMetadata != null && index < localMetadata.length ? (localMetadata[index] & 0x0F) : 0;
							final Dynamic<?> dynamicFixed = BlockStateFlatteningMap.getFixedNBTForID((blockId << 4) | metadata);
							jumpBlock.blockState = NBTUtil.readBlockState((CompoundNBT) dynamicFixed.getValue());
						} else {
							final BlockState blockState = blockStatePalette.get(blockId);
							if (blockState != null) {
								jumpBlock.blockState = blockState;
							}
						}
						// only add NBT for non-air blocks due to missing blocks
						if (jumpBlock.blockState.getBlock() != Blocks.AIR) {
							jumpBlock.blockNBT = tileEntities[index];
						}
						
						if (jumpBlock.blockState != null) {
							if (WarpDriveConfig.LOGGING_BUILDING) {
								if (tileEntities[index] == null) {
									WarpDrive.logger.info(String.format("Adding block to deploy: %s (no tile entity)",
									                                    jumpBlock.blockState ));
								} else {
									WarpDrive.logger.info(String.format("Adding block to deploy: %s with tile entity %s",
									                                    jumpBlock.blockState, tileEntities[index].getString("id") ));
								}
							}
						} else {
							jumpBlock = null;
						}
						jumpShip.jumpBlocks[index] = jumpBlock;
					}
				}
			}
		}
		return jumpShip;
	}
	
	public void messageToAllPlayersOnShip(final WarpDriveText textComponent) {
		final String name = (shipCore != null && !shipCore.name.isEmpty()) ? shipCore.name : "ShipCore";
		final ITextComponent messageFormatted = Commons.getNamedPrefix(name)
		                                               .appendSibling(textComponent);
		if (entitiesOnShip == null) {
			// entities not saved yet, get them now
			final WarpDriveText reason = new WarpDriveText();
			saveEntities(reason);
		}
		
		WarpDrive.logger.info(this + " messageToAllPlayersOnShip: " + textComponent.getString());
		for (final MovingEntity movingEntity : entitiesOnShip) {
			final Entity entity = movingEntity.getEntity();
			if (entity instanceof PlayerEntity) {
				Commons.addChatMessage(entity, messageFormatted);
			}
		}
	}
	
	public boolean saveEntities(final WarpDriveText reason) {
		boolean isSuccess = true;
		entitiesOnShip = new ArrayList<>();
		
		if (world == null) {
			WarpDrive.logger.error("Invalid call to saveEntities, please report it to mod author: world is null");
			reason.append(Commons.getStyleWarning(), "warpdrive.error.internal_check_console");
			return false;
		}
		
		final AxisAlignedBB axisalignedbb = new AxisAlignedBB(minX, minY, minZ, maxX + 0.99D, maxY + 0.99D, maxZ + 0.99D);
		
		final List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(null, axisalignedbb);
		
		for (final Entity entity : list) {
			if (entity == null) {
				continue;
			}
			
			if (Dictionary.isAnchor(entity)) {
				reason.append(Commons.getStyleWarning(), "warpdrive.ship.guide.anchor_entity_detected",
				              Dictionary.getId(entity),
				              Math.round(entity.getPosX()), Math.round(entity.getPosY()), Math.round(entity.getPosZ()) );
				isSuccess = false;
				// we need to continue so players are added so they can see the message...
				continue;
			}
			if (Dictionary.isLeftBehind(entity)) {
				if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
					WarpDrive.logger.info(String.format("Leaving entity %s behind: %s",
					                                    Dictionary.getId(entity),
					                                    entity ));
				}
				continue;
			}
			if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
				WarpDrive.logger.info(String.format("Adding entity %s: %s",
				                                    Dictionary.getId(entity),
				                                    entity ));
			}
			final MovingEntity movingEntity = new MovingEntity(entity);
			entitiesOnShip.add(movingEntity);
		}
		
		return isSuccess;
	}
	
	public boolean removeEntities(final WarpDriveText reason) {
		if (world == null) {
			WarpDrive.logger.error("Invalid call to removeEntities, please report it to mod author: world is null");
			reason.append(Commons.getStyleWarning(), "warpdrive.error.internal_check_console");
			return false;
		}
		
		final AxisAlignedBB axisalignedbb = new AxisAlignedBB(minX, minY, minZ, maxX + 0.99D, maxY + 0.99D, maxZ + 0.99D);
		
		final List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(null, axisalignedbb);
		
		for (final Entity entity : list) {
			if ( entity == null
			  || entity instanceof PlayerEntity) {
				continue;
			}
			
			// ignore left behind
			if ( Dictionary.isLeftBehind(entity) ) {
				continue;
			}
			WarpDrive.logger.warn(String.format("Removing entity %s: %s",
			                                    Dictionary.getId(entity),
			                                    entity ));
			entity.remove();
		}
		
		return true;
	}
	
	public void addPlayerToEntities(final String playerName) {
		if (entitiesOnShip == null) {
			entitiesOnShip = new ArrayList<>();
		}
		final ServerPlayerEntity entityServerPlayer = Commons.getOnlinePlayerByName(playerName);
		if (entityServerPlayer == null) {
			WarpDrive.logger.error(String.format("%s Unable to add offline/missing player %s",
			                                     this, playerName ));
			return;
		}
		final MovingEntity movingEntity = new MovingEntity(entityServerPlayer);
		entitiesOnShip.add(movingEntity);
	}
	
	public boolean isUnlimited() {
		if (entitiesOnShip == null) {
			return false;
		}
		for (final MovingEntity movingEntity : entitiesOnShip) {
			if (movingEntity.isUnlimited()) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String toString() {
		return String.format("%s/%d '%s' %s",
		                     getClass().getSimpleName(), hashCode(),
		                     shipCore == null ? "~NULL~" : (shipCore.uuid + ":" + shipCore.name),
			                 Commons.format(world, core));
	}
	
	public boolean checkBorders(final WarpDriveText reason) {
		final BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable();
		// Abort jump if blocks with TE are connecting to the ship (avoid crash when splitting multi-blocks)
		for (int x = minX - 1; x <= maxX + 1; x++) {
			final boolean xBorder = (x == minX - 1) || (x == maxX + 1);
			for (int z = minZ - 1; z <= maxZ + 1; z++) {
				final boolean zBorder = (z == minZ - 1) || (z == maxZ + 1);
				
				// skip the corners
				if (xBorder && zBorder) {
					continue;
				}
				
				for (int y = minY - 1; y <= maxY + 1; y++) {
					final boolean yBorder = (y == minY - 1) || (y == maxY + 1);
					if ((y < 0) || (y > 255)) {
						continue;
					}
					if (!(xBorder || yBorder || zBorder)) {
						continue;
					}
					
					// skip the corners
					if (yBorder && (xBorder || zBorder)) {
						continue;
					}
					
					mutableBlockPos.setPos(x, y, z);
					final BlockState blockState = world.getBlockState(mutableBlockPos);
					
					final Block block = blockState.getBlock();
					
					// Skipping any air block & ignored blocks
					if ( world.isAirBlock(mutableBlockPos)
					  || Dictionary.BLOCKS_LEFTBEHIND.contains(block) ) {
						continue;
					}
					
					// Skipping non-movable blocks
					if (Dictionary.BLOCKS_ANCHOR.contains(block)) {
						continue;
					}
					
					// Skipping blocks without tile entities
					if (!block.hasTileEntity(blockState)) {
						continue;
					}
					
					
					// Check inner block
					mutableBlockPos.setPos(
							x == minX - 1 ? minX : x == maxX + 1 ? maxX : x,
							y == minY - 1 ? minY : y == maxY + 1 ? maxY : y,
							z == minZ - 1 ? minZ : z == maxZ + 1 ? maxZ : z );
					final BlockState blockStateInner = world.getBlockState(mutableBlockPos);
					final Block blockInner = blockStateInner.getBlock();
					
					// Skipping any air block & ignored blocks
					if ( world.isAirBlock(mutableBlockPos)
					  || Dictionary.BLOCKS_LEFTBEHIND.contains(blockInner) ) {
						continue;
					}
					
					// Skipping blocks without tile entities
					if (!blockInner.hasTileEntity(blockStateInner)) {
						continue;
					}
					
					reason.append(Commons.getStyleWarning(), "warpdrive.ship.guide.ship_snagged1",
					              blockState.getBlock().getNameTextComponent(),
					              x, y, z);
					reason.append(Commons.getStyleCommand(), "warpdrive.ship.guide.ship_snagged2");
					world.createExplosion(null, x + 0.5D, y + 0.5D, z + 0.5D, 1.0F, false, Mode.NONE);
					PacketHandler.sendSpawnParticlePacket(world, "jammed", (byte) 5,
					                                      new Vector3(x + 0.5D, y + 0.5D, z + 0.5D),
					                                      new Vector3(0.0D, 0.0D, 0.0D),
					                                      1.0F, 1.0F, 1.0F,
					                                      1.0F, 1.0F, 1.0F,
					                                      32);
					return false;
				}
			}
		}
		
		return true;
	}

	/**
	 * Saving ship to memory
	 */
	public boolean save(final WarpDriveText reason) {
		BlockPos blockPos = new BlockPos(0, -1, 0);
		try {
			final int estimatedVolume = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
			final JumpBlock[][] placeTimeJumpBlocks = { new JumpBlock[estimatedVolume], new JumpBlock[estimatedVolume], new JumpBlock[estimatedVolume], new JumpBlock[estimatedVolume], new JumpBlock[estimatedVolume] };
			final int[] placeTimeIndexes = { 0, 0, 0, 0, 0 };
			
			int actualVolume = 0;
			int newMass = 0;
			final int xc1 = minX >> 4;
			final int xc2 = maxX >> 4;
			final int zc1 = minZ >> 4;
			final int zc2 = maxZ >> 4;
			
			for (int xc = xc1; xc <= xc2; xc++) {
				final int x1 = Math.max(minX, xc << 4);
				final int x2 = Math.min(maxX, (xc << 4) + 15);
				
				for (int zc = zc1; zc <= zc2; zc++) {
					final int z1 = Math.max(minZ, zc << 4);
					final int z2 = Math.min(maxZ, (zc << 4) + 15);
					
					for (int y = minY; y <= maxY; y++) {
						for (int x = x1; x <= x2; x++) {
							for (int z = z1; z <= z2; z++) {
								blockPos = new BlockPos(x, y, z);
								final BlockState blockState = world.getBlockState(blockPos);
								
								// Skipping vanilla air & ignored blocks
								if (blockState.getBlock() == Blocks.AIR || Dictionary.BLOCKS_LEFTBEHIND.contains(blockState.getBlock())) {
									continue;
								}
								actualVolume++;
								
								if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
									WarpDrive.logger.info(String.format("Checking for save from (%d %d %d) of %s",
									                                    x, y, z, blockState ));
								}
								
								if (!Dictionary.BLOCKS_NOMASS.contains(blockState.getBlock())) {
									newMass++;
								}
								
								// Stop on non-movable blocks
								if (Dictionary.BLOCKS_ANCHOR.contains(blockState.getBlock())) {
									reason.append(Commons.getStyleWarning(), "warpdrive.ship.guide.anchor_block_detected",
									              blockState.getBlock().getNameTextComponent(),
									              x, y, z);
									return false;
								}
								
								final TileEntity tileEntity = world.getTileEntity(blockPos);
								final JumpBlock jumpBlock = new JumpBlock(world, blockPos, blockState, tileEntity);
								
								if (tileEntity != null && jumpBlock.externals != null) {
									for (final Entry<String, INBT> external : jumpBlock.externals.entrySet()) {
										final IBlockTransformer blockTransformer = WarpDriveConfig.blockTransformers.get(external.getKey());
										if (blockTransformer != null) {
											if (!blockTransformer.isJumpReady(jumpBlock.blockState, tileEntity, reason)) {
												reason.append(Commons.getStyleWarning(), "warpdrive.ship.guide.block_not_ready_for_jump",
												              jumpBlock.blockState.getBlock().getNameTextComponent(),
												              jumpBlock.x, jumpBlock.y, jumpBlock.z);
												return false;
											}
										}
									}
								}
								
								// default priority is 2 for block, 3 for tile entities
								Integer placeTime = Dictionary.BLOCKS_PLACE.get(blockState.getBlock());
								if (placeTime == null) {
									if (tileEntity == null) {
										placeTime = 2;
									} else {
										placeTime = 3;
									}
								}
								
								placeTimeJumpBlocks[placeTime][placeTimeIndexes[placeTime]] = jumpBlock;
								placeTimeIndexes[placeTime]++;
							}
						}
					}
				}
			}
			
			jumpBlocks = new JumpBlock[actualVolume];
			int indexShip = 0;
			for (int placeTime = 0; placeTime < 5; placeTime++) {
				for (int placeTimeIndex = 0; placeTimeIndex < placeTimeIndexes[placeTime]; placeTimeIndex++) {
					jumpBlocks[indexShip] = placeTimeJumpBlocks[placeTime][placeTimeIndex];
					indexShip++;
				}
			}
			actualMass = newMass;
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
			final WarpDriveText textComponent = new WarpDriveText(Commons.getStyleWarning(), "warpdrive.ship.guide.save_exception",
			                                                      Commons.format(world, blockPos));
			WarpDrive.logger.error(textComponent.getString());
			reason.appendSibling(textComponent);
			return false;
		}
		
		if (WarpDriveConfig.LOGGING_JUMP) {
			WarpDrive.logger.info(String.format("%s Ship saved as %d blocks",
			                                    this, jumpBlocks.length));
		}
		return true;
	}
	
	public void read(@Nonnull final CompoundNBT tagCompound) {
		core = new BlockPos(tagCompound.getInt("coreX"), tagCompound.getInt("coreY"), tagCompound.getInt("coreZ"));
		dx = tagCompound.getInt("dx");
		dz = tagCompound.getInt("dz");
		maxX = tagCompound.getInt("maxX");
		maxZ = tagCompound.getInt("maxZ");
		maxY = tagCompound.getInt("maxY");
		minX = tagCompound.getInt("minX");
		minZ = tagCompound.getInt("minZ");
		minY = tagCompound.getInt("minY");
		actualMass = tagCompound.getInt("actualMass");
		final ListNBT tagList = tagCompound.getList("jumpBlocks", Constants.NBT.TAG_COMPOUND);
		jumpBlocks = new JumpBlock[tagList.size()];
		for (int index = 0; index < tagList.size(); index++) {
			jumpBlocks[index] = new JumpBlock();
			jumpBlocks[index].read(tagList.getCompound(index));
		}
	}
	
	public void write(@Nonnull final CompoundNBT tagCompound) {
		tagCompound.putInt("coreX", core.getX());
		tagCompound.putInt("coreY", core.getY());
		tagCompound.putInt("coreZ", core.getZ());
		tagCompound.putInt("dx", dx);
		tagCompound.putInt("dz", dz);
		tagCompound.putInt("maxX", maxX);
		tagCompound.putInt("maxZ", maxZ);
		tagCompound.putInt("maxY", maxY);
		tagCompound.putInt("minX", minX);
		tagCompound.putInt("minZ", minZ);
		tagCompound.putInt("minY", minY);
		tagCompound.putInt("actualMass", actualMass);
		final ListNBT tagListJumpBlocks = new ListNBT();
		for (final JumpBlock jumpBlock : jumpBlocks) {
			final CompoundNBT tagCompoundBlock = new CompoundNBT();
			jumpBlock.write(world, tagCompoundBlock);
			tagListJumpBlocks.add(tagCompoundBlock);
		}
		tagCompound.put("jumpBlocks", tagListJumpBlocks);
	}
}