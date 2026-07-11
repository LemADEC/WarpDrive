package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IGlobalRegionProvider;
import cr0s.warpdrive.block.TileEntityAbstractEnergyCoreOrController;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumGlobalRegionType;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.render.EntityFXBoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityJumpGateCore extends TileEntityAbstractEnergyCoreOrController implements IGlobalRegionProvider {
	
	private static final int BOUNDING_BOX_INTERVAL_TICKS = 60;
	
	// persistent properties
	private int maxX, maxY, maxZ;
	private int minX, minY, minZ;
	
	// computed properties
	protected boolean showBoundingBox = false;
	private int ticksBoundingBoxUpdate = 0;
	
	private long timeLastScanDone = -1;
	private JumpGateScanner jumpGateScanner = null;
	public int volume;
	public double occupancy;
	
	
	public TileEntityJumpGateCore() {
		super();
		
		peripheralName = "warpdriveJumpGate";
		// addMethods(new String[] {});
		// CC_scripts = Collections.singletonList("startup");
	}
	
	@SideOnly(Side.CLIENT)
	private void doShowBoundingBox() {
		ticksBoundingBoxUpdate--;
		if (ticksBoundingBoxUpdate > 0) {
			return;
		}
		ticksBoundingBoxUpdate = BOUNDING_BOX_INTERVAL_TICKS;
		
		final Vector3 vector3 = new Vector3(this);
		vector3.translate(0.5D);
		
		FMLClientHandler.instance().getClient().effectRenderer.addEffect(
				new EntityFXBoundingBox(world, vector3,
				                        new Vector3(minX - 0.0D, minY - 0.0D, minZ - 0.0D),
				                        new Vector3(maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D),
				                        1.0F, 0.8F, 0.3F, BOUNDING_BOX_INTERVAL_TICKS + 1) );
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			if (showBoundingBox) {
				doShowBoundingBox();
			}
			return;
		}
		
		// scan ship content progressively
		if (timeLastScanDone <= 0L) {
			timeLastScanDone = world.getTotalWorldTime();
			jumpGateScanner = new JumpGateScanner(world, minX, minY, minZ, maxX, maxY, maxZ);
			if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
				WarpDrive.logger.info(String.format("%s scanning started",
				                                    this));
			}
		}
		if (jumpGateScanner != null) {
			if (!jumpGateScanner.tick()) {
				// still scanning => skip state handling
				return;
			}
			
			volume = maxX - minX * maxY - minY * maxZ - minZ;
			occupancy = volume / (float) jumpGateScanner.volumeUsed;
			jumpGateScanner = null;
			if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
				WarpDrive.logger.info(String.format("%s scanning done: volume %d, occupancy %.3f",
				                                    this, volume, occupancy));
			}
		}
	}
	
	@Override
	protected void doUpdateParameters(final boolean isDirty) {
		// no operation
	}
	
	public boolean isBusy() {
		return timeLastScanDone < 0 || jumpGateScanner != null;
	}
	
	@Override
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		minX = tagCompound.getInteger("minX");
		maxX = tagCompound.getInteger("maxX");
		minY = tagCompound.getInteger("minY");
		maxY = tagCompound.getInteger("maxY");
		minZ = tagCompound.getInteger("minZ");
		maxZ = tagCompound.getInteger("maxZ");
		volume = tagCompound.getInteger("volume");
		occupancy = tagCompound.getDouble("occupancy");
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		tagCompound.setInteger("minX", minX);
		tagCompound.setInteger("maxX", maxX);
		tagCompound.setInteger("minY", minY);
		tagCompound.setInteger("maxY", maxY);
		tagCompound.setInteger("minZ", minZ);
		tagCompound.setInteger("maxZ", maxZ);
		tagCompound.setInteger("volume", volume);
		tagCompound.setDouble("occupancy", occupancy);
		
		return tagCompound;
	}
	
	// IGlobalRegionProvider overrides
	@Override
	public EnumGlobalRegionType getGlobalRegionType() {
		return EnumGlobalRegionType.JUMP_GATE;
	}
	
	@Override
	public AxisAlignedBB getGlobalRegionArea() {
		return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
	}
	
	@Override
	public int getMass() {
		return volume;
	}
	
	@Override
	public double getIsolationRate() {
		return 0.0D;
	}
	
	@Override
	public boolean onBlockUpdatingInArea(@Nullable final Entity entity, final BlockPos blockPos, final IBlockState blockState) {
		// no operation
		return true;
	}
	
	// Common OC/CC methods
	
	@Override
	public Object[] getEnergyRequired() {
		return new Object[0];
	}
	
	public Object[] area(final Object[] arguments) {
		try {
			if (arguments != null && arguments.length == 6) {
				final int sizeMax = WarpDriveConfig.JUMP_GATE_SIZE_MAX_PER_SIDE_BY_TIER[enumTier.getIndex()];
				final int minX_new = Commons.clamp(pos.getX() - sizeMax, pos.getX() + sizeMax, Math.abs(Commons.toInt(arguments[0])));
				final int minY_new = Commons.clamp(pos.getY() - sizeMax, pos.getY() + sizeMax, Math.abs(Commons.toInt(arguments[1])));
				final int minZ_new = Commons.clamp(pos.getZ() - sizeMax, pos.getZ() + sizeMax, Math.abs(Commons.toInt(arguments[2])));
				final int maxX_new = Commons.clamp(pos.getX() - sizeMax, pos.getX() + sizeMax, Math.abs(Commons.toInt(arguments[3])));
				final int maxY_new = Commons.clamp(pos.getY() - sizeMax, pos.getY() + sizeMax, Math.abs(Commons.toInt(arguments[4])));
				final int maxZ_new = Commons.clamp(pos.getZ() - sizeMax, pos.getZ() + sizeMax, Math.abs(Commons.toInt(arguments[5])));
				if ( minX_new != minX
				  || minY_new != minY
				  || minZ_new != minZ
				  || maxX_new != maxX
				  || maxY_new != maxY
				  || maxZ_new != maxZ ) {
					minX = minX_new;
					minY = minY_new;
					minZ = minZ_new;
					maxX = maxX_new;
					maxY = maxY_new;
					maxZ = maxZ_new;
					// force a new scan
					timeLastScanDone = -1;
					jumpGateScanner = null;
				}
			}
		} catch (final Exception exception) {
			if (WarpDriveConfig.LOGGING_LUA) {
				WarpDrive.logger.info(String.format("%s Invalid arguments to area(): %s",
				                                    this, Commons.format(arguments)));
			}
		}
		
		return new Integer[] { minX, minY, minZ, maxX, maxY, maxZ };
	}
}
