package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.block.BlockAbstractContainer;
import cr0s.warpdrive.data.EnumComponentType;
import cr0s.warpdrive.data.EnumShipCoreState;
import cr0s.warpdrive.item.ItemComponent;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraftforge.common.util.ForgeDirection;

public class BlockShipCore extends BlockAbstractContainer {
	
	@SideOnly(Side.CLIENT)
	private IIcon[] iconBuffer;
	
	private static final int ICON_TOP_BOTTOM        = 0;
	private static final int ICON_LEFT_OFFLINE      = 1;
	private static final int ICON_LEFT_ONLINE       = 2;
	private static final int ICON_LEFT_ACTIVE       = 3;
	private static final int ICON_LEFT_COOLING      = 4;
	private static final int ICON_RIGHT_OFFLINE     = 5;
	private static final int ICON_RIGHT_ONLINE      = 6;
	private static final int ICON_RIGHT_ACTIVE      = 7;
	private static final int ICON_RIGHT_COOLING     = 8;
	private static final int ICON_CONTROL           = 9;
	
	public BlockShipCore() {
		super(Material.iron);
		setBlockName("warpdrive.movement.ShipCore");
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void registerBlockIcons(IIconRegister iconRegister) {
		iconBuffer = new IIcon[18];
		iconBuffer[ICON_TOP_BOTTOM    ] = iconRegister.registerIcon("warpdrive:movement/ship_core-top-bottom");
		iconBuffer[ICON_LEFT_OFFLINE  ] = iconRegister.registerIcon("warpdrive:movement/ship_core-left_offline");
		iconBuffer[ICON_LEFT_ONLINE   ] = iconRegister.registerIcon("warpdrive:movement/ship_core-left_online");
		iconBuffer[ICON_LEFT_ACTIVE   ] = iconRegister.registerIcon("warpdrive:movement/ship_core-left_active");
		iconBuffer[ICON_LEFT_COOLING  ] = iconRegister.registerIcon("warpdrive:movement/ship_core-left_cooling");
		iconBuffer[ICON_RIGHT_OFFLINE ] = iconRegister.registerIcon("warpdrive:movement/ship_core-right_offline");
		iconBuffer[ICON_RIGHT_ONLINE  ] = iconRegister.registerIcon("warpdrive:movement/ship_core-right_online");
		iconBuffer[ICON_RIGHT_ACTIVE  ] = iconRegister.registerIcon("warpdrive:movement/ship_core-right_active");
		iconBuffer[ICON_RIGHT_COOLING ] = iconRegister.registerIcon("warpdrive:movement/ship_core-right_cooling");
		iconBuffer[ICON_CONTROL    ] = iconRegister.registerIcon("warpdrive:movement/ship_controller-side_inactive");
		iconBuffer[ICON_CONTROL + 1] = iconRegister.registerIcon("warpdrive:movement/ship_controller-side_active0");
		iconBuffer[ICON_CONTROL + 2] = iconRegister.registerIcon("warpdrive:movement/ship_controller-side_active1");
		iconBuffer[ICON_CONTROL + 3] = iconRegister.registerIcon("warpdrive:movement/ship_controller-side_active2");
		iconBuffer[ICON_CONTROL + 4] = iconRegister.registerIcon("warpdrive:movement/ship_controller-side_active3");
		iconBuffer[ICON_CONTROL + 5] = iconRegister.registerIcon("warpdrive:movement/ship_controller-side_active4");
		iconBuffer[ICON_CONTROL + 6] = iconRegister.registerIcon("warpdrive:movement/ship_controller-side_active5");
		iconBuffer[ICON_CONTROL + 7] = iconRegister.registerIcon("warpdrive:movement/ship_controller-side_active6");
		iconBuffer[ICON_CONTROL + 8] = iconRegister.registerIcon("warpdrive:movement/ship_controller-side_active7");
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public IIcon getIcon(final IBlockAccess blockAccess, final int x, final int y, final int z, final int side) {
		if (side == 0 || side == 1) {
			return iconBuffer[ICON_TOP_BOTTOM];
		}
		
		// get orientation
		final TileEntity tileEntity = blockAccess.getTileEntity(x, y, z);
		final ForgeDirection facingTile;
		if (tileEntity instanceof TileEntityShipCore) {
			facingTile = ((TileEntityShipCore) tileEntity).facing;
		} else {
			facingTile = ForgeDirection.DOWN;
		}
		if (facingTile == ForgeDirection.DOWN || facingTile == ForgeDirection.UP) {
			return null;
		}
		final ForgeDirection facingSide = ForgeDirection.getOrientation(side);
		
		// return side texture
		final int metadata = blockAccess.getBlockMetadata(x, y, z);
		final EnumShipCoreState shipCoreState = EnumShipCoreState.get(metadata);
		if (facingTile == facingSide || facingTile == facingSide.getOpposite()) {
			return iconBuffer[ICON_CONTROL + metadata];
		} else if (facingTile == facingSide.getRotation(ForgeDirection.UP)) {
			switch (shipCoreState) {
			default:
			case DISCONNECTED: return iconBuffer[ICON_LEFT_OFFLINE];
			case IDLE        : return iconBuffer[ICON_LEFT_ONLINE];
			case SCANNING    : return iconBuffer[ICON_LEFT_ACTIVE];
			case ONLINE      : return iconBuffer[ICON_LEFT_ACTIVE];
			case WARMING_UP  : return iconBuffer[ICON_LEFT_ACTIVE];
			case COOLING_DOWN: return iconBuffer[ICON_LEFT_COOLING];
			}
		} else {
			switch (shipCoreState) {
			default:
			case DISCONNECTED: return iconBuffer[ICON_RIGHT_OFFLINE];
			case IDLE        : return iconBuffer[ICON_RIGHT_ONLINE];
			case SCANNING    : return iconBuffer[ICON_RIGHT_ACTIVE];
			case ONLINE      : return iconBuffer[ICON_RIGHT_ACTIVE];
			case WARMING_UP  : return iconBuffer[ICON_RIGHT_ACTIVE];
			case COOLING_DOWN: return iconBuffer[ICON_RIGHT_COOLING];
			}
		}
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public IIcon getIcon(final int side, final int metadata) {
		if (side == 0 || side == 1) {
			return iconBuffer[ICON_TOP_BOTTOM];
		}
		
		if (side == 3) {
			return iconBuffer[ICON_CONTROL + 3];
		}
		return iconBuffer[ICON_RIGHT_ONLINE];
	}
	
	@Override
	public TileEntity createNewTileEntity(World var1, int i) {
		return new TileEntityShipCore();
	}
	
	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entityLiving, ItemStack itemStack) {
		super.onBlockPlacedBy(world, x, y, z, entityLiving, itemStack);
		
		final TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (tileEntity instanceof TileEntityShipCore) {
			((TileEntityShipCore) tileEntity).facing = Commons.getHorizontalDirectionFromEntity(entityLiving).getOpposite();
			world.markBlockForUpdate(x, y, z);
		}
	}
	
	@Override
	public int quantityDropped(Random par1Random) {
		return 1;
	}
	
	@Override
	public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (tileEntity instanceof TileEntityShipCore) {
			if (((TileEntityShipCore)tileEntity).jumpCount == 0) {
				return super.getDrops(world, x, y, z, metadata, fortune);
			}
		}
		// trigger explosion
		EntityTNTPrimed entityTNTPrimed = new EntityTNTPrimed(world, x + 0.5F, y + 0.5F, z + 0.5F, null);
		entityTNTPrimed.fuse = 10 + world.rand.nextInt(10);
		world.spawnEntityInWorld(entityTNTPrimed);
		
		// get a chance to get the drops
		ArrayList<ItemStack> itemStacks = new ArrayList<>();
		itemStacks.add(ItemComponent.getItemStackNoCache(EnumComponentType.CAPACITIVE_CRYSTAL, 1));
		if (fortune > 0 && world.rand.nextBoolean()) {
			itemStacks.add(ItemComponent.getItemStackNoCache(EnumComponentType.CAPACITIVE_CRYSTAL, 1));
		}
		if (fortune > 1 && world.rand.nextBoolean()) {
			itemStacks.add(ItemComponent.getItemStackNoCache(EnumComponentType.CAPACITIVE_CRYSTAL, 1));
		}
		if (fortune > 1 & world.rand.nextBoolean()) {
			itemStacks.add(ItemComponent.getItemStackNoCache(EnumComponentType.POWER_INTERFACE, 1));
		}
		return itemStacks;
	}
	
	@Override
	public float getPlayerRelativeBlockHardness(EntityPlayer entityPlayer, World world, int x, int y, int z) {
		boolean willBreak = true;
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (tileEntity instanceof TileEntityShipCore) {
			if (((TileEntityShipCore)tileEntity).jumpCount == 0) {
				willBreak = false;
			}
		}
		return (willBreak ? 0.02F : 1.0F) * super.getPlayerRelativeBlockHardness(entityPlayer, world, x, y, z);
	}
	
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer entityPlayer, int side, float hitX, float hitY, float hitZ) {
		if (entityPlayer.getHeldItem() == null) {
			final TileEntity tileEntity = world.getTileEntity(x, y, z);
			if (tileEntity instanceof TileEntityShipCore) {
				final TileEntityShipCore tileEntityShipCore = (TileEntityShipCore) tileEntity;
				if ( world.isRemote
				  && entityPlayer.isSneaking() ) {
					tileEntityShipCore.showBoundingBox = !tileEntityShipCore.showBoundingBox;
					if (tileEntityShipCore.showBoundingBox) {
						world.playSound(x + 0.5D, y + 0.5D, z + 0.5D, "warpdrive:lowlaser", 4.0F, 2.0F, false);
					} else {
						world.playSound(x + 0.5D, y + 0.5D, z + 0.5D, "warpdrive:lowlaser", 4.0F, 1.4F, false);
					}
					Commons.addChatMessage(entityPlayer, tileEntityShipCore.getBoundingBoxStatus());
					return true;
				} else if ( !world.isRemote
				         && !entityPlayer.isSneaking() ) {
					Commons.addChatMessage(entityPlayer, tileEntityShipCore.getStatus());
					return true;
				}
			}
		}
		
		return false;
	}
}