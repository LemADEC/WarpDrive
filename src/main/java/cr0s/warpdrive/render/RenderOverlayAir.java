package cr0s.warpdrive.render;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.BreathingManager;
import cr0s.warpdrive.api.ExceptionChunkNotLoaded;
import cr0s.warpdrive.block.breathing.BlockAirShield;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.StateAir;

import javax.annotation.Nonnull;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.systems.RenderSystem;

@OnlyIn(Dist.CLIENT)
public class RenderOverlayAir {
	
	private static final int WARNING_ON_JOIN_TICKS = 20 * 20;
	
	private static final Minecraft minecraft = Minecraft.getInstance();
	
	private static float ratioPreviousAir = 1.0F;
	private static long timePreviousAir = 0;
	
	private boolean wasRendered;
	
	private void renderAir(final int width, final int height) {
		// get player
		final PlayerEntity entityPlayer = minecraft.player;
		if (entityPlayer == null) {
			return;
		}
		if ( entityPlayer.isCreative()
		  || entityPlayer.isSpectator() ) {
			return;
		}
		final int x = MathHelper.floor(entityPlayer.getPosX());
		final int y = MathHelper.floor(entityPlayer.getPosY());
		final int z = MathHelper.floor(entityPlayer.getPosZ());
		
		// get celestial object
		final CelestialObject celestialObject = CelestialObjectManager.get(entityPlayer.world, x, z);
		if (celestialObject == null || celestialObject.hasAtmosphere()) {// skip (no display) if environment is breathable
			return;
		}
		
		// bypass for androids
		/* TODO MC1.15 Compatibility classes
		if ( WarpDriveConfig.isMatterOverdriveLoaded
		  && CompatMatterOverdrive.isAndroid(entityPlayer) ) {
			return;
		}
		*/
		
		// get air stats
		final int rangeToVoid = getRangeToVoid(entityPlayer, x, y, z);
		final boolean hasValidSetup = BreathingManager.hasValidSetup(entityPlayer);
		final float ratioAirReserve = BreathingManager.getAirReserveRatio(entityPlayer);
		
		// start rendering
		RenderSystem.enableBlend();
		
		// show splash message
		int alpha = 255;
		if ( rangeToVoid >= 0
		  || entityPlayer.ticksExisted < WARNING_ON_JOIN_TICKS ) {
			if (!hasValidSetup) {
				alpha = RenderCommons.drawSplashAlarm(width, height, "warpdrive.breathing.alarm", "warpdrive.breathing.invalid_setup");
			} else if (ratioAirReserve <= 0.0F) {
				alpha = RenderCommons.drawSplashAlarm(width, height, "warpdrive.breathing.alarm", "warpdrive.breathing.no_air");
			} else if (ratioAirReserve < 0.15F) {
				alpha = RenderCommons.drawSplashAlarm(width, height, "warpdrive.breathing.alarm", "warpdrive.breathing.low_reserve");
			}
		}
		
		// restore texture
		minecraft.getTextureManager().bindTexture(AbstractGui.GUI_ICONS_LOCATION);
		
		// position right above food bar
		final int left = width / 2 + 91;
		final int top = height - ForgeIngameGui.right_height;
		
		// draw animated air bubble
		final long timeWorld =  entityPlayer.world.getGameTime();
		if (ratioAirReserve != ratioPreviousAir) {
			timePreviousAir = timeWorld;
			ratioPreviousAir = ratioAirReserve;
		}
		final long timeDelta = timeWorld - timePreviousAir;
		if (timeDelta >= 0 && timeDelta <= 8) {
			RenderCommons.drawTexturedModalRect(left - 9, top, 25, 18, 9, 9, 100);
		} else if (timeDelta < 0 || timeDelta > 16) {
			RenderCommons.drawTexturedModalRect(left - 9, top, 16, 18, 9, 9, 100);
		}
		
		// draw air level bar
		final int full = MathHelper.ceil(ratioAirReserve * 71.0D);
		RenderCommons.drawTexturedModalRect(left - 81, top + 2, 20, 84, 71, 5, 100);
		final float fRed  ;
		final float fGreen;
		final float fBlue ;
		final float fAlpha;
		if (alpha == 255) {
			fRed   = 1.0F;
			fGreen = 1.0F;
			fBlue  = 1.0F;
			fAlpha = 1.0F;
		} else {
			final float factor = 1.0F - alpha / 255.0F;
			fRed   = 1.0F;
			fGreen = 0.2F + 0.8F * factor;
			fBlue  = 0.2F + 0.8F * factor;
			fAlpha = 1.0F;
		}
		RenderCommons.drawTexturedModalRect(left - 10 - full, top + 2, 91 - full, 89, full, 5, 100, fRed, fGreen, fBlue, fAlpha);
		
		// close rendering
		ForgeIngameGui.right_height += 10;
		
		RenderSystem.disableBlend();
	}
	
	private int cache_rangeToVoid = -1;
	private int cache_ticksVoidCheck = -1;
	private int getRangeToVoid(@Nonnull final LivingEntity entityLivingBase, final int x, final int y, final int z) {
		if (entityLivingBase.ticksExisted == cache_ticksVoidCheck) {
			return cache_rangeToVoid;
		}
		cache_rangeToVoid  = getRangeToVoid_noCache(entityLivingBase.world, x, y, z);
		cache_ticksVoidCheck = entityLivingBase.ticksExisted;
		return cache_rangeToVoid;
	}
	private int getRangeToVoid_noCache(@Nonnull final World world, final int x, final int y, final int z) {
		final BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable(x, y, z);
		final BlockState blockStateSelf = world.getBlockState(mutableBlockPos);
		if (isVoid(blockStateSelf, world, mutableBlockPos)) {
			return 0;
		}
		for (final Direction enumFacing : Commons.FACINGS_HORIZONTAL) {
			// check the block directly next
			mutableBlockPos.setPos(x + enumFacing.getXOffset(),
			                       y + enumFacing.getYOffset(),
			                       z + enumFacing.getZOffset() );
			final BlockState blockStateClose = world.getBlockState(mutableBlockPos);
			if (isVoid(blockStateClose, world, mutableBlockPos)) {
				return 1;
			}
			final StateAir stateAirClose = new StateAir(null);
			try {
				stateAirClose.refresh(world, mutableBlockPos.getX(), mutableBlockPos.getY(), mutableBlockPos.getZ());
			} catch (final ExceptionChunkNotLoaded exceptionChunkNotLoaded) {
				return -1;
			}
			
			// skip if we're sealed, not in an air lock
			final boolean isNonAirShieldSealer = !stateAirClose.isAir()
			                                  && !(blockStateClose.getBlock() instanceof BlockAirShield);
			if (isNonAirShieldSealer) {
				continue;
			}
			
			// check 1 more block away
			mutableBlockPos.setPos(x + 2 * enumFacing.getXOffset(),
			                       y + 2 * enumFacing.getYOffset(),
			                       z + 2 * enumFacing.getZOffset() );
			final BlockState blockStateFar = world.getBlockState(mutableBlockPos);
			if (isVoid(blockStateFar, world, mutableBlockPos)) {
				return 2;
			}
		}
		return -1;
	}
	
	private boolean isVoid(@Nonnull final BlockState blockState, @Nonnull final IWorldReader worldReader, final BlockPos blockPos) {
		return blockState.getBlock().isAir(blockState, worldReader, blockPos)
		    && !BreathingManager.isAirBlock(blockState.getBlock());
	}
	
	@SubscribeEvent
	public void onRender(@Nonnull final RenderGameOverlayEvent.Pre event) {
		switch (event.getType()) {
		case ALL:
			wasRendered = false;
			break;
		case AIR:
			renderAir(event.getWindow().getScaledWidth(), event.getWindow().getScaledHeight());
			wasRendered = true;
			break;
		case CHAT:
			if ( !wasRendered
			  && WarpDriveConfig.CLIENT_BREATHING_OVERLAY_FORCED ) {
				renderAir(event.getWindow().getScaledWidth(), event.getWindow().getScaledHeight());
				wasRendered = true;
			}
			break;
		}
	}
	
	@SubscribeEvent
	public void onRender(@Nonnull final RenderGameOverlayEvent.Post event) {
		switch (event.getType()) {
		case ALL:
			if ( !wasRendered
			  && WarpDriveConfig.CLIENT_BREATHING_OVERLAY_FORCED ) {
				renderAir(event.getWindow().getScaledWidth(), event.getWindow().getScaledHeight());
			}
			break;
		}
	}
}