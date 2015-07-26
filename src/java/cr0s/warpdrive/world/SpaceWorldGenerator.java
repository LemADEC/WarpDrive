package cr0s.warpdrive.world;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import cpw.mods.fml.common.IWorldGenerator;
import cr0s.warpdrive.LocalProfiler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.WarpDriveConfig;

/**
 * @author Cr0s
 */
public class SpaceWorldGenerator implements IWorldGenerator {
	// Radius of simple moon
	public final int MOON_RADIUS = 32;
	public final int MOON_CORE_RADIUS = 10;

	// Star radius
	public final int RED_DWARF_RADIUS = 42;
	public final int YELLOW_GIANT_RADIUS = 64;
	public final int YELLOW_SUPERGIANT_RADIUS = 80;

	// Upper than 200 nothing should generate naturally (safe place)
	public static int Y_LIMIT_HARD_MAX = 200;
	// Upper than 128 almost nothing will be generated
	public static int Y_LIMIT_SOFT_MAX = 128;
	// Lower limit
	public static int Y_LIMIT_SOFT_MIN = 55;

	/**
	 * Generator for chunk
	 *
	 * @param random
	 * @param chunkX
	 * @param chunkZ
	 * @param world
	 * @param chunkGenerator
	 * @param chunkProvider
	 */
	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
		if (world.provider.dimensionId != WarpDriveConfig.G_SPACE_DIMENSION_ID) {
			return;
		}
		int x = (chunkX * 16) + (5 - random.nextInt(10));
		int z = (chunkZ * 16) + (5 - random.nextInt(10));
		if (Math.abs(x) > WarpDriveConfig.G_SPACE_WORLDBORDER_BLOCKS || Math.abs(z) > WarpDriveConfig.G_SPACE_WORLDBORDER_BLOCKS) {
			return;
		}
		int y = Y_LIMIT_SOFT_MIN + random.nextInt(Y_LIMIT_SOFT_MAX - Y_LIMIT_SOFT_MIN);
		// Moon setup
		if (random.nextInt(700) == 1)
			generateMoon(world, x, y, z);
		// Simple asteroids
		else if (random.nextInt(150) == 1) {
			generateAsteroidOfBlock(world, x, y, z, 6, 11, null, 0);
			// Random asteroid of block
		} else if (random.nextInt(400) == 1) {
			generateRandomAsteroid(world, x, y, z, 6, 11);
			if (random.nextBoolean()) {
				generateGasCloudOfColor(world, x, y, z, 6, 11, random.nextInt(12));
			}
		} else if (random.nextInt(200) == 1) {// Ice asteroid
			generateAsteroidOfBlock(world, x, y, z, 6, 11, Blocks.ice, 0);
		} else if (random.nextInt(500) == 1) {// Asteroid field
			generateAsteroidField(world, x, y, z);
		} else if (random.nextInt(1400) == 1) {// Diamond asteroid
			generateAsteroidOfBlock(world, x, y, z, 3, 2, Blocks.diamond_ore, 0);
			// Diamond block core
			world.setBlock(x, y, z, Blocks.diamond_block, 0, 2);
			if (random.nextBoolean()) {
				generateGasCloudOfColor(world, x, y, z, 6, 11, random.nextInt(12));
			}
		} else if (WarpDriveConfig.isAppliedEnergisticsLoaded && random.nextInt(1600) == 1) {// Quartz
			// asteroid
			generateAsteroidOfBlock(world, x, y, z, 3, 2, Block.getBlockFromItem(WarpDriveConfig.getAEBlock("blkQuartzOre").getItem()), WarpDriveConfig
					.getAEBlock("blkQuartzOre").getItemDamage());
			if (random.nextBoolean()) {
				generateGasCloudOfColor(world, x, y, z, 4, 7, random.nextInt(12));
			}
		}
	}

	public static void generateMoon(World world, int x, int y, int z) {
		int coreRadius = 5 + world.rand.nextInt(12);
		int moonRadius = coreRadius + 5 + world.rand.nextInt(26);
		int y2 = Math.max(moonRadius + 6, Math.min(y, Y_LIMIT_HARD_MAX - moonRadius - 6));
		WarpDrive.print("Generating moon at " + x + " " + y2 + " " + z);

		Block block = WarpDriveConfig.getDefaultSurfaceBlock(world.rand, world.rand.nextInt(10) > 8, true);

		// Generate moon's core
		if (block.isAssociatedBlock(Blocks.netherrack)) {
			generateSphereDirect(world, x, y2, z, coreRadius, false, Blocks.lava, 0, false); // Lava
			// core
			world.setBlock(x, y2, z, Blocks.bedrock, 0, 0);
		} else if (!block.isAssociatedBlock(Blocks.end_stone)) {
			if (world.rand.nextInt(100) >= 10) {
				generateSphereDirect(world, x, y2, z, coreRadius, false, Blocks.lava, 0, false); // Lava
				// core
				world.setBlock(x, y2, z, Blocks.bedrock, 0, 0);
				generateSphereDirect(world, x, y2, z, coreRadius + 1, false, Blocks.obsidian, 0, true); // Obsidian
				// shell
			} else {
				coreRadius = Math.max(coreRadius, 7);
				generateSphereDirect(world, x, y2, z, coreRadius, false, Blocks.leaves, 0, false);
				world.setBlock(x, y2, z, Blocks.bedrock, 0, 0);
				generateSmallShip(world, x, y2, z, coreRadius - 6);
			}
		}

		/*
		 * // Place bedrock blocks world.setBlock(x + coreRadius , y2 , z ,
		 * Blocks.bedrock, 0, 0); world.setBlock(x - coreRadius , y2 , z ,
		 * Blocks.bedrock, 0, 0); world.setBlock(x , y2 , z + coreRadius ,
		 * Blocks.bedrock, 0, 0); world.setBlock(x , y2 , z - coreRadius ,
		 * Blocks.bedrock, 0, 0); world.setBlock(x , y2 + coreRadius , z ,
		 * Blocks.bedrock, 0, 0); world.setBlock(x , y2 - coreRadius , z ,
		 * Blocks.bedrock, 0, 0); world.setBlock(x + moonRadius / 2 , y2 , z ,
		 * Blocks.bedrock, 0, 0); world.setBlock(x - moonRadius / 2 , y2 , z ,
		 * Blocks.bedrock, 0, 0); world.setBlock(x , y2 , z + moonRadius / 2 ,
		 * Blocks.bedrock, 0, 0); world.setBlock(x , y2 , z - moonRadius / 2 ,
		 * Blocks.bedrock, 0, 0); world.setBlock(x , y2 + moonRadius / 2 , z ,
		 * Blocks.bedrock, 0, 0); world.setBlock(x , y2 - moonRadius / 2 , z ,
		 * Blocks.bedrock, 0, 0); world.setBlock(x + moonRadius - 10, y2 , z ,
		 * Blocks.bedrock, 0, 0); world.setBlock(x - moonRadius + 10, y2 , z ,
		 * Blocks.bedrock, 0, 0); world.setBlock(x , y2 , z + moonRadius - 10,
		 * Blocks.bedrock, 0, 0); world.setBlock(x , y2 , z - moonRadius + 10,
		 * Blocks.bedrock, 0, 0); world.setBlock(x , y2 + moonRadius - 10, z ,
		 * Blocks.bedrock, 0, 0); world.setBlock(x , y2 - moonRadius + 10, z ,
		 * Blocks.bedrock, 0, 0); /*
		 */

		// Generate moon's blocks
		generateSphereEntity(world, x, y2, z, moonRadius, false, block, 0, true);
		// Generate moon's atmosphere
		if (world.rand.nextBoolean()) {
			generateGasSphereEntity(world, x, y2, z, moonRadius + 2 + world.rand.nextInt(5), true, world.rand.nextInt(12));
		}
	}

	private static void placeStarCore(World world, int x, int y, int z, int radius) {
		EntityStarCore core = new EntityStarCore(world, x, y, z, radius);
		core.xCoord = x;
		core.yCoord = y;
		core.zCoord = z;
		core.setPosition(x, y, z);
		world.spawnEntityInWorld(core);
	}

	public void generateStar(World world, int x, int y, int z, Integer type) {
		Integer starClass = type == -1 ? world.rand.nextInt(3) : type;
		Integer y2 = y;
		WarpDrive.print("Generating star (class " + starClass + ") at " + x + " " + y + " " + z);

		switch (starClass) {
		case 0: // red dwarf
			y2 = Math.max(RED_DWARF_RADIUS + 6, Math.min(y, Y_LIMIT_HARD_MAX - RED_DWARF_RADIUS - 6));
			generateSphereEntity(world, x, y2, z, RED_DWARF_RADIUS, false, Blocks.redstone_block, 0, false);
			// Heliosphere of red gas
			generateGasSphereEntity(world, x, y2, z, RED_DWARF_RADIUS + 6, true, 1);
			placeStarCore(world, x, y2, z, RED_DWARF_RADIUS + 6);
			break;

		case 1:
			y2 = Math.max(YELLOW_GIANT_RADIUS + 6, Math.min(y, Y_LIMIT_HARD_MAX - YELLOW_GIANT_RADIUS - 6));
			generateSphereEntity(world, x, y2, z, YELLOW_GIANT_RADIUS, false, Blocks.glowstone, 0, false);
			// Heliosphere of yellow gas
			generateGasSphereEntity(world, x, y2, z, YELLOW_GIANT_RADIUS + 6, true, 3);
			placeStarCore(world, x, y2, z, YELLOW_GIANT_RADIUS + 6);
			break;

		case 2:
			y2 = Math.max(YELLOW_SUPERGIANT_RADIUS + 6, Math.min(y, Y_LIMIT_HARD_MAX - YELLOW_SUPERGIANT_RADIUS - 6));
			generateSphereEntity(world, x, y2, z, YELLOW_SUPERGIANT_RADIUS, false, Blocks.glowstone, 0, false);
			// Heliosphere of yellow gas
			generateGasSphereEntity(world, x, y2, z, YELLOW_SUPERGIANT_RADIUS + 6, true, 3);
			placeStarCore(world, x, y2, z, YELLOW_SUPERGIANT_RADIUS + 6);
			break;
		}
	}

	private static void generateSphereEntity(World world, int x, int y, int z, int radius, boolean hollow, Block block, int i, boolean generateOres) {
		EntitySphereGen esg = new EntitySphereGen(world, x, y, z, radius, block, i, hollow, true, generateOres);
		world.spawnEntityInWorld(esg);
	}

	private static void generateGasSphereEntity(World world, int x, int y, int z, int radius, boolean hollow, int color) {
		EntitySphereGen esg = new EntitySphereGen(world, x, y, z, radius, WarpDrive.gasBlock, color, hollow, true, false);
		world.spawnEntityInWorld(esg);
	}

	private static void generateSmallShip(World world, int x, int y, int z, int jitter) {
		int x2 = x + (((world.rand.nextBoolean()) ? -1 : 1) * world.rand.nextInt(jitter));
		int y2 = y + (((world.rand.nextBoolean()) ? -1 : 1) * world.rand.nextInt(jitter));
		int z2 = z + (((world.rand.nextBoolean()) ? -1 : 1) * world.rand.nextInt(jitter));
		WarpDrive.print("Generating small ship at " + x2 + "," + y2 + "," + z2);
		new WorldGenSmallShip(world.rand.nextBoolean()).generate(world, world.rand, x2, y2, z2);
	}

	private static void generateStation(World world, int x, int y, int z, int jitter) {
		int x2 = x + (((world.rand.nextBoolean()) ? -1 : 1) * world.rand.nextInt(jitter));
		int y2 = y + (((world.rand.nextBoolean()) ? -1 : 1) * world.rand.nextInt(jitter));
		int z2 = z + (((world.rand.nextBoolean()) ? -1 : 1) * world.rand.nextInt(jitter));
		WarpDrive.print("Generating small ship at " + x2 + "," + y2 + "," + z2);
		new WorldGenStation(world.rand.nextBoolean()).generate(world, world.rand, x2, y2, z2);
	}

	public static void generateRandomAsteroid(World world, int x, int y, int z, int asteroidSizeMax, int centerRadiusMax) {
		if (world.rand.nextInt(30) == 1) {
			Block t = Blocks.air;
			if (world.rand.nextInt(25) == 1) {
				while (t.isAssociatedBlock(Blocks.air)) {
					t = WarpDriveConfig.getRandomNetherBlock(world.rand, Blocks.air, 0);
				}
			} else if (world.rand.nextInt(35) == 1) {
				while (t.isAssociatedBlock(Blocks.air)) {
					t = WarpDriveConfig.getRandomEndBlock(world.rand, Blocks.air, 0);
				}
			} else {
				while (t.isAssociatedBlock(Blocks.air)) {
					t = WarpDriveConfig.getRandomOverworldBlock(world.rand, Blocks.air, 0);
				}
			}
			generateAsteroidOfBlock(world, x, y, z, Math.min(3, asteroidSizeMax), Math.min(2, centerRadiusMax), t, 0);
		} else {
			generateAsteroidOfBlock(world, x, y, z, asteroidSizeMax, centerRadiusMax, null, 0);
		}
	}

	private static float binomialRandom(World world) {
		float linear = world.rand.nextFloat();
		// ideal sphere repartition = x ^ 0.5 (sqrt)
		// Dilution but slow to compute = 0.5 * ( x ^ 0.3 + 1 + (x - 1) ^ 3 )
		// Optimized 'pushed out' form = 1.25 - 0.625 / (0.5 + 2 * x)
		// Natural sphere with ring = (1 - x ^ 2.5) * x ^ 0.5 + x ^ 4

		// rectangular approach: return 0.5F * linear + 0.5F * linear * linear;
		return 1.25F - 0.625F / (0.5F + 2.0F * linear);
	}

	public static void generateAsteroidField(World world, int x, int y1, int z) {
		LocalProfiler.start("SpaceWorldGenerator.generateAsteroidField");
		// 6.0.1 au = 120 radius with 60 to 140 big + 60 to 140 small + 5 to 13
		// gaz
		// 45238 blocks surface with 120 to 280 asteroids => 161 to 376 blocks
		// per asteroid (big & small)

		// 6.0.2 av big = 80 to 180 radius with 40 to 90 big + 80 to 200 small +
		// 5 to 13 gaz
		// 20106 to 101787 surface with 120 to 290 asteroids => 69 to 848 blocks
		// per asteroid

		// 6.0.2 av small = 30 to 80 radius with 2 to 22 big + 15 to 75 small +
		// 0 to 3 gaz
		// 2827 to 20106 surface with 17 to 97 asteroids => 29 to 1182 blocks
		// per asteroid

		// random distanced one = 89727 surface 256 asteroids => 350 blocks per
		// asteroid

		/*
		 * boolean isBig = world.rand.nextInt(3) == 1; int numOfBigAsteroids,
		 * numOfSmallAsteroids, numOfClouds, maxDistance, maxHeight; if (isBig)
		 * { numOfBigAsteroids = 40 + world.rand.nextInt(50);
		 * numOfSmallAsteroids = 80 + world.rand.nextInt(120); numOfClouds = 5 +
		 * world.rand.nextInt(8); maxDistance = 80 + world.rand.nextInt(100);
		 * maxHeight = 40 + world.rand.nextInt(40); } else { numOfBigAsteroids =
		 * 2 + world.rand.nextInt(20); numOfSmallAsteroids = 15 +
		 * world.rand.nextInt(60); numOfClouds = 0 + world.rand.nextInt(3);
		 * maxDistance = 30 + world.rand.nextInt(50); maxHeight = 30 +
		 * world.rand.nextInt(30); }/*
		 */

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
		int y2 = Math.min(Y_LIMIT_HARD_MAX - maxHeight, Math.max(y1, maxHeight));
		WarpDrive.print("Generating asteroid field at " + x + "," + y2 + "," + z + " qty " + numOfBigAsteroids + ", " + numOfSmallAsteroids + ", "
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
			/*
			 * System.out.println(String.format(
			 * "Big asteroid: %.3f %.3f r %.3f r makes %3d, %3d, %3d", new
			 * Object[] { Double.valueOf(binomial),Double.valueOf(bearing),
			 * Double.valueOf(yawn), Integer.valueOf(aX), Integer.valueOf(aY),
			 * Integer.valueOf(aZ)}));/*
			 */
			// Place an asteroid
			generateRandomAsteroid(world, aX, aY, aZ, 4, 6);
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
				generateRandomAsteroid(world, aX, aY, aZ, 3, 3);
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
				generateGasCloudOfColor(world, aX, aY, aZ, 12, 15, world.rand.nextInt(12));
			}
		}

		LocalProfiler.stop();
	}

	/**
	 * Gas cloud generator
	 *
	 * @param x
	 *            coordinate of center
	 * @param y
	 *            coordinate of center
	 * @param z
	 *            coordinate of center
	 * @param cloudSizeMax
	 *            maximum gas cloud size (by number of balls it consists)
	 * @param centerRadiusMax
	 *            maximum radius of central ball
	 */
	public static void generateGasCloudOfColor(World world, int x, int y, int z, int cloudSizeMax, int centerRadiusMax, int color) {
		int cloudSize = 1 + world.rand.nextInt(20);
		if (cloudSizeMax != 0)
			cloudSize = Math.min(cloudSizeMax, cloudSize);
		int centerRadius = 1 + world.rand.nextInt(20);
		if (centerRadiusMax != 0)
			centerRadius = Math.min(centerRadiusMax, centerRadius);
		final int CENTER_SHIFT = 2; // Offset from center of central ball
		// Asteroid's center
		generateGasSphereEntity(world, x, y, z, centerRadius, false, color);
		// Asteroids knolls
		for (int i = 1; i <= cloudSize; i++) {
			int radius = 2 + world.rand.nextInt(centerRadius);
			int newX = x + (((world.rand.nextBoolean()) ? -1 : 1) * world.rand.nextInt(CENTER_SHIFT + centerRadius / 2));
			int newY = y + (((world.rand.nextBoolean()) ? -1 : 1) * world.rand.nextInt(CENTER_SHIFT + centerRadius / 2));
			int newZ = z + (((world.rand.nextBoolean()) ? -1 : 1) * world.rand.nextInt(CENTER_SHIFT + centerRadius / 2));
			generateGasSphereEntity(world, newX, newY, newZ, radius, false, color);
		}
	}

	/**
	 * Asteroid of block generator
	 *
	 * @param x
	 *            coordinate of center
	 * @param y
	 *            coordinate of center
	 * @param z
	 *            coordinate of center
	 * @param asteroidSizeMax
	 *            maximum asteroid size (by number of balls it consists)
	 * @param centerRadiusMax
	 *            maximum radius of central ball
	 */
	private static void generateAsteroidOfBlock(World world, int x, int y, int z, int asteroidSizeMax, int centerRadiusMax, Block ice, int meta) {
		// FIXME: get a proper range of random instead of capping it
		int asteroidSize = 1 + world.rand.nextInt(6);
		if (asteroidSizeMax != 0) {
			asteroidSize = Math.min(asteroidSizeMax, asteroidSize);
		}
		int centerRadius = 1 + world.rand.nextInt(6);
		if (centerRadiusMax != 0) {
			centerRadius = Math.min(centerRadiusMax, centerRadius);
		}
		final int CENTER_SHIFT = 2; // Offset from center of central ball
		// Asteroid's center
		Block t = WarpDriveConfig.getDefaultSurfaceBlock(world.rand, true, false);
		generateSphereDirect(world, x, y, z, centerRadius, true, ice, meta, false, t, 0);
		// Asteroids knolls
		for (int i = 1; i <= asteroidSize; i++) {
			int radius = 1 + world.rand.nextInt(centerRadius);
			int newX = x + (((world.rand.nextBoolean()) ? -1 : 1) * world.rand.nextInt(CENTER_SHIFT + centerRadius / 2));
			int newY = y + (((world.rand.nextBoolean()) ? -1 : 1) * world.rand.nextInt(CENTER_SHIFT + centerRadius / 2));
			int newZ = z + (((world.rand.nextBoolean()) ? -1 : 1) * world.rand.nextInt(CENTER_SHIFT + centerRadius / 2));
			generateSphereDirect(world, newX, newY, newZ, radius, true, ice, meta, false, t, 0);
		}
	}

	/**
	 * Sphere generator
	 *
	 * @param world
	 *            target world
	 * @param xCoord
	 *            center
	 * @param yCoord
	 *            center
	 * @param zCoord
	 *            center
	 * @param radius
	 *            sphere radius
	 * @param corrupted
	 *            skip random blocks when generating (corrupted effect)
	 * @param block
	 *            sphere of specified blocks or random blocks if not specified
	 * @return
	 */
	public static void generateSphereDirect(World world, int xCoord, int yCoord, int zCoord, double radius, boolean corrupted, Block block, int meta,
			boolean hollow) {
		if (block == null) {
			generateSphereDirect(world, xCoord, yCoord, zCoord, radius, corrupted, block, meta, hollow,
					WarpDriveConfig.getDefaultSurfaceBlock(world.rand, corrupted, false), 0);
		} else {
			generateSphereDirect(world, xCoord, yCoord, zCoord, radius, corrupted, block, meta, hollow, block, meta);
		}
	}

	public static void generateSphereDirect(World world, int xCoord, int yCoord, int zCoord, double radius, boolean corrupted, Block ice, int meta,
			boolean hollow, Block t, int meta2) {
		double radiusC = radius + 0.5D; // Radius from center of block
		double radiusSq = radiusC * radiusC; // Optimization to avoid sqrts...
		double radius1Sq = (radiusC - 1.0D) * (radiusC - 1.0D); // for hollow
		// sphere
		int ceilRadius = (int) Math.ceil(radiusC);

		Block block = null;
		if (ice != null) {
			block = ice;
		}
		// Pass the cube and check points for sphere equation x^2 + y^2 + z^2 =
		// r^2
		for (int x = 0; x <= ceilRadius; x++) {
			double x2 = (x + 0.5D) * (x + 0.5D);
			for (int y = 0; y <= ceilRadius; y++) {
				double y2 = (y + 0.5D) * (y + 0.5D);
				for (int z = 0; z <= ceilRadius; z++) {
					double z2 = (z + 0.5D) * (z + 0.5D);
					double dSq = x2 + y2 + z2; // Distance from current position
					// to center

					// Skip too far blocks
					if (dSq > radiusSq)
						continue;

					// Hollow sphere condition
					if ((hollow)
							&& ((dSq < radius1Sq) || ((lengthSq(x + 1.5D, y + 0.5D, z + 0.5D) <= radiusSq)
									&& (lengthSq(x + 0.5D, y + 1.5D, z + 0.5D) <= radiusSq) && (lengthSq(x + 0.5D, y + 0.5D, z + 1.5D) <= radiusSq))))
						continue;

					// Place blocks
					if (!corrupted || world.rand.nextInt(5) != 1) {
						if (ice == null)
							block = WarpDriveConfig.getRandomSurfaceBlock(world.rand, t, meta2, false);
						world.setBlock(xCoord + x, yCoord + y, zCoord + z, block, 0, 2);
						world.setBlock(xCoord - x, yCoord + y, zCoord + z, block, 0, 2);
					}
					if (!corrupted || world.rand.nextInt(5) != 1) {
						if (ice == null)
							block = WarpDriveConfig.getRandomSurfaceBlock(world.rand, t, meta2, false);
						world.setBlock(xCoord + x, yCoord - y, zCoord + z, block, 0, 2);
						world.setBlock(xCoord + x, yCoord + y, zCoord - z, block, 0, 2);
					}
					if (!corrupted || world.rand.nextInt(5) != 1) {
						if (ice == null)
							block = WarpDriveConfig.getRandomSurfaceBlock(world.rand, t, meta2, false);
						world.setBlock(xCoord - x, yCoord - y, zCoord + z, block, 0, 2);
						world.setBlock(xCoord + x, yCoord - y, zCoord - z, block, 0, 2);
					}
					if (!corrupted || world.rand.nextInt(5) != 1) {
						if (ice == null)
							block = WarpDriveConfig.getRandomSurfaceBlock(world.rand, t, meta2, false);
						world.setBlock(xCoord - x, yCoord + y, zCoord - z, block, 0, 2);
						world.setBlock(xCoord - x, yCoord - y, zCoord - z, block, 0, 2);
					}
				}
			}
		}
	}

	private static double lengthSq(double x, double y, double z) {
		return (x * x) + (y * y) + (z * z);
	}
}
