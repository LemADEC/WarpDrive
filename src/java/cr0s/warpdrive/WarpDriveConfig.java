package cr0s.warpdrive;

import ic2.api.item.IC2Items;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.oredict.OreDictionary;
import advsolar.api.ASPItemAPI;
import appeng.api.definitions.IBlocks;
import cpw.mods.fml.common.Loader;
import cr0s.warpdrive.data.TransitionPlane;
import dan200.computercraft.ComputerCraft;

public class WarpDriveConfig {
	private static Configuration config;

	/*
	 * The variables which store whether or not individual mods are loaded
	 */
	public static boolean isForgeMultipartLoaded = false;
	public static boolean isAppliedEnergisticsLoaded = false;
	public static boolean isAdvSolPanelLoaded = false;
	public static boolean isAtomicScienceLoaded = false;
	public static boolean isAEExtraLoaded = false;
	public static boolean isICBMLoaded = false;
	public static boolean isMFFSLoaded = false;
	public static boolean isGraviSuiteLoaded = false;
	public static boolean isICLoaded = false;
	public static boolean isCCLoaded = false;
	public static boolean isUndergroundBiomesLoaded = false;
	public static boolean isNetherOresLoaded = false;
	public static boolean isThermalExpansionLoaded = false;
	public static boolean isAdvancedRepulsionSystemsLoaded = false;
	public static boolean isMagicalCropsLoaded = false;

	// ForgeMultipart (microblocks) support
	public static Method forgeMultipart_helper_createTileFromNBT = null;
	public static Method forgeMultipart_helper_sendDescPacket = null;
	public static Method forgeMultipart_tileMultipart_onChunkLoad = null;

	public static ItemStack IC2_Air;
	public static ItemStack IC2_Empty;
	public static ItemStack IC2_RubberWood;
	public static ItemStack IC2_Resin;
	public static Item IC2_fluidCell;
	public static Block CC_Computer, CC_peripheral, CCT_Turtle, CCT_Upgraded, CCT_Advanced;

	public static Item CC_Floppy;
	public static ItemStack GT_Ores, GT_Granite, GT_Machine;
	public static ItemStack ASP;
	public static int AS_Turbine, AS_deuteriumCell;
	public static int ICBM_Machine, ICBM_Missile, ICBM_Explosive;
	public static ItemStack GS_ultimateLappack;
	public static int UB_igneousStone, UB_igneousCobblestone, UB_metamorphicStone, UB_metamorphicCobblestone, UB_sedimentaryStone;
	public static int NetherOres_count;
	public static int[] NetherOres_block;
	public static ArrayList<Integer> forceFieldBlocks;

	public static ArrayList<Block> minerOres, minerLogs, minerLeaves, scannerIgnoreBlocks;
	public static ArrayList<Item> spaceHelmets, jetpacks;
	private static IBlocks AEBlocks;
	private static Class<?> AEMaterials;
	private static Class<?> AEItems;
	public static ArrayList<Block> commonWorldGenOres;
	public static Item AEExtra_fluidDrive;
	public static Block AEExtra_certusQuartzTank;

	// Mod configuration (see loadWarpDriveConfig() for comments/definitions)
	// General
	public static int G_SPACE_PROVIDER_ID = 14;
	public static int G_SPACE_DIMENSION_ID = -2;
	public static int G_HYPERSPACE_PROVIDER_ID = 15;
	public static int G_HYPERSPACE_DIMENSION_ID = -3;
	public static int G_SPACE_WORLDBORDER_BLOCKS = 100000;
	public static final int LUA_SCRIPTS_NONE = 0;
	public static final int LUA_SCRIPTS_TEMPLATES = 1;
	public static final int LUA_SCRIPTS_ALL = 2;
	public static int G_LUA_SCRIPTS = LUA_SCRIPTS_ALL;
	public static boolean G_DEBUGMODE = false;
	public static String G_SCHEMALOCATION = "warpDrive_schematics";
	public static int G_BLOCKS_PER_TICK = 3500;

	public static boolean G_ENABLE_IC2_RECIPES = true;
	public static boolean G_ENABLE_VANILLA_RECIPES = false;
	public static boolean G_ENABLE_TDK_RECIPES = false;

	// Transition planes
	public static TransitionPlane[] G_TRANSITIONPLANES = null;

	// Warp Drive Core
	public static int WC_MAX_ENERGY_VALUE = 100000000;
	public static int WC_ENERGY_PER_BLOCK_MODE1 = 10;
	public static int WC_ENERGY_PER_DISTANCE_MODE1 = 100;
	public static int WC_ENERGY_PER_BLOCK_MODE2 = 1000;
	public static int WC_ENERGY_PER_DISTANCE_MODE2 = 1000;
	public static int WC_ENERGY_PER_ENTITY_TO_SPACE = 1000000;
	public static int WC_MAX_JUMP_DISTANCE = 128;
	public static int WC_MAX_SHIP_VOLUME_ON_SURFACE = 3000;
	public static int WC_MIN_SHIP_VOLUME_FOR_HYPERSPACE = 1200;
	public static int WC_MAX_SHIP_SIDE = 127;
	public static int WC_COOLDOWN_INTERVAL_SECONDS = 30;
	public static int WC_COLLISION_TOLERANCE_BLOCKS = 3;
	public static int WC_WARMUP_SHORTJUMP_SECONDS = 10;
	public static int WC_WARMUP_LONGJUMP_SECONDS = 30;
	public static int WC_WARMUP_RANDOM_TICKS = 60;
	public static int WC_CORES_REGISTRY_UPDATE_INTERVAL_SECONDS = 10;
	public static int WC_ISOLATION_UPDATE_INTERVAL_SECONDS = 10;
	public static String[] WC_UNLIMITED_PLAYERNAMES = { "notch", "someone" };

	// Warp Radar
	public static int WR_MAX_ENERGY_VALUE = 100000000; // 100kk eU
	public static int WR_MAX_ISOLATION_RANGE = 2;
	public static int WR_MIN_ISOLATION_BLOCKS = 5;
	public static int WR_MAX_ISOLATION_BLOCKS = 132;
	public static double WR_MIN_ISOLATION_EFFECT = 0.12;
	public static double WR_MAX_ISOLATION_EFFECT = 1.00;

	// Ship Scanner
	public static int SS_MAX_ENERGY_VALUE = 500000000;
	public static int SS_EU_PER_BLOCK_SCAN = 100; // eU per block of ship volume
	// (including air)
	public static int SS_EU_PER_BLOCK_DEPLOY = 5000;
	public static int SS_MAX_DEPLOY_RADIUS_BLOCKS = 50;

	// Particle Booster
	public static int PB_MAX_ENERGY_VALUE = 100000;

	// Laser Emitter
	public static int LE_MAX_BOOSTERS_NUMBER = 10;
	public static int LE_MAX_LASER_ENERGY = 4000000;
	public static int LE_EMIT_DELAY_TICKS = 20 * 3;
	public static int LE_EMIT_SCAN_DELAY_TICKS = 10;

	public static double LE_COLLECT_ENERGY_MULTIPLIER = 0.60D;
	public static int LE_BEAM_LENGTH_PER_ENERGY_DIVIDER = 5000;
	public static int LE_ENTITY_HIT_SET_ON_FIRE_TIME = 100;
	public static int LE_ENTITY_HIT_DAMAGE_PER_ENERGY_DIVIDER = 10000;
	public static int LE_ENTITY_HIT_EXPLOSION_LASER_ENERGY = 1000000;
	public static int LE_BLOCK_HIT_CONSUME_ENERGY = 70000;
	public static int LE_BLOCK_HIT_CONSUME_ENERGY_PER_BLOCK_RESISTANCE = 1000;
	public static int LE_BLOCK_HIT_CONSUME_ENERGY_PER_DISTANCE = 10;

	// Mining Laser
	// BuildCraft quarry values for reference
	// - harvesting one block is 60 MJ/block = 600 RF/block = ~145 EU/block
	// - maximum speed is 3.846 ticks per blocks
	// - overall consumption varies from 81.801 to 184.608 MJ/block (depending
	// on speed) = up to 1846.08 RF/block = up to ~448 EU/block
	// - at radius 5, one layer takes ~465 ticks ((ML_MAX_RADIUS * 2 + 1) ^ 2 *
	// 3.846)
	// - overall consumption is ((ML_MAX_RADIUS * 2 + 1) ^ 2) * 448 => ~ 54208
	// EU/layer
	// WarpDrive mining laser in comparison
	// - each mined layer is scanned twice
	// - default ore generation: 1 ore out of 25 blocks
	// - overall consumption in 'all, space' is ML_EU_PER_LAYER_SPACE /
	// ((ML_MAX_RADIUS * 2 + 1) ^ 2) + ML_EU_PER_BLOCK_SPACE => ~ 356 EU/block
	// - overall consumption in 'all, space' is ML_EU_PER_LAYER_SPACE +
	// ((ML_MAX_RADIUS * 2 + 1) ^ 2) * ML_EU_PER_BLOCK_SPACE => ~ 43150 EU/layer
	// - overall consumption in 'ores, space' is ML_EU_PER_LAYER_SPACE +
	// ((ML_MAX_RADIUS * 2 + 1) ^ 2) * ML_EU_PER_BLOCK_SPACE *
	// ML_EU_MUL_ORESONLY / 25 => ~ 28630 EU/layer
	// - at radius 5, one layer takes 403 ticks (2 * ML_SCAN_DELAY_TICKS +
	// ML_MINE_DELAY_TICKS * (ML_MAX_RADIUS * 2 + 1) ^ 2)
	public static int ML_MAX_BOOSTERS_NUMBER = 1;
	public static int ML_WARMUP_DELAY_TICKS = 20;
	public static int ML_SCAN_DELAY_TICKS = 20;
	public static int ML_MINE_DELAY_TICKS = 3;
	public static int ML_EU_PER_LAYER_SPACE = 25000;
	public static int ML_EU_PER_LAYER_EARTH = 35000;
	public static int ML_EU_PER_BLOCK_SPACE = 150;
	public static int ML_EU_PER_BLOCK_EARTH = 300;
	public static double ML_EU_MUL_ORESONLY = 5.0; // lower value encourages to
	// keep the land 'clean'
	public static double ML_EU_MUL_SILKTOUCH = 2.5;
	public static double ML_DEUTERIUM_MUL_SILKTOUCH = 1.0;
	public static double ML_EU_MUL_FORTUNE = 2.5;
	public static int ML_MAX_RADIUS = 5;

	// Tree farm
	public static int TF_MAX_SIZE = 16;

	// Cloaking device core
	public static int CD_MAX_CLOAKING_FIELD_SIDE = 100;
	public static int CD_ENERGY_PER_BLOCK_TIER1 = 125;
	public static int CD_ENERGY_PER_BLOCK_TIER2 = 500;
	public static int CD_FIELD_REFRESH_INTERVAL_SECONDS = 3;
	public static int CD_COIL_CAPTURE_BLOCKS = 5;

	// Air generator
	public static int AG_RF_PER_CANISTER = 20;

	// Reactor monitor
	public static int RM_MAX_ENERGY = 1000000;
	public static double RM_EU_PER_HEAT = 2;

	// Transporter
	public static int TR_MAX_ENERGY = 1000000;
	public static boolean TR_RELATIVE_COORDS = true;
	public static double TR_EU_PER_METRE = 100;
	// public static double TR_MAX_SCAN_RANGE = 4; FIXME: not used ?!?
	public static double TR_MAX_BOOST_MUL = 4;

	// Power reactor
	public static int PR_MAX_ENERGY = 100000000;
	public static int PR_TICK_TIME = 5;
	public static int PR_MAX_LASERS = 6;

	// Power store
	public static int PS_MAX_ENERGY = 1000000;

	// Laser Lift
	public static int LL_MAX_ENERGY = 2400;
	public static int LL_LIFT_ENERGY = 800;
	public static int LL_TICK_RATE = 10;

	// Chunk Loader
	public static int CL_MAX_ENERGY = 1000000;
	public static int CL_MAX_DISTANCE = 2;
	public static int CL_RF_PER_CHUNKTICK = 320;

	public static ItemStack getIC2Item(String id) {
		return IC2Items.getItem(id);
	}

	public static ItemStack getAEBlock(String id) {
		try {
			Object ret = AEBlocks.getField(id).get(null);
			if (ret instanceof ItemStack)
				return (ItemStack) ret;
		} catch (Exception e) {
			WarpDrive.debugPrint("WarpDriveConfig Call getAEBlock failed for " + id);
		}
		return null;
	}

	public static ItemStack getAEMaterial(String id) {
		try {
			Object ret = AEMaterials.getField(id).get(null);
			if (ret instanceof ItemStack)
				return (ItemStack) ret;
		} catch (Exception e) {
			WarpDrive.debugPrint("WarpDriveConfig Call getAEMaterial failed for " + id);
		}
		return null;
	}

	public static ItemStack getAEItem(String id) {
		try {
			Object ret = AEItems.getField(id).get(null);
			if (ret instanceof ItemStack)
				return (ItemStack) ret;
		} catch (Exception e) {
			WarpDrive.debugPrint("WarpDriveConfig Call getAEItem failed for " + id);
		}
		return null;
	}

	public static void preInit(Configuration configIn) {
		config = configIn;
	}

	public static void loadWarpDriveConfig() {
		// General
		G_SPACE_PROVIDER_ID = config.get("General", "space_provider_id", G_SPACE_PROVIDER_ID, "Space dimension provider ID").getInt();
		G_SPACE_DIMENSION_ID = config.get("General", "space_dimension_id", G_SPACE_DIMENSION_ID, "Space dimension world ID").getInt();
		G_HYPERSPACE_PROVIDER_ID = config.get("General", "hyperspace_provider_id", G_HYPERSPACE_PROVIDER_ID, "Hyperspace dimension provider ID").getInt();
		G_HYPERSPACE_DIMENSION_ID = config.get("General", "hyperspace_dimension_id", G_HYPERSPACE_DIMENSION_ID, "Hyperspace dimension world ID").getInt();
		G_SPACE_WORLDBORDER_BLOCKS = config.get("General", "space_worldborder_blocks", G_SPACE_WORLDBORDER_BLOCKS,
				"World border applied to hyperspace & space, set to 0 to disable it").getInt();
		G_LUA_SCRIPTS = config.get("General", "lua_scripts", G_LUA_SCRIPTS,
				"LUA scripts to load when connecting machines: 0 = none, 1 = templates in a subfolder, 2 = ready to roll (templates are still provided)")
				.getInt();
		G_DEBUGMODE = config.get("General", "debug_mode", G_DEBUGMODE, "Detailled logs to help debug the mod, enable it before reporting a bug").getBoolean(
				false);
		G_SCHEMALOCATION = config.get("General", "schematic_location", G_SCHEMALOCATION, "Folder where to save ship schematics").getString();
		G_BLOCKS_PER_TICK = config.get("General", "blocks_per_tick", G_BLOCKS_PER_TICK,
				"Number of blocks to move per ticks, too high will cause lag spikes on ship jumping or deployment, too low may break the ship wirings")
				.getInt();

		G_ENABLE_IC2_RECIPES = config.get("General", "enable_ic2_recipes", G_ENABLE_IC2_RECIPES, "Original recipes based on IndustrialCrat2 by Cr0s")
				.getBoolean(true);
		G_ENABLE_VANILLA_RECIPES = config.get("General", "enable_vanilla_recipes", G_ENABLE_VANILLA_RECIPES, "Vanilla recipes by DarkholmeTenk").getBoolean(
				false);
		G_ENABLE_TDK_RECIPES = config
				.get("General",
						"enable_TDK_recipes",
						G_ENABLE_TDK_RECIPES,
						"Mixed recipes for TDK packs by Lem'ADEC (currently requires at least AppliedEnergistics, Extracells, AtomicScience, IndustrialCraft2, GraviSuite and ThermalExpansion")
						.getBoolean(false);

		// TransitionPlane
		config.addCustomCategoryComment("TransitionPlane",
				"Transition planes defines which region in space allows to go to other dimensions, default is overworld with 100k radius.\n"
						+ "Each plane is square shaped and defined as a list of 7 integers (all measured in blocks, border is the radius from center)");
		String[] transitionNames = { "overworld" };
		transitionNames = config.get("TransitionPlane", "names", transitionNames, "this is the list of transition planes defined hereafter").getStringList();
		int[] defaultPlane = { 0, 0, 0, 30000000, 30000000, 0, 0 }; // 30000000
		// is
		// Minecraft
		// limit for
		// SetBlock
		G_TRANSITIONPLANES = new TransitionPlane[transitionNames.length];
		int index = 0;
		for (String name : transitionNames) {
			int[] plane = config.get("TransitionPlane", name, defaultPlane,
					"dimensionId, dimensionCenterX, dimensionCenterZ, borderSizeX, borderSizeZ, SpaceCenterX, SpaceCenterZ").getIntList();
			if (plane.length != 7) {
				WarpDrive.print("Invalid transition plane definition '" + name + "' (exactly 7 integers are expected), using default instead");
				plane = defaultPlane.clone();
			}
			TransitionPlane newPlane = new TransitionPlane(plane[0], plane[1], plane[2], plane[3], plane[4], plane[5], plane[6]);
			WarpDrive.print("Adding '" + name + "' as " + newPlane.toString());
			G_TRANSITIONPLANES[index] = newPlane;
		}
		// FIXME: check transition planes aren't overlapping
		// FIXME: check transition planes have valid dimension id

		// Warp Core
		WC_MAX_ENERGY_VALUE = config.get("WarpCore", "max_energy_value", WC_MAX_ENERGY_VALUE, "Maximum energy storage").getInt();
		WC_ENERGY_PER_BLOCK_MODE1 = config.get("WarpCore", "energy_per_block_mode1", WC_ENERGY_PER_BLOCK_MODE1).getInt();
		WC_ENERGY_PER_DISTANCE_MODE1 = config.get("WarpCore", "energy_per_distance_mode1", WC_ENERGY_PER_DISTANCE_MODE1).getInt();
		WC_ENERGY_PER_DISTANCE_MODE2 = config.get("WarpCore", "energy_per_distance_mode2", WC_ENERGY_PER_DISTANCE_MODE2).getInt();
		WC_ENERGY_PER_BLOCK_MODE2 = config.get("WarpCore", "energy_per_block_mode2", WC_ENERGY_PER_BLOCK_MODE2).getInt();
		WC_ENERGY_PER_ENTITY_TO_SPACE = config.get("WarpCore", "energy_per_entity_to_space", WC_ENERGY_PER_ENTITY_TO_SPACE).getInt();
		WC_MAX_JUMP_DISTANCE = config.get("WarpCore", "max_jump_distance", WC_MAX_JUMP_DISTANCE, "Maximum jump lenght value in blocks").getInt();
		WC_MAX_SHIP_VOLUME_ON_SURFACE = config.get("WarpCore", "max_ship_volume_on_surface", WC_MAX_SHIP_VOLUME_ON_SURFACE,
				"Maximum ship mass (in blocks) to jump on earth").getInt();
		WC_MIN_SHIP_VOLUME_FOR_HYPERSPACE = config.get("WarpCore", "min_ship_volume_for_hyperspace", WC_MIN_SHIP_VOLUME_FOR_HYPERSPACE,
				"Minimum ship mass (in blocks) to enter or exit hyperspace without a jumpgate").getInt();
		WC_MAX_SHIP_SIDE = config.get("WarpCore", "max_ship_side", WC_MAX_SHIP_SIDE, "Maximum ship size on each axis in blocks").getInt();
		WC_COLLISION_TOLERANCE_BLOCKS = config.get("WarpCore", "collision_tolerance_blocks", WC_COLLISION_TOLERANCE_BLOCKS,
				"Tolerance in block in case of collision before causing damages...").getInt();
		WC_COOLDOWN_INTERVAL_SECONDS = config.get("WarpCore", "cooldown_interval_seconds", WC_COOLDOWN_INTERVAL_SECONDS,
				"Cooldown seconds to wait after jumping").getInt();
		WC_WARMUP_SHORTJUMP_SECONDS = config.get("WarpCore", "warmup_shortjump_seconds", WC_WARMUP_SHORTJUMP_SECONDS, "Short jump means less than 50 blocks")
				.getInt();
		WC_WARMUP_LONGJUMP_SECONDS = config.get("WarpCore", "warmup_longjump_seconds", WC_WARMUP_LONGJUMP_SECONDS, "Long jump means more than 50 blocks")
				.getInt();

		WC_CORES_REGISTRY_UPDATE_INTERVAL_SECONDS = config.get("WarpCore", "cores_registry_update_interval", WC_CORES_REGISTRY_UPDATE_INTERVAL_SECONDS,
				"(measured in seconds)").getInt();
		WC_ISOLATION_UPDATE_INTERVAL_SECONDS = config.get("WarpCore", "isolation_update_interval", WC_ISOLATION_UPDATE_INTERVAL_SECONDS,
				"(measured in seconds)").getInt();
		WC_UNLIMITED_PLAYERNAMES = config.get("WarpCore", "unlimited_playernames", WC_UNLIMITED_PLAYERNAMES,
				"List of player names which gives unlimited block counts to their ship").getStringList();

		// Warp Radar
		WR_MAX_ENERGY_VALUE = config.get("WarpRadar", "max_energy_value", WR_MAX_ENERGY_VALUE).getInt();
		WR_MAX_ISOLATION_RANGE = config.get("WarpRadar", "max_isolation_range", WR_MAX_ISOLATION_RANGE,
				"radius around core where isolation blocks count (2 to 8), higher is lagger").getInt();
		WR_MAX_ISOLATION_RANGE = Math.min(8, Math.max(WR_MAX_ISOLATION_RANGE, 2));
		WR_MIN_ISOLATION_BLOCKS = config.get("WarpRadar", "min_isolation_blocks", WR_MIN_ISOLATION_BLOCKS,
				"number of isolation blocks required to get some isolation (0 to 20)").getInt();
		WR_MIN_ISOLATION_BLOCKS = Math.min(20, Math.max(WR_MIN_ISOLATION_BLOCKS, 0));
		WR_MAX_ISOLATION_BLOCKS = config.get("WarpRadar", "max_isolation_blocks", WR_MAX_ISOLATION_BLOCKS,
				"number of isolation blocks required to reach maximum effect (5 to 100)").getInt();
		WR_MAX_ISOLATION_BLOCKS = Math.min(100, Math.max(WR_MAX_ISOLATION_BLOCKS, 5));
		WR_MIN_ISOLATION_EFFECT = config.get("WarpRadar", "min_isolation_effect", WR_MIN_ISOLATION_EFFECT,
				"isolation effect achieved with min number of isolation blocks (0.01 to 0.95)").getDouble(0.12D);
		WR_MIN_ISOLATION_EFFECT = Math.min(0.95D, Math.max(WR_MIN_ISOLATION_EFFECT, 0.01D));
		WR_MAX_ISOLATION_EFFECT = config.get("WarpRadar", "max_isolation_effect", WR_MAX_ISOLATION_EFFECT,
				"isolation effect achieved with max number of isolation blocks (0.01 to 1.00)").getDouble(1.00D);
		WR_MAX_ISOLATION_EFFECT = Math.min(1.0D, Math.max(WR_MAX_ISOLATION_EFFECT, 0.01D));

		// Ship Scanner
		SS_MAX_ENERGY_VALUE = config.get("WarpCore", "max_energy_value", SS_MAX_ENERGY_VALUE, "Maximum energy storage").getInt();
		SS_EU_PER_BLOCK_SCAN = config.get("ShipScanner", "energy_per_block_when_scanning", SS_EU_PER_BLOCK_SCAN,
				"Energy consummed per block when scanning a ship (use -1 to consume everything)").getInt();
		if (SS_EU_PER_BLOCK_SCAN != -1) {
			SS_EU_PER_BLOCK_SCAN = Math.min(SS_MAX_ENERGY_VALUE, Math.max(SS_EU_PER_BLOCK_SCAN, 1));
		}
		SS_EU_PER_BLOCK_DEPLOY = config.get("ShipScanner", "energy_per_block_when_deploying", SS_EU_PER_BLOCK_DEPLOY,
				"Energy consummed per block when deploying a ship (use -1 to consume everything)").getInt();
		if (SS_EU_PER_BLOCK_DEPLOY != -1) {
			SS_EU_PER_BLOCK_DEPLOY = Math.min(SS_MAX_ENERGY_VALUE, Math.max(SS_EU_PER_BLOCK_DEPLOY, 1));
		}
		SS_MAX_DEPLOY_RADIUS_BLOCKS = config.get("ShipScanner", "max_deploy_radius_blocks", SS_MAX_DEPLOY_RADIUS_BLOCKS,
				"Max distance from ship scanner to ship core, measured in blocks").getInt();

		// Particle Booster
		PB_MAX_ENERGY_VALUE = config.get("ParticleBooster", "max_energy_value", PB_MAX_ENERGY_VALUE).getInt();

		// Laser Emitter
		LE_MAX_BOOSTERS_NUMBER = config.get("LaserEmitter", "max_boosters_number", LE_MAX_BOOSTERS_NUMBER).getInt();
		LE_MAX_LASER_ENERGY = config.get("LaserEmitter", "max_laser_energy", LE_MAX_LASER_ENERGY).getInt();
		LE_EMIT_DELAY_TICKS = config.get("LaserEmitter", "emit_delay_ticks", LE_EMIT_DELAY_TICKS).getInt();
		LE_EMIT_SCAN_DELAY_TICKS = config.get("LaserEmitter", "emit_scan_delay_ticks", LE_EMIT_SCAN_DELAY_TICKS).getInt();

		// Laser Emitter tweaks
		LE_COLLECT_ENERGY_MULTIPLIER = config.get("LaserEmitterTweaks", "collect_energy_multiplier", LE_COLLECT_ENERGY_MULTIPLIER).getDouble(0.6D);
		LE_BEAM_LENGTH_PER_ENERGY_DIVIDER = config.get("LaserEmitterTweaks", "beam_length_per_energy_divider", LE_BEAM_LENGTH_PER_ENERGY_DIVIDER).getInt();
		LE_ENTITY_HIT_SET_ON_FIRE_TIME = config.get("LaserEmitterTweaks", "entity_hit_set_on_fire_time", LE_ENTITY_HIT_SET_ON_FIRE_TIME).getInt();
		LE_ENTITY_HIT_DAMAGE_PER_ENERGY_DIVIDER = config.get("LaserEmitterTweaks", "entity_hit_damage_per_energy_divider",
				LE_ENTITY_HIT_DAMAGE_PER_ENERGY_DIVIDER).getInt();
		LE_ENTITY_HIT_EXPLOSION_LASER_ENERGY = config.get("LaserEmitterTweaks", "entity_hit_explosion_laser_energy", LE_ENTITY_HIT_EXPLOSION_LASER_ENERGY)
				.getInt();
		LE_BLOCK_HIT_CONSUME_ENERGY = config.get("LaserEmitterTweaks", "block_hit_consume_energy", LE_BLOCK_HIT_CONSUME_ENERGY).getInt();
		LE_BLOCK_HIT_CONSUME_ENERGY_PER_BLOCK_RESISTANCE = config.get("LaserEmitterTweaks", "block_hit_consume_energy_per_block_resistance",
				LE_BLOCK_HIT_CONSUME_ENERGY_PER_BLOCK_RESISTANCE).getInt();
		LE_BLOCK_HIT_CONSUME_ENERGY_PER_DISTANCE = config.get("LaserEmitterTweaks", "block_hit_consume_energy_per_distance",
				LE_BLOCK_HIT_CONSUME_ENERGY_PER_DISTANCE).getInt();

		// Mining Laser
		ML_MAX_BOOSTERS_NUMBER = config.get("MiningLaser", "max_boosters_number", ML_MAX_BOOSTERS_NUMBER).getInt();
		ML_WARMUP_DELAY_TICKS = config.get("MiningLaser", "warmup_delay_ticks", ML_WARMUP_DELAY_TICKS).getInt();
		ML_SCAN_DELAY_TICKS = config.get("MiningLaser", "scan_delay_ticks", ML_SCAN_DELAY_TICKS).getInt();
		ML_MINE_DELAY_TICKS = config.get("MiningLaser", "mine_delay_ticks", ML_MINE_DELAY_TICKS).getInt();
		ML_EU_PER_LAYER_SPACE = config.get("MiningLaser", "eu_per_layer_space", ML_EU_PER_LAYER_SPACE).getInt();
		ML_EU_PER_LAYER_EARTH = config.get("MiningLaser", "eu_per_layer_earth", ML_EU_PER_LAYER_EARTH).getInt();
		ML_EU_PER_BLOCK_SPACE = config.get("MiningLaser", "eu_per_block_space", ML_EU_PER_BLOCK_SPACE).getInt();
		ML_EU_PER_BLOCK_EARTH = config.get("MiningLaser", "eu_per_block_earth", ML_EU_PER_BLOCK_EARTH).getInt();
		ML_EU_MUL_ORESONLY = config.get("MiningLaser", "oresonly_power_mul", ML_EU_MUL_ORESONLY).getDouble(4.0);
		ML_EU_MUL_SILKTOUCH = config.get("MiningLaser", "silktouch_power_mul", ML_EU_MUL_SILKTOUCH).getDouble(2.5);
		ML_DEUTERIUM_MUL_SILKTOUCH = config.get("MiningLaser", "silktouch_deuterium_mul", ML_DEUTERIUM_MUL_SILKTOUCH).getDouble(1.0);
		ML_EU_MUL_FORTUNE = config.get("MiningLaser", "fortune_power_base", ML_EU_MUL_FORTUNE).getDouble(2.5);
		ML_MAX_RADIUS = config.get("MiningLaser", "max_radius", ML_MAX_RADIUS).getInt();

		// Tree Farm
		TF_MAX_SIZE = config.get("TreeFarm", "max_treefarm_size", TF_MAX_SIZE).getInt();

		// Cloaking device core
		CD_MAX_CLOAKING_FIELD_SIDE = config.get("CloakingDevice", "max_cloaking_field_side", CD_MAX_CLOAKING_FIELD_SIDE).getInt();
		CD_ENERGY_PER_BLOCK_TIER1 = config.get("CloakingDevice", "energy_per_block_tier1", CD_ENERGY_PER_BLOCK_TIER1).getInt();
		CD_ENERGY_PER_BLOCK_TIER2 = config.get("CloakingDevice", "energy_per_block_tier2", CD_ENERGY_PER_BLOCK_TIER2).getInt();
		CD_FIELD_REFRESH_INTERVAL_SECONDS = config.get("CloakingDevice", "field_refresh_interval_seconds", CD_FIELD_REFRESH_INTERVAL_SECONDS).getInt();
		CD_COIL_CAPTURE_BLOCKS = config.get("CloakingDevice", "coil_capture_blocks", CD_COIL_CAPTURE_BLOCKS, "Extra blocks covered after the outer coils")
				.getInt();

		// Air generator
		AG_RF_PER_CANISTER = config.get("Air Generator", "energy_per_canister", AG_RF_PER_CANISTER).getInt();

		// Reactor monitor
		RM_MAX_ENERGY = config.get("Reactor Monitor", "max_rm_energy", RM_MAX_ENERGY).getInt();
		RM_EU_PER_HEAT = config.get("Reactor Monitor", "eu_per_heat", RM_EU_PER_HEAT).getDouble(2);

		// Transporter
		TR_MAX_ENERGY = config.get("Transporter", "max_energy", TR_MAX_ENERGY).getInt();
		TR_RELATIVE_COORDS = config.get("Transporter", "relative_coords", TR_RELATIVE_COORDS).getBoolean(true);
		TR_EU_PER_METRE = config.get("Transporter", "eu_per_ent_per_metre", TR_EU_PER_METRE).getDouble(100);
		TR_MAX_BOOST_MUL = config.get("Transporter", "max_boost", TR_MAX_BOOST_MUL).getInt();

		// Power reactor
		PR_MAX_ENERGY = config.get("Reactor", "max_energy", PR_MAX_ENERGY).getInt();
		PR_TICK_TIME = config.get("Reactor", "ticks_per_update", PR_TICK_TIME).getInt();
		PR_MAX_LASERS = config.get("Reactor", "max_lasers", PR_MAX_LASERS).getInt();

		// Power store
		PS_MAX_ENERGY = config.get("PowerStore", "max_energy", PS_MAX_ENERGY).getInt();

		// Laser lift
		LL_MAX_ENERGY = config.get("LaserLift", "max_energy", LL_MAX_ENERGY).getInt();
		LL_LIFT_ENERGY = config.get("LaserLift", "lift_energy", LL_LIFT_ENERGY, "Energy consummed per entity moved").getInt();
		LL_TICK_RATE = config.get("LaserLift", "tick_rate", LL_TICK_RATE).getInt();
	}

	public static void load() {
		commonWorldGenOres = new ArrayList<Block>();
		commonWorldGenOres.add(Blocks.iron_ore);
		commonWorldGenOres.add(Blocks.gold_ore);
		commonWorldGenOres.add(Blocks.coal_ore);
		commonWorldGenOres.add(Blocks.emerald_ore);
		commonWorldGenOres.add(Blocks.lapis_ore);
		commonWorldGenOres.add(Blocks.redstone_ore);

		forceFieldBlocks = new ArrayList<Integer>();

		spaceHelmets = new ArrayList<Item>();
		jetpacks = new ArrayList<Item>();
		minerOres = new ArrayList<Block>();
		minerLogs = new ArrayList<Block>();
		minerLeaves = new ArrayList<Block>();
		scannerIgnoreBlocks = new ArrayList<Block>();
		config.load();

		isForgeMultipartLoaded = Loader.isModLoaded("ForgeMultipart");
		if (isForgeMultipartLoaded) {
			loadForgeMultipart();
		}

		isICLoaded = Loader.isModLoaded("IC2");
		if (isICLoaded)
			loadIC2();

		isCCLoaded = Loader.isModLoaded("ComputerCraft");
		if (isCCLoaded)
			loadCC();

		isAppliedEnergisticsLoaded = Loader.isModLoaded("AppliedEnergistics");
		if (isAppliedEnergisticsLoaded)
			loadAppliedEnergistics();

		isAEExtraLoaded = Loader.isModLoaded("extracells");
		if (isAEExtraLoaded)
			loadAEExtra();

		isAdvSolPanelLoaded = Loader.isModLoaded("AdvancedSolarPanel");
		if (isAdvSolPanelLoaded)
			loadASP();

		isAtomicScienceLoaded = Loader.isModLoaded("ResonantInduction|Atomic");
		if (isAtomicScienceLoaded)
			loadAtomicScience();

		isICBMLoaded = Loader.isModLoaded("ICBM|Explosion");
		if (isICBMLoaded)
			loadICBM();

		isMFFSLoaded = Loader.isModLoaded("MFFS");
		if (isMFFSLoaded)
			loadMFFS();

		isGraviSuiteLoaded = Loader.isModLoaded("GraviSuite");
		if (isGraviSuiteLoaded)
			loadGraviSuite();

		isUndergroundBiomesLoaded = Loader.isModLoaded("UndergroundBiomes");
		if (isUndergroundBiomesLoaded)
			loadUndergroundBiomes();

		isNetherOresLoaded = Loader.isModLoaded("NetherOres");
		if (isNetherOresLoaded)
			loadNetherOres();

		isThermalExpansionLoaded = Loader.isModLoaded("ThermalExpansion");
		if (isThermalExpansionLoaded)
			loadThermalExpansion();

		isAdvancedRepulsionSystemsLoaded = Loader.isModLoaded("AdvancedRepulsionSystems");
		if (isAdvancedRepulsionSystemsLoaded) {
			loadAdvancedRepulsionSystems();
		}

		isMagicalCropsLoaded = Loader.isModLoaded("MagicalCrops");
		//
		minerOres.add(WarpDrive.iridiumBlock);
		minerOres.add(Blocks.coal_ore);
		minerOres.add(Blocks.quartz_ore);
		minerOres.add(Blocks.obsidian);
		minerOres.add(Blocks.web);
		minerOres.add(Blocks.fence);
		minerOres.add(Blocks.torch);
		minerOres.add(Blocks.glowstone);
		minerOres.add(Blocks.redstone_block);

		// Ignore WarpDrive blocks (which potentially will be duplicated by
		// cheaters using ship scan/deploy)
		scannerIgnoreBlocks.add(WarpDrive.warpCore);
		scannerIgnoreBlocks.add(WarpDrive.protocolBlock);
		scannerIgnoreBlocks.add(WarpDrive.iridiumBlock);

		if (isICLoaded) {
			scannerIgnoreBlocks.add(Block.getBlockFromItem(IC2Items.getItem("mfsUnit").getItem()));
			scannerIgnoreBlocks.add(Block.getBlockFromItem(IC2Items.getItem("mfeUnit").getItem()));
			scannerIgnoreBlocks.add(Block.getBlockFromItem(IC2Items.getItem("cesuUnit").getItem()));
			scannerIgnoreBlocks.add(Block.getBlockFromItem(IC2Items.getItem("batBox").getItem()));
		}
		if (isICBMLoaded) {
			// scannerIgnoreBlocks.add(ICBM_Explosive); TODO: NO icbm yet
		}
		if (isCCLoaded) {
			scannerIgnoreBlocks.add(CC_Computer);
			scannerIgnoreBlocks.add(CCT_Turtle);
			scannerIgnoreBlocks.add(CCT_Upgraded);
			scannerIgnoreBlocks.add(CCT_Advanced);
		}
		// Do not deploy ores and valuables
		for (Block t : commonWorldGenOres) {
			scannerIgnoreBlocks.add(t);
		}

		loadWarpDriveConfig();
		config.save();
	}

	public static void postInit() {
		LoadOreDict();
	}

	private static void LoadOreDict() {
		String[] oreNames = OreDictionary.getOreNames();
		for (String oreName : oreNames) {
			String lowerOreName = oreName.toLowerCase();
			if (oreName.substring(0, 3).equals("ore")) {
				ArrayList<ItemStack> item = OreDictionary.getOres(oreName);
				for (ItemStack i : item) {
					minerOres.add(Block.getBlockFromItem(i.getItem()));
					WarpDrive.debugPrint("WD: Added ore ID: " + i);
				}
			}
			if (lowerOreName.contains("log")) {
				ArrayList<ItemStack> item = OreDictionary.getOres(oreName);
				for (ItemStack i : item) {
					minerLogs.add(Block.getBlockFromItem(i.getItem()));
					WarpDrive.debugPrint("WD: Added log ID: " + i);
				}
			}
			if (lowerOreName.contains("leave") || lowerOreName.contains("leaf")) {
				ArrayList<ItemStack> item = OreDictionary.getOres(oreName);
				for (ItemStack i : item) {
					minerLeaves.add(Block.getBlockFromItem(i.getItem()));
					WarpDrive.debugPrint("WD: Added leaf ID: " + i);
				}
			}
		}
	}

	private static void loadForgeMultipart() {
		try {
			Class forgeMultipart_helper = Class.forName("codechicken.multipart.MultipartHelper");
			forgeMultipart_helper_createTileFromNBT = forgeMultipart_helper.getDeclaredMethod("createTileFromNBT", World.class, NBTTagCompound.class);
			forgeMultipart_helper_sendDescPacket = forgeMultipart_helper.getDeclaredMethod("sendDescPacket", World.class, TileEntity.class);
			Class forgeMultipart_tileMultipart = Class.forName("codechicken.multipart.TileMultipart");
			forgeMultipart_tileMultipart_onChunkLoad = forgeMultipart_tileMultipart.getDeclaredMethod("onChunkLoad");
		} catch (Exception e) {
			isForgeMultipartLoaded = false;
			WarpDrive.debugPrint("WarpDriveConfig Error loading ForgeMultipart classes");
			e.printStackTrace();
		}
	}

	private static void loadIC2() {
		ASP = IC2Items.getItem("solarPanel");
		spaceHelmets.add(IC2Items.getItem("hazmatHelmet").getItem());
		spaceHelmets.add(IC2Items.getItem("quantumHelmet").getItem());
		jetpacks.add(IC2Items.getItem("jetpack").getItem());
		jetpacks.add(IC2Items.getItem("electricJetpack").getItem());
		IC2_Air = IC2Items.getItem("airCell");
		IC2_Empty = IC2Items.getItem("cell");
		ItemStack rubberWood = IC2Items.getItem("rubberWood");
		IC2_Resin = IC2Items.getItem("resin");
		if (rubberWood != null) {
			IC2_RubberWood = rubberWood;
		}
		ItemStack ore = IC2Items.getItem("uraniumOre");
		if (ore != null)
			commonWorldGenOres.add(Block.getBlockFromItem(ore.getItem()));
		ore = IC2Items.getItem("copperOre");
		if (ore != null)
			commonWorldGenOres.add(Block.getBlockFromItem(ore.getItem()));
		ore = IC2Items.getItem("tinOre");
		if (ore != null)
			commonWorldGenOres.add(Block.getBlockFromItem(ore.getItem()));
		ore = IC2Items.getItem("leadOre");
		if (ore != null)
			commonWorldGenOres.add(Block.getBlockFromItem(ore.getItem()));

		minerOres.add(Block.getBlockFromItem(IC2Items.getItem("rubberWood").getItem()));
		IC2_fluidCell = IC2Items.getItem("FluidCell").getItem();
	}

	private static void loadCC() {
		try {

			CC_Computer = ComputerCraft.Blocks.computer;
			CC_peripheral = ComputerCraft.Blocks.peripheral;
			CC_Floppy = ComputerCraft.Items.disk;
			CCT_Turtle = ComputerCraft.Blocks.turtle;
			CCT_Upgraded = ComputerCraft.Blocks.turtleExpanded;
			CCT_Advanced = ComputerCraft.Blocks.turtleAdvanced;
		} catch (Exception e) {
			WarpDrive.debugPrint("WarpDriveConfig Error loading ComputerCraft classes");
			e.printStackTrace();
		}
	}

	private static void loadAppliedEnergistics() {
		try {
			AEBlocks = appeng.api.definitions.IDefinitions.blocks();
			AEMaterials = Class.forName("appeng.api.Materials");
			AEItems = Class.forName("appeng.api.Items");
			minerOres.add(((ItemStack) AEBlocks.getField("blkQuartzOre").get(null)));
		} catch (Exception e) {
			WarpDrive.debugPrint("WarpDriveConfig Error loading AppliedEnergistics classes");
			e.printStackTrace();
			isAppliedEnergisticsLoaded = false;
		}
	}

	private static void loadAEExtra() {
		try {
			Class<?> z = Class.forName("extracells.ItemEnum");
			Object z1 = z.getEnumConstants()[6];
			AEExtra_fluidDrive = (Item) z1.getClass().getDeclaredMethod("getItemInstance").invoke(z1);
			z = Class.forName("extracells.BlockEnum");
			z1 = z.getEnumConstants()[9];
			AEExtra_certusQuartzTank = (Block) z1.getClass().getDeclaredMethod("getBlockInstance").invoke(z1);
		} catch (Exception e) {
			WarpDrive.debugPrint("WarpDriveConfig Error loading AEExtra classes");
			e.printStackTrace();
			isAEExtraLoaded = false;
		}
	}

	private static void loadASP() {
		try {
			spaceHelmets.add(ASPItemAPI.get("advancedSolarHelmet").getItem());
			spaceHelmets.add(ASPItemAPI.get("hybridSolarHelmet").getItem());
			spaceHelmets.add(ASPItemAPI.get("ultimateSolarHelmet").getItem());
		} catch (Exception e) {
			WarpDrive.debugPrint("WarpDriveConfig Error loading ASP classes");
			e.printStackTrace();
			isAdvSolPanelLoaded = false;
		}
	}

	private static void loadAtomicScience() {
		try {
			Class<?> z = Class.forName("resonantinduction.atomic.Atomic");
			commonWorldGenOres.add(new int[] { ((Block) z.getField("blockUraniumOre").get(null)), 0 });
			AS_Turbine = ((Block) z.getField("blockElectricTurbine").get(null));
			AS_deuteriumCell = ((Item) z.getField("itemDeuteriumCell").get(null));
		} catch (Exception e) {
			WarpDrive.debugPrint("WarpDriveConfig Error loading AS classes");
			isAtomicScienceLoaded = false;
		}
	}

	private static void loadICBM() {
		try {
			Class<?> z = Class.forName("icbm.core.ICBMCore");
			commonWorldGenOres.add(new int[] { ((Block) z.getField("blockSulfurOre").get(null)), 0 });
			z = Class.forName("icbm.explosion.ICBMExplosion");
			ICBM_Machine = ((Block) z.getField("blockMachine").get(null));
			ICBM_Missile = ((Item) z.getField("itemMissile").get(null));
			ICBM_Explosive = ((Block) z.getField("blockExplosive").get(null));
		} catch (Exception e) {
			WarpDrive.debugPrint("WarpDriveConfig Error loading ICBM classes");
			e.printStackTrace();
			isICBMLoaded = false;
		}
	}

	private static void loadMFFS() {
		try {
			Class<?> z = Class.forName("mffs.ModularForceFieldSystem");
			int blockId = ((Block) z.getField("blockForceField").get(null));
			forceFieldBlocks.add(blockId);
		} catch (Exception e) {
			WarpDrive.debugPrint("WarpDriveConfig Error loading MFFS classes");
			e.printStackTrace();
			isICBMLoaded = false;
		}
	}

	private static void loadGraviSuite() {
		try {
			Class<?> z = Class.forName("gravisuite.GraviSuite");
			if (z.getField("ultimateSolarHelmet").get(null) != null)
				spaceHelmets.add(((Item) z.getField("ultimateSolarHelmet").get(null)));
			jetpacks.add(z.getField("advJetpackID").getInt(null) + 256);
			jetpacks.add(z.getField("graviChestPlateID").getInt(null) + 256);
			GS_ultimateLappack = z.getField("ultimateLappackID").getInt(null) + 256;
		} catch (Exception e) {
			WarpDrive.debugPrint("WarpDriveConfig Error loading GS classes");
			e.printStackTrace();
			isGraviSuiteLoaded = false;
		}
	}

	private static void loadUndergroundBiomes() {
		try {
			Class<?> z = Class.forName("exterminatorJeff.undergroundBiomes.common.UndergroundBiomes");
			UB_igneousStone = ((Block) z.getField("igneousStone").get(null));
			UB_igneousCobblestone = ((Block) z.getField("igneousCobblestone").get(null));
			UB_metamorphicStone = ((Block) z.getField("metamorphicStone").get(null));
			UB_metamorphicCobblestone = ((Block) z.getField("metamorphicCobblestone").get(null));
			UB_sedimentaryStone = ((Block) z.getField("sedimentaryStone").get(null));
			WarpDrive.debugPrint("WarpDriveConfig found UndergroundBiomes blocks " + UB_igneousStone + ", " + UB_igneousCobblestone + ", "
					+ UB_metamorphicStone + ", " + UB_metamorphicCobblestone + ", " + UB_sedimentaryStone);
		} catch (Exception e) {
			WarpDrive.debugPrint("WarpDriveConfig Error loading UndergroundBiomes classes");
			e.printStackTrace();
			isUndergroundBiomesLoaded = false;
		}
	}

	private static void loadNetherOres() {
		try {
			NetherOres_count = 21; // FIXME: extract it properly
			/*
			 * Class<?> z = Class.forName("powercrystals.netherores.ores.Ores");
			 * NO_netherOresCount = z.getField("values").get(null).length;
			 * WarpDrive.debugPrint("WarpDriveConfig found NetherOres count " +
			 * NO_netherOresCount);
			 *
			 * z = Class.forName("powercrystals.netherores.NetherOresCore"); for
			 * (int i = 0; i < (NO_netherOresCount + 15) / 16; i++) {
			 * NO_netherOresBlock[i] =
			 * ((Block[])z.getDeclaredField("blockNetherOres").get(null))[i];
			 * WarpDrive.debugPrint("WarpDriveConfig found NetherOres blockId "
			 * + NO_netherOresBlock[i]); }
			 */
			NetherOres_block = new int[(NetherOres_count + 15) / 16];
			NetherOres_block[0] = 1440;
			NetherOres_block[1] = 1442;
			WarpDrive.debugPrint("WarpDriveConfig found " + NetherOres_count + " NetherOres");
		} catch (Exception e) {
			WarpDrive.print("WarpDriveConfig Error loading NetherOres classes");
			e.printStackTrace();
			isNetherOresLoaded = false;
		}
	}

	private static void loadThermalExpansion() {
		try {
			// TEEnergyCell =
			// Class.forName("thermalexpansion.block.energycell.BlockEnergyCell");
			// TEFluids = Class.forName("thermalexpansion.fluid.TEFluids");
		} catch (Exception e) {
			WarpDrive.print("WarpDriveConfig Error loading ThermalExpansion classes");
			e.printStackTrace();
			isThermalExpansionLoaded = false;
		}
	}

	private static void loadAdvancedRepulsionSystems() {
		try {
			Class<?> z = Class.forName("mods.immibis.ars.ARSMod");
			int fieldBlockId = ((Block) z.getField("MFFSFieldblock").get(null));
			forceFieldBlocks.add(fieldBlockId);
		} catch (Exception e) {
			WarpDrive.print("WarpDriveConfig Error loading AdvancedRepulsionSystems classes");
			e.printStackTrace();
			isAdvancedRepulsionSystemsLoaded = false;
		}
	}

	public static int[] getDefaultSurfaceBlock(Random random, boolean corrupted, boolean isMoon) {
		if (isMoon) {
			if (isGregLoaded && (random.nextInt(100) == 1)) {
				if (random.nextBoolean()) {
					return new int[] { GT_Granite, (corrupted && random.nextBoolean()) ? 1 : 0 };
				} else {
					return new int[] { GT_Granite, (corrupted && random.nextBoolean()) ? 9 : 8 };
				}
			} else if (random.nextInt(5) == 1) {
				return new int[] { Block.netherrack, 0 };
			} else if (random.nextInt(15) == 1) {
				return new int[] { Block.whiteStone, 0 };
			}
		} else {
			if (isGregLoaded && (random.nextInt(25) == 1)) {
				if (random.nextBoolean()) {
					return new int[] { GT_Granite, (corrupted && random.nextBoolean()) ? 1 : 0 };
				} else {
					return new int[] { GT_Granite, (corrupted && random.nextBoolean()) ? 9 : 8 };
				}
			} else if (random.nextInt(6) == 1) {
				return new int[] { Block.netherrack, 0 };
			} else if (random.nextInt(50) == 1) {
				return new int[] { Block.whiteStone, 0 };
			}
		}
		if (corrupted && random.nextBoolean()) {
			if (isUndergroundBiomesLoaded) {
				int rnd = random.nextInt(8 + 8 + 2);
				if (rnd < 8) {
					return new int[] { UB_igneousCobblestone, rnd };
				} else if (rnd < (8 + 8)) {
					return new int[] { UB_metamorphicCobblestone, rnd - 8 };
				}
			}
			return new int[] { Block.cobblestone, 0 };
		}
		if (isUndergroundBiomesLoaded) {
			int rnd = random.nextInt(8 + 8 + 8 + 3);
			if (rnd < 8) {
				return new int[] { UB_igneousStone, rnd };
			} else if (rnd < (8 + 8)) {
				return new int[] { UB_metamorphicStone, rnd - 8 };
			} else if (rnd < (8 + 8 + 8)) {
				if (rnd == 8 + 8 + 8) {
					return new int[] { 205, 0 }; // emasher Limestone
				} else {
					return new int[] { UB_sedimentaryStone, rnd - 8 - 8 };
				}
			}
		}
		return new int[] { Block.stone, 0 };
	}

	public static int[] getRandomSurfaceBlock(Random random, int blockID, int blockMeta, boolean bedrock) {
		if (bedrock && (random.nextInt(1000) == 1)) {
			return new int[] { Block.bedrock, 0 };
		} else if (blockID == GT_Granite) {
			if ((blockMeta == 0) || (blockMeta == 1)) {
				int[] t;
				t = getRandomOverworldBlock(random, blockID, blockMeta);
				if (t[0] == blockID)
					t = getRandomOverworldBlock(random, blockID, blockMeta);
				if (t[0] == blockID)
					t = getRandomEndBlock(random, blockID, blockMeta);
				return t;
			} else if ((blockMeta == 8) || (blockMeta == 9)) {
				int[] t;
				t = getRandomOverworldBlock(random, blockID, blockMeta);
				if (t[0] == blockID)
					t = getRandomEndBlock(random, blockID, blockMeta);
				if (t[0] == blockID)
					t = getRandomOverworldBlock(random, blockID, blockMeta);
				return t;
			}
		} else if (blockID == Block.whiteStone) {
			return getRandomEndBlock(random, blockID, blockMeta);
		} else if (blockID == Block.netherrack) {
			return getRandomNetherBlock(random, blockID, blockMeta);
		}
		return getRandomOverworldBlock(random, blockID, blockMeta);
	}

	public static int[] getRandomOverworldBlock(Random random, int blockID, int blockMeta) {
		if (random.nextInt(25) == 5) {
			return commonWorldGenOres.get(random.nextInt(commonWorldGenOres.size()));
		} else if (isMetallurgyLoaded && (random.nextInt(25) == 1)) {
			return Metallurgy_overworldOresBlock[random.nextInt(Metallurgy_overworldOresBlock.length)];
		} else if (isAppliedEnergisticsLoaded && random.nextInt(750) == 1) {
			return new int[] { getAEBlock("blkQuartzOre"), getAEBlock("blkQuartzOre").getItemDamage() };
		} else if (random.nextInt(250) == 1) {
			return new int[] { Block.oreDiamond, 0 };
		} else if (!isNetherOresLoaded && (random.nextInt(10000) == 42)) {
			return new int[] { iridiumBlockID, 0 };
		} else if (!isMagicalCropsLoaded && (random.nextInt(100) == 56)) {
			return new int[] { 3108, 0 }; // Essence ore
		} else if (isGregLoaded) {
			if (random.nextInt(50) == 1)
				return new int[] { GT_Ores, 5 }; // Bauxite S /*
			// Stone/Iron/Diamod pick |
			// +S = Silktouch
			// recommended */
			else if (random.nextInt(50) == 1)
				return new int[] { GT_Ores, 1 }; // Galena S
			else if (random.nextInt(100) == 1)
				return new int[] { GT_Ores, 8 }; // Sphalerite S+S
			else if (random.nextInt(250) == 1)
				return new int[] { GT_Ores, 13 }; // Tetrahedrite I
			else if (random.nextInt(250) == 1)
				return new int[] { GT_Ores, 14 }; // Cassiterite I
			else if (random.nextInt(250) == 1)
				return new int[] { GT_Ores, 15 }; // Nickel I
			else if (random.nextInt(500) == 1)
				return new int[] { GT_Ores, 3 }; // Ruby I+S
			else if (random.nextInt(500) == 1)
				return new int[] { GT_Ores, 4 }; // Sapphire I+S
			else if (random.nextInt(2000) == 1)
				return new int[] { GT_Ores, 2 }; // Iridium D+S
		}
		return new int[] { blockID, blockMeta };
	}

	public static int[] getRandomNetherBlock(Random random, int blockID, int blockMeta) {
		if (isICLoaded && (!isNetherOresLoaded) && (random.nextInt(10000) == 42)) {
			return new int[] { iridiumBlockID, 0 };
		} else if (isNetherOresLoaded && (random.nextInt(25) == 1)) {
			int rnd = random.nextInt(NetherOres_count);
			return new int[] { NetherOres_block[rnd / 16], rnd % 16 };
		} else if (isMetallurgyLoaded && (random.nextInt(25) == 1)) {
			return Metallurgy_netherOresBlock[random.nextInt(Metallurgy_netherOresBlock.length)];
		} else if (random.nextInt(25) == 1) {
			return new int[] { Block.oreNetherQuartz, 0 };
		} else if (!isMagicalCropsLoaded && (random.nextInt(100) == 56)) {
			return new int[] { 3109, 0 }; // Nether essence ore
		} else if (isGregLoaded) {
			if (random.nextInt(100) == 1)
				return new int[] { GT_Ores, 6 }; // Pyrite S+S
			else if (random.nextInt(100) == 1)
				return new int[] { GT_Ores, 8 }; // Sphalerite S+S
			else if (random.nextInt(500) == 1)
				return new int[] { GT_Ores, 7 }; // Cinnabar I+S
		} else if ((!isNetherOresLoaded) && (random.nextInt(100) == 13))
			return commonWorldGenOres.get(random.nextInt(commonWorldGenOres.size()));
		return new int[] { blockID, blockMeta };
	}

	public static int[] getRandomEndBlock(Random random, int blockID, int blockMeta) {
		if (isICLoaded && random.nextInt(10000) == 42) {
			return new int[] { iridiumBlockID, 0 };
		} else if (isGregLoaded) {
			if (random.nextInt(250) == 1)
				return new int[] { GT_Ores, 9 }; // Tungstate I
			else if (random.nextInt(500) == 1)
				return new int[] { GT_Ores, 12 }; // Sodalite I+S
			else if (random.nextInt(500) == 1)
				return new int[] { GT_Ores, 10 }; // Cooperite=Sheldonite D
			else if (random.nextInt(1000) == 1)
				return new int[] { GT_Ores, 11 }; // Olivine D+S
		} else if (isMetallurgyLoaded && (random.nextInt(25) == 1)) {
			return Metallurgy_endOresBlock[random.nextInt(Metallurgy_endOresBlock.length)];
		} else if (random.nextInt(200) == 13) {
			return commonWorldGenOres.get(random.nextInt(commonWorldGenOres.size()));
		}
		return new int[] { blockID, blockMeta };
	}
}
