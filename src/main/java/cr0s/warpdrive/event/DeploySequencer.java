package cr0s.warpdrive.event;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.ISequencerCallbacks;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.block.movement.TileEntityShipCore;
import cr0s.warpdrive.data.EnumShipMovementType;
import cr0s.warpdrive.data.JumpShip;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class DeploySequencer extends JumpSequencer {
	
	private String playerNameRequester;
	private boolean isRequesterCaptain = false;
	private ISequencerCallbacks callback;
	
	/*
	public DeploySequencer(final TileEntityShipCore shipCore, final EnumShipMovementType shipMovementType, final String nameTarget,
	                       final int moveX, final int moveY, final int moveZ, final byte rotationSteps,
	                       final int destX, final int destY, final int destZ) {
		super(shipCore, shipMovementType, nameTarget, moveX, moveY, moveZ, rotationSteps, destX, destY, destZ);
	}
	/**/
	
	public DeploySequencer(final JumpShip jumpShip, final World world, final boolean isInstantiated,
	                       final int destX, final int destY, final int destZ, final byte rotationSteps) {
		super(jumpShip, world, isInstantiated ? EnumShipMovementType.INSTANTIATE : EnumShipMovementType.RESTORE, destX, destY, destZ, rotationSteps);
	}
	
	public void setRequester(final String playerName, final boolean isCaptain) {
		this.playerNameRequester = playerName;
		this.isRequesterCaptain = isCaptain;
		addPlayerToEntities(playerName);
	}
	
	public void setCallback(final ISequencerCallbacks object) {
		this.callback = object;
	}
	
	@Override
	public void disable(final boolean isSuccessful, final WarpDriveText reason) {
		super.disable(isSuccessful, reason);
		callback.sequencer_finished();
	}
	
	@Override
	protected void state_removeBlocks() {
		// skip removal in deployment mode
		actualIndexInShip = ship.jumpBlocks.length;
	}
	
	@Override
	protected boolean state_chunkReleasing() {
		final boolean isDone = super.state_chunkReleasing();
		if (!isDone) {
			return false;
		}
		
		if (playerNameRequester != null) {
			// Warn owner if deployment done but wait next tick for teleportation
			final EntityPlayerMP entityPlayerMP = Commons.getOnlinePlayerByName(playerNameRequester);
			if (entityPlayerMP != null) {
				Commons.addChatMessage(entityPlayerMP, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.builder.guide.ship_deployed"));
			}
		}
		return true;
	}
	
	@Override
	protected void state_finishing() {
		if ( playerNameRequester != null && !playerNameRequester.isEmpty()
		  && isRequesterCaptain ) {
			final EntityPlayerMP entityPlayerMP = Commons.getOnlinePlayerByName(playerNameRequester);
			if (entityPlayerMP != null) {
				final TileEntity tileEntity = worldTarget.getTileEntity(new BlockPos(destX, destY, destZ));
				if (tileEntity instanceof TileEntityShipCore) {
					final boolean isSuccess = ((TileEntityShipCore) tileEntity).summonOwnerOnDeploy(entityPlayerMP);
					if (isSuccess) {
						Commons.addChatMessage(entityPlayerMP, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.builder.guide.welcome_aboard"));
					} else {
						WarpDrive.logger.warn(String.format("Failed to assign new captain %s",
						                                    playerNameRequester));
					}
				} else {
					WarpDrive.logger.warn(String.format("Unable to detect ship core after deployment, found %s",
					                                    tileEntity));
				}
			}
		}
		
		super.state_finishing();
	}
}
