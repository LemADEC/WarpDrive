package cr0s.warpdrive.client;

import cr0s.warpdrive.CommonProxy;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.IItemBase;
import cr0s.warpdrive.block.breathing.BlockColorAirShield;
import cr0s.warpdrive.client.gui.GuiShipNavigation;
import cr0s.warpdrive.entity.EntityNPC;
import cr0s.warpdrive.entity.EntityOfflineAvatar;
import cr0s.warpdrive.entity.EntityParticleBunch;
import cr0s.warpdrive.event.ClientHandler;
import cr0s.warpdrive.event.ModelBakeEventHandler;
import cr0s.warpdrive.event.TooltipHandler;
import cr0s.warpdrive.render.ClientCameraHandler;
import cr0s.warpdrive.render.RenderEntityNPC;
import cr0s.warpdrive.render.RenderEntityOfflineAvatar;
import cr0s.warpdrive.render.RenderEntityParticleBunch;
import cr0s.warpdrive.render.RenderOverlayAir;
import cr0s.warpdrive.render.RenderOverlayCamera;
import cr0s.warpdrive.render.RenderOverlayLocation;

import javax.annotation.Nonnull;

import java.util.Collection;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

public class ClientProxy extends CommonProxy {
	
	@Override
	public boolean isDedicatedServer() {
		return false;
	}
	
	@Override
	public void openShipNavigationGui(final NBTTagCompound tagCompound) {
		Minecraft.getMinecraft().addScheduledTask(() -> GuiShipNavigation.open(tagCompound));
	}
	
	@Override
	public void updateShipNavigationMap(final NBTTagCompound tagCompound) {
		Minecraft.getMinecraft().addScheduledTask(() -> GuiShipNavigation.updateStaticMap(tagCompound));
	}
	
	@Override
	public void onForgePreInitialisation() {
		super.onForgePreInitialisation();
		
		OBJLoader.INSTANCE.addDomain(WarpDrive.MODID);
		
		MinecraftForge.EVENT_BUS.register(ModelBakeEventHandler.INSTANCE);
		MinecraftForge.EVENT_BUS.register(SpriteManager.INSTANCE);
		
		// entity rendering
		RenderingRegistry.registerEntityRenderingHandler(EntityNPC.class, new IRenderFactory<EntityNPC>() {
			@Nonnull
			@Override
			public Render<EntityNPC> createRenderFor(final RenderManager manager) {
				return new RenderEntityNPC(manager);
			}
		});
		RenderingRegistry.registerEntityRenderingHandler(EntityOfflineAvatar.class, new IRenderFactory<EntityOfflineAvatar>() {
			@Nonnull
			@Override
			public Render<EntityOfflineAvatar> createRenderFor(final RenderManager manager) {
				return new RenderEntityOfflineAvatar(manager);
			}
		});
		RenderingRegistry.registerEntityRenderingHandler(EntityParticleBunch.class, new IRenderFactory<EntityParticleBunch>() {
			@Nonnull
			@Override
			public Render<Entity> createRenderFor(final RenderManager manager) {
				return new RenderEntityParticleBunch(manager);
			}
		});
	}
	
	@Override
	public void onForgeInitialisation() {
		super.onForgeInitialisation();
		
		// event handlers
		MinecraftForge.EVENT_BUS.register(new ClientHandler());
		MinecraftForge.EVENT_BUS.register(new TooltipHandler());
		
		// color handlers
		// final Item itemAirShield = Item.getItemFromBlock(blockAirShield);
		// Minecraft.getMinecraft().getItemColors().registerItemColorHandler((IItemColor) itemAirShield, itemAirShield);
		Minecraft.getMinecraft().getBlockColors().registerBlockColorHandler(new BlockColorAirShield(), WarpDrive.blockAirShield);
		
		// generic rendering
		// MinecraftForge.EVENT_BUS.register(new WarpDriveKeyBindings());
		MinecraftForge.EVENT_BUS.register(new RenderOverlayAir());
		MinecraftForge.EVENT_BUS.register(new RenderOverlayCamera());
		MinecraftForge.EVENT_BUS.register(new RenderOverlayLocation());
		
		MinecraftForge.EVENT_BUS.register(new ClientCameraHandler());
	}
	
	@Override
	public void onModelInitialisation(final Object object) {
		if (object instanceof IBlockBase) {
			((IBlockBase) object).modelInitialisation();
			
		} else if (object instanceof IItemBase) {
			((IItemBase) object).modelInitialisation();
			
		} else {
			throw new RuntimeException(String.format("Unsupported object, expecting an IBlockBase or IItemBase instance: %s",
			                                         object));
		}
	}
	
	@Nonnull
	public static ModelResourceLocation getModelResourceLocation(@Nonnull final ItemStack itemStack) {
		final Item item = itemStack.getItem();
		ResourceLocation resourceLocation = item.getRegistryName();
		assert resourceLocation != null;
		
		// reuse blockstate rendering for ItemBlocks when their blockstate have at least one property (typically colored blocks)
		if (item instanceof ItemBlock) {
			final int damage = itemStack.getItemDamage();
			if (damage < 0 || damage > 15) {
				throw new IllegalArgumentException(String.format("Invalid damage %d for %s",
				                                                 damage, itemStack.getItem()));
			}
			final Block block = ((ItemBlock) item).getBlock();
			final IBlockState blockState = block.getStateFromMeta(damage);
			final Collection<IProperty<?>> properties = blockState.getPropertyKeys();
			if (!properties.isEmpty()) {// reuse defined properties
				final String[] blockStateStrings = blockState.toString().split("[\\[\\]]");
				final String variant = blockStateStrings[1];
				return new ModelResourceLocation(resourceLocation, variant);
			}
		}
		
		// use damage value as suffix otherwise
		if (item.getHasSubtypes()) {
			resourceLocation = new ResourceLocation(resourceLocation.getNamespace(), resourceLocation.getPath() + "-" + itemStack.getItemDamage());
		}
		// defaults to inventory variant
		return new ModelResourceLocation(resourceLocation, "inventory");
	}
	
	public static void modelInitialisation(@Nonnull final Item item) {
		if (!(item instanceof IItemBase)) {
			throw new RuntimeException(String.format("Unable to initialize item's model, expecting an IItemBase instance: %s",
			                                         item));
		}
		
		if (!item.getHasSubtypes()) {
			final ModelResourceLocation modelResourceLocation = ((IItemBase) item).getModelResourceLocation(new ItemStack(item));
			ModelLoader.setCustomModelResourceLocation(item, 0, modelResourceLocation);
			
		} else {
			final NonNullList<ItemStack> listItemStacks = NonNullList.create();
			assert item.getCreativeTab() != null;
			item.getSubItems(item.getCreativeTab(), listItemStacks);
			for (final ItemStack itemStack : listItemStacks) {
				final ModelResourceLocation modelResourceLocation = ((IItemBase) item).getModelResourceLocation(itemStack);
				ModelLoader.setCustomModelResourceLocation(item, itemStack.getMetadata(), modelResourceLocation);
			}
		}
	}
}
