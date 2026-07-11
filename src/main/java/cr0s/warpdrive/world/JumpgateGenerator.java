package cr0s.warpdrive.world;

import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class JumpgateGenerator {
	public static final int GATE_SIZE = 52;
	public static final int GATE_LENGTH = 37;

	public static final int GATE_LENGTH_HALF = GATE_LENGTH / 2;
	public static final int GATE_SIZE_HALF = GATE_SIZE / 2;

	public static void generate(final World world, final BlockPos blockPos) {
		final int x = blockPos.getX();
		final int y = blockPos.getY();
		final int z = blockPos.getZ();
		for (int length = -GATE_LENGTH_HALF; length < GATE_LENGTH_HALF; length++) {
			for (int newZ = z - GATE_SIZE_HALF; newZ <= z + GATE_SIZE_HALF; newZ++) {
				world.setBlockState(new BlockPos(x + (2 * length), y + GATE_SIZE_HALF, newZ), Blocks.BEDROCK.getDefaultState());
				world.setBlockState(new BlockPos(x + (2 * length), y - GATE_SIZE_HALF, newZ), Blocks.BEDROCK.getDefaultState());
			}

			for (int newY = y - GATE_SIZE_HALF; newY <= y + GATE_SIZE_HALF; newY++) {
				world.setBlockState(new BlockPos(x + (2 * length), newY, z + GATE_SIZE_HALF), Blocks.BEDROCK.getDefaultState());
				world.setBlockState(new BlockPos(x + (2 * length), newY, z - GATE_SIZE_HALF), Blocks.BEDROCK.getDefaultState());
			}
		}

		for (int length = -GATE_LENGTH; length < GATE_LENGTH; length++) {
			world.setBlockState(new BlockPos(x + length, y + GATE_SIZE_HALF, z), Blocks.BEDROCK.getDefaultState());
			world.setBlockState(new BlockPos(x + length, y - GATE_SIZE_HALF, z), Blocks.BEDROCK.getDefaultState());
			world.setBlockState(new BlockPos(x + length, y + GATE_SIZE_HALF, z + 1), Blocks.BEDROCK.getDefaultState());
			world.setBlockState(new BlockPos(x + length, y - GATE_SIZE_HALF, z - 1), Blocks.BEDROCK.getDefaultState());
			world.setBlockState(new BlockPos(x + length, y + GATE_SIZE_HALF, z + 2), Blocks.BEDROCK.getDefaultState());
			world.setBlockState(new BlockPos(x + length, y - GATE_SIZE_HALF, z - 2), Blocks.BEDROCK.getDefaultState());
			world.setBlockState(new BlockPos(x + length, y, z - GATE_SIZE_HALF), Blocks.BEDROCK.getDefaultState());
			world.setBlockState(new BlockPos(x + length, y, z + GATE_SIZE_HALF), Blocks.BEDROCK.getDefaultState());
			world.setBlockState(new BlockPos(x + length, y + 1, z - GATE_SIZE_HALF), Blocks.BEDROCK.getDefaultState());
			world.setBlockState(new BlockPos(x + length, y - 1, z + GATE_SIZE_HALF), Blocks.BEDROCK.getDefaultState());
			world.setBlockState(new BlockPos(x + length, y + 2, z - GATE_SIZE_HALF), Blocks.BEDROCK.getDefaultState());
			world.setBlockState(new BlockPos(x + length, y - 2, z + GATE_SIZE_HALF), Blocks.BEDROCK.getDefaultState());
		}
	}
}
