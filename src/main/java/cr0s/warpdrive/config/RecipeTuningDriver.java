package cr0s.warpdrive.config;


import cr0s.warpdrive.item.ItemTuningDriver;

import java.util.ArrayList;

import net.minecraft.init.Blocks;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapelessOreRecipe;

// Used to change tuning driver values
public class RecipeTuningDriver implements IRecipe {
	
	private ItemStack itemStackTool;
	private ItemStack itemStackConsumable;
	private int countDyesExpected;
	private ItemStack itemStackResult = new ItemStack(Blocks.fire);
	
	public RecipeTuningDriver(final ItemStack itemStackTool, final ItemStack itemStackConsumable, final int countDyes) {
		this.itemStackTool = itemStackTool.copy();
		this.itemStackConsumable = itemStackConsumable.copy();
		this.countDyesExpected = countDyes;
		
		// add lower priority vanilla Shaped recipe for NEI support
		Object[] recipe = new Object[getRecipeSize()];
		recipe[0] = itemStackTool;
		recipe[1] = itemStackConsumable;
		for (int index = 0; index < countDyes; index++) {
			recipe[2 + index] = "dye";
		}
		GameRegistry.addRecipe(new ShapelessOreRecipe(itemStackTool, recipe));
	}
	
	// Returns an Item that is the result of this recipe
	@Override
	public ItemStack getCraftingResult(InventoryCrafting inventoryCrafting) {
		return itemStackResult.copy();
	}
	
	// Returns the size of the recipe area
	@Override
	public int getRecipeSize() {
		return 1 + (itemStackConsumable != null ? 1 : 0) + countDyesExpected;
	}
	
	@Override
	public ItemStack getRecipeOutput() {
		return itemStackResult;
	}
	
	// check if a recipe matches current crafting inventory
	@Override
	public boolean matches(InventoryCrafting inventoryCrafting, World world) {
		ItemStack itemStackInput = null;
		boolean isConsumableFound = false;
		int dye = 0;
		int countDyesFound = 0;
		for (int indexSlot = 0; indexSlot <= inventoryCrafting.getSizeInventory(); indexSlot++) {
			ItemStack itemStackSlot = inventoryCrafting.getStackInSlot(indexSlot);
			
			//noinspection StatementWithEmptyBody
			if (itemStackSlot == null) {
				// continue
			} else if (OreDictionary.itemMatches(itemStackSlot, itemStackTool, false)) {
				// too many inputs?
				if (itemStackInput != null) {
					return false;
				}
				itemStackInput = itemStackSlot;
				
			} else if (OreDictionary.itemMatches(itemStackSlot, itemStackConsumable, false)) {
				// too many consumables?
				if (isConsumableFound) {
					return false;
				}
				isConsumableFound = true;
				
			} else {
				// find a matching dye from ore dictionary
				boolean matched = false;
				for (int indexDye = 0; indexDye < Recipes.oreDyes.length; indexDye++) {
					ArrayList<ItemStack> itemStackDyes = OreDictionary.getOres(Recipes.oreDyes[indexDye]);
					for (ItemStack itemStackDye : itemStackDyes) {
						if (OreDictionary.itemMatches(itemStackSlot, itemStackDye, true)) {
							// match found, update dye combination
							matched = true;
							countDyesFound++;
							dye = dye * 16 + indexDye;
						}
					}
				}
				if (!matched) {
					return false;
				}
			}
		}
		
		// missing input
		if (itemStackInput == null) {
			return false;
		}
		
		// missing or too many dyes
		if (countDyesFound != countDyesExpected) {
			return false;
		}
		
		// consumable missing or not required
		if ( (itemStackConsumable != null && !isConsumableFound)
		  || (itemStackConsumable == null &&  isConsumableFound) ) {
			return false;
		}
		
		// build result
		itemStackResult = itemStackInput.copy();
		ItemTuningDriver.setValue(itemStackResult, dye);
		
		return true;
	}
}
