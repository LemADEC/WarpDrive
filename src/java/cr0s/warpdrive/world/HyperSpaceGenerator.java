package cr0s.warpdrive.world;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderGenerate;
import cr0s.warpdrive.WarpDrive;

public class HyperSpaceGenerator extends ChunkProviderGenerate implements IChunkProvider {
	private Random rand;
	private BiomeGenBase[] biomesForGeneration = new BiomeGenBase[1];

	/**
	 * Reference to the World object.
	 */
	private World worldObj;

	public HyperSpaceGenerator(World par1World, long par2) {
		super(par1World, par2, false);
		rand = new Random();
		this.worldObj = par1World;
	}

	@Override
	public Chunk provideChunk(int par1, int par2) {
		this.rand.setSeed(par1 * 341873128712L + par2 * 132897987541L);
		Block[] var3 = new Block[32768];
		this.biomesForGeneration[0] = WarpDrive.spaceBiome;
		// this.caveGenerator.generate(this, this.worldObj, par1, par2, var3);
		this.biomesForGeneration[0] = WarpDrive.spaceBiome;
		Chunk var4 = new Chunk(this.worldObj, var3, par1, par2);
		var4.generateSkylightMap();
		return var4;
	}

	@Override
	public Chunk loadChunk(int var1, int var2) {
		// TODO Auto-generated method stub
		return this.provideChunk(var1, var2);
	}

	@Override
	public void populate(IChunkProvider var1, int var2, int var3) {
		// super.populate(var1, var2, var3);
		// Generate chunk population
		// GameRegistry.generateWorld(var2, var3, worldObj, var1, var1);
	}

	@Override
	public boolean saveChunks(boolean var1, IProgressUpdate var2) {
		return super.saveChunks(var1, var2);
	}

	@Override
	public boolean canSave() {
		// TODO Auto-generated method stub
		return super.canSave();
	}

	@Override
	public String makeString() {
		// TODO Auto-generated method stub
		return super.makeString();
	}

	@Override
	public List getPossibleCreatures(EnumCreatureType var1, int var2, int var3, int var4) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getLoadedChunkCount() {
		// TODO Auto-generated method stub
		return super.getLoadedChunkCount();
	}

	@Override
	public void recreateStructures(int var1, int var2) {
		// TODO Auto-generated method stub
	}
}