package cr0s.warpdrive.config;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockTransformer;
/*
import cr0s.warpdrive.compat.CompatActuallyAdditions;
import cr0s.warpdrive.compat.CompatAppliedEnergistics2;
import cr0s.warpdrive.compat.CompatBiblioCraft;
import cr0s.warpdrive.compat.CompatBlockcraftery;
import cr0s.warpdrive.compat.CompatBotania;
import cr0s.warpdrive.compat.CompatBuildCraft;
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
import cr0s.warpdrive.compat.CompatPneumaticCraft;
import cr0s.warpdrive.compat.CompatRealFilingCabinet;
import cr0s.warpdrive.compat.CompatRedstonePaste;
import cr0s.warpdrive.compat.CompatRefinedStorage;
import cr0s.warpdrive.compat.CompatRoots;
import cr0s.warpdrive.compat.CompatRustic;
import cr0s.warpdrive.compat.CompatSGCraft;
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
*/
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
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.common.ForgeConfigSpec.LongValue;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.registries.ForgeRegistries;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
	
	WarpDriveConfig() {
		throw new UnsupportedOperationException("Instantiation is not supported.");
	}
	
	private static final boolean unused = false; // TODO
	
	private static final ForgeConfigSpec CLIENT_CONFIG;
	private static final ForgeConfigSpec COMMON_CONFIG;
	private static final ForgeConfigSpec SERVER_CONFIG;
	
	private static File            fileConfigDirectory;
	private static DocumentBuilder xmlDocumentBuilder;
	private static final String[]  defaultXML_fillerSets = {
			"fillerSets-default.xml",
			"fillerSets-netherores.xml",
			"fillerSets-undergroundbiomes.xml",
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
	public static boolean              isComputerCraftLoaded = false;
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
	
	public enum EnumLUAscripts {
		NONE,
		ONLY_TEMPLATES,
		ALL
	}
	
	// Mod configuration (see loadConfig() for comments/definitions)
	
	// General
	private static IntValue                         general_space_provider_id;
	private static IntValue                         general_hyperspace_provider_id;
	private static EnumValue<EnumLUAscripts>        general_lua_scripts;
	private static ConfigValue<String>              general_schematics_location;
	private static IntValue                         general_assembly_scanning_interval;
	private static IntValue                         general_parameters_update_interval;
	private static IntValue                         general_registry_update_interval;
	private static BooleanValue                     general_enforce_valid_celestial_objects;
	private static IntValue                         general_blocks_per_tick;
	private static BooleanValue                     general_enable_fast_set_blockstate;
	private static BooleanValue                     general_enable_protection_checks;
	private static BooleanValue                     general_enable_experimental_refresh;
	private static DoubleValue                      general_blast_resistance_cap;
	
	// Atomic
	private static IntValue                         atomic_max_particle_bunches;
	
	// Breathing (common)
	private static ConfigValue<List<Integer>>       breathing_max_energy_stored_by_tier;
	private static IntValue                         breathing_energy_per_canister;
	private static ConfigValue<List<Integer>>       breathing_energy_per_new_air_block_by_tier;
	private static ConfigValue<List<Integer>>       breathing_energy_per_existing_air_block_by_tier;
	private static ConfigValue<List<Integer>>       breathing_air_generation_range_blocks;
	
	// Breathing (server)
	private static IntValue                         breathing_air_generation_interval_ticks;
	private static IntValue                         breathing_volume_update_depth_blocks;
	private static IntValue                         breathing_simulation_delay_ticks;
	private static BooleanValue                     breathing_enable_air_at_entity_debug;
	private static IntValue                         breathing_air_tank_breath_duration_ticks;
	private static ConfigValue<List<Integer>>       breathing_air_tank_capacity_by_tier;
	
	// Capacitor
	private static ConfigValue<List<Integer>>       capacitor_max_energy_stored_by_tier;
	private static ConfigValue<List<String>>        capacitor_ic2_sink_tier_name_by_tier;
	private static ConfigValue<List<String>>        capacitor_ic2_source_tier_name_by_tier;
	private static ConfigValue<List<Integer>>       capacitor_flux_rate_input_per_tick_by_tier;
	private static ConfigValue<List<Integer>>       capacitor_flux_rate_output_per_tick_by_tier;
	private static ConfigValue<List<Double>>        capacitor_efficiency_per_upgrade;
	
	// Client
	private static BooleanValue                     client_breathing_overlay_forced;
	private static DoubleValue                      client_location_scale;
	private static ConfigValue<String>              client_location_name_prefix;
	private static ConfigValue<String>              client_location_background_color;
	private static ConfigValue<String>              client_location_text_color;
	private static BooleanValue                     client_location_has_shadow;
	private static EnumValue<EnumDisplayAlignment>  client_location_screen_alignment;
	private static IntValue                         client_location_offset_x;
	private static IntValue                         client_location_offset_y;
	private static EnumValue<EnumDisplayAlignment>  client_location_text_alignment;
	private static DoubleValue                      client_location_width_ratio;
	private static IntValue                         client_location_width_min;
	
	// Chunk loading
	private static IntValue                         chunk_loader_max_energy_stored;
	private static IntValue                         chunk_loader_max_radius;
	private static IntValue                         chunk_loader_energy_per_chunk;
	
	// Cloaking
	private static IntValue                         cloaking_max_energy_stored;
	private static IntValue                         cloaking_coil_capture_blocks;
	private static IntValue                         cloaking_max_field_radius;
	private static IntValue                         cloaking_tier1_energy_per_block;
	private static IntValue                         cloaking_tier2_energy_per_block;
	private static IntValue                         cloaking_tier1_field_refresh_interval_ticks;
	private static IntValue                         cloaking_tier2_field_refresh_interval_ticks;
	private static IntValue                         cloaking_volume_scan_blocks_per_tick;
	private static IntValue                         cloaking_volume_scan_age_tolerance_seconds;
	
	// Enantiomorphic reactor
	private static ConfigValue<List<Integer>>       enantiomorphic_reactor_max_energy_stored_by_tier;
	private static ConfigValue<List<Integer>>       enantiomorphic_reactor_max_lasers_per_second_by_tier;
	private static ConfigValue<List<Integer>>       enantiomorphic_reactor_min_generation_FE_by_tier;
	private static ConfigValue<List<Integer>>       enantiomorphic_reactor_max_generation_FE_by_tier;
	
	// Force field
	private static ConfigValue<List<Integer>>       force_field_projector_max_energy_stored_by_tier;
	private static DoubleValue                      force_field_projector_explosion_scale;
	private static DoubleValue                      force_field_projector_max_laser_required;
	private static DoubleValue                      force_field_explosion_strength_vanilla_cap;
	
	// IC2 Reactor laser
	private static IntValue                         ic2_reactor_laser_max_heat_stored;
	private static IntValue                         ic2_reactor_laser_component_heat_transfer_per_tick;
	private static IntValue                         ic2_reactor_laser_focus_heat_transfer_per_tick;
	private static IntValue                         ic2_reactor_laser_reactor_heat_transfer_per_tick;
	private static IntValue                         ic2_reactor_laser_cooling_per_interval;
	private static DoubleValue                      ic2_reactor_laser_energy_per_heat;
	private static IntValue                         ic2_reactor_laser_cooling_interval_ticks;
	
	// Tooltip
	private static EnumValue<EnumTooltipCondition>  tooltip_add_registry_name;
	private static EnumValue<EnumTooltipCondition>  tooltip_add_ore_dictionary_name;
	private static EnumValue<EnumTooltipCondition>  tooltip_add_armor_points;
	private static EnumValue<EnumTooltipCondition>  tooltip_add_block_material;
	private static EnumValue<EnumTooltipCondition>  tooltip_add_burn_time;
	private static EnumValue<EnumTooltipCondition>  tooltip_add_durability;
	private static EnumValue<EnumTooltipCondition>  tooltip_add_enchantability;
	private static EnumValue<EnumTooltipCondition>  tooltip_add_entity_id;
	private static EnumValue<EnumTooltipCondition>  tooltip_add_flammability;
	private static EnumValue<EnumTooltipCondition>  tooltip_add_fluid_stats;
	private static EnumValue<EnumTooltipCondition>  tooltip_add_hardness;
	private static EnumValue<EnumTooltipCondition>  tooltip_add_harvesting_stats;
	private static EnumValue<EnumTooltipCondition>  tooltip_add_opacity;
	private static EnumValue<EnumTooltipCondition>  tooltip_add_repair_material;
	
	private static ConfigValue<List<String>>        tooltip_cleanup_list;
	
	private static EnumValue<EnumTooltipCondition>  tooltip_enable_deduplication;
	
	// Transporter
	private static IntValue                         transporter_max_energy_stored;
	
	// Tree farm
	private static IntValue                         tree_farm_max_mediums_count;
	private static IntValue                         tree_farm_max_radius_no_laser_medium;
	private static IntValue                         tree_farm_max_radius_per_laser_medium;
	private static IntValue                         tree_farm_max_reach_distance_no_laser_medium;
	private static IntValue                         tree_farm_max_reach_distance_per_laser_medium;
	
	// Energy handling
	private static ConfigValue<String>              energy_display_units;
	private static BooleanValue                     energy_enable_FE;
	private static BooleanValue                     energy_enable_GTCE_EU;
	private static BooleanValue                     energy_enable_IC2_EU;
	private static DoubleValue                      energy_overvoltage_shock_factor;
	private static DoubleValue                      energy_overvoltage_explosion_factor;
	private static IntValue                         energy_scan_interval_ticks;
	
	// Jump gate (common)
	private static ConfigValue<List<Integer>>       jump_gate_size_max_per_side_by_tier;
	
	// Laser medium
	private static ConfigValue<List<Integer>>       laser_medium_max_energy_stored_by_tier;
	private static ConfigValue<List<Double>>        laser_medium_bonus_factor_by_tier;
	
	// Laser cannon (common)
	private static IntValue                         laser_cannon_max_mediums_count;
	private static IntValue                         laser_cannon_max_laser_energy;
	private static DoubleValue                      laser_cannon_booster_beam_energy_efficiency;
	private static IntValue                         laser_cannon_range_max;
	
	// Laser cannon (server)
	private static IntValue                         laser_cannon_emit_fire_delay_ticks;
	private static IntValue                         laser_cannon_emit_scan_delay_ticks;
	private static DoubleValue                      laser_cannon_energy_attenuation_per_air_block;
	private static DoubleValue                      laser_cannon_energy_attenuation_per_void_block;
	private static DoubleValue                      laser_cannon_energy_attenuation_per_broken_block;
	private static IntValue                         laser_cannon_entity_hit_set_on_fire_seconds;
	private static IntValue                         laser_cannon_entity_hit_energy;
	private static IntValue                         laser_cannon_entity_hit_base_damage;
	private static IntValue                         laser_cannon_entity_hit_energy_per_damage;
	private static IntValue                         laser_cannon_entity_hit_max_damage;
	private static IntValue                         laser_cannon_entity_hit_energy_threshold_for_explosion;
	private static DoubleValue                      laser_cannon_entity_hit_explosion_base_strength;
	private static IntValue                         laser_cannon_entity_hit_explosion_energy_per_strength;
	private static DoubleValue                      laser_cannon_entity_hit_explosion_max_strength;
	private static IntValue                         laser_cannon_block_hit_energy_min;
	private static IntValue                         laser_cannon_block_hit_energy_per_block_hardness;
	private static IntValue                         laser_cannon_block_hit_energy_max;
	private static DoubleValue                      laser_cannon_block_hit_absorption_per_block_hardness;
	private static DoubleValue                      laser_cannon_block_hit_absorption_max;
	private static DoubleValue                      laser_cannon_block_hit_explosion_hardness_threshold;
	private static DoubleValue                      laser_cannon_block_hit_explosion_base_strength;
	private static IntValue                         laser_cannon_block_hit_explosion_energy_per_strength;
	private static DoubleValue                      laser_cannon_block_hit_explosion_max_strength;
	
	// Lift
	private static IntValue                         lift_max_energy_stored;
	private static IntValue                         lift_energy_per_entity;
	private static IntValue                         lift_update_interval_ticks;
	private static IntValue                         lift_entity_cooldown_ticks;
	
	// Logging
	private static LongValue                        logging_throttle_ms;
	
	private static BooleanValue                     logging_enable_accelerator_logs;
	private static BooleanValue                     logging_enable_break_place_logs;
	private static BooleanValue                     logging_enable_breathing_logs;
	private static BooleanValue                     logging_enable_building_logs;
	private static BooleanValue                     logging_enable_camera_logs;
	private static BooleanValue                     logging_enable_chunk_handler_logs;
	private static BooleanValue                     logging_enable_chunk_loading_logs;
	private static BooleanValue                     logging_enable_chunk_reloading_logs;
	private static BooleanValue                     logging_enable_collection_logs;
	private static BooleanValue                     logging_enable_dictionary_logs;
	private static BooleanValue                     logging_enable_energy_logs;
	private static BooleanValue                     logging_enable_entity_fx_logs;
	private static BooleanValue                     logging_enable_force_field_logs;
	private static BooleanValue                     logging_enable_force_field_registry_logs;
	private static BooleanValue                     logging_enable_global_region_registry_logs;
	private static BooleanValue                     logging_enable_gravity_logs;
	private static BooleanValue                     logging_enable_jump_logs;
	private static BooleanValue                     logging_enable_jumpblocks_logs;
	private static BooleanValue                     logging_enable_LUA_logs;
	private static BooleanValue                     logging_enable_offline_avatar_logs;
	private static BooleanValue                     logging_enable_profiling_CPU_time;
	private static BooleanValue                     logging_enable_profiling_memory_allocation;
	private static BooleanValue                     logging_enable_profiling_thread_safety;
	private static BooleanValue                     logging_enable_radar_logs;
	private static BooleanValue                     logging_enable_rendering_logs;
	private static BooleanValue                     logging_enable_transporter_logs;
	private static BooleanValue                     logging_enable_XML_preprocessor_logs;
	private static BooleanValue                     logging_enable_weapon_logs;
	private static BooleanValue                     logging_enable_world_generation_logs;
	
	private static BooleanValue                     logging_enable_client_synchronization_logs;
	private static BooleanValue                     logging_enable_cloaking_logs;
	private static BooleanValue                     logging_enable_effects_logs;
	private static BooleanValue                     logging_enable_targeting_logs;
	private static BooleanValue                     logging_enable_videoChannel_logs;
	
	// Mining Laser (common)
	private static IntValue                         mining_laser_max_mediums_count;
	private static IntValue                         mining_laser_radius_no_laser_medium;
	private static IntValue                         mining_laser_radius_per_laser_medium;
	
	// Mining Laser (server)
	private static IntValue                         mining_laser_warmup_delay_ticks;
	private static IntValue                         mining_laser_scan_delay_ticks;
	private static IntValue                         mining_laser_mine_delay_ticks;
	private static IntValue                         mining_laser_scan_energy_per_layer_in_atmosphere;
	private static IntValue                         mining_laser_mine_energy_per_block_in_atmosphere;
	private static IntValue                         mining_laser_scan_energy_per_layer_in_void;
	private static IntValue                         mining_laser_mine_energy_per_block_in_void;
	private static DoubleValue                      mining_laser_mine_ores_only_energy_factor;
	private static DoubleValue                      mining_laser_mine_silktouch_energy_factor;
	private static IntValue                         mining_laser_mine_silktouch_deuterium_mB;
	private static DoubleValue                      mining_laser_fortune_energy_factor;
	
	// Offline avatar
	private static BooleanValue                     offline_avatar_enable;
	private static BooleanValue                     offline_avatar_create_only_aboard_ships;
	private static BooleanValue                     offline_avatar_forget_on_death;
	private static DoubleValue                      offline_avatar_model_scale;
	private static BooleanValue                     offline_avatar_always_render_name_tag;
	private static DoubleValue                      offline_avatar_min_range_for_removal;
	private static DoubleValue                      offline_avatar_max_range_for_removal;
	private static IntValue                         offline_avatar_delay_for_removal_s;
	
	// Radar
	private static IntValue                         radar_max_energy_stored;
	private static IntValue                         radar_energy_cost_min;
	private static ConfigValue<List<Double>>        radar_energy_cost_factors;
	private static IntValue                         radar_scan_min_delay_seconds;
	private static ConfigValue<List<Double>>        radar_scan_delay_factors_seconds;
	private static IntValue                         radar_max_isolation_range;
	private static IntValue                         radar_min_isolation_blocks;
	private static IntValue                         radar_max_isolation_blocks;
	private static DoubleValue                      radar_min_isolation_effect;
	private static DoubleValue                      radar_max_isolation_effect;
	
	// Ship (common)
	private static ConfigValue<List<Integer>>       ship_max_energy_stored_by_tier;
	private static ConfigValue<List<Integer>>       ship_mass_max_by_tier;
	private static ConfigValue<List<Integer>>       ship_mass_min_by_tier;
	private static IntValue                         ship_mass_max_on_planet_surface;
	private static IntValue                         ship_mass_min_for_hyperspace;
	private static ConfigValue<List<Integer>>       ship_size_max_per_side_by_tier;
	
	// Ship (server)
	private static IntValue                         ship_collision_tolerance_blocks;
	private static ConfigValue<List<String>>        ship_mass_unlimited_player_names;
	private static IntValue                         ship_volume_scan_blocks_per_tick;
	private static IntValue                         ship_volume_scan_age_tolerance;
	private static IntValue                         ship_warmup_random_ticks;
	
	// Ship scanner
	private static IntValue                         ship_scanner_max_deploy_radius_blocks;
	private static IntValue                         ship_scanner_search_interval_ticks;
	private static IntValue                         ship_scanner_scan_blocks_per_second;
	private static IntValue                         ship_scanner_deploy_blocks_per_interval;
	private static IntValue                         ship_scanner_deploy_interval_ticks;
	
	// General
	public static int                  G_SPACE_PROVIDER_ID = 14;
	public static int                  G_HYPERSPACE_PROVIDER_ID = 15;
	
	public static EnumLUAscripts       G_LUA_SCRIPTS = EnumLUAscripts.ALL;
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
	public static String           ENERGY_DISPLAY_UNITS = "FE";
	public static boolean          ENERGY_ENABLE_IC2_EU = true;
	public static boolean          ENERGY_ENABLE_FE = true;
	public static boolean          ENERGY_ENABLE_GTCE_EU = true;
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
	
	// Ship (common)
	public static int[]            SHIP_MAX_ENERGY_STORED_BY_TIER = { 0, 500000, 10000000, 100000000 };
	public static int[]            SHIP_MASS_MAX_BY_TIER = { 2000000, 3456, 13824, 110592 };
	public static int[]            SHIP_MASS_MIN_BY_TIER = {       0,   64,  1728,   6912 };
	public static int              SHIP_MASS_MAX_ON_PLANET_SURFACE = 3000;
	public static int              SHIP_MASS_MIN_FOR_HYPERSPACE = 4000;
	public static int[]            SHIP_SIZE_MAX_PER_SIDE_BY_TIER = { 127, 24, 48, 96 };
	
	// Ship (server)
	public static int              SHIP_COLLISION_TOLERANCE_BLOCKS = 3;
	public static String[]         SHIP_MASS_UNLIMITED_PLAYER_NAMES = { "notch", "someone" };
	public static int              SHIP_VOLUME_SCAN_BLOCKS_PER_TICK = 1000;
	public static int              SHIP_VOLUME_SCAN_AGE_TOLERANCE_SECONDS = 120;
	public static int              SHIP_WARMUP_RANDOM_TICKS = 60;
	
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
	public static int              RADAR_MAX_ENERGY_STORED;
	public static int              RADAR_SCAN_MIN_ENERGY_COST;
	public static double[]         RADAR_SCAN_ENERGY_COST_FACTORS;
	public static int              RADAR_SCAN_MIN_DELAY_SECONDS;
	public static double[]         RADAR_SCAN_DELAY_FACTORS_SECONDS;
	public static int              RADAR_MAX_ISOLATION_RANGE;
	public static int              RADAR_MIN_ISOLATION_BLOCKS;
	public static int              RADAR_MAX_ISOLATION_BLOCKS;
	public static double           RADAR_MIN_ISOLATION_EFFECT;
	public static double           RADAR_MAX_ISOLATION_EFFECT;
	
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
	public static double[]         LASER_MEDIUM_BONUS_FACTOR_BY_TIER = { 1.25D, 0.5D, 1.0D, 1.5D };
	
	// Laser cannon
	// 1 main laser + 4 boosting lasers = 10 * 100k + 0.6 * 40 * 100k = 3.4M
	public static int              LASER_CANNON_MAX_MEDIUMS_COUNT;
	public static int              LASER_CANNON_MAX_LASER_ENERGY;
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
	public static int[]            ENAN_REACTOR_GENERATION_MIN_FE_BY_TIER = { 4, 4, 4, 4 };
	public static int[]            ENAN_REACTOR_GENERATION_MAX_FE_BY_TIER = { 64000, 64000, 192000, 576000 };
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
	public static int              CHUNK_LOADER_MAX_ENERGY_STORED;
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
	
	// note: we use an explicit static constructor so it override default values eventually set before
	static {
		CLIENT_CONFIG = buildClientConfig();
		COMMON_CONFIG = buildCommonConfig();
		SERVER_CONFIG = buildServerConfig();
	}
	
	@Nonnull
	public static Block getBlockOrFire(@Nonnull final String registryName) {
		final ResourceLocation resourceLocation = new ResourceLocation(registryName);
		final Block block = ForgeRegistries.BLOCKS.getValue(resourceLocation);
		if (block == Blocks.AIR || block == null) {
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
			final CompoundNBT tagCompound = JsonToNBT.getTagFromJson(stringNBT);
			itemStack.setTag(tagCompound);
		} catch (final CommandSyntaxException exception) {
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
	private static Object getOreOrItemStackOrNull(@Nonnull final String registryName, final int damage) {
		assert registryName.contains(":");
		
		final ResourceLocation resourceLocation = new ResourceLocation(registryName);
		final Tag<Item> itemTagCollection = ItemTags.getCollection().get(resourceLocation);
		if (itemTagCollection != null && !itemTagCollection.getAllElements().isEmpty()) {
			return resourceLocation;
		}
		if (registryName.startsWith("forge:")) {
			WarpDrive.logger.info(String.format("Skipping missing ore dictionary entry %s",
			                                    resourceLocation ));
			return null;
		}
		
		final Item item = ForgeRegistries.ITEMS.getValue(resourceLocation);
		if (item == Items.AIR) {
			WarpDrive.logger.info(String.format("Skipping missing mod item %s@%d",
			                                    registryName, damage ));
			return null;
		}
		final ItemStack itemStack;
		try {
			itemStack = new ItemStack(item);
			if (damage > 0) {
				itemStack.setDamage(damage);
				if (itemStack.getDamage() != damage) {
					throw new RuntimeException(String.format("Invalid damage value found %d, expected %d",
					                                         itemStack.getDamage(), damage ));
				}
			}
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
			WarpDrive.logger.error(String.format("Failed to get mod item for %s@%d",
			                                     registryName, damage ));
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
		final ResourceLocation tag = new ResourceLocation(ore);
		final Tag<Item> itemTagCollection = ItemTags.getCollection().get(tag);
		if (itemTagCollection == null) {
			WarpDrive.logger.info(String.format("Skipping missing ore named %s",
			                                    ore));
			return ItemStack.EMPTY;
		}
		if (itemTagCollection.getAllElements().isEmpty()) {
			WarpDrive.logger.error(String.format("Failed to get item from empty ore dictionary %s",
			                                     ore));
			return ItemStack.EMPTY;
		}
		return new ItemStack(itemTagCollection.getAllElements().iterator().next());
	}
	
	public static void reload(@Nonnull final MinecraftServer server) {
		CelestialObjectManager.clearForReload(false);
		onConstructionOrReloading();
		onFMLCommonSetup();
		
		final List<ServerPlayerEntity> entityPlayers = server.getPlayerList().getPlayers();
		for (final ServerPlayerEntity entityServerPlayer : entityPlayers) {
			if ( !(entityServerPlayer instanceof FakePlayer) ) {
				final CelestialObject celestialObject = CelestialObjectManager.get(entityServerPlayer.world);
				PacketHandler.sendClientSync(entityServerPlayer, celestialObject);
			}
		}
	}
	
	public static void onModConstruction(final String stringConfigDirectory) {
		fileConfigDirectory = new File(stringConfigDirectory, WarpDrive.MODID);
		
		// register configuration files
		ModLoadingContext.get().registerConfig(Type.CLIENT, CLIENT_CONFIG);
		ModLoadingContext.get().registerConfig(Type.COMMON, COMMON_CONFIG);
		ModLoadingContext.get().registerConfig(Type.SERVER, SERVER_CONFIG);
		
		// read mod dependencies at runtime and for recipes
		isComputerCraftLoaded = ModList.get().isLoaded("computercraft");
		isEnderIOLoaded = ModList.get().isLoaded("enderio");
		isGregtechLoaded = ModList.get().isLoaded("gregtech");
		isIndustrialCraft2Loaded = ModList.get().isLoaded("ic2");
		isOpenComputersLoaded = ModList.get().isLoaded("opencomputers");
		
		// read mod dependencies for recipes
		isForgeMultipartLoaded = ModList.get().isLoaded("forgemultipartcbe");
		isICBMClassicLoaded = ModList.get().isLoaded("icbmclassic");
		isMatterOverdriveLoaded = ModList.get().isLoaded("matteroverdrive");
		isNotEnoughItemsLoaded = ModList.get().isLoaded("NotEnoughItems");
		isThermalExpansionLoaded = ModList.get().isLoaded("thermalexpansion");
		isThermalFoundationLoaded = ModList.get().isLoaded("thermalfoundation");
		
		onConstructionOrReloading();
	}
	
	public static void onConstructionOrReloading() {
		// create mod folder
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
	}
	
	public static ForgeConfigSpec buildClientConfig() {
		final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		builder.comment("This is the client configuration for WarpDrive")
		       .push("overlays");
		
		client_breathing_overlay_forced = builder
				.comment("Force rendering the breathing overlay to compensate HUD modifications")
				.translation("warpdrive.config.client.breathing_overlay_forced")
				.define("breathing_overlay_forced", true);
		client_location_scale = builder
				.comment("Scale for location text font")
				.translation("warpdrive.config.client.location_scale")
				.defineInRange("location_scale", 1.0F, 0.25F, 4.0F);
		client_location_name_prefix = builder
				.comment("Prefix for location name, useful to add formatting")
				.translation("warpdrive.config.client.location_name_prefix")
				.define("location_name_prefix", "§l");
		client_location_background_color = builder
		        .comment("Hexadecimal color code for location background (0xAARRGGBB where AA is alpha, RR is Red, GG is Green and BB is Blue component)")
		        .translation("warpdrive.config.client.location_background_color")
		        .define("location_background_color", String.format("0x%6X", Commons.colorARGBtoInt(64, 48, 48, 48)));
		client_location_text_color = builder
				.comment("Hexadecimal color code for location foreground (0xAARRGGBB where AA is alpha, RR is Red, GG is Green and BB is Blue component)")
				.translation("warpdrive.config.client.location_text_color")
				.define("location_text_color", String.format("0x%6X", Commons.colorARGBtoInt(230, 180, 180, 240)));
		client_location_has_shadow = builder
				.comment("Shadow casting option for current celestial object name")
				.translation("warpdrive.config.client.location_has_shadow")
				.define("location_has_shadow", true);
		client_location_screen_alignment = builder
				.comment("Alignment on screen: TOP_LEFT, TOP_CENTER, TOP_RIGHT, MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER or BOTTOM_RIGHT")
				.translation("warpdrive.config.client.location_screen_alignment")
				.defineEnum("location_screen_alignment", EnumDisplayAlignment.MIDDLE_RIGHT);
		client_location_offset_x = builder
				.comment("Horizontal offset on screen, increase to move to the right")
				.translation("warpdrive.config.client.location_offset_x")
				.defineInRange("location_offset_x", 0, -32768, 32767);
		client_location_offset_y = builder
				.comment("Vertical offset on screen, increase to move down")
				.translation("warpdrive.config.client.location_offset_y")
				.defineInRange("location_offset_y", 0, -32768, 32767);
		client_location_text_alignment = builder
				.comment("Text alignment: TOP_LEFT, TOP_CENTER, TOP_RIGHT, MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER or BOTTOM_RIGHT")
				.translation("warpdrive.config.client.location_text_alignment")
				.defineEnum("location_text_alignment", EnumDisplayAlignment.TOP_RIGHT);
		client_location_width_ratio = builder
				.comment("Text width as a ratio of full screen width")
				.translation("warpdrive.config.client.location_width_ratio")
				.defineInRange("location_width_ratio", 0.0F, 0.0F, 1.0F);
		client_location_width_min = builder
				.comment("Text width as a minimum 'pixel' count")
				.translation("warpdrive.config.client.location_width_min")
				.defineInRange("location_width_min", 90, 0, 32767);
		
		builder.pop();
		
		return builder.build();
	}
	
	public static ForgeConfigSpec buildCommonConfig() {
		final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		builder.comment("This is the general common configuration for WarpDrive")
		       .push("general");
		
		general_space_provider_id = builder
				.comment("Space dimension provider ID")
				.translation("warpdrive.config.general.space_provider_id")
				.defineInRange("space_provider_id", 14, Integer.MIN_VALUE, Integer.MAX_VALUE);
		general_hyperspace_provider_id = builder
				.comment("Hyperspace dimension provider ID")
				.translation("warpdrive.config.general.hyperspace_provider_id")
				.defineInRange("hyperspace_provider_id", 15, Integer.MIN_VALUE, Integer.MAX_VALUE);
		
		general_lua_scripts = builder
				.comment("LUA scripts to load when connecting machines: 0 = none, 1 = templates in a subfolder, 2 = ready to roll (templates are still provided)")
				.translation("warpdrive.config.general.lua_scripts")
				.defineEnum("lua_scripts", EnumLUAscripts.ALL);
		
		general_schematics_location = builder
		        .comment("Root folder where to load and save ship schematics")
		        .translation("warpdrive.config.general.schematics_location")
		        .define("schematics_location", "warpDrive_schematics");
		
		general_assembly_scanning_interval = builder
				.comment("Reaction delay when updating blocks in an assembly (measured in seconds)")
				.translation("warpdrive.config.general.assembly_scanning_interval")
				.defineInRange("assembly_scanning_interval", 10, 0, 300);
		general_parameters_update_interval = builder
		        .comment("Complex computation delay in an assembly (measured in ticks)")
		        .translation("warpdrive.config.general.parameters_update_interval")
		        .defineInRange("parameters_update_interval", 20, 0, 300);
		general_registry_update_interval = builder
				.comment("Registration period for an assembly (measured in seconds)")
				.translation("warpdrive.config.general.registry_update_interval")
				.defineInRange("registry_update_interval", 10, 0, 300);
		general_enforce_valid_celestial_objects = builder
		        .comment("Disable to boot the game even when celestial objects are invalid. Use at your own risk!")
		        .translation("warpdrive.config.general.enforce_valid_celestial_objects")
		        .define("enforce_valid_celestial_objects", true);
		
		general_blocks_per_tick = builder
		        .comment("Number of blocks to move per ticks, too high will cause lag spikes on ship jumping or deployment, too low may break the ship wirings")
		        .translation("warpdrive.config.general.blocks_per_tick")
		        .defineInRange("blocks_per_tick", 3500, 100, 100000);
		general_enable_fast_set_blockstate = builder
		        .comment("Enable fast blockstate placement, skipping light computation. Disable if you have world implementations conflicts")
		        .translation("warpdrive.config.general.enable_fast_set_blockstate")
		        .define("enable_fast_set_blockstate", false);
		general_enable_protection_checks = builder
				.comment("Enable area protection checks from other mods or plugins, disable if you use the event system exclusively")
				.translation("warpdrive.config.general.enable_protection_checks")
				.define("enable_protection_checks", true);
		general_enable_experimental_refresh = builder
		        .comment("Enable experimental refresh during jump to prevent duping, use at your own risk")
		        .translation("warpdrive.config.general.enable_experimental_refresh")
		        .define("enable_experimental_refresh", false);
		
		general_blast_resistance_cap = builder
				.comment("Maximum allowed blast resistance for non-hull, breakable blocks from other mods. Required to fix non-sense scaling in modded fluids, etc. Default is basic hull resistance (60).")
				.translation("warpdrive.config.general.blast_resistance_cap")
				.defineInRange("blast_resistance_cap", 60.0F, 10.0F, 6000.0F);
		
		builder.pop();
		
		// Atomic properties
		builder.comment("Atomic properties")
		       .push("atomic");
		
		atomic_max_particle_bunches = builder
				.comment("Maximum number of particle bunches per accelerator controller.")
				.translation("warpdrive.config.atomic.max_particle_bunches")
				.defineInRange("max_particle_bunches", 20, 2, 100);
		
		builder.pop();
		
		// Breathing (common)
		builder.comment("Air generator & general breathing properties")
		       .push("breathing");
		
		breathing_max_energy_stored_by_tier = builder
				.comment("Maximum energy stored for a given tier.")
				.translation("warpdrive.config.breathing.max_energy_stored_by_tier")
				.define("max_energy_stored_by_tier", Arrays.asList(0, 1400, 21000, 304500));
		breathing_energy_per_canister = builder
				.comment("Energy cost per air canister refilled.")
				.translation("warpdrive.config.breathing.energy_per_canister")
				.defineInRange("energy_per_canister", 200, 1, 1400);
		breathing_energy_per_new_air_block_by_tier = builder
				.comment("Energy cost to start air distribution per open side per interval for a given tier.")
				.translation("warpdrive.config.breathing.energy_per_new_air_block_by_tier")
				.define("energy_per_new_air_block_by_tier", Arrays.asList(0, 12, 180, 2610));
		breathing_energy_per_existing_air_block_by_tier = builder
				.comment("Energy cost to sustain air distribution per open side per interval for a given tier.")
				.translation("warpdrive.config.breathing.energy_per_existing_air_block_by_tier")
				.define("energy_per_existing_air_block_by_tier", Arrays.asList(0, 4, 60, 870));
		breathing_air_generation_range_blocks = builder
				.comment("Maximum range of an air generator for each tier, measured in block (8 to 255).")
				.translation("warpdrive.config.breathing.air_generation_range_blocks")
				.define("air_generation_range_blocks", Arrays.asList(200, 16, 48, 144));
		
		builder.pop();
		
		// Subspace capacitor
		builder.comment("Capacitor properties")
		       .push("capacitor");
		
		capacitor_max_energy_stored_by_tier = builder
				.comment("Maximum energy stored for each subspace capacitor tier.")
				.translation("warpdrive.config.capacitor.max_energy_stored_by_tier")
				.define("max_energy_stored_by_tier", Arrays.asList(20000000, 800000, 4000000, 20000000));
		capacitor_ic2_sink_tier_name_by_tier = builder
				.comment("IC2 energy sink tier (ULV, LV, MV, HV, EV, IV, LuV, ZPMV, UV, MaxV) for each subspace capacitor tier.")
				.translation("warpdrive.config.capacitor.ic2_sink_tier_name_by_tier")
				.define("ic2_sink_tier_name_by_tier", Arrays.asList("MaxV", "MV", "HV", "EV"));
		capacitor_ic2_source_tier_name_by_tier = builder
				.comment("IC2 energy source tier (ULV, LV, MV, HV, EV, IV, LuV, ZPMV, UV, MaxV) for each subspace capacitor tier.")
				.translation("warpdrive.config.capacitor.ic2_source_tier_name_by_tier")
				.define("ic2_source_tier_name_by_tier", Arrays.asList("MaxV", "MV", "HV", "EV"));
		capacitor_flux_rate_input_per_tick_by_tier = builder
				.comment("Flux energy transferred per tick for each subspace capacitor tier.")
				.translation("warpdrive.config.capacitor.flux_rate_input_per_tick_by_tier")
				.define("flux_rate_input_per_tick_by_tier", Arrays.asList(Integer.MAX_VALUE / 2, 800, 4000, 20000));
		capacitor_flux_rate_output_per_tick_by_tier = builder
				.comment("Flux energy transferred per tick for each subspace capacitor tier.")
				.translation("warpdrive.config.capacitor.flux_rate_output_per_tick_by_tier")
				.define("flux_rate_output_per_tick_by_tier", Arrays.asList(Integer.MAX_VALUE / 2, 800, 4000, 20000));
		capacitor_efficiency_per_upgrade = builder
				.comment("Energy transfer efficiency for each upgrade apply, first value is without upgrades (0.8 means 20% loss).")
				.translation("warpdrive.config.capacitor.efficiency_per_upgrade")
				.define("efficiency_per_upgrade", Arrays.asList(0.95D, 0.98D, 1.0D));
		
		builder.pop();
		
		// Chunk loader
		builder.comment("Chunk loader properties")
		       .push("chunk_loader");
		
		chunk_loader_max_energy_stored = builder
				.comment("Maximum energy stored.")
				.translation("warpdrive.config.chunk_loader.max_energy_stored")
				.defineInRange("max_energy_stored", 1000000, 1, Integer.MAX_VALUE);
		chunk_loader_max_radius = builder
				.comment("Maximum radius when loading a square shape, measured in chunks. A linear shape can be up to 1 chunk wide by (radius + 1 + radius) ^ 2 chunks long.")
				.translation("warpdrive.config.chunk_loader.max_radius")
				.defineInRange("max_radius", 2, 1, 1000);
		chunk_loader_energy_per_chunk = builder
				.comment("Energy consumed per chunk loaded.")
				.translation("warpdrive.config.chunk_loader.energy_per_chunk")
				.defineInRange("energy_per_chunk", 8, 1, 100);
		
		builder.pop();
		
		// Cloaking
		builder.comment("Cloaking properties")
		       .push("cloaking");
		
		cloaking_max_energy_stored = builder
				.comment("Maximum energy stored.")
				.translation("warpdrive.config.cloaking.max_energy_stored")
				.defineInRange("max_energy_stored", 200, 1, Integer.MAX_VALUE);
		cloaking_coil_capture_blocks = builder
				.comment("Extra blocks covered after the outer coils.")
				.translation("warpdrive.config.cloaking.coil_capture_blocks")
				.defineInRange("coil_capture_blocks", 5, 0, 30);
		cloaking_max_field_radius = builder
				.comment("Maximum distance between cloaking core and any cloaked side.")
				.translation("warpdrive.config.cloaking.max_field_radius")
				.defineInRange("max_field_radius", 63, 3, 128);
		cloaking_tier1_energy_per_block = builder
				.comment("Energy cost per non-air block in a Tier1 cloak.")
				.translation("warpdrive.config.cloaking.tier1_energy_per_block")
				.defineInRange("tier1_energy_per_block", 32, 0, Integer.MAX_VALUE);
		cloaking_tier2_energy_per_block = builder
				.comment("Energy cost per block in a Tier2 cloak.")
				.translation("warpdrive.config.cloaking.tier2_energy_per_block")
				.defineInRange("tier2_energy_per_block", 128, 0, Integer.MAX_VALUE);
		cloaking_tier1_field_refresh_interval_ticks = builder
				.comment("Update speed of a Tier1 cloak.")
				.translation("warpdrive.config.cloaking.tier1_field_refresh_interval_seconds")
				.defineInRange("tier1_field_refresh_interval_seconds", 60, 20, 600);
		cloaking_tier2_field_refresh_interval_ticks = builder
				.comment("Update speed of a Tier2 cloak.")
				.translation("warpdrive.config.cloaking.tier2_field_refresh_interval_seconds")
				.defineInRange("tier2_field_refresh_interval_seconds", 30, 20, 600);
		cloaking_volume_scan_blocks_per_tick = builder
				.comment("Number of blocks to scan per tick when getting cloak bounds, too high will cause lag spikes when resizing a cloak.")
				.translation("warpdrive.config.cloaking.volume_scan_blocks_per_tick")
				.defineInRange("volume_scan_blocks_per_tick", 1000, 100, 100000);
		cloaking_volume_scan_age_tolerance_seconds = builder
				.comment("Cloak volume won't be refreshed unless it's older than that many seconds.")
				.translation("warpdrive.config.cloaking.volume_scan_age_tolerance_seconds")
				.defineInRange("volume_scan_age_tolerance_seconds", 120, 0, 300);
		
		builder.pop();
		
		// Enantiomorphic reactor
		builder.comment("Enantiomorphic reactor properties")
		       .push("enantiomorphic_reactor");
		
		enantiomorphic_reactor_max_energy_stored_by_tier = builder
				.comment("Maximum energy stored in the core for a given tier.")
				.translation("warpdrive.config.enantiomorphic_reactor.max_energy_stored_by_tier")
				.define("max_energy_stored_by_tier", Arrays.asList(100000000, 100000000, 500000000, 2000000000));
		enantiomorphic_reactor_max_lasers_per_second_by_tier = builder
				.comment("Maximum number of stabilisation laser shots per seconds before loosing efficiency.")
				.translation("warpdrive.config.enantiomorphic_reactor.max_lasers_per_second_by_tier")
				.define("max_lasers_per_second_by_tier", Arrays.asList(64, 6, 12, 24));
		enantiomorphic_reactor_min_generation_FE_by_tier = builder
				.comment("Minimum energy added to the core when enabled, measured in FE/t, for a given tier.")
				.translation("warpdrive.config.enantiomorphic_reactor.min_generation_FE_by_tier")
				.define("min_generation_FE_by_tier", Arrays.asList(4, 4, 4, 4));
		enantiomorphic_reactor_max_generation_FE_by_tier = builder
				.comment("Maximum energy added to the core when enabled, measured in FE/t, for a given tier.")
				.translation("warpdrive.config.enantiomorphic_reactor.max_generation_FE_by_tier")
				.define("max_generation_FE_by_tier", Arrays.asList(64000, 64000, 192000, 576000));
		
		builder.pop();
		
		// Force field
		builder.comment("Force field properties")
		       .push("force_field");
		
		 force_field_projector_max_energy_stored_by_tier = builder
				.comment("Maximum energy stored for each projector tier.")
				.translation("warpdrive.config.force_field.projector_max_energy_stored_by_tier")
				.define("projector_max_energy_stored_by_tier", Arrays.asList(20000000, 30000, 90000, 150000)); // 30000 * (1 + 2 * tier)
		force_field_projector_explosion_scale = builder
				.comment("Scale applied to explosion strength, increase the value to reduce explosion impact on a force field. Enable weapon logs to see the damage level.")
				.translation("warpdrive.config.force_field.projector_explosion_scale")
				.defineInRange("projector_explosion_scale", 1000.0D, 1.0D, 1000.0D);
		force_field_projector_max_laser_required = builder
				.comment("Number of maxed out laser cannons required to break a superior force field.")
				.translation("warpdrive.config.force_field.projector_max_laser_required")
				.defineInRange("projector_max_laser_required", 10.0D, 1.0D, 1000.0D);
		force_field_explosion_strength_vanilla_cap = builder
				.comment("Maximum strength for vanilla explosion object used by simple explosives like TechGuns rockets.")
				.translation("warpdrive.config.force_field.explosion_strength_vanilla_cap")
				.defineInRange("explosion_strength_vanilla_cap", 15.0D, 3.0D, 1000.0D);
		
		builder.pop();
		
		// IC2 Reactor cooler
		builder.comment("IC2 Reactor cooler laser properties")
		       .push("ic2_reactor_laser");
		
		ic2_reactor_laser_max_heat_stored = builder
				.comment("Maximum heat stored in the focus.")
				.translation("warpdrive.config.ic2_reactor_laser.max_heat_stored")
				.defineInRange("max_heat_stored", 30000, 1, 32767);
		ic2_reactor_laser_component_heat_transfer_per_tick = builder
				.comment("Maximum component heat added to the focus every reactor tick.")
				.translation("warpdrive.config.ic2_reactor_laser.component_heat_transfer_per_tick")
				.defineInRange("component_heat_transfer_per_tick", 54, 0, 32767);
		ic2_reactor_laser_focus_heat_transfer_per_tick = builder
				.comment("Maximum heat transferred between 2 connected focus every reactor tick.")
				.translation("warpdrive.config.ic2_reactor_laser.focus_heat_transfer_per_tick")
				.defineInRange("focus_heat_transfer_per_tick", 648, 0, 32767);
		ic2_reactor_laser_reactor_heat_transfer_per_tick = builder
				.comment("Maximum reactor heat added to the focus every reactor tick.")
				.translation("warpdrive.config.ic2_reactor_laser.reactor_heat_transfer_per_tick")
				.defineInRange("reactor_heat_transfer_per_tick", 54, 0, 32767);
		ic2_reactor_laser_cooling_per_interval = builder
				.comment("Heat extracted from the focus by interval.")
				.translation("warpdrive.config.ic2_reactor_laser.cooling_per_interval")
				.defineInRange("cooling_per_interval", 1080, 1, 32767);
		ic2_reactor_laser_energy_per_heat = builder
				.comment("Energy cost per heat absorbed.")
				.translation("warpdrive.config.ic2_reactor_laser.energy_per_heat")
				.defineInRange("energy_per_heat", 2.0D, 2.0D, 100000.0D);
		ic2_reactor_laser_cooling_interval_ticks = builder
				.comment("Update speed of the check for reactors to cool down. Use 10 to tick as fast as the reactor simulation.")
				.translation("warpdrive.config.ic2_reactor_laser.cooling_interval_ticks")
				.defineInRange("cooling_interval_ticks", 10, 0, 1200);
		
		builder.pop();
		
		// Jump gate
		builder.comment("This is the tooltip common configuration for WarpDrive")
		       .push("jump_gate");
		
		jump_gate_size_max_per_side_by_tier = builder
				.comment("Maximum jump gate size on each axis in blocks, for a given tier.")
				.translation("warpdrive.config.jump_gate.size_max_per_side_by_tier")
				.define("size_max_per_side_by_tier", Arrays.asList(127, 32, 64, 127));
		
		builder.pop();
		
		// Laser medium
		builder.comment("Laser medium properties")
		       .push("laser_medium");
		
		laser_medium_max_energy_stored_by_tier = builder
				.comment("Maximum energy stored for a given tier.")
				.translation("warpdrive.config.laser_medium.max_energy_stored_by_tier")
				.define("max_energy_stored_by_tier", Arrays.asList(1000000, 10000, 30000, 100000));
		laser_medium_bonus_factor_by_tier = builder
				.comment("Bonus multiplier of a laser medium line for a given tier.")
				.translation("warpdrive.config.laser_medium.bonus_factor_by_tier")
				.define("bonus_factor_by_tier", Arrays.asList(1.25D, 0.5D, 1.0D, 1.5D));
		
		builder.pop();
		
		// Laser cannon (common)
		builder.comment("Laser cannon properties")
		       .push("laser_cannon");
		
		laser_cannon_max_mediums_count = builder
				.comment("Maximum number of laser mediums per laser.")
				.translation("warpdrive.config.laser_cannon.max_mediums_count")
				.defineInRange("max_mediums_count", 10, 1, 64);
		laser_cannon_max_laser_energy = builder
				.comment("Maximum energy in beam after accounting for boosters beams.")
				.translation("warpdrive.config.laser_cannon.max_laser_energy")
				.defineInRange("max_laser_energy", 3400000, 1, Integer.MAX_VALUE);
		laser_cannon_booster_beam_energy_efficiency = builder
				.comment("Energy factor applied from boosting to main laser.")
				.translation("warpdrive.config.laser_cannon.booster_beam_energy_efficiency")
				.defineInRange("booster_beam_energy_efficiency", 0.60D, 0.01D, 10.0D);
		laser_cannon_range_max = builder
				.comment("Maximum distance travelled.")
				.translation("warpdrive.config.laser_cannon.range_max")
				.defineInRange("range_max", 500, 64, 512);
		
		builder.pop();
		
		// Lift
		builder.comment("Lift properties")
		       .push("lift");
		
		lift_max_energy_stored = builder
				.comment("Maximum energy stored.")
				.translation("warpdrive.config.lift.max_energy_stored")
				.defineInRange("max_energy_stored", 900, 1, Integer.MAX_VALUE);
		lift_energy_per_entity = builder
				.comment("Energy consumed per entity moved.")
				.translation("warpdrive.config.lift.energy_per_entity")
				.defineInRange("energy_per_entity", 150, 1, Integer.MAX_VALUE);
		lift_update_interval_ticks = builder
				.comment("Update speed of the check for entities.")
				.translation("warpdrive.config.lift.update_interval_ticks")
				.defineInRange("update_interval_ticks", 10, 1, 60);
		lift_entity_cooldown_ticks = builder
				.comment("Cooldown after moving an entity.")
				.translation("warpdrive.config.lift.entity_cooldown_ticks")
				.defineInRange("entity_cooldown_ticks", 40, 1, 6000);
		
		builder.pop();
		
		// Mining Laser (common)
		builder.comment("Mining laser properties")
		       .push("mining_laser");
		
		mining_laser_max_mediums_count = builder
		        .comment("Maximum number of laser mediums.")
		        .translation("warpdrive.config.mining_laser.max_mediums_count")
		        .defineInRange("max_mediums_count", 500, 1, 10);
		mining_laser_radius_no_laser_medium = builder
		        .comment("Mining radius without any laser medium, measured in blocks.")
		        .translation("warpdrive.config.mining_laser.radius_no_laser_medium")
		        .defineInRange("radius_no_laser_medium", 4, 0, 15);
		mining_laser_radius_per_laser_medium = builder
				.comment("Bonus to mining radius per laser medium, measured in blocks.")
				.translation("warpdrive.config.mining_laser.radius_per_laser_medium")
				.defineInRange("radius_per_laser_medium", 1, 1, 8);
		
		builder.pop();
		
		// Radar
		builder.comment("Radar properties")
		       .push("radar");
		
		radar_max_energy_stored = builder
				.comment("Maximum energy stored.")
				.translation("warpdrive.config.radar.max_energy_stored")
				.defineInRange("max_energy_stored", 100000000, 0, Integer.MAX_VALUE);
		radar_energy_cost_min = builder
				.comment("Minimum energy cost per scan (0+), independently of radius.")
				.translation("warpdrive.config.radar.energy_cost_min")
				.defineInRange("energy_cost_min", 10000, 0, Integer.MAX_VALUE);
		radar_energy_cost_factors = builder
				.comment("Energy cost factors {a, b, c, d}. You need to provide exactly 4 values.\n"
				+ "The equation used is a + b * radius + c * radius^2 + d * radius^3.")
				.translation("warpdrive.config.radar.energy_cost_factors")
				.define("energy_cost_factors", Arrays.asList(0.0D, 0.0D, 0.0D, 0.0001D));
		radar_scan_min_delay_seconds = builder
				.comment("Minimum scan delay per scan (1+), (measured in seconds).")
				.translation("warpdrive.config.radar.scan_min_delay_seconds")
				.defineInRange("scan_min_delay_seconds", 1, 1, Integer.MAX_VALUE);
		radar_scan_delay_factors_seconds = builder
				.comment("Dcan delay factors {a, b, c, d}. You need to provide exactly 4 values.\n"
				+ "The equation used is a + b * radius + c * radius^2 + d * radius^3, (measured in seconds)")
				.translation("warpdrive.config.radar.scan_delay_factors_seconds")
				.define("scan_delay_factors_seconds", Arrays.asList(1.0D, 0.001D, 0.0D, 0.0D));
		radar_max_isolation_range = builder
				.comment("Radius around core where isolation blocks count, higher is lagger.")
				.translation("warpdrive.config.radar.max_isolation_range")
				.defineInRange("max_isolation_range", 2, 2, 8);
		radar_min_isolation_blocks = builder
				.comment("Number of isolation blocks required to get some isolation.")
				.translation("warpdrive.config.radar.min_isolation_blocks")
				.defineInRange("min_isolation_blocks", 2, 0, 20);
		radar_max_isolation_blocks = builder
				.comment("Number of isolation blocks required to reach maximum effect.")
				.translation("warpdrive.config.radar.max_isolation_blocks")
				.defineInRange("max_isolation_blocks", 16, 5, 94);
		radar_min_isolation_effect = builder
				.comment("Isolation effect achieved with min number of isolation blocks.")
				.translation("warpdrive.config.radar.min_isolation_effect")
				.defineInRange("min_isolation_effect", 0.12D, 0.01D, 0.95D);
		radar_max_isolation_effect = builder
				.comment("Isolation effect achieved with max number of isolation blocks.")
				.translation("warpdrive.config.radar.max_isolation_effect")
				.defineInRange("max_isolation_effect", 1.00D, 0.01D, 1.00D);

		builder.pop();
		
		// Ship
		builder.comment("Ship properties")
		       .push("ship");
		
		ship_max_energy_stored_by_tier = builder
				.comment("Maximum energy stored for a given tier.")
				.translation("warpdrive.config.ship.max_energy_stored_by_tier")
				.define("max_energy_stored_by_tier", Arrays.asList(0, 500000, 10000000, 100000000));
		ship_mass_max_by_tier = builder
				.comment("Maximum ship mass (in blocks) for a given tier.")
				.translation("warpdrive.config.ship.mass_max_by_tier")
				.define("mass_max_by_tier", Arrays.asList(2000000, 3456, 13824, 110592));
		ship_mass_min_by_tier = builder
				.comment("Minimum ship mass (in blocks) for a given tier.")
				.translation("warpdrive.config.ship.mass_min_by_tier")
				.define("mass_min_by_tier", Arrays.asList(0, 64, 1728, 6912));
		ship_mass_max_on_planet_surface = builder
				.comment("Maximum ship mass (in blocks) to jump on a planet.")
				.translation("warpdrive.config.ship.mass_max_on_planet_surface")
				.defineInRange("mass_max_on_planet_surface", 3000, 0, 10000000);
		ship_mass_min_for_hyperspace = builder
				.comment("Minimum ship mass (in blocks) to enter or exit hyperspace without a jumpgate.")
				.translation("warpdrive.config.ship.mass_min_for_hyperspace")
				.defineInRange("mass_min_for_hyperspace", 4000, 0, 10000000);
		ship_size_max_per_side_by_tier = builder
				.comment("Maximum ship size on each axis in blocks, for a given tier.")
				.translation("warpdrive.config.ship.size_max_per_side_by_tier")
				.define("size_max_per_side_by_tier", Arrays.asList(127, 24, 48, 96));
		
		builder.pop();
		
		// Tooltips
		builder.comment("Tooltip configuration is server side for storage mods search features, and client side for JEI & in-game display")
		       .push("tooltip");
		
		final String commentTooltip = "When to show %s in tooltips. Valid values are " + EnumTooltipCondition.formatAllValues() + ".";
		tooltip_add_registry_name = builder
				.comment(String.format(commentTooltip, "registry name"))
				.translation("warpdrive.config.tooltip.add_registry_name")
				.defineEnum("add_registry_name", EnumTooltipCondition.ADVANCED_TOOLTIPS);
		tooltip_add_ore_dictionary_name = builder
				.comment(String.format(commentTooltip, "registry name"))
				.translation("warpdrive.config.tooltip.add_ore_dictionary_name")
				.defineEnum("add_ore_dictionary_name", EnumTooltipCondition.ALWAYS);
		tooltip_add_armor_points = builder
				.comment(String.format(commentTooltip, "armor points"))
				.translation("warpdrive.config.tooltip.add_armor_points")
				.defineEnum("add_armor_points", EnumTooltipCondition.NEVER);
		tooltip_add_block_material = builder
				.comment(String.format(commentTooltip, "block material"))
				.translation("warpdrive.config.tooltip.add_block_material")
				.defineEnum("add_block_material", EnumTooltipCondition.ADVANCED_TOOLTIPS);
		tooltip_add_burn_time = builder
				.comment(String.format(commentTooltip, "burn time"))
				.translation("warpdrive.config.tooltip.add_burn_time")
				.defineEnum("add_burn_time", EnumTooltipCondition.ADVANCED_TOOLTIPS);
		tooltip_add_durability = builder
				.comment(String.format(commentTooltip, "durability"))
				.translation("warpdrive.config.tooltip.add_durability")
				.defineEnum("add_durability", EnumTooltipCondition.ALWAYS);
		tooltip_add_enchantability = builder
				.comment(String.format(commentTooltip, "armor & tool enchantability"))
				.translation("warpdrive.config.tooltip.add_enchantability")
				.defineEnum("add_enchantability", EnumTooltipCondition.ON_SNEAK);
		tooltip_add_entity_id = builder
				.comment(String.format(commentTooltip, "entity id"))
				.translation("warpdrive.config.tooltip.add_entity_id")
				.defineEnum("add_entity_id", EnumTooltipCondition.ADVANCED_TOOLTIPS);
		tooltip_add_flammability = builder
				.comment(String.format(commentTooltip, "flammability"))
				.translation("warpdrive.config.tooltip.add_flammability")
				.defineEnum("add_flammability", EnumTooltipCondition.ADVANCED_TOOLTIPS);
		tooltip_add_fluid_stats = builder
		        .comment(String.format(commentTooltip, "fluid stats"))
		        .translation("warpdrive.config.tooltip.add_fluid_stats")
		        .defineEnum("add_fluid_stats", EnumTooltipCondition.ALWAYS);
		tooltip_add_hardness = builder
				.comment(String.format(commentTooltip, "hardness & explosion resistance"))
				.translation("warpdrive.config.tooltip.add_hardness")
				.defineEnum("add_hardness", EnumTooltipCondition.ADVANCED_TOOLTIPS);
		tooltip_add_harvesting_stats = builder
		        .comment(String.format(commentTooltip, "harvesting stats"))
		        .translation("warpdrive.config.tooltip.add_harvesting_stats")
		        .defineEnum("add_harvesting_stats", EnumTooltipCondition.ALWAYS);
		tooltip_add_opacity = builder
				.comment(String.format(commentTooltip, "opacity"))
				.translation("warpdrive.config.tooltip.add_opacity")
				.defineEnum("add_opacity", EnumTooltipCondition.ADVANCED_TOOLTIPS);
		tooltip_add_repair_material = builder
				.comment(String.format(commentTooltip, "repair material"))
				.translation("warpdrive.config.tooltip.add_repair_material")
				.defineEnum("add_repair_material", EnumTooltipCondition.ON_SNEAK);
		
		tooltip_cleanup_list = builder
				.comment("List of lines to remove from tooltips before adding ours. This can be a partial match in a line. Must be lowercase without formatting.")
				.translation("warpdrive.config.tooltip.cleanup_list")
				.define("cleanup_list", Arrays.asList(
						"fuel details",
						"burn time",
						"durability" ));
		
		tooltip_enable_deduplication = builder
				.comment(String.format("When to remove duplicate lines in tooltips. Valid values are %s.", EnumTooltipCondition.formatAllValues()))
				.translation("warpdrive.config.tooltip.enable_deduplication")
				.defineEnum("enable_deduplication", EnumTooltipCondition.ALWAYS);
		
		builder.pop();
		
		// Transporter
		builder.comment("Transporter properties")
		       .push("transporter");
		
		transporter_max_energy_stored = builder
				.comment("Maximum energy stored.")
				.translation("warpdrive.config.transporter.max_energy_stored")
				.defineInRange("max_energy_stored", 1000000, 1, Integer.MAX_VALUE);
		/* TODO transporter configuration?
		DoubleValue transporter_energy_per_block = builder
				                                .comment("Energy cost per block distance.")
				                                .translation("warpdrive.config.transporter.energy_per_block")
				                                .defineInRange("energy_per_block", 100.0D, 1.0D, (double) Integer.MAX_VALUE);
		DoubleValue transporter_max_boost = builder
				                                .comment("Maximum energy boost allowed.")
				                                .translation("warpdrive.config.transporter.max_boost")
				                                .defineInRange("max_boost", 4.0D, 1.0D, 1000.0D);
		*/
		
		builder.pop();
		
		// Tree Farm (common)
		builder.comment("Tree farm properties")
		       .push("tree_farm");
		
		tree_farm_max_mediums_count = builder
				.comment("Maximum number of laser mediums.")
				.translation("warpdrive.config.tree_farm.max_mediums_count")
				.defineInRange("max_mediums_count", 5, 1, 10);
		tree_farm_max_radius_no_laser_medium = builder
				.comment("Maximum scan radius without any laser medium, on X and Z axis, measured in blocks.")
				.translation("warpdrive.config.tree_farm.max_radius_no_laser_medium")
				.defineInRange("max_radius_no_laser_medium", 3, 0, 15);
		tree_farm_max_radius_per_laser_medium = builder
				.comment("Bonus to maximum scan radius per laser medium, on X and Z axis, measured in blocks.")
				.translation("warpdrive.config.tree_farm.max_radius_per_laser_medium")
				.defineInRange("max_radius_per_laser_medium", 2, 1, 8);
		tree_farm_max_reach_distance_no_laser_medium = builder
				.comment("Maximum reach distance of the laser without any laser medium, measured in blocks.")
				.translation("warpdrive.config.tree_farm.max_reach_distance_no_laser_medium")
				.defineInRange("max_reach_distance_no_laser_medium", 5, 1, 64);
		tree_farm_max_reach_distance_per_laser_medium = builder
				.comment("Bonus to maximum reach distance per laser medium, measured in blocks.")
				.translation("warpdrive.config.tree_farm.max_reach_distance_per_laser_medium")
				.defineInRange("max_reach_distance_per_laser_medium", 5, 0, 16);
		
		builder.pop();
		
		return builder.build();
	}
	
	public static ForgeConfigSpec buildServerConfig() {
		final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		
		// Breathing (server)
		builder.comment("Air generator & general breathing properties")
		       .push("breathing");
		
		breathing_air_generation_interval_ticks = builder
				.comment("Update speed of air generation.")
				.translation("warpdrive.config.breathing.air_generation_interval_ticks")
				.defineInRange("air_generation_interval_ticks", 40, 1, 300);
		breathing_volume_update_depth_blocks = builder
				.comment("Maximum depth of blocks to update when a volume has changed.\n"
				+ "Higher values may cause TPS lag spikes, Lower values will exponentially increase the repressurization time.")
				.translation("warpdrive.config.breathing.volume_update_depth_blocks")
				.defineInRange("volume_update_depth_blocks", 256, 10, 256);
		breathing_simulation_delay_ticks = builder
				.comment("Minimum delay between consecutive air propagation updates of the same block.")
				.translation("warpdrive.config.breathing.simulation_delay_ticks")
				.defineInRange("simulation_delay_ticks", 30, 1, 90);
		breathing_enable_air_at_entity_debug = builder
				.comment("Spam creative players with air status around them, use at your own risk.")
				.translation("warpdrive.config.breathing.enable_air_at_entity_debug")
				.define("enable_air_at_entity_debug", false);
		breathing_air_tank_breath_duration_ticks = builder
				.comment("Duration of a single breath cycle measured in ticks.")
				.translation("warpdrive.config.breathing.air_tank_breath_duration_ticks")
				.defineInRange("air_tank_breath_duration_ticks", 300, 100, 1200);
		breathing_air_tank_capacity_by_tier = builder
				.comment("Number of breaths cycles available in a air tank, by tier (canister, normal, advanced, superior).")
				.translation("warpdrive.config.breathing.air_tank_capacity_by_tier")
				.define("air_tank_capacity_by_tier", Arrays.asList(20, 32, 64, 128));
		
		builder.pop();
		
		// Energy handling
		builder.comment("Energy handling")
		       .push("energy");
		
		energy_display_units = builder
				.comment("Default display units for energy (EU, RF, FE, \u0230I).")
				.translation("warpdrive.config.energy.display_units")
				.define("display_units", "FE");
		energy_enable_FE = builder
				.comment("Enable Forge energy support, disable it for a pure EU energy support.")
				.translation("warpdrive.config.energy.enable_FE")
				.define("enable_FE", true);
		energy_enable_GTCE_EU = builder
				.comment("Enable Gregtech EU energy support when the GregtechCE mod is present, disable otherwise.")
				.translation("warpdrive.config.energy.enable_GTCE_EU")
				.define("enable_GTCE_EU", true);
		energy_enable_IC2_EU = builder
				.comment("Enable IC2 EU energy support when the IndustrialCraft2 mod is present, disable otherwise.")
				.translation("warpdrive.config.energy.enable_IC2_EU")
				.define("enable_IC2_EU", true);
		energy_overvoltage_shock_factor = builder
				.comment("Shock damage factor to entities in case of EU voltage overload, set to 0 to disable completely.")
				.translation("warpdrive.config.energy.overvoltage_shock_factor")
				.defineInRange("overvoltage_shock_factor", 1.0F, 0.0F, 10.0F);
		energy_overvoltage_explosion_factor = builder
				.comment("Explosion strength factor in case of EU voltage overload, set to 0 to disable completely.")
				.translation("warpdrive.config.energy.overvoltage_explosion_factor")
				.defineInRange("overvoltage_explosion_factor", 1.0F, 0.0F, 10.0F);
		energy_scan_interval_ticks = builder
				.comment("Delay between scan for energy receivers (measured in ticks).")
				.translation("warpdrive.config.energy.scan_interval_ticks")
				.defineInRange("scan_interval_ticks", 20, 1, 300);
		
		builder.pop();
		
		// Laser cannon (server)
		builder.comment("Laser cannon handling")
		       .push("laser_cannon");
		
		laser_cannon_emit_fire_delay_ticks = builder
				.comment("Delay while booster beams are accepted, before actually shooting (measured in ticks).")
				.translation("warpdrive.config.laser_cannon.emit_fire_delay_ticks")
				.defineInRange("emit_fire_delay_ticks", 5, 1, 100);
		laser_cannon_emit_scan_delay_ticks = builder
				.comment("Delay while booster beams are accepted, before actually scanning")
				.translation("warpdrive.config.laser_cannon.emit_scan_delay_ticks")
				.defineInRange("emit_scan_delay_ticks", 1, 1, 100);
		
		laser_cannon_energy_attenuation_per_air_block = builder
				.comment("Energy attenuation when going through air blocks (on a planet or any gas in space).")
				.translation("warpdrive.config.laser_cannon.energy_attenuation_per_air_block")
				.defineInRange("energy_attenuation_per_air_block", 0.000200D, 0.0D, 0.1D);
		laser_cannon_energy_attenuation_per_void_block = builder
				.comment("Energy attenuation when going through void blocks (in space or hyperspace).")
				.translation("warpdrive.config.laser_cannon.energy_attenuation_per_void_block")
				.defineInRange("energy_attenuation_per_void_block", 0.000005D, 0.0D, 0.1D);
		laser_cannon_energy_attenuation_per_broken_block = builder
				.comment("Energy attenuation when going through a broken block.")
				.translation("warpdrive.config.laser_cannon.energy_attenuation_per_broken_block")
				.defineInRange("energy_attenuation_per_broken_block", 0.23D, 0.0D, 1.0D);
		
		laser_cannon_entity_hit_set_on_fire_seconds = builder
				.comment("Duration of fire effect on entity hit (in seconds).")
				.translation("warpdrive.config.laser_cannon.entity_hit_set_on_fire_seconds")
				.defineInRange("entity_hit_set_on_fire_seconds", 20, 0, 300);
		
		laser_cannon_entity_hit_energy = builder
				.comment("Base energy consumed from hitting an entity.")
				.translation("warpdrive.config.laser_cannon.entity_hit_energy")
				.defineInRange("entity_hit_energy", 15000, 0, Integer.MAX_VALUE);
		laser_cannon_entity_hit_base_damage = builder
				.comment("Minimum damage to entity hit (measured in half hearts).")
				.translation("warpdrive.config.laser_cannon.entity_hit_base_damage")
				.defineInRange("entity_hit_base_damage", 3, 0, Integer.MAX_VALUE);
		laser_cannon_entity_hit_energy_per_damage = builder
				.comment("Energy required by additional hit point (won't be consumed).")
				.translation("warpdrive.config.laser_cannon.entity_hit_energy_per_damage")
				.defineInRange("entity_hit_energy_per_damage", 30000, 0, Integer.MAX_VALUE);
		laser_cannon_entity_hit_max_damage = builder
				.comment("Maximum damage to entity hit, set to 0 to disable damage completely.")
				.translation("warpdrive.config.laser_cannon.entity_hit_max_damage")
				.defineInRange("entity_hit_max_damage", 100, 0, Integer.MAX_VALUE);
		
		laser_cannon_entity_hit_energy_threshold_for_explosion = builder
				.comment("Minimum energy to cause explosion effect.")
				.translation("warpdrive.config.laser_cannon.entity_hit_energy_threshold_for_explosion")
				.defineInRange("entity_hit_energy_threshold_for_explosion", 900000, 0, Integer.MAX_VALUE);
		laser_cannon_entity_hit_explosion_base_strength = builder
				.comment("Explosion base strength, 4 is Vanilla TNT.")
				.translation("warpdrive.config.laser_cannon.entity_hit_explosion_base_strength")
				.defineInRange("entity_hit_explosion_base_strength", 4.0F, 0.0D, 100.0D);
		laser_cannon_entity_hit_explosion_energy_per_strength = builder
				.comment("Energy per added explosion strength.")
				.translation("warpdrive.config.laser_cannon.entity_hit_explosion_energy_per_strength")
				.defineInRange("entity_hit_explosion_energy_per_strength", 125000, 1, Integer.MAX_VALUE);
		laser_cannon_entity_hit_explosion_max_strength = builder
				.comment("Maximum explosion strength, set to 0 to disable explosion completely.")
				.translation("warpdrive.config.laser_cannon.entity_hit_explosion_max_strength")
				.defineInRange("entity_hit_explosion_max_strength", 4.0F, 0.0D, 1000.0D);
		
		laser_cannon_block_hit_energy_min = builder
				.comment("Minimum energy required for breaking a block.")
				.translation("warpdrive.config.laser_cannon.block_hit_energy_min")
				.defineInRange("block_hit_energy_min", 75000, 0, Integer.MAX_VALUE);
		laser_cannon_block_hit_energy_per_block_hardness = builder
				.comment("Energy cost per block hardness for breaking a block.")
				.translation("warpdrive.config.laser_cannon.block_hit_energy_per_block_hardness")
				.defineInRange("block_hit_energy_per_block_hardness", 150000, 0, Integer.MAX_VALUE);
		laser_cannon_block_hit_energy_max = builder
				.comment("Maximum energy required for breaking a block.")
				.translation("warpdrive.config.laser_cannon.block_hit_energy_max")
				.defineInRange("block_hit_energy_max", 750000, 0, Integer.MAX_VALUE);
		laser_cannon_block_hit_absorption_per_block_hardness = builder
				.comment("Probability of energy absorption (i.e. block not breaking) per block hardness. Set to 1.0 to always break the block.")
				.translation("warpdrive.config.laser_cannon.block_hit_absorption_per_block_hardness")
				.defineInRange("block_hit_absorption_per_block_hardness", 0.01D, 0.0D, 1.0D);
		laser_cannon_block_hit_absorption_max = builder
				.comment("Maximum probability of energy absorption (i.e. block not breaking).")
				.translation("warpdrive.config.laser_cannon.block_hit_absorption_max")
				.defineInRange("block_hit_absorption_max", 0.80D, 0.0D, 1.0D);
		
		laser_cannon_block_hit_explosion_hardness_threshold = builder
				.comment("Minimum block hardness required to cause an explosion.")
				.translation("warpdrive.config.laser_cannon.block_hit_explosion_hardness_threshold")
				.defineInRange("block_hit_explosion_hardness_threshold", 5.0D, 0.0D, 10000.0D);
		laser_cannon_block_hit_explosion_base_strength = builder
				.comment("Explosion base strength, 4 is Vanilla TNT.")
				.translation("warpdrive.config.laser_cannon.block_hit_explosion_base_strength")
				.defineInRange("block_hit_explosion_base_strength", 8.0D, 0.0D, 1000.0D);
		laser_cannon_block_hit_explosion_energy_per_strength = builder
				.comment("Energy per added explosion strength.")
				.translation("warpdrive.config.laser_cannon.block_hit_explosion_energy_per_strength")
				.defineInRange("block_hit_explosion_energy_per_strength", 125000, 1, Integer.MAX_VALUE);
		laser_cannon_block_hit_explosion_max_strength = builder
				.comment("Maximum explosion strength, set to 0 to disable explosion completely.")
				.translation("warpdrive.config.laser_cannon.block_hit_explosion_max_strength")
				.defineInRange("block_hit_explosion_max_strength", 50.0D, 0.0D, 1000.0D);
		
		builder.pop();
		
		// Logging
		builder.comment("Logging, mostly server side")
		       .push("logging");
		
		logging_throttle_ms = builder
				.comment("How many milliseconds to wait before logging another occurrence in a time sensitive section of the mod (rendering, events, etc.)")
				.translation("warpdrive.config.logging.throttle_ms")
				.defineInRange("throttle_ms", 5000L, 0L, 600000L);
		
		logging_enable_accelerator_logs = builder
				.comment("Detailed accelerator logs to help debug the mod, enable it before reporting a bug.")
				.translation("warpdrive.config.logging.enable_accelerator_logs")
				.define("enable_accelerator_logs", false);
		logging_enable_break_place_logs = builder
				.comment("Detailed break/place event logs to help debug the mod, enable it before reporting a bug.")
				.translation("warpdrive.config.logging.enable_break_place_logs")
				.define("enable_break_place_logs", false);
		logging_enable_breathing_logs = builder
				.comment("Detailed breathing logs to help debug the mod, enable it before reporting a bug.")
				.translation("warpdrive.config.logging.enable_breathing_logs")
				.define("enable_breathing_logs", false);
		logging_enable_building_logs = builder
				.comment("Detailed building logs to help debug the mod, enable it before reporting a bug.")
				.translation("warpdrive.config.logging.enable_building_logs")
				.define("enable_building_logs", false);
		logging_enable_camera_logs = builder
				.comment("Detailed camera logs to help debug the mod, enable it before reporting a bug.")
				.translation("warpdrive.config.logging.enable_camera_logs")
				.define("enable_camera_logs", false);
		logging_enable_chunk_handler_logs = builder
				.comment("Detailed chunk data logs to help debug the mod.")
				.translation("warpdrive.config.logging.enable_chunk_handler_logs")
				.define("enable_chunk_handler_logs", false);
		logging_enable_chunk_loading_logs = builder
				.comment("Chunk loading logs, enable it to report chunk loaders updates.")
				.translation("warpdrive.config.logging.enable_chunk_loading_logs")
				.define("enable_chunk_loading_logs", false);
		logging_enable_chunk_reloading_logs = builder
				.comment("Report in logs when a chunk is reloaded shortly after being unloaded, usually associated with server lag.")
				.translation("warpdrive.config.logging.enable_chunk_reloading_logs")
				.define("enable_experimental_chunk_reloading_logs", false);
		logging_enable_collection_logs = builder
				.comment("Detailed collection logs to help debug the mod, enable it before reporting a bug.")
				.translation("warpdrive.config.logging.enable_collection_logs")
				.define("enable_collection_logs", false);
		logging_enable_dictionary_logs = builder
				.comment("Dictionary logs, enable it to dump blocks hardness and blast resistance at boot.")
				.translation("warpdrive.config.logging.enable_dictionary_logs")
				.define("enable_dictionary_logs", true);
		logging_enable_energy_logs = builder
				.comment("Detailed energy logs to help debug the mod, will spam your logs...")
				.translation("warpdrive.config.logging.enable_energy_logs")
				.define("enable_energy_logs", false);
		logging_enable_entity_fx_logs = builder
				.comment("EntityFX logs, enable it to dump entityFX registry updates.")
				.translation("warpdrive.config.logging.enable_entity_fx_logs")
				.define("enable_entity_fx_logs", false);
		logging_enable_force_field_logs = builder
				.comment("Detailed force field logs to help debug the mod, enable it before reporting a bug.")
				.translation("warpdrive.config.logging.enable_force_field_logs")
				.define("enable_force_field_logs", false);
		logging_enable_force_field_registry_logs = builder
				.comment("ForceField registry logs, enable it to dump force field registry updates.")
				.translation("warpdrive.config.logging.enable_force_field_registry_logs")
				.define("enable_force_field_registry_logs", false);
		logging_enable_global_region_registry_logs = builder
				.comment("GlobalRegion registry logs, enable it to dump global region registry updates.")
				.translation("warpdrive.config.logging.enable_global_region_registry_logs")
				.define("enable_global_region_registry_logs", false);
		logging_enable_gravity_logs = builder
				.comment("Gravity logs, enable it before reporting fall damage and related issues.")
				.translation("warpdrive.config.logging.enable_XML_preprocessor_logs")
				.define("enable_gravity_logs", false);
		logging_enable_jump_logs = builder
				.comment("Basic jump logs, should always be enabled")
				.translation("warpdrive.config.logging.enable_jump_logs")
				.define("enable_jump_logs", true);
		logging_enable_jumpblocks_logs = builder
				.comment("Detailed jump logs to help debug the mod, will spam your logs...")
				.translation("warpdrive.config.logging.enable_jumpblocks_logs")
				.define("enable_jumpblocks_logs", false);
		logging_enable_LUA_logs = builder
				.comment("Detailed LUA logs to help debug the mod, enable it before reporting a bug.")
				.translation("warpdrive.config.logging.enable_LUA_logs")
				.define("enable_LUA_logs", false);
		logging_enable_offline_avatar_logs = builder
				.comment("Offline avatar logs, enable it before reporting related issues.")
				.translation("warpdrive.config.logging.enable_offline_avatar_logs")
				.define("enable_offline_avatar_logs", false);
		logging_enable_profiling_CPU_time = builder
				.comment("Profiling logs for CPU time, enable it to check for lag.")
				.translation("warpdrive.config.logging.enable_profiling_CPU_time")
				.define("enable_profiling_CPU_time", true);
		logging_enable_profiling_memory_allocation = builder
				.comment("Profiling logs for memory allocation, enable it to check for lag.")
				.translation("warpdrive.config.logging.enable_profiling_memory_allocation")
				.define("enable_profiling_memory_allocation", true);
		logging_enable_profiling_thread_safety = builder
				.comment("Profiling logs for multi-threading, enable it to check for ConcurrentModificationException.")
				.translation("warpdrive.config.logging.enable_profiling_thread_safety")
				.define("enable_profiling_thread_safety", false);
		logging_enable_radar_logs = builder
				.comment("Detailed radar logs to help debug the mod, enable it before reporting a bug.")
				.translation("warpdrive.config.logging.enable_radar_logs")
				.define("enable_radar_logs", false);
		logging_enable_rendering_logs = builder
				.comment("Detailed rendering logs to help debug the mod.")
				.translation("warpdrive.config.logging.enable_rendering_logs")
				.define("enable_rendering_logs", false);
		logging_enable_transporter_logs = builder
				.comment("Detailed transporter logs to help debug the mod, enable it before reporting a bug.")
				.translation("warpdrive.config.logging.enable_transporter_logs")
				.define("enable_transporter_logs", false);
		logging_enable_weapon_logs = builder
				.comment("Detailed weapon logs to help debug the mod, enable it before reporting a bug.")
				.translation("warpdrive.config.logging.enable_weapon_logs")
				.define("enable_weapon_logs", false);
		logging_enable_world_generation_logs = builder
				.comment("Detailed world generation logs to help debug the mod, enable it before reporting a bug.")
				.translation("warpdrive.config.logging.enable_world_generation_logs")
				.define("enable_world_generation_logs", false);
		logging_enable_XML_preprocessor_logs = builder
				.comment("Save XML preprocessor results as output*.xml file, enable it to debug your XML configuration files.")
				.translation("warpdrive.config.logging.enable_XML_preprocessor_logs")
				.define("enable_XML_preprocessor_logs", false);
		
		if (WarpDrive.isDev) {// disabled in production, for obvious reasons :)
			logging_enable_client_synchronization_logs = builder
						.comment("Detailed client synchronization logs to help debug the mod.")
						.translation("warpdrive.config.logging.enable_client_synchronization_logs")
						.define("enable_client_synchronization_logs", false);
			logging_enable_cloaking_logs = builder
						.comment("Detailed cloaking logs to help debug the mod, will spam your logs...")
						.translation("warpdrive.config.logging.enable_cloaking_logs")
						.define("enable_cloaking_logs", false);
			logging_enable_effects_logs = builder
						.comment("Detailed effects logs to help debug the mod, will spam your logs...")
						.translation("warpdrive.config.logging.enable_effects_logs")
						.define("enable_effects_logs", false);
			logging_enable_targeting_logs = builder
						.comment("Detailed targeting logs to help debug the mod, will spam your logs...")
						.translation("warpdrive.config.logging.enable_targeting_logs")
						.define("enable_targeting_logs", false);
			logging_enable_videoChannel_logs = builder
						.comment("Detailed video channel logs to help debug the mod, will spam your logs...")
						.translation("warpdrive.config.logging.enable_videoChannel_logs")
						.define("enable_videoChannel_logs", false);
		}
		
		builder.pop();
		
		// Mining Laser (server)
		builder.comment("Mining Laser properties")
		       .push("mining_laser");
		
		mining_laser_warmup_delay_ticks = builder
				.comment("Warmup duration (buffer on startup when energy source is weak).")
				.translation("warpdrive.config.mining_laser.warmup_delay_ticks")
				.defineInRange("warmup_delay_ticks", 20, 1, 300);
		mining_laser_scan_delay_ticks = builder
				.comment("Scan duration per layer.")
				.translation("warpdrive.config.mining_laser.scan_delay_ticks")
				.defineInRange("scan_delay_ticks", 20, 1, 300);
		mining_laser_mine_delay_ticks = builder
				.comment("Mining duration per scanned block.")
				.translation("warpdrive.config.mining_laser.mine_delay_ticks")
				.defineInRange("mine_delay_ticks", 3, 1, 300);
				
		mining_laser_scan_energy_per_layer_in_atmosphere = builder
				.comment("Energy cost per layer on a planet with atmosphere.")
				.translation("warpdrive.config.mining_laser.scan_energy_per_layer_in_atmosphere")
				.defineInRange("scan_energy_per_layer_in_atmosphere", 1500, 1, Integer.MAX_VALUE);
		mining_laser_mine_energy_per_block_in_atmosphere = builder
				.comment("Energy cost per block on a planet with atmosphere.")
				.translation("warpdrive.config.mining_laser.mine_energy_per_block_in_atmosphere")
				.defineInRange("mine_energy_per_block_in_atmosphere", 2500, 1, Integer.MAX_VALUE);
				
		mining_laser_scan_energy_per_layer_in_void = builder
				.comment("Energy cost per layer in space or a planet without atmosphere.")
				.translation("warpdrive.config.mining_laser.scan_energy_per_layer_in_void")
				.defineInRange("scan_energy_per_layer_in_void", 20000, 1, Integer.MAX_VALUE);
				
		mining_laser_mine_energy_per_block_in_void = builder
				.comment("Energy cost per block in space or a planet without atmosphere.")
				.translation("warpdrive.config.mining_laser.mine_energy_per_block_in_void")
				.defineInRange("mine_energy_per_block_in_void", 1500, 1, Integer.MAX_VALUE);
				
		mining_laser_mine_ores_only_energy_factor = builder
				.comment("Energy cost multiplier per block when mining only ores.")
				.translation("warpdrive.config.mining_laser.mine_ores_only_energy_factor")
				.defineInRange("mine_ores_only_energy_factor", 15.0D, 1.5D, 1000.0D);
		mining_laser_mine_silktouch_energy_factor = builder
				.comment("Energy cost multiplier per block when mining with silktouch.")
				.translation("warpdrive.config.mining_laser.mine_silktouch_energy_factor")
				.defineInRange("mine_silktouch_energy_factor", 1.5D, 1.5D, 1000.0D);
		
		if (unused) {
			mining_laser_mine_silktouch_deuterium_mB = builder
			        .comment("Deuterium cost per block when mining with silktouch (0 to disable).")
			        .translation("warpdrive.config.mining_laser.mine_silktouch_deuterium_mB")
			        .defineInRange("mine_silktouch_deuterium_mB", 0, 0, 10000);
			
			mining_laser_fortune_energy_factor = builder
					.comment("Energy cost multiplier per fortune level.")
					.translation("warpdrive.config.mining_laser.fortune_energy_factor")
					.defineInRange("fortune_energy_factor", 1.5D, 0.01D, 1000.0D);
		}
		
		builder.pop();
		
		// Offline avatar
		builder.comment("Offline avatar")
		       .push("offline_avatar");
		
		offline_avatar_enable = builder
				.comment("Enable creation of offline avatars to follow ship movements. This only disable creating new ones.")
				.translation("warpdrive.config.offline_avatar.enable")
				.define("enable", true);
		offline_avatar_create_only_aboard_ships = builder
				.comment("Only create an offline avatar when player disconnects while inside a ship. Disabling may cause lag in spawn areas...")
				.translation("warpdrive.config.offline_avatar.create_only_aboard_ships")
				.define("create_only_aboard_ships", true);
		offline_avatar_forget_on_death = builder
				.comment("Enable to forget current avatar position when it's killed, or disable player teleportation to last known avatar's position.")
				.translation("warpdrive.config.offline_avatar.forget_on_death")
				.define("forget_on_death", true);
		offline_avatar_model_scale = builder
				.comment("Scale of offline avatar compared to a normal player.")
				.translation("warpdrive.config.offline_avatar.model_scale")
				.defineInRange("model_scale", 0.5D, 0.20D, 2.00D);
		offline_avatar_always_render_name_tag = builder
				.comment("Should avatar name tag always be visible?")
				.translation("warpdrive.config.offline_avatar.always_render_name_tag")
				.define("always_render_name_tag", false);
		offline_avatar_min_range_for_removal = builder
				.comment("Minimum range between a player and their avatar to consider it for removal (i.e. ensuring connection was successful).")
				.translation("warpdrive.config.offline_avatar.min_range_for_removal")
				.defineInRange("min_range_for_removal", 1.0D, 0.10D, 10.00D);
		offline_avatar_max_range_for_removal = builder
				.comment("Maximum range between a player and his/her avatar to consider it for removal.")
				.translation("warpdrive.config.offline_avatar.max_range_for_removal")
				.defineInRange("max_range_for_removal", 1.0D, 3.00D, Float.MAX_VALUE);
		offline_avatar_delay_for_removal_s = builder
				.comment("Delay before removing an avatar when their related player is in range (measured in seconds).")
				.translation("warpdrive.config.offline_avatar.delay_for_removal_s")
				.defineInRange("delay_for_removal_s", 1, 0, 300);
		
		builder.pop();
		
		// Ship
		builder.comment("Ship")
		       .push("ship");
		
		ship_collision_tolerance_blocks = builder
				.comment("Tolerance in block in case of collision before causing damages...")
				.translation("warpdrive.config.ship.collision_tolerance_blocks")
				.defineInRange("collision_tolerance_blocks", 3, 0, 30000000);
		ship_mass_unlimited_player_names = builder
				.comment("List of player names which have unlimited block counts to their ship.")
				.translation("warpdrive.config.ship.mass_unlimited_player_names")
				.define("mass_unlimited_player_names", Arrays.asList("notch", "someone"));
		ship_volume_scan_blocks_per_tick = builder
				.comment("Number of blocks to scan per tick when getting ship bounds, too high will cause lag spikes when resizing a ship.")
				.translation("warpdrive.config.ship.volume_scan_blocks_per_tick")
				.defineInRange("volume_scan_blocks_per_tick", 1000, 100, 100000);
		ship_volume_scan_age_tolerance = builder
				.comment("Ship volume won't be refreshed unless it's older than that many seconds.")
				.translation("warpdrive.config.ship.volume_scan_age_tolerance")
				.defineInRange("volume_scan_age_tolerance", 120, 0, 300);
		ship_warmup_random_ticks = builder
				.comment("Random variation added to warm-up (measured in ticks).")
				.translation("warpdrive.config.ship.warmup_random_ticks")
				.defineInRange("warmup_random_ticks", 60, 10, 200);
		
		builder.pop();
		
		// Ship Scanner
		builder.comment("Ship scanner")
		       .push("ship_scanner");
		
		ship_scanner_max_deploy_radius_blocks = builder
				.comment("Max distance from ship scanner to ship core, measured in blocks.")
				.translation("warpdrive.config.ship_scanner.max_deploy_radius_blocks")
				.defineInRange("max_deploy_radius_blocks", 100, 5, 150);
		ship_scanner_search_interval_ticks = builder
				.comment("Max distance from ship scanner to ship core, measured in blocks.")
				.translation("warpdrive.config.ship_scanner.search_interval_ticks")
				.defineInRange("search_interval_ticks", 20, 5, 200);
		ship_scanner_scan_blocks_per_second = builder
				.comment("Scanning speed, measured in blocks.")
				.translation("warpdrive.config.ship_scanner.scan_blocks_per_second")
				.defineInRange("scan_blocks_per_second", 10, 1, 50000);
		ship_scanner_deploy_blocks_per_interval = builder
				.comment("Deployment speed, measured in blocks.")
				.translation("warpdrive.config.ship_scanner.deploy_blocks_per_interval")
				.defineInRange("deploy_blocks_per_interval", 10, 1, 3000);
		ship_scanner_deploy_interval_ticks = builder
				.comment("Delay between deployment of 2 sets of blocks, measured in ticks.")
				.translation("warpdrive.config.ship_scanner.deploy_interval_ticks")
				.defineInRange("deploy_interval_ticks", 4, 1, 60);
		
		builder.pop();
		
		// Ship movement costs
		builder.comment("Ship movement costs"
		              + "\nEach time, you need to provide exactly 5 values < A B C D E >. The equation used is A + B * mass + C * distance + D * ln( mass ) * exp( distance / E )"
		              + "\nResult is rounded up to an integer. Use 0 to ignore that part of the equation.")
		       .push("ship_movement_costs");
		
		SHIP_MOVEMENT_COSTS_FACTORS = new ShipMovementCosts.Factors[EnumShipMovementType.length];
		for (final EnumShipMovementType shipMovementType : EnumShipMovementType.values()) {
			SHIP_MOVEMENT_COSTS_FACTORS[shipMovementType.ordinal()] = new ShipMovementCosts.Factors(
					shipMovementType.maximumDistanceDefault,
					shipMovementType.energyRequiredDefault,
					shipMovementType.warmupDefault,
					shipMovementType.sicknessDefault,
					shipMovementType.cooldownDefault);
			
			if (shipMovementType.hasConfiguration) {
				SHIP_MOVEMENT_COSTS_FACTORS[shipMovementType.ordinal()].build(builder, shipMovementType.getName(), shipMovementType.getDescription());
			}
		}
		
		builder.pop();
		
		return builder.build();
	}
	
	
	public static void onModConfigLoading(@Nonnull final ModConfig.Loading event) {
		WarpDrive.logger.info(String.format("Loading configuration file %s",
		                                    event.getConfig().getFullPath() ));
		
		switch (event.getConfig().getType()) {
		case CLIENT: loadClientConfig(); break;
		case COMMON: loadCommonConfig(); break;
		case SERVER: loadServerConfig(); break;
		default:
			WarpDrive.logger.error(String.format("Unknown configuration type %s",
			                                     event.getConfig().getType() ));
			break;
		}
	}
	
	public static void loadClientConfig() {
		CLIENT_BREATHING_OVERLAY_FORCED = client_breathing_overlay_forced.get();
		CLIENT_LOCATION_SCALE = client_location_scale.get().floatValue();
		CLIENT_LOCATION_NAME_PREFIX = client_location_name_prefix.get();
		CLIENT_LOCATION_BACKGROUND_COLOR = (int) (Long.decode(client_location_background_color.get()) & 0xFFFFFFFFL);
		CLIENT_LOCATION_TEXT_COLOR = (int) (Long.decode(client_location_text_color.get()) & 0xFFFFFFFFL);
		CLIENT_LOCATION_HAS_SHADOW = client_location_has_shadow.get();
		CLIENT_LOCATION_SCREEN_ALIGNMENT = client_location_screen_alignment.get();
		CLIENT_LOCATION_SCREEN_OFFSET_X = client_location_offset_x.get();
		CLIENT_LOCATION_SCREEN_OFFSET_Y = client_location_offset_y.get();
		CLIENT_LOCATION_TEXT_ALIGNMENT = client_location_text_alignment.get();
		CLIENT_LOCATION_WIDTH_RATIO = client_location_width_ratio.get().floatValue();
		CLIENT_LOCATION_WIDTH_MIN = client_location_width_min.get();
	}
	
	public static void loadCommonConfig() {
		// General
		G_SPACE_PROVIDER_ID = general_space_provider_id.get();
		G_HYPERSPACE_PROVIDER_ID = general_hyperspace_provider_id.get();
		
		G_LUA_SCRIPTS = general_lua_scripts.get();
		G_SCHEMATICS_LOCATION = general_schematics_location.get();
		
		G_ASSEMBLY_SCAN_INTERVAL_SECONDS = general_assembly_scanning_interval.get();
		G_ASSEMBLY_SCAN_INTERVAL_TICKS = 20 * WarpDriveConfig.G_ASSEMBLY_SCAN_INTERVAL_SECONDS;
		G_PARAMETERS_UPDATE_INTERVAL_TICKS = general_parameters_update_interval.get();
		G_REGISTRY_UPDATE_INTERVAL_SECONDS = general_registry_update_interval.get();
		G_REGISTRY_UPDATE_INTERVAL_TICKS = 20 * WarpDriveConfig.G_REGISTRY_UPDATE_INTERVAL_SECONDS;
		G_ENFORCE_VALID_CELESTIAL_OBJECTS = general_enforce_valid_celestial_objects.get();
		G_BLOCKS_PER_TICK = general_blocks_per_tick.get();
		G_ENABLE_FAST_SET_BLOCKSTATE = general_enable_fast_set_blockstate.get();
		G_ENABLE_PROTECTION_CHECKS = general_enable_protection_checks.get();
		G_ENABLE_EXPERIMENTAL_REFRESH = general_enable_experimental_refresh.get();
		G_BLAST_RESISTANCE_CAP = general_blast_resistance_cap.get().floatValue();
		
		// Particles accelerator
		ACCELERATOR_MAX_PARTICLE_BUNCHES = atomic_max_particle_bunches.get();
		
		// Breathing (common)
		BREATHING_MAX_ENERGY_STORED_BY_TIER = Ints.toArray(breathing_max_energy_stored_by_tier.get());
		clampByTier(1, Integer.MAX_VALUE, BREATHING_MAX_ENERGY_STORED_BY_TIER);
		
		BREATHING_ENERGY_PER_CANISTER = Commons.clamp(1, BREATHING_MAX_ENERGY_STORED_BY_TIER[1], breathing_energy_per_canister.get());
		if (BREATHING_ENERGY_PER_CANISTER != breathing_energy_per_canister.get()) {
			breathing_energy_per_canister.set(BREATHING_ENERGY_PER_CANISTER);
		}
		
		BREATHING_ENERGY_PER_NEW_AIR_BLOCK_BY_TIER = Ints.toArray(breathing_energy_per_new_air_block_by_tier.get());
		clampByTier(1, BREATHING_MAX_ENERGY_STORED_BY_TIER[2], BREATHING_ENERGY_PER_NEW_AIR_BLOCK_BY_TIER);
		BREATHING_ENERGY_PER_NEW_AIR_BLOCK_BY_TIER[0] = Commons.clamp(1, BREATHING_MAX_ENERGY_STORED_BY_TIER[0], BREATHING_ENERGY_PER_NEW_AIR_BLOCK_BY_TIER[0]);
		BREATHING_ENERGY_PER_NEW_AIR_BLOCK_BY_TIER[1] = Commons.clamp(1, BREATHING_MAX_ENERGY_STORED_BY_TIER[1], BREATHING_ENERGY_PER_NEW_AIR_BLOCK_BY_TIER[1]);
		BREATHING_ENERGY_PER_NEW_AIR_BLOCK_BY_TIER[2] = Commons.clamp(1, BREATHING_MAX_ENERGY_STORED_BY_TIER[2], BREATHING_ENERGY_PER_NEW_AIR_BLOCK_BY_TIER[2]);
		
		BREATHING_ENERGY_PER_EXISTING_AIR_BLOCK_BY_TIER = Ints.toArray(breathing_energy_per_existing_air_block_by_tier.get());
		clampByTier(1, BREATHING_MAX_ENERGY_STORED_BY_TIER[2], BREATHING_ENERGY_PER_EXISTING_AIR_BLOCK_BY_TIER);
		BREATHING_ENERGY_PER_EXISTING_AIR_BLOCK_BY_TIER[0] = Commons.clamp(1, BREATHING_MAX_ENERGY_STORED_BY_TIER[0], BREATHING_ENERGY_PER_EXISTING_AIR_BLOCK_BY_TIER[0]);
		BREATHING_ENERGY_PER_EXISTING_AIR_BLOCK_BY_TIER[1] = Commons.clamp(1, BREATHING_MAX_ENERGY_STORED_BY_TIER[1], BREATHING_ENERGY_PER_EXISTING_AIR_BLOCK_BY_TIER[1]);
		BREATHING_ENERGY_PER_EXISTING_AIR_BLOCK_BY_TIER[2] = Commons.clamp(1, BREATHING_MAX_ENERGY_STORED_BY_TIER[2], BREATHING_ENERGY_PER_EXISTING_AIR_BLOCK_BY_TIER[2]);
		
		BREATHING_AIR_GENERATION_RANGE_BLOCKS_BY_TIER = Ints.toArray(breathing_air_generation_range_blocks.get());
		clampByTier(8, 256, BREATHING_AIR_GENERATION_RANGE_BLOCKS_BY_TIER);
		
		// Subspace capacitor
		CAPACITOR_MAX_ENERGY_STORED_BY_TIER = Ints.toArray(capacitor_max_energy_stored_by_tier.get());
		clampByTier(0, Integer.MAX_VALUE, CAPACITOR_MAX_ENERGY_STORED_BY_TIER);
		
		CAPACITOR_IC2_SINK_TIER_NAME_BY_TIER = capacitor_ic2_sink_tier_name_by_tier.get().toArray(new String[0]);
		clampByEnergyTierName("ULV", "MaxV", CAPACITOR_IC2_SINK_TIER_NAME_BY_TIER);
		
		CAPACITOR_IC2_SOURCE_TIER_NAME_BY_TIER = capacitor_ic2_source_tier_name_by_tier.get().toArray(new String[0]);
		clampByEnergyTierName("ULV", "MaxV", CAPACITOR_IC2_SOURCE_TIER_NAME_BY_TIER);
		
		CAPACITOR_FLUX_RATE_INPUT_BY_TIER = Ints.toArray(capacitor_flux_rate_input_per_tick_by_tier.get());
		clampByTier(0, Integer.MAX_VALUE / 5, CAPACITOR_FLUX_RATE_INPUT_BY_TIER);
		
		CAPACITOR_FLUX_RATE_OUTPUT_BY_TIER = Ints.toArray(capacitor_flux_rate_output_per_tick_by_tier.get());
		clampByTier(0, Integer.MAX_VALUE / 5, CAPACITOR_FLUX_RATE_OUTPUT_BY_TIER);
		
		CAPACITOR_EFFICIENCY_PER_UPGRADE = Doubles.toArray(capacitor_efficiency_per_upgrade.get());
		assert CAPACITOR_EFFICIENCY_PER_UPGRADE.length >= 1;
		CAPACITOR_EFFICIENCY_PER_UPGRADE[0] = Math.min(1.0D, Commons.clamp(                               0.5D, CAPACITOR_EFFICIENCY_PER_UPGRADE[1], CAPACITOR_EFFICIENCY_PER_UPGRADE[0]));
		CAPACITOR_EFFICIENCY_PER_UPGRADE[1] = Math.min(1.0D, Commons.clamp(CAPACITOR_EFFICIENCY_PER_UPGRADE[0], CAPACITOR_EFFICIENCY_PER_UPGRADE[2], CAPACITOR_EFFICIENCY_PER_UPGRADE[1]));
		CAPACITOR_EFFICIENCY_PER_UPGRADE[2] = Math.min(1.0D, Commons.clamp(CAPACITOR_EFFICIENCY_PER_UPGRADE[1], Integer.MAX_VALUE                  , CAPACITOR_EFFICIENCY_PER_UPGRADE[2]));
		
		// Chunk loader
		CHUNK_LOADER_MAX_ENERGY_STORED = chunk_loader_max_energy_stored.get();
		CHUNK_LOADER_MAX_RADIUS = chunk_loader_max_radius.get();
		CHUNK_LOADER_ENERGY_PER_CHUNK = chunk_loader_energy_per_chunk.get();
		
		// Cloaking
		CLOAKING_MAX_ENERGY_STORED = cloaking_max_energy_stored.get();
		CLOAKING_COIL_CAPTURE_BLOCKS = cloaking_coil_capture_blocks.get();
		CLOAKING_MAX_FIELD_RADIUS = Commons.clamp(CLOAKING_COIL_CAPTURE_BLOCKS + 3, 128, cloaking_max_field_radius.get());
		if (CLOAKING_MAX_FIELD_RADIUS != cloaking_max_field_radius.get()) {
			cloaking_max_field_radius.set(CLOAKING_MAX_FIELD_RADIUS);
		}
		CLOAKING_TIER1_ENERGY_PER_BLOCK = cloaking_tier1_energy_per_block.get();
		CLOAKING_TIER2_ENERGY_PER_BLOCK = Commons.clamp(CLOAKING_TIER1_ENERGY_PER_BLOCK, Integer.MAX_VALUE, cloaking_tier2_energy_per_block.get());
		if (CLOAKING_TIER2_ENERGY_PER_BLOCK != cloaking_tier2_energy_per_block.get()) {
			cloaking_tier2_energy_per_block.set(CLOAKING_TIER2_ENERGY_PER_BLOCK);
		}
		CLOAKING_TIER1_FIELD_REFRESH_INTERVAL_TICKS = cloaking_tier1_field_refresh_interval_ticks.get();
		CLOAKING_TIER2_FIELD_REFRESH_INTERVAL_TICKS = cloaking_tier2_field_refresh_interval_ticks.get();
		CLOAKING_VOLUME_SCAN_BLOCKS_PER_TICK = cloaking_volume_scan_blocks_per_tick.get();
		CLOAKING_VOLUME_SCAN_AGE_TOLERANCE_SECONDS = cloaking_volume_scan_age_tolerance_seconds.get();
		
		// Enantiomorphic reactor
		ENAN_REACTOR_MAX_ENERGY_STORED_BY_TIER = Ints.toArray(enantiomorphic_reactor_max_energy_stored_by_tier.get());
		clampByTier(1, Integer.MAX_VALUE, ENAN_REACTOR_MAX_ENERGY_STORED_BY_TIER);
		ENAN_REACTOR_MAX_LASERS_PER_SECOND = Ints.toArray(enantiomorphic_reactor_max_lasers_per_second_by_tier.get());
		clampByTier(1, Integer.MAX_VALUE, ENAN_REACTOR_MAX_LASERS_PER_SECOND);
		ENAN_REACTOR_GENERATION_MIN_FE_BY_TIER = Ints.toArray(enantiomorphic_reactor_min_generation_FE_by_tier.get());
		clampByTier(1, Integer.MAX_VALUE, ENAN_REACTOR_GENERATION_MIN_FE_BY_TIER);
		ENAN_REACTOR_GENERATION_MAX_FE_BY_TIER = Ints.toArray(enantiomorphic_reactor_max_generation_FE_by_tier.get());
		clampByTier(1, Integer.MAX_VALUE, ENAN_REACTOR_GENERATION_MAX_FE_BY_TIER);
		
		// Force field projector
		FORCE_FIELD_PROJECTOR_MAX_ENERGY_STORED_BY_TIER = Ints.toArray(force_field_projector_max_energy_stored_by_tier.get());
		clampByTier(0, Integer.MAX_VALUE, FORCE_FIELD_PROJECTOR_MAX_ENERGY_STORED_BY_TIER);
		
		FORCE_FIELD_PROJECTOR_EXPLOSION_SCALE = force_field_projector_explosion_scale.get();
		FORCE_FIELD_PROJECTOR_MAX_LASER_REQUIRED = force_field_projector_max_laser_required.get();
		FORCE_FIELD_EXPLOSION_STRENGTH_VANILLA_CAP = force_field_explosion_strength_vanilla_cap.get();
		
		// IC2 Reactor cooler
		IC2_REACTOR_MAX_HEAT_STORED = ic2_reactor_laser_max_heat_stored.get();
		IC2_REACTOR_COMPONENT_HEAT_TRANSFER_PER_TICK = ic2_reactor_laser_component_heat_transfer_per_tick.get();
		IC2_REACTOR_FOCUS_HEAT_TRANSFER_PER_TICK = ic2_reactor_laser_focus_heat_transfer_per_tick.get();
		IC2_REACTOR_REACTOR_HEAT_TRANSFER_PER_TICK = ic2_reactor_laser_reactor_heat_transfer_per_tick.get();
		IC2_REACTOR_COOLING_PER_INTERVAL = ic2_reactor_laser_cooling_per_interval.get();
		IC2_REACTOR_ENERGY_PER_HEAT = ic2_reactor_laser_energy_per_heat.get();
		IC2_REACTOR_COOLING_INTERVAL_TICKS = ic2_reactor_laser_cooling_interval_ticks.get();
		
		// Jump gate
		JUMP_GATE_SIZE_MAX_PER_SIDE_BY_TIER = Ints.toArray(jump_gate_size_max_per_side_by_tier.get());
		clampByTier(1, Integer.MAX_VALUE, JUMP_GATE_SIZE_MAX_PER_SIDE_BY_TIER);
		
		// Laser medium
		LASER_MEDIUM_MAX_ENERGY_STORED_BY_TIER = Ints.toArray(laser_medium_max_energy_stored_by_tier.get());
		clampByTier(1, Integer.MAX_VALUE, LASER_MEDIUM_MAX_ENERGY_STORED_BY_TIER);
		LASER_MEDIUM_BONUS_FACTOR_BY_TIER = Doubles.toArray(laser_medium_bonus_factor_by_tier.get());
		clampByTier(0.0D, 4.0D, LASER_MEDIUM_BONUS_FACTOR_BY_TIER);
		
		// Laser cannon (common)
		LASER_CANNON_MAX_MEDIUMS_COUNT = laser_cannon_max_mediums_count.get();
		LASER_CANNON_MAX_LASER_ENERGY = laser_cannon_max_laser_energy.get();
		LASER_CANNON_BOOSTER_BEAM_ENERGY_EFFICIENCY = laser_cannon_booster_beam_energy_efficiency.get();
		LASER_CANNON_RANGE_MAX = laser_cannon_range_max.get();
		
		// Lift
		LIFT_MAX_ENERGY_STORED = lift_max_energy_stored.get();
		LIFT_ENERGY_PER_ENTITY = lift_energy_per_entity.get();
		LIFT_UPDATE_INTERVAL_TICKS = lift_update_interval_ticks.get();
		LIFT_ENTITY_COOLDOWN_TICKS = lift_entity_cooldown_ticks.get();
		
		// Mining Laser (common)
		MINING_LASER_MAX_MEDIUMS_COUNT = mining_laser_max_mediums_count.get();
		MINING_LASER_RADIUS_NO_LASER_MEDIUM = mining_laser_radius_no_laser_medium.get();
		MINING_LASER_RADIUS_PER_LASER_MEDIUM = mining_laser_radius_per_laser_medium.get();
		
		// Radar
		RADAR_MAX_ENERGY_STORED = radar_max_energy_stored.get();
		
		RADAR_SCAN_MIN_ENERGY_COST = radar_energy_cost_min.get();
		RADAR_SCAN_ENERGY_COST_FACTORS = Doubles.toArray(radar_energy_cost_factors.get());
		if (RADAR_SCAN_ENERGY_COST_FACTORS.length != 4) {
			RADAR_SCAN_ENERGY_COST_FACTORS = new double[4];
			Arrays.fill(RADAR_SCAN_ENERGY_COST_FACTORS, 1.0D);
		}
		RADAR_SCAN_MIN_DELAY_SECONDS = radar_scan_min_delay_seconds.get();
		RADAR_SCAN_DELAY_FACTORS_SECONDS = Doubles.toArray(radar_scan_delay_factors_seconds.get());
		if (RADAR_SCAN_DELAY_FACTORS_SECONDS.length != 4) {
			RADAR_SCAN_DELAY_FACTORS_SECONDS = new double[4];
			Arrays.fill(RADAR_SCAN_DELAY_FACTORS_SECONDS, 1.0D);
		}
		
		RADAR_MAX_ISOLATION_RANGE = radar_max_isolation_range.get();
		RADAR_MIN_ISOLATION_BLOCKS = radar_min_isolation_blocks.get();
		RADAR_MAX_ISOLATION_BLOCKS = radar_max_isolation_blocks.get();
		RADAR_MIN_ISOLATION_EFFECT = radar_min_isolation_effect.get();
		RADAR_MAX_ISOLATION_EFFECT = radar_max_isolation_effect.get();
		
		// Ship
		SHIP_MAX_ENERGY_STORED_BY_TIER = Ints.toArray(ship_max_energy_stored_by_tier.get());
		
		SHIP_MASS_MAX_BY_TIER = Ints.toArray(ship_mass_max_by_tier.get());
		clampByTier(1, Integer.MAX_VALUE, SHIP_MASS_MAX_BY_TIER);
		SHIP_MASS_MIN_BY_TIER = Ints.toArray(ship_mass_min_by_tier.get());
		clampByTier(1, Integer.MAX_VALUE, SHIP_MASS_MIN_BY_TIER);
		// (we don't check min < max here, is it really needed?)
		
		SHIP_MASS_MAX_ON_PLANET_SURFACE = ship_mass_max_on_planet_surface.get();
		SHIP_MASS_MIN_FOR_HYPERSPACE = ship_mass_min_for_hyperspace.get();
		
		SHIP_SIZE_MAX_PER_SIDE_BY_TIER = Ints.toArray(ship_size_max_per_side_by_tier.get());
		clampByTier(1, Integer.MAX_VALUE, SHIP_SIZE_MAX_PER_SIDE_BY_TIER);
		
		// Tooltips
		TOOLTIP_ADD_REGISTRY_NAME = tooltip_add_registry_name.get();
		TOOLTIP_ADD_ORE_DICTIONARY_NAME = tooltip_add_ore_dictionary_name.get();
		TOOLTIP_ADD_ARMOR_POINTS = tooltip_add_armor_points.get();
		TOOLTIP_ADD_BLOCK_MATERIAL = tooltip_add_block_material.get();
		TOOLTIP_ADD_BURN_TIME = tooltip_add_burn_time.get();
		TOOLTIP_ADD_DURABILITY = tooltip_add_durability.get();
		TOOLTIP_ADD_ENCHANTABILITY = tooltip_add_enchantability.get();
		TOOLTIP_ADD_ENTITY_ID = tooltip_add_entity_id.get();
		TOOLTIP_ADD_FLAMMABILITY = tooltip_add_flammability.get();
		TOOLTIP_ADD_FLUID = tooltip_add_fluid_stats.get();
		TOOLTIP_ADD_HARDNESS = tooltip_add_hardness.get();
		TOOLTIP_ADD_HARVESTING = tooltip_add_harvesting_stats.get();
		TOOLTIP_ADD_OPACITY = tooltip_add_opacity.get();
		TOOLTIP_ADD_REPAIR_WITH = tooltip_add_repair_material.get();
		
		TOOLTIP_CLEANUP_LIST = tooltip_cleanup_list.get().toArray(new String[0]);
		for (int index = 0; index < TOOLTIP_CLEANUP_LIST.length; index++) {
			final String old = TOOLTIP_CLEANUP_LIST[index];
			TOOLTIP_CLEANUP_LIST[index] = Commons.removeFormatting(old).toLowerCase();
		}
		
		TOOLTIP_ENABLE_DEDUPLICATION = tooltip_enable_deduplication.get();
		
		// Transporter
		TRANSPORTER_MAX_ENERGY_STORED = transporter_max_energy_stored.get();
//		TRANSPORTER_ENERGY_PER_BLOCK = transporter_energy_per_block.get();
//		TRANSPORTER_MAX_BOOST_MUL = transporter_max_boost.get();
		
		// Tree Farm (common)
		TREE_FARM_MAX_MEDIUMS_COUNT = tree_farm_max_mediums_count.get();
		TREE_FARM_MAX_RADIUS_NO_LASER_MEDIUM = tree_farm_max_radius_no_laser_medium.get();
		TREE_FARM_MAX_RADIUS_PER_LASER_MEDIUM = tree_farm_max_radius_per_laser_medium.get();
		TREE_FARM_totalMaxRadius = TREE_FARM_MAX_RADIUS_NO_LASER_MEDIUM + TREE_FARM_MAX_MEDIUMS_COUNT * TREE_FARM_MAX_RADIUS_PER_LASER_MEDIUM;
		
		TREE_FARM_MAX_DISTANCE_NO_LASER_MEDIUM = tree_farm_max_reach_distance_no_laser_medium.get();
		TREE_FARM_MAX_DISTANCE_PER_MEDIUM = tree_farm_max_reach_distance_per_laser_medium.get();
	}
	
	public static void loadServerConfig() {
		// Breathing (server)
		BREATHING_AIR_GENERATION_TICKS = breathing_air_generation_interval_ticks.get();
		BREATHING_VOLUME_UPDATE_DEPTH_BLOCKS = breathing_volume_update_depth_blocks.get();
		BREATHING_AIR_SIMULATION_DELAY_TICKS = breathing_simulation_delay_ticks.get();
		BREATHING_AIR_AT_ENTITY_DEBUG = breathing_enable_air_at_entity_debug.get();
		BREATHING_AIR_TANK_BREATH_DURATION_TICKS = breathing_air_tank_breath_duration_ticks.get();
		BREATHING_AIR_TANK_CAPACITY_BY_TIER = Ints.toArray(breathing_air_tank_capacity_by_tier.get());
		clampByTier(8, 32767, BREATHING_AIR_TANK_CAPACITY_BY_TIER); // Warning: this is hack since we're using a different tier system
		
		// Energy handling
		ENERGY_DISPLAY_UNITS = energy_display_units.get();
		ENERGY_ENABLE_FE = energy_enable_FE.get();
		ENERGY_ENABLE_GTCE_EU = energy_enable_GTCE_EU.get();
		ENERGY_ENABLE_IC2_EU = energy_enable_IC2_EU.get();
		ENERGY_OVERVOLTAGE_SHOCK_FACTOR = energy_overvoltage_shock_factor.get().floatValue();
		ENERGY_OVERVOLTAGE_EXPLOSION_FACTOR = energy_overvoltage_explosion_factor.get().floatValue();
		ENERGY_SCAN_INTERVAL_TICKS = energy_scan_interval_ticks.get();
		
		// Laser cannon (server)
		LASER_CANNON_EMIT_FIRE_DELAY_TICKS = laser_cannon_emit_fire_delay_ticks.get();
		LASER_CANNON_EMIT_SCAN_DELAY_TICKS = laser_cannon_emit_scan_delay_ticks.get();
		
		LASER_CANNON_ENERGY_ATTENUATION_PER_AIR_BLOCK = laser_cannon_energy_attenuation_per_air_block.get();
		LASER_CANNON_ENERGY_ATTENUATION_PER_VOID_BLOCK = laser_cannon_energy_attenuation_per_void_block.get();
		LASER_CANNON_ENERGY_ATTENUATION_PER_BROKEN_BLOCK = laser_cannon_energy_attenuation_per_broken_block.get();
		
		LASER_CANNON_ENTITY_HIT_SET_ON_FIRE_SECONDS = laser_cannon_entity_hit_set_on_fire_seconds.get();
		
		LASER_CANNON_ENTITY_HIT_ENERGY = laser_cannon_entity_hit_energy.get();
		LASER_CANNON_ENTITY_HIT_BASE_DAMAGE = laser_cannon_entity_hit_base_damage.get();
		LASER_CANNON_ENTITY_HIT_ENERGY_PER_DAMAGE = Commons.clamp(0, LASER_CANNON_MAX_LASER_ENERGY, laser_cannon_entity_hit_energy_per_damage.get());
		LASER_CANNON_ENTITY_HIT_MAX_DAMAGE = laser_cannon_entity_hit_max_damage.get();
		
		LASER_CANNON_ENTITY_HIT_EXPLOSION_ENERGY_THRESHOLD = laser_cannon_entity_hit_energy_threshold_for_explosion.get();
		LASER_CANNON_ENTITY_HIT_EXPLOSION_BASE_STRENGTH = laser_cannon_entity_hit_explosion_base_strength.get().floatValue();
		LASER_CANNON_ENTITY_HIT_EXPLOSION_ENERGY_PER_STRENGTH = laser_cannon_entity_hit_explosion_energy_per_strength.get();
		LASER_CANNON_ENTITY_HIT_EXPLOSION_MAX_STRENGTH = laser_cannon_entity_hit_explosion_max_strength.get().floatValue();
		
		LASER_CANNON_BLOCK_HIT_ENERGY_MIN = laser_cannon_block_hit_energy_min.get();
		LASER_CANNON_BLOCK_HIT_ENERGY_PER_BLOCK_HARDNESS = laser_cannon_block_hit_energy_per_block_hardness.get();
		LASER_CANNON_BLOCK_HIT_ENERGY_MAX = laser_cannon_block_hit_energy_max.get();
		LASER_CANNON_BLOCK_HIT_ABSORPTION_PER_BLOCK_HARDNESS = laser_cannon_block_hit_absorption_per_block_hardness.get();
		LASER_CANNON_BLOCK_HIT_ABSORPTION_MAX = laser_cannon_block_hit_absorption_max.get();
		
		LASER_CANNON_BLOCK_HIT_EXPLOSION_HARDNESS_THRESHOLD = laser_cannon_block_hit_explosion_hardness_threshold.get().floatValue();
		LASER_CANNON_BLOCK_HIT_EXPLOSION_BASE_STRENGTH = laser_cannon_block_hit_explosion_base_strength.get().floatValue();
		LASER_CANNON_BLOCK_HIT_EXPLOSION_ENERGY_PER_STRENGTH = laser_cannon_block_hit_explosion_energy_per_strength.get();
		LASER_CANNON_BLOCK_HIT_EXPLOSION_MAX_STRENGTH = laser_cannon_block_hit_explosion_max_strength.get().floatValue();
		
		// Logging
		LOGGING_THROTTLE_MS = logging_throttle_ms.get();
		
		LOGGING_ACCELERATOR = logging_enable_accelerator_logs.get();
		LOGGING_BREAK_PLACE = logging_enable_break_place_logs.get();
		LOGGING_BREATHING = logging_enable_breathing_logs.get();
		LOGGING_BUILDING = logging_enable_building_logs.get();
		LOGGING_CAMERA = logging_enable_camera_logs.get();
		LOGGING_CHUNK_HANDLER = logging_enable_chunk_handler_logs.get();
		LOGGING_CHUNK_LOADING = logging_enable_chunk_loading_logs.get();
		LOGGING_CHUNK_RELOADING = logging_enable_chunk_reloading_logs.get();
		LOGGING_COLLECTION = logging_enable_collection_logs.get();
		LOGGING_DICTIONARY = logging_enable_dictionary_logs.get();
		LOGGING_ENERGY = logging_enable_energy_logs.get();
		LOGGING_ENTITY_FX = logging_enable_entity_fx_logs.get();
		LOGGING_FORCE_FIELD = logging_enable_force_field_logs.get();
		LOGGING_FORCE_FIELD_REGISTRY = logging_enable_force_field_registry_logs.get();
		LOGGING_GLOBAL_REGION_REGISTRY = logging_enable_global_region_registry_logs.get();
		LOGGING_GRAVITY = logging_enable_gravity_logs.get();
		LOGGING_JUMP = logging_enable_jump_logs.get();
		LOGGING_JUMPBLOCKS = logging_enable_jumpblocks_logs.get();
		LOGGING_LUA = logging_enable_LUA_logs.get();
		LOGGING_OFFLINE_AVATAR = logging_enable_offline_avatar_logs.get();
		LOGGING_PROFILING_CPU_USAGE = logging_enable_profiling_CPU_time.get();
		LOGGING_PROFILING_MEMORY_ALLOCATION = logging_enable_profiling_memory_allocation.get();
		LOGGING_PROFILING_THREAD_SAFETY = logging_enable_profiling_thread_safety.get();
		LOGGING_RADAR = logging_enable_radar_logs.get();
		LOGGING_RENDERING = logging_enable_rendering_logs.get();
		LOGGING_TRANSPORTER = logging_enable_transporter_logs.get();
		LOGGING_WEAPON = logging_enable_weapon_logs.get();
		LOGGING_WORLD_GENERATION = logging_enable_world_generation_logs.get();
		LOGGING_XML_PREPROCESSOR = logging_enable_XML_preprocessor_logs.get();
		
		if (WarpDrive.isDev) {// disabled in production, for obvious reasons :)
			LOGGING_CLIENT_SYNCHRONIZATION = logging_enable_client_synchronization_logs.get();
			LOGGING_CLOAKING = logging_enable_cloaking_logs.get();
			LOGGING_EFFECTS = logging_enable_effects_logs.get();
			LOGGING_TARGETING = logging_enable_targeting_logs.get();
			LOGGING_VIDEO_CHANNEL = logging_enable_videoChannel_logs.get();
		} else {
			LOGGING_CLIENT_SYNCHRONIZATION = false;
			LOGGING_CLOAKING = false;
			LOGGING_EFFECTS = false;
			LOGGING_TARGETING = false;
			LOGGING_VIDEO_CHANNEL = false;
		}
		
		// Mining Laser (server)
		MINING_LASER_WARMUP_DELAY_TICKS = mining_laser_warmup_delay_ticks.get();
		MINING_LASER_SCAN_DELAY_TICKS = mining_laser_scan_delay_ticks.get();
		MINING_LASER_MINE_DELAY_TICKS = mining_laser_mine_delay_ticks.get();
		MINING_LASER_SCAN_ENERGY_PER_LAYER_IN_ATMOSPHERE = mining_laser_scan_energy_per_layer_in_atmosphere.get();
		MINING_LASER_MINE_ENERGY_PER_BLOCK_IN_ATMOSPHERE = mining_laser_mine_energy_per_block_in_atmosphere.get();
		MINING_LASER_SCAN_ENERGY_PER_LAYER_IN_VOID = mining_laser_scan_energy_per_layer_in_void.get();
		MINING_LASER_MINE_ENERGY_PER_BLOCK_IN_VOID = mining_laser_mine_energy_per_block_in_void.get();
		MINING_LASER_MINE_ORES_ONLY_ENERGY_FACTOR = mining_laser_mine_ores_only_energy_factor.get();
		MINING_LASER_MINE_SILKTOUCH_ENERGY_FACTOR = mining_laser_mine_silktouch_energy_factor.get();
		
		if (unused) {
			MINING_LASER_MINE_SILKTOUCH_DEUTERIUM_MB = mining_laser_mine_silktouch_deuterium_mB.get();
			if (MINING_LASER_MINE_SILKTOUCH_DEUTERIUM_MB < 1) {
				MINING_LASER_MINE_SILKTOUCH_DEUTERIUM_MB = 0;
			}
			MINING_LASER_MINE_FORTUNE_ENERGY_FACTOR = mining_laser_fortune_energy_factor.get();
		}
		
		// Offline avatar
		OFFLINE_AVATAR_ENABLE = offline_avatar_enable.get();
		OFFLINE_AVATAR_CREATE_ONLY_ABOARD_SHIPS = offline_avatar_create_only_aboard_ships.get();
		OFFLINE_AVATAR_FORGET_ON_DEATH = offline_avatar_forget_on_death.get();
		OFFLINE_AVATAR_MODEL_SCALE = offline_avatar_model_scale.get().floatValue();
		OFFLINE_AVATAR_ALWAYS_RENDER_NAME_TAG = offline_avatar_always_render_name_tag.get();
		OFFLINE_AVATAR_MIN_RANGE_FOR_REMOVAL =  offline_avatar_min_range_for_removal.get().floatValue();
		OFFLINE_AVATAR_MAX_RANGE_FOR_REMOVAL = (float) Commons.clamp(Math.max(3.00D, OFFLINE_AVATAR_MIN_RANGE_FOR_REMOVAL), Float.MAX_VALUE,
		                                                             offline_avatar_max_range_for_removal.get().floatValue() );
		if (OFFLINE_AVATAR_MAX_RANGE_FOR_REMOVAL != offline_avatar_max_range_for_removal.get().floatValue()) {
			offline_avatar_max_range_for_removal.set((double) OFFLINE_AVATAR_MAX_RANGE_FOR_REMOVAL);
		}
		OFFLINE_AVATAR_DELAY_FOR_REMOVAL_SECONDS = offline_avatar_delay_for_removal_s.get();
		OFFLINE_AVATAR_DELAY_FOR_REMOVAL_TICKS = OFFLINE_AVATAR_DELAY_FOR_REMOVAL_SECONDS * 20;
		
		// Ship
		SHIP_COLLISION_TOLERANCE_BLOCKS = ship_collision_tolerance_blocks.get();
		SHIP_MASS_UNLIMITED_PLAYER_NAMES = ship_mass_unlimited_player_names.get().toArray(new String[0]);
		SHIP_VOLUME_SCAN_BLOCKS_PER_TICK = ship_volume_scan_blocks_per_tick.get();
		SHIP_VOLUME_SCAN_AGE_TOLERANCE_SECONDS = ship_volume_scan_age_tolerance.get();
		SHIP_WARMUP_RANDOM_TICKS = ship_warmup_random_ticks.get();
		
		// Ship Scanner
		SS_MAX_DEPLOY_RADIUS_BLOCKS = ship_scanner_max_deploy_radius_blocks.get();
		SS_SEARCH_INTERVAL_TICKS = ship_scanner_search_interval_ticks.get();
		SS_SCAN_BLOCKS_PER_SECOND = ship_scanner_scan_blocks_per_second.get();
		SS_DEPLOY_BLOCKS_PER_INTERVAL = ship_scanner_deploy_blocks_per_interval.get();
		SS_DEPLOY_INTERVAL_TICKS = ship_scanner_deploy_interval_ticks.get();
		
		// Ship movement costs
		for (final EnumShipMovementType shipMovementType : EnumShipMovementType.values()) {
			if (shipMovementType.hasConfiguration) {
				SHIP_MOVEMENT_COSTS_FACTORS[shipMovementType.ordinal()].load();
			}
		}
		
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
		Dictionary.loadConfig(fileConfigDirectory);
	}
	
	public static void loadDataFixer(final File file) {
		WarpDriveDataFixer.loadConfig(fileConfigDirectory);
	}
	
	public static void registerBlockTransformer(final String modId, final IBlockTransformer blockTransformer) {
		blockTransformers.put(modId, blockTransformer);
		WarpDrive.logger.info(modId + " blockTransformer registered");
	}
	
	public static void onFMLCommonSetup() {
		/* TODO MC1.15 compatibility classes
		CompatWarpDrive.register();
		
		// apply compatibility modules
		final boolean isAppliedEnergistics2Loaded = ModList.get().isLoaded("appliedenergistics2");
		if (isAppliedEnergistics2Loaded) {
			CompatAppliedEnergistics2.register();
		}
		
		final boolean isActuallyAdditionsLoaded = ModList.get().isLoaded("actuallyadditions");
		if (isActuallyAdditionsLoaded) {
			CompatActuallyAdditions.register();
		}
		
		if (isComputerCraftLoaded) {
			CompatComputerCraft.register();
		}
		
		if (isEnderIOLoaded) {
			CompatEnderIO.register();
		}
		
		if (isForgeMultipartLoaded) {
			isForgeMultipartLoaded = CompatForgeMultipart.register();
		}
		
		final boolean isImmersiveEngineeringLoaded = ModList.get().isLoaded("immersiveengineering");
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
		
		final boolean isBotaniaLoaded = ModList.get().isLoaded("botania");
		if (isBotaniaLoaded) {
			CompatBotania.register();
		}
		
		final boolean isBiblioCraftLoaded = ModList.get().isLoaded("bibliocraft");
		if (isBiblioCraftLoaded) {
			CompatBiblioCraft.register();
		}
		
		final boolean isBlockcrafteryLoaded = ModList.get().isLoaded("blockcraftery");
		if (isBlockcrafteryLoaded) {
			CompatBlockcraftery.register();
		}
		
		final boolean isBuildCraftLoaded = ModList.get().isLoaded("buildcraftcore");
		if (isBuildCraftLoaded) {
			CompatBuildCraft.register();
		}
		
		final boolean isCustomNPCsLoaded = ModList.get().isLoaded("customnpcs");
		if (isCustomNPCsLoaded) {
			CompatCustomNPCs.register();
		}
		
		final boolean isDecocraftLoaded = ModList.get().isLoaded("props");
		if (isDecocraftLoaded) {
			CompatDecocraft.register();
		}
		
		final boolean isDeepResonanceLoaded = ModList.get().isLoaded("deepresonance");
		if (isDeepResonanceLoaded) {
			CompatDeepResonance.register();
		}
		
		final boolean isDraconicEvolutionLoaded = ModList.get().isLoaded("draconicevolution");
		if (isDraconicEvolutionLoaded) {
			CompatDraconicEvolution.register();
		}
		
		final boolean isEmbersLoaded = ModList.get().isLoaded("embers");
		if (isEmbersLoaded) {
			CompatEmbers.register();
		}
		
		final boolean isEnvironmentalTechLoaded = ModList.get().isLoaded("environmentaltech");
		if (isEnvironmentalTechLoaded) {
			CompatEnvironmentalTech.register();
		}
		
		final boolean isExtraUtilities2Loaded = ModList.get().isLoaded("extrautils2");
		if (isExtraUtilities2Loaded) {
			CompatExtraUtilities2.register();
		}
		
		final boolean isEvilCraftLoaded = ModList.get().isLoaded("evilcraft");
		if (isEvilCraftLoaded) {
			CompatEvilCraft.register();
		}
		
		final boolean isGalacticraftCoreLoaded = ModList.get().isLoaded("galacticraftcore");
		if (isGalacticraftCoreLoaded) {
			CompatGalacticraft.register();
		}
		
		// final boolean isGregTechLoaded = ModList.get().isLoaded("gregtech");
		if (isGregtechLoaded) {
			CompatGregTech.register();
		}
		
		final boolean isIndustrialForegoingLoaded = ModList.get().isLoaded("industrialforegoing");
		if (isIndustrialForegoingLoaded) {
			CompatIndustrialForegoing.register();
		}
		
		final boolean isIronChestLoaded = ModList.get().isLoaded("ironchest");
		if (isIronChestLoaded) {
			CompatIronChest.register();
		}
		
		final boolean isMekanismLoaded = ModList.get().isLoaded("mekanism");
		if (isMekanismLoaded) {
			CompatMekanism.register();
		}
		
		final boolean isMetalChestsLoaded = ModList.get().isLoaded("metalchests");
		if (isMetalChestsLoaded) {
			CompatMetalChests.register();
		}
		
		final boolean isMysticalAgricultureLoaded = ModList.get().isLoaded("mysticalagriculture");
		if (isMysticalAgricultureLoaded) {
			CompatMysticalAgriculture.register();
		}
		
		final boolean isNaturaLoaded = ModList.get().isLoaded("natura");
		if (isNaturaLoaded) {
			CompatNatura.register();
		}
		
		final boolean isPneumaticCraftLoaded = ModList.get().isLoaded("pneumaticcraft");
		if (isPneumaticCraftLoaded) {
			CompatPneumaticCraft.register();
		}
		
		final boolean isRootsLoaded = ModList.get().isLoaded("roots");
		if (isRootsLoaded) {
			CompatRoots.register();
		}
		
		final boolean isRusticLoaded = ModList.get().isLoaded("rustic");
		if (isRusticLoaded) {
			CompatRustic.register();
		}
		
		final boolean isRedstonePasteLoaded = ModList.get().isLoaded("redstonepaste");
		if (isRedstonePasteLoaded) {
			CompatRedstonePaste.register();
		}
		
		final boolean isRealFilingCabinetLoaded = ModList.get().isLoaded("realfilingcabinet");
		if (isRealFilingCabinetLoaded) {
			CompatRealFilingCabinet.register();
		}
		
		final boolean isRefinedStorageLoaded = ModList.get().isLoaded("refinedstorage");
		if (isRefinedStorageLoaded) {
			CompatRefinedStorage.register();
		}
		
		final boolean isSGCraftLoaded = ModList.get().isLoaded("sgcraft");
		if (isSGCraftLoaded) {
			CompatSGCraft.register();
		}
		
		final boolean isStorageDrawersLoaded = ModList.get().isLoaded("storagedrawers");
		if (isStorageDrawersLoaded) {
			CompatStorageDrawers.register();
		}
		
		final boolean isTConstructLoaded = ModList.get().isLoaded("tconstruct");
		if (isTConstructLoaded) {
			CompatTConstruct.register();
		}
		
		final boolean isTechgunsLoaded = ModList.get().isLoaded("techguns");
		if (isTechgunsLoaded) {
			CompatTechguns.register();
		}
		
		final boolean isThaumcraftLoaded = ModList.get().isLoaded("thaumcraft");
		if (isThaumcraftLoaded) {
			CompatThaumcraft.register();
		}
		
		final boolean isThermalDynamicsLoaded = ModList.get().isLoaded("thermaldynamics");
		if (isThermalDynamicsLoaded) {
			CompatThermalDynamics.register();
		}
		
		final boolean isUndergroundBiomesLoaded = ModList.get().isLoaded("undergroundbiomes");
		if (isUndergroundBiomesLoaded) {
			CompatUndergroundBiomes.register();
		}
		
		final boolean isVariedCommoditiesLoaded = ModList.get().isLoaded("variedcommodities");
		if (isVariedCommoditiesLoaded) {
			CompatVariedCommodities.register();
		}
		
		final boolean isWootloaded = ModList.get().isLoaded("woot");
		if (isWootloaded) {
			CompatWoot.register();
		}
		*/
		
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
		final String resourceName = "data/" + WarpDrive.MODID + "/" + resourcePathSource + "/" + filename;
		
		final File destination = new File(folderTarget, filename);
		
		try {
			final InputStream inputStream = WarpDrive.class.getClassLoader().getResourceAsStream(resourceName);
			final BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(destination));
			
			assert inputStream != null;
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