package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.api.computer.ICoreSignature;
import cr0s.warpdrive.api.computer.ITransporterBeacon;
import cr0s.warpdrive.block.TileEntityAbstractEnergyConsumer;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnergyWrapper;
import cr0s.warpdrive.data.GlobalRegion;
import cr0s.warpdrive.data.GlobalRegionManager;
import cr0s.warpdrive.data.EnumGlobalRegionType;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;
import java.util.UUID;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.WorldServer;

import net.minecraftforge.fml.common.Optional;

public class TileEntityTransporterBeacon extends TileEntityAbstractEnergyConsumer implements ITransporterBeacon {
	
	// persistent properties
	private String nameTransporterCore;
	private UUID uuidTransporterCore;
	private int tickDeploying = 0;
	
	// computed properties
	private boolean isActive = false;
	protected String stateTransporter = "";
	
	public TileEntityTransporterBeacon() {
		super();
		
		isEnergyLostWhenBroken = false;
		
		peripheralName = "warpdriveTransporterBeacon";
		addMethods(new String[] {
				"isActive"
		});
		doRequireUpgradeToInterface();
	}
	
	@Override
	protected void onConstructed() {
		super.onConstructed();
		
		energy_setParameters(WarpDriveConfig.TRANSPORTER_BEACON_MAX_ENERGY_STORED,
		                     1024, 0,
		                     "MV", 2, "MV", 0);
	}
	
	@Override
	public void update() {
		final IBlockState blockState_actual = world.getBlockState(pos);
		if (isInvalidBlockState(blockState_actual, BlockTransporterBeacon.class, BlockProperties.ACTIVE, BlockTransporterBeacon.DEPLOYED)) {
			return;
		}

		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		// deploy
		final boolean isDeployed = tickDeploying > WarpDriveConfig.TRANSPORTER_BEACON_DEPLOYING_DELAY_TICKS;
		if (!isDeployed) {
			tickDeploying++;
		}
		
		if (!isEnabled) {
			isActive = false;
		} else {
			// get current status
			final boolean isConnected = uuidTransporterCore != null
			                         && ( uuidTransporterCore.getLeastSignificantBits() != 0L
			                           || uuidTransporterCore.getMostSignificantBits() != 0L );
			final boolean isPowered = energy_consume(WarpDriveConfig.TRANSPORTER_BEACON_ENERGY_PER_TICK, true);
			// final boolean isLowPower = energy_getEnergyStored() < WarpDriveConfig.TRANSPORTER_BEACON_ENERGY_PER_TICK * TICK_LOW_POWER;
			
			// reach transporter
			boolean isActiveNew = false;
			if (isPowered) {
				if (isConnected) {// only consume is transporter is reachable
					isActiveNew = pingTransporter();
					if (isActiveNew) {
						energy_consume(WarpDriveConfig.TRANSPORTER_BEACON_ENERGY_PER_TICK, false);
					}
					
				} else {// always consume
					energy_consume(WarpDriveConfig.TRANSPORTER_BEACON_ENERGY_PER_TICK, false);
				}
			}
			isActive = isActiveNew;
		}
		
		// report updated status
		updateBlockState(blockState_actual,
		                 blockState_actual.withProperty(BlockProperties.ACTIVE, isActive)
		                                  .withProperty(BlockTransporterBeacon.DEPLOYED, isDeployed));
	}
	
	private boolean pingTransporter() {
		final GlobalRegion globalRegion = GlobalRegionManager.getByUUID(EnumGlobalRegionType.TRANSPORTER, uuidTransporterCore);
		if (globalRegion == null) {
			return false;
		}
		
		final WorldServer worldTransporter = Commons.getOrCreateWorldServer(globalRegion.dimensionId);
		if (worldTransporter == null) {
			WarpDrive.logger.error(String.format("%s Unable to load dimension %d for transporter with UUID %s",
			                                     this, globalRegion.dimensionId, uuidTransporterCore));
			return false;
		}
		
		final TileEntity tileEntity = worldTransporter.getTileEntity(globalRegion.getBlockPos());
		if (!(tileEntity instanceof TileEntityTransporterCore)) {
			WarpDrive.logger.warn(String.format("%s Transporter has gone missing for %s, found %s",
			                                    this, globalRegion, tileEntity));
			return false;
		}
		
		final TileEntityTransporterCore tileEntityTransporterCore = (TileEntityTransporterCore) tileEntity;
		final boolean isActive = tileEntityTransporterCore.updateBeacon(this, uuidTransporterCore);
		final Object[] state = tileEntityTransporterCore.state();
		stateTransporter = (String) state[1];
		
		return isActive;
	}
	
	@Override
	public void energizeDone() {
		isEnabled = false;
	}
	
	// Common OC/CC methods
	@Override
	public void setIsEnabled(final boolean isEnabled) {
		super.setIsEnabled(isEnabled);
		// enabling up => redeploy
		if (isEnabled) {
			tickDeploying = 0;
		}
		// always clear status
		stateTransporter = "";
	}
	
	@Override
	public Boolean[] isActive(final Object[] arguments) {
		return new Boolean[] { isActive };
	}
	
	@Override
	public boolean isActive() {
		return isActive;
	}
	
	@Override
	public Object[] getEnergyRequired() {
		final String units = energy_getDisplayUnits();
		return new Object[] {
				true,
				EnergyWrapper.convert(WarpDriveConfig.TRANSPORTER_BEACON_ENERGY_PER_TICK, units) };
	}
	
	// OpenComputers callback methods
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] isActive(final Context context, final Arguments arguments) {
		return isActive(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "isActive":
			return isActive(arguments);
		}
		
		return super.CC_callMethod(methodName, arguments);
	}
	
	// TileEntityAbstractBase overrides
	private WarpDriveText getSignatureStatus() {
		if (uuidTransporterCore == null) {
			return new WarpDriveText(Commons.getStyleWarning(), "warpdrive.transporter_signature.status_line.invalid");
		}
		return new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.transporter_signature.status_line.valid",
		                         nameTransporterCore, uuidTransporterCore);
	}
	
	@Override
	public WarpDriveText getStatus() {
		final WarpDriveText textSignatureStatus = getSignatureStatus();
		if (textSignatureStatus.getUnformattedText().isEmpty()) {
			return super.getStatus();
		} else {
			return super.getStatus()
			            .append(textSignatureStatus);
		}
	}
	
	// TileEntityAbstractEnergy overrides
	@Override
	public boolean energy_canInput(final EnumFacing from) {
		// only from bottom
		return (from == EnumFacing.DOWN);
	}
	
	@Override
	public boolean energy_canOutput(final EnumFacing to) {
		return false;
	}
	
	// Forge overrides
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		if (uuidTransporterCore != null) {
			tagCompound.setString(ICoreSignature.NAME_TAG, nameTransporterCore);
			tagCompound.setLong(ICoreSignature.UUID_MOST_TAG, uuidTransporterCore.getMostSignificantBits());
			tagCompound.setLong(ICoreSignature.UUID_LEAST_TAG, uuidTransporterCore.getLeastSignificantBits());
		}
		
		tagCompound.setInteger("tickDeploying", tickDeploying);
		return tagCompound;
	}
	
	@Override
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		nameTransporterCore = tagCompound.getString(ICoreSignature.NAME_TAG);
		uuidTransporterCore = new UUID(tagCompound.getLong(ICoreSignature.UUID_MOST_TAG), tagCompound.getLong(ICoreSignature.UUID_LEAST_TAG));
		if (uuidTransporterCore.getMostSignificantBits() == 0L && uuidTransporterCore.getLeastSignificantBits() == 0L) {
			uuidTransporterCore = null;
			nameTransporterCore = "";
		}
		
		tickDeploying = tagCompound.getInteger("tickDeploying");
	}
	
	@Override
	public NBTTagCompound writeItemDropNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeItemDropNBT(tagCompound);
		tagCompound.removeTag("tickDeploying");
		return tagCompound;
	}
	
	@Override
	public String toString() {
		return String.format("%s %s %8d EU linked to %s %s",
		                     getClass().getSimpleName(),
		                     Commons.format(world, pos),
		                     energy_getEnergyStored(),
		                     nameTransporterCore, uuidTransporterCore);
	}
}
