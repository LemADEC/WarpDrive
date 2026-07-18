package cr0s.warpdrive.render;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IMyBakedModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.world.World;

import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.pipeline.BakedQuadBuilder;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;

public abstract class BakedModelAbstractBase implements IMyBakedModel {
	
	protected static final List<BakedQuad> EMPTY = new ArrayList<>(0);
	
	protected ModelResourceLocation modelResourceLocation;
	protected IBakedModel bakedModelOriginal;
	
	protected TextureAtlasSprite spriteOriginal;
	protected int tintIndexOriginal = -1;
	protected VertexFormat format = DefaultVertexFormats.POSITION_COLOR_TEX;
	
	protected BlockState blockStateDefault;
	
	public BakedModelAbstractBase() {
		super();
	}
	
	@Override
	public void setModelResourceLocation(final ModelResourceLocation modelResourceLocation) {
		this.modelResourceLocation = modelResourceLocation;
	}
	
	private boolean saveOriginalValues(@Nonnull final List<BakedQuad> bakedQuads) {
		if (!bakedQuads.isEmpty()) {
			final BakedQuad bakedQuad = bakedQuads.get(0);
			spriteOriginal = bakedQuad.func_187508_a();
			if (bakedQuad.hasTintIndex()) {
				tintIndexOriginal = bakedQuad.getTintIndex();
			}
			return true;
		}
		return false;
	}
	
	@Override
	public void setOriginalBakedModel(@Nonnull final IBakedModel bakedModel) {
		this.bakedModelOriginal = bakedModel;
		format = DefaultVertexFormats.POSITION_COLOR_TEX;
		try {
			if (saveOriginalValues(bakedModel.getQuads(null, null, null))) {
				return;
			}
			for (final Direction enumFacing : Direction.values()) {
				if (saveOriginalValues(bakedModel.getQuads(null, enumFacing, null))) {
					break;
				}
			}
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
			WarpDrive.logger.error(String.format("Exception trying to retrieve format for %s original baked model %s, defaulting to forge",
			                                     modelResourceLocation, bakedModelOriginal));
		}
	}
	
	protected void putVertex(final BakedQuadBuilder builder,
                     final float x, final float y, final float z,
                     final float red, final float green, final float blue, final float alpha,
                     final float u, final float v,
                     @Nullable final Vector3f normal) {
		ImmutableList<VertexFormatElement> elements = format.getElements();
		for (int index = 0; index < elements.size(); index++) {
			final VertexFormatElement element = elements.get(index);
			switch (element.getUsage()) {
			case POSITION:
				builder.put(index, x, y, z, 1.0F);
				break;
				
			case NORMAL:
				if (normal != null) {
					builder.put(index, normal.getX(), normal.getY(), normal.getZ());
				} else {
					WarpDrive.logger.warn(String.format("Missing normal vector, it's required in format %s",
					                                    format));
					builder.put(index);
				}
				break;
				
			case COLOR:
				builder.put(index, red, green, blue, alpha);
				break;
				
			case UV:
				// Usage-index 0 is the texture UV (TEX_2F).
				// Higher indexes are the lightmap/overlay (e.g. BLOCK's TEX_2SB, index 2).
				// Writing the texture UV into the lightmap element adds garbage with causes an overflow in IVertexBuilder.addQuad
				// resulting in out of range lightmap values and darkening everything.
				// Passing zeroed lightmap means the renderer's own light (passed to addQuad) wins.
				if (element.getIndex() == 0) {
					builder.put(index, u, v, 0.0F, 1.0F);
				} else {
					builder.put(index, 0.0F, 0.0F, 0.0F, 1.0F);
				}
				break;
				
//			case MATRIX:
//			case BLEND_WEIGHT:
			
			case PADDING:
				builder.put(index);
				break;
				
//			case GENERIC:
			
			default:
				WarpDrive.logger.warn(String.format("Unsupported format element #%d %s in %s",
				                                    index, element, format));
				builder.put(index);
				break;
			}
		}
	}
	
	protected void addBakedQuad(final List<BakedQuad> quads, final TextureAtlasSprite sprite,
	                            final float red, final float green, final float blue, final float alpha,
	                            final float x1, final float y1, final float z1, final float u1, final float v1,
	                            final float x2, final float y2, final float z2, final float u2, final float v2,
	                            final float x3, final float y3, final float z3, final float u3, final float v3,
	                            final float x4, final float y4, final float z4, final float u4, final float v4) {
		final Vector3f vectorNormal;
		if (format.hasNormal()) {
			vectorNormal = new Vector3f(x3 - x2, y3 - y2, z3 - z2);
			final Vector3f vectorTemp = new Vector3f(x1 - x2, y1 - y2, z1 - z2);
			vectorNormal.cross(vectorTemp);
			vectorNormal.normalize();
		} else {
			vectorNormal = null;
		}
		
		final BakedQuadBuilder builder = new BakedQuadBuilder(sprite);
		// A face is required by IVertexBuilder.addQuad (it reads getFace() for the normal); only set it when we actually
		// have a normal (BLOCK format), so the POSITION_COLOR_TEX baked models keep their previous face-less behaviour.
		if (vectorNormal != null) {
			builder.setQuadOrientation(Direction.getFacingFromVector(vectorNormal.getX(), vectorNormal.getY(), vectorNormal.getZ()));
		}
		putVertex(builder, x1, y1, z1, red, green, blue, alpha, u1, v1, vectorNormal);
		putVertex(builder, x2, y2, z2, red, green, blue, alpha, u2, v2, vectorNormal);
		putVertex(builder, x3, y3, z3, red, green, blue, alpha, u3, v3, vectorNormal);
		putVertex(builder, x4, y4, z4, red, green, blue, alpha, u4, v4, vectorNormal);
		quads.add(builder.build());
	}
	
	protected void addBakedQuad(final List<BakedQuad> quads, final TextureAtlasSprite sprite, final int color,
	                            final float x1, final float y1, final float z1, final float u1, final float v1,
	                            final float x2, final float y2, final float z2, final float u2, final float v2,
	                            final float x3, final float y3, final float z3, final float u3, final float v3,
	                            final float x4, final float y4, final float z4, final float u4, final float v4) {
		final float[] rgba = { (color >> 16 & 0xFF) / 255.0F,
		                       (color >>  8 & 0xFF) / 255.0F,
		                       (color       & 0xFF) / 255.0F,
		                       (color >> 24 & 0xFF) / 255.0F };
		if (rgba[3] == 0.0F) {
			rgba[3] = 1.0F;
		}
		addBakedQuad(quads, sprite,
		             rgba[0], rgba[1], rgba[2], rgba[3],
		             x1, y1, z1, u1, v1,
		             x2, y2, z2, u2, v2,
		             x3, y3, z3, u3, v3,
		             x4, y4, z4, u4, v4);
	}
	
	@Nonnull
	@Override
	public List<BakedQuad> getQuads(@Nullable final BlockState blockState, @Nullable final Direction side, @Nonnull final Random rand) {
		return bakedModelOriginal.getQuads(blockState, side, rand);
	}
	
	@Override
	public boolean isAmbientOcclusion() {
		return bakedModelOriginal.isAmbientOcclusion();
	}
	
	@Override
	public boolean isGui3d() {
		return bakedModelOriginal.isGui3d();
	}
	
	@Override
	public boolean func_230044_c_() {
		return bakedModelOriginal.func_230044_c_();
	}
	
	@Override
	public boolean isBuiltInRenderer() {
		return bakedModelOriginal.isBuiltInRenderer();
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public TextureAtlasSprite getParticleTexture() {
		return bakedModelOriginal.getParticleTexture();
	}
	
	@Nonnull
	@Override
	public TextureAtlasSprite getParticleTexture(@Nonnull final IModelData modelData) {
		// Minecraft.getInstance().getTextureMapBlocks().getAtlasSprite("warpdrive:someTexture")
		return bakedModelOriginal.getParticleTexture(modelData);
	}
	
	@Nonnull
	@Override
	public ItemOverrideList getOverrides() {
		return bakedModelOriginal.getOverrides();
	}
	
	protected ItemOverrideList itemBlockOverrideList = new ItemOverrideList() {
		@Override
		public IBakedModel getModelWithOverrides(@Nonnull final IBakedModel bakedModel, @Nonnull final ItemStack itemStack,
		                                         final World world, final LivingEntity entity) {
			if (itemStack.getItem() instanceof BlockItem) {
				final Block block = ((BlockItem) itemStack.getItem()).getBlock();
				final BlockState blockState = block.getDefaultState();
				final IBakedModel bakedModelNew = Minecraft.getInstance().getBlockRendererDispatcher().getModelForState(blockState);
				blockStateDefault = blockState;
				return bakedModelNew;
			} else {
				return bakedModelOriginal.getOverrides().getModelWithOverrides(bakedModel, itemStack, world, entity);
			}
		}
	};
	
	@Nonnull
	@Override
	public IBakedModel handlePerspective(@Nonnull final ItemCameraTransforms.TransformType cameraTransformType, final MatrixStack matrixStack) {
		if (bakedModelOriginal == null) {
			return net.minecraftforge.client.ForgeHooksClient.handlePerspective(this, cameraTransformType, matrixStack);
		}
		return bakedModelOriginal.handlePerspective(cameraTransformType, matrixStack);
	}
	
	protected String formatDetails() {
		return String.format("modelResourceLocation %s\nbakedModelOriginal %s\nblockStateDefault %s]",
		                     modelResourceLocation,
		                     bakedModelOriginal,
		                     blockStateDefault );
	}
}