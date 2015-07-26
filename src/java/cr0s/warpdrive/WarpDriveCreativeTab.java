package cr0s.warpdrive;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class WarpDriveCreativeTab extends CreativeTabs {
	String topLabel;

	public WarpDriveCreativeTab(int par1, String par2Str) {
		super(par1, par2Str);
	}
	
	public WarpDriveCreativeTab(String par1Str,String topLabelIn) {
		super(par1Str);
		topLabel = topLabelIn;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public Item getTabIconItem() {
		return WarpDrive.componentItem;
        // return WarpDrive.reactorLaserFocusItem;
    }
	
	@Override
	public String getTranslatedTabLabel() {
        return topLabel;
    }
}
