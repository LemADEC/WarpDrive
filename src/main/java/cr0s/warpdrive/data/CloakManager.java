package cr0s.warpdrive.data;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.network.PacketHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CopyOnWriteArraySet;

import net.minecraft.block.BlockState;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.block.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.dimension.DimensionType;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.thread.EffectiveSide;

public class CloakManager {
	
	private static final CopyOnWriteArraySet<CloakedArea> cloaks = new CopyOnWriteArraySet<>();
	private static final CopyOnWriteArraySet<CloakedArea> cloakToRefresh = new CopyOnWriteArraySet<>();
	
	public CloakManager() { }
	
	public void onChunkLoaded(final ServerPlayerEntity player, final int chunkPosX, final int chunkPosZ) {
		for (final CloakedArea area : cloaks) {
			// skip other dimensions
			if (!area.dimensionId.equals(player.world.getDimension().getType().getRegistryName())) {
				continue;
			}
			
			// force refresh if the chunk overlap the cloak
			if ( area.minX <= (chunkPosX << 4 + 15) && area.maxX >= (chunkPosX << 4)
			  && area.minZ <= (chunkPosZ << 4 + 15) && area.maxZ >= (chunkPosZ << 4) ) {
				PacketHandler.sendCloakPacket(player, area, false);
			}
		}
	}
	
	public void onPlayerJoinWorld(final ServerPlayerEntity entityServerPlayer, final World world) {
		if (WarpDriveConfig.LOGGING_CLOAKING) {
			WarpDrive.logger.info(String.format("CloakManager.onPlayerJoinWorld %s", entityServerPlayer));
		}
		for (final CloakedArea area : cloaks) {
			// skip other dimensions
			if (!area.dimensionId.equals(world.getDimension().getType().getRegistryName())) {
				continue;
			}
			
			// force refresh if player is outside the cloak
			if ( area.minX > entityServerPlayer.getPosX() || area.maxX < entityServerPlayer.getPosX()
			  || area.minY > entityServerPlayer.getPosY() || area.maxY < entityServerPlayer.getPosY()
			  || area.minZ > entityServerPlayer.getPosZ() || area.maxZ < entityServerPlayer.getPosZ() ) {
				PacketHandler.sendCloakPacket(entityServerPlayer, area, false);
			}
		}
	}
	
	public boolean isAreaExists(final World world, final BlockPos blockPos) {
		return (getCloakedArea(world, blockPos) != null);
	}
	
	public CloakedArea updateCloakedArea(
			@Nonnull final World world, @Nonnull final BlockPos blockPosCore, final boolean isFullyTransparent,
			final int minX, final int minY, final int minZ,
			final int maxX, final int maxY, final int maxZ) {
		assert world.getDimension().getType().getRegistryName() != null;
		final CloakedArea cloakedAreaNew = new CloakedArea(world, world.getDimension().getType().getRegistryName(), blockPosCore, isFullyTransparent,
		                                                   minX, minY, minZ, maxX, maxY, maxZ );
		
		// find existing one
		for (final CloakedArea cloakedArea : cloaks) {
			if ( cloakedArea.dimensionId.equals(world.getDimension().getType().getRegistryName())
			  && cloakedArea.blockPosCore.equals(blockPosCore) ) {
				cloaks.remove(cloakedArea);
				break;
			}
		}
		cloaks.add(cloakedAreaNew);
		if (world.isRemote()) {
			cloakToRefresh.add(cloakedAreaNew);
		}
		if (WarpDriveConfig.LOGGING_CLOAKING) {
			WarpDrive.logger.info(String.format("Cloak count is %s", cloaks.size()));
		}
		return cloakedAreaNew;
	}
	
	@OnlyIn(Dist.CLIENT)
	public void onClientTick() {
		@Nullable
		final ClientPlayerEntity player = Minecraft.getInstance().player;
		if (player == null) {
			// skip without clearing the cache while client world is loading
			return;
		}
		final CloakedArea[] cloakedAreas = cloakToRefresh.toArray(new CloakedArea[0]);
		cloakToRefresh.clear();
		for (final CloakedArea cloakedArea : cloakedAreas) {
			cloakedArea.clientCloak(player);
		}
	}
	
	public void removeCloakedArea(final ResourceLocation dimensionId, final BlockPos blockPos) {
		for (final CloakedArea area : cloaks) {
			if ( area.blockPosCore.equals(blockPos)
			  && area.dimensionId.equals(dimensionId) ) {
				if (EffectiveSide.get() == LogicalSide.CLIENT) {
					area.clientDecloak();
				} else {
					area.sendCloakPacketToPlayersEx(true); // send info about collapsing cloaking field
				}
				cloaks.remove(area);
				break;
			}
		}
	}
	
	public CloakedArea getCloakedArea(final World world, final BlockPos blockPos) {
		for (final CloakedArea area : cloaks) {
			if ( area.blockPosCore.equals(blockPos)
			  && area.dimensionId.equals(world.getDimension().getType().getRegistryName()) ) {
				return area;
			}
		}
		
		return null;
	}
	
	public void updatePlayer(final ServerPlayerEntity entityServerPlayer) {
		for (final CloakedArea area : cloaks) {
			area.updatePlayer(entityServerPlayer);
		}
	}
	
	@Nullable
	public static CloakedArea getContainingArea(@Nonnull final World world, @Nonnull final BlockPos blockPos1, @Nullable final BlockPos blockPos2) {
		for (final CloakedArea area : cloaks) {
			if ( area.dimensionId.equals(world.getDimension().getType().getRegistryName())
			  && area.isBlockWithinArea(blockPos1) ) {
				return blockPos2 == null || area.isBlockWithinArea(blockPos2) ? area : null;
			}
		}
		return null;
	}
	
	@SuppressWarnings("unused") // Core mod
	@OnlyIn(Dist.CLIENT)
	public static void WorldClient_invalidateRegionAndSetBlock_setBlockState(@Nonnull final BlockPos blockPos, @Nonnull final BlockState blockState, final int flags) {
		final World world = Minecraft.getInstance().world;
		assert world != null;
		final PlayerEntity playerEntity = Minecraft.getInstance().player;
		assert playerEntity != null;
		
		if (blockState.getBlock() != Blocks.AIR) {
			for (final CloakedArea area : cloaks) {
				if (area.isBlockWithinArea(blockPos)) {
					if (WarpDrive.isDev && WarpDriveConfig.LOGGING_CLOAKING) {
						WarpDrive.logger.info("CloakManager block is inside");
					}
					if (!area.isEntityWithinArea(playerEntity)) {
						if (WarpDrive.isDev && WarpDriveConfig.LOGGING_CLOAKING) {
							WarpDrive.logger.info("CloakManager player is outside");
						}
						world.setBlockState(blockPos, area.blockStateFog, flags);
						return;
					}
				}
			}
		}
		world.setBlockState(blockPos, blockState, flags);
	}
	
	@OnlyIn(Dist.CLIENT)
	public static void Chunk_read(@Nonnull final Chunk chunk) {
		final PlayerEntity playerEntity = Minecraft.getInstance().player;
		assert playerEntity != null;
		final int chunkX_min = chunk.getPos().x * 16;
		final int chunkX_max = chunk.getPos().x * 16 + 15;
		final int chunkZ_min = chunk.getPos().z * 16;
		final int chunkZ_max = chunk.getPos().z * 16 + 15;
		if (WarpDrive.isDev && WarpDriveConfig.LOGGING_CLOAKING) {
			WarpDrive.logger.info(String.format("CloakManager Chunk_read (%d %d) %d cloak(s) from (%d %d) to (%d %d)",
			                                    chunk.getPos().x, chunk.getPos().z, cloaks.size(),
			                                    chunkX_min, chunkZ_min, chunkX_max, chunkZ_max));
		}
		
		for (final CloakedArea area : cloaks) {
			if ( area.minX <= chunkX_max && area.maxX >= chunkX_min
			  && area.minZ <= chunkZ_max && area.maxZ >= chunkZ_min ) {
				if (WarpDrive.isDev && WarpDriveConfig.LOGGING_CLOAKING) {
					WarpDrive.logger.info("CloakManager chunk is inside");
				}
				if (!area.isEntityWithinArea(playerEntity)) {
					if (WarpDrive.isDev && WarpDriveConfig.LOGGING_CLOAKING) {
						WarpDrive.logger.info("CloakManager player is outside");
					}
					
					final int areaX_min = Math.max(chunkX_min, area.minX) & 15;
					final int areaX_max = Math.min(chunkX_max, area.maxX) & 15;
					final int areaZ_min = Math.max(chunkZ_min, area.minZ) & 15;
					final int areaZ_max = Math.min(chunkZ_max, area.maxZ) & 15;
					
					for (int x = areaX_min; x <= areaX_max; x++) {
						for (int z = areaZ_min; z <= areaZ_max; z++) {
							for (int y = area.maxY; y >= area.minY; y--) {
								final BlockPos blockPos = new BlockPos(x, y, z);
								if (chunk.getBlockState(blockPos).getBlock() != Blocks.AIR) {
									chunk.setBlockState(blockPos, area.blockStateFog, false);
								}
							}
						}
					}
				}
			}
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public void onClientChangingDimension() {
		cloaks.clear();
	}
}