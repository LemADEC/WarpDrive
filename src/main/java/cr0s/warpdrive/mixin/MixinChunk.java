package cr0s.warpdrive.mixin;

import cr0s.warpdrive.data.CloakManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunk;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Chunk.class)
public abstract class MixinChunk
		extends net.minecraftforge.common.capabilities.CapabilityProvider<Chunk>
		implements IChunk, net.minecraftforge.common.extensions.IForgeChunk {
	
	protected MixinChunk(Class<Chunk> baseClass) {
		super(baseClass);
		throw new RuntimeException("Invalid injection");
	}
	
	@Shadow
	@Nonnull
	public ChunkPos getPos() {
		throw new RuntimeException("Invalid injection");
	}
	
	@Shadow @Final private World world;
	
	@Inject(at = @At("TAIL"),
	        method = "read", cancellable = false, remap = false, expect = 1)
	private void read(@Nullable final BiomeContainer biomeContainer,
	                  @Nonnull final PacketBuffer packetBuffer,
	                  @Nonnull final CompoundNBT nbt,
	                  final int availableSections,
	                  @Nonnull final CallbackInfo callback) {
		// Pass the Chunk directely since it's not registered in the provider yet...
		CloakManager.Chunk_read((Chunk) (Object) this);
	}
}