package cr0s.warpdrive.block.energy;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.api.computer.IEnanReactorLaser;
import cr0s.warpdrive.block.TileEntityAbstractLaser;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnergyWrapper;
import cr0s.warpdrive.data.ReactorFace;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.network.PacketHandler;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.lang.ref.WeakReference;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import net.minecraftforge.fml.common.Optional;

public class TileEntityEnanReactorLaser extends TileEntityAbstractLaser implements IEnanReactorLaser {
	
	// persistent properties
	private ReactorFace reactorFace = ReactorFace.UNKNOWN;
	private int energyStabilizationRequest = 0;
	
	// computed properties
	private Vector3 vLaser;
	private String reactorSignatureName;
	private WeakReference<TileEntityEnanReactorCore> weakReactorCore;
	
	public TileEntityEnanReactorLaser() {
		super();
		
		peripheralName = "warpdriveEnanReactorLaser";
		addMethods(new String[] {
				"side",
				"stabilize"
		});
		laserMedium_maxCount = 1;
		laserMedium_directionsValid = new EnumFacing[] { EnumFacing.UP, EnumFacing.DOWN };
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
		
		vLaser = new Vector3(this).translate(0.5);
	}
	
	@Override
	public void update() {
		super.update();
		
		if (energyStabilizationRequest > 0) {
			doStabilize(energyStabilizationRequest);
			energyStabilizationRequest = 0;
		}
	}
	
	@Nonnull 
	public ReactorFace getReactorFace() {
		return reactorFace != null ? reactorFace : ReactorFace.UNKNOWN;
	}
	
	public void setReactorFace(@Nonnull final ReactorFace reactorFace, final TileEntityEnanReactorCore reactorCore) {
		final TileEntityEnanReactorCore reactorCoreActual = getReactorCore();
		
		// skip if it's already set to another valid reactor core
		if ( this.reactorFace != reactorFace
		  && this.reactorFace != ReactorFace.UNKNOWN
		  && reactorCoreActual != null) {
			WarpDrive.logger.warn(String.format("%s Ignoring new reactor %s on %s due to existing reactor %s on %s.",
			                                    this,
				                                reactorCore, reactorFace.name,
				                                reactorCoreActual, this.reactorFace.name ));
			return;
		}
		
		// always update cached signature name
		reactorSignatureName = reactorCore != null ? reactorCore.getSignatureName() : "";
		
		// skip if it's already set to save resources
		if (reactorCoreActual == reactorCore) {
			return;
		}
		
		// update properties
		this.reactorFace = reactorFace;
		this.weakReactorCore = reactorCore != null && reactorFace != ReactorFace.UNKNOWN ? new WeakReference<>(reactorCore) : null;
	}
	
	@Nullable
	private TileEntityEnanReactorCore getReactorCore() {
		if (reactorFace == ReactorFace.UNKNOWN) {
			return null;
		}
		TileEntityEnanReactorCore reactorCore = weakReactorCore != null ? weakReactorCore.get() : null;
		if ( reactorCore == null
		  || reactorCore.isInvalid() ) {
			final BlockPos blockPos = pos.add(- reactorFace.x, - reactorFace.y, - reactorFace.z);
			final TileEntity tileEntity = world.getTileEntity(blockPos);
			if (tileEntity instanceof TileEntityEnanReactorCore) {
				reactorCore = (TileEntityEnanReactorCore) tileEntity;
				weakReactorCore = new WeakReference<>(reactorCore);
			} else {
				if (tileEntity != null) {
					WarpDrive.logger.error(String.format("%s Invalid TileEntityEnanReactorCore %s: %s",
					                                     this,
					                                     Commons.format(world, pos),
					                                     tileEntity));
				}
				reactorFace = ReactorFace.UNKNOWN;
				weakReactorCore = null;
			}
		}
		return reactorCore;
	}
	
	@Override
	protected boolean doScanAssembly(final boolean isDirty, final WarpDriveText textReason) {
		final boolean isValid = super.doScanAssembly(isDirty, textReason);
		
		// check if the reactor core is still there
		getReactorCore();
		
		if (reactorFace == ReactorFace.UNKNOWN) {
			textReason.append(Commons.getStyleWarning(), "warpdrive.enan_reactor.status_line.missing_reactor_core");
			return false;
		}
		
		return isValid;
	}
	
	@Override
	protected void doUpdateParameters(final boolean isDirty) {
		super.doUpdateParameters(isDirty);
		
		// refresh blockstate
		final IBlockState blockState_old = world.getBlockState(pos);
		if (isInvalidBlockState(blockState_old, BlockEnanReactorLaser.class, BlockProperties.ACTIVE, BlockProperties.FACING)) {
			return;
		}
		final IBlockState blockState_new;
		if (reactorFace.facingLaserProperty != null) {
			blockState_new = blockState_old.withProperty(BlockProperties.ACTIVE, true)
			                               .withProperty(BlockProperties.FACING, reactorFace.facingLaserProperty);
		} else {
			blockState_new = blockState_old.withProperty(BlockProperties.ACTIVE, false)
			                               .withProperty(BlockProperties.FACING, EnumFacing.DOWN);
		}
		updateBlockState(blockState_old, blockState_new);
	}
	
	protected int stabilize(final int energy) {
		if (energy <= 0) {
			return 0;
		}
		
		if (laserMedium_direction == null) {
			WarpDrive.logger.warn(String.format("ReactorLaser %s on %s side doesn't have a laser medium, unable to stabilize %d",
			                                    Commons.format(world, pos), reactorFace.name, energy ));
			return 0;
		}
		
		if (reactorFace == ReactorFace.UNKNOWN) {
			WarpDrive.logger.warn(String.format("ReactorLaser %s on %s side doesn't have a core to stabilize %d",
			                                    Commons.format(world, pos), reactorFace.name, energy ));
			return 0;
		}
		
		if (energyStabilizationRequest > 0) {
			WarpDrive.logger.debug(String.format("%s Stabilization already requested for %s",
			                                     this, energy ));
			return -energy;
		}
		energyStabilizationRequest = energy;
		return energy;
	}
	
	private void doStabilize(final int energy) {
		if (energy <= 0) {
			WarpDrive.logger.error(String.format("ReactorLaser %s on %s side can't stabilize without energy, please report to mod author %d",
			                                     Commons.format(world, pos), reactorFace.name, energy ));
			return;
		}
		
		if (laserMedium_direction == null) {
			WarpDrive.logger.warn(String.format("ReactorLaser %s on %s side no longer has a laser medium, unable to stabilize %d",
			                                    Commons.format(world, pos), reactorFace.name, energy ));
			return;
		}
		
		final TileEntityEnanReactorCore reactorCore = getReactorCore();
		if (reactorCore == null) {
			WarpDrive.logger.warn(String.format("ReactorLaser %s on %s side no longer has a core to stabilize %d",
			                                     Commons.format(world, pos), reactorFace.name, energy ));
			return;
		}
		
		if (!laserMedium_consumeExactly(energy, false)) {
			WarpDrive.logger.warn(String.format("ReactorLaser %s on %s side doesn't have enough energy %d/%d while core can output %d/%d",
			                                    Commons.format(world, pos),
			                                    reactorFace.name,
			                                    laserMedium_getEnergyStored(true),
			                                    energy,
			                                    reactorCore.energy_getPotentialOutput(),
			                                    reactorCore.energy_getEnergyStored() ));
			return;
		}
		
		if (WarpDriveConfig.LOGGING_ENERGY && WarpDriveConfig.LOGGING_LUA) {
			WarpDrive.logger.info(String.format("ReactorLaser %s on %s side stabilizing %d",
			                                    Commons.format(world, pos), reactorFace.name, energy ));
		}
		reactorCore.decreaseInstability(reactorFace, energy);
		final Vector3 vReactorCore = reactorCore.getCenter();
		if (vReactorCore == null) {
			WarpDrive.logger.warn(String.format("ReactorLaser %s on %s side has a core %s with no center defined, can't stabilize %d",
			                                    Commons.format(world, pos), reactorFace.name, reactorCore, energy ));
		} else {
			PacketHandler.sendBeamPacket(world, vLaser, vReactorCore, 0.1F, 0.2F, 1.0F, 25, 50, 100);
		}
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		if (reactorFace != null && reactorFace != ReactorFace.UNKNOWN) {
			tagCompound.setString("reactorFace", reactorFace.getName());
		}
		tagCompound.setInteger("energyStabilizationRequest", energyStabilizationRequest);
		return tagCompound;
	}
	
	@Override
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		reactorFace = ReactorFace.get(tagCompound.getString("reactorFace"));
		if (reactorFace == null) {
			reactorFace = ReactorFace.UNKNOWN;
		}
		energyStabilizationRequest = tagCompound.getInteger("energyStabilizationRequest");
	}
	
	@Override
	public NBTTagCompound writeItemDropNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeItemDropNBT(tagCompound);
		
		tagCompound.removeTag("reactorFace");
		tagCompound.removeTag("energyStabilizationRequest");
		return tagCompound;
	}
	
	
	// Common OC/CC methods
	@Override
	public Object[] getEnergyRequired() {
		final String units = energy_getDisplayUnits();
		return new Object[] { true,
		                      EnergyWrapper.convert(energyStabilizationRequest, units) };
	}
	
	@Override
	public Object[] stabilize(final Object[] arguments) {
		if (arguments.length == 1) {
			final int energy;
			try {
				energy = Commons.toInt(arguments[0]);
				return new Object[] { stabilize(energy) };
			} catch (final Exception exception) {
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.error(String.format("%s LUA error on stabilize(): Integer expected for 1st argument %s",
					                                     this, arguments[0]));
				}
			}
		}
		return new Object[] { energyStabilizationRequest };
	}
	
	@Override
	public Object[] side() {
		if (reactorFace == null || reactorFace.enumTier == null) {
			return new Object[] { null, null, null };
		}
		return new Object[] { reactorFace.indexStability, reactorFace.enumTier.getName(), reactorSignatureName };
	}
	
	// OpenComputers callback methods
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] stabilize(final Context context, final Arguments arguments) {
		return stabilize(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] side(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return side();
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "stabilize":
			return stabilize(arguments);
			
		case "side":
			return side();
		}
		
		return super.CC_callMethod(methodName, arguments);
	}
}
