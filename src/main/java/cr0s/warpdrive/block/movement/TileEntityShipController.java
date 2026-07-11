package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.data.EnergyWrapper;
import cr0s.warpdrive.data.EnumGlobalRegionType;
import cr0s.warpdrive.data.GlobalRegionManager;
import cr0s.warpdrive.data.GlobalRegion;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.util.Collections;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;

public class TileEntityShipController extends TileEntityAbstractShipController {
	
	// persistent properties
	// (none)
	
	// computed properties
	private int tickBooting = 20;
	
	private WeakReference<TileEntityShipCore> tileEntityShipCoreWeakReference = null;
	
	public TileEntityShipController() {
		super();
		
		peripheralName = "warpdriveShipController";
		// addMethods(new String[] {});
		CC_scripts = Collections.singletonList("startup");
	}
    
    @Override
    public void update() {
		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		// accelerate update ticks during boot
		if (tickBooting > 0) {
			tickBooting--;
			if (tileEntityShipCoreWeakReference == null) {
				markDirtyAssembly();
			}
		}
	}
	
	public TileEntityShipCore getLinkedShipCore() {
		return tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
	}
	
	public TileEntityShipCore getLinkedShipCoreRefresh() {
		TileEntityShipCore tileEntityShipCore = getLinkedShipCore();
		if (tileEntityShipCore != null && !tileEntityShipCore.isInvalid()) {
			return tileEntityShipCore;
		}
		final WarpDriveText textReason = new WarpDriveText();
		doScanAssembly(true, textReason);
		tileEntityShipCore = getLinkedShipCore();
		return tileEntityShipCore == null || tileEntityShipCore.isInvalid() ? null : tileEntityShipCore;
	}
	
	@Override
	protected boolean doScanAssembly(final boolean isDirty, final WarpDriveText textReason) {
		final boolean isValid = super.doScanAssembly(isDirty, textReason);
		
		// validate existing link
		TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference != null ? tileEntityShipCoreWeakReference.get() : null;
		if ( tileEntityShipCore == null
		  || tileEntityShipCore.isInvalid()
		  || uuid == null
		  || !uuid.equals(tileEntityShipCore.getSignatureUUID()) ) {
			tileEntityShipCore = null;
			tileEntityShipCoreWeakReference = null;
		}
		
		// refresh as needed
		// note: it's up to players to break the link, so if the world is partially restored we won't lose the link
		if (tileEntityShipCore == null) {
			final GlobalRegion globalRegion = GlobalRegionManager.getByUUID(EnumGlobalRegionType.SHIP, uuid);
			if (globalRegion == null) {
				textReason.append(Commons.getStyleWarning(), "warpdrive.core_signature.status_line.unknown_core_signature");
				return false;
			}
			final WorldServer worldServer = globalRegion.getWorldServerIfLoaded();
			if (worldServer == null) {
				textReason.append(Commons.getStyleWarning(), "warpdrive.core_signature.status_line.world_not_loaded");
				return false;
			}
			final TileEntity tileEntity = worldServer.getTileEntity(globalRegion.getBlockPos());
			if ( !(tileEntity instanceof TileEntityShipCore)
			  || tileEntity.isInvalid()
			  || uuid == null
			  || !uuid.equals(((TileEntityShipCore) tileEntity).getSignatureUUID()) ) {
				textReason.append(Commons.getStyleWarning(), "warpdrive.core_signature.status_line.unknown_core_signature");
				return false;
			}
			tileEntityShipCore = (TileEntityShipCore) tileEntity;
			tileEntityShipCoreWeakReference = new WeakReference<>(tileEntityShipCore);
		}
		// (tileEntityShipCore is defined and valid)
		
		final boolean isSynchronized = tileEntityShipCore.refreshLink(this);
		if (isSynchronized) {
			onCoreUpdated(tileEntityShipCore);
			// send command as soon as link is re-established
			if ( !tileEntityShipCore.isCommandConfirmed
			  && isCommandConfirmed ) {
				tileEntityShipCore.command(new Object[] { enumShipCommand.getName(), true });
			}
		}
		
		updateBlockState(null, BlockShipController.COMMAND, enumShipCommand);
		
		return isValid;
	}
	
	@Override
	protected void doUpdateParameters(final boolean isDirty) {
		// no operation
	}
	
	@Override
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		return tagCompound;
	}
	
	@Override
	public NBTTagCompound writeItemDropNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeItemDropNBT(tagCompound);
		
		return tagCompound;
	}
	
	@Nonnull
	@Override
	protected WarpDriveText getCoreSignatureStatus(final String nameSignature) {
		if (nameSignature == null || nameSignature.isEmpty()) {
			return new WarpDriveText(Commons.getStyleWarning(), "warpdrive.core_signature.status_line.undefined");
		}
		return super.getCoreSignatureStatus(nameSignature);
	}
	
	// Common OC/CC methods
	@Override
	public Object[] getLocalPosition() {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return null;
		}
		return tileEntityShipCore.getLocalPosition();
	}
	
	@Override
	public Object[] getAssemblyStatus() {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return new Object[] { false, "No core detected" };
		}
		return tileEntityShipCore.getAssemblyStatus();
	}
	
	@Override
	public Object[] getOrientation() {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return null;
		}
		return tileEntityShipCore.getOrientation();
	}
	
	@Override
	public Object[] isInSpace() {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return null;
		}
		return tileEntityShipCore.isInSpace();
	}
	
	@Override
	public Object[] isInHyperspace() {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return null;
		}
		return tileEntityShipCore.isInHyperspace();
	}
	
	@Override
	public String[] name(final Object[] arguments) {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return super.name(null); // return current local values
		}
		return tileEntityShipCore.name(arguments);
	}
	
	@Override
	public Object[] dim_positive(final Object[] arguments) {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return super.dim_positive(null); // return current local values
		}
		return tileEntityShipCore.dim_positive(arguments);
	}
	
	@Override
	public Object[] dim_negative(final Object[] arguments) {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return super.dim_negative(null); // return current local values
		}
		return tileEntityShipCore.dim_negative(arguments);
	}
	
	@Override
	public Object[] energyDisplayUnits(final Object[] arguments) {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return null;
		}
		return tileEntityShipCore.energyDisplayUnits(arguments);
	}
	
	@Override
	public Object[] getEnergyStatus() {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return null;
		}
		return tileEntityShipCore.getEnergyStatus();
	}
	
	@Override
	public Object[] command(final Object[] arguments) {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return null;
		}
		return tileEntityShipCore.command(arguments);
	}
	
	@Override
	public Object[] getShipSize() {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return null;
		}
		return tileEntityShipCore.getShipSize();
	}
	
	@Override
	public Object[] movement(final Object[] arguments) {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return super.movement(arguments); // return current local values
		}
		return tileEntityShipCore.movement(arguments);
	}
	
	@Override
	public Object[] getMaxJumpDistance() {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return new Object[] { false, "No ship core detected" };
		}
		return tileEntityShipCore.getMaxJumpDistance();
	}
	
	@Override
	public Object[] validateMovement(final Object[] arguments) {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return new Object[] { false, "warpdrive.navigation.no_core", "No ship core detected" };
		}
		return tileEntityShipCore.validateMovement(arguments);
	}
	
	@Override
	public Object[] validateNavigation() {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return new Object[] { false, "warpdrive.navigation.no_core", "No ship core detected" };
		}
		return tileEntityShipCore.validateNavigation();
	}
	
	@Override
	public Object[] rotationSteps(final Object[] arguments) {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return super.rotationSteps(arguments); // return current local values
		}
		return tileEntityShipCore.rotationSteps(arguments);
	}
	
	@Override
	public Object[] state() {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return new Object[] { "No ship core detected", false, "-NotDetected-", 0 };
		}
		return tileEntityShipCore.state();
	}
	
	@Override
	public Object[] getEnergyRequired() {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return new Object[] { false, "No ship core detected" };
		}
		return tileEntityShipCore.getEnergyRequired();
	}
}
