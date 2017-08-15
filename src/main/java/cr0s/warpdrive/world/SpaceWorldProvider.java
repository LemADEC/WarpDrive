package cr0s.warpdrive.world;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.client.ClientProxy;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.StarMapRegistry;
import cr0s.warpdrive.render.RenderBlank;
import cr0s.warpdrive.render.RenderSpaceSky;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManagerHell;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class SpaceWorldProvider extends WorldProvider {
	
	private CelestialObject celestialObjectDimension = null;
	
	public SpaceWorldProvider() {
		worldChunkMgr = new WorldChunkManagerHell(WarpDrive.spaceBiome, 0.0F);
		hasNoSky = false;
	}
	
	@Override
	public void setDimension(final int dimensionId) {
		super.setDimension(dimensionId);
		celestialObjectDimension = CelestialObjectManager.get(WarpDrive.proxy instanceof ClientProxy, dimensionId, 0, 0);
	}
	
	@Override
	public String getSaveFolder() {
		return celestialObjectDimension == null ? "WarpDriveSpace" + dimensionId : celestialObjectDimension.id;
	}
	
	@Override
	public String getDimensionName() {
		return celestialObjectDimension == null ? "Space" + dimensionId : celestialObjectDimension.id;
	}
	
	@Override
	public boolean canRespawnHere() {
		return true;
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
		return -256;
	}
	
	@Override
	public void updateWeather() {
		super.resetRainAndThunder();
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
	public float calculateCelestialAngle(long time, float partialTick) {
		return 0.0F;
	}
	
	@Override
	protected void generateLightBrightnessTable() {
		float f = 0.0F;	// 0.1F
		
		for (int i = 0; i <= 15; ++i) {
			float f1 = 1.0F - i / 15.0F;
			lightBrightnessTable[i] = (1.0F - f1) / (f1 * 3.0F + 1.0F) * (1.0F - f) + f;
		}
	}
	
	@Override
	public boolean canCoordinateBeSpawn(int x, int z) {
		int y = worldObj.getTopSolidOrLiquidBlock(x, z);
		return y != 0;
	}
	
	// shared for getFogColor(), getStarBrightness()
	// @SideOnly(Side.CLIENT)
	private static CelestialObject celestialObject = null;
	
	@SideOnly(Side.CLIENT)
	@Override
	public Vec3 getSkyColor(Entity cameraEntity, float partialTicks) {
		if (getCloudRenderer() == null) {
			setCloudRenderer(RenderBlank.getInstance());
		}
		if (getSkyRenderer() == null) {
			setSkyRenderer(RenderSpaceSky.getInstance());
		}
		
		celestialObject = cameraEntity.worldObj == null ? null : CelestialObjectManager.get(
				cameraEntity.worldObj,
				MathHelper.floor_double(cameraEntity.posX), MathHelper.floor_double(cameraEntity.posZ));
		if (celestialObject == null) {
			return Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
		} else {
			return Vec3.createVectorHelper(celestialObject.backgroundColor.red, celestialObject.backgroundColor.green, celestialObject.backgroundColor.blue);
		}
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public Vec3 getFogColor(float celestialAngle, float par2) {
		final float factor = Commons.clamp(0.0F, 1.0F, MathHelper.cos(celestialAngle * (float) Math.PI * 2.0F) * 2.0F + 0.5F);
		
		float red   = celestialObject == null ? 0.0F : celestialObject.colorFog.red;
		float green = celestialObject == null ? 0.0F : celestialObject.colorFog.green;
		float blue  = celestialObject == null ? 0.0F : celestialObject.colorFog.blue;
		float factorRed   = celestialObject == null ? 0.0F : celestialObject.factorFog.red;
		float factorGreen = celestialObject == null ? 0.0F : celestialObject.factorFog.green;
		float factorBlue  = celestialObject == null ? 0.0F : celestialObject.factorFog.blue;
		red   *= factor * factorRed   + (1.0F - factorRed  );
		green *= factor * factorGreen + (1.0F - factorGreen);
		blue  *= factor * factorBlue  + (1.0F - factorBlue );
		return Vec3.createVectorHelper(red, green, blue);
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public float getStarBrightness(float partialTicks) {
		if (celestialObject == null) {
			return 0.0F;
		}
		final float starBrightnessVanilla = super.getStarBrightness(partialTicks);
		return celestialObject.baseStarBrightness + celestialObject.vanillaStarBrightness * starBrightnessVanilla;
	}
	
	@SideOnly(Side.CLIENT)
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
		if (player == null || player.worldObj == null) {
			WarpDrive.logger.error("Invalid player passed to getRespawnDimension: " + player);
			return 0;
		}
		return StarMapRegistry.getSpaceDimensionId(player.worldObj, (int) player.posX, (int) player.posZ);
	}
	
	@Override
	public IChunkProvider createChunkGenerator() {
		return new SpaceChunkProvider(worldObj, 45);
	}
	
	@Override
	public boolean canBlockFreeze(int x, int y, int z, boolean byWater) {
		return false;
	}
	
	/*
	@Override
	public ChunkCoordinates getRandomizedSpawnPoint() {
		ChunkCoordinates var5 = new ChunkCoordinates(worldObj.getSpawnPoint());
		
		//boolean isAdventure = worldObj.getWorldInfo().getGameType() == EnumGameType.ADVENTURE;
		int spawnFuzz = 1000;
		int spawnFuzzHalf = spawnFuzz / 2;
		
		{
			var5.posX += worldObj.rand.nextInt(spawnFuzz) - spawnFuzzHalf;
			var5.posZ += worldObj.rand.nextInt(spawnFuzz) - spawnFuzzHalf;
			var5.posY = 200;
		}
		
		if (worldObj.isAirBlock(var5.posX, var5.posY, var5.posZ)) {
			worldObj.setBlock(var5.posX, var5.posY, var5.posZ, Blocks.stone, 0, 2);
			
			worldObj.setBlock(var5.posX + 1, var5.posY + 1, var5.posZ, Blocks.glass, 0, 2);
			worldObj.setBlock(var5.posX + 1, var5.posY + 2, var5.posZ, Blocks.glass, 0, 2);
			
			worldObj.setBlock(var5.posX - 1, var5.posY + 1, var5.posZ, Blocks.glass, 0, 2);
			worldObj.setBlock(var5.posX - 1, var5.posY + 2, var5.posZ, Blocks.glass, 0, 2);
			
			worldObj.setBlock(var5.posX, var5.posY + 1, var5.posZ + 1, Blocks.glass, 0, 2);
			worldObj.setBlock(var5.posX, var5.posY + 2, var5.posZ + 1, Blocks.glass, 0, 2);
			
			worldObj.setBlock(var5.posX, var5.posY + 1, var5.posZ - 1, Blocks.glass, 0, 2);
			worldObj.setBlock(var5.posX, var5.posY + 3, var5.posZ - 1, Blocks.glass, 0, 2);
			
			// worldObj.setBlockWithNotify(var5.posX, var5.posY + 3, var5.posZ, Block.glass.blockID);
		}
		return var5;
	}
	/**/
	
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