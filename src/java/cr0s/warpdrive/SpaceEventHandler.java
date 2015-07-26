package cr0s.warpdrive;

import ic2.api.item.IC2Items;

import java.util.HashMap;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cr0s.warpdrive.api.IBreathingHelmet;
import cr0s.warpdrive.world.SpaceTeleporter;

/**
 *
 * @author Cr0s
 */
public class SpaceEventHandler {
	private HashMap<Integer, Integer> entity_airBlock;
	private HashMap<String, Integer> player_airTank;
	private HashMap<String, Integer> player_cloakTicks;

	private final int CLOAK_CHECK_TIMEOUT_TICKS = 100;
	private final int AIR_BLOCK_TICKS = 20;
	private final int AIR_TANK_TICKS = 300;
	private final int AIR_DROWN_TICKS = 20;

	public SpaceEventHandler() {
		entity_airBlock = new HashMap<Integer, Integer>();
		player_airTank = new HashMap<String, Integer>();
		player_cloakTicks = new HashMap<String, Integer>();
	}

	@SubscribeEvent
	public void livingUpdate(LivingUpdateEvent event) {
		if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
			return;
		}

		EntityLivingBase entity = event.entityLiving;
		int x = MathHelper.floor_double(entity.posX);
		int y = MathHelper.floor_double(entity.posY);
		int z = MathHelper.floor_double(entity.posZ);

		// Instant kill if entity exceeds world's limit
		if (WarpDriveConfig.G_SPACE_WORLDBORDER_BLOCKS > 0
				&& (Math.abs(x) > WarpDriveConfig.G_SPACE_WORLDBORDER_BLOCKS || Math.abs(z) > WarpDriveConfig.G_SPACE_WORLDBORDER_BLOCKS)) {
			if (entity instanceof EntityPlayerMP) {
				if (((EntityPlayerMP) entity).capabilities.isCreativeMode) {
					return;
				}
			}

			entity.attackEntityFrom(DamageSource.outOfWorld, 9000);
			return;
		}
		if (entity instanceof EntityPlayerMP) {
			updatePlayerCloakState(entity);

			// skip players in creative
			if (((EntityPlayerMP) entity).capabilities.isCreativeMode) {
				return;
			}
		}

		// skip dead or invulnerable entities
		if (entity.isDead || entity.isEntityInvulnerable()) {
			return;
		}

		// If entity is in vacuum, check and start consuming air cells
		if (entity.worldObj.provider.dimensionId == WarpDriveConfig.G_SPACE_DIMENSION_ID
				|| entity.worldObj.provider.dimensionId == WarpDriveConfig.G_HYPERSPACE_DIMENSION_ID) {
			Block block1 = entity.worldObj.getBlock(x, y, z);
			Block block2 = entity.worldObj.getBlock(x, y + 1, z);
			boolean inVacuum = (!block1.isAssociatedBlock(WarpDrive.airBlock) && !block2.isAssociatedBlock(WarpDrive.airBlock));
			Integer air;
			if (!inVacuum) {// In space with air blocks
				air = entity_airBlock.get(entity.getEntityId());
				if (air == null) {
					entity_airBlock.put(entity.getEntityId(), AIR_BLOCK_TICKS);
				} else if (air <= 1) {// time elapsed => consume air block
					entity_airBlock.put(entity.getEntityId(), AIR_BLOCK_TICKS);

					int metadata;
					if (block1.isAssociatedBlock(WarpDrive.airBlock)) {
						metadata = entity.worldObj.getBlockMetadata(x, y, z);
						if (metadata > 0 && metadata < 15) {
							entity.worldObj.setBlockMetadataWithNotify(x, y, z, metadata - 1, 2);
						}
					} else {
						metadata = entity.worldObj.getBlockMetadata(x, y + 1, z);
						if (metadata > 0 && metadata < 15) {
							entity.worldObj.setBlockMetadataWithNotify(x, y + 1, z, metadata - 1, 2);
						}
					}
				} else {
					entity_airBlock.put(entity.getEntityId(), air - 1);
				}
			} else {// In space without air blocks
				// Damage entity if in vacuum without protection
				if (entity instanceof EntityPlayerMP) {
					EntityPlayerMP player = (EntityPlayerMP) entity;
					air = player_airTank.get(player.getDisplayName());

					boolean hasHelmet = false;
					ItemStack helmetStack = player.getCurrentArmor(3);
					if (helmetStack != null) {
						Item helmet = helmetStack.getItem();
						if (helmet instanceof IBreathingHelmet) {
							IBreathingHelmet breathHelmet = (IBreathingHelmet) helmet;
							int airTicks = breathHelmet.ticksPerCanDamage();
							if (breathHelmet.canBreath(player)) {
								hasHelmet = true;
								if (air == null) {// new player in space =>
									// grace period
									player_airTank.put(player.getDisplayName(), airTicks);
								} else if (air <= 1) {
									if (breathHelmet.removeAir(player)) {
										player_airTank.put(player.getDisplayName(), airTicks);
									} else {
										player_airTank.put(player.getDisplayName(), AIR_DROWN_TICKS);
										player.attackEntityFrom(DamageSource.drown, 2.0F);
									}
								} else {
									player_airTank.put(player.getDisplayName(), air - 1);
								}
							}
						} else if (WarpDriveConfig.spaceHelmets.contains(helmetStack)) {
							hasHelmet = true;
							if (air == null) {// new player in space => grace
								// period
								player_airTank.put(player.getDisplayName(), AIR_TANK_TICKS);
							} else if (air <= 1) {
								if (consumeO2(player.inventory.mainInventory, player)) {
									player_airTank.put(player.getDisplayName(), AIR_TANK_TICKS);
								} else {
									player_airTank.put(player.getDisplayName(), AIR_DROWN_TICKS);
									entity.attackEntityFrom(DamageSource.drown, 2.0F);
								}
							} else {
								player_airTank.put(player.getDisplayName(), air - 1);
							}
						}
					}

					if (!hasHelmet) {
						if (air == null) {// new player in space => grace period
							player_airTank.put(player.getDisplayName(), AIR_TANK_TICKS);
						} else if (air <= 1) {
							player_airTank.put(player.getDisplayName(), AIR_DROWN_TICKS);
							entity.attackEntityFrom(DamageSource.drown, 2.0F);
						} else {
							player_airTank.put(player.getDisplayName(), air - 1);
						}
					}

					// If player falling down, teleport on earth
					if (entity.posY < -10.0D) {
						player.mcServer.getConfigurationManager().transferPlayerToDimension(player, 0,
								new SpaceTeleporter(DimensionManager.getWorld(WarpDriveConfig.G_SPACE_DIMENSION_ID), 0, x, 250, z));
						player.setFire(30);
						player.setPositionAndUpdate(entity.posX, 250.0D, entity.posZ);
					}
				} else {// (in space, no air block and not a player)
					entity_airBlock.put(entity.getEntityId(), 0);
					entity.attackEntityFrom(DamageSource.drown, 2.0F);
				}
			}
		}
	}

	private void updatePlayerCloakState(EntityLivingBase entity) {
		try {
			EntityPlayerMP player = (EntityPlayerMP) entity;
			Integer cloakTicks = player_cloakTicks.get(player.getDisplayName());

			if (cloakTicks == null) {
				player_cloakTicks.put(player.getDisplayName(), 0);
				return;
			}

			if (cloakTicks >= CLOAK_CHECK_TIMEOUT_TICKS) {
				player_cloakTicks.put(player.getDisplayName(), 0);

				WarpDrive.cloaks.updatePlayer(player);
			} else {
				player_cloakTicks.put(player.getDisplayName(), cloakTicks + 1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static boolean consumeO2(ItemStack[] inventory, EntityPlayerMP entityPlayer) {
		for (int j = 0; j < inventory.length; ++j) {
			if (inventory[j] != null && inventory[j] == WarpDriveConfig.IC2_Air) {
				inventory[j].stackSize--;
				if (inventory[j].stackSize <= 0) {
					inventory[j] = null;
				}

				if (WarpDriveConfig.IC2_Empty != null) {
					// WarpDrive.debugPrint("giveEmptyCell");
					ItemStack emptyCell = new ItemStack(WarpDriveConfig.IC2_Empty.getItem(), 1, 0);
					if (!entityPlayer.inventory.addItemStackToInventory(emptyCell)) {
						World world = entityPlayer.worldObj;
						EntityItem itemEnt = new EntityItem(world, entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ, emptyCell);
						entityPlayer.worldObj.spawnEntityInWorld(itemEnt);
					}
				}
				return true;
			}
		}
		return false;
	}

	@SubscribeEvent
	public void livingFall(LivingFallEvent event) {
		EntityLivingBase entity = event.entityLiving;
		float distance = event.distance;

		if (entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) entity;
			int check = MathHelper.ceiling_float_int(distance - 3.0F);

			if (check > 0) {
				if ((player.getCurrentArmor(0) != null && player.getCurrentArmor(0) == IC2Items.getItem("quantumBoots"))
						|| (player.getCurrentArmor(2) != null && WarpDriveConfig.jetpacks.contains(player.getCurrentArmor(2)))) {
					event.setCanceled(true); // Don't damage player
				}
			}
		}
	}
}
