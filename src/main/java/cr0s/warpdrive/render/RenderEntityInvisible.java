package cr0s.warpdrive.render;

import javax.annotation.Nonnull;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.matrix.MatrixStack;

// Renders nothing. For those non-visual entities that still REQUIRE a client renderer...
@OnlyIn(Dist.CLIENT)
public class RenderEntityInvisible<T extends Entity> extends EntityRenderer<T> {

	private static final ResourceLocation TEXTURE_LOCATION = new ResourceLocation("minecraft:textures/misc/white_wool.png");

	public RenderEntityInvisible(final EntityRendererManager renderManager) {
		super(renderManager);
	}

	@Nonnull
	@Override
	public ResourceLocation getEntityTexture(@Nonnull final T entity) {
		return TEXTURE_LOCATION;
	}

	@Override
	public void render(@Nonnull final T entity, final float entityYaw, final float partialTicks,
	                   @Nonnull final MatrixStack matrixStack, @Nonnull final IRenderTypeBuffer buffer, final int packedLight) {
		// Intentionally renders nothing
	}
}
