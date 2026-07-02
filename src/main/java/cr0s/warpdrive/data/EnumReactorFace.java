package cr0s.warpdrive.data;

import cr0s.warpdrive.api.IStringSerializable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import net.minecraft.util.EnumFacing;

public enum EnumReactorFace implements IStringSerializable {
	
	//                tier           inst name         facing             x   y   z  propertyLaser
	UNKNOWN          (null          , -1, "unknown"  , null            ,  0,  0,  0, null            ),
	BASIC_SOUTH      (EnumTier.BASIC,  1, "south"    , EnumFacing.SOUTH,  0,  0, -2, EnumFacing.NORTH),
	BASIC_NORTH      (EnumTier.BASIC,  0, "north"    , EnumFacing.NORTH,  0,  0,  2, EnumFacing.SOUTH),
	BASIC_EAST       (EnumTier.BASIC,  2, "east"     , EnumFacing.EAST , -2,  0,  0, EnumFacing.WEST ),
	BASIC_WEST       (EnumTier.BASIC,  3, "west"     , EnumFacing.WEST ,  2,  0,  0, EnumFacing.EAST ),
	BASIC_NORTH_AIR  (EnumTier.BASIC, -1, "north_air", EnumFacing.NORTH,  0,  0, -1, null            ),
	BASIC_SOUTH_AIR  (EnumTier.BASIC, -1, "south_air", EnumFacing.SOUTH,  0,  0,  1, null            ),
	BASIC_EAST_AIR   (EnumTier.BASIC, -1, "east_air" , EnumFacing.EAST , -1,  0,  0, null            ),
	BASIC_WEST_AIR   (EnumTier.BASIC, -1, "west_air" , EnumFacing.WEST ,  1,  0,  0, null            );
	
	public final EnumTier tier;
	public final int indexStability;
	public final String name;
	public final EnumFacing facing;
	public final int x;
	public final int y;
	public final int z;
	public final EnumFacing propertyLaser;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumReactorFace> ID_MAP = new HashMap<>();
	private static final HashMap<EnumTier, EnumReactorFace[]> TIER_LASERS = new HashMap<>(EnumTier.length);
	
	static {
		length = EnumReactorFace.values().length;
		for (final EnumReactorFace reactorFace : values()) {
			ID_MAP.put(reactorFace.ordinal(), reactorFace);
		}
		
		// pre-build the list of lasers in the structure
		final HashMap<EnumTier, ArrayList<EnumReactorFace>> tierLasers = new HashMap<>(EnumTier.length);
		for (final EnumTier tierLoop : EnumTier.values()) {
			tierLasers.put(tierLoop, new ArrayList<>(16));
		}
		for (final EnumReactorFace reactorFace : values()) {
			if (reactorFace.indexStability >= 0) {
				tierLasers.get(reactorFace.tier).add(reactorFace);
			}
		}
		for (final Entry<EnumTier, ArrayList<EnumReactorFace>> entry : tierLasers.entrySet()) {
			TIER_LASERS.put(entry.getKey(), entry.getValue().toArray(new EnumReactorFace[0]));
		}
	}
	
	EnumReactorFace(final EnumTier tier, final int indexStability, final String name,
	                final EnumFacing facing, final int x, final int y, final int z,
	                final EnumFacing propertyLaser) {
		this.tier = tier;
		this.indexStability = indexStability;
		this.name = name;
		this.facing = facing;
		this.x = x;
		this.y = y;
		this.z = z;
		this.propertyLaser = propertyLaser;
	}
	
	public static EnumReactorFace[] getLasers(final EnumTier tier) {
		return TIER_LASERS.get(tier);
	}
	
	@Nonnull
	@Override
	public String getName() {
		return name;
	}
	
	public static EnumReactorFace get(final int ordinal) {
		return ID_MAP.get(ordinal);
	}
}
