package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;

import javax.annotation.Nonnull;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

public class GlobalPosition {
	
	public final ResourceLocation dimensionId;
	public final int x, y, z;
	private BlockPos cache_blockPos;
	
	public GlobalPosition(final ResourceLocation dimensionId, final int x, final int y, final int z) {
		this.dimensionId = dimensionId;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public GlobalPosition(final ResourceLocation dimensionId, @Nonnull final BlockPos blockPos) {
		this.dimensionId = dimensionId;
		this.x = blockPos.getX();
		this.y = blockPos.getY();
		this.z = blockPos.getZ();
	}
	
	public GlobalPosition(@Nonnull final GlobalPosition globalPosition) {
		this(globalPosition.dimensionId, globalPosition.x, globalPosition.y, globalPosition.z);
		this.cache_blockPos = globalPosition.cache_blockPos;
	}
	
	public GlobalPosition(@Nonnull final TileEntity tileEntity) {
		this(tileEntity.getWorld().getDimension().getType().getRegistryName(), tileEntity.getPos().getX(), tileEntity.getPos().getY(), tileEntity.getPos().getZ());
	}
	
	public GlobalPosition(@Nonnull final Entity entity) {
		this(entity.world.getDimension().getType().getRegistryName(),
			(int) Math.floor(entity.getPosX()),
			(int) Math.floor(entity.getPosY()),
			(int) Math.floor(entity.getPosZ()));
	}
	
	public ServerWorld getWorldServerIfLoaded() {
		final ServerWorld world = Commons.getLoadedWorldServer(dimensionId);
		// skip unloaded worlds
		if (world == null) {
			return null;
		}
		
		if (!world.chunkExists(x >> 4, z >> 4)) {
			return null;
		}
		return world;
	}
	
	public boolean isLoaded() {
		return getWorldServerIfLoaded() != null;
	}
	
	public CelestialObject getCelestialObject(final boolean isRemote) {
		return CelestialObjectManager.get(isRemote, dimensionId, x, z);
	}
	
	public Vector3 getUniversalCoordinates(final boolean isRemote) {
		final CelestialObject celestialObject = CelestialObjectManager.get(isRemote, dimensionId, x, z);
		return GlobalRegionManager.getUniversalCoordinates(celestialObject, x, y, z);
	}
	
	public BlockPos getBlockPos() {
		if ( cache_blockPos == null
		  || cache_blockPos.getX() != x
		  || cache_blockPos.getY() != y
		  || cache_blockPos.getZ() != z ) {
			cache_blockPos = new BlockPos(x, y, z);
		}
		return cache_blockPos;
	}
	
	public int distance2To(@Nonnull final TileEntity tileEntity) {
		assert tileEntity.getWorld() != null;
		assert tileEntity.getWorld().getDimension().getType().getRegistryName() != null;
		if (!tileEntity.getWorld().getDimension().getType().getRegistryName().equals(dimensionId)) {
			return Integer.MAX_VALUE;
		}
		final int newX = tileEntity.getPos().getX() - x;
		final int newY = tileEntity.getPos().getY() - y;
		final int newZ = tileEntity.getPos().getZ() - z;
		return newX * newX + newY * newY + newZ * newZ;
	}
	
	public double distance2To(@Nonnull final Entity entity) {
		assert entity.world.getDimension().getType().getRegistryName() != null;
		if (!entity.world.getDimension().getType().getRegistryName().equals(dimensionId)) {
			return Double.MAX_VALUE;
		}
		final double newX = entity.getPosX() - x;
		final double newY = entity.getPosY() - y;
		final double newZ = entity.getPosZ() - z;
		return newX * newX + newY * newY + newZ * newZ;
	}
	
	public GlobalPosition(@Nonnull final CompoundNBT tagCompound) {
		dimensionId = new ResourceLocation(tagCompound.getString("dimensionId"));
		x = tagCompound.getInt("x");
		y = tagCompound.getInt("y");
		z = tagCompound.getInt("z");
	}
	
	public void write(@Nonnull final CompoundNBT tagCompound) {
		tagCompound.putString("dimensionId", dimensionId.toString());
		tagCompound.putInt("x", x);
		tagCompound.putInt("y", y);
		tagCompound.putInt("z", z);
	}
	
	public boolean equals(@Nonnull final TileEntity tileEntity) {
		return tileEntity.getWorld() != null
			&& x == tileEntity.getPos().getX()
		    && y == tileEntity.getPos().getY()
		    && z == tileEntity.getPos().getZ()
		    && dimensionId.equals(tileEntity.getWorld().getDimension().getType().getRegistryName());
	}
	
	@Override
	public boolean equals(final Object object) {
		if (object instanceof GlobalPosition) {
			final GlobalPosition globalPosition = (GlobalPosition) object;
			return (dimensionId == globalPosition.dimensionId) && (x == globalPosition.x) && (y == globalPosition.y) && (z == globalPosition.z);
		} else if (object instanceof VectorI) {
			final VectorI vector = (VectorI) object;
			return (x == vector.x) && (y == vector.y) && (z == vector.z);
		} else if (object instanceof TileEntity) {
			final TileEntity tileEntity = (TileEntity) object;
			return tileEntity.getWorld() != null
			    && x == tileEntity.getPos().getX()
			    && y == tileEntity.getPos().getY()
			    && z == tileEntity.getPos().getZ()
			    && dimensionId.equals(tileEntity.getWorld().getDimension().getType().getRegistryName());
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return dimensionId.hashCode() << 24 + (x >> 10) << 12 + y << 10 + (z >> 10);
	}
	
	@Override
	public String toString() {
		return String.format("GlobalPosition{%s (%d %d %d)}",
		                     dimensionId, x, y, z );
	}
}