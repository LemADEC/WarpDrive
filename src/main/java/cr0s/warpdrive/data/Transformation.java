package cr0s.warpdrive.data;

import cr0s.warpdrive.api.ITransformation;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class Transformation implements ITransformation {
	private final VectorI sourceCore;
	private final VectorI targetCore;
	private final VectorI move;
	private final byte rotationSteps;
	private final World targetWorld;
	private final int minX;
	private final int minY;
	private final int minZ;
	private final int maxX;
	private final int maxY;
	private final int maxZ;
	
	public Transformation(JumpShip ship, World targetWorld, int moveX, int moveY, int moveZ, byte rotationSteps) {
		sourceCore = new VectorI(ship.coreX, ship.coreY, ship.coreZ);
		minX = ship.minX;
		minY = ship.minY;
		minZ = ship.minZ;
		maxX = ship.maxX;
		maxY = ship.maxY;
		maxZ = ship.maxZ;
		this.targetWorld = targetWorld;
		move = new VectorI(moveX, moveY, moveZ);
		targetCore = sourceCore.add(move);
		this.rotationSteps = (byte) ((rotationSteps + 4) % 4);
	}
	
	@Override
	public World getTargetWorld() {
		return targetWorld;
	}
	
	@Override
	public byte getRotationSteps() {
		return rotationSteps;
	}
	
	@Override
	public float getRotationYaw() {
		return 90.0F * rotationSteps;
	}
	
	@Override
	public boolean isInside(final double x, final double y, final double z) {
		return minX <= x && x <= maxX + 1.0D
		    && minY <= y && y <= maxY + 1.0D
		    && minZ <= z && z <= maxZ + 1.0D;
	}
	
	@Override
	public boolean isInside(final int x, final int y, final int z) {
		return minX <= x && x <= maxX
		    && minY <= y && y <= maxY
		    && minZ <= z && z <= maxZ;
	}
	
	@Override
	public Vec3 apply(final double sourceX, final double sourceY, final double sourceZ) {
		if (rotationSteps == 0) {
			return Vec3.createVectorHelper(sourceX + move.x, sourceY + move.y, sourceZ + move.z);
		} else {
			double dX = sourceX - sourceCore.x - 0.5D;
			double dZ = sourceZ - sourceCore.z - 0.5D;
			switch (rotationSteps) {
			case 1:
				return Vec3.createVectorHelper(targetCore.x + 0.5D - dZ, sourceY + move.y, targetCore.z + 0.5D + dX);
			case 2:
				return Vec3.createVectorHelper(targetCore.x + 0.5D - dX, sourceY + move.y, targetCore.z + 0.5D - dZ);
			case 3:
				return Vec3.createVectorHelper(targetCore.x + 0.5D + dZ, sourceY + move.y, targetCore.z + 0.5D - dX);
			default:
				return null; // dead code
			}
		}
	}
	
	@Override
	public ChunkCoordinates apply(final int sourceX, final int sourceY, final int sourceZ) {
		if (rotationSteps == 0) {
			return new ChunkCoordinates(sourceX + move.x, sourceY + move.y, sourceZ + move.z);
		} else {
			int dX = sourceX - sourceCore.x;
			int dZ = sourceZ - sourceCore.z;
			switch (rotationSteps) {
			case 1:
				return new ChunkCoordinates(targetCore.x - dZ, sourceY + move.y, targetCore.z + dX);
			case 2:
				return new ChunkCoordinates(targetCore.x - dX, sourceY + move.y, targetCore.z - dZ);
			case 3:
				return new ChunkCoordinates(targetCore.x + dZ, sourceY + move.y, targetCore.z - dX);
			default:
				return null; // dead code
			}
		}
	}
	
	@Override
	public ChunkCoordinates apply(final TileEntity tileEntity) {
		return apply(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
	}
	
	@Override
	public ChunkCoordinates apply(final ChunkCoordinates chunkCoordinates) {
		return apply(chunkCoordinates.posX, chunkCoordinates.posY, chunkCoordinates.posZ);
	}

	public void rotate(Entity entity) {
		if (rotationSteps == 0) {
			return;
		}
		entity.rotationYaw = (entity.rotationYaw + 270.0F * rotationSteps) % 360.0F - 180.0F; 
	}
}
