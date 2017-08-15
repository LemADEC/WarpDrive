package cr0s.warpdrive.api;

import net.minecraft.world.World;

import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;

public abstract class EventWarpDrive extends Event {
	
	EventWarpDrive() {
		super();
	}
	
	public static abstract class Ship extends EventWarpDrive {
		
		// ship core location
		public final World worldCurrent;
		public final int xCurrent;
		public final int yCurrent;
		public final int zCurrent;
		
		// ship stats
		public final int mass;
		
		// movement description
		public final String jumpType;
		public final int distance;
		
		public Ship(final World world, final int x, final int y, final int z,
		            final int mass, final String jumpType, final int distance) {
			super();
			
			this.worldCurrent = world;
			this.xCurrent = x;
			this.yCurrent = y;
			this.zCurrent = z;
			this.mass = mass;
			this.jumpType = jumpType;
			this.distance = distance;
		}
		
		public static class MovementCosts extends Ship {
			
			// original values for reference
			public final int warmup_seconds_initial;
			public final int energyRequired_initial;
			public final int cooldown_seconds_initial;
			public final int sickness_seconds_initial;
			public final int maximumDistance_blocks_initial;
			
			// applied values
			private int maximumDistance_blocks;
			private int energyRequired;
			private int warmup_seconds;
			private int sickness_seconds;
			private int cooldown_seconds;
			
			public MovementCosts(final World world, final int x, final int y, final int z,
			                     final int mass, final String jumpType, final int distance,
			                     final int maximumDistance_blocks,
			                     final int energyRequired,
			                     final int warmup_seconds,
			                     final int sickness_seconds,
			                     final int cooldown_seconds) {
				super(world, x, y, z, mass, jumpType, distance);
				
				this.warmup_seconds_initial = warmup_seconds;
				this.warmup_seconds = warmup_seconds;
				this.energyRequired_initial = energyRequired;
				this.energyRequired = energyRequired;
				this.cooldown_seconds_initial = cooldown_seconds;
				this.cooldown_seconds = cooldown_seconds;
				this.sickness_seconds_initial = sickness_seconds;
				this.sickness_seconds = sickness_seconds;
				this.maximumDistance_blocks_initial = maximumDistance_blocks;
				this.maximumDistance_blocks = maximumDistance_blocks;
			}
			
			public int getMaximumDistance_blocks() {
				return maximumDistance_blocks;
			}
			
			public void setMaximumDistance_blocks(final int maximumDistance_blocks) {
				this.maximumDistance_blocks = maximumDistance_blocks;
			}
			
			public int getEnergyRequired() {
				return energyRequired;
			}
			
			public void setEnergyRequired(final int energyRequired) {
				this.energyRequired = energyRequired;
			}
			
			public int getWarmup_seconds() {
				return warmup_seconds;
			}
			
			public void setWarmup_seconds(final int warmup_seconds) {
				this.warmup_seconds = warmup_seconds;
			}
			
			public int getSickness_seconds() {
				return sickness_seconds;
			}
			
			public void setSickness_seconds(final int sickness_seconds) {
				this.sickness_seconds = sickness_seconds;
			}
			
			public int getCooldown_seconds() {
				return cooldown_seconds;
			}
			
			public void setCooldown_seconds(final int cooldown_seconds) {
				this.cooldown_seconds = cooldown_seconds;
			}
		}
		
		@Cancelable
		public static class PreJump extends Ship {
			
			// cancellation message
			private final StringBuilder reason;
			
			public PreJump(final World world, final int x, final int y, final int z,
			               final int mass, final String jumpType, final int distance) {
				super(world, x, y, z, mass, jumpType, distance);
				
				this.reason = new StringBuilder();
			}
			
			public String getReason() {
				return reason.toString();
			}
			
			public void appendReason(final String reasonAdded) {
				if (reason.length() > 0) {
					reason.append("\n");
				}
				reason.append(reasonAdded);
			}
		}
		
		public static class PostJump extends Ship {
			
			public PostJump(final World world, final int x, final int y, final int z, 
			                final int mass, final String jumpType, final int distance) {
				super(world, x, y, z, mass, jumpType, distance);
			}
		}
	}
	
}
