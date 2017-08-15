package cr0s.warpdrive.config.structures;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IStringSerializable;

import java.util.Random;

import net.minecraft.world.World;

public class StructureGroup implements IStringSerializable {
	
	private static final String NONE = "-none-";
	protected String group;
	protected String name;
	
	public StructureGroup(final String group, final String name) {
		this.group = group == null || group.isEmpty() ? NONE : group;
		this.name = name;
	}
	
	@Override
	public String getName() {
		return group + ":" + (name == null || name.isEmpty() ? "*" : name);
	}
	
	public void generate(final World world, final Random random, final int x, final int y, final int z) {
		if (group.equals(NONE)) {
			return;
		}
		final AbstractStructure abstractStructure = StructureManager.getStructure(random, group, name);
		if (abstractStructure == null) {
			WarpDrive.logger.warn(String.format("Dimension %d @ %d %d %d refers to unknown structure %s. Probably a bad configuration. Skipping for now.",
			                                    world.provider.dimensionId, x, y, z, getName()));
			return;
		}
		final AbstractStructureInstance abstractStructureInstance = abstractStructure.instantiate(random);
		abstractStructureInstance.generate(world, random, x, y, z);
	}
	
	public String getGroup() {
		return group;
	}
	
	@Override
	public String toString() {
		return String.format("StructureGroup %s", getName());
	}
}
