package cr0s.warpdrive.render;

import cr0s.warpdrive.data.Vector3;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class EntityFXBeam extends EntityFX {
    
    private static final int ROTATION_SPEED = 20;
    private static final float END_MODIFIER = 1.0F;
    private static final ResourceLocation TEXTURE = new ResourceLocation("warpdrive", "textures/blocks/energy_grey.png");;
    
    private float length = 0.0F;
    private float rotYaw = 0.0F;
    private float rotPitch = 0.0F;
    private float prevYaw = 0.0F;
    private float prevPitch = 0.0F;
    private float prevSize = 0.0F;
    
    public EntityFXBeam(World par1World, Vector3 position, Vector3 target, float red, float green, float blue, int age, int energy) {
        super(par1World, position.x, position.y, position.z, 0.0D, 0.0D, 0.0D);
        this.setRBGColorF(red, green, blue);
        this.setSize(0.02F, 0.02F);
        this.noClip = true;
        this.motionX = 0.0D;
        this.motionY = 0.0D;
        this.motionZ = 0.0D;
        
        float xd = (float)(this.posX - target.x);
        float yd = (float)(this.posY - target.y);
        float zd = (float)(this.posZ - target.z);
        this.length = (float) new Vector3(this).distanceTo(target);
        final double lengthXZ = MathHelper.sqrt_double(xd * xd + zd * zd);
        this.rotYaw = (float) (Math.atan2(xd, zd) * 180.0D / Math.PI);
        this.rotPitch = (float) (Math.atan2(yd, lengthXZ) * 180.0D / Math.PI);
        this.prevYaw = this.rotYaw;
        this.prevPitch = this.rotPitch;
        this.particleMaxAge = age;
        
        // kill the particle if it's too far away
        EntityLivingBase entityRender = Minecraft.getMinecraft().renderViewEntity;
        int visibleDistance = 300;
        
        if (!Minecraft.getMinecraft().gameSettings.fancyGraphics) {
            visibleDistance = 100;
        }
        
        if (entityRender.getDistance(posX, posY, posZ) > visibleDistance) {
            particleMaxAge = 0;
        }
    }
    
    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        prevYaw = rotYaw;
        prevPitch = rotPitch;
        
        if (particleAge++ >= particleMaxAge) {
            setDead();
        }
    }
    
    @Override
    public void renderParticle(final Tessellator tessellator, final float partialTick, final float f1, final float f2, final float f3, final float f4, final float f5) {
        tessellator.draw();
        GL11.glPushMatrix();
        
        final float rot = worldObj.provider.getWorldTime() % (360 / ROTATION_SPEED) * ROTATION_SPEED + ROTATION_SPEED * partialTick;
        
        final float sizeTarget = Math.min(particleAge / 4.0F, 1.0F);
        final float size = prevSize + (sizeTarget - prevSize) * partialTick;
        
        // alpha starts at 50%, vanishing to 10% during last ticks
        float alpha = 0.5F;
        if (particleMaxAge - particleAge <= 4) {
            alpha = 0.5F - (4 - (particleMaxAge - particleAge)) * 0.1F;
        }

        FMLClientHandler.instance().getClient().renderEngine.bindTexture(TEXTURE);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glDisable(GL11.GL_CULL_FACE);
        
        float relativeTime = worldObj.getTotalWorldTime() + partialTick;
        final float vOffset = -relativeTime * 0.2F - MathHelper.floor_float(-relativeTime * 0.1F);
        
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDepthMask(false);
        
        final float xx = (float)(prevPosX + (posX - prevPosX) * partialTick - interpPosX);
        final float yy = (float)(prevPosY + (posY - prevPosY) * partialTick - interpPosY);
        final float zz = (float)(prevPosZ + (posZ - prevPosZ) * partialTick - interpPosZ);
        GL11.glTranslated(xx, yy, zz);
        
        float rotYaw = prevYaw + (this.rotYaw - prevYaw) * partialTick;
        float rotPitch = prevPitch + (this.rotPitch - prevPitch) * partialTick;
        GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(180.0F + rotYaw, 0.0F, 0.0F, -1.0F);
        GL11.glRotatef(rotPitch, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(rot, 0.0F, 1.0F, 0.0F);
        
        final double xMinStart = -0.15D * size;
        final double xMaxStart = 0.15D * size;
        final double xMinEnd = -0.15D * size * END_MODIFIER;
        final double xMaxEnd = 0.15D * size * END_MODIFIER;
        final double yMax = length * size;
        final double uMin = 0.0D;
        final double uMax = 1.0D;

        for (int t = 0; t < 3; t++) {
            final double vMin = -1.0F + vOffset + t / 3.0F;
            final double vMax = vMin + length * size;
            GL11.glRotatef(60.0F, 0.0F, 1.0F, 0.0F);
            tessellator.startDrawingQuads();
            tessellator.setBrightness(200);
            tessellator.setColorRGBA_F(particleRed, particleGreen, particleBlue, alpha);
            tessellator.addVertexWithUV(xMinEnd, yMax, 0.0D, uMax, vMax);
            tessellator.addVertexWithUV(xMinStart, 0.0D, 0.0D, uMax, vMin);
            tessellator.addVertexWithUV(xMaxStart, 0.0D, 0.0D, uMin, vMin);
            tessellator.addVertexWithUV(xMaxEnd, yMax, 0.0D, uMin, vMax);
            tessellator.draw();
        }

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPopMatrix();
        tessellator.startDrawingQuads();
        prevSize = size;
        FMLClientHandler.instance().getClient().renderEngine.bindTexture(new ResourceLocation("textures/particle/particles.png"));
    }
}