package cr0s.warpdrive.world;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.WarpDriveConfig;

public class WorldGenSmallShip extends WorldGenerator {
	private boolean corrupted;
	private int solarType;

	public WorldGenSmallShip(boolean corrupted) {
		this.corrupted = corrupted;
		solarType = WarpDriveConfig.getIC2Item("solarPanel").getItemDamage();
	}

	@Override
	public boolean generate(World world, Random rand, int centerX, int centerY, int centerZ) {
		if (WarpDriveConfig.isAdvSolPanelLoaded)
			solarType = rand.nextInt(2);

		ItemStack cableType = WarpDriveConfig.getIC2Item("copperCableBlock").copy();

		switch (rand.nextInt(4)) {
		case 0:
			cableType = WarpDriveConfig.getIC2Item("glassFiberCableBlock").copy();
			break;

		case 1:
			cableType = WarpDriveConfig.getIC2Item("glassFiberCableBlock").copy();
			break;

		case 2:
			cableType = WarpDriveConfig.getIC2Item("insulatedGoldCableBlock").copy();
			break;

		case 3:
			cableType = WarpDriveConfig.getIC2Item("doubleInsulatedIronCableBlock").copy();
			break;
		}

		int i = centerX - 5, j = centerY - 4, k = centerZ - 6;
		world.setBlock(i + 0, j + 1, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 0, j + 1, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 1, j + 1, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 1, j + 1, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 1, j + 1, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 1, j + 1, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 1, j + 2, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 1, j + 2, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 2, j + 1, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 2, j + 1, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 2, j + 1, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 2, j + 1, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 2, j + 2, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 2, j + 2, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 2, j + 3, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 2, j + 3, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 2, j + 3, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 2, j + 4, k + 6, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 2, j + 4, k + 7, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 2, j + 4, k + 8, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 2, j + 5, k + 6, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 2, j + 5, k + 7, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 2, j + 5, k + 8, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 3, j + 1, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 3, j + 1, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 3, j + 1, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 3, j + 1, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 3, j + 2, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 3, j + 2, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 3, j + 2, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 3, j + 2, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 3, j + 2, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 3, j + 2, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 3, j + 2, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 3, j + 3, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 3, j + 3, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 3, j + 4, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 3, j + 4, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 3, j + 5, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 3, j + 5, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 3, j + 6, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 3, j + 6, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 3, j + 6, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 4, j + 1, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 4, j + 1, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 4, j + 1, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 4, j + 1, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 4, j + 1, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 4, j + 1, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 4, j + 1, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 4, j + 2, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 4, j + 2, k + 5, Blocks.glowstone);
		world.setBlock(i + 4, j + 2, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 4, j + 2, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 4, j + 2, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 4, j + 2, k + 9, Blocks.glowstone);
		world.setBlock(i + 4, j + 2, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 4, j + 3, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 4, j + 3, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 4, j + 4, k + 4, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 4, j + 4, k + 10, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 4, j + 5, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 4, j + 5, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 4, j + 5, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 4, j + 5, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 4, j + 6, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 4, j + 6, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 4, j + 6, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 5, j + 1, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 5, j + 1, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 5, j + 1, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 5, j + 1, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 5, j + 1, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 5, j + 1, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 5, j + 1, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 5, j + 2, k + 3, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 5, j + 2, k + 4, Blocks.glowstone);
		world.setBlock(i + 5, j + 2, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 5, j + 2, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 5, j + 2, k + 7, Blocks.wool, 14, 0);
		world.setBlock(i + 5, j + 2, k + 8, Blocks.wool, 8, 0);
		world.setBlock(i + 5, j + 2, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 5, j + 2, k + 10, Blocks.glowstone);
		world.setBlock(i + 5, j + 2, k + 11, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 5, j + 3, k + 3, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 5, j + 3, k + 11, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 5, j + 4, k + 3, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 5, j + 4, k + 11, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 5, j + 5, k + 3, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 5, j + 5, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 5, j + 5, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 5, j + 5, k + 11, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 5, j + 6, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 5, j + 6, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 5, j + 6, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 5, j + 6, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 5, j + 6, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 5, j + 7, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 5, j + 7, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 5, j + 7, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 1, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 1, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 1, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 1, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 1, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 1, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 1, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 2, k + 3, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 2, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 2, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 2, k + 6, Blocks.wool, 14, 0);
		world.setBlock(i + 6, j + 2, k + 7, Blocks.wool, 8, 0);
		world.setBlock(i + 6, j + 2, k + 8, Blocks.wool, 14, 0);
		world.setBlock(i + 6, j + 2, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 2, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 2, k + 11, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 3, k + 2, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 3, k + 3, Blocks.chest, 3, 0);
		fillChestWithBonuses(world, rand, i + 6, j + 3, k + 3);
		world.setBlock(i + 6, j + 3, k + 11, Blocks.chest, 2, 0);
		fillChestWithBonuses(world, rand, i + 6, j + 3, k + 11);
		world.setBlock(i + 6, j + 3, k + 12, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 4, k + 2, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 4, k + 3, Blocks.chest, 3, 0);
		fillChestWithBonuses(world, rand, i + 6, j + 4, k + 3);
		world.setBlock(i + 6, j + 4, k + 11, Blocks.chest, 2, 0);
		fillChestWithBonuses(world, rand, i + 6, j + 4, k + 11);
		world.setBlock(i + 6, j + 4, k + 12, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 5, k + 2, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 5, k + 12, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 6, k + 3, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 6, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 6, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 6, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 6, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 6, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 6, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 6, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 6, k + 11, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 7, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 7, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 7, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 7, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 6, j + 7, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 7, j + 1, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 7, j + 1, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 7, j + 1, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 7, j + 1, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 7, j + 1, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 7, j + 1, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 7, j + 1, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 7, j + 2, k + 3, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 7, j + 2, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 7, j + 2, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 7, j + 2, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 7, j + 2, k + 7, Blocks.wool, 8, 0);
		world.setBlock(i + 7, j + 2, k + 8, Blocks.wool, 8, 0);
		world.setBlock(i + 7, j + 2, k + 9, Blocks.wool, 14, 0);
		world.setBlock(i + 7, j + 2, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 7, j + 2, k + 11, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 7, j + 3, k + 2, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 7, j + 3, k + 12, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 7, j + 4, k + 2, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 7, j + 4, k + 12, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 7, j + 5, k + 2, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 7, j + 5, k + 12, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 7, j + 6, k + 3, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 7, j + 6, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 7, j + 6, k + 6, Block.getBlockFromItem(cableType.getItem()), cableType.getItemDamage(), 0);
		world.setBlock(i + 7, j + 6, k + 7, Block.getBlockFromItem(WarpDriveConfig.ASP), solarType, 0);
		world.setBlock(i + 7, j + 6, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 7, j + 6, k + 11, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 7, j + 7, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 7, j + 7, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 7, j + 7, k + 7, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 7, j + 7, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 7, j + 7, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 8, j + 1, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 8, j + 1, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 8, j + 1, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 8, j + 1, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 8, j + 1, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 8, j + 1, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 8, j + 1, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 8, j + 2, k + 3, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 8, j + 2, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 8, j + 2, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 8, j + 2, k + 6, Blocks.wool, 14, 0);
		world.setBlock(i + 8, j + 2, k + 7, Blocks.wool, 14, 0);
		world.setBlock(i + 8, j + 2, k + 8, Blocks.wool, 14, 0);
		world.setBlock(i + 8, j + 2, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 8, j + 2, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 8, j + 2, k + 11, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 8, j + 3, k + 2, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 8, j + 3, k + 12, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 8, j + 4, k + 2, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 8, j + 4, k + 12, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 8, j + 5, k + 2, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 8, j + 5, k + 12, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 8, j + 6, k + 3, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 8, j + 6, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 8, j + 6, k + 6, Block.getBlockFromItem(cableType.getItem()), cableType.getItemDamage(), 0);
		world.setBlock(i + 8, j + 6, k + 7, Block.getBlockFromItem(WarpDriveConfig.ASP), solarType, 0);
		world.setBlock(i + 8, j + 6, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 8, j + 6, k + 11, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 8, j + 7, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 8, j + 7, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 8, j + 7, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 8, j + 7, k + 7, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 8, j + 7, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 8, j + 7, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 8, j + 7, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 9, j + 1, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 9, j + 1, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 9, j + 1, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 9, j + 1, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 9, j + 1, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 9, j + 1, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 9, j + 1, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 9, j + 2, k + 3, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 9, j + 2, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 9, j + 2, k + 5, Blocks.wool, 14, 0);
		world.setBlock(i + 9, j + 2, k + 6, Blocks.wool, 8, 0);
		world.setBlock(i + 9, j + 2, k + 7, Blocks.wool, 14, 0);
		world.setBlock(i + 9, j + 2, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 9, j + 2, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 9, j + 2, k + 10, Blocks.wool, 14, 0);
		world.setBlock(i + 9, j + 2, k + 11, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 9, j + 3, k + 2, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 9, j + 3, k + 12, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 9, j + 4, k + 2, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 9, j + 4, k + 12, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 9, j + 5, k + 2, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 9, j + 5, k + 12, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 9, j + 6, k + 3, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 9, j + 6, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 9, j + 6, k + 6, Block.getBlockFromItem(cableType.getItem()), cableType.getItemDamage(), 0);
		world.setBlock(i + 9, j + 6, k + 7, Block.getBlockFromItem(WarpDriveConfig.ASP), solarType, 0);
		// Placing air generator
		world.setBlock(i + 9, j + 5, k + 7, WarpDrive.airgenBlock, 0, 0);
		world.setBlock(i + 9, j + 6, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 9, j + 6, k + 11, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 9, j + 7, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 9, j + 7, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 9, j + 7, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 9, j + 7, k + 7, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 9, j + 7, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 9, j + 7, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 9, j + 7, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 9, j + 8, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 9, j + 8, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 10, j + 1, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 10, j + 1, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 10, j + 1, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 10, j + 1, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 10, j + 1, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 10, j + 1, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 10, j + 1, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 10, j + 2, k + 3, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 10, j + 2, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 10, j + 2, k + 5, Blocks.wool, 8, 0);
		world.setBlock(i + 10, j + 2, k + 6, Blocks.wool, 8, 0);
		world.setBlock(i + 10, j + 2, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 10, j + 2, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 10, j + 2, k + 9, Blocks.wool, 8, 0);
		world.setBlock(i + 10, j + 2, k + 10, Blocks.wool, 14, 0);
		world.setBlock(i + 10, j + 2, k + 11, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 10, j + 3, k + 2, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 10, j + 3, k + 7, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 10, j + 3, k + 12, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 10, j + 4, k + 2, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 10, j + 4, k + 12, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 10, j + 5, k + 2, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 10, j + 5, k + 12, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 10, j + 6, k + 3, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 10, j + 6, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 10, j + 6, k + 6, Block.getBlockFromItem(cableType.getItem()), cableType.getItemDamage(), 0);
		world.setBlock(i + 10, j + 6, k + 7, Block.getBlockFromItem(WarpDriveConfig.ASP), solarType, 0);
		// Placing air generator
		world.setBlock(i + 10, j + 5, k + 7, WarpDrive.airgenBlock, 0, 0);
		world.setBlock(i + 10, j + 6, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 10, j + 6, k + 11, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 10, j + 7, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 10, j + 7, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 10, j + 7, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 10, j + 7, k + 7, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 10, j + 7, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 10, j + 7, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 10, j + 7, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 10, j + 8, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 10, j + 8, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 1, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 1, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 1, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 1, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 1, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 1, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 1, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 2, k + 3, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 2, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 2, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 2, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 2, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 2, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 2, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 2, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 2, k + 11, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 3, k + 2, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 3, k + 6, WorldGenStructure.getGlassBlock(corrupted, rand));

		// Place warp-controller
		if (rand.nextBoolean()) {
			world.setBlock(i + 11, j + 3, k + 7, WarpDrive.protocolBlock);
		}

		world.setBlock(i + 11, j + 3, k + 8, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 11, j + 3, k + 12, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 4, k + 2, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 11, j + 4, k + 6, WorldGenStructure.getGlassBlock(corrupted, rand));

		// Place computer
		if (rand.nextBoolean()) {
			world.setBlock(i + 11, j + 4, k + 7, WarpDriveConfig.CC_Computer, 12, 3);
		}

		world.setBlock(i + 11, j + 4, k + 8, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 11, j + 4, k + 12, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 11, j + 5, k + 2, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 11, j + 5, k + 7, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 11, j + 5, k + 12, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 11, j + 6, k + 3, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 6, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 6, k + 6, Block.getBlockFromItem(cableType.getItem()), cableType.getItemDamage(), 0);
		world.setBlock(i + 11, j + 6, k + 7, Block.getBlockFromItem(WarpDriveConfig.ASP), solarType, 0);
		world.setBlock(i + 11, j + 6, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 6, k + 11, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 7, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 7, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 7, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 7, k + 7, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 11, j + 7, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 7, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 7, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 8, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 8, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 9, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 11, j + 9, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 12, j + 1, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 12, j + 1, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 12, j + 1, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 12, j + 1, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 12, j + 1, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 12, j + 1, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 12, j + 2, k + 3, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 12, j + 2, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 12, j + 2, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 12, j + 2, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 12, j + 2, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 12, j + 2, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 12, j + 2, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 12, j + 2, k + 11, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 12, j + 3, k + 2, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 12, j + 3, k + 6, WorldGenStructure.getGlassBlock(corrupted, rand));

		// Place warp-core
		if (rand.nextBoolean()) {
			world.setBlock(i + 12, j + 3, k + 7, WarpDrive.warpCore);
		}

		world.setBlock(i + 12, j + 3, k + 8, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 12, j + 3, k + 12, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 12, j + 4, k + 2, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 12, j + 4, k + 6, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 12, j + 4, k + 7, Block.getBlockFromItem(cableType.getItem()), cableType.getItemDamage(), 0);
		world.setBlock(i + 12, j + 4, k + 8, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 12, j + 4, k + 12, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 12, j + 5, k + 2, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 12, j + 5, k + 7, Block.getBlockFromItem(cableType.getItem()), cableType.getItemDamage(), 0);
		world.setBlock(i + 12, j + 5, k + 12, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 12, j + 6, k + 3, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 12, j + 6, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 12, j + 6, k + 6, Block.getBlockFromItem(cableType.getItem()), cableType.getItemDamage(), 0);
		world.setBlock(i + 12, j + 6, k + 7, Block.getBlockFromItem(cableType.getItem()), cableType.getItemDamage(), 0);
		world.setBlock(i + 12, j + 6, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 12, j + 6, k + 11, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 12, j + 7, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 12, j + 7, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 12, j + 7, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 12, j + 7, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 12, j + 7, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 1, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 1, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 1, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 1, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 1, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 1, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 1, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 2, k + 3, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 2, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 2, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 2, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 2, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 2, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 2, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 2, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 2, k + 11, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 3, k + 2, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 3, k + 3, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 3, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 3, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 3, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 3, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 3, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 3, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 3, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 3, k + 11, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 3, k + 12, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 4, k + 2, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 4, k + 3, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 13, j + 4, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 4, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 4, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 4, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 4, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 4, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 4, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 4, k + 11, WorldGenStructure.getGlassBlock(corrupted, rand));
		world.setBlock(i + 13, j + 4, k + 12, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 5, k + 2, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 5, k + 3, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 5, k + 4, Blocks.glowstone);
		world.setBlock(i + 13, j + 5, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 5, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 5, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 5, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 5, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 5, k + 10, Blocks.glowstone);
		world.setBlock(i + 13, j + 5, k + 11, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 5, k + 12, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 6, k + 3, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 6, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 6, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 6, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 6, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 6, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 6, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 6, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 6, k + 11, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 7, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 7, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 13, j + 7, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 14, j + 2, k + 3, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 14, j + 2, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 14, j + 2, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 14, j + 2, k + 11, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 14, j + 3, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 14, j + 3, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 14, j + 3, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 14, j + 3, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 14, j + 3, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 14, j + 3, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 14, j + 3, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 14, j + 4, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 14, j + 4, k + 5, Blocks.redstone_block);
		world.setBlock(i + 14, j + 4, k + 6, Blocks.redstone_block);
		world.setBlock(i + 14, j + 4, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 14, j + 4, k + 8, Blocks.redstone_block);
		world.setBlock(i + 14, j + 4, k + 9, Blocks.redstone_block);
		world.setBlock(i + 14, j + 4, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 14, j + 5, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 14, j + 5, k + 5, Blocks.redstone_block);
		world.setBlock(i + 14, j + 5, k + 6, Blocks.redstone_block);
		world.setBlock(i + 14, j + 5, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 14, j + 5, k + 8, Blocks.redstone_block);
		world.setBlock(i + 14, j + 5, k + 9, Blocks.redstone_block);
		world.setBlock(i + 14, j + 5, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 14, j + 6, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 14, j + 6, k + 5, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 14, j + 6, k + 6, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 14, j + 6, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 14, j + 6, k + 8, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 14, j + 6, k + 9, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 14, j + 6, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 15, j + 2, k + 3, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 15, j + 2, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 15, j + 2, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 15, j + 2, k + 11, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 15, j + 3, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 15, j + 3, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 15, j + 4, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 15, j + 5, k + 7, Blocks.redstone_block);
		world.setBlock(i + 15, j + 6, k + 4, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 15, j + 6, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 15, j + 6, k + 10, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 16, j + 4, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 16, j + 5, k + 7, Blocks.redstone_block);
		world.setBlock(i + 16, j + 6, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 17, j + 5, k + 7, WorldGenStructure.getStoneBlock(corrupted, rand));
		world.setBlock(i + 12, j + 2, k + 9, Blocks.trapdoor, 10, 0);
		spawnNPC(world, i + 9, j + 3, k + 5);
		return true;
	}

	public static void spawnNPC(World world, int i, int j, int k) {
		int numMobs = 2 + world.rand.nextInt(10);

		if (world.rand.nextBoolean()) // Villagers
		{
			for (int idx = 0; idx < numMobs; idx++) {
				EntityVillager entityvillager = new EntityVillager(world, 0);
				entityvillager.setLocationAndAngles(i + 0.5D, j, k + 0.5D, 0.0F, 0.0F);
				world.spawnEntityInWorld(entityvillager);
			}
		} else // Zombies
		{
			for (int idx = 0; idx < numMobs; idx++) {
				EntityZombie entityzombie = new EntityZombie(world);
				entityzombie.setLocationAndAngles(i + 0.5D, j, k + 0.5D, 0.0F, 0.0F);
				world.spawnEntityInWorld(entityzombie);
			}
		}
	}

	public void fillChestWithBonuses(World worldObj, Random rand, int x, int y, int z) {
		TileEntity te = worldObj.getTileEntity(x, y, z);

		if (te != null) {
			TileEntityChest chest = (TileEntityChest) te;
			int size = chest.getSizeInventory();
			int numBonuses = rand.nextInt(size) / 2;

			for (int i = 0; i < size; i++) {
				if (rand.nextInt(size) <= numBonuses) {
					numBonuses--;
					chest.setInventorySlotContents(i, getRandomBonus(rand));
				}
			}
		}
	}

	private ItemStack getRandomBonus(Random rand) {
		ItemStack res = null;
		boolean isDone = false;

		while (!isDone) {
			switch (rand.nextInt(14)) {
			case 0:
				res = WarpDriveConfig.getIC2Item("massFabricator").copy();
				res.stackSize = 1; // + rand.nextInt(2);
				isDone = true;
				break;

			case 1:
				res = WarpDriveConfig.getIC2Item("nuke").copy();
				res.stackSize = 1 + rand.nextInt(2);
				isDone = true;
				break;

			case 2: // Quantum armor bonuses
			case 3:
			case 4:
			case 5:
				isDone = true;
				break;// skipped

			case 6:
				res = WarpDriveConfig.getIC2Item("glassFiberCableItem").copy();
				res.stackSize = 2 + rand.nextInt(12);
				isDone = true;
				break;

			case 7:
				res = WarpDriveConfig.getIC2Item("matter").copy();
				res.stackSize = 2 + rand.nextInt(14);
				isDone = true;
				break;

			case 8:
				isDone = true;
				break;// skipped

			// AE Quarz
			case 9:
				if (WarpDriveConfig.isAppliedEnergisticsLoaded) {
					res = WarpDriveConfig.getAEMaterial("matQuartz").copy();
					res.stackSize = 2 + rand.nextInt(22);
					isDone = true;
				}

				break;

			// AE improved processor
			case 10:
				if (WarpDriveConfig.isAppliedEnergisticsLoaded) {
					res = WarpDriveConfig.getAEMaterial("matProcessorAdvanced").copy();
					res.stackSize = 1 + rand.nextInt(3);
					isDone = true;
				}
				break;

			// Rocket launcher platform Tier3
			case 11:
				if (WarpDriveConfig.isICBMLoaded) {
					// TODO: No 1.7 ICBM yet
					// res = new ItemStack(WarpDriveConfig.ICBM_Machine, 1 +
					// rand.nextInt(1), 2).copy();
					isDone = true;
				}
				break;

			// Missles from conventional to hypersonic
			case 12:
				if (WarpDriveConfig.isICBMLoaded) {
					// TODO: No 1.7 ICBM yet
					// res = new ItemStack(WarpDriveConfig.ICBM_Missile, 2 +
					// rand.nextInt(1), rand.nextInt(10)).copy();
					isDone = true;
				}
				break;

			// Advanced solar panels
			case 13:
				if (WarpDriveConfig.isAdvSolPanelLoaded) {
					res = new ItemStack(WarpDriveConfig.ASP, rand.nextInt(3), solarType).copy();
					isDone = true;
				}
				break;
			}
		}

		return res;
	}
}
