package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.LocalProfiler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.InvalidXmlException;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.config.XmlFileManager;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;

import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;

import net.minecraftforge.common.DimensionManager;

public class CelestialObjectManager extends XmlFileManager {
	
	private static final CelestialObjectManager SERVER = new CelestialObjectManager();
	private static final CelestialObjectManager CLIENT = new CelestialObjectManager();
	// mutable accumulator, written only while parsing/reloading; runtime reads go through the immutable 'registry' below
	private HashMap<String, CelestialObject> celestialObjectsById = new HashMap<>();
	// immutable snapshot swapped atomically in rebuildAndValidate() so concurrent readers (e.g. the render thread)
	// always observe a consistent set of indexes
	private volatile Registry registry = Registry.EMPTY;
	
	// bundles every runtime lookup structure so they can be published as a single atomic unit
	private static final class Registry {
		private static final Registry EMPTY = new Registry(new HashMap<>(), new HashMap<>(), new HashMap<>(), 0.0D);
		final Map<String, CelestialObject> byId;                 // every object, by id
		final Map<Integer, List<CelestialObject>> byDimensionId; // non-virtual objects, by dimensionId
		final Map<String, List<CelestialObject>> byParentId;     // every object, by parentId (direct children)
		final double maxWorldBorder;
		private Registry(final Map<String, CelestialObject> byId,
		                 final Map<Integer, List<CelestialObject>> byDimensionId,
		                 final Map<String, List<CelestialObject>> byParentId,
		                 final double maxWorldBorder) {
			this.byId = byId;
			this.byDimensionId = byDimensionId;
			this.byParentId = byParentId;
			this.maxWorldBorder = maxWorldBorder;
		}
	}
	
	// *** mixed statics ***
	
	public static void clearForReload(final boolean isRemote) {
		// create a new object instead of clearing, in case another thread is iterating through it
		(isRemote ? CLIENT : SERVER).celestialObjectsById = new HashMap<>();
	}
	
	public static CelestialObject get(final boolean isRemote, final String id) {
		return (isRemote ? CLIENT : SERVER).registry.byId.get(id);
	}
	
	public static CelestialObject[] getRegistrySnapshot(final boolean isRemote) {
		return (isRemote ? CLIENT : SERVER).registry.byId.values().toArray(new CelestialObject[0]);
	}
	
	public static CelestialObject get(final World world, final int x, final int z) {
		if (world == null) {
			return null;
		}
		return (world.isRemote ? CLIENT : SERVER).get(world.provider.getDimension(), x, z);
	}
	
	public static CelestialObject get(final boolean isRemote, final int dimensionId, final int x, final int z) {
		return (isRemote ? CLIENT : SERVER).get(dimensionId, x, z);
	}
	
	public static CelestialObject getClosestChild(final World world, final int x, final int z) {
		double closestPlanetDistance = Double.POSITIVE_INFINITY;
		CelestialObject celestialObjectClosest = null;
		if (world != null) {
			// scope to the children of the celestial object we're actually in, so a dimension shared by several
			// objects returns *our* object's children rather than a sibling's
			final CelestialObject celestialObjectCurrent = get(world.isRemote, world.provider.getDimension(), x, z);
			if (celestialObjectCurrent != null) {
				final List<CelestialObject> children = (world.isRemote ? CLIENT : SERVER).registry.byParentId.get(celestialObjectCurrent.id);
				if (children != null) {
					for (final CelestialObject celestialObject : children) {
						final double distanceSquared = celestialObject.getSquareDistanceInParent(world.provider.getDimension(), x, z);
						if (distanceSquared <= 0.0D) {
							return celestialObject;
						} else if (closestPlanetDistance > distanceSquared) {
							closestPlanetDistance = distanceSquared;
							celestialObjectClosest = celestialObject;
						}
					}
				}
			}
		}
		return celestialObjectClosest;
	}
	
	public static boolean isInSpace(final World world, final int x, final int z) {
		final CelestialObject celestialObject = get(world, x, z);
		return celestialObject != null && celestialObject.isSpace();
	}
	
	public static boolean isInHyperspace(final World world, final int x, final int z) {
		final CelestialObject celestialObject = get(world, x, z);
		return celestialObject != null && celestialObject.isHyperspace();
	}
	
	public static boolean hasAtmosphere(final World world, final int x, final int z) {
		final CelestialObject celestialObject = get(world, x, z);
		return celestialObject == null || celestialObject.hasAtmosphere();
	}
	
	public static boolean isPlanet(final World world, final int x, final int z) {
		final CelestialObject celestialObject = get(world, x, z);
		return celestialObject == null
		    || (!celestialObject.isSpace() && !celestialObject.isHyperspace());
	}
	
	public static double getGravity(@Nonnull final Entity entity) {
		final CelestialObject celestialObject = get(entity.world, (int) Math.floor(entity.posX), (int) Math.floor(entity.posZ));
		return celestialObject == null ? 1.0D : celestialObject.getGravity();
	}
	
	public static int getSpaceDimensionId(final World world, final int x, final int z) {
		CelestialObject celestialObject = get(world, x, z);
		if (celestialObject == null) {
			return world.provider.getDimension();
		}
		// already in space?
		if (celestialObject.isSpace()) {
			return celestialObject.dimensionId;
		}
		// coming from hyperspace?
		if (celestialObject.isHyperspace()) {
			celestialObject = getClosestChild(world, x, z);
			return celestialObject == null ? 0 : celestialObject.dimensionId;
		}
		// coming from a planet?
		while (celestialObject != null && !celestialObject.isSpace()) {
			celestialObject = celestialObject.parent;
		}
		return celestialObject == null ? 0 : celestialObject.dimensionId;
	}
	
	public static int getHyperspaceDimensionId(final World world, final int x, final int z) {
		CelestialObject celestialObject = get(world, x, z);
		if (celestialObject == null) {
			return world.provider.getDimension();
		}
		// already in hyperspace?
		if (celestialObject.isHyperspace()) {
			return celestialObject.dimensionId;
		}
		// coming from space?
		if (celestialObject.isSpace()) {
			return celestialObject.parent.dimensionId;
		}
		// coming from a planet?
		while (celestialObject != null && !celestialObject.isSpace()) {
			celestialObject = celestialObject.parent;
		}
		return celestialObject == null || celestialObject.parent == null ? 0 : celestialObject.parent.dimensionId;
	}
	
	public static int getDimensionId(final String stringDimension, final Entity entity) {
		switch (stringDimension.toLowerCase()) {
		case "world":
		case "overworld":
		case "0":
			return 0;
		case "nether":
		case "thenether":
		case "-1":
			return -1;
		case "s":
		case "space":
			return getSpaceDimensionId(entity.world, (int) entity.posX, (int) entity.posZ);
		case "h":
		case "hyper":
		case "hyperspace":
			return getHyperspaceDimensionId(entity.world, (int) entity.posX, (int) entity.posZ);
		default:
			try {
				return Integer.parseInt(stringDimension);
			} catch (final Exception exception) {
				// exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.info(String.format("Invalid dimension %s, expecting integer or overworld/nether/end/theend/space/hyper/hyperspace",
				                                    stringDimension));
			}
		}
		return 0;
	}
	
	public static double getMaxWorldBorder(final World world) {
		return (world.isRemote ? CLIENT : SERVER).getMaxWorldBorder();
	}
	
	// *** server side only ***
	
	public static void onFMLInitialization() {
		// only create dimensions if we own them
		for (final CelestialObject celestialObject : SERVER.registry.byId.values()) {
			if (!celestialObject.isVirtual()) {
				switch (celestialObject.provider) {
				case CelestialObject.PROVIDER_SPACE:
					if (celestialObject.isSpace()) {
						// several celestial objects may share one dimension, so register it only once
						if (!DimensionManager.isDimensionRegistered(celestialObject.dimensionId)) {
							DimensionManager.registerDimension(celestialObject.dimensionId, WarpDrive.dimensionTypeSpace);
						}
					} else {
						WarpDrive.logger.error(String.format("Only a space dimension can be provided by WarpDriveSpace. Dimension %d is not one of those.",
						                                     celestialObject.dimensionId));
					}
					break;
					
				case CelestialObject.PROVIDER_HYPERSPACE:
					if (celestialObject.isHyperspace()) {
						// several celestial objects may share one dimension, so register it only once
						if (!DimensionManager.isDimensionRegistered(celestialObject.dimensionId)) {
							DimensionManager.registerDimension(celestialObject.dimensionId, WarpDrive.dimensionTypeHyperSpace);
						}
					} else {
						WarpDrive.logger.error(String.format("Only an hyperspace dimension can be provided by WarpDriveHyperspace. Dimension %d is not one of those.",
						                                     celestialObject.dimensionId));
					}
					break;
					
				case CelestialObject.PROVIDER_OTHER:
					// nothing
					break;
					
				default:
					WarpDrive.logger.error(String.format("Unknown dimension provider %s for dimension %d, ignoring...",
					                                     celestialObject.provider,
					                                     celestialObject.dimensionId));
					break;
				}
			}
		}
	}
	
	public static void load(final File dir) {
		SERVER.load(dir, "celestialObjects", "celestialObject");
		SERVER.rebuildAndValidate(false);
	}
	
	// @TODO add a proper API
	public static void updateInRegistry(final CelestialObject celestialObject) {
		SERVER.addOrUpdateInRegistry(celestialObject, true);
		SERVER.rebuildAndValidate(true);
	}
	
	public static NBTBase writeClientSync(final EntityPlayerMP entityPlayerMP, final CelestialObject celestialObject) {
		final NBTTagList nbtTagList = new NBTTagList();
		if (celestialObject != null) {
			// sync every object sharing the player's current dimension (siblings), so the client can resolve
			// get(dimensionId, x, z) by position as the player moves between same-dimension zones, without needing a
			// re-sync on movement. For each, include its full parent chain up to hyperspace (required or the client's
			// resolveParent() breaks) and its direct children; dedupe by id so the shared parent chain is sent once.
			final Set<String> sentIds = new HashSet<>();
			final List<CelestialObject> sameDimension = SERVER.registry.byDimensionId.get(celestialObject.dimensionId);
			final List<CelestialObject> roots = sameDimension != null ? sameDimension : Collections.singletonList(celestialObject);
			for (final CelestialObject root : roots) {
				// the object with all its direct parents, up to hyperspace
				CelestialObject celestialObjectParent = root;
				while (celestialObjectParent != null) {
					if (sentIds.add(celestialObjectParent.id)) {
						nbtTagList.appendTag(celestialObjectParent.writeToNBT(new NBTTagCompound()));
					}
					celestialObjectParent = celestialObjectParent.parent;
				}

				// all its direct children
				final List<CelestialObject> children = SERVER.registry.byParentId.get(root.id);
				if (children != null) {
					for (final CelestialObject celestialObjectChild : children) {
						if (sentIds.add(celestialObjectChild.id)) {
							nbtTagList.appendTag(celestialObjectChild.writeToNBT(new NBTTagCompound()));
						}
					}
				}
			}
		}
		return nbtTagList;
	}
	
	public static boolean onOpeningNetherPortal(@Nonnull final World world, @Nonnull final BlockPos blockPos) {
		// prevent creating a portal outside the world border
		final CelestialObject celestialObjectPortal = get(world, blockPos.getX(), blockPos.getZ());
		if (celestialObjectPortal != null) {
			if (!celestialObjectPortal.isInsideBorder(blockPos.getX(), blockPos.getZ()) ) {
				final EntityPlayer entityPlayer = world.getClosestPlayer(blockPos.getX(), blockPos.getY(), blockPos.getZ(), 10.0D, false);
				if (entityPlayer != null) {
					entityPlayer.sendStatusMessage(
							new WarpDriveText(Commons.getStyleWarning(), "warpdrive.world_border.portal_denied"), true);
				}
				WarpDrive.logger.info(String.format("Nether portal opening cancelled %s for player %s: portal entry is outside the world border",
				                                    entityPlayer == null ? "-null-" : entityPlayer.getName(),
				                                    Commons.format(world, blockPos) ));
				return false;
			}
			
			// @TODO prevent creating a portal in specific dimensions
		}
		
		// prevent creating a portal leading outside the world border
		// note: look up the exit object at the SCALED destination (not at (0,0)), so a multi-object target dimension
		// validates against the zone the portal actually leads to
		final boolean isInTheNether = world.provider.getDimension() == -1;
		final double factor = isInTheNether ? 8.0D : 1 / 8.0D;
		final int xExit = (int) Math.floor(blockPos.getX() * factor);
		final int zExit = (int) Math.floor(blockPos.getZ() * factor);
		final CelestialObject celestialObjectExit = get(false, isInTheNether ? 0 : -1, xExit, zExit);
		if (celestialObjectExit != null) {
			if ( Math.abs(xExit - celestialObjectExit.dimensionCenterX) > celestialObjectExit.borderRadiusX
			  || Math.abs(zExit - celestialObjectExit.dimensionCenterZ) > celestialObjectExit.borderRadiusZ ) {
				final EntityPlayer entityPlayer = world.getClosestPlayer(blockPos.getX(), blockPos.getY(), blockPos.getZ(), 10.0D, false);
				if (entityPlayer != null) {
					entityPlayer.sendStatusMessage(
							new WarpDriveText(Commons.getStyleWarning(), "warpdrive.world_border.portal_denied"), true);
				}
				WarpDrive.logger.info(String.format("Nether portal opening cancelled for player %s %s: portal exit is outside the world border",
				                                    entityPlayer == null ? "-null-" : entityPlayer.getName(),
				                                    Commons.format(world, blockPos) ));
				return false;
			}
		}
		
		if (WarpDrive.isDev) {
			WarpDrive.logger.info(String.format("Opening Nether portal %s",
			                                    Commons.format(world, blockPos) ));
		}
		
		return true;
	}
	
	// *** client side only ***
	
	@SideOnly(Side.CLIENT)
	public static void readClientSync(final NBTTagList nbtTagList) {
		clearForReload(true);
		if (nbtTagList != null && nbtTagList.tagCount() > 0) {
			for (int index = 0; index < nbtTagList.tagCount(); index++) {
				final CelestialObject celestialObject = new CelestialObject(nbtTagList.getCompoundTagAt(index));
				CLIENT.addOrUpdateInRegistry(celestialObject, false);
			}
		}
		CLIENT.rebuildAndValidate(true);
	}
	
	@SideOnly(Side.CLIENT)
	public static List<CelestialObject> getRenderChildren(final String parentId) {
		final List<CelestialObject> children = CLIENT.registry.byParentId.get(parentId);
		return children == null ? Collections.emptyList() : children;
	}
	
	@SuppressWarnings("unused") // Core mod
	@SideOnly(Side.CLIENT)
	public static WorldBorder World_getWorldBorder(@Nonnull final World world) {
		final WorldBorder worldBorder = world.getWorldBorder();
		final EntityPlayer entityPlayer = Minecraft.getMinecraft().player;
		if ( entityPlayer == null
		  || entityPlayer.world != world ) {
			return worldBorder;
		}
		final CelestialObject celestialObject = get(world, (int) entityPlayer.posX, (int) entityPlayer.posZ);
		if (celestialObject == null) {
			return worldBorder;
		}
		return celestialObject.getWorldBorder();
	}
	
	// *** non-static methods ***
	
	private void addOrUpdateInRegistry(@Nonnull final CelestialObject celestialObject, final boolean isUpdating) {
		final CelestialObject celestialObjectExisting = celestialObjectsById.get(celestialObject.id);
		if (celestialObjectExisting == null || isUpdating) {
			celestialObjectsById.put(celestialObject.id, celestialObject);
		} else {
			WarpDrive.logger.warn(String.format("Celestial object %s is already defined, keeping original definition", celestialObject.id));
		}
	}
	
	private void rebuildAndValidate(final boolean isRemote) {
		LocalProfiler.start("CelestialMap validation");

		// snapshot the accumulator, then (re)build the immutable indexes from it
		final Map<String, CelestialObject> byId = new HashMap<>(celestialObjectsById);
		final Map<Integer, List<CelestialObject>> byDimensionId = new HashMap<>();       // non-virtual, by dimensionId
		final Map<String, List<CelestialObject>> byParentId = new HashMap<>();           // every object, by parentId
		final Map<Integer, List<CelestialObject>> byParentDimensionId = new HashMap<>(); // local only, for overlap checks

		// resolve parents first, since validation/bucketing below relies on the whole set being linked
		for (final CelestialObject celestialObject : byId.values()) {
			celestialObject.resolveParent(byId.get(celestialObject.parentId));
		}

		// per-object pass: finalize, gather stats, validate coordinates, and populate the indexes
		int countErrors = 0;
		int countHyperspace = 0;
		int countSpace = 0;
		double maxWorldBorderTemp = 0.0D;
		for (final CelestialObject celestialObject1 : byId.values()) {
			celestialObject1.lateUpdate();
			
			// stats
			if (celestialObject1.isHyperspace()) {
				countHyperspace++;
			} else if (celestialObject1.isSpace()) {
				countSpace++;
			}
			final AxisAlignedBB worldBorderArea1 = celestialObject1.getWorldBorderArea();
			maxWorldBorderTemp = Math.max(maxWorldBorderTemp, 2 * Math.max(Math.max(Math.abs(worldBorderArea1.minX), Math.abs(worldBorderArea1.minZ)),
			                                                               Math.max(Math.abs(worldBorderArea1.maxX), Math.abs(worldBorderArea1.maxZ)) ) );
			
			// validate coordinates
			if (!celestialObject1.isVirtual()) {
				if ( celestialObject1.parent == null
				  || celestialObject1.parent.dimensionId != celestialObject1.dimensionId ) {// not hyperspace
					final CelestialObject celestialObjectParent = byId.get(celestialObject1.parentId);
					if (celestialObjectParent == null) {
						if ( !isRemote
						  && celestialObject1.parentId != null
						  && !celestialObject1.parentId.isEmpty() ) {
							countErrors++;
							WarpDrive.logger.error(String.format("CelestialObjects validation error #%d\nCelestial object %s refers to unknown parent %s",
							                                     countErrors,
							                                     celestialObject1.id,
							                                     celestialObject1.parentId));
						}
					} else if ( celestialObject1.parentCenterX - celestialObject1.borderRadiusX < celestialObjectParent.dimensionCenterX - celestialObjectParent.borderRadiusX 
					         || celestialObject1.parentCenterZ - celestialObject1.borderRadiusZ < celestialObjectParent.dimensionCenterZ - celestialObjectParent.borderRadiusZ
					         || celestialObject1.parentCenterX + celestialObject1.borderRadiusX > celestialObjectParent.dimensionCenterX + celestialObjectParent.borderRadiusX
					         || celestialObject1.parentCenterZ + celestialObject1.borderRadiusZ > celestialObjectParent.dimensionCenterZ + celestialObjectParent.borderRadiusZ ) {
						countErrors++;
						WarpDrive.logger.error(String.format("CelestialObjects validation error #%d\nCelestial object %s is outside its parent border.\n%s\n%s\n%s's area in parent %s is outside %s's border %s",
						                                     countErrors,
						                                     celestialObject1.id,
						                                     celestialObject1,
						                                     celestialObjectParent,
						                                     celestialObject1.id,
						                                     celestialObject1.getAreaInParent(),
						                                     celestialObjectParent.id,
						                                     celestialObjectParent.getWorldBorderArea() ));
					}
				}
				if ( celestialObject1.dimensionCenterX - celestialObject1.borderRadiusX < -30000000
				  || celestialObject1.dimensionCenterZ - celestialObject1.borderRadiusZ < -30000000
				  || celestialObject1.dimensionCenterX + celestialObject1.borderRadiusX >= 30000000
				  || celestialObject1.dimensionCenterZ + celestialObject1.borderRadiusZ >= 30000000 ) {
					countErrors++;
					WarpDrive.logger.error(String.format("CelestialObjects validation error #%d\nCelestial object %s is outside the game border +/-30000000.\n%s\n%s border is %s",
					                                     countErrors,
					                                     celestialObject1.id,
					                                     celestialObject1,
					                                     celestialObject1.id,
					                                     celestialObject1.getWorldBorderArea() ));
				}
			}
			
			// index this object for O(1) runtime lookups and for the bucketed overlap checks below
			byParentId.computeIfAbsent(celestialObject1.parentId, key -> new ArrayList<>()).add(celestialObject1);
			if (!celestialObject1.isVirtual()) {
				byDimensionId.computeIfAbsent(celestialObject1.dimensionId, key -> new ArrayList<>()).add(celestialObject1);
			}
			if ( !celestialObject1.isHyperspace()
			  && celestialObject1.parent != null ) {
				byParentDimensionId.computeIfAbsent(celestialObject1.parent.dimensionId, key -> new ArrayList<>()).add(celestialObject1);
			}
		}

		// overlap validation, bucketed so only objects that can actually conflict are compared
		// 1) objects sharing a parent dimension: their areas in that parent must not intersect
		for (final List<CelestialObject> bucket : byParentDimensionId.values()) {
			for (int index1 = 0; index1 < bucket.size(); index1++) {
				final CelestialObject celestialObject1 = bucket.get(index1);
				final AxisAlignedBB areaInParent1 = celestialObject1.getAreaInParent();
				for (int index2 = index1 + 1; index2 < bucket.size(); index2++) {
					final CelestialObject celestialObject2 = bucket.get(index2);
					final AxisAlignedBB areaInParent2 = celestialObject2.getAreaInParent();
					if (areaInParent1.intersects(areaInParent2)) {
						countErrors++;
						WarpDrive.logger.error(String.format("CelestialObjects validation error #%d\nOverlapping parent areas detected in dimension %d between %s and %s\nArea1 %s from %s\nArea2 %s from %s",
						                                     countErrors,
						                                     celestialObject1.parent.dimensionId,
						                                     celestialObject1.id,
						                                     celestialObject2.id,
						                                     areaInParent1,
						                                     celestialObject1,
						                                     areaInParent2,
						                                     celestialObject2 ));
					}
				}
			}
		}
		// 2) non-virtual objects sharing a dimension: their world borders must not intersect
		for (final List<CelestialObject> bucket : byDimensionId.values()) {
			for (int index1 = 0; index1 < bucket.size(); index1++) {
				final CelestialObject celestialObject1 = bucket.get(index1);
				final AxisAlignedBB worldBorderArea1 = celestialObject1.getWorldBorderArea();
				for (int index2 = index1 + 1; index2 < bucket.size(); index2++) {
					final CelestialObject celestialObject2 = bucket.get(index2);
					final AxisAlignedBB worldBorderArea2 = celestialObject2.getWorldBorderArea();
					if (worldBorderArea1.intersects(worldBorderArea2)) {
						countErrors++;
						WarpDrive.logger.error(String.format("CelestialObjects validation error #%d\nOverlapping areas detected in dimension %d between %s and %s\nArea1 %s from %s\nArea2 %s from %s",
						                                     countErrors,
						                                     celestialObject1.dimensionId,
						                                     celestialObject1.id,
						                                     celestialObject2.id,
						                                     worldBorderArea1,
						                                     celestialObject1,
						                                     worldBorderArea2,
						                                     celestialObject2 ));
					}
				}
			}
		}
		
		if (!isRemote && countHyperspace == 0) {
			countErrors++;
			WarpDrive.logger.error(String.format("CelestialObjects validation error #%d\nAt least one hyperspace celestial object should be defined!",
			                                     countErrors ));
		} else if (!isRemote && countSpace == 0) {
			countErrors++;
			WarpDrive.logger.error(String.format("CelestialObjects validation error #%d\nAt least one space celestial object should be defined!",
			                                     countErrors ));
		}
		
		if (WarpDriveConfig.G_ENFORCE_VALID_CELESTIAL_OBJECTS) {
			if (countErrors == 1) {
				throw new RuntimeException("Invalid celestial objects definition: update your configuration to fix this validation error, search your logs for 'CelestialObjects validation error' to get more details.");
			} else if (countErrors > 0) {
				throw new RuntimeException(String.format(
						"Invalid celestial objects definition: update your configuration to fix those %d validation errors, search your logs for 'CelestialObjects validation error' to get more details.",
						countErrors));
			}
		} else {
			FMLLog.bigWarning("Invalid celestial objects definition: bad things will happen, fix those before reporting any issue!");
		}
		
		
		// We're not checking invalid dimension id, so they can be pre-allocated (see MystCraft)

		// publish the new indexes as a single immutable snapshot so readers never observe a half-updated set
		registry = new Registry(byId, byDimensionId, byParentId, maxWorldBorderTemp);

		LocalProfiler.stop();
	}
	
	@Override
	protected void parseRootElement(final String location, final Element elementCelestialObject) throws InvalidXmlException {
		parseCelestiaObjectElement(location, elementCelestialObject, "");
	}
	
	private void parseCelestiaObjectElement(final String location, final Element elementCelestialObject, final String parentId) throws InvalidXmlException {
		final CelestialObject celestialObjectRead = new CelestialObject(location, parentId, elementCelestialObject);
		
		addOrUpdateInRegistry(celestialObjectRead, false);
		
		// look for optional child element(s)
		final List<Element> listChildren = XmlFileManager.getChildrenElementByTagName(elementCelestialObject, "celestialObject");
		if (!listChildren.isEmpty()) {
			for (int indexElement = 0; indexElement < listChildren.size(); indexElement++) {
				final Element elementChild = listChildren.get(indexElement);
				final String locationChild = String.format("%s Celestial object %s > child %d/%d",
				                                           location, celestialObjectRead.id, indexElement + 1, listChildren.size());
				parseCelestiaObjectElement(locationChild, elementChild, celestialObjectRead.id);
			}
		}
	}
	
	public CelestialObject get(final String id) {
		return registry.byId.get(id);
	}
	
	public CelestialObject get(final int dimensionId, final int x, final int z) {
		// O(1) fetch of that dimension's objects (already non-virtual + dimension-matching) instead of scanning all
		final List<CelestialObject> candidates = registry.byDimensionId.get(dimensionId);
		if (candidates == null) {
			return null;
		}
		double distanceClosest = Double.POSITIVE_INFINITY;
		CelestialObject celestialObjectClosest = null;
		for (final CelestialObject celestialObject : candidates) {
			final double distanceSquared = celestialObject.getSquareDistanceOutsideBorder(x, z);
			if (distanceSquared <= 0) {
				return celestialObject;
			} else if (distanceClosest > distanceSquared) {
				distanceClosest = distanceSquared;
				celestialObjectClosest = celestialObject;
			}
		}
		return celestialObjectClosest;
	}
	
	public double getMaxWorldBorder() {
		return registry.maxWorldBorder < 1000 ? 6.0E7D : registry.maxWorldBorder;
	}
}
