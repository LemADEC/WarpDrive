package cr0s.warpdrive.config;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.ParticleRegistry;
import cr0s.warpdrive.block.decoration.BlockDecorative;
import cr0s.warpdrive.data.EnumAirTankTier;
import cr0s.warpdrive.data.EnumComponentType;
import cr0s.warpdrive.data.EnumDecorativeType;
import cr0s.warpdrive.data.EnumForceFieldShape;
import cr0s.warpdrive.data.EnumForceFieldUpgrade;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.item.ItemComponent;
import cr0s.warpdrive.item.ItemElectromagneticCell;
import cr0s.warpdrive.item.ItemForceFieldShape;
import cr0s.warpdrive.item.ItemForceFieldUpgrade;
import cr0s.warpdrive.item.ItemTuningDriver;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.DyeColor;

import net.minecraftforge.common.crafting.NBTIngredient;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonSyntaxException;

/**
 * Hold the different recipe sets
 */
public class Recipes {
	
	private static final String groupComponents   = "components";
	private static final String groupDecorations  = "decoration";
	private static final String groupMachines     = "machines";
	private static final String groupTools        = "tools";
	
	private static final String groupHulls        = "hulls";
	private static final String groupTaintedHulls = "tainted_hulls";
	
	public static final HashMap<DyeColor, String> oreDyes = new HashMap<>(16);
	static {
		oreDyes.put(DyeColor.WHITE     , "forge:dyes/white");
		oreDyes.put(DyeColor.ORANGE    , "forge:dyes/orange");
		oreDyes.put(DyeColor.MAGENTA   , "forge:dyes/magenta");
		oreDyes.put(DyeColor.LIGHT_BLUE, "forge:dyes/light_blue");
		oreDyes.put(DyeColor.YELLOW    , "forge:dyes/yellow");
		oreDyes.put(DyeColor.LIME      , "forge:dyes/lime");
		oreDyes.put(DyeColor.PINK      , "forge:dyes/pink");
		oreDyes.put(DyeColor.GRAY      , "forge:dyes/gray");
		oreDyes.put(DyeColor.LIGHT_GRAY, "forge:dyes/light_gray");
		oreDyes.put(DyeColor.CYAN      , "forge:dyes/cyan");
		oreDyes.put(DyeColor.PURPLE    , "forge:dyes/purple");
		oreDyes.put(DyeColor.BLUE      , "forge:dyes/blue");
		oreDyes.put(DyeColor.BROWN     , "forge:dyes/brown");
		oreDyes.put(DyeColor.GREEN     , "forge:dyes/green");
		oreDyes.put(DyeColor.RED       , "forge:dyes/red");
		oreDyes.put(DyeColor.BLACK     , "forge:dyes/black");
	}
	
	private static ItemStack[] itemStackMachineCasings;
	private static ItemStack[] itemStackMotors;
	private static Object barsIron;
	private static Object ingotIronOrSteel;
	private static Object rubber;
	private static Object goldNuggetOrBasicCircuit;
	private static Object goldIngotOrAdvancedCircuit;
	private static Object emeraldOrSuperiorCircuit;
	
	@Nonnull
	public static ResourceLocation buildRecipeId(@Nonnull final String suffix, @Nonnull final ItemStack itemStackOutput) {
		final ResourceLocation resourceLocationItem = itemStackOutput.getItem().getRegistryName();
		assert resourceLocationItem != null;
		
		final String path;
		assert itemStackOutput.getItem().getRegistryName() != null;
		if (itemStackOutput.isEmpty()) {
			assert false;
			path = "invalid";
		} else if (itemStackOutput.getCount() == 1) {
			path = String.format("%s_%d%s",
			                     itemStackOutput.getItem().getRegistryName().getPath(),
			                     itemStackOutput.getDamage(),
			                     suffix );
		} else {
			path = String.format("%s_%dx%d%s",
			                     itemStackOutput.getItem().getRegistryName().getPath(),
			                     itemStackOutput.getDamage(),
			                     itemStackOutput.getCount(),
			                     suffix );
		}
		return new ResourceLocation(WarpDrive.MODID, path);
	}
	
	static class ItemStackIngredient extends NBTIngredient {
		ItemStackIngredient(@Nonnull final ItemStack itemStack) {
			super(itemStack);
		}
	}
	
	private static void registerShapelessRecipe(@Nonnull final String groupName,
	                                            @Nonnull final ItemStack itemStackOutput, @Nonnull final Object... rawIngredients) {
		registerShapelessRecipe(groupName, "", itemStackOutput, rawIngredients);
	}
	
	private static Ingredient getIngredient(@Nonnull final String context, @Nullable final Object rawIngredient) {
		if (rawIngredient instanceof ItemStack) {
			return new ItemStackIngredient((ItemStack) rawIngredient);
		} else if (rawIngredient instanceof Item) {
			return new ItemStackIngredient(new ItemStack((Item) rawIngredient));
		} else if (rawIngredient instanceof Block) {
			return new ItemStackIngredient(new ItemStack((Block) rawIngredient));
		} else if (rawIngredient instanceof String) {
			// TODO MC1.15 tagged ingredients
			return new ItemStackIngredient(new ItemStack(Blocks.FIRE));
		} else if (rawIngredient == null) {
			WarpDrive.logger.warn(String.format("Skipping %s due to a null ingredient...", context));
			return null;
		}
		
		WarpDrive.logger.error(String.format("Skipping %s due to an invalid ingredient type %s", context, rawIngredient.getClass().toString()));
		return null;
	}
	
	private static void registerShapelessRecipe(@Nonnull final String groupName, @Nonnull final String suffix,
	                                            @Nonnull final ItemStack itemStackOutput, @Nonnull final Object... rawIngredients) {
		if (itemStackOutput.getItem() == Items.AIR) {
			WarpDrive.logger.warn("Skipping shapeless recipe with invalid air output...");
			return;
		}
		final String context = String.format("shapeless recipe with %s output", itemStackOutput);
		final NonNullList<Ingredient> nnlIngredients = NonNullList.create();
		for (final Object rawIngredient : rawIngredients) {
			final Ingredient ingredient = getIngredient(context, rawIngredient);
			if (ingredient == null) {
				return;
			}
			nnlIngredients.add(ingredient);
		}
		WarpDrive.register(new ShapelessRecipe(buildRecipeId(suffix, itemStackOutput), groupName, itemStackOutput, nnlIngredients));
	}
	
	@Nullable
	private static NonNullList<Ingredient> parseShaped(@Nonnull final String context, @Nonnull final Object... recipe) {
		final boolean isMirrored;
		int width = 0;
		int height = 0;
		String shape = "";
		int index = 0;
		
		// first entry is optional isMirrored boolean, defaults to false
		if (recipe[index] instanceof Boolean) {
			isMirrored = (Boolean) recipe[index];
			index = 1;
		} else {
			isMirrored = false;
		}
		
		// then the recipe shape as an array, or inline
		if (recipe[index] instanceof String[]) {
			final String[] parts = ((String[]) recipe[index++]);
			
			for (String part : parts) {
				width = part.length();
				shape += part;
			}
			
			height = parts.length;
		} else {
			while (recipe[index] instanceof String) {
				final String part = (String) recipe[index++];
				shape += part;
				width = part.length();
				height++;
			}
		}
		// validate shape
		if ( width * height != shape.length()
		  || shape.length() == 0 ) {
			String err = "Invalid shaped recipe: ";
			for (Object tmp : recipe) {
				err += tmp + ", ";
			}
			throw new RuntimeException(err);
		}
		
		// then the ingredients
		final HashMap<Character, Ingredient> mapIngredients = Maps.newHashMap();
		mapIngredients.put(' ', Ingredient.EMPTY);
		
		for (; index < recipe.length; index += 2) {
			final Character symbol = (Character) recipe[index];
			if (symbol == ' ') {
				throw new JsonSyntaxException("Invalid key entry: ' ' is a reserved symbol.");
			}
			
			final Object rawIngredient = recipe[index + 1];
			final Ingredient ingredient = getIngredient(context, rawIngredient);
			if (ingredient == null) {
				return null;
			}
			
			mapIngredients.put(symbol, ingredient);
		}
		
		assert !isMirrored; // TODO mirrored recipes aren't supported
		final NonNullList<Ingredient> nnlIngredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
		
		final Set<Character> symbols = Sets.newHashSet(mapIngredients.keySet());
		symbols.remove(' ');
		
		int indexIngredient = 0;
		for (final char symbol : shape.toCharArray()) {
			final Ingredient ingredient = mapIngredients.get(symbol);
			if (ingredient == null) {
				throw new IllegalArgumentException("Pattern references symbol '" + symbol + "' but it's not defined in the key");
			}
			nnlIngredients.set(indexIngredient++, ingredient);
			symbols.remove(symbol);
		}
		
		if (!symbols.isEmpty()) {
			throw new IllegalArgumentException("At least one symbol is defined but not used in pattern: " + symbols);
		}
		
		return nnlIngredients;
	}
	
	private static void registerShapedRecipe(@Nonnull final String groupName,
	                                         @Nonnull final Block blockOutput, @Nonnull final Object... recipe) {
		registerShapedRecipe(groupName, "", new ItemStack(blockOutput), recipe);
	}
	
	private static void registerShapedRecipe(@Nonnull final String groupName,
	                                         @Nonnull final Item itemOutput, @Nonnull final Object... recipe) {
		registerShapedRecipe(groupName, "", new ItemStack(itemOutput), recipe);
	}
	
	private static void registerShapedRecipe(@Nonnull final String groupName,
	                                         @Nonnull final ItemStack itemStackOutput, @Nonnull final Object... recipe) {
		registerShapedRecipe(groupName, "", itemStackOutput, recipe);
	}
	
	private static void registerShapedRecipe(@Nonnull final String groupName, @Nonnull final String suffix,
	                                         @Nonnull final Block blockOutput, @Nonnull final Object... recipe) {
		registerShapedRecipe(groupName, suffix, new ItemStack(blockOutput), recipe);
	}
	
	private static void registerShapedRecipe(@Nonnull final String groupName, @Nonnull final String suffix,
	                                         @Nonnull final ItemStack itemStackOutput, @Nonnull final Object... recipe) {
		if (itemStackOutput.getItem() == Items.AIR) {
			WarpDrive.logger.warn("Skipping shaped recipe with invalid air output...");
			return;
		}
		final String context = String.format("shaped recipe with %s output", itemStackOutput);
		final NonNullList<Ingredient> nnlIngredients = parseShaped(context, recipe);
		if (nnlIngredients == null) {
			return;
		}
		WarpDrive.register(new ShapedRecipe(buildRecipeId(suffix, itemStackOutput), groupName, 3, 3, nnlIngredients, itemStackOutput));
	}
	
	private static void registerSmeltingRecipe(@Nonnull final ItemStack itemStackInput, @Nonnull final ItemStack itemStackOutput, final float xp) {
		// TODO MC1.15 implement furnace recipes
		// WarpDrive.register(new ShapedRecipe(buildRecipeId("", itemStackOutput), "groupName", 3, 3, nnlIngredients, itemStackOutput));
	}
	
	private static boolean isTagDefined(@Nonnull final String tag) {
		final Tag<Item> itemTagCollection = ItemTags.getCollection().get(new ResourceLocation(tag));
		return itemTagCollection != null
		    && !itemTagCollection.getAllElements().isEmpty();
	}
	
	private static void initIngredients() {
		// Get the machine casing to use
		final ItemStack itemStackMachineCasingLV;
		final ItemStack itemStackMachineCasingMV;
		final ItemStack itemStackMachineCasingHV;
		final ItemStack itemStackMachineCasingEV;
		ItemStack itemStackMotorLV = ItemComponent.getItemStack(EnumComponentType.MOTOR);
		ItemStack itemStackMotorMV = ItemComponent.getItemStack(EnumComponentType.MOTOR);
		ItemStack itemStackMotorHV = ItemComponent.getItemStack(EnumComponentType.MOTOR);
		ItemStack itemStackMotorEV = ItemComponent.getItemStack(EnumComponentType.MOTOR);
		
		if (WarpDriveConfig.isGregtechLoaded) {
			itemStackMachineCasingLV = WarpDriveConfig.getItemStackOrFire("gregtech:machine_casing", 1); // LV machine casing (Steel)
			itemStackMachineCasingMV = WarpDriveConfig.getItemStackOrFire("gregtech:machine_casing", 2); // MV machine casing (Aluminium)
			itemStackMachineCasingHV = WarpDriveConfig.getItemStackOrFire("gregtech:machine_casing", 3); // HV machine casing (Stainless Steel)
			itemStackMachineCasingEV = WarpDriveConfig.getItemStackOrFire("gregtech:machine_casing", 4); // EV machine casing (Titanium)
			
			itemStackMotorLV = WarpDriveConfig.getItemStackOrFire("gregtech:meta_item_1", 127); // LV Motor
			itemStackMotorMV = WarpDriveConfig.getItemStackOrFire("gregtech:meta_item_1", 128); // MV Motor
			itemStackMotorHV = WarpDriveConfig.getItemStackOrFire("gregtech:meta_item_1", 129); // HV Motor
			itemStackMotorEV = WarpDriveConfig.getItemStackOrFire("gregtech:meta_item_1", 130); // EV Motor
			
		} else if (WarpDriveConfig.isIndustrialCraft2Loaded) {
			itemStackMachineCasingLV = (ItemStack) WarpDriveConfig.getOreOrItemStack("ic2:resource", 12,       // IC2 Experimental Basic machine casing
			                                                                         "ic2:blockmachinelv", 0); // IC2 Classic Machine block
			itemStackMachineCasingMV = (ItemStack) WarpDriveConfig.getOreOrItemStack("ic2:resource", 13,       // IC2 Experimental Advanced machine casing
			                                                                         "ic2:blackmachinemv", 0); // IC2 Classic Advanced machine block
			itemStackMachineCasingHV = new ItemStack(WarpDrive.blockHighlyAdvancedMachine);
			itemStackMachineCasingEV = new ItemStack(WarpDrive.blockHighlyAdvancedMachine);
			
			final ItemStack itemStackMotor = WarpDriveConfig.getItemStackOrFire("ic2:crafting", 6); // IC2 Experimental Electric motor
			if (!itemStackMotor.isEmpty()) {
				itemStackMotorHV = itemStackMotor;
				itemStackMotorEV = itemStackMotor;
			}
			
			registerShapedRecipe(groupComponents,
			                                       new ItemStack(WarpDrive.blockHighlyAdvancedMachine), false, "iii", "imi", "iii",
			                                       'i', "forge:plates/iridium_alloy",
			                                       'm', itemStackMachineCasingMV);
			
		} else if (WarpDriveConfig.isThermalFoundationLoaded) {
			// These are upgrade kits, there is only 1 machine frame tier as of Thermal Foundation 1.12.2-5.5.0.29
			itemStackMachineCasingLV = WarpDriveConfig.getItemStackOrFire("thermalfoundation:upgrade", 0);
			itemStackMachineCasingMV = WarpDriveConfig.getItemStackOrFire("thermalfoundation:upgrade", 1);
			itemStackMachineCasingHV = WarpDriveConfig.getItemStackOrFire("thermalfoundation:upgrade", 2);
			itemStackMachineCasingEV = WarpDriveConfig.getItemStackOrFire("thermalfoundation:upgrade", 3);
			
		} else if (WarpDriveConfig.isEnderIOLoaded) {
			// As of EnderIO on MC 1.12.2 there are 5 machine chassis
			itemStackMachineCasingLV = WarpDriveConfig.getItemStackOrFire("enderio:item_material", 0);     // Simple Machine chassis
			itemStackMachineCasingMV = WarpDriveConfig.getItemStackOrFire("enderio:item_material", 1);     // Industrial Machine chassis
			itemStackMachineCasingHV = WarpDriveConfig.getItemStackOrFire("enderio:item_material", 54);    // Enhanced Machine chassis
			itemStackMachineCasingEV = WarpDriveConfig.getItemStackOrFire("enderio:item_material", 55);    // Soulless Machine chassis
			// itemStackMachineCasingEV = WarpDriveConfig.getItemStackOrFire("enderio:item_material", 53);    // Soul Machine chassis
			
		} else {// vanilla
			itemStackMachineCasingLV = new ItemStack(Blocks.IRON_BLOCK);
			itemStackMachineCasingMV = new ItemStack(Blocks.DIAMOND_BLOCK);
			itemStackMachineCasingHV = new ItemStack(WarpDrive.blockHighlyAdvancedMachine);
			itemStackMachineCasingEV = new ItemStack(Blocks.BEACON);
			
			registerShapedRecipe(groupComponents,
			                                       new ItemStack(WarpDrive.blockHighlyAdvancedMachine, 4), "pep", "ede", "pep",
			                                       'e', Items.EMERALD,
			                                       'p', Items.ENDER_EYE,
			                                       'd', Blocks.DIAMOND_BLOCK);
		}
		
		// @TODO implement machine casing from hull + iron bars/etc., gives 2
		
		itemStackMachineCasings = new ItemStack[] { itemStackMachineCasingLV, itemStackMachineCasingMV, itemStackMachineCasingHV, itemStackMachineCasingEV };
		itemStackMotors = new ItemStack[] { itemStackMotorLV, itemStackMotorMV, itemStackMotorHV, itemStackMotorEV };
		
		// integrate with iron bars from all mods
		barsIron = WarpDriveConfig.getOreOrItemStack(
				"forge:bars/iron", 0,
				"minecraft:iron_bars", 0 );
		
		// integrate with steel and aluminium ingots from all mods
		ingotIronOrSteel = WarpDriveConfig.getOreOrItemStack(
				"forge:ingots/steel", 0,
				"forge:ingots/aluminium", 0,
				"forge:ingots/aluminum", 0,
				"forge:ingots/iron", 0 );
		
		// integrate with rubber from all mods
		rubber = WarpDriveConfig.getOreOrItemStack(
				"forge:plates/rubber", 0,       // comes with GregTech
				"forge:rubber", 0 );            // comes with WarpDrive, IndustrialCraft2, IndustrialForegoing, TechReborn
		
		// integrate with circuits from all mods
		goldNuggetOrBasicCircuit = WarpDriveConfig.getOreOrItemStack(
				"forge:circuits/basic", 0,      // comes with IndustrialCraft2, Mekanism, VoltzEngine
				"forge:nuggets/gold", 0 );
		goldIngotOrAdvancedCircuit = WarpDriveConfig.getOreOrItemStack(
				"forge:circuits/advanced", 0,   // comes with IndustrialCraft2, Mekanism, VoltzEngine
				"forge:ingots/gold", 0 );
		emeraldOrSuperiorCircuit = WarpDriveConfig.getOreOrItemStack(
				"forge:circuits/elite", 0,      // comes with Mekanism, VoltzEngine
				"forge:gems/emerald", 0 );
		
		// Iridium block is just that
		if (WarpDriveConfig.isGregtechLoaded) {
			registerShapedRecipe(groupComponents,
			                                       new ItemStack(WarpDrive.blockIridium), "iii", "iii", "iii",
			                                       'i', "forge:plates/iridium");
			final ItemStack itemStackIridiumAlloy = WarpDriveConfig.getOreDictionaryEntry("forge:plates/iridium");
			registerShapelessRecipe(groupComponents,
			                        new ItemStack(itemStackIridiumAlloy.getItem(), 9), new ItemStack(WarpDrive.blockIridium));
			
		} else if (isTagDefined("forge:plates/iridium_alloy")) {// IC2
			registerShapedRecipe(groupComponents,
			                                       new ItemStack(WarpDrive.blockIridium), "iii", "iii", "iii",
			                                       'i', "forge:plates/iridium_alloy");
			final ItemStack itemStackIridiumAlloy = WarpDriveConfig.getOreDictionaryEntry("forge:plates/iridium_alloy");
			registerShapelessRecipe(groupComponents,
			                                          new ItemStack(itemStackIridiumAlloy.getItem(), 9),
			                                          new ItemStack(WarpDrive.blockIridium));
			
		} else if ( WarpDriveConfig.isThermalFoundationLoaded
		         || WarpDriveConfig.isEnderIOLoaded ) {// give alternate options when IC2 & GregTech are missing
			// Warning: because those are alternatives, we're not giving an uncrafting recipe, nor registering the iridium block to the ore dictionary
			
			if (WarpDriveConfig.isThermalFoundationLoaded) {
				registerShapedRecipe(groupComponents, "_thermal",
				                                       new ItemStack(WarpDrive.blockIridium, 2), "ses", "ele", "ses",
				                                       'l', "forge:ingots/lumium",
				                                       's', "forge:ingots/signalum",
				                                       'e', "forge:ingots/enderium");
			}
			
			if (isTagDefined("forge:plates/iridium")) {// ThermalFoundation
				registerShapedRecipe(groupComponents, "_plates",
				                                       new ItemStack(WarpDrive.blockIridium), "iii", "iii", "iii",
				                                       'i', "forge:plates/iridium");
			}
			
			if (WarpDriveConfig.isEnderIOLoaded) {
				final ItemStack itemStackVibrantAlloy = WarpDriveConfig.getItemStackOrFire("enderio:item_alloy_ingot", 2);
				final ItemStack itemStackRedstoneAlloy = WarpDriveConfig.getItemStackOrFire("enderio:item_alloy_ingot", 3);
				final ItemStack itemStackFranckNZombie = WarpDriveConfig.getItemStackOrFire("enderio:item_material", 42);
				registerShapedRecipe(groupComponents, "_enderio",
				                                       new ItemStack(WarpDrive.blockIridium, 4), "ses", "ele", "ses",
				                                       'l', itemStackFranckNZombie,
				                                       's', itemStackRedstoneAlloy,
				                                       'e', itemStackVibrantAlloy);
			}
			
		} else {
			registerShapedRecipe(groupComponents,
			                                       new ItemStack(WarpDrive.blockIridium), "ded", "yty", "ded",
			                                       't', Items.GHAST_TEAR,
			                                       'd', Items.DIAMOND,
			                                       'e', Items.EMERALD,
			                                       'y', Items.ENDER_EYE);
		}
		
		// *** Laser medium
		// basic    is 2 dyes, 1 water bottle, 1 gold nugget, 1 LV casing
		// advanced is 2 redstone dust, 1 awkward potion, 2 lapis, 1 glass tank,  1 power interface, 1 MV casing
		// superior is 1 laser medium (empty), 4 redstone blocks, 4 lapis blocks
		final ItemStack itemStackWaterBottle = WarpDriveConfig.getItemStackOrFire("minecraft:potion", 0, "{Potion: \"minecraft:water\"}");
		final ItemStack itemStackAwkwardPotion = WarpDriveConfig.getItemStackOrFire("minecraft:potion", 0, "{Potion: \"minecraft:awkward\"}");
		WarpDrive.register(new RecipeParticleShapedOre(groupMachines, "",
		                                               new ItemStack(WarpDrive.blockLaserMediums[EnumTier.BASIC.getIndex()]), false, "   ", "dwd", "pm ",
		                                               'd', "forge:dyes",
		                                               'w', itemStackWaterBottle,
		                                               'p', "forge:nuggets/gold",
		                                               'm', itemStackMachineCasings[0] ));
		WarpDrive.register(new RecipeParticleShapedOre(groupMachines, "",
		                                               new ItemStack(WarpDrive.blockLaserMediums[EnumTier.ADVANCED.getIndex()]), false, "rAr", "lBl", "pm ",
		                                               'r', "forge:dusts/redstone",
		                                               'A', itemStackAwkwardPotion,
		                                               'l', "forge:gems/lapis",
		                                               'B', ItemComponent.getItem(EnumComponentType.GLASS_TANK),
		                                               'p', ItemComponent.getItem(EnumComponentType.POWER_INTERFACE),
		                                               'm', itemStackMachineCasings[1] ));
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockLaserMediums[EnumTier.SUPERIOR.getIndex()]), false, "lrl", "rmr", "lrl",
		                                       'm', ItemComponent.getItem(EnumComponentType.LASER_MEDIUM_EMPTY),
		                                       'r', "forge:storage_blocks/redstone",
		                                       'l', "forge:storage_blocks/lapis");
		
		// *** Security Station
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockSecurityStation), "ede", "eme", "eMe",
		                                       'd', ItemComponent.getItem(EnumComponentType.DIAMOND_CRYSTAL),
		                                       'e', ItemComponent.getItem(EnumComponentType.ENDER_COIL),
		                                       'M', ItemComponent.getItem(EnumComponentType.MEMORY_CRYSTAL),
		                                       'm', itemStackMachineCasings[0] );
	}
	
	private static void initComponents() {
		// *** memory storage
		// Memory crystal is 2 Papers, 2 Iron bars, 4 Comparators, 1 Redstone
		final Object memory = WarpDriveConfig.getOreOrItemStack(
				"forge:circuits/primitive", 0,   // comes with GregTech
				"oc:ram2", 0,
				"opencomputers:components", 8,   // Memory Tier 1.5 (workaround for ore dictionary oc:ram2)
				"minecraft:comparator", 0 );
		registerShapedRecipe(groupComponents,
		                                       ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL), false, "gmg", "gmg", "prp",
		                                       'g', "forge:glass_panes/colorless",
		                                       'm', memory,
		                                       'r', Items.REDSTONE,
		                                       'p', Items.PAPER );
		registerShapelessRecipe(groupComponents,
		                                          ItemComponent.getItemStack(EnumComponentType.MEMORY_CLUSTER),
		                                          ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL),
		                                          ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL),
		                                          ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL),
		                                          ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL) );
		
		// *** processing
		// Diamond crystal
		registerShapedRecipe(groupComponents,
		                                       ItemComponent.getItemStack(EnumComponentType.DIAMOND_CRYSTAL), false, " d ", "BBB", "prp",
		                                       'd', Items.DIAMOND,
		                                       'B', barsIron,
		                                       'r', Items.REDSTONE,
		                                       'p', Items.PAPER );
		
		// Emerald crystal
		registerShapedRecipe(groupComponents,
		                                       ItemComponent.getItem(EnumComponentType.EMERALD_CRYSTAL), false, " e ", "BBB", "qrq",
		                                       'e', Items.EMERALD,
		                                       'B', barsIron,
		                                       'r', Items.REDSTONE,
		                                       'q', Items.QUARTZ );
		
		// *** energy storage
		// Capacitive crystal is 2 Redstone block, 4 Paper, 1 Regeneration potion, 2 (lithium dust or electrum dust or electrical steel ingot or gold ingot)
		final Object lithiumOrElectrum = WarpDriveConfig.getOreOrItemStack(
				"forge:dusts/lithium", 0,           // comes with GregTech, Industrial Craft 2 and Mekanism
				"forge:dusts/electrum", 0,          // comes with ImmersiveEngineering, ThermalFoundation, Metallurgy
				"forge:ingots/electrical_steel", 0, // comes with EnderIO
				"forge:ingots/gold", 0 );
		// (Lithium is processed from nether quartz)
		// (IC2 Experimental is 1 Lithium dust from 18 nether quartz)
		// Regeneration II (ghast tear + glowstone)
		final ItemStack itemStackStrongRegeneration = WarpDriveConfig.getItemStackOrFire("minecraft:potion", 0, "{Potion: \"minecraft:strong_regeneration\"}");
		WarpDrive.register(new RecipeParticleShapedOre(groupComponents, "",
		                                               ItemComponent.getItemStackNoCache(EnumComponentType.CAPACITIVE_CRYSTAL, 2), false, "prp", "lRl", "prp",
		                                               'R', itemStackStrongRegeneration,
		                                               'r', "forge:storage_blocks/redstone",
		                                               'l', lithiumOrElectrum,
		                                               'p', Items.PAPER ));
		registerShapelessRecipe(groupComponents,
		                                          ItemComponent.getItemStack(EnumComponentType.CAPACITIVE_CLUSTER),
		                                          ItemComponent.getItemStack(EnumComponentType.CAPACITIVE_CRYSTAL),
		                                          ItemComponent.getItemStack(EnumComponentType.CAPACITIVE_CRYSTAL),
		                                          ItemComponent.getItemStack(EnumComponentType.CAPACITIVE_CRYSTAL),
		                                          ItemComponent.getItemStack(EnumComponentType.CAPACITIVE_CRYSTAL) );
		
		// *** networking
		// Ender coil crystal
		final Object nuggetGoldOrSilver = WarpDriveConfig.getOreOrItemStack(
				"forge:nuggets/electrum", 0,
				"forge:nuggets/silver", 0,
				"forge:nuggets/gold", 0 );
		registerShapedRecipe(groupComponents,
		                                       ItemComponent.getItemStackNoCache(EnumComponentType.ENDER_COIL, 2), false, "GGg", "rer", "gGG",
		                                       'e', Items.ENDER_PEARL,
		                                       'G', "forge:glass_panes/colorless",
		                                       'r', Items.REDSTONE,
		                                       'g', nuggetGoldOrSilver );
		
		// Diamond coil is 6 Iron bars, 2 Gold ingots, 1 Diamond crystal, gives 12
		final Object ingotGoldOrSilver = WarpDriveConfig.getOreOrItemStack(
				"forge:ingots/electrum", 0,
				"forge:ingots/silver", 0,
				"forge:ingots/gold", 0 );
		registerShapedRecipe(groupComponents,
		                                       ItemComponent.getItemStackNoCache(EnumComponentType.DIAMOND_COIL, 12), false, "bbg", "bdb", "gbb",
		                                       'b', barsIron,
		                                       'g', ingotGoldOrSilver,
		                                       'd', ItemComponent.getItem(EnumComponentType.DIAMOND_CRYSTAL) );
		
		// Computer interface is 2 Gold ingot, 2 Wired modems (or redstone), 1 Lead/Tin ingot
		Object redstoneOrModem = Items.REDSTONE;
		if (WarpDriveConfig.isComputerCraftLoaded) {
			redstoneOrModem = WarpDriveConfig.getItemStackOrFire("computercraft:cable", 1); // Wired modem
		}
		
		final Object controlUnitOrBasicCircuit = WarpDriveConfig.getOreOrItemStack(
				"oc:material_cu", 0,
				"opencomputers:material", 11, // Control unit is 5 gold ingot, 2 redstone, 1 paper, 3 iron ingot
				"forge:circuits/basic", 0,
				"minecraft:light_weighted_pressure_plate", 0 );
		
		// Computer interface: double output with Soldering alloy
		if (isTagDefined("forge:ingots/soldering_alloy")) {
			registerShapedRecipe(groupComponents,
			                                       ItemComponent.getItemStackNoCache(EnumComponentType.COMPUTER_INTERFACE, 4), false, "   ", "rar", "gGg",
			                                       'G', controlUnitOrBasicCircuit,
			                                       'g', "forge:ingots/gold",
			                                       'r', redstoneOrModem,
			                                       'a', "forge:ingots/soldering_alloy" );
		}
		
		// Computer interface: simple output
		final Object slimeOrTinOrLead = WarpDriveConfig.getOreOrItemStack(
				"forge:ingots/tin", 0,
				"forge:ingots/lead", 0,
				"forge:slimeballs", 0 );
		registerShapedRecipe(groupComponents,
		                                       ItemComponent.getItemStackNoCache(EnumComponentType.COMPUTER_INTERFACE, 2), false, "   ", "rar", "gGg",
		                                       'G', controlUnitOrBasicCircuit,
		                                       'g', "forge:ingots/gold",
		                                       'r', redstoneOrModem,
		                                       'a', slimeOrTinOrLead );
		
		// *** breathing
		// Bone charcoal is smelting 1 Bone
		registerSmeltingRecipe(new ItemStack(Items.BONE), ItemComponent.getItemStackNoCache(EnumComponentType.BONE_CHARCOAL, 1), 1);
		
		// Activated carbon is 3 bone charcoal, 3 leaves, 2 water bottles, 1 sulfur dust or gunpowder
		final Object leaves = WarpDriveConfig.getOreOrItemStack(
				"minecraft:leaves", 0 );
		final Object gunpowderOrSulfur = WarpDriveConfig.getOreOrItemStack(
				"forge:dusts/sulfur", 0,
				"forge:gunpowder", 0,
				"minecraft:gunpowder", 0 );
		final ItemStack itemStackWaterBottle = WarpDriveConfig.getItemStackOrFire("minecraft:potion", 0, "{Potion: \"minecraft:water\"}");
		WarpDrive.register(new RecipeParticleShapedOre(groupComponents, "",
		                                               ItemComponent.getItemStack(EnumComponentType.ACTIVATED_CARBON), false, "lll", "aaa", "wgw",
		                                               'l', leaves,
		                                               'a', ItemComponent.getItemStack(EnumComponentType.BONE_CHARCOAL),
		                                               'w', itemStackWaterBottle,
		                                               'g', gunpowderOrSulfur ));
		
		// Air canister is 4 iron bars, 2 rubber, 2 yellow wool, 1 tank
		registerShapedRecipe(groupComponents,
		                                       ItemComponent.getItemStackNoCache(EnumComponentType.AIR_CANISTER, 4), false, "iyi", "rgr", "iyi",
		                                       'r', rubber,
		                                       'g', ItemComponent.getItemStack(EnumComponentType.GLASS_TANK),
		                                       'y', Items.PURPLE_WOOL,
		                                       'i', barsIron );
		
		// *** human interface
		// Flat screen is 3 Dyes, 1 Glowstone dust, 2 Paper, 3 Glass panes
		registerShapedRecipe(groupComponents,
		                                       ItemComponent.getItemStack(EnumComponentType.FLAT_SCREEN), false, "gRp", "gGd", "gBp",
		                                       'R', "forge:dyes/red",
		                                       'G', "forge:dyes/lime",
		                                       'B', "forge:dyes/blue",
		                                       'd', "forge:dusts/glowstone",
		                                       'g', "forge:glass_panes/colorless",
		                                       'p', Items.PAPER );
		
		// Holographic projector is 5 Flat screens, 1 Zoom, 1 Emerald crystal, 1 Memory crystal
		registerShapedRecipe(groupComponents,
		                                       ItemComponent.getItemStack(EnumComponentType.HOLOGRAPHIC_PROJECTOR), false, "ssM", "szc", "ssE",
		                                       's', ItemComponent.getItemStack(EnumComponentType.FLAT_SCREEN),
		                                       'z', ItemComponent.getItemStack(EnumComponentType.ZOOM),
		                                       'M', ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL),
		                                       'c', ItemComponent.getItemStack(EnumComponentType.COMPUTER_INTERFACE),
		                                       'E', ItemComponent.getItemStack(EnumComponentType.EMERALD_CRYSTAL) );
		
		// *** mechanical
		// Glass tank is 4 Slime balls, 4 Glass
		// slimeball && blockGlass are defined by forge itself
		registerShapedRecipe(groupComponents,
		                                       ItemComponent.getItemStackNoCache(EnumComponentType.GLASS_TANK, 4), false, "sgs", "g g", "sgs",
		                                       's', "forge:slimeballs",
		                                       'g', "forge:glass" );
		
		// Motor is 2 Gold nuggets (wires), 3 Iron ingots (steel rods), 4 Iron bars (coils)
		registerShapedRecipe(groupComponents,
		                                       ItemComponent.getItemStack(EnumComponentType.MOTOR), false, "bbn", "iii", "bbn",
		                                       'b', barsIron,
		                                       'i', "forge:ingots/iron",
		                                       'n', "forge:nuggets/gold" );
		
		// Pump is 2 Motor, 1 Iron ingot, 2 Tank, 4 Rubber, gives 2
		registerShapedRecipe(groupComponents,
		                                       ItemComponent.getItemStackNoCache(EnumComponentType.PUMP, 2), false, "sst", "mim", "tss",
		                                       's', rubber,
		                                       'i', ingotIronOrSteel,
		                                       'm', itemStackMotors[0],
		                                       't', ItemComponent.getItemStack(EnumComponentType.GLASS_TANK) );
		
		// *** optical
		// Lens is 1 Diamond, 6 Gold nugget, 2 Glass panel, gives 2
		final Object diamondLensOrGem = WarpDriveConfig.getOreOrItemStack(
				"forge:lens/diamond", 0,
				"forge:dems/diamond", 0 );
		final Object whiteLensOrGlassPane = WarpDriveConfig.getOreOrItemStack(
				"ore:craftingLensWhite", 0,
				"ore:paneGlassColorless", 0 );
		final Object glassPane = WarpDriveConfig.getOreOrItemStack(
				"ore:plateGlass", 0,
				"ore:paneGlassColorless", 0 );
		registerShapedRecipe(groupComponents,
		                                       ItemComponent.getItemStackNoCache(EnumComponentType.LENS, 2), false, "ggg", "pdp", "ggg",
		                                       'g', "forge:nuggets/gold",
		                                       'p', whiteLensOrGlassPane,
		                                       'd', diamondLensOrGem ));
		// in case craftingLens was found, add a secondary more expensive recipe with just glass
		// note: this is done in the spirit of Gregtech progression while keeping early access to laser tree farm
		if (!glassPane.equals(whiteLensOrGlassPane)) {
			registerShapedRecipe(groupComponents,
			                                       ItemComponent.getItemStackNoCache(EnumComponentType.LENS, 1), false, "ggg", "pdp", "ggg",
			                                       'g', "nuggetGold",
			                                       'p', glassPane,
			                                       'd', diamondLensOrGem ));
		}
		
		// Zoom is 3 Lens, 2 Iron ingot, 2 Dyes, 1 Redstone, 1 Basic motor
		registerShapedRecipe(groupComponents,
		                                       ItemComponent.getItemStack(EnumComponentType.ZOOM), false, "dir", "lll", "dit",
		                                       'r', Items.REDSTONE,
		                                       'i', ingotIronOrSteel,
		                                       'l', ItemComponent.getItem(EnumComponentType.LENS),
		                                       't', itemStackMotors[0],
		                                       'd', "forge:dyes" );
		
		// Diffraction grating is 1 Ghast tear, 3 Iron bar, 3 Glowstone block
		registerShapedRecipe(groupComponents,
		                                       ItemComponent.getItemStack(EnumComponentType.DIFFRACTION_GRATING), false, " t ", "iii", "ggg",
		                                       't', Items.GHAST_TEAR,
		                                       'i', barsIron,
		                                       'g', Blocks.GLOWSTONE );
		
		// *** energy interface
		// Power interface is 4 Redstone, 2 Rubber, 3 Gold ingot
		registerShapedRecipe(groupComponents,
		                                       ItemComponent.getItemStackNoCache(EnumComponentType.POWER_INTERFACE, 3), false, "rgr", "RgR", "rgr",
		                                       'g', "forge:ingots/gold",
		                                       'R', rubber,
		                                       'r', Items.REDSTONE );
		
		// Superconductor is 1 Ender crystal, 2 Power interface, 2 Cryotheum dust/Lapis block/10k Coolant cell
		final Object coolant = WarpDriveConfig.getOreOrItemStack(
				"forge:dusts/cryotheum", 0,     // comes with ThermalFoundation
				"ic2:heat_storage", 0,          // IC2 Experimental 10k Coolant Cell
				"ic2:itemheatstorage", 0,       // IC2 Classic 10k Coolant Cell
				"forge:storage_blocks/lapis", 0 );
		registerShapedRecipe(groupComponents, "_direct",
		                                       ItemComponent.getItemStack(EnumComponentType.SUPERCONDUCTOR), false, " c ", "pep", " c ",
		                                       'p', ItemComponent.getItemStack(EnumComponentType.POWER_INTERFACE),
		                                       'e', ItemComponent.getItemStack(EnumComponentType.ENDER_COIL),
		                                       'c', coolant );
		registerShapedRecipe(groupComponents, "_rotated",
		                                       ItemComponent.getItemStack(EnumComponentType.SUPERCONDUCTOR), false, " p ", "cec", " p ",
		                                       'p', ItemComponent.getItemStack(EnumComponentType.POWER_INTERFACE),
		                                       'e', ItemComponent.getItemStack(EnumComponentType.ENDER_COIL),
		                                       'c', coolant );
		
		// *** crafting components
		// Laser medium (empty) is 3 Glass tanks, 1 Power interface, 1 Computer interface, 1 MV Machine casing
		final ItemStack itemStackAwkwardPotion = WarpDriveConfig.getItemStackOrFire("minecraft:potion", 0, "{Potion: \"minecraft:awkward\"}");
		WarpDrive.register(new RecipeParticleShapedOre(groupMachines, "",
		                                               ItemComponent.getItemStack(EnumComponentType.LASER_MEDIUM_EMPTY), false, "   ", "gBg", "pm ",
		                                               'B', itemStackAwkwardPotion,
		                                               'g', ItemComponent.getItemStack(EnumComponentType.GLASS_TANK),
		                                               'p', ItemComponent.getItemStack(EnumComponentType.POWER_INTERFACE),
		                                               'm', itemStackMachineCasings[2] ));
		
		// Electromagnetic Projector is 5 Coil crystals, 1 Power interface, 1 Computer interface, 2 Motors
		registerShapedRecipe(groupMachines,
		                                       ItemComponent.getItemStack(EnumComponentType.ELECTROMAGNETIC_PROJECTOR), false, "CCm", "Cpc", "CCm",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'p', ItemComponent.getItemStack(EnumComponentType.POWER_INTERFACE),
		                                       'm', itemStackMotors[2],
		                                       'c', ItemComponent.getItemStack(EnumComponentType.COMPUTER_INTERFACE));
		
		// Intermediary component for Reactor core
		if (!WarpDriveConfig.ACCELERATOR_ENABLE) {
			registerShapedRecipe(groupMachines,
			                                       ItemComponent.getItemStack(EnumComponentType.REACTOR_CORE), false, "shs", "hmh", "shs",
			                                       's', Items.NETHER_STAR,
			                                       'h', "warpdrive:hull3/plain",
			                                       'm', itemStackMachineCasings[2]);
		} else {
			WarpDrive.register(new RecipeParticleShapedOre(groupMachines, "",
			                                               ItemComponent.getItemStack(EnumComponentType.REACTOR_CORE), false, "chc", "hph", "cec",
			                                               'p', ItemElectromagneticCell.getItemStackNoCache(EnumTier.ADVANCED, ParticleRegistry.ION, 1000),
			                                               'h', "warpdrive:hull3/plain",
			                                               'c', ItemComponent.getItemStack(EnumComponentType.CAPACITIVE_CRYSTAL),
			                                               'e', ItemComponent.getItemStack(EnumComponentType.EMERALD_CRYSTAL)));
		}
		
		// *** rubber material
		// Raw rubber lump is produced from Jungle wood in the laser tree farm
		// (no direct recipe)
		
		// Rubber is the product of smelting (vulcanize) Raw rubber lump
		// (in reality, vulcanization requires additives. This refining is optional, so low tiers could still use the Raw rubber lump)
		registerSmeltingRecipe(
				ItemComponent.getItemStack(EnumComponentType.RAW_RUBBER),
				ItemComponent.getItemStack(EnumComponentType.RUBBER),
				0 );
		
		// *** composite materials
		// Biopulp is some mycelium and lots of leaves
		// silktouch recipe
		registerShapedRecipe(groupMachines, "_block",
		                                       ItemComponent.getItemStackNoCache(EnumComponentType.BIOPULP, 9), false, "lll", "lml", "lll",
		                                       'l', leaves,
		                                       'm', "forge:blocks/mushroom" );
		
		// easier but more expensive from fiber (sugar cane)
		final Object oreOrBrownMushroom = WarpDriveConfig.getOreOrItemStack(
				"forge:mushrooms", 0,
				"minecraft:brown_mushroom", 0 );
		final Object oreOrRedMushroom = WarpDriveConfig.getOreOrItemStack(
				"forge:mushrooms", 0,
				"minecraft:red_mushroom", 0 );
		registerShapedRecipe(groupMachines, "_sugarcane",
		                                       ItemComponent.getItemStackNoCache(EnumComponentType.BIOPULP, 2), false, "lll", "mbM", "lll",
		                                       'b', Items.SUGAR_CANE,
		                                       'l', leaves,
		                                       'm', oreOrBrownMushroom,
		                                       'M', oreOrRedMushroom );
		
		// Biofiber is the product of washing/filtering/drying Biopulp
		registerSmeltingRecipe(
				ItemComponent.getItemStack(EnumComponentType.BIOPULP),
				ItemComponent.getItemStack(EnumComponentType.BIOFIBER),
				0 );
		
		// Raw ceramic is clay, with silicate
		registerShapelessRecipe(groupMachines,
		                                          ItemComponent.getItemStackNoCache(EnumComponentType.RAW_CERAMIC, 4),
		                                          Items.CLAY_BALL,
		                                          Items.CLAY_BALL,
		                                          Items.CLAY_BALL,
		                                          "sand" );
		
		// Biofiber is the product of washing/filtering/drying Biopulp
		registerSmeltingRecipe(
				ItemComponent.getItemStack(EnumComponentType.RAW_CERAMIC),
				ItemComponent.getItemStack(EnumComponentType.CERAMIC),
				0 );
		
		// Carbon fiber plate is a slow/expensive process from making fiber, then making mesh than cooking it
		// Raw carbon fiber from 8 coal (dust), 1 blaze powder, gives 4
		// for reference:
		// - IC2 is from 4 coal dust, gives 1 fiber
		// - TechGuns is 1 blaze powder, 1 diamond, 1B lava, gives 2 fiber/plate
		final Object coalDustOrCoal = WarpDriveConfig.getOreOrItemStack(
				"forge:dusts/coal", 0,
				"minecraft:coal", 0 );
		registerShapelessRecipe(groupMachines, "coal",
		                                          ItemComponent.getItemStackNoCache(EnumComponentType.RAW_CARBON_FIBER, 4),
		                                          Items.BLAZE_POWDER,
		                                          coalDustOrCoal, coalDustOrCoal, coalDustOrCoal, coalDustOrCoal,
		                                          coalDustOrCoal, coalDustOrCoal, coalDustOrCoal, coalDustOrCoal );
		// (alternate recipe, more expensive from charcoal)
		final Object coalDustOrCharcoal = WarpDriveConfig.getOreOrItemStack(
				"forge:dusts/charcoal", 0,
				"minecraft:coal", 1 );
		registerShapelessRecipe(groupMachines, "charcoal",
		                                          ItemComponent.getItemStack(EnumComponentType.RAW_CARBON_FIBER),
		                                          Items.BLAZE_POWDER,
		                                          coalDustOrCharcoal, coalDustOrCharcoal, coalDustOrCharcoal, coalDustOrCharcoal );
		
		// Raw carbon mesh is 2 Biofiber, 3 Carbon fiber
		// for reference:
		// - IC2 is 2 fiber, gives 1 mesh
		registerShapedRecipe(groupMachines,
		                                       ItemComponent.getItemStackNoCache(EnumComponentType.RAW_CARBON_MESH, 4), false, "fcf", "ccc", "fcf",
		                                       'f', ItemComponent.getItem(EnumComponentType.BIOFIBER),
		                                       'c', ItemComponent.getItem(EnumComponentType.RAW_CARBON_FIBER) );
		
		// Carbon fiber is the product of cooking the mesh (under pressure?)
		registerSmeltingRecipe(
				ItemComponent.getItemStack(EnumComponentType.RAW_CARBON_MESH),
				ItemComponent.getItemStack(EnumComponentType.CARBON_FIBER),
				0 );
	}
	
	private static void initToolsAndArmor() {
		// Warp helmet
		registerShapedRecipe(groupTools,
		                                       WarpDrive.itemWarpArmor[EnumTier.BASIC.getIndex()][EquipmentSlotType.HEAD.getIndex()], false, "mmm", "mgm", "ici",
		                                       'm', "forge:rubber",
		                                       'g', "warpdrive:hull1/glass",
		                                       'c', ItemComponent.getItem(EnumComponentType.AIR_CANISTER),
		                                       'i', "forge:nuggets/iron" );
		registerShapedRecipe(groupTools,
		                                       WarpDrive.itemWarpArmor[EnumTier.ADVANCED.getIndex()][EquipmentSlotType.HEAD.getIndex()], false, "fmf", "mam", "   ",
		                                       'a', WarpDrive.itemWarpArmor[EnumTier.BASIC.getIndex()][EquipmentSlotType.HEAD.getIndex()],
		                                       'm', "forge:ceramic",
		                                       'f', "forge:biofiber" );
		registerShapedRecipe(groupTools,
		                                       WarpDrive.itemWarpArmor[EnumTier.SUPERIOR.getIndex()][EquipmentSlotType.HEAD.getIndex()], false, "mmm", "mam", "   ",
		                                       'a', WarpDrive.itemWarpArmor[EnumTier.ADVANCED.getIndex()][EquipmentSlotType.HEAD.getIndex()],
		                                       'm', "forge:plates/carbon" );
		
		// Warp chestplate
		registerShapedRecipe(groupTools,
		                                       WarpDrive.itemWarpArmor[EnumTier.BASIC.getIndex()][EquipmentSlotType.CHEST.getIndex()], false, "i i", "mmm", "mim",
		                                       'm', "forge:rubber",
		                                       'i', "forge:nuggets/iron" );
		registerShapedRecipe(groupTools,
		                                       WarpDrive.itemWarpArmor[EnumTier.ADVANCED.getIndex()][EquipmentSlotType.CHEST.getIndex()], false, "faf", "mmm", "mfm",
		                                       'a', WarpDrive.itemWarpArmor[EnumTier.BASIC.getIndex()][EquipmentSlotType.CHEST.getIndex()],
		                                       'm', "forge:ceramic",
		                                       'f', "forge:biofiber" );
		registerShapedRecipe(groupTools,
		                                       WarpDrive.itemWarpArmor[EnumTier.SUPERIOR.getIndex()][EquipmentSlotType.CHEST.getIndex()], false, "mam", "mpm", "mcm",
		                                       'a', WarpDrive.itemWarpArmor[EnumTier.ADVANCED.getIndex()][EquipmentSlotType.CHEST.getIndex()],
		                                       'm', "forge:plates/carbon",
		                                       'p', ItemComponent.getItem(EnumComponentType.PUMP),
		                                       'c', ItemComponent.getItem(EnumComponentType.AIR_CANISTER) );
		
		// Warp Leggings
		registerShapedRecipe(groupTools,
		                                       WarpDrive.itemWarpArmor[EnumTier.BASIC.getIndex()][EquipmentSlotType.LEGS.getIndex()], false, "imi", "m m", "m m",
		                                       'm', "forge:rubber",
		                                       'i', "forge:nuggets/iron" );
		registerShapedRecipe(groupTools,
		                                       WarpDrive.itemWarpArmor[EnumTier.ADVANCED.getIndex()][EquipmentSlotType.LEGS.getIndex()], false, "faf", "mMm", "w w",
		                                       'a', WarpDrive.itemWarpArmor[EnumTier.BASIC.getIndex()][EquipmentSlotType.LEGS.getIndex()],
		                                       'm', "forge:ceramic",
		                                       'f', "forge:biofiber",
		                                       'w', "minecraft:wool",
		                                       'M', itemStackMotors[1] );
		registerShapedRecipe(groupTools,
		                                       WarpDrive.itemWarpArmor[EnumTier.SUPERIOR.getIndex()][EquipmentSlotType.LEGS.getIndex()], false, "mam", "m m", "m m",
		                                       'a', WarpDrive.itemWarpArmor[EnumTier.ADVANCED.getIndex()][EquipmentSlotType.LEGS.getIndex()],
		                                       'm', "forge:plates/carbon" );
		
		// Warp boots
		registerShapedRecipe(groupTools,
		                                       WarpDrive.itemWarpArmor[EnumTier.BASIC.getIndex()][EquipmentSlotType.FEET.getIndex()], false, "i i", "m m", "   ",
		                                       'm', "forge:items/rubber",
		                                       'i', "forge:nuggets/iron" );
		registerShapedRecipe(groupTools,
		                                       WarpDrive.itemWarpArmor[EnumTier.ADVANCED.getIndex()][EquipmentSlotType.FEET.getIndex()], false, "mam", "fMf", "w w",
		                                       'a', WarpDrive.itemWarpArmor[EnumTier.BASIC.getIndex()][EquipmentSlotType.FEET.getIndex()],
		                                       'm', "forge:items/ceramic",
		                                       'f', "forge:items/biofiber",
		                                       'w', "minecraft:wool",
		                                       'M', itemStackMotors[1] );
		registerShapedRecipe(groupTools,
		                                       WarpDrive.itemWarpArmor[EnumTier.SUPERIOR.getIndex()][EquipmentSlotType.FEET.getIndex()], false, "mam", "m m", "   ",
		                                       'a', WarpDrive.itemWarpArmor[EnumTier.ADVANCED.getIndex()][EquipmentSlotType.FEET.getIndex()],
		                                       'm', "forge:plates/carbon" );
		
		// Wrench
		registerShapedRecipe(groupTools,
		                                       WarpDrive.itemWrench, false, "n n", "nin", " m ",
		                                       'm', "forge:items/rubber",
		                                       'i', "forge:ingots/iron",
		                                       'n', "forge:nuggets/iron" );
		
		// Tuning fork variations
		for (final DyeColor dyeColor : DyeColor.values()) {
			final int indexColor = dyeColor.getId();
			
			// crafting tuning fork
			registerShapedRecipe(groupTools,
			                                       new ItemStack(WarpDrive.itemTuningForks[indexColor], 1), false, "  q", "iX ", " i ",
			                                       'q', "forge:gems/quartz",
			                                       'i', "forge:ingots/iron",
			                                       'X', oreDyes.get(dyeColor) );
			
			// changing colors
			registerShapelessRecipe(groupTools, "_dye",
			                                          new ItemStack(WarpDrive.itemTuningForks[indexColor], 1),
			                                          oreDyes.get(dyeColor),
			                                          "warpdrive:tuning/fork");
		}
		
		// Tuning driver crafting
		registerShapedRecipe(groupTools,
		                     new ItemStack(WarpDrive.itemTuningDriver, 1), false, "  q", "pm ", "d  ",
		                     'q', "forge:gems/quartz",
		                     'p', Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE,
		                     'd', ItemComponent.getItem(EnumComponentType.DIAMOND_CRYSTAL),
		                     'm', ItemComponent.getItem(EnumComponentType.MEMORY_CRYSTAL) );
		
		// Tuning driver configuration
		WarpDrive.register(new RecipeTuningDriver(groupTools, "_video_channel",
		                                          new ItemStack(WarpDrive.itemTuningDriver, 1),
		                                          ItemTuningDriver.Mode.VIDEO_CHANNEL,
		                                          new ItemStack(Items.REDSTONE), 7));
		WarpDrive.register(new RecipeTuningDriver(groupTools, "_bream_frequency",
		                                          new ItemStack(WarpDrive.itemTuningDriver, 1),
		                                          ItemTuningDriver.Mode.BEAM_FREQUENCY,
		                                          new ItemStack(Items.REDSTONE), 4));
		WarpDrive.register(new RecipeTuningDriver(groupTools, "_control_channel",
		                                          new ItemStack(WarpDrive.itemTuningDriver, 1),
		                                          ItemTuningDriver.Mode.CONTROL_CHANNEL,
		                                          new ItemStack(Items.REDSTONE), 7));
		
		// User manual
		final ItemStack itemStackManual = WarpDriveConfig.getItemStackOrFire("patchouli:guide_book", 0, "{\"patchouli:book\": \"warpdrive:warpdrive_manual\"}");
		if (!itemStackManual.isEmpty()) {
			registerShapedRecipe(groupTools,
			                                       itemStackManual, false, " g ", "ibi", " i ",
			                                       'g', "forge:nuggets/gold",
			                                       'i', "forge:nuggets/iron",
			                                       'b', Items.BOOK );
		}
	}
	
	public static void initDynamic() {
		initIngredients();
		initComponents();
		initToolsAndArmor();
		
		// Ship scanner is creative only => no recipe
		/*
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockShipScanner), false, "ici", "isi", "mcm",
		                                       'm', mfsu,
		                                       'i', iridiumAlloy,
		                                       'c', goldIngotOrAdvancedCircuit,
		                                       's', WarpDriveConfig.getModItemStack("ic2", "te", 64) ); // Scanner
		/**/
		
		if (WarpDriveConfig.ACCELERATOR_ENABLE) {
			initAtomic();
		}
		initBreathing();
		initCollection();
		initDecoration();
		initDetection();
		initEnergy();
		initForceField();
		initHull();
		initMovement();
		initWeapon();
	}
	
	private static void initAtomic() {
		// Void shells is Hull, Power interface, Steel or Iron
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockVoidShellPlain, 6), "psh", "s s", "hsp",
		                                       'h', "warpdrive:hull1/plain",
		                                       'p', ItemComponent.getItem(EnumComponentType.POWER_INTERFACE),
		                                       's', ingotIronOrSteel);
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockVoidShellGlass, 6), "psh", "s s", "hsp",
		                                       'h', "warpdrive:hull1/glass",
		                                       'p', ItemComponent.getItem(EnumComponentType.POWER_INTERFACE),
		                                       's', ingotIronOrSteel);
		
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockVoidShellGlass, 2), "g g", "sfs", "g g",
		                                       'g', "forge:glass",
		                                       'f', "forge:dusts/glowstone",
		                                       's', WarpDrive.blockVoidShellPlain);
		
		// Electromagnetic cell
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.itemElectromagneticCell[EnumTier.BASIC.getIndex()], 2), "iri", "i i", "ici",
		                                       'i', barsIron,
		                                       'c', ItemComponent.getItem(EnumComponentType.CAPACITIVE_CRYSTAL),
		                                       'r', Items.REDSTONE);
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.itemElectromagneticCell[EnumTier.ADVANCED.getIndex()], 2), "iei", "iei", "gcg",
		                                       'e', WarpDrive.itemElectromagneticCell[EnumTier.BASIC.getIndex()],
		                                       'i', barsIron,
		                                       'g', Items.GOLD_NUGGET,
		                                       'c', ItemComponent.getItem(EnumComponentType.CAPACITIVE_CRYSTAL));
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.itemElectromagneticCell[EnumTier.SUPERIOR.getIndex()], 2), "geg", "geg", "gcg",
		                                       'e', WarpDrive.itemElectromagneticCell[EnumTier.ADVANCED.getIndex()],
		                                       'g', Items.GOLD_NUGGET,
		                                       'c', ItemComponent.getItem(EnumComponentType.CAPACITIVE_CRYSTAL));
		
		// Plasma torch
		/*
		registerShapedRecipe(groupTools,
		                                       WarpDrive.itemPlasmaTorch[EnumTier.BASIC.getIndex()], false, "tcr", "mgb", "i  ",
		                                       't', WarpDrive.itemElectromagneticCell[EnumTier.BASIC.getIndex()],
		                                       'c', ItemComponent.getItem(EnumComponentType.ACTIVATED_CARBON),
		                                       'r', Items.BLAZE_ROD,
		                                       'm', ItemComponent.getItem(EnumComponentType.PUMP),
		                                       'g', "forge:ingots/gold",
		                                       'b', Blocks.STONE_BUTTON,
		                                       'i', ingotIronOrSteel);
		/**/
		
		// Accelerator control point
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockAcceleratorControlPoint), "hd ", "vc ", "he ",
		                                       'h', Blocks.HOPPER,
		                                       'd', ItemComponent.getItem(EnumComponentType.DIAMOND_CRYSTAL),
		                                       'e', ItemComponent.getItem(EnumComponentType.EMERALD_CRYSTAL),
		                                       'c', ItemComponent.getItem(EnumComponentType.COMPUTER_INTERFACE),
		                                       'v', "warpdrive:void_shell");
		
		// Particles injector
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockParticlesInjector), "mm ", "vvp", "mmc",
		                                       'p', Blocks.PISTON,
		                                       'm', "warpdrive:electromagnet1",
		                                       'c', WarpDrive.blockAcceleratorControlPoint,
		                                       'v', "warpdrive:void_shell");
		
		// Accelerator controller
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockAcceleratorCore), "MmM", "mcm", "MmM",
		                                       'M', ItemComponent.getItem(EnumComponentType.MEMORY_CRYSTAL),
		                                       'm', "warpdrive:electromagnet1",
		                                       'c', WarpDrive.blockAcceleratorControlPoint);
		
		// Particles collider
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockParticlesCollider), "hoh", "odo", "hoh",
		                                       'h', "warpdrive:hull1/plain",
		                                       'o', Blocks.OBSIDIAN,
		                                       'd', Items.DIAMOND);
		
		// Chillers
		Object snowOrIce = Blocks.SNOW;
		if (isTagDefined("forge:dusts/cryotheum")) {
			snowOrIce = Blocks.ICE;
		}
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockChillers[EnumTier.BASIC.getIndex()]), "wgw", "sms", "bMb",
		                                       'w', snowOrIce,
		                                       'g', Items.GHAST_TEAR,
		                                       's', ingotIronOrSteel,
		                                       'm', itemStackMotors[0],
		                                       'b', barsIron,
		                                       'M', "warpdrive:electromagnet1");
		
		Object nitrogen = Blocks.ICE;
		if (isTagDefined("forge:dusts/cryotheum")) {
			nitrogen = Blocks.PACKED_ICE;
		}
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockChillers[EnumTier.ADVANCED.getIndex()]), "ngn", "dmd", "bMb",
		                                       'n', nitrogen,
		                                       'g', Items.GHAST_TEAR,
		                                       'd', Items.DIAMOND,
		                                       'm', itemStackMotors[1],
		                                       'b', barsIron,
		                                       'M', "warpdrive:electromagnet2");
		
		Object helium = Blocks.PACKED_ICE;
		if (isTagDefined("forge:dusts/cryotheum")) {
			helium = "forge:dusts/cryotheum";
		}
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockChillers[EnumTier.SUPERIOR.getIndex()]), "hgh", "eme", "bMb",
		                                       'h', helium,
		                                       'g', Items.GHAST_TEAR,
		                                       'e', Items.EMERALD,
		                                       'm', itemStackMotors[2],
		                                       'b', barsIron,
		                                       'M', "warpdrive:electromagnet3");
		
		// Lower tier coil is iron, copper or coil
		// note: IC2 Classic has no coil, so we fallback to other mods or Copper ingot
		final Object ironIngotOrCopperIngotOrCoil1 = WarpDriveConfig.getOreOrItemStack(
				"ic2:crafting", 5,                      // IC2 Experimental Coil
				"immersiveengineering:wirecoil", 1,     // ImmersiveEngineering MV wire coil
				"enderio:item_power_conduit", 1,        // EnderIO Enhanced energy conduit
				"forge:ingots/copper", 0,
				"forge:ingots/steel", 0,
				"minecraft:iron_ingot", 0 );
		final Object ironIngotOrCopperIngotOrCoil2 = WarpDriveConfig.getOreOrItemStack(
				"gregtech:wire_coil", 0,                // GregTech Cupronickel Coil block
				"ic2:crafting", 5,                      // IC2 Experimental Coil
				"thermalfoundation:material", 513,      // ThermalFoundation Redstone reception coil
				"immersiveengineering:wirecoil", 1,     // ImmersiveEngineering MV wire coil
				"enderio:item_power_conduit", 1,        // EnderIO Enhanced energy conduit
				"forge:ingots/copper", 0,
				"forge:ingots/steel", 0,
				"minecraft:iron_ingot", 0 );
		
		// Normal electromagnets
		registerShapedRecipe(groupMachines, "",
		                                       new ItemStack(WarpDrive.blockElectromagnets_plain[EnumTier.BASIC.getIndex()], 4), "   ", "cdc", "Cmt",
		                                       'c', ironIngotOrCopperIngotOrCoil1,
		                                       'd', ironIngotOrCopperIngotOrCoil2,
		                                       't', ItemComponent.getItem(EnumComponentType.GLASS_TANK),
		                                       'm', itemStackMotors[0],
		                                       'C', ItemComponent.getItem(EnumComponentType.CAPACITIVE_CRYSTAL));
		registerShapedRecipe(groupMachines, "",
		                                       new ItemStack(WarpDrive.blockElectromagnets_glass[EnumTier.BASIC.getIndex()], 4), "mgm", "g g", "mgm",
		                                       'g', Blocks.GLASS,
		                                       'm', WarpDrive.blockElectromagnets_plain[EnumTier.BASIC.getIndex()]);
		
		// Advanced electromagnets
		WarpDrive.register(new RecipeParticleShapedOre(groupMachines, "",
		                                               new ItemStack(WarpDrive.blockElectromagnets_plain[EnumTier.ADVANCED.getIndex()], 6), "mpm", "pip", "mpm",
		                                               'i', ItemElectromagneticCell.getItemStackNoCache(EnumTier.BASIC, ParticleRegistry.ION, 200),
		                                               'p', ItemComponent.getItem(EnumComponentType.POWER_INTERFACE),
		                                               'm', WarpDrive.blockElectromagnets_plain[EnumTier.BASIC.getIndex()]));
		WarpDrive.register(new RecipeParticleShapedOre(groupMachines, "",
		                                               new ItemStack(WarpDrive.blockElectromagnets_glass[EnumTier.ADVANCED.getIndex()], 6), "mpm", "pip", "mpm",
		                                               'i', ItemElectromagneticCell.getItemStackNoCache(EnumTier.BASIC, ParticleRegistry.ION, 200),
		                                               'p', ItemComponent.getItem(EnumComponentType.POWER_INTERFACE),
		                                               'm', WarpDrive.blockElectromagnets_glass[EnumTier.BASIC.getIndex()]));
		
		// Superior electromagnets
		WarpDrive.register(new RecipeParticleShapedOre(groupMachines, "",
		                                               new ItemStack(WarpDrive.blockElectromagnets_plain[EnumTier.SUPERIOR.getIndex()], 6), "mtm", "sps", "mMm",
		                                               't', ItemComponent.getItem(EnumComponentType.GLASS_TANK),
		                                               's', ItemComponent.getItem(EnumComponentType.SUPERCONDUCTOR),
		                                               'p', ItemElectromagneticCell.getItemStackNoCache(EnumTier.BASIC, ParticleRegistry.PROTON, 24),
		                                               'M', itemStackMotors[2],
		                                               'm', WarpDrive.blockElectromagnets_plain[EnumTier.ADVANCED.getIndex()]));
		WarpDrive.register(new RecipeParticleShapedOre(groupMachines, "",
		                                               new ItemStack(WarpDrive.blockElectromagnets_glass[EnumTier.SUPERIOR.getIndex()], 6), "mtm", "sps", "mMm",
		                                               't', ItemComponent.getItem(EnumComponentType.GLASS_TANK),
		                                               's', ItemComponent.getItem(EnumComponentType.SUPERCONDUCTOR),
		                                               'p', ItemElectromagneticCell.getItemStackNoCache(EnumTier.BASIC, ParticleRegistry.PROTON, 24),
		                                               'M', itemStackMotors[2],
		                                               'm', WarpDrive.blockElectromagnets_glass[EnumTier.ADVANCED.getIndex()]));
		
		// ICBM classic
		if (WarpDriveConfig.isICBMClassicLoaded) {
			// antimatter
			final ItemStack itemStackAntimatterExplosive = WarpDriveConfig.getItemStackOrFire("icbmclassic:explosives", 22); // Antimatter Explosive
			removeRecipe(itemStackAntimatterExplosive);
			WarpDrive.register(new RecipeParticleShapedOre(groupComponents, "",
			                                               itemStackAntimatterExplosive, "aaa", "ana", "aaa",
			                                               'a', ItemElectromagneticCell.getItemStackNoCache(EnumTier.ADVANCED, ParticleRegistry.ANTIMATTER, 1000),
			                                               'n', WarpDriveConfig.getItemStackOrFire("icbmclassic:explosives", 15)));
			
			// red matter
			final ItemStack itemStackRedMatterExplosive = WarpDriveConfig.getItemStackOrFire("icbmclassic:explosives", 23); // Red Matter Explosive
			removeRecipe(itemStackRedMatterExplosive);
			WarpDrive.register(new RecipeParticleShapedOre(groupComponents, "",
			                                               itemStackRedMatterExplosive, "sss", "sas", "sss",
			                                               's', ItemElectromagneticCell.getItemStackNoCache(EnumTier.ADVANCED, ParticleRegistry.STRANGE_MATTER, 1000),
			                                               'a', WarpDriveConfig.getItemStackOrFire("icbmclassic:explosives", 22)));
		}
	}
	
	private static void initBreathing() {
		// Basic Air Tank is 2 Air canisters, 1 Pump, 1 Gold nugget, 1 Basic circuit, 4 Rubber
		registerShapedRecipe(groupTools,
		                                       WarpDrive.itemAirTanks[EnumAirTankTier.BASIC.getIndex()], false, "rnr", "tpt", "rcr",
		                                       'r', rubber,
		                                       'p', ItemComponent.getItem(EnumComponentType.PUMP),
		                                       't', ItemComponent.getItem(EnumComponentType.AIR_CANISTER),
		                                       'c', goldNuggetOrBasicCircuit,
		                                       'n', "forge:nuggets/gold" );
		
		// Advanced Air Tank is 2 Basic air tank, 1 Pump, 1 Gold nugget, 1 Advanced circuit, 4 Rubber
		registerShapedRecipe(groupTools,
		                                       WarpDrive.itemAirTanks[EnumAirTankTier.ADVANCED.getIndex()], false, "rnr", "tpt", "rcr",
		                                       'r', rubber,
		                                       'p', itemStackMotors[1],
		                                       't', WarpDrive.itemAirTanks[EnumAirTankTier.BASIC.getIndex()],
		                                       'c', goldIngotOrAdvancedCircuit,
		                                       'n', "forge:nuggets/gold" );
		
		// Superior Air Tank is 2 Advanced air tank, 1 Pump, 1 Gold nugget, 1 Elite circuit, 4 Rubber
		registerShapedRecipe(groupTools,
		                                       WarpDrive.itemAirTanks[EnumAirTankTier.SUPERIOR.getIndex()], false, "rnr", "tpt", "rcr",
		                                       'r', rubber,
		                                       'p', itemStackMotors[2],
		                                       't', WarpDrive.itemAirTanks[EnumAirTankTier.ADVANCED.getIndex()],
		                                       'c', emeraldOrSuperiorCircuit,
		                                       'n', "forge:nuggets/gold" );
		
		// Uncrafting air tanks and canister
		registerShapelessRecipe(groupComponents, "_uncrafting",
		                                          ItemComponent.getItemStackNoCache(EnumComponentType.GLASS_TANK, 1),
		                                          WarpDrive.itemAirTanks[EnumAirTankTier.CANISTER.getIndex()],
		                                          WarpDrive.itemAirTanks[EnumAirTankTier.CANISTER.getIndex()],
		                                          WarpDrive.itemAirTanks[EnumAirTankTier.CANISTER.getIndex()],
		                                          WarpDrive.itemAirTanks[EnumAirTankTier.CANISTER.getIndex()] );
		registerShapelessRecipe(groupComponents, "_uncrafting",
		                                          ItemComponent.getItemStackNoCache(EnumComponentType.AIR_CANISTER, 2),
		                                          WarpDrive.itemAirTanks[EnumAirTankTier.BASIC.getIndex()]);
		registerShapelessRecipe(groupComponents, "_uncrafting",
		                                          ItemComponent.getItemStackNoCache(EnumComponentType.AIR_CANISTER, 4),
		                                          WarpDrive.itemAirTanks[EnumAirTankTier.ADVANCED.getIndex()]);
		registerShapelessRecipe(groupComponents, "_uncrafting",
		                                          ItemComponent.getItemStackNoCache(EnumComponentType.AIR_CANISTER, 8),
		                                          WarpDrive.itemAirTanks[EnumAirTankTier.SUPERIOR.getIndex()]);
		
		// Air generator is 1 Power interface, 4 Activated carbon, 1 Motor, 1 MV Machine casing, 2 Glass tanks
		final Object bronzeRotorOrIronBars = WarpDriveConfig.getOreOrItemStack(
				"forge:rotors/bronze", 0,       // GregTech CE Bronze rotor
				"forge:plates/bronze", 8,       // IC2 or ThermalExpansion Bronze plate
				"forge:gears/iron_infinity", 0, // EnderIO Infinity Bimetal Gear
				"forge:bars/iron", 0,           // Ore dictionary iron bars
				"minecraft:iron_bars", 0 );
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockAirGeneratorTiered[EnumTier.BASIC.getIndex()]), false, "aba", "ata", "gmp",
		                                       'p', ItemComponent.getItem(EnumComponentType.POWER_INTERFACE),
		                                       'a', ItemComponent.getItem(EnumComponentType.ACTIVATED_CARBON),
		                                       't', ItemComponent.getItem(EnumComponentType.PUMP),
		                                       'g', ItemComponent.getItem(EnumComponentType.GLASS_TANK),
		                                       'm', itemStackMachineCasings[1],
		                                       'b', bronzeRotorOrIronBars);
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockAirGeneratorTiered[EnumTier.ADVANCED.getIndex()]), false, "aaa", "ata", "ama",
		                                       'a', WarpDrive.blockAirGeneratorTiered[EnumTier.BASIC.getIndex()],
		                                       't', itemStackMotors[2],
		                                       'm', itemStackMachineCasings[2]);
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockAirGeneratorTiered[EnumTier.SUPERIOR.getIndex()]), false, "aaa", "ata", "ama",
		                                       'a', WarpDrive.blockAirGeneratorTiered[EnumTier.ADVANCED.getIndex()],
		                                       't', itemStackMotors[3],
		                                       'm', itemStackMachineCasings[3]);
		
		// Air shield is 4 Glowstones, 4 Omnipanels and 1 coil crystal
		for (final DyeColor dyeColor : DyeColor.values()) {
			final int idColor = dyeColor.getId();
			
			registerShapedRecipe(groupMachines,
			                                       new ItemStack(WarpDrive.blockAirShields[idColor], 4), false, "gog", "oco", "gog",
			                                       'g', "forge:dusts/glowstone",
			                                       'o', new ItemStack(WarpDrive.blockHulls_omnipanel[EnumTier.BASIC.getIndex()][idColor], 1),
			                                       'c', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL) );
			registerShapedRecipe(groupMachines,
			                                       new ItemStack(WarpDrive.blockAirShields[idColor], 6), false, "###", "gXg", "###",
			                                       '#', "warpdrive:air_shield",
			                                       'g', Items.GOLD_NUGGET,
			                                       'X', oreDyes.get(dyeColor) );
			registerShapelessRecipe(groupMachines,
			                                          new ItemStack(WarpDrive.blockAirShields[idColor], 1),
			                                          "warpdrive:air_shield",
			                                          oreDyes.get(dyeColor) );
		}
	}
	
	private static void initCollection() {
		// Mining laser is 2 Motors, 1 Diffraction grating, 1 Lens, 1 Computer interface, 1 MV Machine casing, 1 Diamond pick, 2 Glass pane
		{
			registerShapedRecipe(groupMachines,
			                                       new ItemStack(WarpDrive.blockMiningLaser), false, " mp", "tdt", "glg",
			                                       't', itemStackMotors[1],
			                                       'd', ItemComponent.getItem(EnumComponentType.DIFFRACTION_GRATING),
			                                       'l', ItemComponent.getItem(EnumComponentType.LENS),
			                                       'm', itemStackMachineCasings[1],
			                                       'p', Items.DIAMOND_PICKAXE,
			                                       'g', "forge:glass_panes/colorless");
		}
		
		// Laser tree farm is 2 Motors, 2 Lenses, 1 Computer interface, 1 LV Machine casing, 1 Iron axe, 2 Glass pane
		// note: diamond axe was more consistent with Buildcraft, nowadays other mods have cheaper options
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockLaserTreeFarm), false, "glg", "tlt", "am ",
		                                       't', itemStackMotors[0],
		                                       'l', ItemComponent.getItem(EnumComponentType.LENS),
		                                       'm', itemStackMachineCasings[0],
		                                       'a', Items.IRON_AXE,
		                                       'g', "forge:glass_panes/colorless");
	}
	
	private static void initDecoration() {
		// Decorative blocks are metallic in nature
		// base block is very cheap (iron and paper)
		registerShapedRecipe(groupDecorations,
		                                       BlockDecorative.getItemStackNoCache(EnumDecorativeType.PLAIN, 12), false, "ipi", "pbp", "ipi",
		                                       'i', Items.IRON_INGOT,
		                                       'b', Blocks.IRON_BARS,
		                                       'p', Items.PAPER );
		
		// variations are 'died' from each others
		registerShapedRecipe(groupDecorations, "_dye",
		                                       BlockDecorative.getItemStackNoCache(EnumDecorativeType.PLAIN, 8), false, "sss", "scs", "sss",
		                                       's', "warpdrive:decorative",
		                                       'c', "forge:dyes/white");
		registerShapedRecipe(groupDecorations, "_dye",
		                                       BlockDecorative.getItemStackNoCache(EnumDecorativeType.GRATED, 8), false, "sss", "sbs", "sss",
		                                       's', "warpdrive:decorative",
		                                       'b', barsIron );
		registerShapedRecipe(groupDecorations, "_dye",
		                                       BlockDecorative.getItemStackNoCache(EnumDecorativeType.GLASS, 8), false, "sss", "scs", "sss",
		                                       's', "warpdrive:decorative",
		                                       'c', "glass" );
		registerShapedRecipe(groupDecorations, "_dye",
		                                       BlockDecorative.getItemStackNoCache(EnumDecorativeType.STRIPES_BLACK_DOWN, 7), false, "bss", "sss", "ssy",
		                                       's', "warpdrive:decorative",
		                                       'b', "forge:dyes/black",
		                                       'y', "forge:dyes/yellow" );
		registerShapedRecipe(groupDecorations, "_dye",
		                                       BlockDecorative.getItemStackNoCache(EnumDecorativeType.STRIPES_BLACK_UP, 7), false, "ssy", "sss", "bss",
		                                       's', "warpdrive:decorative",
		                                       'b', "forge:dyes/black",
		                                       'y', "forge:dyes/yellow" );
		registerShapedRecipe(groupDecorations, "_dye",
		                                       BlockDecorative.getItemStackNoCache(EnumDecorativeType.STRIPES_YELLOW_DOWN, 7), false, "yss", "sss", "ssb",
		                                       's', "warpdrive:decorative",
		                                       'b', "forge:dyes/black",
		                                       'y', "forge:dyes/yellow" );
		registerShapedRecipe(groupDecorations, "_dye",
		                                    BlockDecorative.getItemStackNoCache(EnumDecorativeType.STRIPES_YELLOW_UP, 7), false, "ssb", "sss", "yss",
		                                    's', "warpdrive:decorative",
		                                    'b', "forge:dyes/black",
		                                    'y', "forge:dyes/yellow" );
		
		// stripes can toggled to each others (reducing dye consumption)
		registerShapelessRecipe(groupDecorations, "_toggle",
		                                          BlockDecorative.getItemStackNoCache(EnumDecorativeType.STRIPES_BLACK_DOWN, 1),
		                                          BlockDecorative.getItemStackNoCache(EnumDecorativeType.STRIPES_BLACK_UP, 1) );
		registerShapelessRecipe(groupDecorations, "_toggle",
		                                          BlockDecorative.getItemStackNoCache(EnumDecorativeType.STRIPES_BLACK_UP, 1),
		                                          BlockDecorative.getItemStackNoCache(EnumDecorativeType.STRIPES_YELLOW_DOWN, 1) );
		registerShapelessRecipe(groupDecorations, "_toggle",
		                                          BlockDecorative.getItemStackNoCache(EnumDecorativeType.STRIPES_YELLOW_DOWN, 1),
		                                          BlockDecorative.getItemStackNoCache(EnumDecorativeType.STRIPES_YELLOW_UP, 1) );
		registerShapelessRecipe(groupDecorations, "_toggle",
		                                       BlockDecorative.getItemStackNoCache(EnumDecorativeType.STRIPES_YELLOW_UP, 1),
		                                       BlockDecorative.getItemStackNoCache(EnumDecorativeType.STRIPES_BLACK_DOWN, 1) );
		
		// Lamps
		registerShapedRecipe(groupDecorations,
		                                       WarpDrive.blockLamp_bubble, false, " g ", "glg", "h  ",
		                                       'g', "forge:glass",
		                                       'l', Blocks.REDSTONE_LAMP,
		                                       'h', "warpdrive:hull1/plain");
		registerShapedRecipe(groupDecorations,
		                                       WarpDrive.blockLamp_flat, false, " g ", "glg", " h ",
		                                       'g', "forge:glass",
		                                       'l', Blocks.REDSTONE_LAMP,
		                                       'h', "warpdrive:hull1/plain");
		registerShapedRecipe(groupDecorations,
		                                       WarpDrive.blockLamp_long, false, " g ", "glg", "  h",
		                                       'g', "forge:glass",
		                                       'l', Blocks.REDSTONE_LAMP,
		                                       'h', "warpdrive:hull1/plain");
	}
	
	private static void initDetection() {
		// Biometric scanner
		if (!WarpDriveConfig.ACCELERATOR_ENABLE) {
			registerShapedRecipe(groupMachines,
			                                       WarpDrive.blockBiometricScanner, false, "rDr", "EmE", "rCr",
			                                       'r', rubber,
			                                       'm', itemStackMachineCasings[1],
			                                       'E', ItemComponent.getItem(EnumComponentType.ENDER_COIL),
			                                       'D', ItemComponent.getItem(EnumComponentType.DIAMOND_CRYSTAL),
			                                       'C', ItemComponent.getItem(EnumComponentType.COMPUTER_INTERFACE) );
		} else {
			registerShapedRecipe(groupMachines,
			                                       WarpDrive.blockBiometricScanner, false, "rDr", "EmE", "rCr",
			                                       'r', rubber,
			                                       'm', "warpdrive:electromagnet1",
			                                       'E', ItemComponent.getItem(EnumComponentType.ENDER_COIL),
			                                       'D', ItemComponent.getItem(EnumComponentType.DIAMOND_CRYSTAL),
			                                       'C', ItemComponent.getItem(EnumComponentType.COMPUTER_INTERFACE) );
		}
		
		// Camera is 1 Daylight sensor, 2 Motors, 1 Computer interface, 2 Glass panel, 1 Tuning diamond, 1 LV Machine casing
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockCamera), false, "gtd", "zlm", "gt ",
		                                       't', itemStackMotors[0],
		                                       'z', ItemComponent.getItem(EnumComponentType.ZOOM),
		                                       'd', ItemComponent.getItem(EnumComponentType.DIAMOND_CRYSTAL),
		                                       'm', itemStackMachineCasings[0],
		                                       'l', Blocks.DAYLIGHT_DETECTOR,
		                                       'g', "forge:glass_panes/colorless");
		
		// Cloaking core is 3 Cloaking coils, 4 Iridium blocks, 1 Ship controller, 1 Power interface
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockCloakingCore), false, "ici", "csc", "ipi",
		                                       'i', WarpDrive.blockIridium,
		                                       'c', WarpDrive.blockCloakingCoil,
		                                       's', WarpDrive.blockShipControllers[EnumTier.SUPERIOR.getIndex()],
		                                       'p', ItemComponent.getItem(EnumComponentType.SUPERCONDUCTOR) );
		
		// Cloaking coil is 1 Titanium plate, 4 Reinforced iridium plate, 1 EV Machine casing (Ti) or 1 Beacon, 4 Emerald, 4 Diamond
		final Object oreGoldIngotOrCoil = WarpDriveConfig.getOreOrItemStack(
				"gregtech:wire_coil", 3,                    // GregTech Tungstensteel Coil block
				"ic2:crafting", 5,                          // IC2 Experimental Coil
				"thermalfoundation:material", 515,          // ThermalFoundation Redstone conductance coil
				"immersiveengineering:connector", 8,        // ImmersiveEngineering HV Transformer (coils wires are too cheap)
				"enderio:item_power_conduit", 2,            // EnderIO Ender energy conduit
				"minecraft:gold_ingot", 0 );
		final Object oreGoldIngotOrTitaniumPlate = WarpDriveConfig.getOreOrItemStack(
				"forge:plates/titanium", 0,
				"advanced_solar_panels:crafting", 0,	    // ASP Sunnarium
				"forge:plates/dense_steel", 0,
				"thermalfoundation:glass", 6,               // ThermalFoundation Hardened Platinum Glass
				"immersiveengineering:metal_device1", 3,    // ImmersiveEngineering Thermoelectric Generator
				"forge:ingots/vibrant_alloy", 2,	        // EnderIO Vibrant alloy
				"minecraft:gold_ingot", 0 );
		final Object oreEmeraldOrIridiumPlate = WarpDriveConfig.getOreOrItemStack(
				"forge:plates/iridium", 0,                  // GregTech
				"forge:plates/alloy_tridium", 0,            // IndustrialCraft2
				"enderio:item_material", 42,                // EnderIO Frank'N'Zombie
				"forge:ingots/lumium", 0,                   // ThermalFoundation lumium ingot
				"forge:gems/emerald", 0 );
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockCloakingCoil), false, "iti", "cmc", "iti",
		                                       't', oreGoldIngotOrTitaniumPlate,
		                                       'i', oreEmeraldOrIridiumPlate,
		                                       'c', oreGoldIngotOrCoil,
		                                       'm', itemStackMachineCasings[3] );
		
		// Environmental sensor
		registerShapedRecipe(groupMachines,
		                                       WarpDrive.blockEnvironmentalSensor, "   ", "dcd", "rCr",
		                                       'r', rubber,
		                                       'c', Items.CLOCK,
		                                       'd', Blocks.DAYLIGHT_DETECTOR,
		                                       'C', ItemComponent.getItem(EnumComponentType.COMPUTER_INTERFACE) );
		
		// Monitor is 3 Flat screen, 1 Computer interface, 1 Tuning diamond, 1 LV Machine casing
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockMonitor), false, "fd ", "fm ", "f  ",
		                                       'f', ItemComponent.getItem(EnumComponentType.FLAT_SCREEN),
		                                       'd', ItemComponent.getItem(EnumComponentType.DIAMOND_CRYSTAL),
		                                       'm', itemStackMachineCasings[0]);
		
		// Radar is 1 motor, 4 Titanium plate (diamond), 1 Quarztite rod (nether quartz), 1 Computer interface, 1 HV Machine casing, 1 Power interface
		final Object oreRadarDish = WarpDriveConfig.getOreOrItemStack(
				"forge:plates/titanium", 0,         // GregTech
				"forge:plates/enderium", 0,         // ThermalExpansion
				"forge:ingots/vibrant_alloy", 0,    // EnderIO
				"forge:plates/iridium_alloy", 0,    // IndustrialCraft2
				"forge:gems/quartz", 0 );
		final Object oreRadarSensor = WarpDriveConfig.getOreOrItemStack(
				"forge:sticks/quartzite", 0,        // GregTech
				"forge:ingots/signalum", 0,         // ThermalExpansion
				"forge:nuggets/pulsating_iron", 0,  // EnderIO
				"minecraft:ghast_tear", 0 );
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockRadar), false, "PAP", "PtP", "pmc",
		                                       't', itemStackMotors[2],
		                                       'P', oreRadarDish,
		                                       'A', oreRadarSensor,
		                                       'c', ItemComponent.getItem(EnumComponentType.COMPUTER_INTERFACE),
		                                       'm', itemStackMachineCasings[2],
		                                       'p', ItemComponent.getItem(EnumComponentType.SUPERCONDUCTOR) );
		
		// Sirens
		registerShapedRecipe(groupMachines,
		                                       WarpDrive.blockSirenIndustrials[EnumTier.BASIC.getIndex()], "pip", "pNp", "pip",
		                                       'p', "minecraft:planks",
		                                       'i', "forge:ingots/iron",
		                                       'N', new ItemStack(Blocks.NOTE_BLOCK, 1) );
		registerShapedRecipe(groupMachines,
		                                       WarpDrive.blockSirenIndustrials[EnumTier.ADVANCED.getIndex()], " I ", "ISI", " I ",
		                                       'I', "forge:ingots/gold",
		                                       'S', WarpDrive.blockSirenIndustrials[EnumTier.BASIC.getIndex()] );
		registerShapedRecipe(groupMachines,
		                                       WarpDrive.blockSirenIndustrials[EnumTier.SUPERIOR.getIndex()], " I ", "ISI", " I ",
		                                       'I', "forge:gems/diamond",
		                                       'S', WarpDrive.blockSirenIndustrials[EnumTier.ADVANCED.getIndex()] );
		
		registerShapedRecipe(groupMachines,
		                                       WarpDrive.blockSirenMilitaries[EnumTier.BASIC.getIndex()], "ppp", "iNi", "ppp",
		                                       'p', "minecraft:planks",
		                                       'i', "forge:ingots/iron",
		                                       'N', new ItemStack(Blocks.NOTE_BLOCK, 1) );
		registerShapedRecipe(groupMachines,
		                                       WarpDrive.blockSirenMilitaries[EnumTier.ADVANCED.getIndex()], " I ", "ISI", " I ",
		                                       'I', "forge:ingots/gold",
		                                       'S', WarpDrive.blockSirenMilitaries[EnumTier.BASIC.getIndex()] );
		registerShapedRecipe(groupMachines,
		                                       WarpDrive.blockSirenMilitaries[EnumTier.SUPERIOR.getIndex()], " I ", "ISI", " I ",
		                                       'I', "forge:gems/diamond",
		                                       'S', WarpDrive.blockSirenMilitaries[EnumTier.ADVANCED.getIndex()] );
		
		// Speakers
		registerShapedRecipe(groupMachines,
		                                       WarpDrive.blockSpeakers[EnumTier.BASIC.getIndex()], "BBB", "rDr", "rCr",
		                                       'B', ItemComponent.getItem(EnumComponentType.BIOFIBER),
		                                       'r', rubber,
		                                       'D', ItemComponent.getItem(EnumComponentType.DIAMOND_CRYSTAL),
		                                       'C', ItemComponent.getItem(EnumComponentType.COMPUTER_INTERFACE) );
		registerShapedRecipe(groupMachines,
		                                       WarpDrive.blockSpeakers[EnumTier.ADVANCED.getIndex()], " I ", "ISI", " I ",
		                                       'I', "forge:ingots/gold",
		                                       'S', WarpDrive.blockSpeakers[EnumTier.BASIC.getIndex()] );
		registerShapedRecipe(groupMachines,
		                                       WarpDrive.blockSpeakers[EnumTier.SUPERIOR.getIndex()], " I ", "ISI", " I ",
		                                       'I', "forge:gems/diamond",
		                                       'S', WarpDrive.blockSpeakers[EnumTier.ADVANCED.getIndex()] );
		
		// Virtual assistants
		registerShapedRecipe(groupMachines,
		                                       WarpDrive.blockVirtualAssistants[EnumTier.BASIC.getIndex()], "BEB", "rmr", "rCr",
		                                       'B', ItemComponent.getItem(EnumComponentType.BIOFIBER),
		                                       'm', itemStackMachineCasings[1],
		                                       'r', rubber,
		                                       'E', ItemComponent.getItem(EnumComponentType.EMERALD_CRYSTAL),
		                                       'C', ItemComponent.getItem(EnumComponentType.COMPUTER_INTERFACE) );
		registerShapedRecipe(groupMachines,
		                                       WarpDrive.blockVirtualAssistants[EnumTier.ADVANCED.getIndex()], "DCD", "CSC", "DCD",
		                                       'D', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
		                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_CRYSTAL),
		                                       'S', WarpDrive.blockVirtualAssistants[EnumTier.BASIC.getIndex()] );
		registerShapedRecipe(groupMachines,
		                                       WarpDrive.blockVirtualAssistants[EnumTier.SUPERIOR.getIndex()], "EYE", "YSY", "EYE",
		                                       'E', ItemComponent.getItem(EnumComponentType.ENDER_COIL),
		                                       'Y', Items.ENDER_EYE,
		                                       'S', WarpDrive.blockVirtualAssistants[EnumTier.ADVANCED.getIndex()] );
		
		// Warp isolation is 1 EV Machine casing (Ti), 4 Titanium plates/Enderium ingots/Vibrant alloy/Iridium plates/quartz
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockWarpIsolation), false, "i i", " m ", "i i",
		                                       'i', oreRadarDish,
		                                       'm', itemStackMachineCasings[3]);
	}
	
	private static void initEnergy() {
		// IC2 needs to be loaded for the following 2 recipes
		if (WarpDriveConfig.isIndustrialCraft2Loaded) {
			final Object overclockedHeatVent = WarpDriveConfig.getOreOrItemStack(
					"ic2:overclocked_heat_vent", 0,      // IC2 Experimental Overclocked heat vent
					"ic2:itemheatvent", 2 );             // IC2 Classic Overclocked heat vent (not the electric variant)
			// (there's no coolant in GT6 version 6.06.05, nor in GregTech CE version 1.12.2-0.4.5.9, so we're falling back to IC2)
			final Object reactorCoolant = WarpDriveConfig.getOreOrItemStack(
					"ic2:hex_heat_storage", 0,           // IC2 Experimental 60k Coolant Cell
					"ic2:itemheatstorage", 2 );          // IC2 Classic 60k Coolant Cell
			
			registerShapedRecipe(groupMachines,
			                                       new ItemStack(WarpDrive.itemIC2reactorLaserFocus), false, "cld", "lhl", "dlc",
			                                       'l', ItemComponent.getItem(EnumComponentType.LENS),
			                                       'h', overclockedHeatVent,
			                                       'c', reactorCoolant,
			                                       'd', reactorCoolant );
			
			registerShapedRecipe(groupMachines,
			                                       new ItemStack(WarpDrive.blockIC2reactorLaserCooler), false, "gCp", "lme", "gC ",
			                                       'l', ItemComponent.getItem(EnumComponentType.LENS),
			                                       'e', ItemComponent.getItem(EnumComponentType.EMERALD_CRYSTAL),
			                                       'C', ItemComponent.getItem(EnumComponentType.CAPACITIVE_CRYSTAL),
			                                       'p', ItemComponent.getItem(EnumComponentType.POWER_INTERFACE),
			                                       'g', "minecraft:planks",
			                                       'm', itemStackMachineCasings[1] );
		}
		
		// Enantiomorphic reactor core is 1 EV Machine casing, 4 Capacitive crystal, 1 Computer interface, 1 Power interface, 2 Lenses
		if (!WarpDriveConfig.ACCELERATOR_ENABLE) {
			registerShapedRecipe(groupMachines,
			                                       WarpDrive.blockEnanReactorCores[EnumTier.BASIC.getIndex()], false, "CpC", "lml", "CcC",
			                                       'm', ItemComponent.getItem(EnumComponentType.REACTOR_CORE),
			                                       'l', ItemComponent.getItem(EnumComponentType.LENS),
			                                       'p', ItemComponent.getItem(EnumComponentType.POWER_INTERFACE),
			                                       'c', ItemComponent.getItem(EnumComponentType.COMPUTER_INTERFACE),
			                                       'C', ItemComponent.getItem(EnumComponentType.CAPACITIVE_CRYSTAL));
		} else {
			registerShapedRecipe(groupMachines,
			                                       WarpDrive.blockEnanReactorCores[EnumTier.BASIC.getIndex()], false, " p ", "lCl", "cpm",
			                                       'C', ItemComponent.getItem(EnumComponentType.REACTOR_CORE),
			                                       'l', ItemComponent.getItem(EnumComponentType.LENS),
			                                       'p', ItemComponent.getItem(EnumComponentType.POWER_INTERFACE),
			                                       'c', ItemComponent.getItem(EnumComponentType.COMPUTER_INTERFACE),
			                                       'm', itemStackMachineCasings[2]);
		}
		registerShapedRecipe(groupMachines,
		                                       WarpDrive.blockEnanReactorCores[EnumTier.ADVANCED.getIndex()], false, "lcl", "CRC", "lcl",
		                                       'C', ItemComponent.getItem(EnumComponentType.REACTOR_CORE),
		                                       'R', WarpDrive.blockEnanReactorCores[EnumTier.BASIC.getIndex()],
		                                       'l', ItemComponent.getItem(EnumComponentType.LENS),
		                                       'c', ItemComponent.getItem(EnumComponentType.CAPACITIVE_CRYSTAL) );
		registerShapedRecipe(groupMachines,
		                                       WarpDrive.blockEnanReactorCores[EnumTier.SUPERIOR.getIndex()], false, "lSl", "CRC", "lSl",
		                                       'C', ItemComponent.getItem(EnumComponentType.REACTOR_CORE),
		                                       'R', WarpDrive.blockEnanReactorCores[EnumTier.ADVANCED.getIndex()],
		                                       'l', ItemComponent.getItem(EnumComponentType.LENS),
		                                       'S', ItemComponent.getItem(EnumComponentType.SUPERCONDUCTOR) );
		
		// Enantiomorphic reactor stabilization laser is 1 HV Machine casing, 2 Advanced hull, 1 Computer interface, 1 Power interface, 1 Lens, 1 Redstone, 2 Glass panes
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockEnanReactorLaser), false, "g h", "ldm", "g c",
		                                       'd', ItemComponent.getItem(EnumComponentType.DIFFRACTION_GRATING),
		                                       'l', ItemComponent.getItem(EnumComponentType.LENS),
		                                       'c', ItemComponent.getItem(EnumComponentType.COMPUTER_INTERFACE),
		                                       'm', itemStackMachineCasings[1],
		                                       'g', "forge:glass_panes/colorless",
		                                       'h', "warpdrive:hull2/plain");
		
		// Basic subspace capacitor is 1 Capacitive crystal, 1 Gold ingot, 3 Bio fiber, 4 Iron bars
		registerShapedRecipe(groupMachines,
		                                       WarpDrive.blockCapacitors[EnumTier.BASIC.getIndex()], false, "iPi", "pcp", "ipi",
		                                       'c', ItemComponent.getItem(EnumComponentType.CAPACITIVE_CRYSTAL),
		                                       'i', barsIron,
		                                       'p', "forge:items/biofiber",
		                                       'P', ItemComponent.getItem(EnumComponentType.POWER_INTERFACE) );
		
		// Advanced subspace capacitor is 2 Capacitive crystal, 1 Power interface, 4 Rubber, 2 Gold ingot
		// Advanced subspace capacitor is 2 Basic subspace capacitor, 1 Power interface
		registerShapedRecipe(groupMachines, "_direct",
		                                       WarpDrive.blockCapacitors[EnumTier.ADVANCED.getIndex()], false, "rir", "cpc", "rir",
		                                       'c', ItemComponent.getItem(EnumComponentType.CAPACITIVE_CRYSTAL),
		                                       'i', "forge:ingots/iron",
		                                       'r', "forge:rubber",
		                                       'p', ItemComponent.getItem(EnumComponentType.POWER_INTERFACE) );
		// or 2 Basic subspace capacitor, 1 Power interface
		registerShapedRecipe(groupMachines, "_upgrade",
		                                       WarpDrive.blockCapacitors[EnumTier.ADVANCED.getIndex()], false, "r r", "cpc", "r r",
		                                       'c', new ItemStack(WarpDrive.blockCapacitors[EnumTier.BASIC.getIndex()]),
		                                       'r', "forge:rubber",
		                                       'p', ItemComponent.getItem(EnumComponentType.POWER_INTERFACE) );
		
		// Superior subspace capacitor is 1 Capacitive cluster, 4 Carbon fiber, 2 Power interface, 1 Gold ingot, 1 Superconductor
		registerShapedRecipe(groupMachines, "_direct",
		                                       WarpDrive.blockCapacitors[EnumTier.SUPERIOR.getIndex()], false, "psp", "ici", "pgp",
		                                       'c', ItemComponent.getItem(EnumComponentType.CAPACITIVE_CLUSTER),
		                                       'p', ItemComponent.getItem(EnumComponentType.CARBON_FIBER),
		                                       'i', ItemComponent.getItem(EnumComponentType.POWER_INTERFACE),
		                                       'g', "forge:ingots/gold",
		                                       's', ItemComponent.getItem(EnumComponentType.SUPERCONDUCTOR) );
		// or 2 Advanced subspace capacitor, 4 Carbon fiber, 1 Superconductor
		registerShapedRecipe(groupMachines, "_upgrade",
		                                       WarpDrive.blockCapacitors[EnumTier.SUPERIOR.getIndex()], false, "p p", "csc", "p p",
		                                       'c', new ItemStack(WarpDrive.blockCapacitors[EnumTier.ADVANCED.getIndex()]),
		                                       'p', ItemComponent.getItem(EnumComponentType.CARBON_FIBER),
		                                       's', ItemComponent.getItem(EnumComponentType.SUPERCONDUCTOR) );
	}
	
	private static void initForceField() {
		// *** Force field shapes
		// Force field shapes are 1 Memory crystal, 3 to 5 Coil crystal
		registerShapedRecipe(groupComponents,
		                                       ItemForceFieldShape.getItemStack(EnumForceFieldShape.SPHERE), false, "   ", "CmC", "CCC",
		                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
		                                       'm', ItemComponent.getItem(EnumComponentType.MEMORY_CRYSTAL));
		registerShapedRecipe(groupComponents,
		                                       ItemForceFieldShape.getItemStack(EnumForceFieldShape.CYLINDER_H), false, "C C", " m ", "C C",
		                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
		                                       'm', ItemComponent.getItem(EnumComponentType.MEMORY_CRYSTAL));
		registerShapedRecipe(groupComponents,
		                                       ItemForceFieldShape.getItemStack(EnumForceFieldShape.CYLINDER_V), false, " C ", "CmC", " C ",
		                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
		                                       'm', ItemComponent.getItem(EnumComponentType.MEMORY_CRYSTAL));
		registerShapedRecipe(groupComponents,
		                                       ItemForceFieldShape.getItemStack(EnumForceFieldShape.CUBE), false, "CCC", "CmC", "   ",
		                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
		                                       'm', ItemComponent.getItem(EnumComponentType.MEMORY_CRYSTAL));
		registerShapedRecipe(groupComponents,
		                                       ItemForceFieldShape.getItemStack(EnumForceFieldShape.PLANE), false, "CCC", " m ", "   ",
		                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
		                                       'm', ItemComponent.getItem(EnumComponentType.MEMORY_CRYSTAL));
		registerShapedRecipe(groupComponents,
		                                       ItemForceFieldShape.getItemStack(EnumForceFieldShape.TUBE), false, "   ", "CmC", "C C",
		                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
		                                       'm', ItemComponent.getItem(EnumComponentType.MEMORY_CRYSTAL));
		registerShapedRecipe(groupComponents,
		                                       ItemForceFieldShape.getItemStack(EnumForceFieldShape.TUNNEL), false, "C C", "CmC", "   ",
		                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
		                                       'm', ItemComponent.getItem(EnumComponentType.MEMORY_CRYSTAL));
		
		// *** Force field upgrades
		// Force field attraction upgrade is 3 Coil crystal, 1 Iron block, 2 Redstone block, 1 MV motor
		registerShapedRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.ATTRACTION), false, "CCC", "rir", " m ",
		                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
		                                       'r', "forge:storage_blocks/redstone",
		                                       'i', Blocks.IRON_BLOCK,
		                                       'm', itemStackMotors[1]);
		// Force field breaking upgrade is 3 Coil crystal, 1 Diamond axe, 1 Diamond shovel, 1 Diamond pick, gives 2
		registerShapedRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStackNoCache(EnumForceFieldUpgrade.BREAKING, 2), false, "CCC", "sap", "   ",
		                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
		                                       's', Items.DIAMOND_AXE,
		                                       'a', Items.DIAMOND_SHOVEL,
		                                       'p', Items.DIAMOND_PICKAXE);
		// Force field camouflage upgrade is 3 Coil crystal, 2 Diffraction grating, 1 Zoom, 1 Emerald crystal
		registerShapedRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.CAMOUFLAGE), false, "CCC", "zre", "   ",
		                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
		                                       'z', ItemComponent.getItem(EnumComponentType.ZOOM),
		                                       'r', Blocks.DAYLIGHT_DETECTOR,
		                                       'e', ItemComponent.getItem(EnumComponentType.EMERALD_CRYSTAL));
		// Force field cooling upgrade is 3 Coil crystal, 2 Ice, 1 MV Motor
		registerShapedRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.COOLING), false, "CCC", "imi", "   ",
		                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
		                                       'i', Blocks.ICE,
		                                       'm', ItemComponent.getItem(EnumComponentType.PUMP) );
		// Force field fusion upgrade is 3 Coil crystal, 2 Computer interface, 1 Emerald crystal
		registerShapedRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.FUSION), false, "CCC", "cec", "   ",
		                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
		                                       'c', ItemComponent.getItem(EnumComponentType.COMPUTER_INTERFACE),
		                                       'e', ItemComponent.getItem(EnumComponentType.EMERALD_CRYSTAL) );
		// Force field heating upgrade is 3 Coil crystal, 2 Blaze rod, 1 MV Motor
		registerShapedRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.HEATING), false, "CCC", "bmb", "   ",
		                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
		                                       'b', Items.BLAZE_ROD,
		                                       'm', ItemComponent.getItem(EnumComponentType.PUMP) );
		// Force field inversion upgrade is 3 Coil crystal, 1 Gold nugget, 2 Redstone
		registerShapedRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.INVERSION), false, "rgr", "CCC", "CCC",
		                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
		                                       'r', Items.REDSTONE,
		                                       'g', Items.GOLD_NUGGET );
		// Force field item port upgrade is 3 Coil crystal, 3 wooden chests, 1 MV motor
		registerShapedRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.ITEM_PORT), false, "CCC", "cmc", " c ",
		                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
		                                       'c', "forge:chests/wooden",
		                                       'm', itemStackMotors[1] );
		// Force field silencer upgrade is 3 Coil crystal, 3 Wool
		registerShapedRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.SILENCER), false, "CCC", "www", "   ",
		                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
		                                       'w', "minecraft:wool" );
		// Force field pumping upgrade is 3 Coil crystal, 1 MV Motor, 2 Glass tanks
		registerShapedRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.PUMPING), false, "CCC", "tmt", "   ",
		                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
		                                       'm', ItemComponent.getItem(EnumComponentType.PUMP),
		                                       't', ItemComponent.getItem(EnumComponentType.GLASS_TANK) );
		// Force field range upgrade is 3 Coil crystal, 2 Memory crystal, 1 Redstone block
		registerShapedRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.RANGE), false, "CCC", "RMR", "   ",
		                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
		                                       'M', ItemComponent.getItem(EnumComponentType.MEMORY_CRYSTAL),
		                                       'R', "forge:storage_blocks/redstone" );
		// Force field repulsion upgrade is 3 Coil crystal, 1 Iron block, 2 Redstone block, 1 MV motor
		registerShapedRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.REPULSION), false, " m ", "rir", "CCC",
		                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
		                                       'r', "forge:storage_blocks/redstone",
		                                       'i', Blocks.IRON_BLOCK,
		                                       'm', itemStackMotors[1] );
		// Force field rotation upgrade is 3 Coil crystal, 2 MV Motors, 1 Computer interface
		registerShapedRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStackNoCache(EnumForceFieldUpgrade.ROTATION, 2), false, "CCC", " m ", " mc",
		                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
		                                       'm', itemStackMotors[1],
		                                       'c', ItemComponent.getItem(EnumComponentType.COMPUTER_INTERFACE) );
		// Force field shock upgrade is 3 Coil crystal, 1 Power interface
		registerShapedRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.SHOCK), false, "CCC", " p ", "   ",
		                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
		                                       'p', ItemComponent.getItem(EnumComponentType.POWER_INTERFACE) );
		// Force field speed upgrade is 3 Coil crystal, 2 Ghast tear, 1 Emerald crystal
		registerShapedRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.SPEED), false, "CCC", "geg", "   ",
		                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
		                                       'g', Items.GHAST_TEAR,
		                                       'e', ItemComponent.getItem(EnumComponentType.EMERALD_CRYSTAL) );
		// Force field stabilization upgrade is 3 Coil crystal, 1 Memory crystal, 2 Lapis block
		registerShapedRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.STABILIZATION), "CCC", "lMl", "   ",
		                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
		                                       'M', ItemComponent.getItem(EnumComponentType.MEMORY_CRYSTAL),
		                                       'l', "forge:storage_blocks/lapis" );
		// Force field thickness upgrade is 8 Coil crystal, 1 Diamond crystal
		registerShapedRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.THICKNESS), false, "CCC", "CpC", "   ",
		                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
		                                       'p', ItemComponent.getItem(EnumComponentType.ELECTROMAGNETIC_PROJECTOR));
		// Force field translation upgrade is 3 Coil crystal, 2 MV Motor, 1 Computer interface
		registerShapedRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStackNoCache(EnumForceFieldUpgrade.TRANSLATION, 2), false, "CCC", "m m", " c ",
		                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
		                                       'm', itemStackMotors[1],
		                                       'c', ItemComponent.getItem(EnumComponentType.COMPUTER_INTERFACE) );
		
		// Force field projector is 1 or 2 Electromagnetic Projector, 1 LV/MV/HV Machine casing, 1 Ender crystal, 1 Redstone
		for (final EnumTier enumTier : EnumTier.nonCreative()) {
			final int index = enumTier.getIndex();
			registerShapedRecipe(groupMachines, "_left",
			                                       new ItemStack(WarpDrive.blockForceFieldHalfProjectors[index], 1), false, " e ", "pm ", " r ",
			                                       'p', ItemComponent.getItem(EnumComponentType.ELECTROMAGNETIC_PROJECTOR),
			                                       'm', itemStackMachineCasings[index],
			                                       'e', ItemComponent.getItem(EnumComponentType.ENDER_COIL),
			                                       'r', Items.REDSTONE);
			registerShapedRecipe(groupMachines, "_right",
			                                       new ItemStack(WarpDrive.blockForceFieldHalfProjectors[index], 1), false, " e ", " mp", " r ",
			                                       'p', ItemComponent.getItem(EnumComponentType.ELECTROMAGNETIC_PROJECTOR),
			                                       'm', itemStackMachineCasings[index],
			                                       'e', ItemComponent.getItem(EnumComponentType.ENDER_COIL),
			                                       'r', Items.REDSTONE);
			registerShapedRecipe(groupMachines,
			                                       new ItemStack(WarpDrive.blockForceFieldFullProjectors[index], 1), false, " e ", "pmp", " r ",
			                                       'p', ItemComponent.getItem(EnumComponentType.ELECTROMAGNETIC_PROJECTOR),
			                                       'm', itemStackMachineCasings[index],
			                                       'e', ItemComponent.getItem(EnumComponentType.ENDER_COIL),
			                                       'r', Items.REDSTONE);
		}
		
		// Force field relay is 2 Coil crystals, 1 LV/MV/HV Machine casing, 1 Ender crystal, 1 Redstone
		for (final EnumTier enumTier : EnumTier.nonCreative()) {
			final int index = enumTier.getIndex();
			registerShapedRecipe(groupMachines,
			                                       new ItemStack(WarpDrive.blockForceFieldRelays[index]), false, " e ", "CmC", " r ",
			                                       'C', ItemComponent.getItem(EnumComponentType.DIAMOND_COIL),
			                                       'm', itemStackMachineCasings[index],
			                                       'e', ItemComponent.getItem(EnumComponentType.ENDER_COIL),
			                                       'r', Items.REDSTONE);
		}
	}
	
	private static void initHull() {
		// *** Hull blocks plain
		
		for (final DyeColor dyeColor : DyeColor.values()) {
			final int indexColor = dyeColor.getId();
			
			// Tier 1 = 4 obsidian, 4 reinforced stone gives 10
			//  IC2 Reinforced stone is 1 scaffolding = 7.5 * 144 / 16 = 67.5 mB of Iron
			//  => 27 mB of Iron per Basic hull
			if (WarpDriveConfig.isIndustrialCraft2Loaded) {
				final ItemStack reinforcedStone = (ItemStack) WarpDriveConfig.getOreOrItemStack("ic2:resource", 11,       // IC2 Experimental Reinforced stone
				                                                                                "ic2:blockutility", 2 );  // IC2 Classic Reinforced stone (not cracked)
				registerShapedRecipe(groupHulls, "_ic2",
				                                       new ItemStack(WarpDrive.blockHulls_plain[EnumTier.BASIC.getIndex()][0][indexColor], 10), false, "cbc", "bXb", "cbc",
				                                       'b', reinforcedStone,
				                                       'c', Blocks.OBSIDIAN,
				                                       'X', oreDyes.get(dyeColor) );
			}
			
			// Tier 1 = 1 concrete, 3 iron bars, 1 ceramic gives 4
			//  1 Iron bar is 6 * 144 / 16 = 54 mB of Iron
			//  => 40.5 mB of Iron per Basic hull (close to reinforced stone production)
			/* TODO MC1.15 concrete mapping
			registerShapedRecipe(groupTaintedHulls, "_ceramic",
			                                       new ItemStack(WarpDrive.blockHulls_plain[EnumTier.BASIC.getIndex()][0][indexColor], 4), false, " b ", "bcb", " C ",
			                                       'c', new ItemStack(Blocks.CONCRETE[indexColor], 1),
			                                       'b', barsIron,
			                                       'C', "forge:ceramic" );
			*/
			
			// Tier 1 = 5 stone, 4 steel ingots gives 10
			// Tier 1 = 5 stone, 4 iron ingots gives 10
			//  => 57.6 mB of Iron/Steel per hull (twice more expensive using less crafting steps)
			final Object ingotSteelOrIron = WarpDriveConfig.getOreOrItemStack("forge:ingots/steel", 0,
			                                                                  "forge:ingots/refined_iron", 0,
			                                                                  "forge:ingots/iron", 0 );
			registerShapedRecipe(groupHulls, "_steel",
			                                       new ItemStack(WarpDrive.blockHulls_plain[EnumTier.BASIC.getIndex()][0][indexColor], 10), false, "cbc", "bXb", "cbc",
			                                       'b', ingotSteelOrIron,
			                                       'c', "forge:stone",
			                                       'X', oreDyes.get(dyeColor) );
			
			// Tier 1 = 5 stone, 4 bronze ingots gives 6
			//  => 96 mB of Bronze (almost twice more expensive using an common alloy) 
			if (isTagDefined("forge:ingots/bronze")) {
				registerShapedRecipe(groupHulls, "_bronze",
				                                       new ItemStack(WarpDrive.blockHulls_plain[EnumTier.BASIC.getIndex()][0][indexColor], 5), false, "cbc", "bXb", "cbc",
				                                       'b', "forge:ingots/bronze",
				                                       'c', "forge:stone",
				                                       'X', oreDyes.get(dyeColor) );
			}
			
			// Tier 1 = 5 stone, 4 aluminium ingots gives 3
			//  => 192 mB of Aluminium (very expensive with frequent but hardly used metal)
			if (isTagDefined("forge:ingots/aluminium")) {
				registerShapedRecipe(groupHulls, "_aluminium",
				                                       new ItemStack(WarpDrive.blockHulls_plain[EnumTier.BASIC.getIndex()][0][indexColor], 3), false, "cbc", "bXb", "cbc",
				                                       'b', "forge:ingots/aluminium",
				                                       'c', "forge:stone",
				                                       'X', oreDyes.get(dyeColor) );
			} else if (isTagDefined("forge:ingots/aluminum")) {
				registerShapedRecipe(groupHulls, "_aluminum",
				                                       new ItemStack(WarpDrive.blockHulls_plain[EnumTier.BASIC.getIndex()][0][indexColor], 3), false, "cbc", "bXb", "cbc",
				                                       'b', "forge:ingots/aluminum",
				                                       'c', "forge:stone",
				                                       'X', oreDyes.get(dyeColor) );
			}
		}
		
		// Tier 2 = 4 Tier 1, 4 GregTech 5 TungstenSteel reinforced block, IC2 Carbon plate, DarkSteel ingots or Obsidian, gives 4
		final Object oreObsidianTungstenSteelPlate = WarpDriveConfig.getOreOrItemStack(
				"forge:plates/tungsten_steel", 0,   // GregTech CE TungstenSteel Plate
				"ic2:crafting", 15,                 // IC2 Experimental Carbon plate
				"ic2:itemmisc", 256,                // IC2 Classic Carbon plate
				"thermalfoundation:glass", 3,       // ThermalFoundation Hardened glass
				"forge:ingots/dark_steel", 0,       // EnderIO DarkSteel ingot
				"minecraft:obsidian", 0 );
		for (final DyeColor dyeColor : DyeColor.values()) {
			final int indexColor = dyeColor.getId();
			registerShapedRecipe(groupTaintedHulls,
			                                       new ItemStack(WarpDrive.blockHulls_plain[EnumTier.ADVANCED.getIndex()][0][indexColor], 4), false, "cbc", "b b", "cbc",
			                                       'b', new ItemStack(WarpDrive.blockHulls_plain[EnumTier.BASIC.getIndex()][0][indexColor], 1),
			                                       'c', oreObsidianTungstenSteelPlate );
			registerShapedRecipe(groupTaintedHulls, "_dye",
			                                       new ItemStack(WarpDrive.blockHulls_plain[EnumTier.ADVANCED.getIndex()][0][indexColor], 4), false, "cbc", "bXb", "cbc",
			                                       'b', "warpdrive:hull1/plain",
			                                       'c', oreObsidianTungstenSteelPlate,
			                                       'X', oreDyes.get(dyeColor) );
		}
		
		// Tier 3 = 4 Tier 2, 1 GregTech Naquadah plate, IC2 Iridium plate, EnderIO Pulsating crystal or Diamond, gives 4
		final Object oreDiamondOrNaquadahPlate = WarpDriveConfig.getOreOrItemStack(
				"forge:plates/naquadah", 0,         // GregTech CE Naquadah plate
				"forge:plates/iridium_alloy", 0,    // IC2 Iridium alloy
				"forge:gems/pulsating_crystal", 0,  // EnderIO Pulsating crystal
				"forge:gems/diamond", 0 );
		for (final DyeColor dyeColor : DyeColor.values()) {
			final int indexColor = dyeColor.getId();
			registerShapedRecipe(groupTaintedHulls,
			                                       new ItemStack(WarpDrive.blockHulls_plain[EnumTier.SUPERIOR.getIndex()][0][indexColor], 4), false, " b ", "bcb", " b ",
			                                       'b', new ItemStack(WarpDrive.blockHulls_plain[EnumTier.ADVANCED.getIndex()][0][indexColor], 1),
			                                       'c', oreDiamondOrNaquadahPlate );
			registerShapedRecipe(groupTaintedHulls, "_dye",
			                                       new ItemStack(WarpDrive.blockHulls_plain[EnumTier.SUPERIOR.getIndex()][0][indexColor], 4), false, "Xb ", "bcb", " b ",
			                                       'b', "warpdrive:hull2/plain",
			                                       'c', oreDiamondOrNaquadahPlate,
			                                       'X', oreDyes.get(dyeColor) );
		}
		
		// Hull blocks variation
		for (final EnumTier enumTier : EnumTier.nonCreative()) {
			final int index = enumTier.getIndex();
			for (final DyeColor dyeColor : DyeColor.values()) {
				final int indexColor = dyeColor.getId();
				
				// crafting glass
				registerShapedRecipe(groupHulls,
				                     new ItemStack(WarpDrive.blockHulls_glass[index][indexColor], 4), false, "gpg", "pFp", "gpg",
				                     'g', "forge:glass",
				                     'p', new ItemStack(WarpDrive.blockHulls_plain[index][0][indexColor], 1),
				                     'F', "forge:dusts/glowstone" );
				
				// crafting stairs
				registerShapedRecipe(groupHulls,
				                     new ItemStack(WarpDrive.blockHulls_stairs[index][0][indexColor], 4), false, "p  ", "pp ", "ppp",
				                     'p', new ItemStack(WarpDrive.blockHulls_plain[index][0][indexColor], 1) );
				registerShapedRecipe(groupHulls,
				                     new ItemStack(WarpDrive.blockHulls_stairs[index][1][indexColor], 4), false, "p  ", "pp ", "ppp",
				                     'p', new ItemStack(WarpDrive.blockHulls_plain[index][1][indexColor], 1) );
				
				// uncrafting stairs
				registerShapelessRecipe(groupHulls,
				                        new ItemStack(WarpDrive.blockHulls_plain[index][0][indexColor], 6),
				                        WarpDrive.blockHulls_stairs[index][0][indexColor],
				                        WarpDrive.blockHulls_stairs[index][0][indexColor],
				                        WarpDrive.blockHulls_stairs[index][0][indexColor],
				                        WarpDrive.blockHulls_stairs[index][0][indexColor] );
				registerShapelessRecipe(groupHulls,
				                        new ItemStack(WarpDrive.blockHulls_plain[index][1][indexColor], 6),
				                        WarpDrive.blockHulls_stairs[index][1][indexColor],
				                        WarpDrive.blockHulls_stairs[index][1][indexColor],
				                        WarpDrive.blockHulls_stairs[index][1][indexColor],
				                        WarpDrive.blockHulls_stairs[index][1][indexColor] );
				
				// smelting tiled
				registerSmeltingRecipe(
						new ItemStack(WarpDrive.blockHulls_plain[index][0][indexColor], 1),
						new ItemStack(WarpDrive.blockHulls_plain[index][1][indexColor], 1),
						0);
				
				// uncrafting tiled
				registerShapelessRecipe(groupHulls,
				                        new ItemStack(WarpDrive.blockHulls_plain[index][0][indexColor], 1),
				                        new ItemStack(WarpDrive.blockHulls_plain[index][1][indexColor], 1));
				
				// crafting omnipanel
				registerShapedRecipe(groupHulls,
				                     new ItemStack(WarpDrive.blockHulls_omnipanel[index][indexColor], 16), false, "ggg", "ggg",
				                     'g', new ItemStack(WarpDrive.blockHulls_glass[index][indexColor], 1));
				
				// uncrafting omnipanel
				registerShapelessRecipe(groupHulls,
				                        new ItemStack(WarpDrive.blockHulls_glass[index][indexColor], 3),
				                        new ItemStack(WarpDrive.blockHulls_omnipanel[index][indexColor], 1),
				                        new ItemStack(WarpDrive.blockHulls_omnipanel[index][indexColor], 1),
				                        new ItemStack(WarpDrive.blockHulls_omnipanel[index][indexColor], 1),
				                        new ItemStack(WarpDrive.blockHulls_omnipanel[index][indexColor], 1),
				                        new ItemStack(WarpDrive.blockHulls_omnipanel[index][indexColor], 1),
				                        new ItemStack(WarpDrive.blockHulls_omnipanel[index][indexColor], 1),
				                        new ItemStack(WarpDrive.blockHulls_omnipanel[index][indexColor], 1),
				                        new ItemStack(WarpDrive.blockHulls_omnipanel[index][indexColor], 1) );
				
				// crafting slab
				registerShapedRecipe(groupHulls, "_plain_horizontal",
				                     new ItemStack(WarpDrive.blockHulls_slab[index][0][indexColor], 6), false, "bbb",
				                     'b', new ItemStack(WarpDrive.blockHulls_plain[index][0][indexColor], 1));
				registerShapedRecipe(groupHulls, "_plain_vertical",
				                     new ItemStack(WarpDrive.blockHulls_slab[index][0][indexColor], 6), false, "b", "b", "b",
				                     'b', new ItemStack(WarpDrive.blockHulls_plain[index][0][indexColor], 1));
				registerShapedRecipe(groupHulls, "_tiled_horizontal",
				                     new ItemStack(WarpDrive.blockHulls_slab[index][1][indexColor], 6), false, "bbb",
				                     'b', new ItemStack(WarpDrive.blockHulls_plain[index][1][indexColor], 1));
				registerShapedRecipe(groupHulls, "_tiled_vertical",
				                     new ItemStack(WarpDrive.blockHulls_slab[index][1][indexColor], 6), false, "b", "b", "b",
				                     'b', new ItemStack(WarpDrive.blockHulls_plain[index][1][indexColor], 1));
				
				// uncrafting slab
				registerShapedRecipe(groupHulls, "_uncrafting_h",
				                     new ItemStack(WarpDrive.blockHulls_plain[index][0][indexColor], 1), false, "s", "s",
				                     's', new ItemStack(WarpDrive.blockHulls_slab[index][0][indexColor], 1));
				registerShapedRecipe(groupHulls, "_uncrafting_v",
				                     new ItemStack(WarpDrive.blockHulls_plain[index][0][indexColor], 1), false, "ss",
				                     's', new ItemStack(WarpDrive.blockHulls_slab[index][0][indexColor], 1));
				registerShapedRecipe(groupHulls, "_uncrafting_h",
				                     new ItemStack(WarpDrive.blockHulls_plain[index][1][indexColor], 1), false, "s", "s",
				                     's', new ItemStack(WarpDrive.blockHulls_slab[index][1][indexColor], 1));
				registerShapedRecipe(groupHulls, "_uncrafting_v",
				                     new ItemStack(WarpDrive.blockHulls_plain[index][1][indexColor], 1), false, "ss",
				                     's', new ItemStack(WarpDrive.blockHulls_slab[index][1][indexColor], 1));
				registerShapelessRecipe(groupHulls, "_uncrafting_x",
				                        new ItemStack(WarpDrive.blockHulls_slab[index][0][indexColor], 2),
				                        new ItemStack(WarpDrive.blockHulls_slab[index][0][indexColor], 1));
				registerShapelessRecipe(groupHulls, "_uncrafting_y",
				                        new ItemStack(WarpDrive.blockHulls_slab[index][0][indexColor], 2),
				                        new ItemStack(WarpDrive.blockHulls_slab[index][0][indexColor], 1));
				registerShapelessRecipe(groupHulls, "_uncrafting_z",
				                        new ItemStack(WarpDrive.blockHulls_slab[index][0][indexColor], 2),
				                        new ItemStack(WarpDrive.blockHulls_slab[index][0][indexColor], 1));
				registerShapelessRecipe(groupHulls, "_uncrafting_x",
				                        new ItemStack(WarpDrive.blockHulls_slab[index][1][indexColor], 2),
				                        new ItemStack(WarpDrive.blockHulls_slab[index][1][indexColor], 1));
				registerShapelessRecipe(groupHulls, "_uncrafting_y",
				                        new ItemStack(WarpDrive.blockHulls_slab[index][1][indexColor], 2),
				                        new ItemStack(WarpDrive.blockHulls_slab[index][1][indexColor], 1));
				registerShapelessRecipe(groupHulls, "_uncrafting_z",
				                        new ItemStack(WarpDrive.blockHulls_slab[index][1][indexColor], 2),
				                        new ItemStack(WarpDrive.blockHulls_slab[index][1][indexColor], 1));
				
				// changing colors
				registerShapelessRecipe(groupTaintedHulls, "_dye",
				                        new ItemStack(WarpDrive.blockHulls_plain[index][0][indexColor], 1),
				                        oreDyes.get(dyeColor),
				                        "warpdrive:hull" + index + "/plain");
				registerShapelessRecipe(groupTaintedHulls, "_dye",
				                        new ItemStack(WarpDrive.blockHulls_glass[index][indexColor], 1),
				                        oreDyes.get(dyeColor),
				                        "warpdrive:hull" + index + "/glass");
				registerShapelessRecipe(groupTaintedHulls, "_dye",
				                        new ItemStack(WarpDrive.blockHulls_stairs[index][0][indexColor], 1),
				                        oreDyes.get(dyeColor),
				                        "warpdrive:hull" + index + "/stairs.plain");
				registerShapelessRecipe(groupTaintedHulls, "_dye",
				                        new ItemStack(WarpDrive.blockHulls_stairs[index][1][indexColor], 1),
				                        oreDyes.get(dyeColor),
				                        "warpdrive:hull" + index + "/stairs.tiled");
				registerShapedRecipe(groupTaintedHulls, "_dye",
				                     new ItemStack(WarpDrive.blockHulls_plain[index][0][indexColor], 8), false, "###", "#X#", "###",
				                     '#', "warpdrive:hull" + index + "/plain",
				                     'X', oreDyes.get(dyeColor) );
				registerShapedRecipe(groupTaintedHulls, "_dye",
				                     new ItemStack(WarpDrive.blockHulls_glass[index][indexColor], 8), false, "###", "#X#", "###",
				                     '#', "warpdrive:hull" + index + "/glass",
				                     'X', oreDyes.get(dyeColor) );
				registerShapedRecipe(groupTaintedHulls, "_dye",
				                     new ItemStack(WarpDrive.blockHulls_stairs[index][0][indexColor], 8), false, "###", "#X#", "###",
				                     '#', "warpdrive:hull" + index + "/stairs.plain",
				                     'X', oreDyes.get(dyeColor) );
				registerShapedRecipe(groupTaintedHulls, "_dye",
				                     new ItemStack(WarpDrive.blockHulls_stairs[index][1][indexColor], 8), false, "###", "#X#", "###",
				                     '#', "warpdrive:hull" + index + "/stairs.plain",
				                     'X', oreDyes.get(dyeColor) );
			}
		}
	}
	
	private static void initMovement() {
		// Ship core
		// note:
		// - we want to recycle the previous tier
		// - Ship controller should be more expensive than the core, so it can't be used as ingredient
		// basic    (shuttle ) is 1 Diamond crystal, 3 Redstone dust     , 1 Power interface, 1 Memory crystal, 1 MV Machine casing, 1 Computer interface
		// advanced (corvette) is 1 Emerald crystal, 3 Capacitive crystal, 1 Power interface, 1 Memory crystal, 1 MV Machine casing, 1 basic Ship core
		// superior (frigate ) is 1 Nether star    , 3 Capacitive cluster, 1 Superconductor , 1 Memory cluster, 1 HV Machine casing, 1 advanced Ship core
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockShipCores[EnumTier.BASIC.getIndex()]),"ce ", "pmc", "cCM",
		                                       'e', ItemComponent.getItem(EnumComponentType.DIAMOND_CRYSTAL),
		                                       'c', Items.REDSTONE,
		                                       'p', ItemComponent.getItem(EnumComponentType.POWER_INTERFACE),
		                                       'M', ItemComponent.getItem(EnumComponentType.MEMORY_CRYSTAL),
		                                       'm', itemStackMachineCasings[0],
		                                       'C', ItemComponent.getItem(EnumComponentType.COMPUTER_INTERFACE) );
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockShipCores[EnumTier.ADVANCED.getIndex()]),"ce ", "pmc", "cCM",
		                                       'e', ItemComponent.getItem(EnumComponentType.EMERALD_CRYSTAL),
		                                       'c', ItemComponent.getItem(EnumComponentType.CAPACITIVE_CRYSTAL),
		                                       'p', ItemComponent.getItem(EnumComponentType.POWER_INTERFACE),
		                                       'M', ItemComponent.getItem(EnumComponentType.MEMORY_CRYSTAL),
		                                       'm', itemStackMachineCasings[1],
		                                       'C', new ItemStack(WarpDrive.blockShipCores[EnumTier.BASIC.getIndex()]) );
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockShipCores[EnumTier.SUPERIOR.getIndex()]),"ce ", "pmc", "cCM",
		                                       'e', Items.NETHER_STAR,
		                                       'c', ItemComponent.getItem(EnumComponentType.CAPACITIVE_CLUSTER),
		                                       'p', ItemComponent.getItem(EnumComponentType.SUPERCONDUCTOR),
		                                       'M', ItemComponent.getItem(EnumComponentType.MEMORY_CLUSTER),
		                                       'm', itemStackMachineCasings[2],
		                                       'C', new ItemStack(WarpDrive.blockShipCores[EnumTier.ADVANCED.getIndex()]) );
		
		// Remote ship controller
		// basic    is 1 Ender pearl, 3 Ender coil, 1 Diamond crystal, 1 LV Machine casing, 1 Memory crystal, 1 Ender coil, 1 Computer interface
		// advanced is 1 Ender pearl, 3 Ender coil, 1 Emerald crystal, 1 MV Machine casing, 1 Memory cluster, 2 Ender coil, 1 basic Ship controller
		// superior is 1 Ender pearl, 3 Ender coil, 1 Nether star    , 1 HV Machine casing, 1 Memory cluster, 4 Ender coil, 1 advanced Ship controller
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockShipControllers[EnumTier.BASIC.getIndex()]), false, "ce ", "pmc", "cCM",
		                                       'e', ItemComponent.getItem(EnumComponentType.DIAMOND_CRYSTAL),
		                                       'c', ItemComponent.getItem(EnumComponentType.ENDER_COIL),
		                                       'p', Items.ENDER_PEARL,
		                                       'M', ItemComponent.getItem(EnumComponentType.MEMORY_CRYSTAL),
		                                       'm', itemStackMachineCasings[0],
		                                       'C', ItemComponent.getItem(EnumComponentType.COMPUTER_INTERFACE) );
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockShipControllers[EnumTier.ADVANCED.getIndex()]), false, "ce ", "pmc", "cCM",
		                                       'e', ItemComponent.getItem(EnumComponentType.EMERALD_CRYSTAL),
		                                       'c', ItemComponent.getItem(EnumComponentType.ENDER_COIL),
		                                       'p', Items.ENDER_PEARL,
		                                       'M', ItemComponent.getItem(EnumComponentType.MEMORY_CLUSTER),
		                                       'm', itemStackMachineCasings[1],
		                                       'C', new ItemStack(WarpDrive.blockShipControllers[EnumTier.BASIC.getIndex()]) );
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockShipControllers[EnumTier.SUPERIOR.getIndex()]), false, "ce ", "pmc", "cCM",
		                                       'e', Items.NETHER_STAR,
		                                       'c', ItemComponent.getItem(EnumComponentType.ENDER_COIL),
		                                       'p', Items.ENDER_PEARL,
		                                       'M', ItemComponent.getItem(EnumComponentType.MEMORY_CLUSTER),
		                                       'm', itemStackMachineCasings[2],
		                                       'C', new ItemStack(WarpDrive.blockShipControllers[EnumTier.ADVANCED.getIndex()]) );
		
		// Laser lift is ...
		final Object enderPearlOrMagnetizer = WarpDriveConfig.getOreOrItemStack(
				"gregtech:machine", 420,            // Gregtech Basic polarizer
				"ic2:te", 37,                       // IC2 Experimental Magnetizer
				"ic2:blockmachinelv", 10,           // IC2 Classic Magnetizer
				"forge:ingots/pulsating_iron", 0,   // EnderIO iron ingot with ender pearl
				"minecraft:ender_pearl", 0 );
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockLift), false, "wlw", "per", "glg",
		                                       'r', Items.REDSTONE,
		                                       'w', "minecraft:wool",
		                                       'l', ItemComponent.getItem(EnumComponentType.LENS),
		                                       'e', enderPearlOrMagnetizer,
		                                       'p', ItemComponent.getItem(EnumComponentType.POWER_INTERFACE),
		                                       'g', "forge:glass_panes/colorless");
		
		// Transporter Beacon is 1 Ender pearl, 1 Memory crystal, 1 Diamond crystal, 2 Sticks
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockTransporterBeacon), false, " e ", " m ", "sds",
		                                       'e', Items.ENDER_PEARL,
		                                       'd', ItemComponent.getItem(EnumComponentType.DIAMOND_CRYSTAL),
		                                       'm', ItemComponent.getItem(EnumComponentType.MEMORY_CRYSTAL),
		                                       's', Items.STICK);
		
		// Transporter containment is 1 HV Machine casing, 2 Ender crystal, gives 2
		if (!WarpDriveConfig.ACCELERATOR_ENABLE) {
			registerShapedRecipe(groupMachines,
			                                       new ItemStack(WarpDrive.blockTransporterContainment, 2), false, " e ", " m ", " e ",
			                                       'm', itemStackMachineCasings[2],
			                                       'e', ItemComponent.getItem(EnumComponentType.ENDER_COIL));
		} else {
			registerShapedRecipe(groupMachines,
			                                       new ItemStack(WarpDrive.blockTransporterContainment, 2), false, " e ", " m ", " e ",
			                                       'm', "warpdrive:electromagnet2",
			                                       'e', ItemComponent.getItem(EnumComponentType.ENDER_COIL));
		}
		
		// Transporter core is 1 HV Machine casing, 1 Emerald crystal, 1 Capacitive crystal, 1 Diamond crystal, 1 Power interface, 1 Computer interface
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockTransporterCore), false, " E ", "pmd", " c ",
		                                       'm', itemStackMachineCasings[2],
		                                       'c', ItemComponent.getItem(EnumComponentType.COMPUTER_INTERFACE),
		                                       'd', ItemComponent.getItem(EnumComponentType.DIAMOND_CRYSTAL),
		                                       'E', ItemComponent.getItem(EnumComponentType.EMERALD_CRYSTAL),
		                                       'p', ItemComponent.getItem(EnumComponentType.POWER_INTERFACE));
		
		// Transporter scanner is 1 HV Machine casing, 1 Emerald crystal, 3 Capacitive crystal, 2 Ender crystal
		if (!WarpDriveConfig.ACCELERATOR_ENABLE) {
			registerShapedRecipe(groupMachines,
			                                       new ItemStack(WarpDrive.blockTransporterScanner), false, " E ", "eme", "CCC",
			                                       'm', itemStackMachineCasings[2],
			                                       'e', ItemComponent.getItem(EnumComponentType.ENDER_COIL),
			                                       'E', ItemComponent.getItem(EnumComponentType.EMERALD_CRYSTAL),
			                                       'C', ItemComponent.getItem(EnumComponentType.CAPACITIVE_CRYSTAL));
		} else {
			registerShapedRecipe(groupMachines,
			                                       new ItemStack(WarpDrive.blockTransporterScanner), false, " E ", "eme", "CCC",
			                                       'm', "warpdrive:electromagnet2",
			                                       'e', ItemComponent.getItem(EnumComponentType.ENDER_COIL),
			                                       'E', ItemComponent.getItem(EnumComponentType.EMERALD_CRYSTAL),
			                                       'C', ItemComponent.getItem(EnumComponentType.CAPACITIVE_CRYSTAL));
		}
	}
	
	private static void initWeapon() {
		// Laser cannon is 2 Motors, 1 Diffraction grating, 1 lens, 1 Computer interface, 1 HV Machine casing, 1 Redstone dust, 2 Glass pane
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockLaser), false, "gtr", "ldm", "gt ",
		                                       't', itemStackMotors[2],
		                                       'd', ItemComponent.getItem(EnumComponentType.DIFFRACTION_GRATING),
		                                       'l', ItemComponent.getItem(EnumComponentType.LENS),
		                                       'm', itemStackMachineCasings[1],
		                                       'r', Items.REDSTONE,
		                                       'g', "forge:glass_panes/colorless");
		
		// Laser camera is just Laser + Camera
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockLaserCamera), false, "rlr", "rsr", "rcr",
		                                       'r', rubber,
		                                       's', goldNuggetOrBasicCircuit,
		                                       'l', WarpDrive.blockLaser,
		                                       'c', WarpDrive.blockCamera );
		
		// Weapon controller is diamond sword with Ship controller
		registerShapedRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockWeaponController), false, "rwr", "msm", "rcr",
		                                       'r', rubber,
		                                       's', ItemComponent.getItem(EnumComponentType.EMERALD_CRYSTAL),
		                                       'm', ItemComponent.getItem(EnumComponentType.MEMORY_CRYSTAL),
		                                       'w', Items.DIAMOND_SWORD,
		                                       'c', WarpDrive.blockShipControllers[EnumTier.ADVANCED.getIndex()] );
	}
	
	/*
	public static Ingredient getIngredient(final Object object) {
		if (object instanceof ItemStack) {
			return Ingredient.fromStacks((ItemStack) object);
		}
		if (object instanceof Item) {
			return Ingredient.fromItem((Item) object);
		}
		if (object instanceof String) {
			return new OreIngredient((String) object);
		}
		final ItemStack itemStack = ItemStack.EMPTY;
		if (object != null) {
			itemStack.setStackDisplayName(object.toString());
		}
		return Ingredient.fromStacks(itemStack);
	}
	/**/
	
	private static void removeRecipe(final ItemStack itemStackOutputOfRecipeToRemove) {
		/* TODO MC1.15 ICBM compatibility
		ResourceLocation recipeToRemove = null;
		for (final Entry<ResourceLocation, IRecipe> entryRecipe : ForgeRegistries.RECIPES.getEntries()) {
			final IRecipe recipe = entryRecipe.getValue();
			final ItemStack itemStackRecipeOutput = recipe.getRecipeOutput();
			if ( !itemStackRecipeOutput.isEmpty()
			  && itemStackRecipeOutput.isItemEqual(itemStackOutputOfRecipeToRemove) ) {
				recipeToRemove = entryRecipe.getKey();
				break;
			}
		}
		if (recipeToRemove == null) {
			WarpDrive.logger.error(String.format("Unable to find any recipe to remove with output %s", itemStackOutputOfRecipeToRemove));
		} else {
			WarpDrive.logger.info(String.format("Removing recipe %s with output %s", recipeToRemove, itemStackOutputOfRecipeToRemove));
			((ForgeRegistry<IRecipe>) ForgeRegistries.RECIPES).remove(recipeToRemove);
		}
		*/
	}
}
