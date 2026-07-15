package cr0s.warpdrive.world;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.Filler;
import cr0s.warpdrive.config.GenericSet;
import cr0s.warpdrive.config.Loot;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.config.structures.Schematic.Insertion;
import cr0s.warpdrive.data.InventoryWrapper;
import cr0s.warpdrive.data.JumpBlock;
import cr0s.warpdrive.data.JumpShip;
import cr0s.warpdrive.data.Transformation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion.Mode;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

public class WorldGenStructure {
	
	private final boolean corrupted;
	private final Random rand;
	private final Filler fillerHullPlain;
	private final Filler fillerHullGlass;
	private final Filler fillerSolarPanel;
	private final Filler fillerWiring;
	private final Filler fillerPropulsion;
	private final Filler fillerFullBlockLight;
	private final Filler fillerWallLight;
	private final Filler fillerComputerCore;
	private final Filler fillerComputerScreen;
	private final Filler fillerComputerKeyboard;
	private final Filler fillerComputerFloppy;
	
	public WorldGenStructure(final boolean corrupted, final Random rand) {
		super();
		
		this.corrupted = corrupted;
		this.rand = rand;
		
		// hull plain and glass are linked by same name
		final GenericSet<Filler> fillerSetHull_plain = WarpDriveConfig.FillerManager.getRandomSetFromGroup(rand, "hull_plain");
		if (fillerSetHull_plain == null) {
			WarpDrive.logger.warn(String.format("No FillerSet found within group %s during world generation: check your configuration",
			                                    "hull_plain"));
			fillerHullPlain = new Filler();
			fillerHullPlain.blockState = Blocks.STONE.getDefaultState();
			fillerHullGlass = new Filler();
			fillerHullGlass.blockState = Blocks.GLASS.getDefaultState();
		} else {
			fillerHullPlain = fillerSetHull_plain.getRandomUnit(rand);
			fillerHullGlass = getGenericSetWithDefault(rand, "hull_glass:" + fillerSetHull_plain.getName(), Blocks.GLASS.getDefaultState());
		}
		
		// solarPanel and wiring are linked by same name
		final GenericSet<Filler> fillerSetSolarPanel = WarpDriveConfig.FillerManager.getRandomSetFromGroup(rand, "ship_solarPanel");
		if (fillerSetSolarPanel == null) {
			WarpDrive.logger.warn(String.format("No FillerSet found within group %s during world generation: check your configuration",
			                                    "ship_solarPanel"));
			fillerSolarPanel = new Filler();
			fillerSolarPanel.blockState = Blocks.SANDSTONE.getDefaultState();
			fillerWiring = new Filler();
			fillerWiring.blockState = Blocks.OAK_FENCE.getDefaultState();
		} else {
			fillerSolarPanel = fillerSetSolarPanel.getRandomUnit(rand);
			fillerWiring = getGenericSetWithDefault(rand, "ship_wiring:" + fillerSetSolarPanel.getName(), Blocks.OAK_FENCE.getDefaultState());
		}
		
		// Computer core and screen are linked by same name
		final String nameFillerComputer = "ship_computerCore";
		final GenericSet<Filler> fillerSetComputerCore = WarpDriveConfig.FillerManager.getRandomSetFromGroup(rand, nameFillerComputer);
		if (fillerSetComputerCore == null) {
			WarpDrive.logger.warn(String.format("No FillerSet found within group %s during world generation: check your configuration",
			                                    nameFillerComputer));
			fillerComputerCore = new Filler();
			fillerComputerCore.blockState = Blocks.GOLD_BLOCK.getDefaultState();
			fillerComputerScreen = new Filler();
			fillerComputerScreen.blockState = Blocks.GLASS_PANE.getDefaultState();
			fillerComputerKeyboard = new Filler();
			fillerComputerKeyboard.blockState = Blocks.OAK_SIGN.getDefaultState();
			fillerComputerFloppy = new Filler();
			fillerComputerFloppy.blockState = Blocks.OAK_SIGN.getDefaultState();
		} else {
			fillerComputerCore = fillerSetComputerCore.getRandomUnit(rand);
			fillerComputerScreen = getGenericSetWithDefault(rand, "ship_computerScreen:" + fillerSetComputerCore.getName(), Blocks.GLASS_PANE.getDefaultState());
			fillerComputerKeyboard = getGenericSetWithDefault(rand, "ship_computerKeyboard:" + fillerSetComputerCore.getName(), Blocks.OAK_SIGN.getDefaultState());
			fillerComputerFloppy = getGenericSetWithDefault(rand, "ship_computerFloppy:" + fillerSetComputerCore.getName(), Blocks.OAK_SIGN.getDefaultState());
		}
		
		// propulsion is on it's own
		final GenericSet<Filler> fillerSetPropulsion = WarpDriveConfig.FillerManager.getRandomSetFromGroup(rand, "ship_propulsion");
		if (fillerSetPropulsion == null) {
			WarpDrive.logger.warn(String.format("No FillerSet found within group %s during world generation: check your configuration",
			                                    "ship_propulsion"));
			fillerPropulsion = new Filler();
			fillerPropulsion.blockState = Blocks.OAK_LOG.getDefaultState();
		} else {
			fillerPropulsion = fillerSetPropulsion.getRandomUnit(rand);
		}
		
		// full block light is on it's own
		final GenericSet<Filler> fillerSetFullBlockLight = WarpDriveConfig.FillerManager.getRandomSetFromGroup(rand, "ship_full_block_light");
		if (fillerSetFullBlockLight == null) {
			WarpDrive.logger.warn(String.format("No FillerSet found within group %s during world generation: check your configuration",
			                                    "ship_full_block_light"));
			fillerFullBlockLight = new Filler();
			fillerFullBlockLight.blockState = Blocks.GLOWSTONE.getDefaultState();
		} else {
			fillerFullBlockLight = fillerSetFullBlockLight.getRandomUnit(rand);
		}
		
		// wall light is on it's own
		final GenericSet<Filler> fillerSetWallLight = WarpDriveConfig.FillerManager.getRandomSetFromGroup(rand, "ship_wall_light");
		if (fillerSetWallLight == null) {
			WarpDrive.logger.warn(String.format("No FillerSet found within group %s during world generation: check your configuration",
			                                    "ship_wall_light"));
			fillerWallLight = new Filler();
			fillerWallLight.blockState = Blocks.REDSTONE_TORCH.getDefaultState();
		} else {
			fillerWallLight = fillerSetWallLight.getRandomUnit(rand);
		}
	}
	
	private Filler getGenericSetWithDefault(final Random rand, final String nameFillerKeyboard, final BlockState blockState) {
		final GenericSet<Filler> fillerSetKeyboard = WarpDriveConfig.FillerManager.getGenericSet(nameFillerKeyboard);
		final Filler result;
		if (fillerSetKeyboard == null) {
			WarpDrive.logger.warn(String.format("No FillerSet found within group %s during world generation: check your configuration",
			                                    nameFillerKeyboard));
			result = new Filler();
			result.blockState = blockState;
		} else {
			result = fillerSetKeyboard.getRandomUnit(rand);
		}
		return result;
	}
	
	public void setHullPlain(final World world, final int x, final int y, final int z) {
		if (corrupted && (rand.nextInt(400) == 1)) {
			world.createExplosion(null, x + 0.5D, y + 0.5D, z + 0.5D, 17.0F, false, Mode.DESTROY);
		} else if (corrupted && (rand.nextInt(10) == 1)) {
			world.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState(), 2);
		} else {
			fillerHullPlain.setBlock(world, new BlockPos(x, y, z));
		}
	}
	
	public void setHullGlass(final World world, final int x, final int y, final int z) {
		if (corrupted && (rand.nextInt(5) == 1)) {
			world.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState(), 2);
		} else {
			fillerHullGlass.setBlock(world, new BlockPos(x, y, z));
		}
	}
	
	public void setComputerCore(final World world, final int x, final int y, final int z) {
		if (corrupted && (rand.nextInt(3) == 1)) {
			world.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState(), 2);
		} else {
			fillerComputerCore.setBlock(world, new BlockPos(x, y, z));
			fillInventoryWithLoot(world, rand, x, y, z, "ship_computerCore");
		}
	}
	
	public void setComputerScreen(final World world, final int x, final int y, final int z) {
		if (corrupted && (rand.nextInt(3) == 1)) {
			world.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState(), 2);
		} else {
			fillerComputerScreen.setBlock(world, new BlockPos(x, y, z));
		}
	}
	
	public void setComputerKeyboard(final World world, final int x, final int y, final int z) {
		if (corrupted && (rand.nextInt(3) == 1)) {
			world.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState(), 2);
		} else {
			fillerComputerKeyboard.setBlock(world, new BlockPos(x, y, z));
		}
	}
	
	public void setComputerFloppy(final World world, final int x, final int y, final int z) {
		if (corrupted && (rand.nextInt(3) == 1)) {
			world.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState(), 2);
		} else {
			fillerComputerFloppy.setBlock(world, new BlockPos(x, y, z));
			fillInventoryWithLoot(world, rand, x, y, z, "ship_computerFloppy");
		}
	}
	
	public void setSolarPanel(final World world, final int x, final int y, final int z) {
		if (corrupted && (rand.nextInt(3) == 1)) {
			world.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState(), 2);
		} else {
			fillerSolarPanel.setBlock(world, new BlockPos(x, y, z));
		}
	}
	
	public void setWiring(final World world, final int x, final int y, final int z) {
		if (corrupted && (rand.nextInt(3) == 1)) {
			world.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState(), 2);
		} else {
			fillerWiring.setBlock(world, new BlockPos(x, y, z));
		}
	}
	
	public void setPropulsion(final World world, final int x, final int y, final int z) {
		fillerPropulsion.setBlock(world, new BlockPos(x, y, z));
	}
	
	public void setFullBlockLight(final World world, final int x, final int y, final int z) {
		fillerFullBlockLight.setBlock(world, new BlockPos(x, y, z));
	}
	
	public void setWallLight(final World world, final int x, final int y, final int z) {
		fillerWallLight.setBlock(world, new BlockPos(x, y, z));
	}
	
	public void fillInventoryWithLoot(final World world, final Random rand, final int x, final int y, final int z, final String group) {
		fillInventoryWithLoot(world, rand, new BlockPos(x, y, z), group, 0, 3, 4, 3);
	}
	
	public void fillInventoryWithLoot(final World world, final Random rand, final BlockPos blockPos, final String group,
	                                  final int quantityMin, final int quantityRandom1, final int quantityRandom2,
	                                  final int maxRetries) {
		
		final GenericSet<Loot> lootSet = WarpDriveConfig.LootManager.getRandomSetFromGroup(rand, group);
		if (lootSet == null) {
			WarpDrive.logger.warn(String.format("Unable to fill inventory with LootSet %s %s: no LootSet found with group %s, check your configuration",
			                                    group,
			                                    Commons.format(world, blockPos),
			                                    group ));
			return;
		}
		
		fillInventoryWithLoot(world, rand, blockPos, lootSet, quantityMin, quantityRandom1, quantityRandom2, maxRetries);
	}
	
	public void fillInventoryWithLoot(final World world, final Random rand, final BlockPos blockPos, final GenericSet<Loot> lootSet,
	                                  final int quantityMin, final int quantityRandom1, final int quantityRandom2,
	                                  final int maxRetries) {
		// validate context
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		final Object inventory = InventoryWrapper.getInventory(tileEntity, null);
		
		if (inventory == null) {
			WarpDrive.logger.warn(String.format("Unable to fill inventory with LootSet %s %s: %s has no inventory",
			                                    lootSet.getFullName(),
			                                    Commons.format(world, blockPos),
			                                    tileEntity ));
			return;
		}
		
		if (tileEntity.isRemoved()) {
			WarpDrive.logger.warn(String.format("Unable to fill inventory with LootSet %s %s: %s is Invalid",
			                                    lootSet.getFullName(),
			                                    Commons.format(world, blockPos),
			                                    tileEntity ));
			return;
		}
		
		// evaluate parameters: quantity of loot, actual loot set
		final int size = InventoryWrapper.getSize(inventory);
		final int countLoots = Math.min(quantityMin + rand.nextInt(quantityRandom1) + rand.nextInt(quantityRandom2), size);
		
		// shuffle slot indexes to reduce random calls and loops later on
		final ArrayList<Integer> indexSlots = new ArrayList<>(size);
		for (int indexSlot = 0; indexSlot < size; indexSlot++) {
			final ItemStack itemStack = InventoryWrapper.getStackInSlot(inventory, indexSlot);
			if (itemStack.isEmpty()) {
				indexSlots.add(indexSlot);
			}
		}
		Collections.shuffle(indexSlots);
		
		if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
			WarpDrive.logger.debug(String.format("About to add %d loots from set %s into inventory %s at %s with max retries %d for each",
			                                     countLoots,
			                                     lootSet.getFullName(),
			                                     inventory,
			                                     Commons.format(world, blockPos),
			                                     maxRetries));
		}
		// for all loots to add
		ItemStack itemStackLoot;
		boolean isAdded;
		for (int i = 0; i < countLoots; i++) {
			isAdded = false;
			// with a few retries
			for (int countLootRetries = 0; countLootRetries < maxRetries; countLootRetries++) {
				// pick a loot
				itemStackLoot = lootSet.getRandomUnit(rand).getItemStack(rand);
				
				// find a valid slot for it
				for (final Iterator<Integer> iterator = indexSlots.iterator(); iterator.hasNext(); ) {
					final Integer indexSlot = iterator.next();
					if (!InventoryWrapper.getStackInSlot(inventory, indexSlot).isEmpty()) {
						assert false;   // index used were already removed, so we shouldn't reach this
						continue;
					}
					if (InventoryWrapper.isItemValid(inventory, indexSlot, itemStackLoot)) {
						// remove that slot & item, even if insertion fail, to avoid a spam
						iterator.remove();
						isAdded = true;
						try {
							InventoryWrapper.insertItem(inventory, indexSlot, itemStackLoot);
							if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
								WarpDrive.logger.debug(String.format(" + placed %s into slot %d",
								                                     Commons.format(itemStackLoot),
								                                     indexSlot));
							}
						} catch (final Exception exception) {
							exception.printStackTrace(WarpDrive.printStreamError);
							WarpDrive.logger.error(String.format(" ! Exception while placing %s into slot %d: %s",
							                                     Commons.format(itemStackLoot),
							                                     indexSlot,
							                                     exception.getMessage() ));
						}
						break;
					}
				}
				if (isAdded || indexSlots.isEmpty()) {
					break;
				}
			}
			if (!isAdded) {
				WarpDrive.logger.debug(String.format("Unable to find a valid loot from LootSet %s for inventory %s in %s: check your configuration",
				                                     lootSet.getFullName(),
				                                     inventory,
				                                     Commons.format(world, blockPos) ));
			}
		}
	}
	
	public void generateFromFile(final World world, final String filename, final int targetX, final int targetY, final int targetZ, final byte rotationSteps) {
		final WarpDriveText reason = new WarpDriveText();
		final JumpShip jumpShip = JumpShip.createFromFile(filename, reason);
		if (jumpShip == null) {
			WarpDrive.logger.error(String.format("%s Failed to read schematic %s: %s", this, filename, reason.toString()));
			return;
		}
		deployShip(world, jumpShip, targetX, targetY, targetZ, rotationSteps);
	}
	
	public void deployShip(final World world, final JumpShip jumpShip, final int targetX, final int targetY, final int targetZ, final byte rotationSteps) {
		deployShip(world, jumpShip, targetX, targetY, targetZ, rotationSteps, null);
	}
	
	public void deployShip(final World world, final JumpShip jumpShip, final int targetX, final int targetY, final int targetZ, final byte rotationSteps, @Nullable final Insertion[] insertions) {
		
		final Transformation transformation = new Transformation(jumpShip, world,
			targetX - jumpShip.core.getX(),
			targetY - jumpShip.core.getY(),
			targetZ - jumpShip.core.getZ(),
			rotationSteps);
		for (int index = 0; index < jumpShip.jumpBlocks.length; index++) {
			// Deploy single block
			final JumpBlock jumpBlock = jumpShip.jumpBlocks[index];
			
			if (jumpBlock == null) {
				if (WarpDriveConfig.LOGGING_BUILDING) {
					WarpDrive.logger.info(String.format("At index %d, skipping undefined block", index));
				}
			} else if (jumpBlock.blockState.getBlock() == Blocks.AIR) {
				if (WarpDriveConfig.LOGGING_BUILDING) {
					WarpDrive.logger.info(String.format("At index %d, skipping air block", index));
				}
			} else if (Dictionary.BLOCKS_ANCHOR.contains(jumpBlock.blockState.getBlock())) {
				if (WarpDriveConfig.LOGGING_BUILDING) {
					WarpDrive.logger.info(String.format("At index %d, skipping anchor block %s", index, jumpBlock.blockState));
				}
			} else {
				if (WarpDrive.isDev && WarpDriveConfig.LOGGING_WORLD_GENERATION) {
					WarpDrive.logger.info(String.format("At index %d, deploying %s ",
					                                    index, jumpBlock));
				}
				final BlockPos targetLocation = transformation.apply(jumpBlock.x, jumpBlock.y, jumpBlock.z);
				final Block blockAtTarget = world.getBlockState(targetLocation).getBlock();
				if (blockAtTarget == Blocks.AIR || Dictionary.BLOCKS_EXPANDABLE.contains(blockAtTarget)) {
					jumpBlock.deploy(null, world, transformation);
					
					// Apply insertions as defined
					if (insertions != null) {
						for(final Insertion insertion : insertions){
							if (insertion.isMatching(jumpBlock)){
								final BlockPos deployedLocation = transformation.apply(jumpBlock.x, jumpBlock.y, jumpBlock.z);
								
								fillInventoryWithLoot(world, rand, deployedLocation,
								                      insertion,
								                      insertion.getMinQuantity(),
								                      insertion.getMaxQuantity(),
								                      0,
								                      insertion.getMaxRetries() );
							}
						}
					}
					
				} else {
					if (WarpDrive.isDev && WarpDriveConfig.LOGGING_WORLD_GENERATION) {
						WarpDrive.logger.info(String.format("Deployment collision detected %s with %s during world generation, skipping this block...",
						                                    Commons.format(world, targetX + jumpBlock.x, targetY + jumpBlock.y, targetZ + jumpBlock.z),
						                                    blockAtTarget.getRegistryName()));
					}
				}
			}
		}
	}
}
