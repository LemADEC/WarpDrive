package cr0s.warpdrive.world;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class JumpgateGenerator {
	public static final int GATE_SIZE = 52;
	public static final int GATE_LENGTH = 37;

	public static final int GATE_LENGTH_HALF = GATE_LENGTH / 2;
	public static final int GATE_SIZE_HALF = GATE_SIZE / 2;

	public static void generate(World worldObj, int x, int y, int z) {
		for (int length = -GATE_LENGTH_HALF; length < GATE_LENGTH_HALF; length++) {
			for (int newZ = z - GATE_SIZE_HALF; newZ <= z + GATE_SIZE_HALF; newZ++) {
				worldObj.setBlock(x + (2 * length), y + GATE_SIZE_HALF, newZ, Blocks.bedrock);
				worldObj.setBlock(x + (2 * length), y - GATE_SIZE_HALF, newZ, Blocks.bedrock);
			}

			for (int newY = y - GATE_SIZE_HALF; newY <= y + GATE_SIZE_HALF; newY++) {
				worldObj.setBlock(x + (2 * length), newY, z + GATE_SIZE_HALF, Blocks.bedrock);
				worldObj.setBlock(x + (2 * length), newY, z - GATE_SIZE_HALF, Blocks.bedrock);
			}
		}

		for (int length = -GATE_LENGTH; length < GATE_LENGTH; length++) {
			worldObj.setBlock(x + length, y + GATE_SIZE_HALF, z, Blocks.bedrock);
			worldObj.setBlock(x + length, y - GATE_SIZE_HALF, z, Blocks.bedrock);
			worldObj.setBlock(x + length, y + GATE_SIZE_HALF, z + 1, Blocks.bedrock);
			worldObj.setBlock(x + length, y - GATE_SIZE_HALF, z - 1, Blocks.bedrock);
			worldObj.setBlock(x + length, y + GATE_SIZE_HALF, z + 2, Blocks.bedrock);
			worldObj.setBlock(x + length, y - GATE_SIZE_HALF, z - 2, Blocks.bedrock);
			worldObj.setBlock(x + length, y, z - GATE_SIZE_HALF, Blocks.bedrock);
			worldObj.setBlock(x + length, y, z + GATE_SIZE_HALF, Blocks.bedrock);
			worldObj.setBlock(x + length, y + 1, z - GATE_SIZE_HALF, Blocks.bedrock);
			worldObj.setBlock(x + length, y - 1, z + GATE_SIZE_HALF, Blocks.bedrock);
			worldObj.setBlock(x + length, y + 2, z - GATE_SIZE_HALF, Blocks.bedrock);
			worldObj.setBlock(x + length, y - 2, z + GATE_SIZE_HALF, Blocks.bedrock);
		}
	}
}
