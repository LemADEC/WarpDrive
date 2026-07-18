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
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
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
	
	// Resolved initially at construction at the dimension origin, for server side usage (getLightBrightness API is position-less);
	// Resolved periodically on client side based on player's actual position
	protected CelestialObject celestialObject;

	AbstractVoidDimension(@Nonnull final World world, @Nonnull final DimensionType dimensionType) {
		super(world, dimensionType, ambientBrightnessOf(getBaselineCelestialObject(dimensionType)));
		celestialObject = getBaselineCelestialObject(dimensionType);
	}

	// Coordinate-based baseline resolved at the dimension origin (0,0) so it's deterministic (the object whose border contains
	// the origin, else the nearest), non-null whenever the dimension holds any object.
	// Called during construction, where world.isRemote() isn't reliable yet, so we probe the client registry then the server one.
	@Nullable
	private static CelestialObject getBaselineCelestialObject(@Nonnull final DimensionType dimensionType) {
		final ResourceLocation dimensionId = dimensionType.getRegistryName();
		final CelestialObject celestialObjectClient = CelestialObjectManager.get(true, dimensionId, 0, 0);
		return celestialObjectClient != null ? celestialObjectClient : CelestialObjectManager.get(false, dimensionId, 0, 0);
	}

	private static float ambientBrightnessOf(@Nullable final CelestialObject celestialObject) {
		return celestialObject == null ? 0.0F : celestialObject.ambientBrightness;
	}

	// Re-resolves celestialObject from the local player's exact position and rebuilds the (cached) vanilla light table
	// from its ambientBrightness (used by getLightBrightness/getFogColor/stars). No-op while the object is unchanged.
	@OnlyIn(Dist.CLIENT)
	public void refreshFromLocalPlayer() {
		final PlayerEntity entityPlayer = Minecraft.getInstance().player;
		if ( entityPlayer == null
		  || entityPlayer.world != world ) {
			return;
		}
		final CelestialObject celestialObjectNew = CelestialObjectManager.get(world, (int) entityPlayer.getPosX(), (int) entityPlayer.getPosZ());
		if (celestialObjectNew == celestialObject) {
			return;
		}
		celestialObject = celestialObjectNew;
		final float ambientBrightness = ambientBrightnessOf(celestialObjectNew);
		for (int lightLevel = 0; lightLevel <= 15; lightLevel++) {
			final float ratio   = (float) lightLevel / 15.0F;
			final float vanilla = ratio / (4.0F - 3.0F * ratio);
			lightBrightnessTable[lightLevel] = MathHelper.lerp(ambientBrightness, vanilla, 1.0F);
		}
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
	
	// getLightBrightness used parent, using the cached lightBrightnessTable (0 -> ambientBrightness, 15 -> 1.0).
}