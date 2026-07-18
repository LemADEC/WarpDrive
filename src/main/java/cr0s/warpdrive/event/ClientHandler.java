package cr0s.warpdrive.event;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.world.AbstractVoidDimension;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

public class ClientHandler {
	
	@SubscribeEvent
	public void onClientTick(@Nonnull final ClientTickEvent event) {
		if (event.side != LogicalSide.CLIENT || event.phase != Phase.END) {
			return;
		}
		
		WarpDrive.cloaks.onClientTick();

		// Keep the void dimensions' celestial object resolved
		final World world = Minecraft.getInstance().world;
		if ( world != null
		  && world.getDimension() instanceof AbstractVoidDimension ) {
			((AbstractVoidDimension) world.getDimension()).refreshFromLocalPlayer();
		}
	}
}