package cr0s.warpdrive.data;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.common.util.ForgeDirection;

/**
 * Vector3 Class is used for defining objects in a 3D space.
 *
 * @author Calclavia
 */

public class Vector3 implements Cloneable {
	public double x;
	public double y;
	public double z;
	
	public Vector3() {
		this(0, 0, 0);
	}
	
	public Vector3(final double x, final double y, final double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Vector3(final Entity entity) {
		x = entity.posX;
		y = entity.posY;
		z = entity.posZ;
	}
	
	public Vector3(final TileEntity tileEntity) {
		x = tileEntity.xCoord;
		y = tileEntity.yCoord;
		z = tileEntity.zCoord;
	}
	
	public Vector3(final Vec3 vec3) {
		x = vec3.xCoord;
		y = vec3.yCoord;
		z = vec3.zCoord;
	}
	
	public Vector3(final MovingObjectPosition movingObjectPosition) {
		x = movingObjectPosition.blockX;
		y = movingObjectPosition.blockY;
		z = movingObjectPosition.blockZ;
	}
	
	public Vector3(final ChunkCoordinates chunkCoordinates) {
		x = chunkCoordinates.posX;
		y = chunkCoordinates.posY;
		z = chunkCoordinates.posZ;
	}
	
	public Vector3(final ForgeDirection direction) {
		x = direction.offsetX;
		y = direction.offsetY;
		z = direction.offsetZ;
	}
	
	/**
	 * Returns the coordinates as integers, ideal for block placement.
	 */
	public int intX() {
		return (int) Math.floor(x);
	}
	
	public int intY() {
		return (int) Math.floor(y);
	}
	
	public int intZ() {
		return (int) Math.floor(z);
	}
	
	/**
	 * Makes a new copy of this Vector. Prevents variable referencing problems.
	 */
	@Override
	public Vector3 clone() {
		return new Vector3(x, y, z);
	}
	
	public Block getBlockID(IBlockAccess blockAccess) {
		return blockAccess.getBlock(intX(), intY(), intZ());
	}
	
	public int getBlockMetadata(IBlockAccess blockAccess) {
		return blockAccess.getBlockMetadata(intX(), intY(), intZ());
	}
	
	public TileEntity getTileEntity(IBlockAccess blockAccess) {
		return blockAccess.getTileEntity(intX(), intY(), intZ());
	}
	
	public boolean setBlock(World world, Block id, int metadata, int notify) {
		return world.setBlock(intX(), intY(), intZ(), id, metadata, notify);
	}
	
	public boolean setBlock(World world, Block id, int metadata) {
		return setBlock(world, id, metadata, 3);
	}
	
	public boolean setBlock(World world, Block id) {
		return setBlock(world, id, 0);
	}
	
	/**
	 * Converts this vector three into a Minecraft Vec3 object
	 */
	public Vec3 toVec3() {
		return Vec3.createVectorHelper(x, y, z);
	}
	
	public double getMagnitude() {
		return Math.sqrt(getMagnitudeSquared());
	}
	
	public double getMagnitudeSquared() {
		return x * x + y * y + z * z;
	}
	
	public Vector3 normalize() {
		double d = getMagnitude();
		
		if (d != 0) {
			scale(1 / d);
		}
		
		return this;
	}
	
	/**
	 * Gets the distance between two vectors
	 *
	 * @return The distance
	 */
	public static double distance(Vector3 par1, Vector3 par2) {
		double var2 = par1.x - par2.x;
		double var4 = par1.y - par2.y;
		double var6 = par1.z - par2.z;
		return Math.sqrt(var2 * var2 + var4 * var4 + var6 * var6);
	}
	
	public double distanceTo(Vector3 vector3) {
		double var2 = vector3.x - x;
		double var4 = vector3.y - y;
		double var6 = vector3.z - z;
		return Math.sqrt(var2 * var2 + var4 * var4 + var6 * var6);
	}
	
	public double distanceTo_square(Vector3 vector3) {
		double var2 = vector3.x - x;
		double var4 = vector3.y - y;
		double var6 = vector3.z - z;
		return var2 * var2 + var4 * var4 + var6 * var6;
	}
	
	public double distanceTo_square(Entity entity) {
		double var2 = entity.posX - x;
		double var4 = entity.posY - y;
		double var6 = entity.posZ - z;
		return var2 * var2 + var4 * var4 + var6 * var6;
	}
	
	/**
	 * Multiplies the vector by negative one.
	 */
	public Vector3 invert() {
		scale(-1);
		return this;
	}
	
	public Vector3 translate(Vector3 par1) {
		x += par1.x;
		y += par1.y;
		z += par1.z;
		return this;
	}
	
	public Vector3 translate(double par1) {
		x += par1;
		y += par1;
		z += par1;
		return this;
	}
	
	// modify current vector by translation of amount block in side direction
	public Vector3 translate(final ForgeDirection side, final double amount) {
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
	public Vector3 translate(final ForgeDirection side) {
		x += side.offsetX;
		y += side.offsetY;
		z += side.offsetZ;
		return this;
	}
	
	public static Vector3 translate(Vector3 translate, Vector3 par1) {
		translate.x += par1.x;
		translate.y += par1.y;
		translate.z += par1.z;
		return translate;
	}
	
	public Vector3 translateFactor(Vector3 direction, double factor) {
		this.x += direction.x * factor;
		this.y += direction.y * factor;
		this.z += direction.z * factor;
		return this;
	}
	
	public Vector3 subtract(Vector3 amount) {
		return translate(amount.clone().invert());
	}
	
	public Vector3 subtract(double amount) {
		return translate(-amount);
	}
	
	public Vector3 scale(double amount) {
		x *= amount;
		y *= amount;
		z *= amount;
		return this;
	}
	
	public Vector3 scale(Vector3 amount) {
		x *= amount.x;
		y *= amount.y;
		z *= amount.z;
		return this;
	}
	
	public static Vector3 scale(Vector3 vec, double amount) {
		return vec.scale(amount);
	}
	
	public static Vector3 scale(Vector3 vec, Vector3 amount) {
		return vec.scale(amount);
	}
	
	/**
	 * Static versions of a lot of functions
	 */
	public Vector3 round() {
		return new Vector3(Math.round(x), Math.round(y), Math.round(z));
	}
	
	public Vector3 ceil() {
		return new Vector3(Math.ceil(x), Math.ceil(y), Math.ceil(z));
	}
	
	public Vector3 floor() {
		return new Vector3(Math.floor(x), Math.floor(y), Math.floor(z));
	}
	
	public Vector3 toRound() {
		x = Math.round(x);
		y = Math.round(y);
		z = Math.round(z);
		return this;
	}
	
	public Vector3 toCeil() {
		x = Math.ceil(x);
		y = Math.ceil(y);
		z = Math.ceil(z);
		return this;
	}
	
	public Vector3 toFloor() {
		x = Math.floor(x);
		y = Math.floor(y);
		z = Math.floor(z);
		return this;
	}
	
	/**
	 * Gets all entities inside of this position in block space.
	 */
	public List<Entity> getEntitiesWithin(World worldObj, Class<? extends Entity> par1Class) {
		return worldObj.getEntitiesWithinAABB(par1Class,
				AxisAlignedBB.getBoundingBox(intX(), intY(), intZ(), intX() + 1, intY() + 1, intZ() + 1));
	}
	
	/**
	 * Gets a position relative to a position's side
	 */
	public Vector3 modifyPositionFromSide(ForgeDirection side, double amount) {
		switch (side.ordinal()) {
		case 0:
			y -= amount;
			break;
		
		case 1:
			y += amount;
			break;
		
		case 2:
			z -= amount;
			break;
		
		case 3:
			z += amount;
			break;
		
		case 4:
			x -= amount;
			break;
		
		case 5:
			x += amount;
			break;
		
		default:
			break;
		}
		
		return this;
	}
	
	public Vector3 modifyPositionFromSide(ForgeDirection side) {
		modifyPositionFromSide(side, 1);
		return this;
	}
	
	/**
	 * Cross product functions
	 *
	 * @return The cross product between this vector and another.
	 */
	public Vector3 crossProduct(Vector3 vec2) {
		return new Vector3(y * vec2.z - z * vec2.y, z * vec2.x - x * vec2.z, x * vec2.y - y * vec2.x);
	}
	
	public Vector3 xCrossProduct() {
		return new Vector3(0.0D, z, -y);
	}
	
	public Vector3 zCrossProduct() {
		return new Vector3(-y, x, 0.0D);
	}
	
	public double dotProduct(Vector3 vec2) {
		return x * vec2.x + y * vec2.y + z * vec2.z;
	}
	
	/**
	 * @return The perpendicular vector.
	 */
	public Vector3 getPerpendicular() {
		if (z == 0.0F) {
			return zCrossProduct();
		}
		
		return xCrossProduct();
	}
	
	/**
	 * @return True if this Vector3 is zero.
	 */
	public boolean isZero() {
		return (x == 0.0F) && (y == 0.0F) && (z == 0.0F);
	}
	
	/**
	 * Rotate by a this vector around an axis.
	 *
	 * @return The new Vector3 rotation.
	 */
	public Vector3 rotate(float angle, Vector3 axis) {
		return translateMatrix(getRotationMatrix(angle, axis), clone());
	}
	
	public double[] getRotationMatrix(float angle_deg) {
		double[] matrix = new double[16];
		Vector3 axis = clone().normalize();
		double xn = axis.x;
		double yn = axis.y;
		double zn = axis.z;
		float angle_rad = angle_deg * 0.0174532925F;
		float cos = (float) Math.cos(angle_rad);
		float oCos = 1.0F - cos;
		float sin = (float) Math.sin(angle_rad);
		matrix[0] = (xn * xn * oCos + cos);
		matrix[1] = (yn * xn * oCos + zn * sin);
		matrix[2] = (xn * zn * oCos - yn * sin);
		matrix[4] = (xn * yn * oCos - zn * sin);
		matrix[5] = (yn * yn * oCos + cos);
		matrix[6] = (yn * zn * oCos + xn * sin);
		matrix[8] = (xn * zn * oCos + yn * sin);
		matrix[9] = (yn * zn * oCos - xn * sin);
		matrix[10] = (zn * zn * oCos + cos);
		matrix[15] = 1.0F;
		return matrix;
	}
	
	public static Vector3 translateMatrix(double[] matrix, Vector3 translation) {
		double x = translation.x * matrix[0] + translation.y * matrix[1] + translation.z * matrix[2] + matrix[3];
		double y = translation.x * matrix[4] + translation.y * matrix[5] + translation.z * matrix[6] + matrix[7];
		double z = translation.x * matrix[8] + translation.y * matrix[9] + translation.z * matrix[10] + matrix[11];
		translation.x = x;
		translation.y = y;
		translation.z = z;
		return translation;
	}
	
	public static double[] getRotationMatrix(float angle, Vector3 axis) {
		return axis.getRotationMatrix(angle);
	}
	
	/**
	 * Rotates this Vector by a yaw, pitch and roll value.
	 */
	public void rotate(double yaw, double pitch, double roll) {
		double yawRadians = Math.toRadians(yaw);
		double pitchRadians = Math.toRadians(pitch);
		double rollRadians = Math.toRadians(roll);
		double oldX = x;
		double oldY = y;
		double oldZ = z;
		x = oldX * Math.cos(yawRadians) * Math.cos(pitchRadians) + oldZ
				* (Math.cos(yawRadians) * Math.sin(pitchRadians) * Math.sin(rollRadians) - Math.sin(yawRadians) * Math.cos(rollRadians)) + oldY
				* (Math.cos(yawRadians) * Math.sin(pitchRadians) * Math.cos(rollRadians) + Math.sin(yawRadians) * Math.sin(rollRadians));
		z = oldX * Math.sin(yawRadians) * Math.cos(pitchRadians) + oldZ
				* (Math.sin(yawRadians) * Math.sin(pitchRadians) * Math.sin(rollRadians) + Math.cos(yawRadians) * Math.cos(rollRadians)) + oldY
				* (Math.sin(yawRadians) * Math.sin(pitchRadians) * Math.cos(rollRadians) - Math.cos(yawRadians) * Math.sin(rollRadians));
		y = -oldX * Math.sin(pitchRadians) + oldZ * Math.cos(pitchRadians) * Math.sin(rollRadians) + oldY * Math.cos(pitchRadians) * Math.cos(rollRadians);
	}
	
	/**
	 * Rotates a point by a yaw and pitch around the anchor 0,0 by a specific
	 * angle.
	 */
	public void rotate(double yaw, double pitch) {
		rotate(yaw, pitch, 0);
	}
	
	public void rotate(double yaw) {
		double yawRadians = Math.toRadians(yaw);
		double oldX = x;
		double oldZ = z;
		
		if (yaw != 0) {
			x = oldX * Math.cos(yawRadians) - oldZ * Math.sin(yawRadians);
			z = oldX * Math.sin(yawRadians) + oldZ * Math.cos(yawRadians);
		}
	}
	
	/**
	 * Gets the delta look position based on the rotation yaw and pitch.
	 * Minecraft coordinates are messed up. Y and Z are flipped. Yaw is
	 * displaced by 90 degrees. Pitch is inverted.
	 */
	public static Vector3 getDeltaPositionFromRotation(float rotationYaw1, float rotationPitch1) {
		float rotationYaw2 = rotationYaw1 + 90;
		float rotationPitch2 = -rotationPitch1;
		return new Vector3(Math.cos(Math.toRadians(rotationYaw2)), Math.sin(Math.toRadians(rotationPitch2)), Math.sin(Math.toRadians(rotationYaw2)));
	}
	
	/**
	 * Gets the angle between this vector and another vector.
	 *
	 * @return Angle in degrees
	 */
	public double getAngle(Vector3 vec2) {
		return anglePreNorm(clone().normalize(), vec2.clone().normalize());
	}
	
	public static double getAngle(Vector3 vec1, Vector3 vec2) {
		return vec1.getAngle(vec2);
	}
	
	public double anglePreNorm(Vector3 vec2) {
		return Math.acos(dotProduct(vec2));
	}
	
	public static double anglePreNorm(Vector3 vec1, Vector3 vec2) {
		return Math.acos(vec1.clone().dotProduct(vec2));
	}
	
	
	public static Vector3 createFromNBT(NBTTagCompound nbtTagCompound) {
		Vector3 vector = new Vector3();
		vector.readFromNBT(nbtTagCompound);
		return vector;
	}
	
	public void readFromNBT(NBTTagCompound nbtTagCompound) {
		x = nbtTagCompound.getDouble("x");
		y = nbtTagCompound.getDouble("y");
		z = nbtTagCompound.getDouble("z");
	}
	
	public NBTTagCompound writeToNBT(NBTTagCompound nbtTagCompound) {
		nbtTagCompound.setDouble("x", x);
		nbtTagCompound.setDouble("y", y);
		nbtTagCompound.setDouble("z", z);
		return nbtTagCompound;
	}
	
	public static Vector3 UP() {
		return new Vector3(0, 1, 0);
	}
	
	public static Vector3 DOWN() {
		return new Vector3(0, -1, 0);
	}
	
	public static Vector3 NORTH() {
		return new Vector3(0, 0, -1);
	}
	
	public static Vector3 SOUTH() {
		return new Vector3(0, 0, 1);
	}
	
	public static Vector3 WEST() {
		return new Vector3(-1, 0, 0);
	}
	
	public static Vector3 EAST() {
		return new Vector3(1, 0, 0);
	}
	
	/**
	 * RayTrace Code, retrieved from MachineMuse.
	 *
	 * @author MachineMuse
	 */
	public MovingObjectPosition rayTrace(World world, float rotationYaw, float rotationPitch, boolean collisionFlag, double reachDistance) {
		// Somehow this destroys the playerPosition vector -.-
		MovingObjectPosition pickedBlock = rayTraceBlocks(world, rotationYaw, rotationPitch, reachDistance);
		MovingObjectPosition pickedEntity = rayTraceEntities(world, rotationYaw, rotationPitch, reachDistance);
		
		if (pickedBlock == null) {
			return pickedEntity;
		} else if (pickedEntity == null) {
			return pickedBlock;
		} else {
			double dBlock = distanceTo(new Vector3(pickedBlock.hitVec));
			double dEntity = distanceTo(new Vector3(pickedEntity.hitVec));
			
			if (dEntity < dBlock) {
				return pickedEntity;
			} else {
				return pickedBlock;
			}
		}
	}
	
	public MovingObjectPosition rayTraceBlocks(World world, float rotationYaw, float rotationPitch, double reachDistance) {
		Vector3 lookVector = getDeltaPositionFromRotation(rotationYaw, rotationPitch);
		Vector3 reachPoint = this.clone().translateFactor(lookVector, reachDistance);
		return world.rayTraceBlocks(toVec3(), reachPoint.toVec3());// TODO: Removed collision flag
	}
	
	public MovingObjectPosition rayTraceEntities(World world, float rotationYaw, float rotationPitch, double reachDistance) {
		MovingObjectPosition pickedEntity = null;
		Vec3 startingPosition = toVec3();
		Vec3 look = getDeltaPositionFromRotation(rotationYaw, rotationPitch).toVec3();
		Vec3 reachPoint = Vec3.createVectorHelper(startingPosition.xCoord + look.xCoord * reachDistance, startingPosition.yCoord + look.yCoord * reachDistance,
				startingPosition.zCoord + look.zCoord * reachDistance);
		double playerBorder = 1.1 * reachDistance;
		AxisAlignedBB boxToScan = AxisAlignedBB.getBoundingBox(-playerBorder, -playerBorder, -playerBorder, playerBorder, playerBorder, playerBorder);
		List<Entity> entitiesHit = world.getEntitiesWithinAABBExcludingEntity(null, boxToScan);
		double closestEntity = reachDistance;
		
		if (entitiesHit == null || entitiesHit.isEmpty()) {
			return null;
		}
		
		for (Entity entityHit : entitiesHit) {
			if (entityHit != null && entityHit.canBeCollidedWith() && entityHit.boundingBox != null) {
				float border = entityHit.getCollisionBorderSize();
				AxisAlignedBB aabb = entityHit.boundingBox.expand(border, border, border);
				MovingObjectPosition hitMOP = aabb.calculateIntercept(startingPosition, reachPoint);
				
				if (hitMOP != null) {
					if (aabb.isVecInside(startingPosition)) {
						if (0.0D < closestEntity || closestEntity == 0.0D) {
							pickedEntity = new MovingObjectPosition(entityHit);
							pickedEntity.hitVec = hitMOP.hitVec;
							closestEntity = 0.0D;
						}
					} else {
						double distance = startingPosition.distanceTo(hitMOP.hitVec);

						if (distance < closestEntity || closestEntity == 0.0D) {
							pickedEntity = new MovingObjectPosition(entityHit);
							pickedEntity.hitVec = hitMOP.hitVec;
							closestEntity = distance;
						}
					}
				}
			}
		}
		
		return pickedEntity;
	}
	
	@Override
	public int hashCode() {
		return ("X:" + x + "Y:" + y + "Z:" + z).hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Vector3) {
			Vector3 vector3 = (Vector3) o;
			return x == vector3.x && y == vector3.y && z == vector3.z;
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		return String.format("Vector3 [%.3f %.3f %.3f]", x, y, z);
	}
}