package cr0s.warpdrive.block.forcefield;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBeamFrequency;
import cr0s.warpdrive.api.IForceFieldShape;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnumForceFieldShape;
import cr0s.warpdrive.data.EnumForceFieldUpgrade;
import cr0s.warpdrive.data.FluidWrapper;
import cr0s.warpdrive.data.ForceFieldSetup;
import cr0s.warpdrive.data.InventoryWrapper;
import cr0s.warpdrive.data.SoundEvents;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.data.VectorI;
import cr0s.warpdrive.network.PacketHandler;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.common.Optional;

public class TileEntityForceFieldProjector extends TileEntityAbstractForceField {
	
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
	private boolean isActive = false;
	
	// computed properties
	private int cooldownTicks;
	private int setupTicks;
	private int updateTicks;
	private int soundTicks;
	private int guideTicks;
	private double damagesEnergyCost = 0.0D;
	private final HashSet<UUID> setInteractedEntities = new HashSet<>();
	private ForceFieldSetup cache_forceFieldSetup;
	private ForceFieldSetup legacy_forceFieldSetup;
	private double consumptionLeftOver = 0.0D;
	public EnumFacing enumFacing = EnumFacing.UP;
	
	// carry over speed to next tick, useful for slow interactions
	private float carryScanSpeed;
	private float carryPlaceSpeed;
	
	private Set<VectorI> calculated_interiorField = null;
	private Set<VectorI> calculated_forceField = null;
	private Iterator<VectorI> iteratorForceField = null;
	
	// currently placed force field blocks
	private final Set<VectorI> vForceFields = new HashSet<>();
	
	// schedule removal/destruction in main thread
	final CopyOnWriteArraySet<VectorI> vForceFields_forRemoval = new CopyOnWriteArraySet<>();
	
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
		doRequireUpgradeToInterface();
		
		for (final EnumForceFieldUpgrade enumForceFieldUpgrade : EnumForceFieldUpgrade.values()) {
			if (enumForceFieldUpgrade.maxCountOnProjector > 0) {
				registerUpgradeSlot(enumForceFieldUpgrade.getProjectorUpgradeSlot());
			}
		}
	}
	
	@Override
	protected void onConstructed() {
		super.onConstructed();
		
		energy_setParameters(WarpDriveConfig.FORCE_FIELD_PROJECTOR_MAX_ENERGY_STORED_BY_TIER[enumTier.getIndex()], 4096, 0,
		                     "EV", 2, "EV", 0);
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
		
		cooldownTicks = 0;
		setupTicks = world.rand.nextInt(PROJECTOR_SETUP_TICKS);
		updateTicks = world.rand.nextInt(PROJECTOR_PROJECTION_UPDATE_TICKS);
		guideTicks = PROJECTOR_GUIDE_UPDATE_TICKS;
		
		// recover is_double_sided from blockstate property
		final IBlockState blockState = world.getBlockState(pos);
		if (isInvalidBlockState(blockState, BlockForceFieldProjector.class, BlockProperties.FACING, BlockForceFieldProjector.IS_DOUBLE_SIDED)) {
			return;
		}
		enumFacing = blockState.getValue(BlockProperties.FACING);
		if (blockState.getValue(BlockForceFieldProjector.IS_DOUBLE_SIDED)) {
			isDoubleSided = true;
		} else if (isDoubleSided) {
			// up to 1.5.13, blockstate wasn't set properly so we resynchronize here
			world.setBlockState(pos, blockState.withProperty(BlockForceFieldProjector.IS_DOUBLE_SIDED, true));
		}
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
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
		
		// remove from main thread
		doScheduledForceFieldRemoval();
		
		// resolve interactions
		final ForceFieldSetup forceFieldSetup = getForceFieldSetup();
		final int countEntityInteractions = setInteractedEntities.size();
		if (countEntityInteractions > 0) {
			setInteractedEntities.clear();
			consumeEnergy(forceFieldSetup.getEntityEnergyCost(countEntityInteractions), false);
		}
		
		if (damagesEnergyCost > 0.0D) {
			if (WarpDriveConfig.LOGGING_WEAPON) {
				WarpDrive.logger.info(String.format("%s damages received, energy lost %.2f / %d",
				                                    this, damagesEnergyCost, energy_getEnergyStored() ));
			}
			consumeEnergy(damagesEnergyCost, false);
			damagesEnergyCost = 0.0D;
		}
		
		// Powered ?
		final int energyRequired;
		if (!isActive) {
			energyRequired = (int) Math.round(forceFieldSetup.startupEnergyCost + forceFieldSetup.placeEnergyCost * forceFieldSetup.placeSpeed * PROJECTOR_PROJECTION_UPDATE_TICKS / 20.0F);
		} else {
			energyRequired = (int) Math.round(                                    forceFieldSetup.scanEnergyCost * forceFieldSetup.scanSpeed * PROJECTOR_PROJECTION_UPDATE_TICKS / 20.0F);
		}
		if (energyRequired > energy_getMaxStorage()) {
			WarpDrive.logger.error(String.format("Force field projector requires %d to get started bu can only store %d",
			                                     energyRequired, energy_getMaxStorage()));
		}
		final boolean isPowered = energy_getEnergyStored() >= energyRequired;
		
		final boolean isEnabledAndValid = isEnabled && isAssemblyValid;
		final boolean new_isActive = isEnabledAndValid && cooldownTicks <= 0 && isPowered;
		if (new_isActive) {
			if (!isActive) {
				consumeEnergy(forceFieldSetup.startupEnergyCost, false);
				if (WarpDriveConfig.LOGGING_FORCE_FIELD) {
					WarpDrive.logger.info(String.format("%s starting up...", this));
				}
				isActive = true;
				markDirty();
			}
			cooldownTicks = 0;
			
			updateTicks--;
			if (updateTicks <= 0) {
				updateTicks = PROJECTOR_PROJECTION_UPDATE_TICKS;
				if (!isCalculated()) {
					if (forceFieldSetup.shapeProvider != EnumForceFieldShape.NONE) {
						calculation_start();
					}
				} else {
					projectForceField();
				}
			}
			
			soundTicks--;
			if (soundTicks < 0) {
				soundTicks = PROJECTOR_SOUND_UPDATE_TICKS;
				assert EnumForceFieldUpgrade.SILENCER.getProjectorUpgradeSlot() != null;
				if (!hasUpgrade(EnumForceFieldUpgrade.SILENCER.getProjectorUpgradeSlot())) {
					world.playSound(null, pos, SoundEvents.PROJECTING, SoundCategory.BLOCKS, 1.0F, 0.85F + 0.15F * world.rand.nextFloat());
				}
			}
			
		} else {
			if (isActive) {
				if (WarpDriveConfig.LOGGING_FORCE_FIELD) {
					WarpDrive.logger.info(String.format("%s shutting down...", this));
				}
				isActive = false;
				markDirty();
				cooldownTicks = PROJECTOR_COOLDOWN_TICKS;
				guideTicks = 0;
				
				destroyForceField("projector deactivation");
			}
			
			if (isEnabledAndValid) {
				if (guideTicks <= 0) {
					guideTicks = PROJECTOR_GUIDE_UPDATE_TICKS;
					
					final WarpDriveText msg = Commons.getChatPrefix(getBlockType());
					msg.appendSibling(new WarpDriveText(Commons.getStyleWarning(), "warpdrive.force_field.guide.low_power"));
					
					final AxisAlignedBB axisalignedbb = new AxisAlignedBB(pos.getX() - 10, pos.getY() - 10, pos.getZ() - 10, pos.getX() + 10, pos.getY() + 10, pos.getZ() + 10);
					final List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(null, axisalignedbb);
					
					for (final Entity entity : list) {
						if ( (!(entity instanceof EntityPlayer))
						  || entity instanceof FakePlayer ) {
							continue;
						}
						
						Commons.addChatMessage(entity, msg);
					}
				}
			}
		}
	}
	
	@Override
	public void onBlockBroken(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState) {
		destroyForceField("projector block broken");
		try {
			doScheduledForceFieldRemoval();
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
		}
		super.onBlockBroken(world, blockPos, blockState);
	}
	
	@Override
	protected boolean doScanAssembly(final boolean isDirty, final WarpDriveText textReason) {
		final boolean isValid = super.doScanAssembly(isDirty, textReason);
		
		if (getShape() == EnumForceFieldShape.NONE) {
			textReason.append(Commons.getStyleWarning(), "warpdrive.force_field.shape.status_line.none");
			return false;
		}
		
		return isValid;
	}
	
	@Override
	protected boolean calculation_start() {
		final boolean isStarting = super.calculation_start();
		if (isStarting) {
			if (WarpDriveConfig.LOGGING_FORCE_FIELD) {
				WarpDrive.logger.info(String.format("Calculation initiated for %s",
				                                    this));
			}
			iteratorForceField = null;
			calculated_interiorField = null;
			calculated_forceField = null;
			vForceFields.clear();
			
			new ThreadCalculation(this).start();
		}
		return isStarting;
	}
	
	private void calculation_done(final Set<VectorI> interiorField, final Set<VectorI> forceField) {
		if (WarpDriveConfig.LOGGING_FORCE_FIELD) {
			WarpDrive.logger.info(String.format("Calculation done for %s",
			                                    this));
		}
		if (interiorField == null || forceField == null) {
			calculated_interiorField = new HashSet<>(0);
			calculated_forceField = new HashSet<>(0);
		} else {
			calculated_interiorField = interiorField;
			calculated_forceField = forceField;
		}
		calculation_done();
	}
	
	boolean isActive() {
		return isActive;
	}
	
	boolean isPartOfForceField(final VectorI vector) {
		if ( vForceFields_forRemoval.isEmpty()
		  && ( !isEnabled
		    || !isAssemblyValid ) ) {
			return false;
		}
		if (!isCalculated()) {
			return true;
		}
		// only consider the force field itself
		return calculated_forceField.contains(vector);
	}
	
	private boolean isPartOfInterior(final VectorI vector) {
		if (!isEnabled || !isAssemblyValid) {
			return false;
		}
		if (!isCalculated()) {
			return false;
		}
		// only consider the force field interior
		return calculated_interiorField.contains(vector);
	}
	
	public boolean onEntityInteracted(final UUID uniqueID) {
		return setInteractedEntities.add(uniqueID);
	}
	
	public void onEnergyDamage(final double energyCost) {
		damagesEnergyCost += energyCost;
	}
	
	private void projectForceField() {
		assert !world.isRemote && isCalculated();
		
		final ForceFieldSetup forceFieldSetup = getForceFieldSetup();
		
		// compute maximum number of blocks to scan
		int countScanned = 0;
		final float floatScanSpeed = Math.min(forceFieldSetup.scanSpeed * PROJECTOR_PROJECTION_UPDATE_TICKS / 20.0F + carryScanSpeed, calculated_forceField.size());
		final int countMaxScanned = (int)Math.floor(floatScanSpeed);
		carryScanSpeed = floatScanSpeed - countMaxScanned;
		
		// compute maximum number of blocks to place
		int countPlaced = 0;
		final float floatPlaceSpeed = Math.min(forceFieldSetup.placeSpeed * PROJECTOR_PROJECTION_UPDATE_TICKS / 20.0F + carryPlaceSpeed, calculated_forceField.size());
		final int countMaxPlaced = (int)Math.floor(floatPlaceSpeed);
		carryPlaceSpeed = floatPlaceSpeed - countMaxPlaced;
		
		// evaluate force field block metadata
		final IBlockState blockStateForceField;
		{
			final IBlockState blockStateCamouflage = forceFieldSetup.getCamouflageBlockState();
			final int metadata;
			if (blockStateCamouflage == null) {
				metadata = Math.min(15, (beamFrequency * 16) / IBeamFrequency.BEAM_FREQUENCY_MAX);
			} else {
				metadata = blockStateCamouflage.getBlock().getMetaFromState(blockStateCamouflage);
			}
			blockStateForceField = WarpDrive.blockForceFields[enumTier.getIndex()].getStateFromMeta(metadata);
		}
		
		VectorI vector;
		IBlockState blockState;
		boolean doProjectThisBlock;
		
		while ( countScanned < countMaxScanned
		     && countPlaced < countMaxPlaced
			 && consumeEnergy(Math.max(forceFieldSetup.scanEnergyCost, forceFieldSetup.placeEnergyCost), true)) {
			if (iteratorForceField == null || !iteratorForceField.hasNext()) {
				iteratorForceField = calculated_forceField.iterator();
			}
			countScanned++;
			
			vector = iteratorForceField.next();
			
			// skip non loaded chunks
			if ( !world.isBlockLoaded(vector.getBlockPos(), false)
			  || !world.getChunk(vector.getBlockPos()).isLoaded() ) {
				continue;
			}

			blockState = vector.getBlockState(world);
			doProjectThisBlock = true;
			
			// skip if fusion upgrade is present and it's inside another projector area
			if (forceFieldSetup.hasFusion) {
				for (final TileEntityForceFieldProjector projector : forceFieldSetup.projectors) {
					if (projector.isPartOfInterior(vector)) {
						doProjectThisBlock = false;
						break;
					}
				}
			}
			
			Fluid fluid = null;
			
			// skip if block properties prevents it
			if ( doProjectThisBlock
			  && (blockState.getBlock() != Blocks.TALLGRASS)
			  && (blockState.getBlock() != Blocks.DEADBUSH)
			  && !Dictionary.BLOCKS_EXPANDABLE.contains(blockState.getBlock()) ) {
				// MFR laser is unbreakable and replaceable
				// Liquid, vine and snow are replaceable
				final Block block = blockState.getBlock();
				fluid = FluidWrapper.getFluid(blockState);
				if (fluid != null) {
					if (WarpDriveConfig.LOGGING_FORCE_FIELD) {
						WarpDrive.logger.info(String.format("Block %s %s Fluid %s with viscosity %d, projector max is %.1f: %s %s",
						                                    block.getTranslationKey(),
						                                    blockState,
						                                    fluid.getName(),
						                                    fluid.getViscosity(),
						                                    forceFieldSetup.pumping_maxViscosity,
						                                    block, fluid));
					}
					doProjectThisBlock = forceFieldSetup.pumping_maxViscosity >= fluid.getViscosity();
					
				} else if (forceFieldSetup.breaking_maxHardness > 0) {
					final float blockHardness = blockState.getBlockHardness(world, vector.getBlockPos());
					// stops on unbreakable or too hard
					if (blockHardness == -1.0F || blockHardness > forceFieldSetup.breaking_maxHardness || world.isAirBlock(vector.getBlockPos())) {
						doProjectThisBlock = false;
					}
					
				} else {// doesn't have disintegration, not a liquid
					
					// recover force field blocks
					if (blockState.getBlock() instanceof BlockForceField) {
						final TileEntity tileEntity = vector.getTileEntity(world);
						if (!(tileEntity instanceof TileEntityForceField)) {
							// missing a valid tile entity
							// => force a new placement
							world.setBlockToAir(vector.getBlockPos());
							blockState = Blocks.AIR.getDefaultState();
							
						} else {
							final TileEntityForceField tileEntityForceField = ((TileEntityForceField) tileEntity);
							final TileEntityForceFieldProjector tileEntityForceFieldProjector = tileEntityForceField.getProjector(this);
							if (tileEntityForceFieldProjector == null) {
								// orphan force field, probably from an explosion
								// => recover it
								tileEntityForceField.setProjector(pos);
								tileEntityForceField.cache_blockStateCamouflage = forceFieldSetup.getCamouflageBlockState();
								// world.setBlockState(vector.getBlockPos(), tileEntityForceField.cache_blockStateCamouflage, 2);
								
							} else if (tileEntityForceFieldProjector == this) {// this is ours
								if ( ( tileEntityForceField.cache_blockStateCamouflage == null
								    && forceFieldSetup.getCamouflageBlockState() != null )
								  || ( tileEntityForceField.cache_blockStateCamouflage != null
								    && !tileEntityForceField.cache_blockStateCamouflage.equals(forceFieldSetup.getCamouflageBlockState()) )
								  || !blockState.equals(blockStateForceField) ) {
									// camouflage changed while chunk wasn't loaded or de-synchronisation
									// force field downgraded during explosion
									// => force a new placement
									world.setBlockToAir(vector.getBlockPos());
									blockState = Blocks.AIR.getDefaultState();
								}
							}
						}
					}
					
					doProjectThisBlock = blockState.getBlock().isReplaceable(world, vector.getBlockPos())
					                  || blockState.getBlock() == WarpDrive.blockForceFields[enumTier.getIndex()];
				}
			}
			
			// skip if area is protected
			if (doProjectThisBlock) {
				if (forceFieldSetup.breaking_maxHardness > 0) {
					doProjectThisBlock = ! isBlockBreakCanceled(null, world, vector.getBlockPos());
				} else if (!(blockState.getBlock() instanceof BlockForceField)) {
					doProjectThisBlock = ! isBlockPlaceCanceled(null, world, vector.getBlockPos(), blockStateForceField);
				}
			}
			
			if (doProjectThisBlock) {
				if ((blockState.getBlock() != WarpDrive.blockForceFields[enumTier.getIndex()]) && (!vector.equals(this))) {
					boolean hasPlaced = false;
					if (fluid != null) {
						hasPlaced = true;
						doPumping(forceFieldSetup, blockStateForceField, vector, blockState, fluid);
						
					} else if (forceFieldSetup.breaking_maxHardness > 0) {
						hasPlaced = true;
						if (doBreaking(forceFieldSetup, vector, blockState)) {
							return;
						}
						
					} else if (forceFieldSetup.hasStabilize) {
						hasPlaced = true;
						if (doStabilize(forceFieldSetup, vector)) {
							return;
						}
						
					} else if (forceFieldSetup.isInverted && (forceFieldSetup.temperatureLevel < 295.0F || forceFieldSetup.temperatureLevel > 305.0F)) {
						doTerraforming(forceFieldSetup, vector, blockState);
						
					} else if (!forceFieldSetup.isInverted) {
						hasPlaced = true;
						world.setBlockState(vector.getBlockPos(), blockStateForceField, 2);
						
						final TileEntity tileEntity = world.getTileEntity(vector.getBlockPos());
						if (tileEntity instanceof TileEntityForceField) {
							((TileEntityForceField) tileEntity).setProjector(pos);
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
					if (blockState.getBlock() == WarpDrive.blockForceFields[enumTier.getIndex()] && !vForceFields.contains(vector)) {
						final TileEntity tileEntity = world.getTileEntity(vector.getBlockPos());
						if (tileEntity instanceof TileEntityForceField && (((TileEntityForceField) tileEntity).getProjector(this) == this)) {
							vForceFields.add(vector);
						}
					}
				}
				
			} else {
				// scanning an invalid position
				consumeEnergy(forceFieldSetup.scanEnergyCost, false);
				
				// remove our own force field block
				if (blockState.getBlock() == WarpDrive.blockForceFields[enumTier.getIndex()]) {
					assert blockState.getBlock() instanceof BlockForceField;
					if (((BlockForceField) blockState.getBlock()).getProjector(world, vector.getBlockPos(), this) == this) {
						world.setBlockToAir(vector.getBlockPos());
						vForceFields.remove(vector);
					}
				}
			}
		}
	}
	
	private void doPumping(final ForceFieldSetup forceFieldSetup, final IBlockState blockStateForceField, final VectorI vector,
	                       final IBlockState blockState,
	                       final Fluid fluid) {
		final Block block = blockState.getBlock();
		boolean isAlreadyRemoved = false;
		final boolean isSourceBlock = FluidWrapper.isSourceBlock(world, vector.getBlockPos(), blockState);
		if (isSourceBlock) {
			final FluidStack fluidStack;
			if (block instanceof IFluidBlock) {
				fluidStack = ((IFluidBlock) block).drain(world, vector.getBlockPos(), true);
				assert fluidStack != null;
				isAlreadyRemoved = true;
			} else {
				fluidStack = new FluidStack(fluid, 1000);
			}
			if (WarpDriveConfig.LOGGING_FORCE_FIELD) {
				WarpDrive.logger.info(String.format("Fluid source found! %s x %d mB",
				                                    fluidStack.getUnlocalizedName(), fluidStack.amount));
			}
			// @TODO store fluid using IFluidHandler
			
		} else {
			if (WarpDriveConfig.LOGGING_FORCE_FIELD) {
				WarpDrive.logger.info(String.format("Fluid flow found! %s", fluid.getUnlocalizedName()));
			}
		}
		
		if (forceFieldSetup.isInverted || forceFieldSetup.breaking_maxHardness > 0) {
			if (!isAlreadyRemoved) {
				world.setBlockState(vector.getBlockPos(), Blocks.AIR.getDefaultState(), 2);
			}
		} else {
			world.setBlockState(vector.getBlockPos(), blockStateForceField, 2);
			
			final TileEntity tileEntity = world.getTileEntity(vector.getBlockPos());
			if (tileEntity instanceof TileEntityForceField) {
				((TileEntityForceField) tileEntity).setProjector(pos);
			}
			
			vForceFields.add(vector);
		}
	}
	
	private boolean doStabilize(final ForceFieldSetup forceFieldSetup, final VectorI vector) {
		int slotIndex = 0;
		boolean found = false;
		int countItemBlocks = 0;
		Block blockToPlace = null;
		int metadataToPlace = -1;
		Object inventory = null;
		final BlockPos blockPos = vector.getBlockPos();
		for (final Object inventoryLoop : forceFieldSetup.inventories) {
			if (!found) {
				slotIndex = 0;
			}
			while (slotIndex < InventoryWrapper.getSize(inventoryLoop) && !found) {
				final ItemStack itemStack = InventoryWrapper.getStackInSlot(inventoryLoop, slotIndex);
				if (itemStack.isEmpty()) {
					slotIndex++;
					continue;
				}
				blockToPlace = Block.getBlockFromItem(itemStack.getItem());
				if (blockToPlace == Blocks.AIR) {
					slotIndex++;
					continue;
				}
				countItemBlocks++;
				metadataToPlace = itemStack.getItem().getMetadata(itemStack.getItemDamage());
				if (metadataToPlace == 0 && itemStack.getItemDamage() != 0) {
					metadataToPlace = itemStack.getItemDamage();
				}
				if (WarpDriveConfig.LOGGING_FORCE_FIELD) {
					WarpDrive.logger.info(String.format("Slot %d as %s known as block %s:%d",
					                                    slotIndex, itemStack, blockToPlace, metadataToPlace));
				}
				
				if (!blockToPlace.canPlaceBlockAt(world, blockPos)) {
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
			if (WarpDriveConfig.LOGGING_FORCE_FIELD) {
				WarpDrive.logger.debug("No item to place found");
			}
			// skip the next scans...
			return true;
		}
		//noinspection ConstantConditions
		assert found;
		
		// check area protection
		final IBlockState blockStateToPlace = blockToPlace.getStateFromMeta(metadataToPlace);
		if (isBlockPlaceCanceled(null, world, blockPos, blockStateToPlace)) {
			if (WarpDriveConfig.LOGGING_FORCE_FIELD) {
				WarpDrive.logger.info(String.format("%s Placing cancelled %s",
				                                    this, Commons.format(world, blockPos)));
			}
			// skip the next scans...
			return true;
		}
		
		InventoryWrapper.decrStackSize(inventory, slotIndex, 1);
		
		final int age = Math.max(10, Math.round((4 + world.rand.nextFloat()) * WarpDriveConfig.MINING_LASER_MINE_DELAY_TICKS));
		PacketHandler.sendBeamPacket(world, new Vector3(this).translate(0.5D), new Vector3(vector.x, vector.y, vector.z).translate(0.5D),
			0.2F, 0.7F, 0.4F, age, 0, 50);
		// world.playSound(null, pos, SoundEvents.LASER_LOW, SoundCategory.BLOCKS, 4.0F, 1.0F);
		
		// standard place sound effect
		final SoundType soundType = blockStateToPlace.getBlock().getSoundType(blockStateToPlace, world, blockPos, null);
		world.playSound(null, blockPos, soundType.getPlaceSound(), SoundCategory.BLOCKS,
				(soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
		
		world.setBlockState(blockPos, blockStateToPlace, 3);
		return false;
	}
	
	private void doTerraforming(final ForceFieldSetup forceFieldSetup, final VectorI vector, final IBlockState blockState) {
		assert vector != null;
		assert blockState != null;
		if (forceFieldSetup.temperatureLevel > 300.0F) {
			
		} else {
			
		}
		// TODO glass <> sandstone <> sand <> gravel <> cobblestone <> stone <> obsidian
		// TODO ice <> snow <> water <> air > fire
		// TODO obsidian < lava
	}
	
	private boolean doBreaking(final ForceFieldSetup forceFieldSetup, final VectorI vector, final IBlockState blockState) {
		List<ItemStack> itemStacks;
		try {
			itemStacks = blockState.getBlock().getDrops(world, vector.getBlockPos(), blockState, 0);
		} catch (final Exception exception) {// protect in case the mined block is corrupted
			exception.printStackTrace(WarpDrive.printStreamError);
			itemStacks = null;
		}
		
		if (itemStacks != null) {
			if (forceFieldSetup.hasCollection) {
				if (InventoryWrapper.addToInventories(world, pos, forceFieldSetup.inventories, itemStacks)) {
					return true;
				}
			} else {
				for (final ItemStack itemStackDrop : itemStacks) {
					final ItemStack drop = itemStackDrop.copy();
					final EntityItem entityItem = new EntityItem(world, vector.x + 0.5D, vector.y + 1.0D, vector.z + 0.5D, drop);
					world.spawnEntity(entityItem);
				}
			}
		}
		final int age = Math.max(10, Math.round((4 + world.rand.nextFloat()) * WarpDriveConfig.MINING_LASER_MINE_DELAY_TICKS));
		PacketHandler.sendBeamPacket(world, new Vector3(vector.x, vector.y, vector.z).translate(0.5D), new Vector3(this).translate(0.5D),
			0.7F, 0.4F, 0.2F, age, 0, 50);
		// standard harvest block effect
		world.playEvent(2001, vector.getBlockPos(), Block.getStateId(blockState));
		world.setBlockToAir(vector.getBlockPos());
		return false;
	}
	
	private void destroyForceField(final String context) {
		if (world == null || world.isRemote) {
			return;
		}
		
		final int countPlaced = vForceFields != null ? vForceFields.size() : 0;
		final int countCalculated = calculated_forceField != null ? calculated_forceField.size() : 0;
		if (countPlaced + countCalculated > 0) {
			WarpDrive.logger.info(String.format("%s destroying force field of %d placed out of %d calculated blocks due to %s",
			                                    this, countPlaced, countCalculated, context));
			if (WarpDriveConfig.LOGGING_FORCE_FIELD) {
				new RuntimeException().printStackTrace(WarpDrive.printStreamInfo);
			}
		}
		
		if (countPlaced > 0) {
			vForceFields_forRemoval.addAll(vForceFields);
			vForceFields.clear();
		}
		
		// add calculated blocks only once
		if ( isCalculated()
		  && vForceFields_forRemoval.size() < calculated_forceField.size() ) {
			vForceFields_forRemoval.addAll(calculated_forceField);
		}
		
		// force a reboot
		isActive = false;
		
		// invalidate() can be multi-threaded, so we're delaying the destruction
		if (Commons.isSafeThread()) {
			doScheduledForceFieldRemoval();
		}
	}
	
	private void doScheduledForceFieldRemoval() {
		if (!Commons.isSafeThread()) {
			WarpDrive.logger.warn("Removing force field blocks outside main thread, bad things may happen...");
		}
		final VectorI[] vForceFields_cache = vForceFields_forRemoval.toArray(new VectorI[0]);
		
		for (final VectorI vector : vForceFields_cache) {
			final BlockPos blockPos = vector.getBlockPos();
			final IBlockState blockState = world.getBlockState(blockPos);
			if (blockState.getBlock() == WarpDrive.blockForceFields[enumTier.getIndex()]) {
				final TileEntity tileEntity = world.getTileEntity(blockPos);
				if ( tileEntity instanceof TileEntityForceField
				  && (((TileEntityForceField) tileEntity).getProjector(this) == this) ) {
					world.setBlockToAir(vector.getBlockPos());
				}
			}
		}
		
		vForceFields_forRemoval.clear();
	}
	
	public IForceFieldShape getShapeProvider() {
		return getShape();
	}
	
	@Override
	public int getBeamFrequency() {
		return beamFrequency;
	}
	
	@Override
	public void setBeamFrequency(final int beamFrequency) {
		super.setBeamFrequency(beamFrequency);
		cache_forceFieldSetup = null;
	}
	
	public Vector3 getMin() {
		assert EnumForceFieldUpgrade.RANGE.getProjectorUpgradeSlot() != null;
		if (hasUpgrade(EnumForceFieldUpgrade.RANGE.getProjectorUpgradeSlot())) {
			return v3Min;
		} else {
			return new Vector3(-1.0D, -1.0D, -1.0D);
		}
	}
	
	private void setMin(final float x, final float y, final float z) {
		v3Min = new Vector3(Commons.clamp(-1.0D, 0.0D, x), Commons.clamp(-1.0D, 0.0D, y), Commons.clamp(-1.0D, 0.0D, z));
	}
	
	public Vector3 getMax() {
		assert EnumForceFieldUpgrade.RANGE.getProjectorUpgradeSlot() != null;
		if (hasUpgrade(EnumForceFieldUpgrade.RANGE.getProjectorUpgradeSlot())) {
			return v3Max;
		} else {
			return new Vector3( 1.0D,  1.0D,  1.0D);
		}
	}
	
	private void setMax(final float x, final float y, final float z) {
		v3Max = new Vector3(Commons.clamp(0.0D, 1.0D, x), Commons.clamp(0.0D, 1.0D, y), Commons.clamp(0.0D, 1.0D, z));
	}
	
	public float getRotationYaw() {
		final int metadata = getBlockMetadata();
		float totalYaw;
		switch (EnumFacing.byIndex(metadata & 0x7)) {
		case DOWN : totalYaw =   0.0F; break;
		case UP   : totalYaw =   0.0F; break;
		case NORTH: totalYaw =  90.0F; break;
		case SOUTH: totalYaw = 270.0F; break;
		case WEST : totalYaw =   0.0F; break;
		case EAST : totalYaw = 180.0F; break;
		default   : totalYaw =   0.0F; break;
		}
		assert EnumForceFieldUpgrade.ROTATION.getProjectorUpgradeSlot() != null;
		if (hasUpgrade(EnumForceFieldUpgrade.ROTATION.getProjectorUpgradeSlot())) {
			totalYaw += rotationYaw;
		}
		return (totalYaw + 540.0F) % 360.0F - 180.0F; 
	}
	
	public float getRotationPitch() {
		final int metadata = getBlockMetadata();
		float totalPitch;
		switch (EnumFacing.byIndex(metadata & 0x7)) {
		case DOWN : totalPitch =  180.0F; break;
		case UP   : totalPitch =    0.0F; break;
		case NORTH: totalPitch =  -90.0F; break;
		case SOUTH: totalPitch =  -90.0F; break;
		case WEST : totalPitch =  -90.0F; break;
		case EAST : totalPitch =  -90.0F; break;
		default   : totalPitch =    0.0F; break;
		}
		assert EnumForceFieldUpgrade.ROTATION.getProjectorUpgradeSlot() != null;
		if (hasUpgrade(EnumForceFieldUpgrade.ROTATION.getProjectorUpgradeSlot())) {
			totalPitch += rotationPitch;
		}
		return (totalPitch + 540.0F) % 360.0F - 180.0F;
	}
	
	public float getRotationRoll() {
		assert EnumForceFieldUpgrade.ROTATION.getProjectorUpgradeSlot() != null;
		if (hasUpgrade(EnumForceFieldUpgrade.ROTATION.getProjectorUpgradeSlot())) {
			return (rotationRoll + 540.0F) % 360.0F - 180.0F;
		} else {
			return 0.0F;
		}
	}
	
	private void setRotation(final float rotationYaw, final float rotationPitch, final float rotationRoll) {
		final float oldYaw = this.rotationYaw;
		final float oldPitch = this.rotationPitch;
		final float oldRoll = this.rotationRoll;
		this.rotationYaw = Commons.clamp( -45.0F, +45.0F, rotationYaw);
		this.rotationPitch = Commons.clamp( -45.0F, +45.0F, rotationPitch);
		this.rotationRoll = (rotationRoll + 540.0F) % 360.0F - 180.0F;
		if ( oldYaw != this.rotationYaw
		  || oldPitch != this.rotationPitch
		  || oldRoll != this.rotationRoll ) {
			cache_forceFieldSetup = null;
		}
	}
	
	public EnumForceFieldShape getShape() {
		if (shape == null) {
			return EnumForceFieldShape.NONE;
		}
		return shape;
	}
	
	void setShape(final EnumForceFieldShape shape) {
		if (this.shape != shape) {
			this.shape = shape;
			cache_forceFieldSetup = null;
			
			// refresh block rendering
			markDirty();
		}
	}
	
	public Vector3 getTranslation() {
		assert EnumForceFieldUpgrade.TRANSLATION.getProjectorUpgradeSlot() != null;
		if (hasUpgrade(EnumForceFieldUpgrade.TRANSLATION.getProjectorUpgradeSlot())) {
			return v3Translation;
		} else {
			return new Vector3(0.0D, 0.0D, 0.0D);
		}
	}
	
	private void setTranslation(final float x, final float y, final float z) {
		final Vector3 oldTranslation = this.v3Translation;
		v3Translation = new Vector3(Commons.clamp(-1.0D, 1.0D, x), Commons.clamp(-1.0D, 1.0D, y), Commons.clamp(-1.0D, 1.0D, z));
		if (!oldTranslation.equals(v3Translation)) {
			cache_forceFieldSetup = null;
		}
	}
	
	@Override
	protected void onUpgradeChanged(@Nonnull final UpgradeSlot upgradeSlot, final int countNew, final boolean isAdded) {
		super.onUpgradeChanged(upgradeSlot, countNew, isAdded);
		cache_forceFieldSetup = null;
	}
	
	@Nonnull
	private WarpDriveText getShapeStatus() {
		final EnumForceFieldShape enumForceFieldShape = getShape();
		final WarpDriveText displayName = new WarpDriveText(null, "warpdrive.force_field.shape.status_line." + enumForceFieldShape.getName());
		if (enumForceFieldShape == EnumForceFieldShape.NONE) {
			return new WarpDriveText(Commons.getStyleWarning(), "warpdrive.force_field.shape.status_line.none",
			                         displayName);
		} else if (isDoubleSided) {
			return new WarpDriveText(null, "warpdrive.force_field.shape.status_line.double",
			                         displayName);
		} else {
			return new WarpDriveText(null, "warpdrive.force_field.shape.status_line.single",
			                         displayName);
		}
	}
	
	@Override
	public WarpDriveText getStatus() {
		return super.getStatus().append(getShapeStatus());
	}
	
	@Override
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
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
		
		isActive = tagCompound.getBoolean("isOn");
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
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
		
		tagCompound.setBoolean("isOn", isActive);
		
		return tagCompound;
	}
	
	public ForceFieldSetup getForceFieldSetup() {
		if (cache_forceFieldSetup == null) {
			// don't try until projector has initialised (see: entities colliding with the world during chunk loading)
			if (isFirstTick()) {
				return null;
			}
			
			cache_forceFieldSetup = new ForceFieldSetup(world.provider.getDimension(), pos, enumTier, beamFrequency);
			setupTicks = Math.max(setupTicks, 10);
			
			// reset field in case of major changes
			if (legacy_forceFieldSetup != null) {
				final int energyRequired = (int) Math.max(0, Math.round(cache_forceFieldSetup.startupEnergyCost - legacy_forceFieldSetup.startupEnergyCost));
				final IBlockState blockStateLegacyCamouflage = legacy_forceFieldSetup.getCamouflageBlockState();
				final IBlockState blockStateCachedCamouflage = cache_forceFieldSetup.getCamouflageBlockState();
				if ( (blockStateLegacyCamouflage == null && blockStateCachedCamouflage != null)
				  || (blockStateLegacyCamouflage != null && !blockStateLegacyCamouflage.equals(blockStateCachedCamouflage))
				  || legacy_forceFieldSetup.beamFrequency != cache_forceFieldSetup.beamFrequency
				  || !energy_consume(energyRequired, false) ) {
					if (WarpDriveConfig.LOGGING_FORCE_FIELD) {
						WarpDrive.logger.info(this + " rebooting with new rendering...");
					}
					destroyForceField("new rendering");
					
				} else if ( legacy_forceFieldSetup.isInverted != cache_forceFieldSetup.isInverted
				         || legacy_forceFieldSetup.shapeProvider != cache_forceFieldSetup.shapeProvider
				         || legacy_forceFieldSetup.thickness != cache_forceFieldSetup.thickness
				         || !legacy_forceFieldSetup.vMin.equals(cache_forceFieldSetup.vMin)
				         || !legacy_forceFieldSetup.vMax.equals(cache_forceFieldSetup.vMax)
				         || !legacy_forceFieldSetup.vTranslation.equals(cache_forceFieldSetup.vTranslation)
				         || legacy_forceFieldSetup.rotationYaw != cache_forceFieldSetup.rotationYaw
				         || legacy_forceFieldSetup.rotationPitch != cache_forceFieldSetup.rotationPitch
				         || legacy_forceFieldSetup.rotationRoll != cache_forceFieldSetup.rotationRoll
					     || (legacy_forceFieldSetup.breaking_maxHardness <= 0 && cache_forceFieldSetup.breaking_maxHardness > 0) ) {
					if (WarpDriveConfig.LOGGING_FORCE_FIELD) {
						WarpDrive.logger.info(this + " rebooting with new shape...");
					}
					destroyForceField("new shape");
					isDirty.set(true);
				}
			}
		}
		return cache_forceFieldSetup;
	}
	
	@Override
	public boolean energy_canInput(final EnumFacing from) {
		return true;
	}
	
	public boolean consumeEnergy(final double amount_internal, final boolean simulate) {
		final long longAmount = (long) Math.min(energy_getEnergyStored(), Math.floor(amount_internal + consumptionLeftOver));
		final boolean bResult = energy_consume(longAmount, simulate);
		if (!simulate) {
			consumptionLeftOver = amount_internal + consumptionLeftOver - longAmount;
		}
		return bResult;
	}
	
	// Common OC/CC methods
	@Override
	public Object[] getEnergyRequired() {
		return new Object[0];   // @TODO getEnergyRequired for projector
	}
	
	private Object[] state() {    // isConnected, isPowered, shape
		final long energy = energy_getEnergyStored();
		final String status = getStatusHeaderInPureText();
		return new Object[] { status, isEnabled, isConnected, isActive, getShape().name(), energy };
	}
	
	public Object[] min(final Object[] arguments) {
		assert EnumForceFieldUpgrade.RANGE.getProjectorUpgradeSlot() != null;
		return computer_getOrSetVector3(this::getMin, this::setMin, EnumForceFieldUpgrade.RANGE.getProjectorUpgradeSlot(), arguments);
	}
	
	public Object[] max(final Object[] arguments) {
		assert EnumForceFieldUpgrade.RANGE.getProjectorUpgradeSlot() != null;
		return computer_getOrSetVector3(this::getMax, this::setMax, EnumForceFieldUpgrade.RANGE.getProjectorUpgradeSlot(), arguments);
	}
	
	public Object[] rotation(final Object[] arguments) {
		if ( arguments != null
		  && arguments.length > 0
		  && arguments[0] != null ) {
			try {
				if (arguments.length == 1) {
					final float value = Commons.toFloat(arguments[0]);
					setRotation(value, 0, 0);
				} else if (arguments.length == 2) {
					final float value1 = Commons.toFloat(arguments[0]);
					final float value2 = Commons.toFloat(arguments[1]);
					setRotation(value1, value2, 0);
				} else if (arguments.length == 3) {
					final float value1 = Commons.toFloat(arguments[0]);
					final float value2 = Commons.toFloat(arguments[1]);
					final float value3 = Commons.toFloat(arguments[2]);
					setRotation(value1, value2, value3);
				}
			} catch (final Exception exception) {
				final String message = String.format("Float expected for all arguments %s",
				                                     Arrays.toString(arguments));
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.error(String.format("%s LUA error on rotation(): %s",
					                                     this, message));
				}
				return new Object[] { rotationYaw, rotationPitch, rotationRoll, message };
			}
		}
		assert EnumForceFieldUpgrade.ROTATION.getProjectorUpgradeSlot() != null;
		if (hasUpgrade(EnumForceFieldUpgrade.ROTATION.getProjectorUpgradeSlot())) {
			return new Float[] { rotationYaw, rotationPitch, rotationRoll };
		} else {
			return new Object[] { 0.0F, 0.0F, 0.0F, "Missing " + EnumForceFieldUpgrade.ROTATION.getProjectorUpgradeSlot().itemStack.getDisplayName() };
		}
	}
	
	public Object[] translation(final Object[] arguments) {
		assert EnumForceFieldUpgrade.TRANSLATION.getProjectorUpgradeSlot() != null;
		return computer_getOrSetVector3(this::getTranslation, this::setTranslation, EnumForceFieldUpgrade.TRANSLATION.getProjectorUpgradeSlot(), arguments);
	}
	
	// OpenComputers callback methods
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] state(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return state();
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] min(final Context context, final Arguments arguments) {
		return min(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] max(final Context context, final Arguments arguments) {
		return max(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] rotation(final Context context, final Arguments arguments) {
		return rotation(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] translation(final Context context, final Arguments arguments) {
		return translation(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "min":
			return min(arguments);
		
		case "max":
			return max(arguments);
		
		case "rotation":
			return rotation(arguments);
		
		case "state":
			return state();
		
		case "translation":
			return translation(arguments);
		}
		
		return super.CC_callMethod(methodName, arguments);
	}
	
	private static class ThreadCalculation extends Thread {
		
		private final WeakReference<TileEntityForceFieldProjector> weakProjector;
		private final String stringProjector;
		
		ThreadCalculation(final TileEntityForceFieldProjector projector) {
			this.weakProjector = new WeakReference<>(projector);
			stringProjector = projector.toString();
		}
		
		@Override
		public void run() {
			TileEntityForceFieldProjector projector;
			Set<VectorI> vPerimeterBlocks = null;
			Set<VectorI> vInteriorBlocks = null;
			
			// calculation start is done synchronously, by caller
			try {
				projector = weakProjector.get();
				if ( projector != null
				  && !projector.isInvalid()
				  && projector.isEnabled
				  && projector.isAssemblyValid ) {
					// collect what we need, then release the object
					final ForceFieldSetup forceFieldSetup = projector.getForceFieldSetup();
					if (forceFieldSetup.shapeProvider == EnumForceFieldShape.NONE) {
						if (WarpDriveConfig.LOGGING_FORCE_FIELD) {
							WarpDrive.logger.warn(String.format("%s Calculation aborted (no shape)",
							                                    this));
						}
						projector.calculation_done(null, null);
						return;
					}
					final int heightWorld = projector.world.getHeight();
					projector = null;
					
					if (WarpDriveConfig.LOGGING_FORCE_FIELD) {
						WarpDrive.logger.debug(String.format("%s Calculation started for %s",
						                                     this, stringProjector));
					}
					
					// create HashSets
					final VectorI vScale = forceFieldSetup.vMax.clone().subtract(forceFieldSetup.vMin);
					vInteriorBlocks = new HashSet<>(vScale.x * vScale.y * vScale.z);
					vPerimeterBlocks = new HashSet<>(2 * vScale.x * vScale.y + 2 * vScale.x * vScale.z + 2 * vScale.y * vScale.z);
					
					// compute interior fields to remove overlapping parts
					final Map<VectorI, Boolean> vertexes = forceFieldSetup.shapeProvider.getVertexes(forceFieldSetup);
					if (vertexes.isEmpty()) {
						WarpDrive.logger.error(String.format("%s No vertexes for %s at %s",
						                                     this, forceFieldSetup, stringProjector));
					}
					for (final Map.Entry<VectorI, Boolean> entry : vertexes.entrySet()) {
						final VectorI vPosition = entry.getKey();
						if (forceFieldSetup.isDoubleSided || vPosition.y >= 0) {
							if ( (forceFieldSetup.rotationYaw != 0.0F)
							  || (forceFieldSetup.rotationPitch != 0.0F)
							  || (forceFieldSetup.rotationRoll != 0.0F) ) {
								vPosition.rotateByAngle(forceFieldSetup.rotationYaw, forceFieldSetup.rotationPitch, forceFieldSetup.rotationRoll);
							}
							
							vPosition.translate(forceFieldSetup.vTranslation);
							
							if (vPosition.y > 0 && vPosition.y <= heightWorld) {
								if (entry.getValue()) {
									vPerimeterBlocks.add(vPosition);
								} else {
									vInteriorBlocks.add(vPosition);
								}
							}
						}
					}
					
					// compute force field itself
					if (forceFieldSetup.isInverted) {
						// inverted mode => same as interior before fusion => need to be fully cloned
						vPerimeterBlocks = new HashSet<>(vInteriorBlocks);
					}
					
					if (WarpDriveConfig.LOGGING_FORCE_FIELD) {
						WarpDrive.logger.debug(String.format("%s Calculation done: %s blocks inside, including %s blocks to place",
						                                     this, vInteriorBlocks.size(), vPerimeterBlocks.size()));
					}
				} else {
					if (WarpDriveConfig.LOGGING_FORCE_FIELD) {
						WarpDrive.logger.error(String.format("%s Calculation aborted",
						                                     this));
					}
				}
			} catch (final Exception exception) {
				vInteriorBlocks = null;
				vPerimeterBlocks = null;
				exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.error(String.format("%s Calculation failed for %s",
				                                     this, stringProjector));
			}
			
			projector = weakProjector.get();
			if (projector != null) {
				projector.calculation_done(vInteriorBlocks, vPerimeterBlocks);
			}
		}
	}
}
