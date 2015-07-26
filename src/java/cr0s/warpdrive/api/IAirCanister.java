package cr0s.warpdrive.api;

import net.minecraft.item.ItemStack;

public interface IAirCanister {
	public ItemStack emptyDrop(ItemStack can);
	public ItemStack fullDrop(ItemStack can);
	public boolean canContainAir(ItemStack can);
	public boolean containsAir(ItemStack can);
}
