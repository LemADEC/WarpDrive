package cr0s.warpdrive.event;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.world.FakeWorld;

import javax.annotation.Nonnull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;


public class TooltipHandler {
	
	private static final BlockPos blockPosDummy = new BlockPos(0, -1, 0);
	
	@SideOnly(Side.CLIENT)
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onTooltipEvent_first(@Nonnull final ItemTooltipEvent event) {
		if (event.getEntityPlayer() == null) {
			return;
		}
		if (event.getItemStack().isEmpty()) {
			return;
		}
		
		// add dictionary information
		if (Dictionary.ITEMS_BREATHING_HELMET.contains(event.getItemStack().getItem())) {
			Commons.addTooltip(event.getToolTip(), new TextComponentTranslation("warpdrive.tooltip.item_tag.breathing_helmet").getFormattedText());
		}
		if (Dictionary.ITEMS_FLYINSPACE.contains(event.getItemStack().getItem())) {
			Commons.addTooltip(event.getToolTip(), new TextComponentTranslation("warpdrive.tooltip.item_tag.fly_in_space").getFormattedText());
		}
		if (Dictionary.ITEMS_NOFALLDAMAGE.contains(event.getItemStack().getItem())) {
			Commons.addTooltip(event.getToolTip(), new TextComponentTranslation("warpdrive.tooltip.item_tag.no_fall_damage").getFormattedText());
		}
	}
	
	@SideOnly(Side.CLIENT)
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onTooltipEvent_last(@Nonnull final ItemTooltipEvent event) {
		if (event.getEntityPlayer() == null) {
			return;
		}
		if (event.getItemStack().isEmpty()) {
			return;
		}
		
		// note: event.getEntityPlayer().isSneaking() remains false inside GUIs, so we ask directly the driver
		final KeyBinding keyBindingSneak = Minecraft.getMinecraft().gameSettings.keyBindSneak;
		final boolean isSneaking = Commons.isKeyPressed(keyBindingSneak);
		final boolean isCreativeMode = event.getEntityPlayer().capabilities.isCreativeMode;
		
		// cleanup the mess every mods add (notably the registry name)
		cleanupTooltip(event.getToolTip(), isSneaking, isCreativeMode);
		
		// add block/items details
		final Block block = Block.getBlockFromItem(event.getItemStack().getItem());
		if (block != Blocks.AIR) {
			addBlockDetails(event, isSneaking, isCreativeMode, block);
		} else {
			addItemDetails(event, isSneaking, isCreativeMode, event.getItemStack());
		}
		
		// add burn time details
		if (WarpDriveConfig.TOOLTIP_ADD_BURN_TIME.isEnabled(isSneaking, isCreativeMode)) {
			try {
				final int fuelEvent = ForgeEventFactory.getItemBurnTime(event.getItemStack());
				final int fuelFurnace = Math.round(TileEntityFurnace.getItemBurnTime(event.getItemStack()));
				final int fuelValue = fuelEvent >= 0 ? 0 : fuelFurnace;
				if (fuelValue > 0) {
					Commons.addTooltip(event.getToolTip(), String.format("§8Fuel to burn %.1f ores", fuelValue / 200.0F));
				}
			} catch (final Exception exception) {
				// no operation
			}
		}
		
		// add ore dictionary names
		if (WarpDriveConfig.TOOLTIP_ADD_ORE_DICTIONARY_NAME.isEnabled(isSneaking, isCreativeMode)) {
			final int[] idOres = OreDictionary.getOreIDs(event.getItemStack());
			if (idOres.length != 0) {
				Commons.addTooltip(event.getToolTip(), "Ore dictionary names:");
				for (final int idOre : idOres) {
					final String nameOre = OreDictionary.getOreName(idOre);
					Commons.addTooltip(event.getToolTip(), "- " + nameOre);
				}
			}
		}
	}
	
	// remove redundant information in tooltips
	private static void cleanupTooltip(@Nonnull final List<String> list, final boolean isSneaking, final boolean isCreativeMode) {
		// skip empty tooltip
		if (list.isEmpty()) {
			return;
		}
		
		// skip if disabled
		if (!WarpDriveConfig.TOOLTIP_ENABLE_DEDUPLICATION.isEnabled(isSneaking, isCreativeMode)) {
			return;
		}
		
		// remove duplicates
		final HashSet<String> setClean = new HashSet<>(list.size());
		Iterator<String> iterator = list.iterator();
		while (iterator.hasNext()) {
			final String original = iterator.next();
			final String clean = Commons.removeFormatting(original).trim().toLowerCase();
			if (clean.isEmpty()) {
				continue;
			}
			
			boolean doRemove = setClean.contains(clean);
			for (final String key : WarpDriveConfig.TOOLTIP_CLEANUP_LIST) {
				if (clean.contains(key)) {
					doRemove = true;
					break;
				}
			}
			if (doRemove) {
				iterator.remove();
			} else {
				setClean.add(clean);
			}
		}
		
		// remove extra separator lines that might be resulting from the cleanup (i.e. 2 consecutive empty lines or a final empty line)
		boolean wasEmpty = false;
		iterator = list.iterator();
		while (iterator.hasNext()) {
			final String original = iterator.next();
			final String clean = Commons.removeFormatting(original).trim();
			// keep line with content or at least 4 spaces (for mods adding image overlays)
			if ( !clean.isEmpty()
			     || original.length() > 4 ) {
				wasEmpty = false;
				continue;
			}
			// only keep first empty line in a sequence
			// always remove the last line when it's empty
			if ( wasEmpty
			     || !iterator.hasNext() ) {
				iterator.remove();
			}
			wasEmpty = true;
		}
	}
	
	@SideOnly(Side.CLIENT)
	@SuppressWarnings("deprecation")
	private static void addBlockDetails(@Nonnull final ItemTooltipEvent event, final boolean isSneaking, final boolean isCreativeMode, final Block block) {
		// item registry name
		final ResourceLocation registryNameItem = event.getItemStack().getItem().getRegistryName();
		if (registryNameItem == null) {
			Commons.addTooltip(event.getToolTip(), "§4Invalid item with no registry name!");
			return;
		}
		
		// registry name
		if (WarpDriveConfig.TOOLTIP_ADD_REGISTRY_NAME.isEnabled(isSneaking, isCreativeMode)) {
			try {
				final ResourceLocation registryNameBlock = Block.REGISTRY.getNameForObject(block);
				// noinspection ConstantConditions
				if (registryNameBlock != null) {
					Commons.addTooltip(event.getToolTip(), "§8" + registryNameBlock);
				}
			} catch (final Exception exception) {
				// no operation
			}
		}
		
		// material stats
		final IBlockState blockState = getStateForPlacement(block,
		                                                    null, null,
		                                                    EnumFacing.DOWN, 0.0F, 0.0F, 0.0F, event.getItemStack().getMetadata(),
		                                                    null, EnumHand.MAIN_HAND);
		if (WarpDriveConfig.TOOLTIP_ADD_BLOCK_MATERIAL.isEnabled(isSneaking, isCreativeMode)) {
			try {
				final Material material = blockState.getMaterial();
				final String name = Commons.format(material);
				Commons.addTooltip(event.getToolTip(), String.format("§8Material is %s",
				                                                     name ));
			} catch (final Exception exception) {
				// no operation
			}
		}
		
		// tool related stats
		if (WarpDriveConfig.TOOLTIP_ADD_HARVESTING.isEnabled(isSneaking, isCreativeMode)) {
			try {
				final String harvestTool = block.getHarvestTool(blockState);
				if (harvestTool != null) {
					Commons.addTooltip(event.getToolTip(), String.format("Harvest with %s (%d)",
					                                                     harvestTool, 
					                                                     block.getHarvestLevel(blockState)));
				}
			} catch (final Exception exception) {
				// no operation
			}
		}
		
		// generic properties
		if ( WarpDriveConfig.TOOLTIP_ADD_OPACITY.isEnabled(isSneaking, isCreativeMode)
		  && blockState != null ) {
			try {
				Commons.addTooltip(event.getToolTip(), String.format("§8Light opacity is %s",
				                                                     block.getLightOpacity(blockState)));
				if (WarpDrive.isDev) {
					Commons.addTooltip(event.getToolTip(), String.format("§8isFullBlock is %s",
					                                                     block.isFullBlock(blockState)));
					Commons.addTooltip(event.getToolTip(), String.format("§8isFullCube is %s",
					                                                     block.isFullCube(blockState)));
					Commons.addTooltip(event.getToolTip(), String.format("§8isTopSolid is %s",
					                                                     block.isTopSolid(blockState)));
					Commons.addTooltip(event.getToolTip(), String.format("§8isBlockNormalCube is %s",
					                                                     block.isBlockNormalCube(blockState)));
					Commons.addTooltip(event.getToolTip(), String.format("§8isNormalCube is %s",
					                                                     block.isNormalCube(blockState)));
					Commons.addTooltip(event.getToolTip(), String.format("§8causesSuffocation is %s",
					                                                     block.causesSuffocation(blockState)));
				}
			} catch (final Exception exception) {
				// no operation
			}
		}
		
		if (WarpDriveConfig.TOOLTIP_ADD_HARDNESS.isEnabled(isSneaking, isCreativeMode)) {
			final FakeWorld fakeWorld = new FakeWorld(blockState, true);
			try {
				final float hardness1 = blockState.getBlockHardness(fakeWorld, BlockPos.ORIGIN);
				final float hardness2 = block.blockHardness;
				if ( hardness2 == 0.0F
				  || hardness1 == hardness2 ) {
					Commons.addTooltip(event.getToolTip(), String.format("§8Hardness is %.1f",
					                                                     hardness1 ));
				} else {
					Commons.addTooltip(event.getToolTip(), String.format("§8Hardness is %.1f (%.1f)",
					                                                     hardness1, hardness2 ));
				}
			} catch (final Exception exception) {
				// no operation
			}
			try {
				final float resistance1 = block.getExplosionResistance(fakeWorld, BlockPos.ORIGIN, null, null);
				final float resistance2 = block.getExplosionResistance(null);
				if ( resistance2 == 0.0F
				  || resistance1 == resistance2 ) {
					Commons.addTooltip(event.getToolTip(), String.format("§8Explosion resistance is %.1f",
					                                                     resistance1 ));
				} else {
					Commons.addTooltip(event.getToolTip(), String.format("§8Explosion resistance is %.1f (%.1f)",
					                                                     resistance1, resistance2 ));
				}
			} catch (final Exception exception) {
				// no operation
			}
		}
		
		// flammability
		if (WarpDriveConfig.TOOLTIP_ADD_FLAMMABILITY.isEnabled(isSneaking, isCreativeMode)) {
			try {
				final int flammability = Blocks.FIRE.getFlammability(block);
				final int fireSpread = Blocks.FIRE.getEncouragement(block);
				if (flammability > 0) {
					Commons.addTooltip(event.getToolTip(), String.format("§8Flammability is %d, spread %d",
					                                                     flammability, fireSpread));
				}
			} catch (final Exception exception) {
				// no operation
			}
		}
		
		// fluid stats
		if (WarpDriveConfig.TOOLTIP_ADD_FLUID.isEnabled(isSneaking, isCreativeMode)) {
			try {
				final Fluid fluid = FluidRegistry.lookupFluidForBlock(block);
				if (fluid != null) {
					if (fluid.isGaseous()) {
						Commons.addTooltip(event.getToolTip(), String.format("Gas viscosity is %d",
						                                                     fluid.getViscosity()));
						Commons.addTooltip(event.getToolTip(), String.format("Gas density is %d",
						                                                     fluid.getDensity()));
					} else {
						Commons.addTooltip(event.getToolTip(), String.format("Liquid viscosity is %d",
						                                                     fluid.getViscosity()));
						Commons.addTooltip(event.getToolTip(), String.format("Liquid density is %d",
						                                                     fluid.getDensity()));
					}
					Commons.addTooltip(event.getToolTip(), String.format("Temperature is %d K",
					                                                     fluid.getTemperature()));
				}
			} catch (final Exception exception) {
				// no operation
			}
		}
	}
	
	// Some mods read tooltips server side for searching through them.
	// However, world & placer are unknown on server side, so we're defaulting to null which doesn't always work (see stairs for example).
	// IC2 is throwing assertions due to a bug somewhere...
	public static IBlockState getStateForPlacement(final Block block,
	                                               final World world, final BlockPos blockPos, @Nonnull final EnumFacing facing,
	                                               final float hitX, final float hitY, final float hitZ, final int metadata,
	                                               final EntityLivingBase entityLivingBase, @Nonnull final EnumHand enumHand) {
		final BlockPos blockPosToUse = blockPos != null ? blockPos : blockPosDummy;
		final IBlockState blockState;
		if ( (world != null && world.isRemote)
		  || Thread.currentThread().getName().equals("Client thread") ) {// we're on client side, we can fallback to current player
			blockState = getStateForPlacement_client(block,
			                                         world, blockPosToUse, facing,
			                                         hitX, hitY, hitZ, metadata,
			                                         entityLivingBase, enumHand);
		} else {// we're server side, we try to be more 'flexible'
			blockState = getStateForPlacement_safely(block,
			                                         world, blockPosToUse, facing,
			                                         hitX, hitY, hitZ, metadata,
			                                         entityLivingBase, enumHand);
		}
		return blockState;
	}
	
	@SideOnly(Side.CLIENT)
	private static IBlockState getStateForPlacement_client(final Block block,
	                                                       final World world, final BlockPos blockPos, @Nonnull final EnumFacing facing,
	                                                       final float hitX, final float hitY, final float hitZ, final int metadata,
	                                                       final EntityLivingBase entityLivingBase, @Nonnull final EnumHand enumHand) {
		final EntityLivingBase entityLivingBaseToUse = entityLivingBase != null ? entityLivingBase : Minecraft.getMinecraft().player;
		final World worldToUse = world != null ? world : entityLivingBase != null ? entityLivingBaseToUse.getEntityWorld() : null;
		// during boot, even on client, player isn't defined yet, so we fallback to the server 'flexible' approach
		return getStateForPlacement_safely(block,
		                                   worldToUse, blockPos, facing,
		                                   hitX, hitY, hitZ, metadata,
		                                   entityLivingBaseToUse, enumHand);
	}
	
	@SuppressWarnings("deprecation")
	private static IBlockState getStateForPlacement_safely(final Block block,
	                                                       final World world, final BlockPos blockPos, @Nonnull final EnumFacing facing,
	                                                       final float hitX, final float hitY, final float hitZ, final int metadata,
	                                                       final EntityLivingBase entityLivingBase, @Nonnull final EnumHand enumHand) {
		IBlockState blockState;
		try {
			// world or entity might be null at this point
			blockState = block.getStateForPlacement(world, blockPos, facing,
			                                        hitX, hitY, hitZ, metadata,
			                                        entityLivingBase, enumHand);
		} catch (final Exception | AssertionError throwable1) {
			try {
				// first fallback in case world was really needed...
				blockState = block.getStateFromMeta(metadata);
				
				// just report in dev if a mandatory value was null
				if (WarpDrive.isDev && (world == null || entityLivingBase == null)) {
					WarpDrive.logger.debug(String.format("Exception getting block state from block %s (%s %d): %s",
					                                     block,
					                                     block.getRegistryName(),
					                                     metadata,
					                                     throwable1));
				}
			} catch (final Exception | AssertionError throwable2) {
				WarpDrive.logger.error(String.format("Exception getting block state from item stack %s (%s %d): %s",
				                                     block,
				                                     block.getRegistryName(),
				                                     metadata,
				                                     throwable1 ));
				throwable1.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.error(String.format("followed by %s",
				                                     throwable2 ));
				throwable2.printStackTrace(WarpDrive.printStreamError);
				// Final fallback to default
				blockState = block.getDefaultState();
			}
		}
		return blockState;
	}
	
	@SideOnly(Side.CLIENT)
	private static void addItemDetails(final ItemTooltipEvent event, final boolean isSneaking, final boolean isCreativeMode, @Nonnull final ItemStack itemStack) {
		final Item item = itemStack.getItem();
		
		// registry name
		if (WarpDriveConfig.TOOLTIP_ADD_REGISTRY_NAME.isEnabled(isSneaking, isCreativeMode)) {
			try {
				final ResourceLocation registryNameItem = Item.REGISTRY.getNameForObject(itemStack.getItem());
				if (registryNameItem == null) {
					Commons.addTooltip(event.getToolTip(), "§4Invalid item with no registry name!");
					return;
				}
				Commons.addTooltip(event.getToolTip(), "§8" + registryNameItem);
			} catch (final Exception exception) {
				// no operation
			}
		}
		
		// (item duration can't be directly understood => out)
		
		// durability
		if (WarpDriveConfig.TOOLTIP_ADD_DURABILITY.isEnabled(isSneaking, isCreativeMode)) {
			try {
				if (itemStack.isItemStackDamageable()) {
					Commons.addTooltip(event.getToolTip(), String.format("Durability: %d / %d",
					                                                     itemStack.getMaxDamage() - itemStack.getItemDamage(),
					                                                     itemStack.getMaxDamage() ));
				}
			} catch (final Exception exception) {
				// no operation
			}
		}
		
		// armor points
		if (WarpDriveConfig.TOOLTIP_ADD_ARMOR_POINTS.isEnabled(isSneaking, isCreativeMode)) {
			try {
				if (item instanceof ItemArmor) {
					Commons.addTooltip(event.getToolTip(), String.format("§8Armor points is %d",
					                                                     ((ItemArmor) item).damageReduceAmount ));
				}
			} catch (final Exception exception) {
				// no operation
			}
		}
		
		// harvesting stats
		if (WarpDriveConfig.TOOLTIP_ADD_HARVESTING.isEnabled(isSneaking, isCreativeMode)) {
			try {
				final Set<String> toolClasses = item.getToolClasses(itemStack);
				for (final String toolClass : toolClasses) {
					final int harvestLevel = item.getHarvestLevel(itemStack, toolClass, event.getEntityPlayer(), null);
					if (harvestLevel == -1) {// (invalid tool class)
						continue;
					}
					Commons.addTooltip(event.getToolTip(), String.format("§8Tool class is %s (%d)",
					                                                     toolClass, harvestLevel ));
				}
			} catch (final Exception exception) {
				// no operation
			}
		}
		
		// enchantability
		if (WarpDriveConfig.TOOLTIP_ADD_ENCHANTABILITY.isEnabled(isSneaking, isCreativeMode)) {
			final int enchantability = item.getItemEnchantability();
			if (enchantability > 0) {
				Commons.addTooltip(event.getToolTip(), String.format("§8Enchantability is %d",
				                                                     enchantability));
			}
		}
		
		// repair material
		if (WarpDriveConfig.TOOLTIP_ADD_REPAIR_WITH.isEnabled(isSneaking, isCreativeMode)) {
			try {
				// get the default repair material
				final ItemStack itemStackRepair;
				if (item instanceof ItemArmor) {
					final ArmorMaterial armorMaterial = ((ItemArmor) item).getArmorMaterial();
					itemStackRepair = armorMaterial.getRepairItemStack();
					
				} else if (item instanceof ItemTool) {
					final String nameMaterial = ((ItemTool) item).getToolMaterialName();
					final ToolMaterial toolMaterial = ToolMaterial.valueOf(nameMaterial);
					
					itemStackRepair = toolMaterial.getRepairItemStack();
					
				} else if (item instanceof ItemSword) {
					final String nameMaterial = ((ItemSword) item).getToolMaterialName();
					final ToolMaterial toolMaterial = ToolMaterial.valueOf(nameMaterial);
					
					itemStackRepair = toolMaterial.getRepairItemStack();
					
				} else {
					itemStackRepair = ItemStack.EMPTY;
				}
				
				// add tooltip
				if (!itemStackRepair.isEmpty()) {
					Commons.addTooltip(event.getToolTip(), String.format("§8Repairable with %s",
					                                                     new TextComponentTranslation(itemStackRepair.getTranslationKey() + ".name").getFormattedText() ));
				}
				
			} catch (final Exception exception) {
				// no operation
			}
		}
		
		// entity data
		if (item instanceof ItemMonsterPlacer) {
			if (WarpDriveConfig.TOOLTIP_ADD_ENTITY_ID.isEnabled(isSneaking, isCreativeMode)) {
				final ResourceLocation entityId = ItemMonsterPlacer.getNamedIdFrom(itemStack);
				if (entityId != null) {
					Commons.addTooltip(event.getToolTip(), String.format("Entity is %s",
					                                                     entityId));
				} else {
					Commons.addTooltip(event.getToolTip(), "Entity is §4-undefined");
				}
			}
		}
	}
}
