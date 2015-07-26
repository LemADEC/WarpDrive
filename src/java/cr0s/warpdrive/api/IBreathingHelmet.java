package cr0s.warpdrive.api;

import net.minecraft.entity.Entity;

public interface IBreathingHelmet {
	public boolean canBreath(Entity player);
	public boolean removeAir(Entity player);
	public int ticksPerCanDamage();
}