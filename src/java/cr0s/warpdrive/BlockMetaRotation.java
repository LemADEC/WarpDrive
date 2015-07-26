package cr0s.warpdrive;

import net.minecraft.block.Block;

public class BlockMetaRotation {
	public final Block block;
	public final int[] metaRotation;
	private int bitMask;

	protected BlockMetaRotation(Block block, int[] metarotation, int bitmask) {
		if (metarotation.length != 4) {
			throw new IllegalArgumentException("MetaRotation int array must have length 4");
		} else {
			this.block = block;
			this.metaRotation = metarotation;
			this.bitMask = bitmask;
		}
	}

	public int getRotatedMeta(int currentmeta, int rotate) {
		for (int i = 0; i < this.metaRotation.length; ++i) {
			if (this.metaRotation[i] == (currentmeta & this.bitMask)) {
				int mr = currentmeta & ~this.bitMask | this.metaRotation[wrapRotationIndex(i + rotate)] & this.bitMask;
				return mr;
			}
		}

		return currentmeta;
	}

	public static int wrapRotationIndex(int i) {
		return i & 3;
	}
}
