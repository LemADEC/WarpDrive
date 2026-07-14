package cr0s.warpdrive.client;

import cr0s.warpdrive.CommonProxy;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.IItemBase;
import cr0s.warpdrive.entity.EntityLaserExploder;
import cr0s.warpdrive.entity.EntityNPC;
import cr0s.warpdrive.entity.EntityOfflineAvatar;
import cr0s.warpdrive.entity.EntityParticleBunch;
import cr0s.warpdrive.entity.EntitySeat;
import cr0s.warpdrive.event.ClientHandler;
import cr0s.warpdrive.event.ModelHandler;
import cr0s.warpdrive.event.TooltipHandler;
import cr0s.warpdrive.render.ClientCameraHandler;
import cr0s.warpdrive.render.EntityCamera;
import cr0s.warpdrive.render.RenderEntityInvisible;
import cr0s.warpdrive.render.RenderEntityNPC;
import cr0s.warpdrive.render.RenderEntityOfflineAvatar;
import cr0s.warpdrive.render.RenderEntityParticleBunch;
import cr0s.warpdrive.world.EntitySphereGen;
import cr0s.warpdrive.world.EntityStarCore;
import cr0s.warpdrive.render.RenderOverlayAir;
import cr0s.warpdrive.render.RenderOverlayCamera;
import cr0s.warpdrive.render.RenderOverlayLocation;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ClientProxy extends CommonProxy {
	
	public static final KeyBinding keyBindingCameraCenter  = new KeyBinding("key.camera.center.name"  , InputMappings.Type.MOUSE, 3, WarpDrive.MODID);
	public static final KeyBinding keyBindingCameraShoot   = new KeyBinding("key.camera.shoot.name"   , 32, WarpDrive.MODID);
	public static final KeyBinding keyBindingCameraZoomIn  = new KeyBinding("key.camera.zoom_in.name" , InputMappings.Type.MOUSE, 0, WarpDrive.MODID);
	public static final KeyBinding keyBindingCameraZoomOut = new KeyBinding("key.camera.zoom_out.name", InputMappings.Type.MOUSE, 1, WarpDrive.MODID);
	
	@Override
	public void onModConstruction() {
		super.onModConstruction();
		
		// client events
		FMLJavaModLoadingContext.get().getModEventBus().register(ModelHandler.INSTANCE);
		FMLJavaModLoadingContext.get().getModEventBus().register(SpriteManager.INSTANCE);
		
		// entity rendering
		RenderingRegistry.registerEntityRenderingHandler(EntityNPC.TYPE, new IRenderFactory<EntityNPC>() {
			@Nonnull
			@Override
			public EntityRenderer<EntityNPC> createRenderFor(final EntityRendererManager manager) {
				return new RenderEntityNPC(manager);
			}
		});
		RenderingRegistry.registerEntityRenderingHandler(EntityOfflineAvatar.TYPE, new IRenderFactory<EntityOfflineAvatar>() {
			@Nonnull
			@Override
			public EntityRenderer<EntityOfflineAvatar> createRenderFor(final EntityRendererManager manager) {
				return new RenderEntityOfflineAvatar(manager);
			}
		});
		RenderingRegistry.registerEntityRenderingHandler(EntityParticleBunch.TYPE, new IRenderFactory<EntityParticleBunch>() {
			@Nonnull
			@Override
			public EntityRenderer<EntityParticleBunch> createRenderFor(final EntityRendererManager manager) {
				return new RenderEntityParticleBunch(manager);
			}
		});
		
		// non-visual entities still need a renderer in 1.15+ (else "No renderer registered for ...") — render nothing
		RenderingRegistry.registerEntityRenderingHandler(EntitySphereGen.TYPE, manager -> new RenderEntityInvisible<EntitySphereGen>(manager));
		RenderingRegistry.registerEntityRenderingHandler(EntityStarCore.TYPE, manager -> new RenderEntityInvisible<EntityStarCore>(manager));
		RenderingRegistry.registerEntityRenderingHandler(EntitySeat.TYPE, manager -> new RenderEntityInvisible<EntitySeat>(manager));
		RenderingRegistry.registerEntityRenderingHandler(EntityLaserExploder.TYPE, manager -> new RenderEntityInvisible<EntityLaserExploder>(manager));
		RenderingRegistry.registerEntityRenderingHandler(EntityCamera.TYPE, manager -> new RenderEntityInvisible<EntityCamera>(manager));
		
		// Key bindings (skipped during data generation run)
		if (Minecraft.getInstance() != null) {
			ClientRegistry.registerKeyBinding(keyBindingCameraCenter);
			ClientRegistry.registerKeyBinding(keyBindingCameraShoot);
			ClientRegistry.registerKeyBinding(keyBindingCameraZoomIn);
			ClientRegistry.registerKeyBinding(keyBindingCameraZoomOut);
		}
		
		// event handlers
		MinecraftForge.EVENT_BUS.register(new ClientHandler());
		MinecraftForge.EVENT_BUS.register(new TooltipHandler());
		
		// color handlers
		// final Item itemAirShield = Item.getItemFromBlock(WarpDrive.blockAirShields);
		// Minecraft.getInstance().getItemColors().registerItemColorHandler((IItemColor) itemAirShield, itemAirShield);
		// TODO MC1.15 air shield colors
		// Minecraft.getInstance().getBlockColors().registerBlockColorHandler(new BlockColorAirShield(), WarpDrive.blockAirShields);
		
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
}