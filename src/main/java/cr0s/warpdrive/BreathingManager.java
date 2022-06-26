package cr0s.warpdrive;

import cr0s.warpdrive.api.IAirContainerItem;
import cr0s.warpdrive.api.IBreathingHelmet;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.data.StateAir;
import cr0s.warpdrive.data.VectorI;
import cr0s.warpdrive.event.ChunkHandler;
import cr0s.warpdrive.data.EnergyWrapper;
import cr0s.warpdrive.render.EntityCamera;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BreathingManager {
	
	private static final int AIR_BLOCK_TICKS = 20;
	private static final int AIR_DROWN_TICKS = 20;
	private static final int AIR_FIRST_BREATH_TICKS = 300;
	
	private static final int AIR_IC2_COMPRESSED_AIR_TICKS = 300;
	
	private static final int AIR_ENERGY_FOR_ELECTROLYSE = 2000;
	
	private static final VectorI[] vAirOffsets = {
			new VectorI(0, 0, 0), new VectorI(0, 1, 0),
			new VectorI(0, 1, 1), new VectorI(0, 1, -1), new VectorI(1, 1, 0), new VectorI(1, 1, 0),
			new VectorI(0, 0, 1), new VectorI(0, 0, -1), new VectorI(1, 0, 0), new VectorI(1, 0, 0) };
	
	private static final HashMap<UUID, Integer> entity_airBlock = new HashMap<>();
	private static final HashMap<UUID, Integer> player_airTank = new HashMap<>();
	
	public static boolean hasAirBlock(final LivingEntity entityLivingBase, final int x, final int y, final int z) {
		final BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable();
		for (final VectorI vOffset : vAirOffsets) {
			mutableBlockPos.setPos(x + vOffset.x, y + vOffset.y, z + vOffset.z);
			final Block block = entityLivingBase.world.getBlockState(mutableBlockPos).getBlock();
			if (isAirBlock(block)) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isAirBlock(@Nonnull final Block block) {
		return block == WarpDrive.blockAirSource
		    || block == WarpDrive.blockAirFlow;
	}
	
	public static boolean onLivingJoinEvent(final LivingEntity entityLivingBase, final int x, final int y, final int z) {
		if ( entityLivingBase instanceof EntityCamera
		  && !entityLivingBase.getEntityWorld().isRemote ) {
			WarpDrive.logger.warn(String.format("EntityCamera is client-side only, deny spawning %s entityId '%s'",
			                                    Commons.format(entityLivingBase.world, x, y, z),
			                                    Dictionary.getId(entityLivingBase) ));
			return false;
		}
		
		// skip living entities who don't need air
		if (Dictionary.isLivingWithoutAir(entityLivingBase)) {
			return true;
		}
		
		if (hasAirBlock(entityLivingBase, x, y, z)) {
			return true;
		}
		if (hasValidSetup(entityLivingBase)) {
			return true;
		}
		
		if (WarpDriveConfig.LOGGING_BREATHING) {
			WarpDrive.logger.warn(String.format("Entity spawn denied %s entityId '%s'",
			                                    Commons.format(entityLivingBase.world, x, y, z),
			                                    Dictionary.getId(entityLivingBase) ));
		}
		return false;
	}
	
	public static void onLivingUpdateEvent(final LivingEntity entityLivingBase, final int x, final int y, final int z) {
		// skip living entities who don't need air
		if (Dictionary.isLivingWithoutAir(entityLivingBase)) {
			return;
		}
		
		// find an air block
		final UUID uuidEntity = entityLivingBase.getUniqueID();
		boolean notInVacuum = false;
		final BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable();
		BlockState blockState;
		Block block;
		for (final VectorI vOffset : vAirOffsets) {
			mutableBlockPos.setPos(x + vOffset.x, y + vOffset.y, z + vOffset.z);
			blockState = entityLivingBase.world.getBlockState(mutableBlockPos);
			block = blockState.getBlock();
			if (isAirBlock(block)) {
				notInVacuum = true;
				break;
			} else if (block != Blocks.AIR) {
				final StateAir stateAir = ChunkHandler.getStateAir(entityLivingBase.world, mutableBlockPos.getX(), mutableBlockPos.getY(), mutableBlockPos.getZ());
				if ( stateAir == null
				  || stateAir.concentration > 0 ) {
					notInVacuum = true;
					break;
				}
			}
		}
		
		Integer air = entity_airBlock.get(uuidEntity);
		if (notInVacuum) {// no atmosphere with air blocks
			if (air == null) {
				entity_airBlock.put(uuidEntity, AIR_BLOCK_TICKS);
			} else if (air <= 1) {// time elapsed => consume air block
				entity_airBlock.put(uuidEntity, AIR_BLOCK_TICKS);
			} else {
				entity_airBlock.put(uuidEntity, air - 1);
			}
			
		} else {// no atmosphere without air blocks
			// add grace period on first breath
			if (air == null) {
				entity_airBlock.put(uuidEntity, AIR_FIRST_BREATH_TICKS);
				return;
			}
			// players have a grace period when exiting breathable area
			// others just finish air from blocks
			if (air > 0) {
				if (entityLivingBase instanceof ServerPlayerEntity) {
					entity_airBlock.put(uuidEntity, 0);
					player_airTank.put(uuidEntity, AIR_FIRST_BREATH_TICKS);
					return;
				} else {
					entity_airBlock.put(uuidEntity, air - 1);
					return;
				} 
			}
			
			// damage entity if in vacuum without protection
			final boolean hasValidSetup = hasValidSetup(entityLivingBase);
			if (entityLivingBase instanceof ServerPlayerEntity) {
				final ServerPlayerEntity player = (ServerPlayerEntity) entityLivingBase;
				air = player_airTank.get(uuidEntity);
				
				boolean hasHelmet = hasValidSetup;
				if (hasValidSetup) {
					if (air == null) {// new player in space => grace period
						player_airTank.put(uuidEntity, AIR_FIRST_BREATH_TICKS);
					} else if (air <= 1) {
						final int ticksAir = consumeAir(player);
						if (ticksAir > 0) {
							player_airTank.put(uuidEntity, ticksAir);
						} else {
							hasHelmet = false;
						}
					} else {
						player_airTank.put(uuidEntity, air - 1);
					}
				}
				
				if (!hasHelmet) {
					if (air == null) {// new player in space => grace period
						player_airTank.put(uuidEntity, AIR_FIRST_BREATH_TICKS);
					} else if (air <= 1) {
						player_airTank.put(uuidEntity, AIR_DROWN_TICKS);
						entityLivingBase.attackEntityFrom(WarpDrive.damageAsphyxia, 2.0F);
					} else {
						player_airTank.put(uuidEntity, air - 1);
					}
				}
				
			} else {// (in space, no air block and not a player)
				if (hasValidSetup) {
					// let it live for now, checking periodically if helmet gets broken in combat
					entity_airBlock.put(uuidEntity, AIR_FIRST_BREATH_TICKS);
				} else {
					entity_airBlock.put(uuidEntity, 0);
					entityLivingBase.attackEntityFrom(WarpDrive.damageAsphyxia, 2.0F);
				}
			}
		}
	}
	
	private static int consumeAir(final LivingEntity entityLivingBase) {
		if (WarpDriveConfig.LOGGING_BREATHING) {
			WarpDrive.logger.info("Checking inventory for air reserves...");
		}
		if (!(entityLivingBase instanceof ServerPlayerEntity)) {
			return 0;
		}
		
		final ServerPlayerEntity entityPlayer = (ServerPlayerEntity) entityLivingBase;
		final NonNullList<ItemStack> playerInventory = entityPlayer.inventory.mainInventory;
		int slotAirCanisterFound = -1;
		float fillingRatioAirCanisterFound = 0.0F;
		
		// find most consumed air canister with smallest stack
		for (int slotIndex = 0; slotIndex < playerInventory.size(); slotIndex++) {
			final ItemStack itemStack = playerInventory.get(slotIndex);
			if ( itemStack != ItemStack.EMPTY
			  && itemStack.getCount() > 0
			  && itemStack.getItem() instanceof IAirContainerItem) {
				final IAirContainerItem airContainerItem = (IAirContainerItem) itemStack.getItem();
				final int airAvailable = airContainerItem.getCurrentAirStorage(itemStack);
				if (airAvailable > 0) {
					float fillingRatio = airAvailable / (float) airContainerItem.getMaxAirStorage(itemStack);
					fillingRatio -= itemStack.getCount() / 1000.0F;
					if (fillingRatioAirCanisterFound <= 0.0F || fillingRatio < fillingRatioAirCanisterFound) {
						slotAirCanisterFound = slotIndex;
						fillingRatioAirCanisterFound = fillingRatio;
					}
				}
			}
		}
		// consume air on the selected Air canister
		if (slotAirCanisterFound >= 0) {
			final ItemStack itemStack = playerInventory.get(slotAirCanisterFound);
			if ( !itemStack.isEmpty()
			  && itemStack.getItem() instanceof IAirContainerItem ) {
				final IAirContainerItem airContainerItem = (IAirContainerItem) itemStack.getItem();
				final int airAvailable = airContainerItem.getCurrentAirStorage(itemStack);
				if (airAvailable > 0) {
					if (itemStack.getCount() > 1) {// unstack
						itemStack.shrink(1);
						ItemStack itemStackToAdd = itemStack.copy();
						itemStackToAdd.setCount(1);
						itemStackToAdd = airContainerItem.consumeAir(itemStackToAdd);
						if (!entityPlayer.inventory.addItemStackToInventory(itemStackToAdd)) {
							final ItemEntity entityItem = new ItemEntity(entityPlayer.world, entityPlayer.getPosX(), entityPlayer.getPosY(), entityPlayer.getPosZ(), itemStackToAdd);
							entityPlayer.world.addEntity(entityItem);
						}
						entityPlayer.sendContainerToPlayer(entityPlayer.container);
					} else {
						final ItemStack itemStackNew = airContainerItem.consumeAir(itemStack);
						if (itemStack != itemStackNew) {
							playerInventory.set(slotAirCanisterFound, itemStackNew);
						}
					}
					return airContainerItem.getAirTicksPerConsumption(itemStack);
				}
			}
		}
		
		// (no air canister or all empty)
		// check IC2 compressed air cells
		if (WarpDriveConfig.IC2_compressedAir != null) {
			for (int slotIndex = 0; slotIndex < playerInventory.size(); ++slotIndex) {
				final ItemStack itemStack = playerInventory.get(slotIndex);
				if ( !itemStack.isEmpty()
				  && WarpDriveConfig.isIC2CompressedAir(itemStack) ) {
					itemStack.shrink(1);
					playerInventory.set(slotIndex, itemStack);
					
					if (WarpDriveConfig.IC2_emptyCell != null) {
						final ItemStack emptyCell = new ItemStack(WarpDriveConfig.IC2_emptyCell.getItem(), 1);
						if (!entityPlayer.inventory.addItemStackToInventory(emptyCell)) {
							final World world = entityPlayer.world;
							final ItemEntity entityItem = new ItemEntity(world, entityPlayer.getPosX(), entityPlayer.getPosY(), entityPlayer.getPosZ(), emptyCell);
							entityPlayer.world.addEntity(entityItem);
						}
						entityPlayer.sendContainerToPlayer(entityPlayer.container);
					}
					return AIR_IC2_COMPRESSED_AIR_TICKS;
				}
			}
		}
		
		// all air containers are empty
		final ItemStack itemStackChestplate = entityLivingBase.getItemStackFromSlot(EquipmentSlotType.CHEST);
		if (!itemStackChestplate.isEmpty()) {
			final Item itemChestplate = itemStackChestplate.getItem();
			if (itemChestplate == WarpDrive.itemWarpArmor[EnumTier.SUPERIOR.getIndex()][2]) {
				return electrolyseIceToAir(entityLivingBase);
			}
		}
		return 0;
	}
	
	public static boolean hasValidSetup(@Nonnull final LivingEntity entityLivingBase) {
		final ItemStack itemStackHelmet = entityLivingBase.getItemStackFromSlot(EquipmentSlotType.HEAD);
		if (entityLivingBase instanceof PlayerEntity) {
			final ItemStack itemStackChestplate = entityLivingBase.getItemStackFromSlot(EquipmentSlotType.CHEST);
			final ItemStack itemStackLeggings = entityLivingBase.getItemStackFromSlot(EquipmentSlotType.LEGS);
			final ItemStack itemStackBoots = entityLivingBase.getItemStackFromSlot(EquipmentSlotType.FEET);
			// need full armor set to breath
			if ( !itemStackHelmet.isEmpty()
			  && !itemStackChestplate.isEmpty()
			  && !itemStackLeggings.isEmpty()
			  && !itemStackBoots.isEmpty() ) {
				// need a working breathing helmet to breath
				final Item itemHelmet = itemStackHelmet.getItem();
				return (itemHelmet instanceof IBreathingHelmet && ((IBreathingHelmet) itemHelmet).canBreath(entityLivingBase))
				    || Dictionary.ITEMS_BREATHING_HELMET.contains(itemHelmet);
			}
			
		} else {
			// need just a working breathing helmet to breath
			if (!itemStackHelmet.isEmpty()) {
				final Item itemHelmet = itemStackHelmet.getItem();
				return (itemHelmet instanceof IBreathingHelmet && ((IBreathingHelmet) itemHelmet).canBreath(entityLivingBase))
				    || Dictionary.ITEMS_BREATHING_HELMET.contains(itemHelmet);
			}
		}
		return false;
	}
	
	public static float getAirReserveRatio(@Nonnull final PlayerEntity entityPlayer) {
		final NonNullList<ItemStack> playerInventory = entityPlayer.inventory.mainInventory;
		
		// check electrolysing
		boolean canElectrolyse = false;
		final ItemStack itemStackChestplate = entityPlayer.getItemStackFromSlot(EquipmentSlotType.CHEST);
		if (!itemStackChestplate.isEmpty()) {
			final Item itemChestplate = itemStackChestplate.getItem();
			if (itemChestplate == WarpDrive.itemWarpArmor[EnumTier.SUPERIOR.getIndex()][2]) {
				canElectrolyse = true;
			}
		}
		
		// check all inventory slots for air containers, etc.
		final Item itemIce = Items.ICE;
		int sumAirCapacityTicks = 0;
		int sumAirStoredTicks = 0;
		int countAirContainer = 0;
		int countIce = 0;
		int countEnergy = 0;
		ItemStack itemStackAirContainer = null;
		for (final ItemStack itemStack : playerInventory) {
			if (!itemStack.isEmpty()) {
				if (itemStack.getItem() instanceof IAirContainerItem) {
					countAirContainer++;
					itemStackAirContainer = itemStack;
					final IAirContainerItem airContainerItem = (IAirContainerItem) itemStack.getItem();
					final int airAvailable = airContainerItem.getCurrentAirStorage(itemStack);
					if (airAvailable > 0) {
						sumAirStoredTicks += airAvailable * airContainerItem.getAirTicksPerConsumption(itemStack);
					}
					final int airCapacity = airContainerItem.getMaxAirStorage(itemStack);
					sumAirCapacityTicks += airCapacity * airContainerItem.getAirTicksPerConsumption(itemStack);
					
				} else if ( WarpDriveConfig.IC2_compressedAir != null
				         && WarpDriveConfig.isIC2CompressedAir(itemStack) ) {
					sumAirStoredTicks += AIR_IC2_COMPRESSED_AIR_TICKS * itemStack.getCount();
					sumAirCapacityTicks += AIR_IC2_COMPRESSED_AIR_TICKS * itemStack.getCount();
					
				} else if ( WarpDriveConfig.IC2_emptyCell != null
				         && itemStack.isItemEqual(WarpDriveConfig.IC2_emptyCell) ) {
					sumAirCapacityTicks += AIR_IC2_COMPRESSED_AIR_TICKS * itemStack.getCount();
					
				} else if (canElectrolyse) {
					if (itemStack.getItem() == itemIce) {
						countIce += itemStack.getCount();
					} else if ( EnergyWrapper.isEnergyContainer(itemStack)
					         && EnergyWrapper.canOutput(itemStack)
					         && EnergyWrapper.getEnergyStored(itemStack) >= AIR_ENERGY_FOR_ELECTROLYSE ) {
						countEnergy += EnergyWrapper.getEnergyStored(itemStack) / AIR_ENERGY_FOR_ELECTROLYSE;
					}
				}
			}
		}
		
		// add electrolyse bonus
		if (countAirContainer >= 1 && countIce > 0 && countEnergy > 0 && itemStackAirContainer.getItem() instanceof IAirContainerItem) {
			final IAirContainerItem airContainerItem = (IAirContainerItem) itemStackAirContainer.getItem();
			final int sumElectrolyseTicks =
					  Math.min(2, countAirContainer)       // up to 2 containers refilled
			        * Math.min(countIce, countEnergy)      // requiring both ice and energy
			        * airContainerItem.getMaxAirStorage(itemStackAirContainer)
			        * airContainerItem.getAirTicksPerConsumption(itemStackAirContainer);
			sumAirStoredTicks += sumElectrolyseTicks;
			sumAirCapacityTicks += sumElectrolyseTicks;
		}
		
		return sumAirCapacityTicks > 0 ? sumAirStoredTicks / (float) sumAirCapacityTicks : 0.0F;
	}
	
	private static int electrolyseIceToAir(final Entity entity) {
		if (WarpDriveConfig.LOGGING_BREATHING) {
			WarpDrive.logger.info("Checking inventory for ice electrolysing...");
		}
		if (!(entity instanceof ServerPlayerEntity)) {
			return 0;
		}
		final ServerPlayerEntity entityPlayer = (ServerPlayerEntity) entity;
		final NonNullList<ItemStack> playerInventory = entityPlayer.inventory.mainInventory;
		int slotIceFound = -1;
		int slotFirstEmptyAirContainerFound = -1;
		int slotSecondEmptyAirContainerFound = -1;
		int slotEnergyContainer = -1;
		
		// find 1 ice, 1 energy and up to 2 empty air containers
		final Item itemIce = Items.ICE;
		for (int slotIndex = 0; slotIndex < playerInventory.size(); slotIndex++) {
			final ItemStack itemStack = playerInventory.get(slotIndex);
			if (itemStack.isEmpty()) {
				continue;
			}
			
			if (itemStack.getItem() == itemIce) {
				slotIceFound = slotIndex;
				if ( slotSecondEmptyAirContainerFound >= 0
				  && slotEnergyContainer >= 0 ) {
					break;
				}
				
			} else if ( itemStack.getCount() == 1
			         && itemStack.getItem() instanceof IAirContainerItem ) {
				final IAirContainerItem airCanister = (IAirContainerItem) itemStack.getItem();
				if ( airCanister.canContainAir(itemStack)
				  && airCanister.getCurrentAirStorage(itemStack) >= 0 ) {
					if (slotFirstEmptyAirContainerFound < 0) {
						slotFirstEmptyAirContainerFound = slotIndex;
					} else if (slotSecondEmptyAirContainerFound < 0) {
						slotSecondEmptyAirContainerFound = slotIndex;
						if ( slotIceFound >= 0
						  && slotEnergyContainer >= 0 ) {
							break;
						}
					}
				}
			} else if ( slotEnergyContainer < 0
			         && EnergyWrapper.isEnergyContainer(itemStack)
			         && EnergyWrapper.canOutput(itemStack)
			         && EnergyWrapper.getEnergyStored(itemStack) >= AIR_ENERGY_FOR_ELECTROLYSE ) {
				slotEnergyContainer = slotIndex;
				if ( slotIceFound >= 0
				  && slotSecondEmptyAirContainerFound >= 0 ) {
					break;
				}
			}
		}
		
		// skip if we're missing energy, ice or air container
		if ( slotEnergyContainer < 0
		  || slotIceFound < 0
		  || slotFirstEmptyAirContainerFound < 0 ) {
			return 0;
		}
		
		// consume energy
		final ItemStack itemStackEnergyContainer = playerInventory.get(slotEnergyContainer);
		final int energyProvided = EnergyWrapper.consume(itemStackEnergyContainer, AIR_ENERGY_FOR_ELECTROLYSE, false);
		if (energyProvided <= 0) {
			return 0;
		}
		
		// consume ice
		final ItemStack itemStackIce = playerInventory.get(slotIceFound);
		itemStackIce.shrink(1);
		playerInventory.set(slotIceFound, itemStackIce);
		
		// fill air canister(s)
		ItemStack itemStackAirCanister = playerInventory.get(slotFirstEmptyAirContainerFound);
		IAirContainerItem airCanister = (IAirContainerItem) itemStackAirCanister.getItem();
		playerInventory.set(slotFirstEmptyAirContainerFound, airCanister.getFullAirContainer(itemStackAirCanister));
		
		if (slotSecondEmptyAirContainerFound >= 0) {
			itemStackAirCanister = playerInventory.get(slotSecondEmptyAirContainerFound);
			airCanister = (IAirContainerItem) itemStackAirCanister.getItem();
			playerInventory.set(slotSecondEmptyAirContainerFound, airCanister.getFullAirContainer(itemStackAirCanister));
		}
		entityPlayer.sendContainerToPlayer(entityPlayer.container);
		
		// first air breath is free
		return airCanister.getAirTicksPerConsumption(itemStackAirCanister);
	}
	
	public static void onEntityLivingDeath(@Nonnull final LivingEntity entityLivingBase) {
		entity_airBlock.remove(entityLivingBase.getUniqueID());
	}
}
