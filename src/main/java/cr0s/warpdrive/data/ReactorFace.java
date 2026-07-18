package cr0s.warpdrive.data;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import net.minecraft.util.Direction;
import net.minecraft.util.IStringSerializable;

public class ReactorFace implements IStringSerializable {
	
	public static final ReactorFace UNKNOWN;
	
	public final EnumTier enumTier;
	public final int indexStability; // >= 0 for lasers, -1 otherwise
	public final String name; // non null
	public final int x;
	public final int y;
	public final int z;
	public final Direction facingLaserProperty; // defined for laser, null otherwise
	
	// cached values
	public static final int maxInstabilities;
	private static final HashMap<String, ReactorFace> NAME_MAP = new HashMap<>();
	private static final HashMap<EnumTier, ReactorFace[]> FACES_BY_TIER;
	private static final HashMap<EnumTier, ReactorFace[]> LASERS_BY_TIER;
	private static final ReactorFace[] LASERS;
	
	static {
		UNKNOWN = new ReactorFace(null, -1, "unknown", 0, 0, 0, null);
		
		// basic reactor has core for core
		new ReactorFace(EnumTier.BASIC   ,  0, "laser.basic.south"      ,  0, 0, -2, Direction.SOUTH);
		new ReactorFace(EnumTier.BASIC   ,  1, "laser.basic.north"      ,  0, 0,  2, Direction.NORTH);
		new ReactorFace(EnumTier.BASIC   ,  2, "laser.basic.east"       , -2, 0,  0, Direction.EAST );
		new ReactorFace(EnumTier.BASIC   ,  3, "laser.basic.west"       ,  2, 0,  0, Direction.WEST );
		
		// advanced reactor has core offset with small radius
		new ReactorFace(EnumTier.ADVANCED,  0, "laser.advanced.south+"  ,  1, 2, -3, Direction.SOUTH);
		new ReactorFace(EnumTier.ADVANCED,  1, "laser.advanced.south-"  , -1, 4, -3, Direction.SOUTH);
		new ReactorFace(EnumTier.ADVANCED,  2, "laser.advanced.north-"  , -1, 2,  3, Direction.NORTH);
		new ReactorFace(EnumTier.ADVANCED,  3, "laser.advanced.north+"  ,  1, 4,  3, Direction.NORTH);
		new ReactorFace(EnumTier.ADVANCED,  4, "laser.advanced.east-"   , -3, 2, -1, Direction.EAST );
		new ReactorFace(EnumTier.ADVANCED,  5, "laser.advanced.east+"   , -3, 4,  1, Direction.EAST );
		new ReactorFace(EnumTier.ADVANCED,  6, "laser.advanced.west+"   ,  3, 2,  1, Direction.WEST );
		new ReactorFace(EnumTier.ADVANCED,  7, "laser.advanced.west-"   ,  3, 4, -1, Direction.WEST );
		addCoreBlocks(EnumTier.ADVANCED, 1, 0, 3, 0);
		
		// superior reactor has core offset with larger radius
		new ReactorFace(EnumTier.SUPERIOR,  0, "laser.superior.south+"  ,  1, 3, -4, Direction.SOUTH);
		new ReactorFace(EnumTier.SUPERIOR,  1, "laser.superior.south-"  , -1, 5, -4, Direction.SOUTH);
		new ReactorFace(EnumTier.SUPERIOR,  2, "laser.superior.north-"  , -1, 3,  4, Direction.NORTH);
		new ReactorFace(EnumTier.SUPERIOR,  3, "laser.superior.north+"  ,  1, 5,  4, Direction.NORTH);
		new ReactorFace(EnumTier.SUPERIOR,  4, "laser.superior.east-"   , -4, 3, -1, Direction.EAST );
		new ReactorFace(EnumTier.SUPERIOR,  5, "laser.superior.east+"   , -4, 5,  1, Direction.EAST );
		new ReactorFace(EnumTier.SUPERIOR,  6, "laser.superior.west+"   ,  4, 3,  1, Direction.WEST );
		new ReactorFace(EnumTier.SUPERIOR,  7, "laser.superior.west-"   ,  4, 5, -1, Direction.WEST );
		
		new ReactorFace(EnumTier.SUPERIOR,  8, "laser.superior.south--" , -2, 2, -4, Direction.SOUTH);
		new ReactorFace(EnumTier.SUPERIOR,  9, "laser.superior.south++" ,  2, 6, -4, Direction.SOUTH);
		new ReactorFace(EnumTier.SUPERIOR, 10, "laser.superior.north++" ,  2, 2,  4, Direction.NORTH);
		new ReactorFace(EnumTier.SUPERIOR, 11, "laser.superior.north--" , -2, 6,  4, Direction.NORTH);
		new ReactorFace(EnumTier.SUPERIOR, 12, "laser.superior.east++"  , -4, 2,  2, Direction.EAST );
		new ReactorFace(EnumTier.SUPERIOR, 13, "laser.superior.east--"  , -4, 6, -2, Direction.EAST );
		new ReactorFace(EnumTier.SUPERIOR, 14, "laser.superior.west--"  ,  4, 2, -2, Direction.WEST );
		new ReactorFace(EnumTier.SUPERIOR, 15, "laser.superior.west++"  ,  4, 6,  2, Direction.WEST );
		addCoreBlocks(EnumTier.SUPERIOR, 2, 0, 4, 0);
		
		// pre-build the list of faces and lasers in the structure
		final HashMap<EnumTier, ArrayList<ReactorFace>> facesByTiers = new HashMap<>(EnumTier.length);
		final HashMap<EnumTier, ArrayList<ReactorFace>> lasersByTiers = new HashMap<>(EnumTier.length);
		for (final EnumTier tierLoop : EnumTier.values()) {
			facesByTiers.put(tierLoop, new ArrayList<>(16));
			lasersByTiers.put(tierLoop, new ArrayList<>(16));
		}
		for (final ReactorFace reactorFace : NAME_MAP.values()) {
			if (reactorFace.enumTier == null) {
				continue;
			}
			facesByTiers.get(reactorFace.enumTier).add(reactorFace);
			if (reactorFace.indexStability >= 0) {
				lasersByTiers.get(reactorFace.enumTier).add(reactorFace);
			}
		}
		FACES_BY_TIER = new HashMap<>(EnumTier.length);
		for (final Entry<EnumTier, ArrayList<ReactorFace>> entry : facesByTiers.entrySet()) {
			FACES_BY_TIER.put(entry.getKey(), entry.getValue().toArray(new ReactorFace[0]));
		}
		int max = 0;
		LASERS_BY_TIER = new HashMap<>(EnumTier.length);
		final ArrayList<ReactorFace> lasers = new ArrayList<>(32);
		for (final Entry<EnumTier, ArrayList<ReactorFace>> entry : lasersByTiers.entrySet()) {
			LASERS_BY_TIER.put(entry.getKey(), entry.getValue().toArray(new ReactorFace[0]));
			lasers.addAll(entry.getValue());
			max = Math.max(max, entry.getValue().size());
		}
		LASERS = lasers.toArray(new ReactorFace[0]);
		maxInstabilities = max;
	}
	
	private static void addCoreBlocks(final EnumTier enumTier, final int radius, final int xOffset, final int yOffset, final int zOffset) {
		// square radius from center of block
		final double sqRadiusHigh = (radius + 0.5D) * (radius + 0.5D);
		
		// sphere
		final int radiusCeil = radius + 1;
		
		// Pass the cube and check points for sphere equation x^2 + y^2 + z^2 = r^2
		for (int x = -radiusCeil; x <= radiusCeil; x++) {
			final double x2 = (x + 0.5D) * (x + 0.5D);
			for (int y = -radiusCeil; y <= radiusCeil; y++) {
				final double x2y2 = x2 + (y + 0.5D) * (y + 0.5D);
				for (int z = -radiusCeil; z <= radiusCeil; z++) {
					final double sqRange = x2y2 + (z + 0.5D) * (z + 0.5D); // Square distance from current position to center
					
					// Skip too far blocks
					if (sqRange > sqRadiusHigh) {
						continue;
					}
					new ReactorFace(enumTier,
					                -1,
					                String.format("core.%s.[%d,%d,%d]", enumTier.getName(), x, y, z),
					                    xOffset + x, yOffset + y, zOffset + z,
					                null);
				}
			}
		}
	}
	
	ReactorFace(final EnumTier enumTier, final int indexStability, final String name,
	            final int x, final int y, final int z,
	            final Direction facingLaserProperty) {
		this.enumTier = enumTier;
		this.indexStability = indexStability;
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
		this.facingLaserProperty = facingLaserProperty;
		
		// register
		NAME_MAP.put(name, this);
		
		// add lens
		if (facingLaserProperty != null) {
			new ReactorFace(enumTier, -1, name + ".lens",
			                    x + facingLaserProperty.getXOffset(),
			                    y + facingLaserProperty.getYOffset(),
			                    z + facingLaserProperty.getZOffset(),
			                null);
		}
	}
	
	@Nonnull
	public static ReactorFace[] get(@Nonnull final EnumTier tier) {
		return FACES_BY_TIER.get(tier);
	}
	
	@Nonnull
	public static ReactorFace[] getLasers() {
		return LASERS;
	}
	
	@Nonnull
	public static ReactorFace[] getLasers(@Nonnull final EnumTier tier) {
		return LASERS_BY_TIER.get(tier);
	}
	
	@Nonnull
	@Override
	public String getName() {
		return name;
	}
	
	public static ReactorFace get(final String name) {
		return NAME_MAP.get(name);
	}
}
