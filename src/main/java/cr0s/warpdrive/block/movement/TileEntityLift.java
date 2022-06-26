package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.computer.ILift;
import cr0s.warpdrive.block.TileEntityAbstractEnergyConsumer;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnergyWrapper;
import cr0s.warpdrive.data.EnumComponentType;
import cr0s.warpdrive.data.EnumGlobalRegionType;
import cr0s.warpdrive.data.EnumLiftMode;
import cr0s.warpdrive.data.GlobalRegion;
import cr0s.warpdrive.data.GlobalRegionManager;
import cr0s.warpdrive.data.SoundEvents;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.item.ItemComponent;
import cr0s.warpdrive.network.PacketHandler;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;

public class TileEntityLift extends TileEntityAbstractEnergyConsumer implements ILift {
	
	// global properties
	private static final double LIFT_GRAB_RADIUS = 0.4D;
	private static final UpgradeSlot upgradeSlotSecurity = new UpgradeSlot("lift.security",
	                                                                       ItemComponent.getItemStackNoCache(EnumComponentType.DIAMOND_CRYSTAL, 1),
	                                                                       1);
	
	// persistent properties
	private EnumLiftMode mode = EnumLiftMode.INACTIVE;
	private EnumLiftMode computerMode = EnumLiftMode.REDSTONE;
	
	// computed properties
	private int updateTicks = 0;
	private boolean isActive = false;
	private boolean isValid = false;
	private int firstUncoveredY;
	
	public TileEntityLift(@Nonnull final IBlockBase blockBase) {
		super(blockBase);
		
		peripheralName = "warpdriveLift";
		addMethods(new String[] {
				"mode",
				"state"
		});
		doRequireUpgradeToInterface();
		
		registerUpgradeSlot(upgradeSlotSecurity);
	}
	
	@Override
	protected void onConstructed() {
		super.onConstructed();
		
		energy_setParameters(WarpDriveConfig.LIFT_MAX_ENERGY_STORED,
		                     1024, 0,
		                     "MV", 2, "MV", 0);
	}
	
	@Override
	public void tick() {
		super.tick();
		
		assert world != null;
		if (world.isRemote()) {
			return;
		}
		
		updateTicks--;
		if (updateTicks < 0) {
			updateTicks = WarpDriveConfig.LIFT_UPDATE_INTERVAL_TICKS;
			
			// Switching mode
			if (  computerMode == EnumLiftMode.DOWN
			  || (computerMode == EnumLiftMode.REDSTONE && world.getRedstonePowerFromNeighbors(pos) > 0)) {
				mode = EnumLiftMode.DOWN;
			} else {
				mode = EnumLiftMode.UP;
			}
			
			isValid = isPassableBlock(pos.getY() + 1)
			       && isPassableBlock(pos.getY() + 2)
			       && isPassableBlock(pos.getY() - 1)
			       && isPassableBlock(pos.getY() - 2);
			isActive = isEnabled && isValid;
			
			final BlockState blockState = world.getBlockState(pos);
			if (energy_getEnergyStored() < WarpDriveConfig.LIFT_ENERGY_PER_ENTITY || !isActive) {
				mode = EnumLiftMode.INACTIVE;
				if (blockState.get(BlockLift.MODE) != EnumLiftMode.INACTIVE) {
					world.setBlockState(pos, blockState.with(BlockLift.MODE, EnumLiftMode.INACTIVE));
				}
				return;
			}
			
			if (blockState.get(BlockLift.MODE) != mode) {
				world.setBlockState(pos, blockState.with(BlockLift.MODE, mode));
			}
			
			// Launch a beam: search non-air blocks under lift
			for (int ny = pos.getY() - 2; ny > 0; ny--) {
				if (!isPassableBlock(ny)) {
					firstUncoveredY = ny + 1;
					break;
				}
			}
			
			if (pos.getY() - firstUncoveredY >= 2) {
				if (mode == EnumLiftMode.UP) {
					PacketHandler.sendBeamPacket(world,
							new Vector3(pos.getX() + 0.5D, firstUncoveredY, pos.getZ() + 0.5D),
							new Vector3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D),
							0f, 1f, 0f, 40, 0, 100);
				} else if (mode == EnumLiftMode.DOWN) {
					PacketHandler.sendBeamPacket(world,
							new Vector3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D),
							new Vector3(pos.getX() + 0.5D, firstUncoveredY, pos.getZ() + 0.5D), 0f,
							0f, 1f, 40, 0, 100);
				}
				
				if (liftEntity()) {
					updateTicks = WarpDriveConfig.LIFT_ENTITY_COOLDOWN_TICKS;
				}
			}
		}
	}
	
	private boolean isPassableBlock(final int yPosition) {
		final BlockPos blockPos = new BlockPos(pos.getX(), yPosition, pos.getZ());
		assert world != null;
		final BlockState blockState = world.getBlockState(blockPos);
		return blockState.getBlock() == Blocks.AIR
			|| world.isAirBlock(blockPos)
			|| blockState.getCollisionShape(world, blockPos, ISelectionContext.dummy()).isEmpty();
	}
	
	private boolean liftEntity() {
		assert world != null;
		
		// get list of potential entities
		final double xMin = pos.getX() + 0.5 - LIFT_GRAB_RADIUS;
		final double xMax = pos.getX() + 0.5 + LIFT_GRAB_RADIUS;
		final double zMin = pos.getZ() + 0.5 - LIFT_GRAB_RADIUS;
		final double zMax = pos.getZ() + 0.5 + LIFT_GRAB_RADIUS;
		
		final AxisAlignedBB aabb;
		if (mode == EnumLiftMode.UP) {
			aabb = new AxisAlignedBB(
					xMin, firstUncoveredY, zMin,
					xMax, pos.getY(), zMax);
			
		} else if (mode == EnumLiftMode.DOWN) {
			aabb = new AxisAlignedBB(
					xMin, Math.min(firstUncoveredY + 4.0D, pos.getY()), zMin,
					xMax, pos.getY() + 2.0D, zMax);
		} else {
			return false;
		}
		final List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(null, aabb);
		if (list.isEmpty()) {
			return false;
		}
		
		// cache security constrains
		final ArrayList<GlobalRegion> globalRegions = getUpgradeCount(upgradeSlotSecurity) <= 0
		                                            ? new ArrayList<>(0)
		                                            : GlobalRegionManager.getContainers(EnumGlobalRegionType.SHIP, world, pos);
		final ArrayList<TileEntityShipCore> tileEntityShipCores = new ArrayList<>(globalRegions.size());
		if (!globalRegions.isEmpty()) {
			for (final GlobalRegion globalRegion : globalRegions) {
				// abort on invalid ship cores
				final TileEntity tileEntity = world.getTileEntity(globalRegion.getBlockPos());
				if (!(tileEntity instanceof TileEntityShipCore)) {
					if (Commons.throttleMe("liftEntity-InvalidInstance")) {
						WarpDrive.logger.error(String.format("Unable to lift entity due to invalid tile entity for global region, expecting TileEntityShipCore, got %s",
						                                     tileEntity ));
					}
					return false;
				}
				final TileEntityShipCore tileEntityShipCore = (TileEntityShipCore) tileEntity;
				if (!tileEntityShipCore.isAssemblyValid()) {
					if (Commons.throttleMe("liftEntity-InvalidAssembly")) {
						WarpDrive.logger.error(String.format("Unable to lift entity due to invalid ship assembly for %s",
						                                     tileEntity ));
					}
					return false;
				}
				tileEntityShipCores.add(tileEntityShipCore);
			}
		}
		
		// go through each entity
		boolean isTransferDone = false;
		for (final Entity entity : list) {
			if (!(entity instanceof LivingEntity)) {
				continue;
			}
			// apply security constrains
			if (!globalRegions.isEmpty()) {
				if (!(entity instanceof PlayerEntity)) {// only players are allowed
					continue;
				}
				boolean isCrewMember = true;
				for (final TileEntityShipCore tileEntityShipCore : tileEntityShipCores) {
					isCrewMember &= tileEntityShipCore.isCrewMember((PlayerEntity) entity);
				}
				if (!isCrewMember) {
					continue;
				}
			}
			
			// consume power and move
			if (energy_consume(WarpDriveConfig.LIFT_ENERGY_PER_ENTITY, true)) {
				if (mode == EnumLiftMode.UP) {// Lift up
					entity.setPositionAndUpdate(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
					PacketHandler.sendBeamPacket(world,
					                             new Vector3(pos.getX() + 0.5D, firstUncoveredY, pos.getZ() + 0.5D),
					                             new Vector3(pos.getX() + 0.5D, pos.getY()     , pos.getZ() + 0.5D),
					                             1.0F, 1.0F, 0.0F, 40, 0, 100);
				} else {
					entity.setPositionAndUpdate(pos.getX() + 0.5D, firstUncoveredY, pos.getZ() + 0.5D);
					PacketHandler.sendBeamPacket(world,
					                             new Vector3(pos.getX() + 0.5D, pos.getY()     , pos.getZ() + 0.5D),
					                             new Vector3(pos.getX() + 0.5D, firstUncoveredY, pos.getZ() + 0.5D),
					                             1.0F, 1.0F, 0.0F, 40, 0, 100);
				}
				if (!isTransferDone) {
					world.playSound(null, pos, SoundEvents.LASER_HIGH, SoundCategory.AMBIENT, 1.0F, 1.0F);
				}
				energy_consume(WarpDriveConfig.LIFT_ENERGY_PER_ENTITY, false);
				isTransferDone = true;
			}
		}
		
		return isTransferDone;
	}
	
	@Override
	public void read(@Nonnull final CompoundNBT tagCompound) {
		super.read(tagCompound);
		if (tagCompound.contains("mode")) {
			final byte byteValue = tagCompound.getByte("mode");
			mode = EnumLiftMode.get(Commons.clamp(0, 3, byteValue == -1 ? 3 : byteValue));
		}
		if (tagCompound.contains("computerMode")) {
			final byte byteValue = tagCompound.getByte("computerMode");
			computerMode = EnumLiftMode.get(Commons.clamp(0, 3, byteValue == -1 ? 3 : byteValue));
		}
	}
	
	@Nonnull
	@Override
	public CompoundNBT write(@Nonnull CompoundNBT tagCompound) {
		tagCompound = super.write(tagCompound);
		tagCompound.putByte("mode", (byte) mode.ordinal());
		tagCompound.putByte("computerMode", (byte) computerMode.ordinal());
		return tagCompound;
	}
	
	@Override
	public CompoundNBT writeItemDropNBT(CompoundNBT tagCompound) {
		tagCompound = super.writeItemDropNBT(tagCompound);
		tagCompound.remove("mode");
		return tagCompound;
	}
	
	@Override
	public boolean energy_canInput(final Direction from) {
		return true;
	}
	
	// Common OC/CC methods
	@Override
	public Object[] getEnergyRequired() {
		final String units = energy_getDisplayUnits();
		return new Object[] {
				true,
				EnergyWrapper.convert(WarpDriveConfig.LIFT_ENERGY_PER_ENTITY, units) };
	}
	
	@Override
	public Object[] mode(@Nonnull final Object[] arguments) {
		if (arguments.length == 1 && arguments[0] instanceof String) {
			final String stringValue = (String) arguments[0];
			if (stringValue.equalsIgnoreCase("up")) {
				computerMode = EnumLiftMode.UP;
			} else if (stringValue.equalsIgnoreCase("down")) {
				computerMode = EnumLiftMode.DOWN;
			} else {
				computerMode = EnumLiftMode.REDSTONE;
			}
			markDirty();
		}
		
		return new Object[] { computerMode.getName() };
	}
	
	@Override
	public Object[] state() {
		final long energy = energy_getEnergyStored();
		final String status = getStatusHeaderInPureText();
		return new Object[] { status, isActive, energy, isValid, isEnabled, computerMode.getName() };
	}
	
	// OpenComputers callback methods
	@Callback(direct = true)
	public Object[] mode(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return mode(
			new Object[] {
				arguments.checkString(0)
			}
		);
	}
	
	@Callback(direct = true)
	public Object[] state(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return state();
	}
	
	// ComputerCraft IDynamicPeripheral methods
	@Override
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "mode":
			return mode(arguments);
			
		case "state":
			return state();
			
		default:
			return super.CC_callMethod(methodName, arguments);
		}
	}
}