package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.block.TileEntityAbstractMachine;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.SoundEvents;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.network.PacketHandler;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;

import net.minecraftforge.fml.common.Optional;

public class TileEntityBiometricScanner extends TileEntityAbstractMachine {
	
	// persistent properties
	private UUID uuidLastPlayer = null;
	private String nameLastPlayer = "";
	
	// computed properties
	private int tickUpdate;
	private AxisAlignedBB aabbRange = null;
	private int tickScanning = -1; // < 0 when IDLE, >= 0 when SCANNING
	
	public TileEntityBiometricScanner() {
		super();
		
		peripheralName = "warpdriveBiometricScanner";
		addMethods(new String[] {
			"getScanResults"
		});
		CC_scripts = Collections.singletonList("scan");
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
		
		final IBlockState blockState = world.getBlockState(pos);
		if (isInvalidBlockState(blockState, BlockBiometricScanner.class, BlockProperties.FACING)) {
			tickScanning = -1;
			aabbRange = null;
			return;
		}
		final EnumFacing enumFacing = blockState.getValue(BlockProperties.FACING);
		final float radius = WarpDriveConfig.BIOMETRIC_SCANNER_RANGE_BLOCKS / 2.0F;
		final Vector3 v3Center = new Vector3(
				pos.getX() + 0.5F + (radius + 0.5F) * enumFacing.getXOffset(),
				pos.getY() + 0.5F + (radius + 0.5F) * enumFacing.getYOffset(),
				pos.getZ() + 0.5F + (radius + 0.5F) * enumFacing.getZOffset() );
		aabbRange = new AxisAlignedBB(
				v3Center.x - radius, v3Center.y - radius, v3Center.z - radius,
				v3Center.x + radius, v3Center.y + radius, v3Center.z + radius );
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		tickUpdate--;
		if (tickUpdate < 0) {
			tickUpdate = WarpDriveConfig.G_PARAMETERS_UPDATE_INTERVAL_TICKS;
			
			final IBlockState blockState = world.getBlockState(pos);
			updateBlockState(blockState, BlockProperties.ACTIVE, isEnabled);
		}
		
		if ( isEnabled
		  && tickScanning >= 0 ) {
			if (aabbRange == null) {
				tickScanning = -1;
				uuidLastPlayer = null;
				nameLastPlayer = "";
				return;
			}
			tickScanning--;
			
			// check for exclusive player presence
			final List<EntityPlayerMP> playersInRange = world.getEntitiesWithinAABB(EntityPlayerMP.class, aabbRange,
			                                                                        entityPlayerMP -> entityPlayerMP != null
			                                                                                       && entityPlayerMP.isEntityAlive()
			                                                                                       && !entityPlayerMP.isSpectator() );
			boolean isJammed = false;
			boolean isPresent = false;
			for (final EntityPlayerMP entityPlayerMP : playersInRange) {
				if (entityPlayerMP.getUniqueID().equals(uuidLastPlayer)) {
					isPresent = true;
				} else {
					isJammed = true;
					PacketHandler.sendSpawnParticlePacket(world, "jammed", (byte) 5,
					                                      new Vector3(entityPlayerMP.posX, entityPlayerMP.posY, entityPlayerMP.posZ),
					                                      new Vector3(0.0D, 0.0D, 0.0D),
					                                      1.0F, 1.0F, 1.0F,
					                                      1.0F, 1.0F, 1.0F,
					                                      32 );
				}
			}
			if ( !isPresent
			  || isJammed ) {
				PacketHandler.sendSpawnParticlePacket(world, "jammed", (byte) 5,
				                                      new Vector3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D),
				                                      new Vector3(0.0D, 0.0D, 0.0D),
				                                      1.0F, 1.0F, 1.0F,
				                                      1.0F, 1.0F, 1.0F,
				                                      32 );
				tickScanning = -1;
				uuidLastPlayer = null;
				nameLastPlayer = "";
				sendEvent("biometricScanAborted");
				
			} else if (tickScanning < 0) {
				sendEvent("biometricScanDone", uuidLastPlayer.toString(), nameLastPlayer);
				world.playSound(null, pos, SoundEvents.DING, SoundCategory.BLOCKS, 1.0F, 1.0F);
				
			} else if (tickScanning == WarpDriveConfig.BIOMETRIC_SCANNER_DURATION_TICKS - 1) {
				PacketHandler.sendScanningPacket(world,
				                                 (int) aabbRange.minX, (int) aabbRange.minY, (int) aabbRange.minZ,
				                                 (int) aabbRange.maxX, (int) aabbRange.maxY, (int) aabbRange.maxZ,
				                                 0.3F, 0.0F, 1.0F, WarpDriveConfig.BIOMETRIC_SCANNER_DURATION_TICKS);
			}
		}
	}
	
	public boolean startScanning(@Nonnull final EntityPlayer entityPlayer, @Nonnull final WarpDriveText textReason) {
		if (!isEnabled) {
			textReason.append(Commons.getStyleWarning(), "warpdrive.machine.is_enabled.get.disabled",
			                  (int) Math.ceil(tickScanning / 20.0F) );
			return false;
		}
		if (tickScanning >= 0) {
			textReason.append(Commons.getStyleWarning(), "warpdrive.biometric_scanner.start_scanning.in_progress",
			                  (int) Math.ceil(tickScanning / 20.0F) );
			return false;
		}
		
		uuidLastPlayer = entityPlayer.getUniqueID();
		nameLastPlayer = entityPlayer.getName();
		tickScanning = WarpDriveConfig.BIOMETRIC_SCANNER_DURATION_TICKS;
		textReason.append(Commons.getStyleCorrect(), "warpdrive.biometric_scanner.start_scanning.started");
		return true;
	}
	
	@Override
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		if (tagCompound.hasUniqueId("uuidLastPlayer")) {
			uuidLastPlayer = tagCompound.getUniqueId("uuidLastPlayer");
			nameLastPlayer = tagCompound.getString("nameLastPlayer");
		} else {
			uuidLastPlayer = null;
			nameLastPlayer = "";
		}
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		// only save if scanning has concluded
		if ( tickScanning < 0
		  && uuidLastPlayer != null
		  && uuidLastPlayer.getMostSignificantBits() != 0L
		  && uuidLastPlayer.getLeastSignificantBits() != 0L ) {
			tagCompound.setUniqueId("uuidLastPlayer", uuidLastPlayer);
			tagCompound.setString("nameLastPlayer", nameLastPlayer);
		} else {
			tagCompound.removeTag("uuidLastPlayer");
			tagCompound.removeTag("nameLastPlayer");
		}
		
		return tagCompound;
	}
	
	// TileEntityAbstractBase overrides
	@Nonnull
	private WarpDriveText getScanStatus() {
		if (tickScanning >= 0) {
			return new WarpDriveText(Commons.getStyleWarning(), "warpdrive.biometric_scanner.status_line.scan_in_progress",
			                         (int) Math.ceil(tickScanning / 20.0F) );
		}
		if (uuidLastPlayer == null) {
			return new WarpDriveText(Commons.getStyleWarning(), "warpdrive.biometric_scanner.status_line.invalid");
		}
		return new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.biometric_scanner.status_line.valid",
		                         nameLastPlayer, uuidLastPlayer );
	}
	
	@Override
	public WarpDriveText getStatus() {
		final WarpDriveText textScanStatus = getScanStatus();
		if (textScanStatus.getUnformattedText().isEmpty()) {
			return super.getStatus();
		} else {
			return super.getStatus()
			            .append(textScanStatus);
		}
	}
	
	// Common OC/CC methods
	public Object[] getScanResults() {
		if (tickScanning >= 0) {
			return new Object[] { false, "Scan is in progress..." };
		}
		if (uuidLastPlayer == null) {
			return new Object[] { false, "No results available." };
		}
		return new Object[] { true, uuidLastPlayer, nameLastPlayer };
	}
	
	// OpenComputers callback methods
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getScanResults(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getScanResults();
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "getScanResults":
			return getScanResults();
		}
		
		return super.CC_callMethod(methodName, arguments);
	}
}
