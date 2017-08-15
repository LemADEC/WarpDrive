package cr0s.warpdrive.data;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IStarMapRegistryTileEntity;
import cr0s.warpdrive.config.WarpDriveConfig;

import java.util.HashMap;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

public class StarMapRegistryItem extends GlobalPosition {
	
	public final EnumStarMapEntryType type;
	public final UUID uuid;
	public int maxX, maxY, maxZ;
	public int minX, minY, minZ;
	public int mass;
	public double isolationRate = 0.0D;
	public String name = "default";
	
	public enum EnumStarMapEntryType {
		UNDEFINED(0, "-undefined-"),
		SHIP(1, "ship"),	        	// a ship core
		JUMPGATE(2, "jumpgate"),	    // a jump gate
		PLANET(3, "planet"),		    // a planet (a transition plane allowing to move to another dimension)
		STAR(4, "star"),    		    // a star
		STRUCTURE(5, "structure"),	    // a structure from WorldGeneration (moon, asteroid field, etc.)
		WARP_ECHO(6, "warp_echo"),	    // remains of a warp
		ACCELERATOR(7, "accelerator");	// an accelerator setup
		
		private final int id;
		private final String name;
		
		// cached values
		public static final int length;
		private static final HashMap<String, EnumStarMapEntryType> mapNames = new HashMap<>();
		
		static {
			length = EnumStarMapEntryType.values().length;
			for (EnumStarMapEntryType enumStarMapEntryType : values()) {
				mapNames.put(enumStarMapEntryType.getName(), enumStarMapEntryType);
			}
		}
		
		EnumStarMapEntryType(final int id, final String name) {
			this.id = id;
			this.name = name;
		}
		
		public int getId() {
			return id;
		}
		
		public String getName() {
			return name;
		}
		
		public static EnumStarMapEntryType getByName(final String name) {
			return mapNames.get(name);
		}
	}
	
	public StarMapRegistryItem(
	                          final EnumStarMapEntryType type, final UUID uuid,
	                          final int dimensionId, final int x, final int y, final int z,
	                          final AxisAlignedBB aabbArea,
	                          final int mass, final double isolationRate,
	                          final String name) {
		super(dimensionId, x, y, z);
		this.type = type;
		this.uuid = uuid;
		if (aabbArea == null) {
			this.maxX = x;
			this.maxY = y;
			this.maxZ = z;
			this.minX = x;
			this.minY = y;
			this.minZ = z;
		} else {
			this.maxX = (int) aabbArea.maxX;
			this.maxY = (int) aabbArea.maxY;
			this.maxZ = (int) aabbArea.maxZ;
			this.minX = (int) aabbArea.minX;
			this.minY = (int) aabbArea.minY;
			this.minZ = (int) aabbArea.minZ;
		}
		this.mass = mass;
		this.isolationRate = isolationRate;
		this.name = name;
	}
	
	public StarMapRegistryItem(final IStarMapRegistryTileEntity tileEntity) {
		this(
			EnumStarMapEntryType.getByName(tileEntity.getStarMapType()), tileEntity.getUUID(),
			((TileEntity) tileEntity).getWorldObj().provider.dimensionId,
			((TileEntity) tileEntity).xCoord, ((TileEntity) tileEntity).yCoord, ((TileEntity) tileEntity).zCoord,
			tileEntity.getStarMapArea(),
			tileEntity.getMass(), tileEntity.getIsolationRate(),
			tileEntity.getStarMapName());
	}
	
	public boolean sameIdOrCoordinates(final IStarMapRegistryTileEntity tileEntity) {
		assert(tileEntity instanceof TileEntity);
		return uuid == tileEntity.getUUID()
			|| dimensionId == ((TileEntity) tileEntity).getWorldObj().provider.dimensionId
			&& x == ((TileEntity) tileEntity).xCoord
			&& y == ((TileEntity) tileEntity).yCoord
			&& z == ((TileEntity) tileEntity).zCoord;
	}
	
	public void update(final IStarMapRegistryTileEntity tileEntity) {
		if (WarpDrive.isDev) {
			assert (tileEntity instanceof TileEntity);
			assert (sameIdOrCoordinates(tileEntity));
		}
		final AxisAlignedBB aabbArea = tileEntity.getStarMapArea();
		if (aabbArea != null) {
			maxX = (int) aabbArea.maxX;
			maxY = (int) aabbArea.maxY;
			maxZ = (int) aabbArea.maxZ;
			minX = (int) aabbArea.minX;
			minY = (int) aabbArea.minY;
			minZ = (int) aabbArea.minZ;
		}
		mass = tileEntity.getMass();
		isolationRate = tileEntity.getIsolationRate();
		name = tileEntity.getStarMapName();
	}
	
	public boolean isSameTileEntity(final IStarMapRegistryTileEntity tileEntity) {
		assert(tileEntity instanceof TileEntity);
		return dimensionId == ((TileEntity) tileEntity).getWorldObj().provider.dimensionId
		  && x == ((TileEntity) tileEntity).xCoord
		  && y == ((TileEntity) tileEntity).yCoord
		  && z == ((TileEntity) tileEntity).zCoord;
	}
	
	public boolean contains(final int x, final int y, final int z) {
		return minX <= x && x <= maxX && minY <= y && y <= maxY && minZ <= z && z <= maxZ;
	}
	
	
	public StarMapRegistryItem(final NBTTagCompound tagCompound) {
		super(tagCompound);
		type = EnumStarMapEntryType.getByName(tagCompound.getString("type"));
		UUID uuidLocal = new UUID(tagCompound.getLong("uuidMost"), tagCompound.getLong("uuidLeast"));
		if (uuidLocal.getMostSignificantBits() == 0 && uuidLocal.getLeastSignificantBits() == 0) {
			uuidLocal = UUID.randomUUID();
		}
		uuid = uuidLocal;
		maxX = tagCompound.getInteger("maxX");
		maxY = tagCompound.getInteger("maxY");
		maxZ = tagCompound.getInteger("maxZ");
		minX = tagCompound.getInteger("minX");
		minY = tagCompound.getInteger("minY");
		minZ = tagCompound.getInteger("minZ");
		mass = tagCompound.getInteger("mass");
		isolationRate = tagCompound.getDouble("isolationRate");
		name = tagCompound.getString("name");
	}
	
	@Override
	public void writeToNBT(final NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
		tagCompound.setString("type", type.getName());
		if (uuid != null) {
			tagCompound.setLong("uuidMost", uuid.getMostSignificantBits());
			tagCompound.setLong("uuidLeast", uuid.getLeastSignificantBits());
		}
		tagCompound.setInteger("maxX", maxX);
		tagCompound.setInteger("maxY", maxY);
		tagCompound.setInteger("maxZ", maxZ);
		tagCompound.setInteger("minX", minX);
		tagCompound.setInteger("minY", minY);
		tagCompound.setInteger("minZ", minZ);
		tagCompound.setInteger("mass", mass);
		tagCompound.setDouble("isolationRate", isolationRate);
		if (name != null && !name.isEmpty()) {
			tagCompound.setString("name", name);
		}
	}
	
	public String getFormattedLocation() {
		final CelestialObject celestialObject = CelestialObjectManager.get(false, dimensionId, x, z);
		if (celestialObject == null) {
			return String.format("DIM%d @ (%d %d %d)",
			                     dimensionId,
			                     x, y, z);
		} else {
			return String.format("%s [DIM%d] @ (%d %d %d)",
			                     celestialObject.getDisplayName(),
			                     dimensionId,
			                     x, y, z);
		}
	}
	
	@Override
	public int hashCode() {
		return dimensionId << 24 + (x >> 10) << 12 + y << 10 + (z >> 10);
	}
	
	@Override
	public String toString() {
		return String.format("%s @ DIM%d (%d %d %d) (%d %d %d) -> (%d %d %d)",
			getClass().getSimpleName(), dimensionId,
			x, y, z,
			minX, minY, minZ,
			maxX, maxY, maxZ);
	}
}