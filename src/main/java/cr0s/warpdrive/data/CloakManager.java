package cr0s.warpdrive.data;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.network.PacketHandler;

import java.util.concurrent.CopyOnWriteArraySet;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Cloak manager stores cloaking devices covered areas
 *
 * @author Cr0s
 */
public class CloakManager {
	
	private static CopyOnWriteArraySet<CloakedArea> cloaks = new CopyOnWriteArraySet<>();
	
	public CloakManager() { }
	
	public boolean isCloaked(final int dimensionID, final int x, final int y, final int z) {
		for (final CloakedArea area : cloaks) {
			if (area.dimensionId != dimensionID) {
				continue;
			}
			
			if (area.minX <= x && area.maxX >= x && area.minY <= y && area.maxY >= y && area.minZ <= z && area.maxZ >= z) {
				return true;
			}
		}
		
		return false;
	}
	
	public void onChunkLoaded(final EntityPlayerMP player, final int chunkPosX, final int chunkPosZ) {
		for (final CloakedArea area : cloaks) {
			// skip other dimensions
			if (area.dimensionId != player.worldObj.provider.dimensionId) {
				continue;
			}
			
			// force refresh if the chunk overlap the cloak
			if ( area.minX <= (chunkPosX << 4 + 15) && area.maxX >= (chunkPosX << 4)
			  && area.minZ <= (chunkPosZ << 4 + 15) && area.maxZ >= (chunkPosZ << 4) ) {
				PacketHandler.sendCloakPacket(player, area, false);
			}
		}
	}
	
	public void onPlayerJoinWorld(final EntityPlayerMP entityPlayerMP, final World world) {
		if (WarpDriveConfig.LOGGING_CLOAKING) {
			WarpDrive.logger.info(String.format("CloakManager.onPlayerJoinWorld %s", entityPlayerMP));
		}
		for (final CloakedArea area : cloaks) {
			// skip other dimensions
			if (area.dimensionId != world.provider.dimensionId) {
				continue;
			}
			
			// force refresh if player is outside the cloak
			if ( area.minX > entityPlayerMP.posX || area.maxX < entityPlayerMP.posX
			  || area.minY > entityPlayerMP.posY || area.maxY < entityPlayerMP.posY
			  || area.minZ > entityPlayerMP.posZ || area.maxZ < entityPlayerMP.posZ ) {
				PacketHandler.sendCloakPacket(entityPlayerMP, area, false);
			}
		}
	}
	
	public boolean isAreaExists(final World world, final int x, final int y, final int z) {
		return (getCloakedArea(world, x, y, z) != null);
	}
	
	public void updateCloakedArea(
			final World world,
			final int dimensionId, final int coreX, final int coreY, final int coreZ, final byte tier,
			final int minX, final int minY, final int minZ,
			final int maxX, final int maxY, final int maxZ) {
		final CloakedArea newArea = new CloakedArea(world, dimensionId, coreX, coreY, coreZ, tier, minX, minY, minZ, maxX, maxY, maxZ);
		
		// find existing one
		for (final CloakedArea area : cloaks) {
			if ( area.dimensionId == world.provider.dimensionId
			  && area.coreX == coreX
			  && area.coreY == coreY
			  && area.coreZ == coreZ ) {
				cloaks.remove(area);
				break;
			}
		}
		cloaks.add(newArea);
		if (world.isRemote) {
			newArea.clientCloak();
		}
		if (WarpDriveConfig.LOGGING_CLOAKING) { WarpDrive.logger.info("Cloak count is " + cloaks.size()); }
	}
	
	public void removeCloakedArea(final int dimensionId, final int coreX, final int coreY, final int coreZ) {
		for (final CloakedArea area : cloaks) {
			if ( area.dimensionId == dimensionId
			  && area.coreX == coreX
			  && area.coreY == coreY
			  && area.coreZ == coreZ ) {
				if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
					area.clientDecloak();
				} else {
					area.sendCloakPacketToPlayersEx(true); // send info about collapsing cloaking field
				}
				cloaks.remove(area);
				break;
			}
		}
	}
	
	public CloakedArea getCloakedArea(final World world, final int x, final int y, final int z) {
		for (final CloakedArea area : cloaks) {
			if (area.dimensionId == world.provider.dimensionId && area.coreX == x && area.coreY == y && area.coreZ == z)
				return area;
		}
		
		return null;
	}
	
	@SideOnly(Side.CLIENT)
	public CloakedArea getCloakedArea(final int x, final int y, final int z) {
		// client only 
		for (final CloakedArea area : cloaks) {
			if (area.coreX == x && area.coreY == y && area.coreZ == z)
				return area;
		}
		
		return null;
	}
	
	public void updatePlayer(final EntityPlayerMP entityPlayerMP) {
		for (final CloakedArea area : cloaks) {
			area.updatePlayer(entityPlayerMP);
		}
	}
	
	@SideOnly(Side.CLIENT)
	public static boolean onBlockChange(final int x, final int y, final int z, final Block block, final int metadata, final int flag) {
		if (block != Blocks.air && cloaks != null) {
			for (final CloakedArea area : cloaks) {
				if (area.isBlockWithinArea(x, y, z)) {
					// WarpDrive.logger.info("CM block is inside");
					if (!area.isEntityWithinArea(Minecraft.getMinecraft().thePlayer)) {
						// WarpDrive.logger.info("CM player is outside");
						return Minecraft.getMinecraft().theWorld.setBlock(x, y, z, area.fogBlock, area.fogMetadata, flag);
					}
				}
			}
		}
		return Minecraft.getMinecraft().theWorld.setBlock(x, y, z, block, metadata, flag);
	}
	
	@SideOnly(Side.CLIENT)
	public static void onFillChunk(final Chunk chunk) {
		if (cloaks == null) {
			// WarpDrive.logger.info("CM onFillChunk (" + chunk.xPosition + " " + chunk.zPosition + ") no cloaks");
			return;
		}
		
		final int chunkX_min = chunk.xPosition * 16;
		final int chunkX_max = chunk.xPosition * 16 + 15;
		final int chunkZ_min = chunk.zPosition * 16;
		final int chunkZ_max = chunk.zPosition * 16 + 15;
		// WarpDrive.logger.info("CM onFillChunk (" + chunk.xPosition + " " + chunk.zPosition + ") " + cloaks.size() + " cloak(s) from (" + chunkX_min + " " + chunkZ_min + ") to (" + chunkX_max + " " + chunkZ_max + ")");
		
		for (final CloakedArea area : cloaks) {
			if ( area.minX <= chunkX_max && area.maxX >= chunkX_min
			  && area.minZ <= chunkZ_max && area.maxZ >= chunkZ_min ) {
				// WarpDrive.logger.info("CM chunk is inside");
				if (!area.isEntityWithinArea(Minecraft.getMinecraft().thePlayer)) {
					// WarpDrive.logger.info("CM player is outside");
					
					int areaX_min = Math.max(chunkX_min, area.minX) & 15;
					int areaX_max = Math.min(chunkX_max, area.maxX) & 15;
					int areaZ_min = Math.max(chunkZ_min, area.minZ) & 15;
					int areaZ_max = Math.min(chunkZ_max, area.maxZ) & 15;
					
					for (int x = areaX_min; x <= areaX_max; x++) {
						for (int z = areaZ_min; z <= areaZ_max; z++) {
							for (int y = area.maxY; y >= area.minY; y--) {
								if (chunk.getBlock(x, y, z) != Blocks.air) {
									chunk.func_150807_a(x, y, z, area.fogBlock, area.fogMetadata);
								}
								
							}
						}
					}
				}
			}
		}
	}
	
	@SideOnly(Side.CLIENT)
	public void onClientChangingDimension() {
		cloaks.clear();
	}
}
