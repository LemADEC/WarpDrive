package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.ShipMovementCosts;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.EnumShipCommand;
import cr0s.warpdrive.data.EnumShipMovementType;
import cr0s.warpdrive.data.EnumShipNavigationLegType;
import cr0s.warpdrive.data.VectorI;
import cr0s.warpdrive.WarpDrive;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public final class ShipNavigationHelper {

	private static final int ORBIT_Y = 128;
	private static final int TAKEOFF_MARGIN_BLOCKS = 16;
	private static final int LANDING_MARGIN_BLOCKS = 16;
	private static final int MAP_VERSION = 2;

	private ShipNavigationHelper() {
		// static helper
	}

	@Nonnull
	public static NBTTagCompound buildSnapshot(@Nonnull final EntityPlayerMP entityPlayerMP,
	                                           @Nonnull final TileEntityShipCore shipCore,
	                                           @Nullable final String notice) {
		return buildSnapshot(entityPlayerMP, shipCore, shipCore.getPos(), notice);
	}

	@Nonnull
	public static NBTTagCompound buildSnapshot(@Nonnull final EntityPlayerMP entityPlayerMP,
	                                           @Nonnull final TileEntityShipCore shipCore,
	                                           @Nonnull final BlockPos blockPosAccess,
	                                           @Nullable final String notice) {
		return buildSnapshot(entityPlayerMP, shipCore, blockPosAccess, notice, null);
	}
	
	@Nonnull
	public static NBTTagCompound buildSnapshot(@Nonnull final EntityPlayerMP entityPlayerMP,
	                                           @Nonnull final TileEntityShipCore shipCore,
	                                           @Nonnull final BlockPos blockPosAccess,
	                                           @Nullable final String notice,
	                                           @Nullable final ShipMovementPreview movementPreview) {
		shipCore.requestShipScanIfStale();
		final NBTTagCompound tagCompound = new NBTTagCompound();
		tagCompound.setInteger("coreX", shipCore.getPos().getX());
		tagCompound.setInteger("coreY", shipCore.getPos().getY());
		tagCompound.setInteger("coreZ", shipCore.getPos().getZ());
		tagCompound.setInteger("accessX", blockPosAccess.getX());
		tagCompound.setInteger("accessY", blockPosAccess.getY());
		tagCompound.setInteger("accessZ", blockPosAccess.getZ());
		tagCompound.setInteger("dimensionId", shipCore.getWorld().provider.getDimension());
		tagCompound.setBoolean("allowed", shipCore.isCrewMember(entityPlayerMP));
		tagCompound.setBoolean("enabled", shipCore.getIsEnabled());
		tagCompound.setBoolean("offline", shipCore.isOffline());
		tagCompound.setBoolean("maintenance", shipCore.isUnderMaintenance());
		tagCompound.setBoolean("busy", shipCore.isBusy());
		tagCompound.setString("shipName", shipCore.name(null)[0]);
		tagCompound.setString("command", shipCore.getCommand().getName());
		tagCompound.setInteger("shipMass", shipCore.shipMass);
		tagCompound.setInteger("shipVolume", shipCore.shipVolume);
		tagCompound.setLong("energyStored", shipCore.energy_getEnergyStored());
		tagCompound.setString("energyUnits", shipCore.energy_getDisplayUnits());
		tagCompound.setTag("dimensions", writeDimensions(shipCore));
		tagCompound.setTag("movement", writeMovement(shipCore));
		tagCompound.setTag("orientation", writeOrientation(shipCore));
		tagCompound.setTag("tier", writeObjectArray(shipCore.getTier(), "index", "name"));
		tagCompound.setTag("upgrades", writeObjectArray(shipCore.getUpgrades(), "upgradeable", "status"));
		tagCompound.setTag("version", writeVersion(shipCore.getVersion()));
		tagCompound.setTag("assembly", writeObjectArray(shipCore.getAssemblyStatus(), "valid", "status"));
		tagCompound.setTag("energyStatus", writeObjectArray(shipCore.getEnergyStatus(), "stored", "capacity", "units"));
		tagCompound.setTag("state", writeObjectArray(shipCore.state(), "status", "enabled", "state", "energy"));
		tagCompound.setTag("driveStatus", writeDriveStatus(shipCore));
		tagCompound.setBoolean("inSpace", boolFromObjectArray(shipCore.isInSpace()));
		tagCompound.setBoolean("inHyperspace", boolFromObjectArray(shipCore.isInHyperspace()));
		tagCompound.setString("navigationTargetId", shipCore.getNavigationTargetId());
		tagCompound.setInteger("mapVersion", getMapVersion());
		tagCompound.setLong("serverTimeMs", shipCore.getWorld().getTotalWorldTime() * 50L);
		if (notice != null && !notice.isEmpty()) {
			tagCompound.setString("notice", notice);
		}

		final CelestialObject celestialObjectCurrent = CelestialObjectManager.get(shipCore.getWorld(), shipCore.getPos().getX(), shipCore.getPos().getZ());
		if (celestialObjectCurrent != null) {
			tagCompound.setString("currentCelestialId", celestialObjectCurrent.id);
			tagCompound.setString("currentCelestialName", celestialObjectCurrent.getDisplayName());
		}

		final CelestialObject celestialObjectTarget = getTarget(shipCore);
		final NBTTagCompound tagHeavy = getHeavySnapshot(shipCore, celestialObjectCurrent, celestialObjectTarget);
		tagCompound.setTag("route", tagHeavy.getCompoundTag("route"));
		tagCompound.setTag("destinations", tagHeavy.getTagList("destinations", 10));
		final ShipMovementPreview previewEffective;
		if (movementPreview == null) {
			final VectorI movement = shipCore.getMovement();
			previewEffective = shipCore.previewMovement(shipCore.getCommand(), movement.x, movement.y, movement.z, shipCore.getRotationSteps());
		} else {
			previewEffective = movementPreview;
		}
		tagCompound.setTag("movementPreview", previewEffective.writeToNBT());
		return tagCompound;
	}

	@Nonnull
	private static NBTTagCompound getHeavySnapshot(@Nonnull final TileEntityShipCore shipCore,
	                                               @Nullable final CelestialObject celestialObjectCurrent,
	                                               @Nullable final CelestialObject celestialObjectTarget) {
		final String cacheKey = String.format("%d|%d|%d|%d|%d|%s|%d|%d|%d|%d|%d|%d|%d",
		                                      getMapVersion(),
		                                      shipCore.getWorld().provider.getDimension(),
		                                      shipCore.getPos().getX(),
		                                      shipCore.getPos().getY(),
		                                      shipCore.getPos().getZ(),
		                                      celestialObjectCurrent == null ? "" : celestialObjectCurrent.id,
		                                      shipCore.getShipScanSignature(),
		                                      shipCore.shipMass,
		                                      shipCore.getTierIndex(),
		                                      shipCore.getFront() + shipCore.getBack(),
		                                      shipCore.getLeft() + shipCore.getRight(),
		                                      shipCore.getUp() + shipCore.getDown(),
		                                      shipCore.getAutopilotMode().ordinal());
		final NBTTagCompound tagCached = shipCore.getNavigationHeavyCache(cacheKey);
		final NBTTagCompound tagHeavy = new NBTTagCompound();
		tagHeavy.setTag("route", buildRoute(shipCore, celestialObjectCurrent, celestialObjectTarget));
		if (tagCached != null) {
			tagHeavy.setTag("destinations", tagCached.getTagList("destinations", 10));
			return tagHeavy;
		}
		final NBTTagCompound tagDestinations = new NBTTagCompound();
		tagDestinations.setTag("destinations", buildDestinations(shipCore, celestialObjectCurrent));
		shipCore.setNavigationHeavyCache(cacheKey, tagDestinations);
		tagHeavy.setTag("destinations", tagDestinations.getTagList("destinations", 10));
		return tagHeavy;
	}

	@Nonnull
	private static NBTTagCompound writeDimensions(@Nonnull final TileEntityShipCore shipCore) {
		final NBTTagCompound tagCompound = new NBTTagCompound();
		tagCompound.setInteger("front", shipCore.getFront());
		tagCompound.setInteger("right", shipCore.getRight());
		tagCompound.setInteger("up", shipCore.getUp());
		tagCompound.setInteger("back", shipCore.getBack());
		tagCompound.setInteger("left", shipCore.getLeft());
		tagCompound.setInteger("down", shipCore.getDown());
		tagCompound.setInteger("minX", shipCore.minX);
		tagCompound.setInteger("minY", shipCore.minY);
		tagCompound.setInteger("minZ", shipCore.minZ);
		tagCompound.setInteger("maxX", shipCore.maxX);
		tagCompound.setInteger("maxY", shipCore.maxY);
		tagCompound.setInteger("maxZ", shipCore.maxZ);
		return tagCompound;
	}

	@Nonnull
	private static NBTTagCompound writeMovement(@Nonnull final TileEntityShipCore shipCore) {
		final VectorI movement = shipCore.getMovement();
		final NBTTagCompound tagCompound = new NBTTagCompound();
		tagCompound.setInteger("front", movement.x);
		tagCompound.setInteger("up", movement.y);
		tagCompound.setInteger("right", movement.z);
		tagCompound.setInteger("rotationSteps", shipCore.getRotationSteps());
		return tagCompound;
	}

	@Nonnull
	private static NBTTagCompound writeOrientation(@Nonnull final TileEntityShipCore shipCore) {
		final Object[] values = shipCore.getOrientation();
		final NBTTagCompound tagCompound = new NBTTagCompound();
		tagCompound.setInteger("x", values.length > 0 && values[0] instanceof Number ? ((Number) values[0]).intValue() : 0);
		tagCompound.setInteger("y", values.length > 1 && values[1] instanceof Number ? ((Number) values[1]).intValue() : 0);
		tagCompound.setInteger("z", values.length > 2 && values[2] instanceof Number ? ((Number) values[2]).intValue() : 0);
		return tagCompound;
	}

	@Nonnull
	private static NBTTagCompound writeVersion(@Nonnull final Integer[] version) {
		final NBTTagCompound tagCompound = new NBTTagCompound();
		tagCompound.setString("text", version.length >= 3 ? version[0] + "." + version[1] + "." + version[2] : WarpDrive.VERSION);
		for (int index = 0; index < version.length; index++) {
			tagCompound.setInteger("part" + index, version[index]);
		}
		return tagCompound;
	}

	@Nonnull
	private static NBTTagCompound writeDriveStatus(@Nonnull final TileEntityShipCore shipCore) {
		final NBTTagCompound tagCompound = new NBTTagCompound();
		tagCompound.setString("stateName", shipCore.getDriveStateName());
		tagCompound.setInteger("warmupTicks", shipCore.getWarmupRemainingTicks());
		tagCompound.setInteger("warmupTotal", Math.max(1, shipCore.getWarmupTotalTicks()));
		tagCompound.setInteger("cooldownTicks", shipCore.getCooldownRemainingTicks());
		tagCompound.setInteger("cooldownTotal", Math.max(1, shipCore.getCooldownTotalTicks()));
		tagCompound.setBoolean("jumpInProgress", shipCore.isJumpInProgress());
		tagCompound.setBoolean("cooling", shipCore.getCooldownRemainingTicks() > 0);
		tagCompound.setString("movementType", shipCore.getShipMovementTypeName());
		tagCompound.setString("autopilotMode", shipCore.getAutopilotMode().getName());
		tagCompound.setString("autopilotStatus", shipCore.getAutopilotStatus().getName());
		tagCompound.setString("autopilotErrorKey", shipCore.getAutopilotLastErrorKey());
		tagCompound.setInteger("autopilotLegs", shipCore.getAutopilotLegsExecuted());
		return tagCompound;
	}

	@Nonnull
	private static NBTTagCompound writeObjectArray(@Nullable final Object[] values, @Nonnull final String... keys) {
		final NBTTagCompound tagCompound = new NBTTagCompound();
		if (values == null) {
			return tagCompound;
		}
		for (int index = 0; index < keys.length && index < values.length; index++) {
			final Object value = values[index];
			if (value instanceof Boolean) {
				tagCompound.setBoolean(keys[index], (Boolean) value);
			} else if (value instanceof Number) {
				tagCompound.setLong(keys[index], ((Number) value).longValue());
			} else if (value != null) {
				tagCompound.setString(keys[index], value.toString());
			}
		}
		return tagCompound;
	}

	private static boolean boolFromObjectArray(@Nullable final Object[] values) {
		return values != null && values.length > 0 && values[0] instanceof Boolean && (Boolean) values[0];
	}

	@Nonnull
	public static ShipMovementPreview previewMovement(@Nonnull final TileEntityShipCore shipCore,
	                                                  @Nonnull final NBTTagCompound payload) {
		final EnumShipCommand command = parseShipCommand(payload.getString("command"));
		if (command == null) {
			return shipCore.previewMovement(EnumShipCommand.IDLE, 0, 0, 0, (byte) 0);
		}
		final ShipMovementPreview preview = shipCore.previewMovement(command,
		                                                             payload.getInteger("moveFront"),
		                                                             payload.getInteger("moveUp"),
		                                                             payload.getInteger("moveRight"),
		                                                             payload.getByte("rotationSteps"));
		if ("warpdrive.navigation.blocker.stale_scan".equals(preview.blockerKey)) {
			shipCore.requestShipScan();
		}
		return preview;
	}

	public static boolean executeMovement(@Nonnull final TileEntityShipCore shipCore,
	                                      @Nonnull final NBTTagCompound payload) {
		return executeMovement(shipCore, payload, null);
	}
	
	public static boolean executeMovement(@Nonnull final TileEntityShipCore shipCore,
	                                      @Nonnull final NBTTagCompound payload,
	                                      @Nullable final ShipMovementPreview previewCached) {
		final EnumShipCommand command = parseShipCommand(payload.getString("command"));
		if (command == null) {
			return false;
		}
		// a manual command from the Move/Drive tab cancels any running autopilot route (player took over)
		shipCore.cancelAutopilot();
		if (!command.isMovement()) {
			shipCore.command(new Object[] { command.getName(), true });
			return true;
		}
		final ShipMovementPreview preview = previewCached == null ? previewMovement(shipCore, payload) : previewCached;
		if (!preview.canEngage) {
			return false;
		}
		shipCore.movement(new Object[] { preview.effectiveMovement.x, preview.effectiveMovement.y, preview.effectiveMovement.z });
		shipCore.rotationSteps(new Object[] { (int) preview.rotationSteps });
		shipCore.command(new Object[] { command.getName(), true });
		return true;
	}

	public static void applySettings(@Nonnull final TileEntityShipCore shipCore,
	                                 @Nonnull final NBTTagCompound payload) {
		if (payload.hasKey("name")) {
			shipCore.name(new Object[] { payload.getString("name") });
		}
		if (payload.hasKey("enable")) {
			shipCore.enable(new Object[] { payload.getBoolean("enable") });
		}
		if (payload.hasKey("energyDisplayUnits")) {
			shipCore.energyDisplayUnits(new Object[] { payload.getString("energyDisplayUnits") });
		}
		if (payload.hasKey("dim_positive")) {
			final NBTTagCompound dimensions = payload.getCompoundTag("dim_positive");
			shipCore.dim_positive(new Object[] {
					dimensions.getInteger("front"),
					dimensions.getInteger("right"),
					dimensions.getInteger("up")
			});
		}
		if (payload.hasKey("dim_negative")) {
			final NBTTagCompound dimensions = payload.getCompoundTag("dim_negative");
			shipCore.dim_negative(new Object[] {
					dimensions.getInteger("back"),
					dimensions.getInteger("left"),
					dimensions.getInteger("down")
			});
		}
	}

	@Nullable
	public static EnumShipCommand parseShipCommand(@Nullable final String name) {
		if (name == null) {
			return null;
		}
		for (final EnumShipCommand command : EnumShipCommand.values()) {
			if ( command.name().equalsIgnoreCase(name)
			  || command.getName().equalsIgnoreCase(name) ) {
				return command;
			}
		}
		return null;
	}

	public static int getMapVersion() {
		int hash = MAP_VERSION;
		for (final CelestialObject celestialObject : CelestialObjectManager.getRegistrySnapshot(false)) {
			hash = 31 * hash + celestialObject.id.hashCode();
			hash = 31 * hash + celestialObject.getDisplayName().hashCode();
			hash = 31 * hash + celestialObject.dimensionId;
			hash = 31 * hash + celestialObject.getParentCenterX();
			hash = 31 * hash + celestialObject.getParentCenterZ();
			hash = 31 * hash + celestialObject.borderRadiusX;
			hash = 31 * hash + celestialObject.borderRadiusZ;
			hash = 31 * hash + (celestialObject.isVirtual() ? 1 : 0);
			hash = 31 * hash + (celestialObject.isSpace() ? 1 : 0);
			hash = 31 * hash + (celestialObject.isHyperspace() ? 1 : 0);
			for (final CelestialObject.RenderData renderData : celestialObject.setRenderData) {
				hash = 31 * hash + Float.floatToIntBits(renderData.red);
				hash = 31 * hash + Float.floatToIntBits(renderData.green);
				hash = 31 * hash + Float.floatToIntBits(renderData.blue);
				hash = 31 * hash + (renderData.texture == null ? 0 : renderData.texture.hashCode());
			}
			if (celestialObject.boxTextures != null) {
				for (int index = 0; index < celestialObject.boxTextures.length; index++) {
					hash = 31 * hash + celestialObject.boxTextures[index].toString().hashCode();
				}
			}
		}
		return hash;
	}

	@Nonnull
	public static NBTTagCompound buildStaticMapSnapshot() {
		final NBTTagCompound tagCompound = new NBTTagCompound();
		tagCompound.setInteger("mapVersion", getMapVersion());
		final NBTTagList tagListObjects = new NBTTagList();
		for (final CelestialObject celestialObject : CelestialObjectManager.getRegistrySnapshot(false)) {
			tagListObjects.appendTag(writeCelestialObject(celestialObject));
		}
		tagCompound.setTag("celestialObjects", tagListObjects);
		return tagCompound;
	}

	@Nonnull
	@SuppressWarnings("PMD.NPathComplexity")
	private static NBTTagCompound writeCelestialObject(@Nonnull final CelestialObject celestialObject) {
		final NBTTagCompound tagCompound = new NBTTagCompound();
		tagCompound.setString("id", celestialObject.id);
		tagCompound.setString("parentId", celestialObject.parentId == null ? "" : celestialObject.parentId);
		tagCompound.setString("name", celestialObject.getDisplayName());
		tagCompound.setString("description", celestialObject.getDescription());
		tagCompound.setBoolean("virtual", celestialObject.isVirtual());
		tagCompound.setBoolean("space", celestialObject.isSpace());
		tagCompound.setBoolean("hyperspace", celestialObject.isHyperspace());
		tagCompound.setBoolean("atmosphere", celestialObject.hasAtmosphere());
		tagCompound.setInteger("dimensionId", celestialObject.dimensionId);
		tagCompound.setInteger("dimensionCenterX", celestialObject.dimensionCenterX);
		tagCompound.setInteger("dimensionCenterZ", celestialObject.dimensionCenterZ);
		tagCompound.setInteger("parentCenterX", celestialObject.getParentCenterX());
		tagCompound.setInteger("parentCenterZ", celestialObject.getParentCenterZ());
		tagCompound.setInteger("borderRadiusX", celestialObject.borderRadiusX);
		tagCompound.setInteger("borderRadiusZ", celestialObject.borderRadiusZ);

		float red = celestialObject.isHyperspace() ? 0.85F : celestialObject.isSpace() ? 0.25F : 0.45F;
		float green = celestialObject.isHyperspace() ? 0.20F : celestialObject.isSpace() ? 0.55F : 0.75F;
		float blue = celestialObject.isHyperspace() ? 0.20F : celestialObject.isSpace() ? 1.00F : 1.00F;
		if (!celestialObject.setRenderData.isEmpty()) {
			final CelestialObject.RenderData renderData = celestialObject.setRenderData.iterator().next();
			red = renderData.red;
			green = renderData.green;
			blue = renderData.blue;
		}
		for (final CelestialObject.RenderData renderData : celestialObject.setRenderData) {
			if (renderData.texture != null) {
				tagCompound.setString("iconTexture", renderData.texture);
				break;
			}
		}
		if (!tagCompound.hasKey("iconTexture") && celestialObject.boxTextures != null && celestialObject.boxTextures.length > 0) {
			tagCompound.setString("iconTexture", celestialObject.boxTextures[0].toString());
		}
		tagCompound.setFloat("red", red);
		tagCompound.setFloat("green", green);
		tagCompound.setFloat("blue", blue);
		return tagCompound;
	}

	@Nonnull
	@SuppressWarnings("PMD.NPathComplexity")
	private static NBTTagCompound buildRoute(@Nonnull final TileEntityShipCore shipCore,
	                                         @Nullable final CelestialObject celestialObjectCurrent,
	                                         @Nullable final CelestialObject celestialObjectTarget) {
		final NBTTagCompound tagCompound = new NBTTagCompound();
		final NBTTagList tagListLegs = new NBTTagList();
		if (celestialObjectTarget == null) {
			return route(tagCompound, tagListLegs, false, "warpdrive.navigation.route.select_destination", "");
		}
		if (celestialObjectCurrent == null) {
			return route(tagCompound, tagListLegs, false, "warpdrive.navigation.route.no_celestial_here", "warpdrive.navigation.route.move_into_region");
		}
		if (celestialObjectTarget.isVirtual()) {
			return route(tagCompound, tagListLegs, false, "warpdrive.navigation.route.visual_only", "warpdrive.navigation.route.no_dimension");
		}
		if (isAtNavigationDestination(shipCore, celestialObjectCurrent, celestialObjectTarget)) {
			return route(tagCompound, tagListLegs, false, "warpdrive.navigation.route.already_at_destination", "");
		}

		final Leg nextLeg = computeNextLeg(shipCore, celestialObjectCurrent, celestialObjectTarget);
		if (nextLeg == null) {
			return route(tagCompound, tagListLegs, false, "warpdrive.navigation.route.no_route", "warpdrive.navigation.route.no_shared_hierarchy");
		}

		final ShipMovementPreview preview = nextLeg.preview == null
		                                  ? shipCore.previewMovement(nextLeg.command, nextLeg.moveFront, nextLeg.moveUp, nextLeg.moveRight, (byte) 0)
		                                  : nextLeg.preview;
		tagCompound.setBoolean("canEngage", preview.canEngage);
		tagCompound.setString("statusKey", "warpdrive.navigation.route.ready");
		tagCompound.setString("warningKey", nextLeg.warningKey);
		tagCompound.setInteger("warningStep", nextLeg.stepDistance);
		tagCompound.setInteger("warningRemaining", nextLeg.remainingDistance);
		tagCompound.setString("blockerKey", preview.canEngage ? "" : preview.blockerKey);
		tagCompound.setString("blockerMessage", preview.canEngage ? "" : preview.blockerMessage);
		tagCompound.setTag("validation", preview.writeToNBT());
		tagListLegs.appendTag(writeLeg(nextLeg, preview));
		appendPreviewLegs(shipCore, tagListLegs, celestialObjectCurrent, celestialObjectTarget, nextLeg);
		tagCompound.setTag("legs", tagListLegs);
		return tagCompound;
	}

	@Nonnull
	private static NBTTagCompound route(@Nonnull final NBTTagCompound tagCompound, @Nonnull final NBTTagList tagListLegs,
	                                    final boolean canEngage, @Nonnull final String statusKey, @Nonnull final String warningKey) {
		tagCompound.setBoolean("canEngage", canEngage);
		tagCompound.setString("statusKey", statusKey);
		tagCompound.setString("warningKey", warningKey);
		tagCompound.setTag("legs", tagListLegs);
		return tagCompound;
	}

	private static void appendPreviewLegs(@Nonnull final TileEntityShipCore shipCore,
	                                      @Nonnull final NBTTagList tagListLegs,
	                                      @Nonnull final CelestialObject celestialObjectCurrent,
	                                      @Nonnull final CelestialObject celestialObjectTarget,
	                                      @Nonnull final Leg nextLeg) {
		if (nextLeg.type == EnumShipNavigationLegType.TAKEOFF) {
			tagListLegs.appendTag(writePreviewLeg(EnumShipNavigationLegType.SPACE_CRUISE));
		}
		final CelestialObject targetSpace = getSpaceFor(celestialObjectTarget);
		if (targetSpace != null && !celestialObjectCurrent.isHyperspace() && !celestialObjectCurrent.isSpace()
		    && celestialObjectCurrent.parent != targetSpace) {
			tagListLegs.appendTag(writePreviewLeg(EnumShipNavigationLegType.HYPERSPACE_ENTER));
		}
		if (targetSpace != null && celestialObjectCurrent.isHyperspace()) {
			tagListLegs.appendTag(writePreviewLeg(EnumShipNavigationLegType.HYPERSPACE_EXIT));
		}
		if (!celestialObjectTarget.isSpace() && !celestialObjectTarget.isHyperspace()) {
			tagListLegs.appendTag(writePreviewLeg(EnumShipNavigationLegType.ORBITAL_APPROACH));
			if (canLandOnPlanet(shipCore)) {
				tagListLegs.appendTag(writePreviewLeg(EnumShipNavigationLegType.LANDING));
			}
		}
	}

	@Nonnull
	private static NBTTagCompound writePreviewLeg(@Nonnull final EnumShipNavigationLegType type) {
		final NBTTagCompound tagCompound = new NBTTagCompound();
		tagCompound.setString("type", type.getName());
		tagCompound.setString("command", "preview");
		tagCompound.setInteger("moveFront", 0);
		tagCompound.setInteger("moveUp", 0);
		tagCompound.setInteger("moveRight", 0);
		tagCompound.setBoolean("requiresConfirmation", type.requiresConfirmation());
		tagCompound.setBoolean("preview", true);
		tagCompound.setInteger("stepDistance", 0);
		tagCompound.setInteger("remainingDistance", 0);
		return tagCompound;
	}

	@Nonnull
	private static NBTTagCompound writeLeg(@Nonnull final Leg leg, @Nonnull final ShipMovementPreview preview) {
		final NBTTagCompound tagCompound = new NBTTagCompound();
		tagCompound.setString("type", leg.type.getName());
		tagCompound.setString("command", leg.command.getName());
		tagCompound.setInteger("moveFront", leg.moveFront);
		tagCompound.setInteger("moveUp", leg.moveUp);
		tagCompound.setInteger("moveRight", leg.moveRight);
		tagCompound.setBoolean("requiresConfirmation", leg.requiresConfirmation);
		tagCompound.setBoolean("preview", false);
		tagCompound.setString("warningKey", leg.warningKey);
		tagCompound.setInteger("stepDistance", leg.stepDistance);
		tagCompound.setInteger("remainingDistance", leg.remainingDistance);
		tagCompound.setInteger("effectiveDistance", preview.effectiveDistance);
		tagCompound.setInteger("maximumDistance", preview.maximumDistance);
		tagCompound.setInteger("energyRequired", preview.energyRequired);
		return tagCompound;
	}

	// ----- destinations overview (jumps / energy / eta to every reachable body) -----

	@Nonnull
	@SuppressWarnings("PMD.NPathComplexity")
	private static NBTTagList buildDestinations(@Nonnull final TileEntityShipCore shipCore,
	                                            @Nullable final CelestialObject celestialObjectCurrent) {
		final NBTTagList tagList = new NBTTagList();
		for (final CelestialObject celestialObject : CelestialObjectManager.getRegistrySnapshot(false)) {
			if (celestialObject.isVirtual()) {
				continue;
			}
			final NBTTagCompound tagCompound = new NBTTagCompound();
			tagCompound.setString("id", celestialObject.id);
			tagCompound.setString("parentId", celestialObject.parentId == null ? "" : celestialObject.parentId);
			tagCompound.setString("name", celestialObject.getDisplayName());
			tagCompound.setString("type", celestialObject.isHyperspace() ? "hyperspace" : celestialObject.isSpace() ? "space" : "body");
			tagCompound.setInteger("mapX", celestialObject.parentId == null ? celestialObject.dimensionCenterX : celestialObject.getParentCenterX());
			tagCompound.setInteger("mapZ", celestialObject.parentId == null ? celestialObject.dimensionCenterZ : celestialObject.getParentCenterZ());
			final boolean isCurrent = celestialObjectCurrent != null
			                       && isAtNavigationDestination(shipCore, celestialObjectCurrent, celestialObject);
			tagCompound.setBoolean("current", isCurrent);

			final DestinationEstimate estimate = isCurrent || celestialObjectCurrent == null
			                                     ? null
			                                     : estimateDestination(shipCore, celestialObjectCurrent, celestialObject);
			if (estimate == null) {
				tagCompound.setBoolean("reachable", isCurrent);
				tagCompound.setInteger("legs", 0);
				tagCompound.setInteger("jumps", 0);
				tagCompound.setLong("energy", 0L);
				tagCompound.setInteger("eta", 0);
				tagCompound.setInteger("maxRange", 0);
				tagCompound.setInteger("distance", 0);
			} else {
				tagCompound.setBoolean("reachable", true);
				tagCompound.setInteger("legs", estimate.legs);
				tagCompound.setInteger("jumps", estimate.jumps);
				tagCompound.setLong("energy", estimate.energy);
				tagCompound.setInteger("eta", estimate.eta);
				tagCompound.setInteger("maxRange", estimate.maxRange);
				tagCompound.setInteger("distance", estimate.distance);
			}
			tagList.appendTag(tagCompound);
		}
		return tagList;
	}

	@Nullable
	@SuppressWarnings("PMD.NPathComplexity")
	private static DestinationEstimate estimateDestination(@Nonnull final TileEntityShipCore shipCore,
	                                                       @Nonnull final CelestialObject celestialObjectCurrent,
	                                                       @Nonnull final CelestialObject celestialObjectTarget) {
		if ( celestialObjectTarget.isVirtual()
		  || isAtNavigationDestination(shipCore, celestialObjectCurrent, celestialObjectTarget) ) {
			return null;
		}
		final DestinationEstimate estimate = new DestinationEstimate();
		CelestialObject currentSpace;
		int currentX;
		int currentZ;
		if (!celestialObjectCurrent.isSpace() && !celestialObjectCurrent.isHyperspace()) {
			if (celestialObjectCurrent.parent == null) {
				return null;
			}
			addSingleLegEstimate(shipCore, estimate, EnumShipNavigationLegType.TAKEOFF,
			                     Math.max(1, 256 - shipCore.maxY + TAKEOFF_MARGIN_BLOCKS));
			currentSpace = getSpaceFor(celestialObjectCurrent);
			currentX = celestialObjectCurrent.getParentCenterX();
			currentZ = celestialObjectCurrent.getParentCenterZ();
		} else {
			currentSpace = celestialObjectCurrent.isHyperspace() ? null : celestialObjectCurrent;
			currentX = shipCore.getPos().getX();
			currentZ = shipCore.getPos().getZ();
		}

		if (celestialObjectTarget.isHyperspace()) {
			addSingleLegEstimate(shipCore, estimate, EnumShipNavigationLegType.HYPERSPACE_ENTER, 1);
			return estimate;
		}
		final CelestialObject targetSpace = getSpaceFor(celestialObjectTarget);
		if (targetSpace == null) {
			return null;
		}
		if (currentSpace == null) {
			final int targetX = targetSpace.getParentCenterX();
			final int targetZ = targetSpace.getParentCenterZ();
			addCruiseEstimate(shipCore, estimate, EnumShipNavigationLegType.HYPERSPACE_CRUISE,
			                  targetX - currentX, ORBIT_Y - shipCore.getPos().getY(), targetZ - currentZ);
			addSingleLegEstimate(shipCore, estimate, EnumShipNavigationLegType.HYPERSPACE_EXIT, 1);
			currentSpace = targetSpace;
			currentX = targetSpace.dimensionCenterX;
			currentZ = targetSpace.dimensionCenterZ;
		} else if (currentSpace != targetSpace) {
			addSingleLegEstimate(shipCore, estimate, EnumShipNavigationLegType.HYPERSPACE_ENTER, 1);
			addCruiseEstimate(shipCore, estimate, EnumShipNavigationLegType.HYPERSPACE_CRUISE,
			                  targetSpace.getParentCenterX() - currentSpace.getParentCenterX(),
			                  ORBIT_Y - shipCore.getPos().getY(),
			                  targetSpace.getParentCenterZ() - currentSpace.getParentCenterZ());
			addSingleLegEstimate(shipCore, estimate, EnumShipNavigationLegType.HYPERSPACE_EXIT, 1);
			currentSpace = targetSpace;
			currentX = targetSpace.dimensionCenterX;
			currentZ = targetSpace.dimensionCenterZ;
		}

		if (celestialObjectTarget.isSpace()) {
			return estimate.legs == 0 ? null : estimate;
		}
		if (celestialObjectTarget.parent != currentSpace) {
			return null;
		}
		if (!celestialObjectTarget.isInOrbit(currentSpace.dimensionId, currentX, currentZ)) {
			addCruiseEstimate(shipCore, estimate, EnumShipNavigationLegType.ORBITAL_APPROACH,
			                  celestialObjectTarget.getParentCenterX() - currentX,
			                  ORBIT_Y - shipCore.getPos().getY(),
			                  celestialObjectTarget.getParentCenterZ() - currentZ);
		}
		if (canLandOnPlanet(shipCore)) {
			addSingleLegEstimate(shipCore, estimate, EnumShipNavigationLegType.LANDING,
			                     Math.max(1, Math.abs(shipCore.minY + LANDING_MARGIN_BLOCKS)));
		}
		return estimate.legs == 0 ? null : estimate;
	}

	private static void addSingleLegEstimate(@Nonnull final TileEntityShipCore shipCore,
	                                         @Nonnull final DestinationEstimate estimate,
	                                         @Nonnull final EnumShipNavigationLegType legType,
	                                         final int distance) {
		final EnumShipMovementType movementType = movementTypeForEstimate(legType);
		final ShipMovementCosts costs = new ShipMovementCosts(shipCore.getWorld(), shipCore.getPos(), shipCore,
		                                                      movementType, shipCore.shipMass, Math.max(1, distance));
		estimate.addLeg(Math.max(1, distance), costs);
	}

	private static void addCruiseEstimate(@Nonnull final TileEntityShipCore shipCore,
	                                      @Nonnull final DestinationEstimate estimate,
	                                      @Nonnull final EnumShipNavigationLegType legType,
	                                      final int moveX, final int moveY, final int moveZ) {
		final VectorI remaining = toLocalMovement(shipCore, moveX, moveY, moveZ);
		if (remaining.getMagnitudeSquared() <= 0L) {
			return;
		}
		final EnumShipMovementType movementType = movementTypeForEstimate(legType);
		for (int guard = 0; guard < 100000 && remaining.getMagnitudeSquared() > 0L; guard++) {
			final ShipMovementPreview preview = shipCore.previewMovement(legType.getCommand(),
			                                                             remaining.x, remaining.y, remaining.z,
			                                                             (byte) 0);
			final VectorI step = preview.effectiveMovement;
			if (step.getMagnitudeSquared() <= 0L) {
				break;
			}
			final int stepDistance = Math.max(1, (int) Math.ceil(Math.sqrt(step.getMagnitudeSquared())));
			final ShipMovementCosts stepCosts = new ShipMovementCosts(shipCore.getWorld(), shipCore.getPos(), shipCore,
			                                                          movementType, shipCore.shipMass, stepDistance);
			estimate.addLeg(stepDistance, stepCosts);
			remaining.x -= step.x;
			remaining.y -= step.y;
			remaining.z -= step.z;
		}
	}

	private static final class DestinationEstimate {
		private int legs;
		private int jumps;
		private long energy;
		private int eta;
		private int maxRange;
		private int distance;

		private void addLeg(final int distance, @Nonnull final ShipMovementCosts costs) {
			addLegs(1, distance, costs);
		}

		private void addLegs(final int count, final long distance, @Nonnull final ShipMovementCosts costs) {
			legs += count;
			jumps += count;
			energy += (long) count * costs.energyRequired;
			eta += count * (costs.warmup_seconds + costs.cooldown_seconds);
			maxRange = Math.max(maxRange, costs.maximumDistance_blocks);
			this.distance = (int) Math.min(Integer.MAX_VALUE, this.distance + distance);
		}
	}

	@Nonnull
	private static EnumShipMovementType movementTypeForEstimate(@Nonnull final EnumShipNavigationLegType legType) {
		switch (legType) {
		case TAKEOFF:           return EnumShipMovementType.PLANET_TAKEOFF;
		case LANDING:           return EnumShipMovementType.PLANET_LANDING;
		case SPACE_CRUISE:
		case ORBITAL_APPROACH:  return EnumShipMovementType.SPACE_MOVING;
		case HYPERSPACE_ENTER:  return EnumShipMovementType.HYPERSPACE_ENTERING;
		case HYPERSPACE_EXIT:   return EnumShipMovementType.HYPERSPACE_EXITING;
		case HYPERSPACE_CRUISE: return EnumShipMovementType.HYPERSPACE_MOVING;
		default:                return EnumShipMovementType.SPACE_MOVING;
		}
	}

	// ----- destination planning / engagement -----

	public static boolean setDestination(@Nonnull final EntityPlayerMP entityPlayerMP,
	                                     @Nonnull final TileEntityShipCore shipCore,
	                                     @Nonnull final String celestialObjectId) {
		if (!shipCore.isCrewMember(entityPlayerMP)) {
			Commons.addChatMessage(entityPlayerMP, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.navigation.denied"));
			return false;
		}
		final CelestialObject celestialObject = CelestialObjectManager.get(false, celestialObjectId);
		if (celestialObject == null) {
			Commons.addChatMessage(entityPlayerMP, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.navigation.unknown_destination", celestialObjectId));
			return false;
		}
		shipCore.setNavigationTargetId(celestialObjectId);
		return true;
	}

	public static boolean cancel(@Nonnull final EntityPlayerMP entityPlayerMP,
	                             @Nonnull final TileEntityShipCore shipCore) {
		if (!shipCore.isCrewMember(entityPlayerMP)) {
			Commons.addChatMessage(entityPlayerMP, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.navigation.denied"));
			return false;
		}
		shipCore.cancelAutopilot();
		shipCore.clearNavigationTarget();
		return true;
	}

	@SuppressWarnings("PMD.NPathComplexity")
	public static boolean engage(@Nonnull final EntityPlayerMP entityPlayerMP,
	                             @Nonnull final TileEntityShipCore shipCore) {
		if (!shipCore.isCrewMember(entityPlayerMP)) {
			Commons.addChatMessage(entityPlayerMP, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.navigation.denied"));
			return false;
		}
		if (shipCore.isOffline() || shipCore.isUnderMaintenance() || shipCore.isBusy()) {
			Commons.addChatMessage(entityPlayerMP, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.navigation.busy"));
			return false;
		}

		final CelestialObject celestialObjectCurrent = CelestialObjectManager.get(shipCore.getWorld(), shipCore.getPos().getX(), shipCore.getPos().getZ());
		final CelestialObject celestialObjectTarget = getTarget(shipCore);
		if (celestialObjectCurrent == null || celestialObjectTarget == null) {
			Commons.addChatMessage(entityPlayerMP, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.navigation.no_route"));
			return false;
		}
		if (isAtNavigationDestination(shipCore, celestialObjectCurrent, celestialObjectTarget)) {
			shipCore.cancelAutopilot();
			shipCore.clearNavigationTarget();
			Commons.addChatMessage(entityPlayerMP, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.navigation.arrived"));
			return true;
		}

		final Leg leg = computeNextLeg(shipCore, celestialObjectCurrent, celestialObjectTarget);
		if (leg == null) {
			Commons.addChatMessage(entityPlayerMP, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.navigation.no_route"));
			return false;
		}
		final ShipMovementPreview preview = leg.preview == null
		                                  ? shipCore.previewMovement(leg.command, leg.moveFront, leg.moveUp, leg.moveRight, (byte) 0)
		                                  : leg.preview;
		if (!preview.canEngage) {
			if ("warpdrive.navigation.blocker.stale_scan".equals(preview.blockerKey)) {
				shipCore.requestShipScan();
			}
			Commons.addChatMessage(entityPlayerMP, new WarpDriveText(Commons.getStyleWarning(),
			                                                         preview.blockerKey.isEmpty() ? "warpdrive.navigation.no_route" : preview.blockerKey));
			return false;
		}

		// the click explicitly confirms this leg; the autopilot pump chains subsequent legs per the ship's mode
		commitLeg(shipCore, celestialObjectTarget.id, leg);
		shipCore.startAutopilotRun(false);
		Commons.messageToAllPlayersInArea(shipCore, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.navigation.engaging",
		                                                               new WarpDriveText(null, leg.type.getTitleKey()), celestialObjectTarget.getDisplayName()));
		return true;
	}

	/** Engage exactly one leg, then halt at the next decision point regardless of autopilot mode. */
	public static boolean step(@Nonnull final EntityPlayerMP entityPlayerMP,
	                           @Nonnull final TileEntityShipCore shipCore) {
		final boolean ok = engage(entityPlayerMP, shipCore);
		if (ok) {
			shipCore.startAutopilotRun(true);
		}
		return ok;
	}

	public static boolean setAutopilotMode(@Nonnull final EntityPlayerMP entityPlayerMP,
	                                       @Nonnull final TileEntityShipCore shipCore,
	                                       @Nullable final String mode) {
		if (!shipCore.isCrewMember(entityPlayerMP)) {
			Commons.addChatMessage(entityPlayerMP, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.navigation.denied"));
			return false;
		}
		shipCore.setAutopilotMode(cr0s.warpdrive.data.EnumShipAutopilotMode.get(mode));
		return true;
	}

	public static boolean pause(@Nonnull final EntityPlayerMP entityPlayerMP, @Nonnull final TileEntityShipCore shipCore) {
		if (!shipCore.isCrewMember(entityPlayerMP)) {
			return false;
		}
		shipCore.pauseAutopilot();
		return true;
	}

	public static boolean resume(@Nonnull final EntityPlayerMP entityPlayerMP, @Nonnull final TileEntityShipCore shipCore) {
		if (!shipCore.isCrewMember(entityPlayerMP)) {
			return false;
		}
		shipCore.resumeAutopilot();
		return true;
	}

	/** Apply a leg's movement + command on the core, marking the final destination as the engaged target. */
	public static void commitLeg(@Nonnull final TileEntityShipCore shipCore, @Nonnull final String engagedTargetId, @Nonnull final Leg leg) {
		shipCore.setNavigationEngagedTargetId(engagedTargetId);
		shipCore.movement(new Object[] { leg.moveFront, leg.moveUp, leg.moveRight });
		shipCore.rotationSteps(new Object[] { 0 });
		shipCore.command(new Object[] { leg.command.getName(), true });
	}

	@Nullable
	public static ShipMovementPreview previewNavigation(@Nonnull final TileEntityShipCore shipCore) {
		final Leg leg = computeNextLeg(shipCore);
		if (leg == null) {
			return null;
		}
		return shipCore.previewMovement(leg.command, leg.moveFront, leg.moveUp, leg.moveRight, (byte) 0);
	}

	@Nullable
	private static CelestialObject getTarget(@Nonnull final TileEntityShipCore shipCore) {
		final String navigationTargetId = shipCore.getNavigationTargetId();
		if (navigationTargetId == null || navigationTargetId.isEmpty()) {
			return null;
		}
		return CelestialObjectManager.get(false, navigationTargetId);
	}

	/** Resolve current/target from live ship state and compute the next leg (used by the autopilot pump). */
	@Nullable
	public static Leg computeNextLeg(@Nonnull final TileEntityShipCore shipCore) {
		final CelestialObject celestialObjectCurrent = CelestialObjectManager.get(shipCore.getWorld(), shipCore.getPos().getX(), shipCore.getPos().getZ());
		final CelestialObject celestialObjectTarget = getTarget(shipCore);
		if ( celestialObjectCurrent == null
		  || celestialObjectTarget == null
		  || celestialObjectTarget.isVirtual()
		  || isAtNavigationDestination(shipCore, celestialObjectCurrent, celestialObjectTarget) ) {
			return null;
		}
		return computeNextLeg(shipCore, celestialObjectCurrent, celestialObjectTarget);
	}

	@Nullable
	@SuppressWarnings("PMD.NPathComplexity")
	private static Leg computeNextLeg(@Nonnull final TileEntityShipCore shipCore,
	                                  @Nonnull final CelestialObject celestialObjectCurrent,
	                                  @Nonnull final CelestialObject celestialObjectTarget) {
		if (celestialObjectTarget.isVirtual()) {
			return null;
		}

		if (!celestialObjectCurrent.isSpace() && !celestialObjectCurrent.isHyperspace()) {
			if (celestialObjectCurrent.parent == null) {
				return null;
			}
			return new Leg(EnumShipNavigationLegType.TAKEOFF,
			               0, Math.max(1, 256 - shipCore.maxY + TAKEOFF_MARGIN_BLOCKS), 0, "", 0, 0);
		}

		if (celestialObjectCurrent.isSpace()) {
			if (celestialObjectTarget.isHyperspace()) {
				return new Leg(EnumShipNavigationLegType.HYPERSPACE_ENTER, 0, 0, 0, "", 0, 0);
			}
			if (celestialObjectTarget.isSpace()) {
				if (celestialObjectCurrent.id.equals(celestialObjectTarget.id)) {
					return null;
				}
				return new Leg(EnumShipNavigationLegType.HYPERSPACE_ENTER, 0, 0, 0, "", 0, 0);
			}

			if (isParentOf(celestialObjectCurrent, celestialObjectTarget)) {
				if (celestialObjectTarget.isInOrbit(shipCore.getWorld().provider.getDimension(), shipCore.getPos().getX(), shipCore.getPos().getZ())) {
					if (!canLandOnPlanet(shipCore)) {
						return null;
					}
					return new Leg(EnumShipNavigationLegType.LANDING,
					               0, -Math.max(1, shipCore.minY + LANDING_MARGIN_BLOCKS), 0, "", 0, 0);
				}
				return createApproachLeg(shipCore, EnumShipNavigationLegType.ORBITAL_APPROACH,
				                         celestialObjectTarget.getParentCenterX(), celestialObjectTarget.getParentCenterZ());
			}

			final CelestialObject targetSpace = getSpaceFor(celestialObjectTarget);
			if (targetSpace != null && targetSpace != celestialObjectCurrent) {
				return new Leg(EnumShipNavigationLegType.HYPERSPACE_ENTER, 0, 0, 0, "", 0, 0);
			}
		}

		if (celestialObjectCurrent.isHyperspace()) {
			final CelestialObject targetSpace = getSpaceFor(celestialObjectTarget);
			if (targetSpace == null) {
				return null;
			}
			if (targetSpace.isInOrbit(shipCore.getWorld().provider.getDimension(), shipCore.getPos().getX(), shipCore.getPos().getZ())) {
				return new Leg(EnumShipNavigationLegType.HYPERSPACE_EXIT, 0, 0, 0, "", 0, 0);
			}
			return createApproachLeg(shipCore, EnumShipNavigationLegType.HYPERSPACE_CRUISE,
			                         targetSpace.getParentCenterX(), targetSpace.getParentCenterZ());
		}

		return null;
	}

	public static boolean isAtNavigationDestination(@Nonnull final TileEntityShipCore shipCore,
	                                                @Nullable final CelestialObject celestialObjectCurrent,
	                                                @Nullable final CelestialObject celestialObjectTarget) {
		if ( celestialObjectCurrent == null
		  || celestialObjectTarget == null
		  || celestialObjectTarget.isVirtual() ) {
			return false;
		}
		if (celestialObjectCurrent.id.equals(celestialObjectTarget.id)) {
			return true;
		}
		if ( canLandOnPlanet(shipCore)
		  || !celestialObjectCurrent.isSpace()
		  || celestialObjectTarget.isSpace()
		  || celestialObjectTarget.isHyperspace()
		  || !isParentOf(celestialObjectCurrent, celestialObjectTarget) ) {
			return false;
		}
		return celestialObjectTarget.isInOrbit(shipCore.getWorld().provider.getDimension(),
		                                       shipCore.getPos().getX(), shipCore.getPos().getZ());
	}

	private static boolean isParentOf(@Nonnull final CelestialObject celestialObjectCurrent,
	                                  @Nonnull final CelestialObject celestialObjectTarget) {
		return celestialObjectTarget.parent != null
		    && ( celestialObjectTarget.parent == celestialObjectCurrent
		      || celestialObjectTarget.parent.id.equals(celestialObjectCurrent.id) );
	}

	private static boolean canLandOnPlanet(@Nonnull final TileEntityShipCore shipCore) {
		if (!shipCore.isShipScanReady()) {
			return true;
		}
		final int shipHeight = Math.max(1, shipCore.maxY - shipCore.minY + 1);
		return shipHeight + LANDING_MARGIN_BLOCKS <= 256
		    && shipCore.shipMass <= WarpDriveConfig.SHIP_MASS_MAX_ON_PLANET_SURFACE;
	}

	@Nullable
	private static CelestialObject getSpaceFor(@Nonnull final CelestialObject celestialObject) {
		if (celestialObject.isSpace()) {
			return celestialObject;
		}
		CelestialObject celestialObjectParent = celestialObject.parent;
		while (celestialObjectParent != null) {
			if (celestialObjectParent.isSpace()) {
				return celestialObjectParent;
			}
			celestialObjectParent = celestialObjectParent.parent;
		}
		return null;
	}

	@Nonnull
	private static Leg createApproachLeg(@Nonnull final TileEntityShipCore shipCore,
	                                     @Nonnull final EnumShipNavigationLegType type,
	                                     final int targetX, final int targetZ) {
		final int moveX = targetX - shipCore.getPos().getX();
		final int moveZ = targetZ - shipCore.getPos().getZ();
		final VectorI movement = toLocalMovement(shipCore, moveX, ORBIT_Y - shipCore.getPos().getY(), moveZ);
		final int remainingDistance = (int) Math.ceil(Math.sqrt(movement.x * (double) movement.x
		                                                      + movement.y * (double) movement.y
		                                                      + movement.z * (double) movement.z));
		final ShipMovementPreview routePreview = shipCore.previewMovement(type.getCommand(),
		                                                                   movement.x, movement.y, movement.z,
		                                                                   (byte) 0);
		final VectorI step = routePreview.effectiveMovement;
		final ShipMovementPreview stepPreview = shipCore.previewMovement(type.getCommand(),
		                                                                 step.x, step.y, step.z,
		                                                                 (byte) 0);
		final int stepDistance = (int) Math.ceil(Math.sqrt(step.x * (double) step.x + step.y * (double) step.y + step.z * (double) step.z));
		final String warningKey;
		if (movement.getMagnitudeSquared() <= 0L) {
			warningKey = "warpdrive.navigation.route.already_aligned";
		} else if (routePreview.wouldBeClamped || stepDistance < remainingDistance) {
			warningKey = "warpdrive.navigation.route.cruise_split";
		} else {
			warningKey = "";
		}
		return new Leg(type, step.x, step.y, step.z, warningKey, stepDistance, remainingDistance, stepPreview);
	}

	@Nonnull
	private static VectorI toLocalMovement(@Nonnull final TileEntityShipCore shipCore, final int moveX, final int moveY, final int moveZ) {
		final EnumFacing facing = getFacing(shipCore);
		final int moveFront = facing.getXOffset() * moveX + facing.getZOffset() * moveZ;
		final int moveRight = -facing.getZOffset() * moveX + facing.getXOffset() * moveZ;
		return new VectorI(moveFront, moveY, moveRight);
	}

	@Nonnull
	private static EnumFacing getFacing(@Nonnull final TileEntityShipCore shipCore) {
		if (shipCore.facing != null) {
			return shipCore.facing;
		}
		final IBlockState blockState = shipCore.getWorld().getBlockState(shipCore.getPos());
		if ( blockState.getBlock() instanceof BlockShipCore
		  && blockState.getPropertyKeys().contains(BlockProperties.FACING_HORIZONTAL) ) {
			return blockState.getValue(BlockProperties.FACING_HORIZONTAL);
		}
		return EnumFacing.NORTH;
	}

	/** A planned navigation leg. The display title/description are derived client-side from {@link #type}. */
	public static final class Leg {
		public final EnumShipNavigationLegType type;
		public final EnumShipCommand command;
		public final int moveFront;
		public final int moveUp;
		public final int moveRight;
		public final boolean requiresConfirmation;
		public final int stepDistance;
		public final int remainingDistance;
		public final String warningKey;
		@Nullable
		public final ShipMovementPreview preview;

		private Leg(@Nonnull final EnumShipNavigationLegType type,
		            final int moveFront, final int moveUp, final int moveRight,
		            @Nonnull final String warningKey,
		            final int stepDistance, final int remainingDistance) {
			this(type, moveFront, moveUp, moveRight, warningKey, stepDistance, remainingDistance, null);
		}
		
		private Leg(@Nonnull final EnumShipNavigationLegType type,
		            final int moveFront, final int moveUp, final int moveRight,
		            @Nonnull final String warningKey,
		            final int stepDistance, final int remainingDistance,
		            @Nullable final ShipMovementPreview preview) {
			this.type = type;
			this.command = type.getCommand();
			this.moveFront = moveFront;
			this.moveUp = moveUp;
			this.moveRight = moveRight;
			this.requiresConfirmation = type.requiresConfirmation();
			this.stepDistance = stepDistance;
			this.remainingDistance = remainingDistance;
			this.warningKey = warningKey;
			this.preview = preview;
		}
	}
}
