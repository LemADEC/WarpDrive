package cr0s.warpdrive.data;


import net.minecraft.nbt.NBTTagCompound;

/**
 * TransitionPlane Class is used for defining transition planes between dimensions.
 *
 * @author LemADEC
 */

public class TransitionPlane implements Cloneable {
    public int	dimensionId;
	public int  dimensionCenterX, dimensionCenterZ;
	public int  borderSizeX, borderSizeZ;
	public int  spaceCenterX, spaceCenterZ;

    public TransitionPlane() {
        this(0, 0, 0, 5000, 5000, 0, 0);
    }

    public TransitionPlane(int parDimensionId, int parDimensionCenterX, int parDimensionCenterZ, int parBorderSizeX, int parBorderSizeZ, int parSpaceCenterX, int parSpaceCenterZ) {
        this.dimensionId = parDimensionId;
        this.spaceCenterX = parSpaceCenterX;
        this.spaceCenterZ = parSpaceCenterZ;
        this.dimensionCenterX = parDimensionCenterX;
        this.dimensionCenterZ = parDimensionCenterZ;
        this.borderSizeX = parBorderSizeX;
        this.borderSizeZ = parBorderSizeZ;
    }

    public TransitionPlane(NBTTagCompound nbt) {
    	readFromNBT(nbt);
    }

    /**
     * Makes a new copy of this TransitionPlane. Prevents variable referencing problems.
     */
    @Override
    public TransitionPlane clone() {
        return new TransitionPlane(dimensionId, dimensionCenterX, dimensionCenterZ, borderSizeX, borderSizeZ, spaceCenterX, spaceCenterZ);
    }

    /**
     * Check if current coordinates allow to take off from this dimension to reach space.
     * It's up to caller to verify if this transition plane match current dimension.
     *
     * @param current position in the dimension
     * @return distance to transition borders, 0 if take off is possible
     */
    public int isValidToSpace(Vector3 currentPosition) {
        if ( (Math.abs(currentPosition.x - dimensionCenterX) <= borderSizeX) && (Math.abs(currentPosition.z - dimensionCenterZ) <= borderSizeZ) ) {
        	return 0;
        }
        return (int) Math.sqrt(
        		  Math.pow(Math.max(0D, Math.abs(currentPosition.x - dimensionCenterX) - borderSizeX), 2.0D)
        		+ Math.pow(Math.max(0D, Math.abs(currentPosition.z - dimensionCenterZ) - borderSizeZ), 2.0D) );
    }

    /**
     * Check if current space coordinates allow to enter this dimension atmosphere from space.
     * It's up to caller to verify if we're actually in space.
     *
     * @param current position in space
     * @return distance to transition borders, 0 if entry is possible
     */
    public int isValidFromSpace(Vector3 currentPosition) {
        if ( (Math.abs(currentPosition.x - spaceCenterX) <= borderSizeX) && (Math.abs(currentPosition.z - spaceCenterZ) <= borderSizeZ) ) {
        	return 0;
        }
        return (int) Math.sqrt(
      		  Math.pow(Math.max(0D, Math.abs(currentPosition.x - spaceCenterX) - borderSizeX), 2.0D)
      		+ Math.pow(Math.max(0D, Math.abs(currentPosition.z - spaceCenterZ) - borderSizeZ), 2.0D) );
    }

    public void readFromNBT(NBTTagCompound tag) {
        this.dimensionId = tag.getInteger("dimensionId");
        this.dimensionCenterX = tag.getInteger("dimensionCenterX");
        this.dimensionCenterZ = tag.getInteger("dimensionCenterZ");
        this.borderSizeX = tag.getInteger("borderSizeX");
        this.borderSizeZ = tag.getInteger("borderSizeZ");
        this.spaceCenterX = tag.getInteger("spaceCenterX");
        this.spaceCenterZ = tag.getInteger("spaceCenterZ");
    }
    
    public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("dimensionId", dimensionId);
        tag.setInteger("dimensionCenterX", dimensionCenterX);
        tag.setInteger("dimensionCenterZ", dimensionCenterZ);
        tag.setInteger("borderSizeX", borderSizeX);
        tag.setInteger("borderSizeZ", borderSizeZ);
        tag.setInteger("spaceCenterX", spaceCenterX);
        tag.setInteger("spaceCenterZ", spaceCenterZ);
    }


    @Override
    public int hashCode() {
        return ("dim:" + dimensionId + "(" + dimensionCenterX + "," + dimensionCenterZ + ")_Border(" + borderSizeX + "," + borderSizeZ + ")_Space(" + spaceCenterX + "," + spaceCenterZ).hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TransitionPlane) {
        	TransitionPlane transitionPlane = (TransitionPlane) o;
            return this.dimensionId == transitionPlane.dimensionId
            	&& this.dimensionCenterX == transitionPlane.dimensionCenterX && this.dimensionCenterZ == transitionPlane.dimensionCenterZ
            	&& this.borderSizeX == transitionPlane.borderSizeX && this.borderSizeZ == transitionPlane.borderSizeZ
            	&& this.spaceCenterX == transitionPlane.spaceCenterX && this.spaceCenterZ == transitionPlane.spaceCenterZ;
        }

        return false;
    }

    @Override
    public String toString() {
        return "TransitionPlane [dim:" + dimensionId + "(" + dimensionCenterX + ", " + dimensionCenterZ + ") Border(" + borderSizeX + ", " + borderSizeZ + ") Space(" + spaceCenterX + ", " + spaceCenterZ + ")]";
    }
}