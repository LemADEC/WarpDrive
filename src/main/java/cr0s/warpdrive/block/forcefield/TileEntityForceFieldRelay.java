package cr0s.warpdrive.block.forcefield;

import cr0s.warpdrive.api.IForceFieldUpgrade;
import cr0s.warpdrive.api.IForceFieldUpgradeEffector;
import cr0s.warpdrive.data.EnumForceFieldUpgrade;
import cr0s.warpdrive.data.ForceFieldSetup;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.util.StatCollector;

public class TileEntityForceFieldRelay extends TileEntityAbstractForceField implements IForceFieldUpgrade {

	// persistent properties
	private EnumForceFieldUpgrade upgrade = EnumForceFieldUpgrade.NONE;
	
	public TileEntityForceFieldRelay() {
		super();
		
		peripheralName = "warpdriveForceFieldRelay";
	}
	
	// onFirstUpdateTick
	// updateEntity
	
	protected EnumForceFieldUpgrade getUpgrade() {
		if (upgrade == null) {
			return EnumForceFieldUpgrade.NONE;
		}
		return upgrade;
	}
	
	protected void setUpgrade(EnumForceFieldUpgrade upgrade) {
		this.upgrade = upgrade;
		markDirty();
	}
	
	@Override
	public String getUpgradeStatus() {
		EnumForceFieldUpgrade enumForceFieldUpgrade = getUpgrade();
		String strDisplayName = StatCollector.translateToLocalFormatted("warpdrive.forcefield.upgrade.statusLine." + enumForceFieldUpgrade.unlocalizedName);
		if (enumForceFieldUpgrade == EnumForceFieldUpgrade.NONE) {
			return StatCollector.translateToLocalFormatted("warpdrive.upgrade.statusLine.none",
				strDisplayName);
		} else {
			return StatCollector.translateToLocalFormatted("warpdrive.upgrade.statusLine.valid",
				strDisplayName);
		}
	}
	
	public String getStatus() {
		return super.getStatus()
			       + "\n" + getUpgradeStatus();
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		setUpgrade(EnumForceFieldUpgrade.get(tagCompound.getByte("upgrade")));
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
		tagCompound.setByte("upgrade", (byte) getUpgrade().ordinal());
	}
	
	@Override
	public Packet getDescriptionPacket() {
		final NBTTagCompound tagCompound = new NBTTagCompound();
		writeToNBT(tagCompound);
		return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tagCompound);
	}
	
	@Override
	public void onDataPacket(NetworkManager networkManager, S35PacketUpdateTileEntity packet) {
		final NBTTagCompound tagCompound = packet.func_148857_g();
		readFromNBT(tagCompound);
	}
	
	@Override
	public IForceFieldUpgradeEffector getUpgradeEffector() {
		return isEnabled ? getUpgrade() : null;
	}
	
	@Override
	public float getUpgradeValue() {
		return isEnabled ? getUpgrade().getUpgradeValue() * (1.0F + (tier - 1) * ForceFieldSetup.FORCEFIELD_UPGRADE_BOOST_FACTOR_PER_RELAY_TIER) : 0.0F;
	}
}
