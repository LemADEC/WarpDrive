package cr0s.warpdrive.world;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.render.RenderBlank;
import cr0s.warpdrive.render.RenderSpaceSky;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.provider.BiomeProviderType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationSettings;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.IRenderHandler;

public abstract class AbstractVoidDimension extends Dimension {
	
	protected final CelestialObject celestialObject;
	
	AbstractVoidDimension(@Nonnull final World world, @Nonnull final DimensionType dimensionType) {
		super(world, dimensionType, getCelestialObjectInConstructor(dimensionType).ambientBrightness);
		celestialObject = getCelestialObjectInConstructor(dimensionType);
	}
	
	// TODO 1.15/1.21: this resolves the celestial object ONCE, at dimension construction, with no player position
	// available, so it falls back to CelestialObjectManager's (deprecated) representative — an arbitrary object among
	// those sharing a space dimension (1:N model), which can be the wrong one. Investigate refreshing it dynamically
	// (e.g. ~1/second from the local player's X,Z when one exists) to resolve the exact per-position object via
	// get(dimensionId, x, z); would require celestialObject to become non-final + a tick hook. See BACKLOG "1.15".
	private static CelestialObject getCelestialObjectInConstructor(@Nonnull final DimensionType dimensionType) {
		// note: world is being constructed at that time, isRemote isn't set yet, so we try client first, then server
		final CelestialObject celestialObjectServer = CelestialObjectManager.get(false, dimensionType);
		final CelestialObject celestialObjectClient = CelestialObjectManager.get(true, dimensionType);
		return celestialObjectClient != null ? celestialObjectClient : celestialObjectServer;
	}
	
	@Override
	public boolean isDaytime() {
		return true;
	}
	
	@Nonnull
	@Override
	public SleepResult canSleepAt(final PlayerEntity player, @Nonnull final BlockPos blockPos) {
		return SleepResult.ALLOW;
	}
	
	@Override
	public DimensionType getRespawnDimension(@Nonnull final ServerPlayerEntity entityServerPlayer) {
		if (entityServerPlayer.world == null) {
			WarpDrive.logger.error(String.format("Invalid player passed to getRespawnDimension: %s",
			                                     entityServerPlayer));
			return DimensionType.getById(0);
		}
		
		return getType();
	}
	
	@Nonnull
	@Override
	public ChunkGenerator<?> createChunkGenerator() {
		final GenerationSettings generationSettings = new GenerationSettings();
		generationSettings.setDefaultBlock(Blocks.AIR.getDefaultState());
		generationSettings.setDefaultFluid(Blocks.AIR.getDefaultState());
		return new VoidChunkProvider(world,
		                             BiomeProviderType.FIXED.create(BiomeProviderType.FIXED.createSettings(world.getWorldInfo()).setBiome(WarpDrive.biomeSpace)),
		                             generationSettings );
	}
	
	@Nullable
	@Override
	public BlockPos findSpawn(@Nonnull final ChunkPos chunkPos, final boolean checkValid) {
		return null;
	}
	
	@Nullable
	@Override
	public BlockPos findSpawn(final int posX, final int posZ, final boolean checkValid) {
		return null;
	}
	
	@Override
	public boolean canDoLightning(@Nonnull final Chunk chunk) {
		return false;
	}
	
	@Override
	public boolean canDoRainSnowIce(@Nonnull final Chunk chunk) {
		return false;
	}
	
	@Override
	public void updateWeather(@Nonnull final Runnable defaultLogic) {
		super.resetRainAndThunder();
	}
	
	@OnlyIn(Dist.CLIENT)
	@Nullable
	@Override
	public IRenderHandler getCloudRenderer() {
		if (super.getCloudRenderer() == null) {
			setCloudRenderer(RenderBlank.getInstance());
		}
		return super.getCloudRenderer();
	}
	
	@Override
	public IRenderHandler getSkyRenderer() {
		if (super.getSkyRenderer() == null) {
			setSkyRenderer(RenderSpaceSky.getInstance());
		}
		return super.getSkyRenderer();
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public boolean doesXZShowFog(int x, int z) {
		return false; // disable distance based fog
	}
	
	@OnlyIn(Dist.CLIENT)
	public double getVoidFogYFactor() {
		return 0.0D; // disable Vanilla's void fog
	}
	
	@OnlyIn(Dist.CLIENT)
	@Nonnull
	@Override
	public Vec3d getFogColor(final float celestialAngle, final float par2) {
		final float factor = Commons.clamp(0.0F, 1.0F, MathHelper.cos(celestialAngle * (float) Math.PI * 2.0F) * 2.0F + 0.5F);
		
		float red   = celestialObject == null ? 0.0F : celestialObject.colorFog.red;
		float green = celestialObject == null ? 0.0F : celestialObject.colorFog.green;
		float blue  = celestialObject == null ? 0.0F : celestialObject.colorFog.blue;
		final float factorRed   = celestialObject == null ? 0.0F : celestialObject.factorFog.red;
		final float factorGreen = celestialObject == null ? 0.0F : celestialObject.factorFog.green;
		final float factorBlue  = celestialObject == null ? 0.0F : celestialObject.factorFog.blue;
		red   *= factor * factorRed   + (1.0F - factorRed  );
		green *= factor * factorGreen + (1.0F - factorGreen);
		blue  *= factor * factorBlue  + (1.0F - factorBlue );
		return new Vec3d(red, green, blue);
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public float getLightBrightness(final int lightLevel) {
		if (celestialObject == null) {
			return 0.0F;
		}
		final float starBrightnessVanilla = super.getLightBrightness(lightLevel);
		return celestialObject.baseStarBrightness + celestialObject.vanillaStarBrightness * starBrightnessVanilla;
	}
}