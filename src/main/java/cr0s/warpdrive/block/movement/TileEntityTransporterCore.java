package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBeamFrequency;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.IItemTransporterBeacon;
import cr0s.warpdrive.api.IGlobalRegionProvider;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.api.computer.ICoreSignature;
import cr0s.warpdrive.api.computer.ITransporterBeacon;
import cr0s.warpdrive.api.computer.ITransporterCore;
import cr0s.warpdrive.block.TileEntityAbstractEnergyCoreOrController;
import cr0s.warpdrive.block.force_field.BlockForceField;
import cr0s.warpdrive.block.force_field.TileEntityForceField;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.EnergyWrapper;
import cr0s.warpdrive.data.EnumComponentType;
import cr0s.warpdrive.data.EnumGlobalRegionType;
import cr0s.warpdrive.data.EnumTransporterState;
import cr0s.warpdrive.data.ForceFieldSetup;
import cr0s.warpdrive.data.GlobalPosition;
import cr0s.warpdrive.data.GlobalRegionManager;
import cr0s.warpdrive.data.MovingEntity;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.data.VectorI;
import cr0s.warpdrive.event.ChunkLoadingHandler;
import cr0s.warpdrive.item.ItemComponent;
import cr0s.warpdrive.network.PacketHandler;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.INBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import net.minecraftforge.common.util.Constants;

public class TileEntityTransporterCore extends TileEntityAbstractEnergyCoreOrController implements ITransporterCore, IBeamFrequency, IGlobalRegionProvider {
	
	// global properties
	private static final UpgradeSlot upgradeSlotEnergyStorage = new UpgradeSlot("transporter.energy_storage",
	                                                                            ItemComponent.getItemStackNoCache(EnumComponentType.CAPACITIVE_CRYSTAL, 1),
	                                                                            WarpDriveConfig.TRANSPORTER_ENERGY_STORED_UPGRADE_MAX_QUANTITY );
	private static final UpgradeSlot upgradeSlotFocus = new UpgradeSlot("transporter.focus",
	                                                                    ItemComponent.getItemStackNoCache(EnumComponentType.EMERALD_CRYSTAL, 1),
	                                                                    WarpDriveConfig.TRANSPORTER_LOCKING_UPGRADE_MAX_QUANTITY );
	private static final UpgradeSlot upgradeSlotRange = new UpgradeSlot("transporter.range",
	                                                                    ItemComponent.getItemStackNoCache(EnumComponentType.ENDER_COIL, 1),
	                                                                    WarpDriveConfig.TRANSPORTER_RANGE_UPGRADE_MAX_QUANTITY );
	
	// persistent properties
	private ArrayList<BlockPos> vLocalScanners = null;
	private int beamFrequency = -1;
	private boolean isLockRequested = false;
	private boolean isEnergizeRequested = false;
	private Object remoteLocationRequested = null;  // can be VectorI() coordinates, or transporter UUID, or player name
	private double energyFactor = 1.0D;
	private double lockStrengthActual = 0.0D;
	private int tickCooldown = 0;
	private EnumTransporterState transporterState = EnumTransporterState.DISABLED;
	
	// computed properties
	private ArrayList<BlockPos> vLocalContainments = null;
	private AxisAlignedBB aabbLocalScanners = null;
	private int tickComputerPulse = 0;
	private boolean isConnected = false;
	private GlobalPosition globalPositionBeacon = null;
	private boolean isChunkLoading;
	private double energyCostForAcquiring = 0.0D;
	private double energyCostForEnergizing = 0.0D;
	private double lockStrengthOptimal = -1.0D;
	private double lockStrengthSpeed = 0.0D;
	private boolean isJammed = false;
	private String reasonJammed = "";
	private GlobalPosition globalPositionLocal = null;
	private GlobalPosition globalPositionRemote = null;
	private ArrayList<BlockPos> vRemoteScanners = null;
	private final HashMap<Integer, MovingEntity> movingEntitiesLocal = new HashMap<>(8);
	private final HashMap<Integer, MovingEntity> movingEntitiesRemote = new HashMap<>(8);
	private int tickEnergizing = 0;
	
	public TileEntityTransporterCore(@Nonnull final IBlockBase blockBase) {
		super(blockBase);
		
		peripheralName = "warpdriveTransporterCore";
		addMethods(new String[] {
			"beamFrequency",
			"state",
			"remoteLocation",
			"lock",
			"energyFactor",
			"getLockStrength",
			"energize"
		});
		CC_scripts = Collections.singletonList("startup");
		
		registerUpgradeSlot(upgradeSlotEnergyStorage);
		registerUpgradeSlot(upgradeSlotFocus);
		registerUpgradeSlot(upgradeSlotRange);
	}
	
	@Override
	protected void onConstructed() {
		super.onConstructed();
		
		refreshEnergyParameters();
	}
	
	@Override
	protected void onFirstTick() {
		super.onFirstTick();
		
		globalPositionLocal = new GlobalPosition(this);
	}
	
	@Override
	public void tick() {
		super.tick();
		
		assert world != null;
		if (world.isRemote()) {
			return;
		}
		
		// frequency status
		isConnected = IBeamFrequency.isValid(beamFrequency);
		
		// always cool down
		if (tickCooldown > 0) {
			tickCooldown--;
		} else {
			tickCooldown = 0;
		}
		
		// consume power and apply general lock strength increase and decay
		final boolean isPowered;
		if (!isEnabled) {
			transporterState = EnumTransporterState.DISABLED;
			isPowered = false;
			// (lock strength is cleared in state machine)
		} else {
			// energy consumption
			final int energyRequired = getEnergyRequired(transporterState);
			if (energyRequired > 0) {
				isPowered = energy_consume(energyRequired, false);
				if (!isPowered) {
					reasonJammed = "Insufficient energy for operation";
					transporterState = EnumTransporterState.IDLE;
					tickCooldown = Math.max(tickCooldown, WarpDriveConfig.TRANSPORTER_JAMMED_COOLDOWN_TICKS);
				}
			} else {
				isPowered = true;
			}
			
			
			// lock strength is capped at optimal, increasing when powered, decaying otherwise
			// final double lockStrengthPrevious = lockStrengthActual;
			if ( isPowered
			  && !isJammed
			  && ( transporterState == EnumTransporterState.ACQUIRING
			    || transporterState == EnumTransporterState.ENERGIZING ) ) {
				// a slight overshoot is added to force convergence
				final double overshoot = 0.01D;
				lockStrengthActual = Math.min(lockStrengthOptimal,
				                              lockStrengthActual + lockStrengthSpeed * (lockStrengthOptimal - lockStrengthActual + overshoot));
			} else {
				lockStrengthActual = Math.max(0.0D, lockStrengthActual * WarpDriveConfig.TRANSPORTER_LOCKING_STRENGTH_FACTOR_PER_TICK);
			}
			// WarpDrive.logger.info(String.format("Transporter strength %.5f -> %.5f", lockStrengthPrevious, lockStrengthActual));
		}
		
		// state feedback
		updateBlockState(null, BlockTransporterCore.VARIANT, !isConnected ? EnumTransporterState.DISABLED
		                                                               : !isEnabled ? EnumTransporterState.IDLE
		                                                               : !isPowered ? EnumTransporterState.ACQUIRING
		                                                               : EnumTransporterState.ENERGIZING); // @TODO add proper mapping
		if (isConnected && isEnabled) {
			if (isLockRequested && isJammed) {
				PacketHandler.sendSpawnParticlePacket(world, "jammed", (byte) 5,
				                                      new Vector3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D),
				                                      new Vector3(0.0D, 0.0D, 0.0D),
				                                      1.0F, 1.0F, 1.0F,
				                                      1.0F, 1.0F, 1.0F,
				                                      32);
			}
			if ( lockStrengthActual > 0.01F
			  || (transporterState == EnumTransporterState.ENERGIZING && tickEnergizing > 0)
			  || tickCooldown > 0 ) {
				PacketHandler.sendTransporterEffectPacket(world, globalPositionLocal, globalPositionRemote, lockStrengthActual,
				                                          movingEntitiesLocal.values(), movingEntitiesRemote.values(),
				                                          tickEnergizing, tickCooldown, 64);
			}
		}
		
		tickComputerPulse--;
		if (tickComputerPulse < 0) {
			tickComputerPulse = 20;
			if (lockStrengthActual > 0.01F) {
				sendEvent("transporterPulse", lockStrengthActual);
			}
		}
		
		// execute state transitions
		switch (transporterState) {
		case DISABLED:
			releaseChunks();
			isLockRequested = false;
			isEnergizeRequested = false;
			lockStrengthActual = 0.0D;
			if (isConnected && isEnabled) {
				transporterState = EnumTransporterState.IDLE;
			}
			break;
		
		case IDLE:
			if ( isLockRequested
			  && tickCooldown == 0 ) {
				// force parameters validation for next tick
				markDirtyParameters();
				transporterState = EnumTransporterState.ACQUIRING;
			} else {
				releaseChunks();
			}
			break;
		
		case ACQUIRING:
			if (!isLockRequested) {
				transporterState = EnumTransporterState.IDLE;
				
			} else if (isJammed) {// (jammed while acquiring)
				tickCooldown += WarpDriveConfig.TRANSPORTER_JAMMED_COOLDOWN_TICKS;
				transporterState = EnumTransporterState.IDLE;
				
			} else if (tickCooldown == 0) {
				if ( globalPositionBeacon != null
				  && !isEnergizeRequested
				  && lockStrengthActual >= 0.85D ) {// automatic energizing triggered by beacon
					isEnergizeRequested = true;
					
				} else if (isEnergizeRequested) {
					// force parameters validation for next tick
					markDirtyParameters();
					
					tickEnergizing = WarpDriveConfig.TRANSPORTER_ENERGIZING_CHARGING_TICKS;
					transporterState = EnumTransporterState.ENERGIZING;
				}
			}
			break;
		
		case ENERGIZING:
			if (!isLockRequested) {
				transporterState = EnumTransporterState.IDLE;
				
			} else if (!isEnergizeRequested) {
				transporterState = EnumTransporterState.ACQUIRING;
				
			} else if (isJammed) {// (jammed while energizing)
				tickCooldown += WarpDriveConfig.TRANSPORTER_JAMMED_COOLDOWN_TICKS;
				transporterState = EnumTransporterState.IDLE;
				
			} else if (tickCooldown <= 0) {// (not cooling down)
				state_energizing();
			}
			break;
		
		default:
			transporterState = EnumTransporterState.DISABLED;
			break;
		}
	}
	
	@Override
	public void onChunkUnloaded() {
		releaseChunks();
		
		super.onChunkUnloaded();
	}
	
	@Override
	public void remove() {
		assert world != null;
		if (!world.isRemote()) {
			rebootTransporter();
		}
		super.remove();
	}
	
	private void rebootTransporter() {
		// switch connected scanners to 'offline'
		if (vLocalScanners != null) {
			assert world != null;
			for (final BlockPos vScanner : vLocalScanners) {
				final BlockState blockState = world.getBlockState(vScanner);
				if (blockState.getBlock() instanceof BlockTransporterScanner) {
					world.setBlockState(vScanner, blockState.with(BlockProperties.ACTIVE, false), 2);
				}
			}
		}
		releaseChunks();
	}
	
	private void state_energizing() {
		// load remote world
		final ServerWorld worldRemote = Commons.getOrCreateWorldServer(globalPositionRemote.dimensionId);
		if (worldRemote == null) {
			WarpDrive.logger.error(String.format("Unable to initialize dimension %s for %s",
			                                     globalPositionRemote.dimensionId,
			                                     this ));
			isJammed = true;
			reasonJammed = String.format("Unable to initialize dimension %s",
			                             globalPositionRemote.dimensionId);
			return;
		}
		
		// get entities
		final EntityValues entityValues = updateEntitiesToEnergize(worldRemote);
		
		// post event on first tick
		if (tickEnergizing == WarpDriveConfig.TRANSPORTER_ENERGIZING_CHARGING_TICKS) {
			sendEvent("transporterEnergizing", entityValues.count);
		}
		
		// cancel if no entity was found
		if (entityValues.count == 0) {
			// cancel transfer, cool down, don't loose strength
			isEnergizeRequested = false;
			tickCooldown += WarpDriveConfig.TRANSPORTER_ENERGIZING_COOLDOWN_TICKS;
			transporterState = EnumTransporterState.ACQUIRING;
			return;
		}
		
		// warm-up
		if (tickEnergizing > 0) {
			tickEnergizing--;
			return;
		}
		
		// transfer at final tick
		if ( vRemoteScanners == null
		  || vRemoteScanners.isEmpty() ) {
			energizeEntities(lockStrengthActual, movingEntitiesLocal, worldRemote, globalPositionRemote.getBlockPos());
		} else {
			energizeEntities(lockStrengthActual, movingEntitiesLocal, worldRemote, vRemoteScanners);
		}
		if ( vLocalScanners != null
		  && !vLocalScanners.isEmpty() ) {
			energizeEntities(lockStrengthActual, movingEntitiesRemote, world, vLocalScanners);
		}
		
		// clear entities, cancel transfer, cool down, loose a bit of strength
		isEnergizeRequested = false;
		markDirtyParameters();
		tickCooldown += WarpDriveConfig.TRANSPORTER_ENERGIZING_COOLDOWN_TICKS;
		lockStrengthActual = Math.max(0.0D, lockStrengthActual - WarpDriveConfig.TRANSPORTER_ENERGIZING_LOCKING_LOST);
		transporterState = EnumTransporterState.ACQUIRING;
		
		// inform beacon provider
		if (globalPositionBeacon != null) {
			final World world = globalPositionBeacon.getWorldServerIfLoaded();
			if (world == null) {
				WarpDrive.logger.warn(String.format("Unable to disable TransporterBeacon %s: world isn't loaded",
				                      globalPositionBeacon));
			} else {
				final TileEntity tileEntity = world.getTileEntity(globalPositionBeacon.getBlockPos());
				if (tileEntity instanceof ITransporterBeacon) {
					((ITransporterBeacon) tileEntity).energizeDone();
				} else {
					WarpDrive.logger.warn(String.format("Unable to disable TransporterBeacon %s: unsupported tile entity %s",
					                      globalPositionBeacon, tileEntity));
				}
			}
		}
	}
	
	private void energizeEntities(final double lockStrength, final HashMap<Integer, MovingEntity> movingEntities, final World world, final BlockPos vPosition) {
		for (final Entry<Integer, MovingEntity> entryEntity : movingEntities.entrySet()) {
			final MovingEntity movingEntity = entryEntity.getValue();
			energizeEntity(lockStrength, movingEntity, world, vPosition);
		}
	}
	private void energizeEntities(final double lockStrength, final HashMap<Integer, MovingEntity> movingEntities, final World world, final ArrayList<BlockPos> vScanners) {
		for (final Entry<Integer, MovingEntity> entryEntity : movingEntities.entrySet()) {
			final int indexEntity = entryEntity.getKey();
			final MovingEntity movingEntity = entryEntity.getValue();
			final BlockPos vScanner = vScanners.get(indexEntity);
			energizeEntity(lockStrength, movingEntity, world, vScanner);
		}
	}
	private void energizeEntity(final double lockStrength, final MovingEntity movingEntity, final World world, final BlockPos vPosition) {
		// check entity is valid
		if (movingEntity == MovingEntity.INVALID) {
			return;
		}
		if ( movingEntity == null
		  || vPosition == null ) {// corrupted data
			WarpDrive.logger.warn(String.format("Invalid entity %s for position %s, skipping transportation...",
			                                    movingEntity, vPosition));
			return;
		}
		final Entity entity = movingEntity.getEntity();
		if (entity == null) {// (bad state)
			WarpDrive.logger.warn("Entity went missing, skipping transportation...");
			return;
		}
		
		// compute friendly name
		final String nameEntity = entity.getName().getString();
		
		// check lock strength
		if ( lockStrength < 1.0D
		  && world.rand.nextDouble() > lockStrength ) {
			if (WarpDriveConfig.LOGGING_TRANSPORTER) {
				WarpDrive.logger.info(String.format("Insufficient lock strength %.3f to transport %s",
				                                    lockStrength, entity));
			}
			applyTeleportationDamages(false, entity, lockStrength);
			
			// post event on transfer done
			sendEvent("transporterFailure", nameEntity);
			return;
		}
		
		// teleport
		final Vector3 v3Target = new Vector3(
				vPosition.getX() + 0.5D,
				vPosition.getY() + 0.99D,
				vPosition.getZ() + 0.5D);
		if (WarpDriveConfig.LOGGING_TRANSPORTER) {
			WarpDrive.logger.info(String.format("Transporting entity %s to %s",
			                                    entity, v3Target));
		}
		Commons.moveEntity(entity, world, v3Target);
		applyTeleportationDamages(false, entity, lockStrength);
		
		// post event on transfer done
		sendEvent("transporterSuccess", nameEntity);
	}
	
	@Override
	public WarpDriveText getStatusHeader() {
		if ( globalPositionLocal == null
		  || globalPositionRemote == null ) {
			return super.getStatusHeader();
		}
		return super.getStatusHeader()
		            .append(null, "warpdrive.transporter.status_line.from_to",
		                    globalPositionLocal.x, globalPositionLocal.y, globalPositionLocal.z,
		                    globalPositionRemote.x, globalPositionRemote.y, globalPositionRemote.z);
	}
	
	@Override
	public int getBeamFrequency() {
		return beamFrequency;
	}
	
	@Override
	public void setBeamFrequency(final int parBeamFrequency) {
		if ( beamFrequency != parBeamFrequency
		  && IBeamFrequency.isValid(parBeamFrequency) ) {
			final int beamFrequencyOld = beamFrequency;
			beamFrequency = parBeamFrequency;
			markDirtyParameters();
			if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
				WarpDrive.logger.info(String.format("%s Beam frequency set from %d to %d",
				                                    this, beamFrequencyOld, beamFrequency ));
			}
		}
		markDirty();
	}
	
	// IGlobalRegionProvider overrides
	@Override
	public EnumGlobalRegionType getGlobalRegionType() {
		return EnumGlobalRegionType.TRANSPORTER;
	}
	
	@Override
	public AxisAlignedBB getGlobalRegionArea() {
		return new AxisAlignedBB(
			Math.min(pos.getX() - WarpDriveConfig.TRANSPORTER_SETUP_SCANNER_RANGE_XZ_BLOCKS     , aabbLocalScanners == null ? pos.getX() : aabbLocalScanners.minX),
			Math.min(pos.getY() - WarpDriveConfig.TRANSPORTER_SETUP_SCANNER_RANGE_Y_BELOW_BLOCKS, aabbLocalScanners == null ? pos.getY() : aabbLocalScanners.minY),
			Math.min(pos.getZ() - WarpDriveConfig.TRANSPORTER_SETUP_SCANNER_RANGE_XZ_BLOCKS     , aabbLocalScanners == null ? pos.getZ() : aabbLocalScanners.minZ),
			Math.max(pos.getX() + WarpDriveConfig.TRANSPORTER_SETUP_SCANNER_RANGE_XZ_BLOCKS     , aabbLocalScanners == null ? pos.getX() : aabbLocalScanners.maxX),
			Math.max(pos.getY() + WarpDriveConfig.TRANSPORTER_SETUP_SCANNER_RANGE_Y_ABOVE_BLOCKS, aabbLocalScanners == null ? pos.getY() : aabbLocalScanners.maxY),
			Math.max(pos.getZ() + WarpDriveConfig.TRANSPORTER_SETUP_SCANNER_RANGE_XZ_BLOCKS     , aabbLocalScanners == null ? pos.getZ() : aabbLocalScanners.maxZ) );
	}
	
	@Override
	public int getMass() {
		return vLocalScanners == null ? 0 : vLocalScanners.size();
	}
	
	@Override
	public double getIsolationRate() {
		return 0.0D;
	}
	
	@Override
	public boolean onBlockUpdatingInArea(@Nullable final Entity entity, final BlockPos blockPos, final BlockState blockState) {
		// skip in case of explosion, etc.
		if (isDirtyAssembly()) {
			return true;
		}
		
		// check for significant change
		final Block block = blockState.getBlock();
		if ( block instanceof BlockTransporterScanner
		  || block instanceof BlockTransporterContainment) {
			markDirtyAssembly();
			return true;
		}
		if ( aabbLocalScanners != null
		  && blockPos.getX() >= aabbLocalScanners.minX
		  && blockPos.getY() >= aabbLocalScanners.minY
		  && blockPos.getZ() >= aabbLocalScanners.minZ
		  && blockPos.getX() <  aabbLocalScanners.maxX
		  && blockPos.getY() <  aabbLocalScanners.maxY
		  && blockPos.getZ() <  aabbLocalScanners.maxZ ) {
			if (WarpDriveConfig.LOGGING_TRANSPORTER) {
				WarpDrive.logger.info(String.format("onBlockUpdatingInArea %s %s",
				                                    blockState,
				                                    Commons.format(world, blockPos) ));
			}
			markDirtyAssembly();
		}
		return true;
	}
	
	@Override
	protected boolean doScanAssembly(final boolean isDirty, final WarpDriveText textReason) {
		final boolean isValid = super.doScanAssembly(isDirty, textReason);
		assert world != null;
		
		// scan the whole area for scanners
		final int xMin = pos.getX() - WarpDriveConfig.TRANSPORTER_SETUP_SCANNER_RANGE_XZ_BLOCKS;
		final int xMax = pos.getX() + WarpDriveConfig.TRANSPORTER_SETUP_SCANNER_RANGE_XZ_BLOCKS;
		final int yMin = pos.getY() - WarpDriveConfig.TRANSPORTER_SETUP_SCANNER_RANGE_Y_BELOW_BLOCKS;
		final int yMax = pos.getY() + WarpDriveConfig.TRANSPORTER_SETUP_SCANNER_RANGE_Y_ABOVE_BLOCKS;
		final int zMin = pos.getZ() - WarpDriveConfig.TRANSPORTER_SETUP_SCANNER_RANGE_XZ_BLOCKS;
		final int zMax = pos.getZ() + WarpDriveConfig.TRANSPORTER_SETUP_SCANNER_RANGE_XZ_BLOCKS;
		
		final ArrayList<BlockPos> vScanners = new ArrayList<>(16);
		final HashSet<BlockPos> vContainments = new HashSet<>(64);
		final BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable(0, 0, 0);
		for (int x = xMin; x <= xMax; x++) {
			for (int y = yMin; y <= yMax; y++) {
				if (y < 0 || y > 254) {
					continue;
				}
				
				for (int z = zMin; z <= zMax; z++) {
					mutableBlockPos.setPos(x, y, z);
					final BlockState blockState = world.getBlockState(mutableBlockPos);
					final Block block = blockState.getBlock();
					if (block instanceof BlockTransporterScanner) {
						
						// only accept valid ones, spawn particles on others
						final Collection<BlockPos> vValidContainments = ((BlockTransporterScanner) block).getValidContainment(world, mutableBlockPos);
						if (vValidContainments == null || vValidContainments.isEmpty()) {
							textReason.append(Commons.getStyleWarning(), "warpdrive.transporter.status_line.missing_containment",
							                  Commons.format(world, mutableBlockPos) );
							world.setBlockState(mutableBlockPos, blockState.with(BlockProperties.ACTIVE, false), 2);
							PacketHandler.sendSpawnParticlePacket(world, "jammed", (byte) 5,
							                                      new Vector3(x + 0.5D, y + 1.5D, z + 0.5D),
							                                      new Vector3(0.0D, 0.0D, 0.0D),
							                                      1.0F, 1.0F, 1.0F,
							                                      1.0F, 1.0F, 1.0F,
							                                      32);
						} else {
							vScanners.add(mutableBlockPos.toImmutable());
							vContainments.addAll(vValidContainments);
							world.setBlockState(mutableBlockPos, blockState.with(BlockProperties.ACTIVE, true), 2);
						}
					}
				}
			}
		}
		setLocalScanners(vScanners, vContainments);
		
		// cascade updates
		markDirtyParameters();
		markDirtyGlobalRegion();
		
		return isValid;
	}
	
	private void setLocalScanners(final ArrayList<BlockPos> vScanners, final Collection<BlockPos> vContainments) {
		// no scanner defined => force null
		if (vScanners == null || vScanners.isEmpty()) {
			vLocalScanners = null;
			vLocalContainments = null;
			aabbLocalScanners = null;
			return;
		}
		
		// cache block update detection area
		final VectorI vMin = new VectorI(vScanners.get(0));
		final VectorI vMax = new VectorI(vScanners.get(0));
		for (final BlockPos vScanner : vScanners) {
			vMin.x = Math.min(vMin.x, vScanner.getX() - 1);
			vMin.y = Math.min(vMin.y, vScanner.getY()    );
			vMin.z = Math.min(vMin.z, vScanner.getZ() - 1);
			vMax.x = Math.max(vMax.x, vScanner.getX() + 1);
			vMax.y = Math.max(vMax.y, vScanner.getY() + 2);
			vMax.z = Math.max(vMax.z, vScanner.getZ() + 1);
		}
		
		// save values
		vLocalScanners = vScanners;
		vLocalContainments = new ArrayList<>(vContainments);
		aabbLocalScanners = new AxisAlignedBB(
				vMin.x, vMin.y, vMin.z,
				vMax.x + 1.0D, vMax.y + 1.0D, vMax.z + 1.0D);
	}
	
	public Collection<BlockPos> getContainments() {
		return vLocalContainments;
	}
	
	private static class FocusValues {
		ArrayList<BlockPos> vScanners;
		int countRangeUpgrades;
		double strength;
		double speed;
	}
	
	private static class EntityValues {
		int count;
		long mass;
	}
	
	private void checkBeaconObsolescence() {
		if (globalPositionBeacon != null) {
			final ServerWorld worldBeacon = globalPositionBeacon.getWorldServerIfLoaded();
			if (worldBeacon == null) {
				globalPositionBeacon = null;
				isLockRequested = false;
				isEnergizeRequested = false;
				isJammed = false;
				reasonJammed = "";
				
			} else {
				final TileEntity tileEntity = worldBeacon.getTileEntity(globalPositionBeacon.getBlockPos());
				if ( !(tileEntity instanceof ITransporterBeacon)
				  || !((ITransporterBeacon) tileEntity).isActive() ) {
					globalPositionBeacon = null;
					isLockRequested = false;
					isEnergizeRequested = false;
					isJammed = false;
					reasonJammed = "";
				}
			}
		}
	}
	
	@Override
	protected void doUpdateParameters(final boolean isDirty) {
		isJammed = false;
		reasonJammed = "";
		
		// reset entities to grab
		if (transporterState != EnumTransporterState.ENERGIZING) {
			movingEntitiesLocal.clear();
			movingEntitiesRemote.clear();
		}
		
		// check connection
		if (!isConnected) {
			isJammed = true;
			reasonJammed = "Beam frequency not set";
			return;
		}
		
		// compute local universal coordinates
		final CelestialObject celestialObjectLocal = CelestialObjectManager.get(world, globalPositionLocal.x, globalPositionLocal.z);
		final Vector3 v3Local_universal = GlobalRegionManager.getUniversalCoordinates(celestialObjectLocal, globalPositionLocal.x, globalPositionLocal.y, globalPositionLocal.z);
		
		// validate context
		checkBeaconObsolescence();
		
		// compute remote global position
		GlobalPosition globalPositionRemoteNew = null;
		if (globalPositionBeacon != null) {
			globalPositionRemoteNew = globalPositionBeacon;
			
		} else if (remoteLocationRequested instanceof VectorI) {
			final VectorI vRequest = ((VectorI) remoteLocationRequested).clone();
			if (vRequest.y < 0) {
				final CelestialObject celestialObjectChild = CelestialObjectManager.getClosestChild(world, pos.getX(), pos.getZ());
				if (celestialObjectChild == null) {
					reasonJammed = "Not in orbit of a planet!\nIncrease target Y coordinate.";
				} else {
					vRequest.translate(celestialObjectChild.getEntryOffset());
					globalPositionRemoteNew = new GlobalPosition(celestialObjectChild.dimensionId, vRequest.x, (vRequest.y + 1024) % 256, vRequest.z);
				}
			} else if (vRequest.y > 256) {
				if (celestialObjectLocal == null) {
					reasonJammed = "Unknown local celestial object!\nThere's no orbit to reach from here.\nReduce target Y coordinate.";
				} else if (celestialObjectLocal.parent == null) {
					reasonJammed = "No parent dimension!\nThere's no orbit to reach from here.\nReduce target Y coordinate.";
				} else {
					vRequest.translateBack(celestialObjectLocal.getEntryOffset());
					globalPositionRemoteNew = new GlobalPosition(celestialObjectLocal.parent.dimensionId, vRequest.x, vRequest.y % 256, vRequest.z);
				}
				
			} else {
				globalPositionRemoteNew = new GlobalPosition(world.getDimension().getType().getRegistryName(), vRequest.x, vRequest.y, vRequest.z);
			}
			
		} else if (remoteLocationRequested instanceof UUID) {
			globalPositionRemoteNew = GlobalRegionManager.getByUUID(EnumGlobalRegionType.TRANSPORTER, (UUID) remoteLocationRequested);
			if (globalPositionRemoteNew == null) {
				reasonJammed = "Unknown transporter signature";
			}
			
		} else if (remoteLocationRequested instanceof String) {
			final ServerPlayerEntity entityPlayer = Commons.getOnlinePlayerByName((String) remoteLocationRequested);
			if (entityPlayer == null) {
				reasonJammed = "No player by that name";
			} else {
				ItemStack itemStackHeld = entityPlayer.getHeldItem(Hand.MAIN_HAND);
				if ( itemStackHeld.isEmpty()
				  || !(itemStackHeld.getItem() instanceof IItemTransporterBeacon) ) {
					itemStackHeld = entityPlayer.getHeldItem(Hand.OFF_HAND);
				}
				if ( itemStackHeld.isEmpty()
				  || !(itemStackHeld.getItem() instanceof IItemTransporterBeacon) ) {
				    reasonJammed = "No transporter beacon in player hand";
				} else if (!((IItemTransporterBeacon) itemStackHeld.getItem()).isActive(itemStackHeld)) {
					reasonJammed = "Player beacon is out of power";
				} else {
					globalPositionRemoteNew = new GlobalPosition(entityPlayer);
				}
			}
		} else {
			reasonJammed = "No remote location defined";
		}
		
		if ( globalPositionRemoteNew == null
		  || !globalPositionRemoteNew.equals(globalPositionRemote) ) {
			globalPositionRemote = globalPositionRemoteNew;
			lockStrengthActual = 0.0D;
			if (transporterState == EnumTransporterState.ENERGIZING) {
				transporterState = EnumTransporterState.ACQUIRING;
			}
			isJammed = (globalPositionRemoteNew == null);
			if (isJammed) {
				return;
			}
		}
		
		// validate target dimension
		final CelestialObject celestialObjectRemote = globalPositionRemote.getCelestialObject(world.isRemote());
		final Vector3 v3Remote_universal = globalPositionRemote.getUniversalCoordinates(world.isRemote());
		
		if (celestialObjectRemote == null) {
			isJammed = true;
			reasonJammed = "Unknown remote celestial object";
			return;
		}
		
		// validate cross dimension transport rules
		if (celestialObjectLocal != celestialObjectRemote) {
			if ( ( celestialObjectLocal != null
			    && celestialObjectLocal.isHyperspace() )
			  || celestialObjectRemote.isHyperspace() ) {
				isJammed = true;
				reasonJammed = "Blocked by warp field barrier!\nExit hyperspace to use transporter room.";
				return;
			}
			
			if (celestialObjectRemote.isVirtual()) {
				isJammed = true;
				reasonJammed = "Unable to reach virtual planet";
				return;
			}
		}
		
		// get remote world, only load while acquiring or energizing
		final ServerWorld worldRemote;
		if ( transporterState == EnumTransporterState.ACQUIRING
		  || transporterState == EnumTransporterState.ENERGIZING ) {
			worldRemote = Commons.getOrCreateWorldServer(celestialObjectRemote.dimensionId);
			if (worldRemote == null) {
				WarpDrive.logger.error(String.format("Unable to initialize dimension %s for %s",
				                                     celestialObjectRemote.dimensionId,
				                                     this ));
				isJammed = true;
				reasonJammed = String.format("Unable to initialize dimension %s",
				                             celestialObjectRemote.dimensionId);
				return;
			}
		} else {
			worldRemote = Commons.getOrCreateWorldServer(celestialObjectRemote.dimensionId);
		}
		
		// compute range
		assert v3Local_universal != null;
		final double rangeActualSquared = v3Local_universal.clone().subtract(v3Remote_universal).getMagnitudeSquared();
		final int rangeActual = (int) Math.ceil(Math.sqrt(rangeActualSquared));
		
		// compute focalization bonuses
		final FocusValues focusValuesLocal  = getFocusValueAtCoordinates(world, globalPositionLocal.getBlockPos(), 0);
		final FocusValues focusValuesRemote = getFocusValueAtCoordinates(worldRemote, globalPositionRemote.getBlockPos(), WarpDriveConfig.TRANSPORTER_FOCUS_SEARCH_RADIUS_BLOCKS);
		final double focusBoost = Commons.interpolate(
				1.0D,
				0.0D,
				WarpDriveConfig.TRANSPORTER_ENERGIZING_MAX_ENERGY_FACTOR,
				WarpDriveConfig.TRANSPORTER_LOCKING_STRENGTH_BONUS_AT_MAX_ENERGY_FACTOR,
				energyFactor);
		lockStrengthOptimal = (focusValuesLocal.strength + focusValuesRemote.strength) / 2.0D + focusBoost;
		lockStrengthSpeed   = (focusValuesLocal.speed + focusValuesRemote.speed) / 2.0D
		                    / WarpDriveConfig.TRANSPORTER_LOCKING_SPEED_OPTIMAL_TICKS;
		
		final int rangeMax = WarpDriveConfig.TRANSPORTER_RANGE_BASE_BLOCKS
		                   + WarpDriveConfig.TRANSPORTER_RANGE_UPGRADE_BLOCKS * focusValuesLocal.countRangeUpgrades
		                   + WarpDriveConfig.TRANSPORTER_RANGE_UPGRADE_BLOCKS * focusValuesRemote.countRangeUpgrades;
		
		// retrieve remote scanner positions
		vRemoteScanners = focusValuesRemote.vScanners;
		
		// update entities in range
		final EntityValues entityValues = updateEntitiesToEnergize(worldRemote);
		
		// compute energy cost from range
		energyCostForAcquiring = Math.max(0, WarpDriveConfig.TRANSPORTER_LOCKING_ENERGY_FACTORS[0]
		                                   + WarpDriveConfig.TRANSPORTER_LOCKING_ENERGY_FACTORS[1]
		                                     * (vLocalScanners == null ? 1 : vLocalScanners.size())
		                                     * ( Math.log(1.0D + WarpDriveConfig.TRANSPORTER_LOCKING_ENERGY_FACTORS[2] * rangeActual)
		                                       + Math.pow(WarpDriveConfig.TRANSPORTER_LOCKING_ENERGY_FACTORS[3] + rangeActual,
		                                                  WarpDriveConfig.TRANSPORTER_LOCKING_ENERGY_FACTORS[4]) ) );
		
		energyCostForEnergizing = Math.max(0, WarpDriveConfig.TRANSPORTER_ENERGIZING_ENERGY_FACTORS[0]
		                                    + WarpDriveConfig.TRANSPORTER_ENERGIZING_ENERGY_FACTORS[1]
		                                      * entityValues.mass
		                                      * ( Math.log(1.0D + WarpDriveConfig.TRANSPORTER_ENERGIZING_ENERGY_FACTORS[2] * rangeActual)
		                                        + Math.pow(WarpDriveConfig.TRANSPORTER_ENERGIZING_ENERGY_FACTORS[3] + rangeActual,
		                                                   WarpDriveConfig.TRANSPORTER_ENERGIZING_ENERGY_FACTORS[4]) ) );
		
		if (WarpDriveConfig.LOGGING_TRANSPORTER) {
			WarpDrive.logger.info(String.format("Transporter parameters at %s are range (actual %d max %d) lockStrength (actual %.5f optimal %.5f speed %.5f)",
			                                    Commons.format(world, pos),
			                                    rangeActual, rangeMax,
			                                    lockStrengthActual, lockStrengthOptimal, lockStrengthSpeed));
		}
		
		// check minimum range
		if (rangeActual < 16) {
			isJammed = true;
			reasonJammed = "Remote location is too close";
			return;
		}
		
		//  check maximum range
		if (rangeActual > rangeMax) {
			isJammed = true;
			reasonJammed = String.format("Out of range: %d > %d m", rangeActual, rangeMax);
			return;
		}
		
		// validate shields along trajectory
		if (world == worldRemote) {// same world
			isJammed |= isJammedTrajectory(world, globalPositionLocal.getBlockPos(), globalPositionRemote.getBlockPos(), beamFrequency);
		} else if (v3Local_universal.y > v3Remote_universal.y) {// remote is below us
			isJammed |= isJammedTrajectory(world,
			                               globalPositionLocal.getBlockPos(),
			                               new BlockPos(globalPositionLocal.x, -1, globalPositionLocal.z),
			                               beamFrequency);
			isJammed |= isJammedTrajectory(worldRemote,
			                               new BlockPos(globalPositionRemote.x, 256, globalPositionRemote.z),
			                               globalPositionRemote.getBlockPos(),
			                               beamFrequency);
		} else {// remote is above us
			isJammed |= isJammedTrajectory(world,
			                               globalPositionLocal.getBlockPos(),
			                               new BlockPos(globalPositionLocal.x, 256, globalPositionLocal.z),
			                               beamFrequency);
			isJammed |= isJammedTrajectory(worldRemote,
			                               new BlockPos(globalPositionRemote.x, -1, globalPositionRemote.z),
			                               globalPositionRemote.getBlockPos(),
			                               beamFrequency);
		}
		if (isJammed) {
			reasonJammed = "Blocked by force field or unbreakable block";
		}
	}
	
	public boolean updateBeacon(final TileEntity tileEntity, final UUID uuidTransporterCore) {
		if ( tileEntity == null
		  || this.uuid == null
		  || !this.uuid.equals(uuidTransporterCore) ) {
			WarpDrive.logger.error(String.format("%s Invalid parameters in beacon call to transporter as %s, %s",
			                                     this, tileEntity, uuidTransporterCore));
			// just ignore it
			return false;
		}
		
		// check for overlapping beacon requests
		if ( globalPositionBeacon != null
		  && !globalPositionBeacon.equals(tileEntity) ) {
			final long radius2 = (long) WarpDriveConfig.TRANSPORTER_FOCUS_SEARCH_RADIUS_BLOCKS * WarpDriveConfig.TRANSPORTER_FOCUS_SEARCH_RADIUS_BLOCKS;
			if (globalPositionBeacon.distance2To(tileEntity) <= radius2) {// it's a beacon party! we're happy with it...
				return true;
			}

			// always validate context
			checkBeaconObsolescence();
			if (globalPositionBeacon != null) {
				if (!isJammed && WarpDriveConfig.LOGGING_TRANSPORTER) {// only log first jamming occurrence
					WarpDrive.logger.info(String.format("%s Conflicting beacon requests received %s is not %s",
					                                    this, tileEntity, uuid));
				}
				isJammed = true;
				reasonJammed = "Conflicting beacon requests received";
				tickCooldown = Math.max(tickCooldown, WarpDriveConfig.TRANSPORTER_JAMMED_COOLDOWN_TICKS);
				return false;
			}
		}
		
		// check for new beacon
		if (globalPositionBeacon == null) {
			globalPositionBeacon = new GlobalPosition(tileEntity);
			energyFactor = Math.max(4.0D, energyFactor);    // ensure minimum energy factor for beacon activation
			isJammed = true;
			reasonJammed = "Beacon request received";
			lockStrengthActual = 0.0D;
			forceChunks();
			if (transporterState == EnumTransporterState.ENERGIZING) {
				transporterState = EnumTransporterState.ACQUIRING;
			}
		}
		
		isLockRequested = true;
		return true;
	}
	
	private void forceChunks() {
		if (isChunkLoading) {
			WarpDrive.logger.warn(String.format("%s Already chunkloading...",
			                                     this ));
			return;
		}
		
		if (WarpDriveConfig.LOGGING_TRANSPORTER) {
			WarpDrive.logger.info(String.format("%s Forcing chunks",
			                                    this ));
		}
		
		assert world instanceof ServerWorld;
		final AxisAlignedBB aabbArea = getGlobalRegionArea();
		final int minX = (int) aabbArea.minX >> 4;
		final int maxX = (int) aabbArea.maxX >> 4;
		final int minZ = (int) aabbArea.minZ >> 4;
		final int maxZ = (int) aabbArea.maxZ >> 4;
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				ChunkLoadingHandler.registerTicket((ServerWorld) world, new ChunkPos(x, z), 1);
			}
		}
	}
	
	private void releaseChunks() {
		if (!isChunkLoading) {
			return;
		}
		
		if (WarpDriveConfig.LOGGING_TRANSPORTER) {
			WarpDrive.logger.info(this + " Releasing chunks");
		}
		
		assert world instanceof ServerWorld;
		final AxisAlignedBB aabbArea = getGlobalRegionArea();
		final int minX = (int) aabbArea.minX >> 4;
		final int maxX = (int) aabbArea.maxX >> 4;
		final int minZ = (int) aabbArea.minZ >> 4;
		final int maxZ = (int) aabbArea.maxZ >> 4;
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				ChunkLoadingHandler.releaseTicket((ServerWorld) world, new ChunkPos(x, z), 1);
			}
		}
		isChunkLoading = false;
	}
	
	private static FocusValues getFocusValueAtCoordinates(final World world, final BlockPos blockPos, final int radius) {
		//  return default values if world isn't loaded
		if (world == null) {
			final FocusValues result = new FocusValues();
			result.countRangeUpgrades = 0;
			result.speed = WarpDriveConfig.TRANSPORTER_LOCKING_SPEED_IN_WILDERNESS;
			result.strength = WarpDriveConfig.TRANSPORTER_LOCKING_STRENGTH_IN_WILDERNESS;
			return result;
		}
		
		// scan the area
		int countBeacons = 0;
		int countTransporters = 0;
		int sumRangeUpgrades = 0;
		int sumFocusUpgrades = 0;
		
		final int xMin = blockPos.getX() - radius;
		final int xMax = blockPos.getX() + radius;
		final int yMin = blockPos.getY() - radius;
		final int yMax = blockPos.getY() + radius;
		final int zMin = blockPos.getZ() - radius;
		final int zMax = blockPos.getZ() + radius;
		
		ArrayList<BlockPos> vScanners = null;
		final BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable(0, 0, 0);
		for (int x = xMin; x <= xMax; x++) {
			for (int y = yMin; y <= yMax; y++) {
				if (y < 0 || y > 254) {
					continue;
				}
				
				for (int z = zMin; z <= zMax; z++) {
					mutableBlockPos.setPos(x, y, z);
					final Block block = world.getBlockState(mutableBlockPos).getBlock();
					if (block instanceof BlockTransporterBeacon) {
						// count active beacons
						final TileEntity tileEntity = world.getTileEntity(mutableBlockPos);
						if ( tileEntity instanceof TileEntityTransporterBeacon
						  && ((TileEntityTransporterBeacon) tileEntity).isActive() ) {
							countBeacons++;
						}
						
					} else if (block instanceof BlockTransporterCore) {
						// count active transporters
						final TileEntity tileEntity = world.getTileEntity(mutableBlockPos);
						if ( tileEntity instanceof TileEntityTransporterCore
						  && ((TileEntityTransporterCore) tileEntity).isEnabled
						  && ((TileEntityTransporterCore) tileEntity).isConnected ) {
							countTransporters++;
							// remember scanners coordinates
							vScanners = ((TileEntityTransporterCore) tileEntity).vLocalScanners;
							// remember upgrades
							sumRangeUpgrades += ((TileEntityTransporterCore) tileEntity).getUpgradeCount(upgradeSlotRange);
							sumFocusUpgrades += ((TileEntityTransporterCore) tileEntity).getUpgradeCount(upgradeSlotFocus);
						}
					}
				}
			}
		}
		
		// compute values
		final FocusValues result = new FocusValues();
		if (countTransporters == 1) {
			result.vScanners = vScanners;
			result.countRangeUpgrades = sumRangeUpgrades;
			result.speed = WarpDriveConfig.TRANSPORTER_LOCKING_SPEED_AT_TRANSPORTER + sumFocusUpgrades * WarpDriveConfig.TRANSPORTER_LOCKING_STRENGTH_UPGRADE;
			result.strength = WarpDriveConfig.TRANSPORTER_LOCKING_STRENGTH_AT_TRANSPORTER + sumFocusUpgrades * WarpDriveConfig.TRANSPORTER_LOCKING_SPEED_UPGRADE;
		} else if (countBeacons > 0) {
			result.countRangeUpgrades = 0;
			result.speed = WarpDriveConfig.TRANSPORTER_LOCKING_SPEED_AT_BEACON;
			result.strength = WarpDriveConfig.TRANSPORTER_LOCKING_STRENGTH_AT_BEACON;
		} else {
			result.countRangeUpgrades = 0;
			result.speed = WarpDriveConfig.TRANSPORTER_LOCKING_SPEED_IN_WILDERNESS;
			result.strength = WarpDriveConfig.TRANSPORTER_LOCKING_STRENGTH_IN_WILDERNESS;
		}
		
		if (WarpDriveConfig.LOGGING_TRANSPORTER) {
			WarpDrive.logger.info(String.format("Transporter getFocusValueAtCoordinates %s gives range %d speed %.3f strength %.3f",
			                                    blockPos, result.countRangeUpgrades, result.speed, result.strength ));
		}
		
		return result;
	}
	
	private static boolean isJammedTrajectory(final World world, final BlockPos blockPosSource, final BlockPos blockPosDestination, final int beamFrequency) {
		// return default value if world isn't loaded
		if (world == null) {
			return false;
		}
		
		final Vec3d v3dPath = new Vec3d(blockPosDestination.getX() - blockPosSource.getX(),
		                                blockPosDestination.getY() - blockPosSource.getY(),
		                                blockPosDestination.getZ() - blockPosSource.getZ() );
		final int length = (int) Math.ceil(3 * v3dPath.length());
		final Vec3d v3Delta = new Vec3d(v3dPath.x / (double) length, v3dPath.y / (double) length, v3dPath.z / (double) length);
		
		// scan along given trajectory
		// note: we use our own Vector3 as a mutable Vec3d to save from memory fragmentation/lag
		final Vector3 v3Current = new Vector3(blockPosSource.getX() + 0.5D,
		                                      blockPosSource.getY() + 0.5D,
		                                      blockPosSource.getZ() + 0.5D );
		final BlockPos.Mutable blockPosCurrent = new BlockPos.Mutable(blockPosSource);
		final BlockPos.Mutable blockPosPrevious = new BlockPos.Mutable(blockPosCurrent);
		for (int step = 0; step < length; step++) {
			v3Current.translate(v3Delta);
			blockPosCurrent.setPos((int) Math.round(v3Current.x),
			                       (int) Math.round(v3Current.y),
			                       (int) Math.round(v3Current.z) );
			// skip repeating coordinates
			if (blockPosCurrent.equals(blockPosPrevious)) {
				continue;
			}
			if (isJammedCoordinate(world, blockPosCurrent, beamFrequency)) return true;
			
			// remember this coordinates
			blockPosPrevious.setPos(blockPosCurrent);
		}
		return false;
	}
	private static boolean isJammedCoordinate(final World world, final BlockPos blockPos, final int beamFrequency) {
		// check block blacklist for blinking
		final Block block = world.getBlockState(blockPos).getBlock();
		if (Dictionary.BLOCKS_NOBLINK.contains(block)) {
			if (WarpDriveConfig.LOGGING_TRANSPORTER) {
				WarpDrive.logger.info(String.format("Transporter beam jammed by blacklisted block %s", block));
			}
			return true;
		}
		
		// allow passing through force fields with same beam frequency
		if (block instanceof BlockForceField) {
			final TileEntity tileEntity = world.getTileEntity(blockPos);
			if (tileEntity instanceof TileEntityForceField) {
				final ForceFieldSetup forceFieldSetup = ((TileEntityForceField) tileEntity).getForceFieldSetup();
				if (forceFieldSetup == null) {
					// projector not loaded yet, consider it jammed by default
					WarpDrive.logger.warn(String.format("Transporter beam jammed by non-loaded force field projector at %s", tileEntity));
					return true;
				}
				if (forceFieldSetup.beamFrequency != beamFrequency) {
					// jammed by invalid beam frequency
					if (WarpDriveConfig.LOGGING_TRANSPORTER) {
						WarpDrive.logger.info(String.format("Transporter beam jammed by invalid frequency against %s", tileEntity));
					}
					return true;
				}
			}
		}
		return false;
	}
	
	private int getEnergyRequired(final EnumTransporterState transporterState) {
		switch (transporterState) {
		case DISABLED:
		case IDLE:
		default:
			return 0;
			
		case ACQUIRING:
			return (int) Math.ceil(energyCostForAcquiring * energyFactor);
			
		case ENERGIZING:
			return (int) Math.ceil(energyCostForEnergizing * energyFactor / WarpDriveConfig.TRANSPORTER_ENERGIZING_CHARGING_TICKS);
		}
	}
	
	private static void applyTeleportationDamages(final boolean isPreTeleportation, final Entity entity, final double strength) {
		// skip invulnerable
		if ( !entity.isAlive()
		  || entity.isInvulnerableTo(WarpDrive.damageTeleportation) ) {
			return;
		}
		
		// add bonus if transport was successful
		final double strengthToUse = isPreTeleportation ? strength : Math.random() * WarpDriveConfig.TRANSPORTER_ENERGIZING_SUCCESS_LOCK_BONUS + strength;
		
		final double strengthSafe = 0.95D;
		final double strengthMaxDamage = 0.65D;
		final double strengthNoDamage = 0.10D;
		if (strengthToUse > strengthSafe) {
			return;
		}
		final double damageNormalized = (strengthSafe - strengthToUse) / (strengthSafe - strengthMaxDamage);
		final double damageMax = isPreTeleportation ? WarpDriveConfig.TRANSPORTER_ENERGIZING_FAILURE_MAX_DAMAGE : WarpDriveConfig.TRANSPORTER_ENERGIZING_SUCCESS_MAX_DAMAGE;
		// final double damageAmount = Commons.clamp(1.0D, 1000.0D, Math.pow(10.0D, 10.0D * damageNormalized));
		final double damageAmount = Commons.clamp(1.0D, 1000.0D, damageMax * damageNormalized);
		
		if (WarpDriveConfig.LOGGING_TRANSPORTER) {
			WarpDrive.logger.info(String.format("Applying teleportation damages %s transport with %.3f strength, %.2f damage towards %s",
			                                    isPreTeleportation ? "pre" : "post",
			                                    strengthToUse,
			                                    damageAmount,
			                                    entity));
		}
		
		if (entity instanceof LivingEntity) {
			entity.attackEntityFrom(WarpDrive.damageTeleportation, (float) damageAmount);
			final boolean isCreative = (entity instanceof PlayerEntity) && ((PlayerEntity) entity).isCreative();
			if (!isCreative) {
				if (isPreTeleportation) {
					// add 1s nausea
					((LivingEntity) entity).addPotionEffect(new EffectInstance(Effects.NAUSEA, 20));
				} else {
					// add 5s poison
					((LivingEntity) entity).addPotionEffect(new EffectInstance(Effects.POISON, 5 * 20));
				}
			}
		} else if (strengthToUse > strengthNoDamage) {
			// add lava blade at location
			final BlockPos blockPos = new BlockPos(entity);
			if (entity.world.isAirBlock(blockPos)) {
				entity.world.setBlockState(blockPos, Blocks.LAVA.getDefaultState().with(FlowingFluidBlock.LEVEL, 6), 2);
			}
		}
	}
	
	private EntityValues updateEntitiesToEnergize(final ServerWorld worldRemote) {
		final int countLocalScanners = vLocalScanners == null ? 0 : vLocalScanners.size();
		final int countScanners = vRemoteScanners == null ? countLocalScanners : Math.min(countLocalScanners, vRemoteScanners.size());
		
		final EntityValues entityValues = new EntityValues();
		
		// return default values unless we're acquiring or energizing
		if ( transporterState != EnumTransporterState.ENERGIZING
		  && transporterState != EnumTransporterState.ACQUIRING ) {
			entityValues.count = countScanners;
			entityValues.mass = 2L * countScanners;
			return entityValues;
		}
		
		// collect all candidates at local location
		final EntityValues entityValuesLocal = updateEntitiesOnScanners(world, vLocalScanners, countScanners, movingEntitiesLocal);
		
		// collect all candidates at remote location
		final EntityValues entityValuesRemote;
		if (vRemoteScanners != null) {
			entityValuesRemote = updateEntitiesOnScanners(worldRemote, vRemoteScanners, countScanners, movingEntitiesRemote);
		} else {
			entityValuesRemote = updateEntitiesInArea(worldRemote, globalPositionRemote, countScanners, movingEntitiesRemote);
		}
		entityValues.count = entityValuesLocal.count + entityValuesRemote.count;
		entityValues.mass  = entityValuesLocal.mass  + entityValuesRemote.mass;
		return entityValues;
	}
	
	private static EntityValues updateEntitiesOnScanners(final World world, final ArrayList<BlockPos> vScanners, final int countScanners,
	                                                     final HashMap<Integer, MovingEntity> movingEntities) {
		final double tolerance2 = WarpDriveConfig.TRANSPORTER_ENERGIZING_ENTITY_MOVEMENT_TOLERANCE_BLOCKS
		                        * WarpDriveConfig.TRANSPORTER_ENERGIZING_ENTITY_MOVEMENT_TOLERANCE_BLOCKS;
		// remember entities allocated so we don't double grab them
		final HashSet<Entity> entitiesOnScanners = new HashSet<>(countScanners);
		final EntityValues entityValues = new EntityValues();
		
		// allocate an entity to each scanner
		for (int index = 0; index < countScanners; index++) {
			MovingEntity movingEntity = movingEntities.get(index);
			// skip marked spots
			if (movingEntity == MovingEntity.INVALID) {
				continue;
			}
			
			// validate existing entity
			if (movingEntity != null) {
				final Entity entity = movingEntity.getEntity();
				if ( entity == null
				  || !entity.isAlive() ) {// no longer valid => search a new one, no energy lost
					movingEntity = null;
					
				} else {
					final double distance2 = movingEntity.getDistanceMoved_square();
					if (distance2 > tolerance2) {// entity moved too much => damage existing one, loose this slot
						final double strength = Math.sqrt(distance2) / Math.sqrt(tolerance2) / 2.0D;
						applyTeleportationDamages(true, entity, strength);
						movingEntity = MovingEntity.INVALID;
					}
				}
			}
			
			// grab another entity
			if ( movingEntity == null
			  && vScanners != null ) {
				final Entity entityOnScanner = getCandidateEntityOnScanner(world, vScanners.get(index), entitiesOnScanners);
				if (entityOnScanner != null) {
					movingEntity = new MovingEntity(entityOnScanner);
					entitiesOnScanners.add(entityOnScanner);
				}
			}
			
			// save updated entity
			if (movingEntity == null) {// no candidate => mark the spot so we don't grab new ones while energizing
				movingEntities.put(index, MovingEntity.INVALID);
			} else {
				movingEntities.put(index, movingEntity);
				entityValues.count++;
				entityValues.mass += movingEntity.getMassFactor();
			}
		}
		
		return entityValues;
	}
	
	private static EntityValues updateEntitiesInArea(final World world, final GlobalPosition globalPosition, final int countScanners,
	                                                 final HashMap<Integer, MovingEntity> movingEntities) {
		final double tolerance2 = WarpDriveConfig.TRANSPORTER_ENERGIZING_ENTITY_MOVEMENT_TOLERANCE_BLOCKS
		                        * WarpDriveConfig.TRANSPORTER_ENERGIZING_ENTITY_MOVEMENT_TOLERANCE_BLOCKS;
		final LinkedHashSet<Entity> entities = getCandidateEntitiesInArea(world, globalPosition);
		final EntityValues entityValues = new EntityValues();
		
		// allocate an entity to each scanner
		for (int index = 0; index < countScanners; index++) {
			MovingEntity movingEntity = movingEntities.get(index);
			// skip marked spots
			if (movingEntity == MovingEntity.INVALID) {
				continue;
			}
			
			// validate existing entity
			if (movingEntity != null) {
				final Entity entity = movingEntity.getEntity();
				if ( entity == null
				  || !entity.isAlive() ) {// no longer valid => search a new one, no energy lost
					movingEntity = null;
					
				} else if (entities.contains(entity)) {// still in the list
					final double distance2 = movingEntity.getDistanceMoved_square();
					if (distance2 > tolerance2) {// entity moved too much => damage existing one, loose this slot
						final double strength = Math.sqrt(distance2) / Math.sqrt(tolerance2) / 2.0D;
						applyTeleportationDamages(true, entity, strength);
						movingEntity = MovingEntity.INVALID;
					} else {// still valid => remove it from candidates to avoid double grab
						entities.remove(entity);
					}
				} else {// exited the area or shifted in order => invalidate the spot
					movingEntity = MovingEntity.INVALID;
				}
			}
			
			// grab another entity
			if ( movingEntity == null
			  && !entities.isEmpty() ) {
				final Iterator<Entity> entityIterable = entities.iterator();
				final Entity entity = entityIterable.next();
				if (entity != null) {
					movingEntity = new MovingEntity(entity);
					entityIterable.remove();
				}
			}
			
			// save updated entity
			if (movingEntity == null) {// no candidate => mark the spot so we don't grab new ones while energizing
				movingEntities.put(index, MovingEntity.INVALID);
			} else {
				movingEntities.put(index, movingEntity);
				entityValues.count++;
				entityValues.mass += movingEntity.getMassFactor();
			}
		}
		
		return entityValues;
	}
	
	private static Entity getCandidateEntityOnScanner(final World world, final BlockPos blockPos, final HashSet<Entity> entitiesOnScanners) {
		if ( blockPos == null
		  || world == null ) {
			return null;
		}
		
		final AxisAlignedBB aabb = new AxisAlignedBB(
				blockPos.getX() - 0.05D,
				blockPos.getY() - 1.00D,
				blockPos.getZ() - 0.05D,
				blockPos.getX() + 1.05D,
				blockPos.getY() + 2.00D,
				blockPos.getZ() + 1.05D);
		
		final List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(null, aabb);
		Entity entityReturn = null;
		int countEntities = 0;
		for (final Entity entity : entities) {
			// (particle effects are client side only, no need to filter them out)
			
			// skip blacklisted ids
			if (Dictionary.isLeftBehind(entity)) {
				if (WarpDriveConfig.LOGGING_TRANSPORTER) {
					WarpDrive.logger.info(String.format("Entity is not valid for transportation (id %s) %s",
					                                    Dictionary.getId(entity),
					                                    entity ));
				}
				continue;
			}
			
			// skip already attached entities
			if (entitiesOnScanners.contains(entity)) {
				continue;
			}
			
			// keep it
			entityReturn = entity;
			countEntities++;
		}
		
		// only accept a single entity in the area
		if (countEntities > 1) {
			PacketHandler.sendSpawnParticlePacket(world, "jammed", (byte) 5,
			                                      new Vector3(blockPos.getX() + 0.5D, blockPos.getY() + 1.5D, blockPos.getZ() + 0.5D),
			                                      new Vector3(0.0D, 0.0D, 0.0D),
			                                      1.0F, 1.0F, 1.0F,
			                                      1.0F, 1.0F, 1.0F,
			                                      32);
			return null;
		}
		return entityReturn;
	}
	
	private static LinkedHashSet<Entity> getCandidateEntitiesInArea(final World world, final GlobalPosition globalPosition) {
		if (world == null) {
			return new LinkedHashSet<>(0);
		}
		
		final AxisAlignedBB aabb = new AxisAlignedBB(
			globalPosition.x - WarpDriveConfig.TRANSPORTER_ENTITY_GRAB_RADIUS_BLOCKS,
			globalPosition.y - 1.0D,
			globalPosition.z - WarpDriveConfig.TRANSPORTER_ENTITY_GRAB_RADIUS_BLOCKS,
			globalPosition.x + WarpDriveConfig.TRANSPORTER_ENTITY_GRAB_RADIUS_BLOCKS + 1.0D,
			globalPosition.y + 2.0D,
			globalPosition.z + WarpDriveConfig.TRANSPORTER_ENTITY_GRAB_RADIUS_BLOCKS + 1.0D);
		
		final List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(null, aabb);
		final LinkedHashSet<Entity> entitiesReturn = new LinkedHashSet<>(entities.size());
		for (final Entity entity : entities) {
			// (particle effects are client side only, no need to filter them out)
			
			// skip blacklisted ids
			if (Dictionary.isLeftBehind(entity)) {
				if (WarpDriveConfig.LOGGING_TRANSPORTER) {
					WarpDrive.logger.info(String.format("Entity is not valid for transportation (id %s) %s", 
					                                    Dictionary.getId(entity),
					                                    entity ));
				}
				continue;
			}
			
			// keep it
			entitiesReturn.add(entity);
		}
		
		return entitiesReturn;
	}
	
	@Override
	protected void onUpgradeChanged(@Nonnull final UpgradeSlot upgradeSlot, final int countNew, final boolean isAdded) {
		if (upgradeSlot == upgradeSlotEnergyStorage) {
			refreshEnergyParameters();
		}
		super.onUpgradeChanged(upgradeSlot, countNew, isAdded);
	}
	
	private void refreshEnergyParameters() {
		final int energyUpgrades = getUpgradeCount(upgradeSlotEnergyStorage);
		energy_setParameters(WarpDriveConfig.TRANSPORTER_MAX_ENERGY_STORED + (long) energyUpgrades * WarpDriveConfig.TRANSPORTER_ENERGY_STORED_UPGRADE_BONUS,
		                     4096, 0,
		                     "HV", 2, "HV", 0);
	}
	
	@Override
	public boolean energy_canInput(final Direction from) {
		return from != Direction.UP;
	}
	
	@Nonnull
	@Override
	public CompoundNBT write(@Nonnull CompoundNBT tagCompound) {
		tagCompound = super.write(tagCompound);
		
		if ( vLocalScanners != null
		  && vLocalContainments != null ) {
			final ListNBT tagListScanners = new ListNBT();
			for (final BlockPos vScanner : vLocalScanners) {
				final CompoundNBT tagCompoundScanner = Commons.writeBlockPosToNBT(vScanner, new CompoundNBT());
				tagListScanners.add(tagCompoundScanner);
			}
			tagCompound.put("scanners", tagListScanners);
			
			final ListNBT tagListContainments = new ListNBT();
			for (final BlockPos vContainment : vLocalContainments) {
				final CompoundNBT tagCompoundContainment = Commons.writeBlockPosToNBT(vContainment, new CompoundNBT());
				tagListContainments.add(tagCompoundContainment);
			}
			tagCompound.put("containments", tagListContainments);
		}
		
		tagCompound.putInt(IBeamFrequency.BEAM_FREQUENCY_TAG, beamFrequency);
		tagCompound.putBoolean("isLockRequested", isLockRequested);
		tagCompound.putBoolean("isEnergizeRequested", isEnergizeRequested);
		
		CompoundNBT tagRemoteLocation = new CompoundNBT();
		if (remoteLocationRequested instanceof UUID) {
			tagRemoteLocation.putLong(ICoreSignature.UUID_MOST_TAG, ((UUID) remoteLocationRequested).getMostSignificantBits());
			tagRemoteLocation.putLong(ICoreSignature.UUID_LEAST_TAG, ((UUID) remoteLocationRequested).getLeastSignificantBits());
		} else if (remoteLocationRequested instanceof VectorI) {
			tagRemoteLocation = ((VectorI) remoteLocationRequested).write(tagRemoteLocation);
		} else if (remoteLocationRequested instanceof String) {
			tagRemoteLocation.putString("playerName", (String) remoteLocationRequested);
		}
		tagCompound.put("remoteLocation", tagRemoteLocation);
		
		tagCompound.putDouble("energyFactor", energyFactor);
		tagCompound.putDouble("lockStrengthActual", lockStrengthActual);
		tagCompound.putInt("tickCooldown", tickCooldown);
		
		tagCompound.putString("state", transporterState.toString());
		
		return tagCompound;
	}
	
	@Override
	public void read(@Nonnull final CompoundNBT tagCompound) {
		super.read(tagCompound);
		
		if ( tagCompound.contains("scanners", Constants.NBT.TAG_LIST)
		  && tagCompound.contains("containments", Constants.NBT.TAG_LIST)) {
			final ListNBT tagListScanners = (ListNBT) tagCompound.get("scanners");
			assert tagListScanners != null;
			final ArrayList<BlockPos> vScanners = new ArrayList<>(tagListScanners.size());
			for (int indexScanner = 0; indexScanner < tagListScanners.size(); indexScanner++) {
				final BlockPos vScanner = Commons.createBlockPosFromNBT(tagListScanners.getCompound(indexScanner));
				vScanners.add(vScanner);
			}
			
			final ListNBT tagListContainments = (ListNBT) tagCompound.get("containments");
			assert tagListContainments != null;
			final ArrayList<BlockPos> vContainments = new ArrayList<>(tagListContainments.size());
			for (int indexContainment = 0; indexContainment < tagListContainments.size(); indexContainment++) {
				final BlockPos vContainment = Commons.createBlockPosFromNBT(tagListContainments.getCompound(indexContainment));
				vContainments.add(vContainment);
			}
			setLocalScanners(vScanners, vContainments);
		}
		
		beamFrequency = tagCompound.getInt(IBeamFrequency.BEAM_FREQUENCY_TAG);
		isLockRequested = tagCompound.getBoolean("isLockRequested");
		isEnergizeRequested = tagCompound.getBoolean("isEnergizeRequested");
		
		final INBT tagRemoteLocation = tagCompound.get("remoteLocation");
		if (tagRemoteLocation instanceof CompoundNBT) {
			final CompoundNBT tagCompoundRemoteLocation = (CompoundNBT) tagRemoteLocation;
			if (tagCompoundRemoteLocation.contains(ICoreSignature.UUID_MOST_TAG)) {
				remoteLocationRequested = new UUID(
						tagCompoundRemoteLocation.getLong(ICoreSignature.UUID_MOST_TAG),
						tagCompoundRemoteLocation.getLong(ICoreSignature.UUID_LEAST_TAG));
				
			} else if (tagCompoundRemoteLocation.contains("x")) {
				remoteLocationRequested = new VectorI();
				((VectorI) remoteLocationRequested).read(tagCompoundRemoteLocation);
				
			} else if (tagCompoundRemoteLocation.contains("playerName")) {
				remoteLocationRequested = tagCompoundRemoteLocation.getString("playerName");
			}
		}
		
		energyFactor = Commons.clamp(1, WarpDriveConfig.TRANSPORTER_ENERGIZING_MAX_ENERGY_FACTOR, tagCompound.getDouble("energyFactor"));
		lockStrengthActual = tagCompound.getDouble("lockStrengthActual");
		tickCooldown = tagCompound.getInt("tickCooldown");
		
		try {
			transporterState = EnumTransporterState.valueOf(tagCompound.getString("state"));
		} catch (final IllegalArgumentException exception) {
			transporterState = EnumTransporterState.DISABLED;
		}
	}
	
	@Nonnull
	@Override
	public CompoundNBT getUpdateTag() {
		final CompoundNBT tagCompound = super.getUpdateTag();
		
		tagCompound.remove(ICoreSignature.UUID_MOST_TAG);
		tagCompound.remove(ICoreSignature.UUID_LEAST_TAG);
		tagCompound.remove(IBeamFrequency.BEAM_FREQUENCY_TAG);
		
		return tagCompound;
	}
	
	@Override
	public CompoundNBT writeItemDropNBT(CompoundNBT tagCompound) {
		tagCompound = super.writeItemDropNBT(tagCompound);
		
		return tagCompound;
	}
	
	// Common OC/CC methods
	@Override
	public String[] name(@Nonnull final Object[] arguments) {
		final String name_old = name;
		super.name(arguments);
		if (!name_old.equals(name)) {
			uuid = UUID.randomUUID();
		}
		return new String[] { name, uuid == null ? null : uuid.toString() };
	}
	
	public Object[] beamFrequency(@Nonnull final Object[] arguments) {
		if (arguments.length == 1) {
			setBeamFrequency(Commons.toInt(arguments[0]));
		}
		return new Integer[] { getBeamFrequency() };
	}
	
	@Override
	public Object[] state() {
		final long energy = energy_getEnergyStored();
		final String status = getStatusHeaderInPureText();
		final String state = isJammed ? reasonJammed : tickCooldown > 0 ? String.format("Cooling down %d s", Math.round(tickCooldown / 20.0F)) : transporterState.getName();
		return new Object[] { status, state, isConnected, isEnabled, isJammed, energy, lockStrengthActual };
	}
	
	@Override
	public Object[] remoteLocation(@Nonnull final Object[] arguments) {
		if (arguments.length == 3) {
			if (remoteLocationRequested instanceof VectorI) {// already using direct coordinates
				final VectorI vNew = computer_getVectorI((VectorI) remoteLocationRequested, arguments);
				if (!vNew.equals(remoteLocationRequested)) {
					remoteLocationRequested = vNew;
					markDirtyParameters();
				}
				
			} else {
				final VectorI vNew = computer_getVectorI(null, arguments);
				if (vNew != null) {
					remoteLocationRequested = vNew;
					markDirtyParameters();
				}
			}
		} else if (arguments.length == 1 && arguments[0] != null) {
			// is it a UUID?
			final UUID uuidNew = computer_getUUID(null, arguments);
			if (uuidNew != null) {
				if (remoteLocationRequested instanceof UUID) {
					if (!uuidNew.equals(remoteLocationRequested)) {// replacing existing UUID
						remoteLocationRequested = uuidNew;
						markDirtyParameters();
					}
				} else {
					remoteLocationRequested = uuidNew;
					markDirtyParameters();
				}
			} else {// new player name
				final String playerNameNew = (String) arguments[0];
				if ( playerNameNew != null
				  && !playerNameNew.equals(remoteLocationRequested) ) {
					remoteLocationRequested = playerNameNew;
					markDirtyParameters();
				}
			}
		}
		
		// return (updated) value
		if (remoteLocationRequested instanceof VectorI) {
			final VectorI vRemoteLocation = (VectorI) remoteLocationRequested;
			return new Integer[] { vRemoteLocation.x, vRemoteLocation.y, vRemoteLocation.z };
		}
		return new Object[] { remoteLocationRequested == null ? null : remoteLocationRequested.toString() };
	}
	
	@Override
	public Boolean[] lock(@Nonnull final Object[] arguments) {
		if (arguments.length == 1 && arguments[0] != null) {
			isLockRequested = Commons.toBool(arguments[0]);
			markDirty();
		}
		return new Boolean[] { isLockRequested };
	}
	
	@Override
	public Double[] energyFactor(@Nonnull final Object[] arguments) {
		try {
			if (arguments.length >= 1) {
				energyFactor = Commons.clamp(1, WarpDriveConfig.TRANSPORTER_ENERGIZING_MAX_ENERGY_FACTOR, Commons.toDouble(arguments[0]));
			}
		} catch (final NumberFormatException exception) {
			// ignore
		}
		
		return new Double[] { energyFactor };
	}
	
	@Override
	public Double[] getLockStrength() {
		return new Double[] { lockStrengthActual };
	}
	
	@Override
	public Object[] getEnergyRequired() {
		final String units = energy_getDisplayUnits();
		return new Object[] {
				true,
				EnergyWrapper.convert(getEnergyRequired(EnumTransporterState.ACQUIRING), units),
				EnergyWrapper.convert(getEnergyRequired(EnumTransporterState.ENERGIZING), units) };
	}
	
	@Override
	public Boolean[] energize(@Nonnull final Object[] arguments) {
		if (arguments.length == 1 && arguments[0] != null) {
			isEnergizeRequested = Commons.toBool(arguments[0]);
			markDirty();
		}
		return new Boolean[] { isEnergizeRequested };
	}
	
	// OpenComputers callback methods
	@Callback(direct = true)
	public Object[] beamFrequency(final Context context, final Arguments arguments) {
		return beamFrequency(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	public Object[] state(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return state();
	}
	
	@Callback(direct = true)
	public Object[] remoteLocation(final Context context, final Arguments arguments) {
		return remoteLocation(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	public Object[] lock(final Context context, final Arguments arguments) {
		return lock(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	public Object[] energyFactor(final Context context, final Arguments arguments) {
		return energyFactor(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	public Object[] getLockStrength(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getLockStrength();
	}
	
	@Callback(direct = true)
	public Object[] energize(final Context context, final Arguments arguments) {
		return energize(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	// ComputerCraft IDynamicPeripheral methods
	@Override
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "beamFrequency":
			return beamFrequency(arguments);
			
		case "state":
			return state();
		
		case "remoteLocation":
			return remoteLocation(arguments);
		
		case "lock":
			return lock(arguments);
		
		case "energyFactor":
			return energyFactor(arguments);
		
		case "getLockStrength":
			return getLockStrength();
		
		case "energize":
			return energize(arguments);
			
		default:
			return super.CC_callMethod(methodName, arguments);
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s '%s' %s Beam %d %s",
		                     getClass().getSimpleName(),
		                     name, uuid,
		                     beamFrequency,
		                     Commons.format(world, pos));
	}
}