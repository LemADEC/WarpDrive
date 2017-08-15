package cr0s.warpdrive.config;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.EventWarpDrive.Ship.MovementCosts;
import cr0s.warpdrive.data.EnumShipMovementType;

import net.minecraft.world.World;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;

public class ShipMovementCosts {
	
	public final int maximumDistance_blocks;
	public final int energyRequired;
	public final int warmup_seconds;
	public final int sickness_seconds;
	public final int cooldown_seconds;
	
	public ShipMovementCosts(final World world, final int x, final int y, final int z, 
	                         final int mass, final EnumShipMovementType shipMovementType, final int distance) {
		final Factors factorsForJumpParameters = WarpDriveConfig.SHIP_MOVEMENT_COSTS_FACTORS[shipMovementType.ordinal()];
		final int maximumDistance_blocks = Commons.clamp(0, 30000000, evaluate(mass, distance, factorsForJumpParameters.maximumDistance));
		final int energyRequired   = Commons.clamp(0, Integer.MAX_VALUE, evaluate(mass, distance, factorsForJumpParameters.energyRequired));
		final int warmup_seconds   = Commons.clamp(0, 3600, evaluate(mass, distance, factorsForJumpParameters.warmup));
		final int sickness_seconds = Commons.clamp(0, 3600, evaluate(mass, distance, factorsForJumpParameters.sickness));
		final int cooldown_seconds = Commons.clamp(0, 3600, evaluate(mass, distance, factorsForJumpParameters.cooldown));
		
		// post event allowing other mods to adjust it
		final MovementCosts movementCosts = new MovementCosts(world, x, y, z,
		                                                      mass, shipMovementType.getName(), distance,
		                                                      maximumDistance_blocks, energyRequired, warmup_seconds, sickness_seconds, cooldown_seconds);
		MinecraftForge.EVENT_BUS.post(movementCosts);
		
		this.maximumDistance_blocks = movementCosts.getMaximumDistance_blocks();
		this.energyRequired   = movementCosts.getEnergyRequired();
		this.warmup_seconds   = movementCosts.getWarmup_seconds();
		this.sickness_seconds = movementCosts.getSickness_seconds();
		this.cooldown_seconds = movementCosts.getCooldown_seconds();
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("Ship movement %s with mass %d over %d blocks is capped to %d blocks, will cost %d EU, %d s warmup, %d s sickness, %d s cooldown",
			                                    shipMovementType, mass, distance,
			                                    this.maximumDistance_blocks, this.energyRequired,
			                                    this.warmup_seconds, this.sickness_seconds, this.cooldown_seconds));
		}
	}
	
	private static int evaluate(final int mass, final int distance, final double[] factors) {
		if (factors.length != 5) {
			return Integer.MAX_VALUE;
		}
		final double value = factors[0]
		                     + factors[1] * mass
		                     + factors[2] * distance
		                     + factors[3] * Math.log(Math.max(1.0D, mass)) * (factors[4] != 0.0D ? Math.exp(distance / factors[4]) : 1.0D);
		return (int) Math.ceil(value);
	}
	
	public static class Factors {
		
		public double[] maximumDistance;
		public double[] energyRequired;
		public double[] warmup;
		public double[] sickness;
		public double[] cooldown;
		
		Factors(final double[] maximumDistanceDefault,
		        final double[] energyRequiredDefault,
		        final double[] warmupDefault,
		        final double[] sicknessDefault,
		        final double[] cooldownDefault) {
			maximumDistance = maximumDistanceDefault;
			energyRequired = energyRequiredDefault;
			warmup = warmupDefault;
			sickness = sicknessDefault;
			cooldown = cooldownDefault;
		}
		
		public void load(final Configuration config, final String category, final String prefixKey, final String comment) {
			final String COMMENT_FACTORS = "\n"
					+ "You need to provide exactly 5 values { A, B, C, D, E }. The equation used is A + B * mass + C * distance + D * ln( mass ) * exp( distance / E )\n"
					+ "Result is rounded up to an integer. Use 0 to ignore that part of the equation.";
			
			maximumDistance = WarpDriveConfig.getDoubleList(config, category, prefixKey + "_max_jump_distance",
			                                                "Maximum jump length value in blocks " + comment + "." + COMMENT_FACTORS,
			                                                maximumDistance);
			
			energyRequired = WarpDriveConfig.getDoubleList(config, category, prefixKey + "_energyRequired_factors",
			                                               "energy required measured in internal units " + comment + "." + COMMENT_FACTORS,
			                                               energyRequired);
			
			warmup = WarpDriveConfig.getDoubleList(config, category, prefixKey + "_warmup_seconds",
			                                       "Warmup seconds to wait before starting jump " + comment + "." + COMMENT_FACTORS,
			                                       warmup);
			
			sickness = WarpDriveConfig.getDoubleList(config, category, prefixKey + "_sickness_seconds",
			                                       "Motion sickness duration measured in seconds " + comment + "." + COMMENT_FACTORS,
			                                         sickness);
			
			cooldown = WarpDriveConfig.getDoubleList(config, category, prefixKey + "_cooldown_interval_seconds",
			                                         "Cooldown seconds to wait after jumping " + comment + "." + COMMENT_FACTORS,
			                                         cooldown);
		}
	}
}
