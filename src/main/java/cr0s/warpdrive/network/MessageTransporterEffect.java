package cr0s.warpdrive.network;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.movement.TileEntityTransporterCore;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EntityFXRegistry;
import cr0s.warpdrive.data.GlobalPosition;
import cr0s.warpdrive.data.MovingEntity;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.network.PacketHandler.IMessage;
import cr0s.warpdrive.render.AbstractEntityFX;
import cr0s.warpdrive.render.EntityFXDot;
import cr0s.warpdrive.render.EntityFXEnergizing;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent.Context;

import java.util.ArrayList;
import java.util.Collection;

public class MessageTransporterEffect implements IMessage {
	
	private boolean isTransporterRoom;
	private GlobalPosition globalPosition;
	private ArrayList<Integer> idEntities;
	private ArrayList<Vector3> v3EntityPositions;
	private double lockStrength;
	private int tickEnergizing;
	private int tickCooldown;
	
	@SuppressWarnings("unused")
	public MessageTransporterEffect() {
		// required on receiving side
	}
	
	public MessageTransporterEffect(final boolean isTransporterRoom, final GlobalPosition globalPosition,
									final Collection<MovingEntity> movingEntities,
									final double lockStrength, final int tickEnergizing, final int tickCooldown) {
		this.isTransporterRoom = isTransporterRoom;
		this.globalPosition = globalPosition;
		if ( movingEntities == null
		  || movingEntities.isEmpty() ) {
			this.idEntities = null;
			this.v3EntityPositions = null;
		} else {
			idEntities = new ArrayList<>(movingEntities.size());
			v3EntityPositions = new ArrayList<>(movingEntities.size());
			for (final MovingEntity movingEntity : movingEntities) {
				if (movingEntity == MovingEntity.INVALID) {
					continue;
				}
				final Entity entity = movingEntity.getEntity();
				if (entity != null) {
					idEntities.add(entity.getEntityId());
					v3EntityPositions.add(movingEntity.v3OriginalPosition);
				}
			}
		}
		this.lockStrength = lockStrength;
		this.tickEnergizing = tickEnergizing;
		this.tickCooldown = tickCooldown;
	}
	
	@Override
	public void decode(@Nonnull final PacketBuffer buffer) {
		isTransporterRoom = buffer.readBoolean();
		
		final ResourceLocation dimensionId = new ResourceLocation(buffer.readString());
		final int x = buffer.readInt();
		final int y = buffer.readInt();
		final int z = buffer.readInt();
		globalPosition = new GlobalPosition(dimensionId, x, y, z);
		
		final int countEntities = buffer.readByte();
		idEntities = new ArrayList<>(countEntities);
		v3EntityPositions = new ArrayList<>(countEntities);
		for (int indexEntity = 0; indexEntity < countEntities; indexEntity++) {
			final int idEntity = buffer.readInt();
			idEntities.add(idEntity);
			
			final double xEntity = buffer.readDouble();
			final double yEntity = buffer.readDouble();
			final double zEntity = buffer.readDouble();
			v3EntityPositions.add(new Vector3(xEntity, yEntity, zEntity));
		}
		
		lockStrength = buffer.readFloat();
		tickEnergizing = buffer.readShort();
		tickCooldown = buffer.readShort();
	}
	
	@Override
	public void encode(@Nonnull final PacketBuffer buffer) {
		buffer.writeBoolean(isTransporterRoom);
		
		buffer.writeString(globalPosition.dimensionId.toString());
		buffer.writeInt(globalPosition.x);
		buffer.writeInt(globalPosition.y);
		buffer.writeInt(globalPosition.z);
		
		final int countEntities = idEntities == null ? 0 : idEntities.size();
		buffer.writeByte(countEntities);
		for (int indexEntity = 0; indexEntity < countEntities; indexEntity++) {
			buffer.writeInt(idEntities.get(indexEntity));
			final Vector3 v3EntityPosition = v3EntityPositions.get(indexEntity);
			buffer.writeDouble(v3EntityPosition.x);
			buffer.writeDouble(v3EntityPosition.y);
			buffer.writeDouble(v3EntityPosition.z);
		}
		
		buffer.writeFloat((float) lockStrength);
		buffer.writeShort(tickEnergizing);
		buffer.writeShort(tickCooldown);
	}
	
	@OnlyIn(Dist.CLIENT)
	private void handle(final World world) {
		// adjust render distance
		final int maxRenderDistance = Minecraft.getInstance().gameSettings.renderDistanceChunks * 16;
		final long maxRenderDistance_squared = (long) maxRenderDistance * maxRenderDistance;
		
		final PlayerEntity player = Minecraft.getInstance().player;
		assert player != null;
		
		// handle source
		if (globalPosition.distance2To(player) > maxRenderDistance_squared) {
			return;
		}
		
		// add flying particles in area of effect when in the wild
		if (!isTransporterRoom) {
			spawnParticlesInArea(world, globalPosition.getBlockPos(), false,
					0.4F, 0.7F, 0.9F,
					0.10F, 0.15F, 0.10F);
		} else {
			spawnParticlesInTransporterRoom(world, globalPosition.getBlockPos(), false,
			                                0.4F, 0.7F, 0.9F,
			                                0.10F, 0.15F, 0.10F);
		}
		
		// get actual entity
		for (int indexEntity = 0; indexEntity < idEntities.size(); indexEntity++) {
			final Entity entity = world.getEntityByID(idEntities.get(indexEntity));
			
			// energizing
			if (entity != null) {
				// check existing particle at position
				final Vector3 v3Position = v3EntityPositions.get(indexEntity).clone();
				AbstractEntityFX effect = EntityFXRegistry.get(v3Position, 0.5D);
				if (effect == null) {
					// compute height with a margin
					final Vector3 v3Target = v3Position.clone().translate(Direction.UP, entity.getHeight() + 0.5F);
					// add particle to world
					effect = new EntityFXEnergizing(world, v3Position, v3Target,
					                                0.35F + 0.05F * world.rand.nextFloat(),
					                                0.50F + 0.15F * world.rand.nextFloat(),
					                                0.85F + 0.10F * world.rand.nextFloat(),
					                                20, entity.getWidth() );
					Minecraft.getInstance().particles.addEffect(effect);
					EntityFXRegistry.add(effect);
				} else {
					effect.refresh();
				}
			}
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	private void spawnParticlesInArea(final World world, final BlockPos blockPosCenter, final boolean isFalling,
	                                  final float redBase, final float greenBase, final float blueBase,
	                                  final float redFactor, final float greenFactor, final float blueFactor) {
		
		// compute area of effect
		final AxisAlignedBB aabb = new AxisAlignedBB(
				blockPosCenter.getX() - WarpDriveConfig.TRANSPORTER_ENTITY_GRAB_RADIUS_BLOCKS,
				blockPosCenter.getY() - 1.0D,
				blockPosCenter.getZ() - WarpDriveConfig.TRANSPORTER_ENTITY_GRAB_RADIUS_BLOCKS,
				blockPosCenter.getX() + WarpDriveConfig.TRANSPORTER_ENTITY_GRAB_RADIUS_BLOCKS + 1.0D,
				blockPosCenter.getY() + 2.0D,
				blockPosCenter.getZ() + WarpDriveConfig.TRANSPORTER_ENTITY_GRAB_RADIUS_BLOCKS + 1.0D );
		
		final Vector3 v3Motion = new Vector3(0.0D, 0.0D, 0.0D);
		final Vector3 v3Acceleration = new Vector3(0.0D, isFalling ? -0.001D : 0.001D, 0.0D);
		final double yRange = aabb.maxY - aabb.minY;
		
		// adjust quantity to lockStrength
		final int quantityInArea = (int) Commons.clamp(1, 5, Math.round(Commons.interpolate(0.2D, 1.0D, 0.8D, 5.0D, lockStrength)));
		for (int count = 0; count < quantityInArea; count++) {
			// start preferably from top or bottom side
			double y;
			if (isFalling) {
				y = aabb.maxY - Math.pow(world.rand.nextDouble(), 3.0D) * yRange;
			} else {
				y = aabb.minY + Math.pow(world.rand.nextDouble(), 3.0D) * yRange;
			}
			final Vector3 v3Position = new Vector3(
					aabb.minX + world.rand.nextDouble() * (aabb.maxX - aabb.minX),
					y,
					aabb.minZ + world.rand.nextDouble() * (aabb.maxZ - aabb.minZ));
			
			// adjust to block presence
			if ( ( isFalling && MathHelper.floor(y) == MathHelper.floor(aabb.maxY))
			  || (!isFalling && MathHelper.floor(y) == MathHelper.floor(aabb.minY)) ) {
				final BlockPos position = new BlockPos(MathHelper.floor(v3Position.x),
				                                       MathHelper.floor(v3Position.y),
				                                       MathHelper.floor(v3Position.z));
				final BlockState blockState = world.getBlockState(position);
				final Block block = blockState.getBlock();
				if ( !block.isAir(blockState, world, position)
				  && blockState.isOpaqueCube(world, position) ) {
					y += isFalling ? -1.0D : 1.0D;
					v3Position.y = y;
				}
			}
			
			// add particle
			final Particle effect = new EntityFXDot(world, v3Position,
			                                        v3Motion, v3Acceleration, 0.98D,
			                                        30);
			effect.setColor(redBase   + redFactor   * world.rand.nextFloat(),
			                greenBase + greenFactor * world.rand.nextFloat(),
			                blueBase  + blueFactor  * world.rand.nextFloat() );
			effect.setAlphaF(1.0F);
			Minecraft.getInstance().particles.addEffect(effect);
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	private void spawnParticlesInTransporterRoom(final World world, final BlockPos blockPosTransporter, final boolean isFalling,
	                                             final float redBase, final float greenBase, final float blueBase,
	                                             final float redFactor, final float greenFactor, final float blueFactor) {
		
		final TileEntity tileEntity = world.getTileEntity(blockPosTransporter);
		if (!(tileEntity instanceof TileEntityTransporterCore)) {
			if (Commons.throttleMe(String.format("spawnParticlesInTransporterRoom %s", Commons.format(world, blockPosTransporter)))) {
				WarpDrive.logger.warn(String.format("Missing transporter core at %s, is chunk loaded yet? %s",
				                                    blockPosTransporter, tileEntity ));
			}
			return;
		}
		
		final Vector3 v3Motion = new Vector3(0.0D, 0.0D, 0.0D);
		final Vector3 v3Acceleration = new Vector3(0.0D, isFalling ? -0.001D : 0.001D, 0.0D);
		final double yRange = 2.5D;
		
		final Collection<BlockPos> vContainments = ((TileEntityTransporterCore) tileEntity).getContainments();
		if (vContainments == null) {
			if (Commons.throttleMe("noContainmentsBlock")) {
				WarpDrive.logger.error(String.format("No containment block identified for transporter core at %s",
				                                     blockPosTransporter ));
			}
			return;
		}
		for (final BlockPos vContainment : vContainments) {
			if (world.rand.nextFloat() < 0.85F) {
				continue;
			}
			
			// start preferably from top or bottom side
			final double y;
			if (isFalling) {
				y = vContainment.getY() + 0.5D - Math.pow(world.rand.nextDouble(), 3.0D) * yRange;
			} else {
				y = vContainment.getY() + 0.5D + Math.pow(world.rand.nextDouble(), 3.0D) * yRange;
			}
			final Vector3 v3Position = new Vector3(
					vContainment.getX() + world.rand.nextDouble(),
					y,
					vContainment.getZ() + world.rand.nextDouble());
			
			// add particle
			final Particle effect = new EntityFXDot(world, v3Position,
			                                        v3Motion, v3Acceleration, 0.98D,
			                                        30);
			effect.setColor(redBase   + redFactor   * world.rand.nextFloat(),
			                greenBase + greenFactor * world.rand.nextFloat(),
			                blueBase  + blueFactor  * world.rand.nextFloat() );
			effect.setAlphaF(1.0F);
			Minecraft.getInstance().particles.addEffect(effect);
		}
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public IMessage process(@Nonnull final Context context) {
		// skip in case player just logged in
		if (Minecraft.getInstance().world == null) {
			WarpDrive.logger.error("WorldObj is null, ignoring particle packet");
			return null;
		}
		
		if (WarpDriveConfig.LOGGING_EFFECTS) {
			WarpDrive.logger.info(String.format("Received transporter effect isTransporterRoom %s at %s towards %d entities, with %.3f lockStrength, energizing in %d ticks, cool down for %d ticks",
			                      isTransporterRoom, globalPosition,
			                      idEntities.size(),
			                      lockStrength, tickEnergizing, tickCooldown));
		}
		
		handle(Minecraft.getInstance().world);
		
		return null;	// no response
	}
}
