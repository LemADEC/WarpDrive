package cr0s.warpdrive.item;

import javax.swing.Icon;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IAirCanister;

public class ItemWarpAirCanister extends Item implements IAirCanister {
	
	private IIcon icon;
	
	public ItemWarpAirCanister() {
		super();
		setMaxDamage(20);
		setCreativeTab(WarpDrive.warpdriveTab);
		setMaxStackSize(1);
		setUnlocalizedName("warpdrive.items.AirCanisterFull");
	}
	
	@Override
	public void registerIcons(IIconRegister ir) {
		icon = ir.registerIcon("warpdrive:componentAirCanisterFull");
	}
	
	@Override
	public IIcon getIconFromDamage(int damage) {
		return icon;
	}

	@Override
	public ItemStack emptyDrop(ItemStack is) {
 		return WarpDrive.componentItem.getISNoCache(1, 8);
 	}
 
	@Override
	public ItemStack fullDrop(ItemStack can) {
		return new ItemStack(WarpDrive.airCanisterItem,1);
	}
	
	@Override
	public boolean canContainAir(ItemStack can) {
		if (can != null && can.getItem() instanceof ItemWarpAirCanister) {
			return can.getItemDamage() > 0;
		}
		return false;
	}

	@Override
	public boolean containsAir(ItemStack can) {
		return true;
	}
}
