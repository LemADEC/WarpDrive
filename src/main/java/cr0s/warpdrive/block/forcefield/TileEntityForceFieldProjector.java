package cr0s.warpdrive.block.forcefield;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBeamFrequency;
import cr0s.warpdrive.api.IForceFieldShape;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumForceFieldShape;
import cr0s.warpdrive.data.EnumForceFieldUpgrade;
import cr0s.warpdrive.data.ForceFieldSetup;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.data.VectorI;
import cr0s.warpdrive.network.PacketHandler;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockStaticLiquid;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.StatCollector;

import cpw.mods.fml.common.Optional;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

public class TileEntityForceFieldProjector extends TileEntityAbstractForceField {
	
	private static final int PROJECTOR_MAX_ENERGY_STORED = 30000;
	private static final int PROJECTOR_COOLDOWN_TICKS = 300;
	public static final int PROJECTOR_PROJECTION_UPDATE_TICKS = 8;
	private static final int PROJECTOR_SETUP_TICKS = 20;
	private static final int PROJECTOR_SOUND_UPDATE_TICKS = 60;
	private static final int PROJECTOR_GUIDE_UPDATE_TICKS = 300;
	
	// persistent properties
	public boolean isDoubleSided;
	private EnumForceFieldShape shape;
	// rotation provided by player, before applying block orientation
	private float rotationYaw;
	private float rotationPitch;
	private float rotationRoll;
	private Vector3 v3Min = new Vector3(-1.0D, -1.0D, -1.0D);
	private Vector3 v3Max = new Vector3( 1.0D,  1.0D,  1.0D);
	private Vector3 v3Translation = new Vector3( 0.0D,  0.0D,  0.0D);
	private boolean legacy_isOn = false;
	
	// computed properties
	private int maxEnergyStored;
	private int cooldownTicks;
	private int setupTicks;
	private int updateTicks;
	private int soundTicks;
	private int guideTicks;
	private double damagesEnergyCost = 0.0D;
	private final HashSet<UUID> setInteractedEntities = new HashSet<>();
	protected boolean isPowered = true;
	private ForceFieldSetup cache_forceFieldSetup;
	private ForceFieldSetup legacy_forceFieldSetup;
	private double consumptionLeftOver = 0.0D;
	
	// carry over speed to next tick, useful for slow interactions
	private float carryScanSpeed;
	private float carryPlaceSpeed;
	
	// allow only one computation at a time
	private static final AtomicBoolean isGlobalThreadRunning = new AtomicBoolean(false);
	// computation is ongoing for this specific tile
	private final AtomicBoolean isThreadRunning = new AtomicBoolean(false);
	// parameters have changed, new computation is required
	private final AtomicBoolean isDirty = new AtomicBoolean(true);
	
	private Set<VectorI> calculated_interiorField = null;
	private Set<VectorI> calculated_forceField = null;
	private Iterator<VectorI> iteratorForcefield = null;
	
	// currently placed forcefield blocks
	private final Set<VectorI> vForceFields = new HashSet<>();
	
	public TileEntityForceFieldProjector() {
		super();
		
		peripheralName = "warpdriveForceFieldProjector";
		addMethods(new String[] {
			"min",
			"max",
			"rotation",
			"state",
			"translation"
		});
		CC_scripts = Arrays.asList("enable", "disable");
		
		for (EnumForceFieldUpgrade enumForceFieldUpgrade : EnumForceFieldUpgrade.values()) {
			if (enumForceFieldUpgrade.maxCountOnProjector > 0) {
				setUpgradeMaxCount(enumForceFieldUpgrade, enumForceFieldUpgrade.maxCountOnProjector);
			}
		}
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
		maxEnergyStored = PROJECTOR_MAX_ENERGY_STORED * (1 + 2 * tier);
		cooldownTicks = 0;
		setupTicks = worldObj.rand.nextInt(PROJECTOR_SETUP_TICKS);
		updateTicks = worldObj.rand.nextInt(PROJECTOR_PROJECTION_UPDATE_TICKS);
		guideTicks = PROJECTOR_GUIDE_UPDATE_TICKS;
	}
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		
		if (worldObj.isRemote) {
			return;
		}
		
		// Frequency is not set
		if (!isConnected) {
			return;
		}
		
		// clear setup cache periodically
		setupTicks--;
		if (setupTicks <= 0) {
			setupTicks = PROJECTOR_SETUP_TICKS;
			if (cache_forceFieldSetup != null) {
				legacy_forceFieldSetup = cache_forceFieldSetup;
				cache_forceFieldSetup = null;
			}
		}
		
		// update counters
		if (cooldownTicks > 0) {
			cooldownTicks--;
		}
		if (guideTicks > 0) {
			guideTicks--;
		}
		
		// Powered ?
		ForceFieldSetup forceFieldSetup = getForceFieldSetup();
		int energyRequired;
		if (!legacy_isOn) {
			energyRequired = (int)Math.round(forceFieldSetup.startupEnergyCost + forceFieldSetup.placeEnergyCost * forceFieldSetup.placeSpeed * PROJECTOR_PROJECTION_UPDATE_TICKS / 20.0F);
		} else {
			energyRequired = (int)Math.round(                                    forceFieldSetup.scanEnergyCost * forceFieldSetup.scanSpeed * PROJECTOR_PROJECTION_UPDATE_TICKS / 20.0F);
		}
		if (energyRequired > energy_getMaxStorage()) {
			WarpDrive.logger.error("Force field projector requires " + energyRequired + " to get started but can only store " + energy_getMaxStorage());
		}
		final boolean new_isPowered = energy_getEnergyStored() >= energyRequired;
		if (isPowered != new_isPowered) {
			isPowered = new_isPowered;
			markDirty();
		}
		
		boolean isEnabledAndValid = isEnabled && isValid();
		boolean isOn = isEnabledAndValid && cooldownTicks <= 0 && isPowered;
		if (isOn) {
			if (!legacy_isOn) {
				consumeEnergy(forceFieldSetup.startupEnergyCost, false);
				if (WarpDriveConfig.LOGGING_FORCEFIELD) {
					WarpDrive.logger.info(this + " starting up...");
				}
				legacy_isOn = true;
			}
			cooldownTicks = 0;
			
			int countEntityInteractions = setInteractedEntities.size();
			if (countEntityInteractions > 0) {
				setInteractedEntities.clear();
				consumeEnergy(forceFieldSetup.getEntityEnergyCost(countEntityInteractions), false);
			}
			
			if (damagesEnergyCost > 0.0D) {
				if (WarpDriveConfig.LOGGING_FORCEFIELD) {
					WarpDrive.logger.info(String.format("%s damages received, energy lost: %.6f", toString(), damagesEnergyCost));
				}
				consumeEnergy(damagesEnergyCost, false);
				damagesEnergyCost = 0.0D;
			}
			
			updateTicks--;
			if (updateTicks <= 0) {
				updateTicks = PROJECTOR_PROJECTION_UPDATE_TICKS;
				if (!isCalculated()) {
					calculateForceField();
				} else {
					projectForceField();
				}
			}
			
			soundTicks--;
			if (soundTicks < 0) {
				soundTicks = PROJECTOR_SOUND_UPDATE_TICKS;
				if (!hasUpgrade(EnumForceFieldUpgrade.SILENCER)) {
					worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, "warpdrive:projecting", 1.0F, 0.85F + 0.15F * worldObj.rand.nextFloat());
				}
			}
			
		} else {
			if (legacy_isOn) {
				if (WarpDriveConfig.LOGGING_FORCEFIELD) {
					WarpDrive.logger.info(this + " shutting down...");
				}
				legacy_isOn = false;
				cooldownTicks = PROJECTOR_COOLDOWN_TICKS;
				guideTicks = 0;
			}
			destroyForceField(false);
			
			if (isEnabledAndValid) {
				if (guideTicks <= 0) {
					guideTicks = PROJECTOR_GUIDE_UPDATE_TICKS;
					
					String msg = StatCollector.translateToLocalFormatted("warpdrive.guide.prefix", getBlockType().getLocalizedName())
					           + StatCollector.translateToLocalFormatted("warpdrive.forcefield.guide.lowPower");
					
					AxisAlignedBB axisalignedbb = AxisAlignedBB.getBoundingBox(xCoord - 10, yCoord - 10, zCoord - 10, xCoord + 10, yCoord + 10, zCoord + 10);
					List<Entity> list = worldObj.getEntitiesWithinAABBExcludingEntity(null, axisalignedbb);
					
					for (Entity entity : list) {
						if (entity == null || (!(entity instanceof EntityPlayer)) || entity instanceof FakePlayer) {
							continue;
						}
						
						Commons.addChatMessage((EntityPlayer) entity, msg);
					}
				}
			}
		}
	}
	
	@Override
	public void invalidate() {
		super.invalidate();
		destroyForceField(true);
	}
	
	public boolean isValid() {
		return getShape() != EnumForceFieldShape.NONE;
	}
	
	public boolean isCalculated() {
		return !isDirty.get() && !isThreadRunning.get();
	}
	
	private void calculateForceField() {
		if ((!worldObj.isRemote) && isValid()) {
			if (!isGlobalThreadRunning.getAndSet(true)) {
				if (WarpDriveConfig.LOGGING_FORCEFIELD) {
					WarpDrive.logger.info("Calculation initiated for " + this);
				}
				isThreadRunning.set(true);
				isDirty.set(false);
				iteratorForcefield = null;
				calculated_interiorField = null;
				calculated_forceField = null;
				vForceFields.clear();
				
				new ThreadCalculation(this).start();
			}
		}
	}
	
	private void calculation_done(Set<VectorI> interiorField, Set<VectorI> forceField) {
		if (WarpDriveConfig.LOGGING_FORCEFIELD) {
			WarpDrive.logger.info("Calculation done for " + this);
		}
		if (interiorField == null || forceField == null) {
			calculated_interiorField = new HashSet<>(0);
			calculated_forceField = new HashSet<>(0);
		} else {
			calculated_interiorField = interiorField;
			calculated_forceField = forceField;
		}
		isThreadRunning.set(false);
		isGlobalThreadRunning.set(false);
	}
	
	boolean isOn() {
		return legacy_isOn;
	}
	
	boolean isPartOfForceField(VectorI vector) {
		if (!isEnabled || !isValid()) {
			return false;
		}
		if (!isCalculated()) {
			return true;
		}
		// only consider the forcefield itself
		return calculated_forceField.contains(vector);
	}
	
	private boolean isPartOfInterior(VectorI vector) {
		if (!isEnabled || !isValid()) {
			return false;
		}
		if (!isCalculated()) {
			return false;
		}
		// only consider the forcefield interior
		return calculated_interiorField.contains(vector);
	}
	
	public boolean onEntityInteracted(final UUID uniqueID) {
		return setInteractedEntities.add(uniqueID);
	}
	
	public void onEnergyDamage(final double energyCost) {
		damagesEnergyCost += energyCost;
	}
	
	private void projectForceField() {
		assert(!worldObj.isRemote && isCalculated());
		
		ForceFieldSetup forceFieldSetup = getForceFieldSetup();
		
		// compute maximum number of blocks to scan
		int countScanned = 0;
		float floatScanSpeed = Math.min(forceFieldSetup.scanSpeed * PROJECTOR_PROJECTION_UPDATE_TICKS / 20.0F + carryScanSpeed, calculated_forceField.size());
		int countMaxScanned = (int)Math.floor(floatScanSpeed);
		carryScanSpeed = floatScanSpeed - countMaxScanned;
		
		// compute maximum number of blocks to place
		int countPlaced = 0;
		float floatPlaceSpeed = Math.min(forceFieldSetup.placeSpeed * PROJECTOR_PROJECTION_UPDATE_TICKS / 20.0F + carryPlaceSpeed, calculated_forceField.size());
		int countMaxPlaced = (int)Math.floor(floatPlaceSpeed);
		carryPlaceSpeed = floatPlaceSpeed - countMaxPlaced;
		
		// evaluate force field block metadata
		int metadataForceField = Math.min(15, (beamFrequency * 16) / IBeamFrequency.BEAM_FREQUENCY_MAX);
		if (forceFieldSetup.getCamouflageBlock() != null) {
			metadataForceField = forceFieldSetup.getCamouflageMetadata();
		}
		
		VectorI vector;
		Block block;
		boolean doProjectThisBlock;
		
		while ( countScanned < countMaxScanned
		     && countPlaced < countMaxPlaced
			 && consumeEnergy(Math.max(forceFieldSetup.scanEnergyCost, forceFieldSetup.placeEnergyCost), true)) {
			if (iteratorForcefield == null || !iteratorForcefield.hasNext()) {
				iteratorForcefield = calculated_forceField.iterator();
			}
			countScanned++;
			
			vector = iteratorForcefield.next();
			
			if (!worldObj.blockExists(vector.x, vector.y, vector.z) || !worldObj.getChunkFromBlockCoords(vector.x, vector.z).isChunkLoaded) {
				continue;
			}
			
			block = vector.getBlock(worldObj);
			doProjectThisBlock = true;
			
			// skip if fusion upgrade is present and it's inside another projector area
			if (forceFieldSetup.hasFusion) {
				for (TileEntityForceFieldProjector projector : forceFieldSetup.projectors) {
					if (projector.isPartOfInterior(vector)) {
						doProjectThisBlock = false;
						break;
					}
				}
			}
			
			// skip if block properties prevents it
			if (doProjectThisBlock && (block != Blocks.tallgrass) && (block != Blocks.deadbush) && !Dictionary.BLOCKS_EXPANDABLE.contains(block)) {
				// MFR laser is unbreakable and replaceable
				// Liquid, vine and snow are replaceable
				if (block instanceof BlockLiquid) {
					Fluid fluid = FluidRegistry.lookupFluidForBlock(block);
					doProjectThisBlock = fluid == null || forceFieldSetup.pumping_maxViscosity >= fluid.getViscosity();
					
				} else if (forceFieldSetup.breaking_maxHardness > 0) {
					float blockHardness = block.getBlockHardness(worldObj, vector.x, vector.y, vector.z);
					// stops on unbreakable or too hard
					if (blockHardness == -1.0F || blockHardness > forceFieldSetup.breaking_maxHardness || worldObj.isAirBlock(vector.x, vector.y, vector.z)) {
						doProjectThisBlock = false;
					}
					
				} else {// doesn't have disintegration, not a liquid
					
					// recover force field blocks
					if (block instanceof BlockForceField) {
						TileEntity tileEntity = vector.getTileEntity(worldObj);
						if (!(tileEntity instanceof TileEntityForceField)) {
							// missing a valid tile entity
							// => force a new placement
							worldObj.setBlockToAir(vector.x, vector.y, vector.z);
							block = Blocks.air;
							
						} else {
							TileEntityForceField tileEntityForceField = ((TileEntityForceField)tileEntity);
							TileEntityForceFieldProjector tileEntityForceFieldProjector = tileEntityForceField.getProjector();
							if (tileEntityForceFieldProjector == null) {
								// orphan force field, probably from an explosion
								// => recover it
								tileEntityForceField.setProjector(new VectorI(this));
								tileEntityForceField.cache_blockCamouflage = forceFieldSetup.getCamouflageBlock();
								tileEntityForceField.cache_metadataCamouflage = forceFieldSetup.getCamouflageMetadata();
								worldObj.setBlockMetadataWithNotify(vector.x, vector.y, vector.z, tileEntityForceField.cache_metadataCamouflage, 2);
								
							} else if (tileEntityForceFieldProjector == this) {// this is ours
								if ( tileEntityForceField.cache_blockCamouflage != forceFieldSetup.getCamouflageBlock()
								  || tileEntityForceField.cache_metadataCamouflage != forceFieldSetup.getCamouflageMetadata()
								  || block != WarpDrive.blockForceFields[tier - 1]
								  || vector.getBlockMetadata(worldObj) != metadataForceField ) {
									// camouflage changed while chunk wasn't loaded or de-synchronisation
									// force field downgraded during explosion
									// => force a new placement
									worldObj.setBlockToAir(vector.x, vector.y, vector.z);
									block = Blocks.air;
								}
							}
						}
					}
					
					doProjectThisBlock = block.isReplaceable(worldObj, vector.x, vector.y, vector.z) || (block == WarpDrive.blockForceFields[tier - 1]);
				}
			}
			
			// skip if area is protected
			if (doProjectThisBlock) {
				if (forceFieldSetup.breaking_maxHardness > 0) {
					doProjectThisBlock = ! isBlockBreakCanceled(null, worldObj, vector.x, vector.y, vector.z);
				} else if (!(block instanceof BlockForceField)) {
					doProjectThisBlock = ! isBlockPlaceCanceled(null, worldObj, vector.x, vector.y, vector.z, WarpDrive.blockForceFields[tier - 1], metadataForceField);
				}
			}
			
			if (doProjectThisBlock) {
				if ((block != WarpDrive.blockForceFields[tier - 1]) && (!vector.equals(this))) {
					boolean hasPlaced = false;
					if (block instanceof BlockLiquid) {
						hasPlaced = true;
						doPumping(forceFieldSetup, metadataForceField, vector, block);
						
					} else if (forceFieldSetup.breaking_maxHardness > 0) {
						hasPlaced = true;
						if (doBreaking(forceFieldSetup, vector, block)) {
							return;
						}
						
					} else if (forceFieldSetup.hasStabilize) {
						hasPlaced = true;
						if (doStabilize(forceFieldSetup, vector)) {
							return;
						}
						
					} else if (forceFieldSetup.isInverted && (forceFieldSetup.temperatureLevel < 295.0F || forceFieldSetup.temperatureLevel > 305.0F)) {
						doTerraforming(forceFieldSetup, vector, block);
						
					} else if (!forceFieldSetup.isInverted) {
						hasPlaced = true;
						worldObj.setBlock(vector.x, vector.y, vector.z, WarpDrive.blockForceFields[tier - 1], metadataForceField, 2);
						
						TileEntity tileEntity = worldObj.getTileEntity(vector.x, vector.y, vector.z);
						if (tileEntity instanceof TileEntityForceField) {
							((TileEntityForceField) tileEntity).setProjector(new VectorI(this));
						}
						
						vForceFields.add(vector);
					}
					if (hasPlaced) {
						countPlaced++;
						consumeEnergy(forceFieldSetup.placeEnergyCost, false);
					} else {
						consumeEnergy(forceFieldSetup.scanEnergyCost, false);
					}
					
				} else {
					// scanning a valid position
					consumeEnergy(forceFieldSetup.scanEnergyCost, false);
					
					// recover forcefield blocks from recalculation or chunk loading
					if (block == WarpDrive.blockForceFields[tier - 1] && !vForceFields.contains(vector)) {
						TileEntity tileEntity = worldObj.getTileEntity(vector.x, vector.y, vector.z);
						if (tileEntity instanceof TileEntityForceField && (((TileEntityForceField) tileEntity).getProjector() == this)) {
							vForceFields.add(vector);
						}
					}
				}
				
			} else {
				// scanning an invalid position
				consumeEnergy(forceFieldSetup.scanEnergyCost, false);
				
				// remove our own force field block
				if (block == WarpDrive.blockForceFields[tier - 1]) {
					assert(block instanceof BlockForceField);
					if (((BlockForceField) block).getProjector(worldObj, vector.x, vector.y, vector.z) == this) {
						worldObj.setBlockToAir(vector.x, vector.y, vector.z);
						vForceFields.remove(vector);
					}
				}
			}
		}
	}
	
	private void doPumping(final ForceFieldSetup forceFieldSetup, final int metadataForceField, final VectorI vector, final Block block) {
		if (block instanceof BlockStaticLiquid) {// it's a source block
			// TODO collect fluid
		}
		
		if (forceFieldSetup.isInverted || forceFieldSetup.breaking_maxHardness > 0) {
			worldObj.setBlock(vector.x, vector.y, vector.z, Blocks.air, 0, 2);
		} else {
			worldObj.setBlock(vector.x, vector.y, vector.z, WarpDrive.blockForceFields[tier - 1], metadataForceField, 2);
			
			TileEntity tileEntity = worldObj.getTileEntity(vector.x, vector.y, vector.z);
			if (tileEntity instanceof TileEntityForceField) {
				((TileEntityForceField) tileEntity).setProjector(new VectorI(this));
			}
			
			vForceFields.add(vector);
		}
	}
	
	private boolean doStabilize(final ForceFieldSetup forceFieldSetup, final VectorI vector) {
		int slotIndex = 0;
		boolean found = false;
		int countItemBlocks = 0;
		ItemStack itemStack = null;
		Block blockToPlace = null;
		int metadataToPlace = -1;
		IInventory inventory = null;
		for (IInventory inventoryLoop : forceFieldSetup.inventories) {
			if (!found) {
				slotIndex = 0;
			}
			while (slotIndex < inventoryLoop.getSizeInventory() && !found) {
				itemStack = inventoryLoop.getStackInSlot(slotIndex);
				if (itemStack == null || itemStack.stackSize <= 0) {
					slotIndex++;
					continue;
				}
				blockToPlace = Block.getBlockFromItem(itemStack.getItem());
				if (blockToPlace == Blocks.air) {
					slotIndex++;
					continue;
				}
				countItemBlocks++;
				metadataToPlace = itemStack.getItem().getMetadata(itemStack.getItemDamage());
				if (metadataToPlace == 0 && itemStack.getItemDamage() != 0) {
					metadataToPlace = itemStack.getItemDamage();
				}
				if (WarpDriveConfig.LOGGING_FORCEFIELD) {
					WarpDrive.logger.info("Slot " + slotIndex + " as " + itemStack + " known as block " + blockToPlace + ":" + metadataToPlace);
				}
				
				if (!blockToPlace.canPlaceBlockAt(worldObj, vector.x, vector.y, vector.z)) {
					slotIndex++;
					continue;
				}
				// TODO place block using ItemBlock.place?
				
				found = true;
				inventory = inventoryLoop;
			}
		}
		
		// no ItemBlocks found at all
		if (countItemBlocks <= 0) {
			// skip the next scans...
			return true;
		}
		
		if (inventory == null) {
			if (WarpDriveConfig.LOGGING_FORCEFIELD) {
				WarpDrive.logger.debug("No item to place found");
			}
			// skip the next scans...
			return true;
		}
		//noinspection ConstantConditions
		assert(found);
		
		// check area protection
		if (isBlockPlaceCanceled(null, worldObj, vector.x, vector.y, vector.z, blockToPlace, metadataToPlace)) {
			if (WarpDriveConfig.LOGGING_FORCEFIELD) {
				WarpDrive.logger.info(this + " Placing cancelled at (" + vector.x + " " + vector.y + " " + vector.z + ")");
			}
			// skip the next scans...
			return true;
		}
		
		itemStack.stackSize--;
		if (itemStack.stackSize <= 0) {
			itemStack = null;
		}
		inventory.setInventorySlotContents(slotIndex, itemStack);
		
		int age = Math.max(10, Math.round((4 + worldObj.rand.nextFloat()) * WarpDriveConfig.MINING_LASER_MINE_DELAY_TICKS));
		PacketHandler.sendBeamPacket(worldObj, new Vector3(this).translate(0.5D), new Vector3(vector.x, vector.y, vector.z).translate(0.5D),
			0.2F, 0.7F, 0.4F, age, 0, 50);
		// worldObj.playSoundEffect(xCoord + 0.5f, yCoord, zCoord + 0.5f, "warpdrive:lowlaser", 4F, 1F);
		
		// standard place sound effect
		worldObj.playSoundEffect(vector.x + 0.5F, vector.y + 0.5F, vector.z + 0.5F,
			blockToPlace.stepSound.func_150496_b(), (blockToPlace.stepSound.getVolume() + 1.0F) / 2.0F, blockToPlace.stepSound.getPitch() * 0.8F);
		
		worldObj.setBlock(vector.x, vector.y, vector.z, blockToPlace, metadataToPlace, 3);
		return false;
	}
	
	private void doTerraforming(final ForceFieldSetup forceFieldSetup, final VectorI vector, final Block block) {
		assert(vector != null);
		assert(block != null);
		if (forceFieldSetup.temperatureLevel > 300.0F) {
			
		} else {
			
		}
		// TODO glass <> sandstone <> sand <> gravel <> cobblestone <> stone <> obsidian
		// TODO ice <> snow <> water <> air > fire
		// TODO obsidian < lava
	}
	
	private boolean doBreaking(final ForceFieldSetup forceFieldSetup, final VectorI vector, final Block block) {
		List<ItemStack> itemStacks;
		int metadata = 0;
		try {
			metadata = worldObj.getBlockMetadata(vector.x, vector.y, vector.z);
			itemStacks = block.getDrops(worldObj, vector.x, vector.y, vector.z, metadata, 0);
		} catch (Exception exception) {// protect in case the mined block is corrupted
			exception.printStackTrace();
			itemStacks = null;
		}
		
		if (itemStacks != null) {
			if (forceFieldSetup.hasCollection) {
				if (addToInventories(itemStacks, forceFieldSetup.inventories)) {
					return true;
				}
			} else {
				for (ItemStack itemStackDrop : itemStacks) {
					ItemStack drop = itemStackDrop.copy();
					EntityItem entityItem = new EntityItem(worldObj, vector.x + 0.5D, vector.y + 1.0D, vector.z + 0.5D, drop);
					worldObj.spawnEntityInWorld(entityItem);
				}
			}
		}
		int age = Math.max(10, Math.round((4 + worldObj.rand.nextFloat()) * WarpDriveConfig.MINING_LASER_MINE_DELAY_TICKS));
		PacketHandler.sendBeamPacket(worldObj, new Vector3(vector.x, vector.y, vector.z).translate(0.5D), new Vector3(this).translate(0.5D),
			0.7F, 0.4F, 0.2F, age, 0, 50);
		// standard harvest block effect
		worldObj.playAuxSFXAtEntity(null, 2001, vector.x, vector.y, vector.z, Block.getIdFromBlock(block) + (metadata << 12));
		worldObj.setBlockToAir(vector.x, vector.y, vector.z);
		return false;
	}
	
	private void destroyForceField(boolean isChunkLoading) {
		if (worldObj == null || worldObj.isRemote) {
			return;
		}
		
		legacy_isOn = false;
		if (!vForceFields.isEmpty()) {
			for (Iterator<VectorI> iterator = vForceFields.iterator(); iterator.hasNext();) {
				VectorI vector = iterator.next();
				if (!isChunkLoading) {
					if (!(worldObj.blockExists(vector.x, vector.y, vector.z))) {// chunk is not loaded, skip it
						continue;
					}
					if (!worldObj.getChunkFromBlockCoords(vector.x, vector.z).isChunkLoaded) {// chunk is unloading, skip it
						continue;
					}
				}
				Block block = vector.getBlock(worldObj);
				
				if (block == WarpDrive.blockForceFields[tier - 1]) {
					worldObj.setBlockToAir(vector.x, vector.y, vector.z);
				}
				iterator.remove();
			}
		}
		
		if (isCalculated() && isChunkLoading) {
			for (VectorI vector : calculated_forceField) {
				Block block = vector.getBlock(worldObj);
				
				if (block == WarpDrive.blockForceFields[tier - 1]) {
					TileEntity tileEntity = worldObj.getTileEntity(vector.x, vector.y, vector.z);
					if (tileEntity instanceof TileEntityForceField && (((TileEntityForceField) tileEntity).getProjector() == this)) {
						worldObj.setBlockToAir(vector.x, vector.y, vector.z);
					}
				}
			}
		}
	}
	
	public IForceFieldShape getShapeProvider() {
		return getShape();
	}
	
	@Override
	public int getBeamFrequency() {
		return beamFrequency;
	}
	
	@Override
	public void setBeamFrequency(final int parBeamFrequency) {
		super.setBeamFrequency(parBeamFrequency);
		cache_forceFieldSetup = null;
		isDirty.set(true);
		destroyForceField(false);
	}
	
	public Vector3 getMin() {
		return v3Min;
	}
	
	private void setMin(final float x, final float y, final float z) {
		v3Min = new Vector3(Commons.clamp(-1.0D, 0.0D, x), Commons.clamp(-1.0D, 0.0D, y), Commons.clamp(-1.0D, 0.0D, z));
	}
	
	public Vector3 getMax() {
		return v3Max;
	}
	
	private void setMax(final float x, final float y, final float z) {
		v3Max = new Vector3(Commons.clamp(0.0D, 1.0D, x), Commons.clamp(0.0D, 1.0D, y), Commons.clamp(0.0D, 1.0D, z));
	}
	
	public float getRotationYaw() {
		int metadata = getBlockMetadata();
		float totalYaw;
		switch (ForgeDirection.getOrientation(metadata & 7)) {
		case DOWN : totalYaw =   0.0F; break;
		case UP   : totalYaw =   0.0F; break;
		case NORTH: totalYaw =  90.0F; break;
		case SOUTH: totalYaw = 270.0F; break;
		case WEST : totalYaw =   0.0F; break;
		case EAST : totalYaw = 180.0F; break;
		default   : totalYaw =   0.0F; break;
		}
		if (hasUpgrade(EnumForceFieldUpgrade.ROTATION)) {
			totalYaw += rotationYaw;
		}
		return (totalYaw + 540.0F) % 360.0F - 180.0F; 
	}
	
	public float getRotationPitch() {
		int metadata = getBlockMetadata();
		float totalPitch;
		switch (ForgeDirection.getOrientation(metadata & 7)) {
		case DOWN : totalPitch =  180.0F; break;
		case UP   : totalPitch =    0.0F; break;
		case NORTH: totalPitch =  -90.0F; break;
		case SOUTH: totalPitch =  -90.0F; break;
		case WEST : totalPitch =  -90.0F; break;
		case EAST : totalPitch =  -90.0F; break;
		default   : totalPitch =    0.0F; break;
		}
		if (hasUpgrade(EnumForceFieldUpgrade.ROTATION)) {
			totalPitch += rotationPitch;
		}
		return (totalPitch + 540.0F) % 360.0F - 180.0F;
	}
	
	public float getRotationRoll() {
		if (hasUpgrade(EnumForceFieldUpgrade.ROTATION)) {
			return (rotationRoll + 540.0F) % 360.0F - 180.0F;
		} else {
			return 0.0F;
		}
	}
	
	private void setRotation(final float rotationYaw, final float rotationPitch, final float rotationRoll) {
		float oldYaw = this.rotationYaw;
		float oldPitch = this.rotationPitch;
		float oldRoll = this.rotationRoll;
		this.rotationYaw = Commons.clamp( -45.0F, +45.0F, rotationYaw);
		this.rotationPitch = Commons.clamp( -45.0F, +45.0F, rotationPitch);
		this.rotationRoll = (rotationRoll + 720.0F) % 360.0F - 180.0F;
		if (oldYaw != this.rotationYaw || oldPitch != this.rotationPitch || oldRoll != this.rotationRoll) {
			isDirty.set(true);
			destroyForceField(false);
			markDirty();
		}
	}
	
	public EnumForceFieldShape getShape() {
		if (shape == null) {
			return EnumForceFieldShape.NONE;
		}
		return shape;
	}
	
	void setShape(EnumForceFieldShape shape) {
		this.shape = shape;
		cache_forceFieldSetup = null;
		isDirty.set(true);
		markDirty();
		if (hasWorldObj()) {
			destroyForceField(false);
		}
	}
	
	public Vector3 getTranslation() {
		if (hasUpgrade(EnumForceFieldUpgrade.TRANSLATION)) {
			return v3Translation;
		} else {
			return new Vector3(0.0D, 0.0D, 0.0D);
		}
	}
	
	private void setTranslation(final float x, final float y, final float z) {
		v3Translation = new Vector3(Commons.clamp(-1.0D, 1.0D, x), Commons.clamp(-1.0D, 1.0D, y), Commons.clamp(-1.0D, 1.0D, z));
	}
	
	@Override
	public boolean mountUpgrade(Object upgrade) {
		if  (super.mountUpgrade(upgrade)) {
			cache_forceFieldSetup = null;
			isDirty.set(true);
			destroyForceField(false);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean dismountUpgrade(Object upgrade) {
		if (super.dismountUpgrade(upgrade)) {
			cache_forceFieldSetup = null;
			isDirty.set(true);
			destroyForceField(false);
			return true;
		}
		return false;
	}
	
	private String getShapeStatus() {
		EnumForceFieldShape enumForceFieldShape = getShape();
		String strDisplayName = StatCollector.translateToLocalFormatted("warpdrive.forcefield.shape.statusLine." + enumForceFieldShape.unlocalizedName);
		if (enumForceFieldShape == EnumForceFieldShape.NONE) {
			return StatCollector.translateToLocalFormatted("warpdrive.forcefield.shape.statusLine.none", 
				strDisplayName);
		} else if (isDoubleSided) {
			return StatCollector.translateToLocalFormatted("warpdrive.forcefield.shape.statusLine.double",
				strDisplayName);
		} else {
			return StatCollector.translateToLocalFormatted("warpdrive.forcefield.shape.statusLine.single", 
				strDisplayName);
		}
	}
	
	@Override
	public String getStatus() {
		return super.getStatus()
			+ "\n" + getShapeStatus()
			+ "\n" + getUpgradeStatus();
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		isDoubleSided = tagCompound.getBoolean("isDoubleSided");
		
		if (tagCompound.hasKey("minX")) {
			setMin(tagCompound.getFloat("minX"), tagCompound.getFloat("minY"), tagCompound.getFloat("minZ"));
		} else {
			setMin(-1.0F, -1.0F, -1.0F);
		}
		if (tagCompound.hasKey("maxX")) {
			setMax(tagCompound.getFloat("maxX"), tagCompound.getFloat("maxY"), tagCompound.getFloat("maxZ"));
		} else {
			setMax(1.0F, 1.0F, 1.0F);
		}
		
		setRotation(tagCompound.getFloat("rotationYaw"), tagCompound.getFloat("rotationPitch"), tagCompound.getFloat("rotationRoll"));
		
		setShape(EnumForceFieldShape.get(tagCompound.getByte("shape")));
		
		setTranslation(tagCompound.getFloat("translationX"), tagCompound.getFloat("translationY"), tagCompound.getFloat("translationZ"));
		
		legacy_isOn = tagCompound.getBoolean("isOn");
		
		isPowered = tagCompound.getBoolean("isPowered");
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
		tagCompound.setBoolean("isDoubleSided", isDoubleSided);
		
		if (v3Min.x != -1.0D || v3Min.y != -1.0D || v3Min.z != -1.0D) {
			tagCompound.setFloat("minX", (float)v3Min.x);
			tagCompound.setFloat("minY", (float)v3Min.y);
			tagCompound.setFloat("minZ", (float)v3Min.z);
		}
		if (v3Max.x !=  1.0D || v3Max.y !=  1.0D || v3Max.z !=  1.0D) {
			tagCompound.setFloat("maxX", (float)v3Max.x);
			tagCompound.setFloat("maxY", (float)v3Max.y);
			tagCompound.setFloat("maxZ", (float)v3Max.z);
		}
		
		if (rotationYaw != 0.0F) {
			tagCompound.setFloat("rotationYaw", rotationYaw);
		}
		if (rotationPitch != 0.0F) {
			tagCompound.setFloat("rotationPitch", rotationPitch);
		}
		if (rotationRoll != 0.0F) {
			tagCompound.setFloat("rotationRoll", rotationRoll);
		}
		
		tagCompound.setByte("shape", (byte) getShape().ordinal());
		
		if (v3Translation.x !=  0.0D || v3Translation.y !=  0.0D || v3Translation.z !=  0.0D) {
			tagCompound.setFloat("translationX", (float)v3Translation.x);
			tagCompound.setFloat("translationY", (float)v3Translation.y);
			tagCompound.setFloat("translationZ", (float)v3Translation.z);
		}
		
		tagCompound.setBoolean("isOn", legacy_isOn);
		
		tagCompound.setBoolean("isPowered", isPowered);
	}
	
	@Override
	public Packet getDescriptionPacket() {
		final NBTTagCompound tagCompound = new NBTTagCompound();
		writeToNBT(tagCompound);
		return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tagCompound);
	}
	
	@Override
	public void onDataPacket(NetworkManager networkManager, S35PacketUpdateTileEntity packet) {
		final NBTTagCompound tagCompound = packet.func_148857_g();
		readFromNBT(tagCompound);
	}
	
	public ForceFieldSetup getForceFieldSetup() {
		if (cache_forceFieldSetup == null) {
			cache_forceFieldSetup = new ForceFieldSetup(worldObj.provider.dimensionId, xCoord, yCoord, zCoord, tier, beamFrequency);
			setupTicks = Math.max(setupTicks, 10);
			
			// reset field in case of major changes
			if (legacy_forceFieldSetup != null) {
				int energyRequired = (int)Math.max(0, Math.round(cache_forceFieldSetup.startupEnergyCost - legacy_forceFieldSetup.startupEnergyCost));
				if ( legacy_forceFieldSetup.getCamouflageBlock() != cache_forceFieldSetup.getCamouflageBlock()
				  || legacy_forceFieldSetup.getCamouflageMetadata() != cache_forceFieldSetup.getCamouflageMetadata()
				  || legacy_forceFieldSetup.beamFrequency != cache_forceFieldSetup.beamFrequency
				  || !energy_consume(energyRequired, false)) {
					if (WarpDriveConfig.LOGGING_FORCEFIELD) {
						WarpDrive.logger.info(this + " rebooting with new rendering...");
					}
					destroyForceField(true);
					
				} else if ( legacy_forceFieldSetup.isInverted != cache_forceFieldSetup.isInverted
				         || legacy_forceFieldSetup.shapeProvider != cache_forceFieldSetup.shapeProvider
				         || legacy_forceFieldSetup.thickness != cache_forceFieldSetup.thickness
				         || !legacy_forceFieldSetup.vMin.equals(cache_forceFieldSetup.vMin)
				         || !legacy_forceFieldSetup.vMax.equals(cache_forceFieldSetup.vMax)
				         || !legacy_forceFieldSetup.vTranslation.equals(cache_forceFieldSetup.vTranslation)
					     || (legacy_forceFieldSetup.breaking_maxHardness <= 0 && cache_forceFieldSetup.breaking_maxHardness > 0) ) {
					if (WarpDriveConfig.LOGGING_FORCEFIELD) {
						WarpDrive.logger.info(this + " rebooting with new shape...");
					}
					destroyForceField(true);
					isDirty.set(true);
				}
			}
		}
		return cache_forceFieldSetup;
	}
	
	@Override
	public int energy_getMaxStorage() {
		return maxEnergyStored;
	}
	
	@Override
	public boolean energy_canInput(ForgeDirection from) {
		return true;
	}
	
	public boolean consumeEnergy(final double amount_internal, boolean simulate) {
		int intAmount = (int)Math.floor(amount_internal + consumptionLeftOver);
		boolean bResult = super.energy_consume(intAmount, simulate); 
		if (!simulate) {
			consumptionLeftOver = amount_internal + consumptionLeftOver - intAmount;
		}
		return bResult;
	}
	
	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] state(Context context, Arguments arguments) {
		return state();
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] min(Context context, Arguments arguments) {
		if (arguments.count() == 1) {
			setMin((float)arguments.checkDouble(0), (float)arguments.checkDouble(0), (float)arguments.checkDouble(0));
		} else if (arguments.count() == 2) {
			setMin((float)arguments.checkDouble(0), (float)arguments.checkDouble(1), (float)arguments.checkDouble(0));
		} else if (arguments.count() == 3) {
			setMin((float)arguments.checkDouble(0), (float)arguments.checkDouble(1), (float)arguments.checkDouble(2));
		}
		return new Double[] { v3Min.x, v3Min.y, v3Min.z };
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] max(Context context, Arguments arguments) {
		if (arguments.count() == 1) {
			setMax((float)arguments.checkDouble(0), (float)arguments.checkDouble(0), (float)arguments.checkDouble(0));
		} else if (arguments.count() == 2) {
			setMax((float)arguments.checkDouble(0), (float)arguments.checkDouble(1), (float)arguments.checkDouble(0));
		} else if (arguments.count() == 3) {
			setMax((float)arguments.checkDouble(0), (float)arguments.checkDouble(1), (float)arguments.checkDouble(2));
		}
		return new Double[] { v3Max.x, v3Max.y, v3Max.z };
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] rotation(Context context, Arguments arguments) {
		if (arguments.count() == 1) {
			setRotation((float)arguments.checkDouble(0), rotationPitch, rotationRoll);
		} else if (arguments.count() == 2) {
			setRotation((float)arguments.checkDouble(0), (float)arguments.checkDouble(1), rotationRoll);
		} else if (arguments.count() == 3) {
			setRotation((float)arguments.checkDouble(0), (float)arguments.checkDouble(1), (float)arguments.checkDouble(2));
		}
		return new Float[] { rotationYaw, rotationPitch, rotationRoll };
	}
	
	// Common OC/CC methods
	private Object[] state() {    // isConnected, isPowered, shape
		final int energy = energy_getEnergyStored();
		final String status = getStatusHeaderInPureText();
		return new Object[] { status, isEnabled, isConnected, isPowered, getShape().name(), energy };
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] translation(Context context, Arguments arguments) {
		if (arguments.count() == 1) {
			setTranslation((float)arguments.checkDouble(0), (float)arguments.checkDouble(0), (float)arguments.checkDouble(0));
		} else if (arguments.count() == 2) {
			setTranslation((float)arguments.checkDouble(0), (float)arguments.checkDouble(1), (float)arguments.checkDouble(0));
		} else if (arguments.count() == 3) {
			setTranslation((float)arguments.checkDouble(0), (float)arguments.checkDouble(1), (float)arguments.checkDouble(2));
		}
		return new Double[] { v3Translation.x, v3Translation.y, v3Translation.z };
	}
	
	// ComputerCraft IPeripheral methods implementation
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) {
		final String methodName = getMethodName(method);
		
		switch (methodName) {
		case "min":
			if (arguments.length == 1) {
				setMin(Commons.toFloat(arguments[0]), Commons.toFloat(arguments[0]), Commons.toFloat(arguments[0]));
			} else if (arguments.length == 2) {
				setMin(Commons.toFloat(arguments[0]), Commons.toFloat(arguments[1]), Commons.toFloat(arguments[0]));
			} else if (arguments.length == 3) {
				setMin(Commons.toFloat(arguments[0]), Commons.toFloat(arguments[1]), Commons.toFloat(arguments[2]));
			}
			return new Double[] { v3Min.x, v3Min.y, v3Min.z };
		
		case "max":
			if (arguments.length == 1) {
				setMax(Commons.toFloat(arguments[0]), Commons.toFloat(arguments[0]), Commons.toFloat(arguments[0]));
			} else if (arguments.length == 2) {
				setMax(Commons.toFloat(arguments[0]), Commons.toFloat(arguments[1]), Commons.toFloat(arguments[0]));
			} else if (arguments.length == 3) {
				setMax(Commons.toFloat(arguments[0]), Commons.toFloat(arguments[1]), Commons.toFloat(arguments[2]));
			}
			return new Double[] { v3Max.x, v3Max.y, v3Max.z };
		
		case "rotation":
			if (arguments.length == 1) {
				setRotation(Commons.toFloat(arguments[0]), rotationPitch, rotationRoll);
			} else if (arguments.length == 2) {
				setRotation(Commons.toFloat(arguments[0]), Commons.toFloat(arguments[1]), rotationRoll);
			} else if (arguments.length == 3) {
				setRotation(Commons.toFloat(arguments[0]), Commons.toFloat(arguments[1]), Commons.toFloat(arguments[2]));
			}
			return new Float[] { rotationYaw, rotationPitch, rotationRoll };
		
		case "state":
			return state();
		
		case "translation":
			if (arguments.length == 1) {
				setTranslation(Commons.toFloat(arguments[0]), Commons.toFloat(arguments[0]), Commons.toFloat(arguments[0]));
			} else if (arguments.length == 2) {
				setTranslation(Commons.toFloat(arguments[0]), Commons.toFloat(arguments[1]), Commons.toFloat(arguments[0]));
			} else if (arguments.length == 3) {
				setTranslation(Commons.toFloat(arguments[0]), Commons.toFloat(arguments[1]), Commons.toFloat(arguments[2]));
			}
			return new Double[] { v3Translation.x, v3Translation.y, v3Translation.z };
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
	
	private class ThreadCalculation extends Thread {
		private final TileEntityForceFieldProjector projector;
		
		ThreadCalculation(TileEntityForceFieldProjector projector) {
			this.projector = projector;
		}
		
		@Override
		public void run() {
			Set<VectorI> vPerimeterBlocks = null;
			Set<VectorI> vInteriorBlocks = null;
			
			// calculation start is done synchronously, by caller
			try {
				if (projector.isValid()) {
					ForceFieldSetup forceFieldSetup = projector.getForceFieldSetup();
					if (WarpDriveConfig.LOGGING_FORCEFIELD) {
						WarpDrive.logger.debug(this + " Calculation started for " + projector);
					}
					
					// create HashSets
					VectorI vScale = forceFieldSetup.vMax.clone().subtract(forceFieldSetup.vMin);
					vInteriorBlocks = new HashSet<>(vScale.x * vScale.y * vScale.z);
					vPerimeterBlocks = new HashSet<>(2 * vScale.x * vScale.y + 2 * vScale.x * vScale.z + 2 * vScale.y * vScale.z);
					
					// compute interior fields to remove overlapping parts
					for (Map.Entry<VectorI, Boolean> entry : forceFieldSetup.shapeProvider.getVertexes(forceFieldSetup).entrySet()) {
						VectorI vPosition = entry.getKey();
						if (forceFieldSetup.isDoubleSided || vPosition.y >= 0) {
							if ((forceFieldSetup.rotationYaw != 0.0F) || (forceFieldSetup.rotationPitch != 0.0F) || (forceFieldSetup.rotationRoll != 0.0F)) {
								vPosition.rotateByAngle(forceFieldSetup.rotationYaw, forceFieldSetup.rotationPitch, forceFieldSetup.rotationRoll);
							}
							
							vPosition.translate(forceFieldSetup.vTranslation);
							
							if (vPosition.y > 0 && vPosition.y <= projector.worldObj.getHeight()) {
								if (entry.getValue()) {
									vPerimeterBlocks.add(vPosition);
								} else {
									vInteriorBlocks.add(vPosition);
								}
							}
						}
					}
					
					// compute forcefield itself
					if (forceFieldSetup.isInverted) {
						// inverted mode => same as interior before fusion => need to be fully cloned
						vPerimeterBlocks = new HashSet<>(vInteriorBlocks);
					}
					
					if (WarpDriveConfig.LOGGING_FORCEFIELD) {
						WarpDrive.logger.debug(this + " Calculation done: "
							+ vInteriorBlocks.size() + " blocks inside, including " + vPerimeterBlocks.size() + " blocks to place");
					}
				} else {
					if (WarpDriveConfig.LOGGING_FORCEFIELD) {
						WarpDrive.logger.error(this + " Calculation aborted");
					}
				}
			} catch (Exception exception) {
				vInteriorBlocks = null;
				vPerimeterBlocks = null;
				exception.printStackTrace();
				WarpDrive.logger.error(this + " Calculation failed");
			}
			
			projector.calculation_done(vInteriorBlocks, vPerimeterBlocks);
		}
	}
}
