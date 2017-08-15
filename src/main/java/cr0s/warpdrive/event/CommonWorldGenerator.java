package cr0s.warpdrive.event;

import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.config.Filler;
import cr0s.warpdrive.config.structures.Orb.OrbShell;
import cr0s.warpdrive.config.structures.OrbInstance;
import cr0s.warpdrive.config.structures.StructureGroup;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.event.ChunkHandler;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import cpw.mods.fml.common.IWorldGenerator;

public class CommonWorldGenerator implements IWorldGenerator {

	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
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
			if ( celestialObject.borderRadiusX > 0
			  && ( Math.abs(x - celestialObject.dimensionCenterX) > celestialObject.borderRadiusX
			    || Math.abs(z - celestialObject.dimensionCenterZ) > celestialObject.borderRadiusZ ) ) {
				return;
			}
			final int y = WarpDriveConfig.SPACE_GENERATOR_Y_MIN_CENTER
			            + random.nextInt(WarpDriveConfig.SPACE_GENERATOR_Y_MAX_CENTER - WarpDriveConfig.SPACE_GENERATOR_Y_MIN_CENTER);
			
			final StructureGroup structureGroup = celestialObject.getRandomStructure(random, x, z);
			if (structureGroup == null) {
				return;
			}
			structureGroup.generate(world, random, x, y, z);
			
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @deprecated reference design for EntitySphereGenerator
	 **/
	@Deprecated
	public static void generateSphereDirect(
	                                       OrbInstance orbInstance, World world, int xCoord, int yCoord, int zCoord) {
		double radiusC = orbInstance.getTotalThickness() + 0.5D; // Radius from center of block
		double radiusSq = radiusC * radiusC; // Optimization to avoid square roots...
		// sphere
		int ceilRadius = (int) Math.ceil(radiusC);
		
		// Pass the cube and check points for sphere equation x^2 + y^2 + z^2 = r^2
		for (int x = 0; x <= ceilRadius; x++) {
			double dX2 = (x + 0.5D) * (x + 0.5D);
			for (int y = 0; y <= ceilRadius; y++) {
				double dX2Y2 = dX2 + (y + 0.5D) * (y + 0.5D);
				for (int z = 0; z <= ceilRadius; z++) {
					double dZ2 = (z + 0.5D) * (z + 0.5D);
					double dSq = dX2Y2 + dZ2; // squared distance from current position
					
					// Skip too far blocks
					if (dSq > radiusSq) {
						continue;
					}
					
					// Place blocks
					// cheat by using axial symmetry so we don't create random numbers too frequently
					
					OrbShell orbShell = orbInstance.getShellForSqRadius(dSq);
					Filler filler = orbShell.getRandomUnit(world.rand);
					filler.setBlock(world, xCoord + x, yCoord + y, zCoord + z);
					filler.setBlock(world, xCoord - x, yCoord + y, zCoord + z);
					filler.setBlock(world, xCoord + x, yCoord - y, zCoord + z);
					filler.setBlock(world, xCoord + x, yCoord + y, zCoord - z);
					filler.setBlock(world, xCoord - x, yCoord - y, zCoord + z);
					filler.setBlock(world, xCoord + x, yCoord - y, zCoord - z);
					filler.setBlock(world, xCoord - x, yCoord + y, zCoord - z);
					filler.setBlock(world, xCoord - x, yCoord - y, zCoord - z);
				}
			}
		}
	}
}
