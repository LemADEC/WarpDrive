package cr0s.warpdrive.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManagerHell;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cr0s.warpdrive.WarpDrive;

public class SpaceProvider extends WorldProvider {
	public int exitXCoord;
	public int exitYCoord;
	public int exitZCoord;
	public int exitDimID;

	public SpaceProvider() {
		this.worldChunkMgr = new WorldChunkManagerHell(WarpDrive.spaceBiome, 0.0F);
		this.hasNoSky = false;
	}

	@Override
	public String getDimensionName() {
		return "Space";
	}

	@Override
	public boolean canRespawnHere() {
		return false;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public float getStarBrightness(float par1) {
		return 1.0F;
	}

	@Override
	public boolean isSurfaceWorld() {
		return true;
	}

	@Override
	public int getAverageGroundLevel() {
		return 1;
	}

	@Override
	public double getHorizon() {
		return 1;
	}

	@Override
	public void updateWeather() {
		this.worldObj.getWorldInfo().setRainTime(0);
		this.worldObj.getWorldInfo().setRaining(false);
		this.worldObj.getWorldInfo().setThunderTime(0);
		this.worldObj.getWorldInfo().setThundering(false);
	}

	@Override
	public BiomeGenBase getBiomeGenForCoords(int x, int z) {
		return WarpDrive.spaceBiome;
	}

	@Override
	public void setAllowedSpawnTypes(boolean allowHostile, boolean allowPeaceful) {
		super.setAllowedSpawnTypes(true, true);
	}

	@Override
	public float calculateCelestialAngle(long par1, float par3) {
		return 0F;
	}

	@Override
	protected void generateLightBrightnessTable() {
		/*
		 * float var1 = 0.1F;
		 * 
		 * for (int var2 = 0; var2 <= 15; ++var2) { float var3 = 1.0F -
		 * (float)var2 / 15.0F; this.lightBrightnessTable[var2] = (1.0F - var3)
		 * / (var3 * 3.0F + 1.0F) * (1.0F - var1) + var1; }
		 */
		float var1 = 0.0F;

		for (int var2 = 0; var2 <= 15; ++var2) {
			float var3 = 1.0F - var2 / 15.0F;
			this.lightBrightnessTable[var2] = (1.0F - var3) / (var3 * 3.0F + 1.0F) * (1.0F - var1) + var1;
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public String getSaveFolder() {
		return (dimensionId == 0 ? null : "WarpDriveSpace" + dimensionId);
	}

	/*
	 * @Override public boolean canCoordinateBeSpawn(int par1, int par2) { int
	 * var3 = this.worldObj.getFirstUncoveredBlock(par1, par2); return var3 !=
	 * 0; }
	 */

	@Override
	public Vec3 getSkyColor(Entity cameraEntity, float partialTicks) {
		setCloudRenderer(new CloudRenderBlank());
		// setSkyRenderer(new SpaceSkyRenderer());
		return Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
	}

	@Override
	public Vec3 getFogColor(float par1, float par2) {
		return Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
	}

	@Override
	public boolean isSkyColored() {
		return false;
	}

	@Override
	public ChunkCoordinates getEntrancePortalLocation() {
		return null;
	}

	@Override
	public int getRespawnDimension(EntityPlayerMP player) {
		return 0; // respawn on Earth
	}

	@Override
	public IChunkProvider createChunkGenerator() {
		return new SpaceGenerator(worldObj, 45);
	}

	@Override
	public boolean canBlockFreeze(int x, int y, int z, boolean byWater) {
		return false;
	}

	/*
	 * @Override public ChunkCoordinates getRandomizedSpawnPoint() {
	 * ChunkCoordinates var5 = new
	 * ChunkCoordinates(this.worldObj.getSpawnPoint());
	 * 
	 * //boolean isAdventure = worldObj.getWorldInfo().getGameType() ==
	 * EnumGameType.ADVENTURE; int spawnFuzz = 1000; int spawnFuzzHalf =
	 * spawnFuzz / 2;
	 * 
	 * { var5.posX += this.worldObj.rand.nextInt(spawnFuzz) - spawnFuzzHalf;
	 * var5.posZ += this.worldObj.rand.nextInt(spawnFuzz) - spawnFuzzHalf;
	 * var5.posY = 200; }
	 * 
	 * if (worldObj.isAirBlock(var5.posX, var5.posY, var5.posZ)) {
	 * worldObj.setBlock(var5.posX, var5.posY, var5.posZ, Block.stone.blockID,
	 * 0, 2);
	 * 
	 * worldObj.setBlock(var5.posX + 1, var5.posY + 1, var5.posZ,
	 * Block.glass.blockID, 0, 2); worldObj.setBlock(var5.posX + 1, var5.posY +
	 * 2, var5.posZ, Block.glass.blockID, 0, 2);
	 * 
	 * worldObj.setBlock(var5.posX - 1, var5.posY + 1, var5.posZ,
	 * Block.glass.blockID, 0, 2); worldObj.setBlock(var5.posX - 1, var5.posY +
	 * 2, var5.posZ, Block.glass.blockID, 0, 2);
	 * 
	 * worldObj.setBlock(var5.posX, var5.posY + 1, var5.posZ + 1,
	 * Block.glass.blockID, 0, 2); worldObj.setBlock(var5.posX, var5.posY + 2,
	 * var5.posZ + 1, Block.glass.blockID, 0, 2);
	 * 
	 * worldObj.setBlock(var5.posX, var5.posY + 1, var5.posZ - 1,
	 * Block.glass.blockID, 0, 2); worldObj.setBlock(var5.posX, var5.posY + 3,
	 * var5.posZ - 1, Block.glass.blockID, 0, 2);
	 * 
	 * //worldObj.setBlockWithNotify(var5.posX, var5.posY + 3, var5.posZ,
	 * Block.glass.blockID); } return var5; }
	 */
	@Override
	public boolean getWorldHasVoidParticles() {
		return false;
	}

	@Override
	public boolean isDaytime() {
		return true;
	}

	@Override
	public boolean canDoLightning(Chunk chunk) {
		return false;
	}

	@Override
	public boolean canDoRainSnowIce(Chunk chunk) {
		return false;
	}
}