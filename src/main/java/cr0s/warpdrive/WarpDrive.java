package cr0s.warpdrive;

import cr0s.warpdrive.block.BlockChunkLoader;
import cr0s.warpdrive.block.BlockLaser;
import cr0s.warpdrive.block.BlockLaserMedium;
import cr0s.warpdrive.block.ItemBlockAbstractBase;
import cr0s.warpdrive.block.TileEntityAbstractChunkLoading;
import cr0s.warpdrive.block.TileEntityChunkLoader;
import cr0s.warpdrive.block.TileEntityLaser;
import cr0s.warpdrive.block.TileEntityLaserMedium;
import cr0s.warpdrive.block.atomic.BlockAcceleratorControlPoint;
import cr0s.warpdrive.block.atomic.BlockAcceleratorController;
import cr0s.warpdrive.block.atomic.BlockChiller;
import cr0s.warpdrive.block.atomic.BlockElectromagnetGlass;
import cr0s.warpdrive.block.atomic.BlockElectromagnetPlain;
import cr0s.warpdrive.block.atomic.BlockParticlesCollider;
import cr0s.warpdrive.block.atomic.BlockParticlesInjector;
import cr0s.warpdrive.block.atomic.BlockVoidShellGlass;
import cr0s.warpdrive.block.atomic.BlockVoidShellPlain;
import cr0s.warpdrive.block.atomic.TileEntityAcceleratorControlPoint;
import cr0s.warpdrive.block.atomic.TileEntityAcceleratorController;
import cr0s.warpdrive.block.atomic.TileEntityParticlesInjector;
import cr0s.warpdrive.block.breathing.BlockAirFlow;
import cr0s.warpdrive.block.breathing.BlockAirGenerator;
import cr0s.warpdrive.block.breathing.BlockAirGeneratorTiered;
import cr0s.warpdrive.block.breathing.BlockAirShield;
import cr0s.warpdrive.block.breathing.BlockAirSource;
import cr0s.warpdrive.block.breathing.ItemBlockAirShield;
import cr0s.warpdrive.block.breathing.TileEntityAirGenerator;
import cr0s.warpdrive.block.breathing.TileEntityAirGeneratorTiered;
import cr0s.warpdrive.block.building.BlockShipScanner;
import cr0s.warpdrive.block.building.TileEntityShipScanner;
import cr0s.warpdrive.block.collection.BlockLaserTreeFarm;
import cr0s.warpdrive.block.collection.BlockMiningLaser;
import cr0s.warpdrive.block.collection.TileEntityLaserTreeFarm;
import cr0s.warpdrive.block.collection.TileEntityMiningLaser;
import cr0s.warpdrive.block.detection.BlockCamera;
import cr0s.warpdrive.block.detection.BlockCloakingCoil;
import cr0s.warpdrive.block.detection.BlockCloakingCore;
import cr0s.warpdrive.block.detection.BlockMonitor;
import cr0s.warpdrive.block.detection.BlockRadar;
import cr0s.warpdrive.block.detection.BlockSiren;
import cr0s.warpdrive.block.detection.BlockWarpIsolation;
import cr0s.warpdrive.block.detection.TileEntityCamera;
import cr0s.warpdrive.block.detection.TileEntityCloakingCore;
import cr0s.warpdrive.block.detection.TileEntityMonitor;
import cr0s.warpdrive.block.detection.TileEntityRadar;
import cr0s.warpdrive.block.detection.TileEntitySiren;
import cr0s.warpdrive.block.energy.BlockEnanReactorCore;
import cr0s.warpdrive.block.energy.BlockEnanReactorLaser;
import cr0s.warpdrive.block.energy.BlockEnergyBank;
import cr0s.warpdrive.block.energy.BlockIC2reactorLaserMonitor;
import cr0s.warpdrive.block.energy.TileEntityEnanReactorCore;
import cr0s.warpdrive.block.energy.TileEntityEnanReactorLaser;
import cr0s.warpdrive.block.energy.TileEntityEnergyBank;
import cr0s.warpdrive.block.energy.TileEntityIC2reactorLaserMonitor;
import cr0s.warpdrive.block.forcefield.BlockForceField;
import cr0s.warpdrive.block.forcefield.BlockForceFieldProjector;
import cr0s.warpdrive.block.forcefield.BlockForceFieldRelay;
import cr0s.warpdrive.block.forcefield.ItemBlockForceFieldProjector;
import cr0s.warpdrive.block.forcefield.ItemBlockForceFieldRelay;
import cr0s.warpdrive.block.forcefield.TileEntityForceField;
import cr0s.warpdrive.block.forcefield.TileEntityForceFieldProjector;
import cr0s.warpdrive.block.forcefield.TileEntityForceFieldRelay;
import cr0s.warpdrive.block.hull.BlockHullGlass;
import cr0s.warpdrive.block.hull.BlockHullOmnipanel;
import cr0s.warpdrive.block.hull.BlockHullPlain;
import cr0s.warpdrive.block.hull.BlockHullSlab;
import cr0s.warpdrive.block.hull.BlockHullStairs;
import cr0s.warpdrive.block.hull.ItemBlockHull;
import cr0s.warpdrive.block.hull.ItemBlockHullSlab;
import cr0s.warpdrive.block.movement.BlockLift;
import cr0s.warpdrive.block.movement.BlockShipController;
import cr0s.warpdrive.block.movement.BlockShipCore;
import cr0s.warpdrive.block.movement.BlockTransporter;
import cr0s.warpdrive.block.movement.TileEntityLift;
import cr0s.warpdrive.block.movement.TileEntityShipController;
import cr0s.warpdrive.block.movement.TileEntityShipCore;
import cr0s.warpdrive.block.movement.TileEntityTransporter;
import cr0s.warpdrive.block.breathing.BlockAir;
import cr0s.warpdrive.block.passive.BlockBedrockGlass;
import cr0s.warpdrive.block.passive.BlockDecorative;
import cr0s.warpdrive.block.passive.BlockGas;
import cr0s.warpdrive.block.passive.BlockHighlyAdvancedMachine;
import cr0s.warpdrive.block.passive.BlockIridium;
import cr0s.warpdrive.block.passive.BlockTransportBeacon;
import cr0s.warpdrive.block.passive.ItemBlockDecorative;
import cr0s.warpdrive.block.weapon.BlockLaserCamera;
import cr0s.warpdrive.block.weapon.BlockWeaponController;
import cr0s.warpdrive.block.weapon.TileEntityLaserCamera;
import cr0s.warpdrive.block.weapon.TileEntityWeaponController;
import cr0s.warpdrive.command.CommandDebug;
import cr0s.warpdrive.command.CommandDump;
import cr0s.warpdrive.command.CommandEntity;
import cr0s.warpdrive.command.CommandFind;
import cr0s.warpdrive.command.CommandGenerate;
import cr0s.warpdrive.command.CommandBed;
import cr0s.warpdrive.command.CommandInvisible;
import cr0s.warpdrive.command.CommandJumpgates;
import cr0s.warpdrive.command.CommandReload;
import cr0s.warpdrive.command.CommandSpace;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.config.RecipeParticleShapedOre;
import cr0s.warpdrive.config.RecipeTuningDriver;
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
import cr0s.warpdrive.data.CloakManager;
import cr0s.warpdrive.data.EnumHullPlainType;
import cr0s.warpdrive.data.JumpgatesRegistry;
import cr0s.warpdrive.data.StarMapRegistry;
import cr0s.warpdrive.event.ChunkHandler;
import cr0s.warpdrive.event.ClientHandler;
import cr0s.warpdrive.event.CommonWorldGenerator;
import cr0s.warpdrive.event.LivingHandler;
import cr0s.warpdrive.event.WorldHandler;
import cr0s.warpdrive.item.ItemAirTank;
import cr0s.warpdrive.item.ItemComponent;
import cr0s.warpdrive.item.ItemShipToken;
import cr0s.warpdrive.item.ItemElectromagneticCell;
import cr0s.warpdrive.item.ItemForceFieldShape;
import cr0s.warpdrive.item.ItemForceFieldUpgrade;
import cr0s.warpdrive.item.ItemIC2reactorLaserFocus;
import cr0s.warpdrive.item.ItemTuningDriver;
import cr0s.warpdrive.item.ItemTuningFork;
import cr0s.warpdrive.item.ItemUpgrade;
import cr0s.warpdrive.item.ItemWarpArmor;
import cr0s.warpdrive.network.PacketHandler;
import cr0s.warpdrive.render.ClientCameraHandler;
import cr0s.warpdrive.render.RenderBlockShipScanner;
import cr0s.warpdrive.render.RenderBlockForceField;
import cr0s.warpdrive.render.RenderBlockOmnipanel;
import cr0s.warpdrive.render.RenderBlockStandard;
import cr0s.warpdrive.render.RenderOverlayAir;
import cr0s.warpdrive.render.RenderOverlayCamera;
import cr0s.warpdrive.render.RenderOverlayLocation;
import cr0s.warpdrive.world.BiomeSpace;
import cr0s.warpdrive.world.HyperSpaceWorldProvider;
import cr0s.warpdrive.world.SpaceWorldProvider;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.block.Block;
import net.minecraft.block.BlockColored;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.item.ItemDye;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.LoadingCallback;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.oredict.RecipeSorter;

@Mod(modid = WarpDrive.MODID, name = "WarpDrive", version = WarpDrive.VERSION, dependencies = "after:IC2;" + " after:CoFHCore;" + " after:ComputerCraft;"
		+ " after:OpenComputer;" + " after:CCTurtle;" + " after:gregtech;" + " after:AppliedEnergistics;" + " after:EnderIO;" + " after:DefenseTech;" + " after:icbmclassic;")
public class WarpDrive implements LoadingCallback {
	public static final String MODID = "WarpDrive";
	public static final String VERSION = "@version@";
	public static final boolean isDev = VERSION.equals("@" + "version" + "@") || VERSION.contains("-dev");
	public static GameProfile gameProfile = new GameProfile(UUID.nameUUIDFromBytes("[WarpDrive]".getBytes()), "[WarpDrive]");
	
	public static Block blockShipCore;
	public static Block blockShipController;
	public static Block blockRadar;
	public static Block blockWarpIsolation;
	public static Block blockAirGenerator;
	public static Block[] blockAirGeneratorTiered;
	public static Block blockLaser;
	public static Block blockLaserCamera;
	public static Block blockWeaponController;
	public static Block blockCamera;
	public static Block blockMonitor;
	public static Block blockLaserMedium;
	public static Block blockMiningLaser;
	public static Block blockLaserTreeFarm;
	public static Block blockLift;
	public static Block blockShipScanner;
	public static Block blockCloakingCore;
	public static Block blockCloakingCoil;
	public static Block blockTransporter;
	public static Block blockIC2reactorLaserMonitor;
	public static Block blockEnanReactorCore;
	public static Block blockEnanReactorLaser;
	public static Block blockEnergyBank;
	public static Block blockAir;
	public static Block blockAirFlow;
	public static Block blockAirSource;
	public static Block blockAirShield;
	public static Block blockBedrockGlass;
	public static Block blockGas;
	public static Block blockIridium;
	public static Block blockHighlyAdvancedMachine;
	public static Block blockTransportBeacon;
	public static Block blockChunkLoader;
	public static Block[] blockForceFields;
	public static Block[] blockForceFieldProjectors;
	public static Block[] blockForceFieldRelays;
	public static Block blockAcceleratorController;
	public static Block blockAcceleratorControlPoint;
	public static Block blockParticlesCollider;
	public static Block blockParticlesInjector;
	public static Block blockVoidShellPlain;
	public static Block blockVoidShellGlass;
	public static Block[] blockElectromagnetPlain;
	public static Block[] blockElectromagnetGlass;
	public static Block[] blockChillers;
	public static Block blockDecorative;
	public static Block[][] blockHulls_plain;
	public static Block[] blockHulls_glass;
	public static Block[] blockHulls_omnipanel;
	public static Block[][] blockHulls_stairs;
	public static Block[][] blockHulls_slab;
	public static Block blockSiren;
	
	public static Item itemIC2reactorLaserFocus;
	public static ItemComponent itemComponent;
	public static ItemShipToken itemShipToken;
	public static ItemUpgrade itemUpgrade;
	public static ItemTuningFork itemTuningFork;
	public static ItemTuningDriver itemTuningDriver;
	public static ItemForceFieldShape itemForceFieldShape;
	public static ItemForceFieldUpgrade itemForceFieldUpgrade;
	public static ItemElectromagneticCell itemElectromagneticCell;
	
	public static final ArmorMaterial armorMaterial = EnumHelper.addArmorMaterial("WARP", 18, new int[] { 2, 6, 5, 2 }, 9);
	public static ItemArmor[] itemWarpArmor;
	public static ItemAirTank[] itemAirTanks;
	
	public static DamageAsphyxia damageAsphyxia;
	public static DamageCold damageCold;
	public static DamageIrradiation damageIrradiation;
	public static DamageLaser damageLaser;
	public static DamageShock damageShock;
	public static DamageTeleportation damageTeleportation;
	public static DamageWarm damageWarm;
	
	public static BiomeGenBase spaceBiome;
	@SuppressWarnings("FieldCanBeLocal")
	private CommonWorldGenerator commonWorldGenerator;
	
	public static Field fieldBlockHardness = null;
	
	// Client settings
	public static final CreativeTabs creativeTabWarpDrive = new CreativeTabWarpDrive(MODID.toLowerCase());
	
	@Instance(WarpDrive.MODID)
	public static WarpDrive instance;
	@SidedProxy(clientSide = "cr0s.warpdrive.client.ClientProxy", serverSide = "cr0s.warpdrive.CommonProxy")
	public static CommonProxy proxy;
	
	public static StarMapRegistry starMap;
	public static JumpgatesRegistry jumpgates;
	public static CloakManager cloaks;
	public static CamerasRegistry cameras;
	
	@SuppressWarnings("FieldCanBeLocal")
	private static WarpDrivePeripheralHandler peripheralHandler = null;
	
	public static Logger logger;
	
	@EventHandler
	public void onFMLPreInitialization(FMLPreInitializationEvent event) {
		logger = event.getModLog();
		
		WarpDriveConfig.onFMLpreInitialization(event.getModConfigurationDirectory().getAbsolutePath());
		
		RecipeSorter.register("warpdrive:particleShaped", RecipeParticleShapedOre.class, RecipeSorter.Category.SHAPED, "before:minecraft:shaped");
		RecipeSorter.register("warpdrive:tuningDriver", RecipeTuningDriver.class, RecipeSorter.Category.SHAPELESS, "before:minecraft:shapeless");
		
		if (event.getSide() == Side.CLIENT) {
			MinecraftForge.EVENT_BUS.register(new RenderOverlayAir());
			MinecraftForge.EVENT_BUS.register(new RenderOverlayCamera());
			MinecraftForge.EVENT_BUS.register(new RenderOverlayLocation());
			
			FMLCommonHandler.instance().bus().register(new ClientCameraHandler());
			
			RenderBlockStandard.renderId = RenderingRegistry.getNextAvailableRenderId();
			RenderingRegistry.registerBlockHandler(RenderBlockStandard.instance);
			
			RenderBlockForceField.renderId = RenderingRegistry.getNextAvailableRenderId();
			RenderingRegistry.registerBlockHandler(RenderBlockForceField.instance);
			
			RenderBlockOmnipanel.renderId = RenderingRegistry.getNextAvailableRenderId();
			RenderingRegistry.registerBlockHandler(RenderBlockOmnipanel.instance);
			
			RenderBlockShipScanner.renderId = RenderingRegistry.getNextAvailableRenderId();
			RenderingRegistry.registerBlockHandler(RenderBlockShipScanner.instance);
		}
	}
	
	@EventHandler
	public void onFMLInitialization(FMLInitializationEvent event) {
		PacketHandler.init();
		
		WarpDriveConfig.onFMLInitialization();
		
		// open access to Block.blockHardness
		fieldBlockHardness = Commons.getField(Block.class, "blockHardness", "field_149782_v");
		
		// CORE CONTROLLER
		blockShipController = new BlockShipController();
		
		GameRegistry.registerBlock(blockShipController, ItemBlockAbstractBase.class, "blockShipController");
		GameRegistry.registerTileEntity(TileEntityShipController.class, MODID + ":blockShipController");
		
		// SHIP CORE
		blockShipCore = new BlockShipCore();
		
		GameRegistry.registerBlock(blockShipCore, ItemBlockAbstractBase.class, "blockShipCore");
		GameRegistry.registerTileEntity(TileEntityShipCore.class, MODID + ":blockShipCore");
		
		// RADAR
		blockRadar = new BlockRadar();
		
		GameRegistry.registerBlock(blockRadar, ItemBlockAbstractBase.class, "blockRadar");
		GameRegistry.registerTileEntity(TileEntityRadar.class, MODID + ":blockRadar");
		
		// WARP ISOLATION
		blockWarpIsolation = new BlockWarpIsolation();
		
		GameRegistry.registerBlock(blockWarpIsolation, ItemBlockAbstractBase.class, "blockWarpIsolation");
		
		// AIR GENERATOR
		blockAirGenerator = new BlockAirGenerator();
		
		GameRegistry.registerBlock(blockAirGenerator, ItemBlockAbstractBase.class, "blockAirGenerator");
		GameRegistry.registerTileEntity(TileEntityAirGenerator.class, MODID + ":blockAirGenerator");
		
		blockAirGeneratorTiered = new Block[3];
		for(byte tier = 1; tier <= 3; tier++) {
			final int index = tier - 1;
			blockAirGeneratorTiered[index] = new BlockAirGeneratorTiered(tier);
			GameRegistry.registerBlock(blockAirGeneratorTiered[index], ItemBlockAbstractBase.class, "blockAirGenerator" + tier);
			GameRegistry.registerTileEntity(TileEntityAirGeneratorTiered.class, MODID + ":blockAirGenerator" + tier);
		}
		
		// AIR BLOCK
		blockAir = new BlockAir();
		blockAirFlow = new BlockAirFlow();
		blockAirSource = new BlockAirSource();
		blockAirShield = new BlockAirShield();
		
		GameRegistry.registerBlock(blockAir, ItemBlockAbstractBase.class, "blockAir");
		GameRegistry.registerBlock(blockAirFlow, ItemBlockAbstractBase.class, "blockAirFlow");
		GameRegistry.registerBlock(blockAirSource, ItemBlockAbstractBase.class, "blockAirSource");
		GameRegistry.registerBlock(blockAirShield, ItemBlockAirShield.class, "blockAirShield");
		
		// BEDROCK GLASS
		blockBedrockGlass = new BlockBedrockGlass();
		
		GameRegistry.registerBlock(blockBedrockGlass, ItemBlockAbstractBase.class, "blockBedrockGlass");
		
		// GAS BLOCK
		blockGas = new BlockGas();
		
		GameRegistry.registerBlock(blockGas, ItemBlockAbstractBase.class, "blockGas");
		
		// LASER EMITTER
		blockLaser = new BlockLaser();
		
		GameRegistry.registerBlock(blockLaser, ItemBlockAbstractBase.class, "blockLaser");
		GameRegistry.registerTileEntity(TileEntityLaser.class, MODID + ":blockLaser");
		
		// LASER EMITTER WITH CAMERA
		blockLaserCamera = new BlockLaserCamera();
		
		GameRegistry.registerBlock(blockLaserCamera, ItemBlockAbstractBase.class, "blockLaserCamera");
		GameRegistry.registerTileEntity(TileEntityLaserCamera.class, MODID + ":blockLaserCamera");
		
		// LASER EMITTER WITH CAMERA
		blockWeaponController = new BlockWeaponController();
		
		GameRegistry.registerBlock(blockWeaponController, ItemBlockAbstractBase.class, "blockWeaponController");
		GameRegistry.registerTileEntity(TileEntityWeaponController.class, MODID + ":blockWeaponController");
		
		// CAMERA
		blockCamera = new BlockCamera();
		
		GameRegistry.registerBlock(blockCamera, ItemBlockAbstractBase.class, "blockCamera");
		GameRegistry.registerTileEntity(TileEntityCamera.class, MODID + ":blockCamera");
		
		// MONITOR
		blockMonitor = new BlockMonitor();
		
		GameRegistry.registerBlock(blockMonitor, ItemBlockAbstractBase.class, "blockMonitor");
		GameRegistry.registerTileEntity(TileEntityMonitor.class, MODID + ":blockMonitor");
		
		// MINING LASER
		blockMiningLaser = new BlockMiningLaser();
		
		GameRegistry.registerBlock(blockMiningLaser, ItemBlockAbstractBase.class, "blockMiningLaser");
		GameRegistry.registerTileEntity(TileEntityMiningLaser.class, MODID + ":blockMiningLaser");
		
		// LASER TREE FARM
		blockLaserTreeFarm = new BlockLaserTreeFarm();
		
		GameRegistry.registerBlock(blockLaserTreeFarm, ItemBlockAbstractBase.class, "blockLaserTreeFarm");
		GameRegistry.registerTileEntity(TileEntityLaserTreeFarm.class, MODID + ":blockLaserTreeFarm");
		
		// LASER MEDIUM
		blockLaserMedium = new BlockLaserMedium();
		
		GameRegistry.registerBlock(blockLaserMedium, ItemBlockAbstractBase.class, "blockLaserMedium");
		GameRegistry.registerTileEntity(TileEntityLaserMedium.class, MODID + ":blockLaserMedium");
		
		// LIFT
		blockLift = new BlockLift();
		
		GameRegistry.registerBlock(blockLift, ItemBlockAbstractBase.class, "blockLift");
		GameRegistry.registerTileEntity(TileEntityLift.class, MODID + ":blockLift");
		
		// IRIDIUM BLOCK
		blockIridium = new BlockIridium();
		
		GameRegistry.registerBlock(blockIridium, ItemBlockAbstractBase.class, "blockIridium");
		
		// HIGHLY ADVANCED MACHINE BLOCK
		blockHighlyAdvancedMachine = new BlockHighlyAdvancedMachine();
		
		GameRegistry.registerBlock(blockHighlyAdvancedMachine, ItemBlockAbstractBase.class, "blockHighlyAdvancedMachine");
		
		// building blocks
		blockShipScanner = new BlockShipScanner();
		
		GameRegistry.registerBlock(blockShipScanner, ItemBlockAbstractBase.class, "blockShipScanner");
		GameRegistry.registerTileEntity(TileEntityShipScanner.class, MODID + ":blockShipScanner");
		
		// CLOAKING DEVICE CORE
		blockCloakingCore = new BlockCloakingCore();
		
		GameRegistry.registerBlock(blockCloakingCore, ItemBlockAbstractBase.class, "blockCloakingCore");
		GameRegistry.registerTileEntity(TileEntityCloakingCore.class, MODID + ":blockCloakingCore");
		
		// CLOAKING DEVICE COIL
		blockCloakingCoil = new BlockCloakingCoil();
		
		GameRegistry.registerBlock(blockCloakingCoil, ItemBlockAbstractBase.class, "blockCloakingCoil");
		
		// TRANSPORTER
		blockTransporter = new BlockTransporter();
		
		GameRegistry.registerBlock(blockTransporter, ItemBlockAbstractBase.class, "blockTransporter");
		GameRegistry.registerTileEntity(TileEntityTransporter.class, MODID + ":blockTransporter");
		
		// IC2 REACTOR LASER MONITOR
		if (WarpDriveConfig.isIndustrialCraft2Loaded) {
			blockIC2reactorLaserMonitor = new BlockIC2reactorLaserMonitor();
			
			GameRegistry.registerBlock(blockIC2reactorLaserMonitor, ItemBlockAbstractBase.class, "blockIC2reactorLaserMonitor");
			GameRegistry.registerTileEntity(TileEntityIC2reactorLaserMonitor.class, MODID + ":blockIC2reactorLaserMonitor");
		}
		
		// TRANSPORT BEACON
		blockTransportBeacon = new BlockTransportBeacon();
		
		GameRegistry.registerBlock(blockTransportBeacon, ItemBlockAbstractBase.class, "blockTransportBeacon");
		
		// POWER REACTOR, LASER, STORE
		blockEnanReactorCore = new BlockEnanReactorCore();
		GameRegistry.registerBlock(blockEnanReactorCore, ItemBlockAbstractBase.class, "blockEnanReactorCore");
		GameRegistry.registerTileEntity(TileEntityEnanReactorCore.class, MODID + ":blockEnanReactorCore");
		
		blockEnanReactorLaser = new BlockEnanReactorLaser();
		GameRegistry.registerBlock(blockEnanReactorLaser, ItemBlockAbstractBase.class, "blockEnanReactorLaser");
		GameRegistry.registerTileEntity(TileEntityEnanReactorLaser.class, MODID + ":blockEnanReactorLaser");
		
		blockEnergyBank = new BlockEnergyBank();
		GameRegistry.registerBlock(blockEnergyBank, ItemBlockAbstractBase.class, "blockEnergyBank");
		GameRegistry.registerTileEntity(TileEntityEnergyBank.class, MODID + ":blockEnergyBank");
		
		// CHUNK LOADER
		blockChunkLoader = new BlockChunkLoader();
		GameRegistry.registerBlock(blockChunkLoader, ItemBlockAbstractBase.class, "blockChunkLoader");
		GameRegistry.registerTileEntity(TileEntityChunkLoader.class, MODID + ":blockChunkLoader");
		
		// force field blocks and items
		blockForceFields = new Block[3];
		blockForceFieldProjectors = new Block[3];
		blockForceFieldRelays = new Block[3];
		for(byte tier = 1; tier <= 3; tier++) {
			int index = tier - 1;
			// FORCE FIELD
			blockForceFields[index] = new BlockForceField(tier);
			GameRegistry.registerBlock(blockForceFields[index], ItemBlockAbstractBase.class, "blockForceField" + tier);
			GameRegistry.registerTileEntity(TileEntityForceField.class, MODID + ":blockForceField" + tier);
			
			// FORCE FIELD PROJECTOR
			blockForceFieldProjectors[index] = new BlockForceFieldProjector(tier);
			GameRegistry.registerBlock(blockForceFieldProjectors[index], ItemBlockForceFieldProjector.class, "blockProjector" + tier);
			GameRegistry.registerTileEntity(TileEntityForceFieldProjector.class, MODID + ":blockProjector" + tier);
			
			// FORCE FIELD RELAY
			blockForceFieldRelays[index] = new BlockForceFieldRelay(tier);
			GameRegistry.registerBlock(blockForceFieldRelays[index], ItemBlockForceFieldRelay.class, "blockForceFieldRelay" + tier);
			GameRegistry.registerTileEntity(TileEntityForceFieldRelay.class, MODID + ":blockForceFieldRelay" + tier);
		}
		/* TODO
		blockSecurityStation = new BlockSecurityStation();
		GameRegistry.registerBlock(blockSecurityStation, ItemBlockAbstractBase.class, "blockSecurityStation");
		GameRegistry.registerTileEntity(TileEntitySecurityStation.class, MODID + ":blockSecurityStation");
		*/
		
		itemForceFieldShape = new ItemForceFieldShape();
		GameRegistry.registerItem(itemForceFieldShape, "itemForceFieldShape");
		
		itemForceFieldUpgrade = new ItemForceFieldUpgrade();
		GameRegistry.registerItem(itemForceFieldUpgrade, "itemForceFieldUpgrade");
		
		if (WarpDriveConfig.ACCELERATOR_ENABLE) {
			// ACCELERATOR CONTROLLER
			blockAcceleratorController = new BlockAcceleratorController();
			GameRegistry.registerBlock(blockAcceleratorController, ItemBlockAbstractBase.class, "blockAcceleratorController");
			GameRegistry.registerTileEntity(TileEntityAcceleratorController.class, MODID + ":blockAcceleratorController");
			
			// ACCELERATOR CONTROL POINT 
			blockAcceleratorControlPoint = new BlockAcceleratorControlPoint();
			GameRegistry.registerBlock(blockAcceleratorControlPoint, ItemBlockAbstractBase.class, "blockAcceleratorControlPoint");
			GameRegistry.registerTileEntity(TileEntityAcceleratorControlPoint.class, MODID + ":blockAcceleratorControlPoint");
			
			// PARTICLES COLLIDER 
			blockParticlesCollider = new BlockParticlesCollider();
			GameRegistry.registerBlock(blockParticlesCollider, ItemBlockAbstractBase.class, "blockParticlesCollider");
			
			// PARTICLES INJECTOR 
			blockParticlesInjector = new BlockParticlesInjector();
			GameRegistry.registerBlock(blockParticlesInjector, ItemBlockAbstractBase.class, "blockParticlesInjector");
			GameRegistry.registerTileEntity(TileEntityParticlesInjector.class, MODID + ":blockParticlesInjector");
			
			// VOID SHELL PLAIN/GLASS 
			blockVoidShellPlain = new BlockVoidShellPlain();
			GameRegistry.registerBlock(blockVoidShellPlain, ItemBlockAbstractBase.class, "blockVoidShellPlain");
			blockVoidShellGlass = new BlockVoidShellGlass();
			GameRegistry.registerBlock(blockVoidShellGlass, ItemBlockAbstractBase.class, "blockVoidShellGlass");
			
			blockElectromagnetPlain = new Block[3];
			blockElectromagnetGlass = new Block[3];
			blockChillers = new Block[3];
			for (byte tier = 1; tier <= 3; tier++) {
				final int index = tier - 1;
				// plain electromagnets
				blockElectromagnetPlain[index] = new BlockElectromagnetPlain(tier);
				GameRegistry.registerBlock(blockElectromagnetPlain[index], ItemBlockAbstractBase.class, "blockElectromagnetPlain" + tier);
				
				// glass electromagnets
				blockElectromagnetGlass[index] = new BlockElectromagnetGlass(tier);
				GameRegistry.registerBlock(blockElectromagnetGlass[index], ItemBlockAbstractBase.class, "blockElectromagnetGlass" + tier);
				
				// chiller
				blockChillers[index] = new BlockChiller(tier);
				GameRegistry.registerBlock(blockChillers[index], ItemBlockAbstractBase.class, "blockChiller" + tier);
			}
		}
		
		// decorative
		blockDecorative = new BlockDecorative();
		GameRegistry.registerBlock(blockDecorative, ItemBlockDecorative.class, "blockDecorative");
		
		// hull blocks
		blockHulls_plain = new Block[3][EnumHullPlainType.length];
		blockHulls_glass = new Block[3];
		blockHulls_omnipanel = new Block[3];
		blockHulls_stairs = new Block[3][16];
		blockHulls_slab = new Block[3][16];
		
		for(byte tier = 1; tier <= 3; tier++) {
			int index = tier - 1;
			for (EnumHullPlainType enumHullPlainType : EnumHullPlainType.values()) {
				blockHulls_plain[index][enumHullPlainType.ordinal()] = new BlockHullPlain(tier, enumHullPlainType);
				GameRegistry.registerBlock(blockHulls_plain[index][enumHullPlainType.ordinal()], ItemBlockHull.class, "blockHull" + tier + "_" + enumHullPlainType.getName());
			}
			blockHulls_glass[index] = new BlockHullGlass(tier);
			GameRegistry.registerBlock(blockHulls_glass[index], ItemBlockHull.class, "blockHull" + tier + "_glass");
			blockHulls_omnipanel[index] = new BlockHullOmnipanel(tier);
			GameRegistry.registerBlock(blockHulls_omnipanel[index], ItemBlockHull.class, "blockHull" + tier + "_omnipanel");
			for (int woolColor = 0; woolColor <= 15; woolColor++) {
				blockHulls_stairs[index][woolColor] = new BlockHullStairs(blockHulls_plain[index][0], woolColor, tier);
				GameRegistry.registerBlock(blockHulls_stairs[index][woolColor], ItemBlockHull.class, "blockHull" + tier + "_stairs_" + ItemDye.field_150923_a[BlockColored.func_150031_c(woolColor)]);
				blockHulls_slab[index][woolColor] = new BlockHullSlab(woolColor, tier);
				GameRegistry.registerBlock(blockHulls_slab[index][woolColor], ItemBlockHullSlab.class, "blockHull" + tier + "_slab_" + ItemDye.field_150923_a[BlockColored.func_150031_c(woolColor)]);
			}
		}
		
		// SIRENS
		blockSiren = new BlockSiren();
		
		GameRegistry.registerBlock(blockSiren, ItemBlockAbstractBase.class, "siren");
		GameRegistry.registerTileEntity(TileEntitySiren.class, MODID + ":tileEntitySiren");
		
		// REACTOR LASER FOCUS
		if (WarpDriveConfig.isIndustrialCraft2Loaded) {
			itemIC2reactorLaserFocus = new ItemIC2reactorLaserFocus();
			GameRegistry.registerItem(itemIC2reactorLaserFocus, "itemIC2reactorLaserFocus");
		}
		
		// COMPONENT ITEMS
		itemComponent = new ItemComponent();
		GameRegistry.registerItem(itemComponent, "itemComponent");
		
		itemShipToken = new ItemShipToken();
		GameRegistry.registerItem(itemShipToken, "itemShipToken");
		
		itemWarpArmor = new ItemArmor[4];
		for (int armorPart = 0; armorPart < 4; armorPart++) {
			itemWarpArmor[armorPart] = new ItemWarpArmor(armorMaterial, 3, armorPart);
			GameRegistry.registerItem(itemWarpArmor[armorPart], "itemWarpArmor_" + ItemWarpArmor.suffixes[armorPart]);
		}
		
		itemAirTanks = new ItemAirTank[4];
		for (int index = 0; index < 4; index++) {
			itemAirTanks[index] = new ItemAirTank((byte) index);
			GameRegistry.registerItem(itemAirTanks[index], "itemAirTank" + index);
		}
		
		if (WarpDriveConfig.RECIPES_ENABLE_VANILLA) {
			itemUpgrade = new ItemUpgrade();
			GameRegistry.registerItem(itemUpgrade, "itemUpgrade");
		}
		
		// TOOL ITEMS
		itemTuningFork = new ItemTuningFork();
		GameRegistry.registerItem(itemTuningFork, "itemTuningFork");
		
		itemTuningDriver = new ItemTuningDriver();
		GameRegistry.registerItem(itemTuningDriver, "itemTuningDriver");
		
		// electromagnetic cell
		if (WarpDriveConfig.ACCELERATOR_ENABLE) {
			itemElectromagneticCell = new ItemElectromagneticCell();
			GameRegistry.registerItem(itemElectromagneticCell, "itemElectromagneticCell");
		}
		
		// damage sources
		damageAsphyxia = new DamageAsphyxia();
		damageCold = new DamageCold();
		damageIrradiation = new DamageIrradiation();
		damageLaser = new DamageLaser();
		damageShock = new DamageShock();
		damageTeleportation = new DamageTeleportation();
		damageWarm = new DamageWarm();
		
		proxy.registerEntities();
		proxy.registerRendering();
		
		ForgeChunkManager.setForcedChunkLoadingCallback(instance, instance);
		
		commonWorldGenerator = new CommonWorldGenerator();
		GameRegistry.registerWorldGenerator(commonWorldGenerator, 0);
		
		spaceBiome = (new BiomeSpace(WarpDriveConfig.G_SPACE_BIOME_ID)).setColor(0).setDisableRain().setBiomeName("Space");
		BiomeDictionary.registerBiomeType(spaceBiome, BiomeDictionary.Type.DEAD, BiomeDictionary.Type.WASTELAND);
		DimensionManager.registerProviderType(WarpDriveConfig.G_SPACE_PROVIDER_ID, SpaceWorldProvider.class, true);
		
		DimensionManager.registerProviderType(WarpDriveConfig.G_HYPERSPACE_PROVIDER_ID, HyperSpaceWorldProvider.class, true);
		
		CelestialObjectManager.onFMLInitialization();
		
		if (getClass().desiredAssertionStatus()) {
			Recipes.patchOredictionary();
		}
	}
	
	@EventHandler
	public void onFMLPostInitialization(FMLPostInitializationEvent event) {
		/* @TODO not sure why it would be needed, disabling for now
		// load all owned dimensions at boot
		for (CelestialObject celestialObject : CelestialObjectManager.celestialObjects) {
			if (celestialObject.provider.equals(CelestialObject.PROVIDER_OTHER)) {
				DimensionManager.getWorld(celestialObject.dimensionId);
			}
		}
		/**/
		
		WarpDriveConfig.onFMLPostInitialization();
		
		if (WarpDriveConfig.RECIPES_ENABLE_DYNAMIC) {
			Recipes.initDynamic();
		} else {
			if (WarpDriveConfig.isIndustrialCraft2Loaded && WarpDriveConfig.RECIPES_ENABLE_IC2) {
				Recipes.initIC2();
			}
			if (WarpDriveConfig.isIndustrialCraft2Loaded && WarpDriveConfig.RECIPES_ENABLE_HARD_IC2) {
				Recipes.initHardIC2();
			}
			if (WarpDriveConfig.RECIPES_ENABLE_VANILLA) {
				Recipes.initVanilla();
			}
		}
		
		// Registers
		starMap = new StarMapRegistry();
		jumpgates = new JumpgatesRegistry();
		cloaks = new CloakManager();
		cameras = new CamerasRegistry();
		
		// Event handlers
		MinecraftForge.EVENT_BUS.register(new ClientHandler());
		
		MinecraftForge.EVENT_BUS.register(new LivingHandler());
		
		if (WarpDriveConfig.isComputerCraftLoaded) {
			peripheralHandler = new WarpDrivePeripheralHandler();
			peripheralHandler.register();
		}
		
		WorldHandler worldHandler = new WorldHandler();
		MinecraftForge.EVENT_BUS.register(worldHandler);
		FMLCommonHandler.instance().bus().register(worldHandler);
		
		ChunkHandler chunkHandler = new ChunkHandler();
		MinecraftForge.EVENT_BUS.register(chunkHandler);
		FMLCommonHandler.instance().bus().register(chunkHandler);
	}
	
	@EventHandler
	public void onFMLServerStarting(FMLServerStartingEvent event) {
		event.registerServerCommand(new CommandDebug());
		event.registerServerCommand(new CommandDump());
		event.registerServerCommand(new CommandEntity());
		event.registerServerCommand(new CommandFind());
		event.registerServerCommand(new CommandGenerate());
		event.registerServerCommand(new CommandBed());
		event.registerServerCommand(new CommandInvisible());
		event.registerServerCommand(new CommandJumpgates());
		event.registerServerCommand(new CommandReload());
		event.registerServerCommand(new CommandSpace());
	}
	
	public Ticket registerChunkLoadTileEntity(TileEntityAbstractChunkLoading tileEntity) {
		World worldObj = tileEntity.getWorldObj();
		if (ForgeChunkManager.ticketCountAvailableFor(this, worldObj) > 0) {
			Ticket ticket = ForgeChunkManager.requestTicket(this, worldObj, Type.NORMAL);
			if (ticket != null) {
				tileEntity.giveTicket(ticket); // FIXME calling the caller is a bad idea
				return ticket;
			} else {
				WarpDrive.logger.error("Ticket not granted");
			}
		} else {
			WarpDrive.logger.error("No tickets left!");
		}
		return null;
	}
	
	@Override
	public void ticketsLoaded(List<Ticket> tickets, World world) {
		for (Ticket ticket : tickets) {
			NBTTagCompound data = ticket.getModData();
			if (data != null) {
				int w = data.getInteger("ticketWorldObj");
				int x = data.getInteger("ticketX");
				int y = data.getInteger("ticketY");
				int z = data.getInteger("ticketZ");
				if (w != 0 || x != 0 || y != 0 || z != 0) {
					WorldServer worldServer = DimensionManager.getWorld(w);
					if (worldServer != null) {// skip non-loaded worlds
						TileEntity tileEntity = worldServer.getTileEntity(x, y, z);
						if (tileEntity != null && tileEntity instanceof TileEntityAbstractChunkLoading) {
							if (((TileEntityAbstractChunkLoading) tileEntity).shouldChunkLoad()) {
								WarpDrive.logger.info("ChunkLoadingTicket is loading " + tileEntity);
								((TileEntityAbstractChunkLoading) tileEntity).giveTicket(ticket);
								((TileEntityAbstractChunkLoading) tileEntity).refreshLoading(true);
								return;
							}
						}
					}
				}
			}
			
			ForgeChunkManager.releaseTicket(ticket);
		}
	}
	
	@Mod.EventHandler
	public void onFMLMissingMappings(FMLMissingMappingsEvent event) {
		for (FMLMissingMappingsEvent.MissingMapping mapping: event.get()) {
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
						mapping.remap(itemWarpArmor[0]);
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
						mapping.remap(Item.getItemFromBlock(blockEnergyBank));
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
						mapping.remap(Item.getItemFromBlock(blockIC2reactorLaserMonitor));
						break;
					case "WarpDrive:scannerBlock":
						mapping.remap(Item.getItemFromBlock(blockShipScanner));
						break;
					case "WarpDrive:transportBeacon":
						mapping.remap(Item.getItemFromBlock(blockTransportBeacon));
						break;
					case "WarpDrive:transporter":
						mapping.remap(Item.getItemFromBlock(blockTransporter));
						break;
					case "WarpDrive:upgrade":
						mapping.remap(itemUpgrade);
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
						mapping.remap(blockEnergyBank);
						break;
					case "WarpDrive:protocolBlock":
						mapping.remap(blockShipController);
						break;
					case "WarpDrive:radarBlock":
						mapping.remap(blockRadar);
						break;
					case "WarpDrive:reactorMonitor":
						mapping.remap(blockIC2reactorLaserMonitor);
						break;
					case "WarpDrive:scannerBlock":
						mapping.remap(blockShipScanner);
						break;
					case "WarpDrive:transportBeacon":
						mapping.remap(blockTransportBeacon);
						break;
					case "WarpDrive:transporter":
						mapping.remap(blockTransporter);
						break;
					case "WarpDrive:warpCore":
						mapping.remap(blockShipCore);
						break;
				}
			}
		}
	}
	
}
