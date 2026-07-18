package cr0s.warpdrive.event;

import cr0s.warpdrive.BreathingManager;
import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.GlobalRegionManager;
import cr0s.warpdrive.network.PacketHandler;

import javax.annotation.Nonnull;
import java.util.List;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;

import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedInEvent;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;

/**
*
* @author LemADEC
*/
public class WorldHandler {
	
	// Server side
	@SubscribeEvent
	public void onEntityJoinWorld(@Nonnull final EntityJoinWorldEvent event){
		if ( event.getWorld().isRemote()
		  || !(event.getEntity() instanceof PlayerEntity) ) {
			return;
		}
		WarpDrive.logger.info(String.format("onEntityJoinWorld %s", event.getEntity()));
		if (event.getEntity() instanceof LivingEntity) {
			final LivingEntity entityLivingBase = (LivingEntity) event.getEntity();
			final int x = MathHelper.floor(entityLivingBase.getPosX());
			final int y = MathHelper.floor(entityLivingBase.getPosY());
			final int z = MathHelper.floor(entityLivingBase.getPosZ());
			final CelestialObject celestialObject = CelestialObjectManager.get(event.getWorld(), x, z);
			
			if (entityLivingBase instanceof ServerPlayerEntity) {
				WarpDrive.cloaks.onPlayerJoinWorld((ServerPlayerEntity) entityLivingBase, event.getWorld());
				PacketHandler.sendClientSync((ServerPlayerEntity) entityLivingBase, celestialObject);
				
			} else {
				if (celestialObject == null) {
					// unregistered dimension => exit
					return;
				}
				if (entityLivingBase.ticksExisted > 5) {
					// just changing dimension
					return;
				}
				if (!celestialObject.hasAtmosphere()) {
					final boolean canJoin = BreathingManager.onLivingJoinEvent(entityLivingBase, x, y, z);
					if (!canJoin) {
						event.setCanceled(true);
					}
				}
				if (!celestialObject.isInsideBorder(entityLivingBase.getPosX(), entityLivingBase.getPosZ())) {
					event.setCanceled(true);
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onPlayerChangedDimension(@Nonnull final PlayerChangedDimensionEvent event) {
		WarpDrive.logger.info(String.format("onPlayerChangedDimension %s %s -> %s (%.1f %.1f %.1f)",
		                                    event.getPlayer().getName(), event.getFrom().getRegistryName(), event.getTo().getRegistryName(),
		                                    event.getPlayer().getPosX(), event.getPlayer().getPosY(), event.getPlayer().getPosZ() ));
		WarpDrive.cloaks.onPlayerJoinWorld((ServerPlayerEntity) event.getPlayer(), ((ServerPlayerEntity) event.getPlayer()).world);
	}
	
	// Client side
	@SubscribeEvent
	@OnlyIn(Dist.CLIENT)
	public void onClientConnectedToServer(@Nonnull final LoggedInEvent event) {
		WarpDrive.logger.info(String.format("onClientConnectedToServer player %s networkManager %s isLocal %s",
		                                    event.getPlayer(),
		                                    event.getNetworkManager(),
		                                    event.getNetworkManager() == null ? "n/a" : event.getNetworkManager().isLocalChannel() ));
		WarpDrive.cloaks.onClientChangingDimension();
	}
	
	@SubscribeEvent
	@OnlyIn(Dist.CLIENT)
	public void onWorldUnload(final WorldEvent.Unload event) {
		// WarpDrive.logger.info(String.format("onWorldUnload world %s", Commons.format(event.getWorld()));
		WarpDrive.cloaks.onClientChangingDimension();
	}
	
	@SubscribeEvent
	public void onServerTick(@Nonnull final ServerTickEvent event) {
		if (event.side != LogicalSide.SERVER || event.phase != Phase.END) {
			return;
		}
		
		AbstractSequencer.updateTick();
		LivingHandler.updateTick();
	}
	
	// BreakEvent = entity is breaking a block (no ancestor)
	// EntityPlaceEvent = entity is EntityFallingBlock
	// HarvestDropsEvent = collecting drops
	// NeighborNotifyEvent = neighbours update, snow placed/removed by environment, WorldEdit (can't be cancelled)
	// PlaceEvent (EntityPlaceEvent) = player is (re)placing a block
	// PortalSpawnEvent = nether portal is opening (fire placed inside an obsidian frame)
	@SuppressWarnings("ConstantConditions")
	@SubscribeEvent
	public void onBlockEvent(final BlockEvent blockEvent) {
		if ( WarpDriveConfig.isGregtechLoaded
		  && blockEvent.getWorld().getWorldInfo().getWorldName().equals("DummyServer") ) {
			return;
		}
		
		final Entity entity;
		final BlockState blockStateBefore;
		final BlockState blockStatePlaced;
		if (blockEvent instanceof BlockEvent.EntityPlaceEvent) {
			final BlockEvent.EntityPlaceEvent entityPlaceEvent = (BlockEvent.EntityPlaceEvent) blockEvent; 
			entity = entityPlaceEvent.getEntity();
			if (entity instanceof PlayerEntity) {
				blockStateBefore = entityPlaceEvent.getBlockSnapshot().getReplacedBlock();
				blockStatePlaced = entityPlaceEvent.getPlacedBlock();
			} else {
				blockStateBefore = entityPlaceEvent.getPlacedAgainst();
				blockStatePlaced = entityPlaceEvent.getPlacedBlock();
			}
		} else if (blockEvent instanceof BlockEvent.BreakEvent) {
			entity = ((BlockEvent.BreakEvent) blockEvent).getPlayer();
			blockStateBefore = blockEvent.getWorld().getBlockState(blockEvent.getPos());
			blockStatePlaced = blockEvent.getState();
		} else {
			entity = null;
			blockStateBefore = blockEvent.getWorld().getBlockState(blockEvent.getPos());
			blockStatePlaced = blockEvent.getState();
		}
		if (WarpDrive.isDev && WarpDriveConfig.LOGGING_BREAK_PLACE) {
			if (blockStateBefore != blockStatePlaced) {
				WarpDrive.logger.info(String.format("onBlockEvent %s %s -> %s %s by %s",
				                                    blockEvent.getClass().getSimpleName(),
				                                    blockStateBefore,
				                                    blockStatePlaced,
				                                    Commons.format(blockEvent.getWorld(), blockEvent.getPos()),
				                                    entity ));
			} else {
				WarpDrive.logger.info(String.format("onBlockEvent %s %s %s by %s",
				                                    blockEvent.getClass().getSimpleName(),
				                                    blockStatePlaced,
				                                    Commons.format(blockEvent.getWorld(), blockEvent.getPos()),
				                                    entity ));
			}
		}
		boolean isAllowed = true;
		if (blockEvent instanceof BlockEvent.EntityMultiPlaceEvent) {
			final List<BlockSnapshot> listBlockSnapshots = ((BlockEvent.EntityMultiPlaceEvent) blockEvent).getReplacedBlockSnapshots();
			for (final BlockSnapshot blockSnapshot : listBlockSnapshots) {
				final BlockState blockStateCurrent = blockSnapshot.getCurrentBlock();
				isAllowed = isAllowed && GlobalRegionManager.onBlockUpdating(entity, blockEvent.getWorld(), blockSnapshot.getPos(), blockStateCurrent);
				if (blockStateCurrent != blockSnapshot.getReplacedBlock()) {
					isAllowed = isAllowed && GlobalRegionManager.onBlockUpdating(entity, blockEvent.getWorld(), blockSnapshot.getPos(), blockSnapshot.getReplacedBlock());
				}
			}
		} else if (blockEvent instanceof BlockEvent.PortalSpawnEvent) {
			isAllowed = isAllowed && CelestialObjectManager.onOpeningNetherPortal(blockEvent.getWorld(), blockEvent.getPos());
		} else {
			isAllowed = isAllowed && GlobalRegionManager.onBlockUpdating(entity, blockEvent.getWorld(), blockEvent.getPos(), blockEvent.getState());
		}
		if ( blockEvent instanceof BlockEvent.BreakEvent
		  && entity instanceof PlayerEntity ) {
			isAllowed = isAllowed && PlayerHandler.checkMaintenanceAndCrew(blockEvent, (PlayerEntity) entity, blockEvent.getPos(), blockStateBefore);
		}
		if (!isAllowed) {
			if (blockEvent.isCancelable()) {
				blockEvent.setCanceled(true);
			} else if (blockEvent instanceof BlockEvent.HarvestDropsEvent) {
				if (Commons.throttleMe("WorldHandler.onBlockEvent")) {
					WarpDrive.logger.info(String.format("Skipping HarvestDropsEvent %s %s %s by %s",
					                                    blockEvent.getClass().getSimpleName(),
					                                    blockStatePlaced,
					                                    Commons.format(blockEvent.getWorld(), blockEvent.getPos()),
					                                    entity ));
				}
			} else {
				try {
					blockEvent.getWorld().removeBlock(blockEvent.getPos(), false);
				} catch (final Exception exception) {
					if (Commons.throttleMe("WorldHandler.onBlockEvent")) {
						exception.printStackTrace();
						WarpDrive.logger.info(String.format("Exception with %s %s %s by %s",
						                                    blockEvent.getClass().getSimpleName(),
						                                    blockStatePlaced,
						                                    Commons.format(blockEvent.getWorld(), blockEvent.getPos()),
						                                    entity ));
					}
				}
			}
			return;
		}
		
		if (blockEvent instanceof BlockEvent.EntityMultiPlaceEvent) {
			final List<BlockSnapshot> listBlockSnapshots = ((BlockEvent.EntityMultiPlaceEvent) blockEvent).getReplacedBlockSnapshots();
			for (final BlockSnapshot blockSnapshot : listBlockSnapshots) {
				ChunkHandler.onBlockUpdated(blockEvent.getWorld(), blockSnapshot.getPos());
			}
		} else {
			ChunkHandler.onBlockUpdated(blockEvent.getWorld(), blockEvent.getPos());
		}
	}
}
