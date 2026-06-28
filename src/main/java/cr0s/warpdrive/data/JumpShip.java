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

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.text.ITextComponent;
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
		final NBTTagCompound schematic = Commons.readNBTFromFile(WarpDriveConfig.G_SCHEMATICS_LOCATION + "/" + fileName + ".schematic");
		if (schematic == null) {
			reason.append(Commons.getStyleWarning(), "warpdrive.ship.guide.schematic_not_found",
			              fileName + ".schematic");
			return null;
		}
		
		final JumpShip jumpShip = new JumpShip();
		
		// Compute geometry
		// int shipMass = schematic.getInteger("shipMass");
		// String shipName = schematic.getString("shipName");
		// int shipVolume = schematic.getInteger("shipVolume");
		if (schematic.hasKey("ship")) {
			jumpShip.readFromNBT(schematic.getCompoundTag("ship"));
			
		} else {
			// Read dimensions
			// note: WorldEdit uses shorts. Sponge uses integers.
			final int width = schematic.getInteger("Width");
			final int height = schematic.getInteger("Height");
			final int length = schematic.getInteger("Length");
			if (width <= 0 || height <= 0 || length <= 0) {
				reason.append(new WarpDriveText(Commons.getStyleWarning(), "warpdrive.ship.guide.schematic_invalid_format",
				                                String.format("Invalid size values: expecting strictly positive integers, found %1d %2d %3d.",
				                                              width, height, length) ));
				return null;
			}
			
			// Read core offset & original position
			final boolean isWorldEdit = schematic.hasKey("WEOffsetX");
			final VectorI vCore;
			final VectorI vOrigin;
			if (isWorldEdit) {
				// WEOffset is origin position relative to the core
				vCore = new VectorI(-schematic.getInteger("WEOffsetX"),
				                    -schematic.getInteger("WEOffsetY"),
				                    -schematic.getInteger("WEOffsetZ") );
				
				// WEOrigin is offset on tile entity coordinates (position of the first block in original world)
				vOrigin = new VectorI(schematic.getInteger("WEOriginX"),
				                      schematic.getInteger("WEOriginY"),
				                      schematic.getInteger("WEOriginZ") );
				
			} else if (schematic.hasKey("Offset")){
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
			final HashMap<Integer, IBlockState> blockStatePalette;
			if (schematic.hasKey("Palette")) {
				final NBTTagCompound tagCompoundPalette = schematic.getCompoundTag("Palette");
				blockStatePalette = new HashMap<>(tagCompoundPalette.getKeySet().size());
				for (final String stringBlockstate : tagCompoundPalette.getKeySet()) {
					final IBlockState blockState = WarpDriveDataFixer.getBlockState(stringBlockstate);
					if (blockState != null) {
						blockStatePalette.put(tagCompoundPalette.getInteger(stringBlockstate), blockState);
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
			final byte[] localBlocks = schematic.hasKey("Blocks") ? schematic.getByteArray("Blocks") : schematic.getByteArray("BlockData");
			final byte[] localAddBlocks = schematic.hasKey("AddBlocks") ? schematic.getByteArray("AddBlocks") : null;
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
			final NBTTagCompound[] tileEntities = new NBTTagCompound[jumpShip.jumpBlocks.length];
			final NBTTagList tagListTileEntities = schematic.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND);
			
			for (int index = 0; index < tagListTileEntities.tagCount(); index++) {
				final NBTTagCompound tagCompoundTileEntity = tagListTileEntities.getCompoundTagAt(index);
				final int xTileEntity;
				final int yTileEntity;
				final int zTileEntity;
				if (isWorldEdit) {
					xTileEntity = tagCompoundTileEntity.getInteger("x");
					yTileEntity = tagCompoundTileEntity.getInteger("y");
					zTileEntity = tagCompoundTileEntity.getInteger("z");
				} else if (tagCompoundTileEntity.hasKey("Pos")) {
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
				tagCompoundTileEntity.setInteger("x", vOrigin.x + xTileEntity);
				tagCompoundTileEntity.setInteger("y", vOrigin.y + yTileEntity);
				tagCompoundTileEntity.setInteger("z", vOrigin.z + zTileEntity);
				
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
							jumpBlock.block = Block.getBlockById(blockId);
							jumpBlock.blockMeta = (localMetadata[index]) & 0x0F;
						} else {
							final IBlockState blockState = blockStatePalette.get(blockId);
							if (blockState != null) {
								jumpBlock.block = blockState.getBlock();
								jumpBlock.blockMeta = blockState.getBlock().getMetaFromState(blockState);
							}
						}
						// only add NBT for non-air blocks due to missing blocks
						if (jumpBlock.block != Blocks.AIR) {
							jumpBlock.blockNBT = tileEntities[index];
						} else {
							jumpBlock.blockMeta = 0;
						}
						
						if (jumpBlock.block != null) {
							if (WarpDriveConfig.LOGGING_BUILDING) {
								if (tileEntities[index] == null) {
									WarpDrive.logger.info("Adding block to deploy: "
										                      + jumpBlock.block.getTranslationKey() + ":" + jumpBlock.blockMeta
										                      + " (no tile entity)");
								} else {
									WarpDrive.logger.info("Adding block to deploy: "
										                      + jumpBlock.block.getTranslationKey() + ":" + jumpBlock.blockMeta
										                      + " with tile entity " + tileEntities[index].getString("id"));
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
		
		WarpDrive.logger.info(this + " messageToAllPlayersOnShip: " + textComponent.getUnformattedText());
		for (final MovingEntity movingEntity : entitiesOnShip) {
			final Entity entity = movingEntity.getEntity();
			if (entity instanceof EntityPlayer) {
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
				              Math.round(entity.posX), Math.round(entity.posY), Math.round(entity.posZ) );
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
			  || entity instanceof EntityPlayer ) {
				continue;
			}
			
			// ignore left behind
			if ( Dictionary.isLeftBehind(entity) ) {
				continue;
			}
			WarpDrive.logger.warn(String.format("Removing entity %s: %s",
			                                    Dictionary.getId(entity),
			                                    entity ));
			world.removeEntity(entity);
		}
		
		return true;
	}
	
	public void addPlayerToEntities(final String playerName) {
		if (entitiesOnShip == null) {
			entitiesOnShip = new ArrayList<>();
		}
		final EntityPlayerMP entityPlayerMP = Commons.getOnlinePlayerByName(playerName);
		if (entityPlayerMP == null) {
			WarpDrive.logger.error(String.format("%s Unable to add offline/missing player %s",
			                                     this, playerName ));
			return;
		}
		final MovingEntity movingEntity = new MovingEntity(entityPlayerMP);
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
		final MutableBlockPos mutableBlockPos = new MutableBlockPos();
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
					final IBlockState blockState = world.getBlockState(mutableBlockPos);
					
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
					final IBlockState blockStateInner = world.getBlockState(mutableBlockPos);
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
					              blockState.getBlock().getLocalizedName(),
					              x, y, z);
					reason.append(Commons.getStyleCommand(), "warpdrive.ship.guide.ship_snagged2");
					world.newExplosion(null, x + 0.5D, y + 0.5D, z + 0.5D, 1.0F, false, false);
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
			final MutableBlockPos mutableBlockPos = new MutableBlockPos();
			@SuppressWarnings("unchecked")
			final ArrayList<JumpBlock>[] placeTimeJumpBlocks = new ArrayList[5];
			for (int placeTime = 0; placeTime < placeTimeJumpBlocks.length; placeTime++) {
				placeTimeJumpBlocks[placeTime] = new ArrayList<>();
			}
			
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
								mutableBlockPos.setPos(x, y, z);
								blockPos = mutableBlockPos;
								final IBlockState blockState = world.getBlockState(blockPos);
								final Block block = blockState.getBlock();
								
								// Skipping vanilla air & ignored blocks
								if (block == Blocks.AIR || Dictionary.BLOCKS_LEFTBEHIND.contains(block)) {
									continue;
								}
								actualVolume++;
								
								if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
									WarpDrive.logger.info(String.format("Checking for save from (%d %d %d) of %s (%d)",
								                                    x, y, z, blockState, blockState.getBlock().getMetaFromState(blockState)));
								}
								
								if (!Dictionary.BLOCKS_NOMASS.contains(block)) {
									newMass++;
								}
								
								// Stop on non-movable blocks
								if (Dictionary.BLOCKS_ANCHOR.contains(block)) {
									reason.append(Commons.getStyleWarning(), "warpdrive.ship.guide.anchor_block_detected",
									              block.getLocalizedName(),
									              x, y, z);
									return false;
								}
								
								final TileEntity tileEntity = world.getTileEntity(blockPos);
								final JumpBlock jumpBlock = new JumpBlock(world, blockPos, blockState, tileEntity);
								
								if (tileEntity != null && jumpBlock.externals != null) {
									for (final Entry<String, NBTBase> external : jumpBlock.externals.entrySet()) {
										final IBlockTransformer blockTransformer = WarpDriveConfig.blockTransformers.get(external.getKey());
										if (blockTransformer != null) {
											if (!blockTransformer.isJumpReady(jumpBlock.block, jumpBlock.blockMeta, tileEntity, reason)) {
												reason.append(Commons.getStyleWarning(), "warpdrive.ship.guide.block_not_ready_for_jump",
												              jumpBlock.block.getLocalizedName(),
												              jumpBlock.x, jumpBlock.y, jumpBlock.z);
												return false;
											}
										}
									}
								}
								
								// default priority is 2 for block, 3 for tile entities
								Integer placeTime = Dictionary.BLOCKS_PLACE.get(block);
								if (placeTime == null) {
									if (tileEntity == null) {
										placeTime = 2;
									} else {
										placeTime = 3;
									}
								}
								
								placeTimeJumpBlocks[placeTime].add(jumpBlock);
							}
						}
					}
				}
			}
			
			jumpBlocks = new JumpBlock[actualVolume];
			int indexShip = 0;
			for (int placeTime = 0; placeTime < 5; placeTime++) {
				for (final JumpBlock jumpBlock : placeTimeJumpBlocks[placeTime]) {
					jumpBlocks[indexShip] = jumpBlock;
					indexShip++;
				}
			}
			actualMass = newMass;
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
			final WarpDriveText textComponent = new WarpDriveText(Commons.getStyleWarning(), "warpdrive.ship.guide.save_exception",
			                                                      Commons.format(world, blockPos));
			WarpDrive.logger.error(textComponent.getUnformattedText());
			reason.appendSibling(textComponent);
			return false;
		}
		
		if (WarpDriveConfig.LOGGING_JUMP) {
			WarpDrive.logger.info(String.format("%s Ship saved as %d blocks",
			                                    this, jumpBlocks.length));
		}
		return true;
	}
	
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		core = new BlockPos(tagCompound.getInteger("coreX"), tagCompound.getInteger("coreY"), tagCompound.getInteger("coreZ"));
		dx = tagCompound.getInteger("dx");
		dz = tagCompound.getInteger("dz");
		maxX = tagCompound.getInteger("maxX");
		maxZ = tagCompound.getInteger("maxZ");
		maxY = tagCompound.getInteger("maxY");
		minX = tagCompound.getInteger("minX");
		minZ = tagCompound.getInteger("minZ");
		minY = tagCompound.getInteger("minY");
		actualMass = tagCompound.getInteger("actualMass");
		final NBTTagList tagList = tagCompound.getTagList("jumpBlocks", Constants.NBT.TAG_COMPOUND);
		jumpBlocks = new JumpBlock[tagList.tagCount()];
		for (int index = 0; index < tagList.tagCount(); index++) {
			jumpBlocks[index] = new JumpBlock();
			jumpBlocks[index].readFromNBT(tagList.getCompoundTagAt(index));
		}
	}
	
	public void writeToNBT(@Nonnull final NBTTagCompound tagCompound) {
		tagCompound.setInteger("coreX", core.getX());
		tagCompound.setInteger("coreY", core.getY());
		tagCompound.setInteger("coreZ", core.getZ());
		tagCompound.setInteger("dx", dx);
		tagCompound.setInteger("dz", dz);
		tagCompound.setInteger("maxX", maxX);
		tagCompound.setInteger("maxZ", maxZ);
		tagCompound.setInteger("maxY", maxY);
		tagCompound.setInteger("minX", minX);
		tagCompound.setInteger("minZ", minZ);
		tagCompound.setInteger("minY", minY);
		tagCompound.setInteger("actualMass", actualMass);
		final NBTTagList tagListJumpBlocks = new NBTTagList();
		for (final JumpBlock jumpBlock : jumpBlocks) {
			final NBTTagCompound tagCompoundBlock = new NBTTagCompound();
			jumpBlock.writeToNBT(world, tagCompoundBlock);
			tagListJumpBlocks.appendTag(tagCompoundBlock);
		}
		tagCompound.setTag("jumpBlocks", tagListJumpBlocks);
	}
}
