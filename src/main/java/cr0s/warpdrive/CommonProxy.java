package cr0s.warpdrive;

import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.entity.EntityParticleBunch;
import cr0s.warpdrive.render.EntityCamera;
import cr0s.warpdrive.world.EntitySphereGen;
import cr0s.warpdrive.world.EntityStarCore;

import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.WeakHashMap;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;

import cpw.mods.fml.common.registry.EntityRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.world.BlockEvent;

public class CommonProxy {
	
	private static final WeakHashMap<GameProfile, WeakReference<EntityPlayer>> fakePlayers = new WeakHashMap<>(100);
	
	void registerEntities() {
		EntityRegistry.registerModEntity(EntitySphereGen.class    , "EntitySphereGenerator", WarpDriveConfig.G_ENTITY_SPHERE_GENERATOR_ID, WarpDrive.instance, 200, 1, false);
		EntityRegistry.registerModEntity(EntityStarCore.class     , "EntityStarCore"       , WarpDriveConfig.G_ENTITY_STAR_CORE_ID       , WarpDrive.instance, 300, 1, false);
		EntityRegistry.registerModEntity(EntityCamera.class       , "EntityCamera"         , WarpDriveConfig.G_ENTITY_CAMERA_ID          , WarpDrive.instance, 300, 1, false);
		EntityRegistry.registerModEntity(EntityParticleBunch.class, "EntityParticleBunch"  , WarpDriveConfig.G_ENTITY_PARTICLE_BUNCH_ID  , WarpDrive.instance, 300, 1, false);
	}
	
	public void registerRendering() {
		// client side only
	}
	
	private static EntityPlayerMP getPlayer(final UUID uuidPlayer) {
		for (Object object : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
			if (object instanceof EntityPlayerMP) {
				final EntityPlayerMP entityPlayerMP = (EntityPlayerMP) object;
				if (entityPlayerMP.getUniqueID() == uuidPlayer) {
					return entityPlayerMP;
				}
			}
		}
		return null;
	}
	
	private static EntityPlayer getFakePlayer(final UUID uuidPlayer, final WorldServer world, final double x, final double y, final double z) {
		final EntityPlayer entityPlayer = uuidPlayer == null ? null : getPlayer(uuidPlayer);
		final GameProfile gameProfile = entityPlayer == null ? WarpDrive.gameProfile : entityPlayer.getGameProfile();
		WeakReference<EntityPlayer> weakFakePlayer = fakePlayers.get(gameProfile);
		EntityPlayer entityFakePlayer = (weakFakePlayer == null) ? null : weakFakePlayer.get();
		if (entityFakePlayer == null) {
			entityFakePlayer = FakePlayerFactory.get(world, gameProfile);
			((EntityPlayerMP)entityFakePlayer).theItemInWorldManager.setGameType(WorldSettings.GameType.SURVIVAL);
			weakFakePlayer = new WeakReference<>(entityFakePlayer);
			fakePlayers.put(gameProfile, weakFakePlayer);
		} else {
			entityFakePlayer.worldObj = world;
		}
		entityFakePlayer.posX = x;
		entityFakePlayer.posY = y;
		entityFakePlayer.posZ = z;
		
		return entityFakePlayer;
	}
	
	public static EntityPlayer getFakePlayer(final UUID uuidPlayer, final World world, final double x, final double y, final double z) {
		if (world.isRemote || !(world instanceof WorldServer)) {
			return null;
		}
		return getFakePlayer(uuidPlayer, (WorldServer) world, x, y, z);
	}
	
	public static boolean isBlockBreakCanceled(final UUID uuidPlayer, final int sourceX, final int sourceY, final int sourceZ,
	                                           final World world, final int eventX, final int eventY, final int eventZ) {
		if (world.isRemote || !(world instanceof WorldServer)) {
			return false;
		}
		if (WarpDriveConfig.LOGGING_BREAK_PLACE) {
			WarpDrive.logger.info("isBlockBreakCanceled by " + uuidPlayer + " at " + sourceX + " " + sourceY + " " + sourceZ
				+ " to " + world.provider.getDimensionName() + " " + eventX + " " + eventY + " " + eventZ);
		}
		
		final Block block = world.getBlock(eventX, eventY, eventZ);
		if (!block.isAir(world, eventX, eventY, eventZ)) {
			final BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(eventX, eventY, eventZ, world,
				world.getBlock(eventX, eventY, eventZ), world.getBlockMetadata(eventX, eventY, eventZ),
				getFakePlayer(uuidPlayer, (WorldServer) world, sourceX + 0.5D, sourceY + 0.5D, sourceZ + 0.5D));
			MinecraftForge.EVENT_BUS.post(breakEvent);
			if (WarpDriveConfig.LOGGING_BREAK_PLACE) {
				WarpDrive.logger.info("isBlockBreakCanceled player " + breakEvent.getPlayer()
					+ " isCanceled " + breakEvent.isCanceled());
			}
			return breakEvent.isCanceled();
		}
		return false;
	}
	
	public static boolean isBlockPlaceCanceled(final UUID uuidPlayer, final int sourceX, final int sourceY, final int sourceZ,
	                                           final World world, final int eventX, final int eventY, final int eventZ,
	                                           final Block block, final int metadata) {
		if (world.isRemote || !(world instanceof WorldServer)) {
			return false;
		}
		if (WarpDriveConfig.LOGGING_BREAK_PLACE) {
			WarpDrive.logger.info("isBlockPlaceCanceled by " + uuidPlayer + " at " + sourceX + " " + sourceY + " " + sourceZ
				+ " to " + world.provider.getDimensionName() + " " + eventX + " " + eventY + " " + eventZ + " of " + Block.blockRegistry.getNameForObject(block) + ":" + metadata);
		}
		final BlockEvent.PlaceEvent placeEvent = new BlockEvent.PlaceEvent(
			new BlockSnapshot(world, eventX, eventY, eventZ, block, metadata), Blocks.air,
			getFakePlayer(uuidPlayer, (WorldServer) world, sourceX + 0.5D, sourceY + 0.5D, sourceZ + 0.5D) );
		
		MinecraftForge.EVENT_BUS.post(placeEvent);
		if (WarpDriveConfig.LOGGING_BREAK_PLACE) {
			WarpDrive.logger.info("isBlockPlaceCanceled player " + placeEvent.player + " isCanceled " + placeEvent.isCanceled());
		}
		return placeEvent.isCanceled();
	}
}