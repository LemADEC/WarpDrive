package cr0s.warpdrive;

import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.block.BlockChunkLoader;
import cr0s.warpdrive.block.BlockLaser;
import cr0s.warpdrive.block.BlockLaserMedium;
import cr0s.warpdrive.block.BlockSecurityStation;
import cr0s.warpdrive.block.TileEntityChunkLoader;
import cr0s.warpdrive.block.TileEntityLaser;
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
import cr0s.warpdrive.block.forcefield.BlockForceField;
import cr0s.warpdrive.block.forcefield.BlockForceFieldProjector;
import cr0s.warpdrive.block.forcefield.BlockForceFieldRelay;
import cr0s.warpdrive.block.forcefield.TileEntityForceField;
import cr0s.warpdrive.block.forcefield.TileEntityForceFieldProjector;
import cr0s.warpdrive.block.forcefield.TileEntityForceFieldRelay;
import cr0s.warpdrive.block.hull.BlockHullGlass;
import cr0s.warpdrive.block.hull.BlockHullOmnipanel;
import cr0s.warpdrive.block.hull.BlockHullPlain;
import cr0s.warpdrive.block.hull.BlockHullSlab;
import cr0s.warpdrive.block.hull.BlockHullStairs;
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
import cr0s.warpdrive.block.passive.BlockHighlyAdvancedMachine;
import cr0s.warpdrive.block.passive.BlockIridium;
import cr0s.warpdrive.block.weapon.BlockLaserCamera;
import cr0s.warpdrive.block.weapon.BlockWeaponController;
import cr0s.warpdrive.block.weapon.TileEntityLaserCamera;
import cr0s.warpdrive.block.weapon.TileEntityWeaponController;
import cr0s.warpdrive.client.CreativeTabHull;
import cr0s.warpdrive.client.CreativeTabMain;
import cr0s.warpdrive.command.CommandDebug;
import cr0s.warpdrive.command.CommandDump;
import cr0s.warpdrive.command.CommandEntity;
import cr0s.warpdrive.command.CommandFind;
import cr0s.warpdrive.command.CommandGenerate;
import cr0s.warpdrive.command.CommandBed;
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
import cr0s.warpdrive.data.EnumHullPlainType;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.entity.EntityLaserExploder;
import cr0s.warpdrive.entity.EntityNPC;
import cr0s.warpdrive.entity.EntityOfflineAvatar;
import cr0s.warpdrive.entity.EntityParticleBunch;
import cr0s.warpdrive.entity.EntitySeat;
import cr0s.warpdrive.event.ChatHandler;
import cr0s.warpdrive.event.ChunkHandler;
import cr0s.warpdrive.event.ChunkLoadingHandler;
import cr0s.warpdrive.event.CommonWorldGenerator;
import cr0s.warpdrive.event.WorldHandler;
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
import cr0s.warpdrive.world.HyperSpaceWorldProvider;
import cr0s.warpdrive.world.SpaceWorldProvider;

import net.minecraft.block.Block;
import net.minecraft.block.BlockColored;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.DimensionType;
import net.minecraft.world.biome.Biome;

import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerProfession;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import com.mojang.authlib.GameProfile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Mod(modid = WarpDrive.MODID,
     name = "WarpDrive",
     version = WarpDrive.VERSION,
     dependencies = ""
                  + "required-after:forge@[14.23.5.2817,);"
                  + "after:appliedenergistics;"
                  + "after:cofhcore;"
                  + "after:ccturtle;"
                  + "after:computercraft@[1.82.1,);"
                  + "after:enderio@[5.1.52,);"
                  + "after:gregtech;"
                  + "after:ic2;"
                  + "after:icbmclassic@[1.12.2-4.0.0,);"
                  + "after:matteroverdrive;"
                  + "after:metalchests@[v5.8.1,);"
                  + "after:opencomputers;"
//                + "after:sgcraft@[2.0.0,);"
		,
     certificateFingerprint = "f7be6b40743c6a8205df86c5e57547d578605d8a"
)
public class WarpDrive {
	public static final String MODID = "warpdrive";
	public static final String VERSION = "@version@";
	public static final Integer[] VERSION_NUMBERS;
	@SuppressWarnings("ConstantConditions")
	public static final boolean isDev = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0
	                                 || VERSION.contains("-dev");
	public static GameProfile gameProfile = new GameProfile(UUID.nameUUIDFromBytes("[WarpDrive]".getBytes()), "[WarpDrive]");
	
	// common blocks and items
	public static Block blockLaser;
	public static Block[] blockChunkLoaders;
	public static Block[] blockLaserMediums;
	public static ItemComponent itemComponent;
	
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
	public static ItemShipToken itemShipToken;
	
	// breathing
	public static Block blockAirFlow;
	public static Block blockAirSource;
	public static Block blockAirShield;
	public static Block[] blockAirGeneratorTiered;
	
	// collection blocks
	public static Block blockMiningLaser;
	public static Block blockLaserTreeFarm;
	
	// decoration
	public static Block blockBedrockGlass;
	public static Block blockDecorative;
	public static Block blockGas;
	public static Block blockLamp_bubble;
	public static Block blockLamp_flat;
	public static Block blockLamp_long;
	
	// detection blocks
	public static Block blockBiometricScanner;
	public static Block blockCamera;
	public static Block blockCloakingCoil;
	public static Block blockCloakingCore;
	public static Block blockEnvironmentalSensor;
	public static Block blockMonitor;
	public static Block blockRadar;
	public static Block[] blockSirenIndustrial;
	public static Block[] blockSirenMilitary;
	public static Block[] blockSpeaker;
	public static Block[] blockVirtualAssistant;
	public static Block blockWarpIsolation;
	
	// energy blocks and items
	public static Block[] blockCapacitor;
	public static Block[] blockEnanReactorCores;
	public static Block blockEnanReactorLaser;
	public static Block blockIC2reactorLaserCooler;
	public static Item itemIC2reactorLaserFocus;
	
	// force field blocks and items
	public static Block[] blockForceFields;
	public static Block[] blockForceFieldProjectors;
	public static Block[] blockForceFieldRelays;
	public static Block blockSecurityStation;
	public static ItemForceFieldShape itemForceFieldShape;
	public static ItemForceFieldUpgrade itemForceFieldUpgrade;
	
	// hull blocks
	public static Block[][] blockHulls_plain;
	public static Block[] blockHulls_glass;
	public static Block[] blockHulls_omnipanel;
	public static Block[][] blockHulls_stairs;
	public static Block[][] blockHulls_slab;
	
	// movement blocks
	public static Block blockLift;
	public static Block[] blockShipCores;
	public static Block[] blockShipControllers;
	public static Block blockTransporterBeacon;
	public static Block blockTransporterCore;
	public static Block blockTransporterContainment;
	public static Block blockTransporterScanner;
	
	// passive blocks
	public static Block blockHighlyAdvancedMachine;
	public static Block blockIridium;
	
	// weapon blocks
	public static Block blockLaserCamera;
	public static Block blockWeaponController;
	
	public static final ArmorMaterial[] armorMaterial = new ArmorMaterial[EnumTier.length];
	
	// equipment items
	public static ItemAirTank[] itemAirTanks;
	public static ItemTuningFork itemTuningFork;
	public static ItemTuningDriver itemTuningDriver;
	public static ItemWrench itemWrench;
	public static ItemArmor[][] itemWarpArmor;
	
	// damage sources
	public static DamageAsphyxia damageAsphyxia;
	public static DamageCold damageCold;
	public static DamageIrradiation damageIrradiation;
	public static DamageLaser damageLaser;
	public static DamageShock damageShock;
	public static DamageTeleportation damageTeleportation;
	public static DamageWarm damageWarm;
	
	// world generation
	public static Biome biomeSpace;
	public static DimensionType dimensionTypeSpace;
	public static DimensionType dimensionTypeHyperSpace;
	@SuppressWarnings("FieldCanBeLocal")
	private CommonWorldGenerator commonWorldGenerator;
	
	public static Method methodBlock_getSilkTouch = null;
	
	// Client settings
	public static final CreativeTabs creativeTabMain = new CreativeTabMain(MODID.toLowerCase() + ".main");
	public static final CreativeTabs creativeTabHull = new CreativeTabHull(MODID.toLowerCase() + ".hull");
	
	@Instance(WarpDrive.MODID)
	public static WarpDrive instance;
	@SidedProxy(clientSide = "cr0s.warpdrive.client.ClientProxy", serverSide = "cr0s.warpdrive.CommonProxy")
	public static CommonProxy proxy;
	
	public static CloakManager cloaks;
	public static CamerasRegistry cameras;
	
	@SuppressWarnings("FieldCanBeLocal")
	private static WarpDrivePeripheralHandler peripheralHandler = null;
	
	public static Logger logger;
	public static LoggerPrintStream printStreamError;
	public static LoggerPrintStream printStreamWarn;
	public static LoggerPrintStream printStreamInfo;
	
	static {
		// pre-calculate and sanitize the version numbers
		String[] strings = WarpDrive.VERSION.split("-");
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
		VERSION_NUMBERS = integers.toArray(new Integer[0]);
	}
	
	public WarpDrive() {
	}
	
	@EventHandler
	public void onFMLPreInitialization(@Nonnull final FMLPreInitializationEvent event) {
		logger = event.getModLog();
		printStreamError = new LoggerPrintStream(Level.ERROR);
		printStreamWarn = new LoggerPrintStream(Level.WARN);
		printStreamInfo = new LoggerPrintStream(Level.INFO);
		
		WarpDriveConfig.onFMLpreInitialization(event.getModConfigurationDirectory().getAbsolutePath());
		
		methodBlock_getSilkTouch = ReflectionHelper.findMethod(Block.class, "getSilkTouchDrop", "func_180643_i", IBlockState.class);
		
		// common blocks and items
		blockLaser = new BlockLaser("laser", EnumTier.BASIC);
		
		blockChunkLoaders = new Block[EnumTier.length];
		for (final EnumTier enumTier : EnumTier.nonCreative()) {
			final int index = enumTier.getIndex();
			blockChunkLoaders[index] = new BlockChunkLoader("chunk_loader." + enumTier.getName(), enumTier);
		}
		
		blockLaserMediums = new Block[EnumTier.length];
		for (final EnumTier enumTier : EnumTier.nonCreative()) {
			final int index = enumTier.getIndex();
			blockLaserMediums[index] = new BlockLaserMedium("laser_medium." + enumTier.getName(), enumTier);
		}
		
		itemComponent = new ItemComponent("component", EnumTier.BASIC);
		
		// 20% more durability, same enchantability (except basic is slightly lower), increased toughness
		armorMaterial[EnumTier.BASIC.getIndex()   ] = EnumHelper.addArmorMaterial("rubber"      , "warpdrive:warp",  6, new int[] { 1, 2, 3, 1 }, 12, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, 0.0F);
		armorMaterial[EnumTier.BASIC.getIndex()   ].setRepairItem(ItemComponent.getItemStack(EnumComponentType.RUBBER));
		armorMaterial[EnumTier.ADVANCED.getIndex()] = EnumHelper.addArmorMaterial("ceramic"     , "warpdrive:warp", 18, new int[] { 2, 6, 5, 2 },  9, SoundEvents.ITEM_ARMOR_EQUIP_IRON   , 1.0F);
		armorMaterial[EnumTier.ADVANCED.getIndex()].setRepairItem(ItemComponent.getItemStack(EnumComponentType.CERAMIC));
		armorMaterial[EnumTier.SUPERIOR.getIndex()] = EnumHelper.addArmorMaterial("carbon_fiber", "warpdrive:warp", 40, new int[] { 3, 6, 8, 3 }, 10, SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND, 2.5F);
		armorMaterial[EnumTier.SUPERIOR.getIndex()].setRepairItem(ItemComponent.getItemStack(EnumComponentType.CARBON_FIBER));
		
		// atomic blocks and items
		if (WarpDriveConfig.ACCELERATOR_ENABLE) {
			blockAcceleratorCore = new BlockAcceleratorCore("accelerator_core", EnumTier.BASIC);
			blockAcceleratorControlPoint = new BlockAcceleratorControlPoint("accelerator_control_point", EnumTier.BASIC, false);
			blockParticlesCollider = new BlockParticlesCollider("particles_collider", EnumTier.BASIC);
			blockParticlesInjector = new BlockParticlesInjector("particles_injector", EnumTier.BASIC);
			blockVoidShellPlain = new BlockVoidShellPlain("void_shell.plain", EnumTier.BASIC);
			blockVoidShellGlass = new BlockVoidShellGlass("void_shell.glass", EnumTier.BASIC);
			
			blockElectromagnets_plain = new Block[EnumTier.length];
			blockElectromagnets_glass = new Block[EnumTier.length];
			blockChillers = new Block[EnumTier.length];
			itemElectromagneticCell = new ItemElectromagneticCell[EnumTier.length];
			itemPlasmaTorch = new ItemPlasmaTorch[EnumTier.length];
			for(final EnumTier enumTier : EnumTier.nonCreative()) {
				final int index = enumTier.getIndex();
				blockElectromagnets_plain[index] = new BlockElectromagnetPlain("electromagnet." + enumTier.getName() + ".plain", enumTier);
				blockElectromagnets_glass[index] = new BlockElectromagnetGlass("electromagnet." + enumTier.getName() + ".glass", enumTier);
				blockChillers[index] = new BlockChiller("chiller." + enumTier.getName(), enumTier);
				itemElectromagneticCell[index] = new ItemElectromagneticCell("electromagnetic_cell." + enumTier.getName(), enumTier);
				// itemPlasmaTorch[index] = new ItemPlasmaTorch("plasma_torch." + enumTier.getName(), enumTier);
			}
		}
		
		// building blocks and items
		blockShipScanners = new Block[EnumTier.length];
		for(final EnumTier enumTier : EnumTier.nonCreative()) {
			final int index = enumTier.getIndex();
			blockShipScanners[index] = new BlockShipScanner("ship_scanner." + enumTier.getName(), enumTier);
		}
		itemShipToken = new ItemShipToken("ship_token", EnumTier.BASIC);
		
		// breathing blocks and items
		blockAirFlow = new BlockAirFlow("air_flow", EnumTier.BASIC);
		blockAirSource = new BlockAirSource("air_source", EnumTier.BASIC);
		blockAirShield = new BlockAirShield("air_shield", EnumTier.BASIC);
		
		blockAirGeneratorTiered = new Block[EnumTier.length];
		for(final EnumTier enumTier : EnumTier.nonCreative()) {
			final int index = enumTier.getIndex();
			blockAirGeneratorTiered[index] = new BlockAirGeneratorTiered("air_generator." + enumTier.getName(), enumTier);
		}
		
		itemAirTanks = new ItemAirTank[4];
		for (final EnumAirTankTier enumAirTankTier : EnumAirTankTier.values()) {
			itemAirTanks[enumAirTankTier.getIndex()] = new ItemAirTank("air_tank." + enumAirTankTier.getName(), enumAirTankTier);
		}
		
		// collection blocks
		blockMiningLaser = new BlockMiningLaser("mining_laser", EnumTier.BASIC);
		blockLaserTreeFarm = new BlockLaserTreeFarm("laser_tree_farm", EnumTier.BASIC);
		
		// decoration
		blockBedrockGlass = new BlockBedrockGlass("bedrock_glass", EnumTier.CREATIVE);
		blockDecorative = new BlockDecorative("decorative", EnumTier.BASIC);
		blockGas = new BlockGas("gas", EnumTier.BASIC);
		blockLamp_bubble = new BlockLamp_bubble("lamp_bubble", EnumTier.BASIC);
		blockLamp_flat = new BlockLamp_flat("lamp_flat", EnumTier.BASIC);
		blockLamp_long = new BlockLamp_long("lamp_long", EnumTier.BASIC);
		
		// detection blocks
		blockBiometricScanner = new BlockBiometricScanner("biometric_scanner", EnumTier.BASIC);
		blockCamera = new BlockCamera("camera", EnumTier.BASIC);
		blockCloakingCoil = new BlockCloakingCoil("cloaking_coil", EnumTier.BASIC);
		blockCloakingCore = new BlockCloakingCore("cloaking_core", EnumTier.BASIC);
		blockEnvironmentalSensor = new BlockEnvironmentalSensor("environmental_sensor", EnumTier.BASIC);
		blockMonitor = new BlockMonitor("monitor", EnumTier.BASIC);
		blockRadar = new BlockRadar("radar", EnumTier.BASIC);
		
		blockSirenIndustrial = new Block[EnumTier.length];
		blockSirenMilitary = new Block[EnumTier.length];
		blockSpeaker = new Block[EnumTier.length];
		blockVirtualAssistant = new Block[EnumTier.length];
		for(final EnumTier enumTier : EnumTier.nonCreative()) {
			final int index = enumTier.getIndex();
			blockSirenIndustrial[index] = new BlockSiren("siren_industrial." + enumTier.getName(), enumTier, true);
			blockSirenMilitary[index] = new BlockSiren("siren_military." + enumTier.getName(), enumTier, false);
			blockSpeaker[index] = new BlockSpeaker("speaker." + enumTier.getName(), enumTier);
			blockVirtualAssistant[index] = new BlockVirtualAssistant("virtual_assistant." + enumTier.getName(), enumTier);
		}
		blockWarpIsolation = new BlockWarpIsolation("warp_isolation", EnumTier.BASIC);
		
		// energy blocks and items
		blockCapacitor = new Block[EnumTier.length];
		for(final EnumTier enumTier : EnumTier.values()) {
			final int index = enumTier.getIndex();
			blockCapacitor[index] = new BlockCapacitor("capacitor." + enumTier.getName(), enumTier);
		}
		blockEnanReactorCores = new Block[EnumTier.length];
		for(final EnumTier enumTier : EnumTier.nonCreative()) {
			final int index = enumTier.getIndex();
			blockEnanReactorCores[index] = new BlockEnanReactorCore("enan_reactor_core." + enumTier.getName(), enumTier);
		}
		blockEnanReactorLaser = new BlockEnanReactorLaser("enan_reactor_laser", EnumTier.BASIC);
		
		if (WarpDriveConfig.isIndustrialCraft2Loaded) {
			blockIC2reactorLaserCooler = new BlockIC2reactorLaserCooler("ic2_reactor_laser_cooler", EnumTier.BASIC);
			itemIC2reactorLaserFocus = new ItemIC2reactorLaserFocus("ic2_reactor_laser_focus", EnumTier.BASIC);
		}
		
		// force field blocks and items
		blockForceFields = new Block[EnumTier.length];
		blockForceFieldProjectors = new Block[EnumTier.length];
		blockForceFieldRelays = new Block[EnumTier.length];
		for(final EnumTier enumTier : EnumTier.nonCreative()) {
			final int index = enumTier.getIndex();
			blockForceFields[index] = new BlockForceField("force_field." + enumTier.getName(), enumTier);
			blockForceFieldProjectors[index] = new BlockForceFieldProjector("projector." + enumTier.getName(), enumTier);
			blockForceFieldRelays[index] = new BlockForceFieldRelay("force_field_relay." + enumTier.getName(), enumTier);
		}
		blockSecurityStation = new BlockSecurityStation("security_station", EnumTier.BASIC);
		itemForceFieldShape = new ItemForceFieldShape("force_field_shape", EnumTier.BASIC);
		itemForceFieldUpgrade = new ItemForceFieldUpgrade("force_field_upgrade", EnumTier.BASIC);
		
		// hull blocks
		blockHulls_plain = new Block[EnumTier.length][EnumHullPlainType.length];
		blockHulls_glass = new Block[EnumTier.length];
		blockHulls_omnipanel = new Block[EnumTier.length];
		blockHulls_stairs = new Block[EnumTier.length][16];
		blockHulls_slab = new Block[EnumTier.length][16];
		
		for(final EnumTier enumTier : EnumTier.nonCreative()) {
			final int index = enumTier.getIndex();
			for (final EnumHullPlainType hullPlainType : EnumHullPlainType.values()) {
				blockHulls_plain[index][hullPlainType.ordinal()] = new BlockHullPlain("hull." + enumTier.getName() + "." + hullPlainType.getName(), enumTier, hullPlainType);
			}
			blockHulls_glass[index] = new BlockHullGlass("hull." + enumTier.getName() + ".glass", enumTier);
			blockHulls_omnipanel[index] = new BlockHullOmnipanel("hull." + enumTier.getName() + ".omnipanel", enumTier);
			for (final EnumDyeColor enumDyeColor : EnumDyeColor.values()) {
				final int metadata = enumDyeColor.getMetadata();
				blockHulls_stairs[index][metadata] = new BlockHullStairs("hull." + enumTier.getName() + ".stairs_" + enumDyeColor.getName(), enumTier,
				                                                         blockHulls_plain[index][0].getDefaultState().withProperty(BlockColored.COLOR, enumDyeColor));
				blockHulls_slab[index][metadata] = new BlockHullSlab("hull." + enumTier.getName() + ".slab_" + enumDyeColor.getName(), enumTier,
				                                                     blockHulls_plain[index][0].getDefaultState().withProperty(BlockColored.COLOR, enumDyeColor));
			}
		}
		
		// movement blocks
		blockLift = new BlockLift("lift", EnumTier.BASIC);
		
		blockShipControllers = new Block[EnumTier.length];
		blockShipCores = new Block[EnumTier.length];
		for(final EnumTier enumTier : EnumTier.nonCreative()) {
			final int index = enumTier.getIndex();
			blockShipControllers[index] = new BlockShipController("ship_controller." + enumTier.getName(), enumTier);
			blockShipCores[index] = new BlockShipCore("ship_core." + enumTier.getName(), enumTier);
		}
		
		blockTransporterBeacon = new BlockTransporterBeacon("transporter_beacon", EnumTier.BASIC);
		blockTransporterContainment = new BlockTransporterContainment("transporter_containment", EnumTier.BASIC);
		blockTransporterCore = new BlockTransporterCore("transporter_core", EnumTier.BASIC);
		blockTransporterScanner = new BlockTransporterScanner("transporter_scanner", EnumTier.BASIC);
		
		// passive blocks
		blockHighlyAdvancedMachine = new BlockHighlyAdvancedMachine("highly_advanced_machine", EnumTier.BASIC);
		blockIridium = new BlockIridium("iridium_block", EnumTier.BASIC);
		
		// weapon blocks
		blockLaserCamera = new BlockLaserCamera("laser_camera", EnumTier.BASIC);
		blockWeaponController = new BlockWeaponController("weapon_controller", EnumTier.BASIC);
		
		// equipment items
		itemTuningFork = new ItemTuningFork("tuning_fork", EnumTier.BASIC);
		itemTuningDriver = new ItemTuningDriver("tuning_driver", EnumTier.ADVANCED);
		itemWrench = new ItemWrench("wrench", EnumTier.BASIC);
		
		itemWarpArmor = new ItemArmor[EnumTier.length][4];
		for(final EnumTier enumTier : EnumTier.nonCreative()) {
			final int index = enumTier.getIndex();
			itemWarpArmor[index][EntityEquipmentSlot.HEAD.getIndex() ] = new ItemWarpArmor("warp_armor." + enumTier.getName() + "." + ItemWarpArmor.suffixes[EntityEquipmentSlot.HEAD.getIndex() ], enumTier, armorMaterial[index], 3, EntityEquipmentSlot.HEAD );
			itemWarpArmor[index][EntityEquipmentSlot.CHEST.getIndex()] = new ItemWarpArmor("warp_armor." + enumTier.getName() + "." + ItemWarpArmor.suffixes[EntityEquipmentSlot.CHEST.getIndex()], enumTier, armorMaterial[index], 3, EntityEquipmentSlot.CHEST);
			itemWarpArmor[index][EntityEquipmentSlot.LEGS.getIndex() ] = new ItemWarpArmor("warp_armor." + enumTier.getName() + "." + ItemWarpArmor.suffixes[EntityEquipmentSlot.LEGS.getIndex() ], enumTier, armorMaterial[index], 3, EntityEquipmentSlot.LEGS );
			itemWarpArmor[index][EntityEquipmentSlot.FEET.getIndex() ] = new ItemWarpArmor("warp_armor." + enumTier.getName() + "." + ItemWarpArmor.suffixes[EntityEquipmentSlot.FEET.getIndex() ], enumTier, armorMaterial[index], 3, EntityEquipmentSlot.FEET );
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
		final Biome.BiomeProperties biomeProperties = new Biome.BiomeProperties("space").setRainDisabled().setWaterColor(0);
		biomeSpace = new BiomeSpace(biomeProperties);
		register(biomeSpace);
		
		// chunk loading
		ForgeChunkManager.setForcedChunkLoadingCallback(instance, ChunkLoadingHandler.INSTANCE);
		
		// Event handlers
		MinecraftForge.EVENT_BUS.register(this);
		
		proxy.onForgePreInitialisation();
	}
	
	@EventHandler
	public void onFMLInitialization(final FMLInitializationEvent event) {
		PacketHandler.init();
		
		WarpDriveConfig.onFMLInitialization();
		
		// world generation
		commonWorldGenerator = new CommonWorldGenerator();
		GameRegistry.registerWorldGenerator(commonWorldGenerator, 0);
		
		dimensionTypeSpace = DimensionType.register("space", "_space", WarpDriveConfig.G_SPACE_PROVIDER_ID, SpaceWorldProvider.class, true);
		dimensionTypeHyperSpace = DimensionType.register("hyperspace", "_hyperspace", WarpDriveConfig.G_HYPERSPACE_PROVIDER_ID, HyperSpaceWorldProvider.class, true);
		
		// Registers
		cloaks = new CloakManager();
		cameras = new CamerasRegistry();
		
		CelestialObjectManager.onFMLInitialization();
		
		proxy.onForgeInitialisation();
	}
	
	@EventHandler
	public void onFMLPostInitialization(final FMLPostInitializationEvent event) {
		WarpDriveConfig.onFMLPostInitialization();
		
		if (WarpDriveConfig.isComputerCraftLoaded) {
			peripheralHandler = new WarpDrivePeripheralHandler();
			peripheralHandler.register();
		}
		
		final WorldHandler worldHandler = new WorldHandler();
		MinecraftForge.EVENT_BUS.register(worldHandler);
		
		final ChunkHandler chunkHandler = new ChunkHandler();
		MinecraftForge.EVENT_BUS.register(chunkHandler);
		
		final ChatHandler chatHandler = new ChatHandler();
		MinecraftForge.EVENT_BUS.register(chatHandler);
	}
	
	@EventHandler
	public void onFMLServerStarting(final FMLServerStartingEvent event) {
		event.registerServerCommand(new CommandBed());
		event.registerServerCommand(new CommandDebug());
		event.registerServerCommand(new CommandDump());
		event.registerServerCommand(new CommandEntity());
		event.registerServerCommand(new CommandFind());
		event.registerServerCommand(new CommandGenerate());
		event.registerServerCommand(new CommandInvisible());
		event.registerServerCommand(new CommandNPC());
		event.registerServerCommand(new CommandReload());
		event.registerServerCommand(new CommandRender());
		event.registerServerCommand(new CommandSpace());
	}
	
	/* DataFixer documentation/feature on limbo => use midas configuration instead
	public static void registerFixes(final DataFixer dataFixer) {
		final CompoundDataFixer compoundDataFixer = FMLServerHandler.instance().getDataFixer();
		compoundDataFixer.registerWalker(FixTypes.BLOCK_ENTITY, new ItemStackData(TileEntityEnanReactorLaser.class, WarpDrive.MODID + ":enan_reactor_laser"));
	}
	
	@SubscribeEvent
	public void onMissingMappings(final RegistryEvent.MissingMappings<Block> event) {
		WarpDrive.logger.debug(String.format("Missing mappings %s", event.getName()));
		// event.getMappings();
	}
	
	@SuppressWarnings("ConstantConditions")
	@Mod.EventHandler
	public void onFMLMissingMappings(final FMLMissingMappingsEvent event) {
		for (final FMLMissingMappingsEvent.MissingMapping mapping: event.get()) {
			if (mapping.type == GameRegistry.Type.ITEM) {
				switch (mapping.name) {
					case "WarpDrive:airBlock":
						mapping.remap(Item.getItemFromBlock(blockAir));
						break;
					case "WarpDrive:airCanisterFull":
					case "WarpDrive:itemAirCanisterFull":
					case "WarpDrive:itemAirTank":
						mapping.remap(itemAirTanks[0]);
						break;
					case "WarpDrive:airgenBlock":
						mapping.remap(Item.getItemFromBlock(blockAirGenerator));
						break;
					case "WarpDrive:blockHAMachine":
						mapping.remap(Item.getItemFromBlock(blockHighlyAdvancedMachine));
						break;
					case "WarpDrive:boosterBlock":
						mapping.remap(Item.getItemFromBlock(blockLaserMedium));
						break;
					case "WarpDrive:cameraBlock":
						mapping.remap(Item.getItemFromBlock(blockCamera));
						break;
					case "WarpDrive:chunkLoader":
						mapping.remap(Item.getItemFromBlock(blockChunkLoader));
						break;
					case "WarpDrive:cloakBlock":
						mapping.remap(Item.getItemFromBlock(blockCloakingCore));
						break;
					case "WarpDrive:cloakCoilBlock":
						mapping.remap(Item.getItemFromBlock(blockCloakingCoil));
						break;
					case "WarpDrive:component":
						mapping.remap(itemComponent);
						break;
					case "WarpDrive:decorative":
						mapping.remap(Item.getItemFromBlock(blockDecorative));
						break;
					case "WarpDrive:gasBlock":
						mapping.remap(Item.getItemFromBlock(blockGas));
						break;
					case "WarpDrive:helmet":
					case "WarpDrive:itemHelmet":
						mapping.remap(itemWarpArmor[3]);
						break;
					case "WarpDrive:iridiumBlock":
						mapping.remap(Item.getItemFromBlock(blockIridium));
						break;
					case "WarpDrive:isolationBlock":
						mapping.remap(Item.getItemFromBlock(blockWarpIsolation));
						break;
					case "WarpDrive:laserBlock":
						mapping.remap(Item.getItemFromBlock(blockLaser));
						break;
					case "WarpDrive:laserCamBlock":
						mapping.remap(Item.getItemFromBlock(blockLaserCamera));
						break;
					case "WarpDrive:laserTreeFarmBlock":
						mapping.remap(Item.getItemFromBlock(blockLaserTreeFarm));
						break;
					case "WarpDrive:liftBlock":
						mapping.remap(Item.getItemFromBlock(blockLift));
						break;
					case "WarpDrive:miningLaserBlock":
						mapping.remap(Item.getItemFromBlock(blockMiningLaser));
						break;
					case "WarpDrive:monitorBlock":
						mapping.remap(Item.getItemFromBlock(blockMonitor));
						break;
					case "WarpDrive:powerLaser":
						mapping.remap(Item.getItemFromBlock(blockEnanReactorLaser));
						break;
					case "WarpDrive:powerReactor":
						mapping.remap(Item.getItemFromBlock(blockEnanReactorCore));
						break;
					case "WarpDrive:powerStore":
						mapping.remap(Item.getItemFromBlock(blockCapacitor));
						break;
					case "WarpDrive:protocolBlock":
						mapping.remap(Item.getItemFromBlock(blockShipController));
						break;
					case "WarpDrive:radarBlock":
						mapping.remap(Item.getItemFromBlock(blockRadar));
						break;
					case "WarpDrive:reactorLaserFocus":
						mapping.remap(itemIC2reactorLaserFocus);
						break;
					case "WarpDrive:reactorMonitor":
						mapping.remap(Item.getItemFromBlock(blockIC2reactorLaserCooler));
						break;
					case "WarpDrive:scannerBlock":
						mapping.remap(Item.getItemFromBlock(blockShipScanner));
						break;
					case "WarpDrive:transportBeacon":
					case "WarpDrive:blockTransportBeacon":
						mapping.remap(Item.getItemFromBlock(blockTransporterBeacon));
						break;
					case "WarpDrive:transporter":
					case "WarpDrive:blockTransporter":
						mapping.remap(Item.getItemFromBlock(blockTransporterCore));
						break;
					case "WarpDrive:warpCore":
						mapping.remap(Item.getItemFromBlock(blockShipCore));
						break;
					case "WarpDrive:itemTuningRod":
						mapping.remap(itemTuningFork);
						break;
					case "WarpDrive:itemCrystalToken":
						mapping.remap(itemShipToken);
						break;
				}
				
			} else if (mapping.type == GameRegistry.Type.BLOCK) {
				switch (mapping.name) {
					case "WarpDrive:airBlock":
						mapping.remap(blockAir);
						break;
					case "WarpDrive:airgenBlock":
						mapping.remap(blockAirGenerator);
						break;
					case "WarpDrive:blockHAMachine":
						mapping.remap(blockHighlyAdvancedMachine);
						break;
					case "WarpDrive:boosterBlock":
						mapping.remap(blockLaserMedium);
						break;
					case "WarpDrive:cameraBlock":
						mapping.remap(blockCamera);
						break;
					case "WarpDrive:chunkLoader":
						mapping.remap(blockChunkLoader);
						break;
					case "WarpDrive:cloakBlock":
						mapping.remap(blockCloakingCore);
						break;
					case "WarpDrive:cloakCoilBlock":
						mapping.remap(blockCloakingCoil);
						break;
					case "WarpDrive:decorative":
						mapping.remap(blockDecorative);
						break;
					case "WarpDrive:gasBlock":
						mapping.remap(blockGas);
						break;
					case "WarpDrive:iridiumBlock":
						mapping.remap(blockIridium);
						break;
					case "WarpDrive:isolationBlock":
						mapping.remap(blockWarpIsolation);
						break;
					case "WarpDrive:laserBlock":
						mapping.remap(blockLaser);
						break;
					case "WarpDrive:laserCamBlock":
						mapping.remap(blockLaserCamera);
						break;
					case "WarpDrive:laserTreeFarmBlock":
						mapping.remap(blockLaserTreeFarm);
						break;
					case "WarpDrive:liftBlock":
						mapping.remap(blockLift);
						break;
					case "WarpDrive:miningLaserBlock":
						mapping.remap(blockMiningLaser);
						break;
					case "WarpDrive:monitorBlock":
						mapping.remap(blockMonitor);
						break;
					case "WarpDrive:powerLaser":
						mapping.remap(blockEnanReactorLaser);
						break;
					case "WarpDrive:powerReactor":
						mapping.remap(blockEnanReactorCore);
						break;
					case "WarpDrive:powerStore":
						mapping.remap(blockCapacitor);
						break;
					case "WarpDrive:protocolBlock":
						mapping.remap(blockShipController);
						break;
					case "WarpDrive:radarBlock":
						mapping.remap(blockRadar);
						break;
					case "WarpDrive:reactorMonitor":
						mapping.remap(blockIC2reactorLaserCooler);
						break;
					case "WarpDrive:scannerBlock":
						mapping.remap(blockShipScanner);
						break;
					case "WarpDrive:transportBeacon":
					case "WarpDrive:blockTransportBeacon":
						mapping.remap(blockTransporterBeacon);
						break;
					case "WarpDrive:transporter":
					case "WarpDrive:blockTransporter":
						mapping.remap(blockTransporterCore);
						break;
					case "WarpDrive:warpCore":
						mapping.remap(blockShipCore);
						break;
				}
			}
		}
	}
	/**/
	
	final public static ArrayList<Biome> biomes = new ArrayList<>(10);
	final public static ArrayList<Block> blocks = new ArrayList<>(100);
	final public static ArrayList<Enchantment> enchantments = new ArrayList<>(10);
	final public static ArrayList<Item> items = new ArrayList<>(50);
	final public static ArrayList<Potion> potions = new ArrayList<>(10);
	final public static ArrayList<PotionType> potionTypes = new ArrayList<>(10);
	final public static ArrayList<SoundEvent> soundEvents = new ArrayList<>(100);
	final public static HashMap<ResourceLocation, IRecipe> recipes = new HashMap<>(100);
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
	public static <BLOCK extends Block> BLOCK register(@Nonnull final BLOCK block, @Nullable final ItemBlock itemBlock) {
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
	public static <POTION extends Potion> POTION register(@Nonnull final POTION potion) {
		potions.add(potion);
		return potion;
	}
	
	// Register an PotionType.
	public static <POTION_TYPE extends PotionType> POTION_TYPE register(@Nonnull final POTION_TYPE potionType) {
		potionTypes.add(potionType);
		return potionType;
	}
	
	// Register a recipe.
	public static <RECIPE extends IRecipe> RECIPE register(@Nonnull final RECIPE recipe) {
		return register(recipe, "");
	}
	public static <RECIPE extends IRecipe> RECIPE register(@Nonnull final RECIPE recipe, final String suffix) {
		ResourceLocation registryName = recipe.getRegistryName();
		if (registryName == null) {
			final String path;
			final ItemStack itemStackOutput = recipe.getRecipeOutput();
			assert itemStackOutput.getItem().getRegistryName() != null;
			if (itemStackOutput.isEmpty()) {
				path = recipe.toString();
			} else if (itemStackOutput.getCount() == 1) {
				path = String.format("%s@%d%s",
				                     itemStackOutput.getItem().getRegistryName().getPath(),
				                     itemStackOutput.getItemDamage(),
				                     suffix );
			} else {
				path = String.format("%s@%dx%d%s",
				                     itemStackOutput.getItem().getRegistryName().getPath(),
				                     itemStackOutput.getItemDamage(),
				                     itemStackOutput.getCount(),
				                     suffix );
			}
			registryName = new ResourceLocation(MODID, path);
			if (recipes.containsKey(registryName)) {
				logger.error(String.format("Overlapping recipe detected, please report this to the mod author %s",
				                           registryName));
				registryName = new ResourceLocation(MODID, path + "!" + System.nanoTime());
				assert false;
			}
			recipe.setRegistryName(registryName);
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
	public void onRegisterBiomes(@Nonnull final RegistryEvent.Register<Biome> event) {
		LocalProfiler.start(String.format("Registering %s", event.getName()));
		
		for (final Biome biome : biomes) {
			event.getRegistry().register(biome);
		}
		
		BiomeDictionary.addTypes(biomeSpace, BiomeDictionary.Type.DEAD, BiomeDictionary.Type.WASTELAND);
		
		LocalProfiler.stop(1000);
	}
	
	@SubscribeEvent
	public void onRegisterBlocks(@Nonnull final RegistryEvent.Register<Block> event) {
		LocalProfiler.start(String.format("Registering %s", event.getName()));
		
		for (final Block block : blocks) {
			event.getRegistry().register(block);
		}
		
		GameRegistry.registerTileEntity(TileEntityAcceleratorCore.class, new ResourceLocation(WarpDrive.MODID, "accelerator_core"));
		GameRegistry.registerTileEntity(TileEntityAcceleratorControlPoint.class, new ResourceLocation(WarpDrive.MODID, "accelerator_control_point"));
		GameRegistry.registerTileEntity(TileEntityAirGeneratorTiered.class, new ResourceLocation(WarpDrive.MODID, "air_generator"));
		GameRegistry.registerTileEntity(TileEntityBiometricScanner.class, new ResourceLocation(WarpDrive.MODID, "biometric_scanner"));
		GameRegistry.registerTileEntity(TileEntityCamera.class, new ResourceLocation(WarpDrive.MODID, "camera"));
		GameRegistry.registerTileEntity(TileEntityCapacitor.class, new ResourceLocation(WarpDrive.MODID, "capacitor"));
		GameRegistry.registerTileEntity(TileEntityChunkLoader.class, new ResourceLocation(WarpDrive.MODID, "chunk_loader"));
		GameRegistry.registerTileEntity(TileEntityCloakingCore.class, new ResourceLocation(WarpDrive.MODID, "cloaking_core"));
		GameRegistry.registerTileEntity(TileEntityEnanReactorCore.class, new ResourceLocation(WarpDrive.MODID, "enan_reactor_core"));
		GameRegistry.registerTileEntity(TileEntityEnanReactorLaser.class, new ResourceLocation(WarpDrive.MODID, "enan_reactor_laser"));
		GameRegistry.registerTileEntity(TileEntityEnvironmentalSensor.class, new ResourceLocation(WarpDrive.MODID, "environmental_sensor"));
		GameRegistry.registerTileEntity(TileEntityForceField.class, new ResourceLocation(WarpDrive.MODID, "force_field"));
		GameRegistry.registerTileEntity(TileEntityForceFieldProjector.class, new ResourceLocation(WarpDrive.MODID, "force_field_projector"));
		GameRegistry.registerTileEntity(TileEntityForceFieldRelay.class, new ResourceLocation(WarpDrive.MODID, "force_field_relay"));
		GameRegistry.registerTileEntity(TileEntityIC2reactorLaserMonitor.class, new ResourceLocation(WarpDrive.MODID, "ic2_reactor_laser_monitor"));
		GameRegistry.registerTileEntity(TileEntityLaser.class, new ResourceLocation(WarpDrive.MODID, "laser"));
		GameRegistry.registerTileEntity(TileEntityLaserCamera.class, new ResourceLocation(WarpDrive.MODID, "laser_camera"));
		GameRegistry.registerTileEntity(TileEntityLaserMedium.class, new ResourceLocation(WarpDrive.MODID, "laser_medium"));
		GameRegistry.registerTileEntity(TileEntityLaserTreeFarm.class, new ResourceLocation(WarpDrive.MODID, "laser_tree_farm"));
		GameRegistry.registerTileEntity(TileEntityLift.class, new ResourceLocation(WarpDrive.MODID, "lift"));
		GameRegistry.registerTileEntity(TileEntityMiningLaser.class, new ResourceLocation(WarpDrive.MODID, "mining_laser"));
		GameRegistry.registerTileEntity(TileEntityMonitor.class, new ResourceLocation(WarpDrive.MODID, "monitor"));
		GameRegistry.registerTileEntity(TileEntityParticlesInjector.class, new ResourceLocation(WarpDrive.MODID, "particles_injector"));
		GameRegistry.registerTileEntity(TileEntityRadar.class, new ResourceLocation(WarpDrive.MODID, "radar"));
		GameRegistry.registerTileEntity(TileEntitySecurityStation.class, new ResourceLocation(WarpDrive.MODID, "security_station"));
		GameRegistry.registerTileEntity(TileEntityShipController.class, new ResourceLocation(WarpDrive.MODID, "ship_controller"));
		GameRegistry.registerTileEntity(TileEntityShipCore.class, new ResourceLocation(WarpDrive.MODID, "ship_core"));
		GameRegistry.registerTileEntity(TileEntityShipScanner.class, new ResourceLocation(WarpDrive.MODID, "ship_scanner"));
		GameRegistry.registerTileEntity(TileEntitySiren.class, new ResourceLocation(WarpDrive.MODID, "siren"));
		GameRegistry.registerTileEntity(TileEntitySpeaker.class, new ResourceLocation(WarpDrive.MODID, "speaker"));
		GameRegistry.registerTileEntity(TileEntityTransporterBeacon.class, new ResourceLocation(WarpDrive.MODID, "transporter_beacon"));
		GameRegistry.registerTileEntity(TileEntityTransporterCore.class, new ResourceLocation(WarpDrive.MODID, "transporter_core"));
		GameRegistry.registerTileEntity(TileEntityVirtualAssistant.class, new ResourceLocation(WarpDrive.MODID, "virtual_assistant"));
		GameRegistry.registerTileEntity(TileEntityWeaponController.class, new ResourceLocation(WarpDrive.MODID, "weapon_controller"));
		
		LocalProfiler.stop(1000);
	}
	
	@SubscribeEvent
	public void onRegisterEnchantments(@Nonnull final RegistryEvent.Register<Enchantment> event) {
		LocalProfiler.start(String.format("Registering %s", event.getName()));
		
		for (final Enchantment enchantment : enchantments) {
			event.getRegistry().register(enchantment);
		}
		
		LocalProfiler.stop(1000);
	}
	
	@SubscribeEvent
	public void onRegisterEntities(@Nonnull final RegistryEvent.Register<EntityEntry> event) {
		LocalProfiler.start(String.format("Registering %s", event.getName()));
		
		EntityEntry entityEntry;
		
		// @TODO: MC1.15 change id to lower case
		entityEntry = EntityEntryBuilder.create()
		                                .entity(EntitySphereGen.class).factory(EntitySphereGen::new)
		                                .tracker(200, 1, false)
		                                .id("entitySphereGenerator", WarpDriveConfig.G_ENTITY_SPHERE_GENERATOR_ID).name("EntitySphereGenerator")
		                                .build();
		event.getRegistry().register(entityEntry);
		
		entityEntry = EntityEntryBuilder.create()
		                                .entity(EntityStarCore.class).factory(EntityStarCore::new)
		                                .tracker(300, 1, false)
		                                .id("entity_star_sore", WarpDriveConfig.G_ENTITY_STAR_CORE_ID).name("EntityStarCore")
		                                .build();
		event.getRegistry().register(entityEntry);
		
		entityEntry = EntityEntryBuilder.create()
		                                .entity(EntityCamera.class).factory(EntityCamera::new)
		                                .tracker(300, 1, false)
		                                .id("entity_camera", WarpDriveConfig.G_ENTITY_CAMERA_ID).name("EntityCamera")
		                                .build();
		event.getRegistry().register(entityEntry);
		
		entityEntry = EntityEntryBuilder.create()
		                                .entity(EntityParticleBunch.class).factory(EntityParticleBunch::new)
		                                .tracker(300, 1, false)
		                                .id("entity_particle_bunch", WarpDriveConfig.G_ENTITY_PARTICLE_BUNCH_ID).name("EntityParticleBunch")
		                                .build();
		event.getRegistry().register(entityEntry);
		
		entityEntry = EntityEntryBuilder.create()
		                                .entity(EntityLaserExploder.class).factory(EntityLaserExploder::new)
		                                .tracker(8, 1000, false)
		                                .id("entity_laser_exploder", WarpDriveConfig.G_ENTITY_LASER_EXPLODER_ID).name("entityLaserExploder")
		                                .build();
		event.getRegistry().register(entityEntry);
		
		entityEntry = EntityEntryBuilder.create()
		                                .entity(EntityNPC.class).factory(EntityNPC::new)
		                                .tracker(200, 1, false)
		                                .id("entity_npc", WarpDriveConfig.G_ENTITY_NPC_ID).name("EntityNPC")
		                                .build();
		event.getRegistry().register(entityEntry);
		
		entityEntry = EntityEntryBuilder.create()
		                                .entity(EntityOfflineAvatar.class).factory(EntityOfflineAvatar::new)
		                                .tracker(200, 1, false)
		                                .id("entity_offline_avatar", WarpDriveConfig.G_ENTITY_OFFLINE_AVATAR_ID).name("EntityOfflineAvatar")
		                                .build();
		event.getRegistry().register(entityEntry);
		
		entityEntry = EntityEntryBuilder.create()
		                                .entity(EntitySeat.class).factory(EntitySeat::new)
		                                .tracker(200, 1, false)
		                                .id("entity_seat", WarpDriveConfig.G_ENTITY_SEAT_ID).name("EntitySeat")
		                                .build();
		event.getRegistry().register(entityEntry);
		
		LocalProfiler.stop(1000);
	}
	
	@SubscribeEvent
	public void onRegisterItems(@Nonnull final RegistryEvent.Register<Item> event) {
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
	public void onRegisterPotions(@Nonnull final RegistryEvent.Register<Potion> event) {
		LocalProfiler.start(String.format("Registering %s", event.getName()));
		
		for (final Potion potion : potions) {
			event.getRegistry().register(potion);
		}
		
		LocalProfiler.stop(1000);
	}
	
	@SubscribeEvent
	public void onRegisterPotionTypes(@Nonnull final RegistryEvent.Register<PotionType> event) {
		LocalProfiler.start(String.format("Registering %s", event.getName()));
		
		for (final PotionType potionType : potionTypes) {
			event.getRegistry().register(potionType);
		}
		
		LocalProfiler.stop(1000);
	}
	
	@SubscribeEvent
	public void onRegisterRecipes(@Nonnull final RegistryEvent.Register<IRecipe> event) {
		LocalProfiler.start(String.format("Registering %s step 1", event.getName()));
		
		Recipes.initOreDictionary();
		
		Recipes.initDynamic();
		
		LocalProfiler.stop(1000);
		
		LocalProfiler.start(String.format("Registering %s step 2", event.getName()));
		
		for (final IRecipe recipe : recipes.values()) {
			event.getRegistry().register(recipe);
		}
		
		LocalProfiler.stop(1000);
	}
	
	@SubscribeEvent
	public void onRegisterSoundEvents(@Nonnull final RegistryEvent.Register<SoundEvent> event) {
		LocalProfiler.start(String.format("Registering %s", event.getName()));
		
		cr0s.warpdrive.data.SoundEvents.registerSounds();
		for (final SoundEvent soundEvent : soundEvents) {
			event.getRegistry().register(soundEvent);
		}
		
		LocalProfiler.stop(1000);
	}
	
	@SubscribeEvent
	public void onRegisterVillagerProfessions(@Nonnull final RegistryEvent.Register<VillagerProfession> event) {
		LocalProfiler.start(String.format("Registering %s", event.getName()));
		
		for (final VillagerProfession villagerProfession : villagerProfessions) {
			event.getRegistry().register(villagerProfession);
		}
		
		LocalProfiler.stop(1000);
	}
}
