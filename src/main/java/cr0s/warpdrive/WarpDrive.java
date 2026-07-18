package cr0s.warpdrive;

import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.block.BlockChunkLoader;
import cr0s.warpdrive.block.BlockHighlyAdvancedMachine;
import cr0s.warpdrive.block.BlockLaserMedium;
import cr0s.warpdrive.block.BlockSecurityStation;
import cr0s.warpdrive.block.TileEntityAbstractBase;
import cr0s.warpdrive.block.TileEntityChunkLoader;
import cr0s.warpdrive.block.TileEntityLaserMedium;
import cr0s.warpdrive.block.TileEntitySecurityStation;
import cr0s.warpdrive.block.atomic.BlockAcceleratorControlPoint;
import cr0s.warpdrive.block.atomic.BlockAcceleratorCore;
import cr0s.warpdrive.block.atomic.BlockChiller;
import cr0s.warpdrive.block.atomic.BlockElectromagnetGlass;
import cr0s.warpdrive.block.atomic.BlockElectromagnetPlain;
import cr0s.warpdrive.block.atomic.BlockParticlesCollider;
import cr0s.warpdrive.block.atomic.BlockParticlesInjector;
import cr0s.warpdrive.block.atomic.BlockVoidShellGlass;
import cr0s.warpdrive.block.atomic.BlockVoidShellPlain;
import cr0s.warpdrive.block.atomic.TileEntityAcceleratorControlPoint;
import cr0s.warpdrive.block.atomic.TileEntityAcceleratorCore;
import cr0s.warpdrive.block.atomic.TileEntityParticlesInjector;
import cr0s.warpdrive.block.breathing.BlockAirFlow;
import cr0s.warpdrive.block.breathing.BlockAirGeneratorTiered;
import cr0s.warpdrive.block.breathing.BlockAirShield;
import cr0s.warpdrive.block.breathing.BlockAirSource;
import cr0s.warpdrive.block.breathing.TileEntityAirGeneratorTiered;
import cr0s.warpdrive.block.building.BlockShipScanner;
import cr0s.warpdrive.block.building.TileEntityShipScanner;
import cr0s.warpdrive.block.collection.BlockLaserTreeFarm;
import cr0s.warpdrive.block.collection.BlockMiningLaser;
import cr0s.warpdrive.block.collection.TileEntityLaserTreeFarm;
import cr0s.warpdrive.block.collection.TileEntityMiningLaser;
import cr0s.warpdrive.block.decoration.BlockBedrockGlass;
import cr0s.warpdrive.block.decoration.BlockDecorative;
import cr0s.warpdrive.block.decoration.BlockGas;
import cr0s.warpdrive.block.decoration.BlockLamp_bubble;
import cr0s.warpdrive.block.decoration.BlockLamp_flat;
import cr0s.warpdrive.block.decoration.BlockLamp_long;
import cr0s.warpdrive.block.detection.BlockBiometricScanner;
import cr0s.warpdrive.block.detection.BlockCamera;
import cr0s.warpdrive.block.detection.BlockCamouflage;
import cr0s.warpdrive.block.detection.BlockCloakingCoil;
import cr0s.warpdrive.block.detection.BlockCloakingCore;
import cr0s.warpdrive.block.detection.BlockEnvironmentalSensor;
import cr0s.warpdrive.block.detection.BlockMonitor;
import cr0s.warpdrive.block.detection.BlockRadar;
import cr0s.warpdrive.block.detection.BlockSiren;
import cr0s.warpdrive.block.detection.BlockSpeaker;
import cr0s.warpdrive.block.detection.BlockVirtualAssistant;
import cr0s.warpdrive.block.detection.BlockWarpIsolation;
import cr0s.warpdrive.block.detection.TileEntityBiometricScanner;
import cr0s.warpdrive.block.detection.TileEntityCamera;
import cr0s.warpdrive.block.detection.TileEntityCamouflage;
import cr0s.warpdrive.block.detection.TileEntityCloakingCore;
import cr0s.warpdrive.block.detection.TileEntityEnvironmentalSensor;
import cr0s.warpdrive.block.detection.TileEntityMonitor;
import cr0s.warpdrive.block.detection.TileEntityRadar;
import cr0s.warpdrive.block.detection.TileEntitySiren;
import cr0s.warpdrive.block.detection.TileEntitySpeaker;
import cr0s.warpdrive.block.detection.TileEntityVirtualAssistant;
import cr0s.warpdrive.block.energy.BlockCapacitor;
import cr0s.warpdrive.block.energy.BlockEnanReactorCore;
import cr0s.warpdrive.block.energy.BlockEnanReactorLaser;
import cr0s.warpdrive.block.energy.BlockIC2reactorLaserCooler;
import cr0s.warpdrive.block.energy.TileEntityCapacitor;
import cr0s.warpdrive.block.energy.TileEntityEnanReactorCore;
import cr0s.warpdrive.block.energy.TileEntityEnanReactorLaser;
import cr0s.warpdrive.block.energy.TileEntityIC2reactorLaserMonitor;
import cr0s.warpdrive.block.force_field.BlockForceField;
import cr0s.warpdrive.block.force_field.BlockForceFieldProjector;
import cr0s.warpdrive.block.force_field.BlockForceFieldRelay;
import cr0s.warpdrive.block.force_field.TileEntityForceField;
import cr0s.warpdrive.block.force_field.TileEntityForceFieldProjector;
import cr0s.warpdrive.block.force_field.TileEntityForceFieldRelay;
import cr0s.warpdrive.block.hull.BlockHullGlass;
import cr0s.warpdrive.block.hull.BlockHullOmnipanel;
import cr0s.warpdrive.block.hull.BlockHullPlain;
import cr0s.warpdrive.block.hull.BlockHullSlab;
import cr0s.warpdrive.block.hull.BlockHullStairs;
import cr0s.warpdrive.block.hull.BlockIridium;
import cr0s.warpdrive.block.movement.BlockLift;
import cr0s.warpdrive.block.movement.BlockShipController;
import cr0s.warpdrive.block.movement.BlockShipCore;
import cr0s.warpdrive.block.movement.BlockTransporterBeacon;
import cr0s.warpdrive.block.movement.BlockTransporterContainment;
import cr0s.warpdrive.block.movement.BlockTransporterCore;
import cr0s.warpdrive.block.movement.BlockTransporterScanner;
import cr0s.warpdrive.block.movement.TileEntityLift;
import cr0s.warpdrive.block.movement.TileEntityShipController;
import cr0s.warpdrive.block.movement.TileEntityShipCore;
import cr0s.warpdrive.block.movement.TileEntityTransporterBeacon;
import cr0s.warpdrive.block.movement.TileEntityTransporterCore;
import cr0s.warpdrive.block.weapon.BlockLaser;
import cr0s.warpdrive.block.weapon.BlockLaserCamera;
import cr0s.warpdrive.block.weapon.BlockWeaponController;
import cr0s.warpdrive.block.weapon.TileEntityLaser;
import cr0s.warpdrive.block.weapon.TileEntityLaserCamera;
import cr0s.warpdrive.block.weapon.TileEntityWeaponController;
import cr0s.warpdrive.client.ClientProxy;
import cr0s.warpdrive.client.ItemGroupHull;
import cr0s.warpdrive.client.ItemGroupMain;
import cr0s.warpdrive.command.CommandBed;
import cr0s.warpdrive.command.CommandDebug;
import cr0s.warpdrive.command.CommandDump;
import cr0s.warpdrive.command.CommandEntity;
import cr0s.warpdrive.command.CommandFind;
import cr0s.warpdrive.command.CommandGenerate;
import cr0s.warpdrive.command.CommandInvisible;
import cr0s.warpdrive.command.CommandNPC;
import cr0s.warpdrive.command.CommandReload;
import cr0s.warpdrive.command.CommandRender;
import cr0s.warpdrive.command.CommandSpace;
import cr0s.warpdrive.config.Recipes;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.damage.DamageAsphyxia;
import cr0s.warpdrive.damage.DamageCold;
import cr0s.warpdrive.damage.DamageIrradiation;
import cr0s.warpdrive.damage.DamageLaser;
import cr0s.warpdrive.damage.DamageShock;
import cr0s.warpdrive.damage.DamageTeleportation;
import cr0s.warpdrive.damage.DamageWarm;
import cr0s.warpdrive.data.CamerasRegistry;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.CloakManager;
import cr0s.warpdrive.data.EnumAirTankTier;
import cr0s.warpdrive.data.EnumComponentType;
import cr0s.warpdrive.data.EnumDecorativeType;
import cr0s.warpdrive.data.EnumForceFieldShape;
import cr0s.warpdrive.data.EnumForceFieldUpgrade;
import cr0s.warpdrive.data.EnumGasColor;
import cr0s.warpdrive.data.EnumHullPlainType;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.data.WarpDriveArmorMaterial;
import cr0s.warpdrive.entity.EntityLaserExploder;
import cr0s.warpdrive.entity.EntityNPC;
import cr0s.warpdrive.entity.EntityOfflineAvatar;
import cr0s.warpdrive.entity.EntityParticleBunch;
import cr0s.warpdrive.entity.EntitySeat;
import cr0s.warpdrive.event.ChatHandler;
import cr0s.warpdrive.event.ChunkHandler;
import cr0s.warpdrive.event.CommonWorldGenerator;
import cr0s.warpdrive.event.EMPReceiver;
import cr0s.warpdrive.event.ItemHandler;
import cr0s.warpdrive.event.LivingHandler;
import cr0s.warpdrive.event.PlayerHandler;
import cr0s.warpdrive.event.WorldHandler;
import cr0s.warpdrive.generators.MyBlockStateProvider;
import cr0s.warpdrive.item.ItemAirTank;
import cr0s.warpdrive.item.ItemComponent;
import cr0s.warpdrive.item.ItemElectromagneticCell;
import cr0s.warpdrive.item.ItemForceFieldShape;
import cr0s.warpdrive.item.ItemForceFieldUpgrade;
import cr0s.warpdrive.item.ItemIC2reactorLaserFocus;
import cr0s.warpdrive.item.ItemPlasmaTorch;
import cr0s.warpdrive.item.ItemShipToken;
import cr0s.warpdrive.item.ItemTuningDriver;
import cr0s.warpdrive.item.ItemTuningFork;
import cr0s.warpdrive.item.ItemWarpArmor;
import cr0s.warpdrive.item.ItemWrench;
import cr0s.warpdrive.network.PacketHandler;
import cr0s.warpdrive.render.EntityCamera;
import cr0s.warpdrive.world.BiomeSpace;
import cr0s.warpdrive.world.EntitySphereGen;
import cr0s.warpdrive.world.EntityStarCore;
import cr0s.warpdrive.world.HyperSpaceDimension;
import cr0s.warpdrive.world.SpaceDimension;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.DyeColor;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.ItemGroup;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.potion.Effect;
import net.minecraft.potion.Potion;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import cr0s.warpdrive.render.TileEntityEnanReactorCoreRenderer;
import cr0s.warpdrive.render.TileEntityShipScannerRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.biome.Biome;

import net.minecraftforge.client.model.generators.ExistingFileHelper;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ModDimension;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.RegisterDimensionsEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.loading.FMLPaths;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Sets;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.authlib.GameProfile;
import org.apache.logging.log4j.core.config.Configurator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@EventBusSubscriber(bus = Bus.MOD)
@Mod(WarpDrive.MODID)
public class WarpDrive {
	public static final String MODID = "warpdrive";
	public static final String MOD_VERSION = "@version@";
	public static final Integer[] MOD_VERSION_NUMBERS;
	public static final String PROTOCOL_VERSION = "@version@";
	@SuppressWarnings("ConstantConditions")
	public static final boolean isDev = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0
	                                 || MOD_VERSION.contains("-dev")
	                                 || !net.minecraftforge.fml.loading.FMLEnvironment.production;
	public static GameProfile gameProfile = new GameProfile(UUID.nameUUIDFromBytes("[WarpDrive]".getBytes()), "[WarpDrive]");
	
	// common blocks and items
	public static Block blockLaser;
	public static Block[] blockChunkLoaders;
	public static Block blockHighlyAdvancedMachine;
	public static Block[] blockLaserMediums;
	public static ItemComponent[] itemComponents;
	
	// atomic blocks and items
	public static Block blockAcceleratorCore;
	public static Block blockAcceleratorControlPoint;
	public static Block blockParticlesCollider;
	public static Block blockParticlesInjector;
	public static Block blockVoidShellPlain;
	public static Block blockVoidShellGlass;
	public static Block[] blockElectromagnets_plain;
	public static Block[] blockElectromagnets_glass;
	public static Block[] blockChillers;
	public static ItemElectromagneticCell[] itemElectromagneticCell;
	public static ItemPlasmaTorch[] itemPlasmaTorch;
	
	// building blocks and items
	public static Block[] blockShipScanners;
	public static Map<Integer, ItemShipToken> mapItemShipTokens;
	
	// breathing
	public static Block blockAirFlow;
	public static Block blockAirSource;
	public static Block[] blockAirShields;
	public static Block[] blockAirGeneratorTiered;
	
	// collection blocks
	public static Block blockMiningLaser;
	public static Block blockLaserTreeFarm;
	
	// decoration
	public static Block blockBedrockGlass;
	public static Block[] blockDecoratives;
	public static Block[] blockGas;
	public static Block blockLamp_bubble;
	public static Block blockLamp_flat;
	public static Block blockLamp_long;
	
	// detection blocks
	public static Block blockBiometricScanner;
	public static Block blockCamera;
	public static Block blockCamouflage;
	public static Block blockCloakingCoil;
	public static Block blockCloakingCore;
	public static Block blockEnvironmentalSensor;
	public static Block blockMonitor;
	public static Block blockRadar;
	public static Block[] blockSirenIndustrials;
	public static Block[] blockSirenMilitaries;
	public static Block[] blockSpeakers;
	public static Block[] blockVirtualAssistants;
	public static Block blockWarpIsolation;
	
	// energy blocks and items
	public static Block[] blockCapacitors;
	public static Block[] blockEnanReactorCores;
	public static Block blockEnanReactorLaser;
	public static Block blockIC2reactorLaserCooler;
	public static Item itemIC2reactorLaserFocus;
	
	// force field blocks and items
	public static Block[][] blockForceFields;
	public static Block[] blockForceFieldHalfProjectors;
	public static Block[] blockForceFieldFullProjectors;
	public static Block[] blockForceFieldRelays;
	public static Block blockSecurityStation;
	public static ItemForceFieldShape[] itemForceFieldShapes;
	public static ItemForceFieldUpgrade[] itemForceFieldUpgrades;
	
	// hull blocks
	public static Block[][][] blockHulls_plain;
	public static Block[][] blockHulls_glass;
	public static Block[][] blockHulls_omnipanel;
	public static Block[][][] blockHulls_stairs;
	public static Block[][][] blockHulls_slab;
	public static Block blockIridium;
	
	// movement blocks
	public static Block blockLift;
	public static Block[] blockShipCores;
	public static Block[] blockShipControllers;
	public static Block blockTransporterBeacon;
	public static Block blockTransporterCore;
	public static Block blockTransporterContainment;
	public static Block blockTransporterScanner;
	
	// weapon blocks
	public static Block blockLaserCamera;
	public static Block blockWeaponController;
	
	public static final IArmorMaterial[] armorMaterials = new IArmorMaterial[EnumTier.length];
	
	// equipment items
	public static ItemAirTank[] itemAirTanks;
	public static ItemTuningFork[] itemTuningForks;
	public static ItemTuningDriver itemTuningDriver;
	public static ItemWrench itemWrench;
	public static ArmorItem[][] itemWarpArmor;
	
	// damage sources
	public static DamageAsphyxia damageAsphyxia;
	public static DamageCold damageCold;
	public static DamageIrradiation damageIrradiation;
	public static DamageLaser damageLaser;
	public static DamageShock damageShock;
	public static DamageTeleportation damageTeleportation;
	public static DamageWarm damageWarm;
	
	// world generation
	public static ModDimension modDimensionHyperspace;
	public static ModDimension modDimensionSpace;
	public static Biome biomeSpace;
	@SuppressWarnings("FieldCanBeLocal")
	private CommonWorldGenerator commonWorldGenerator;
	
	public static Method methodBlock_getSilkTouch = null;
	
	// Client settings
	public static final ItemGroup itemGroupMain = new ItemGroupMain(MODID + ".main");
	public static final ItemGroup itemGroupHull = new ItemGroupHull(MODID + ".hull");
	
	public static CommonProxy proxy = DistExecutor.safeRunForDist(()-> ClientProxy::new, ()->CommonProxy::new);
	
	public static CloakManager cloaks;
	public static CamerasRegistry cameras;
	
	public static final Logger logger = LogManager.getLogger(MODID);
	public static final LoggerPrintStream printStreamError = new LoggerPrintStream(Level.ERROR);
	public static final LoggerPrintStream printStreamWarn = new LoggerPrintStream(Level.WARN);
	public static final LoggerPrintStream printStreamInfo = new LoggerPrintStream(Level.INFO);
	
	static {
		// pre-calculate and sanitize the version numbers
		String[] strings = WarpDrive.MOD_VERSION.split("-");
		if (strings.length < 2) {
			strings = "0.0.0-0.0.0".split("-");
		}
		if (WarpDrive.isDev && strings[strings.length - 1].contains("dev")) {
			strings = strings[strings.length - 2].split("\\.");
		} else {
			strings = strings[strings.length - 1].split("\\.");
		}
		final ArrayList<Integer> integers = new ArrayList<>(strings.length);
		for (final String string : strings) {
			try {
				integers.add(Integer.parseInt(string));
			} catch (final NumberFormatException exception) {
				// ignore
			}
		}
		while (integers.size() < 3) {
			integers.add(0);
		}
		MOD_VERSION_NUMBERS = integers.toArray(new Integer[0]);
	}
	
	public WarpDrive() {
		final String configPath = FMLPaths.CONFIGDIR.get().toAbsolutePath().toString();
		WarpDriveConfig.onModConstruction(configPath);
		
		// TODO MC1.15 Silktouch laser mining
		// methodBlock_getSilkTouch = ReflectionHelper.findMethod(Block.class, "getSilkTouchDrop", "func_180643_i", BlockState.class);
		
		// common blocks and items
		blockChunkLoaders = new Block[EnumTier.length];
		for (final EnumTier enumTier : EnumTier.nonCreative()) {
			final int indexTier = enumTier.getIndex();
			blockChunkLoaders[indexTier] = new BlockChunkLoader("machine.chunk_loader." + enumTier.getName(), enumTier);
		}
		
		blockHighlyAdvancedMachine = new BlockHighlyAdvancedMachine("machine.highly_advanced_machine", EnumTier.BASIC);
		
		blockLaserMediums = new Block[EnumTier.length];
		for (final EnumTier enumTier : EnumTier.nonCreative()) {
			final int indexTier = enumTier.getIndex();
			blockLaserMediums[indexTier] = new BlockLaserMedium("machine.laser_medium." + enumTier.getName(), enumTier);
		}
		
		itemComponents = new ItemComponent[EnumComponentType.length];
		for (final EnumComponentType enumComponentType : EnumComponentType.values()) {
			final int indexType = enumComponentType.ordinal();
			itemComponents[indexType] = new ItemComponent("component." + enumComponentType.getName(), EnumTier.BASIC, enumComponentType);
		}
		
		// 20% more durability, same enchantability (except basic is slightly lower), increased toughness
		armorMaterials[EnumTier.BASIC.getIndex()   ] = WarpDriveArmorMaterial.RUBBER;
		armorMaterials[EnumTier.ADVANCED.getIndex()] = WarpDriveArmorMaterial.CERAMIC;
		armorMaterials[EnumTier.SUPERIOR.getIndex()] = WarpDriveArmorMaterial.CARBON_FIBER;
		
		// atomic blocks and items
		blockAcceleratorCore = new BlockAcceleratorCore("atomic.accelerator_core", EnumTier.BASIC);
		blockAcceleratorControlPoint = new BlockAcceleratorControlPoint("atomic.accelerator_control_point", EnumTier.BASIC);
		blockParticlesCollider = new BlockParticlesCollider("atomic.particles_collider", EnumTier.BASIC);
		blockParticlesInjector = new BlockParticlesInjector("atomic.particles_injector", EnumTier.BASIC);
		blockVoidShellPlain = new BlockVoidShellPlain("atomic.void_shell.plain", EnumTier.BASIC);
		blockVoidShellGlass = new BlockVoidShellGlass("atomic.void_shell.glass", EnumTier.BASIC);
		
		blockElectromagnets_plain = new Block[EnumTier.length];
		blockElectromagnets_glass = new Block[EnumTier.length];
		blockChillers = new Block[EnumTier.length];
		itemElectromagneticCell = new ItemElectromagneticCell[EnumTier.length];
		itemPlasmaTorch = new ItemPlasmaTorch[EnumTier.length];
		for(final EnumTier enumTier : EnumTier.nonCreative()) {
			final int indexTier = enumTier.getIndex();
			blockElectromagnets_plain[indexTier] = new BlockElectromagnetPlain("atomic.electromagnet." + enumTier.getName() + ".plain", enumTier);
			blockElectromagnets_glass[indexTier] = new BlockElectromagnetGlass("atomic.electromagnet." + enumTier.getName() + ".glass", enumTier);
			blockChillers[indexTier] = new BlockChiller("atomic.chiller." + enumTier.getName(), enumTier);
			
			itemElectromagneticCell[indexTier] = new ItemElectromagneticCell("atomic.electromagnetic_cell." + enumTier.getName(), enumTier);
			// itemPlasmaTorch[indexTier] = new ItemPlasmaTorch("plasma_torch." + enumTier.getName(), enumTier);
		}
		
		// building blocks and items
		blockShipScanners = new Block[EnumTier.length];
		for(final EnumTier enumTier : EnumTier.nonCreative()) {
			final int indexTier = enumTier.getIndex();
			blockShipScanners[indexTier] = new BlockShipScanner("building.ship_scanner." + enumTier.getName(), enumTier);
		}
		mapItemShipTokens = new HashMap<>(ItemShipToken.TOKEN_IDs.length);
		for(final int tokenId : ItemShipToken.TOKEN_IDs) {
			mapItemShipTokens.put(tokenId, new ItemShipToken("building.ship_token." + tokenId, EnumTier.BASIC, tokenId));
		}
		
		// breathing blocks and items
		blockAirFlow = new BlockAirFlow("breathing.air_flow", EnumTier.BASIC);
		blockAirSource = new BlockAirSource("breathing.air_source", EnumTier.BASIC);
		blockAirShields = new Block[DyeColor.values().length];
		for (final DyeColor dyeColor : DyeColor.values()) {
			final int indexColor = dyeColor.getId();
//			blockAirShields[indexColor] = new BlockAirShield("breathing.air_shield." + dyeColor.getName(), EnumTier.BASIC, dyeColor);
		}
		
		blockAirGeneratorTiered = new Block[EnumTier.length];
		for(final EnumTier enumTier : EnumTier.nonCreative()) {
			final int indexTier = enumTier.getIndex();
			blockAirGeneratorTiered[indexTier] = new BlockAirGeneratorTiered("breathing.air_generator." + enumTier.getName(), enumTier);
		}
		
		itemAirTanks = new ItemAirTank[4];
		for (final EnumAirTankTier enumAirTankTier : EnumAirTankTier.values()) {
			itemAirTanks[enumAirTankTier.getIndex()] = new ItemAirTank("breathing.air_tank." + enumAirTankTier.getName(), enumAirTankTier);
		}
		
		// collection blocks
		blockMiningLaser = new BlockMiningLaser("collection.mining_laser", EnumTier.BASIC);
		blockLaserTreeFarm = new BlockLaserTreeFarm("collection.laser_tree_farm", EnumTier.BASIC);
		
		// decoration
		blockBedrockGlass = new BlockBedrockGlass("decoration.bedrock_glass", EnumTier.CREATIVE);
		
		blockDecoratives = new Block[EnumDecorativeType.values().length];
		for (final EnumDecorativeType enumDecorativeType : EnumDecorativeType.values()) {
			final int indexType = enumDecorativeType.ordinal();
			blockDecoratives[indexType] = new BlockDecorative("decoration.decorative." + enumDecorativeType.getName(), EnumTier.BASIC, enumDecorativeType);
		}
		
		blockGas = new Block[EnumGasColor.length];
		for (final EnumGasColor enumGasColor : EnumGasColor.values()) {
			final int indexColor = enumGasColor.ordinal();
			blockGas[indexColor] = new BlockGas("decoration.gas." + enumGasColor.getName(), EnumTier.BASIC);
		}
		
		blockLamp_bubble = new BlockLamp_bubble("decoration.lamp_bubble", EnumTier.BASIC);
		blockLamp_flat = new BlockLamp_flat("decoration.lamp_flat", EnumTier.BASIC);
		blockLamp_long = new BlockLamp_long("decoration.lamp_long", EnumTier.BASIC);
		
		// detection blocks
		blockBiometricScanner = new BlockBiometricScanner("detection.biometric_scanner", EnumTier.BASIC);
		blockCamera = new BlockCamera("detection.camera", EnumTier.BASIC);
		blockCamouflage = new BlockCamouflage("detection.camouflage", EnumTier.BASIC);
		blockCloakingCoil = new BlockCloakingCoil("detection.cloaking_coil", EnumTier.BASIC);
		blockCloakingCore = new BlockCloakingCore("detection.cloaking_core", EnumTier.BASIC);
		blockEnvironmentalSensor = new BlockEnvironmentalSensor("detection.environmental_sensor", EnumTier.BASIC);
		blockMonitor = new BlockMonitor("detection.monitor", EnumTier.BASIC);
		blockRadar = new BlockRadar("detection.radar", EnumTier.BASIC);
		
		blockSirenIndustrials = new Block[EnumTier.length];
		blockSirenMilitaries = new Block[EnumTier.length];
		blockSpeakers = new Block[EnumTier.length];
		blockVirtualAssistants = new Block[EnumTier.length];
		for(final EnumTier enumTier : EnumTier.nonCreative()) {
			final int indexTier = enumTier.getIndex();
			blockSirenIndustrials[indexTier] = new BlockSiren("detection.siren_industrial." + enumTier.getName(), enumTier, true);
			blockSirenMilitaries[indexTier] = new BlockSiren("detection.siren_military." + enumTier.getName(), enumTier, false);
			blockSpeakers[indexTier] = new BlockSpeaker("detection.speaker." + enumTier.getName(), enumTier);
			blockVirtualAssistants[indexTier] = new BlockVirtualAssistant("detection.virtual_assistant." + enumTier.getName(), enumTier);
		}
		blockWarpIsolation = new BlockWarpIsolation("detection.warp_isolation", EnumTier.BASIC);
		
		// energy blocks and items
		blockCapacitors = new Block[EnumTier.length];
		for(final EnumTier enumTier : EnumTier.values()) {
			final int indexTier = enumTier.getIndex();
			blockCapacitors[indexTier] = new BlockCapacitor("energy.capacitor." + enumTier.getName(), enumTier);
		}
		blockEnanReactorCores = new Block[EnumTier.length];
		for(final EnumTier enumTier : EnumTier.nonCreative()) {
			final int indexTier = enumTier.getIndex();
			blockEnanReactorCores[indexTier] = new BlockEnanReactorCore("energy.enan_reactor_core." + enumTier.getName(), enumTier);
		}
		blockEnanReactorLaser = new BlockEnanReactorLaser("energy.enan_reactor_laser", EnumTier.BASIC);
		
		if (WarpDriveConfig.isIndustrialCraft2Loaded) {
			blockIC2reactorLaserCooler = new BlockIC2reactorLaserCooler("energy.ic2_reactor_laser_cooler", EnumTier.BASIC);
			itemIC2reactorLaserFocus = new ItemIC2reactorLaserFocus("energy.ic2_reactor_laser_focus", EnumTier.BASIC);
		}
		
		// force field blocks and items
		blockForceFields = new Block[EnumTier.length][16];
		blockForceFieldHalfProjectors = new Block[EnumTier.length];
		blockForceFieldFullProjectors = new Block[EnumTier.length];
		blockForceFieldRelays = new Block[EnumTier.length];
		for(final EnumTier enumTier : EnumTier.nonCreative()) {
			final int indexTier = enumTier.getIndex();
			for (int frequency = 0; frequency < 16; frequency++) {
				blockForceFields[indexTier][frequency] = new BlockForceField("force_field.block." + enumTier.getName() + "." + frequency, enumTier, frequency);
			}
			blockForceFieldHalfProjectors[indexTier] = new BlockForceFieldProjector("force_field.projector." + enumTier.getName() + ".half", enumTier, false);
			blockForceFieldFullProjectors[indexTier] = new BlockForceFieldProjector("force_field.projector." + enumTier.getName() + ".full", enumTier, true);
			blockForceFieldRelays[indexTier] = new BlockForceFieldRelay("force_field.relay." + enumTier.getName(), enumTier);
		}
		blockSecurityStation = new BlockSecurityStation("machine.security_station", EnumTier.BASIC);
		
		itemForceFieldShapes = new ItemForceFieldShape[EnumForceFieldShape.length];
		for (final EnumForceFieldShape forceFieldShape : EnumForceFieldShape.values()) {
			final int indexShape = forceFieldShape.ordinal();
			if (forceFieldShape != EnumForceFieldShape.NONE) {
				itemForceFieldShapes[indexShape] = new ItemForceFieldShape("force_field.shape." + forceFieldShape.getName(), EnumTier.BASIC, forceFieldShape);
			}
		}
		
		itemForceFieldUpgrades = new ItemForceFieldUpgrade[EnumForceFieldUpgrade.length];
		for (final EnumForceFieldUpgrade forceFieldUpgrade : EnumForceFieldUpgrade.values()) {
			final int indexShape = forceFieldUpgrade.ordinal();
			if (forceFieldUpgrade != EnumForceFieldUpgrade.NONE) {
				itemForceFieldUpgrades[indexShape] = new ItemForceFieldUpgrade("force_field.upgrade." + forceFieldUpgrade.getName(), EnumTier.BASIC, forceFieldUpgrade);
			}
		}
		
		// hull blocks
		blockHulls_plain = new Block[EnumTier.length][EnumHullPlainType.length][16];
		blockHulls_glass = new Block[EnumTier.length][16];
		blockHulls_omnipanel = new Block[EnumTier.length][16];
		blockHulls_stairs = new Block[EnumTier.length][EnumHullPlainType.length][16];
		blockHulls_slab = new Block[EnumTier.length][EnumHullPlainType.length][16];
		
		for(final EnumTier enumTier : EnumTier.nonCreative()) {
			final int indexTier = enumTier.getIndex();
			for (final DyeColor dyeColor : DyeColor.values()) {
				final int indexColor = dyeColor.getId();
				for (final EnumHullPlainType hullPlainType : EnumHullPlainType.values()) {
					final int indexType = hullPlainType.ordinal();
					blockHulls_plain[indexTier][indexType][indexColor] = new BlockHullPlain(
							"hull." + enumTier.getName() + "." + hullPlainType.getName() + "." + dyeColor.getName(),
							enumTier, hullPlainType, dyeColor );
					blockHulls_stairs[indexTier][indexType][indexColor] = new BlockHullStairs(
							"hull." + enumTier.getName() + ".stairs." + hullPlainType.getName() + "." + dyeColor.getName(),
							blockHulls_plain[indexTier][indexType][indexColor].getDefaultState() );
					blockHulls_slab[indexTier][indexType][indexColor] = new BlockHullSlab(
							"hull." + enumTier.getName() + ".slab." + hullPlainType.getName() + "." + dyeColor.getName(),
							blockHulls_plain[indexTier][0][indexColor].getDefaultState() );
				}
				blockHulls_glass[indexTier][indexColor] = new BlockHullGlass(
						"hull." + enumTier.getName() + ".glass." + dyeColor.getName(), enumTier, dyeColor );
//				blockHulls_omnipanel[indexTier][indexColor] = new BlockHullOmnipanel(
//						"hull." + enumTier.getName() + ".omnipanel." + dyeColor.getName(), enumTier, dyeColor );
			}
		}
		
		blockIridium = new BlockIridium("hull.iridium_block", EnumTier.BASIC);
		
		// movement blocks
		blockLift = new BlockLift("movement.lift", EnumTier.BASIC);
		
		blockShipControllers = new Block[EnumTier.length];
		blockShipCores = new Block[EnumTier.length];
		for(final EnumTier enumTier : EnumTier.nonCreative()) {
			final int indexTier = enumTier.getIndex();
			blockShipControllers[indexTier] = new BlockShipController("movement.ship_controller." + enumTier.getName(), enumTier);
			blockShipCores[indexTier] = new BlockShipCore("movement.ship_core." + enumTier.getName(), enumTier);
		}
		
		blockTransporterBeacon = new BlockTransporterBeacon("movement.transporter_beacon", EnumTier.BASIC);
		blockTransporterContainment = new BlockTransporterContainment("movement.transporter_containment", EnumTier.BASIC);
		blockTransporterCore = new BlockTransporterCore("movement.transporter_core", EnumTier.BASIC);
		blockTransporterScanner = new BlockTransporterScanner("movement.transporter_scanner", EnumTier.BASIC);
		
		// weapon blocks
		blockLaser = new BlockLaser("weapon.laser", EnumTier.BASIC);
		blockLaserCamera = new BlockLaserCamera("weapon.laser_camera", EnumTier.BASIC);
		blockWeaponController = new BlockWeaponController("weapon.weapon_controller", EnumTier.BASIC);
		
		// equipment items
		itemTuningForks = new ItemTuningFork[16];
		for (final DyeColor dyeColor : DyeColor.values()) {
			final int indexColor = dyeColor.getId();
			itemTuningForks[indexColor] = new ItemTuningFork("tool.tuning_fork." + dyeColor.getName(), EnumTier.BASIC);
		}
		itemTuningDriver = new ItemTuningDriver("tool.tuning_driver", EnumTier.ADVANCED);
		itemWrench = new ItemWrench("tool.wrench", EnumTier.BASIC);
		
		itemWarpArmor = new ArmorItem[EnumTier.length][4];
		for(final EnumTier enumTier : EnumTier.nonCreative()) {
			final int indexTier = enumTier.getIndex();
			itemWarpArmor[indexTier][EquipmentSlotType.HEAD.getIndex() ] = new ItemWarpArmor("armor." + enumTier.getName() + "." + ItemWarpArmor.suffixes[EquipmentSlotType.HEAD.getIndex() ], enumTier, armorMaterials[indexTier], EquipmentSlotType.HEAD );
			itemWarpArmor[indexTier][EquipmentSlotType.CHEST.getIndex()] = new ItemWarpArmor("armor." + enumTier.getName() + "." + ItemWarpArmor.suffixes[EquipmentSlotType.CHEST.getIndex()], enumTier, armorMaterials[indexTier], EquipmentSlotType.CHEST);
			itemWarpArmor[indexTier][EquipmentSlotType.LEGS.getIndex() ] = new ItemWarpArmor("armor." + enumTier.getName() + "." + ItemWarpArmor.suffixes[EquipmentSlotType.LEGS.getIndex() ], enumTier, armorMaterials[indexTier], EquipmentSlotType.LEGS );
			itemWarpArmor[indexTier][EquipmentSlotType.FEET.getIndex() ] = new ItemWarpArmor("armor." + enumTier.getName() + "." + ItemWarpArmor.suffixes[EquipmentSlotType.FEET.getIndex() ], enumTier, armorMaterials[indexTier], EquipmentSlotType.FEET );
		}
		
		// damage sources
		damageAsphyxia = new DamageAsphyxia();
		damageCold = new DamageCold();
		damageIrradiation = new DamageIrradiation();
		damageLaser = new DamageLaser();
		damageShock = new DamageShock();
		damageTeleportation = new DamageTeleportation();
		damageWarm = new DamageWarm();
		
		// entities
		// (done in the event handler)
		
		// world generation
		modDimensionHyperspace = ModDimension.withFactory(HyperSpaceDimension::new).setRegistryName("warpdrive:mod_dimension.hyperspace");
		modDimensionSpace      = ModDimension.withFactory(SpaceDimension::new).setRegistryName("warpdrive:mod_dimension.space");
		biomeSpace = new BiomeSpace("warpdrive:space");
		register(biomeSpace);
		/* TODO MC1.15 world generation
		commonWorldGenerator = new CommonWorldGenerator();
		GameRegistry.registerWorldGenerator(commonWorldGenerator, 0);
		*/
		
		// Event handlers
		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.register(new ChatHandler());
		MinecraftForge.EVENT_BUS.register(new ChunkHandler());
		MinecraftForge.EVENT_BUS.register(new ItemHandler());
		MinecraftForge.EVENT_BUS.register(new LivingHandler());
		MinecraftForge.EVENT_BUS.register(new PlayerHandler());
		MinecraftForge.EVENT_BUS.register(new WorldHandler());
		if (WarpDriveConfig.isICBMClassicLoaded) {
			MinecraftForge.EVENT_BUS.register(EMPReceiver.class);
		}
		
		// Networking
		PacketHandler.init();
		
		// Internal registries
		cloaks = new CloakManager();
		cameras = new CamerasRegistry();
		
		proxy.onModConstruction();
	}
	
	@SubscribeEvent
	public static void onModConfigLoading(@Nonnull final ModConfig.Loading event) {
		WarpDriveConfig.onModConfigLoading(event);
	}
	
	@SubscribeEvent
	public static void onGatherData(@Nonnull final GatherDataEvent gatherDataEvent) {
		final ExistingFileHelper helper = gatherDataEvent.getExistingFileHelper();
		if (gatherDataEvent.includeServer()) {
			// gatherDataEvent.getGenerator().addProvider(new BlockLootProvider(gatherDataEvent.getGenerator()));
			// BlockTagProvider blockTagProvider = new BlockTagProvider(gatherDataEvent.getGenerator(), helper);
			// gatherDataEvent.getGenerator().addProvider(blockTagProvider);
			// gatherDataEvent.getGenerator().addProvider(new ItemTagProvider(gatherDataEvent.getGenerator(), blockTagProvider, helper));
			// gatherDataEvent.getGenerator().addProvider(new EntityTagProvider(gatherDataEvent.getGenerator(), helper));
			// gatherDataEvent.getGenerator().addProvider(new StoneCuttingProvider(gatherDataEvent.getGenerator()));
			// gatherDataEvent.getGenerator().addProvider(new RecipeProvider(gatherDataEvent.getGenerator()));
			// gatherDataEvent.getGenerator().addProvider(new SmeltingProvider(gatherDataEvent.getGenerator()));
			// gatherDataEvent.getGenerator().addProvider(new BrewProvider(gatherDataEvent.getGenerator()));
		}
		if (gatherDataEvent.includeClient()) {
			gatherDataEvent.getGenerator().addProvider(new MyBlockStateProvider(gatherDataEvent.getGenerator(), helper));
			// gatherDataEvent.getGenerator().addProvider(new MyItemModelProvider(gatherDataEvent.getGenerator(), helper));
		}
	}
	
	@SubscribeEvent
	public static void onCommonSetup(@Nonnull final FMLCommonSetupEvent event) {
		WarpDriveConfig.onFMLCommonSetup();
		
		if (WarpDriveConfig.isComputerCraftLoaded) {
			final WarpDrivePeripheralHandler peripheralHandler = new WarpDrivePeripheralHandler();
			peripheralHandler.register();
		}
	}
	
	@SubscribeEvent
	public void onFMLServerStarting(@Nonnull final FMLServerStartingEvent event) {
		Configurator.setLevel("net.minecraft.command", Level.DEBUG); // For printing command errors to console
		
		CommandBed.register(event.getCommandDispatcher());
		CommandDebug.register(event.getCommandDispatcher());
		CommandDump.register(event.getCommandDispatcher());
		CommandEntity.register(event.getCommandDispatcher());
		CommandFind.register(event.getCommandDispatcher());
		CommandGenerate.register(event.getCommandDispatcher());
		CommandInvisible.register(event.getCommandDispatcher());
		CommandNPC.register(event.getCommandDispatcher());
		CommandReload.register(event.getCommandDispatcher());
		CommandRender.register(event.getCommandDispatcher());
		CommandSpace.register(event.getCommandDispatcher());
	}
	
	@SubscribeEvent
	public static void onClientSetup(@Nonnull final FMLClientSetupEvent event) {
		
		for (final EnumTier enumTier : EnumTier.nonCreative()) {
			// Reactor core TESR for Advanced & Superior tiers only
			final Block blockEnanReactorCore = blockEnanReactorCores[enumTier.getIndex()];
			if ( blockEnanReactorCore != null
			  && enumTier != EnumTier.BASIC ) {
				//noinspection unchecked
				ClientRegistry.bindTileEntityRenderer(
						(TileEntityType<TileEntityEnanReactorCore>) TileEntityAbstractBase.getTileEntityType((IBlockBase) blockEnanReactorCore),
						TileEntityEnanReactorCoreRenderer::new);
			}

			// Ship scanner TESR for all tiers
			final Block blockShipScanner = blockShipScanners[enumTier.getIndex()];
			if (blockShipScanner != null) {
				//noinspection unchecked
				ClientRegistry.bindTileEntityRenderer(
						(TileEntityType<TileEntityShipScanner>) TileEntityAbstractBase.getTileEntityType((IBlockBase) blockShipScanner),
						TileEntityShipScannerRenderer::new);
			}
		}
		
		// Block rendering layers
		RenderTypeLookup.setRenderLayer(WarpDrive.blockAirSource, RenderType.getTranslucent());
		RenderTypeLookup.setRenderLayer(WarpDrive.blockAirFlow, RenderType.getTranslucent());
		RenderTypeLookup.setRenderLayer(WarpDrive.blockCamouflage, RenderType.getTranslucent());
		RenderTypeLookup.setRenderLayer(WarpDrive.blockLamp_bubble, RenderType.getTranslucent());
		RenderTypeLookup.setRenderLayer(WarpDrive.blockDecoratives[EnumDecorativeType.GLASS.ordinal()], RenderType.getTranslucent());
		RenderTypeLookup.setRenderLayer(WarpDrive.blockTransporterBeacon, RenderType.getCutout());
		RenderTypeLookup.setRenderLayer(WarpDrive.blockVoidShellGlass, RenderType.getTranslucent());
		
		for (final EnumGasColor gasColor : EnumGasColor.values()) {
			final int indexColor = gasColor.ordinal();
			RenderTypeLookup.setRenderLayer(WarpDrive.blockGas[indexColor], RenderType.getTranslucent());
		}
		
		for(final EnumTier enumTier : EnumTier.nonCreative()) {
			final int indexTier = enumTier.getIndex();
			RenderTypeLookup.setRenderLayer(WarpDrive.blockElectromagnets_glass[indexTier], RenderType.getTranslucent());
			
			for (final DyeColor dyeColor : DyeColor.values()) {
				final int indexColor = dyeColor.getId();
				RenderTypeLookup.setRenderLayer(WarpDrive.blockForceFields[indexTier][indexColor], RenderType.getTranslucent());
				RenderTypeLookup.setRenderLayer(WarpDrive.blockHulls_glass[indexTier][indexColor], RenderType.getTranslucent());
//				RenderTypeLookup.setRenderLayer(WarpDrive.blockHulls_omnipanel[indexTier][indexColor], RenderType.getTranslucent());
			}
		}
	}
	
	final public static ArrayList<Biome> biomes = new ArrayList<>(10);
	final public static ArrayList<Block> blocks = new ArrayList<>(100);
	final public static ArrayList<Enchantment> enchantments = new ArrayList<>(10);
	final public static ArrayList<Item> items = new ArrayList<>(50);
	final public static ArrayList<Effect> potions = new ArrayList<>(10);
	final public static ArrayList<Potion> potionTypes = new ArrayList<>(10);
	final public static ArrayList<SoundEvent> soundEvents = new ArrayList<>(100);
	final public static HashMap<ResourceLocation, IRecipe<?>> recipes = new HashMap<>(100);
	final public static ArrayList<VillagerProfession> villagerProfessions = new ArrayList<>(10);
	
	// Register a Biome.
	public static <BIOME extends Biome> BIOME register(@Nonnull final BIOME biome) {
		biomes.add(biome);
		return biome;
	}
	
	// Register a Block with the default ItemBlock class.
	public static <BLOCK extends Block> BLOCK register(@Nonnull final BLOCK block) {
		assert block instanceof IBlockBase;
		return register(block, ((IBlockBase) block).createItemBlock());
	}
	
	// Register a Block with a custom ItemBlock class.
	public static <BLOCK extends Block> BLOCK register(@Nonnull final BLOCK block, @Nullable final BlockItem itemBlock) {
		final ResourceLocation resourceLocation = block.getRegistryName();
		if (resourceLocation == null) {
			WarpDrive.logger.error(String.format("Missing registry name for block %s, ignoring registration...",
			                                     block));
			return block;
		}
		
		assert !blocks.contains(block);
		blocks.add(block);
		
		if (itemBlock != null) {
			itemBlock.setRegistryName(resourceLocation);
			register(itemBlock);
		}
		
		return block;
	}
	
	// Register an Enchantment.
	public static <ENCHANTMENT extends Enchantment> ENCHANTMENT register(@Nonnull final ENCHANTMENT enchantment) {
		enchantments.add(enchantment);
		return enchantment;
	}
	
	// Register an Item.
	public static <ITEM extends Item> ITEM register(@Nonnull final ITEM item) {
		items.add(item);
		return item;
	}
	
	// Register an Potion.
	public static <POTION extends Effect> POTION register(@Nonnull final POTION potion) {
		potions.add(potion);
		return potion;
	}
	
	// Register an PotionType.
	public static <POTION_TYPE extends Potion> POTION_TYPE register(@Nonnull final POTION_TYPE potionType) {
		potionTypes.add(potionType);
		return potionType;
	}
	
	// Register a recipe.
	@Nonnull
	public static <RECIPE extends IRecipe<?>> RECIPE register(@Nonnull final RECIPE recipe) {
		ResourceLocation registryName = recipe.getId();
		if (recipes.containsKey(registryName)) {
			throw new RuntimeException(String.format("Overlapping recipe detected, please report this to the mod author %s",
			                                     registryName));
		}
		recipes.put(registryName, recipe);
		return recipe;
	}
	
	// Register a SoundEvent.
	public static <SOUND_EVENT extends SoundEvent> SOUND_EVENT register(@Nonnull final SOUND_EVENT soundEvent) {
		soundEvents.add(soundEvent);
		return soundEvent;
	}
	
	// Register a VillagerProfession.
	public static <VILLAGER_PROFESSION extends VillagerProfession> VILLAGER_PROFESSION register(@Nonnull final VILLAGER_PROFESSION villagerProfession) {
		villagerProfessions.add(villagerProfession);
		return villagerProfession;
	}
	
	@SubscribeEvent
	public static void onRegisterBiomes(@Nonnull final RegistryEvent.Register<Biome> event) {
		LocalProfiler.start(String.format("Registering %s", event.getName()));
		
		for (final Biome biome : biomes) {
			event.getRegistry().register(biome);
		}
		
		BiomeDictionary.addTypes(biomeSpace, BiomeDictionary.Type.DEAD, BiomeDictionary.Type.WASTELAND);
		
		LocalProfiler.stop(1000);
	}
	
	@SubscribeEvent
	public static void onRegisterBlocks(@Nonnull final RegistryEvent.Register<Block> event) {
		LocalProfiler.start(String.format("Registering %s", event.getName()));
		
		for (final Block block : blocks) {
			event.getRegistry().register(block);
		}
	}
	
	@SubscribeEvent
	public static void onRegisterEnchantments(@Nonnull final RegistryEvent.Register<Enchantment> event) {
		LocalProfiler.start(String.format("Registering %s", event.getName()));
		
		for (final Enchantment enchantment : enchantments) {
			event.getRegistry().register(enchantment);
		}
		
		LocalProfiler.stop(1000);
	}
	
	@SubscribeEvent
	public static void onRegisterEntities(@Nonnull final RegistryEvent.Register<EntityType<?>> event) {
		LocalProfiler.start(String.format("Registering %s", event.getName()));
		
		event.getRegistry().register(EntitySphereGen.TYPE);
		event.getRegistry().register(EntityStarCore.TYPE);
		event.getRegistry().register(EntityCamera.TYPE);
		event.getRegistry().register(EntityParticleBunch.TYPE);
		event.getRegistry().register(EntityLaserExploder.TYPE);
		event.getRegistry().register(EntityNPC.TYPE);
		event.getRegistry().register(EntityOfflineAvatar.TYPE);
		event.getRegistry().register(EntitySeat.TYPE);
		
		LocalProfiler.stop(1000);
	}
	
	@SubscribeEvent
	public static void onRegisterItems(@Nonnull final RegistryEvent.Register<Item> event) {
		LocalProfiler.start(String.format("Registering %s", event.getName()));
		
		for (final Item item : items) {
			event.getRegistry().register(item);
			proxy.onModelInitialisation(item);
		}
		for (final Block block : blocks) {
			proxy.onModelInitialisation(block);
		}
		
		LocalProfiler.stop(1000);
	}
	
	@SubscribeEvent
	public static void onRegisterModDimensions(@Nonnull final RegistryEvent.Register<ModDimension> event) {
		LocalProfiler.start(String.format("Registering %s", event.getName()));
		
		event.getRegistry().register(modDimensionHyperspace);
		event.getRegistry().register(modDimensionSpace);
		
		LocalProfiler.stop(1000);
	}
	
	@SubscribeEvent
	public void onRegisterDimensions(@Nonnull final RegisterDimensionsEvent event) {
		LocalProfiler.start("RegisterDimensionsEvent");
		
		final Set<ResourceLocation> missings = event.getMissingNames();
		for (final ResourceLocation resourceLocation : missings) {
			WarpDrive.logger.info(String.format("Missing dimension %s", resourceLocation));
		}
		CelestialObjectManager.onRegisterDimensions();
		
		LocalProfiler.stop(1000);
	}
	
	@SubscribeEvent
	public static void onRegisterPotions(@Nonnull final RegistryEvent.Register<Effect> event) {
		LocalProfiler.start(String.format("Registering %s", event.getName()));
		
		for (final Effect potion : potions) {
			event.getRegistry().register(potion);
		}
		
		LocalProfiler.stop(1000);
	}
	
	@SubscribeEvent
	public static void onRegisterPotionTypes(@Nonnull final RegistryEvent.Register<Potion> event) {
		LocalProfiler.start(String.format("Registering %s", event.getName()));
		
		for (final Potion potionType : potionTypes) {
			event.getRegistry().register(potionType);
		}
		
		LocalProfiler.stop(1000);
	}
	
	@SubscribeEvent
	public static void onRegisterRecipes(@Nonnull final RegistryEvent.Register<IRecipeSerializer<?>> event) {
		LocalProfiler.start(String.format("Registering %s step 1", event.getName()));
		
		Recipes.initDynamic();
		
		LocalProfiler.stop(1000);
		
		LocalProfiler.start(String.format("Registering %s step 2", event.getName()));
		
		for (final IRecipe<?> recipe : recipes.values()) {
			// TODO MC1.15 Recipe registration
			// event.getRegistry().register(recipe);
		}
		
		LocalProfiler.stop(1000);
	}
	
	@SubscribeEvent
	public static void onRegisterSoundEvents(@Nonnull final RegistryEvent.Register<SoundEvent> event) {
		LocalProfiler.start(String.format("Registering %s", event.getName()));
		
		cr0s.warpdrive.data.SoundEvents.registerSounds();
		for (final SoundEvent soundEvent : soundEvents) {
			event.getRegistry().register(soundEvent);
		}
		
		LocalProfiler.stop(1000);
	}
	
	// Note: tier information is stored in the block itself since 1.13 (previously, it was the block attribute/state).
	// On 1.12, onLoad() wasn't always called, several workarounds were required to get the tier.
	// On 1.15, we can't access the world during the onLoad() event while reloading a save.
	// Instead, we register different types for each individual block and pass the block during the construction.
	private static <T extends TileEntity> void doRegisterTileEntity(
			@Nonnull final RegistryEvent.Register<TileEntityType<?>> event,
			@Nonnull final Class<T> classTileEntity,
			@Nonnull final Block[]... blockArrays ) {
		for (final Block[] blockArray : blockArrays) {
			for (final Block block : blockArray) {
				doRegisterTileEntity(event, classTileEntity, block);
			}
		}
	}
	
	private static <T extends TileEntity> void doRegisterTileEntity(
			@Nonnull final RegistryEvent.Register<TileEntityType<?>> event,
			@Nonnull final Class<T> classTileEntity,
			@Nullable final Block block ) {
		// skip non instantiated blocks
		if (block == null) {
			return;
		}
		// create the type
		final TileEntityType<T> tileEntityType = new TileEntityType<>(() -> {
			try {
				final Constructor<T> constructor = classTileEntity.getConstructor(IBlockBase.class);
				return constructor.newInstance((IBlockBase) block);
			} catch (final Exception exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.error(String.format("Failed to instantiate a new tile entity named %s:%s",
				                                     WarpDrive.MODID, block.getRegistryName()));
				throw new RuntimeException(exception);
			}
		}, Sets.newHashSet(blocks), null);
		assert block.getRegistryName() != null;
		tileEntityType.setRegistryName(block.getRegistryName());
		
		// save in the base class lookup during instantiation
		TileEntityAbstractBase.register((IBlockBase) block, tileEntityType);
		
		// actually register the type
		event.getRegistry().register(tileEntityType);
	}
	
	@SubscribeEvent
	public static void onRegisterTileEntities(@Nonnull final RegistryEvent.Register<TileEntityType<?>> event) {
		LocalProfiler.start(String.format("Registering %s", event.getName()));
		
		doRegisterTileEntity(event, TileEntityAcceleratorCore.class, blockAcceleratorCore);
		doRegisterTileEntity(event, TileEntityAcceleratorControlPoint.class, blockAcceleratorControlPoint);
		doRegisterTileEntity(event, TileEntityAirGeneratorTiered.class, blockAirGeneratorTiered);
		doRegisterTileEntity(event, TileEntityBiometricScanner.class, blockBiometricScanner);
		doRegisterTileEntity(event, TileEntityCamera.class, blockCamera);
		doRegisterTileEntity(event, TileEntityCamouflage.class, blockCamouflage);
		doRegisterTileEntity(event, TileEntityCapacitor.class, blockCapacitors);
		doRegisterTileEntity(event, TileEntityChunkLoader.class, blockChunkLoaders);
		doRegisterTileEntity(event, TileEntityCloakingCore.class, blockCloakingCore);
		doRegisterTileEntity(event, TileEntityEnanReactorCore.class, blockEnanReactorCores);
		doRegisterTileEntity(event, TileEntityEnanReactorLaser.class, blockEnanReactorLaser);
		doRegisterTileEntity(event, TileEntityEnvironmentalSensor.class, blockEnvironmentalSensor);
		doRegisterTileEntity(event, TileEntityForceField.class, blockForceFields);
		doRegisterTileEntity(event, TileEntityForceFieldProjector.class, blockForceFieldHalfProjectors, blockForceFieldFullProjectors);
		doRegisterTileEntity(event, TileEntityForceFieldRelay.class, blockForceFieldRelays);
		doRegisterTileEntity(event, TileEntityIC2reactorLaserMonitor.class, blockIC2reactorLaserCooler);
		// doRegisterTileEntity(event, TileEntityJumpGateCore.class, blockJumpGateCores);
		doRegisterTileEntity(event, TileEntityLaser.class, blockLaser);
		doRegisterTileEntity(event, TileEntityLaserCamera.class, blockLaserCamera);
		doRegisterTileEntity(event, TileEntityLaserMedium.class, blockLaserMediums);
		doRegisterTileEntity(event, TileEntityLaserTreeFarm.class, blockLaserTreeFarm);
		doRegisterTileEntity(event, TileEntityLift.class, blockLift);
		doRegisterTileEntity(event, TileEntityMiningLaser.class, blockMiningLaser);
		doRegisterTileEntity(event, TileEntityMonitor.class, blockMonitor);
		doRegisterTileEntity(event, TileEntityParticlesInjector.class, blockParticlesInjector);
		doRegisterTileEntity(event, TileEntityRadar.class, blockRadar);
		doRegisterTileEntity(event, TileEntitySecurityStation.class, blockSecurityStation);
		doRegisterTileEntity(event, TileEntityShipController.class, blockShipControllers);
		doRegisterTileEntity(event, TileEntityShipCore.class, blockShipCores);
		doRegisterTileEntity(event, TileEntityShipScanner.class, blockShipScanners);
		doRegisterTileEntity(event, TileEntitySiren.class, blockSirenIndustrials, blockSirenMilitaries);
		doRegisterTileEntity(event, TileEntitySpeaker.class, blockSpeakers);
		doRegisterTileEntity(event, TileEntityTransporterBeacon.class, blockTransporterBeacon);
		doRegisterTileEntity(event, TileEntityTransporterCore.class, blockTransporterCore);
		doRegisterTileEntity(event, TileEntityVirtualAssistant.class, blockVirtualAssistants);
		doRegisterTileEntity(event, TileEntityWeaponController.class, blockWeaponController);
		
		LocalProfiler.stop(1000);
	}
	
	@SubscribeEvent
	public static void onRegisterVillagerProfessions(@Nonnull final RegistryEvent.Register<VillagerProfession> event) {
		LocalProfiler.start(String.format("Registering %s", event.getName()));
		
		for (final VillagerProfession villagerProfession : villagerProfessions) {
			event.getRegistry().register(villagerProfession);
		}
		
		LocalProfiler.stop(1000);
	}
}