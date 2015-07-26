package cr0s.warpdrive.item;

import ic2.api.reactor.IReactor;
import ic2.api.reactor.IReactorComponent;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import cr0s.warpdrive.WarpDrive;

public class ItemReactorLaserFocus extends Item implements IReactorComponent
{
	private final static int maxHeat = 3000;
	
	public ItemReactorLaserFocus()
	{
		super();
		setMaxDamage(maxHeat);
		setCreativeTab(WarpDrive.warpdriveTab);
		setUnlocalizedName("warpdrive.items.ReactorLaserFocus");
	}
	
	@Override
    public void registerIcons(IIconRegister par1IconRegister)
    {
        this.itemIcon = par1IconRegister.registerIcon("warpdrive:reactorFocus");
    }
	
	private static void damageComponent(ItemStack self,int damage)
	{
		//WarpDrive.debugPrint("ReactorCompDamage:" + damage);
		int currDamage = self.getItemDamage();
		int nextDamage = Math.min(maxHeat,Math.max(0, currDamage + damage));
		self.setItemDamage(nextDamage);
	}
	
	private static void balanceComponent(ItemStack self, ItemStack other)
	{
		final int selfBalance = 4;
		int otherDamage = other.getItemDamage();
		int myDamage = self.getItemDamage();
		int newOne = (otherDamage + (selfBalance-1)*myDamage) / selfBalance;
		int newTwo = otherDamage - (newOne - myDamage);
		self.setItemDamage(newTwo);
		other.setItemDamage(newOne);
	}
	
	private static void coolComponent(ItemStack self, IReactorComponent comp, IReactor reactor, ItemStack stack, int x, int y)
	{
		int maxTransfer = maxHeat - self.getItemDamage();
		int compHeat = comp.getCurrentHeat(reactor, stack, x, y);
		int transferHeat = - Math.min(compHeat,maxTransfer);
		int retained = comp.alterHeat(reactor, stack, x, y, transferHeat);
		damageComponent(self,retained - transferHeat);
	}
	
	private static void coolReactor(IReactor reactor,ItemStack stack)
	{
		int reactorHeat = reactor.getHeat();
		int myHeat = stack.getItemDamage();
		int transfer = Math.min(maxHeat - myHeat,reactorHeat);
		reactor.addHeat(-transfer);
		damageComponent(stack,transfer);
	}

	@Override
	public void processChamber(IReactor reactor, ItemStack yourStack, int x, int y, boolean heatrun)
	{
		if(heatrun)
		{
			int[] xDif = {-1,0,0,1};
			int[] yDif = {0,-1,1,0};
			for(int i=0;i<xDif.length;i++)
			{
				int iX = x + xDif[i];
				int iY = y + yDif[i];
				ItemStack stack = reactor.getItemAt(iX,iY);
				if(stack != null)
				{
					Item item = stack.getItem();
					if(item instanceof ItemReactorLaserFocus)
						balanceComponent(yourStack,stack);
					else if(item instanceof IReactorComponent)
						coolComponent(yourStack,(IReactorComponent) item, reactor, stack, iX, iY);
				}
			}
			
			coolReactor(reactor,yourStack);
		}
	}

	@Override
	public boolean acceptUraniumPulse(IReactor reactor, ItemStack yourStack,ItemStack pulsingStack,
			int youX, int youY, int pulseX, int pulseY, boolean heatrun)
	{
		return false;
	}

	@Override
	public boolean canStoreHeat(IReactor reactor, ItemStack yourStack, int x, int y)
	{
		return true;
	}

	@Override
	public int getMaxHeat(IReactor reactor, ItemStack yourStack, int x, int y)
	{
		return maxHeat;
	}

	@Override
	public int getCurrentHeat(IReactor reactor, ItemStack yourStack, int x, int y)
	{
		return yourStack.getItemDamage();
	}

	@Override
	public int alterHeat(IReactor reactor, ItemStack yourStack, int x, int y, int heat)
	{
		//WarpDrive.debugPrint("ReactorLaserAlter:" + heat);
		int transferred = Math.min(heat, maxHeat - yourStack.getItemDamage());
		damageComponent(yourStack,transferred);
		return heat - transferred;
	}

	@Override
	public float influenceExplosion(IReactor reactor, ItemStack yourStack)
	{
		return 0;
	}

}
