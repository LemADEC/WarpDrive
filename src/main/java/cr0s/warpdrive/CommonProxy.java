package cr0s.warpdrive;

import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.event.EMPReceiver;
import cr0s.warpdrive.event.ItemHandler;
import cr0s.warpdrive.event.LivingHandler;
import cr0s.warpdrive.event.PlayerHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.WeakHashMap;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.world.BlockEvent;

public class CommonProxy {
	
	private static final WeakHashMap<GameProfile, WeakReference<EntityPlayer>> fakePlayers = new WeakHashMap<>(100);
	
	public boolean isDedicatedServer() {
		return true;
	}
	
	public void openShipNavigationGui(final NBTTagCompound tagCompound) {
		// client-side only
	}
	
	public void updateShipNavigationMap(final NBTTagCompound tagCompound) {
		// client-side only
	}
	
	@Nullable
	private static EntityPlayerMP getPlayer(@Nonnull final WorldServer world, final UUID uuidPlayer) {
		assert world.getMinecraftServer() != null;
		for (final EntityPlayerMP entityPlayerMP : world.getMinecraftServer().getPlayerList().getPlayers()) {
			if (entityPlayerMP.getUniqueID() == uuidPlayer) {
				return entityPlayerMP;
			}
		}
		return null;
	}
	
	public static EntityPlayer getFakePlayer(@Nullable final UUID uuidPlayer, @Nonnull final WorldServer world, @Nonnull final BlockPos blockPos) {
		final EntityPlayer entityPlayer = uuidPlayer == null ? null : getPlayer(world, uuidPlayer);
		final GameProfile gameProfile = entityPlayer == null ? WarpDrive.gameProfile : entityPlayer.getGameProfile();
		WeakReference<EntityPlayer> weakFakePlayer = fakePlayers.get(gameProfile);
		EntityPlayer entityFakePlayer = (weakFakePlayer == null) ? null : weakFakePlayer.get();
		if (entityFakePlayer == null) {
			entityFakePlayer = FakePlayerFactory.get(world, gameProfile);
			((EntityPlayerMP) entityFakePlayer).interactionManager.setGameType(GameType.SURVIVAL);
			weakFakePlayer = new WeakReference<>(entityFakePlayer);
			fakePlayers.put(gameProfile, weakFakePlayer);
		} else {
			entityFakePlayer.world = world;
		}
		entityFakePlayer.posX = blockPos.getX() + 0.5D;
		entityFakePlayer.posY = blockPos.getY() + 0.5D;
		entityFakePlayer.posZ = blockPos.getZ() + 0.5D;
		
		return entityFakePlayer;
	}
	
	public static boolean isBlockBreakCanceled(final UUID uuidPlayer, final BlockPos blockPosSource,
	                                           @Nonnull final World world, final BlockPos blockPosEvent) {
		if (world.isRemote || !(world instanceof WorldServer)) {
			return false;
		}
		if (WarpDriveConfig.LOGGING_BREAK_PLACE) {
			WarpDrive.logger.info(String.format("isBlockBreakCanceled by %s %s to block %s",
			                                    uuidPlayer, Commons.format(world, blockPosSource), Commons.format(world, blockPosEvent)));
		}
		
		final IBlockState blockState = world.getBlockState(blockPosEvent);
		if (!blockState.getBlock().isAir(blockState, world, blockPosEvent)) {
			final BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(
			    world, blockPosEvent, world.getBlockState(blockPosEvent),
				getFakePlayer(uuidPlayer, (WorldServer) world, blockPosSource));
			MinecraftForge.EVENT_BUS.post(breakEvent);
			if (WarpDriveConfig.LOGGING_BREAK_PLACE) {
				WarpDrive.logger.info(String.format("isBlockBreakCanceled player %s isCanceled %s",
				                                    breakEvent.getPlayer(), breakEvent.isCanceled()));
			}
			return breakEvent.isCanceled();
		}
		return false;
	}
	
	public static boolean isBlockPlaceCanceled(final UUID uuidPlayer, final BlockPos blockPosSource,
	                                           @Nonnull final World world, final BlockPos blockPosEvent, final IBlockState blockState) {
		if (world.isRemote || !(world instanceof WorldServer)) {
			return false;
		}
		if (WarpDriveConfig.LOGGING_BREAK_PLACE) {
			WarpDrive.logger.info(String.format("isBlockPlaceCanceled by %s %s to block %s %s",
			                                    uuidPlayer, Commons.format(world, blockPosSource),
			                                    Commons.format(world, blockPosEvent), blockState));
		}
		final BlockEvent.PlaceEvent placeEvent = new BlockEvent.PlaceEvent(
				new BlockSnapshot(world, blockPosEvent, blockState),
				Blocks.AIR.getDefaultState(),
				getFakePlayer(uuidPlayer, (WorldServer) world, blockPosSource),
				EnumHand.MAIN_HAND);
		
		MinecraftForge.EVENT_BUS.post(placeEvent);
		if (WarpDriveConfig.LOGGING_BREAK_PLACE) {
			WarpDrive.logger.info(String.format("isBlockPlaceCanceled player %s isCanceled %s",
			                                    placeEvent.getPlayer(), placeEvent.isCanceled()));
		}
		return placeEvent.isCanceled();
	}
	
	public void onForgePreInitialisation() {
	
	}
	
	public void onModelInitialisation(final Object object) {
	
	}
	
	public void onForgeInitialisation() {
		// event handlers
		MinecraftForge.EVENT_BUS.register(new ItemHandler());
		MinecraftForge.EVENT_BUS.register(new LivingHandler());
		MinecraftForge.EVENT_BUS.register(new PlayerHandler());
		MinecraftForge.EVENT_BUS.register(EMPReceiver.class);
	}
}
