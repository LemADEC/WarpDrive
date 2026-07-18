package cr0s.warpdrive.block;

import cr0s.warpdrive.CommonProxy;
import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBeamFrequency;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.IBlockUpdateDetector;
import cr0s.warpdrive.api.IVideoChannel;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.api.computer.ICoreSignature;
import cr0s.warpdrive.data.CameraRegistryItem;
import cr0s.warpdrive.data.EnumComponentType;
import cr0s.warpdrive.data.EnumForceFieldUpgrade;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.data.GlobalRegionManager;
import cr0s.warpdrive.item.ItemComponent;
import cr0s.warpdrive.item.ItemForceFieldUpgrade;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.state.IProperty;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class TileEntityAbstractBase extends TileEntity implements IBlockUpdateDetector, ITickableTileEntity {
	
	// static properties
	private static final HashMap<IBlockBase, TileEntityType<? extends TileEntity>> REGISTRY = new HashMap<>(120);
	
	// persistent properties
	protected final EnumTier enumTier;
	
	// computed properties
	private boolean isConstructed = false;
	private boolean isFirstTick = true;
	private boolean isDirty = false;
	
	public TileEntityAbstractBase(@Nonnull final IBlockBase blockBase) {
		super(getTileEntityType(blockBase));
		
		enumTier = blockBase.getTier();
	}
	
	public static <T extends TileEntity> void register(@Nonnull final IBlockBase blockBase, @Nonnull final TileEntityType<T> tileEntityType) {
		REGISTRY.put(blockBase, tileEntityType);
	}
	
	@Nullable
	public static TileEntity createNewTileEntity(@Nonnull final IBlockBase blockBase) {
		// Items search list is build before registering tile entities, so we can't create tile entities yet during loading...
		final TileEntityType<?> tileEntityType = REGISTRY.get(blockBase);
		return tileEntityType == null ? null : tileEntityType.create();
	}
	
	public static TileEntityType<?> getTileEntityType(@Nonnull final IBlockBase blockBase) {
		return REGISTRY.get(blockBase);
	}
	
	@Override
	public void onLoad() {
		super.onLoad();
		assert hasWorld() && pos != BlockPos.ZERO;
		if (!isConstructed) {
			onConstructed();
		}
	}
	
	protected void onConstructed() {
		// warning: we can't use Block.CreateNewTileEntity() as world loading calls the TileEntity constructor directly
		// warning: we can't use setWorldAndPos() or validate() as getBlockState() will cause a stack overflow
		// warning: we can't use onLoad() to trigger this method as onLoad() isn't always called, see https://github.com/MinecraftForge/MinecraftForge/issues/5061
		
		if (world == null) {// we're client side, tier is already set
			return;
		}
		
		if (false && WarpDrive.isDev && this instanceof TileEntityAbstractMachine) {
			WarpDrive.logger.info(String.format("%s onConstructed at %d", this, world.getGameTime()));
			if (Commons.throttleMe("onConstructed")) {
				new Exception().printStackTrace(WarpDrive.printStreamInfo);
			}
		}
		
		isConstructed = true;
	}
	
	public void finishConstruction() {
		if (!isConstructed) {
			onConstructed();
			if (Commons.throttleMe("finishConstruction")) {
				new RuntimeException(String.format("%s Recovered from missing call to onConstructed",
				                                   this )).printStackTrace(WarpDrive.printStreamWarn);
			}
		}
	}
	
	protected void onFirstTick() {
		// No operation
		assert isConstructed;
		assert enumTier != null;
	}
	
	@Override
	public void tick() {
		if (isFirstTick) {
			isFirstTick = false;
			onFirstTick();
		}
		
		if (isDirty) {
			markDirty();
		}
	}
	
	protected boolean isFirstTick() {
		return isFirstTick;
	}
	
	// Custom hook to react to a genuine block break/replace. Routed from BlockAbstractContainer.onReplaced().
	public void onBlockBroken(final net.minecraft.block.BlockState blockStateOld, final net.minecraft.world.World world, final net.minecraft.util.math.BlockPos blockPos, final net.minecraft.block.BlockState blockStateNew) {
	}
	
	@Override
	public void onBlockUpdateDetected(@Nonnull final BlockPos blockPosUpdated) {
		assert Commons.isSafeThread();
		if (!isConstructed) {
			onConstructed();
		}
	}
	
	protected <T extends Comparable<T>, V extends T> void updateBlockState(final BlockState blockState_in, final IProperty<T> property, final V value) {
		assert world != null;
		BlockState blockState_old = blockState_in;
		if (blockState_old == null) {
			blockState_old = world.getBlockState(pos);
		}
		BlockState blockState_new = blockState_old;
		if (property != null) {
			if (!blockState_new.getProperties().contains(property)) {
				WarpDrive.logger.error(String.format("Unable to update block state due to missing property in %s: %s calling updateBlockState(%s, %s, %s)",
				                                     blockState_new.getBlock(), this, blockState_in, property, value));
				return;
			}
			if (blockState_new.get(property) == value) {
				return;
			}
			blockState_new = blockState_new.with(property, value);
		}
		if (blockState_old != blockState_new) {
			world.setBlockState(pos, blockState_new, 2);
		}
	}
	
	protected void updateBlockState(final BlockState blockState_in, @Nonnull final BlockState blockState_new) {
		assert world != null;
		BlockState blockState_old = blockState_in;
		if (blockState_old == null) {
			blockState_old = world.getBlockState(pos);
		}
		
		final Block block_old = blockState_old.getBlock();
		final Block block_new = blockState_new.getBlock();
		if (block_new != block_old) {
			WarpDrive.logger.error(String.format("Unable to update block state from %s to %s: %s calling updateBlockState(%s, %s)",
			                                     block_old, block_new, this, blockState_in, blockState_new));
			return;
		}
		
		if (blockState_old != blockState_new) {
			world.setBlockState(pos, blockState_new, 2);
		}
	}
	
	@Override
	public void markDirty() {
		if ( hasWorld()
		  && Commons.isSafeThread() ) {
			super.markDirty();
			isDirty = false;
			assert world != null;
			final BlockState blockState = world.getBlockState(pos);
			world.notifyBlockUpdate(pos, blockState, blockState, 3);
			GlobalRegionManager.onBlockUpdating(null, world, pos, blockState);
		} else {
			isDirty = true;
		}
	}
	
	// area protection
	protected boolean isBlockBreakCanceled(final UUID uuidPlayer, final World world, final BlockPos blockPosEvent) {
		return CommonProxy.isBlockBreakCanceled(uuidPlayer, pos, world, blockPosEvent);
	}
	
	protected boolean isBlockPlaceCanceled(final UUID uuidPlayer, final World world, final BlockPos blockPosEvent, final BlockState blockState) {
		return CommonProxy.isBlockPlaceCanceled(uuidPlayer, pos, world, blockPosEvent, blockState);
	}
	
	// saved properties
	@Override
	public void read(@Nonnull final CompoundNBT tagCompound) {
		super.read(tagCompound);
		if (tagCompound.contains("upgrades")) {
			final CompoundNBT nbtTagCompoundUpgrades = tagCompound.getCompound("upgrades");
			final Set<String> keys = nbtTagCompoundUpgrades.keySet();
			for (final String key : keys) {
				UpgradeSlot upgradeSlot = getUpgradeSlot(key);
				final int quantity = nbtTagCompoundUpgrades.getByte(key);
				if (upgradeSlot == null) {
					WarpDrive.logger.error(String.format("Found an unknown upgrade named %s in %s",
					                                     key, this));
					upgradeSlot = new UpgradeSlot(key, ItemStack.EMPTY, 0);
				}
				upgradeSlots.put(upgradeSlot, quantity);
			}
		}
	}
	
	@Nonnull
	@Override
	public CompoundNBT write(@Nonnull CompoundNBT tagCompound) {
		tagCompound = super.write(tagCompound);
		
		// forge cleanup
		if (tagCompound.getCompound("ForgeCaps").isEmpty()) {
			tagCompound.remove("ForgeCaps");
		}
		
		if (!upgradeSlots.isEmpty()) {
			final CompoundNBT nbtTagCompoundUpgrades = new CompoundNBT();
			for (final Entry<UpgradeSlot, Integer> entry : upgradeSlots.entrySet()) {
				if (entry.getValue() != 0) {
					final String key = entry.getKey().toString();
					nbtTagCompoundUpgrades.putByte(key, (byte) (int) entry.getValue());
				}
			}
			if (!nbtTagCompoundUpgrades.isEmpty()) {
				tagCompound.put("upgrades", nbtTagCompoundUpgrades);
			}
		}
		
		return tagCompound;
	}
	
	public CompoundNBT writeItemDropNBT(final CompoundNBT tagCompound) {
		write(tagCompound);
		tagCompound.remove("id");
		tagCompound.remove("x");
		tagCompound.remove("y");
		tagCompound.remove("z");
		return tagCompound;
	}
	
	@Nonnull
	@Override
	public CompoundNBT getUpdateTag() {
		return write(super.getUpdateTag());
	}
	
	@Nullable
	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		return new SUpdateTileEntityPacket(pos, -1, getUpdateTag());
	}
	
	@Override
	public void onDataPacket(@Nonnull final NetworkManager networkManager, @Nonnull final SUpdateTileEntityPacket packet) {
		final CompoundNBT tagCompound = packet.getNbtCompound();
		read(tagCompound);
	}
	
	// tier
	public int getTierIndex() {
		if (!isConstructed) {
			onConstructed();
			WarpDrive.logger.error(String.format("%s Tile entity was used before being loaded! this is a forge issue.",
			                                     this ));
			if (Commons.throttleMe("TileEntityAbstractBase.getTierIndex")) {
				new RuntimeException().printStackTrace(WarpDrive.printStreamInfo);
			}
		}
		return enumTier.getIndex();
	}
	
	// status
	protected WarpDriveText getUpgradeStatus(final boolean isAnimated) {
		if (!isUpgradeable()) {
			return new WarpDriveText(null, "");
		}
		
		final boolean isShowingItemNames = !isAnimated || (System.currentTimeMillis() % 4000) > 2000;
		
		final WarpDriveText warpDriveText = new WarpDriveText(null, "warpdrive.upgrade.status_line.header");
		for (final Entry<UpgradeSlot, Integer> entry : upgradeSlots.entrySet()) {
			final UpgradeSlot upgradeSlot = entry.getKey();
			final String keyName = isShowingItemNames ? upgradeSlot.itemStack.getTranslationKey() : upgradeSlot.getTranslationKey();
			final Style style = entry.getValue() == 0 ? Commons.getStyleDisabled() : Commons.getStyleCorrect();
			warpDriveText.append(Commons.getStyleDisabled(), "- %1$s/%2$s x %3$s",
			                     new WarpDriveText(Commons.getStyleValue(), "%1$s", entry.getValue()),
			                     entry.getKey().maxCount,
			                     new WarpDriveText(style, keyName) );
		}
		return warpDriveText;
	}
	
	@Nonnull
	protected WarpDriveText getStatusPrefix() {
		if (world != null) {
			final Item item = Item.getItemFromBlock(getBlockState().getBlock());
			if (item != Items.AIR) {
				final ItemStack itemStack = new ItemStack(item, 1);
				return Commons.getChatPrefix(itemStack);
			}
		}
		return new WarpDriveText();
	}
	
	@Nonnull
	protected WarpDriveText getBeamFrequencyStatus(final int beamFrequency) {
		if (beamFrequency == -1) {
			return new WarpDriveText(Commons.getStyleWarning(), "warpdrive.beam_frequency.status_line.undefined");
		} else if (beamFrequency < 0) {
			return new WarpDriveText(Commons.getStyleWarning(), "warpdrive.beam_frequency.status_line.invalid", beamFrequency);
		} else {
			return new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.beam_frequency.status_line.valid", beamFrequency);
		}
	}
	
	@Nonnull
	protected WarpDriveText getVideoChannelStatus(final int videoChannel) {
		if (videoChannel == -1) {
			return new WarpDriveText(Commons.getStyleWarning(), "warpdrive.video_channel.status_line.undefined");
		} else if (videoChannel < 0) {
			return new WarpDriveText(Commons.getStyleWarning(), "warpdrive.video_channel.status_line.invalid",
			                         videoChannel);
		} else {
			final CameraRegistryItem camera = WarpDrive.cameras.getCameraByVideoChannel(world, videoChannel);
			if (camera == null) {
				return new WarpDriveText(Commons.getStyleWarning(), "warpdrive.video_channel.status_line.not_loaded",
				                         videoChannel);
			} else if (camera.isTileEntity(this)) {
				return new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.video_channel.status_line.valid_self",
				                         videoChannel);
			} else {
				return new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.video_channel.status_line.valid_other",
				                         videoChannel,
				                         Commons.format(world, camera.blockPos) );
			}
		}
	}
	
	@Nonnull
	protected WarpDriveText getCoreSignatureStatus(final String nameSignature) {
		// note: we only report 'undefined' status for Remote controllers
		if (nameSignature != null && !nameSignature.isEmpty()) {
			return new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.core_signature.status_line.defined",
			                         nameSignature);
		}
		return new WarpDriveText();
	}
	
	public WarpDriveText getStatusHeader() {
		return new WarpDriveText();
	}
	
	public WarpDriveText getStatus() {
		final WarpDriveText message = getStatusPrefix();
		message.appendSibling( getStatusHeader() );
		
		if (this instanceof IBeamFrequency) {
			// only show in item form or from server side
			if ( world == null
			  || !world.isRemote() ) {
				message.append( getBeamFrequencyStatus(((IBeamFrequency) this).getBeamFrequency()) );
			}
		}
		
		if (this instanceof IVideoChannel) {
			// only show in item form or from client side
			if ( world == null
			  || world.isRemote() ) {
				message.append( getVideoChannelStatus(((IVideoChannel) this).getVideoChannel()) );
			}
		}
		
		if (this instanceof ICoreSignature) {
			// only show in item form or from client side
			if ( world == null
			  || world.isRemote() ) {
				message.append( getCoreSignatureStatus(((ICoreSignature) this).getSignatureName()) );
			}
		}
		
		if (isUpgradeable()) {
			// hide upgrades in the world on client side (server side will do it)
			if ( world == null
			  || !world.isRemote() ) {
				// show updates details in the world or while sneaking in the inventory
				boolean showDetails = hasWorld();
				if (Commons.isClientThread()) {
					final KeyBinding keyBindingSneak = Minecraft.getInstance().gameSettings.keyBindSneak;
					final String keyName = keyBindingSneak.getLocalizedName();
					showDetails = Commons.isKeyPressed(keyBindingSneak);
					if (!showDetails) {
						message.append(null, "warpdrive.upgrade.status_line.upgradeable",
						               new WarpDriveText(Commons.getStyleCommand(), "%1$s", keyName));
					}
				}
				if (showDetails) {
					// show animation only in the inventory (i.e. no world defined)
					message.append(getUpgradeStatus(!hasWorld()));
				}
			}
		}
		
		return message;
	}
	
	public final WarpDriveText getStatus(@Nonnull final ItemStack itemStack, @Nonnull final BlockState blockState) {
		// (this is a temporary object to compute status)
		// get tier from ItemStack
		final Block block = blockState.getBlock();
		if (block instanceof IBlockBase) {
			onConstructed();
		}
		
		// get persistent properties
		final CompoundNBT tagCompound = itemStack.getTag();
		if (tagCompound != null) {
			read(tagCompound);
		}
		
		// compute status
		return getStatus();
	}
	
	public String getStatusHeaderInPureText() {
		return Commons.removeFormatting( getStatusHeader().getString() );
	}
	
	public String getInternalStatus() {
		return String.format("%s\n"
		                   + "NBT %s\n"
		                   + "isConstructed %s isFirstTick %s isDirty %s",
		                     this,
		                     write(new CompoundNBT()),
		                     isConstructed, isFirstTick, isDirty );
	}
	
	// upgrade system
	public static class UpgradeSlot {
		
		// unique name for NBT saving and translation
		public final String name;
		// item stack for upgrading, quantity & NBT are ignored
		public final ItemStack itemStack;
		// max quantity of upgrades that can be installed
		public final int maxCount;
		// cached hashcode
		private final int hashCode;
		
		public UpgradeSlot(@Nonnull final String name, @Nonnull final ItemStack itemStack, final int maxCount) {
			this.name = name;
			this.itemStack = itemStack;
			this.maxCount = maxCount;
			this.hashCode = name.hashCode();
		}
		
		@Nonnull
		String getTranslationKey() {
			return "warpdrive.upgrade.description." + name;
		}
		
		@Override
		public int hashCode() {
			return hashCode;
		}
		
		@Override
		public String toString() {
			return name;
		}
	}
	private final HashMap<UpgradeSlot, Integer> upgradeSlots = new HashMap<>(10);
	public boolean isUpgradeable() {
		return !upgradeSlots.isEmpty();
	}
	public boolean hasUpgrade(@Nonnull final UpgradeSlot upgradeSlot) {
		return getUpgradeCount(upgradeSlot) > 0;
	}
	
	@Nullable
	private UpgradeSlot getUpgradeSlot(@Nonnull final String name) {
		for (final UpgradeSlot upgradeSlot : upgradeSlots.keySet()) {
			if (upgradeSlot.name.equals(name)) {
				return upgradeSlot;
			}
		}
		try {
			final EnumComponentType componentType = EnumComponentType.valueOf(name);
			return getUpgradeSlot(ItemComponent.getItemStackNoCache(componentType, 1));
		} catch (final IllegalArgumentException exception) {
			// ignore for now
		}
		try {
			final EnumForceFieldUpgrade forceFieldUpgrade = EnumForceFieldUpgrade.valueOf(name);
			return getUpgradeSlot(ItemForceFieldUpgrade.getItemStackNoCache(forceFieldUpgrade, 1));
		} catch (final IllegalArgumentException exception) {
			// ignore for now
		}
		return null;
	}
	
	@Nullable
	public UpgradeSlot getUpgradeSlot(@Nonnull final ItemStack itemStack) {
		// fast check
		if (itemStack.isEmpty()) {
			return null;
		}
		// check all slots, ignoring NBT
		for (final UpgradeSlot upgradeSlot : upgradeSlots.keySet()) {
			if (upgradeSlot.itemStack.isItemEqual(itemStack)) {
				return upgradeSlot;
			}
		}
		return null;
	}
	
	@Nullable
	public UpgradeSlot getFirstUpgradeOfType(@Nonnull final Class<?> clazz, @Nullable final UpgradeSlot defaultValue) {
		for (final Entry<UpgradeSlot, Integer> entry : upgradeSlots.entrySet()) {
			if ( entry.getValue() > 0
			  && ( clazz.isInstance(entry.getKey())
			    || clazz.isInstance(entry.getKey().itemStack.getItem()) ) ) {
				return entry.getKey();
			}
		}
		return defaultValue;
	}
	
	public Map<UpgradeSlot, Integer> getUpgradesOfType(final Class<?> clazz) {
		if (clazz == null) {
			return upgradeSlots;
		}
		final Map<UpgradeSlot, Integer> mapResult = new HashMap<>(upgradeSlots.size());
		for (final Entry<UpgradeSlot, Integer> entry : upgradeSlots.entrySet()) {
			if (clazz.isInstance(entry.getKey())) {
				mapResult.put(entry.getKey(), entry.getValue());
			}
		}
		return mapResult;
	}
	
	public int getValidUpgradeCount(@Nonnull final UpgradeSlot upgrade) {
		return Math.min(getUpgradeMaxCount(upgrade), getUpgradeCount(upgrade));
	}
	
	public int getUpgradeCount(@Nonnull final UpgradeSlot upgradeSlot) {
		final Integer value = upgradeSlots.get(upgradeSlot);
		return value == null ? 0 : value;
	}
	
	public int getUpgradeMaxCount(@Nonnull final UpgradeSlot upgradeSlot) {
		return upgradeSlot.maxCount;
	}
	
	protected void registerUpgradeSlot(final UpgradeSlot upgradeSlot) {
		upgradeSlots.put(upgradeSlot, 0);
	}
	
	public boolean canMountUpgrade(@Nonnull final UpgradeSlot upgradeSlot) {
		return upgradeSlots.containsKey(upgradeSlot)
		    && upgradeSlot.maxCount >= getUpgradeCount(upgradeSlot) + 1;
	}
	
	public final boolean mountUpgrade(@Nonnull final UpgradeSlot upgradeSlot) {
		if (canMountUpgrade(upgradeSlot)) {
			final int countNew = getUpgradeCount(upgradeSlot) + 1;
			upgradeSlots.put(upgradeSlot, countNew);
			onUpgradeChanged(upgradeSlot, countNew, true);
			markDirty();
			return true;
		}
		return false;
	}
	
	public boolean canDismountUpgrade(@Nonnull final UpgradeSlot upgradeSlot) {
		return upgradeSlots.containsKey(upgradeSlot)
		    && getUpgradeCount(upgradeSlot) > 0;
	}
	
	public final boolean dismountUpgrade(@Nonnull final UpgradeSlot upgradeSlot ) {
		if (canDismountUpgrade(upgradeSlot)) {
			final int countNew = getUpgradeCount(upgradeSlot) - 1;
			if (countNew >= 0) {
				upgradeSlots.put(upgradeSlot, countNew);
				onUpgradeChanged(upgradeSlot, countNew, false);
				markDirty();
				return true;
			}
		}
		return false;
	}
	
	protected void onUpgradeChanged(@Nonnull final UpgradeSlot upgradeSlot, final int countNew, final boolean isAdded) {
	
	}
	
	// apply debug/creative values
	public void setDebugValues() {
		// no operation
	}
	
	// apply EMP effect
	public void onEMP(final float efficiency) {
		// no operation
	}
	
	@Override
	public String toString() {
		return String.format("%s %s",
		                     getClass().getSimpleName(),
		                     Commons.format(world, pos));
	}
}