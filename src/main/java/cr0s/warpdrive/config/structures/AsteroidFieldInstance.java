package cr0s.warpdrive.config.structures;

import cr0s.warpdrive.LocalProfiler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.world.WorldGenSmallShip;
import cr0s.warpdrive.world.WorldGenStation;

import java.util.Random;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class AsteroidFieldInstance extends AbstractStructureInstance {
	
	public AsteroidFieldInstance(final AsteroidField asteroidField, final Random random) {
		super(asteroidField, random);
	}
	
	public AsteroidFieldInstance(final NBTTagCompound tag) {
		super(tag);
		// TODO not implemented
	}
	
	@Override
	public void WriteToNBT(final NBTTagCompound tagCompound) {
		super.WriteToNBT(tagCompound);
		// TODO not implemented
	}
	
	private static float binomialRandom(final World world) {
		float linear = world.rand.nextFloat();
		// ideal sphere repartition = x ^ 0.5 (sqrt)
		// Dilution but slow to compute = 0.5 * ( x ^ 0.3 + 1 + (x - 1) ^ 3 )
		// Optimized 'pushed out' form = 1.25 - 0.625 / (0.5 + 2 * x)
		// Natural sphere with ring = (1 - x ^ 2.5) * x ^ 0.5 + x ^ 4
		
		// rectangular approach: return 0.5F * linear + 0.5F * linear * linear;
		return 1.25F - 0.625F / (0.5F + 2.0F * linear);
	}
	
	@Override
	public boolean generate(final World world, final Random random, final int x, final int y1, final int z) {
		LocalProfiler.start("AsteroidFieldInstance.generate");
		// 6.0.1 au = 120 radius with 60 to 140 big + 60 to 140 small + 5 to 13 gaz
		// 45238 blocks surface with 120 to 280 asteroids => 161 to 376 blocks per asteroid (big & small)
		
		// 6.0.2 av big = 80 to 180 radius with 40 to 90 big + 80 to 200 small + 5 to 13 gaz
		// 20106 to 101787 surface with 120 to 290 asteroids => 69 to 848 blocks per asteroid
		
		// 6.0.2 av small = 30 to 80 radius with 2 to 22 big + 15 to 75 small + 0 to 3 gaz
		// 2827 to 20106 surface with 17 to 97 asteroids => 29 to 1182 blocks per asteroid
		
		// random distanced one = 89727 surface 256 asteroids => 350 blocks per asteroid

		/*
		boolean isBig = world.rand.nextInt(3) == 1;
		int numOfBigAsteroids, numOfSmallAsteroids, numOfClouds, maxDistance, maxHeight;
		if (isBig) {
			numOfBigAsteroids = 40 + world.rand.nextInt(50);
			numOfSmallAsteroids = 80 + world.rand.nextInt(120);
			numOfClouds = 5 + world.rand.nextInt(8);
			maxDistance = 80 + world.rand.nextInt(100);
			maxHeight = 40 + world.rand.nextInt(40);
		} else {
			numOfBigAsteroids = 2 + world.rand.nextInt(20);
			numOfSmallAsteroids = 15 + world.rand.nextInt(60);
			numOfClouds = 0 + world.rand.nextInt(3);
			maxDistance = 30 + world.rand.nextInt(50);
			maxHeight = 30 + world.rand.nextInt(30);
		}
		/**/
		
		float surfacePerAsteroid = 80.0F + world.rand.nextFloat() * 300;
		int maxDistance = 30 + world.rand.nextInt(170);
		int maxDistanceBig = Math.round(maxDistance * (0.6F + 0.2F * world.rand.nextFloat()));
		int maxDistanceSmall = Math.round(maxDistance * 1.1F);
		float bigRatio = 0.3F + world.rand.nextFloat() * 0.3F;
		float surfaceBig = (float) (Math.PI * Math.pow(maxDistanceBig, 2));
		float surfaceSmall = (float) (Math.PI * Math.pow(maxDistanceSmall, 2));
		int numOfBigAsteroids = Math.round(bigRatio * surfaceBig / surfacePerAsteroid);
		int numOfSmallAsteroids = Math.round((1.0F - bigRatio) * surfaceSmall / surfacePerAsteroid);
		int numOfClouds = Math.round(numOfBigAsteroids * 1.0F / (10.0F + world.rand.nextInt(10)));
		int maxHeight = 70 + world.rand.nextInt(50);
		int y2 = Math.min(WarpDriveConfig.SPACE_GENERATOR_Y_MAX_BORDER - maxHeight,
		Math.max(y1, WarpDriveConfig.SPACE_GENERATOR_Y_MIN_BORDER + maxHeight));
		WarpDrive.logger.info("Generating asteroid field at (" + x + " " + y2 + " " + z + ") qty " + numOfBigAsteroids + ", " + numOfSmallAsteroids + ", "
		                      + numOfClouds + " over " + maxDistance + ", " + maxHeight + " surfacePerAsteroid " + String.format("%.1f", surfacePerAsteroid));
		
		// Setting up of big asteroids
		for (int i = 1; i <= numOfBigAsteroids; i++) {
			float binomial = binomialRandom(world);
			double bearing = world.rand.nextFloat() * 2.0D * Math.PI;
			double yawn = world.rand.nextFloat() * Math.PI;
			float horizontalRange = Math.max(6.0F, binomial * maxDistanceBig);
			float verticalRange = Math.max(3.0F, binomial * maxHeight);
			
			int aX = (int) (x + Math.round(horizontalRange * Math.cos(bearing)));
			int aY = (int) (y2 + Math.round(verticalRange * Math.cos(yawn)));
			int aZ = (int) (z + Math.round(horizontalRange * Math.sin(bearing)));
			
			if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
				WarpDrive.logger.info(String.format("Big asteroid: %.3f %.3f r %.3f r makes (%3d %3d %3d)",
					(double) binomial, bearing, yawn, aX, aY, aZ));
			}
			
			// Place an asteroid
			AbstractStructure moon = StructureManager.getStructure(world.rand, StructureManager.GROUP_ASTEROIDS, null);
			moon.generate(world, world.rand, aX, aY, aZ);
		}
		
		// Setting up small asteroids
		for (int i = 1; i <= numOfSmallAsteroids; i++) {
			float binomial = binomialRandom(world);
			double bearing = world.rand.nextFloat() * 2.0D * Math.PI;
			double yawn = world.rand.nextFloat() * Math.PI;
			float horizontalRange = Math.max(6.0F, binomial * maxDistanceSmall);
			float verticalRange = Math.max(3.0F, binomial * maxHeight);
			
			int aX = (int) (x + Math.round(horizontalRange * Math.cos(bearing)));
			int aY = (int) (y2 + Math.round(verticalRange * Math.cos(yawn)));
			int aZ = (int) (z + Math.round(horizontalRange * Math.sin(bearing)));
			
			// Placing
			if (world.rand.nextInt(400) != 1) {
				AbstractStructure moon = StructureManager.getStructure(world.rand, StructureManager.GROUP_ASTEROIDS, null);
				moon.generate(world, world.rand, aX, aY, aZ);
			} else {
				if (world.rand.nextInt(20) != 1) {
					generateSmallShip(world, aX, aY, aZ, 8);
				} else {
					generateStation(world, aX, aY, aZ, 8);
				}
			}
		}
		
		// Setting up gas clouds
		for (int i = 1; i <= numOfClouds; i++) {
			float binomial = binomialRandom(world);
			double bearing = world.rand.nextFloat() * 2.0D * Math.PI;
			double yawn = world.rand.nextFloat() * Math.PI;
			float horizontalRange = Math.max(6.0F, binomial * maxDistanceBig);
			float verticalRange = Math.max(3.0F, binomial * maxHeight);
			
			int aX = (int) (x + Math.round(horizontalRange * Math.cos(bearing)));
			int aY = (int) (y2 + Math.round(verticalRange * Math.cos(yawn)));
			int aZ = (int) (z + Math.round(horizontalRange * Math.sin(bearing)));
			
			// Placing
			if (world.rand.nextBoolean()) {
				AbstractStructure gasCloud = StructureManager.getStructure(world.rand, StructureManager.GROUP_GAS_CLOUDS, null);
				if (gasCloud != null) {
					gasCloud.generate(world, world.rand, aX, aY, aZ);
				}
			}
		}
		
		LocalProfiler.stop();
		return true;
	}
	
	private static void generateSmallShip(final World world, final int x, final int y, final int z, final int jitter) {
		int x2 = x + (((world.rand.nextBoolean()) ? -1 : 1) * world.rand.nextInt(jitter));
		int y2 = y + (((world.rand.nextBoolean()) ? -1 : 1) * world.rand.nextInt(jitter));
		int z2 = z + (((world.rand.nextBoolean()) ? -1 : 1) * world.rand.nextInt(jitter));
		WarpDrive.logger.info("Generating small ship at " + x2 + " " + y2 + " " + z2);
		new WorldGenSmallShip(world.rand.nextFloat() > 0.2F, false).generate(world, world.rand, x2, y2, z2);
	}
	
	private static void generateStation(final World world, final int x, final int y, final int z, final int jitter) {
		int x2 = x + (((world.rand.nextBoolean()) ? -1 : 1) * world.rand.nextInt(jitter));
		int y2 = y + (((world.rand.nextBoolean()) ? -1 : 1) * world.rand.nextInt(jitter));
		int z2 = z + (((world.rand.nextBoolean()) ? -1 : 1) * world.rand.nextInt(jitter));
		WarpDrive.logger.info("Generating station at " + x2 + " " + y2 + " " + z2);
		new WorldGenStation(world.rand.nextBoolean()).generate(world, world.rand, x2, y2, z2);
	}
}
