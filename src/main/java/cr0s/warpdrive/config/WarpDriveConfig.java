package cr0s.warpdrive.config;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.compat.CompatActuallyAdditions;
import cr0s.warpdrive.compat.CompatAdvancedRepulsionSystems;
import cr0s.warpdrive.compat.CompatAppliedEnergistics2;
import cr0s.warpdrive.compat.CompatArsMagica2;
import cr0s.warpdrive.compat.CompatBiblioCraft;
import cr0s.warpdrive.compat.CompatBlockcraftery;
import cr0s.warpdrive.compat.CompatBotania;
import cr0s.warpdrive.compat.CompatBuildCraft;
import cr0s.warpdrive.compat.CompatCarpentersBlocks;
import cr0s.warpdrive.compat.CompatComputerCraft;
import cr0s.warpdrive.compat.CompatCustomNPCs;
import cr0s.warpdrive.compat.CompatDecocraft;
import cr0s.warpdrive.compat.CompatDeepResonance;
import cr0s.warpdrive.compat.CompatDraconicEvolution;
import cr0s.warpdrive.compat.CompatEmbers;
import cr0s.warpdrive.compat.CompatEnderIO;
import cr0s.warpdrive.compat.CompatEnvironmentalTech;
import cr0s.warpdrive.compat.CompatEvilCraft;
import cr0s.warpdrive.compat.CompatExtraUtilities2;
import cr0s.warpdrive.compat.CompatForgeMultipart;
import cr0s.warpdrive.compat.CompatGalacticraft;
import cr0s.warpdrive.compat.CompatGregTech;
import cr0s.warpdrive.compat.CompatImmersiveEngineering;
import cr0s.warpdrive.compat.CompatIndustrialCraft2;
import cr0s.warpdrive.compat.CompatIndustrialForegoing;
import cr0s.warpdrive.compat.CompatIronChest;
import cr0s.warpdrive.compat.CompatMekanism;
import cr0s.warpdrive.compat.CompatMetalChests;
import cr0s.warpdrive.compat.CompatMysticalAgriculture;
import cr0s.warpdrive.compat.CompatNatura;
import cr0s.warpdrive.compat.CompatOpenComputers;
import cr0s.warpdrive.compat.CompatParziStarWars;
import cr0s.warpdrive.compat.CompatPneumaticCraft;
import cr0s.warpdrive.compat.CompatRealFilingCabinet;
import cr0s.warpdrive.compat.CompatRedstonePaste;
import cr0s.warpdrive.compat.CompatRefinedStorage;
import cr0s.warpdrive.compat.CompatRoots;
import cr0s.warpdrive.compat.CompatRustic;
import cr0s.warpdrive.compat.CompatSGCraft;
import cr0s.warpdrive.compat.CompatJSG;
import cr0s.warpdrive.compat.CompatStorageDrawers;
import cr0s.warpdrive.compat.CompatTConstruct;
import cr0s.warpdrive.compat.CompatTechguns;
import cr0s.warpdrive.compat.CompatThaumcraft;
import cr0s.warpdrive.compat.CompatThermalDynamics;
import cr0s.warpdrive.compat.CompatThermalExpansion;
import cr0s.warpdrive.compat.CompatUndergroundBiomes;
import cr0s.warpdrive.compat.CompatVariedCommodities;
import cr0s.warpdrive.compat.CompatWarpDrive;
import cr0s.warpdrive.compat.CompatWoot;
import cr0s.warpdrive.config.structures.StructureManager;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.EnergyWrapper;
import cr0s.warpdrive.data.EnumShipMovementType;
import cr0s.warpdrive.data.EnumDisplayAlignment;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.data.EnumTooltipCondition;
import cr0s.warpdrive.network.PacketHandler;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.oredict.OreDictionary;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class WarpDriveConfig {
	
	private static final boolean unused = false; // TODO
	
	private static String          stringConfigDirectory;
	private static File            fileConfigDirectory;
	private static DocumentBuilder xmlDocumentBuilder;
	private static final String[]  defaultXML_fillerSets = {
			"fillerSets-default.xml",
			"fillerSets-GTCEu.xml",
			"fillerSets-netherores.xml",
			"fillerSets-undergroundbiomes.xml",
			"fillerSets-undergroundBiomes_GTCEu.xml",
	};
	private static final String[]  defaultXML_lootSets = {
			"lootSets-default.xml",
	};
	private static final String[]  defaultXML_schematicSets = {
			"schematicSets-default.xml",
	};
	private static final String[]  defaultXML_structures = {
			"structures-default.xml",
			"structures-netherores.xml",
			"structures-ship.xml",
	};
	private static final String[]  defaultXML_celestialObjects = {
			"celestialObjects-default.xml",
			"celestialObjects-Galacticraft+ExtraPlanets.xml",
	};
	private static final String[]  defaultSchematics = {
			"default-legacy_1.schematic",
			"default-legacy_2.schematic",
	};
	
	public static GenericSetManager<Filler> FillerManager = new GenericSetManager<>("filler", "filler", "fillerSet", Filler.DEFAULT);
	public static GenericSetManager<Loot> LootManager = new GenericSetManager<>("loot", "loot", "lootSet", Loot.DEFAULT);
	
	/*
	 * The variables which store whether or not individual mods are loaded
	 */
	public static boolean              isAdvancedRepulsionSystemLoaded = false;
	public static boolean              isRedstoneFluxLoaded = false;
	public static boolean              isComputerCraftLoaded = false;
	public static boolean              isCCTweakedLoaded = false;
	public static boolean              isEnderIOLoaded = false;
	public static boolean              isForgeMultipartLoaded = false;
	public static boolean              isGregtechLoaded = false;
	public static boolean              isICBMClassicLoaded = false;
	public static boolean              isIndustrialCraft2Loaded = false;
	public static boolean              isMatterOverdriveLoaded = false;
	public static boolean              isNotEnoughItemsLoaded = false;
	public static boolean              isOpenComputersLoaded = false;
	public static boolean              isThermalExpansionLoaded = false;
	public static boolean              isThermalFoundationLoaded = false;
	
	public static ItemStack            IC2_compressedAir;
	public static ItemStack            IC2_emptyCell;
	public static Block                IC2_rubberWood;
	public static ItemStack            IC2_Resin;
	
	// Mod configuration (see loadConfig() for comments/definitions)
	// General
	public static int                  G_SPACE_BIOME_ID = 95;
	public static int                  G_SPACE_PROVIDER_ID = 14;
	public static int                  G_HYPERSPACE_PROVIDER_ID = 15;
	public static int                  G_ENTITY_SPHERE_GENERATOR_ID = 241;
	public static int                  G_ENTITY_STAR_CORE_ID = 242;
	public static int                  G_ENTITY_CAMERA_ID = 243;
	public static int                  G_ENTITY_PARTICLE_BUNCH_ID = 244;
	public static int                  G_ENTITY_LASER_EXPLODER_ID = 245;
	public static int                  G_ENTITY_NPC_ID = 246;
	public static int                  G_ENTITY_OFFLINE_AVATAR_ID = 247;
	public static int                  G_ENTITY_SEAT_ID = 248;
	
	public static final int            LUA_SCRIPTS_NONE = 0;
	public static final int            LUA_SCRIPTS_TEMPLATES = 1;
	public static final int            LUA_SCRIPTS_ALL = 2;
	public static int                  G_LUA_SCRIPTS = LUA_SCRIPTS_ALL;
	public static String               G_SCHEMATICS_LOCATION = "warpDrive_schematics";
	
	private static int                 G_ASSEMBLY_SCAN_INTERVAL_SECONDS = 10;
	public static int                  G_ASSEMBLY_SCAN_INTERVAL_TICKS = 20 * WarpDriveConfig.G_ASSEMBLY_SCAN_INTERVAL_SECONDS;
	public static int                  G_PARAMETERS_UPDATE_INTERVAL_TICKS = 20;
	private static int                 G_REGISTRY_UPDATE_INTERVAL_SECONDS = 10;
	public static int                  G_REGISTRY_UPDATE_INTERVAL_TICKS = 20 * WarpDriveConfig.G_REGISTRY_UPDATE_INTERVAL_SECONDS;
	public static boolean              G_ENFORCE_VALID_CELESTIAL_OBJECTS = true;
	public static int                  G_BLOCKS_PER_TICK = 3500;
	public static boolean              G_ENABLE_FAST_SET_BLOCKSTATE = false;
	public static boolean              G_ENABLE_PROTECTION_CHECKS = true;
	public static boolean              G_ENABLE_EXPERIMENTAL_REFRESH = false;
	public static boolean              G_ENABLE_EXPERIMENTAL_UNLOAD = true;
	public static int                  G_MINIMUM_DIMENSION_UNLOAD_QUEUE_DELAY = 100;
	public static boolean              G_ENABLE_FORGE_CHUNK_MANAGER = true;
	
	public static float                G_BLAST_RESISTANCE_CAP = 60.0F;
	
	// Client
	public static boolean              CLIENT_BREATHING_OVERLAY_FORCED = true;
	public static float                CLIENT_LOCATION_SCALE = 1.0F;
	public static String               CLIENT_LOCATION_NAME_PREFIX = "§l";
	public static int                  CLIENT_LOCATION_BACKGROUND_COLOR = Commons.colorARGBtoInt(64, 48, 48, 48);
	public static int                  CLIENT_LOCATION_TEXT_COLOR = Commons.colorARGBtoInt(230, 180, 180, 240);
	public static boolean              CLIENT_LOCATION_HAS_SHADOW = true;
	public static EnumDisplayAlignment CLIENT_LOCATION_SCREEN_ALIGNMENT = EnumDisplayAlignment.MIDDLE_RIGHT;
	public static int                  CLIENT_LOCATION_SCREEN_OFFSET_X = 0;
	public static int                  CLIENT_LOCATION_SCREEN_OFFSET_Y = -20;
	public static EnumDisplayAlignment CLIENT_LOCATION_TEXT_ALIGNMENT = EnumDisplayAlignment.TOP_RIGHT;
	public static float                CLIENT_LOCATION_WIDTH_RATIO = 0.0F;
	public static int                  CLIENT_LOCATION_WIDTH_MIN = 90;
	
	// Tooltip
	public static EnumTooltipCondition TOOLTIP_ENABLE_DEDUPLICATION = EnumTooltipCondition.ALWAYS;
	public static String[]             TOOLTIP_CLEANUP_LIST = new String[] {
			"fuel details",
			"burn time",
			"durability"
	};
	public static EnumTooltipCondition TOOLTIP_ADD_REGISTRY_NAME = EnumTooltipCondition.ADVANCED_TOOLTIPS;
	public static EnumTooltipCondition TOOLTIP_ADD_ORE_DICTIONARY_NAME = EnumTooltipCondition.ALWAYS;
	public static EnumTooltipCondition TOOLTIP_ADD_ARMOR_POINTS = EnumTooltipCondition.NEVER;
	public static EnumTooltipCondition TOOLTIP_ADD_BLOCK_MATERIAL = EnumTooltipCondition.ADVANCED_TOOLTIPS;
	public static EnumTooltipCondition TOOLTIP_ADD_BURN_TIME = EnumTooltipCondition.ADVANCED_TOOLTIPS;
	public static EnumTooltipCondition TOOLTIP_ADD_DURABILITY = EnumTooltipCondition.ALWAYS;
	public static EnumTooltipCondition TOOLTIP_ADD_ENCHANTABILITY = EnumTooltipCondition.ON_SNEAK;
	public static EnumTooltipCondition TOOLTIP_ADD_ENTITY_ID = EnumTooltipCondition.ADVANCED_TOOLTIPS;
	public static EnumTooltipCondition TOOLTIP_ADD_FLAMMABILITY = EnumTooltipCondition.ADVANCED_TOOLTIPS;
	public static EnumTooltipCondition TOOLTIP_ADD_FLUID = EnumTooltipCondition.ALWAYS;
	public static EnumTooltipCondition TOOLTIP_ADD_HARDNESS = EnumTooltipCondition.ADVANCED_TOOLTIPS;
	public static EnumTooltipCondition TOOLTIP_ADD_HARVESTING = EnumTooltipCondition.ALWAYS;
	public static EnumTooltipCondition TOOLTIP_ADD_OPACITY = EnumTooltipCondition.ADVANCED_TOOLTIPS;
	public static EnumTooltipCondition TOOLTIP_ADD_REPAIR_WITH = EnumTooltipCondition.ON_SNEAK;
	
	// Logging
	public static long LOGGING_THROTTLE_MS = 5000L;
	public static boolean LOGGING_JUMP = true;
	public static boolean LOGGING_JUMPBLOCKS = false;
	public static boolean LOGGING_ENERGY = false;
	public static boolean LOGGING_EFFECTS = false;
	public static boolean LOGGING_CLOAKING = false;
	public static boolean LOGGING_VIDEO_CHANNEL = false;
	public static boolean LOGGING_TARGETING = false;
	public static boolean LOGGING_WEAPON = false;
	public static boolean LOGGING_CAMERA = false;
	public static boolean LOGGING_BUILDING = false;
	public static boolean LOGGING_COLLECTION = false;
	public static boolean LOGGING_TRANSPORTER = false;
	public static boolean LOGGING_LUA = false;
	public static boolean LOGGING_RADAR = false;
	public static boolean LOGGING_BREATHING = false;
	public static boolean LOGGING_WORLD_GENERATION = false;
	public static boolean LOGGING_PROFILING_CPU_USAGE = true;
	public static boolean LOGGING_PROFILING_MEMORY_ALLOCATION = false;
	public static boolean LOGGING_PROFILING_THREAD_SAFETY = false;
	public static boolean LOGGING_DICTIONARY = false;
	public static boolean LOGGING_GLOBAL_REGION_REGISTRY = false;
	public static boolean LOGGING_BREAK_PLACE = false;
	public static boolean LOGGING_FORCE_FIELD = false;
	public static boolean LOGGING_FORCE_FIELD_REGISTRY = false;
	public static boolean LOGGING_ACCELERATOR = false;
	public static boolean LOGGING_XML_PREPROCESSOR = false;
	public static boolean LOGGING_RENDERING = false;
	public static boolean LOGGING_CHUNK_HANDLER = false;
	public static boolean LOGGING_CHUNK_RELOADING = false;
	public static boolean LOGGING_CHUNK_LOADING = true;
	public static boolean LOGGING_ENTITY_FX = false;
	public static boolean LOGGING_CLIENT_SYNCHRONIZATION = false;
	public static boolean LOGGING_GRAVITY = false;
	public static boolean LOGGING_OFFLINE_AVATAR = true;
	
	// Energy
	public static String           ENERGY_DISPLAY_UNITS = "RF";
	public static boolean          ENERGY_ENABLE_IC2_EU = true;
	public static boolean          ENERGY_ENABLE_FE = true;
	public static boolean          ENERGY_ENABLE_GTCE_EU = true;
	public static boolean          ENERGY_ENABLE_RF = true;
	public static float            ENERGY_OVERVOLTAGE_SHOCK_FACTOR = 1.0F;
	public static float            ENERGY_OVERVOLTAGE_EXPLOSION_FACTOR = 1.0F;
	public static int              ENERGY_SCAN_INTERVAL_TICKS = 20;
	
	// Space generator
	public static int              SPACE_GENERATOR_Y_MIN_CENTER = 55;
	public static int              SPACE_GENERATOR_Y_MAX_CENTER = 128;
	public static int              SPACE_GENERATOR_Y_MIN_BORDER = 5;
	public static int              SPACE_GENERATOR_Y_MAX_BORDER = 200;
	
	// Ship movement costs
	public static ShipMovementCosts.Factors[] SHIP_MOVEMENT_COSTS_FACTORS = null;
	
	// Ship
	public static int[]            SHIP_MAX_ENERGY_STORED_BY_TIER = { 0, 500000, 10000000, 100000000 };
	public static int[]            SHIP_MASS_MAX_BY_TIER = { 2000000, 3456, 13824, 110592 };
	public static int[]            SHIP_MASS_MIN_BY_TIER = {       0,   64,  1728,   6912 };
	public static int              SHIP_MASS_MAX_ON_PLANET_SURFACE = 3000;
	public static int              SHIP_MASS_MIN_FOR_HYPERSPACE = 4000;
	public static int[]            SHIP_SIZE_MAX_PER_SIDE_BY_TIER = { 127, 24, 48, 96 };
	public static int              SHIP_COLLISION_TOLERANCE_BLOCKS = 3;
	public static int              SHIP_WARMUP_RANDOM_TICKS = 60;
	public static int              SHIP_VOLUME_SCAN_BLOCKS_PER_TICK = 1000;
	public static int              SHIP_VOLUME_SCAN_AGE_TOLERANCE_SECONDS = 120;
	public static String[]         SHIP_MASS_UNLIMITED_PLAYER_NAMES = { "notch", "someone" };
	
	// Jump gate
	public static int[]            JUMP_GATE_SIZE_MAX_PER_SIDE_BY_TIER = { 127, 32, 64, 127 };
	
	// Biometric scanner
	public static int              BIOMETRIC_SCANNER_DURATION_TICKS = 100;
	public static int              BIOMETRIC_SCANNER_RANGE_BLOCKS = 3;
	
	// Camera
	public static int              CAMERA_IMAGE_RECOGNITION_INTERVAL_TICKS = 20;
	public static int              CAMERA_RANGE_BASE_BLOCKS = 0;
	public static int              CAMERA_RANGE_UPGRADE_BLOCKS = 8;
	public static int              CAMERA_RANGE_UPGRADE_MAX_QUANTITY = 8;
	
	// Offline avatar
	public static boolean          OFFLINE_AVATAR_ENABLE = true;
	public static boolean          OFFLINE_AVATAR_CREATE_ONLY_ABOARD_SHIPS = true;
	public static boolean          OFFLINE_AVATAR_FORGET_ON_DEATH = false;
	public static float            OFFLINE_AVATAR_MODEL_SCALE = 0.5F;
	public static boolean          OFFLINE_AVATAR_ALWAYS_RENDER_NAME_TAG = false;
	public static float            OFFLINE_AVATAR_MIN_RANGE_FOR_REMOVAL = 1.0F;
	public static float            OFFLINE_AVATAR_MAX_RANGE_FOR_REMOVAL = 5.0F;
	public static int              OFFLINE_AVATAR_DELAY_FOR_REMOVAL_SECONDS = 1;
	public static int              OFFLINE_AVATAR_DELAY_FOR_REMOVAL_TICKS = 20 * OFFLINE_AVATAR_DELAY_FOR_REMOVAL_SECONDS;
	
	// Radar
	public static int              RADAR_MAX_ENERGY_STORED = 100000000; // 100kk eU
	public static int              RADAR_SCAN_MIN_ENERGY_COST = 10000;
	public static double[]         RADAR_SCAN_ENERGY_COST_FACTORS = { 0.0, 0.0, 0.0, 0.0001 };
	public static int              RADAR_SCAN_MIN_DELAY_SECONDS = 1;
	public static double[]         RADAR_SCAN_DELAY_FACTORS_SECONDS = { 1.0, 0.001, 0.0, 0.0 };
	public static int              RADAR_MAX_ISOLATION_RANGE = 2;
	public static int              RADAR_MIN_ISOLATION_BLOCKS = 2;
	public static int              RADAR_MAX_ISOLATION_BLOCKS = 16;
	public static double           RADAR_MIN_ISOLATION_EFFECT = 0.12;
	public static double           RADAR_MAX_ISOLATION_EFFECT = 1.00;
	
	// Siren
	public static float[]          SIREN_RANGE_BLOCKS_BY_TIER = { 0.0F, 32.0F, 64.0F, 128.0F };
	
	// Speaker
	public static float[]          SPEAKER_RANGE_BLOCKS_BY_TIER = { 0.0F, 16.0F, 32.0F, 64.0F };
	public static float            SPEAKER_QUEUE_MAX_MESSAGES = 12;
	public static float            SPEAKER_RATE_MAX_MESSAGES = 3;
	public static int              SPEAKER_RATE_PERIOD_TICKS = 60;
	
	// Ship Scanner
	public static int              SS_MAX_DEPLOY_RADIUS_BLOCKS = 100;
	public static int              SS_SEARCH_INTERVAL_TICKS = 20;
	public static int              SS_SCAN_BLOCKS_PER_SECOND = 10;
	public static int              SS_DEPLOY_BLOCKS_PER_INTERVAL = 10;
	public static int              SS_DEPLOY_INTERVAL_TICKS = 4;
	
	// Virtual Assistant
	public static int[]            VIRTUAL_ASSISTANT_ENERGY_PER_TICK_BY_TIER = { 0, 10, 40, 160 };
	public static boolean          VIRTUAL_ASSISTANT_HIDE_COMMANDS_IN_CHAT = false;
	public static int[]            VIRTUAL_ASSISTANT_MAX_ENERGY_STORED_BY_TIER = { 1000000, 10000, 30000, 100000 };
	public static float[]          VIRTUAL_ASSISTANT_RANGE_BLOCKS_BY_TIER = { 0.0F, 32.0F, 64.0F, 128.0F };
	
	// Laser medium
	public static int[]            LASER_MEDIUM_MAX_ENERGY_STORED_BY_TIER = { 1000000, 10000, 30000, 100000 };
	public static double[]         LASER_MEDIUM_FACTOR_BY_TIER = { 1.25D, 0.5D, 1.0D, 1.5D };
	
	// Laser cannon
	// 1 main laser + 4 boosting lasers = 10 * 100k + 0.6 * 40 * 100k = 3.4M
	public static int              LASER_CANNON_MAX_MEDIUMS_COUNT = 10;
	public static int              LASER_CANNON_MAX_LASER_ENERGY = 3400000;
	public static int              LASER_CANNON_EMIT_FIRE_DELAY_TICKS = 5;
	public static int              LASER_CANNON_EMIT_SCAN_DELAY_TICKS = 1;
	
	public static double           LASER_CANNON_BOOSTER_BEAM_ENERGY_EFFICIENCY = 0.60D;
	public static double           LASER_CANNON_ENERGY_ATTENUATION_PER_AIR_BLOCK  = 0.000200D;
	public static double           LASER_CANNON_ENERGY_ATTENUATION_PER_VOID_BLOCK = 0.000005D;
	public static double           LASER_CANNON_ENERGY_ATTENUATION_PER_BROKEN_BLOCK = 0.23D;
	public static int              LASER_CANNON_RANGE_MAX = 500;
	
	public static int              LASER_CANNON_ENTITY_HIT_SET_ON_FIRE_SECONDS = 20;
	public static int              LASER_CANNON_ENTITY_HIT_ENERGY = 15000;
	public static int              LASER_CANNON_ENTITY_HIT_BASE_DAMAGE = 3;
	public static int              LASER_CANNON_ENTITY_HIT_ENERGY_PER_DAMAGE = 30000;
	public static int              LASER_CANNON_ENTITY_HIT_MAX_DAMAGE = 100;
	
	public static int              LASER_CANNON_ENTITY_HIT_EXPLOSION_ENERGY_THRESHOLD = 900000;
	public static float            LASER_CANNON_ENTITY_HIT_EXPLOSION_BASE_STRENGTH = 4.0F;
	public static int              LASER_CANNON_ENTITY_HIT_EXPLOSION_ENERGY_PER_STRENGTH = 125000;
	public static float            LASER_CANNON_ENTITY_HIT_EXPLOSION_MAX_STRENGTH = 4.0F;
	
	public static int              LASER_CANNON_BLOCK_HIT_ENERGY_MIN = 75000;
	public static int              LASER_CANNON_BLOCK_HIT_ENERGY_PER_BLOCK_HARDNESS = 150000;
	public static int              LASER_CANNON_BLOCK_HIT_ENERGY_MAX = 750000;
	public static double           LASER_CANNON_BLOCK_HIT_ABSORPTION_PER_BLOCK_HARDNESS = 0.01;
	public static double           LASER_CANNON_BLOCK_HIT_ABSORPTION_MAX = 0.80;
	
	public static float            LASER_CANNON_BLOCK_HIT_EXPLOSION_HARDNESS_THRESHOLD = 5.0F;
	public static float            LASER_CANNON_BLOCK_HIT_EXPLOSION_BASE_STRENGTH = 8.0F;
	public static int              LASER_CANNON_BLOCK_HIT_EXPLOSION_ENERGY_PER_STRENGTH = 125000;
	public static float            LASER_CANNON_BLOCK_HIT_EXPLOSION_MAX_STRENGTH = 50F;
	
	// Mining laser
	// BuildCraft quarry values for reference
	// - harvesting one block is 60 MJ/block = 600 RF/block = ~145 EU/block
	// - maximum speed is 3.846 ticks per blocks
	// - overall consumption varies from 81.801 to 184.608 MJ/block (depending on speed) = up to 1846.08 RF/block = up to ~448 EU/block
	// - at radius 5, one layer takes ~465 ticks ((radius * 2 + 1) ^ 2 * 3.846)
	// - overall consumption is ((radius * 2 + 1) ^ 2) * 448 => ~ 54208 EU/layer
	// WarpDrive mining laser in comparison
	// - each mined layer is scanned twice
	// - default ore generation: 1 ore out of 25 blocks
	// - overall consumption in 'all, space' is energyPerLayer / ((radius * 2 + 1) ^ 2) + energyPerBlock => ~ 356 EU/block in space
	// - overall consumption in 'all, space' is energyPerLayer + ((radius * 2 + 1) ^ 2) * energyPerBlock => ~ 43150 EU/layer in space
	// - overall consumption in 'ores, space' is energyPerLayer + ((radius * 2 + 1) ^ 2) * energyPerBlock * factorOresOnly / 25 => ~ 28630 EU/layer in space
	// - at radius 5, one layer takes (2 * MINING_LASER_SCAN_DELAY_TICKS + MINING_LASER_MINE_DELAY_TICKS * (radius * 2 + 1) ^ 2) => 403 ticks
	// Nota: this is only assuming minimum radius of 5 (11x11), with 1 ore for 25 blocks mined.
	public static int              MINING_LASER_MAX_MEDIUMS_COUNT = 3;
	public static int              MINING_LASER_RADIUS_NO_LASER_MEDIUM = 4;
	public static int              MINING_LASER_RADIUS_PER_LASER_MEDIUM = 1;
	
	public static int              MINING_LASER_SETUP_UPDATE_PARAMETERS_TICKS = 20;
	public static int              MINING_LASER_WARMUP_DELAY_TICKS = 20;
	public static int              MINING_LASER_SCAN_DELAY_TICKS = 20;
	public static int              MINING_LASER_MINE_DELAY_TICKS = 3;
	
	public static int              MINING_LASER_SCAN_ENERGY_PER_LAYER_IN_VOID = 20000;
	public static int              MINING_LASER_SCAN_ENERGY_PER_LAYER_IN_ATMOSPHERE = 30000;
	public static int              MINING_LASER_MINE_ENERGY_PER_BLOCK_IN_VOID = 1500;
	public static int              MINING_LASER_MINE_ENERGY_PER_BLOCK_IN_ATMOSPHERE = 2500;
	public static double           MINING_LASER_MINE_ORES_ONLY_ENERGY_FACTOR = 15.0; // lower than 25 to encourage keeping the land 'clean', higher than 13 to use more than scanning 
	public static double           MINING_LASER_MINE_SILKTOUCH_ENERGY_FACTOR = 1.5;
	public static int              MINING_LASER_MINE_SILKTOUCH_DEUTERIUM_MB = 0;
	public static double           MINING_LASER_MINE_FORTUNE_ENERGY_FACTOR = 1.5;
	public static boolean          MINING_LASER_PUMP_UPGRADE_HARVEST_FLUID = false;
	
	// Laser tree farm
	// oak      tree height is 8 to 11 logs + 2 leaves
	// dark oak tree height is up to 25 logs + 2 leaves
	// jungle   tree height is up to 30 logs + 1 leaf
	// => basic setup is 8, then 18, then up to 32
	public static int              TREE_FARM_MAX_MEDIUMS_COUNT = 5;
	public static int              TREE_FARM_MAX_RADIUS_NO_LASER_MEDIUM = 3;
	public static int              TREE_FARM_MAX_RADIUS_PER_LASER_MEDIUM = 2;
	public static int              TREE_FARM_totalMaxRadius = 0;
	public static int              TREE_FARM_MAX_DISTANCE_NO_LASER_MEDIUM = 8;
	public static int              TREE_FARM_MAX_DISTANCE_PER_MEDIUM = 6;
	
	public static int              TREE_FARM_WARM_UP_DELAY_TICKS = 40;
	public static int              TREE_FARM_SCAN_DELAY_TICKS = 40;
	public static int              TREE_FARM_HARVEST_LOG_DELAY_TICKS = 4;
	public static int              TREE_FARM_BREAK_LEAF_DELAY_TICKS = 2;
	public static int              TREE_FARM_SILKTOUCH_LEAF_DELAY_TICKS = 4;
	public static int              TREE_FARM_TAP_WET_SPOT_DELAY_TICKS = 4;
	public static int              TREE_FARM_TAP_DRY_SPOT_DELAY_TICKS = 1;
	public static int              TREE_FARM_TAP_RUBBER_LOG_DELAY_TICKS = 6;
	public static int              TREE_FARM_PLANT_DELAY_TICKS = 2;
	
	public static int              TREE_FARM_SCAN_ENERGY_PER_SURFACE = 1;
	public static int              TREE_FARM_TAP_WET_SPOT_ENERGY_PER_BLOCK = 1;
	public static int              TREE_FARM_TAP_RUBBER_LOG_ENERGY_PER_BLOCK = 2;
	public static int              TREE_FARM_HARVEST_LOG_ENERGY_PER_BLOCK = 1;
	public static int              TREE_FARM_HARVEST_LEAF_ENERGY_PER_BLOCK = 1;
	public static int              TREE_FARM_SILKTOUCH_LOG_ENERGY_PER_BLOCK = 2;
	public static int              TREE_FARM_SILKTOUCH_LEAF_ENERGY_PER_BLOCK = 2;
	public static int              TREE_FARM_PLANT_ENERGY_PER_BLOCK = 1;
	
	// Laser harvester
	// @TODO
	
	// Laser pump
	// @TODO
	
	// Cloaking
	public static int              CLOAKING_MAX_ENERGY_STORED = 500000000;
	public static int              CLOAKING_COIL_CAPTURE_BLOCKS = 5;
	public static int              CLOAKING_MAX_FIELD_RADIUS = 63;
	public static int              CLOAKING_TIER1_ENERGY_PER_BLOCK = 32;
	public static int              CLOAKING_TIER2_ENERGY_PER_BLOCK = 128;
	public static int              CLOAKING_TIER1_FIELD_REFRESH_INTERVAL_TICKS = 60;
	public static int              CLOAKING_TIER2_FIELD_REFRESH_INTERVAL_TICKS = 30;
	public static int              CLOAKING_VOLUME_SCAN_BLOCKS_PER_TICK = 1000;
	public static int              CLOAKING_VOLUME_SCAN_AGE_TOLERANCE_SECONDS = 120;
	
	// Breathing
	public static int              BREATHING_ENERGY_PER_CANISTER = 200;
	public static int[]            BREATHING_ENERGY_PER_NEW_AIR_BLOCK_BY_TIER = { 0, 12, 180, 2610 };
	public static int[]            BREATHING_ENERGY_PER_EXISTING_AIR_BLOCK_BY_TIER = { 0, 4, 60, 870 };
	public static int[]            BREATHING_MAX_ENERGY_STORED_BY_TIER = { 0, 1400, 21000, 304500 };  // almost 6 mn of autonomy
	public static int              BREATHING_AIR_GENERATION_TICKS = 40;
	public static int[]            BREATHING_AIR_GENERATION_RANGE_BLOCKS_BY_TIER = { 200, 16, 48, 144 };
	public static int              BREATHING_VOLUME_UPDATE_DEPTH_BLOCKS = 256;
	public static int              BREATHING_AIR_SIMULATION_DELAY_TICKS = 30;
	public static final boolean    BREATHING_AIR_BLOCK_DEBUG = false;
	public static boolean          BREATHING_AIR_AT_ENTITY_DEBUG = false;
	
	public static int              BREATHING_AIR_TANK_BREATH_DURATION_TICKS = 300;
	public static int[]            BREATHING_AIR_TANK_CAPACITY_BY_TIER = { 20, 32, 64, 128 };
	
	// IC2 Reactor cooler
	public static int              IC2_REACTOR_MAX_HEAT_STORED = 30000;
	public static int              IC2_REACTOR_FOCUS_HEAT_TRANSFER_PER_TICK = 648;
	public static int              IC2_REACTOR_COMPONENT_HEAT_TRANSFER_PER_TICK = 54;
	public static int              IC2_REACTOR_REACTOR_HEAT_TRANSFER_PER_TICK = 54;
	public static int              IC2_REACTOR_COOLING_PER_INTERVAL = 1080;
	public static double           IC2_REACTOR_ENERGY_PER_HEAT = 2.0D;
	public static int              IC2_REACTOR_COOLING_INTERVAL_TICKS = 10;
	
	// Transporter
	public static int              TRANSPORTER_MAX_ENERGY_STORED = 1000000;
	public static int              TRANSPORTER_ENERGY_STORED_UPGRADE_BONUS = TRANSPORTER_MAX_ENERGY_STORED / 2;
	public static int              TRANSPORTER_ENERGY_STORED_UPGRADE_MAX_QUANTITY = 8;
	public static int              TRANSPORTER_SETUP_SCANNER_RANGE_XZ_BLOCKS = 8;
	public static int              TRANSPORTER_SETUP_SCANNER_RANGE_Y_BELOW_BLOCKS = 3;
	public static int              TRANSPORTER_SETUP_SCANNER_RANGE_Y_ABOVE_BLOCKS = 1;
	public static int              TRANSPORTER_RANGE_BASE_BLOCKS = 256;
	public static int              TRANSPORTER_RANGE_UPGRADE_BLOCKS = 64;
	public static int              TRANSPORTER_RANGE_UPGRADE_MAX_QUANTITY = 8;
	public static double[]         TRANSPORTER_LOCKING_ENERGY_FACTORS = { 20.0, 3.0, 0.0, 10.0, 1.0 / Math.sqrt(2.0) };
	public static double           TRANSPORTER_LOCKING_STRENGTH_FACTOR_PER_TICK = Math.pow(0.01D, 1.0D / 300.0D); // natural decay down to 1% over 300 ticks
	public static double           TRANSPORTER_LOCKING_STRENGTH_IN_WILDERNESS = 0.25D;
	public static double           TRANSPORTER_LOCKING_STRENGTH_AT_BEACON = 0.50D;
	public static double           TRANSPORTER_LOCKING_STRENGTH_AT_TRANSPORTER = 1.00D;
	public static double           TRANSPORTER_LOCKING_STRENGTH_BONUS_AT_MAX_ENERGY_FACTOR = 0.5D;
	public static double           TRANSPORTER_LOCKING_STRENGTH_UPGRADE = 0.15D;
	public static double           TRANSPORTER_LOCKING_SPEED_IN_WILDERNESS = 0.25D;
	public static double           TRANSPORTER_LOCKING_SPEED_AT_BEACON = 0.75D;
	public static double           TRANSPORTER_LOCKING_SPEED_AT_TRANSPORTER = 1.0D;
	public static double           TRANSPORTER_LOCKING_SPEED_UPGRADE = 0.25D;
	public static int              TRANSPORTER_LOCKING_SPEED_OPTIMAL_TICKS = 5 * 20;
	public static int              TRANSPORTER_LOCKING_UPGRADE_MAX_QUANTITY = 2;
	public static int              TRANSPORTER_JAMMED_COOLDOWN_TICKS = 2 * 20;
	public static double[]         TRANSPORTER_ENERGIZING_ENERGY_FACTORS = { 10000.0, 1500.0, 0.0, 10.0, 1.0 / Math.sqrt(2.0) };
	public static double           TRANSPORTER_ENERGIZING_MAX_ENERGY_FACTOR = 10.0D;
	public static int              TRANSPORTER_ENERGIZING_FAILURE_MAX_DAMAGE = 5;
	public static double           TRANSPORTER_ENERGIZING_SUCCESS_LOCK_BONUS = 0.20D;
	public static int              TRANSPORTER_ENERGIZING_SUCCESS_MAX_DAMAGE = 100;
	public static double           TRANSPORTER_ENERGIZING_LOCKING_LOST = 0.5D;
	public static int              TRANSPORTER_ENERGIZING_CHARGING_TICKS = 3 * 20;
	public static int              TRANSPORTER_ENERGIZING_COOLDOWN_TICKS = 10 * 20;
	public static double           TRANSPORTER_ENERGIZING_ENTITY_MOVEMENT_TOLERANCE_BLOCKS = 1.0D;
	public static int              TRANSPORTER_ENTITY_GRAB_RADIUS_BLOCKS = 2;
	public static int              TRANSPORTER_FOCUS_SEARCH_RADIUS_BLOCKS = 2;
	public static int              TRANSPORTER_BEACON_MAX_ENERGY_STORED = 60000;
	public static int              TRANSPORTER_BEACON_ENERGY_PER_TICK = 60000 / (300 * 20);  // 10 EU/t over 5 minutes
	public static int              TRANSPORTER_BEACON_DEPLOYING_DELAY_TICKS = 20;
	
	// Enantiomorphic power reactor
	public static int[]            ENAN_REACTOR_MAX_ENERGY_STORED_BY_TIER = { 100000000, 100000000, 500000000, 2000000000 };
	public static final int        ENAN_REACTOR_UPDATE_INTERVAL_TICKS = 5; // hardcoded in the equations
	public static final int        ENAN_REACTOR_FREEZE_INTERVAL_TICKS = 40;
	public static int[]            ENAN_REACTOR_MAX_LASERS_PER_SECOND = { 64, 6, 12, 24 };
	public static int[]            ENAN_REACTOR_GENERATION_MIN_RF_BY_TIER = { 4, 4, 4, 4 };
	public static int[]            ENAN_REACTOR_GENERATION_MAX_RF_BY_TIER = { 64000, 64000, 192000, 576000 };
	public static int[]            ENAN_REACTOR_EXPLOSION_MAX_RADIUS_BY_TIER = { 6, 6, 8, 10 };
	public static double[]         ENAN_REACTOR_EXPLOSION_MAX_REMOVAL_CHANCE_BY_TIER = { 0.1D, 0.1D, 0.1D, 0.1D };
	public static int[]            ENAN_REACTOR_EXPLOSION_COUNT_BY_TIER = { 3, 3, 3, 3 };
	public static float[]          ENAN_REACTOR_EXPLOSION_STRENGTH_MIN_BY_TIER = { 4.0F, 4.0F, 5.0F, 6.0F };
	public static float[]          ENAN_REACTOR_EXPLOSION_STRENGTH_MAX_BY_TIER = { 7.0F, 7.0F, 9.0F, 11.0F };
	
	// Force field setup
	public static int[]            FORCE_FIELD_PROJECTOR_MAX_ENERGY_STORED_BY_TIER = { 20000000, 30000, 90000, 150000 }; // 30000 * (1 + 2 * tier)
	public static double           FORCE_FIELD_PROJECTOR_EXPLOSION_SCALE = 1000.0D;
	public static double           FORCE_FIELD_PROJECTOR_MAX_LASER_REQUIRED = 10.0D;
	public static double           FORCE_FIELD_EXPLOSION_STRENGTH_VANILLA_CAP = 15.0D;
	
	// Subspace capacitor
	public static int[]            CAPACITOR_MAX_ENERGY_STORED_BY_TIER = { 20000000, 800000, 4000000, 20000000 };
	public static String[]         CAPACITOR_IC2_SINK_TIER_NAME_BY_TIER = { "MaxV", "MV", "HV", "EV" };
	public static String[]         CAPACITOR_IC2_SOURCE_TIER_NAME_BY_TIER = { "MaxV", "MV", "HV", "EV" };
	public static int[]            CAPACITOR_FLUX_RATE_INPUT_BY_TIER = { Integer.MAX_VALUE / 2, 800, 4000, 20000 };
	public static int[]            CAPACITOR_FLUX_RATE_OUTPUT_BY_TIER = { Integer.MAX_VALUE / 2, 800, 4000, 20000 };
	public static double[]         CAPACITOR_EFFICIENCY_PER_UPGRADE = { 0.95D, 0.98D, 1.0D };
	
	// Laser lift
	public static int              LIFT_MAX_ENERGY_STORED = 900;
	public static int              LIFT_ENERGY_PER_ENTITY = 150;
	public static int              LIFT_UPDATE_INTERVAL_TICKS = 10;
	public static int              LIFT_ENTITY_COOLDOWN_TICKS = 40;
	
	// Chunk loader
	public static int              CHUNK_LOADER_MAX_ENERGY_STORED = 1000000;
	public static int              CHUNK_LOADER_MAX_RADIUS = 2;
	public static int              CHUNK_LOADER_ENERGY_PER_CHUNK = 8;
	
	// Hull
	public static float[]          HULL_HARDNESS = { 666666.0F, 25.0F, 50.0F, 80.0F };
	public static float[]          HULL_BLAST_RESISTANCE = { 666666.0F, 60.0F, 90.0F, 120.0F };
	public static int[]            HULL_HARVEST_LEVEL = { 666666, 2, 3, 3 };
	
	// Block transformers library
	public static HashMap<String, IBlockTransformer> blockTransformers = new HashMap<>(30);
	
	// Particles accelerator
	public static boolean          ACCELERATOR_ENABLE = true;
	public static final double[]   ACCELERATOR_TEMPERATURES_K = { 270.0, 200.0, 7.0 };
	public static final double     ACCELERATOR_THRESHOLD_DEFAULT = 0.95D;
	public static int              ACCELERATOR_MAX_PARTICLE_BUNCHES = 20;
	
	// Electromagnetic cell
	public static int[]            ELECTROMAGNETIC_CELL_CAPACITY_BY_TIER = { 16000, 500, 1000, 2000 };
	
	// Plasma torch
	public static int[]            PLASMA_TORCH_CAPACITY_BY_TIER = { 16000, 200, 400, 800 };
	
	@Nonnull
	public static Block getBlockOrFire(@Nonnull final String registryName) {
		final ResourceLocation resourceLocation = new ResourceLocation(registryName);
		final Block block = Block.REGISTRY.getObject(resourceLocation);
		if (block == Blocks.AIR) {
			WarpDrive.logger.error(String.format("Failed to get mod block for %s",
			                                     registryName));
			return Blocks.FIRE;
		}
		return block;
	}
	
	@Nonnull
	public static ItemStack getItemStackOrFire(@Nonnull final String registryName, final int meta, final String stringNBT) {
		final Object object = getOreOrItemStackOrNull(registryName, meta);
		if (!(object instanceof ItemStack)) {
			return ItemStack.EMPTY;
		}
		final ItemStack itemStack = (ItemStack) object;
		if (stringNBT == null || stringNBT.isEmpty()) {
			return itemStack;
		}
		try {
			final NBTTagCompound tagCompound = JsonToNBT.getTagFromJson(stringNBT);
			itemStack.setTagCompound(tagCompound);
		} catch (final NBTException exception) {
			WarpDrive.logger.error(exception.getMessage());
			exception.printStackTrace(WarpDrive.printStreamError);
			WarpDrive.logger.error(String.format("Invalid NBT for %s@%d %s",
			                                     registryName, meta, stringNBT));
			return ItemStack.EMPTY;
		}
		return itemStack;
	}
	
	@Nonnull
	public static ItemStack getItemStackOrFire(@Nonnull final String registryName, final int meta) {
		return getItemStackOrFire(registryName, meta, "");
	}
	
	@Nullable
	private static Object getOreOrItemStackOrNull(@Nonnull final String registryName, final int meta) {
		assert registryName.contains(":");
		
		if (registryName.startsWith("ore:")) {
			final String ore = registryName.substring(4);
			if (OreDictionary.doesOreNameExist(ore) && !OreDictionary.getOres(ore).isEmpty()) {
				return ore;
			}
			WarpDrive.logger.info(String.format("Skipping missing ore dictionary entry %s",
			                                    ore));
			return null;
		}
		
		final ResourceLocation resourceLocation = new ResourceLocation(registryName);
		final Item item = Item.REGISTRY.getObject(resourceLocation);
		if (item == null) {
			WarpDrive.logger.info(String.format("Skipping missing mod item %s@%d",
			                                    registryName, meta));
			return null;
		}
		final ItemStack itemStack;
		try {
			if (meta == -1) {
				itemStack = new ItemStack(item);
			} else {
				itemStack = new ItemStack(item, 1, meta);
				if (itemStack.getMetadata() != meta) {
					throw new RuntimeException(String.format("Invalid meta value found %d, expected %d",
					                                         itemStack.getMetadata(), meta ));
				}
			}
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
			WarpDrive.logger.error(String.format("Failed to get mod item for %s@%d",
			                                     registryName, meta ));
			return null;
		}
		return itemStack;
	}
	
	public static Object getOreOrItemStack(final String registryName1, final int meta1,
	                                       @Nonnull final Object... args) {
		// always validate parameters in dev space
		assert args.length % 2 == 0;
		for (int index = 0; index < args.length; index += 2) {
			assert args[index    ] instanceof String;
			assert ((String) args[index]).contains(":");
			assert args[index + 1] instanceof Integer;
		}
		
		// try the first one
		Object object = getOreOrItemStackOrNull(registryName1, meta1);
		if (object != null) {
			return object;
		}
		
		// try the next ones
		for (int index = 0; index < args.length; index += 2) {
			object = getOreOrItemStackOrNull((String) args[index], (Integer) args[index + 1]);
			if (object != null) {
				return object;
			}
		}
		
		return ItemStack.EMPTY;
	}
	
	public static ItemStack getOreDictionaryEntry(final String ore) {
		if (!OreDictionary.doesOreNameExist(ore)) {
			WarpDrive.logger.info(String.format("Skipping missing ore named %s",
			                                    ore));
			return ItemStack.EMPTY;
		}
		final List<ItemStack> itemStacks = OreDictionary.getOres(ore);
		if (itemStacks.isEmpty()) {
			WarpDrive.logger.error(String.format("Failed to get item from empty ore dictionary %s",
			                                     ore));
			return ItemStack.EMPTY;
		}
		return itemStacks.get(0);
	}
	
	protected static double[] getDoubleList(@Nonnull final Configuration config, final String category, final String key, final String comment, final double[] valuesDefault) {
		double[] valuesRead = config.get(category, key, valuesDefault, comment).getDoubleList();
		if (valuesRead.length != valuesDefault.length) {
			valuesRead = valuesDefault.clone();
		}
		
		return valuesRead;
	}
	
	public static void reload(@Nonnull final MinecraftServer server) {
		CelestialObjectManager.clearForReload(false);
		onFMLpreInitialization(stringConfigDirectory);
		onFMLPostInitialization();
		
		final List<EntityPlayerMP> entityPlayers = server.getPlayerList().getPlayers();
		for (final EntityPlayerMP entityPlayerMP : entityPlayers) {
			if ( !(entityPlayerMP instanceof FakePlayer) ) {
				final CelestialObject celestialObject = CelestialObjectManager.get(entityPlayerMP.world,
				                                                                   MathHelper.floor(entityPlayerMP.posX),
				                                                                   MathHelper.floor(entityPlayerMP.posZ));
				PacketHandler.sendClientSync(entityPlayerMP, celestialObject);
			}
		}
	}
	
	public static void onFMLpreInitialization(final String stringConfigDirectory) {
		WarpDriveConfig.stringConfigDirectory = stringConfigDirectory;
		
		// create mod folder
		fileConfigDirectory = new File(stringConfigDirectory, WarpDrive.MODID);
		//noinspection ResultOfMethodCallIgnored
		fileConfigDirectory.mkdir();
		if (!fileConfigDirectory.isDirectory()) {
			throw new RuntimeException(String.format("Unable to create config directory %s",
			                                         fileConfigDirectory));
		}
		
		// unpack default XML files if none are defined
		unpackResourcesToFolder("fillerSets", ".xml", defaultXML_fillerSets, "config", fileConfigDirectory);
		unpackResourcesToFolder("lootSets", ".xml", defaultXML_lootSets, "config", fileConfigDirectory);
		unpackResourcesToFolder("schematicSets", ".xml", defaultXML_schematicSets, "config", fileConfigDirectory);
		unpackResourcesToFolder("structures", ".xml", defaultXML_structures, "config", fileConfigDirectory);
		unpackResourcesToFolder("celestialObjects", ".xml", defaultXML_celestialObjects, "config", fileConfigDirectory);
		
		// always unpack the XML Schema
		unpackResourceToFolder("WarpDrive.xsd", "config", fileConfigDirectory);
		
		// read configuration files
		loadConfig(new File(fileConfigDirectory, "config.yml"));
		loadDictionary(new File(fileConfigDirectory, "dictionary.yml"));
		loadDataFixer(new File(fileConfigDirectory, "dataFixer.yml"));
		CelestialObjectManager.load(fileConfigDirectory);
		
		// create schematics folder
		final File fileSchematicsDirectory = new File(G_SCHEMATICS_LOCATION);
		//noinspection ResultOfMethodCallIgnored
		fileSchematicsDirectory.mkdir();
		if (!fileSchematicsDirectory.isDirectory()) {
			throw new RuntimeException(String.format("Unable to create schematic directory %s",
			                                         fileSchematicsDirectory));
		}
		
		// unpack default schematic files if none are defined
		unpackResourcesToFolder("default", ".schematic", defaultSchematics, "schematics", fileSchematicsDirectory);
		
		// read mod dependencies at runtime and for recipes
		isRedstoneFluxLoaded = Loader.isModLoaded("redstoneflux");
		isComputerCraftLoaded = Loader.isModLoaded("computercraft");
		isCCTweakedLoaded = Loader.isModLoaded("cctweaked");
		isEnderIOLoaded = Loader.isModLoaded("enderio");
		isGregtechLoaded = Loader.isModLoaded("gregtech");
		isIndustrialCraft2Loaded = Loader.isModLoaded("ic2");
		isOpenComputersLoaded = Loader.isModLoaded("opencomputers");
		
		// read mod dependencies for recipes
		isAdvancedRepulsionSystemLoaded = Loader.isModLoaded("AdvancedRepulsionSystems");
		isForgeMultipartLoaded = Loader.isModLoaded("forgemultipartcbe");
		isICBMClassicLoaded = Loader.isModLoaded("icbmclassic");
		isMatterOverdriveLoaded = Loader.isModLoaded("matteroverdrive");
		isNotEnoughItemsLoaded = Loader.isModLoaded("NotEnoughItems");
		isThermalExpansionLoaded = Loader.isModLoaded("thermalexpansion");
		isThermalFoundationLoaded = Loader.isModLoaded("thermalfoundation");
	}
	
	public static void loadConfig(final File file) {
		final Configuration config = new Configuration(file);
		config.load();
		
		// General
		G_SPACE_BIOME_ID = Commons.clamp(Integer.MIN_VALUE, Integer.MAX_VALUE,
				config.get("general", "space_biome_id", G_SPACE_BIOME_ID, "Space biome ID").getInt());
		G_SPACE_PROVIDER_ID = Commons.clamp(Integer.MIN_VALUE, Integer.MAX_VALUE,
				config.get("general", "space_provider_id", G_SPACE_PROVIDER_ID, "Space dimension provider ID").getInt());
		G_HYPERSPACE_PROVIDER_ID = Commons.clamp(Integer.MIN_VALUE, Integer.MAX_VALUE,
				config.get("general", "hyperspace_provider_id", G_HYPERSPACE_PROVIDER_ID, "Hyperspace dimension provider ID").getInt());
		
		G_ENTITY_SPHERE_GENERATOR_ID = Commons.clamp(Integer.MIN_VALUE, Integer.MAX_VALUE,
				config.get("general", "entity_sphere_generator_id", G_ENTITY_SPHERE_GENERATOR_ID, "Entity sphere generator ID").getInt());
		G_ENTITY_STAR_CORE_ID = Commons.clamp(Integer.MIN_VALUE, Integer.MAX_VALUE,
				config.get("general", "entity_star_core_id", G_ENTITY_STAR_CORE_ID, "Entity star core ID").getInt());
		G_ENTITY_CAMERA_ID = Commons.clamp(Integer.MIN_VALUE, Integer.MAX_VALUE,
				config.get("general", "entity_camera_id", G_ENTITY_CAMERA_ID, "Entity camera ID").getInt());
		G_ENTITY_PARTICLE_BUNCH_ID = Commons.clamp(Integer.MIN_VALUE, Integer.MAX_VALUE,
				config.get("general", "entity_particle_bunch_id", G_ENTITY_PARTICLE_BUNCH_ID, "Entity particle bunch ID").getInt());
		G_ENTITY_LASER_EXPLODER_ID = Commons.clamp(Integer.MIN_VALUE, Integer.MAX_VALUE,
				config.get("general", "entity_laser_exploder_id", G_ENTITY_LASER_EXPLODER_ID, "Entity laser exploder ID").getInt());
		G_ENTITY_NPC_ID = Commons.clamp(Integer.MIN_VALUE, Integer.MAX_VALUE,
		        config.get("general", "entity_NPC_id", G_ENTITY_NPC_ID, "Entity NPC ID").getInt());
		G_ENTITY_OFFLINE_AVATAR_ID = Commons.clamp(Integer.MIN_VALUE, Integer.MAX_VALUE,
		        config.get("general", "entity_offline_avatar_id", G_ENTITY_OFFLINE_AVATAR_ID, "Entity offline avatar ID").getInt());
		G_ENTITY_SEAT_ID = Commons.clamp(Integer.MIN_VALUE, Integer.MAX_VALUE,
		        config.get("general", "entity_seat_id", G_ENTITY_SEAT_ID, "Entity seat ID").getInt());
		
		G_LUA_SCRIPTS = Commons.clamp(0, 2,
				config.get("general", "lua_scripts", G_LUA_SCRIPTS,
						"LUA scripts to load when connecting machines: 0 = none, 1 = templates in a subfolder, 2 = ready to roll (templates are still provided)").getInt());
		G_SCHEMATICS_LOCATION = config.get("general", "schematics_location", G_SCHEMATICS_LOCATION, "Root folder where to load and save ship schematics").getString();
		
		G_ASSEMBLY_SCAN_INTERVAL_SECONDS = Commons.clamp(0, 300,
				config.get("general", "assembly_scanning_interval", G_ASSEMBLY_SCAN_INTERVAL_SECONDS,
				           "Reaction delay when updating blocks in an assembly (measured in seconds)").getInt());
		G_ASSEMBLY_SCAN_INTERVAL_TICKS = 20 * WarpDriveConfig.G_ASSEMBLY_SCAN_INTERVAL_SECONDS;
		G_PARAMETERS_UPDATE_INTERVAL_TICKS = Commons.clamp(0, 300,
				config.get("general", "parameters_update_interval", G_PARAMETERS_UPDATE_INTERVAL_TICKS,
				           "Complex computation delay in an assembly (measured in ticks)").getInt());
		G_REGISTRY_UPDATE_INTERVAL_SECONDS = Commons.clamp(0, 300,
		        config.get("general", "registry_update_interval", G_REGISTRY_UPDATE_INTERVAL_SECONDS,
		                   "Registration period for an assembly (measured in seconds)").getInt());
		G_REGISTRY_UPDATE_INTERVAL_TICKS = 20 * WarpDriveConfig.G_REGISTRY_UPDATE_INTERVAL_SECONDS;
		G_ENFORCE_VALID_CELESTIAL_OBJECTS =
				config.get("general", "enforce_valid_celestial_objects", G_ENFORCE_VALID_CELESTIAL_OBJECTS,
				           "Disable to boot the game even when celestial objects are invalid. Use at your own risk!").getBoolean();
		G_BLOCKS_PER_TICK = Commons.clamp(100, 100000,
				config.get("general", "blocks_per_tick", G_BLOCKS_PER_TICK,
				           "Number of blocks to move per ticks, too high will cause lag spikes on ship jumping or deployment, too low may break the ship wirings").getInt());
		G_ENABLE_FAST_SET_BLOCKSTATE = config.get("general", "enable_fast_set_blockstate", G_ENABLE_FAST_SET_BLOCKSTATE,
		                                          "Enable fast blockstate placement, skipping light computation. Disable if you have world implementations conflicts").getBoolean(G_ENABLE_FAST_SET_BLOCKSTATE);
		G_ENABLE_PROTECTION_CHECKS = config.get("general", "enable_protection_checks", G_ENABLE_PROTECTION_CHECKS,
		                                        "Enable area protection checks from other mods or plugins, disable if you use the event system exclusively").getBoolean(G_ENABLE_PROTECTION_CHECKS);
		G_ENABLE_EXPERIMENTAL_REFRESH  = config.get("general", "enable_experimental_refresh", G_ENABLE_EXPERIMENTAL_REFRESH,
		                                            "Enable experimental refresh during jump to prevent duping, use at your own risk").getBoolean(G_ENABLE_EXPERIMENTAL_REFRESH);
		G_ENABLE_EXPERIMENTAL_UNLOAD  = config.get("general", "enable_experimental_unload", G_ENABLE_EXPERIMENTAL_UNLOAD,
		                                           "Enable experimental tile entity unloading during jump to force a cleanup, required for IC2 Classic, may cause issues with other mods").getBoolean(G_ENABLE_EXPERIMENTAL_UNLOAD);
		G_MINIMUM_DIMENSION_UNLOAD_QUEUE_DELAY = Commons.clamp(0, 1000,
				config.get("general", "minimum_dimension_unload_queue_delay_ticks", G_MINIMUM_DIMENSION_UNLOAD_QUEUE_DELAY,
		                   "Enforce a minimum value for Forge's dimensionUnloadQueueDelay to fix various dimension transition issues from unloading the world too soon (set below 100 at your own risk)").getInt());
		ForgeModContainer.dimensionUnloadQueueDelay = Math.max(ForgeModContainer.dimensionUnloadQueueDelay, G_MINIMUM_DIMENSION_UNLOAD_QUEUE_DELAY);
		WarpDrive.logger.info(String.format("Forge's dimensionUnloadQueueDelay is set to %d ticks",
		                                    ForgeModContainer.dimensionUnloadQueueDelay ));
		if (ForgeModContainer.dimensionUnloadQueueDelay < 100) {
			FMLLog.bigWarning("Forge's dimensionUnloadQueueDelay is lower than 100 ticks, world transitions won't work properly!");
			try {
				Thread.sleep(1000L);
			} catch (final Exception exception) {
				// no operation
			}
		}
		G_ENABLE_FORGE_CHUNK_MANAGER = config.get("general", "enable_forge_chunk_manager", G_ENABLE_FORGE_CHUNK_MANAGER,
		                                                   "Enable automatic configuration of Forge's ChunkManager for WarpDrive. Disable to control manually WarpDrive specific settings.").getBoolean(G_ENABLE_FORGE_CHUNK_MANAGER);
		
		G_BLAST_RESISTANCE_CAP = Commons.clamp(10.0F, 6000.0F,
				(float) config.get("general", "blast_resistance_cap", G_BLAST_RESISTANCE_CAP,
				           "Maximum allowed blast resistance for non-hull, breakable blocks from other mods. Required to fix non-sense scaling in modded fluids, etc. Default is basic hull resistance (60).").getDouble(G_BLAST_RESISTANCE_CAP));
		
		// Client
		CLIENT_BREATHING_OVERLAY_FORCED = config.get("client", "breathing_overlay_forced", CLIENT_BREATHING_OVERLAY_FORCED,
		                                             "Force rendering the breathing overlay to compensate HUD modifications").getBoolean(false);
		CLIENT_LOCATION_SCALE = Commons.clamp(0.25F, 4.0F, (float) config.get("client", "location_scale", CLIENT_LOCATION_SCALE,
		                                   "Scale for location text font").getDouble() );
		
		CLIENT_LOCATION_NAME_PREFIX = config.get("client", "location_name_prefix", CLIENT_LOCATION_NAME_PREFIX,
		                                           "Prefix for location name, useful to add formatting").getString();
		{
			String stringValue = config.get("client", "location_background_color", String.format("0x%6X", CLIENT_LOCATION_BACKGROUND_COLOR),
			                                      "Hexadecimal color code for location background (0xAARRGGBB where AA is alpha, RR is Red, GG is Green and BB is Blue component)").getString();
			CLIENT_LOCATION_BACKGROUND_COLOR = (int) (Long.decode(stringValue) & 0xFFFFFFFFL);
			
			stringValue = config.get("client", "location_text_color", String.format("0x%6X", CLIENT_LOCATION_TEXT_COLOR),
			                         "Hexadecimal color code for location foreground (0xAARRGGBB where AA is alpha, RR is Red, GG is Green and BB is Blue component)").getString();
			CLIENT_LOCATION_TEXT_COLOR = (int) (Long.decode(stringValue) & 0xFFFFFFFFL);
		}
		CLIENT_LOCATION_HAS_SHADOW = config.get("client", "location_has_shadow", CLIENT_LOCATION_HAS_SHADOW,
		                                        "Shadow casting option for current celestial object name").getBoolean(CLIENT_LOCATION_HAS_SHADOW);
		CLIENT_LOCATION_SCREEN_ALIGNMENT = EnumDisplayAlignment.valueOf(config.get("client", "location_screen_alignment", CLIENT_LOCATION_SCREEN_ALIGNMENT.name(),
		                                              "Alignment on screen: TOP_LEFT, TOP_CENTER, TOP_RIGHT, MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER or BOTTOM_RIGHT").getString());
		CLIENT_LOCATION_SCREEN_OFFSET_X = config.get("client", "location_offset_x", CLIENT_LOCATION_SCREEN_OFFSET_X,
		                                             "Horizontal offset on screen, increase to move to the right").getInt();
		CLIENT_LOCATION_SCREEN_OFFSET_Y = config.get("client", "location_offset_y", CLIENT_LOCATION_SCREEN_OFFSET_Y,
		                                             "Vertical offset on screen, increase to move down").getInt();
		CLIENT_LOCATION_TEXT_ALIGNMENT = EnumDisplayAlignment.valueOf(config.get("client", "location_text_alignment", CLIENT_LOCATION_TEXT_ALIGNMENT.name(),
		                                            "Text alignment: TOP_LEFT, TOP_CENTER, TOP_RIGHT, MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER or BOTTOM_RIGHT").getString());
		CLIENT_LOCATION_WIDTH_RATIO = (float) config.get("client", "location_width_ratio", CLIENT_LOCATION_WIDTH_RATIO,
		                                         "Text width as a ratio of full screen width").getDouble();
		CLIENT_LOCATION_WIDTH_MIN = config.get("client", "location_width_min", CLIENT_LOCATION_WIDTH_MIN,
		                                       "Text width as a minimum 'pixel' count").getInt();
		
		// Tooltip
		final String commentTooltip = "When to show %s in tooltips. Valid values are " + EnumTooltipCondition.formatAllValues() + ".";
		TOOLTIP_ADD_REGISTRY_NAME = EnumTooltipCondition.valueOf(config.get("tooltip", "add_registry_name", TOOLTIP_ADD_REGISTRY_NAME.name(),
		                                                                     String.format(commentTooltip, "registry name")).getString());
		TOOLTIP_ADD_ORE_DICTIONARY_NAME = EnumTooltipCondition.valueOf(config.get("tooltip", "add_ore_dictionary_name", TOOLTIP_ADD_ORE_DICTIONARY_NAME.name(),
		                                                                           String.format(commentTooltip, "ore dictionary names")).getString());
		TOOLTIP_ADD_ARMOR_POINTS = EnumTooltipCondition.valueOf(config.get("tooltip", "add_armor_points", TOOLTIP_ADD_ARMOR_POINTS.name(),
		                                                                    String.format(commentTooltip, "armor points")).getString());
		TOOLTIP_ADD_BLOCK_MATERIAL = EnumTooltipCondition.valueOf(config.get("tooltip", "add_block_material", TOOLTIP_ADD_BLOCK_MATERIAL.name(),
		                                                                      String.format(commentTooltip, "block material")).getString());
		TOOLTIP_ADD_BURN_TIME = EnumTooltipCondition.valueOf(config.get("tooltip", "add_burn_time", TOOLTIP_ADD_BURN_TIME.name(),
		                                                                 String.format(commentTooltip, "burn time")).getString());
		TOOLTIP_ADD_DURABILITY = EnumTooltipCondition.valueOf(config.get("tooltip", "add_durability", TOOLTIP_ADD_DURABILITY.name(),
		                                                                  String.format(commentTooltip, "durability")).getString());
		TOOLTIP_ADD_ENCHANTABILITY = EnumTooltipCondition.valueOf(config.get("tooltip", "add_enchantability", TOOLTIP_ADD_ENCHANTABILITY.name(),
		                                                                      String.format(commentTooltip, "armor & tool enchantability")).getString());
		TOOLTIP_ADD_ENTITY_ID = EnumTooltipCondition.valueOf(config.get("tooltip", "add_entity_id", TOOLTIP_ADD_ENTITY_ID.name(),
		                                                                 String.format(commentTooltip, "entity id")).getString());
		TOOLTIP_ADD_FLAMMABILITY = EnumTooltipCondition.valueOf(config.get("tooltip", "add_flammability", TOOLTIP_ADD_FLAMMABILITY.name(),
		                                                                    String.format(commentTooltip, "flammability")).getString());
		TOOLTIP_ADD_FLUID = EnumTooltipCondition.valueOf(config.get("tooltip", "add_fluid_stats", TOOLTIP_ADD_FLUID.name(),
		                                                             String.format(commentTooltip, "fluid stats")).getString());
		TOOLTIP_ADD_HARDNESS = EnumTooltipCondition.valueOf(config.get("tooltip", "add_hardness", TOOLTIP_ADD_HARDNESS.name(),
		                                                                String.format(commentTooltip, "hardness & explosion resistance")).getString());
		TOOLTIP_ADD_HARVESTING = EnumTooltipCondition.valueOf(config.get("tooltip", "add_harvesting_stats", TOOLTIP_ADD_HARVESTING.name(),
		                                                                  String.format(commentTooltip, "harvesting stats")).getString());
		TOOLTIP_ADD_OPACITY = EnumTooltipCondition.valueOf(config.get("tooltip", "add_opacity", TOOLTIP_ADD_OPACITY.name(),
		                                                               String.format(commentTooltip, "opacity")).getString());
		TOOLTIP_ADD_REPAIR_WITH = EnumTooltipCondition.valueOf(config.get("tooltip", "add_repair_material", TOOLTIP_ADD_REPAIR_WITH.name(),
		                                                                   String.format(commentTooltip, "repair material")).getString());
		
		TOOLTIP_CLEANUP_LIST = config.get("tooltip", "cleanup_list", TOOLTIP_CLEANUP_LIST,
		                                     "List of lines to remove from tooltips before adding ours. This can be a partial match in a line. Must be lowercase without formatting.").getStringList();
		for (int index = 0; index < TOOLTIP_CLEANUP_LIST.length; index++) {
			final String old = TOOLTIP_CLEANUP_LIST[index];
			TOOLTIP_CLEANUP_LIST[index] = Commons.removeFormatting(old).toLowerCase();
		}
		
		TOOLTIP_ENABLE_DEDUPLICATION = EnumTooltipCondition.valueOf(config.get("tooltip", "enable_deduplication", TOOLTIP_ENABLE_DEDUPLICATION.name(),
		                                                                       String.format("When to remove duplicate lines in tooltips. Valid values are %s.", EnumTooltipCondition.formatAllValues())).getString());
		
		
		// Logging
		LOGGING_THROTTLE_MS = Commons.clamp(0L, 600000L,
		                                    config.get("logging", "throttle_ms", LOGGING_THROTTLE_MS, "How many milliseconds to wait before logging another occurrence in a time sensitive section of the mod (rendering, events, etc.)").getLong(LOGGING_THROTTLE_MS));
		LOGGING_JUMP = config.get("logging", "enable_jump_logs", LOGGING_JUMP, "Basic jump logs, should always be enabled").getBoolean(true);
		LOGGING_JUMPBLOCKS = config.get("logging", "enable_jumpblocks_logs", LOGGING_JUMPBLOCKS, "Detailed jump logs to help debug the mod, will spam your logs...").getBoolean(false);
		LOGGING_ENERGY = config.get("logging", "enable_energy_logs", LOGGING_ENERGY, "Detailed energy logs to help debug the mod, enable it before reporting a bug").getBoolean(false);
		if (WarpDrive.isDev) {// disabled in production, for obvious reasons :)
			LOGGING_EFFECTS = config.get("logging", "enable_effects_logs", LOGGING_EFFECTS, "Detailed effects logs to help debug the mod, will spam your console!").getBoolean(false);
			LOGGING_CLOAKING = config.get("logging", "enable_cloaking_logs", LOGGING_CLOAKING, "Detailed cloaking logs to help debug the mod, will spam your console!").getBoolean(false);
			LOGGING_VIDEO_CHANNEL = config.get("logging", "enable_videoChannel_logs", LOGGING_VIDEO_CHANNEL, "Detailed video channel logs to help debug the mod, will spam your console!").getBoolean(false);
			LOGGING_TARGETING = config.get("logging", "enable_targeting_logs", LOGGING_TARGETING, "Detailed targeting logs to help debug the mod, will spam your console!").getBoolean(false);
			LOGGING_CLIENT_SYNCHRONIZATION = config.get("logging", "enable_client_synchronization_logs", LOGGING_CLIENT_SYNCHRONIZATION, "Detailed client synchronization logs to help debug the mod.").getBoolean(false);
		} else {
			LOGGING_EFFECTS = false;
			LOGGING_CLOAKING = false;
			LOGGING_VIDEO_CHANNEL = false;
			LOGGING_TARGETING = false;
			LOGGING_CLIENT_SYNCHRONIZATION = false;
		}
		LOGGING_WEAPON = config.get("logging", "enable_weapon_logs", LOGGING_WEAPON, "Detailed weapon logs to help debug the mod, enable it before reporting a bug").getBoolean(false);
		LOGGING_CAMERA = config.get("logging", "enable_camera_logs", LOGGING_CAMERA, "Detailed camera logs to help debug the mod, enable it before reporting a bug").getBoolean(false);
		LOGGING_BUILDING = config.get("logging", "enable_building_logs", LOGGING_BUILDING, "Detailed building logs to help debug the mod, enable it before reporting a bug").getBoolean(false);
		LOGGING_COLLECTION = config.get("logging", "enable_collection_logs", LOGGING_COLLECTION, "Detailed collection logs to help debug the mod, enable it before reporting a bug").getBoolean(false);
		LOGGING_TRANSPORTER = config.get("logging", "enable_transporter_logs", LOGGING_TRANSPORTER, "Detailed transporter logs to help debug the mod, enable it before reporting a bug").getBoolean(false);
		LOGGING_LUA = config.get("logging", "enable_LUA_logs", LOGGING_LUA, "Detailed LUA logs to help debug the mod, enable it before reporting a bug").getBoolean(false);
		LOGGING_RADAR = config.get("logging", "enable_radar_logs", LOGGING_RADAR, "Detailed radar logs to help debug the mod, enable it before reporting a bug").getBoolean(false);
		LOGGING_BREATHING = config.get("logging", "enable_breathing_logs", LOGGING_BREATHING, "Detailed breathing logs to help debug the mod, enable it before reporting a bug").getBoolean(false);
		LOGGING_WORLD_GENERATION = config.get("logging", "enable_world_generation_logs", LOGGING_WORLD_GENERATION, "Detailed world generation logs to help debug the mod, enable it before reporting a bug").getBoolean(false);
		LOGGING_PROFILING_CPU_USAGE = config.get("logging", "enable_profiling_CPU_time", LOGGING_PROFILING_CPU_USAGE, "Profiling logs for CPU time, enable it to check for lag").getBoolean(true);
		LOGGING_PROFILING_MEMORY_ALLOCATION = config.get("logging", "enable_profiling_memory_allocation", LOGGING_PROFILING_MEMORY_ALLOCATION, "Profiling logs for memory allocation, enable it to check for lag").getBoolean(true);
		LOGGING_PROFILING_THREAD_SAFETY = config.get("logging", "enable_profiling_thread_safety", LOGGING_PROFILING_THREAD_SAFETY, "Profiling logs for multi-threading, enable it to check for ConcurrentModificationException").getBoolean(false);
		LOGGING_DICTIONARY = config.get("logging", "enable_dictionary_logs", LOGGING_DICTIONARY, "Dictionary logs, enable it to dump blocks hardness and blast resistance at boot").getBoolean(true);
		LOGGING_GLOBAL_REGION_REGISTRY = config.get("logging", "enable_global_region_registry_logs", LOGGING_GLOBAL_REGION_REGISTRY, "GlobalRegion registry logs, enable it to dump global region registry updates").getBoolean(false);
		LOGGING_BREAK_PLACE = config.get("logging", "enable_break_place_logs", LOGGING_BREAK_PLACE, "Detailed break/place event logs to help debug the mod, enable it before reporting a bug").getBoolean(false);
		LOGGING_FORCE_FIELD = config.get("logging", "enable_force_field_logs", LOGGING_FORCE_FIELD, "Detailed force field logs to help debug the mod, enable it before reporting a bug").getBoolean(false);
		LOGGING_FORCE_FIELD_REGISTRY = config.get("logging", "enable_force_field_registry_logs", LOGGING_FORCE_FIELD_REGISTRY, "ForceField registry logs, enable it to dump force field registry updates").getBoolean(false);
		LOGGING_ACCELERATOR = config.get("logging", "enable_accelerator_logs", LOGGING_ACCELERATOR, "Detailed accelerator logs to help debug the mod, enable it before reporting a bug").getBoolean(false);
		LOGGING_XML_PREPROCESSOR = config.get("logging", "enable_XML_preprocessor_logs", LOGGING_XML_PREPROCESSOR, "Save XML preprocessor results as output*.xml file, enable it to debug your XML configuration files").getBoolean(false);
		LOGGING_RENDERING = config.get("logging", "enable_rendering_logs", LOGGING_RENDERING, "Detailed rendering logs to help debug the mod.").getBoolean(false);
		LOGGING_CHUNK_HANDLER = config.get("logging", "enable_chunk_handler_logs", LOGGING_CHUNK_HANDLER, "Detailed chunk data logs to help debug the mod.").getBoolean(false);
		LOGGING_CHUNK_RELOADING = config.get("logging", "enable_experimental_chunk_reloading_logs", LOGGING_CHUNK_RELOADING, "Report in logs when a chunk is reloaded shortly after being unloaded, usually associated with server lag.").getBoolean(false);
		LOGGING_CHUNK_LOADING = config.get("logging", "enable_chunk_loading_logs", LOGGING_CHUNK_LOADING, "Chunk loading logs, enable it to report chunk loaders updates").getBoolean(false);
		LOGGING_ENTITY_FX = config.get("logging", "enable_entity_fx_logs", LOGGING_ENTITY_FX, "EntityFX logs, enable it to dump entityFX registry updates").getBoolean(false);
		LOGGING_GRAVITY = config.get("logging", "enable_gravity_logs", LOGGING_GRAVITY, "Gravity logs, enable it before reporting fall damage and related issues").getBoolean(false);
		LOGGING_OFFLINE_AVATAR = config.get("logging", "enable_offline_avatar_logs", LOGGING_OFFLINE_AVATAR, "Offline avatar logs, enable it before reporting related issues").getBoolean(true);
		
		// Energy handling
		ENERGY_DISPLAY_UNITS = config.get("energy", "display_units", ENERGY_DISPLAY_UNITS, "display units for energy (EU, RF, FE, \u0230I)").getString();
		ENERGY_ENABLE_FE = config.get("energy", "enable_FE", ENERGY_ENABLE_FE, "Enable Forge energy support, disable it for a pure EU or RF energy support").getBoolean(true);
		ENERGY_ENABLE_GTCE_EU = config.get("energy", "enable_GTCE_EU", ENERGY_ENABLE_GTCE_EU, "Enable Gregtech EU energy support when the GregtechCE mod is present, disable otherwise").getBoolean(true);
		ENERGY_ENABLE_IC2_EU = config.get("energy", "enable_IC2_EU", ENERGY_ENABLE_IC2_EU, "Enable IC2 EU energy support when the IndustrialCraft2 mod is present, disable otherwise").getBoolean(true);
		ENERGY_ENABLE_RF = config.get("energy", "enable_RF", ENERGY_ENABLE_RF, "Enable RF energy support when the RedstoneFlux mod is present, disable otherwise").getBoolean(true);
		ENERGY_OVERVOLTAGE_SHOCK_FACTOR = Commons.clamp(0.0F, 10.0F,
				(float) config.get("energy", "overvoltage_shock_factor", ENERGY_OVERVOLTAGE_SHOCK_FACTOR, "Shock damage factor to entities in case of EU voltage overload, set to 0 to disable completely").getDouble());
		ENERGY_OVERVOLTAGE_EXPLOSION_FACTOR = Commons.clamp(0.0F, 10.0F,
				(float) config.get("energy", "overvoltage_explosion_factor", ENERGY_OVERVOLTAGE_EXPLOSION_FACTOR, "Explosion strength factor in case of EU voltage overload, set to 0 to disable completely").getDouble());
		ENERGY_SCAN_INTERVAL_TICKS = Commons.clamp(1, 300,
		        config.get("energy", "scan_interval_ticks", ENERGY_SCAN_INTERVAL_TICKS, "delay between scan for energy receivers (measured in ticks)").getInt());
		
		// Ship movement costs
		SHIP_MOVEMENT_COSTS_FACTORS = new ShipMovementCosts.Factors[EnumShipMovementType.length];
		for (final EnumShipMovementType shipMovementType : EnumShipMovementType.values()) {
			SHIP_MOVEMENT_COSTS_FACTORS[shipMovementType.ordinal()] = new ShipMovementCosts.Factors(
			        shipMovementType.maximumDistanceDefault,
			        shipMovementType.energyRequiredDefault,
			        shipMovementType.warmupDefault,
			        shipMovementType.sicknessDefault,
			        shipMovementType.cooldownDefault);
			if (shipMovementType.hasConfiguration) {
				SHIP_MOVEMENT_COSTS_FACTORS[shipMovementType.ordinal()].load(config, "ship_movement_costs", shipMovementType.getName(), shipMovementType.getDescription());
			}
		}
		
		// Ship
		SHIP_MAX_ENERGY_STORED_BY_TIER =
				config.get("ship", "max_energy_stored_by_tier", SHIP_MAX_ENERGY_STORED_BY_TIER, "Maximum energy stored for a given tier").getIntList();
		clampByTier(1, Integer.MAX_VALUE, SHIP_MAX_ENERGY_STORED_BY_TIER);
		
		SHIP_MASS_MAX_BY_TIER =
		        config.get("ship", "mass_max_by_tier", SHIP_MASS_MAX_BY_TIER, "Maximum ship mass (in blocks) for a given tier").getIntList();
		clampByTier(1, Integer.MAX_VALUE, SHIP_MASS_MAX_BY_TIER);
		SHIP_MASS_MIN_BY_TIER =
		        config.get("ship", "mass_min_by_tier", SHIP_MASS_MIN_BY_TIER, "Minimum ship mass (in blocks) for a given tier").getIntList();
		clampByTier(1, Integer.MAX_VALUE, SHIP_MASS_MIN_BY_TIER);
		// (we don't check min < max here, is it really needed?)
		
		SHIP_MASS_MAX_ON_PLANET_SURFACE = Commons.clamp(0, 10000000,
		        config.get("ship", "volume_max_on_planet_surface", SHIP_MASS_MAX_ON_PLANET_SURFACE, "Maximum ship mass (in blocks) to jump on a planet").getInt());
		SHIP_MASS_MIN_FOR_HYPERSPACE = Commons.clamp(0, 10000000,
		        config.get("ship", "volume_min_for_hyperspace", SHIP_MASS_MIN_FOR_HYPERSPACE, "Minimum ship mass (in blocks) to enter or exit hyperspace without a jumpgate").getInt());
		SHIP_MASS_UNLIMITED_PLAYER_NAMES = config.get("ship", "mass_unlimited_player_names", SHIP_MASS_UNLIMITED_PLAYER_NAMES,
				"List of player names which have unlimited block counts to their ship").getStringList();
		
		SHIP_SIZE_MAX_PER_SIDE_BY_TIER =
				config.get("ship", "size_max_per_side_by_tier", SHIP_SIZE_MAX_PER_SIDE_BY_TIER, "Maximum ship size on each axis in blocks, for a given tier").getIntList();
		clampByTier(1, Integer.MAX_VALUE, SHIP_SIZE_MAX_PER_SIDE_BY_TIER);
		
		SHIP_COLLISION_TOLERANCE_BLOCKS = Commons.clamp(0, 30000000,
				config.get("ship", "collision_tolerance_blocks", SHIP_COLLISION_TOLERANCE_BLOCKS, "Tolerance in block in case of collision before causing damages...").getInt());
		
		SHIP_WARMUP_RANDOM_TICKS = Commons.clamp(10, 200,
				config.get("ship", "warmup_random_ticks", SHIP_WARMUP_RANDOM_TICKS, "Random variation added to warm-up (measured in ticks)").getInt());
		
		SHIP_VOLUME_SCAN_BLOCKS_PER_TICK = Commons.clamp(100, 100000,
		        config.get("ship", "volume_scan_blocks_per_tick", SHIP_VOLUME_SCAN_BLOCKS_PER_TICK, "Number of blocks to scan per tick when getting ship bounds, too high will cause lag spikes when resizing a ship").getInt());
		SHIP_VOLUME_SCAN_AGE_TOLERANCE_SECONDS = Commons.clamp(0, 300,
                config.get("ship", "volume_scan_age_tolerance", SHIP_VOLUME_SCAN_AGE_TOLERANCE_SECONDS, "Ship volume won't be refreshed unless it's older than that many seconds").getInt());
		
		// Jump gate
		JUMP_GATE_SIZE_MAX_PER_SIDE_BY_TIER =
				config.get("jump_gate", "size_max_per_side_by_tier", JUMP_GATE_SIZE_MAX_PER_SIDE_BY_TIER, "Maximum jump gate size on each axis in blocks, for a given tier").getIntList();
		clampByTier(1, Integer.MAX_VALUE, JUMP_GATE_SIZE_MAX_PER_SIDE_BY_TIER);
		
		// Offline avatar
		OFFLINE_AVATAR_ENABLE =
				config.get("offline_avatar", "enable", OFFLINE_AVATAR_ENABLE,
				           "Enable creation of offline avatars to follow ship movements. This only disable creating new ones.").getBoolean(OFFLINE_AVATAR_ENABLE);
		OFFLINE_AVATAR_CREATE_ONLY_ABOARD_SHIPS =
				config.get("offline_avatar", "create_only_aboard_ships", OFFLINE_AVATAR_CREATE_ONLY_ABOARD_SHIPS,
				           "Only create an offline avatar when player disconnects while inside a ship. Disabling may cause lag in spawn areas...").getBoolean(OFFLINE_AVATAR_CREATE_ONLY_ABOARD_SHIPS);
		OFFLINE_AVATAR_FORGET_ON_DEATH =
				config.get("offline_avatar", "forget_on_death", OFFLINE_AVATAR_FORGET_ON_DEATH,
				           "Enable to forget current avatar position when it's killed, or disable player teleportation to last known avatar's position").getBoolean(OFFLINE_AVATAR_FORGET_ON_DEATH);
		OFFLINE_AVATAR_MODEL_SCALE = (float) Commons.clamp(0.20D, 2.00D,
				config.get("offline_avatar", "model_scale", OFFLINE_AVATAR_MODEL_SCALE,
				           "Scale of offline avatar compared to a normal player").getDouble(OFFLINE_AVATAR_MODEL_SCALE));
		OFFLINE_AVATAR_ALWAYS_RENDER_NAME_TAG =
				config.get("offline_avatar", "always_render_name_tag", OFFLINE_AVATAR_ALWAYS_RENDER_NAME_TAG, 
				           "Should avatar name tag always be visible?").getBoolean(OFFLINE_AVATAR_ALWAYS_RENDER_NAME_TAG);
		OFFLINE_AVATAR_MIN_RANGE_FOR_REMOVAL = (float) Commons.clamp(0.10D, 10.00D,
				config.get("offline_avatar", "min_range_for_removal", OFFLINE_AVATAR_MIN_RANGE_FOR_REMOVAL,
				           "Minimum range between a player and their avatar to consider it for removal (i.e. ensuring connection was successful)").getDouble(OFFLINE_AVATAR_MIN_RANGE_FOR_REMOVAL));
		OFFLINE_AVATAR_MAX_RANGE_FOR_REMOVAL = (float) Commons.clamp(Math.max(3.00D, OFFLINE_AVATAR_MIN_RANGE_FOR_REMOVAL), Float.MAX_VALUE,
				config.get("offline_avatar", "max_range_for_removal", OFFLINE_AVATAR_MAX_RANGE_FOR_REMOVAL,
				           "Maximum range between a player and his/her avatar to consider it for removal").getDouble(OFFLINE_AVATAR_MAX_RANGE_FOR_REMOVAL));
		OFFLINE_AVATAR_DELAY_FOR_REMOVAL_SECONDS = Commons.clamp(0, 300,
		        config.get("offline_avatar", "delay_for_removal_s", OFFLINE_AVATAR_DELAY_FOR_REMOVAL_SECONDS,
		                   "Delay before removing an avatar when their related player is in range (measured in seconds)").getInt());
		OFFLINE_AVATAR_DELAY_FOR_REMOVAL_TICKS = OFFLINE_AVATAR_DELAY_FOR_REMOVAL_SECONDS * 20;
		
		// Radar
		RADAR_MAX_ENERGY_STORED = Commons.clamp(0, Integer.MAX_VALUE,
				config.get("radar", "max_energy_stored", RADAR_MAX_ENERGY_STORED, "maximum energy stored").getInt());
		
		RADAR_SCAN_MIN_ENERGY_COST = Commons.clamp(0, Integer.MAX_VALUE,
				config.get("radar", "min_energy_cost", RADAR_SCAN_MIN_ENERGY_COST, "minimum energy cost per scan (0+), independently of radius").getInt());
		RADAR_SCAN_ENERGY_COST_FACTORS = 
				config.get("radar", "factors_energy_cost", RADAR_SCAN_ENERGY_COST_FACTORS, "energy cost factors {a, b, c, d}. You need to provide exactly 4 values.\n"
						+ "The equation used is a + b * radius + c * radius^2 + d * radius^3").getDoubleList();
		if (RADAR_SCAN_ENERGY_COST_FACTORS.length != 4) {
			RADAR_SCAN_ENERGY_COST_FACTORS = new double[4];
			Arrays.fill(RADAR_SCAN_ENERGY_COST_FACTORS, 1.0);
		}
		RADAR_SCAN_MIN_DELAY_SECONDS = Commons.clamp(1, Integer.MAX_VALUE,
				config.get("radar", "scan_min_delay_seconds", RADAR_SCAN_MIN_DELAY_SECONDS, "minimum scan delay per scan (1+), (measured in seconds)").getInt());
		RADAR_SCAN_DELAY_FACTORS_SECONDS = 
				config.get("radar", "scan_delay_factors_seconds", RADAR_SCAN_DELAY_FACTORS_SECONDS, "scan delay factors {a, b, c, d}. You need to provide exactly 4 values.\n"
						+ "The equation used is a + b * radius + c * radius^2 + d * radius^3, (measured in seconds)").getDoubleList();
		if (RADAR_SCAN_DELAY_FACTORS_SECONDS.length != 4) {
			RADAR_SCAN_DELAY_FACTORS_SECONDS = new double[4];
			Arrays.fill(RADAR_SCAN_DELAY_FACTORS_SECONDS, 1.0);
		}
		
		RADAR_MAX_ISOLATION_RANGE = Commons.clamp(2, 8,
				config.get("radar", "max_isolation_range", RADAR_MAX_ISOLATION_RANGE, "radius around core where isolation blocks count (2 to 8), higher is lagger").getInt());
		
		RADAR_MIN_ISOLATION_BLOCKS = Commons.clamp(0, 20,
				config.get("radar", "min_isolation_blocks", RADAR_MIN_ISOLATION_BLOCKS, "number of isolation blocks required to get some isolation (0 to 20)").getInt());
		RADAR_MAX_ISOLATION_BLOCKS = Commons.clamp(5, 94,
				config.get("radar", "max_isolation_blocks", RADAR_MAX_ISOLATION_BLOCKS, "number of isolation blocks required to reach maximum effect (5 to 94)").getInt());
		
		RADAR_MIN_ISOLATION_EFFECT = Commons.clamp(0.01D, 0.95D,
				config.get("radar", "min_isolation_effect", RADAR_MIN_ISOLATION_EFFECT, "isolation effect achieved with min number of isolation blocks (0.01 to 0.95)").getDouble(0.12D));
		RADAR_MAX_ISOLATION_EFFECT = Commons.clamp(0.01D, 1.0D,
				config.get("radar", "max_isolation_effect", RADAR_MAX_ISOLATION_EFFECT, "isolation effect achieved with max number of isolation blocks (0.01 to 1.00)").getDouble(1.00D));
		
		// Ship Scanner
		SS_MAX_DEPLOY_RADIUS_BLOCKS = Commons.clamp(5, 150,
				config.get("ship_scanner", "max_deploy_radius_blocks", SS_MAX_DEPLOY_RADIUS_BLOCKS, "Max distance from ship scanner to ship core, measured in blocks (5-150)").getInt());
		SS_SEARCH_INTERVAL_TICKS = Commons.clamp(5, 150,
			config.get("ship_scanner", "search_interval_ticks", SS_SEARCH_INTERVAL_TICKS, "Max distance from ship scanner to ship core, measured in blocks (5-150)").getInt());
		SS_SCAN_BLOCKS_PER_SECOND = Commons.clamp(1, 50000,
			config.get("ship_scanner", "scan_blocks_per_second", SS_SCAN_BLOCKS_PER_SECOND, "Scanning speed, measured in blocks (1-5000)").getInt());
		SS_DEPLOY_BLOCKS_PER_INTERVAL = Commons.clamp(1, 3000,
			config.get("ship_scanner", "deploy_blocks_per_interval", SS_DEPLOY_BLOCKS_PER_INTERVAL, "Deployment speed, measured in blocks (1-3000)").getInt());
		SS_DEPLOY_INTERVAL_TICKS = Commons.clamp(1, 60,
			config.get("ship_scanner", "deploy_interval_ticks", SS_DEPLOY_INTERVAL_TICKS, "Delay between deployment of 2 sets of blocks, measured in ticks (1-60)").getInt());
		
		// Laser medium
		LASER_MEDIUM_MAX_ENERGY_STORED_BY_TIER =
				config.get("laser_medium", "max_energy_stored_by_tier", LASER_MEDIUM_MAX_ENERGY_STORED_BY_TIER, "Maximum energy stored for a given tier").getIntList();
		clampByTier(1, Integer.MAX_VALUE, LASER_MEDIUM_MAX_ENERGY_STORED_BY_TIER);
		LASER_MEDIUM_FACTOR_BY_TIER =
				config.get("laser_medium", "bonus_factor_by_tier", LASER_MEDIUM_FACTOR_BY_TIER, "Bonus multiplier of a laser medium line for a given tier").getDoubleList();
		clampByTier(0.0D, 4.0D, LASER_MEDIUM_FACTOR_BY_TIER);
		
		// Laser cannon
		LASER_CANNON_MAX_MEDIUMS_COUNT = Commons.clamp(1, 64,
				config.get("laser_cannon", "max_mediums_count", LASER_CANNON_MAX_MEDIUMS_COUNT, "Maximum number of laser mediums per laser").getInt());
		LASER_CANNON_MAX_LASER_ENERGY = Commons.clamp(1, Integer.MAX_VALUE,
				config.get("laser_cannon", "max_laser_energy", LASER_CANNON_MAX_LASER_ENERGY, "Maximum energy in beam after accounting for boosters beams").getInt());
		LASER_CANNON_EMIT_FIRE_DELAY_TICKS = Commons.clamp(1, 100,
				config.get("laser_cannon", "emit_fire_delay_ticks", LASER_CANNON_EMIT_FIRE_DELAY_TICKS, "Delay while booster beams are accepted, before actually shooting").getInt());
		LASER_CANNON_EMIT_SCAN_DELAY_TICKS = Commons.clamp(1, 100,
				config.get("laser_cannon", "emit_scan_delay_ticks", LASER_CANNON_EMIT_SCAN_DELAY_TICKS, "Delay while booster beams are accepted, before actually scanning").getInt());
		
		LASER_CANNON_BOOSTER_BEAM_ENERGY_EFFICIENCY = Commons.clamp(0.01D, 10.0D,
				config.get("laser_cannon", "booster_beam_energy_efficiency", LASER_CANNON_BOOSTER_BEAM_ENERGY_EFFICIENCY, "Energy factor applied from boosting to main laser").getDouble(0.6D));
		LASER_CANNON_ENERGY_ATTENUATION_PER_AIR_BLOCK = Commons.clamp(0.0D, 0.1D,
				config.get("laser_cannon", "energy_attenuation_per_air_block", LASER_CANNON_ENERGY_ATTENUATION_PER_AIR_BLOCK, "Energy attenuation when going through air blocks (on a planet or any gas in space)").getDouble());
		LASER_CANNON_ENERGY_ATTENUATION_PER_VOID_BLOCK = Commons.clamp(0.0D, 0.1D,
				config.get("laser_cannon", "energy_attenuation_per_void_block", LASER_CANNON_ENERGY_ATTENUATION_PER_VOID_BLOCK, "Energy attenuation when going through void blocks (in space or hyperspace)").getDouble());
		LASER_CANNON_ENERGY_ATTENUATION_PER_BROKEN_BLOCK = Commons.clamp(0.0D, 1.0D,
				config.get("laser_cannon", "energy_attenuation_per_broken_block", LASER_CANNON_ENERGY_ATTENUATION_PER_BROKEN_BLOCK, "Energy attenuation when going through a broken block").getDouble());
		LASER_CANNON_RANGE_MAX = Commons.clamp(64, 512,
				config.get("laser_cannon", "range_max", LASER_CANNON_RANGE_MAX, "Maximum distance travelled").getInt());
		
		LASER_CANNON_ENTITY_HIT_SET_ON_FIRE_SECONDS = Commons.clamp(0, 300,
				config.get("laser_cannon", "entity_hit_set_on_fire_seconds", LASER_CANNON_ENTITY_HIT_SET_ON_FIRE_SECONDS, "Duration of fire effect on entity hit (in seconds)").getInt());
		
		LASER_CANNON_ENTITY_HIT_ENERGY = Commons.clamp(0, LASER_CANNON_MAX_LASER_ENERGY,
				config.get("laser_cannon", "entity_hit_energy", LASER_CANNON_ENTITY_HIT_ENERGY, "Base energy consumed from hitting an entity").getInt());
		LASER_CANNON_ENTITY_HIT_BASE_DAMAGE = Commons.clamp(0, LASER_CANNON_MAX_LASER_ENERGY,
				config.get("laser_cannon", "entity_hit_base_damage", LASER_CANNON_ENTITY_HIT_BASE_DAMAGE, "Minimum damage to entity hit (measured in half hearts)").getInt());
		LASER_CANNON_ENTITY_HIT_ENERGY_PER_DAMAGE = Commons.clamp(0, LASER_CANNON_MAX_LASER_ENERGY,
				config.get("laser_cannon", "entity_hit_energy_per_damage", LASER_CANNON_ENTITY_HIT_ENERGY_PER_DAMAGE, "Energy required by additional hit point (won't be consumed)").getInt());
		LASER_CANNON_ENTITY_HIT_MAX_DAMAGE = Commons.clamp(0, Integer.MAX_VALUE,
				config.get("laser_cannon", "entity_hit_max_damage", LASER_CANNON_ENTITY_HIT_MAX_DAMAGE, "Maximum damage to entity hit, set to 0 to disable damage completely").getInt());
		
		LASER_CANNON_ENTITY_HIT_EXPLOSION_ENERGY_THRESHOLD = Commons.clamp(0, Integer.MAX_VALUE,
				config.get("laser_cannon", "entity_hit_energy_threshold_for_explosion", LASER_CANNON_ENTITY_HIT_EXPLOSION_ENERGY_THRESHOLD, "Minimum energy to cause explosion effect").getInt());
		LASER_CANNON_ENTITY_HIT_EXPLOSION_BASE_STRENGTH = (float) Commons.clamp(0.0D, 100.0D,
				config.get("laser_cannon", "entity_hit_explosion_base_strength", LASER_CANNON_ENTITY_HIT_EXPLOSION_BASE_STRENGTH, "Explosion base strength, 4 is Vanilla TNT").getDouble());
		LASER_CANNON_ENTITY_HIT_EXPLOSION_ENERGY_PER_STRENGTH = Commons.clamp(1, Integer.MAX_VALUE,
				config.get("laser_cannon", "entity_hit_explosion_energy_per_strength", LASER_CANNON_ENTITY_HIT_EXPLOSION_ENERGY_PER_STRENGTH, "Energy per added explosion strength").getInt());
		LASER_CANNON_ENTITY_HIT_EXPLOSION_MAX_STRENGTH = (float) Commons.clamp(0.0D, 1000.0D,
				config.get("laser_cannon", "entity_hit_explosion_max_strength", LASER_CANNON_ENTITY_HIT_EXPLOSION_MAX_STRENGTH, "Maximum explosion strength, set to 0 to disable explosion completely").getDouble());
		
		LASER_CANNON_BLOCK_HIT_ENERGY_MIN = Commons.clamp(0, Integer.MAX_VALUE,
				config.get("laser_cannon", "block_hit_energy_min", LASER_CANNON_BLOCK_HIT_ENERGY_MIN, "Minimum energy required for breaking a block").getInt());
		LASER_CANNON_BLOCK_HIT_ENERGY_PER_BLOCK_HARDNESS = Commons.clamp(0, Integer.MAX_VALUE,
				config.get("laser_cannon", "block_hit_energy_per_block_hardness", LASER_CANNON_BLOCK_HIT_ENERGY_PER_BLOCK_HARDNESS, "Energy cost per block hardness for breaking a block").getInt());
		LASER_CANNON_BLOCK_HIT_ENERGY_MAX = Commons.clamp(0, Integer.MAX_VALUE,
				config.get("laser_cannon", "block_hit_energy_max", LASER_CANNON_BLOCK_HIT_ENERGY_MAX, "Maximum energy required for breaking a block").getInt());
		LASER_CANNON_BLOCK_HIT_ABSORPTION_PER_BLOCK_HARDNESS = Commons.clamp(0.0D, 1.0D,
				config.get("laser_cannon", "block_hit_absorption_per_block_hardness", LASER_CANNON_BLOCK_HIT_ABSORPTION_PER_BLOCK_HARDNESS, "Probability of energy absorption (i.e. block not breaking) per block hardness. Set to 1.0 to always break the block.").getDouble());
		LASER_CANNON_BLOCK_HIT_ABSORPTION_MAX = Commons.clamp(0.0D, 1.0D,
				config.get("laser_cannon", "block_hit_absorption_max", LASER_CANNON_BLOCK_HIT_ABSORPTION_MAX, "Maximum probability of energy absorption (i.e. block not breaking)").getDouble());
		
		LASER_CANNON_BLOCK_HIT_EXPLOSION_HARDNESS_THRESHOLD = (float) Commons.clamp(0.0D, 10000.0D,
				config.get("laser_cannon", "block_hit_explosion_hardness_threshold", LASER_CANNON_BLOCK_HIT_EXPLOSION_HARDNESS_THRESHOLD,
						"Minimum block hardness required to cause an explosion").getDouble());
		LASER_CANNON_BLOCK_HIT_EXPLOSION_BASE_STRENGTH = (float) Commons.clamp(0.0D, 1000.0D,
				config.get("laser_cannon", "block_hit_explosion_base_strength", LASER_CANNON_BLOCK_HIT_EXPLOSION_BASE_STRENGTH, "Explosion base strength, 4 is Vanilla TNT").getDouble());
		LASER_CANNON_BLOCK_HIT_EXPLOSION_ENERGY_PER_STRENGTH = Commons.clamp(1, Integer.MAX_VALUE,
				config.get("laser_cannon", "block_hit_explosion_energy_per_strength", LASER_CANNON_BLOCK_HIT_EXPLOSION_ENERGY_PER_STRENGTH, "Energy per added explosion strength").getInt());
		LASER_CANNON_BLOCK_HIT_EXPLOSION_MAX_STRENGTH = (float) Commons.clamp(0.0D, 1000.0D,
				config.get("laser_cannon", "block_hit_explosion_max_strength", LASER_CANNON_BLOCK_HIT_EXPLOSION_MAX_STRENGTH, "Maximum explosion strength, set to 0 to disable explosion completely").getDouble());
		
		// Mining Laser
		MINING_LASER_MAX_MEDIUMS_COUNT = Commons.clamp(1, 10,
				config.get("mining_laser", "max_mediums_count", MINING_LASER_MAX_MEDIUMS_COUNT, "Maximum number of laser mediums").getInt());
		MINING_LASER_RADIUS_NO_LASER_MEDIUM = Commons.clamp(0, 15,
		                                                    config.get("mining_laser", "radius_no_laser_medium", MINING_LASER_RADIUS_NO_LASER_MEDIUM, "Mining radius without any laser medium, measured in blocks").getInt());
		MINING_LASER_RADIUS_PER_LASER_MEDIUM = Commons.clamp(1, 8,
		                                                     config.get("mining_laser", "radius_per_laser_medium", MINING_LASER_RADIUS_PER_LASER_MEDIUM, "Bonus to mining radius per laser medium, measured in blocks").getInt());
		
		MINING_LASER_WARMUP_DELAY_TICKS = Commons.clamp(1, 300,
				config.get("mining_laser", "warmup_delay_ticks", MINING_LASER_WARMUP_DELAY_TICKS, "Warmup duration (buffer on startup when energy source is weak)").getInt());
		MINING_LASER_SCAN_DELAY_TICKS = Commons.clamp(1, 300,
				config.get("mining_laser", "scan_delay_ticks", MINING_LASER_SCAN_DELAY_TICKS, "Scan duration per layer").getInt());
		MINING_LASER_MINE_DELAY_TICKS = Commons.clamp(1, 300,
				config.get("mining_laser", "mine_delay_ticks", MINING_LASER_MINE_DELAY_TICKS, "Mining duration per scanned block").getInt());
		
		MINING_LASER_SCAN_ENERGY_PER_LAYER_IN_ATMOSPHERE = Commons.clamp(1, Integer.MAX_VALUE,
				config.get("mining_laser", "scan_energy_per_layer_in_atmosphere", MINING_LASER_SCAN_ENERGY_PER_LAYER_IN_ATMOSPHERE, "Energy cost per layer on a planet with atmosphere").getInt());
		MINING_LASER_MINE_ENERGY_PER_BLOCK_IN_ATMOSPHERE = Commons.clamp(1, Integer.MAX_VALUE,
				config.get("mining_laser", "mine_energy_per_block_in_atmosphere", MINING_LASER_MINE_ENERGY_PER_BLOCK_IN_ATMOSPHERE, "Energy cost per block on a planet with atmosphere").getInt());
		MINING_LASER_SCAN_ENERGY_PER_LAYER_IN_VOID = Commons.clamp(1, Integer.MAX_VALUE,
				config.get("mining_laser", "scan_energy_per_layer_in_void", MINING_LASER_SCAN_ENERGY_PER_LAYER_IN_VOID, "Energy cost per layer in space or a planet without atmosphere").getInt());
		MINING_LASER_MINE_ENERGY_PER_BLOCK_IN_VOID = Commons.clamp(1, Integer.MAX_VALUE,
				config.get("mining_laser", "mine_energy_per_block_in_void", MINING_LASER_MINE_ENERGY_PER_BLOCK_IN_VOID, "Energy cost per block in space or a planet without atmosphere").getInt());
		
		MINING_LASER_MINE_ORES_ONLY_ENERGY_FACTOR = Commons.clamp(1.5D, 1000.0D,
				config.get("mining_laser", "mine_ores_only_energy_factor", MINING_LASER_MINE_ORES_ONLY_ENERGY_FACTOR, "Energy cost multiplier per block when mining only ores").getDouble(MINING_LASER_MINE_ORES_ONLY_ENERGY_FACTOR));
		MINING_LASER_MINE_SILKTOUCH_ENERGY_FACTOR = Commons.clamp(1.5D, 1000.0D,
				config.get("mining_laser", "mine_silktouch_energy_factor", MINING_LASER_MINE_SILKTOUCH_ENERGY_FACTOR, "Energy cost multiplier per block when mining with silktouch").getDouble(MINING_LASER_MINE_SILKTOUCH_ENERGY_FACTOR));
		
		if (unused) {
			MINING_LASER_MINE_SILKTOUCH_DEUTERIUM_MB = Commons.clamp(0, 10000,
					config.get("mining_laser", "mine_silktouch_deuterium_mB", MINING_LASER_MINE_SILKTOUCH_DEUTERIUM_MB, "Deuterium cost per block when mining with silktouch (0 to disable)").getInt());
			if (MINING_LASER_MINE_SILKTOUCH_DEUTERIUM_MB < 1) {
				MINING_LASER_MINE_SILKTOUCH_DEUTERIUM_MB = 0;
			}
			MINING_LASER_MINE_FORTUNE_ENERGY_FACTOR = Commons.clamp(0.01D, 1000.0D,
					config.get("mining_laser", "fortune_energy_factor", MINING_LASER_MINE_FORTUNE_ENERGY_FACTOR, "Energy cost multiplier per fortune level").getDouble(MINING_LASER_MINE_FORTUNE_ENERGY_FACTOR));
		}
		MINING_LASER_PUMP_UPGRADE_HARVEST_FLUID = config.get("mining_laser", "pump_upgrade_harvest_fluid", MINING_LASER_PUMP_UPGRADE_HARVEST_FLUID, "Pump upgrade will actually pump fluid source if a tank is found, instead of just evaporating it").getBoolean(false);
		
		// Tree Farm
		TREE_FARM_MAX_MEDIUMS_COUNT = Commons.clamp(1, 10,
				config.get("tree_farm", "max_mediums_count", TREE_FARM_MAX_MEDIUMS_COUNT, "Maximum number of laser mediums").getInt());
		TREE_FARM_MAX_RADIUS_NO_LASER_MEDIUM = Commons.clamp(0, 15,
				config.get("tree_farm", "max_radius_no_laser_medium", TREE_FARM_MAX_RADIUS_NO_LASER_MEDIUM, "Maximum scan radius without any laser medium, on X and Z axis, measured in blocks").getInt());
		TREE_FARM_MAX_RADIUS_PER_LASER_MEDIUM = Commons.clamp(1, 8,
				config.get("tree_farm", "max_radius_per_laser_medium", TREE_FARM_MAX_RADIUS_PER_LASER_MEDIUM, "Bonus to maximum scan radius per laser medium, on X and Z axis, measured in blocks").getInt());
		TREE_FARM_totalMaxRadius = TREE_FARM_MAX_RADIUS_NO_LASER_MEDIUM + TREE_FARM_MAX_MEDIUMS_COUNT * TREE_FARM_MAX_RADIUS_PER_LASER_MEDIUM;
		
		TREE_FARM_MAX_DISTANCE_NO_LASER_MEDIUM = Commons.clamp(1, 64,
				config.get("tree_farm", "max_reach_distance_no_laser_medium", TREE_FARM_MAX_DISTANCE_NO_LASER_MEDIUM, "Maximum reach distance of the laser without any laser medium, measured in blocks").getInt());
		TREE_FARM_MAX_DISTANCE_PER_MEDIUM = Commons.clamp(0, 16,
				config.get("tree_farm", "max_reach_distance_per_laser_medium", TREE_FARM_MAX_DISTANCE_PER_MEDIUM, "Bonus to maximum reach distance per laser medium, measured in blocks").getInt());
		
		// Cloaking
		CLOAKING_MAX_ENERGY_STORED = Commons.clamp(1, Integer.MAX_VALUE,
				config.get("cloaking", "max_energy_stored", CLOAKING_MAX_ENERGY_STORED, "Maximum energy stored").getInt());
		CLOAKING_COIL_CAPTURE_BLOCKS = Commons.clamp(0, 30,
				config.get("cloaking", "coil_capture_blocks", CLOAKING_COIL_CAPTURE_BLOCKS, "Extra blocks covered after the outer coils").getInt());
		CLOAKING_MAX_FIELD_RADIUS = Commons.clamp(CLOAKING_COIL_CAPTURE_BLOCKS + 3, 128,
				config.get("cloaking", "max_field_radius", CLOAKING_MAX_FIELD_RADIUS, "Maximum distance between cloaking core and any cloaked side").getInt());
		CLOAKING_TIER1_ENERGY_PER_BLOCK = Commons.clamp(0, Integer.MAX_VALUE,
				config.get("cloaking", "tier1_energy_per_block", CLOAKING_TIER1_ENERGY_PER_BLOCK, "Energy cost per non-air block in a Tier1 cloak").getInt());
		CLOAKING_TIER2_ENERGY_PER_BLOCK = Commons.clamp(CLOAKING_TIER1_ENERGY_PER_BLOCK, Integer.MAX_VALUE,
				config.get("cloaking", "tier2_energy_per_block", CLOAKING_TIER2_ENERGY_PER_BLOCK, "Energy cost per non-air block in a Tier2 cloak").getInt());
		CLOAKING_TIER1_FIELD_REFRESH_INTERVAL_TICKS = Commons.clamp(20, 600,
				config.get("cloaking", "tier1_field_refresh_interval_ticks", CLOAKING_TIER1_FIELD_REFRESH_INTERVAL_TICKS, "Update speed of a Tier1 cloak").getInt());
		CLOAKING_TIER2_FIELD_REFRESH_INTERVAL_TICKS = Commons.clamp(20, 600,
                config.get("cloaking", "tier2_field_refresh_interval_ticks", CLOAKING_TIER2_FIELD_REFRESH_INTERVAL_TICKS, "Update speed of a Tier2 cloak").getInt());
		CLOAKING_VOLUME_SCAN_BLOCKS_PER_TICK = Commons.clamp(100, 100000,
				config.get("cloaking", "volume_scan_blocks_per_tick", CLOAKING_VOLUME_SCAN_BLOCKS_PER_TICK, "Number of blocks to scan per tick when getting cloak bounds, too high will cause lag spikes when resizing a cloak").getInt());
		CLOAKING_VOLUME_SCAN_AGE_TOLERANCE_SECONDS = Commons.clamp(0, 300,
				config.get("cloaking", "volume_scan_age_tolerance", CLOAKING_VOLUME_SCAN_AGE_TOLERANCE_SECONDS, "Cloak volume won't be refreshed unless it's older than that many seconds").getInt());
		
		// Air generator
		BREATHING_MAX_ENERGY_STORED_BY_TIER = config.get("breathing", "max_energy_stored_by_tier", BREATHING_MAX_ENERGY_STORED_BY_TIER, "Maximum energy stored for a given tier").getIntList();
		clampByTier(1, Integer.MAX_VALUE, BREATHING_MAX_ENERGY_STORED_BY_TIER);
		
		BREATHING_ENERGY_PER_CANISTER = Commons.clamp(1, BREATHING_MAX_ENERGY_STORED_BY_TIER[1],
		        config.get("breathing", "energy_per_canister", BREATHING_ENERGY_PER_CANISTER, "Energy cost per air canister refilled").getInt());
		
		BREATHING_ENERGY_PER_NEW_AIR_BLOCK_BY_TIER = config.get("breathing", "energy_per_new_air_block_by_tier", BREATHING_ENERGY_PER_NEW_AIR_BLOCK_BY_TIER, "Energy cost to start air distribution per open side per interval for a given tier").getIntList();
		clampByTier(1, BREATHING_MAX_ENERGY_STORED_BY_TIER[2], BREATHING_ENERGY_PER_NEW_AIR_BLOCK_BY_TIER);
		BREATHING_ENERGY_PER_NEW_AIR_BLOCK_BY_TIER[0] = Commons.clamp(1, BREATHING_MAX_ENERGY_STORED_BY_TIER[0], BREATHING_ENERGY_PER_NEW_AIR_BLOCK_BY_TIER[0]);
		BREATHING_ENERGY_PER_NEW_AIR_BLOCK_BY_TIER[1] = Commons.clamp(1, BREATHING_MAX_ENERGY_STORED_BY_TIER[1], BREATHING_ENERGY_PER_NEW_AIR_BLOCK_BY_TIER[1]);
		BREATHING_ENERGY_PER_NEW_AIR_BLOCK_BY_TIER[2] = Commons.clamp(1, BREATHING_MAX_ENERGY_STORED_BY_TIER[2], BREATHING_ENERGY_PER_NEW_AIR_BLOCK_BY_TIER[2]);
		
		BREATHING_ENERGY_PER_EXISTING_AIR_BLOCK_BY_TIER = config.get("breathing", "energy_per_existing_air_block_by_tier", BREATHING_ENERGY_PER_EXISTING_AIR_BLOCK_BY_TIER, "Energy cost to sustain air distribution per open side per interval").getIntList();
		clampByTier(1, BREATHING_MAX_ENERGY_STORED_BY_TIER[2], BREATHING_ENERGY_PER_EXISTING_AIR_BLOCK_BY_TIER);
		BREATHING_ENERGY_PER_EXISTING_AIR_BLOCK_BY_TIER[0] = Commons.clamp(1, BREATHING_MAX_ENERGY_STORED_BY_TIER[0], BREATHING_ENERGY_PER_EXISTING_AIR_BLOCK_BY_TIER[0]);
		BREATHING_ENERGY_PER_EXISTING_AIR_BLOCK_BY_TIER[1] = Commons.clamp(1, BREATHING_MAX_ENERGY_STORED_BY_TIER[1], BREATHING_ENERGY_PER_EXISTING_AIR_BLOCK_BY_TIER[1]);
		BREATHING_ENERGY_PER_EXISTING_AIR_BLOCK_BY_TIER[2] = Commons.clamp(1, BREATHING_MAX_ENERGY_STORED_BY_TIER[2], BREATHING_ENERGY_PER_EXISTING_AIR_BLOCK_BY_TIER[2]);
		
		BREATHING_AIR_GENERATION_TICKS = Commons.clamp(1, 300,
				config.get("breathing", "air_generation_interval_ticks", BREATHING_AIR_GENERATION_TICKS, "Update speed of air generation").getInt());
		
		BREATHING_AIR_GENERATION_RANGE_BLOCKS_BY_TIER = config.get("breathing", "air_generation_range_blocks", BREATHING_AIR_GENERATION_RANGE_BLOCKS_BY_TIER, "Maximum range of an air generator for each tier, measured in block").getIntList();
		clampByTier(8, 256, BREATHING_AIR_GENERATION_RANGE_BLOCKS_BY_TIER);
		
		BREATHING_VOLUME_UPDATE_DEPTH_BLOCKS = Commons.clamp(10, 256,
		        config.get("breathing", "volume_update_depth_blocks", BREATHING_VOLUME_UPDATE_DEPTH_BLOCKS, "Maximum depth of blocks to update when a volume has changed.\nHigher values may cause TPS lag spikes, Lower values will exponentially increase the repressurization time").getInt());
		BREATHING_AIR_SIMULATION_DELAY_TICKS = Commons.clamp(1, 90,
				config.get("breathing", "simulation_delay_ticks", BREATHING_AIR_SIMULATION_DELAY_TICKS, "Minimum delay between consecutive air propagation updates of the same block.").getInt());
		BREATHING_AIR_AT_ENTITY_DEBUG = config.get("breathing", "enable_air_at_entity_debug", BREATHING_AIR_AT_ENTITY_DEBUG, "Spam creative players with air status around them, use at your own risk.").getBoolean(false);
		
		BREATHING_AIR_TANK_BREATH_DURATION_TICKS = Commons.clamp(100, 1200,
				config.get("breathing", "air_tank_breath_duration_ticks", BREATHING_AIR_TANK_BREATH_DURATION_TICKS, "Duration of a single breath cycle measured in ticks.").getInt());
		BREATHING_AIR_TANK_CAPACITY_BY_TIER = config.get("breathing", "air_tank_capacity_by_tier", BREATHING_AIR_TANK_CAPACITY_BY_TIER, "Number of breaths cycles available in a air tank, by tier (canister, normal, advanced, superior).").getIntList();
		clampByTier(8, 32767, BREATHING_AIR_TANK_CAPACITY_BY_TIER); // Warning: this is hack since we're using a different tier system
		
		// IC2 Reactor cooler
		IC2_REACTOR_MAX_HEAT_STORED = Commons.clamp(1, 32767,
		        config.get("ic2_reactor_laser", "max_heat_stored", IC2_REACTOR_MAX_HEAT_STORED, "Maximum heat stored in the focus").getInt());
		IC2_REACTOR_COMPONENT_HEAT_TRANSFER_PER_TICK = Commons.clamp(0, 32767,
		        config.get("ic2_reactor_laser", "component_heat_transfer_per_tick", IC2_REACTOR_COMPONENT_HEAT_TRANSFER_PER_TICK, "Maximum component heat added to the focus every reactor tick").getInt());
		IC2_REACTOR_FOCUS_HEAT_TRANSFER_PER_TICK = Commons.clamp(0, 32767,
		        config.get("ic2_reactor_laser", "focus_heat_transfer_per_tick", IC2_REACTOR_FOCUS_HEAT_TRANSFER_PER_TICK, "Maximum heat transferred between 2 connected focus every reactor tick").getInt());
		IC2_REACTOR_REACTOR_HEAT_TRANSFER_PER_TICK = Commons.clamp(0, 32767,
		        config.get("ic2_reactor_laser", "reactor_heat_transfer_per_tick", IC2_REACTOR_REACTOR_HEAT_TRANSFER_PER_TICK, "Maximum reactor heat added to the focus every reactor tick").getInt());
		IC2_REACTOR_COOLING_PER_INTERVAL = Commons.clamp(1, 32767,
		        config.get("ic2_reactor_laser", "cooling_per_interval", IC2_REACTOR_COOLING_PER_INTERVAL, "Heat extracted from the focus by interval").getInt());
		IC2_REACTOR_ENERGY_PER_HEAT = Commons.clamp(2.0D, 100000.0D,
				config.get("ic2_reactor_laser", "energy_per_heat", IC2_REACTOR_ENERGY_PER_HEAT, "Energy cost per heat absorbed").getDouble());
		IC2_REACTOR_COOLING_INTERVAL_TICKS = Commons.clamp(0, 1200,
				config.get("ic2_reactor_laser", "cooling_interval_ticks", IC2_REACTOR_COOLING_INTERVAL_TICKS, "Update speed of the check for reactors to cool down. Use 10 to tick as fast as the reactor simulation").getInt());
		
		// Transporter
		TRANSPORTER_MAX_ENERGY_STORED = Commons.clamp(1, Integer.MAX_VALUE,
				config.get("transporter", "max_energy_stored", TRANSPORTER_MAX_ENERGY_STORED, "Maximum energy stored").getInt());
//		TRANSPORTER_ENERGY_PER_BLOCK = Commons.clamp(1.0D, TRANSPORTER_MAX_ENERGY_STORED / 10.0D,
//				config.get("transporter", "energy_per_block", TRANSPORTER_ENERGY_PER_BLOCK, "Energy cost per block distance").getDouble(100.0D));
//		TRANSPORTER_MAX_BOOST_MUL = Commons.clamp(1.0D, 1000.0D,
//				config.get("transporter", "max_boost", TRANSPORTER_MAX_BOOST_MUL, "Maximum energy boost allowed").getDouble(4.0));
		
		// Enantiomorphic reactor
		ENAN_REACTOR_MAX_ENERGY_STORED_BY_TIER =
				config.get("enantiomorphic_reactor", "max_energy_stored_by_tier", ENAN_REACTOR_MAX_ENERGY_STORED_BY_TIER, "Maximum energy stored in the core for a given tier").getIntList();
		clampByTier(1, Integer.MAX_VALUE, ENAN_REACTOR_MAX_ENERGY_STORED_BY_TIER);
		ENAN_REACTOR_MAX_LASERS_PER_SECOND =
				config.get("enantiomorphic_reactor", "max_lasers", ENAN_REACTOR_MAX_LASERS_PER_SECOND, "Maximum number of stabilisation laser shots per seconds before loosing efficiency").getIntList();
		clampByTier(1, Integer.MAX_VALUE, ENAN_REACTOR_MAX_LASERS_PER_SECOND);
		ENAN_REACTOR_GENERATION_MIN_RF_BY_TIER =
		        config.get("enantiomorphic_reactor", "min_generation_RF_by_tier", ENAN_REACTOR_GENERATION_MIN_RF_BY_TIER, "Minimum energy added to the core when enabled, measured in RF/t, for a given tier").getIntList();
		clampByTier(1, Integer.MAX_VALUE, ENAN_REACTOR_GENERATION_MIN_RF_BY_TIER);
		ENAN_REACTOR_GENERATION_MAX_RF_BY_TIER =
				config.get("enantiomorphic_reactor", "max_generation_RF_by_tier", ENAN_REACTOR_GENERATION_MAX_RF_BY_TIER, "Maximum energy added to the core when enabled, measured in RF/t, for a given tier").getIntList();
		clampByTier(1, Integer.MAX_VALUE, ENAN_REACTOR_GENERATION_MAX_RF_BY_TIER);
		
		// Force field projector
		FORCE_FIELD_PROJECTOR_MAX_ENERGY_STORED_BY_TIER = config.get("force_field", "projector_max_energy_stored_by_tier", FORCE_FIELD_PROJECTOR_MAX_ENERGY_STORED_BY_TIER, "Maximum energy stored for each projector tier").getIntList();
		clampByTier(0, Integer.MAX_VALUE, FORCE_FIELD_PROJECTOR_MAX_ENERGY_STORED_BY_TIER);
		
		FORCE_FIELD_PROJECTOR_EXPLOSION_SCALE = Commons.clamp(1.0D, 1000.0D,
				config.get("force_field", "projector_explosion_scale", FORCE_FIELD_PROJECTOR_EXPLOSION_SCALE,
				           "Scale applied to explosion strength, increase the value to reduce explosion impact on a force field. Enable weapon logs to see the damage level.").getDouble(FORCE_FIELD_PROJECTOR_EXPLOSION_SCALE));
		
		FORCE_FIELD_PROJECTOR_MAX_LASER_REQUIRED = Commons.clamp(1.0D, 1000.0D,
				config.get("force_field", "projector_max_laser_required", FORCE_FIELD_PROJECTOR_MAX_LASER_REQUIRED,
				           "Number of maxed out laser cannons required to break a superior force field.").getDouble(FORCE_FIELD_PROJECTOR_MAX_LASER_REQUIRED));
		
		FORCE_FIELD_EXPLOSION_STRENGTH_VANILLA_CAP = Commons.clamp(3.0D, 1000.0D,
				config.get("force_field", "explosion_strength_vanilla_cap", FORCE_FIELD_EXPLOSION_STRENGTH_VANILLA_CAP,
		                   "Maximum strength for vanilla explosion object used by simple explosives like TechGuns rockets.").getDouble(FORCE_FIELD_EXPLOSION_STRENGTH_VANILLA_CAP));
		
		
		// Subspace capacitor
		CAPACITOR_MAX_ENERGY_STORED_BY_TIER = config.get("capacitor", "max_energy_stored_by_tier", CAPACITOR_MAX_ENERGY_STORED_BY_TIER, "Maximum energy stored for each subspace capacitor tier").getIntList();
		clampByTier(0, Integer.MAX_VALUE, CAPACITOR_MAX_ENERGY_STORED_BY_TIER);
		
		CAPACITOR_IC2_SINK_TIER_NAME_BY_TIER = config.get("capacitor", "ic2_sink_tier_name_by_tier", CAPACITOR_IC2_SINK_TIER_NAME_BY_TIER, "IC2 energy sink tier (ULV, LV, MV, HV, EV, IV, LuV, ZPMV, UV, MaxV) for each subspace capacitor tier").getStringList();
		clampByEnergyTierName("ULV", "MaxV", CAPACITOR_IC2_SINK_TIER_NAME_BY_TIER);
		
		CAPACITOR_IC2_SOURCE_TIER_NAME_BY_TIER = config.get("capacitor", "ic2_source_tier_name_by_tier", CAPACITOR_IC2_SOURCE_TIER_NAME_BY_TIER, "IC2 energy source tier (ULV, LV, MV, HV, EV, IV, LuV, ZPMV, UV, MaxV) for each subspace capacitor tier").getStringList();
		clampByEnergyTierName("ULV", "MaxV", CAPACITOR_IC2_SOURCE_TIER_NAME_BY_TIER);
		
		CAPACITOR_FLUX_RATE_INPUT_BY_TIER = config.get("capacitor", "flux_rate_input_per_tick_by_tier", CAPACITOR_FLUX_RATE_INPUT_BY_TIER, "Flux energy transferred per tick for each subspace capacitor tier").getIntList();
		clampByTier(0, Integer.MAX_VALUE / 5, CAPACITOR_FLUX_RATE_INPUT_BY_TIER);
		
		CAPACITOR_FLUX_RATE_OUTPUT_BY_TIER = config.get("capacitor", "flux_rate_output_per_tick_by_tier", CAPACITOR_FLUX_RATE_OUTPUT_BY_TIER, "Flux energy transferred per tick for each subspace capacitor tier").getIntList();
		clampByTier(0, Integer.MAX_VALUE / 5, CAPACITOR_FLUX_RATE_OUTPUT_BY_TIER);
		
		CAPACITOR_EFFICIENCY_PER_UPGRADE = config.get("capacitor", "efficiency_per_upgrade", CAPACITOR_EFFICIENCY_PER_UPGRADE, "Energy transfer efficiency for each upgrade apply, first value is without upgrades (0.8 means 20% loss)").getDoubleList();
		assert CAPACITOR_EFFICIENCY_PER_UPGRADE.length >= 1;
		CAPACITOR_EFFICIENCY_PER_UPGRADE[0] = Math.min(1.0D, Commons.clamp(                               0.5D, CAPACITOR_EFFICIENCY_PER_UPGRADE[1], CAPACITOR_EFFICIENCY_PER_UPGRADE[0]));
		CAPACITOR_EFFICIENCY_PER_UPGRADE[1] = Math.min(1.0D, Commons.clamp(CAPACITOR_EFFICIENCY_PER_UPGRADE[0], CAPACITOR_EFFICIENCY_PER_UPGRADE[2], CAPACITOR_EFFICIENCY_PER_UPGRADE[1]));
		CAPACITOR_EFFICIENCY_PER_UPGRADE[2] = Math.min(1.0D, Commons.clamp(CAPACITOR_EFFICIENCY_PER_UPGRADE[1], Integer.MAX_VALUE                  , CAPACITOR_EFFICIENCY_PER_UPGRADE[2]));
		
		
		// Lift
		LIFT_MAX_ENERGY_STORED = Commons.clamp(1, Integer.MAX_VALUE,
				config.get("lift", "max_energy_stored", LIFT_MAX_ENERGY_STORED, "Maximum energy stored").getInt());
		LIFT_ENERGY_PER_ENTITY = Commons.clamp(1, Integer.MAX_VALUE,
				config.get("lift", "energy_per_entity", LIFT_ENERGY_PER_ENTITY, "Energy consumed per entity moved").getInt());
		LIFT_UPDATE_INTERVAL_TICKS = Commons.clamp(1, 60,
				config.get("lift", "update_interval_ticks", LIFT_UPDATE_INTERVAL_TICKS, "Update speed of the check for entities").getInt());
		LIFT_ENTITY_COOLDOWN_TICKS = Commons.clamp(1, 6000,
				config.get("lift", "entity_cooldown_ticks", LIFT_ENTITY_COOLDOWN_TICKS, "Cooldown after moving an entity").getInt());
		
		
		// Chunk loader
		CHUNK_LOADER_MAX_ENERGY_STORED = Commons.clamp(1, Integer.MAX_VALUE,
				config.get("chunk_loader", "max_energy_stored", CHUNK_LOADER_MAX_ENERGY_STORED, "Maximum energy stored").getInt());
		CHUNK_LOADER_MAX_RADIUS = Commons.clamp(1, 1000,
				config.get("chunk_loader", "max_radius", CHUNK_LOADER_MAX_RADIUS, "Maximum radius when loading a square shape, measured in chunks. A linear shape can be up to 1 chunk wide by (radius + 1 + radius) ^ 2 chunks long.").getInt());
		CHUNK_LOADER_ENERGY_PER_CHUNK = Commons.clamp(1, 100,
				config.get("chunk_loader", "energy_per_chunk", CHUNK_LOADER_ENERGY_PER_CHUNK, "Energy consumed per chunk loaded").getInt());
		
		
		// Particles accelerator
		ACCELERATOR_ENABLE = config.get("accelerator", "enable", ACCELERATOR_ENABLE, "Enable accelerator blocks. Requires a compatible server, as it won't work in single player").getBoolean(false);
		
		ACCELERATOR_MAX_PARTICLE_BUNCHES = Commons.clamp(2, 100,
				config.get("accelerator", "max_particle_bunches", ACCELERATOR_MAX_PARTICLE_BUNCHES, "Maximum number of particle bunches per accelerator controller").getInt());
		
		
		// post treatment for forge configuration
		if (G_ENABLE_FORGE_CHUNK_MANAGER) {
			final int sideShipMax_chunks = (int) Math.ceil(SHIP_SIZE_MAX_PER_SIDE_BY_TIER[3] / 16.0F) + 1;
			final int sizeShipMax_chunks = sideShipMax_chunks * sideShipMax_chunks;
			final int sizeChunkLoaderMax_chunks = (1 + 2 * CHUNK_LOADER_MAX_RADIUS) * (1 + 2 * CHUNK_LOADER_MAX_RADIUS);
			final int sizeMax_chunks = Math.max(sizeShipMax_chunks, sizeChunkLoaderMax_chunks);
			final Configuration configForgeChunks = ForgeChunkManager.getConfig();
			final Property propertyMaximumTicketCount = configForgeChunks.get(WarpDrive.MODID, "maximumTicketCount", 100);
			if (propertyMaximumTicketCount.getInt() < 2) {
				propertyMaximumTicketCount.set(2);
			}
			final Property propertyMaximumChunksPerTicket = configForgeChunks.get(WarpDrive.MODID, "maximumChunksPerTicket", sizeMax_chunks);
			if (propertyMaximumChunksPerTicket.getInt() < sizeMax_chunks) {
				propertyMaximumChunksPerTicket.set(sizeMax_chunks);
			}
			if (configForgeChunks.hasChanged()) {
				configForgeChunks.save();
			}
		}
		
		// always save to ensure comments are up-to-date
		config.save();
	}
	
	public static void clampByTier(final int min, final int max, @Nonnull final int[] values) {
		if (values.length != EnumTier.length) {
			WarpDrive.logger.error(String.format("Invalid configuration value, expected %d values, got %d %s. Update your configuration and restart your game!",
			                                     EnumTier.length, values.length, Arrays.toString(values)));
			assert false;
			return;
		}
		values[0] = Commons.clamp(min      , max      , values[0]);
		values[1] = Commons.clamp(min      , values[2], values[1]);
		values[2] = Commons.clamp(values[1], values[3], values[2]);
		values[3] = Commons.clamp(values[2], max      , values[3]);
	}
	
	public static void clampByTier(final double min, final double max, @Nonnull final double[] values) {
		if (values.length != EnumTier.length) {
			WarpDrive.logger.error(String.format("Invalid configuration value, expected %d values, got %d %s. Update your configuration and restart your game!",
			                                     EnumTier.length, values.length, Arrays.toString(values)));
			assert false;
			return;
		}
		values[0] = Commons.clamp(min      , max      , values[0]);
		values[1] = Commons.clamp(min      , values[2], values[1]);
		values[2] = Commons.clamp(values[1], values[3], values[2]);
		values[3] = Commons.clamp(values[2], max      , values[3]);
	}
	
	public static void clampByEnergyTierName(final String nameMin, final String nameMax, @Nonnull final String[] names) {
		if (names.length != EnumTier.length) {
			WarpDrive.logger.error(String.format("Invalid configuration value, expected %d string, got %d %s. Update your configuration and restart your game!",
			                                     EnumTier.length, names.length, Arrays.toString(names)));
			assert false;
			return;
		}
		// convert to integer values
		final int min = EnergyWrapper.EU_getTierByName(nameMin);
		final int max = EnergyWrapper.EU_getTierByName(nameMax);
		final int[] values = new int[EnumTier.length];
		for (int index = 0; index < EnumTier.length; index++) {
			values[index] = EnergyWrapper.EU_getTierByName(names[index]);
		}
		clampByTier(min, max, values);
		for (int index = 0; index < EnumTier.length; index++) {
			names[index] = EnergyWrapper.EU_nameTier[values[index]];
		}
	}
	
	public static void loadDictionary(final File file) {
		final Configuration config = new Configuration(file);
		config.load();
		
		Dictionary.loadConfig(config);
	
		config.save();
	}
	
	public static void loadDataFixer(final File file) {
		final Configuration config = new Configuration(file);
		config.load();
		
		WarpDriveDataFixer.loadConfig(config);
		
		config.save();
	}
	
	public static void registerBlockTransformer(final String modId, final IBlockTransformer blockTransformer) {
		blockTransformers.put(modId, blockTransformer);
		WarpDrive.logger.info(modId + " blockTransformer registered");
	}
	
	public static void onFMLInitialization() {
		CompatWarpDrive.register();
		
		// apply compatibility modules
		if (isAdvancedRepulsionSystemLoaded) {
			CompatAdvancedRepulsionSystems.register();
		}
		
		final boolean isAppliedEnergistics2Loaded = Loader.isModLoaded("appliedenergistics2");
		if (isAppliedEnergistics2Loaded) {
			CompatAppliedEnergistics2.register();
		}
		
		final boolean isActuallyAdditionsLoaded = Loader.isModLoaded("actuallyadditions");
		if (isActuallyAdditionsLoaded) {
			CompatActuallyAdditions.register();
		}
		
		final boolean isArsMagica2Loaded = Loader.isModLoaded("arsmagica2");
		if (isArsMagica2Loaded) {
			CompatArsMagica2.register();
		}
		
		if (isComputerCraftLoaded) {
			CompatComputerCraft.register(isCCTweakedLoaded);
		}
		
		if (isEnderIOLoaded) {
			CompatEnderIO.register();
		}
		
		if (isForgeMultipartLoaded) {
			isForgeMultipartLoaded = CompatForgeMultipart.register();
		}
		
		final boolean isImmersiveEngineeringLoaded = Loader.isModLoaded("immersiveengineering");
		if (isImmersiveEngineeringLoaded) {
			CompatImmersiveEngineering.register();
		}
		
		if (isIndustrialCraft2Loaded) {
			loadIC2();
			CompatIndustrialCraft2.register();
		}
		
		if (isOpenComputersLoaded) {
			CompatOpenComputers.register();
		}
		
		if (isThermalExpansionLoaded) {
			CompatThermalExpansion.register();
		}
		
		final boolean isBotaniaLoaded = Loader.isModLoaded("botania");
		if (isBotaniaLoaded) {
			CompatBotania.register();
		}
		
		final boolean isBiblioCraftLoaded = Loader.isModLoaded("bibliocraft");
		if (isBiblioCraftLoaded) {
			CompatBiblioCraft.register();
		}
		
		final boolean isBlockcrafteryLoaded = Loader.isModLoaded("blockcraftery");
		if (isBlockcrafteryLoaded) {
			CompatBlockcraftery.register();
		}
		
		final boolean isBuildCraftLoaded = Loader.isModLoaded("buildcraftcore");
		if (isBuildCraftLoaded) {
			CompatBuildCraft.register();
		}
		
		final boolean isCarpentersBlocksLoaded = Loader.isModLoaded("CarpentersBlocks");
		if (isCarpentersBlocksLoaded) {
			CompatCarpentersBlocks.register();
		}
		
		final boolean isCustomNPCsLoaded = Loader.isModLoaded("customnpcs");
		if (isCustomNPCsLoaded) {
			CompatCustomNPCs.register();
		}
		
		final boolean isDecocraftLoaded = Loader.isModLoaded("props");
		if (isDecocraftLoaded) {
			CompatDecocraft.register();
		}
		
		final boolean isDeepResonanceLoaded = Loader.isModLoaded("deepresonance");
		if (isDeepResonanceLoaded) {
			CompatDeepResonance.register();
		}
		
		final boolean isDraconicEvolutionLoaded = Loader.isModLoaded("draconicevolution");
		if (isDraconicEvolutionLoaded) {
			CompatDraconicEvolution.register();
		}
		
		final boolean isEmbersLoaded = Loader.isModLoaded("embers");
		if (isEmbersLoaded) {
			CompatEmbers.register();
		}
		
		final boolean isEnvironmentalTechLoaded = Loader.isModLoaded("environmentaltech");
		if (isEnvironmentalTechLoaded) {
			CompatEnvironmentalTech.register();
		}
		
		final boolean isExtraUtilities2Loaded = Loader.isModLoaded("extrautils2");
		if (isExtraUtilities2Loaded) {
			CompatExtraUtilities2.register();
		}
		
		final boolean isEvilCraftLoaded = Loader.isModLoaded("evilcraft");
		if (isEvilCraftLoaded) {
			CompatEvilCraft.register();
		}
		
		final boolean isGalacticraftCoreLoaded = Loader.isModLoaded("galacticraftcore");
		if (isGalacticraftCoreLoaded) {
			CompatGalacticraft.register();
		}
		
		// final boolean isGregTechLoaded = Loader.isModLoaded("gregtech");
		if (isGregtechLoaded) {
			CompatGregTech.register();
		}
		
		final boolean isIndustrialForegoingLoaded = Loader.isModLoaded("industrialforegoing");
		if (isIndustrialForegoingLoaded) {
			CompatIndustrialForegoing.register();
		}
		
		final boolean isIronChestLoaded = Loader.isModLoaded("ironchest");
		if (isIronChestLoaded) {
			CompatIronChest.register();
		}
		
		final boolean isMekanismLoaded = Loader.isModLoaded("mekanism");
		if (isMekanismLoaded) {
			CompatMekanism.register();
		}
		
		final boolean isMetalChestsLoaded = Loader.isModLoaded("metalchests");
		if (isMetalChestsLoaded) {
			CompatMetalChests.register();
		}
		
		final boolean isMysticalAgricultureLoaded = Loader.isModLoaded("mysticalagriculture");
		if (isMysticalAgricultureLoaded) {
			CompatMysticalAgriculture.register();
		}
		
		final boolean isNaturaLoaded = Loader.isModLoaded("natura");
		if (isNaturaLoaded) {
			CompatNatura.register();
		}
		
		final boolean isPneumaticCraftLoaded = Loader.isModLoaded("pneumaticcraft");
		if (isPneumaticCraftLoaded) {
			CompatPneumaticCraft.register();
		}
		
		final boolean isRootsLoaded = Loader.isModLoaded("roots");
		if (isRootsLoaded) {
			CompatRoots.register();
		}
		
		final boolean isRusticLoaded = Loader.isModLoaded("rustic");
		if (isRusticLoaded) {
			CompatRustic.register();
		}
		
		final boolean isParziStarWarsLoaded = Loader.isModLoaded("starwarsmod");
		if (isParziStarWarsLoaded) {
			CompatParziStarWars.register();
		}
		
		final boolean isRedstonePasteLoaded = Loader.isModLoaded("redstonepaste");
		if (isRedstonePasteLoaded) {
			CompatRedstonePaste.register();
		}
		
		final boolean isRealFilingCabinetLoaded = Loader.isModLoaded("realfilingcabinet");
		if (isRealFilingCabinetLoaded) {
			CompatRealFilingCabinet.register();
		}
		
		final boolean isRefinedStorageLoaded = Loader.isModLoaded("refinedstorage");
		if (isRefinedStorageLoaded) {
			CompatRefinedStorage.register();
		}
		
		final boolean isSGCraftLoaded = Loader.isModLoaded("sgcraft");
		if (isSGCraftLoaded) {
			CompatSGCraft.register();
		}

		final boolean isJSGLoaded = Loader.isModLoaded("jsg");
		if (isJSGLoaded) {
			CompatJSG.register();
		}
		
		final boolean isStorageDrawersLoaded = Loader.isModLoaded("storagedrawers");
		if (isStorageDrawersLoaded) {
			CompatStorageDrawers.register();
		}
		
		final boolean isTConstructLoaded = Loader.isModLoaded("tconstruct");
		if (isTConstructLoaded) {
			CompatTConstruct.register();
		}
		
		final boolean isTechgunsLoaded = Loader.isModLoaded("techguns");
		if (isTechgunsLoaded) {
			CompatTechguns.register();
		}
		
		final boolean isThaumcraftLoaded = Loader.isModLoaded("thaumcraft");
		if (isThaumcraftLoaded) {
			CompatThaumcraft.register();
		}
		
		final boolean isThermalDynamicsLoaded = Loader.isModLoaded("thermaldynamics");
		if (isThermalDynamicsLoaded) {
			CompatThermalDynamics.register();
		}
		
		final boolean isUndergroundBiomesLoaded = Loader.isModLoaded("undergroundbiomes");
		if (isUndergroundBiomesLoaded) {
			CompatUndergroundBiomes.register();
		}
		
		final boolean isVariedCommoditiesLoaded = Loader.isModLoaded("variedcommodities");
		if (isVariedCommoditiesLoaded) {
			CompatVariedCommodities.register();
		}
		
		final boolean isWootloaded = Loader.isModLoaded("woot");
		if (isWootloaded) {
			CompatWoot.register();
		}
	}
	
	public static void onFMLPostInitialization() {
		// load XML files
		FillerManager.load(fileConfigDirectory);
		LootManager.load(fileConfigDirectory);
		StructureManager.load(fileConfigDirectory);
		
		Dictionary.apply();
		WarpDriveDataFixer.apply();
	}
	
	private static void loadIC2() {
		try {
			// first try IC2 Experimental
			IC2_emptyCell = (ItemStack) getOreOrItemStack("ic2:fluid_cell", 0);
			if (!IC2_emptyCell.isEmpty()) {
				IC2_compressedAir = getItemStackOrFire("ic2:fluid_cell", 0, "{Fluid:{FluidName:\"ic2air\",Amount:1000}}");
				
				IC2_rubberWood = getBlockOrFire("ic2:rubber_wood");
				IC2_Resin = getItemStackOrFire("ic2:misc_resource", 4);
			} else {
				// then go with IC2 Classic
				IC2_emptyCell = getItemStackOrFire("ic2:itemcellempty", 0);
				IC2_compressedAir = getItemStackOrFire("ic2:itemmisc", 100);
				
				IC2_rubberWood = getBlockOrFire("ic2:blockrubwood");
				IC2_Resin = getItemStackOrFire("ic2:itemharz", 0);
			}
			
			// finally, validate results
			if ( IC2_emptyCell.isEmpty()
			  || IC2_compressedAir.isEmpty()
			  || IC2_rubberWood == Blocks.FIRE
			  || IC2_Resin.isEmpty() ) {
				throw new RuntimeException("Unsupported IC2 blocks & items, unable to proceed further");
			}
		} catch (final Exception exception) {
			WarpDrive.logger.error("Error loading IndustrialCraft2 blocks and items");
			exception.printStackTrace(WarpDrive.printStreamError);
		}
	}
	
	public static boolean isIC2CompressedAir(@Nonnull final ItemStack itemStack) {
		final NBTTagCompound nbtCompressedAir = WarpDriveConfig.IC2_compressedAir.getTagCompound();
		return !itemStack.isEmpty()
		    && itemStack.isItemEqual(WarpDriveConfig.IC2_compressedAir)
		    && ( (nbtCompressedAir == null)
		      || (itemStack.getTagCompound() != null && nbtCompressedAir.equals(itemStack.getTagCompound())) );
	}
	
	public static DocumentBuilder getXmlDocumentBuilder() {
		if (xmlDocumentBuilder == null) {
			
			final ErrorHandler xmlErrorHandler = new ErrorHandler() {
				@Override
				public void warning(final SAXParseException exception) {
					// exception.printStackTrace(WarpDrive.printStreamError);
					WarpDrive.logger.warn(String.format("XML warning at line %d: %s",
					                                    exception.getLineNumber(),
					                                    exception.getLocalizedMessage() ));
				}
				
				@Override
				public void fatalError(final SAXParseException exception) {
					// exception.printStackTrace(WarpDrive.printStreamError);
					WarpDrive.logger.warn(String.format("XML fatal error at line %d: %s",
					                      exception.getLineNumber(),
					                      exception.getLocalizedMessage() ));
				}
				
				@Override
				public void error(final SAXParseException exception) {
					// exception.printStackTrace(WarpDrive.printStreamError);
					WarpDrive.logger.warn(String.format("XML error at line %d: %s",
					                                    exception.getLineNumber(),
					                                    exception.getLocalizedMessage() ));
				}
			};
			
			final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilderFactory.setIgnoringComments(false);
			documentBuilderFactory.setNamespaceAware(true);
			documentBuilderFactory.setValidating(true);
			documentBuilderFactory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
			
			try {
				xmlDocumentBuilder = documentBuilderFactory.newDocumentBuilder();
			} catch (final ParserConfigurationException exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
			}
			xmlDocumentBuilder.setErrorHandler(xmlErrorHandler);
		}
		
		return xmlDocumentBuilder;
	}
	
	/**
	 * Check if a category of configuration files are missing, unpack default ones from the mod's resources to the specified target folder
	 * Target folder should be already created
	 **/
	private static void unpackResourcesToFolder(final String prefix, final String suffix, final String[] filenames, final String resourcePathSource, final File folderTarget) {
		final File[] files = fileConfigDirectory.listFiles((file_notUsed, name) -> name.startsWith(prefix) && name.endsWith(suffix));
		if (files == null) {
			throw new RuntimeException(String.format("Critical error accessing target directory, searching for %s*%s files: %s",
			                                         prefix, suffix, folderTarget));
		}
		if (files.length == 0) {
			for (final String filename : filenames) {
				unpackResourceToFolder(filename, resourcePathSource, folderTarget);
			}
		}
	}
	
	/**
	 * Copy a default configuration file from the mod's resources to the specified configuration folder
	 * Target folder should be already created
	 **/
	private static void unpackResourceToFolder(final String filename, final String resourcePathSource, final File folderTarget) {
		final String resourceName = resourcePathSource + "/" + filename;
		
		final File destination = new File(folderTarget, filename);
		
		try {
			final InputStream inputStream = WarpDrive.class.getClassLoader().getResourceAsStream(resourceName);
			final BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(destination));
			
			final byte[] byteBuffer = new byte[Math.max(8192, inputStream.available())];
			int bytesRead;
			while ((bytesRead = inputStream.read(byteBuffer)) >= 0) {
				outputStream.write(byteBuffer, 0, bytesRead);
			}
			
			inputStream.close();
			outputStream.close();
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
			WarpDrive.logger.error(String.format("Failed to unpack resource '%s' into '%s'",
			                                     resourceName, destination ));
		}
	}
}
