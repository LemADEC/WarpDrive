package cr0s.warpdrive.render;

import cr0s.warpdrive.BreathingManager;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.CelestialObject;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;

@SideOnly(Side.CLIENT)
public class RenderOverlayAir {
	
	private static final int WARNING_ON_JOIN_TICKS = 20 * 20;
	
	private static Minecraft minecraft = Minecraft.getMinecraft();
	
	private static float ratioPreviousAir = 1.0F;
	private static long timePreviousAir = 0;
	
	private void renderAir(final int width, final int height) {
		// get player
		EntityPlayer entityPlayer = minecraft.thePlayer;
		if (entityPlayer == null) {
			return;
		}
		final int x = MathHelper.floor_double(entityPlayer.posX);
		final int y = MathHelper.floor_double(entityPlayer.posY);
		final int z = MathHelper.floor_double(entityPlayer.posZ);
		
		// get celestial object
		final CelestialObject celestialObject = CelestialObjectManager.get(entityPlayer.worldObj, x, z);
		if (celestialObject == null || celestialObject.hasAtmosphere()) {// skip (no display) if environment is breathable
			return;
		}
		
		// get air stats
		final boolean hasVoidNearby = isVoid(entityPlayer.worldObj, x, y, z)
		                           || isVoid(entityPlayer.worldObj, x - 2, y, z)
		                           || isVoid(entityPlayer.worldObj, x + 2, y, z)
		                           || isVoid(entityPlayer.worldObj, x, y, z - 2)
		                           || isVoid(entityPlayer.worldObj, x, y, z + 2);
		final boolean hasValidSetup = BreathingManager.hasValidSetup(entityPlayer);
		final float ratioAirReserve = BreathingManager.getAirReserveRatio(entityPlayer);
		
		// start rendering
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		
		// show splash message
		int alpha = 255;
		if (hasVoidNearby || entityPlayer.ticksExisted < WARNING_ON_JOIN_TICKS) {
			if (!hasValidSetup) {
				alpha = RenderCommons.drawSplashAlarm(width, height, "warpdrive.breathing.alarm", "warpdrive.breathing.invalid_setup");
			} else if (ratioAirReserve <= 0.0F) {
				alpha = RenderCommons.drawSplashAlarm(width, height, "warpdrive.breathing.alarm", "warpdrive.breathing.no_air");
			} else if (ratioAirReserve < 0.15F) {
				alpha = RenderCommons.drawSplashAlarm(width, height, "warpdrive.breathing.alarm", "warpdrive.breathing.low_reserve");
			}
		}
		
		// restore texture
		minecraft.getTextureManager().bindTexture(Gui.icons);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		
		// position right above food bar
		final int left = width / 2 + 91;
		final int top = height - GuiIngameForge.right_height;
		
		// draw animated air bubble
		final long timeWorld =  entityPlayer.worldObj.getTotalWorldTime();
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
		final int full = MathHelper.ceiling_double_int(ratioAirReserve * 71.0D);
		RenderCommons.drawTexturedModalRect(left - 81, top + 2, 20, 84, 71, 5, 100);
		if (alpha != 255) {
			final float factor = 1.0F - alpha / 255.0F;
			GL11.glColor4f(1.0F, 0.2F + 0.8F * factor, 0.2F + 0.8F * factor, 1.0F);
		}
		RenderCommons.drawTexturedModalRect(left - 10 - full, top + 2, 91 - full, 89, full, 5, 100);
		
		// close rendering
		GuiIngameForge.right_height += 10;
		
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	private boolean isVoid(final IBlockAccess blockAccess, final int x, final int y, final int z) {
		final Block block = blockAccess.getBlock(x, y, z);
		return block.isAir(blockAccess, x, y, z) && !BreathingManager.isAirBlock(block);
	}
	
	@SubscribeEvent
	public void onRender(RenderGameOverlayEvent.Pre event) {
		if (event.type == ElementType.AIR) {
			renderAir(event.resolution.getScaledWidth(), event.resolution.getScaledHeight());
		}
	}
}