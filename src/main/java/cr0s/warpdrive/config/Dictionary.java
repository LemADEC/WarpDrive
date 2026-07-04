package cr0s.warpdrive.config;

import cr0s.warpdrive.compat.CompatMatterOverdrive;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.BlockAbstractBase;
import cr0s.warpdrive.block.BlockAbstractContainer;
import cr0s.warpdrive.block.forcefield.BlockForceField;
import cr0s.warpdrive.block.hull.BlockHullGlass;
import cr0s.warpdrive.block.hull.BlockHullStairs;
import cr0s.warpdrive.world.FakeWorld;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.registries.IForgeRegistryEntry;

public class Dictionary {
	
	// Tagged blocks and entities (loaded from configuration file at PreInit, parsed at PostInit)
	private static HashMap<String, String> taggedBlocks = null;
	private static HashMap<String, String> taggedEntities = null;
	private static HashMap<String, String> taggedItems = null;
	
	// Blocks dictionary
	public static HashSet<Block> BLOCKS_ORES = null;
	private static HashSet<Block> BLOCKS_SOILS = null;
	private static HashSet<Block> BLOCKS_LOGS = null;
	private static HashSet<Block> BLOCKS_STACKING_PLANTS = null;
	private static HashSet<Block> BLOCKS_LEAVES = null;
	public static HashSet<Block> BLOCKS_ANCHOR = null;
	public static HashSet<Block> BLOCKS_NOMASS = null;
	public static HashSet<Block> BLOCKS_LEFTBEHIND = null;
	public static HashSet<Block> BLOCKS_EXPANDABLE = null;
	public static HashSet<Block> BLOCKS_MINING = null;
	public static HashSet<Block> BLOCKS_SKIPMINING = null;
	public static HashSet<Block> BLOCKS_STOPMINING = null;
	public static HashMap<Block, Integer> BLOCKS_PLACE = null;
	public static HashSet<Block> BLOCKS_NOCAMOUFLAGE = null;
	public static HashSet<Block> BLOCKS_NOBLINK = null;
	
	private static HashSet<Block> cache_blocks_logsAndStackings = null;
	private static HashSet<Block> cache_blocks_logsLeavesAndStackings = null;
	
	// Entities dictionary
	private static HashSet<ResourceLocation> ENTITIES_ANCHOR = null;
	private static HashSet<ResourceLocation> ENTITIES_NOMASS = null;
	private static HashSet<ResourceLocation> ENTITIES_LEFTBEHIND = null;
	private static HashSet<ResourceLocation> ENTITIES_NONLIVINGTARGET = null;
	private static HashSet<ResourceLocation> ENTITIES_LIVING_WITHOUT_AIR = null;
	private static HashSet<ResourceLocation> ENTITIES_NO_REVEAL = null;
	
	// Items dictionary
	public static HashSet<Item> ITEMS_FLYINSPACE = null;
	public static HashSet<Item> ITEMS_NOFALLDAMAGE = null;
	public static HashSet<Item> ITEMS_BREATHING_HELMET = null;
	public static HashSet<Item> ITEMS_EXCLUDED_AVATAR = null;
	
	public static void loadConfig(@Nonnull final Configuration config) {
		
		// Block dictionary
		{
			config.addCustomCategoryComment("block_tags",
					  "Use this section to enable special behavior on blocks using tags.\n"
					+ "Most blocks are already supported automatically. Only modify this section when something doesn't work!\n" + "\n"
					+ "Tags shall be separated by at least one space, comma or tabulation.\n"
					+ "Invalid tags will be ignored silently. Tags and block names are case sensitive.\n"
					+ "In case of conflicts, the latest tag overwrite the previous ones.\n"
					+ "- Soil: this block is a soil for plants (default: dirt, farmland, grass, sand & soul sand).\n"
					+ "- Log: this block is harvestable as a wood log (default: all 'log*', '*log' & '*logs' blocks from the ore dictionary).\n"
					+ "- Leaf: this block is harvestable as a leaf (default: all 'leave*', '*leave' & '*leaves' blocks from the ore dictionary).\n"
					+ "- StackingPlant: this block is harvestable as a reed or cactus (i.e. base block is kept).\n"
					+ "- Anchor: ship can't move with this block aboard (default: bedrock and assimilated).\n"
					+ "- NoMass: this block doesn't count when calculating ship volume/mass (default: leaves, all 'air' blocks).\n"
					+ "- LeftBehind: this block won't move with your ship (default: RailCraft heat, WarpDrive gases).\n"
					+ "- Expandable: this block will be squished/ignored in case of collision.\n"
					+ "- Mining: this block is mineable (default: all 'ore' blocks from the ore dictionary).\n"
					+ "- SkipMining: this block is ignored from mining (default: bedrock).\n"
					+ "- StopMining: this block will prevent mining through it (default: command/creative, force fields).\n"
					+ "- PlaceEarliest: this block will be removed last and placed first (default: ship hull and projectors).\n"
					+ "- PlaceEarlier: this block will be placed fairly soon (default: force field blocks).\n"
					+ "- PlaceNormal: this block will be removed and placed with non-tile entities.\n"
					+ "- PlaceLater: this block will be placed fairly late (default: IC2 Reactor core).\n"
					+ "- PlaceLatest: this block will be removed first and placed last (default: IC2 Reactor chamber).\n"
					+ "- NoCamouflage: this block isn't valid for camouflage.\n"
					+ "- NoBlink: this block will prevent teleportation through it (default: bedrock, force fields)");
			
			final ConfigCategory categoryBlockTags = config.getCategory("block_tags");
			
			// *** enforce default values
			// farming
			config.get("block_tags", "minecraft:dirt"                                       , "Soil").getString();
			config.get("block_tags", "minecraft:farmland"                                   , "Soil").getString();
			config.get("block_tags", "minecraft:grass"                                      , "Soil").getString();
			config.get("block_tags", "minecraft:mycelium"                                   , "Soil").getString();
			config.get("block_tags", "minecraft:sand"                                       , "Soil").getString();
			config.get("block_tags", "minecraft:soul_sand"                                  , "Soil").getString();
			config.get("block_tags", "minecraft:melon_block"                                , "Log").getString();
			config.get("block_tags", "minecraft:pumpkin"                                    , "Log").getString();
			config.get("block_tags", "minecraft:reeds"                                      , "StackingPlant").getString();
			config.get("block_tags", "minecraft:cactus"                                     , "StackingPlant").getString();
			
			config.get("block_tags", "ic2:rubber_wood"                                      , "Log").getString(); // IC2 Experimental
			config.get("block_tags", "ic2:blockrubwood"                                     , "Log").getString(); // IC2 Classic
			config.get("block_tags", "tconstruct:slime_congealed"                           , "Log").getString();
			config.get("block_tags", "tconstruct:slime_leaves"                              , "Leaf").getString();
			
			// anchors
			config.get("block_tags", "minecraft:barrier"                                    , "Anchor SkipMining").getString(); // creative
			config.get("block_tags", "minecraft:bedrock"                                    , "Anchor SkipMining NoBlink").getString(); // creative
			config.get("block_tags", "minecraft:chain_command_block"                        , "Anchor StopMining").getString(); // creative
			config.get("block_tags", "minecraft:command_block"                              , "Anchor StopMining").getString(); // creative
			config.get("block_tags", "minecraft:end_gateway"                                , "Anchor StopMining").getString(); // portal
			config.get("block_tags", "minecraft:end_portal_frame"                           , "Anchor StopMining").getString(); // portal
			config.get("block_tags", "minecraft:end_portal"                                 , "Anchor StopMining").getString(); // portal
			config.get("block_tags", "minecraft:portal"                                     , "Anchor StopMining").getString(); // portal
			config.get("block_tags", "minecraft:repeating_command_block"                    , "Anchor StopMining").getString(); // creative
			config.get("block_tags", "minecraft:structure_block"                            , "Anchor StopMining").getString(); // creative
			
			config.get("block_tags", "actuallyadditions:block_laser_relay"                  , "Anchor").getString(); // network is saved outside the map, not supported
			config.get("block_tags", "actuallyadditions:block_laser_relay_advanced"         , "Anchor").getString(); // network is saved outside the map, not supported
			config.get("block_tags", "actuallyadditions:block_laser_relay_extreme"          , "Anchor").getString(); // network is saved outside the map, not supported
			config.get("block_tags", "actuallyadditions:block_laser_relay_fluids"           , "Anchor").getString(); // network is saved outside the map, not supported
			config.get("block_tags", "actuallyadditions:block_laser_relay_item"             , "Anchor").getString(); // network is saved outside the map, not supported
			config.get("block_tags", "actuallyadditions:block_laser_relay_item_whitelist"   , "Anchor").getString(); // network is saved outside the map, not supported
			config.get("block_tags", "appliedenergistics2:matrix_frame"                     , "Anchor StopMining").getString(); // creative
			config.get("block_tags", "aroma1997sdimension:miningportal"                     , "Anchor StopMining").getString(); // portal
			config.get("block_tags", "astralsorcery:blockcelestialcollectorcrystal"         , "Anchor SkipMining").getString(); // lore
			config.get("block_tags", "astralsorcery:blockcollectorcrystal"                  , "Anchor SkipMining").getString(); // lore
			config.get("block_tags", "bloodmagic:dimensional_portal"                        , "Anchor StopMining").getString(); // portal
			config.get("block_tags", "computercraft:command_computer"                       , "Anchor SkipMining").getString(); // creative
			config.get("block_tags", "darkutils:sneaky_bedrock"                             , "Anchor SkipMining NoBlink").getString(); // creative
			config.get("block_tags", "draconicevolution:chaos_crystal"                      , "Anchor SkipMining").getString(); // static
			config.get("block_tags", "draconicevolution:portal"                             , "Anchor StopMining").getString(); // lore
			config.get("block_tags", "enderio:block_creative_spawner"                       , "Anchor SkipMining").getString(); // creative
			config.get("block_tags", "enderio:block_killer_joe"                             , "Anchor").getString(); // high blast resistance
			config.get("block_tags", "enderio:block_enhanced_alloy_smelter_top"             , "Anchor SkipMining").getString(); // high blast resistance
			config.get("block_tags", "enderio:block_enhanced_combustion_generator_top"      , "Anchor SkipMining").getString(); // high blast resistance
			config.get("block_tags", "enderio:block_enhanced_sag_mill_top"                  , "Anchor SkipMining").getString(); // high blast resistance
			config.get("block_tags", "enderio:block_enhanced_vat_top"                       , "Anchor SkipMining").getString(); // high blast resistance
			config.get("block_tags", "enderio:block_enhanced_wired_charger_top"             , "Anchor SkipMining").getString(); // high blast resistance
			config.get("block_tags", "enderio:block_transceiver"                            , "Anchor").getString(); // Dimensional transceiver is not supported
			config.get("block_tags", "extrautils2:creativechest"                            , "Anchor SkipMining").getString(); // creative
			config.get("block_tags", "extrautils2:creativeenergy"                           , "Anchor SkipMining").getString(); // creative
			config.get("block_tags", "extrautils2:creativeharvest"                          , "Anchor SkipMining").getString(); // creative
			config.get("block_tags", "extrautils2:decorativebedrock"                        , "Anchor SkipMining").getString(); // creative
			config.get("block_tags", "extrautils2:spike_creative"                           , "Anchor SkipMining").getString(); // creative
			// @TODO MC1.12+ config.get("block_tags", "ic2:te"                                    , "Anchor SkipMining").getString(); // IC2 Experimental personal chest, need property filtering
			config.get("block_tags", "ic2:blockpersonal"                                    , "Anchor SkipMining").getString(); // IC2 Classic
			config.get("block_tags", "galacticraftcore:arclamp"                             , "Anchor").getString(); // includes hard links to its fake air lights
			config.get("block_tags", "galacticraftcore:boss_spawner"                        , "Anchor StopMining").getString(); // includes hard links to the boss room
			config.get("block_tags", "galacticraftcore:grating"                             , "Anchor StopMining").getString(); // unable to break block without spamming console
			config.get("block_tags", "galacticraftcore:grating1"                            , "Anchor StopMining").getString(); // unable to break block without spamming console
			config.get("block_tags", "galacticraftcore:grating2"                            , "Anchor StopMining").getString(); // unable to break block without spamming console
			config.get("block_tags", "galacticraftcore:grating3"                            , "Anchor StopMining").getString(); // unable to break block without spamming console
			config.get("block_tags", "galacticraftcore:grating4"                            , "Anchor StopMining").getString(); // unable to break block without spamming console
			config.get("block_tags", "galacticraftcore:grating5"                            , "Anchor StopMining").getString(); // unable to break block without spamming console
			config.get("block_tags", "galacticraftcore:grating6"                            , "Anchor StopMining").getString(); // unable to break block without spamming console
			config.get("block_tags", "galacticraftcore:grating7"                            , "Anchor StopMining").getString(); // unable to break block without spamming console
			config.get("block_tags", "galacticraftcore:grating8"                            , "Anchor StopMining").getString(); // unable to break block without spamming console
			config.get("block_tags", "galacticraftcore:grating9"                            , "Anchor StopMining").getString(); // unable to break block without spamming console
			config.get("block_tags", "galacticraftcore:grating10"                           , "Anchor StopMining").getString(); // unable to break block without spamming console
			config.get("block_tags", "galacticraftcore:grating11"                           , "Anchor StopMining").getString(); // unable to break block without spamming console
			config.get("block_tags", "galacticraftcore:grating12"                           , "Anchor StopMining").getString(); // unable to break block without spamming console
			config.get("block_tags", "galacticraftcore:grating13"                           , "Anchor StopMining").getString(); // unable to break block without spamming console
			config.get("block_tags", "galacticraftcore:grating14"                           , "Anchor StopMining").getString(); // unable to break block without spamming console
			config.get("block_tags", "galacticraftcore:space_station_base"                  , "Anchor StopMining").getString(); // unbreakable
			config.get("block_tags", "galacticraftcore:telemetry"                           , "Anchor").getString(); // can't make it work, is it fully implemented yet?
			config.get("block_tags", "galacticraftcore:view_screen"                         , "Anchor StopMining").getString(); // unable to break block without spamming console
			config.get("block_tags", "galacticraftplanets:beam_receiver"                    , "Anchor StopMining").getString(); // spamming console during jump
			config.get("block_tags", "galacticraftplanets:boss_spawner_mars"                , "Anchor StopMining").getString(); // includes hard links to the boss room
			config.get("block_tags", "galacticraftplanets:boss_spawner_venus"               , "Anchor StopMining").getString(); // includes hard links to the boss room
			config.get("block_tags", "galacticraftplanets:miner_base"                       , "Anchor StopMining").getString(); // too complex, no reason to support
			config.get("block_tags", "galacticraftplanets:miner_base_full"                  , "Anchor StopMining").getString(); // too complex, no reason to support
			config.get("block_tags", "galacticraftplanets:telepad_fake"                     , "PlaceLater").getString(); // glitch after movement (no more collision?)
			config.get("block_tags", "galacticraftplanets:telepad_short"                    , "PlaceNormal").getString(); // glitch after movement (no more collision?)
			config.get("block_tags", "malisisdoors:forcefielddoor"                          , "Anchor NoBlink").getString(); // creative
			config.get("block_tags", "malisisdoors:rustyhatch"                              , "Anchor").getString(); // random crashes on 1.7.10
			config.get("block_tags", "nex:nether_portal"                                    , "Anchor StopMining").getString();
			config.get("block_tags", "projectred-relocation:blockmovingrow"                 , "Anchor StopMining NoMass").getString(); // moving block
			config.get("block_tags", "randomthings:spectreblock"                            , "Anchor StopMining NoBlink").getString();
			config.get("block_tags", "rangedpumps:pump"                                     , "Anchor").getString(); // need to reset its search area after each jump
			config.get("block_tags", "rustic:stake_tied"                                    , "Anchor").getString();
			config.get("block_tags", "storagedrawers:framingtable"                          , "Anchor").getString(); // invalid metadata conversion, see https://github.com/jaquadro/StorageDrawers/issues/683
			config.get("block_tags", "warpdrive:bedrock_glass"                              , "Anchor StopMining NoBlink").getString(); // creative
			config.get("block_tags", "rftools:camo_shield_block"                            , "Anchor StopMining NoBlink").getString();
			config.get("block_tags", "rftools:camo_shield_block_opaque"                     , "Anchor StopMining NoBlink").getString();
			config.get("block_tags", "rftools:invisible_shield_block"                       , "Anchor StopMining NoBlink").getString();
			config.get("block_tags", "rftools:invisible_shield_block_opaque"                , "Anchor StopMining NoBlink").getString();
			config.get("block_tags", "rftools:notick_camo_shield_block"                     , "Anchor StopMining NoBlink").getString();
			config.get("block_tags", "rftools:notick_camo_shield_block_opaque"              , "Anchor StopMining NoBlink").getString();
			config.get("block_tags", "rftools:notick_invisible_shield_block"                , "Anchor StopMining NoBlink").getString();
			config.get("block_tags", "rftools:notick_invisible_shield_block_opaque"         , "Anchor StopMining NoBlink").getString();
			config.get("block_tags", "rftools:notick_solid_shield_block"                    , "Anchor StopMining NoBlink").getString();
			config.get("block_tags", "rftools:notick_solid_shield_block_opaque"             , "Anchor StopMining NoBlink").getString();
			config.get("block_tags", "rftools:solid_shield_block"                           , "Anchor StopMining NoBlink").getString();
			config.get("block_tags", "rftools:solid_shield_block_opaque"                    , "Anchor StopMining NoBlink").getString();
			config.get("block_tags", "techguns:orecluster"                                  , "Anchor SkipMining").getString(); // localized strategic resource
			config.get("block_tags", "techguns:tg_spawner"                                  , "Anchor").getString(); // don't move dungeons around
			config.get("block_tags", "thaumcraft:hole"                                      , "Anchor StopMining").getString(); // lore
			config.get("block_tags", "thaumcraft:stone_ancient_doorway"                     , "Anchor StopMining NoBlink").getString(); // lore
			config.get("block_tags", "thaumcraft:stone_ancient_rock"                        , "Anchor StopMining NoBlink").getString(); // lore
			config.get("block_tags", "tombstone:grave_cross"                                , "Anchor SkipMining").getString(); // gravestone
			config.get("block_tags", "tombstone:grave_normal"                               , "Anchor SkipMining").getString(); // gravestone
			config.get("block_tags", "tombstone:grave_simple"                               , "Anchor SkipMining").getString(); // gravestone
			config.get("block_tags", "tombstone:tombstone"                                  , "Anchor SkipMining").getString(); // gravestone
			config.get("block_tags", "twilightforest:boss_spawner"                          , "Anchor SkipMining").getString();
			config.get("block_tags", "twilightforest:force_field"                           , "Anchor StopMining NoBlink").getString();
			config.get("block_tags", "twilightforest:stronghold_shield"                     , "Anchor SkipMining NoBlink").getString();
			config.get("block_tags", "twilightforest:twilight_portal"                       , "Anchor StopMining").getString(); // portal
			
			// placement priorities
			config.get("block_tags", "minecraft:lever"                                 , "PlaceLatest").getString();
			config.get("block_tags", "minecraft:obsidian"                              , "PlaceEarliest Mining").getString();
			config.get("block_tags", "minecraft:redstone_torch"                        , "PlaceLatest Mining").getString();
			config.get("block_tags", "minecraft:torch"                                 , "PlaceLatest Mining").getString();
			config.get("block_tags", "minecraft:unlit_redstone_torch"                  , "PlaceLatest Mining").getString();
			config.get("block_tags", "minecraft:redstone_wire"                         , "PlaceLater Mining").getString(); // redstone wire on top of furnace
			config.get("block_tags", "warpdrive:hull.basic.plain"                      , "PlaceEarliest StopMining").getString();
			config.get("block_tags", "warpdrive:hull.advanced.plain"                   , "PlaceEarliest StopMining").getString();
			config.get("block_tags", "warpdrive:hull.superior.plain"                   , "PlaceEarliest StopMining").getString();
			config.get("block_tags", "warpdrive:hull.basic.glass"                      , "PlaceEarliest StopMining").getString();
			config.get("block_tags", "warpdrive:hull.advanced.glass"                   , "PlaceEarliest StopMining").getString();
			config.get("block_tags", "warpdrive:hull.superior.glass"                   , "PlaceEarliest StopMining").getString();
			config.get("block_tags", "warpdrive:lamp_bubble"                           , "PlaceEarliest StopMining").getString();
			config.get("block_tags", "warpdrive:lamp_flat"                             , "PlaceEarliest StopMining").getString();
			config.get("block_tags", "warpdrive:lamp_long"                             , "PlaceEarliest StopMining").getString();
			config.get("block_tags", "warpdrive:force_field.basic"                     , "PlaceLatest StopMining NoMass").getString();
			config.get("block_tags", "warpdrive:force_field.advanced"                  , "PlaceLatest StopMining NoMass").getString();
			config.get("block_tags", "warpdrive:force_field.superior"                  , "PlaceLatest StopMining NoMass").getString();
			
			// config.get("block_tags", "AdvancedRepulsionSystems:field"                  , "PlaceEarlier StopMining NoBlink").getString();
			config.get("block_tags", "advancedrocketry:forcefield"                     , "PlaceLatest StopMining NoMass").getString();
			config.get("block_tags", "computercraft:advanced_modem"                    , "PlaceLatest").getString();
			config.get("block_tags", "embers:caminite_lever"                           , "PlaceLatest").getString();
			config.get("block_tags", "energycontrol:info_panel_advanced"               , "PlaceLatest").getString();
			config.get("block_tags", "energycontrol:info_panel_advanced_extender"      , "PlaceLater").getString();
			config.get("block_tags", "energycontrol:info_panel"                        , "PlaceLatest").getString();
			config.get("block_tags", "energycontrol:info_panel_extender"               , "PlaceLater").getString();
			config.get("block_tags", "energycontrol:holo_panel"                        , "PlaceLatest").getString();
			config.get("block_tags", "energycontrol:holo_panel_extender"               , "PlaceLater").getString();
			config.get("block_tags", "galacticraftcore:glowstone_torch"                , "PlaceLatest").getString();
			config.get("block_tags", "galacticraftcore:unlit_torch"                    , "PlaceLatest").getString();
			config.get("block_tags", "galacticraftcore:unlit_torch_lit"                , "PlaceLatest").getString();
			config.get("block_tags", "galacticraftplanets:telepad_fake"                , "PlaceLatest").getString();
			config.get("block_tags", "galacticraftplanets:web_torch"                   , "PlaceLatest").getString();
			// @TODO MC1.12+ config.get("block_tags", "ic2:resource"                                  , "PlaceEarliest StopMining").getString(); // IC2 Experimental Reinforced stone, need property filtering
			config.get("block_tags", "ic2:glass"                                       , "PlaceEarliest StopMining").getString(); // IC2 Experimental Reinforced glass
			config.get("block_tags", "ic2:reinforced_door"                             , "PlaceEarliest StopMining").getString(); // IC2 Experimental Reinforced door
			config.get("block_tags", "ic2:blockutility"                                , "PlaceEarliest StopMining").getString(); // IC2 Classic Reinforced stone & glass
			// @TODO MC1.12 config.get("block_tags", "ic2:te"                                          , "PlaceLater").getString(); // IC2 Experimental Nuclear reactor, need property filtering type:nuclear_reactor
			// @TODO MC1.12 config.get("block_tags", "ic2:te"                                          , "PlaceLatest").getString(); // IC2 Experimental Reactor chamber, need property filtering type:reactor_chamber
			config.get("block_tags", "ic2:blockgenerator"                              , "PlaceLater").getString(); // IC2 Classic Nuclear reactor
			config.get("block_tags", "ic2:blockchambers"                               , "PlaceLatest").getString(); // IC2 Classic Reactor chamber
			config.get("block_tags", "immersiveengineering:connector"                  , "PlaceLatest").getString();
			config.get("block_tags", "industrialforegoing:black_hole_label"            , "PlaceLatest").getString();
			config.get("block_tags", "modularforcefieldsystem:force_field"             , "PlaceEarlier StopMining NoBlink").getString();
			config.get("block_tags", "opencomputers:case1"                             , "PlaceLatest").getString();
			config.get("block_tags", "opencomputers:case2"                             , "PlaceLatest").getString();
			config.get("block_tags", "opencomputers:case3"                             , "PlaceLatest").getString();
			config.get("block_tags", "opencomputers:casecreative"                      , "PlaceLatest").getString();
			config.get("block_tags", "opencomputers:keyboard"                          , "PlaceLatest").getString();
			config.get("block_tags", "pneumaticcraft:pressure_chamber_valve"           , "PlaceEarlier").getString();
			config.get("block_tags", "rustic:candle"                                   , "PlaceLatest").getString();
			config.get("block_tags", "sgcraft:stargatebase"                            , "PlaceEarliest").getString(); // Stargate Atlantiscraft 1.3
			config.get("block_tags", "sgcraft:stargatering"                            , "PlaceEarlier").getString();
			config.get("block_tags", "sgcraft:stargatecontroller"                      , "PlaceLatest").getString();
			config.get("block_tags", "techguns:lamp0"                                  , "PlaceLatest").getString();
			config.get("block_tags", "tconstruct:stone_torch"                          , "PlaceLatest").getString();
			
			// expendables, a.k.a. "don't blow my ship with this..."
			config.get("block_tags", "minecraft:snow_layer"                            , "NoMass Expandable").getString();
			config.get("block_tags", "warpdrive:gas"                                   , "LeftBehind Expandable").getString();
			config.get("block_tags", "warpdrive:air_flow"                              , "NoMass Expandable PlaceLatest").getString();
			config.get("block_tags", "warpdrive:air_source"                            , "NoMass Expandable PlaceLatest").getString();
			
			config.get("block_tags", "botania:bifrost"                                 , "SkipMining NoMass Expandable").getString();
			config.get("block_tags", "botania:bifrostperm"                             , "SkipMining NoMass Expandable").getString();
			config.get("block_tags", "buildcraftcore:marker_path"                      , "NoMass LeftBehind Expandable").getString(); // depends on an unsupported separate cache
			config.get("block_tags", "buildcraftcore:marker_volume"                    , "NoMass LeftBehind Expandable").getString(); // depends on an unsupported separate cache
			config.get("block_tags", "chisel:cloud"                                    , "LeftBehind Expandable").getString();
			config.get("block_tags", "ebwizardry:spectral_block"                       , "SkipMining NoMass Expandable").getString();
			config.get("block_tags", "galacticraftcore:bright_air"                     , "NoMass Expandable").getString();
			config.get("block_tags", "galacticraftcore:breatheable_air"                , "NoMass Expandable").getString();
			config.get("block_tags", "galacticraftcore:bright_breathable_air"          , "NoMass Expandable").getString();
			config.get("block_tags", "railcraft:residual.heat"                         , "LeftBehind Expandable").getString();
			config.get("block_tags", "rftools:screen_hitblock"                         , "SkipMining NoMass Expandable").getString();
			
			// mining a mineshaft...
			config.get("block_tags", "minecraft:web"                                   , "Mining").getString();
			config.get("block_tags", "minecraft:fence"                                 , "Mining").getString();
			config.get("block_tags", "minecraft:glowstone"                             , "Mining").getString();
			config.get("block_tags", "minecraft:redstone_block"                        , "Mining").getString();
			
			// mining an 'end' moon
			config.get("block_tags", "warpdrive:iridium_block"                         , "Mining").getString();	// stronger than obsidian but can still be mined (see ender moon)
			
			// force field camouflage blacklisting
			config.get("block_tags", "deepresonance:energy_collector"                  , "NoCamouflage").getString();
			config.get("block_tags", "deepresonance:resonating_crystal"                , "NoCamouflage").getString();
			config.get("block_tags", "evilcraft:blood_infuser"                         , "NoCamouflage").getString();
			config.get("block_tags", "evilcraft:dark_ore"                              , "NoCamouflage").getString();
			config.get("block_tags", "evilcraft:sanguinary_environmental_accumulator"  , "NoCamouflage").getString();
			config.get("block_tags", "evilcraft:spirit_reanimator"                     , "NoCamouflage").getString();
			config.get("block_tags", "openmodularturrets:turret_base"                  , "NoCamouflage").getString(); // Turret tiers are stored in meta data
			config.get("block_tags", "thaumcraft:blockCustomPlant"                     , "NoCamouflage").getString(); // To be tested
			config.get("block_tags", "thermalexpansion:cache"                          , "NoCamouflage").getString();
			config.get("block_tags", "thermalexpansion:device"                         , "NoCamouflage").getString();
			config.get("block_tags", "thermalexpansion:machine"                        , "NoCamouflage").getString();
			
			// *** read actual values
			final String[] taggedBlocksName = categoryBlockTags.getValues().keySet().toArray(new String[0]);
			taggedBlocks = new HashMap<>(taggedBlocksName.length);
			for (final String name : taggedBlocksName) {
				final String tags = config.get("block_tags", name, "").getString();
				taggedBlocks.put(name, tags);
			}
		}
		
		// Entity dictionary
		{
			config.addCustomCategoryComment("entity_tags", 
					  "Use this section to enable special behavior on entities using tags.\n"
					+ "Most entities are already supported automatically. Only modify this section when something doesn't work!\n" + "\n"
					+ "Tags shall be separated by at least one space, comma or tabulation.\n" + "Invalid tags will be ignored silently. Tags and block names are case sensitive.\n"
					+ "In case of conflicts, the latest tag overwrite the previous ones.\n" + "- Anchor: ship can't move with this entity aboard (default: none).\n"
					+ "- NoMass: this entity doesn't count when calculating ship volume/mass (default: boats, frames, carts).\n"
					+ "- LeftBehind: this entity won't move with your ship nor transporter (default: particle effects).\n"
					+ "- NoReveal: this entity won't be revealed when uncloaking. Applies to buggy entity with no network packets. (default: SGCraft Iris).\n"
//					+ "- NoTransport: this entity is ignored by the transporter (default: -none-).\n"
					+ "- NonLivingTarget: this non-living entity can be targeted/removed by weapons (default: ItemFrame, Painting).\n"
					+ "- LivingWithoutAir: this living entity doesn't need air to live (default: vanilla zombies and skeletons).");
			
			final ConfigCategory categoryEntityTags = config.getCategory("entity_tags");
			// *** enforce default values
			// weapon target
			config.get("entity_tags", "minecraft:item_frame"                   , "NoMass NonLivingTarget").getString();
			config.get("entity_tags", "minecraft:painting"                     , "NoMass NonLivingTarget").getString();
			config.get("entity_tags", "minecraft:leash_knot"                   , "NoMass NonLivingTarget").getString();
			config.get("entity_tags", "minecraft:boat"                         , "NoMass NonLivingTarget").getString();
			config.get("entity_tags", "minecraft:minecart"                     , "NoMass NonLivingTarget").getString();
			config.get("entity_tags", "minecraft:chest_minecart"               , "NoMass NonLivingTarget").getString();
			config.get("entity_tags", "minecraft:furnace_minecart"             , "NoMass NonLivingTarget").getString();
			config.get("entity_tags", "minecraft:tnt_minecart"                 , "NoMass NonLivingTarget").getString();
			config.get("entity_tags", "minecraft:hopper_minecart"              , "NoMass NonLivingTarget").getString();
			config.get("entity_tags", "minecraft:spawner_minecart"             , "NoMass NonLivingTarget").getString();
			config.get("entity_tags", "minecraft:ender_crystal"                , "NoMass NonLivingTarget").getString();
			config.get("entity_tags", "minecraft:arrow"                        , "NoMass NonLivingTarget").getString();
			
			config.get("entity_tags", "ic2:carbon_boat"                        , "NoMass NonLivingTarget").getString(); // IC2 Experimental
			config.get("entity_tags", "ic2:carbonboat"                         , "NoMass NonLivingTarget").getString(); // IC2 Classic
			config.get("entity_tags", "ic2:rubber_boat"                        , "NoMass NonLivingTarget").getString(); // IC2 Experimental
			config.get("entity_tags", "ic2:rubberboat"                         , "NoMass NonLivingTarget").getString(); // IC2 Classic
			config.get("entity_tags", "ic2:electric_boat"                      , "NoMass NonLivingTarget").getString(); // IC2 Experimental
			config.get("entity_tags", "ic2:electricboat"                       , "NoMass NonLivingTarget").getString(); // IC2 Classic
			config.get("entity_tags", "ic2:nuke"                               , "NoMass NonLivingTarget").getString(); // IC2 Experimental & Classic
			config.get("entity_tags", "ic2:itnt"                               , "NoMass NonLivingTarget").getString(); // IC2 Experimental & Classic
			config.get("entity_tags", "ic2:sticky_dynamite"                    , "NoMass NonLivingTarget").getString(); // IC2 Experimental
			config.get("entity_tags", "ic2:dynamite"                           , "NoMass NonLivingTarget").getString(); // IC2 Experimental & Classic
			
			// undead and dummies
			config.get("entity_tags", "minecraft:armor_stand"                  , "LivingWithoutAir").getString();
			config.get("entity_tags", "minecraft:creeper"                      , "LivingWithoutAir").getString();
			config.get("entity_tags", "minecraft:elder_guardian"               , "LivingWithoutAir").getString();
			config.get("entity_tags", "minecraft:guardian"                     , "LivingWithoutAir").getString();
			config.get("entity_tags", "minecraft:skeleton"                     , "LivingWithoutAir").getString();
			config.get("entity_tags", "minecraft:skeleton_horse"               , "LivingWithoutAir").getString();
			config.get("entity_tags", "minecraft:wither"                       , "LivingWithoutAir").getString();
			config.get("entity_tags", "minecraft:wither_skeleton"              , "LivingWithoutAir").getString();
			config.get("entity_tags", "minecraft:zombie"                       , "LivingWithoutAir").getString();
			config.get("entity_tags", "minecraft:zombie_horse"                 , "LivingWithoutAir").getString();
			config.get("entity_tags", "minecraft:zombie_pigman"                , "LivingWithoutAir").getString();
			config.get("entity_tags", "minecraft:zombie_villager"              , "LivingWithoutAir").getString();
			
			// config.get("entity_tags", "BuildCraft|Robotics.bcRobot"                        , "LivingWithoutAir").getString(); @TODO MC1.12 BuildCraft robots not implemented yet
			config.get("entity_tags", "extraplanets:extraplanets.evolvedmagmacubeboss"  , "LivingWithoutAir").getString();
			config.get("entity_tags", "extraplanets:extraplanets.evolvedfirebatboss"    , "LivingWithoutAir").getString();
			config.get("entity_tags", "extraplanets:extraplanets.evolvedghastboss"      , "LivingWithoutAir").getString();
			config.get("entity_tags", "extraplanets:extraplanets.evolvediceslimeboss"   , "LivingWithoutAir").getString();
			config.get("entity_tags", "extraplanets:extraplanets.evolvedsnowmanboss"    , "LivingWithoutAir").getString();
			config.get("entity_tags", "extraplanets:extraplanets.evolvedspacemanboss"   , "LivingWithoutAir").getString();
			config.get("entity_tags", "extraplanets:extraplanets.evolvedgiantzombieboss", "LivingWithoutAir").getString();
			config.get("entity_tags", "galacticraftcore:evolved_zombie"        , "LivingWithoutAir").getString();
			config.get("entity_tags", "galacticraftcore:evolved_spider"        , "LivingWithoutAir").getString();
			config.get("entity_tags", "galacticraftcore:evolved_creeper"       , "LivingWithoutAir").getString();
			config.get("entity_tags", "galacticraftcore:evolved_skeleton"      , "LivingWithoutAir").getString();
			config.get("entity_tags", "galacticraftcore:evolved_skeleton_boss" , "LivingWithoutAir").getString();
			config.get("entity_tags", "galacticraftcore:evolved_enderman"      , "LivingWithoutAir").getString();
			config.get("entity_tags", "galacticraftcore:evolved_witch"         , "LivingWithoutAir").getString();
			config.get("entity_tags", "galacticraftplanets:creeper_boss"       , "LivingWithoutAir").getString();
			config.get("entity_tags", "icbmclassic:skeleton.xmas.elf"          , "LivingWithoutAir").getString();
			config.get("entity_tags", "icbmclassic:skeleton.xmas.snowman"      , "LivingWithoutAir").getString();
			config.get("entity_tags", "icbmclassic:skeleton.xmas.boss"         , "LivingWithoutAir").getString();
			config.get("entity_tags", "icbmclassic:zombie.xmas.elf"            , "LivingWithoutAir").getString();
			config.get("entity_tags", "icbmclassic:zombie.xmas.creeper"        , "LivingWithoutAir").getString();
			config.get("entity_tags", "icbmclassic:zombie.xmas.boss"           , "LivingWithoutAir").getString();
			config.get("entity_tags", "pneumaticcraft:drone"                   , "LivingWithoutAir").getString();
			config.get("entity_tags", "pneumaticcraft:harvesting_drone"        , "LivingWithoutAir").getString();
			config.get("entity_tags", "pneumaticcraft:logistic_drone"          , "LivingWithoutAir").getString();
			config.get("entity_tags", "techguns:turret"                        , "NoMass LivingWithoutAir LeftBehind").getString();
			config.get("entity_tags", "testdummy:dummy"                        , "NoMass LivingWithoutAir").getString();
			config.get("entity_tags", "thaumcraft:brainyzombie"                , "LivingWithoutAir").getString();
			config.get("entity_tags", "thaumcraft:giantbrainyzombie"           , "LivingWithoutAir").getString();
			config.get("entity_tags", "thaumcraft:mindspider"                  , "LivingWithoutAir").getString();
			config.get("entity_tags", "thaumcraft:taintacle"                   , "LivingWithoutAir").getString();
			config.get("entity_tags", "thaumcraft:taintaclegiant"              , "LivingWithoutAir").getString();
			config.get("entity_tags", "thaumcraft:taintcrawler"                , "LivingWithoutAir").getString();
			config.get("entity_tags", "thaumcraft:taintseed"                   , "LivingWithoutAir").getString();
			config.get("entity_tags", "thaumcraft:taintseedprime"              , "LivingWithoutAir").getString();
			config.get("entity_tags", "thaumcraft:taintswarm"                  , "LivingWithoutAir").getString();
			config.get("entity_tags", "thaumcraft:wisp"                        , "LivingWithoutAir").getString();
			config.get("entity_tags", "twilightforest.skeleton_druid"          , "LivingWithoutAir").getString();
			config.get("entity_tags", "warpdrive:entity_offline_avatar"        , "LivingWithoutAir").getString();
			
			// buggy entities
			config.get("entity_tags", "sgcraft:stargate_iris"                  , "NoReveal").getString();
			
			// *** read actual values
			final String[] taggedEntitiesName = categoryEntityTags.getValues().keySet().toArray(new String[0]);
			taggedEntities = new HashMap<>(taggedEntitiesName.length);
			for (final String name : taggedEntitiesName) {
				final String tags = config.get("entity_tags", name, "").getString();
				taggedEntities.put(name, tags);
			}
		}
		
		// Item dictionary
		{
			config.addCustomCategoryComment("item_tags", "Use this section to enable special behavior on items using tags.\n"
					+ "Most items are already supported automatically. Only modify this section when something doesn't work!\n" + "\n"
					+ "Tags shall be separated by at least one space, comma or tabulation.\n" + "Invalid tags will be ignored silently. Tags and block names are case sensitive.\n"
					+ "In case of conflicts, the latest tag overwrite the previous ones.\n" + "- FlyInSpace: player can move without gravity effect while wearing this item (default: jetpacks).\n"
					+ "- NoFallDamage: player doesn't take fall damage while wearing this armor item (default: IC2 rubber boots).\n"
					+ "- BreathingHelmet: player can breath from WarpDrive air canister or IC2 compressed air while wearing this armor item (default: IC2 nano helmet and Cie).\n"
			        + "- ExcludedAvatar: offline avatars can't wear nor hold this item (default: SimplyJetpacks).\n");
			
			final ConfigCategory categoryItemTags = config.getCategory("item_tags");
			// *** enforce default values
			config.get("item_tags", "advanced_solar_panels:advancedsolarhelmet"    , "BreathingHelmet").getString();
			config.get("item_tags", "advanced_solar_panels:hybridsolarhelmet"      , "BreathingHelmet").getString();
			config.get("item_tags", "advanced_solar_panels:ultimatesolarhelmet"    , "BreathingHelmet").getString();
			config.get("item_tags", "bloodmagic:living_armour_helmet"              , "BreathingHelmet").getString();
			config.get("item_tags", "bloodmagic:sentient_armour_helmet"            , "BreathingHelmet").getString();
			config.get("item_tags", "botania:elementiumhelm"                       , "BreathingHelmet").getString();
			config.get("item_tags", "botania:elementiumhelmreveal"                 , "BreathingHelmet").getString();
			config.get("item_tags", "botania:terrasteelhelm"                       , "BreathingHelmet").getString();
			config.get("item_tags", "botania:terrasteelhelmreveal"                 , "BreathingHelmet").getString();
			config.get("item_tags", "conarm:helmet"                                , "BreathingHelmet").getString();
			config.get("item_tags", "enderio:item_dark_steel_helmet"               , "BreathingHelmet").getString();
			config.get("item_tags", "enderio:item_end_steel_helmet"                , "BreathingHelmet").getString();
			config.get("item_tags", "ic2:hazmat_helmet"                            , "BreathingHelmet").getString(); // IC2 Experimental
			config.get("item_tags", "ic2:solar_helmet"                             , "BreathingHelmet").getString(); // IC2 Experimental
			config.get("item_tags", "ic2:nano_helmet"                              , "BreathingHelmet").getString(); // IC2 Experimental
			config.get("item_tags", "ic2:quantum_helmet"                           , "BreathingHelmet").getString(); // IC2 Experimental
			config.get("item_tags", "ic2:itemarmorhazmathelmet"                    , "BreathingHelmet").getString(); // IC2 Classic
			config.get("item_tags", "ic2:itemsolarhelmets"                         , "BreathingHelmet").getString(); // IC2 Classic
			config.get("item_tags", "ic2:itemarmornanohelmet"                      , "BreathingHelmet").getString(); // IC2 Classic
			config.get("item_tags", "ic2:itemarmorquantumhelmet"                   , "BreathingHelmet").getString(); // IC2 Classic
			config.get("item_tags", "pneumaticcraft:pneumatic_helmet"              , "BreathingHelmet").getString();
			config.get("item_tags", "powersuits:powerarmor_head"                   , "BreathingHelmet").getString();
			config.get("item_tags", "psi:psimetal_exosuit_helmet"                  , "BreathingHelmet").getString();
			config.get("item_tags", "redstonearsenal:armor.helmet_flux"            , "BreathingHelmet").getString();
			config.get("item_tags", "techguns:t3_combat_helmet"                    , "BreathingHelmet").getString();
			config.get("item_tags", "techguns:t3_exo_helmet"                       , "BreathingHelmet").getString();
			config.get("item_tags", "techguns:t3_miner_helmet"                     , "BreathingHelmet").getString();
			config.get("item_tags", "techguns:t3_power_helmet"                     , "BreathingHelmet").getString();
			config.get("item_tags", "techguns:steam_helmet"                        , "BreathingHelmet").getString();
			config.get("item_tags", "techguns:tacticalmask"                        , "BreathingHelmet").getString();
			
			config.get("item_tags", "ic2:jetpack"                                  , "FlyInSpace NoFallDamage").getString(); // IC2 Experimental
			config.get("item_tags", "ic2:jetpack_electric"                         , "FlyInSpace NoFallDamage").getString(); // IC2 Experimental
			config.get("item_tags", "ic2:quantum_chestplate"                       , "FlyInSpace NoFallDamage").getString(); // IC2 Experimental
			config.get("item_tags", "ic2:itemarmorjetpack"                         , "FlyInSpace NoFallDamage").getString(); // IC2 Classic
			config.get("item_tags", "ic2:itemarmorjetpackelectric"                 , "FlyInSpace NoFallDamage").getString(); // IC2 Classic
			config.get("item_tags", "ic2:itemarmorjetpacknuclear"                  , "FlyInSpace NoFallDamage").getString(); // IC2 Classic
			config.get("item_tags", "ic2:itemarmorcombinedjetpack"                 , "FlyInSpace NoFallDamage").getString(); // IC2 Classic
			config.get("item_tags", "ic2:itemarmornuclearcombindedjetpack"         , "FlyInSpace NoFallDamage").getString(); // IC2 Classic
			config.get("item_tags", "ic2:itemquantumarmorjetpack"                  , "FlyInSpace NoFallDamage").getString(); // IC2 Classic
			config.get("item_tags", "ic2:itemquantumarmornuclearjetplate"          , "FlyInSpace NoFallDamage").getString(); // IC2 Classic
			config.get("item_tags", "gravisuite:advancedjetpack"                   , "FlyInSpace NoFallDamage").getString();
			config.get("item_tags", "gravisuite:advancednanochestplate"            , "FlyInSpace NoFallDamage").getString();
			config.get("item_tags", "gravisuite:gravichestplate"                   , "FlyInSpace NoFallDamage").getString();
			
			config.get("item_tags", "ic2:rubber_boots"                             , "NoFallDamage").getString(); // IC2 Experimental
			config.get("item_tags", "ic2:quantum_boots"                            , "NoFallDamage").getString(); // IC2 Experimental
			config.get("item_tags", "ic2:itemarmorrubboots"                        , "NoFallDamage").getString(); // IC2 Classic
			config.get("item_tags", "ic2:itemarmorquantumboots"                    , "NoFallDamage").getString(); // IC2 Classic
			config.get("item_tags", "warpdrive:warp_armor.advanced.leggings"       , "NoFallDamage").getString();
			config.get("item_tags", "warpdrive:warp_armor.advanced.boots"          , "NoFallDamage").getString();
			config.get("item_tags", "warpdrive:warp_armor.superior.leggings"       , "NoFallDamage").getString();
			config.get("item_tags", "warpdrive:warp_armor.superior.boots"          , "NoFallDamage").getString();
			
			config.get("item_tags", "simplyjetpacks:itemjetpack"                   , "ExcludedAvatar").getString();
			
			// *** read actual values
			final String[] taggedItemsName = categoryItemTags.getValues().keySet().toArray(new String[0]);
			taggedItems = new HashMap<>(taggedItemsName.length);
			for (final String name : taggedItemsName) {
				final String tags = config.get("item_tags", name, "").getString();
				taggedItems.put(name, tags);
			}
		}
		
	}
	
	public static void apply() {
		WarpDrive.logger.info("Evaluating dictionary entries");
		
		// get default settings from parsing ore dictionary
		BLOCKS_ORES = new HashSet<>();
		BLOCKS_LOGS = new HashSet<>();
		BLOCKS_LEAVES = new HashSet<>();
		final String[] oreNames = OreDictionary.getOreNames();
		for (final String oreName : oreNames) {
			final String lowerOreName = oreName.toLowerCase();
			if (oreName.length() > 4 && oreName.startsWith("ore")) {
				final List<ItemStack> itemStacks = OreDictionary.getOres(oreName);
				for (final ItemStack itemStack : itemStacks) {
					BLOCKS_ORES.add(Block.getBlockFromItem(itemStack.getItem()));
					// WarpDrive.logger.info(String.format("- added %s to ores as %s", oreName, itemStack));
				}
			}
			if (lowerOreName.startsWith("log") || lowerOreName.endsWith("log") || lowerOreName.endsWith("logs")) {
				final List<ItemStack> itemStacks = OreDictionary.getOres(oreName);
				for (final ItemStack itemStack : itemStacks) {
					BLOCKS_LOGS.add(Block.getBlockFromItem(itemStack.getItem()));
					// WarpDrive.logger.info(String.format("- added %s to logs as %s", oreName, itemStack));
				}
			}
			if (lowerOreName.startsWith("leave") || lowerOreName.endsWith("leave") || lowerOreName.endsWith("leaves")) {
				final List<ItemStack> itemStacks = OreDictionary.getOres(oreName);
				for (final ItemStack itemStack : itemStacks) {
					BLOCKS_LEAVES.add(Block.getBlockFromItem(itemStack.getItem()));
					// WarpDrive.logger.info(String.format("- added %s to leaves as %s", oreName, itemStack));
				}
			}
		}
		
		// apply tagged blocks
		BLOCKS_STACKING_PLANTS = new HashSet<>(taggedBlocks.size());
		BLOCKS_SOILS = new HashSet<>(taggedBlocks.size());
		BLOCKS_ANCHOR = new HashSet<>(taggedBlocks.size());
		BLOCKS_NOMASS = new HashSet<>(taggedBlocks.size() + BLOCKS_LEAVES.size());
		BLOCKS_NOMASS.addAll(BLOCKS_LEAVES);
		BLOCKS_LEFTBEHIND = new HashSet<>(taggedBlocks.size());
		BLOCKS_EXPANDABLE = new HashSet<>(taggedBlocks.size() + BLOCKS_LEAVES.size());
		BLOCKS_EXPANDABLE.addAll(BLOCKS_LEAVES);
		BLOCKS_MINING = new HashSet<>(taggedBlocks.size());
		BLOCKS_SKIPMINING = new HashSet<>(taggedBlocks.size());
		BLOCKS_STOPMINING = new HashSet<>(taggedBlocks.size());
		BLOCKS_PLACE = new HashMap<>(taggedBlocks.size());
		BLOCKS_NOCAMOUFLAGE = new HashSet<>(taggedBlocks.size());
		BLOCKS_NOBLINK = new HashSet<>(taggedBlocks.size());
		cache_blocks_logsAndStackings = null;
		cache_blocks_logsLeavesAndStackings = null;
		for (final Entry<String, String> taggedBlock : taggedBlocks.entrySet()) {
			final Block block = Block.getBlockFromName(taggedBlock.getKey());
			if (block == null) {
				WarpDrive.logger.info(String.format("Ignoring missing block %s", taggedBlock.getKey()));
				continue;
			}
			for (final String tag : taggedBlock.getValue().replace("\t", " ").replace(",", " ").replace("  ", " ").split(" ")) {
				switch (tag) {
				case "Soil"         : BLOCKS_SOILS.add(block); break;
				case "Log"          : BLOCKS_LOGS.add(block); break;
				case "Leaf"         : BLOCKS_LEAVES.add(block); break;
				case "StackingPlant": BLOCKS_STACKING_PLANTS.add(block); break;
				case "Anchor"       : BLOCKS_ANCHOR.add(block); break;
				case "NoMass"       : BLOCKS_NOMASS.add(block); break;
				case "LeftBehind"   : BLOCKS_LEFTBEHIND.add(block); break;
				case "Expandable"   : BLOCKS_EXPANDABLE.add(block); break;
				case "Mining"       : BLOCKS_MINING.add(block); break;
				case "SkipMining"   : BLOCKS_SKIPMINING.add(block); break;
				case "StopMining"   : BLOCKS_STOPMINING.add(block); break;
				case "PlaceEarliest": BLOCKS_PLACE.put(block, 0); break;
				case "PlaceEarlier" : BLOCKS_PLACE.put(block, 1); break;
				case "PlaceNormal"  : BLOCKS_PLACE.put(block, 2); break;
				case "PlaceLater"   : BLOCKS_PLACE.put(block, 3); break;
				case "PlaceLatest"  : BLOCKS_PLACE.put(block, 4); break;
				case "NoCamouflage" : BLOCKS_NOCAMOUFLAGE.add(block); break;
				case "NoBlink"      : BLOCKS_NOBLINK.add(block); break;
				default:
					WarpDrive.logger.error(String.format("Unsupported tag %s for block %s", tag, block));
					break;
				}
			}
		}
		
		// apply tagged entities
		ENTITIES_ANCHOR = new HashSet<>(taggedEntities.size());
		ENTITIES_NOMASS = new HashSet<>(taggedEntities.size());
		ENTITIES_LEFTBEHIND = new HashSet<>(taggedEntities.size());
		ENTITIES_NONLIVINGTARGET = new HashSet<>(taggedEntities.size());
		ENTITIES_LIVING_WITHOUT_AIR = new HashSet<>(taggedEntities.size());
		ENTITIES_NO_REVEAL = new HashSet<>(taggedEntities.size());
		for (final Entry<String, String> taggedEntity : taggedEntities.entrySet()) {
			final ResourceLocation resourceLocation = new ResourceLocation(taggedEntity.getKey());
			if (EntityList.getClass(resourceLocation) == null) {
				WarpDrive.logger.info(String.format("Ignoring missing entity %s",
				                                    resourceLocation ));
				continue;
			}
			for (final String tag : taggedEntity.getValue().replace("\t", " ").replace(",", " ").replace("  ", " ").split(" ")) {
				switch (tag) {
				case "Anchor"          : ENTITIES_ANCHOR.add(resourceLocation); break;
				case "NoMass"          : ENTITIES_NOMASS.add(resourceLocation); break;
				case "LeftBehind"      : ENTITIES_LEFTBEHIND.add(resourceLocation); break;
				case "NonLivingTarget" : ENTITIES_NONLIVINGTARGET.add(resourceLocation); break;
				case "LivingWithoutAir": ENTITIES_LIVING_WITHOUT_AIR.add(resourceLocation); break;
				case "NoReveal"        : ENTITIES_NO_REVEAL.add(resourceLocation); break;
				default:
					WarpDrive.logger.error(String.format("Unsupported tag %s for entity %s",
					                                     tag, resourceLocation ));
					break;
				}
			}
		}
		
		// apply tagged items
		ITEMS_FLYINSPACE = new HashSet<>(taggedItems.size());
		ITEMS_NOFALLDAMAGE = new HashSet<>(taggedItems.size());
		ITEMS_BREATHING_HELMET = new HashSet<>(taggedItems.size());
		ITEMS_EXCLUDED_AVATAR = new HashSet<>(taggedItems.size());
		for (final Entry<String, String> taggedItem : taggedItems.entrySet()) {
			final String itemId = taggedItem.getKey();
			final Item item = Item.REGISTRY.getObject(new ResourceLocation(itemId));
			if (item == null) {
				WarpDrive.logger.info(String.format("Ignoring missing item %s", itemId));
				continue;
			}
			for (final String tag : taggedItem.getValue().replace("\t", " ").replace(",", " ").replace("  ", " ").split(" ")) {
				switch (tag) {
				case "FlyInSpace"     : ITEMS_FLYINSPACE.add(item); break;
				case "NoFallDamage"   : ITEMS_NOFALLDAMAGE.add(item); break;
				case "BreathingHelmet": ITEMS_BREATHING_HELMET.add(item); break;
				case "ExcludedAvatar" : ITEMS_EXCLUDED_AVATAR.add(item); break;
				default:
					WarpDrive.logger.error(String.format("Unsupported tag %s for item %s", tag, item));
					break;
				}
			}
		}
		
		adjustHardnessAndResistance();
		print();
	}
	
	private static void print() {
		// print tagged blocks
		WarpDrive.logger.info("Active blocks dictionary:");
		WarpDrive.logger.info(String.format("- %s ores: %s"                   , BLOCKS_ORES.size(), getHashMessage(BLOCKS_ORES)));
		WarpDrive.logger.info(String.format("- %s soils: %s"                  , BLOCKS_SOILS.size(), getHashMessage(BLOCKS_SOILS)));
		WarpDrive.logger.info(String.format("- %s logs: %s"                   , BLOCKS_LOGS.size(), getHashMessage(BLOCKS_LOGS)));
		WarpDrive.logger.info(String.format("- %s stacking plants: %s"        , BLOCKS_STACKING_PLANTS.size(), getHashMessage(BLOCKS_STACKING_PLANTS)));
		WarpDrive.logger.info(String.format("- %s leaves: %s"                 , BLOCKS_LEAVES.size(), getHashMessage(BLOCKS_LEAVES)));
		WarpDrive.logger.info(String.format("- %s anchors: %s"                , BLOCKS_ANCHOR.size(), getHashMessage(BLOCKS_ANCHOR)));
		WarpDrive.logger.info(String.format("- %s with NoMass tag: %s"        , BLOCKS_NOMASS.size(), getHashMessage(BLOCKS_NOMASS)));
		WarpDrive.logger.info(String.format("- %s with LeftBehind tag: %s"    , BLOCKS_LEFTBEHIND.size(), getHashMessage(BLOCKS_LEFTBEHIND)));
		WarpDrive.logger.info(String.format("- %s expandable: %s"             , BLOCKS_EXPANDABLE.size(), getHashMessage(BLOCKS_EXPANDABLE)));
		WarpDrive.logger.info(String.format("- %s with Mining tag: %s"        , BLOCKS_MINING.size(), getHashMessage(BLOCKS_MINING)));
		WarpDrive.logger.info(String.format("- %s with SkipMining tag: %s"    , BLOCKS_SKIPMINING.size(), getHashMessage(BLOCKS_SKIPMINING)));
		WarpDrive.logger.info(String.format("- %s with StopMining tag: %s"    , BLOCKS_STOPMINING.size(), getHashMessage(BLOCKS_STOPMINING)));
		WarpDrive.logger.info(String.format("- %s with Placement priority: %s", BLOCKS_PLACE.size(), getHashMessage(BLOCKS_PLACE)));
		
		// print tagged entities
		WarpDrive.logger.info("Active entities dictionary:");
		WarpDrive.logger.info(String.format("- %s anchors: %s"                  , ENTITIES_ANCHOR.size(), getHashMessage(ENTITIES_ANCHOR)));
		WarpDrive.logger.info(String.format("- %s with NoMass tag: %s"          , ENTITIES_NOMASS.size(), getHashMessage(ENTITIES_NOMASS)));
		WarpDrive.logger.info(String.format("- %s with LeftBehind tag: %s"      , ENTITIES_LEFTBEHIND.size(), getHashMessage(ENTITIES_LEFTBEHIND)));
		WarpDrive.logger.info(String.format("- %s with NonLivingTarget tag: %s" , ENTITIES_NONLIVINGTARGET.size(), getHashMessage(ENTITIES_NONLIVINGTARGET)));
		WarpDrive.logger.info(String.format("- %s with LivingWithoutAir tag: %s", ENTITIES_LIVING_WITHOUT_AIR.size(), getHashMessage(ENTITIES_LIVING_WITHOUT_AIR)));
		WarpDrive.logger.info(String.format("- %s with NoReveal tag: %s"        , ENTITIES_NO_REVEAL.size(), getHashMessage(ENTITIES_NO_REVEAL)));
		
		// print tagged items
		WarpDrive.logger.info("Active items dictionary:");
		WarpDrive.logger.info(String.format("- %s allowing fly in space: %s" , ITEMS_FLYINSPACE.size(), getHashMessage(ITEMS_FLYINSPACE)));
		WarpDrive.logger.info(String.format("- %s absorbing fall damages: %s", ITEMS_NOFALLDAMAGE.size(), getHashMessage(ITEMS_NOFALLDAMAGE)));
		WarpDrive.logger.info(String.format("- %s allowing breathing air: %s", ITEMS_BREATHING_HELMET.size(), getHashMessage(ITEMS_BREATHING_HELMET)));
		WarpDrive.logger.info(String.format("- %s excluded avatar items: %s" , ITEMS_EXCLUDED_AVATAR.size(), getHashMessage(ITEMS_EXCLUDED_AVATAR)));
	}
	
	private static void adjustHardnessAndResistance() {
		// Apply explosion resistance adjustments
		Blocks.OBSIDIAN.setResistance(60.0F);
		Blocks.ENCHANTING_TABLE.setResistance(60.0F);
		Blocks.ENDER_CHEST.setResistance(60.0F);
		Blocks.ANVIL.setResistance(60.0F);
		Blocks.WATER.setResistance(30.0F);
		Blocks.FLOWING_WATER.setResistance(30.0F);
		Blocks.LAVA.setResistance(30.0F);
		Blocks.FLOWING_LAVA.setResistance(30.0F);
		
		// keep IC2 Reinforced stone stats 'as is'
		/*
		if (WarpDriveConfig.isIndustrialCraft2Loaded) {
			Block blockReinforcedStone = (Block) Block.blockRegistry.getObject("IC2:blockAlloy");
		}
		/**/
		
		// scan blocks registry
		final FakeWorld fakeWorld = new FakeWorld(null, false);
		for (final ResourceLocation resourceLocation : Block.REGISTRY.getKeys()) {
			final Block block = Block.REGISTRY.getObject(resourceLocation);
			WarpDrive.logger.debug(String.format("Checking block registry for %s: %s",
			                                     resourceLocation, block));
			
			try {
				// get hardness and blast resistance
				final IBlockState blockState = block.getDefaultState();
				fakeWorld.setBlockState(BlockPos.ORIGIN, blockState);
				final float hardness = Math.max(block.blockHardness, blockState.getBlockHardness(fakeWorld, BlockPos.ORIGIN));
				final Explosion explosion = new Explosion(fakeWorld, null, 0, 0, 0, 1, true, true);
				final float blastResistance = Math.max(block.getExplosionResistance(null), block.getExplosionResistance(fakeWorld, BlockPos.ORIGIN, null, explosion));
				
				// check actual values
				if ( hardness < 0
				  && (!BLOCKS_ANCHOR.contains(block))
				  && !(block instanceof BlockForceField) ) {// unbreakable block
					WarpDrive.logger.warn(String.format("Found a non-anchor block with unbreakable hardness %s %s (%.2f)",
					                                    resourceLocation, block, hardness));
				} else if ( hardness > WarpDriveConfig.HULL_HARDNESS[1]
				         && !( block instanceof BlockAbstractBase
				            || block instanceof BlockAbstractContainer
				            || block instanceof BlockHullGlass
				            || block instanceof BlockHullStairs
				            || block instanceof IFluidBlock
				            || BLOCKS_ANCHOR.contains(block) ) ) {
					WarpDrive.logger.warn(String.format("Found a non-hull block with high hardness %s %s (%.2f)",
					                                    resourceLocation, block, hardness));
				}
				
				if ( blastResistance > WarpDriveConfig.G_BLAST_RESISTANCE_CAP
				   && !( block instanceof BlockAbstractBase
				      || block instanceof BlockAbstractContainer
				      || block instanceof BlockHullGlass
				      || block instanceof BlockHullStairs
				      || BLOCKS_ANCHOR.contains(block) ) ) {
					if (WarpDriveConfig.LOGGING_DICTIONARY) {
						WarpDrive.logger.warn(String.format("Found a non-anchor block with high blast resistance %s %s (%.2f)",
						                                    resourceLocation, block, blastResistance));
					}
					block.setResistance(WarpDriveConfig.G_BLAST_RESISTANCE_CAP * 5.0F / 3.0F);
					final float blastResistance_new = Math.max(block.getExplosionResistance(null), block.getExplosionResistance(fakeWorld, BlockPos.ORIGIN, null, null));
					if (blastResistance_new <= WarpDriveConfig.G_BLAST_RESISTANCE_CAP) {
						WarpDrive.logger.info(String.format("Adjusted blast resistance of %s %s from %.2f to %.2f",
						                                    resourceLocation, block, blastResistance, blastResistance_new ));
					} else {
						WarpDrive.logger.error(String.format("Blacklisting block with high blast resistance %s %s (%.2f)",
						                                     resourceLocation, block, blastResistance ));
						BLOCKS_ANCHOR.add(block);
						BLOCKS_STOPMINING.add(block);
					}
				}
				
				if (WarpDriveConfig.LOGGING_DICTIONARY) {
					WarpDrive.logger.info(String.format("Block registry for %s; Block %s with hardness %.2f resistance %.2f",
					                                    resourceLocation, block,
					                                    hardness,
					                                    blastResistance ));
				}
			} catch (final Exception exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.error(String.format("Blacklisting block causing exception during dictionary checks %s %s, please report to mod author",
				                                     resourceLocation, block ));
				BLOCKS_ANCHOR.add(block);
				BLOCKS_STOPMINING.add(block);
			}
		}
	}
	
	@Nonnull
	private static String getHashMessage(@Nonnull final HashSet<?> hashSet) {
		final StringBuilder message = new StringBuilder();
		for (final Object object : hashSet) {
			if (message.length() > 0) {
				message.append(", ");
			}
			if (object instanceof IForgeRegistryEntry) {
				message.append(((IForgeRegistryEntry<?>) object).getRegistryName());
			} else if (object instanceof String) {
				message.append((String) object);
			} else {
				message.append(object);
			}
		}
		return message.toString();
	}
	
	@Nonnull
	private static String getHashMessage(@Nonnull final HashMap<Block, Integer> hashMap) {
		final StringBuilder message = new StringBuilder();
		for (final Entry<Block, Integer> entry : hashMap.entrySet()) {
			if (message.length() > 0) {
				message.append(", ");
			}
			message.append(entry.getKey().getRegistryName()).append("=").append(entry.getValue());
		}
		return message.toString();
	}
	
	@Nonnull
	public static NBTBase writeItemsToNBT(@Nonnull final HashSet<Item> hashSetItem) {
		final NBTTagList nbtTagList = new NBTTagList();
		for (final Item item : hashSetItem) {
			assert item.getRegistryName() != null;
			final String registryName = item.getRegistryName().toString();
			nbtTagList.appendTag(new NBTTagString(registryName));
		}
		return nbtTagList;
	}
	
	@Nonnull
	public static HashSet<Item> readItemsFromNBT(@Nonnull final NBTTagList nbtTagList) {
		final int size = nbtTagList.tagCount();
		final HashSet<Item> hashSetItem = new HashSet<>(Math.max(8, size));
		
		if (size > 0) {
			for (int index = 0; index < nbtTagList.tagCount(); index++) {
				final String registryName = nbtTagList.getStringTagAt(index);
				final Item item = Item.REGISTRY.getObject(new ResourceLocation(registryName));
				if (item != null) {
					hashSetItem.add(item);
				} else {
					WarpDrive.logger.warn(String.format("Ignoring unknown item %s", registryName));
				}
			}
		}
		return hashSetItem;
	}
	
	@Nonnull
	public static String getId(final Entity entity) {
		// player entities aren't registered so we use our own definition
		if (entity instanceof EntityPlayer) {
			if (entity instanceof FakePlayer) {
				return "forge:fake_player";
			}
			return "minecraft:player";
		}
		
		final ResourceLocation resourceLocation = EntityList.getKey(entity);
		return resourceLocation == null ? "-null-" : resourceLocation.toString();
	}
	
	public static boolean isSoil(final Block block) {
		return BLOCKS_SOILS.contains(block);
	}
	
	public static HashSet<Block> getLogsAndStackings() {
		if (cache_blocks_logsAndStackings == null) {
			cache_blocks_logsAndStackings = (HashSet<Block>) BLOCKS_LOGS.clone();
			cache_blocks_logsAndStackings.addAll(BLOCKS_STACKING_PLANTS);
		}
		return cache_blocks_logsAndStackings;
	}
	
	@SuppressWarnings("unchecked")
	public static HashSet<Block> getLogsLeavesAndStackings() {
		if (cache_blocks_logsLeavesAndStackings == null) {
			cache_blocks_logsLeavesAndStackings = (HashSet<Block>) BLOCKS_LOGS.clone();
			cache_blocks_logsLeavesAndStackings.addAll(BLOCKS_LEAVES);
			cache_blocks_logsLeavesAndStackings.addAll(BLOCKS_STACKING_PLANTS);
		}
		return cache_blocks_logsLeavesAndStackings;
	}
	
	public static boolean isLog(final Block block) {
		return BLOCKS_LOGS.contains(block);
	}
	
	public static boolean isLeaf(final Block block) {
		return BLOCKS_LEAVES.contains(block);
	}
	
	public static boolean isStackingPlant(final Block block) {
		return BLOCKS_STACKING_PLANTS.contains(block);
	}
	
	public static boolean isAnchor(final Entity entity) {
		final ResourceLocation resourceLocation = EntityList.getKey(entity);
		return ENTITIES_ANCHOR.contains(resourceLocation);
	}
	
	public static boolean isLeftBehind(final Entity entity) {
		final ResourceLocation resourceLocation = EntityList.getKey(entity);
		return ENTITIES_LEFTBEHIND.contains(resourceLocation);
	}
	
	public static boolean isLivingWithoutAir(final Entity entity) {
		// bypass for androids
		if ( WarpDriveConfig.isMatterOverdriveLoaded
		  && entity instanceof EntityPlayer ) {
			return CompatMatterOverdrive.isAndroid((EntityPlayer) entity);
		}
		
		final ResourceLocation resourceLocation = EntityList.getKey(entity);
		return ENTITIES_LIVING_WITHOUT_AIR.contains(resourceLocation);
	}
	
	public static boolean isNonLivingTarget(final Entity entity) {
		final ResourceLocation resourceLocation = EntityList.getKey(entity);
		return ENTITIES_NONLIVINGTARGET.contains(resourceLocation);
	}
	
	public static boolean isNoReveal(final Entity entity) {
		final ResourceLocation resourceLocation = EntityList.getKey(entity);
		return ENTITIES_NO_REVEAL.contains(resourceLocation);
	}
	
	public static boolean addToNoReveal(final Entity entity) {
		final ResourceLocation resourceLocation = EntityList.getKey(entity);
		return ENTITIES_NO_REVEAL.add(resourceLocation);
	}
}
