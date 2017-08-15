package cr0s.warpdrive.api;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public interface ITransformation {
	
	World getTargetWorld();
	
	byte getRotationSteps();
	
	float getRotationYaw();
	
	boolean isInside(final double x, final double y, final double z);
	
	boolean isInside(final int x, final int y, final int z);
	
	Vec3 apply(final double sourceX, final double sourceY, final double sourceZ);
	
	ChunkCoordinates apply(final int sourceX, final int sourceY, final int sourceZ);
	
	ChunkCoordinates apply(final TileEntity tileEntity);
	
	ChunkCoordinates apply(final ChunkCoordinates chunkCoordinates);
}