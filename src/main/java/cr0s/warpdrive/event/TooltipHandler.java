package cr0s.warpdrive.event;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.world.FakeWorld;

import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityType;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.item.TieredItem;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.registries.ForgeRegistries;

import gregtech.api.items.IToolItem;

public class TooltipHandler {
	
	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onTooltipEvent_first(@Nonnull final ItemTooltipEvent event) {
		if (event.getPlayer() == null) {
			return;
		}
		if (event.getItemStack().isEmpty()) {
			return;
		}
		
		// add dictionary information
		if (Dictionary.ITEMS_BREATHING_HELMET.contains(event.getItemStack().getItem())) {
			Commons.addTooltip(event.getToolTip(), new TranslationTextComponent("warpdrive.tooltip.item_tag.breathing_helmet").getFormattedText());
		}
		if (Dictionary.ITEMS_FLYINSPACE.contains(event.getItemStack().getItem())) {
			Commons.addTooltip(event.getToolTip(), new TranslationTextComponent("warpdrive.tooltip.item_tag.fly_in_space").getFormattedText());
		}
		if (Dictionary.ITEMS_NOFALLDAMAGE.contains(event.getItemStack().getItem())) {
			Commons.addTooltip(event.getToolTip(), new TranslationTextComponent("warpdrive.tooltip.item_tag.no_fall_damage").getFormattedText());
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onTooltipEvent_last(@Nonnull final ItemTooltipEvent event) {
		if (event.getPlayer() == null) {
			return;
		}
		if (event.getItemStack().isEmpty()) {
			return;
		}
		
		// note: event.getPlayer().isSneaking() remains false inside GUIs, so we ask directly the driver
		final KeyBinding keyBindingSneak = Minecraft.getInstance().gameSettings.keyBindSneak;
		final boolean isSneaking = Commons.isKeyPressed(keyBindingSneak);
		final boolean isCreativeMode = event.getPlayer().isCreative();
		
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
				final int fuelRaw = Math.round(ForgeHooks.getBurnTime(event.getItemStack()));
				final int fuelValue = ForgeEventFactory.getItemBurnTime(event.getItemStack(), fuelRaw);
				if (fuelValue > 0) {
					Commons.addTooltip(event.getToolTip(), String.format("§8Fuel to burn %.1f ores", fuelValue / 200.0F));
				}
			} catch (final Exception exception) {
				// no operation
			}
		}
		
		// add ore dictionary names
		if (WarpDriveConfig.TOOLTIP_ADD_ORE_DICTIONARY_NAME.isEnabled(isSneaking, isCreativeMode)) {
			final Collection<ResourceLocation> tags = ItemTags.getCollection().getOwningTags(event.getItemStack().getItem());
			if (!tags.isEmpty()) {
				Commons.addTooltip(event.getToolTip(), "Tags:");
				for (final ResourceLocation tag : tags) {
					Commons.addTooltip(event.getToolTip(), "- " + tag);
				}
			}
		}
	}
	
	// remove redundant information in tooltips
	private static void cleanupTooltip(@Nonnull final List<ITextComponent> list, final boolean isSneaking, final boolean isCreativeMode) {
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
		Iterator<ITextComponent> iterator = list.iterator();
		while (iterator.hasNext()) {
			final String original = iterator.next().getFormattedText();
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
			final String original = iterator.next().getFormattedText();
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
	
	@OnlyIn(Dist.CLIENT)
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
				final ResourceLocation registryNameBlock = ForgeRegistries.BLOCKS.getKey(block);
				if (registryNameBlock != null) {
					Commons.addTooltip(event.getToolTip(), "§8" + registryNameBlock);
				}
			} catch (final Exception exception) {
				// no operation
			}
		}
		
		// material stats
		final BlockState blockState = block.getDefaultState();
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
				final ToolType harvestTool = block.getHarvestTool(blockState);
				if (harvestTool != null) {
					Commons.addTooltip(event.getToolTip(), String.format("Harvest with %s (%d)",
					                                                     harvestTool.getName(), 
					                                                     block.getHarvestLevel(blockState)));
				}
			} catch (final Exception exception) {
				// no operation
			}
		}
		
		// generic properties
		if (WarpDriveConfig.TOOLTIP_ADD_OPACITY.isEnabled(isSneaking, isCreativeMode)) {
			final FakeWorld fakeWorld = new FakeWorld(blockState, true);
			try {
				Commons.addTooltip(event.getToolTip(), String.format("§8Light opacity is %s",
				                                                     block.getOpacity(blockState, fakeWorld, BlockPos.ZERO)));
				if (WarpDrive.isDev) {
					Commons.addTooltip(event.getToolTip(), String.format("§8isViewBlocking is %s",
					                                                     block.isViewBlocking(blockState, fakeWorld, BlockPos.ZERO) ));
					Commons.addTooltip(event.getToolTip(), String.format("§8isVariableOpacity is %s",
					                                                     block.isVariableOpacity() ));
					Commons.addTooltip(event.getToolTip(), String.format("§8isTransparent is %s",
					                                                     block.isTransparent(blockState) ));
					Commons.addTooltip(event.getToolTip(), String.format("§8isSolid is %s",
					                                                     block.isSolid(blockState) ));
					Commons.addTooltip(event.getToolTip(), String.format("§8isNormalCube is %s",
					                                                     block.isNormalCube(blockState, fakeWorld, BlockPos.ZERO) ));
					Commons.addTooltip(event.getToolTip(), String.format("§8causesSuffocation is %s",
					                                                     block.causesSuffocation(blockState, fakeWorld, BlockPos.ZERO) ));
				}
			} catch (final Exception exception) {
				// no operation
			}
		}
		
		if (WarpDriveConfig.TOOLTIP_ADD_HARDNESS.isEnabled(isSneaking, isCreativeMode)) {
			final FakeWorld fakeWorld = new FakeWorld(blockState, true);
			try {
				final float hardness1 = blockState.getBlockHardness(fakeWorld, BlockPos.ZERO);
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
				final float resistance1 = block.getExplosionResistance(blockState, fakeWorld, BlockPos.ZERO, null, null);
				final float resistance2 = block.getExplosionResistance();
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
			final FakeWorld fakeWorld = new FakeWorld(blockState, true);
			try {
				final int flammability = Blocks.FIRE.getFlammability(blockState, fakeWorld, BlockPos.ZERO, null);
				final int fireSpread = Blocks.FIRE.getFireSpreadSpeed(blockState, fakeWorld, BlockPos.ZERO, null);
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
				final Fluid fluid = block instanceof FlowingFluidBlock ? ((FlowingFluidBlock) block).getFluid().getFlowingFluid()
				                                                       : block instanceof IFluidBlock ? ((IFluidBlock) block).getFluid()
				                                                                                      : null;
				if (fluid != null) {
					if (fluid.getAttributes().isGaseous()) {
						Commons.addTooltip(event.getToolTip(), String.format("Gas viscosity is %d",
						                                                     fluid.getAttributes().getViscosity() ));
						Commons.addTooltip(event.getToolTip(), String.format("Gas density is %d",
						                                                     fluid.getAttributes().getDensity() ));
					} else {
						Commons.addTooltip(event.getToolTip(), String.format("Liquid viscosity is %d",
						                                                     fluid.getAttributes().getViscosity()));
						Commons.addTooltip(event.getToolTip(), String.format("Liquid density is %d",
						                                                     fluid.getAttributes().getDensity() ));
					}
					Commons.addTooltip(event.getToolTip(), String.format("Temperature is %d K",
					                                                     fluid.getAttributes().getTemperature() ));
				}
			} catch (final Exception exception) {
				// no operation
			}
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	private static void addItemDetails(final ItemTooltipEvent event, final boolean isSneaking, final boolean isCreativeMode, @Nonnull final ItemStack itemStack) {
		final Item item = itemStack.getItem();
		
		// registry name
		if (WarpDriveConfig.TOOLTIP_ADD_REGISTRY_NAME.isEnabled(isSneaking, isCreativeMode)) {
			try {
				final ResourceLocation registryNameItem = ForgeRegistries.ITEMS.getKey(itemStack.getItem());
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
				if (WarpDriveConfig.isGregtechLoaded) {
					if (itemStack.getItem() instanceof IToolItem) {
						final IToolItem toolItem = (IToolItem) itemStack.getItem();
						final int itemDamage = toolItem.getItemDamage(itemStack);
						final int maxDamage = toolItem.getMaxItemDamage(itemStack);
						Commons.addTooltip(event.getToolTip(), String.format("Durability: %d / %d",
						                                                     maxDamage - itemDamage,
						                                                     maxDamage));
					}
				}
				if (itemStack.isDamageable()) {
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
				if (item instanceof ArmorItem) {
					Commons.addTooltip(event.getToolTip(), String.format("§8Armor points is %d",
					                                                     ((ArmorItem) item).getDamageReduceAmount() ));
				}
			} catch (final Exception exception) {
				// no operation
			}
		}
		
		// harvesting stats
		if (WarpDriveConfig.TOOLTIP_ADD_HARVESTING.isEnabled(isSneaking, isCreativeMode)) {
			try {
				final Set<ToolType> toolTypes = item.getToolTypes(itemStack);
				for (final ToolType toolType : toolTypes) {
					final int harvestLevel = item.getHarvestLevel(itemStack, toolType, event.getPlayer(), null);
					if (harvestLevel == -1) {// (invalid tool class)
						continue;
					}
					Commons.addTooltip(event.getToolTip(), String.format("§8Tool class is %s (%d)",
					                                                     toolType, harvestLevel ));
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
				final Ingredient ingredientRepair;
				if (item instanceof ArmorItem) {
					final IArmorMaterial armorMaterial = ((ArmorItem) item).getArmorMaterial();
					ingredientRepair = armorMaterial.getRepairMaterial();
					
				} else if (item instanceof TieredItem) {
					ingredientRepair = ((TieredItem) item).getTier().getRepairMaterial();
					
				} else {
					ingredientRepair = Ingredient.EMPTY;
				}
				
				// add tooltip
				if (!ingredientRepair.hasNoMatchingItems()) {
					final ItemStack itemStackRepairMaterial = ingredientRepair.getMatchingStacks()[0];
					Commons.addTooltip(event.getToolTip(), String.format("§8Repairable with %s",
					                                                     itemStackRepairMaterial.getDisplayName().getFormattedText() ));
				}
				
			} catch (final Exception exception) {
				// no operation
			}
		}
		
		// entity data
		if (WarpDriveConfig.TOOLTIP_ADD_ENTITY_ID.isEnabled(isSneaking, isCreativeMode)) {
			if (item instanceof SpawnEggItem) {
				try {
					final EntityType<?> entityType = ((SpawnEggItem) item).getType(itemStack.getTag());
					Commons.addTooltip(event.getToolTip(), String.format("Entity is %s",
					                                                     entityType.getName() ));
				} catch (final Exception exception) {
					Commons.addTooltip(event.getToolTip(), "Entity is §4-invalid-");
				}
			}
		}
	}
}
