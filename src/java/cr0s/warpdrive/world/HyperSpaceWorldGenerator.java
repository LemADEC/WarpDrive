package cr0s.warpdrive.world;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import cpw.mods.fml.common.IWorldGenerator;
import cr0s.warpdrive.WarpDriveConfig;

/**
 * @author Cr0s
 */
public class HyperSpaceWorldGenerator implements IWorldGenerator
{
    /**
     * Generator for chunk
     * @param random
     * @param chunkX
     * @param chunkZ
     * @param world
     * @param chunkGenerator
     * @param chunkProvider
     */
    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider)
    {
        if (world.provider.dimensionId != WarpDriveConfig.G_HYPERSPACE_DIMENSION_ID)
        {
            // ...
        }
    }
}
