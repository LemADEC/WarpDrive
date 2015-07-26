package cr0s.warpdrive.block;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.oredict.ShapedOreRecipe;
import cpw.mods.fml.common.registry.GameRegistry;
import cr0s.warpdrive.WarpDrive;

public class BlockDecorative extends Block {
	public static enum decorativeTypes {
		Plain, Energized, Network
	};

	private ItemStack[] isCache = new ItemStack[decorativeTypes.values().length];
	private IIcon[] iconBuffer = new IIcon[decorativeTypes.values().length];

	public BlockDecorative() {
		super(Material.iron);
		setHardness(0.5f);
		setStepSound(Block.soundTypeMetal);
		setCreativeTab(WarpDrive.warpdriveTab);
	}

	public boolean isValidDamage(int damage) {
		return damage >= 0 && damage < decorativeTypes.values().length;
	}

	@Override
	public void getSubBlocks(Item par1, CreativeTabs par2CreativeTabs, List par3List) {
		for (decorativeTypes val : decorativeTypes.values())
			par3List.add(new ItemStack(par1, 1, val.ordinal()));
	}

	@Override
	public void registerBlockIcons(IIconRegister ir) {
		for (decorativeTypes val : decorativeTypes.values())
			iconBuffer[val.ordinal()] = ir.registerIcon("warpdrive:decorative" + val.toString());
	}

	@Override
	public IIcon getIcon(int side, int damage) {
		if (isValidDamage(damage))
			return iconBuffer[damage];
		return iconBuffer[0];
	}

	@Override
	public int damageDropped(int damage) {
		return damage;
	}

	public ItemStack getIS(int damage) {
		if (!isValidDamage(damage))
			return null;

		if (isCache[damage] == null)
			isCache[damage] = getISNoCache(damage);
		return isCache[damage];
	}

	public ItemStack getISNoCache(int damage, int amount) {
		if (!isValidDamage(damage))
			return null;

		return new ItemStack(WarpDrive.decorativeBlock, amount, damage);
	}

	public ItemStack getISNoCache(int damage) {
		return getISNoCache(damage, 1);
	}

	public void initRecipes() {
		GameRegistry.addRecipe(new ShapedOreRecipe(getISNoCache(0, 8), false, "sss", "scs", "sss", 's', Blocks.stone, 'c', WarpDrive.componentItem.getIS(0)));

		GameRegistry.addRecipe(new ShapedOreRecipe(getISNoCache(2, 8), false, "sss", "scs", "sss", 's', getIS(0), 'c', WarpDrive.componentItem.getIS(5)));
	}

}
