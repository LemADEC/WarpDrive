package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.IVideoChannel;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.block.TileEntityAbstractMachine;
import cr0s.warpdrive.block.movement.TileEntityShipCore;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnumCameraType;
import cr0s.warpdrive.data.EnumComponentType;
import cr0s.warpdrive.data.EnumGlobalRegionType;
import cr0s.warpdrive.data.GlobalRegion;
import cr0s.warpdrive.data.GlobalRegionManager;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.item.ItemComponent;
import cr0s.warpdrive.network.PacketHandler;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;

import net.minecraftforge.common.util.Constants.NBT;

public class TileEntityCamera extends TileEntityAbstractMachine implements IVideoChannel {
	
	private int videoChannel = -1;

	private static final int REGISTRY_UPDATE_INTERVAL_TICKS = 15 * 20;
	private static final int PACKET_SEND_INTERVAL_TICKS = 60 * 20;
	
	private static final UpgradeSlot upgradeSlotRecognitionRange = new UpgradeSlot("camera.recognition_range",
	                                                                               ItemComponent.getItemStackNoCache(EnumComponentType.DIAMOND_CRYSTAL, 1),
	                                                                               WarpDriveConfig.CAMERA_RANGE_UPGRADE_MAX_QUANTITY );
	
	// persistent properties
	private final CopyOnWriteArrayList<Result> results = new CopyOnWriteArrayList<>();
	
	// computed properties
	private Vec3d vCamera = null;
	private int packetSendTicks = 10;
	private int registryUpdateTicks = 20;
	private boolean hasImageRecognition = false;
	
	private AxisAlignedBB aabbRange = null;
	private int tickSensing = 0;
	
	
	private enum Category {
		PLAYER("player"), MONSTER("monster"), ANIMAL("animal"), UNKNOWN("unknown");
		
		private final String label;
		
		Category(@Nonnull final String label) {
			this.label = label;
		}
		
		String getLabel() {
			return label;
		}
		
		// coarse identity from the entity's base vanilla class
		private static Category of(@Nonnull final Entity entity) {
			if (entity instanceof PlayerEntity) {
				return PLAYER;
			}
			if (entity instanceof IMob) {
				return MONSTER;
			}
			if (entity instanceof AnimalEntity) {
				return ANIMAL;
			}
			return UNKNOWN;
		}
		
		// parse a persisted label, defaulting to UNKNOWN so a corrupt or outdated NBT value is sanitized
		private static Category fromLabel(final String label) {
			for (final Category category : values()) {
				if (category.label.equals(label)) {
					return category;
				}
			}
			return UNKNOWN;
		}
	}
	
	private static final class Result {
		
		public Vector3 position;
		public Vector3 motion;
		public String type;
		public UUID uniqueId;
		public String name;
		public Category category;
		public boolean isCrewMember;
		public int identificationLevel; // 1 = face visible (full ID), 0 = only body visible (category)
		private boolean isUpdated;
		
		Result(@Nonnull final Vector3 position, @Nonnull final Vector3 motion, @Nonnull final String type,
		       @Nonnull final UUID uniqueId, @Nonnull final String name, @Nonnull final Category category,
		       final boolean isCrewMember, final int identificationLevel) {
			this.position = position;
			this.motion = motion;
			this.type = type;
			this.uniqueId = uniqueId;
			this.name = name;
			this.category = category;
			this.isCrewMember = isCrewMember;
			this.identificationLevel = identificationLevel;
			this.isUpdated = false;
		}
		
		Result(@Nonnull final Entity entity, final boolean isCrewMember, final int identificationLevel) {
			this(new Vector3(entity.getPosX(),
			                 entity.getPosY() + entity.getEyeHeight(),
			                 entity.getPosZ() ),
			     new Vector3(entity.getMotion().x,
			                 entity.getMotion().y,
			                 entity.getMotion().z ),
			     Dictionary.getId(entity),
			     entity.getUniqueID(),
			     entity.getName().getString(),
			     Category.of(entity),
			     isCrewMember,
			     identificationLevel );
			// since it was created from an entity, it's already updated
			isUpdated = true;
		}
		
		void markForUpdate() {
			isUpdated = false;
		}
		
		void update(@Nonnull final Entity entity, final boolean isCrewMember, final int identificationLevel) {
			// full refresh: identity can change (rename, crew membership, reload...), so never assume it is stable
			uniqueId = entity.getUniqueID();
			position.x = entity.getPosX();
			position.y = entity.getPosY() + entity.getEyeHeight();
			position.z = entity.getPosZ();
			motion.x = entity.getMotion().x;
			motion.y = entity.getMotion().y;
			motion.z = entity.getMotion().z;
			type = Dictionary.getId(entity);
			name = entity.getName().getString();
			category = Category.of(entity);
			this.isCrewMember = isCrewMember;
			this.identificationLevel = identificationLevel;
			isUpdated = true;
		}
		
		boolean isUpdated() {
			return isUpdated;
		}
		
		// matches the given entity by its immutable identity: persistent random uuid + type. The uuid is distinct from
		// getEntityId() (the reusable index that a later entity may reuse), so a changing name/crew/level is an update of
		// the same result rather than a remove + add. This is the ONLY identity lookup: Result is a unique mutable object
		// and is intentionally not value-comparable, so equals()/hashCode() are left as Object's identity defaults
		// (verified 2026-07 at runtime: nothing calls them - the results list only uses add/get/iteration and matches()).
		boolean matches(@Nonnull final Entity entity) {
			return uniqueId != null && uniqueId.equals(entity.getUniqueID())
			    && type.equals(Dictionary.getId(entity));
		}
		
	}
	
	public TileEntityCamera(@Nonnull final IBlockBase blockBase) {
		super(blockBase);
		
		peripheralName = "warpdriveCamera";
		addMethods(new String[] {
			"videoChannel",
			"getResults",
			"getResultsCount",
			"getResult"
		});
		doRequireUpgradeToInterface();
		CC_scripts = Collections.singletonList("recognize");
		
		registerUpgradeSlot(upgradeSlotRecognitionRange);
	}
	
	@Override
	public void tick() {
		super.tick();
		assert world != null;
		
		// Update video channel on clients (recovery mechanism, no need to go too fast)
		if (!world.isRemote()) {
			packetSendTicks--;
			if (packetSendTicks <= 0) {
				packetSendTicks = PACKET_SEND_INTERVAL_TICKS;
				PacketHandler.sendVideoChannelPacket(world, pos, videoChannel);
			}
		} else {
			registryUpdateTicks--;
			if (registryUpdateTicks <= 0) {
				registryUpdateTicks = REGISTRY_UPDATE_INTERVAL_TICKS;
				if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
					WarpDrive.logger.info(this + " Updating registry (" + videoChannel + ")");
				}
				WarpDrive.cameras.updateInRegistry(world, pos, videoChannel, EnumCameraType.SIMPLE_CAMERA);
			}
			return;
		}
		
		
		if ( isEnabled
		  && hasImageRecognition ) {
			tickSensing--;
			if (tickSensing < 0) {
				tickSensing = WarpDriveConfig.CAMERA_IMAGE_RECOGNITION_INTERVAL_TICKS;
				
				// clear the markers
				for (final Result result : results) {
					result.markForUpdate();
				}
				
				// check for exclusive living entity presence
				int countAdded = 0;
				final int countOld = results.size();
				final List<Entity> entitiesInRange = world.getEntitiesWithinAABB(Entity.class, aabbRange,
				                                                                 entity -> entity != null
				                                                                        && entity.isAlive()
				                                                                        && !entity.isInvisible()
				                                                                        && ( !(entity instanceof PlayerEntity)
				                                                                          || !entity.isSpectator() ));
				for (final Entity entity : entitiesInRange) {
					// check for line of sight, seeing through transparent blocks (glass/ice/water), sampling
					// head/torso/feet so a partially-hidden entity is still detected, like the monitor shows it
					final int identificationLevel = getIdentificationLevel(entity);
					if (identificationLevel < 0) {
						continue;
					}
					
					// check for existing results
					final boolean isCrewMember = identificationLevel >= 1 && getCrewStatus(entity);
					boolean isNew = true;
					for (final Result result : results) {
						if (result.matches(entity)) {
							result.update(entity, isCrewMember, identificationLevel);
							isNew = false;
							break;
						}
					}
					
					// add new result
					if (isNew) {
						countAdded++;
						results.add(new Result(entity, isCrewMember, identificationLevel));
					}
				}
				
				// clear old results
				results.removeIf(result -> !result.isUpdated());
				final int countRemoved = countOld + countAdded - results.size();
				
				// trigger LUA event
				if ( countAdded > 0
				  || countRemoved > 0 ) {
					sendEvent("opticalSensorResultsChanged", countAdded, countRemoved);
				}
			}
		}
	}
	
	// highest visible body region from the camera (through transparent blocks): 1 = face -> full ID, 0 = only body
	// (torso/feet) -> category only, -1 = fully hidden. A single occluding block (e.g. a slab at head height) legitimately
	// drops a distant entity to category-only from that camera's angle, while another camera with a clear face line reports
	// full ID; this per-camera, per-body-region grading is intended.
	private int getIdentificationLevel(@Nonnull final Entity entity) {
		if (hasLineOfSight(new Vec3d(entity.getPosX(), entity.getPosY() + entity.getEyeHeight(), entity.getPosZ()))) {
			return 1; // the face (head/eyes) is visible -> full identification
		}
		if ( hasLineOfSight(new Vec3d(entity.getPosX(), entity.getPosY() + entity.getHeight() * 0.5D, entity.getPosZ()))
		  || hasLineOfSight(new Vec3d(entity.getPosX(), entity.getPosY() + entity.getHeight() * 0.1D, entity.getPosZ())) ) {
			return 0; // only the body (torso/feet) is visible -> category only
		}
		return -1; // fully hidden
	}
	
	// Line of sight from the camera to a point, blocked only by opaque blocks.
	// Non-opaque blocks (glass, ice, water, leaves) are seen through.
	// Block collision geometry is respected (slabs/stairs).
	private boolean hasLineOfSight(@Nonnull final Vec3d vTarget) {
		// Tiny nudge to resume just past each hit face WITHOUT skipping any block: a larger step could jump over the
		// entry-face hit of the block right behind a transparent one (leaking through slabs/stairs on a sloped ray)
		final Vec3d vNudge = vTarget.subtract(vCamera).normalize().scale(0.001D);
		Vec3d vStart = vCamera;
		// Cap the traversal to the ray length (itself bounded by the camera range): each crossed block costs at most
		// ~2 steps (entry+exit of a transparent block), and a length-L ray crosses at most ~1.8*L blocks on a diagonal;
		// x4 covers both with a slight margin. Reaching the cap means the ray never resolved within range -> not visible.
		final int maxSteps = (int) Math.ceil(vCamera.distanceTo(vTarget)) * 4 + 8;
		for (int step = 0; step < maxSteps; step++) {
			final BlockRayTraceResult rayTraceResult = world.rayTraceBlocks(new RayTraceContext(
					vStart, vTarget, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, null));
			if (rayTraceResult.getType() != Type.BLOCK) {
				return true; // reached the target, no opaque block in the way
			}
			final BlockPos blockPosHit = rayTraceResult.getPos();
			final BlockState blockState = world.getBlockState(blockPosHit);
			final Block block = blockState.getBlock();
			final boolean isOwnBlock = blockPosHit.equals(pos); // the camera must not occlude itself
			// Blocks vision only if it is a solid opaque obstruction; see through the own block, non-opaque materials
			// (glass/water/ice/leaves) and blocks tagged Transparent in the dictionary
			final boolean blocksVision = !isOwnBlock
			                          && blockState.getMaterial().isOpaque()
			                          && !Dictionary.BLOCKS_TRANSPARENT.contains(block);
			if (blocksVision) {
				return false; // Solid opaque block whose collision box the ray actually hit
			}
			// Nudge just past the hit face and keep tracing
			vStart = rayTraceResult.getHitVec().add(vNudge);
			if (vCamera.squareDistanceTo(vStart) >= vCamera.squareDistanceTo(vTarget)) {
				return true; // Nudged past the target
			}
		}
		return false; // Exceeded the ray-length traversal budget (out of range / occluded)
	}
	
	private boolean getCrewStatus(final Entity entity) {
		if (!(entity instanceof PlayerEntity)) {
			return false;
		}
		final ArrayList<GlobalRegion> globalRegions = GlobalRegionManager.getContainers(EnumGlobalRegionType.SHIP, world, pos);
		final ArrayList<TileEntityShipCore> tileEntityShipCores = new ArrayList<>(globalRegions.size());
		if (globalRegions.isEmpty()) {
			return false;
		}
		for (final GlobalRegion globalRegion : globalRegions) {
			// abort on invalid ship cores
			final TileEntity tileEntity = world.getTileEntity(globalRegion.getBlockPos());
			if (!(tileEntity instanceof TileEntityShipCore)) {
				if (Commons.throttleMe("cameraGetCrewStatus-InvalidInstance")) {
					WarpDrive.logger.error(String.format("Unable to get crew status due to invalid tile entity for global region, expecting TileEntityShipCore, got %s",
					                                     tileEntity ));
				}
				return false;
			}
			final TileEntityShipCore tileEntityShipCore = (TileEntityShipCore) tileEntity;
			if (!tileEntityShipCore.isAssemblyValid()) {
				if (Commons.throttleMe("cameraGetCrewStatus-InvalidAssembly")) {
					WarpDrive.logger.error(String.format("Unable to get crew status due to invalid ship assembly for %s",
					                                     tileEntity ));
				}
				return false;
			}
			tileEntityShipCores.add(tileEntityShipCore);
		}
		
		boolean isCrewMember = true;
		for (final TileEntityShipCore tileEntityShipCore : tileEntityShipCores) {
			isCrewMember &= tileEntityShipCore.isCrewMember((PlayerEntity) entity);
		}
		
		return isCrewMember;
	}
	
	@Override
	protected void doUpdateParameters(final boolean isDirty) {
		super.doUpdateParameters(isDirty);
		assert world != null;
		
		final BlockState blockState = world.getBlockState(pos);
		updateBlockState(blockState, BlockProperties.ACTIVE, isEnabled);
		
		final int range = WarpDriveConfig.CAMERA_RANGE_BASE_BLOCKS
		                + WarpDriveConfig.CAMERA_RANGE_UPGRADE_BLOCKS * getUpgradeCount(upgradeSlotRecognitionRange);
		hasImageRecognition = range > 0;
		
		if ( hasImageRecognition
		  && blockState.getBlock() instanceof BlockCamera ) {
			final Direction enumFacing = blockState.get(BlockProperties.FACING);
			final float radius = range / 2.0F;
			// Optical center of the camera where line of sight computation starts
			vCamera = new Vec3d(
					pos.getX() + 0.5D,
					pos.getY() + 0.5D,
					pos.getZ() + 0.5D );
			// Observable area
			final Vec3d vCenter = new Vec3d(
					pos.getX() + 0.5F + (radius + 0.5F) * enumFacing.getXOffset(),
					pos.getY() + 0.5F + (radius + 0.5F) * enumFacing.getYOffset(),
					pos.getZ() + 0.5F + (radius + 0.5F) * enumFacing.getZOffset() );
			aabbRange = new AxisAlignedBB(
					vCenter.x - radius, vCenter.y - radius, vCenter.z - radius,
					vCenter.x + radius, vCenter.y + radius, vCenter.z + radius );
		}
	}
	
	@Override
	public int getVideoChannel() {
		return videoChannel;
	}
	
	@Override
	public void setVideoChannel(final int parVideoChannel) {
		if ( videoChannel != parVideoChannel
		  && IVideoChannel.isValid(parVideoChannel) ) {
			videoChannel = parVideoChannel;
			if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
				WarpDrive.logger.info(this + " Video channel set to " + videoChannel);
			}
			markDirty();
			// force update through main thread since CC & OC are running outside the main thread
			packetSendTicks = 0;
			registryUpdateTicks = 0;
		}
	}
	
	@Override
	public void remove() {
		if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
			WarpDrive.logger.info(this + " removed");
		}
		WarpDrive.cameras.removeFromRegistry(world, pos);
		super.remove();
	}
	
	@Override
	public void onChunkUnloaded() {
		if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
			WarpDrive.logger.info(this + " onChunkUnloaded");
		}
		WarpDrive.cameras.removeFromRegistry(world, pos);
		super.onChunkUnloaded();
	}
	
	@Override
	public void read(@Nonnull final CompoundNBT tagCompound) {
		super.read(tagCompound);
		
		videoChannel = tagCompound.getInt("frequency") + tagCompound.getInt(VIDEO_CHANNEL_TAG);
		if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
			WarpDrive.logger.info(this + " readFromNBT");
		}
		
		final ListNBT tagList = tagCompound.getList("results", NBT.TAG_COMPOUND);
		for (final INBT tagResult : tagList) {
			final CompoundNBT tagCompoundResult = (CompoundNBT) tagResult;
			try {
				final Result result = new Result(
						new Vector3(tagCompoundResult.getDouble("posX"), tagCompoundResult.getDouble("posY"), tagCompoundResult.getDouble("posZ")),
						new Vector3(tagCompoundResult.getDouble("motionX"), tagCompoundResult.getDouble("motionY"), tagCompoundResult.getDouble("motionZ")),
						tagCompoundResult.getString("type"),
						Objects.requireNonNull(tagCompoundResult.getUniqueId("uniqueId")),
						tagCompoundResult.getString("name"),
						Category.fromLabel(tagCompoundResult.getString("category")),
						tagCompoundResult.getBoolean("isCrewMember"),
						tagCompoundResult.getInt("identificationLevel") );
				results.add(result);
			} catch (final Exception exception) {
				WarpDrive.logger.error(String.format("%s Exception while reading previous result %s",
				                                     this, tagCompoundResult ));
				exception.printStackTrace(WarpDrive.printStreamError);
			}
		}
	}
	
	@Nonnull
	@Override
	public CompoundNBT write(@Nonnull CompoundNBT tagCompound) {
		tagCompound = super.write(tagCompound);
		
		tagCompound.putInt(VIDEO_CHANNEL_TAG, videoChannel);
		if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
			WarpDrive.logger.info(this + " writeToNBT");
		}
		
		if (!results.isEmpty()) {
			final ListNBT tagList = new ListNBT();
			for (final Result result : results) {
				final CompoundNBT tagCompoundResult = new CompoundNBT();
				tagCompoundResult.putDouble("posX", result.position.x);
				tagCompoundResult.putDouble("posY", result.position.y);
				tagCompoundResult.putDouble("posZ", result.position.z);
				tagCompoundResult.putDouble("motionX", result.motion.x);
				tagCompoundResult.putDouble("motionY", result.motion.y);
				tagCompoundResult.putDouble("motionZ", result.motion.z);
				tagCompoundResult.putString("type", result.type);
				if (result.uniqueId != null) {
					tagCompoundResult.putUniqueId("uniqueId", result.uniqueId);
				}
				if (result.name != null) {
					tagCompoundResult.putString("name", result.name);
				}
				tagCompoundResult.putString("category", result.category.getLabel());
				tagCompoundResult.putBoolean("isCrewMember", result.isCrewMember);
				tagCompoundResult.putInt("identificationLevel", result.identificationLevel);
				tagList.add(tagCompoundResult);
			}
			tagCompound.put("results", tagList);
		} else {
			tagCompound.remove("results");
		}
		
		return tagCompound;
	}
	
	// TileEntityAbstractBase overrides
	@Nonnull
	private WarpDriveText getSensorStatus() {
		if (!hasImageRecognition) {
			return new WarpDriveText();
		}
		if (results.isEmpty()) {
			return new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.optical_sensor.status_line.no_result");
		}
		return new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.optical_sensor.status_line.result_count",
		                         results.size() );
	}
	
	@Override
	public WarpDriveText getStatus() {
		final WarpDriveText textScanStatus = getSensorStatus();
		if (textScanStatus.isEmpty()) {
			return super.getStatus();
		} else {
			return super.getStatus()
			            .append(textScanStatus);
		}
	}
	
	// Common OC/CC methods
	public Object[] videoChannel(@Nonnull final Object[] arguments) {
		if (arguments.length == 1) {
			setVideoChannel(Commons.toInt(arguments[0]));
		}
		return new Integer[] { getVideoChannel() };
	}
	
	private Object[] getResults() {
		if (results == null) {
			return null;
		}
		final Object[] objectResults = new Object[results.size()];
		int index = 0;
		for (final Result result : results) {
			// full identity (type/name/crew) is only exported when the face was seen; category is always available
			final boolean isFullyIdentified = result.identificationLevel >= 1;
			objectResults[index++] = new Object[] {
					isFullyIdentified ? result.type : "",
					isFullyIdentified && result.name != null ? result.name : "",
					result.category.getLabel(),
					result.position.x, result.position.y, result.position.z,
					result.motion.x, result.motion.y, result.motion.z,
					isFullyIdentified && result.isCrewMember,
					result.identificationLevel };
		}
		return objectResults;
	}
	
	private Object[] getResultsCount() {
		if (results != null) {
			return new Integer[] { results.size() };
		}
		return new Integer[] { -1 };
	}
	
	private Object[] getResult(@Nonnull final Object[] arguments) {
		if (arguments.length == 1 && (results != null)) {
			final int index;
			try {
				index = Commons.toInt(arguments[0]);
			} catch(final Exception exception) {
				return new Object[] { false, COMPUTER_ERROR_TAG, COMPUTER_ERROR_TAG, COMPUTER_ERROR_TAG, 0, 0, 0, 0, 0, 0, false, -1 };
			}
			if (index >= 0 && index < results.size()) {
				final Result result = results.get(index);
				final boolean isFullyIdentified = result != null && result.identificationLevel >= 1;
				if (result != null) {
					return new Object[] {
							true,
							isFullyIdentified ? result.type : "",
							isFullyIdentified && result.name != null ? result.name : "",
							result.category.getLabel(),
							result.position.x, result.position.y, result.position.z,
							result.motion.x, result.motion.y, result.motion.z,
							isFullyIdentified && result.isCrewMember,
							result.identificationLevel };
				}
			}
		}
		return new Object[] { false, COMPUTER_ERROR_TAG, COMPUTER_ERROR_TAG, COMPUTER_ERROR_TAG, 0, 0, 0, 0, 0, 0, false, -1 };
	}
	
	// OpenComputers callback methods
	@Callback(direct = true)
	public Object[] videoChannel(final Context context, final Arguments arguments) {
		return videoChannel(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	public Object[] getResults(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getResults();
	}
	
	@Callback(direct = true)
	public Object[] getResultsCount(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getResultsCount();
	}
	
	@Callback(direct = true)
	public Object[] getResult(final Context context, final Arguments arguments) {
		return getResult(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	// ComputerCraft IDynamicPeripheral methods
	@Override
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "videoChannel":
			return videoChannel(arguments);
			
		case "getResults":
			return getResults();
			
		case "getResultsCount":
			return getResultsCount();
			
		case "getResult":
			return getResult(arguments);
			
		default:
			return super.CC_callMethod(methodName, arguments);
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s %d %s",
		                     getClass().getSimpleName(), 
		                     videoChannel,
		                     Commons.format(world, pos) );
	}
}