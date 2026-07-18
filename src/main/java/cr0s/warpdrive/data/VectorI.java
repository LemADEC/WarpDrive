package cr0s.warpdrive.data;


import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

/**
 * Generic 3D vector for efficient block manipulation.
 * Loosely based on Mojang Vec3d and Calclavia Vector3. 
 *
 * @author LemADEC
 */
public class VectorI implements Cloneable {
	public int x;
	public int y;
	public int z;
	
	public VectorI() {
		this(0, 0, 0);
	}
	
	// constructor from float/double is voluntarily skipped
	// if you need it, you're probably doing something wrong :)
	
	public VectorI(final int x, final int y, final int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public VectorI(final Entity entity) {
		x = ((int) Math.floor(entity.getPosX()));
		y = ((int) Math.floor(entity.getPosY()));
		z = ((int) Math.floor(entity.getPosZ()));
	}
	
	public VectorI(final TileEntity tileEntity) {
		x = tileEntity.getPos().getX();
		y = tileEntity.getPos().getY();
		z = tileEntity.getPos().getZ();
	}
	
	public VectorI(final RayTraceResult movingObject) {
		x = (int) Math.floor(movingObject.getHitVec().getX());
		y = (int) Math.floor(movingObject.getHitVec().getY());
		z = (int) Math.floor(movingObject.getHitVec().getZ());
	}
	
	public VectorI(final BlockPos blockPos) {
		x = blockPos.getX();
		y = blockPos.getY();
		z = blockPos.getZ();
	}
	
	public VectorI(final Direction direction) {
		x = direction.getXOffset();
		y = direction.getYOffset();
		z = direction.getZOffset();
	}
	
	
	public Vector3 getBlockCenter() {
		return new Vector3(x + 0.5D, y + 0.5D, z + 0.5D);
	}
	
	
	@Override
	public VectorI clone() {
		return new VectorI(x, y, z);
	}
	
	public VectorI invertedClone() {
		return new VectorI(-x, -y, -z);
	}
	
	// clone in a given direction
	public VectorI clone(final Direction side) {
		return new VectorI(x + side.getXOffset(), y + side.getYOffset(), z + side.getZOffset());
	}
	
	public Block getBlock(final IWorldReader worldReader) {
		return worldReader.getBlockState(new BlockPos(x, y, z)).getBlock();
	}
	
	public BlockState getBlockState(final IWorldReader worldReader) {
		return worldReader.getBlockState(new BlockPos(x, y, z));
	}
	
	public TileEntity getTileEntity(final IWorldReader worldReader) {
		return worldReader.getTileEntity(new BlockPos(x, y, z));
	}
	
	public void setBlockState(final World world, final BlockState blockState) {
		world.setBlockState(getBlockPos(), blockState, 3);
	}
	
	
	// modify current vector by adding another one
	public VectorI translate(final VectorI vector) {
		x += vector.x;
		y += vector.y;
		z += vector.z;
		return this;
	}

	// modify current vector by subtracting another one
	public VectorI translateBack(final VectorI vector) {
		x -= vector.x;
		y -= vector.y;
		z -= vector.z;
		return this;
	}
	
	// modify current vector by translation of amount block in side direction
	public VectorI translate(final Direction side, final int amount) {
		switch (side) {
		case DOWN:
			y -= amount;
			break;
		case UP:
			y += amount;
			break;
		case NORTH:
			z -= amount;
			break;
		case SOUTH:
			z += amount;
			break;
		case WEST:
			x -= amount;
			break;
		case EAST:
			x += amount;
			break;
		default:
			break;
		}
		
		return this;
	}
	
	// modify current vector by translation of 1 block in side direction
	public VectorI translate(final Direction side) {
		x += side.getXOffset();
		y += side.getYOffset();
		z += side.getZOffset();
		return this;
	}
	
	// return a new vector adding both parts
	public static VectorI add(final VectorI vector1, final VectorI vector2) {
		return new VectorI(vector1.x + vector2.x, vector1.y + vector2.y, vector1.z + vector2.z);
	}
	
	// return a new vector adding both parts
	public VectorI add(final VectorI vector) {
		return new VectorI(x + vector.x, y + vector.y, z + vector.z);
	}
	
	// return a new vector adding both parts
	@Deprecated
	public VectorI add(final Vector3 vector) {
		x = ((int) (x + Math.round(vector.x)));
		y = ((int) (y + Math.round(vector.y)));
		z = ((int) (z + Math.round(vector.z)));
		return this;
	}
	
	
	// return a new vector subtracting both parts
	public static VectorI subtract(final VectorI vector1, final VectorI vector2) {
		return new VectorI(vector1.x - vector2.x, vector1.y - vector2.y, vector1.z - vector2.z);
	}
	
	// return a new vector subtracting the argument from current vector
	public VectorI subtract(final VectorI vector) {
		return new VectorI(x - vector.x, y - vector.y, z - vector.z);
	}
	
	
	@Deprecated
	public static VectorI set(final Vector3 vector) {
		return new VectorI((int) Math.round(vector.x), (int) Math.round(vector.y), (int) Math.round(vector.z));
	}
	
	@Override
	public int hashCode() {
		return (x + "X" + y + "Y" + z + "lem").hashCode();
	}
	
	public boolean equals(final TileEntity tileEntity) {
		return (x == tileEntity.getPos().getX()) && (y == tileEntity.getPos().getY()) && (z == tileEntity.getPos().getZ());
	}
	
	@Override
	public boolean equals(final Object object) {
		if (object instanceof VectorI) {
			final VectorI vector = (VectorI) object;
			return (x == vector.x) && (y == vector.y) && (z == vector.z);
		} else if (object instanceof TileEntity) {
			final TileEntity tileEntity = (TileEntity) object;
			return (x == tileEntity.getPos().getX())
			    && (y == tileEntity.getPos().getY())
			    && (z == tileEntity.getPos().getZ());
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		return "VectorI [" + x + " " + y + " " + z + "]";
	}
	
	
	public static VectorI createFromNBT(final CompoundNBT tagCompound) {
		final VectorI vector = new VectorI();
		vector.read(tagCompound);
		return vector;
	}
	
	public void read(@Nonnull final CompoundNBT tagCompound) {
		x = tagCompound.getInt("x");
		y = tagCompound.getInt("y");
		z = tagCompound.getInt("z");
	}
	
	public CompoundNBT write(@Nonnull final CompoundNBT tagCompound) {
		tagCompound.putInt("x", x);
		tagCompound.putInt("y", y);
		tagCompound.putInt("z", z);
		return tagCompound;
	}
	
	public long distance2To(final BlockPos blockPos) {
		final int newX = blockPos.getX() - x;
		final int newY = blockPos.getY() - y;
		final int newZ = blockPos.getZ() - z;
		return (long) newX * newX + (long) newY * newY + (long) newZ * newZ;
	}
	
	public long distance2To(final VectorI vector) {
		final int newX = vector.x - x;
		final int newY = vector.y - y;
		final int newZ = vector.z - z;
		return (long) newX * newX + (long) newY * newY + (long) newZ * newZ;
	}
	
	public double distance2To(final Entity entity) {
		final double newX = entity.getPosX() - x;
		final double newY = entity.getPosY() - y;
		final double newZ = entity.getPosZ() - z;
		return newX * newX + newY * newY + newZ * newZ;
	}
	
	public long distance2To(final TileEntity tileEntity) {
		final int newX = tileEntity.getPos().getX() - x;
		final int newY = tileEntity.getPos().getY() - y;
		final int newZ = tileEntity.getPos().getZ() - z;
		return (long) newX * newX + (long) newY * newY + (long) newZ * newZ;
	}
	
	public static long distance2To(final VectorI vector1, final VectorI vector2) {
		final int newX = vector1.x - vector2.x;
		final int newY = vector1.y - vector2.y;
		final int newZ = vector1.z - vector2.z;
		return (long) newX * newX + (long) newY * newY + (long) newZ * newZ;
	}
	
	public long getMagnitudeSquared() {
		return (long) x * x + (long) y * y + (long) z * z;
	}
	
	public VectorI scale(final float amount) {
		x = Math.round(x * amount);
		y = Math.round(y * amount);
		z = Math.round(z * amount);
		return this;
	}
	
	public void rotateByAngle(final double yaw, final double pitch) {
		rotateByAngle(yaw, pitch, 0.0D);
	}
	
	public void rotateByAngle(final double yaw, final double pitch, final double roll) {
		final double yawRadians = Math.toRadians(yaw);
		final double yawCosinus = Math.cos(yawRadians);
		final double yawSinus = Math.sin(yawRadians);
		final double pitchRadians = Math.toRadians(pitch);
		final double pitchCosinus = Math.cos(pitchRadians);
		final double pitchSinus = Math.sin(pitchRadians);
		final double rollRadians = Math.toRadians(roll);
		final double rollCosinus = Math.cos(rollRadians);
		final double rollSinus = Math.sin(rollRadians);
		
		final double oldX = x;
		final double oldY = y;
		final double oldZ = z;
		
		x = (int)Math.round(( oldX * yawCosinus * pitchCosinus
			+ oldZ * (yawCosinus * pitchSinus * rollSinus - yawSinus * rollCosinus)
			+ oldY * (yawCosinus * pitchSinus * rollCosinus + yawSinus * rollSinus)));
		
		z = (int)Math.round(( oldX * yawSinus * pitchCosinus
			+ oldZ * (yawSinus * pitchSinus * rollSinus + yawCosinus * rollCosinus)
			+ oldY * (yawSinus * pitchSinus * rollCosinus - yawCosinus * rollSinus)));
		
		y = (int)Math.round((-oldX * pitchSinus + oldZ * pitchCosinus * rollSinus
			+ oldY * pitchCosinus * rollCosinus));
	}

	public BlockPos getBlockPos() {
		return new BlockPos(x, y, z);
	}
}