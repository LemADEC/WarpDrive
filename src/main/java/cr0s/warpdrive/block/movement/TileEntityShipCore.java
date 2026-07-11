package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.block.TileEntitySecurityStation;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.EventWarpDrive.Ship.PreJump;
import cr0s.warpdrive.api.IGlobalRegionProvider;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.api.computer.IMultiBlockCoreOrController;
import cr0s.warpdrive.api.computer.IMultiBlockCore;
import cr0s.warpdrive.block.detection.BlockWarpIsolation;
import cr0s.warpdrive.config.ShipMovementCosts;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.EnergyWrapper;
import cr0s.warpdrive.data.EnumGlobalRegionType;
import cr0s.warpdrive.data.EnumShipAutopilotMode;
import cr0s.warpdrive.data.EnumShipAutopilotStatus;
import cr0s.warpdrive.data.EnumShipCommand;
import cr0s.warpdrive.data.EnumShipCoreState;
import cr0s.warpdrive.data.EnumShipMovementType;
import cr0s.warpdrive.data.GlobalRegionManager;
import cr0s.warpdrive.data.SoundEvents;
import cr0s.warpdrive.data.Transformation;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.data.VectorI;
import cr0s.warpdrive.event.JumpSequencer;
import cr0s.warpdrive.render.EntityFXBoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityShipCore extends TileEntityAbstractShipController implements IGlobalRegionProvider, IMultiBlockCore {
	
	private static final int LOG_INTERVAL_TICKS = 20 * 180;
	private static final int BOUNDING_BOX_INTERVAL_TICKS = 60;
	
	// persistent properties
	public EnumFacing facing;
	private double isolationRate = 0.0D;
	private final Set<BlockPos> blockPosShipControllers = new CopyOnWriteArraySet<>();
	private int ticksCooldown = 0;
	private int warmupTime_ticks = 0;
	protected int jumpCount = 0;
	
	// computed properties
	public int maxX, maxY, maxZ;
	public int minX, minY, minZ;
	private AxisAlignedBB cache_aabbArea;
	protected boolean showBoundingBox = false;
	private int ticksBoundingBoxUpdate = 0;
	
	private EnumShipCoreState stateCurrent = EnumShipCoreState.IDLE;
	private EnumShipCommand commandCurrent = EnumShipCommand.IDLE;
	
	private long timeLastShipScanDone = -1;
	private ShipScanner shipScanner = null;
	public int shipMass;
	public int shipVolume;
	private int shipScanSignature = 0;
	private VectorI shipScanSecurityStationLocal = null;
	private BlockPos posSecurityStation = null;
	private WeakReference<TileEntitySecurityStation> weakTileEntitySecurityStation = null;
	private boolean isShipScanValid = false;
	protected WarpDriveText textShipScanIssues = new WarpDriveText();
	
	private EnumShipMovementType shipMovementType;
	private ShipMovementCosts shipMovementCosts;
	private String navigationTargetId = "";
	private String navigationEngagedTargetId = "";
	private String navigationHeavyCacheKey = "";
	private NBTTagCompound navigationHeavyCache = null;

	// live status totals (for client warmup/cooldown progress bars)
	private int warmupTotal_ticks = 0;
	private int cooldownTotal_ticks = 0;

	// autopilot
	private static final int AUTOPILOT_MAX_RETRIES = 3;
	private static final int AUTOPILOT_MAX_LEGS = 64;
	private static final int AUTOPILOT_BACKOFF_TICKS = 40;
	private static final int AUTOPILOT_ENERGY_RECHECK_TICKS = 20;
	private EnumShipAutopilotMode autopilotMode = EnumShipAutopilotMode.SAFETY_STOPS;
	private EnumShipAutopilotStatus autopilotStatus = EnumShipAutopilotStatus.IDLE;
	private long autopilotWaitUntilTick = 0L;
	private int autopilotRetryCount = 0;
	private int autopilotLegsExecuted = 0;
	private String autopilotLastErrorKey = "";
	private boolean autopilotSingleStep = false;
	
	private long distanceSquared = 0;
	private boolean isCooldownReported = false;
	private boolean isMotionSicknessApplied = false;
	private boolean isSoundPlayed = false;
	private boolean isWarmupReported = false;
	protected int randomWarmupAddition_ticks = 0;
	
	private int logTicks = 120;
	
	private int isolationBlocksCount = 0;
	
	
	public TileEntityShipCore() {
		super();
		
		peripheralName = "warpdriveShipCore";
		// addMethods(new String[] {});
		CC_scripts = Collections.singletonList("startup");
	}
	
	@SideOnly(Side.CLIENT)
	private void doShowBoundingBox() {
		ticksBoundingBoxUpdate--;
		if (ticksBoundingBoxUpdate > 0) {
			return;
		}
		ticksBoundingBoxUpdate = BOUNDING_BOX_INTERVAL_TICKS;
		
		// core coordinates
		final Vector3 vector3 = new Vector3(this);
		vector3.translate(0.5D);
		
		// bounding box
		final Vector3 vMin = new Vector3(minX - 0.0D, minY - 0.0D, minZ - 0.0D);
		final Vector3 vMax = new Vector3(maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D);
		FMLClientHandler.instance().getClient().effectRenderer.addEffect(
				new EntityFXBoundingBox(world, vector3,
				                        vMin,
				                        vMax,
				                        1.0F, 0.8F, 0.3F, BOUNDING_BOX_INTERVAL_TICKS + 1) );
		
		// security station
		if (posSecurityStation != null) {
			final Vector3 vMinSecurityStation = new Vector3(posSecurityStation.getX() - 0.0D, posSecurityStation.getY() - 0.0D, posSecurityStation.getZ() - 0.0D);
			final Vector3 vMaxSecurityStation = new Vector3(posSecurityStation.getX() + 1.0D, posSecurityStation.getY() + 1.0D, posSecurityStation.getZ() + 1.0D);
			FMLClientHandler.instance().getClient().effectRenderer.addEffect(
					new EntityFXBoundingBox(world, vector3,
					                        vMinSecurityStation,
					                        vMaxSecurityStation,
					                        1.0F, 0.2F, 0.9F, BOUNDING_BOX_INTERVAL_TICKS + 1) );
		}
		
		// target location
		final VectorI vMovement = getMovement();
		if (vMovement.getMagnitudeSquared() > 0L) {
			final VectorI movement = getMovement();
			final VectorI shipSize = new VectorI(getFront() + 1 + getBack(),
			                                     getUp()    + 1 + getDown(),
			                                     getRight() + 1 + getLeft());
			final int maxDistance = 256;
			if (Math.abs(movement.x) - shipSize.x > maxDistance) {
				movement.x = (int) Math.signum(movement.x) * (shipSize.x + maxDistance);
			}
			if (Math.abs(movement.y) - shipSize.y > maxDistance) {
				movement.y = (int) Math.signum(movement.y) * (shipSize.y + maxDistance);
			}
			if (Math.abs(movement.z) - shipSize.z > maxDistance) {
				movement.z = (int) Math.signum(movement.z) * (shipSize.z + maxDistance);
			}
			final IBlockState blockState = world.getBlockState(pos);
			if (!(blockState.getBlock() instanceof BlockShipCore)) {
				if (Commons.throttleMe("InvalidBlockToRenderBondingBox")) {
					WarpDrive.logger.warn(String.format("Invalid block %s while trying to render ship bounding box with tile entity %s", blockState, this));
				}
				showBoundingBox = false;
				return;
			}
			facing = blockState.getValue(BlockProperties.FACING_HORIZONTAL);
			final int moveX = facing.getXOffset() * movement.x - facing.getZOffset() * movement.z;
			final int moveY = movement.y;
			final int moveZ = facing.getZOffset() * movement.x + facing.getXOffset() * movement.z;
			final Transformation transformation = new Transformation(this, moveX, moveY, moveZ, getRotationSteps());
			final Vec3d vMinTarget = transformation.apply(vMin.x, vMin.y, vMin.z);
			final Vec3d vMaxTarget = transformation.apply(vMax.x, vMax.y, vMax.z);
			
			FMLClientHandler.instance().getClient().effectRenderer.addEffect(
					new EntityFXBoundingBox(world, vector3,
					                        new Vector3(Math.min(vMinTarget.x, vMaxTarget.x), Math.min(vMinTarget.y, vMaxTarget.y), Math.min(vMinTarget.z, vMaxTarget.z)),
					                        new Vector3(Math.max(vMinTarget.x, vMaxTarget.x), Math.max(vMinTarget.y, vMaxTarget.y), Math.max(vMinTarget.z, vMaxTarget.z)),
					                        0.3F, 0.8F, 1.0F, BOUNDING_BOX_INTERVAL_TICKS + 1) );
		}
	}

	@Override
	protected void onConstructed() {
		super.onConstructed();
		
		energy_setParameters(WarpDriveConfig.SHIP_MAX_ENERGY_STORED_BY_TIER[enumTier.getIndex()],
		                     65536, 0,
		                     "EV", 2, "EV", 0);
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			if (showBoundingBox) {
				doShowBoundingBox();
			}
			return;
		}
		
		// always cool down
		if (ticksCooldown > 0) {
			ticksCooldown--;
			
			// report cool down time when a command is requested
			if ( isEnabled
			  && isCommandConfirmed
			  && enumShipCommand.isMovement()
			  && ticksCooldown % 20 == 0 ) {
				final int seconds = ticksCooldown / 20;
				if (!isCooldownReported || (seconds < 5) || ((seconds < 30) && (seconds % 5 == 0)) || (seconds % 10 == 0)) {
					isCooldownReported = true;
					Commons.messageToAllPlayersInArea(this, new WarpDriveText(null, "warpdrive.ship.guide.cooling_countdown",
					                                                          seconds));
				}
			}
			
			if (ticksCooldown == 0) {
				cooldownDone();
			}
		} else {
			isCooldownReported = false;
		}
		
		// enforce emergency stop
		if ( !isEnabled
		  || ( isCommandConfirmed
		    && enumShipCommand == EnumShipCommand.OFFLINE ) ) {
			stateCurrent = EnumShipCoreState.IDLE;
			commandCurrent = EnumShipCommand.OFFLINE;
		}
		
		// periodically log the ship state
		logTicks--;
		if (logTicks <= 0) {
			logTicks = LOG_INTERVAL_TICKS;
			if (WarpDriveConfig.LOGGING_JUMP) {
				WarpDrive.logger.info(String.format("%s, %s, %s, %d controllers, warm-up %d, cool down %d",
				                                    this,
				                                    stateCurrent,
				                                    isEnabled ? "Enabled" : "Disabled",
				                                    blockPosShipControllers.size(),
				                                    warmupTime_ticks,
				                                    ticksCooldown));
			}
		}
		
		// refresh rendering
		final boolean isActive = isEnabled
		                      && enumShipCommand != EnumShipCommand.OFFLINE;
		updateBlockState(null, BlockProperties.ACTIVE, isActive);
		
		// scan ship content progressively
		if (timeLastShipScanDone <= 0L) {
			timeLastShipScanDone = world.getTotalWorldTime();
			
			// validate ship side constrains before scanning
			if ( getBack() == 0 && getFront() == 0
			  && getLeft() == 0 && getRight() == 0
			  && getDown() == 0 && getUp() == 0 ) {
				textShipScanIssues = new WarpDriveText(Commons.getStyleWarning(), "warpdrive.ship.guide.no_dimension_set");
				isShipScanValid = false;
				shipScanSignature = 0;
				return;
			}
			if ( (getBack() + getFront()) > WarpDriveConfig.SHIP_SIZE_MAX_PER_SIDE_BY_TIER[enumTier.getIndex()]
			  || (getLeft() + getRight()) > WarpDriveConfig.SHIP_SIZE_MAX_PER_SIDE_BY_TIER[enumTier.getIndex()]
			  || (getDown() + getUp()   ) > WarpDriveConfig.SHIP_SIZE_MAX_PER_SIDE_BY_TIER[enumTier.getIndex()] ) {
				textShipScanIssues = new WarpDriveText(Commons.getStyleWarning(), "warpdrive.ship.guide.too_large_side_for_tier",
				                                       WarpDriveConfig.SHIP_SIZE_MAX_PER_SIDE_BY_TIER[enumTier.getIndex()]);
				isShipScanValid = false;
				shipScanSignature = 0;
				return;
			}
			
			shipScanner = new ShipScanner(world, pos, minX, minY, minZ, maxX, maxY, maxZ);
			if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
				WarpDrive.logger.info(String.format("%s scanning started",
				                                    this));
			}
		}
		if (shipScanner != null) {
			if (!shipScanner.tick()) {
				// still scanning => skip state handling
				return;
			}
			
			shipMass = shipScanner.mass;
			shipVolume = shipScanner.volume;
			if (posSecurityStation != shipScanner.posSecurityStation) {
				posSecurityStation = shipScanner.posSecurityStation;
				weakTileEntitySecurityStation = null;
			}
			shipScanner = null;
			if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
				WarpDrive.logger.info(String.format("%s scanning done: mass %d, volume %d, security station %s",
				                                    this, shipMass, shipVolume, posSecurityStation ));
			}
			
			// validate results
			boolean isUnlimited = false;
			final AxisAlignedBB axisalignedbb = new AxisAlignedBB(minX, minY, minZ, maxX + 0.99D, maxY + 0.99D, maxZ + 0.99D);
			final List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(null, axisalignedbb);
			for (final Entity entity : list) {
				if (!(entity instanceof EntityPlayer)) {
					continue;
				}
				
				final String playerName = entity.getName();
				for (final String nameUnlimited : WarpDriveConfig.SHIP_MASS_UNLIMITED_PLAYER_NAMES) {
					isUnlimited = isUnlimited || nameUnlimited.equals(playerName);
				}
			}
			if (!isUnlimited) {
				if ( shipMass > WarpDriveConfig.SHIP_MASS_MAX_ON_PLANET_SURFACE
				  && CelestialObjectManager.isPlanet(world, pos.getX(), pos.getZ()) ) {
					textShipScanIssues = new WarpDriveText(Commons.getStyleWarning(), "warpdrive.ship.guide.too_much_mass_for_planet",
					                                       WarpDriveConfig.SHIP_MASS_MAX_ON_PLANET_SURFACE, shipMass );
					isShipScanValid = false;
					shipScanSignature = 0;
					if (isEnabled) {
						commandDone(false, textShipScanIssues);
					}
					return;
				}
				if ( shipMass < WarpDriveConfig.SHIP_MASS_MIN_FOR_HYPERSPACE
				  && CelestialObjectManager.isInHyperspace(world, pos.getX(), pos.getZ()) ) {
					textShipScanIssues = new WarpDriveText(Commons.getStyleWarning(), "warpdrive.ship.guide.insufficient_mass_for_hyperspace",
					                                       WarpDriveConfig.SHIP_MASS_MIN_FOR_HYPERSPACE, shipMass );
					isShipScanValid = false;
					shipScanSignature = 0;
					if (isEnabled) {
						commandDone(false, textShipScanIssues);
					}
					return;
				}
				if (shipMass < WarpDriveConfig.SHIP_MASS_MIN_BY_TIER[enumTier.getIndex()]) {
					textShipScanIssues = new WarpDriveText(Commons.getStyleWarning(), "warpdrive.ship.guide.insufficient_mass_for_tier",
					                                       WarpDriveConfig.SHIP_MASS_MIN_BY_TIER[enumTier.getIndex()], shipMass );
					isShipScanValid = false;
					shipScanSignature = 0;
					if (isEnabled) {
						commandDone(false, textShipScanIssues);
					}
					return;
				}
				if (shipMass > WarpDriveConfig.SHIP_MASS_MAX_BY_TIER[enumTier.getIndex()]) {
					textShipScanIssues = new WarpDriveText(Commons.getStyleWarning(), "warpdrive.ship.guide.too_much_mass_for_tier",
					                                       WarpDriveConfig.SHIP_MASS_MAX_BY_TIER[enumTier.getIndex()], shipMass );
					isShipScanValid = false;
					shipScanSignature = 0;
					if (isEnabled) {
						commandDone(false, textShipScanIssues);
					}
					return;
				}
			}
			textShipScanIssues = new WarpDriveText();
			isShipScanValid = true;
			shipScanSignature = computeShipScanSignature();
			shipScanSecurityStationLocal = posSecurityStation == null ? null : toLocalOffset(posSecurityStation);
			invalidateNavigationCache();
		}
		
		// skip state handling while cooling down
		if (isCooling()) {
			return;
		}
		
		final WarpDriveText reason = new WarpDriveText();
		
		switch (stateCurrent) {
		case IDLE:
			if ( isEnabled
			  && isCommandConfirmed
			  && enumShipCommand.isMovement() ) {
				commandCurrent = enumShipCommand;
				stateCurrent = EnumShipCoreState.EXECUTE;
			}
			break;
		
		case EXECUTE:
			// (disabling will switch back to IDLE and clear variables)
			
			switch (commandCurrent) {
			case MANUAL:
			case HYPERDRIVE:
				// initiating jump
				if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
					WarpDrive.logger.info(String.format("%s state ONLINE -> initiating jump",
					                                    this));
				}
				
				// compute distance
				distanceSquared = getMovement().getMagnitudeSquared();
				// rescan ship mass/volume if it's too old
				if (timeLastShipScanDone + WarpDriveConfig.SHIP_VOLUME_SCAN_AGE_TOLERANCE_SECONDS * 20L < world.getTotalWorldTime()) {
					timeLastShipScanDone = -1;
					break;
				}
				
				Commons.messageToAllPlayersInArea(this, new WarpDriveText(null, "warpdrive.ship.guide.pre_jumping"));
				
				// update ship spatial parameters
				if (!isAssemblyValid) {
					commandDone(false, textValidityIssues);
					return;
				}
				
				// update movement parameters
				if (!validateShipMovementParameters(reason)) {
					commandDone(false, reason);
					return;
				}
				
				// compute random ticks to warm-up so it's harder to 'dup' items
				randomWarmupAddition_ticks = world.rand.nextInt(WarpDriveConfig.SHIP_WARMUP_RANDOM_TICKS);
				
				stateCurrent = EnumShipCoreState.WARMING_UP;
				warmupTime_ticks = shipMovementCosts.warmup_seconds * 20 + randomWarmupAddition_ticks;
				warmupTotal_ticks = Math.max(1, warmupTime_ticks);
				isMotionSicknessApplied = false;
				isSoundPlayed = false;
				isWarmupReported = false;
				break;
			
			default:
				WarpDrive.logger.error(String.format("%s Invalid controller command %s for current state %s",
				                                     this, enumShipCommand, stateCurrent));
				stateCurrent = EnumShipCoreState.IDLE;
				break;
			}
			break;
			
		case WARMING_UP:
			// Apply motion sickness as applicable
			if (shipMovementCosts.sickness_seconds > 0) {
				final int motionSicknessThreshold_ticks = shipMovementCosts.sickness_seconds * 20 - randomWarmupAddition_ticks / 4; 
				if ( !isMotionSicknessApplied
				   && motionSicknessThreshold_ticks >= warmupTime_ticks ) {
					if (WarpDriveConfig.LOGGING_JUMP) {
						WarpDrive.logger.info(this + " Giving warp sickness to on-board players");
					}
					makePlayersOnShipDrunk(shipMovementCosts.sickness_seconds * 20 + WarpDriveConfig.SHIP_WARMUP_RANDOM_TICKS);
					isMotionSicknessApplied = true;
				}
			}
			
			// Select best sound file and adjust offset
			final int soundThreshold;
			final SoundEvent soundEvent;
			if (shipMovementCosts.warmup_seconds < 10) {
				soundThreshold =  4 * 20 - randomWarmupAddition_ticks;
				soundEvent = SoundEvents.WARP_4_SECONDS;
			} else if (shipMovementCosts.warmup_seconds > 29) {
				soundThreshold = 30 * 20 - randomWarmupAddition_ticks;
				soundEvent = SoundEvents.WARP_30_SECONDS;
			} else {
				soundThreshold = 10 * 20 - randomWarmupAddition_ticks;
				soundEvent = SoundEvents.WARP_10_SECONDS;
			}
			
			if ( !isSoundPlayed
			  && soundThreshold >= warmupTime_ticks ) {
				if (WarpDriveConfig.LOGGING_JUMP) {
					WarpDrive.logger.info(this + " Playing sound effect '" + soundEvent + "' soundThreshold " + soundThreshold + " warmupTime " + warmupTime_ticks);
				}
				world.playSound(null, pos, soundEvent, SoundCategory.BLOCKS, 4.0F, 1.0F);
				isSoundPlayed = true;
			}
			
			if (warmupTime_ticks % 20 == 0) {
				final int seconds = warmupTime_ticks / 20;
				if ( !isWarmupReported
				  || (seconds >= 60 && (seconds % 15 == 0))
				  || (seconds <  60 && seconds > 30 && (seconds % 10 == 0)) ) {
					isWarmupReported = true;
					Commons.messageToAllPlayersInArea(this, new WarpDriveText(null, "warpdrive.ship.guide.warming_up",
					                                                          seconds));
				}
			}
			
			// Awaiting warm-up time
			if (warmupTime_ticks > 0) {
				warmupTime_ticks--;
				break;
			}
			
			warmupTime_ticks = 0;
			isMotionSicknessApplied = false;
			isSoundPlayed = false;
			isWarmupReported = false;
			
			if (!isAssemblyValid) {
				commandDone(false, textValidityIssues);
				return;
			}
			
			final TileEntityShipCore shipCoreIntersecting = GlobalRegionManager.getIntersectingShipCore(this);
			if (shipCoreIntersecting != null) {
				commandDone(false, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.ship.guide.warp_field_overlapping",
				                                     shipCoreIntersecting.getSignatureName() ));
				return;
			}
			
			doJump();
			setCooldown(shipMovementCosts.cooldown_seconds * 20);
			commandDone(true, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.ship.guide.pre_jump_success"));
			jumpCount++;
			stateCurrent = EnumShipCoreState.IDLE;
			isCooldownReported = false;
			break;
			
		default:
			break;
		}

		autopilotTick();
	}

	public boolean isOffline() {
		return !isEnabled
		    || enumShipCommand == EnumShipCommand.OFFLINE;
	}
	
	public boolean isUnderMaintenance() {
		return isEnabled
		    && enumShipCommand == EnumShipCommand.MAINTENANCE;
	}

	private int computeShipScanSignature() {
		int hash = enumTier == null ? 0 : enumTier.getIndex();
		hash = 31 * hash + getFront();
		hash = 31 * hash + getBack();
		hash = 31 * hash + getRight();
		hash = 31 * hash + getLeft();
		hash = 31 * hash + getUp();
		hash = 31 * hash + getDown();
		return hash;
	}

	private VectorI toLocalOffset(@Nonnull final BlockPos blockPos) {
		if (facing == null) {
			return new VectorI(blockPos.getX() - pos.getX(), blockPos.getY() - pos.getY(), blockPos.getZ() - pos.getZ());
		}
		final int deltaX = blockPos.getX() - pos.getX();
		final int deltaZ = blockPos.getZ() - pos.getZ();
		final int offsetFront = facing.getXOffset() * deltaX + facing.getZOffset() * deltaZ;
		final int offsetRight = -facing.getZOffset() * deltaX + facing.getXOffset() * deltaZ;
		return new VectorI(offsetFront, blockPos.getY() - pos.getY(), offsetRight);
	}

	private BlockPos fromLocalOffset(@Nonnull final VectorI offset) {
		if (facing == null) {
			return pos.add(offset.x, offset.y, offset.z);
		}
		final int deltaX = facing.getXOffset() * offset.x - facing.getZOffset() * offset.z;
		final int deltaZ = facing.getZOffset() * offset.x + facing.getXOffset() * offset.z;
		return pos.add(deltaX, offset.y, deltaZ);
	}

	private void restoreSecurityStationFromScanCache() {
		if (posSecurityStation == null && shipScanSecurityStationLocal != null) {
			posSecurityStation = fromLocalOffset(shipScanSecurityStationLocal);
			weakTileEntitySecurityStation = null;
		}
	}

	private void invalidateShipScanCache() {
		shipMass = 0;
		shipVolume = 0;
		shipScanSignature = 0;
		shipScanSecurityStationLocal = null;
		posSecurityStation = null;
		weakTileEntitySecurityStation = null;
		cache_aabbArea = null;
		isShipScanValid = false;
		textShipScanIssues = new WarpDriveText();
		timeLastShipScanDone = -1L;
		invalidateNavigationCache();
	}

	public int getShipScanSignature() {
		return shipScanSignature;
	}

	public void invalidateNavigationCache() {
		navigationHeavyCacheKey = "";
		navigationHeavyCache = null;
	}

	@Nullable
	public NBTTagCompound getNavigationHeavyCache(@Nonnull final String cacheKey) {
		if (navigationHeavyCache == null || !navigationHeavyCacheKey.equals(cacheKey)) {
			return null;
		}
		return navigationHeavyCache.copy();
	}

	public void setNavigationHeavyCache(@Nonnull final String cacheKey, @Nonnull final NBTTagCompound tagCompound) {
		navigationHeavyCacheKey = cacheKey;
		navigationHeavyCache = tagCompound.copy();
	}

	public String getNavigationTargetId() {
		return navigationTargetId;
	}

	public void setNavigationTargetId(final String navigationTargetId) {
		final String navigationTargetIdNew = navigationTargetId == null ? "" : navigationTargetId;
		if (!this.navigationTargetId.equals(navigationTargetIdNew)) {
			this.navigationTargetId = navigationTargetIdNew;
			invalidateNavigationCache();
			markDirty();
		}
	}

	public void clearNavigationTarget() {
		setNavigationTargetId("");
	}

	public String getNavigationEngagedTargetId() {
		return navigationEngagedTargetId;
	}

	public void setNavigationEngagedTargetId(final String navigationEngagedTargetId) {
		this.navigationEngagedTargetId = navigationEngagedTargetId == null ? "" : navigationEngagedTargetId;
		markDirty();
	}

	public void clearNavigationEngagedTargetId() {
		navigationEngagedTargetId = "";
		markDirty();
	}

	// ----- autopilot -----

	public EnumShipAutopilotMode getAutopilotMode() {
		return autopilotMode;
	}

	public void setAutopilotMode(final EnumShipAutopilotMode mode) {
		autopilotMode = mode == null ? EnumShipAutopilotMode.OFF : mode;
		if (autopilotMode == EnumShipAutopilotMode.OFF) {
			autopilotStatus = EnumShipAutopilotStatus.IDLE;
		}
		invalidateNavigationCache();
		markDirty();
	}

	public EnumShipAutopilotStatus getAutopilotStatus() {
		return autopilotStatus;
	}

	public String getAutopilotLastErrorKey() {
		return autopilotLastErrorKey;
	}

	public int getAutopilotLegsExecuted() {
		return autopilotLegsExecuted;
	}

	public void startAutopilotRun(final boolean singleStep) {
		// a fresh route (not a confirmation of a paused leg) resets the per-route oscillation counter
		if (!autopilotStatus.isActive() && autopilotStatus != EnumShipAutopilotStatus.WAITING_CONFIRM) {
			autopilotLegsExecuted = 0;
		}
		autopilotSingleStep = singleStep;
		autopilotRetryCount = 0;
		autopilotLastErrorKey = "";
		autopilotStatus = EnumShipAutopilotStatus.RUNNING;
		markDirty();
	}

	public void cancelAutopilot() {
		autopilotStatus = EnumShipAutopilotStatus.IDLE;
		autopilotLegsExecuted = 0;
		autopilotRetryCount = 0;
		autopilotSingleStep = false;
		autopilotLastErrorKey = "";
		autopilotWaitUntilTick = 0L;
		clearNavigationEngagedTargetId();
	}

	public void pauseAutopilot() {
		if (autopilotStatus.isActive() || autopilotStatus == EnumShipAutopilotStatus.WAITING_CONFIRM) {
			autopilotStatus = EnumShipAutopilotStatus.PAUSED;
			markDirty();
		}
	}

	public void resumeAutopilot() {
		if (autopilotStatus == EnumShipAutopilotStatus.PAUSED) {
			autopilotStatus = EnumShipAutopilotStatus.RUNNING;
			autopilotWaitUntilTick = 0L;
			markDirty();
		}
	}

	// invoked by JumpSequencer on the success branch, with the destination that was engaged
	public void onNavigationMovementCompleted(@Nonnull final String navigationEngagedTargetId) {
		if (navigationEngagedTargetId.isEmpty() || navigationTargetId.isEmpty()) {
			return;
		}
		if (!this.navigationEngagedTargetId.equals(navigationEngagedTargetId)) {
			return;
		}
		final CelestialObject celestialObjectCurrent = CelestialObjectManager.get(world, pos.getX(), pos.getZ());
		final CelestialObject celestialObjectTarget = CelestialObjectManager.get(false, navigationTargetId);
		if (ShipNavigationHelper.isAtNavigationDestination(this, celestialObjectCurrent, celestialObjectTarget)) {
			onAutopilotArrived();
			return;
		}
		refreshShipScanCacheTimestamp();
		// an intermediate leg landed - decide whether to chain the next one
		if (autopilotMode == EnumShipAutopilotMode.OFF) {
			clearNavigationEngagedTargetId();
			autopilotStatus = EnumShipAutopilotStatus.IDLE;
			return;
		}
		if (autopilotSingleStep || autopilotMode == EnumShipAutopilotMode.ASSISTED) {
			autopilotSingleStep = false;
			autopilotStatus = EnumShipAutopilotStatus.WAITING_CONFIRM;
			markDirty();
			return;
		}
		// SAFETY_STOPS / FULL_AUTO: keep going once cooldown elapses (the pump re-gates risky legs)
		autopilotStatus = EnumShipAutopilotStatus.WAITING_COOLDOWN;
		autopilotWaitUntilTick = world.getTotalWorldTime() + ticksCooldown;
		markDirty();
	}

	// invoked by JumpSequencer on the failure branch (asynchronous block-move abort)
	public void onNavigationMovementAborted(@Nonnull final String navigationEngagedTargetId) {
		if (!this.navigationEngagedTargetId.equals(navigationEngagedTargetId)) {
			return;
		}
		clearNavigationEngagedTargetId();
		autopilotRetry("warpdrive.navigation.autopilot.leg_failed");
	}

	private void autopilotRetry(final String reasonKey) {
		if ( autopilotMode == EnumShipAutopilotMode.OFF
		  || navigationTargetId.isEmpty()
		  || !(autopilotStatus.isActive() || autopilotStatus == EnumShipAutopilotStatus.WAITING_CONFIRM) ) {
			return;
		}
		autopilotRetryCount++;
		if (autopilotRetryCount > AUTOPILOT_MAX_RETRIES) {
			autopilotFail(reasonKey);
		} else {
			autopilotLastErrorKey = reasonKey == null ? "" : reasonKey;
			autopilotStatus = EnumShipAutopilotStatus.WAITING_COOLDOWN;
			autopilotWaitUntilTick = world.getTotalWorldTime() + (long) AUTOPILOT_BACKOFF_TICKS * autopilotRetryCount + ticksCooldown;
		}
	}

	private void onAutopilotArrived() {
		clearNavigationTarget();
		clearNavigationEngagedTargetId();
		autopilotStatus = EnumShipAutopilotStatus.ARRIVED;
		autopilotLegsExecuted = 0;
		autopilotRetryCount = 0;
		autopilotSingleStep = false;
		autopilotLastErrorKey = "";
		Commons.messageToAllPlayersInArea(this, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.navigation.arrived"));
		sendEvent("shipAutopilotArrived");
	}

	private void autopilotFail(final String reasonKey) {
		autopilotStatus = EnumShipAutopilotStatus.ABORTED;
		autopilotLastErrorKey = reasonKey == null ? "" : reasonKey;
		autopilotSingleStep = false;
		clearNavigationEngagedTargetId();
		Commons.messageToAllPlayersInArea(this, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.navigation.autopilot.aborted",
		                                                          new WarpDriveText(null, autopilotLastErrorKey)));
		sendEvent("shipAutopilotAborted", autopilotLastErrorKey);
	}

	// runs once per server tick from update(); commits the next leg of a running route when the core is free
	@SuppressWarnings("PMD.NPathComplexity")
	private void autopilotTick() {
		if ( autopilotMode == EnumShipAutopilotMode.OFF
		  || !autopilotStatus.isActive() ) {
			return;
		}
		if (navigationTargetId.isEmpty()) {
			autopilotStatus = EnumShipAutopilotStatus.IDLE;
			return;
		}
		if ( isBusy()
		  || stateCurrent != EnumShipCoreState.IDLE
		  || isCommandConfirmed ) {
			return; // a jump is in flight, wait for it to land
		}
		final long worldTime = world.getTotalWorldTime();
		if (worldTime < autopilotWaitUntilTick) {
			return;
		}

		final CelestialObject celestialObjectCurrent = CelestialObjectManager.get(world, pos.getX(), pos.getZ());
		final CelestialObject celestialObjectTarget = CelestialObjectManager.get(false, navigationTargetId);
		if (ShipNavigationHelper.isAtNavigationDestination(this, celestialObjectCurrent, celestialObjectTarget)) {
			onAutopilotArrived();
			return;
		}

		final ShipNavigationHelper.Leg leg = ShipNavigationHelper.computeNextLeg(this);
		if (leg == null) {
			autopilotFail("warpdrive.navigation.autopilot.no_route");
			return;
		}

		// safety gating: pause before risky legs (hyperspace folds, landing) and before every leg in assisted mode
		if ( autopilotMode == EnumShipAutopilotMode.ASSISTED
		  || (autopilotMode == EnumShipAutopilotMode.SAFETY_STOPS && leg.requiresConfirmation) ) {
			autopilotStatus = EnumShipAutopilotStatus.WAITING_CONFIRM;
			autopilotLastErrorKey = "";
			Commons.messageToAllPlayersInArea(this, new WarpDriveText(null, "warpdrive.navigation.autopilot.awaiting_confirmation",
			                                                          new WarpDriveText(null, leg.type.getTitleKey())));
			return;
		}

		final ShipMovementPreview preview = leg.preview == null
		                                  ? previewMovement(leg.command, leg.moveFront, leg.moveUp, leg.moveRight, (byte) 0)
		                                  : leg.preview;
		if (!preview.canEngage) {
			if ("warpdrive.navigation.blocker.insufficient_energy".equals(preview.blockerKey)) {
				autopilotStatus = EnumShipAutopilotStatus.WAITING_ENERGY;
				autopilotLastErrorKey = preview.blockerKey;
				autopilotWaitUntilTick = worldTime + AUTOPILOT_ENERGY_RECHECK_TICKS;
				return;
			}
			if ( "warpdrive.navigation.blocker.busy".equals(preview.blockerKey)
			  || "warpdrive.navigation.blocker.stale_scan".equals(preview.blockerKey) ) {
				if ("warpdrive.navigation.blocker.stale_scan".equals(preview.blockerKey)) {
					requestShipScan();
				}
				autopilotStatus = EnumShipAutopilotStatus.WAITING_COOLDOWN;
				autopilotWaitUntilTick = worldTime + AUTOPILOT_BACKOFF_TICKS;
				return;
			}
			autopilotFail(preview.blockerKey.isEmpty() ? "warpdrive.navigation.autopilot.no_route" : preview.blockerKey);
			return;
		}

		if (autopilotLegsExecuted >= AUTOPILOT_MAX_LEGS) {
			autopilotFail("warpdrive.navigation.autopilot.too_many_legs");
			return;
		}

		ShipNavigationHelper.commitLeg(this, navigationTargetId, leg);
		autopilotLegsExecuted++;
		autopilotRetryCount = 0;
		autopilotLastErrorKey = "";
		autopilotStatus = EnumShipAutopilotStatus.RUNNING;
	}

	// ----- live drive status getters (for the navigation GUI) -----

	public String getDriveStateName() {
		return stateCurrent.getName();
	}

	public int getWarmupRemainingTicks() {
		return warmupTime_ticks;
	}

	public int getWarmupTotalTicks() {
		return warmupTotal_ticks;
	}

	public int getCooldownRemainingTicks() {
		return ticksCooldown;
	}

	public int getCooldownTotalTicks() {
		return cooldownTotal_ticks;
	}

	public boolean isJumpInProgress() {
		return stateCurrent == EnumShipCoreState.WARMING_UP
		    || stateCurrent == EnumShipCoreState.EXECUTE;
	}

	@Nonnull
	public String getShipMovementTypeName() {
		return shipMovementType == null ? "" : shipMovementType.getName();
	}
	
	public boolean isBusy() {
		return timeLastShipScanDone < 0 || shipScanner != null
		    || isCooling()
		    || stateCurrent == EnumShipCoreState.WARMING_UP;
	}

	public boolean isShipScanStale() {
		return !world.isRemote
		    && isShipScanValid
		    && shipScanner == null
		    && timeLastShipScanDone > 0L
		    && timeLastShipScanDone + WarpDriveConfig.SHIP_VOLUME_SCAN_AGE_TOLERANCE_SECONDS * 20L < world.getTotalWorldTime();
	}

	public boolean isShipScanReady() {
		return !world.isRemote
		    && isShipScanValid
		    && shipScanner == null
		    && timeLastShipScanDone > 0L
		    && !isShipScanStale();
	}

	public void requestShipScan() {
		if (world.isRemote || shipScanner != null || timeLastShipScanDone < 0L) {
			return;
		}
		invalidateShipScanCache();
		markDirty();
	}

	public boolean requestShipScanIfStale() {
		if (!isShipScanStale()) {
			return false;
		}
		requestShipScan();
		return true;
	}

	public void refreshShipScanCacheTimestamp() {
		if (!world.isRemote && isShipScanValid && shipScanner == null && timeLastShipScanDone > 0L) {
			timeLastShipScanDone = world.getTotalWorldTime();
			markDirty();
		}
	}
	
	private void setCooldown(final int ticksCooldown) {
		this.ticksCooldown = Math.max(1, Math.max(this.ticksCooldown, ticksCooldown));
		this.cooldownTotal_ticks = this.ticksCooldown;
		isCooldownReported = false;
	}
	
	private int getCooldown() {
		return ticksCooldown;
	}
	
	private boolean isCooling() {
		return ticksCooldown > 0;
	}
	
	@Override
	public boolean refreshLink(final IMultiBlockCoreOrController multiblockController) {
		assert multiblockController instanceof TileEntityShipController;
		final TileEntityShipController tileEntityShipController = (TileEntityShipController) multiblockController;
		
		final boolean isValid = !isUpgradeable();
		
		final BlockPos blockPos = tileEntityShipController.getPos();
		if (blockPosShipControllers.contains(blockPos)) {
			if (!isValid) {
				blockPosShipControllers.remove(blockPos);
				WarpDrive.logger.info(String.format("%s link removed to %s",
				                                    this, tileEntityShipController));
			}
		} else if (isValid) {
			blockPosShipControllers.add(blockPos);
			WarpDrive.logger.info(String.format("%s link added to %s",
			                                    this, tileEntityShipController));
		}
		return isValid;
	}
	
	@Override
	public void removeLink(final IMultiBlockCoreOrController multiblockController) {
		assert multiblockController instanceof TileEntityShipController;
		final TileEntityShipController tileEntityShipController = (TileEntityShipController) multiblockController;
		final BlockPos blockPos = tileEntityShipController.getPos();
		blockPosShipControllers.remove(blockPos);
		WarpDrive.logger.info(String.format("%s link removed to %s",
		                                    this, tileEntityShipController));
	}
	
	@Override
	protected void commandDone(final boolean success, @Nonnull final WarpDriveText reasonRaw) {
		assert success || !reasonRaw.getUnformattedText().isEmpty();
		final WarpDriveText reason;
		if (success || !commandCurrent.isMovement()) {
			reason = reasonRaw;
		} else {
			reason = new WarpDriveText(Commons.getStyleWarning(), "warpdrive.ship.guide.movement_aborted")
					         .append(reasonRaw);
		}
		super.commandDone(success, reason);
		if (!success) {
			clearNavigationEngagedTargetId();
			Commons.messageToAllPlayersInArea(this, reason);
			stateCurrent = EnumShipCoreState.IDLE;
			sendEvent("shipCommandFailure", reason.getUnformattedText());
			// a leg of an active autopilot route failed validation: retry with backoff, then give up
			autopilotRetry("warpdrive.navigation.autopilot.leg_failed");
		}
		for (final BlockPos blockPos : blockPosShipControllers) {
			if (!world.isBlockLoaded(blockPos, false)) {
				continue;
			}
			final TileEntity tileEntity = world.getTileEntity(blockPos);
			if (!(tileEntity instanceof TileEntityShipController)) {
				blockPosShipControllers.remove(blockPos);
				WarpDrive.logger.info(String.format("%s link removed to invalid instance of TileEntityShipController %s",
				                                    this, tileEntity));
				
				continue;
			}
			((TileEntityShipController) tileEntity).commandDone(success, reason);
		}
	}
	
	public String getFirstOnlineCrew() {
		final TileEntitySecurityStation tileEntitySecurityStation = getSecurityStation();
		if (tileEntitySecurityStation == null) {
			return null;
		}
		if (tileEntitySecurityStation == TileEntitySecurityStation.DUMMY) {
			return "-busy-";
		}
		return tileEntitySecurityStation.getFirstOnlinePlayer();
	}
	
	public boolean isCrewMember(final EntityPlayer entityPlayer) {
		final TileEntitySecurityStation tileEntitySecurityStation = getSecurityStation();
		if (tileEntitySecurityStation == null) {
			return true;
		}
		if (tileEntitySecurityStation == TileEntitySecurityStation.DUMMY) {
			return false;
		}
		return tileEntitySecurityStation.isAttachedPlayer(entityPlayer);
	}
	
	@Override
	protected boolean doScanAssembly(final boolean isDirty, final WarpDriveText textReason) {
		final boolean isValid = super.doScanAssembly(isDirty, textReason);
		if (!isShipScanValid && !textShipScanIssues.isEmpty()) {
			textReason.append(textShipScanIssues);
		}
		
		// refresh cache
		final IBlockState blockStateCore = world.getBlockState(pos);
		if (isInvalidBlockState(blockStateCore, BlockShipCore.class, BlockProperties.FACING_HORIZONTAL)) {
			return false;
		}
		facing = blockStateCore.getValue(BlockProperties.FACING_HORIZONTAL);
		restoreSecurityStationFromScanCache();
		
		// Search block in cube around core
		final int xMin = pos.getX() - WarpDriveConfig.RADAR_MAX_ISOLATION_RANGE;
		final int xMax = pos.getX() + WarpDriveConfig.RADAR_MAX_ISOLATION_RANGE;
		
		final int zMin = pos.getZ() - WarpDriveConfig.RADAR_MAX_ISOLATION_RANGE;
		final int zMax = pos.getZ() + WarpDriveConfig.RADAR_MAX_ISOLATION_RANGE;
		
		// scan 1 block higher to encourage putting isolation block on both
		// ground and ceiling
		final int yMin = Math.max(  0, pos.getY() - WarpDriveConfig.RADAR_MAX_ISOLATION_RANGE + 1);
		final int yMax = Math.min(255, pos.getY() + WarpDriveConfig.RADAR_MAX_ISOLATION_RANGE + 1);
		
		int newCount = 0;
		
		// Search for warp isolation blocks
		final MutableBlockPos mutableBlockPos = new MutableBlockPos();
		for (int y = yMin; y <= yMax; y++) {
			for (int x = xMin; x <= xMax; x++) {
				for (int z = zMin; z <= zMax; z++) {
					mutableBlockPos.setPos(x, y, z);
					if (world.getBlockState(mutableBlockPos).getBlock() instanceof BlockWarpIsolation) {
						newCount++;
					}
				}
			}
		}
		isolationBlocksCount = newCount;
		final double legacy_isolationRate = isolationRate;
		if (isolationBlocksCount >= WarpDriveConfig.RADAR_MIN_ISOLATION_BLOCKS) {
			isolationRate = Math.min(WarpDriveConfig.RADAR_MAX_ISOLATION_EFFECT, WarpDriveConfig.RADAR_MIN_ISOLATION_EFFECT
					+ (isolationBlocksCount - WarpDriveConfig.RADAR_MIN_ISOLATION_BLOCKS) // bonus blocks
					* (WarpDriveConfig.RADAR_MAX_ISOLATION_EFFECT - WarpDriveConfig.RADAR_MIN_ISOLATION_EFFECT)
					/ (WarpDriveConfig.RADAR_MAX_ISOLATION_BLOCKS - WarpDriveConfig.RADAR_MIN_ISOLATION_BLOCKS));
		} else {
			isolationRate = 0.0D;
		}
		if (legacy_isolationRate != isolationRate) {
			markDirtyGlobalRegion();
			if (WarpDrive.isDev && WarpDriveConfig.LOGGING_RADAR) {
				WarpDrive.logger.info(String.format("%s Isolation updated to %d (%.1f%%)",
				                                    this, isolationBlocksCount , isolationRate * 100.0));
			}
		}
		
		return isValid;
	}
	
	@Override
	protected void doUpdateParameters(final boolean isDirty) {
		// compute dimensions in game coordinates
		final int old_minX = minX;
		final int old_maxX = maxX;
		final int old_minY = minY;
		final int old_maxY = maxY;
		final int old_minZ = minZ;
		final int old_maxZ = maxZ;
		if (facing.getXOffset() == 1) {
			minX = pos.getX() - getBack();
			maxX = pos.getX() + getFront();
			minZ = pos.getZ() - getLeft();
			maxZ = pos.getZ() + getRight();
		} else if (facing.getXOffset() == -1) {
			minX = pos.getX() - getFront();
			maxX = pos.getX() + getBack();
			minZ = pos.getZ() - getRight();
			maxZ = pos.getZ() + getLeft();
		} else if (facing.getZOffset() == 1) {
			minZ = pos.getZ() - getBack();
			maxZ = pos.getZ() + getFront();
			minX = pos.getX() - getRight();
			maxX = pos.getX() + getLeft();
		} else if (facing.getZOffset() == -1) {
			minZ = pos.getZ() - getFront();
			maxZ = pos.getZ() + getBack();
			minX = pos.getX() - getLeft();
			maxX = pos.getX() + getRight();
		}
		
		minY = pos.getY() - getDown();
		maxY = pos.getY() + getUp();
		
		// recover in case of cache failure
		boolean isDirty2 = false;
		if ( minX != old_minX || maxX != old_maxX
		  || minY != old_minY || maxY != old_maxY
		  || minZ != old_minZ || maxZ != old_maxZ ) {
			if (!isDirty) {
				WarpDrive.logger.error(String.format("Dimensions changed but not dirty, please report this to mod author!\n%s",
				                                     getInternalStatus() ));
				isDirty2 = true;
			}
		}
		
		// update dimensions to client
		if (!isDirty) {
			markDirty();
		}
		
		// request new ship scan
		if (isDirty || isDirty2) {
			cache_aabbArea = null;
			invalidateNavigationCache();
			if (!isShipScanValid || shipScanSignature != computeShipScanSignature()) {
				invalidateShipScanCache();
			}
		}
	}
	
	private void makePlayersOnShipDrunk(final int tickDuration) {
		final AxisAlignedBB axisalignedbb = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
		final List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(null, axisalignedbb);
		
		for (final Entity entity : list) {
			if (!(entity instanceof EntityPlayer)) {
				continue;
			}
			
			// Set "drunk" effect
			((EntityPlayer) entity).addPotionEffect(
					new PotionEffect(MobEffects.NAUSEA, tickDuration, 0, true, true));
		}
	}
	
	@Nullable
	private TileEntitySecurityStation getSecurityStation() {
		if (posSecurityStation == null) {// no crew defined
			return null;
		}
		
		// cache the tile entity to avoid slow access to world object
		TileEntity tileEntity = weakTileEntitySecurityStation == null ? null : weakTileEntitySecurityStation.get();
		if ( tileEntity == null
		  || tileEntity.isInvalid()
		  || !posSecurityStation.equals(tileEntity.getPos()) ) {
			tileEntity = world.getTileEntity(posSecurityStation);
			weakTileEntitySecurityStation = null;
		}
		if ( !(tileEntity instanceof TileEntitySecurityStation)
		  || tileEntity.isInvalid() ) {// we're desync
			if (Commons.throttleMe("SecurityStationDesync")) {
				WarpDrive.logger.warn(String.format("%s: Security station %s has invalid tile entity: %s",
				                                    this, posSecurityStation, tileEntity ));
			}
			// force a refresh
			invalidateShipScanCache();
			return TileEntitySecurityStation.DUMMY;
		}
		if (weakTileEntitySecurityStation == null) {
			weakTileEntitySecurityStation = new WeakReference<>((TileEntitySecurityStation) tileEntity);
		}
		if (!((TileEntitySecurityStation) tileEntity).getIsEnabled()) {// disabled
			return null;
		}
		return (TileEntitySecurityStation) tileEntity;
	}
	
	public boolean summonOwnerOnDeploy(final EntityPlayerMP entityPlayerMP) {
		if (entityPlayerMP == null) {
			WarpDrive.logger.warn(this + " No player given to summonOwnerOnDeploy()");
			return false;
		}
		doUpdateParameters(false);
		// note: ship was just deployed, we assume it's valid instead of delaying while waiting for initial scan
		// consequently, the security station will probably not be defined yet...
		final TileEntitySecurityStation tileEntitySecurityStation = getSecurityStation();
		if ( tileEntitySecurityStation != null
		  && tileEntitySecurityStation != TileEntitySecurityStation.DUMMY ) {
			tileEntitySecurityStation.removeAllAttachedPlayers();
			tileEntitySecurityStation.attachPlayer(entityPlayerMP);
		}
		
		final AxisAlignedBB aabb = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
		if (isOutsideBB(aabb, MathHelper.floor(entityPlayerMP.posX), MathHelper.floor(entityPlayerMP.posY), MathHelper.floor(entityPlayerMP.posZ))) {
			summonPlayer(entityPlayerMP);
		}
		return true;
	}
	
	private static final VectorI[] SUMMON_OFFSETS = {
			new VectorI(-1, 0,  0), new VectorI( 1, 0,  0),
			new VectorI(-1, 0,  1), new VectorI(-1, 0, -1),
			new VectorI( 1, 0,  1), new VectorI( 1, 0, -1),
			new VectorI( 0, 0,  1), new VectorI( 0, 0, -1),
			new VectorI(-2, 0,  0), new VectorI( 2, 0,  0) };
	private void summonPlayer(@Nonnull final EntityPlayerMP entityPlayer) {
		// find a free spot
		final BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos(pos);
		for (final VectorI vOffset : SUMMON_OFFSETS) {
			mutableBlockPos.setPos(
				pos.getX() + facing.getXOffset() * vOffset.x + facing.getZOffset() * vOffset.z,
			    pos.getY(),
				pos.getZ() + facing.getZOffset() * vOffset.x + facing.getXOffset() * vOffset.z);
			if (world.isAirBlock(mutableBlockPos)) {
				if (world.isAirBlock(mutableBlockPos.add(0, 1, 0))) {
					summonPlayer(entityPlayer, mutableBlockPos);
					return;
				}
				mutableBlockPos.move(EnumFacing.DOWN);
				if (world.isAirBlock(mutableBlockPos)) {
					summonPlayer(entityPlayer, mutableBlockPos);
					return;
				}
			} else if ( world.isAirBlock(mutableBlockPos.add(0, -1, 0))
			         && world.isAirBlock(mutableBlockPos.add(0, -2, 0))
			         && !world.isAirBlock(mutableBlockPos.add(0, -3, 0)) ) {
				summonPlayer(entityPlayer, mutableBlockPos.add(0, -2, 0));
				return;
			} else if ( world.isAirBlock(mutableBlockPos.add(0, 1, 0))
			         && world.isAirBlock(mutableBlockPos.add(0, 2, 0))
			         && !world.isAirBlock(mutableBlockPos) ) {
				summonPlayer(entityPlayer, mutableBlockPos.add(0, 1, 0));
				return;
			}
		}
		final WarpDriveText message = new WarpDriveText(Commons.getStyleWarning(), "warpdrive.teleportation.guide.no_safe_spot",
		                                                entityPlayer.getDisplayName());
		Commons.messageToAllPlayersInArea(this, message);
		Commons.addChatMessage(entityPlayer, message);
	}
	
	private void summonPlayer(@Nonnull final EntityPlayerMP player, @Nonnull final BlockPos blockPos) {
		Commons.moveEntity(player, world, new Vector3(blockPos.getX() + 0.5D, blockPos.getY(), blockPos.getZ() + 0.5D));
	}
	
	private boolean validateShipMovementParameters(final WarpDriveText reason) {
		shipMovementType = EnumShipMovementType.compute(world, pos.getX(), minY, maxY, pos.getZ(), commandCurrent, getMovement().y, reason);
		if (shipMovementType == null) {
			return false;
		}
		
		// compute movement costs, then clamp the requested vector to the calculated ship max range
		final MovementResolution movementResolution = resolveMovement(commandCurrent, shipMovementType, getMovement());
		setMovement(movementResolution.movement.x, movementResolution.movement.y, movementResolution.movement.z);
		distanceSquared = movementResolution.movement.getMagnitudeSquared();
		shipMovementCosts = movementResolution.costs;
		
		// allow other mods to validate too
		final PreJump preJump;
		preJump = new PreJump(world, pos, this, shipMovementType.getName());
		MinecraftForge.EVENT_BUS.post(preJump);
		if (preJump.isCanceled()) {
			reason.append(preJump.getReason());
			return false;
		}
		
		return true;
	}

	private MovementResolution resolveMovement(@Nonnull final EnumShipCommand command,
	                                           @Nonnull final EnumShipMovementType movementType,
	                                           @Nonnull final VectorI requestedMovement) {
		VectorI effectiveMovement = requestedMovement.clone();
		ShipMovementCosts movementCosts = computeMovementCosts(movementType, effectiveMovement);
		if ( command == EnumShipCommand.HYPERDRIVE
		  || movementType == EnumShipMovementType.NONE ) {
			return new MovementResolution(effectiveMovement, movementCosts);
		}

		int distancePrevious = -1;
		for (int guard = 0; guard < 16; guard++) {
			effectiveMovement = requestedMovement.limitedToMagnitude(movementCosts.maximumDistance_blocks);
			final int distanceCurrent = getDistance(effectiveMovement);
			final ShipMovementCosts movementCostsCurrent = computeMovementCosts(movementType, effectiveMovement);
			if ( distanceCurrent == distancePrevious
			  && distanceCurrent <= movementCostsCurrent.maximumDistance_blocks ) {
				return new MovementResolution(effectiveMovement, movementCostsCurrent);
			}
			distancePrevious = distanceCurrent;
			movementCosts = movementCostsCurrent;
		}
		return new MovementResolution(effectiveMovement, movementCosts);
	}

	private ShipMovementCosts computeMovementCosts(@Nonnull final EnumShipMovementType movementType,
	                                               @Nonnull final VectorI movement) {
		return new ShipMovementCosts(world, pos, this, movementType, shipMass, getDistance(movement));
	}

	private static int getDistance(@Nonnull final VectorI movement) {
		return (int) Math.ceil(Math.sqrt(movement.getMagnitudeSquared()));
	}

	private static final class MovementResolution {
		private final VectorI movement;
		private final ShipMovementCosts costs;

		private MovementResolution(@Nonnull final VectorI movement, @Nonnull final ShipMovementCosts costs) {
			this.movement = movement;
			this.costs = costs;
		}
	}
	
	@Nonnull
	@SuppressWarnings("PMD.NPathComplexity")
	public ShipMovementPreview previewMovement(@Nonnull final EnumShipCommand command,
	                                           final int moveFront, final int moveUp, final int moveRight,
	                                           final byte rotationSteps) {
		final WarpDriveText reason = new WarpDriveText();
		// defensively clamp the requested movement: this method is reachable directly from computer scripts
		// (validateMovement) with arbitrary arguments, and an unclamped Integer.MIN_VALUE would break Math.abs() below
		final int clampedFront = Commons.clamp(-SHIP_MOVEMENT_INPUT_LIMIT, SHIP_MOVEMENT_INPUT_LIMIT, moveFront);
		final int clampedUp    = Commons.clamp(-SHIP_MOVEMENT_INPUT_LIMIT, SHIP_MOVEMENT_INPUT_LIMIT, moveUp);
		final int clampedRight = Commons.clamp(-SHIP_MOVEMENT_INPUT_LIMIT, SHIP_MOVEMENT_INPUT_LIMIT, moveRight);
		final VectorI requestedMovement = new VectorI(clampedFront, clampedUp, clampedRight);
		EnumShipMovementType previewMovementType = EnumShipMovementType.compute(world, pos.getX(), minY, maxY, pos.getZ(),
		                                                                        command, clampedUp, reason);
		String blockerKey = "";
		String blockerMessage = "";
		if (previewMovementType == null || previewMovementType == EnumShipMovementType.NONE) {
			previewMovementType = EnumShipMovementType.NONE;
			blockerKey = "warpdrive.navigation.blocker.invalid_command";
			blockerMessage = Commons.removeFormatting(reason.getUnformattedText());
		} else if (!getIsEnabled() || isOffline()) {
			blockerKey = "warpdrive.navigation.blocker.offline";
			blockerMessage = "Ship core is offline.";
		} else if (isUnderMaintenance()) {
			blockerKey = "warpdrive.navigation.blocker.maintenance";
			blockerMessage = "Ship core is under maintenance.";
		} else if (isBusy()) {
			blockerKey = "warpdrive.navigation.blocker.busy";
			blockerMessage = "Ship core is busy, cooling down, or scanning.";
		} else if (!isAssemblyValid) {
			blockerKey = "warpdrive.navigation.blocker.invalid_assembly";
			blockerMessage = Commons.removeFormatting(textValidityIssues.getUnformattedText());
		} else if (!isShipScanValid) {
			blockerKey = "warpdrive.navigation.blocker.invalid_scan";
			blockerMessage = Commons.removeFormatting(textShipScanIssues.getUnformattedText());
		} else if (timeLastShipScanDone + WarpDriveConfig.SHIP_VOLUME_SCAN_AGE_TOLERANCE_SECONDS * 20L < world.getTotalWorldTime()) {
			blockerKey = "warpdrive.navigation.blocker.stale_scan";
			blockerMessage = "Ship scan is stale.";
		}

		final int requestedDistance = getDistance(requestedMovement);
		final MovementResolution movementResolution = previewMovementType == EnumShipMovementType.NONE
		                                           ? null
		                                           : resolveMovement(command, previewMovementType, requestedMovement);
		final VectorI effectiveMovement = movementResolution == null ? new VectorI() : movementResolution.movement;
		final int effectiveDistance = getDistance(effectiveMovement);
		final ShipMovementCosts previewMovementCosts = movementResolution == null ? null : movementResolution.costs;
		final int maximumDistance = previewMovementCosts == null ? 0 : previewMovementCosts.maximumDistance_blocks;
		final int energyRequired = previewMovementCosts == null ? 0 : previewMovementCosts.energyRequired;
		if ( blockerKey.isEmpty()
		  && energy_getEnergyStored() < energyRequired ) {
			blockerKey = "warpdrive.navigation.blocker.insufficient_energy";
			blockerMessage = "Insufficient energy in core.";
		}
		final boolean wouldBeClamped = effectiveMovement.x != requestedMovement.x
		                            || effectiveMovement.y != requestedMovement.y
		                            || effectiveMovement.z != requestedMovement.z;

		return new ShipMovementPreview(command, previewMovementType, requestedMovement, effectiveMovement,
		                               (byte) ((rotationSteps + 4) % 4),
		                               requestedDistance, effectiveDistance, maximumDistance, energyRequired,
		                               energy_getEnergyStored(), blockerKey.isEmpty(), wouldBeClamped,
		                               blockerKey, blockerMessage);
	}

	// Computer interface are running independently of updateTicks, hence doing local computations getMaxJumpDistance() and getEnergyRequired()
	protected int getMaxJumpDistance(final EnumShipCommand command, final WarpDriveText reason) {
		final EnumShipMovementType shipMovementType = EnumShipMovementType.compute(world, pos.getX(), minY, maxY, pos.getZ(), command, getMovement().y, reason);
		if (shipMovementType == null) {
			commandDone(false, reason);
			return -1;
		}
		
		// compute movement costs
		final ShipMovementCosts shipMovementCosts = new ShipMovementCosts(world, pos,
		                                                                  this, shipMovementType,
		                                                                  shipMass, (int) Math.ceil(Math.sqrt(distanceSquared)));
		return shipMovementCosts.maximumDistance_blocks;
	}
	
	protected int getEnergyRequired(final EnumShipCommand command, final WarpDriveText reason) {
		final EnumShipMovementType shipMovementType = EnumShipMovementType.compute(world, pos.getX(), minY, maxY, pos.getZ(), command, getMovement().y, reason);
		if (shipMovementType == null) {
			commandDone(false, reason);
			return -1;
		}
		
		// compute movement costs
		final ShipMovementCosts shipMovementCosts = new ShipMovementCosts(world, pos,
		                                                                  this, shipMovementType,
		                                                                  shipMass, (int) Math.ceil(Math.sqrt(distanceSquared)));
		return shipMovementCosts.energyRequired;
	}
	
	private void doJump() {
		
		final int requiredEnergy = shipMovementCosts.energyRequired;
		
		if (!energy_consume(requiredEnergy, true)) {
			final String units = WarpDriveConfig.ENERGY_DISPLAY_UNITS;
			commandDone(false, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.ship.guide.insufficient_energy",
			                                     EnergyWrapper.format(energy_getEnergyStored(), units),
			                                     EnergyWrapper.format(requiredEnergy, units),
			                                     units));
			return;
		}
		
		final String shipInfo = String.format("%d blocks inside (%d %d %d) to (%d %d %d) with an actual mass of %d blocks",
		                                      shipVolume, minX, minY, minZ, maxX, maxY, maxZ, shipMass );
		switch (commandCurrent) {
		case HYPERDRIVE:
			WarpDrive.logger.info(this + " Performing hyperdrive jump of " + shipInfo);
			
			// Check ship size for hyper-space jump
			if (shipMass < WarpDriveConfig.SHIP_MASS_MIN_FOR_HYPERSPACE) {
				commandDone(false, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.ship.guide.insufficient_mass_for_hyperspace",
				                                     WarpDriveConfig.SHIP_MASS_MIN_FOR_HYPERSPACE, shipMass ));
				return;
			}
			break;
			
		case MANUAL:
			WarpDrive.logger.info(String.format("%s Performing manual jump of %s, %s, movement %s, rotationSteps %d",
			                                    this, shipInfo, shipMovementType, getMovement(), getRotationSteps()));
			break;
			
		default:
			WarpDrive.logger.error(String.format("%s Aborting while trying to perform invalid jump command %s",
			                                     this, commandCurrent));
			commandDone(false, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.error.internal_check_console"));
			commandCurrent = EnumShipCommand.IDLE;
			stateCurrent = EnumShipCoreState.IDLE;
			return;
		}
		
		if (!energy_consume(requiredEnergy, false)) {
			final String units = WarpDriveConfig.ENERGY_DISPLAY_UNITS;
			commandDone(false, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.ship.guide.insufficient_energy",
			                                     EnergyWrapper.format(energy_getEnergyStored(), units),
			                                     EnergyWrapper.format(requiredEnergy, units),
			                                     units));
			return;
		}
		
		int moveX = 0;
		int moveY = 0;
		int moveZ = 0;
		
		if (commandCurrent != EnumShipCommand.HYPERDRIVE) {
			final VectorI movement = getMovement();
			final int maxDistance = shipMovementCosts.maximumDistance_blocks;
			final VectorI limitedMovement = movement.limitedToMagnitude(maxDistance);
			movement.x = limitedMovement.x;
			movement.y = limitedMovement.y;
			movement.z = limitedMovement.z;
			moveX = facing.getXOffset() * movement.x - facing.getZOffset() * movement.z;
			moveY = movement.y;
			moveZ = facing.getZOffset() * movement.x + facing.getXOffset() * movement.z;
		}
		
		if (WarpDriveConfig.LOGGING_JUMP) {
			WarpDrive.logger.info(this + " Movement adjusted to (" + moveX + " " + moveY + " " + moveZ + ") blocks.");
		}
		final JumpSequencer jump = new JumpSequencer(this, shipMovementType, null,
				moveX, moveY, moveZ, getRotationSteps(),
				0, 0, 0, navigationEngagedTargetId);
		jump.enable();
	}
	
	private static boolean isOutsideBB(@Nonnull final AxisAlignedBB axisalignedbb, final int x, final int y, final int z) {
		return axisalignedbb.minX > x || axisalignedbb.maxX < x
		    || axisalignedbb.minY > y || axisalignedbb.maxY < y
		    || axisalignedbb.minZ > z || axisalignedbb.maxZ < z;
	}

	@Override
	public WarpDriveText getStatus() {
		final WarpDriveText textStatus = super.getStatus();
		if (ticksCooldown > 0) {
			textStatus.append(null, "warpdrive.ship.status_line.cooling",
			                  ticksCooldown / 20);
		}
		if (isolationBlocksCount > 0) {
			final String strIsolationRate = String.format("%.1f", isolationRate * 100.0D);
			textStatus.append(null, "warpdrive.ship.status_line.isolation",
			                  isolationBlocksCount, strIsolationRate);
		}
		return textStatus;
	}
	
	public ITextComponent getBoundingBoxStatus() {
		return super.getStatusPrefix()
			.appendSibling(new TextComponentTranslation(showBoundingBox ? "tile.warpdrive.movement.ship_core.bounding_box.enabled" : "tile.warpdrive.movement.ship_core.bounding_box.disabled"));
	}
	
	@Override
	public String getInternalStatus() {
		return String.format("%s\n"
		                   + "max %d %d %d min %d %d %d mass %d volume %d aabb %s\n"
		                   + "state %s command %s shipMovementType %s timeLastShipScanDone %d shipScanner %s shipMovementCosts %s\n"
		                   + "distanceSquared %d isCooldownReported %s isMotionSicknessApplied %s isSoundPlayed %s isWarmupReported %s randomWarmupAddition_ticks %d\n",
		                     super.getInternalStatus(),
		                     maxX, maxY, maxZ, minX, minY, minZ, shipMass, shipVolume, cache_aabbArea,
		                     stateCurrent, commandCurrent, shipMovementType, timeLastShipScanDone, shipScanner, shipMovementCosts,
		                     distanceSquared, isCooldownReported, isMotionSicknessApplied, isSoundPlayed, isWarmupReported, randomWarmupAddition_ticks );
	}
	
	@Override
	public boolean energy_canInput(final EnumFacing from) {
		return true;
	}
	
	@Override
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		isolationRate = tagCompound.getDouble("isolationRate");
		ticksCooldown = tagCompound.getInteger("cooldownTime");
		warmupTime_ticks = tagCompound.getInteger("warmupTime");
		jumpCount = tagCompound.getInteger("jumpCount");
		shipMass = tagCompound.getInteger("shipMass");
		shipVolume = tagCompound.getInteger("shipVolume");
		shipScanSignature = tagCompound.getInteger("shipScanSignature");
		isShipScanValid = tagCompound.getBoolean("isShipScanValid")
		               && shipMass > 0
		               && shipVolume > 0;
		if (isShipScanValid) {
			timeLastShipScanDone = tagCompound.hasKey("shipScanTime") ? tagCompound.getLong("shipScanTime") : 1L;
			textShipScanIssues = new WarpDriveText();
			if (tagCompound.hasKey("shipScanSecurityStationLocal")) {
				final NBTTagCompound tagSecurityStation = tagCompound.getCompoundTag("shipScanSecurityStationLocal");
				shipScanSecurityStationLocal = new VectorI(tagSecurityStation.getInteger("front"),
				                                           tagSecurityStation.getInteger("up"),
				                                           tagSecurityStation.getInteger("right"));
			} else {
				shipScanSecurityStationLocal = null;
			}
		} else {
			invalidateShipScanCache();
		}
		navigationTargetId = tagCompound.getString("navigationTargetId");
		navigationEngagedTargetId = tagCompound.getString("navigationEngagedTargetId");
		autopilotMode = EnumShipAutopilotMode.get(tagCompound.getString("autopilotMode"));
		// never blind-resume a half-flown route across a reload: keep the destination, but require a fresh Engage.
		// the leg counter is preserved so the per-route oscillation cap survives the serialize/deserialize a jump performs.
		autopilotStatus = EnumShipAutopilotStatus.IDLE;
		autopilotLegsExecuted = tagCompound.getInteger("autopilotLegs");
		autopilotRetryCount = 0;
		autopilotSingleStep = tagCompound.getBoolean("autopilotSingleStep");
		autopilotLastErrorKey = "";
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		tagCompound.setDouble("isolationRate", isolationRate);
		tagCompound.setInteger("cooldownTime", ticksCooldown);
		tagCompound.setInteger("warmupTime", warmupTime_ticks);
		tagCompound.setInteger("jumpCount", jumpCount);
		tagCompound.setInteger("shipMass", shipMass);
		tagCompound.setInteger("shipVolume", shipVolume);
		tagCompound.setInteger("shipScanSignature", shipScanSignature);
		tagCompound.setBoolean("isShipScanValid", isShipScanValid);
		tagCompound.setLong("shipScanTime", timeLastShipScanDone);
		if (shipScanSecurityStationLocal != null) {
			final NBTTagCompound tagSecurityStation = new NBTTagCompound();
			tagSecurityStation.setInteger("front", shipScanSecurityStationLocal.x);
			tagSecurityStation.setInteger("up", shipScanSecurityStationLocal.y);
			tagSecurityStation.setInteger("right", shipScanSecurityStationLocal.z);
			tagCompound.setTag("shipScanSecurityStationLocal", tagSecurityStation);
		}
		tagCompound.setString("navigationTargetId", navigationTargetId);
		tagCompound.setString("navigationEngagedTargetId", navigationEngagedTargetId);
		tagCompound.setString("autopilotMode", autopilotMode.getName());
		tagCompound.setInteger("autopilotLegs", autopilotLegsExecuted);
		tagCompound.setBoolean("autopilotSingleStep", autopilotSingleStep);

		return tagCompound;
	}
	
	@Nonnull
	@Override
	public NBTTagCompound getUpdateTag() {
		final NBTTagCompound tagCompound = super.getUpdateTag();
		
		if (posSecurityStation != null) {
			tagCompound.setTag("posSecurityStation", NBTUtil.createPosTag(posSecurityStation));
		}
		
		tagCompound.setInteger("minX", minX);
		tagCompound.setInteger("maxX", maxX);
		tagCompound.setInteger("minY", minY);
		tagCompound.setInteger("maxY", maxY);
		tagCompound.setInteger("minZ", minZ);
		tagCompound.setInteger("maxZ", maxZ);
		
		return tagCompound;
	}
	
	@Override
	public void onDataPacket(@Nonnull final NetworkManager networkManager, @Nonnull final SPacketUpdateTileEntity packet) {
		super.onDataPacket(networkManager, packet);
		
		final NBTTagCompound tagCompound = packet.getNbtCompound();
		
		if (tagCompound.hasKey("posSecurityStation")) {
			posSecurityStation = NBTUtil.getPosFromTag(tagCompound.getCompoundTag("posSecurityStation"));
		} else {
			posSecurityStation = null;
		}
		weakTileEntitySecurityStation = null;
		
		minX = tagCompound.getInteger("minX");
		maxX = tagCompound.getInteger("maxX");
		minY = tagCompound.getInteger("minY");
		maxY = tagCompound.getInteger("maxY");
		minZ = tagCompound.getInteger("minZ");
		maxZ = tagCompound.getInteger("maxZ");
		
		cache_aabbArea = null;
	}
	
	@Override
	public String getSignatureName() {
		return name;
	}
	
	// IGlobalRegionProvider overrides
	@Override
	public EnumGlobalRegionType getGlobalRegionType() {
		return EnumGlobalRegionType.SHIP;
	}
	
	@Override
	public AxisAlignedBB getGlobalRegionArea() {
		if (cache_aabbArea == null) {
			cache_aabbArea = new AxisAlignedBB(minX, minY, minZ, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D);
		}
		return cache_aabbArea;
	}
	
	@Override
	public int getMass() {
		return shipMass;
	}
	
	@Override
	public double getIsolationRate() {
		return isolationRate;
	}
	
	@Override
	public boolean onBlockUpdatingInArea(@Nullable final Entity entity, final BlockPos blockPos, final IBlockState blockState) {
		// no operation
		return true;
	}
	
	// Common OC/CC methods
	@Override
	public Object[] getOrientation() {
		if (facing == null) {
			try {
				final IBlockState blockState = world == null ? null : world.getBlockState(pos);
				if ( blockState != null
				  && blockState.getProperties().containsKey(BlockProperties.FACING_HORIZONTAL) ) {
					facing = blockState.getValue(BlockProperties.FACING_HORIZONTAL);
				}
			} catch (final RuntimeException exception) {
				// Fall back below; snapshots can query orientation while a moved core is still settling.
			}
		}
		if (facing == null) {
			facing = EnumFacing.NORTH;
		}
		return new Object[] { facing.getXOffset(), 0, facing.getZOffset() };
	}
	
	@Override
	public Object[] isInSpace() {
		return new Boolean[] { CelestialObjectManager.isInSpace(world, pos.getX(), pos.getZ()) };
	}
	
	@Override
	public Object[] isInHyperspace() {
		return new Boolean[] { CelestialObjectManager.isInHyperspace(world, pos.getX(), pos.getZ()) };
	}
	
	// public Object[] shipName(final Object[] arguments);
	
	@Override
	public Object[] getEnergyRequired() {
		final WarpDriveText reason = new WarpDriveText();
		final int energyRequired = getEnergyRequired(enumShipCommand, reason);
		if (energyRequired < 0) {
			return new Object[] { false, Commons.removeFormatting( reason.getUnformattedText() ) };
		}
		final String units = energy_getDisplayUnits();
		return new Object[] { true, EnergyWrapper.convert(energyRequired, units) };
	}
	
	@Override
	public Object[] getShipSize() {
		return new Object[] { shipMass, shipVolume };
	}
	
	@Override
	public Object[] getMaxJumpDistance() {
		final WarpDriveText reason = new WarpDriveText();
		final int maximumDistance_blocks = getMaxJumpDistance(enumShipCommand, reason);
		if (maximumDistance_blocks < 0) {
			return new Object[] { false, reason.toString() };
		}
		return new Object[] { true, maximumDistance_blocks };
	}
	
	@Override
	public Object[] validateMovement(final Object[] arguments) {
		try {
			if ( arguments == null
			  || arguments.length < 4
			  || arguments[0] == null ) {
				return new Object[] { false, "warpdrive.navigation.blocker.invalid_arguments", "Expected command, moveFront, moveUp, moveRight[, rotationSteps]" };
			}
			final EnumShipCommand command = parseShipCommand(arguments[0].toString());
			if (command == null) {
				return new Object[] { false, "warpdrive.navigation.blocker.invalid_command", "Unknown ship command" };
			}
			final byte rotationSteps = arguments.length >= 5 ? (byte) Commons.toInt(arguments[4]) : 0;
			return previewMovement(command,
			                       Commons.toInt(arguments[1]),
			                       Commons.toInt(arguments[2]),
			                       Commons.toInt(arguments[3]),
			                       rotationSteps).toObjectArray();
		} catch (final Exception exception) {
			return new Object[] { false, "warpdrive.navigation.blocker.invalid_arguments", exception.getMessage() };
		}
	}

	@Override
	public Object[] validateNavigation() {
		final ShipMovementPreview preview = ShipNavigationHelper.previewNavigation(this);
		if (preview == null) {
			return new Object[] { false, "warpdrive.navigation.no_route", "No route is available from the current position." };
		}
		return preview.toObjectArray();
	}

	private static EnumShipCommand parseShipCommand(final String name) {
		for (final EnumShipCommand command : EnumShipCommand.values()) {
			if ( command.name().equalsIgnoreCase(name)
			  || command.getName().equalsIgnoreCase(name) ) {
				return command;
			}
		}
		return null;
	}

	@Override
	public Object[] state() {
		final String units = energy_getDisplayUnits();
		final long energy = EnergyWrapper.convert(energy_getEnergyStored(), units);
		final String status = getStatusHeaderInPureText();
		final String stringState = isOffline() ? "OFFLINE"
		                         : isUnderMaintenance() ? "MAINTENANCE"
		                         : isCooling() ? "COOLING"
		                         : enumShipCommand.name();
		return new Object[] { status, isEnabled, stringState, energy };
	}
}
