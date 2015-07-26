package cr0s.warpdrive.item;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IAirCanister;
import cr0s.warpdrive.api.IBreathingHelmet;

public class ItemWarpArmor extends ItemArmor implements IBreathingHelmet {
	// private static Random ran = new Random();
	private int slot;

	IIcon ic;

	public ItemWarpArmor(ArmorMaterial mat, int slot) {
		super(mat, 0, slot);
		this.slot = slot;
		setUnlocalizedName("warpdrive.armor.Helmet");
		setCreativeTab(WarpDrive.warpdriveTab);
	}

	@Override
	public String getArmorTexture(ItemStack is, Entity en, int parSlot, String type) {
		return "warpdrive:textures/armor/warpArmor_1.png";
	}

	@Override
	public void registerIcons(IIconRegister ir) {
		if (slot == 0) {
			ic = ir.registerIcon("warpdrive:warpArmorHelmet");
		}
	}

	@Override
	public IIcon getIconFromDamage(int damage) {
		return ic;
	}

	@Override
	public boolean canBreath(Entity player) {
		return true;
	}

	@Override
	public boolean removeAir(Entity player) {
		WarpDrive.debugPrint("Checking breathing!");
		if (player instanceof EntityPlayerMP) {
			EntityPlayerMP pl = (EntityPlayerMP) player;
			ItemStack[] plInv = pl.inventory.mainInventory;
			for (int i = 0; i < plInv.length; i++) {
				ItemStack is = plInv[i];
				if (is != null && is.getItem() instanceof IAirCanister) {
					IAirCanister airCanister = (IAirCanister) is.getItem();
					if (airCanister.containsAir(is)) {
						if (is.stackSize > 1) {// unstack
							is.stackSize--;
							ItemStack toAdd = is.copy();
							toAdd.stackSize = 1;
							toAdd.setItemDamage(is.getItemDamage() + 1); // bypass
							// unbreaking
							// enchantment
							if (is.getItemDamage() >= is.getMaxDamage()) {
								toAdd = airCanister.emptyDrop(is);
							}
							if (!pl.inventory.addItemStackToInventory(toAdd)) {
								EntityItem ie = new EntityItem(pl.worldObj, pl.posX, pl.posY, pl.posZ, toAdd);
								pl.worldObj.spawnEntityInWorld(ie);
							}
						} else {
							is.setItemDamage(is.getItemDamage() + 1); // bypass
							// unbreaking
							// enchantment
							if (is.getItemDamage() >= is.getMaxDamage()) {
								plInv[i] = airCanister.emptyDrop(is);
							}
						}
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public int ticksPerCanDamage() {
		return 40;
	}
}
