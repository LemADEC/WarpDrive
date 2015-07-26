package cr0s.warpdrive.data;

import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;

public class CamRegistryItem {
    public int dimensionId = -666;
    public ChunkPosition position = null;
    public int frequency = -1;
    public int type = 0; // 0 - basic camera, 1 - laser camera

    public CamRegistryItem(World parWorldObj, ChunkPosition parPosition, int parFrequency, int parType) {
    	frequency = parFrequency;
        position = parPosition;
        dimensionId = parWorldObj.provider.dimensionId;
        type = parType;
    }
}