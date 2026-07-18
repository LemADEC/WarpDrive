package cr0s.warpdrive.event;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.GenericSet;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.config.Filler;
import cr0s.warpdrive.config.structures.OrbInstance;
import cr0s.warpdrive.config.structures.StructureGroup;
import cr0s.warpdrive.data.CelestialObject;

import java.util.Random;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.gen.ChunkGenerator;

public class CommonWorldGenerator {// TODO MC1.15 world generation StructureStart
	
	public void place(final Random random, final int chunkX, final int chunkZ,
	                  final World world, final ChunkGenerator<?> chunkGenerator, final AbstractChunkProvider chunkProvider) {
		// chunk data creation
		ChunkHandler.onGenerated(world, chunkX, chunkZ);
		
		// actual structure generation
		try {
			final int x = (chunkX * 16) + (5 - random.nextInt(10));
			final int z = (chunkZ * 16) + (5 - random.nextInt(10));
			final CelestialObject celestialObject = CelestialObjectManager.get(world, x, z);
			if (celestialObject == null) {
				// as observed on 1.7.10: during world transition, the generator from the previous world is still called
				return;
			}
			if ( Math.abs(x - celestialObject.dimensionCenterX) > celestialObject.borderRadiusX
			  || Math.abs(z - celestialObject.dimensionCenterZ) > celestialObject.borderRadiusZ ) {
				return;
			}
			final int y = WarpDriveConfig.SPACE_GENERATOR_Y_MIN_CENTER
			            + random.nextInt(WarpDriveConfig.SPACE_GENERATOR_Y_MAX_CENTER - WarpDriveConfig.SPACE_GENERATOR_Y_MIN_CENTER);
			
			final StructureGroup structureGroup = celestialObject.getRandomStructure(random, x, z);
			if (structureGroup == null) {
				return;
			}
			structureGroup.place(world, random, x, y, z);
			
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
		}
	}
	
	/**
	 * 
	 * @deprecated reference design for EntitySphereGenerator
	 **/
	@Deprecated
	public static void generateSphereDirect(
			final  OrbInstance orbInstance, final World world, final int xCoord, final int yCoord, final int zCoord) {
		final double dRadius = orbInstance.getTotalThickness() + 0.5D; // Radius from center of block
		final double dSqRadius = dRadius * dRadius; // Optimization to avoid square roots...
		// sphere
		final int ceilRadius = (int) Math.ceil(dRadius);
		
		// Pass the cube and check points for sphere equation x^2 + y^2 + z^2 = r^2
		final BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable(xCoord, yCoord, zCoord);
		for (int x = 0; x <= ceilRadius; x++) {
			final double dX2 = (x + 0.5D) * (x + 0.5D);
			for (int y = 0; y <= ceilRadius; y++) {
				final double dX2Y2 = dX2 + (y + 0.5D) * (y + 0.5D);
				for (int z = 0; z <= ceilRadius; z++) {
					final double dZ2 = (z + 0.5D) * (z + 0.5D);
					final double dSqRange = dX2Y2 + dZ2; // squared distance from current position
					
					// Skip too far blocks
					if (dSqRange > dSqRadius) {
						continue;
					}
					
					// Place blocks
					// cheat by using axial symmetry so we don't create random numbers too frequently
					final int intSqRange = (int) Math.round(dSqRange);
					final GenericSet<Filler> fillerSet = orbInstance.getFillerSetFromSquareRange(intSqRange);
					final Filler filler = fillerSet.getRandomUnit(world.rand);
					filler.setBlock(world, mutableBlockPos.setPos(xCoord + x, yCoord + y, zCoord + z));
					filler.setBlock(world, mutableBlockPos.setPos(xCoord - x, yCoord + y, zCoord + z));
					filler.setBlock(world, mutableBlockPos.setPos(xCoord + x, yCoord - y, zCoord + z));
					filler.setBlock(world, mutableBlockPos.setPos(xCoord + x, yCoord + y, zCoord - z));
					filler.setBlock(world, mutableBlockPos.setPos(xCoord - x, yCoord - y, zCoord + z));
					filler.setBlock(world, mutableBlockPos.setPos(xCoord + x, yCoord - y, zCoord - z));
					filler.setBlock(world, mutableBlockPos.setPos(xCoord - x, yCoord + y, zCoord - z));
					filler.setBlock(world, mutableBlockPos.setPos(xCoord - x, yCoord - y, zCoord - z));
				}
			}
		}
	}
}
