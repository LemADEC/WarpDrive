package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.data.EnumShipCommand;
import cr0s.warpdrive.data.EnumShipMovementType;
import cr0s.warpdrive.data.VectorI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;

public class ShipMovementPreview {
	
	public final EnumShipCommand command;
	public final EnumShipMovementType movementType;
	public final VectorI requestedMovement;
	public final VectorI effectiveMovement;
	public final byte rotationSteps;
	public final int requestedDistance;
	public final int effectiveDistance;
	public final int maximumDistance;
	public final int energyRequired;
	public final long energyStored;
	public final boolean canEngage;
	public final boolean wouldBeClamped;
	public final String blockerKey;
	public final String blockerMessage;
	
	public ShipMovementPreview(@Nonnull final EnumShipCommand command,
	                           @Nullable final EnumShipMovementType movementType,
	                           @Nonnull final VectorI requestedMovement,
	                           @Nonnull final VectorI effectiveMovement,
	                           final byte rotationSteps,
	                           final int requestedDistance,
	                           final int effectiveDistance,
	                           final int maximumDistance,
	                           final int energyRequired,
	                           final long energyStored,
	                           final boolean canEngage,
	                           final boolean wouldBeClamped,
	                           @Nonnull final String blockerKey,
	                           @Nonnull final String blockerMessage) {
		this.command = command;
		this.movementType = movementType == null ? EnumShipMovementType.NONE : movementType;
		this.requestedMovement = requestedMovement;
		this.effectiveMovement = effectiveMovement;
		this.rotationSteps = rotationSteps;
		this.requestedDistance = requestedDistance;
		this.effectiveDistance = effectiveDistance;
		this.maximumDistance = maximumDistance;
		this.energyRequired = energyRequired;
		this.energyStored = energyStored;
		this.canEngage = canEngage;
		this.wouldBeClamped = wouldBeClamped;
		this.blockerKey = blockerKey;
		this.blockerMessage = blockerMessage;
	}
	
	@Nonnull
	public NBTTagCompound writeToNBT() {
		final NBTTagCompound tagCompound = new NBTTagCompound();
		tagCompound.setString("command", command.getName());
		tagCompound.setString("movementType", movementType.getName());
		tagCompound.setInteger("requestedMoveFront", requestedMovement.x);
		tagCompound.setInteger("requestedMoveUp", requestedMovement.y);
		tagCompound.setInteger("requestedMoveRight", requestedMovement.z);
		tagCompound.setInteger("effectiveMoveFront", effectiveMovement.x);
		tagCompound.setInteger("effectiveMoveUp", effectiveMovement.y);
		tagCompound.setInteger("effectiveMoveRight", effectiveMovement.z);
		tagCompound.setByte("rotationSteps", rotationSteps);
		tagCompound.setInteger("requestedDistance", requestedDistance);
		tagCompound.setInteger("effectiveDistance", effectiveDistance);
		tagCompound.setInteger("maximumDistance", maximumDistance);
		tagCompound.setInteger("energyRequired", energyRequired);
		tagCompound.setLong("energyStored", energyStored);
		tagCompound.setBoolean("canEngage", canEngage);
		tagCompound.setBoolean("wouldBeClamped", wouldBeClamped);
		tagCompound.setString("blockerKey", blockerKey);
		tagCompound.setString("blockerMessage", blockerMessage);
		return tagCompound;
	}
	
	@Nonnull
	public Object[] toObjectArray() {
		return new Object[] {
				canEngage,
				blockerKey,
				blockerMessage,
				command.getName(),
				movementType.getName(),
				requestedMovement.x,
				requestedMovement.y,
				requestedMovement.z,
				effectiveMovement.x,
				effectiveMovement.y,
				effectiveMovement.z,
				(int) rotationSteps,
				requestedDistance,
				effectiveDistance,
				maximumDistance,
				energyRequired,
				energyStored,
				wouldBeClamped
		};
	}
}
