package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.TileEntityAbstractEnergy;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CloakedArea;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.network.PacketHandler;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import java.util.Arrays;

import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.StatCollector;

import cpw.mods.fml.common.Optional;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityCloakingCore extends TileEntityAbstractEnergy {
	
	private static final int CLOAKING_CORE_SOUND_UPDATE_TICKS = 40;
	private static final int DISTANCE_INNER_COILS_BLOCKS = 2;
	private static final int LASER_REFRESH_TICKS = 100;
	private static final int LASER_DURATION_TICKS = 110;
	
	public boolean isEnabled = false;
	public byte tier = 1; // cloaking field tier, 1 or 2
	
	// inner coils color map
	final float[] innerCoilColor_r = { 1.00f, 1.00f, 1.00f, 1.00f, 0.75f, 0.25f, 0.00f, 0.00f, 0.00f, 0.00f, 0.50f, 1.00f }; 
	final float[] innerCoilColor_g = { 0.00f, 0.25f, 0.75f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 0.50f, 0.25f, 0.00f, 0.00f }; 
	final float[] innerCoilColor_b = { 0.25f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.50f, 1.00f, 1.00f, 1.00f, 1.00f, 0.75f }; 
	
	// Spatial cloaking field parameters
	private final boolean[] isValidInnerCoils = { false, false, false, false, false, false };
	private final int[] distanceOuterCoils_blocks = { 0, 0, 0, 0, 0, 0 };   // 0 means invalid
	private int minX = 0;
	private int minY = 0;
	private int minZ = 0;
	private int maxX = 0;
	private int maxY = 0;
	private int maxZ = 0;
	
	private boolean isValid = false;
	private String messageValidityIssues = "";
	private boolean isCloaking = false;
	private int volume = 0;
	private int energyRequired = 0;
	private int updateTicks = 0;
	private int laserDrawingTicks = 0;
	
	private boolean soundPlayed = false;
	private int soundTicks = 0;
	
	public TileEntityCloakingCore() {
		super();
		peripheralName = "warpdriveCloakingCore";
		addMethods(new String[] {
			"tier",				// set field tier to 1 or 2, return field tier
			"isAssemblyValid",	// returns true or false
			"enable"			// set field enable state (true or false), return true if enabled
		});
		CC_scripts = Arrays.asList("cloak1", "cloak2", "uncloak");
	}
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		
		if (worldObj.isRemote) {
			return;
		}
		
		// Reset sound timer
		soundTicks--;
		if (soundTicks < 0) {
			soundTicks = CLOAKING_CORE_SOUND_UPDATE_TICKS;
			soundPlayed = false;
		}
		
		boolean isRefreshNeeded = false;
		
		updateTicks--;
		if (updateTicks <= 0) {
			if (WarpDriveConfig.LOGGING_CLOAKING) {
				WarpDrive.logger.info(this + " Updating cloaking state...");
			}
			updateTicks = ((tier == 1) ? 20 : (tier == 2) ? 10 : 20) * WarpDriveConfig.CLOAKING_FIELD_REFRESH_INTERVAL_SECONDS; // resetting timer
			
			isRefreshNeeded = validateAssembly();
			
			isCloaking = WarpDrive.cloaks.isAreaExists(worldObj, xCoord, yCoord, zCoord); 
			if (!isEnabled) {// disabled
				if (isCloaking) {// disabled, cloaking => stop cloaking
					if (WarpDriveConfig.LOGGING_CLOAKING) {
						WarpDrive.logger.info(this + " Disabled, cloak field going down...");
					}
					disableCloakingField();
					isRefreshNeeded = true;
				} else {// disabled, not cloaking
					// IDLE
					if (isRefreshNeeded) {
						setCoilsState(false);
					}
				}
				
			} else {// isEnabled
				updateVolumeAndEnergyRequired();
				final boolean hasEnoughPower = energy_consume(energyRequired, false);
				if (!isCloaking) {// enabled, not cloaking
					if (hasEnoughPower && isValid) {// enabled, can cloak and able to
						setCoilsState(true);
						isRefreshNeeded = true;
						
						// Register cloak
						WarpDrive.cloaks.updateCloakedArea(worldObj,
								worldObj.provider.dimensionId, xCoord, yCoord, zCoord, tier,
								minX, minY, minZ, maxX, maxY, maxZ);
						if (!soundPlayed) {
							soundPlayed = true;
							worldObj.playSoundEffect(xCoord + 0.5f, yCoord + 0.5f, zCoord + 0.5f, "warpdrive:cloak", 4F, 1F);
						}
						
						// Refresh the field
						CloakedArea area = WarpDrive.cloaks.getCloakedArea(worldObj, xCoord, yCoord, zCoord);
						if (area != null) {
							area.sendCloakPacketToPlayersEx(false); // re-cloak field
						} else {
							if (WarpDriveConfig.LOGGING_CLOAKING) {
								WarpDrive.logger.info("getCloakedArea1 returned null for " + worldObj + " " + xCoord + "," + yCoord + "," + zCoord);
							}
						}
						
					} else {// enabled, not cloaking and not able to
						// IDLE
						setCoilsState(false);
					}
					
				} else {// enabled & cloaking
					if (!isValid) {// enabled, cloaking but invalid
						if (WarpDriveConfig.LOGGING_CLOAKING) {
							WarpDrive.logger.info(this + " Coil(s) lost, cloak field is collapsing...");
						}
						energy_consume(energy_getEnergyStored());
						disableCloakingField();
						isRefreshNeeded = true;
						
					} else {// enabled, cloaking and valid
						if (hasEnoughPower) {// enabled, cloaking and able to
							// IDLE
							// Refresh the field (workaround to re-synchronize players since client may 'eat up' the packets)
							CloakedArea area = WarpDrive.cloaks.getCloakedArea(worldObj, xCoord, yCoord, zCoord);
							if (area != null) {
								area.sendCloakPacketToPlayersEx(false); // re-cloak field
							} else {
								if (WarpDriveConfig.LOGGING_CLOAKING) {
									WarpDrive.logger.info("getCloakedArea2 returned null for " + worldObj + " " + xCoord + "," + yCoord + "," + zCoord);
								}
							}
							setCoilsState(true);
							
						} else {// loosing power
							if (WarpDriveConfig.LOGGING_CLOAKING) {
								WarpDrive.logger.info(this + " Low power, cloak field is collapsing...");
							}
							disableCloakingField();
							isRefreshNeeded = true;
						}
					}
				}
			}
		}
		
		laserDrawingTicks--;
		if (isRefreshNeeded || laserDrawingTicks < 0) {
			laserDrawingTicks = LASER_REFRESH_TICKS;
			
			if (isEnabled && isValid) {
				drawLasers();
			}
		}
	}
	
	private void setCoilsState(final boolean enabled) {
		worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, (enabled) ? 1 : 0, 2);
		
		for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
			if (isValidInnerCoils[direction.ordinal()]) {
				setCoilState(DISTANCE_INNER_COILS_BLOCKS, direction, enabled);
			}
			if (distanceOuterCoils_blocks[direction.ordinal()] > 0) {
				setCoilState(distanceOuterCoils_blocks[direction.ordinal()], direction, enabled);
			}
		}
	}
	
	private void setCoilState(final int distance, final ForgeDirection direction, final boolean enabled) {
		final int x = xCoord + distance * direction.offsetX;
		final int y = yCoord + distance * direction.offsetY;
		final int z = zCoord + distance * direction.offsetZ;
		if (worldObj.getBlock(x, y, z).isAssociatedBlock(WarpDrive.blockCloakingCoil)) {
			final int metadata_current = worldObj.getBlockMetadata(x, y, z);
			final int metadata_new;
			if (distance == DISTANCE_INNER_COILS_BLOCKS) {
				metadata_new = (enabled ? 9 : 1);
			} else {
				metadata_new = (enabled ? 10 : 2) + direction.ordinal();
			}
			if (metadata_current != metadata_new) {
				worldObj.setBlockMetadataWithNotify(x, y, z, metadata_new, 2);
			}
		}
	}
	
	private void drawLasers() {
		float r = 0.0f;
		float g = 1.0f;
		float b = 0.0f;
		if (!isCloaking) {// out of energy
			r = 0.75f;
			g = 0.50f;
			b = 0.50f;
		} else if (tier == 1) {
			r = 0.00f;
			g = 1.00f;
			b = 0.25f;
		} else if (tier == 2) {
			r = 0.00f;
			g = 0.25f;
			b = 1.00f;
		}
		
		// Directions to check (all six directions: left, right, up, down, front, back)
		for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
			if ( isValidInnerCoils[direction.ordinal()]
			  && distanceOuterCoils_blocks[direction.ordinal()] > 0) {
				PacketHandler.sendBeamPacketToPlayersInArea(worldObj,
				        new Vector3(
				                   xCoord + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * direction.offsetX,
				                   yCoord + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * direction.offsetY,
				                   zCoord + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * direction.offsetZ),
				        new Vector3(
				                   xCoord + 0.5D + distanceOuterCoils_blocks[direction.ordinal()] * direction.offsetX,
				                   yCoord + 0.5D + distanceOuterCoils_blocks[direction.ordinal()] * direction.offsetY,
				                   zCoord + 0.5D + distanceOuterCoils_blocks[direction.ordinal()] * direction.offsetZ),
				        r, g, b, LASER_DURATION_TICKS, 0,
				        AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ));
			}
		}
		
		// draw connecting coils
		for (int i = 0; i < 5; i++) {
			final ForgeDirection start = ForgeDirection.VALID_DIRECTIONS[i];
			for (int j = i + 1; j < 6; j++) {
				final ForgeDirection stop = ForgeDirection.VALID_DIRECTIONS[j];
				// skip mirrored coils (removing the inner lines)
				if (start.getOpposite() == stop) {
					continue;
				}
				
				// draw a random colored beam
				int mapIndex = worldObj.rand.nextInt(innerCoilColor_b.length);
				r = innerCoilColor_r[mapIndex];
				g = innerCoilColor_g[mapIndex];
				b = innerCoilColor_b[mapIndex];
				
				PacketHandler.sendBeamPacketToPlayersInArea(worldObj,
					new Vector3(
						xCoord + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * start.offsetX + 0.2D * stop .offsetX,
						yCoord + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * start.offsetY + 0.2D * stop .offsetY,
						zCoord + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * start.offsetZ + 0.2D * stop .offsetZ),
					new Vector3(
						xCoord + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * stop .offsetX + 0.2D * start.offsetX,
						yCoord + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * stop .offsetY + 0.2D * start.offsetY,
						zCoord + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * stop .offsetZ + 0.2D * start.offsetZ),
					r, g, b, LASER_DURATION_TICKS, 0,
					AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ));
			}
		}
	}
	
	public void disableCloakingField() {
		setCoilsState(false);
		if (WarpDrive.cloaks.isAreaExists(worldObj, xCoord, yCoord, zCoord)) {
			WarpDrive.cloaks.removeCloakedArea(worldObj.provider.dimensionId, xCoord, yCoord, zCoord);
			
			if (!soundPlayed) {
				soundPlayed = true;
				worldObj.playSoundEffect(xCoord + 0.5f, yCoord + 0.5f, zCoord + 0.5f, "warpdrive:decloak", 4F, 1F);
			}
		}
	}
	
	public void updateVolumeAndEnergyRequired() {
		int x, y, z;
		int energyRequired_new;
		int volume_new = 0;
		if (tier == 1) {// tier1 = gaz and air blocks don't count
			for (y = minY; y <= maxY; y++) {
				for (x = minX; x <= maxX; x++) {
					for(z = minZ; z <= maxZ; z++) {
						if (!worldObj.isAirBlock(x, y, z)) {
							volume_new++;
						} 
					}
				}
			}
			energyRequired_new = volume_new * WarpDriveConfig.CLOAKING_TIER1_ENERGY_PER_BLOCK;
		} else {// tier2 = everything counts
			for (y = minY; y <= maxY; y++) {
				for (x = minX; x <= maxX; x++) {
					for(z = minZ; z <= maxZ; z++) {
						if (worldObj.getBlock(x, y, z) != Blocks.air) {
							volume_new++;
						} 
					}
				}
			}
			energyRequired_new = volume_new * WarpDriveConfig.CLOAKING_TIER2_ENERGY_PER_BLOCK;
		}
		
		volume = volume_new;
		energyRequired = energyRequired_new;
		
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s Requiring %d EU for %d blocks",
			                                    this, energyRequired, volume));
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		tier = tag.getByte("tier");
		isEnabled = tag.getBoolean("enabled");
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setByte("tier", tier);
		tag.setBoolean("enabled", isEnabled);
	}
	
	public boolean validateAssembly() {
		final int maxOuterCoilDistance = WarpDriveConfig.CLOAKING_MAX_FIELD_RADIUS - WarpDriveConfig.CLOAKING_COIL_CAPTURE_BLOCKS;
		boolean isRefreshNeeded = false;
		int countIntegrity = 1; // 1 for the core + 1 per coil
		final StringBuilder messageInnerCoils = new StringBuilder();
		final StringBuilder messageOuterCoils = new StringBuilder();
		
		// Directions to check (all six directions: left, right, up, down, front, back)
		for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
			
			// check validity of inner coil
			int x = xCoord + DISTANCE_INNER_COILS_BLOCKS * direction.offsetX;
			int y = yCoord + DISTANCE_INNER_COILS_BLOCKS * direction.offsetY;
			int z = zCoord + DISTANCE_INNER_COILS_BLOCKS * direction.offsetZ;
			final boolean isInnerValid = (worldObj.getBlock(x, y, z) == WarpDrive.blockCloakingCoil);
			
			// whenever a change is detected, force a laser redraw 
			if (isInnerValid != isValidInnerCoils[direction.ordinal()]) {
				isRefreshNeeded = true;
				isValidInnerCoils[direction.ordinal()] = isInnerValid;
			}
			
			// update validity results
			if (isValidInnerCoils[direction.ordinal()]) {
				countIntegrity++;
			} else {
				if (messageInnerCoils.length() != 0) {
					messageInnerCoils.append(", ");
				}
				messageInnerCoils.append(direction.name());
			}
			
			// find closest outer coil
			int newCoilDistance = 0;
			for (int distance = DISTANCE_INNER_COILS_BLOCKS + 1; distance < maxOuterCoilDistance; distance++) {
				x += direction.offsetX;
				y += direction.offsetY;
				z += direction.offsetZ;
				
				if (worldObj.getBlock(x, y, z) == WarpDrive.blockCloakingCoil) {
					newCoilDistance = distance;
					break;
				}
			}
			
			// whenever a change is detected, disable previous outer coil if it was valid and force a laser redraw
			final int oldCoilDistance = distanceOuterCoils_blocks[direction.ordinal()];
			if (newCoilDistance != oldCoilDistance) {
				if (oldCoilDistance > 0) {
					final int oldX = xCoord + oldCoilDistance * direction.offsetX;
					final int oldY = yCoord + oldCoilDistance * direction.offsetY;
					final int oldZ = zCoord + oldCoilDistance * direction.offsetZ;
					if (worldObj.getBlock(oldX, oldY, oldZ) == WarpDrive.blockCloakingCoil) {
						worldObj.setBlockMetadataWithNotify(oldX, oldY, oldZ, 0, 2);
					}
				}
				isRefreshNeeded = true;
				distanceOuterCoils_blocks[direction.ordinal()] = Math.max(0, newCoilDistance);
			}
			
			// update validity results
			if (newCoilDistance > 0) {
				countIntegrity++;
			} else {
				if (messageOuterCoils.length() != 0) {
					messageOuterCoils.append(", ");
				}
				messageOuterCoils.append(direction.name());
			}
		}
		
		// build status message
		final float integrity = countIntegrity / 13.0F; 
		if (messageInnerCoils.length() > 0 && messageOuterCoils.length() > 0) {
			messageValidityIssues = StatCollector.translateToLocalFormatted("warpdrive.cloakingCore.missingInnerAndOuter", 
					Math.round(100.0F * integrity), messageInnerCoils, messageOuterCoils); 
		} else if (messageInnerCoils.length() > 0) {
			messageValidityIssues = StatCollector.translateToLocalFormatted("warpdrive.cloakingCore.missingInner",
			        Math.round(100.0F * integrity), messageInnerCoils);
		} else if (messageOuterCoils.length() > 0) {
			messageValidityIssues = StatCollector.translateToLocalFormatted("warpdrive.cloakingCore.missingOuter",
					Math.round(100.0F * integrity), messageOuterCoils);
		} else {
			messageValidityIssues = StatCollector.translateToLocalFormatted("warpdrive.cloakingCore.valid");
		}
		
		// Update cloaking field parameters defined by coils
		isValid = countIntegrity >= 13;
		minX =               xCoord - distanceOuterCoils_blocks[4] - WarpDriveConfig.CLOAKING_COIL_CAPTURE_BLOCKS;
		maxX =               xCoord + distanceOuterCoils_blocks[5] + WarpDriveConfig.CLOAKING_COIL_CAPTURE_BLOCKS;
		minY = Math.max(  0, yCoord - distanceOuterCoils_blocks[0] - WarpDriveConfig.CLOAKING_COIL_CAPTURE_BLOCKS);
		maxY = Math.min(255, yCoord + distanceOuterCoils_blocks[1] + WarpDriveConfig.CLOAKING_COIL_CAPTURE_BLOCKS);
		minZ =               zCoord - distanceOuterCoils_blocks[2] - WarpDriveConfig.CLOAKING_COIL_CAPTURE_BLOCKS;
		maxZ =               zCoord + distanceOuterCoils_blocks[3] + WarpDriveConfig.CLOAKING_COIL_CAPTURE_BLOCKS;
		
		return isRefreshNeeded;
	}
	
	@Override
	public String getStatus() {
		if (worldObj == null) {
			return super.getStatus();
		}
		
		final String unlocalizedStatus;
		if (!isValid) {
			unlocalizedStatus = messageValidityIssues;
		} else if (!isEnabled) {
			unlocalizedStatus = "warpdrive.cloakingCore.disabled";
		} else if (!isCloaking) {
			unlocalizedStatus = "warpdrive.cloakingCore.lowPower";
		} else {
			unlocalizedStatus = "warpdrive.cloakingCore.cloaking";
		}
		return super.getStatus()
		    + "\n" + StatCollector.translateToLocalFormatted(unlocalizedStatus,
				tier,
				volume);
	}
	
	// Common OC/CC methods
	public Object[] tier(Object[] arguments) {
		if (arguments.length == 1) {
			int tier_new;
			try {
				tier_new = Commons.toInt(arguments[0]);
			} catch(Exception exception) {
				return new Integer[] { (int) tier };
			}
			if (tier_new == 2) {
				tier = 2;
			} else {
				tier = 1;
			}
			markDirty();
		}
		return new Integer[] { (int) tier };
	}
	
	public Object[] isAssemblyValid() {
		return new Object[] { isValid, Commons.removeFormatting(messageValidityIssues) };
	}
	
	public Object[] enable(Object[] arguments) {
		if (arguments.length == 1) {
			isEnabled = Commons.toBool(arguments[0]);
			markDirty();
		}
		return new Object[] { isEnabled };
	}
	
	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] tier(Context context, Arguments arguments) {
		return tier(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] isAssemblyValid(Context context, Arguments arguments) {
		return isAssemblyValid();
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] enable(Context context, Arguments arguments) {
		return enable(argumentsOCtoCC(arguments));
	}
	
	// ComputerCraft IPeripheral methods implementation
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) {
		final String methodName = getMethodName(method);
		
		switch (methodName) {
		case "tier":
			return tier(arguments);
			
		case "isAssemblyValid":
			return isAssemblyValid();
			
		case "enable":
			return enable(arguments);
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
	
	@Override
	public int energy_getMaxStorage() {
		return WarpDriveConfig.CLOAKING_MAX_ENERGY_STORED;
	}
	
	@Override
	public boolean energy_canInput(ForgeDirection from) {
		return true;
	}
}
