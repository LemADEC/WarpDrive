package cr0s.warpdrive.item;

import java.util.List;

import cr0s.warpdrive.api.IControlChannel;
import cr0s.warpdrive.api.IWarpTool;
import cr0s.warpdrive.block.energy.BlockEnergyBank;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBeamFrequency;
import cr0s.warpdrive.api.IVideoChannel;

public class ItemTuningDriver extends Item implements IWarpTool {
	static final int MODE_VIDEO_CHANNEL = 0;
	static final int MODE_BEAM_FREQUENCY = 1;
	static final int MODE_CONTROL_CHANNEL = 2;
	
	private IIcon icons[];
	
	public ItemTuningDriver() {
		super();
		setMaxDamage(0);
		setCreativeTab(WarpDrive.creativeTabWarpDrive);
		setMaxStackSize(1);
		setUnlocalizedName("warpdrive.tool.tuning_driver");
		setFull3D();
	}
	
	@Override
	public void registerIcons(IIconRegister iconRegister) {
		icons = new IIcon[3];
		
		icons[MODE_VIDEO_CHANNEL  ] = iconRegister.registerIcon("warpdrive:tool/tuning_driver-cyan");
		icons[MODE_BEAM_FREQUENCY ] = iconRegister.registerIcon("warpdrive:tool/tuning_driver-purple");
		icons[MODE_CONTROL_CHANNEL] = iconRegister.registerIcon("warpdrive:tool/tuning_driver-yellow");
	}
	
	@Override
	public IIcon getIconFromDamage(int damage) {
		if (damage < icons.length) {
			return icons[damage];
		}
		return Blocks.fire.getFireIcon(0);
	}
	
	@Override
	public String getUnlocalizedName(ItemStack itemStack) {
		int damage = itemStack.getItemDamage();
		switch (damage) {
		case MODE_VIDEO_CHANNEL  : return getUnlocalizedName() + ".video_channel";
		case MODE_BEAM_FREQUENCY : return getUnlocalizedName() + ".beam_frequency";
		case MODE_CONTROL_CHANNEL: return getUnlocalizedName() + ".control_channel";
		default: return getUnlocalizedName(); 
		}
	}
	
	public static int getVideoChannel(ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof ItemTuningDriver)) {
			return -1;
		}
		if (!itemStack.hasTagCompound()) {
			return -1;
		}
		NBTTagCompound nbt = itemStack.getTagCompound();
		if (nbt.hasKey(IVideoChannel.VIDEO_CHANNEL_TAG)) {
			return nbt.getInteger(IVideoChannel.VIDEO_CHANNEL_TAG);
		}
		return -1;
	}
	
	public static ItemStack setVideoChannel(ItemStack itemStack, int videoChannel) {
		if (!(itemStack.getItem() instanceof ItemTuningDriver) || videoChannel == -1) {
			return itemStack;
		}
		NBTTagCompound nbt = itemStack.getTagCompound();
		if (nbt == null) {
			nbt = new NBTTagCompound();
		}
		nbt.setInteger(IVideoChannel.VIDEO_CHANNEL_TAG, videoChannel);
		itemStack.setTagCompound(nbt);
		return itemStack;
	}
	
	public static int getBeamFrequency(ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof ItemTuningDriver)) {
			return -1;
		}
		if (!itemStack.hasTagCompound()) {
			return -1;
		}
		NBTTagCompound nbt = itemStack.getTagCompound();
		if (nbt.hasKey(IBeamFrequency.BEAM_FREQUENCY_TAG)) {
			return nbt.getInteger(IBeamFrequency.BEAM_FREQUENCY_TAG);
		}
		return -1;
	}
	
	public static ItemStack setBeamFrequency(ItemStack itemStack, int beamFrequency) {
		if (!(itemStack.getItem() instanceof ItemTuningDriver) || beamFrequency == -1) {
			return itemStack;
		}
		NBTTagCompound nbt = itemStack.getTagCompound();
		if (nbt == null) {
			nbt = new NBTTagCompound();
		}
		nbt.setInteger(IBeamFrequency.BEAM_FREQUENCY_TAG, beamFrequency);
		itemStack.setTagCompound(nbt);
		return itemStack;
	}
	
	public static int getControlChannel(ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof ItemTuningDriver)) {
			return -1;
		}
		if (!itemStack.hasTagCompound()) {
			return -1;
		}
		NBTTagCompound nbt = itemStack.getTagCompound();
		if (nbt.hasKey(IControlChannel.CONTROL_CHANNEL_TAG)) {
			return nbt.getInteger(IControlChannel.CONTROL_CHANNEL_TAG);
		}
		return -1;
	}
	
	public static ItemStack setControlChannel(ItemStack itemStack, int controlChannel) {
		if (!(itemStack.getItem() instanceof ItemTuningDriver) || controlChannel == -1) {
			return itemStack;
		}
		NBTTagCompound nbt = itemStack.getTagCompound();
		if (nbt == null) {
			nbt = new NBTTagCompound();
		}
		nbt.setInteger(IControlChannel.CONTROL_CHANNEL_TAG, controlChannel);
		itemStack.setTagCompound(nbt);
		return itemStack;
	}
	
	@Override
	public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer entityPlayer) {
		if (world.isRemote || !(itemStack.getItem() instanceof ItemTuningDriver)) {
			return itemStack;
		}
		if (entityPlayer.isSneaking()) {
			switch (itemStack.getItemDamage()) {
			case MODE_VIDEO_CHANNEL:
				setVideoChannel(itemStack, world.rand.nextInt(IVideoChannel.VIDEO_CHANNEL_MAX));
				WarpDrive.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted("warpdrive.video_channel.get",
						getVideoChannel(itemStack)));
				return itemStack;
			
			case MODE_BEAM_FREQUENCY:
				setBeamFrequency(itemStack, world.rand.nextInt(IBeamFrequency.BEAM_FREQUENCY_MAX));
				WarpDrive.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted("warpdrive.beam_frequency.get",
						getBeamFrequency(itemStack)));
				return itemStack;
			
			case MODE_CONTROL_CHANNEL:
				setControlChannel(itemStack, world.rand.nextInt(IControlChannel.CONTROL_CHANNEL_MAX));
				WarpDrive.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted("warpdrive.control_channel.get",
						getControlChannel(itemStack)));
				return itemStack;
			
			default:
				return itemStack;
			}
			
		} else {
			switch (itemStack.getItemDamage()) {
			case MODE_VIDEO_CHANNEL:
				itemStack.setItemDamage(MODE_BEAM_FREQUENCY);
				entityPlayer.setCurrentItemOrArmor(0, itemStack);
				break;
				
			case MODE_BEAM_FREQUENCY:
				itemStack.setItemDamage(MODE_CONTROL_CHANNEL);
				entityPlayer.setCurrentItemOrArmor(0, itemStack);
				break;
			
			case MODE_CONTROL_CHANNEL:
				itemStack.setItemDamage(MODE_VIDEO_CHANNEL);
				entityPlayer.setCurrentItemOrArmor(0, itemStack);
				break;
			
			default:
				itemStack.setItemDamage(MODE_VIDEO_CHANNEL);
				break;
			}
			world.playSoundAtEntity(entityPlayer, "WarpDrive:ding", 0.1F, 1F);
			return itemStack;
		}
	}
	
	@Override
	public boolean onItemUse(ItemStack itemStack, EntityPlayer entityPlayer, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
		if (world.isRemote) {
			return false;
		}
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (tileEntity == null) {
			return false;
		}
		
		switch (itemStack.getItemDamage()) {
		case MODE_VIDEO_CHANNEL:
			if (tileEntity instanceof IVideoChannel) {
				if (entityPlayer.isSneaking()) {
					setVideoChannel(itemStack, ((IVideoChannel) tileEntity).getVideoChannel());
					WarpDrive.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted("warpdrive.video_channel.get",
							tileEntity.getBlockType().getLocalizedName(),
							getVideoChannel(itemStack)));
				} else {
					((IVideoChannel) tileEntity).setVideoChannel(getVideoChannel(itemStack));
					WarpDrive.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted("warpdrive.video_channel.set",
							tileEntity.getBlockType().getLocalizedName(),
							getVideoChannel(itemStack)));
				}
				return true;
			}
			return false;
			
		case MODE_BEAM_FREQUENCY:
			if (tileEntity instanceof IBeamFrequency) {
				if (entityPlayer.isSneaking()) {
					setBeamFrequency(itemStack, ((IBeamFrequency) tileEntity).getBeamFrequency());
					WarpDrive.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted("warpdrive.beam_frequency.get",
							tileEntity.getBlockType().getLocalizedName(),
							getBeamFrequency(itemStack)));
				} else {
					((IBeamFrequency) tileEntity).setBeamFrequency(getBeamFrequency(itemStack));
					WarpDrive.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted("warpdrive.beam_frequency.set",
							tileEntity.getBlockType().getLocalizedName(),
							getBeamFrequency(itemStack)));
				}
				return true;
			}
			return false;
		
		case MODE_CONTROL_CHANNEL:
			if (tileEntity instanceof IControlChannel) {
				if (entityPlayer.isSneaking()) {
					setControlChannel(itemStack, ((IControlChannel) tileEntity).getControlChannel());
					WarpDrive.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted("warpdrive.control_channel.get",
							tileEntity.getBlockType().getLocalizedName(),
							getControlChannel(itemStack)));
				} else {
					((IControlChannel) tileEntity).setControlChannel(getControlChannel(itemStack));
					WarpDrive.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted("warpdrive.control_channel.set",
							tileEntity.getBlockType().getLocalizedName(),
							getControlChannel(itemStack)));
				}
				return true;
			}
			return false;
		
		default:
			return false;
		}
	}
	
	@Override
	public boolean doesSneakBypassUse(World world, int x, int y, int z, EntityPlayer player) {
		Block block = world.getBlock(x, y, z);
		return block instanceof BlockEnergyBank || super.doesSneakBypassUse(world, x, y, z, player);
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean advancedItemTooltips) {
		super.addInformation(itemStack, entityPlayer, list, advancedItemTooltips);
		
		String tooltip;
		switch (itemStack.getItemDamage()) {
		case MODE_VIDEO_CHANNEL:
			tooltip = StatCollector.translateToLocalFormatted("warpdrive.video_channel.tooltip", getVideoChannel(itemStack));
			break;
		case MODE_BEAM_FREQUENCY:
			tooltip = StatCollector.translateToLocalFormatted("warpdrive.beam_frequency.tooltip", getBeamFrequency(itemStack));
			break;
		case MODE_CONTROL_CHANNEL:
			tooltip = StatCollector.translateToLocalFormatted("warpdrive.control_channel.tooltip", getControlChannel(itemStack));
			break;
		default:
			tooltip = "I'm broken :(";
			break;
		}
		
		tooltip += "\n" + StatCollector.translateToLocal("item.warpdrive.tool.tuning_driver.tooltip.usage");
		
		WarpDrive.addTooltip(list, tooltip);
	}
}