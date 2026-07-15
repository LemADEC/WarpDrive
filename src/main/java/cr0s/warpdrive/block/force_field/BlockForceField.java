package cr0s.warpdrive.block.force_field;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IDamageReceiver;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumPermissionNode;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.data.ForceFieldSetup;
import cr0s.warpdrive.data.Vector3;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class BlockForceField extends BlockAbstractForceField implements IDamageReceiver {
	
	private static final float BOUNDING_TOLERANCE = 0.05F;
	private static final VoxelShape SHAPE_FORCE_FIELD = VoxelShapes.create(
			    BOUNDING_TOLERANCE,     BOUNDING_TOLERANCE,     BOUNDING_TOLERANCE,
		    1 - BOUNDING_TOLERANCE, 1 - BOUNDING_TOLERANCE, 1 - BOUNDING_TOLERANCE );
	
	private final int frequency;
	
	public BlockForceField(@Nonnull final String registryName, @Nonnull final EnumTier enumTier, final int frequency) {
		super(getDefaultProperties(Material.GLASS)
				      .hardnessAndResistance(-1, WarpDriveConfig.HULL_BLAST_RESISTANCE[enumTier.getIndex()])
				      .sound(SoundType.CLOTH)
				      .notSolid(),
		      registryName, enumTier);
		
		this.frequency = frequency;
	}
	
	@Override
	public boolean propagatesSkylightDown(@Nonnull final BlockState blockState, @Nonnull final IBlockReader blockReader,
	                                      @Nonnull final BlockPos blockPos) {
		return true;
	}
	
	@SuppressWarnings("deprecation")
	@OnlyIn(Dist.CLIENT)
	@Override
	public boolean isSideInvisible(@Nonnull final BlockState blockState, @Nonnull final BlockState blockStateAdjacent, @Nonnull final Direction side) {
		return blockStateAdjacent.getBlock() == this
		    || super.isSideInvisible(blockState, blockStateAdjacent, side);
    }
    
	/* TODO MC1.15 force field camouflage (getMapColor + getExtendedState: needs the 1.15 MaterialColor/blockstate port; Forge removed IExtendedBlockState)
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public MapColor getMapColor(final IBlockState blockState, final IBlockAccess blockAccess, final BlockPos blockPos) {
		final IExtendedBlockState blockStateExtended = (IExtendedBlockState) getExtendedState(blockState, blockAccess, blockPos);
		final IBlockState blockStateCamouflage = blockStateExtended.getValue(BlockProperties.CAMOUFLAGE);
		if ( blockStateCamouflage != null
		  && blockStateCamouflage.getBlock() != Blocks.AIR ) {
			try {
				return blockStateCamouflage.getMapColor(blockAccess, blockPos);
			} catch (final Exception exception) {
				if (!Dictionary.BLOCKS_NOCAMOUFLAGE.contains(blockStateCamouflage.getBlock())) {
					exception.printStackTrace(WarpDrive.printStreamError);
					WarpDrive.logger.error(String.format("Exception trying to get MapColor for %s",
					                                     blockStateCamouflage));
					Dictionary.BLOCKS_NOCAMOUFLAGE.add(blockStateCamouflage.getBlock());
				}
			}
		}
		return MapColor.getBlockColor(EnumDyeColor.byMetadata(blockState.getValue(FREQUENCY)));
	}

	@Nonnull
	@Override
	public BlockState getExtendedState(@Nonnull final BlockState blockState, final IWorldReader worldReader, final BlockPos blockPos) {
		if (!(blockState instanceof IExtendedBlockState)) {
			return blockState;
		}
		final TileEntity tileEntity = worldReader.getTileEntity(blockPos);
		if (!(tileEntity instanceof TileEntityForceField)) {
			return blockState;
		}
		final TileEntityForceField tileEntityForceField = (TileEntityForceField) tileEntity;
		BlockState blockStateCamouflage = tileEntityForceField.cache_blockStateCamouflage;
		if (!Commons.isValidCamouflage(blockStateCamouflage)) {
			blockStateCamouflage = Blocks.AIR.getDefaultState();
		}
		return ((IExtendedBlockState) blockState)
		       .with(BlockProperties.CAMOUFLAGE, blockStateCamouflage);
	}
	*/
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean causesSuffocation(@Nonnull final BlockState blockState, @Nonnull final IBlockReader blockReader, @Nonnull final BlockPos blockPos) {
		return false;
	}
	
	@Nonnull
	@Override
	public ItemStack getPickBlock(@Nonnull final BlockState blockState, final RayTraceResult target,
	                              @Nonnull final IBlockReader world, @Nonnull final BlockPos blockPos, final PlayerEntity entityPlayer) {
		return new ItemStack(Blocks.AIR);
	}
	
	protected TileEntityForceFieldProjector getProjector(@Nonnull final World world, @Nonnull final BlockPos blockPos,
	                                                     @Nullable final TileEntityForceFieldProjector tileEntityForceFieldProjectorCandidate) {
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (tileEntity instanceof TileEntityForceField) {
			return ((TileEntityForceField) tileEntity).getProjector(tileEntityForceFieldProjectorCandidate);
		}
		return null;
	}
	
	@Nullable
	private ForceFieldSetup getForceFieldSetup(@Nonnull final IBlockReader blockReader, @Nonnull final BlockPos blockPos) {
		final TileEntity tileEntity = blockReader.getTileEntity(blockPos);
		if (tileEntity instanceof TileEntityForceField) {
			try {
				return ((TileEntityForceField) tileEntity).getForceFieldSetup();
			} catch (final Exception exception) {
				if (Commons.throttleMe("BlockForceField.getForceFieldSetup")) {
					WarpDrive.logger.error(String.format("Exception trying to get force field setup %s",
					                                     Commons.format(blockReader, blockPos) ));
					exception.printStackTrace(WarpDrive.printStreamError);
				}
			}
		}
		return null;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void onBlockClicked(@Nonnull final BlockState blockState, @Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final PlayerEntity entityPlayer) {
		if (world.isRemote()) {
			return;
		}
		final ForceFieldSetup forceFieldSetup = getForceFieldSetup(world, blockPos);
		if (forceFieldSetup != null) {
			forceFieldSetup.onEntityEffect(world, blockPos, entityPlayer);
		}
	}
	
	private boolean isAccessGranted(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final ForceFieldSetup forceFieldSetup) {
		boolean isAccessGranted = false;
		final List<PlayerEntity> entities = world.getEntitiesWithinAABB(PlayerEntity.class, new AxisAlignedBB(
				blockPos.getX() - 1.0D, blockPos.getY() - 1.0D, blockPos.getZ() - 1.0D,
				blockPos.getX() + 2.0D, blockPos.getY() + 2.0D, blockPos.getZ() + 2.0D), null);
		for (final PlayerEntity entityPlayer : entities) {
			if (entityPlayer != null) {
				if ( entityPlayer.isCreative()
				  || entityPlayer.isSpectator()
				  || forceFieldSetup.isAccessGranted(entityPlayer, EnumPermissionNode.SNEAK_THROUGH) ) {
					isAccessGranted = true;
					break;
				}
			}
		}
		return isAccessGranted;
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public VoxelShape getCollisionShape(@Nonnull final BlockState blockState, @Nonnull final IBlockReader blockReader, @Nonnull final BlockPos blockPos,
	                                    @Nonnull final ISelectionContext selectionContext) {
		final ForceFieldSetup forceFieldSetup = getForceFieldSetup(blockReader, blockPos);
		if ( forceFieldSetup != null
		  && blockReader instanceof World ) {// @TODO lag when placing force field due to permission checks?
			if (isAccessGranted((World) blockReader, blockPos, forceFieldSetup)) {
				return VoxelShapes.empty();
			}
		}
		
		return SHAPE_FORCE_FIELD;
	}
	
	@Override
	public float getSpeedFactor() {
		return super.getSpeedFactor();
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void onEntityCollision(@Nonnull final BlockState blockState, @Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final Entity entity) {
		super.onEntityCollision(blockState, world, blockPos, entity);
		
		doEntityCollision(world, blockPos, entity);
	}
	
	private void doEntityCollision(final World world, final BlockPos blockPos, final Entity entity) {
		if (world.isRemote()) {
			return;
		}
		
		final ForceFieldSetup forceFieldSetup = getForceFieldSetup(world, blockPos);
		if (forceFieldSetup != null) {
			forceFieldSetup.onEntityEffect(world, blockPos, entity);
			if ( entity instanceof LivingEntity
			  && entity.isAlive() ) {
				final Vector3 vCenter = new Vector3(blockPos).translate(0.5F);
				final AxisAlignedBB aabbEntity = entity.getBoundingBox();
				final Optional<Vec3d> optionalVecHit = aabbEntity.rayTrace(vCenter.toVec3d(), entity.getPositionVector());
				if (optionalVecHit.isPresent()) {
					
					final double distanceToCollision = optionalVecHit.get().distanceTo(vCenter.toVec3d());
					final double distanceToCenter = Math.sqrt(vCenter.distanceTo_square(entity));
					final double distanceMin = Math.min(distanceToCenter, distanceToCollision);
					
					// always slowdown
					if (distanceMin > 1.0D) {// keep it light when a bit away
						((LivingEntity) entity).addPotionEffect(new EffectInstance(Effects.SLOWNESS, 10, 0));
						return;
					}
					((LivingEntity) entity).addPotionEffect(new EffectInstance(Effects.SLOWNESS, 20, 1));
					
					// check the whitelist
					final boolean isAccessGranted = isAccessGranted(world, blockPos, forceFieldSetup);
					if (!isAccessGranted) {
						if (distanceMin < 0.50D - BOUNDING_TOLERANCE) {
							if (Commons.throttleMe("ForceFieldEntry" + entity.getEntityId())) {
								WarpDrive.logger.info(String.format("ForceField entry detected at %.3f m for %s %s",
								                                    distanceMin, entity, Commons.format(world, blockPos) ));
							}
							entity.attackEntityFrom(DamageSource.OUT_OF_WORLD, 6666.0F);
						} else {
							if ( entity instanceof PlayerEntity
							  && Commons.throttleMe("ForceFieldProximity" + entity.getEntityId()) ) {
								WarpDrive.logger.info(String.format("ForceField proximity detected at %.3f m for %s %s",
								                                    distanceMin, entity, Commons.format(world, blockPos)) );
							}
							((LivingEntity) entity).addPotionEffect(new EffectInstance(Effects.NAUSEA, 80, 3));
						}
					}
				}
			}
		}
	}
	
	@Override
	public int getLightValue(@Nonnull final BlockState blockState, @Nonnull final IBlockReader blockReader, @Nonnull final BlockPos blockPos) {
		final TileEntity tileEntity = blockReader.getTileEntity(blockPos);
		if (tileEntity instanceof TileEntityForceField) {
			return ((TileEntityForceField) tileEntity).cache_lightCamouflage;
		}
		
		return 0;
	}
	
	private void downgrade(final World world, final BlockPos blockPos) {
		if (enumTier.getIndex() > 1) {
			final TileEntityForceFieldProjector tileEntityForceFieldProjector = getProjector(world, blockPos, null);
			world.setBlockState(blockPos, WarpDrive.blockForceFields[enumTier.getIndex() - 1][(frequency + 1) % 16].getDefaultState(), 2);
			if (tileEntityForceFieldProjector != null) {
				final TileEntity tileEntity = world.getTileEntity(blockPos);
				if (tileEntity instanceof TileEntityForceField) {
					((TileEntityForceField) tileEntity).setProjector(tileEntityForceFieldProjector.getPos());
				}
			}
			
		} else {
			world.removeBlock(blockPos, false);
		}
	}
	
	// explosion handling, preferably without ASM
	private long previous_tickWorld = -1L;
	private DimensionType previous_idDimension = null;
	private Vec3d previous_vExplosion = new Vec3d(0.0D, -1.0D, 0.0D);
	
	@SuppressWarnings("deprecation")
	@Override
	public float getExplosionResistance() {
		if (Commons.isServerThread()) {
			if (Commons.throttleMe("getExplosionResistance")) {
				new RuntimeException("Invalid call to deprecated getExplosionResistance()").printStackTrace(WarpDrive.printStreamError);
			}
			return Float.MAX_VALUE;
		}
		return super.getExplosionResistance();
	}
	
	@Override
	public float getExplosionResistance(@Nonnull final BlockState blockState, @Nonnull final IWorldReader worldReader, @Nonnull final BlockPos blockPos,
	                                    @Nullable final Entity exploder, @Nonnull final Explosion explosion ) {
		final long tickWorld = worldReader instanceof World ? ((World) worldReader).getGameTime() : System.currentTimeMillis() / 50;
		final Vec3d vExplosion = explosion.getPosition();
		final boolean isFirstHit = Math.abs(tickWorld - previous_tickWorld) > 100L
		                        || !previous_idDimension.equals(worldReader.getDimension().getType())
		                        || Math.abs(previous_vExplosion.x - vExplosion.x) > 5.0D
		                        || Math.abs(previous_vExplosion.y - vExplosion.y) > 5.0D
		                        || Math.abs(previous_vExplosion.z - vExplosion.z) > 5.0D;
		if (isFirstHit) {
			previous_tickWorld = tickWorld;
			previous_idDimension = worldReader.getDimension().getType();
			previous_vExplosion = new Vec3d(vExplosion.x, vExplosion.y, vExplosion.z);
			WarpDrive.logger.info(String.format("Force field %s %s: explosion check of size %.3f from exploder %s %s %s explosion %s",
			                                    enumTier,
			                                    Commons.format(worldReader, blockPos),
			                                    explosion.size,
			                                    exploder != null ? exploder.getType().getRegistryName() : "-",
			                                    exploder != null ? exploder.getClass().toString() : "-",
			                                    exploder,
			                                    explosion ));
		}
		if (!Commons.isSafeThread())  {
			if (isFirstHit) {
				new ConcurrentModificationException(String.format("Bad multithreading detected %s from exploder %s explosion %s",
				                                                  Commons.format(worldReader, blockPos), exploder, explosion ))
						.printStackTrace(WarpDrive.printStreamError);
			} else {
				return Float.MAX_VALUE;
			}
		}
		
		// find explosion strength, defaults to no effect
		if ( exploder == null
		  && vExplosion.x == Math.rint(vExplosion.x)
		  && vExplosion.y == Math.rint(vExplosion.y)
		  && vExplosion.z == Math.rint(vExplosion.z) ) {
			final BlockPos blockPosExplosion = new BlockPos((int) vExplosion.x, (int) vExplosion.y, (int) vExplosion.z);
			// IC2 Reactor blowing up => block is already air
			assert blockState == worldReader.getBlockState(blockPosExplosion);
			final TileEntity tileEntity = worldReader.getTileEntity(blockPosExplosion);
			if (isFirstHit) {
				WarpDrive.logger.info(String.format("Force field %s %s: explosion from %s %s with tileEntity %s",
				                                    enumTier, Commons.format(worldReader, blockPos),
				                                    blockState.getBlock(), blockState.getBlock().getRegistryName(), tileEntity ));
			}
			// explosion with no entity and block removed, hence we can't compute the energy impact => boosting explosion resistance
			return Float.MAX_VALUE;
		}
		
		double strength = explosion.size;
		float factorResistance = 1.0F;
		
		// Typical size/strength values
		// Vanilla
		// net.minecraft.entity.item.EntityEnderCrystal 6.0        As of 1.12.2, there's no exploder, just a generic Explosion object
		// net.minecraft.entity.item.EntityMinecartTNT  4.0
		// net.minecraft.entity.item.EntityTNTPrimed    4.0 or 5.0 ?
		// net.minecraft.entity.monster.EntityCreeper   Normal is 3.0, powered ones are *2
		
		// WarpDrive
		// cr0s.warpdrive.entity.EntityLaserExploder    variable    Laser energy level at target is used to compute the explosion strength
		
		// Applied Energistics
		// appeng.entity.EntityTinyTNTPrimed            0.2
		
		// IC2
		// ic2.core.block.EntityItnt                    5.5 
		// ic2.core.block.EntityNuke                    5.0 to 60.0   Loadout does define the size
		// ic2.core.block.EntityDynamite                1.0
		// ic2.core.block.EntityStickyDynamite          1.0
		
		// ICBM Classic S-mine (initial explosion)
		// icbm.classic.content.entity.EntityExplosion  1.5
		
		// ICBM Classic Condensed, Incendiary, Repulsive, Attractive, Fragmentation, Sonic, Breaching, Thermobaric, Nuclear,
		// Exothermic, Endothermic, Anti-gravitational, Hypersonic, (Antimatter?)
		// icbm.classic.content.entity.EntityExplosive  10.0
		
		// ICBM Classic Fragmentation, S-mine fragments
		// icbm.classic.content.entity.EntityFragments  1.5
		// case "class icbm.classic.content.entity.EntityFragments": strength = 0.02D; break;
		
		// ICBM Classic Conventional, Attractive, Repulsive, Sonic, Breaching, Thermobaric, Nuclear, 
		// Exothermic, Endothermic, Anti-Gravitational, Hypersonic missile, (Antimatter?), (Red matter?), (Homing?), (Anti-Ballistic?)
		// icbm.classic.content.entity.EntityMissile    15.0 tbc
		
		// ICBM Classic Conventional/Incendiary/Repulsive grenade
		// icbm.classic.content.entity.EntityGrenade    3.0 tbc
		
		// TechGuns
		// note: that mod is sharing a Vanilla explosion with the player as exploder, so we don't see the mod itself directly
		// Rocket                                       5.0
		// Rocket (High Velocity)                       3.75
		// Tactical Nuke                                25.0
		
		if ( explosion.getClass().equals(Explosion.class)
		  && strength > WarpDriveConfig.FORCE_FIELD_EXPLOSION_STRENGTH_VANILLA_CAP) {
			// assuming its TechGuns, we caps it to be in par with ICBM Nuclear which actually simulate the shockwave
			factorResistance = (float) (strength / WarpDriveConfig.FORCE_FIELD_EXPLOSION_STRENGTH_VANILLA_CAP);
			strength = Math.min(WarpDriveConfig.FORCE_FIELD_EXPLOSION_STRENGTH_VANILLA_CAP, strength);
		}
		
		if (strength == 0.0D) {// (explosion with no size defined, let's check the explosion itself)
			final String nameExplosion = explosion.getClass().toString();
			switch (nameExplosion) {
			case "class icbm.classic.content.explosive.blast.threaded.BlastNuclear": strength = 15.0D; break;
			
			default:
				if (isFirstHit) {
					WarpDrive.logger.error(String.format("Blocking invalid explosion instance %s %s %s",
					                                     vExplosion, nameExplosion, explosion ));
				}
				return Float.MAX_VALUE;
			}
		}
		
		// apply damages to force field by consuming energy
		final Vector3 vDirection = new Vector3(blockPos.getX() + 0.5D - vExplosion.x,
		                                       blockPos.getY() + 0.5D - vExplosion.y,
		                                       blockPos.getZ() + 0.5D - vExplosion.z );
		final double magnitude = Math.max(1.0D, vDirection.getMagnitude());
		if (magnitude > strength) {
			if (isFirstHit) {
				WarpDrive.logger.error(String.format("Blocking out of range explosion instance %s %s at %.1f m",
				                                     vExplosion, explosion, magnitude ));
			}
			return Float.MAX_VALUE;
		}
		if (magnitude != 0) {// normalize
			vDirection.scale(1 / magnitude);
		}
		final double damageLevel = strength / (magnitude * magnitude) * 1.0D;
		double damageLeft = 0;
		final ForceFieldSetup forceFieldSetup = Commons.isSafeThread() ? getForceFieldSetup(worldReader, blockPos) : null;
		if (forceFieldSetup != null) {
			damageLeft = forceFieldSetup.applyDamage(worldReader, DamageSource.causeExplosionDamage(explosion), damageLevel);
		}
		
		assert damageLeft >= 0;
		if (isFirstHit && WarpDriveConfig.LOGGING_FORCE_FIELD) {
			WarpDrive.logger.info(String.format("Force field %s %s: explosion from %s strength %.3f magnitude %.3f damageLevel %.3f damageLeft %.3f",
			                                    enumTier, Commons.format(worldReader, blockPos),
			                                    vExplosion,
			                                    strength, magnitude, damageLevel, damageLeft));
		}
		return factorResistance * super.getExplosionResistance(blockState, worldReader, blockPos, exploder, explosion);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean canDropFromExplosion(@Nonnull final Explosion explosion) {
		return false;
	}
	
	@Override
	public boolean canEntityDestroy(final BlockState blockState, final IBlockReader blockReader, final BlockPos blockPos, final Entity entity) {
		return false;
	}
	
	@Override
	public void onBlockExploded(final BlockState blockState, final World world, @Nonnull final BlockPos blockPos, @Nonnull final Explosion explosion) {
		if (WarpDriveConfig.LOGGING_WEAPON) {
			WarpDrive.logger.warn(String.format("Force field %s %s has exploded in explosion %s at %s",
			                                    enumTier, Commons.format(world, blockPos),
			                                    explosion, explosion.getPosition()));
		}
		downgrade(world, blockPos);
		super.onBlockExploded(blockState, world, blockPos, explosion);
	}
	
	@Override
	public void onEMP(@Nonnull final World world, @Nonnull final BlockPos blockPos, final float efficiency) {
		if (efficiency * (1.0F - 0.20F * (enumTier.getIndex() - 1)) > world.rand.nextFloat()) {
			downgrade(world, blockPos);
		}
		// already handled => no ancestor call
	}
	
	@Override
	public void onExplosionDestroy(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final Explosion explosion) {
		// (block is already set to air by caller, see IC2 iTNT for example)
		downgrade(world, blockPos);
		super.onExplosionDestroy(world, blockPos, explosion);
	}
	
	@Override
	public float getBlockHardness(@Nonnull final BlockState blockState, @Nonnull final World world, @Nonnull final BlockPos blockPos,
	                              @Nonnull final DamageSource damageSource, final int damageParameter, @Nonnull final Vector3 damageDirection, final int damageLevel) {
		return WarpDriveConfig.HULL_HARDNESS[enumTier.getIndex()];
	}
	
	@Override
	public int applyDamage(@Nonnull final BlockState blockState, @Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final DamageSource damageSource,
	                       final int damageParameter, @Nonnull final Vector3 damageDirection, final int damageLevel) {
		final ForceFieldSetup forceFieldSetup = getForceFieldSetup(world, blockPos);
		if (forceFieldSetup != null) {
			return (int) Math.round(forceFieldSetup.applyDamage(world, damageSource, damageLevel));
		}
		
		return damageLevel;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public float getBlockHardness(@Nonnull final BlockState blockState, @Nonnull final IBlockReader blockReader, @Nonnull final BlockPos blockPos) {
		final String name = Thread.currentThread().getName();
		// hide unbreakable status from ICBM explosion handler (as of ICBM-classic-1.12.2-3.3.0b63, Nuclear skip unbreakable blocks)
		if (name.startsWith("ICBM")) {
			return WarpDriveConfig.HULL_HARDNESS[enumTier.getIndex()];
		}
		return super.getBlockHardness(blockState, blockReader, blockPos);
	}
}