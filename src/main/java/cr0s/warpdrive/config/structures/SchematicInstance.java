package cr0s.warpdrive.config.structures;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.Filler;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.config.structures.Schematic.Insertion;
import cr0s.warpdrive.config.structures.Schematic.Replacement;
import cr0s.warpdrive.data.JumpBlock;
import cr0s.warpdrive.data.JumpShip;
import cr0s.warpdrive.world.WorldGenStructure;

import javax.annotation.Nonnull;
import java.util.Random;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SchematicInstance extends AbstractStructureInstance {
	
	protected JumpShip jumpShip;
	protected Replacement[] replacements;
	protected Insertion[] insertions;
	
	public SchematicInstance(final Schematic schematic, final Random random) {
		super(schematic, random);
		
		final WarpDriveText reason = new WarpDriveText();
		jumpShip = JumpShip.createFromFile(schematic.getRandomFileName(random), reason);
		if (jumpShip == null) {
			WarpDrive.logger.error(String.format("Failed to instantiate schematic structure %s due to %s",
			                                     schematic.getFullName(), reason));
			return;
		}
		
		replacements = new Replacement[schematic.replacements.length];
		int replacementIndexOut = 0;
		for (int replacementIndexIn = 0; replacementIndexIn < schematic.replacements.length; replacementIndexIn++) {
			final Replacement replacement = schematic.replacements[replacementIndexIn].instantiate(random);
			if (replacement != null) {
				replacements[replacementIndexOut] = replacement;
				replacementIndexOut++;
			}
		}
		
		insertions = new Insertion[schematic.insertions.length];
		int insertionIndexOut = 0;
		for (int insertionIndexIn = 0; insertionIndexIn < schematic.insertions.length; insertionIndexIn++) {
			final Insertion insertion = schematic.insertions[insertionIndexIn].instantiate(random);
			if (insertion != null) {
				insertions[insertionIndexOut] = insertion;
				insertionIndexOut++;
			}
		}
	}
	
	public SchematicInstance(final CompoundNBT tagCompound) {
		super(tagCompound);
		// TODO not implemented
	}
	
	@Override
	public CompoundNBT write(@Nonnull final CompoundNBT tagCompound) {
		super.write(tagCompound);
		// TODO not implemented
		return tagCompound;
	}
	
	@Override
	public boolean place(@Nonnull final World world, @Nonnull final Random random, @Nonnull final BlockPos blockPos) {
		if (jumpShip == null) {
			return false;
		}
		
		for (final Replacement replacement : this.replacements) {
			// Pick a common replacement block to get an homogenous result
			final Filler filler = replacement.getRandomUnit(random);
			
			// loop through the structure and see if a block need to be replaced
			for (int i = 0; i < jumpShip.jumpBlocks.length; i++) {
				if (replacement.isMatching(jumpShip.jumpBlocks[i])) {
					jumpShip.jumpBlocks[i] = new JumpBlock(filler,
					                                       jumpShip.jumpBlocks[i].x,
					                                       jumpShip.jumpBlocks[i].y,
					                                       jumpShip.jumpBlocks[i].z );
				}
			}
		}
		
		final int y2 = Commons.clamp(
				WarpDriveConfig.SPACE_GENERATOR_Y_MIN_BORDER + (jumpShip.core.getY() - jumpShip.minY),
				WarpDriveConfig.SPACE_GENERATOR_Y_MAX_BORDER - (jumpShip.maxY - jumpShip.core.getY()),
				blockPos.getY() );
		new WorldGenStructure(random.nextFloat() < 0.2F, random).deployShip(world, jumpShip, blockPos.getX(), y2, blockPos.getZ(), (byte) 0, insertions);
		return true;
	}
}
