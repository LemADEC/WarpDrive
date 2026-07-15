package cr0s.warpdrive.config.structures;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.Filler;
import cr0s.warpdrive.config.GenericSet;
import cr0s.warpdrive.config.InvalidXmlException;
import cr0s.warpdrive.config.Loot;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.config.XmlFileManager;
import cr0s.warpdrive.data.JumpBlock;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.IProperty;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.registries.ForgeRegistries;

import org.w3c.dom.Element;

public class Schematic extends AbstractStructure {
	
	protected HashMap<String, Integer> filenames;
	protected Replacement[] replacements;
	protected Insertion[] insertions;
	
	public Schematic(final String group, final String name) {
		super(group, name);
	}
	
	public String getRandomFileName(final Random random) {
		
		// In loadFromXmlElement, it's already checked that there must be at least 1 "schematic" xml node
		// therefore, this should not be possible
		assert(!filenames.isEmpty());
		
		int totalWeight = 0;
		for (final int weight : filenames.values()) {
			totalWeight += weight;
		}
		int result = random.nextInt(totalWeight);
		for (final Map.Entry<String, Integer> entry : filenames.entrySet()) {
			result -= entry.getValue();
			if (result <= 0) {
				return entry.getKey();
			}
		}
		return filenames.keySet().iterator().next();
	}
	
	@Override
	public AbstractStructureInstance instantiate(final Random random) {
		return new SchematicInstance(this, random);
	}
	
	@Override
	public boolean loadFromXmlElement(final Element element) throws InvalidXmlException {
		super.loadFromXmlElement(element);
		
		final List<Element> fileNameList = XmlFileManager.getChildrenElementByTagName(element, "schematic");
		if (fileNameList.isEmpty()) {
			throw new InvalidXmlException("Must have one schematic node with file name!");
		}
		this.filenames = new HashMap<>(fileNameList.size());
		for (final Element entry : fileNameList) {
			final String filename = entry.getAttribute("filename");
			int weight = 1;
			try {
				weight = Integer.parseInt(entry.getAttribute("weight"));
			} catch (final NumberFormatException numberFormatException) {
				throw new InvalidXmlException(String.format("Invalid weight in schematic %s of structure %s:%s",
				                                            filename, group, name));
			}
			this.filenames.put(filename, weight);
		}
		
		// load all replacement elements
		final List<Element> listReplacements = XmlFileManager.getChildrenElementByTagName(element, "replacement");
		replacements = new Replacement[listReplacements.size()];
		int replacementIndexOut = 0;
		for (final Element elementReplacement : listReplacements) {
			final String blockState = elementReplacement.getAttribute("blockState");
			
			replacements[replacementIndexOut] = new Replacement(getFullName(), blockState);
			try {
				replacements[replacementIndexOut].loadFromXmlElement(elementReplacement);
				replacementIndexOut++;
			} catch (final InvalidXmlException exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.error(String.format("Skipping invalid replacement %s",
				                                     blockState));
			}
		}
		
		// load all insertion elements
		final List<Element> listInsertions = XmlFileManager.getChildrenElementByTagName(element, "insertion");
		insertions = new Insertion[listInsertions.size()];
		int insertionIndexOut = 0;
		for (final Element elementInsertion : listInsertions) {
			final String blockState = elementInsertion.getAttribute("blockState");
			
			insertions[insertionIndexOut] = new Insertion(getFullName(), blockState);
			try {
				insertions[insertionIndexOut].loadFromXmlElement(elementInsertion);
				insertionIndexOut++;
			} catch (final InvalidXmlException exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.error(String.format("Skipping invalid insertion %s",
				                                     blockState));
			}
		}
		
		return true;
	}
	
	static class BlockMatcher {
		
		BlockState blockState;
		
		public static BlockMatcher fromXmlElement(final Element element, final GenericSet<?> caller) throws InvalidXmlException{
			final String blockStateString = element.getAttribute("blockState");
			final BlockMatcher blockMatcher;
			
			blockMatcher = BlockMatcher.fromBlockStateString(blockStateString);
			if (blockMatcher == null){
				WarpDrive.logger.warn(String.format("Invalid matching scheme %s found for %s",
				                                    blockStateString,
				                                    caller.getFullName()));
			}
			
			return blockMatcher;
		}
		
		public static BlockMatcher fromBlockStateString(final String blockStateString) {
			// TODO: allow multiple properties (e.g. variant=oak,half=bottom)
			
			final BlockMatcher result = new BlockMatcher();
			
			String blockNameString = "";
			String propertiesString = "*";
			if (blockStateString.contains("@")) {// (with metadata)
				final String[] blockStateParts = blockStateString.split("@");
				blockNameString = blockStateParts[0].trim();
				propertiesString = blockStateParts[1].trim();
			} else {// (without metadata)
				blockNameString = blockStateString;
			}
			final ResourceLocation blockRegistryName = ResourceLocation.tryCreate(blockNameString);
			final Block block = blockRegistryName == null ? null : ForgeRegistries.BLOCKS.getValue(blockRegistryName);
			if (block == null) {
				WarpDrive.logger.warn(String.format("Ignoring invalid block with name %s.", blockNameString));
				return null;
			}
			if (propertiesString.equals("*")) {// (no properties or explicit wildcard)
				result.blockState = block.getDefaultState();
			} else if (propertiesString.contains("=")) {// (in string format (e.g. "color=red"))
				final String[] metaParts = propertiesString.split("=");
				final String propertyKey = metaParts[0].trim();
				final String propertyValue = metaParts[1].trim();
				final IProperty<?> property = block.getStateContainer().getProperty(propertyKey);
				if (property == null) {
					WarpDrive.logger.warn(String.format("Found invalid block property %s for block %s", propertyKey, blockNameString));
					return null;
				}
				final BlockState blockStateWithProperty = withProperty(block.getDefaultState(), property, propertyValue);
				if (blockStateWithProperty == null) {
					WarpDrive.logger.warn(String.format("Unable to find value %s for block property %s of block %s", propertyValue, propertyKey, blockNameString));
					return null;
				}
				result.blockState = blockStateWithProperty;
			} else {// (invalid format)
				WarpDrive.logger.warn(String.format("Missing property value in %s for block %s, please use the property=value syntax.", propertiesString, blockNameString));
				return null;
			}
			return result;
		}
		
		public boolean isMatching(final BlockState blockStateIn) {
			return blockStateIn.equals(blockState);
		}
		
		public boolean isMatching(final JumpBlock jumpBlockIn) {
			return blockState != null && jumpBlockIn != null && blockState.equals(jumpBlockIn.blockState);
		}
		
		private static <T extends Comparable<T>> BlockState withProperty(@Nonnull final BlockState blockState, @Nonnull final IProperty<T> property, @Nonnull final String valueString) {
			for (final T value : property.getAllowedValues()) {
				if (property.getName(value).equalsIgnoreCase(valueString)) {
					return blockState.with(property, value);
				}
			}
			return null;
		}
		
		@Override
		public String toString() {
			return "BlockMatcher{" + (blockState == null ? "null" : blockState.toString()) + "}";
		}
    }
    
    @Override
	public boolean place(@Nonnull final World world, @Nonnull final Random random, @Nonnull final BlockPos blockPos) {
		return instantiate(random).place(world, random, blockPos);
	}
	
	public static class Replacement extends GenericSet<Filler> {
		
		private final String parentFullName;
		protected BlockMatcher matcher;
		
		public Replacement(final String parentFullName, final String name) {
			super(null, name, Filler.DEFAULT, "filler");
			this.parentFullName = parentFullName;
		}
		
		@Override
		public boolean loadFromXmlElement(final Element element) throws InvalidXmlException {
			super.loadFromXmlElement(element);
			
			matcher = BlockMatcher.fromXmlElement(element, this);
			
			if ( WarpDriveConfig.LOGGING_WORLD_GENERATION
			  && matcher != null ) {
				WarpDrive.logger.info(String.format("  + found replacement for block %s", matcher));
			}
			
			// resolve static imports
			for (final String importGroupName : getImportGroupNames()) {
				final GenericSet<Filler> fillerSet = WarpDriveConfig.FillerManager.getGenericSet(importGroupName);
				if (fillerSet == null) {
					WarpDrive.logger.warn(String.format("Skipping missing FillerSet %s in replacement %s of structure %s",
					                                    importGroupName, name, parentFullName));
				} else {
					loadFrom(fillerSet);
				}
			}
			
			// validate dynamic imports
			for (final String importGroup : getImportGroups()) {
				if (!WarpDriveConfig.FillerManager.doesGroupExist(importGroup)) {
					WarpDrive.logger.warn(String.format("An invalid FillerSet group %s is referenced in replacement %s of structure %s",
					                                    importGroup, name, parentFullName));
				}
			}
			
			return true;
		}
		
		public Replacement instantiate(final Random random) {
			final Replacement replacement = new Replacement(parentFullName, name);
			replacement.matcher = this.matcher;
			try {
				replacement.loadFrom(this);
				for (final String importGroup : getImportGroups()) {
					final GenericSet<Filler> fillerSet = WarpDriveConfig.FillerManager.getRandomSetFromGroup(random, importGroup);
					if (fillerSet == null) {
						WarpDrive.logger.warn(String.format("Ignoring invalid group %s in replacement %s of structure %s",
						                                    importGroup, name, parentFullName));
						continue;
					}
					if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
						WarpDrive.logger.info(String.format("Filling %s:%s with %s:%s",
						                                    parentFullName, name, importGroup, fillerSet.getName()));
					}
					replacement.loadFrom(fillerSet);
				}
			} catch (final Exception exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.error(String.format("Failed to instantiate replacement %s from structure %s",
				                                     name, parentFullName));
			}
			if (replacement.isEmpty()) {
				if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
					WarpDrive.logger.info(String.format("Ignoring empty replacement %s in structure %s",
					                                    name, parentFullName));
				}
				return null;
			}
			return replacement;
		}
		
		public boolean isMatching(final BlockState blockStateIn) {
			return matcher != null && matcher.isMatching(blockStateIn);
		}
		
		public boolean isMatching(final JumpBlock jumpBlockIn) {
			return matcher != null && matcher.isMatching(jumpBlockIn);
		}
	}
	
	public static class Insertion extends GenericSet<Loot> {
		
		private final String parentFullName;
		protected BlockMatcher matcher;
		private int minQuantity;
		private int maxQuantity;
		private int maxRetries;
		
		public Insertion(final String parentFullName, final String name) {
			super(null, name, Loot.DEFAULT, "loot");
			this.parentFullName = parentFullName;
		}
		
		@Override
		public boolean loadFromXmlElement(final Element element) throws InvalidXmlException {
			super.loadFromXmlElement(element);
			
			matcher = BlockMatcher.fromXmlElement(element, this);
			
			if ( WarpDriveConfig.LOGGING_WORLD_GENERATION
			  && matcher != null ) {
				WarpDrive.logger.info(String.format("  + found insertion for block %s", matcher));
			}
			
			// get optional minQuantity attribute, defaulting to 0
			minQuantity = 0;
			final String stringMinQuantity = element.getAttribute("minQuantity");
			if (!stringMinQuantity.isEmpty()) {
				minQuantity = Integer.parseInt(stringMinQuantity);
			}
			
			// get optional maxQuantity attribute, defaulting to 7
			maxQuantity = 7;
			final String stringMaxQuantity = element.getAttribute("minQuantity");
			if (!stringMaxQuantity.isEmpty()) {
				maxQuantity = Integer.parseInt(stringMaxQuantity);
			}
			
			// get optional maxTries attribute, defaulting to 3 according to WorldGenStructure#fillInventoryWithLoot
			maxRetries = 3;
			final String stringMaxTries = element.getAttribute("maxRetries");
			if (!stringMaxTries.isEmpty()) {
				maxRetries = Integer.parseInt(stringMaxTries);
			}
			
			// resolve static imports
			for (final String importGroupName : getImportGroupNames()) {
				final GenericSet<Loot> lootSet = WarpDriveConfig.LootManager.getGenericSet(importGroupName);
				if (lootSet == null) {
					WarpDrive.logger.warn(String.format("Skipping missing LootSet %s in insertion %s of structure %s",
					                                    importGroupName, name, parentFullName));
				} else {
					loadFrom(lootSet);
				}
			}
			
			// validate dynamic imports
			for (final String importGroup : getImportGroups()) {
				if (!WarpDriveConfig.LootManager.doesGroupExist(importGroup)) {
					WarpDrive.logger.warn(String.format("An invalid LootSet group %s is referenced in insertion %s of structure %s",
					                                    importGroup, name, parentFullName));
				}
			}
			
			return true;
		}
		
		public Insertion instantiate(final Random random) {
			final Insertion insertion = new Insertion(parentFullName, name);
			insertion.minQuantity = minQuantity;
			insertion.maxQuantity = maxQuantity;
			insertion.maxRetries  = maxRetries;
			insertion.matcher     = matcher;
			try {
				insertion.loadFrom(this);
				for (final String importGroup : getImportGroups()) {
					final GenericSet<Loot> lootSet = WarpDriveConfig.LootManager.getRandomSetFromGroup(random, importGroup);
					if (lootSet == null) {
						WarpDrive.logger.warn(String.format("Ignoring invalid group %s in insertion %s of structure %s",
						                                    importGroup, name, parentFullName));
						continue;
					}
					if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
						WarpDrive.logger.info(String.format("Inserting %s:%s with %s:%s",
						                                    parentFullName, name, importGroup, lootSet.getName()));
					}
					insertion.loadFrom(lootSet);
				}
			} catch (final Exception exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.error(String.format("Failed to instantiate insertion %s from structure %s",
				                                     name, parentFullName));
			}
			if (insertion.isEmpty()) {
				if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
					WarpDrive.logger.info(String.format("Ignoring empty insertion %s in structure %s",
					                                    name, parentFullName));
				}
				return null;
			}
			return insertion;
		}
		
		public int getMinQuantity() {
			return minQuantity;
		}
		
		public int getMaxQuantity() {
			return maxQuantity;
		}
		
		public int getMaxRetries() {
			return maxRetries;
		}
		
		public boolean isMatching(final BlockState blockStateIn) {
			return matcher != null && matcher.isMatching(blockStateIn);
		}
		
		public boolean isMatching(final JumpBlock jumpBlockIn) {
			return matcher != null && matcher.isMatching(jumpBlockIn);
		}
	}
}
