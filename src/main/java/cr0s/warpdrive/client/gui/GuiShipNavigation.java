package cr0s.warpdrive.client.gui;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.data.EnumShipNavigationLegType;
import cr0s.warpdrive.network.MessageShipNavigationAction;
import cr0s.warpdrive.network.PacketHandler;

import java.io.IOException;
import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants.NBT;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class GuiShipNavigation extends GuiScreen {

	// Star Trek / EVE inspired palette
	private static final int COLOR_BACKDROP    = 0xF0070B12;
	private static final int COLOR_PANEL       = 0xDD0C111B;
	private static final int COLOR_PANEL_EDGE  = 0xB014202E;
	private static final int COLOR_CYAN        = 0xFF2DD4FF;
	private static final int COLOR_CYAN_DIM    = 0xFF1C6E8C;
	private static final int COLOR_AMBER       = 0xFFF5C542;
	private static final int COLOR_TEXT        = 0xFFD8E7F3;
	private static final int COLOR_TEXT_DIM    = 0xFF6F879F;
	private static final int COLOR_LABEL       = 0xFF8EA7C0;
	private static final int COLOR_WARN        = 0xFFFF8A75;
	private static final int COLOR_OK          = 0xFF8DFFB4;

	private static final int HEADER_HEIGHT = 46;
	private static final int PANEL_TAB_HEIGHT = 30;

	private static final int BUTTON_TAB_BASE = 10;
	private static final int BUTTON_MAP_FIT = 30;
	private static final int BUTTON_MAP_LOCAL = 38;
	private static final int BUTTON_MAP_HYPERSPACE = 39;
	private static final int BUTTON_ENGAGE = 31;
	private static final int BUTTON_STEP = 32;
	private static final int BUTTON_MODE = 33;
	private static final int BUTTON_PAUSE = 34;
	private static final int BUTTON_RESUME = 35;
	private static final int BUTTON_CANCEL = 36;
	private static final int BUTTON_REFRESH = 37;
	private static final int BUTTON_SAVE_SHIP = 40;
	private static final int BUTTON_ENABLE = 41;
	private static final int BUTTON_IDLE = 42;
	private static final int BUTTON_OFFLINE = 43;
	private static final int BUTTON_MAINTENANCE = 44;
	private static final int BUTTON_PREVIEW_MOVE = 60;
	private static final int BUTTON_EXECUTE_MOVE = 61;
	private static final int BUTTON_MOVE_FORWARD = 62;
	private static final int BUTTON_MOVE_BACK = 63;
	private static final int BUTTON_MOVE_UP = 64;
	private static final int BUTTON_MOVE_DOWN = 65;
	private static final int BUTTON_MOVE_RIGHT = 66;
	private static final int BUTTON_MOVE_LEFT = 67;
	private static final int BUTTON_ROTATE_LEFT = 68;
	private static final int BUTTON_ROTATE_RIGHT = 69;
	private static final int BUTTON_PRESET_TAKEOFF = 70;
	private static final int BUTTON_PRESET_LANDING = 71;
	private static final int BUTTON_HYPERDRIVE = 80;

	private static int cachedMapVersion = Integer.MIN_VALUE;
	private static final ArrayList<MapObject> cachedMapObjects = new ArrayList<>();

	private enum Tab {
		MAP("warpdrive.navigation.gui.tab.map"),
		DESTINATIONS("warpdrive.navigation.gui.tab.destinations"),
		SHIP("warpdrive.navigation.gui.tab.ship"),
		DIMENSIONS("warpdrive.navigation.gui.tab.dimensions"),
		MOVE("warpdrive.navigation.gui.tab.move"),
		DRIVE("warpdrive.navigation.gui.tab.drive");

		private final String titleKey;

		Tab(final String titleKey) {
			this.titleKey = titleKey;
		}

		private String title() {
			return I18n.format(titleKey);
		}
	}

	private NBTTagCompound snapshot;
	private final ArrayList<MapObject> mapObjects = new ArrayList<>();
	private final ArrayList<RouteLeg> routeLegs = new ArrayList<>();
	private final ArrayList<DestinationEntry> destinations = new ArrayList<>();
	private final ArrayList<DestinationRow> destinationRows = new ArrayList<>();
	private final ArrayList<GuiTextField> textFields = new ArrayList<>();
	private Tab selectedTab = Tab.MAP;
	private String selectedId = "";
	private String currentId = "";
	private String targetId = "";
	private String routeStatusKey = "";
	private String routeWarningKey = "";
	private int routeWarningStep;
	private int routeWarningRemaining;
	private String routeBlockerKey = "";
	private int routeEffectiveDistance;
	private int routeMaximumDistance;
	private String notice = "";
	private boolean allowed;
	private boolean canEngage;
	private boolean isStaticMapReady;
	private int dimensionId;
	private int mapVersion;
	private BlockPos blockPosCore = BlockPos.ORIGIN;
	private BlockPos blockPosAccess = BlockPos.ORIGIN;
	private int shipMapX;
	private int shipMapZ;
	private long openedAtMs;

	// live drive status (with client-side interpolation between server snapshots)
	private String driveMovementType = "";
	private int warmupTicks;
	private int warmupTotal = 1;
	private int cooldownTicks;
	private int cooldownTotal = 1;
	private boolean jumpInProgress;
	private boolean cooling;
	private String autopilotMode = "off";
	private String autopilotStatus = "idle";
	private int autopilotLegs;
	private int clientTick;
	private int snapshotClientTick;
	private int lastRefreshTick = -100;

	private int mapX;
	private int mapY;
	private int mapWidth;
	private int mapHeight;
	private int panelX;
	private int panelY;
	private int panelWidth;
	private int panelHeight;
	private double minX;
	private double maxX;
	private double minZ;
	private double maxZ;
	private double viewCenterX;
	private double viewCenterZ;
	private double mapZoom = 1.0D;
	private boolean isMapViewInitialized;
	private boolean isMapHyperspaceView;
	private String mapSpaceId = "";
	private boolean isDraggingMap;
	private int dragStartX;
	private int dragStartY;
	private int dragLastX;
	private int dragLastY;
	private int destinationsScroll;

	private String shipNameInput = "";
	private String energyUnitsInput = "";
	private int dimFront;
	private int dimBack;
	private int dimRight;
	private int dimLeft;
	private int dimUp;
	private int dimDown;
	private boolean isDimensionDraftDirty;
	private NBTTagCompound pendingDimensionPayload;
	private int lastDimensionSendTick = -100;
	private String moveFrontInput = "0";
	private String moveUpInput = "0";
	private String moveRightInput = "0";
	private int rotationStepsInput;

	private GuiTextField fieldShipName;
	private GuiTextField fieldEnergyUnits;
	private GuiTextField fieldDimFront;
	private GuiTextField fieldDimBack;
	private GuiTextField fieldDimRight;
	private GuiTextField fieldDimLeft;
	private GuiTextField fieldDimUp;
	private GuiTextField fieldDimDown;
	private GuiTextField fieldMoveFront;
	private GuiTextField fieldMoveUp;
	private GuiTextField fieldMoveRight;

	public GuiShipNavigation(final NBTTagCompound snapshot) {
		update(snapshot);
		openedAtMs = System.currentTimeMillis();
	}

	public static void open(final NBTTagCompound snapshot) {
		final Minecraft minecraft = Minecraft.getMinecraft();
		if (minecraft.currentScreen instanceof GuiShipNavigation) {
			((GuiShipNavigation) minecraft.currentScreen).update(snapshot);
		} else {
			minecraft.displayGuiScreen(new GuiShipNavigation(snapshot));
		}
	}

	public static void updateStaticMap(final NBTTagCompound tagCompound) {
		if (tagCompound == null) {
			return;
		}
		cachedMapVersion = tagCompound.getInteger("mapVersion");
		cachedMapObjects.clear();
		final NBTTagList tagListObjects = tagCompound.getTagList("celestialObjects", NBT.TAG_COMPOUND);
		for (int index = 0; index < tagListObjects.tagCount(); index++) {
			cachedMapObjects.add(new MapObject(tagListObjects.getCompoundTagAt(index)));
		}
		final Minecraft minecraft = Minecraft.getMinecraft();
		if (minecraft.currentScreen instanceof GuiShipNavigation) {
			((GuiShipNavigation) minecraft.currentScreen).updateStaticMapFromCache();
		}
	}

	@SuppressWarnings("PMD.NPathComplexity")
	private void update(final NBTTagCompound snapshot) {
		this.snapshot = snapshot == null ? new NBTTagCompound() : snapshot;
		dimensionId = this.snapshot.getInteger("dimensionId");
		mapVersion = this.snapshot.getInteger("mapVersion");
		blockPosCore = new BlockPos(this.snapshot.getInteger("coreX"),
		                            this.snapshot.getInteger("coreY"),
		                            this.snapshot.getInteger("coreZ"));
		shipMapX = blockPosCore.getX();
		shipMapZ = blockPosCore.getZ();
		blockPosAccess = new BlockPos(this.snapshot.getInteger("accessX"),
		                              this.snapshot.getInteger("accessY"),
		                              this.snapshot.getInteger("accessZ"));
		currentId = this.snapshot.getString("currentCelestialId");
		final String previousTargetId = targetId;
		targetId = this.snapshot.getString("navigationTargetId");
		notice = this.snapshot.getString("notice");
		allowed = this.snapshot.getBoolean("allowed");
		// clear a stale selection ring once the server confirms a cancel
		if (targetId.isEmpty() && !previousTargetId.isEmpty()) {
			selectedId = "";
		} else if (!targetId.isEmpty()) {
			selectedId = targetId;
		}

		shipNameInput = this.snapshot.getString("shipName");
		energyUnitsInput = this.snapshot.getString("energyUnits");
		final NBTTagCompound dimensions = this.snapshot.getCompoundTag("dimensions");
		if ( pendingDimensionPayload != null
		  && isDimensionSnapshotMatchingPayload(dimensions, pendingDimensionPayload) ) {
			pendingDimensionPayload = null;
			isDimensionDraftDirty = false;
		}
		if (!isDimensionDraftDirty || selectedTab != Tab.DIMENSIONS) {
			setDimensionDraft(dimensions);
		}
		final NBTTagCompound movement = this.snapshot.getCompoundTag("movement");
		moveFrontInput = Integer.toString(movement.getInteger("front"));
		moveUpInput = Integer.toString(movement.getInteger("up"));
		moveRightInput = Integer.toString(movement.getInteger("right"));
		rotationStepsInput = movement.getInteger("rotationSteps");

		final NBTTagCompound driveStatus = this.snapshot.getCompoundTag("driveStatus");
		driveMovementType = driveStatus.getString("movementType");
		warmupTicks = driveStatus.getInteger("warmupTicks");
		warmupTotal = Math.max(1, driveStatus.getInteger("warmupTotal"));
		cooldownTicks = driveStatus.getInteger("cooldownTicks");
		cooldownTotal = Math.max(1, driveStatus.getInteger("cooldownTotal"));
		jumpInProgress = driveStatus.getBoolean("jumpInProgress");
		cooling = driveStatus.getBoolean("cooling");
		autopilotMode = driveStatus.getString("autopilotMode");
		autopilotStatus = driveStatus.getString("autopilotStatus");
		autopilotLegs = driveStatus.getInteger("autopilotLegs");
		snapshotClientTick = clientTick;

		updateStaticMapFromCache();
		updateMapViewDefaults();

		routeLegs.clear();
		final NBTTagCompound route = this.snapshot.getCompoundTag("route");
		routeStatusKey = route.getString("statusKey");
		routeWarningKey = route.getString("warningKey");
		routeWarningStep = route.getInteger("warningStep");
		routeWarningRemaining = route.getInteger("warningRemaining");
		routeBlockerKey = route.getString("blockerKey");
		final NBTTagCompound routeValidation = route.getCompoundTag("validation");
		routeEffectiveDistance = routeValidation.getInteger("effectiveDistance");
		routeMaximumDistance = routeValidation.getInteger("maximumDistance");
		canEngage = isStaticMapReady
		         && route.getBoolean("canEngage")
		         && allowed
		         && this.snapshot.getBoolean("enabled")
		         && !this.snapshot.getBoolean("offline")
		         && !this.snapshot.getBoolean("maintenance")
		         && !this.snapshot.getBoolean("busy");
		final NBTTagList tagListLegs = route.getTagList("legs", NBT.TAG_COMPOUND);
		for (int index = 0; index < tagListLegs.tagCount(); index++) {
			routeLegs.add(new RouteLeg(tagListLegs.getCompoundTagAt(index)));
		}

		destinations.clear();
		final NBTTagList tagListDestinations = this.snapshot.getTagList("destinations", NBT.TAG_COMPOUND);
		for (int index = 0; index < tagListDestinations.tagCount(); index++) {
			destinations.add(new DestinationEntry(tagListDestinations.getCompoundTagAt(index)));
		}
		rebuildDestinationRows();

		computeBounds();
		if (!isMapViewInitialized) {
			resetMapView();
		}
		syncTextFields();
		updateButtons();
	}

	private void updateStaticMapFromCache() {
		mapObjects.clear();
		if (cachedMapVersion == mapVersion && !cachedMapObjects.isEmpty()) {
			mapObjects.addAll(cachedMapObjects);
			isStaticMapReady = true;
		} else if (snapshot != null && snapshot.hasKey("celestialObjects")) {
			final NBTTagList tagListObjects = snapshot.getTagList("celestialObjects", NBT.TAG_COMPOUND);
			for (int index = 0; index < tagListObjects.tagCount(); index++) {
				mapObjects.add(new MapObject(tagListObjects.getCompoundTagAt(index)));
			}
			isStaticMapReady = !mapObjects.isEmpty();
		} else {
			isStaticMapReady = false;
		}
		layoutMapObjects();
	}

	private void updateMapViewDefaults() {
		if (snapshot == null) {
			return;
		}
		if (snapshot.getBoolean("inHyperspace")) {
			if (!isMapHyperspaceView) {
				isMapViewInitialized = false;
			}
			isMapHyperspaceView = true;
			mapSpaceId = "";
			return;
		}
		final MapObject currentSpaceRegion = findContainingSpaceRegion(findObject(currentId));
		final String currentSpaceId = currentSpaceRegion == null ? "" : currentSpaceRegion.id;
		if (!currentSpaceId.equals(mapSpaceId)) {
			mapSpaceId = currentSpaceId;
			isMapHyperspaceView = false;
			isMapViewInitialized = false;
		}
	}

	private void rebuildDestinationRows() {
		destinationRows.clear();
		final ArrayList<String> addedIds = new ArrayList<>();
		for (final DestinationEntry entry : destinations) {
			if (entry.space) {
				appendDestinationRow(entry, 0, addedIds);
				appendDestinationChildren(entry, 1, addedIds);
			}
		}
		for (final DestinationEntry entry : destinations) {
			if (entry.hyperspace) {
				appendDestinationRow(entry, 0, addedIds);
				appendDestinationChildren(entry, 1, addedIds);
			}
		}
		for (final DestinationEntry entry : destinations) {
			if (!addedIds.contains(entry.id)) {
				appendDestinationRow(entry, 0, addedIds);
				appendDestinationChildren(entry, 1, addedIds);
			}
		}
	}

	private void appendDestinationChildren(final DestinationEntry parent, final int depth, final ArrayList<String> addedIds) {
		for (final DestinationEntry child : destinations) {
			if (parent.id.equals(child.parentId)) {
				appendDestinationRow(child, depth, addedIds);
				appendDestinationChildren(child, Math.min(4, depth + 1), addedIds);
			}
		}
	}

	private void appendDestinationRow(final DestinationEntry entry, final int depth, final ArrayList<String> addedIds) {
		if (addedIds.contains(entry.id)) {
			return;
		}
		destinationRows.add(new DestinationRow(entry, depth));
		addedIds.add(entry.id);
	}

	private void layoutMapObjects() {
		for (final MapObject mapObject : mapObjects) {
			resetMapObjectLayout(mapObject);
		}
		for (final MapObject parent : mapObjects) {
			layoutMapChildren(parent);
		}
		for (final MapObject mapObject : mapObjects) {
			layoutMapStack(mapObject);
		}
	}

	private static void resetMapObjectLayout(final MapObject mapObject) {
		mapObject.displayMapX = mapObject.mapX;
		mapObject.displayMapZ = mapObject.mapZ;
		mapObject.stackIndex = 0;
		mapObject.stackCount = 1;
	}

	private void layoutMapChildren(final MapObject parent) {
		final int childCount = countMapChildren(parent);
		if (childCount <= 0) {
			return;
		}
		int childIndex = 0;
		for (final MapObject child : mapObjects) {
			if (parent.id.equals(child.parentId)) {
				layoutMapChild(parent, child, childIndex, childCount);
				childIndex++;
			}
		}
	}

	private int countMapChildren(final MapObject parent) {
		int childCount = 0;
		for (final MapObject child : mapObjects) {
			if (parent.id.equals(child.parentId)) {
				childCount++;
			}
		}
		return childCount;
	}

	private static void layoutMapChild(final MapObject parent, final MapObject child, final int childIndex, final int childCount) {
		final double dx = child.mapX - parent.mapX;
		final double dz = child.mapZ - parent.mapZ;
		final double distance = Math.sqrt(dx * dx + dz * dz);
		final double minimumOrbit = Math.max(9000.0D, Math.min(80000.0D, Math.max(parent.borderRadiusX, parent.borderRadiusZ) * 0.18D));
		if (distance < minimumOrbit * 0.55D) {
			final double angle = Math.PI * 2.0D * childIndex / Math.max(1, childCount) - Math.PI / 2.0D;
			final double orbit = minimumOrbit + childIndex * Math.max(2500.0D, minimumOrbit * 0.10D);
			child.displayMapX = parent.mapX + Math.cos(angle) * orbit;
			child.displayMapZ = parent.mapZ + Math.sin(angle) * orbit;
		}
	}

	private void layoutMapStack(final MapObject mapObject) {
		final int stackCount = countStackedMapObjects(mapObject);
		if (stackCount <= 1) {
			return;
		}
		int stackIndex = 0;
		for (final MapObject stackedObject : mapObjects) {
			if (isSameMapStack(mapObject, stackedObject)) {
				stackedObject.stackIndex = stackIndex++;
				stackedObject.stackCount = stackCount;
			}
		}
	}

	private int countStackedMapObjects(final MapObject mapObject) {
		int stackCount = 0;
		for (final MapObject stackedObject : mapObjects) {
			if (isSameMapStack(mapObject, stackedObject)) {
				stackCount++;
			}
		}
		return stackCount;
	}

	@SuppressWarnings("PMD.NPathComplexity")
	private void computeBounds() {
		minX = -1000.0D;
		maxX = 1000.0D;
		minZ = -1000.0D;
		maxZ = 1000.0D;
		boolean isFirst = true;
		for (final MapObject mapObject : mapObjects) {
			if (!isMapObjectDrawable(mapObject)) {
				continue;
			}
			final double radius = mapObject.hyperspace ? 25000.0D
			                    : mapObject.space ? 35000.0D
			                    : Math.max(6000.0D, Math.min(30000.0D, Math.max(mapObject.borderRadiusX, mapObject.borderRadiusZ)));
			if (isFirst) {
				minX = mapObject.displayMapX - radius;
				maxX = mapObject.displayMapX + radius;
				minZ = mapObject.displayMapZ - radius;
				maxZ = mapObject.displayMapZ + radius;
				isFirst = false;
			} else {
				minX = Math.min(minX, mapObject.displayMapX - radius);
				maxX = Math.max(maxX, mapObject.displayMapX + radius);
				minZ = Math.min(minZ, mapObject.displayMapZ - radius);
				maxZ = Math.max(maxZ, mapObject.displayMapZ + radius);
			}
		}
		if (snapshot != null && snapshot.getBoolean("inHyperspace")) {
			if (isFirst) {
				minX = shipMapX - 1000.0D;
				maxX = shipMapX + 1000.0D;
				minZ = shipMapZ - 1000.0D;
				maxZ = shipMapZ + 1000.0D;
				isFirst = false;
			} else {
				minX = Math.min(minX, shipMapX - 1000.0D);
				maxX = Math.max(maxX, shipMapX + 1000.0D);
				minZ = Math.min(minZ, shipMapZ - 1000.0D);
				maxZ = Math.max(maxZ, shipMapZ + 1000.0D);
			}
		}
		if (maxX - minX < 1.0D) {
			maxX = minX + 1.0D;
		}
		if (maxZ - minZ < 1.0D) {
			maxZ = minZ + 1.0D;
		}
	}

	private void resetMapView() {
		viewCenterX = (minX + maxX) * 0.5D;
		viewCenterZ = (minZ + maxZ) * 0.5D;
		mapZoom = 1.0D;
		isMapViewInitialized = true;
	}

	@Override
	public void initGui() {
		final int top = HEADER_HEIGHT;
		if (width < 680) {
			mapX = 10;
			mapY = top;
			mapWidth = Math.max(220, width - 20);
			mapHeight = Math.max(120, (height - top - 78) / 2);
			panelX = 10;
			panelY = mapY + mapHeight + 8;
			panelWidth = mapWidth;
			panelHeight = Math.max(140, height - panelY - 10);
		} else {
			mapX = 14;
			mapY = top;
			mapWidth = Math.max(300, width - 360);
			mapHeight = Math.max(220, height - top - 14);
			panelX = mapX + mapWidth + 10;
			panelY = mapY;
			panelWidth = Math.max(260, width - panelX - 14);
			panelHeight = mapHeight;
		}

		buttonList.clear();
		textFields.clear();
		addTabButtons();
		buttonList.add(styled(new GuiButton(BUTTON_MAP_FIT, mapX + mapWidth - 50, mapY + 6, 44, 16, I18n.format("warpdrive.navigation.gui.fit"))));
		addMapViewButtons();
		switch (selectedTab) {
		case MAP:
			addMapButtons();
			break;
		case DESTINATIONS:
			break;
		case SHIP:
			addShipButtonsAndFields();
			break;
		case DIMENSIONS:
			addDimensionButtonsAndFields();
			break;
		case MOVE:
			addMoveButtonsAndFields();
			break;
		case DRIVE:
			addDriveButtonsAndFields();
			break;
		default:
			break;
		}
		syncTextFields();
		updateButtons();
	}

	private void addTabButtons() {
		final Tab[] tabs = Tab.values();
		final int gap = 3;
		final int buttonWidth = Math.max(24, (panelWidth - 12 - gap * (tabs.length - 1)) / tabs.length);
		int x = panelX + 6;
		for (final Tab tab : tabs) {
			final int id = BUTTON_TAB_BASE + tab.ordinal();
			buttonList.add(styled(new GuiButton(id, x, panelY + 6, buttonWidth, 16, trimToWidth(tab.title(), buttonWidth - 6))));
			x += buttonWidth + gap;
		}
	}

	private void addMapButtons() {
		final int y = panelY + panelHeight - 50;
		buttonList.add(styled(new GuiButton(BUTTON_ENGAGE, panelX + 8, y, 70, 18, I18n.format("warpdrive.navigation.gui.engage"))));
		buttonList.add(styled(new GuiButton(BUTTON_STEP, panelX + 82, y, 48, 18, I18n.format("warpdrive.navigation.gui.step"))));
		buttonList.add(styled(new GuiButton(BUTTON_MODE, panelX + 134, y, panelWidth - 142, 18, autopilotModeLabel())));
		final int y2 = y + 22;
		buttonList.add(styled(new GuiButton(BUTTON_PAUSE, panelX + 8, y2, 60, 18, I18n.format("warpdrive.navigation.gui.pause"))));
		buttonList.add(styled(new GuiButton(BUTTON_RESUME, panelX + 72, y2, 60, 18, I18n.format("warpdrive.navigation.gui.resume"))));
		buttonList.add(styled(new GuiButton(BUTTON_CANCEL, panelX + 136, y2, 60, 18, I18n.format("warpdrive.navigation.gui.cancel"))));
		buttonList.add(styled(new GuiButton(BUTTON_REFRESH, panelX + panelWidth - 60, y2, 52, 18, I18n.format("warpdrive.navigation.gui.refresh"))));
	}

	private void addMapViewButtons() {
		if (!shouldShowMapViewButtons()) {
			return;
		}
		final int y = mapY + 24;
		final int hyperspaceWidth = 70;
		final int localWidth = 44;
		final int hyperspaceX = mapX + mapWidth - hyperspaceWidth - 6;
		final int localX = hyperspaceX - localWidth - 4;
		buttonList.add(styled(new GuiButton(BUTTON_MAP_LOCAL, localX, y, localWidth, 16, I18n.format("warpdrive.navigation.gui.map.local"))));
		buttonList.add(styled(new GuiButton(BUTTON_MAP_HYPERSPACE, hyperspaceX, y, hyperspaceWidth, 16, I18n.format("warpdrive.navigation.gui.map.hyperspace"))));
	}

	private void addShipButtonsAndFields() {
		int y = panelContentY() + 4;
		fieldShipName = addField(panelX + 96, y, panelWidth - 112, shipNameInput);
		y += 26;
		fieldEnergyUnits = addField(panelX + 96, y, 84, energyUnitsInput);
		y += 28;
		buttonList.add(styled(new GuiButton(BUTTON_SAVE_SHIP, panelX + 8, y, 84, 18, I18n.format("warpdrive.navigation.gui.save"))));
		buttonList.add(styled(new GuiButton(BUTTON_ENABLE, panelX + 98, y, 80, 18,
		                                    snapshot.getBoolean("enabled") ? I18n.format("warpdrive.navigation.gui.disable") : I18n.format("warpdrive.navigation.gui.enable"))));
		y += 24;
		buttonList.add(styled(new GuiButton(BUTTON_IDLE, panelX + 8, y, 60, 18, I18n.format("warpdrive.navigation.gui.idle"))));
		buttonList.add(styled(new GuiButton(BUTTON_OFFLINE, panelX + 74, y, 72, 18, I18n.format("warpdrive.navigation.gui.offline"))));
		buttonList.add(styled(new GuiButton(BUTTON_MAINTENANCE, panelX + 152, y, 100, 18, I18n.format("warpdrive.navigation.gui.maintenance"))));
	}

	private void addDimensionButtonsAndFields() {
		int y = panelContentY() + 6;
		fieldDimFront = addField(panelX + 74, y, 54, Integer.toString(dimFront));
		fieldDimBack = addField(panelX + 202, y, 54, Integer.toString(dimBack));
		y += 24;
		fieldDimRight = addField(panelX + 74, y, 54, Integer.toString(dimRight));
		fieldDimLeft = addField(panelX + 202, y, 54, Integer.toString(dimLeft));
		y += 24;
		fieldDimUp = addField(panelX + 74, y, 54, Integer.toString(dimUp));
		fieldDimDown = addField(panelX + 202, y, 54, Integer.toString(dimDown));
	}

	private void addMoveButtonsAndFields() {
		int y = panelContentY() + 6;
		fieldMoveFront = addField(panelX + 76, y, 66, moveFrontInput);
		fieldMoveUp = addField(panelX + 190, y, 66, moveUpInput);
		y += 24;
		fieldMoveRight = addField(panelX + 76, y, 66, moveRightInput);
		y += 26;
		final int centerX = panelX + 118;
		buttonList.add(styled(new GuiButton(BUTTON_MOVE_FORWARD, centerX - 25, y, 50, 18, "+F")));
		y += 20;
		buttonList.add(styled(new GuiButton(BUTTON_MOVE_LEFT, centerX - 82, y, 50, 18, "-R")));
		buttonList.add(styled(new GuiButton(BUTTON_MOVE_UP, centerX - 25, y, 50, 18, "+U")));
		buttonList.add(styled(new GuiButton(BUTTON_MOVE_RIGHT, centerX + 32, y, 50, 18, "+R")));
		y += 20;
		buttonList.add(styled(new GuiButton(BUTTON_MOVE_BACK, centerX - 25, y, 50, 18, "-F")));
		buttonList.add(styled(new GuiButton(BUTTON_MOVE_DOWN, centerX + 32, y, 50, 18, "-U")));
		y += 26;
		buttonList.add(styled(new GuiButton(BUTTON_ROTATE_LEFT, panelX + 8, y, 54, 18, I18n.format("warpdrive.navigation.gui.rotate_left"))));
		buttonList.add(styled(new GuiButton(BUTTON_ROTATE_RIGHT, panelX + 66, y, 54, 18, I18n.format("warpdrive.navigation.gui.rotate_right"))));
		buttonList.add(styled(new GuiButton(BUTTON_PREVIEW_MOVE, panelX + 124, y, 66, 18, I18n.format("warpdrive.navigation.gui.preview"))));
		buttonList.add(styled(new GuiButton(BUTTON_EXECUTE_MOVE, panelX + 194, y, 66, 18, I18n.format("warpdrive.navigation.gui.execute"))));
		y += 24;
		buttonList.add(styled(new GuiButton(BUTTON_PRESET_TAKEOFF, panelX + 8, y, 76, 18, I18n.format("warpdrive.navigation.gui.takeoff"))));
		buttonList.add(styled(new GuiButton(BUTTON_PRESET_LANDING, panelX + 90, y, 76, 18, I18n.format("warpdrive.navigation.gui.landing"))));
	}

	private void addDriveButtonsAndFields() {
		final int y = panelContentY() + 8;
		buttonList.add(styled(new GuiButton(BUTTON_HYPERDRIVE, panelX + 8, y, 130, 18,
		                                    snapshot.getBoolean("inHyperspace") ? I18n.format("warpdrive.navigation.gui.exit_hyper") : I18n.format("warpdrive.navigation.gui.enter_hyper"))));
	}

	private GuiButton styled(final GuiButton button) {
		return button;
	}

	private GuiTextField addField(final int x, final int y, final int width, final String text) {
		final GuiTextField field = new GuiTextField(textFields.size(), fontRenderer, x, y, width, 16);
		field.setMaxStringLength(96);
		field.setText(text == null ? "" : text);
		textFields.add(field);
		return field;
	}

	private void syncTextFields() {
		if (fieldShipName != null) {
			syncTextField(fieldShipName, shipNameInput);
		}
		if (fieldEnergyUnits != null) {
			syncTextField(fieldEnergyUnits, energyUnitsInput);
		}
		if (fieldDimFront != null && selectedTab != Tab.DIMENSIONS) {
			syncTextField(fieldDimFront, Integer.toString(dimFront));
			syncTextField(fieldDimBack, Integer.toString(dimBack));
			syncTextField(fieldDimRight, Integer.toString(dimRight));
			syncTextField(fieldDimLeft, Integer.toString(dimLeft));
			syncTextField(fieldDimUp, Integer.toString(dimUp));
			syncTextField(fieldDimDown, Integer.toString(dimDown));
		}
		if (fieldMoveFront != null) {
			syncTextField(fieldMoveFront, moveFrontInput);
			syncTextField(fieldMoveUp, moveUpInput);
			syncTextField(fieldMoveRight, moveRightInput);
		}
	}

	private void syncTextField(final GuiTextField field, final String text) {
		if (!field.isFocused()) {
			field.setText(text == null ? "" : text);
		}
	}

	private void updateButtons() {
		final int tabLast = BUTTON_TAB_BASE + Tab.values().length - 1;
		for (final GuiButton button : buttonList) {
			if (button.id >= BUTTON_TAB_BASE && button.id <= tabLast) {
				button.enabled = button.id != BUTTON_TAB_BASE + selectedTab.ordinal();
			} else if (button.id == BUTTON_ENGAGE || button.id == BUTTON_STEP) {
				button.enabled = canEngage || isWaitingConfirm();
			} else if (button.id == BUTTON_PAUSE) {
				button.enabled = allowed && isAutopilotActive();
			} else if (button.id == BUTTON_RESUME) {
				button.enabled = allowed && "paused".equals(autopilotStatus);
			} else if (button.id == BUTTON_CANCEL) {
				button.enabled = allowed && !targetId.isEmpty();
			} else if (button.id == BUTTON_MAP_LOCAL) {
				button.enabled = isMapHyperspaceView;
			} else if (button.id == BUTTON_MAP_HYPERSPACE) {
				button.enabled = !isMapHyperspaceView;
			} else if (button.id != BUTTON_REFRESH && button.id != BUTTON_MAP_FIT && button.id != BUTTON_MODE) {
				button.enabled = allowed;
			} else if (button.id == BUTTON_MODE) {
				button.enabled = allowed;
				button.displayString = autopilotModeLabel();
			}
		}
	}

	private boolean isWaitingConfirm() {
		return "waiting_confirm".equals(autopilotStatus);
	}

	private boolean isAutopilotActive() {
		return "running".equals(autopilotStatus) || "waiting_cooldown".equals(autopilotStatus)
		    || "waiting_energy".equals(autopilotStatus) || "planned".equals(autopilotStatus);
	}

	private String autopilotModeLabel() {
		return I18n.format("warpdrive.navigation.gui.mode") + ": " + I18n.format("warpdrive.navigation.autopilot.mode." + autopilotMode);
	}

	@Override
	protected void actionPerformed(final GuiButton button) {
		final int tabLast = BUTTON_TAB_BASE + Tab.values().length - 1;
		if (button.id >= BUTTON_TAB_BASE && button.id <= tabLast) {
			selectedTab = Tab.values()[button.id - BUTTON_TAB_BASE];
			initGui();
			return;
		}
		switch (button.id) {
		case BUTTON_MAP_FIT:
			resetMapView();
			break;
		case BUTTON_MAP_LOCAL:
			setMapHyperspaceView(false);
			break;
		case BUTTON_MAP_HYPERSPACE:
			setMapHyperspaceView(true);
			break;
		case BUTTON_ENGAGE:
			sendAction(MessageShipNavigationAction.ACTION_ENGAGE, "");
			break;
		case BUTTON_STEP:
			sendAction(MessageShipNavigationAction.ACTION_STEP, "");
			break;
		case BUTTON_MODE:
			sendAction(MessageShipNavigationAction.ACTION_SET_MODE, nextMode());
			break;
		case BUTTON_PAUSE:
			sendAction(MessageShipNavigationAction.ACTION_PAUSE, "");
			break;
		case BUTTON_RESUME:
			sendAction(MessageShipNavigationAction.ACTION_RESUME, "");
			break;
		case BUTTON_CANCEL:
			sendAction(MessageShipNavigationAction.ACTION_CANCEL, "");
			break;
		case BUTTON_REFRESH:
			sendAction(MessageShipNavigationAction.ACTION_REFRESH, "");
			break;
		case BUTTON_SAVE_SHIP:
			sendSettingsPayload(readShipFields());
			break;
		case BUTTON_ENABLE:
			final NBTTagCompound enablePayload = new NBTTagCompound();
			enablePayload.setBoolean("enable", !snapshot.getBoolean("enabled"));
			sendSettingsPayload(enablePayload);
			break;
		case BUTTON_IDLE:
			sendCommand("idle", 0, 0, 0, 0, true);
			break;
		case BUTTON_OFFLINE:
			sendCommand("offline", 0, 0, 0, 0, true);
			break;
		case BUTTON_MAINTENANCE:
			sendCommand("maintenance", 0, 0, 0, 0, true);
			break;
		case BUTTON_MOVE_FORWARD:
			adjustMovement(10, 0, 0);
			break;
		case BUTTON_MOVE_BACK:
			adjustMovement(-10, 0, 0);
			break;
		case BUTTON_MOVE_UP:
			adjustMovement(0, 10, 0);
			break;
		case BUTTON_MOVE_DOWN:
			adjustMovement(0, -10, 0);
			break;
		case BUTTON_MOVE_RIGHT:
			adjustMovement(0, 0, 10);
			break;
		case BUTTON_MOVE_LEFT:
			adjustMovement(0, 0, -10);
			break;
		case BUTTON_ROTATE_LEFT:
			rotationStepsInput = Math.floorMod(rotationStepsInput - 1, 4);
			break;
		case BUTTON_ROTATE_RIGHT:
			rotationStepsInput = Math.floorMod(rotationStepsInput + 1, 4);
			break;
		case BUTTON_PRESET_TAKEOFF:
			setMoveFields(0, Math.max(1, 256 - snapshot.getCompoundTag("dimensions").getInteger("maxY") + 16), 0);
			break;
		case BUTTON_PRESET_LANDING:
			setMoveFields(0, -Math.max(1, snapshot.getCompoundTag("dimensions").getInteger("minY") + 16), 0);
			break;
		case BUTTON_PREVIEW_MOVE:
			sendMovementPayload(MessageShipNavigationAction.ACTION_PREVIEW_MOVEMENT, "manual");
			break;
		case BUTTON_EXECUTE_MOVE:
			sendMovementPayload(MessageShipNavigationAction.ACTION_EXECUTE_MOVEMENT, "manual");
			break;
		case BUTTON_HYPERDRIVE:
			sendCommand("hyperdrive", 0, 0, 0, 0, true);
			break;
		default:
			break;
		}
		updateButtons();
	}

	private void setMapHyperspaceView(final boolean isHyperspaceView) {
		if ( snapshot == null
		  || snapshot.getBoolean("inHyperspace")
		  || this.isMapHyperspaceView == isHyperspaceView ) {
			return;
		}
		this.isMapHyperspaceView = isHyperspaceView;
		computeBounds();
		resetMapView();
	}

	private void setMapViewForSelectedTarget(final String id) {
		if ( snapshot == null
		  || snapshot.getBoolean("inHyperspace") ) {
			return;
		}
		final MapObject selectedObject = findObject(id);
		final MapObject selectedSpaceRegion = selectedObject == null || selectedObject.space ? selectedObject : findContainingSpaceRegion(selectedObject);
		if ( selectedSpaceRegion != null
		  && selectedSpaceRegion.space
		  && !selectedSpaceRegion.hyperspace
		  && !selectedSpaceRegion.id.equals(mapSpaceId) ) {
			setMapHyperspaceView(true);
		}
	}

	private String nextMode() {
		switch (autopilotMode) {
		case "off":          return "assisted";
		case "assisted":     return "safety_stops";
		case "safety_stops": return "full_auto";
		default:             return "off";
		}
	}

	private void sendAction(final byte action, final String targetIdOrMode) {
		PacketHandler.sendShipNavigationAction(action, dimensionId, blockPosCore, blockPosAccess, targetIdOrMode);
	}

	private NBTTagCompound readShipFields() {
		final NBTTagCompound payload = new NBTTagCompound();
		payload.setString("name", fieldShipName == null ? shipNameInput : fieldShipName.getText());
		payload.setString("energyDisplayUnits", fieldEnergyUnits == null ? energyUnitsInput : fieldEnergyUnits.getText());
		return payload;
	}

	private NBTTagCompound readDimensionPayload() {
		final NBTTagCompound payload = new NBTTagCompound();
		final NBTTagCompound positive = new NBTTagCompound();
		positive.setInteger("front", parseInt(fieldDimFront, dimFront));
		positive.setInteger("right", parseInt(fieldDimRight, dimRight));
		positive.setInteger("up", parseInt(fieldDimUp, dimUp));
		payload.setTag("dim_positive", positive);
		final NBTTagCompound negative = new NBTTagCompound();
		negative.setInteger("back", parseInt(fieldDimBack, dimBack));
		negative.setInteger("left", parseInt(fieldDimLeft, dimLeft));
		negative.setInteger("down", parseInt(fieldDimDown, dimDown));
		payload.setTag("dim_negative", negative);
		return payload;
	}

	private void setDimensionDraft(final NBTTagCompound dimensions) {
		dimFront = dimensions.getInteger("front");
		dimBack = dimensions.getInteger("back");
		dimRight = dimensions.getInteger("right");
		dimLeft = dimensions.getInteger("left");
		dimUp = dimensions.getInteger("up");
		dimDown = dimensions.getInteger("down");
	}

	private boolean isDimensionSnapshotMatchingPayload(final NBTTagCompound dimensions, final NBTTagCompound payload) {
		final NBTTagCompound positive = payload.getCompoundTag("dim_positive");
		final NBTTagCompound negative = payload.getCompoundTag("dim_negative");
		return dimensions.getInteger("front") == positive.getInteger("front")
		    && dimensions.getInteger("right") == positive.getInteger("right")
		    && dimensions.getInteger("up") == positive.getInteger("up")
		    && dimensions.getInteger("back") == negative.getInteger("back")
		    && dimensions.getInteger("left") == negative.getInteger("left")
		    && dimensions.getInteger("down") == negative.getInteger("down");
	}

	private boolean areDimensionFieldsValid() {
		return isInteger(fieldDimFront) && isInteger(fieldDimRight) && isInteger(fieldDimUp)
		    && isInteger(fieldDimBack) && isInteger(fieldDimLeft) && isInteger(fieldDimDown);
	}

	private boolean isInteger(final GuiTextField field) {
		if (field == null) {
			return false;
		}
		try {
			Integer.parseInt(field.getText().trim());
			return true;
		} catch (final NumberFormatException exception) {
			return false;
		}
	}

	private void sendSettingsPayload(final NBTTagCompound payload) {
		PacketHandler.sendShipNavigationAction(MessageShipNavigationAction.ACTION_UPDATE_SETTINGS, dimensionId, blockPosCore, blockPosAccess, payload);
	}

	private void sendDimensionPayload(final NBTTagCompound payload) {
		pendingDimensionPayload = payload.copy();
		lastDimensionSendTick = clientTick;
		sendSettingsPayload(payload);
	}

	private void sendMovementPayload(final byte action, final String command) {
		final int moveFront = parseInt(fieldMoveFront, 0);
		final int moveUp = parseInt(fieldMoveUp, 0);
		final int moveRight = parseInt(fieldMoveRight, 0);
		sendCommand(command, moveFront, moveUp, moveRight, rotationStepsInput, action == MessageShipNavigationAction.ACTION_EXECUTE_MOVEMENT);
	}

	private void sendCommand(final String command, final int moveFront, final int moveUp, final int moveRight, final int rotationSteps, final boolean execute) {
		final NBTTagCompound payload = new NBTTagCompound();
		payload.setString("command", command);
		payload.setInteger("moveFront", moveFront);
		payload.setInteger("moveUp", moveUp);
		payload.setInteger("moveRight", moveRight);
		payload.setByte("rotationSteps", (byte) rotationSteps);
		final byte action = execute ? MessageShipNavigationAction.ACTION_EXECUTE_MOVEMENT : MessageShipNavigationAction.ACTION_PREVIEW_MOVEMENT;
		PacketHandler.sendShipNavigationAction(action, dimensionId, blockPosCore, blockPosAccess, payload);
	}

	private void adjustMovement(final int front, final int up, final int right) {
		setMoveFields(parseInt(fieldMoveFront, 0) + front,
		              parseInt(fieldMoveUp, 0) + up,
		              parseInt(fieldMoveRight, 0) + right);
	}

	private void setMoveFields(final int front, final int up, final int right) {
		moveFrontInput = Integer.toString(front);
		moveUpInput = Integer.toString(up);
		moveRightInput = Integer.toString(right);
		if (fieldMoveFront != null) {
			fieldMoveFront.setText(moveFrontInput);
			fieldMoveUp.setText(moveUpInput);
			fieldMoveRight.setText(moveRightInput);
		}
	}

	private int parseInt(final GuiTextField field, final int fallback) {
		if (field == null) {
			return fallback;
		}
		try {
			return Integer.parseInt(field.getText().trim());
		} catch (final NumberFormatException exception) {
			return fallback;
		}
	}

	@Override
	protected void mouseClicked(final int mouseX, final int mouseY, final int mouseButton) throws IOException {
		if (selectedTab == Tab.DESTINATIONS && mouseButton == 0 && isInsidePanelList(mouseX, mouseY)) {
			selectDestination(mouseY);
			return;
		}
		if (mouseButton == 0 && isInsideMap(mouseX, mouseY) && !isOverMapButton(mouseX, mouseY)) {
			isDraggingMap = true;
			dragStartX = mouseX;
			dragStartY = mouseY;
			dragLastX = mouseX;
			dragLastY = mouseY;
			return;
		}
		super.mouseClicked(mouseX, mouseY, mouseButton);
		for (final GuiTextField field : textFields) {
			field.mouseClicked(mouseX, mouseY, mouseButton);
		}
	}

	@Override
	protected void mouseClickMove(final int mouseX, final int mouseY, final int clickedMouseButton, final long timeSinceLastClick) {
		if (isDraggingMap) {
			final double scale = mapScale();
			viewCenterX -= (mouseX - dragLastX) / scale;
			viewCenterZ -= (mouseY - dragLastY) / scale;
			dragLastX = mouseX;
			dragLastY = mouseY;
			return;
		}
		super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
	}

	@Override
	protected void mouseReleased(final int mouseX, final int mouseY, final int state) {
		if (isDraggingMap) {
			final int dx = mouseX - dragStartX;
			final int dy = mouseY - dragStartY;
			isDraggingMap = false;
			if (state == 0 && dx * dx + dy * dy <= 16 && isInsideMap(mouseX, mouseY)) {
				selectMapObject(mouseX, mouseY);
			}
			return;
		}
		super.mouseReleased(mouseX, mouseY, state);
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		final int wheel = Mouse.getEventDWheel();
		if (wheel == 0) {
			return;
		}
		final int mouseX = Mouse.getEventX() * width / mc.displayWidth;
		final int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
		if (selectedTab == Tab.DESTINATIONS && isInsidePanelList(mouseX, mouseY)) {
			destinationsScroll = Math.max(0, destinationsScroll - Integer.signum(wheel));
			return;
		}
		if (!isInsideMap(mouseX, mouseY)) {
			return;
		}
		final double worldX = fromScreenX(mouseX);
		final double worldZ = fromScreenY(mouseY);
		final double zoomFactor = wheel > 0 ? 1.18D : 1.0D / 1.18D;
		mapZoom = clamp(mapZoom * zoomFactor, 0.25D, 32.0D);
		final double scale = mapScale();
		viewCenterX = worldX - (mouseX - (mapX + mapWidth * 0.5D)) / scale;
		viewCenterZ = worldZ - (mouseY - (mapY + mapHeight * 0.5D)) / scale;
	}

	private void selectMapObject(final int mouseX, final int mouseY) {
		if (!allowed) {
			return;
		}
		MapObject bestObject = null;
		double bestDistance = Double.MAX_VALUE;
		for (final MapObject mapObject : mapObjects) {
			if (!isMapObjectDrawable(mapObject)) {
				continue;
			}
			final int screenX = toObjectScreenX(mapObject);
			final int screenY = toObjectScreenY(mapObject);
			final double dx = mouseX - screenX;
			final double dy = mouseY - screenY;
			final double distance = dx * dx + dy * dy;
			final double hitRadius = Math.max(10.0D, renderedRadius(mapObject) + 8.0D);
			if ( distance <= hitRadius * hitRadius
			  && ( bestObject == null
			    || distance < bestDistance
			    || distance == bestDistance && renderedRadius(mapObject) < renderedRadius(bestObject) ) ) {
				bestDistance = distance;
				bestObject = mapObject;
			}
		}
		if (bestObject != null) {
			selectedId = bestObject.id;
			sendAction(MessageShipNavigationAction.ACTION_PLAN, selectedId);
		}
	}

	private void selectDestination(final int mouseY) {
		if (!allowed) {
			return;
		}
		final int rowHeight = 22;
		final int listTop = destinationListTop();
		final int index = destinationsScroll + (mouseY - listTop) / rowHeight;
		if (index >= 0 && index < destinationRows.size()) {
			final DestinationEntry entry = destinationRows.get(index).entry;
			selectedId = entry.id;
			setMapViewForSelectedTarget(entry.id);
			sendAction(MessageShipNavigationAction.ACTION_PLAN, entry.id);
			selectedTab = Tab.MAP;
			initGui();
		}
	}

	@Override
	protected void keyTyped(final char typedChar, final int keyCode) throws IOException {
		for (final GuiTextField field : textFields) {
			if (field.textboxKeyTyped(typedChar, keyCode)) {
				if (selectedTab == Tab.DIMENSIONS) {
					isDimensionDraftDirty = true;
				}
				if ( selectedTab == Tab.DIMENSIONS
				  && areDimensionFieldsValid() ) {
					dimFront = parseInt(fieldDimFront, dimFront);
					dimRight = parseInt(fieldDimRight, dimRight);
					dimUp = parseInt(fieldDimUp, dimUp);
					dimBack = parseInt(fieldDimBack, dimBack);
					dimLeft = parseInt(fieldDimLeft, dimLeft);
					dimDown = parseInt(fieldDimDown, dimDown);
					sendDimensionPayload(readDimensionPayload());
				}
				return;
			}
		}
		super.keyTyped(typedChar, keyCode);
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		clientTick++;
		for (final GuiTextField field : textFields) {
			field.updateCursorCounter();
		}
		if ( selectedTab == Tab.DIMENSIONS
		  && pendingDimensionPayload != null
		  && areDimensionFieldsValid()
		  && clientTick - lastDimensionSendTick >= 10 ) {
			lastDimensionSendTick = clientTick;
			sendSettingsPayload(pendingDimensionPayload.copy());
		}
		// live polling: faster while a jump or autopilot leg is happening
		if (allowed) {
			final int interval = jumpInProgress || cooling || isAutopilotActive() ? 5 : 8;
			if (clientTick - lastRefreshTick >= interval) {
				lastRefreshTick = clientTick;
				sendAction(MessageShipNavigationAction.ACTION_REFRESH, "");
			}
		}
	}

	@Override
	public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
		drawDefaultBackground();
		drawRect(0, 0, width, height, COLOR_BACKDROP);
		drawHeader();
		drawMap(mouseX, mouseY);
		drawPanel(mouseX, mouseY);
		super.drawScreen(mouseX, mouseY, partialTicks);
		for (final GuiTextField field : textFields) {
			field.drawTextBox();
		}
	}

	// ----- always-on live status header -----

	@SuppressWarnings("PMD.NPathComplexity")
	private void drawHeader() {
		drawRect(0, 0, width, HEADER_HEIGHT, COLOR_PANEL);
		drawRect(0, HEADER_HEIGHT - 1, width, HEADER_HEIGHT, COLOR_CYAN_DIM);

		// row 1: name, state pill, location
		final String shipName = snapshot.getString("shipName");
		fontRenderer.drawStringWithShadow(shipName.isEmpty() ? I18n.format("warpdrive.navigation.gui.title") : shipName, 10, 6, COLOR_CYAN);

		final boolean enabled = snapshot.getBoolean("enabled");
		final boolean offline = snapshot.getBoolean("offline");
		final boolean maintenance = snapshot.getBoolean("maintenance");
		final String stateText = offline ? I18n.format("warpdrive.navigation.gui.offline")
		                       : maintenance ? I18n.format("warpdrive.navigation.gui.maintenance")
		                       : cooling ? I18n.format("warpdrive.navigation.gui.cooling")
		                       : jumpInProgress ? I18n.format("warpdrive.navigation.gui.jumping")
		                       : enabled ? I18n.format("warpdrive.navigation.gui.online")
		                       : I18n.format("warpdrive.navigation.gui.offline");
		final int stateColor = offline || maintenance ? COLOR_WARN : jumpInProgress || cooling ? COLOR_AMBER : COLOR_OK;
		drawPill(180, 5, stateText, stateColor);

		final String location = snapshot.getString("currentCelestialName");
		final String region = snapshot.getBoolean("inHyperspace") ? I18n.format("warpdrive.navigation.gui.hyperspace")
		                    : snapshot.getBoolean("inSpace") ? I18n.format("warpdrive.navigation.gui.space_region")
		                    : I18n.format("warpdrive.navigation.gui.celestial_body");
		final String locText = (location.isEmpty() ? "?" : location) + "  [" + region + "]";
		fontRenderer.drawString(trimToWidth(locText, Math.max(60, width / 2)), width - 12 - fontRenderer.getStringWidth(trimToWidth(locText, Math.max(60, width / 2))), 6, COLOR_TEXT_DIM);

		// row 2: energy bar + warmup/cooldown bars (or mass/movement when idle)
		final int barY = 24;
		final int barH = 14;
		final int third = Math.max(120, (width - 30) / 3);
		final NBTTagCompound energyStatus = snapshot.getCompoundTag("energyStatus");
		final long energyStored = energyStatus.getLong("stored");
		final long energyCapacity = Math.max(1L, energyStatus.getLong("capacity"));
		final String energyUnits = snapshot.getString("energyUnits");
		drawBar(10, barY, third - 8, barH, energyStored / (double) energyCapacity, COLOR_CYAN,
		        I18n.format("warpdrive.navigation.gui.energy") + " " + Commons.format(energyStored) + " / " + Commons.format(energyCapacity) + " " + energyUnits);

		final int warmupRemaining = Math.max(0, warmupTicks - (clientTick - snapshotClientTick));
		final int cooldownRemaining = Math.max(0, cooldownTicks - (clientTick - snapshotClientTick));
		if (jumpInProgress && warmupTicks > 0) {
			drawBar(10 + third, barY, third - 8, barH, 1.0D - warmupRemaining / (double) warmupTotal, COLOR_AMBER,
			        I18n.format("warpdrive.navigation.gui.warmup") + " " + (warmupRemaining / 20 + 1) + "s");
		} else {
			fontRenderer.drawString(I18n.format("warpdrive.navigation.gui.mass") + ": " + Commons.format(snapshot.getInteger("shipMass"))
			                        + " t / " + Commons.format(snapshot.getInteger("shipVolume")), 10 + third, barY + 3, COLOR_TEXT_DIM);
		}
		if (cooling && cooldownRemaining > 0) {
			drawBar(10 + third * 2, barY, third - 8, barH, cooldownRemaining / (double) cooldownTotal, COLOR_WARN,
			        I18n.format("warpdrive.navigation.gui.cooldown") + " " + (cooldownRemaining / 20 + 1) + "s");
		} else if (routeMaximumDistance > 0) {
			final String range = routeEffectiveDistance > 0
			                   ? I18n.format("warpdrive.navigation.gui.range_current", Commons.format(routeEffectiveDistance), Commons.format(routeMaximumDistance))
			                   : I18n.format("warpdrive.navigation.gui.range_blocks", Commons.format(routeMaximumDistance));
			fontRenderer.drawString(trimToWidth(I18n.format("warpdrive.navigation.gui.range") + ": " + range, third - 12),
			                        10 + third * 2, barY + 3, COLOR_TEXT_DIM);
		} else if (!driveMovementType.isEmpty()) {
			fontRenderer.drawString(I18n.format("warpdrive.navigation.movement_type." + driveMovementType), 10 + third * 2, barY + 3, COLOR_TEXT_DIM);
		}
	}

	private void drawPill(final int x, final int y, final String text, final int color) {
		final int w = fontRenderer.getStringWidth(text) + 10;
		drawRect(x, y, x + w, y + 12, 0x55000000 | (color & 0x00FFFFFF));
		drawRect(x, y, x + w, y + 1, color);
		drawRect(x, y + 11, x + w, y + 12, color);
		fontRenderer.drawString(text, x + 5, y + 2, color);
	}

	private void drawBar(final int x, final int y, final int w, final int h, final double fraction, final int color, final String label) {
		final double clamped = Math.max(0.0D, Math.min(1.0D, fraction));
		drawRect(x, y, x + w, y + h, 0xC0050A12);
		drawRect(x, y, x + (int) (w * clamped), y + h, 0x88000000 | (color & 0x00FFFFFF));
		drawRect(x, y, x + w, y + 1, color);
		drawRect(x, y + h - 1, x + w, y + h, 0x40000000 | (color & 0x00FFFFFF));
		fontRenderer.drawString(trimToWidth(label, w - 6), x + 4, y + (h - 8) / 2, COLOR_TEXT);
	}

	// ----- starmap -----

	@SuppressWarnings("PMD.NPathComplexity")
	private void drawMap(final int mouseX, final int mouseY) {
		drawRect(mapX, mapY, mapX + mapWidth, mapY + mapHeight, 0xDD040810);
		drawRect(mapX, mapY, mapX + mapWidth, mapY + 1, COLOR_CYAN_DIM);
		drawRect(mapX, mapY + mapHeight - 1, mapX + mapWidth, mapY + mapHeight, COLOR_CYAN_DIM);
		final boolean isHyperspaceView = isHyperspaceMapView();
		final MapObject current = findObject(currentId);
		final MapObject currentSpaceRegion = findContainingSpaceRegion(current);
		final String localTitle = currentSpaceRegion == null ? I18n.format("warpdrive.navigation.gui.starmap") : currentSpaceRegion.name;
		fontRenderer.drawString(isHyperspaceView ? I18n.format("warpdrive.navigation.gui.hyperspace_chart") : trimToWidth(localTitle, 76),
		                        mapX + 10, mapY + 8, 0xFFE8F3FF);
		fontRenderer.drawString(I18n.format("warpdrive.navigation.gui.zoom", String.format("%.1f", mapZoom)), mapX + 92, mapY + 8, COLOR_TEXT_DIM);
		if (isHyperspaceView) {
			fontRenderer.drawString(I18n.format("warpdrive.navigation.gui.hyperspace_chart_hint"), mapX + 10, shouldShowMapViewButtons() ? mapY + 43 : mapY + 21, COLOR_TEXT_DIM);
		}

		final long time = System.currentTimeMillis() - openedAtMs;
		for (int index = 0; index < 72; index++) {
			final int x = mapX + 12 + Math.floorMod(index * 73, Math.max(1, mapWidth - 24));
			final int y = mapY + 30 + Math.floorMod(index * 41, Math.max(1, mapHeight - 44));
			final int brightness = 90 + (int) (50.0D * (0.5D + 0.5D * Math.sin((time + index * 137) / 900.0D)));
			drawRect(x, y, x + 1, y + 1, 0xFF000000 | brightness << 16 | brightness << 8 | brightness);
		}

		final MapObject target = visibleTargetFor(targetId);
		if (!isStaticMapReady || mapObjects.isEmpty()) {
			drawCenteredString(fontRenderer, I18n.format("warpdrive.navigation.gui.map_loading"), mapX + mapWidth / 2, mapY + mapHeight / 2, COLOR_AMBER);
			return;
		}
		final boolean isShipInBodyDimension = !snapshot.getBoolean("inSpace")
		                                   && !snapshot.getBoolean("inHyperspace")
		                                   && current != null
		                                   && !current.space
		                                   && !current.hyperspace;
		final MapObject shipMapObject = isHyperspaceView && !snapshot.getBoolean("inHyperspace") ? currentSpaceRegion
		                          : isShipInBodyDimension ? current : null;
		final double shipDisplayX = shipMapObject == null ? shipMapX : shipMapObject.displayMapX;
		final double shipDisplayZ = shipMapObject == null ? shipMapZ : shipMapObject.displayMapZ;
		if (!isHyperspaceView) {
			final MapObject spaceRegion = current != null && current.space ? current : findSpaceRegion();
			if (spaceRegion != null) {
				drawSpaceRegionBackdrop(spaceRegion);
			}
		}
		if (target != null) {
			final int shipScreenX = shipMapObject == null ? toScreenX(shipDisplayX) : toObjectScreenX(shipMapObject);
			final int shipScreenY = shipMapObject == null ? toScreenY(shipDisplayZ) : toObjectScreenY(shipMapObject);
			drawLine(shipScreenX, shipScreenY, toObjectScreenX(target), toObjectScreenY(target), 0xAA2DD4FF);
		}
		for (final MapObject mapObject : mapObjects) {
			if (!isMapObjectDrawable(mapObject)) {
				continue;
			}
			final MapObject parent = findObject(mapObject.parentId);
			if (parent != null && isMapObjectDrawable(parent) && !mapObject.hyperspace) {
				drawLine(toObjectScreenX(parent), toObjectScreenY(parent),
				         toObjectScreenX(mapObject), toObjectScreenY(mapObject), 0x33477C9A);
			}
		}

		for (final MapObject mapObject : mapObjects) {
			if (!isMapObjectDrawable(mapObject)) {
				continue;
			}
			drawObject(mapObject, mouseX, mouseY, time);
		}

		final ArrayList<LabelBounds> occupiedLabels = new ArrayList<>();
		drawMapObjectLabels(occupiedLabels);

		final int x = shipMapObject == null ? toScreenX(shipDisplayX) : toObjectScreenX(shipMapObject);
		final int y = shipMapObject == null ? toScreenY(shipDisplayZ) : toObjectScreenY(shipMapObject);
		drawCircle(x, y, 9.0F + (float) (2.0D * Math.sin(time / 250.0D)), 0x6630E8FF, 40);
		drawMapLabel(occupiedLabels, I18n.format("warpdrive.navigation.gui.ship"), x, y, 10, 0xFF9DEBFF);
		drawMapLabel(occupiedLabels, trimToWidth("X " + Commons.format(shipMapX) + "  Z " + Commons.format(shipMapZ), 120),
		             x, y + 11, 10, COLOR_TEXT_DIM);
	}

	private void drawSpaceRegionBackdrop(final MapObject spaceRegion) {
		final int color = 0x66000000
		                | ((int) (spaceRegion.red * 255.0F) & 0xFF) << 16
		                | ((int) (spaceRegion.green * 255.0F) & 0xFF) << 8
		                | ((int) (spaceRegion.blue * 255.0F) & 0xFF);
		drawRect(mapX + 6, mapY + 24, mapX + mapWidth - 6, mapY + 25, color);
		drawRect(mapX + 6, mapY + mapHeight - 7, mapX + mapWidth - 6, mapY + mapHeight - 6, color);
		drawRect(mapX + 6, mapY + 24, mapX + 7, mapY + mapHeight - 6, color);
		drawRect(mapX + mapWidth - 7, mapY + 24, mapX + mapWidth - 6, mapY + mapHeight - 6, color);
		fontRenderer.drawString(trimToWidth(spaceRegion.name, mapWidth - 110), mapX + mapWidth - 86, mapY + 8, COLOR_TEXT_DIM);
	}

	@SuppressWarnings("PMD.NPathComplexity")
	private void drawObject(final MapObject mapObject, final int mouseX, final int mouseY, final long time) {
		final int x = toObjectScreenX(mapObject);
		final int y = toObjectScreenY(mapObject);
		if (x < mapX - 48 || x > mapX + mapWidth + 48 || y < mapY - 48 || y > mapY + mapHeight + 48) {
			return;
		}
		final float radius = renderedRadius(mapObject);
		final int rgb = 0xFF000000
		              | ((int) (mapObject.red * 255.0F) & 0xFF) << 16
		              | ((int) (mapObject.green * 255.0F) & 0xFF) << 8
		              | ((int) (mapObject.blue * 255.0F) & 0xFF);
		if (mapObject.space || mapObject.hyperspace) {
			drawCircle(x, y, radius + 9.0F, 0x332D7CFF, 56);
			drawCircle(x, y, radius + 4.0F, 0x2214F0FF, 56);
		} else {
			drawCircle(x, y, radius + 5.0F, 0x221E90FF, 36);
		}
		if (mapObject.id.equals(visibleTargetId(targetId))) {
			drawCircle(x, y, radius + 9.0F + (float) (2.0D * Math.sin(time / 220.0D)), 0x88F5C542, 48);
		}
		if (mapObject.id.equals(selectedId) || mapObject.id.equals(visibleTargetId(selectedId))) {
			drawCircle(x, y, radius + 6.0F, 0x99FFFFFF, 48);
		}
		if (mapObject.hyperspace) {
			drawDiamond(x, y, radius + 5.0F, 0x22000000 | (rgb & 0x00FFFFFF));
			drawIconTexture(mapObject.iconTexture, x, y, radius + 2.0F, 0xFFFFFFFF, 0.90F);
			drawDiamond(x, y, radius + 2.0F, rgb);
			drawDiamond(x, y, radius + 6.0F, 0x99FFAA55);
		} else if (mapObject.space) {
			drawDiamond(x, y, radius + 6.0F, 0x22000000 | (rgb & 0x00FFFFFF));
			drawIconTexture(mapObject.iconTexture, x, y, radius + 4.0F, 0xFFFFFFFF, 0.90F);
			drawDiamond(x, y, radius + 3.0F, rgb);
			drawCircle(x, y, radius * 0.45F, 0xFFF5C542, 24);
		} else {
			drawCircle(x, y, radius + 1.0F, 0x33000000 | (rgb & 0x00FFFFFF), 36);
			drawIconTexture(mapObject.iconTexture, x, y, radius, 0xFFFFFFFF, 0.95F);
			drawCircleOutline(x, y, radius + 1.0F, rgb, 36);
			drawLine(x - (int) (radius + 5), y, x + (int) (radius + 5), y, 0x8830E8FF);
		}
		if (mapObject.virtual) {
			drawCircle(x, y, radius + 2.0F, 0x55808080, 36);
		}

		final double dx = mouseX - x;
		final double dy = mouseY - y;
		if (dx * dx + dy * dy < (radius + 8.0F) * (radius + 8.0F)) {
			final ArrayList<String> tooltip = new ArrayList<>();
			tooltip.add(mapObject.name);
			tooltip.add(mapObject.virtual ? I18n.format("warpdrive.navigation.gui.visual_only") : mapObject.space ? I18n.format("warpdrive.navigation.gui.space_region") : mapObject.hyperspace ? I18n.format("warpdrive.navigation.gui.hyperspace") : I18n.format("warpdrive.navigation.gui.celestial_body"));
			tooltip.add("X " + Commons.format(mapObject.mapX) + "  Z " + Commons.format(mapObject.mapZ));
			tooltip.add(I18n.format("warpdrive.navigation.gui.id", mapObject.id));
			drawHoveringText(tooltip, mouseX, mouseY);
		}
	}

	private void drawMapObjectLabels(final ArrayList<LabelBounds> occupiedLabels) {
		for (final MapObject mapObject : mapObjects) {
			if (!isMapObjectDrawable(mapObject)) {
				continue;
			}
			if (mapZoom < 1.5D && !mapObject.id.equals(currentId) && !mapObject.id.equals(targetId)) {
				continue;
			}
			final int x = toObjectScreenX(mapObject);
			final int y = toObjectScreenY(mapObject);
			if (x < mapX - 48 || x > mapX + mapWidth + 48 || y < mapY - 48 || y > mapY + mapHeight + 48) {
				continue;
			}
			drawMapLabel(occupiedLabels, trimToWidth(mapObject.name, 110), x, y, Math.round(renderedRadius(mapObject)) + 4, 0xFFC8D6E0);
		}
	}

	private void drawMapLabel(final ArrayList<LabelBounds> occupiedLabels,
	                          final String label,
	                          final int markerX,
	                          final int markerY,
	                          final int markerRadius,
	                          final int color) {
		if (label == null || label.isEmpty()) {
			return;
		}
		final int widthLabel = fontRenderer.getStringWidth(label);
		final int heightLabel = 9;
		final int labelX = Math.max(mapX + 4, Math.min(mapX + mapWidth - widthLabel - 4, markerX + markerRadius + 4));
		final int labelY = Math.max(mapY + 26, Math.min(mapY + mapHeight - heightLabel - 4, markerY - heightLabel / 2));
		final LabelBounds bounds = placeMapLabel(occupiedLabels, labelX, labelY, widthLabel, heightLabel);
		if (Math.abs(bounds.x - labelX) > 4 || Math.abs(bounds.y - labelY) > 6) {
			drawLine(markerX, markerY, bounds.x - 3, bounds.y + heightLabel / 2, 0x5530E8FF);
		}
		fontRenderer.drawString(label, bounds.x, bounds.y, color);
		occupiedLabels.add(bounds);
	}

	private LabelBounds placeMapLabel(final ArrayList<LabelBounds> occupiedLabels,
	                                  final int preferredX,
	                                  final int preferredY,
	                                  final int widthLabel,
	                                  final int heightLabel) {
		LabelBounds best = null;
		int bestScore = Integer.MAX_VALUE;
		for (int ring = 0; ring < 18; ring++) {
			final int offsetY = ring == 0 ? 0 : ((ring + 1) / 2) * 11 * (ring % 2 == 0 ? -1 : 1);
			final int[] offsetsX = { 0, -widthLabel - 12, 18 };
			for (final int offsetX : offsetsX) {
				final int x = Math.max(mapX + 4, Math.min(mapX + mapWidth - widthLabel - 4, preferredX + offsetX));
				final int y = Math.max(mapY + 26, Math.min(mapY + mapHeight - heightLabel - 4, preferredY + offsetY));
				final LabelBounds candidate = new LabelBounds(x, y, x + widthLabel + 2, y + heightLabel);
				if (intersectsAny(candidate, occupiedLabels)) {
					continue;
				}
				final int score = Math.abs(x - preferredX) + Math.abs(y - preferredY) * 3;
				if (score < bestScore) {
					best = candidate;
					bestScore = score;
				}
			}
			if (best != null) {
				return best;
			}
		}
		final int fallbackY = Math.max(mapY + 26, Math.min(mapY + mapHeight - heightLabel - 4,
		                                                   preferredY + occupiedLabels.size() * 11));
		return new LabelBounds(preferredX, fallbackY, preferredX + widthLabel + 2, fallbackY + heightLabel);
	}

	private static boolean intersectsAny(final LabelBounds candidate, final ArrayList<LabelBounds> occupiedLabels) {
		for (final LabelBounds occupied : occupiedLabels) {
			if (candidate.intersects(occupied)) {
				return true;
			}
		}
		return false;
	}

	// ----- side panel -----

	private void drawPanel(final int mouseX, final int mouseY) {
		drawRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_PANEL);
		drawRect(panelX, panelY, panelX + panelWidth, panelY + 1, COLOR_PANEL_EDGE);
		switch (selectedTab) {
		case MAP:
			drawMapTab();
			break;
		case DESTINATIONS:
			drawDestinationsTab(mouseX, mouseY);
			break;
		case SHIP:
			drawShipTab();
			break;
		case DIMENSIONS:
			drawDimensionsTab();
			break;
		case MOVE:
			drawMoveTab();
			break;
		case DRIVE:
			drawDriveTab();
			break;
		default:
			break;
		}
		if (!notice.isEmpty()) {
			fontRenderer.drawString(trimToWidth(localize(notice), panelWidth - 20), panelX + 10, panelY + panelHeight - 10, COLOR_OK);
		}
	}

	@SuppressWarnings("PMD.NPathComplexity")
	private void drawMapTab() {
		int y = panelContentY();
		// autopilot status line
		drawStatusLine(I18n.format("warpdrive.navigation.gui.autopilot"),
		               I18n.format("warpdrive.navigation.autopilot.status." + autopilotStatus)
		               + (autopilotLegs > 0 ? "  (" + autopilotLegs + ")" : ""), y);
		y += 14;

		final MapObject selected = findObject(selectedId);
		fontRenderer.drawString(I18n.format("warpdrive.navigation.gui.destination"), panelX + 10, y, COLOR_LABEL);
		y += 12;
		if (selected == null) {
			fontRenderer.drawString(allowed ? I18n.format("warpdrive.navigation.gui.no_target") : I18n.format("warpdrive.navigation.gui.access_denied"),
			                        panelX + 10, y, allowed ? COLOR_TEXT : COLOR_WARN);
			y += 14;
		} else {
			for (final String line : fontRenderer.listFormattedStringToWidth(selected.name, panelWidth - 20)) {
				fontRenderer.drawString(line, panelX + 10, y, selected.virtual ? COLOR_AMBER : 0xFFFFFFFF);
				y += 10;
			}
			y += 2;
		}
		y += 4;
		fontRenderer.drawString(I18n.format("warpdrive.navigation.gui.route"), panelX + 10, y, COLOR_LABEL);
		y += 12;
		fontRenderer.drawString(trimToWidth(routeSummaryText(), panelWidth - 20), panelX + 10, y, COLOR_TEXT);
		y += 12;
		final String warning = routeWarningText();
		if (!warning.isEmpty()) {
			for (final String line : fontRenderer.listFormattedStringToWidth(warning, panelWidth - 20)) {
				fontRenderer.drawString(line, panelX + 10, y, COLOR_AMBER);
				y += 10;
			}
			y += 2;
		}
		int legIndex = 1;
		for (final RouteLeg routeLeg : routeLegs) {
			if (y > panelY + panelHeight - 76) {
				break;
			}
			final int legHeight = !routeLeg.preview && routeLeg.maximumDistance > 0 ? 32 : 22;
			drawRect(panelX + 9, y - 2, panelX + panelWidth - 9, y + legHeight, routeLeg.preview ? 0x44212936 : 0x6630475B);
			fontRenderer.drawString(trimToWidth(legIndex + ". " + routeLeg.title(), panelWidth - 92), panelX + 15, y, routeLeg.preview ? 0xFF91A4B8 : 0xFFFFFFFF);
			if (routeLeg.requiresConfirmation) {
				fontRenderer.drawString(I18n.format("warpdrive.navigation.gui.confirm_tag"), panelX + panelWidth - 70, y, COLOR_AMBER);
			}
			fontRenderer.drawString(trimToWidth(routeLeg.description(), panelWidth - 32), panelX + 15, y + 10, 0xFF8FA1B2);
			if (!routeLeg.preview && routeLeg.maximumDistance > 0) {
				final String range = routeLeg.effectiveDistance > 0
				                   ? I18n.format("warpdrive.navigation.gui.range_current", Commons.format(routeLeg.effectiveDistance), Commons.format(routeLeg.maximumDistance))
				                   : I18n.format("warpdrive.navigation.gui.range_blocks", Commons.format(routeLeg.maximumDistance));
				fontRenderer.drawString(trimToWidth(I18n.format("warpdrive.navigation.gui.range") + ": " + range, panelWidth - 32),
				                        panelX + 15, y + 20, COLOR_TEXT_DIM);
			}
			y += legHeight + 5;
			legIndex++;
		}
	}

	@SuppressWarnings("PMD.NPathComplexity")
	private void drawDestinationsTab(final int mouseX, final int mouseY) {
		fontRenderer.drawString(I18n.format("warpdrive.navigation.gui.destinations_hint"), panelX + 10, panelContentY(), COLOR_TEXT_DIM);
		final int rowHeight = 22;
		final int listTop = destinationListTop();
		final int rows = Math.max(1, (panelY + panelHeight - 14 - listTop) / rowHeight);
		destinationsScroll = Math.max(0, Math.min(destinationsScroll, Math.max(0, destinationRows.size() - rows)));
		for (int row = 0; row < rows; row++) {
			final int index = destinationsScroll + row;
			if (index >= destinationRows.size()) {
				break;
			}
			final DestinationRow destinationRow = destinationRows.get(index);
			final DestinationEntry entry = destinationRow.entry;
			final int y = listTop + row * rowHeight;
			final boolean hover = mouseX >= panelX + 6 && mouseX <= panelX + panelWidth - 6 && mouseY >= y - 1 && mouseY < y + rowHeight - 1;
			drawRect(panelX + 6, y - 1, panelX + panelWidth - 6, y + rowHeight - 2,
			         entry.id.equals(targetId) ? 0x55F5C542 : entry.current ? 0x4430E8FF : entry.space ? 0x33203A52 : hover ? 0x33304B5B : 0x22141E2A);
			final int nameColor = entry.current ? COLOR_OK : entry.reachable ? 0xFFFFFFFF : COLOR_TEXT_DIM;
			final int indent = 12 + destinationRow.depth * 14;
			final String icon = entry.hyperspace ? "* " : entry.space ? "> " : destinationRow.depth > 0 ? "- " : "o ";
			fontRenderer.drawString(trimToWidth(icon + entry.name, panelWidth - 120 - indent), panelX + indent, y + 2, nameColor);
			final String meta;
			if (entry.current) {
				meta = I18n.format("warpdrive.navigation.gui.here") + " - X " + Commons.format(entry.mapX) + " Z " + Commons.format(entry.mapZ);
			} else if (!entry.reachable) {
				meta = "X " + Commons.format(entry.mapX) + " Z " + Commons.format(entry.mapZ) + " - " + I18n.format("warpdrive.navigation.gui.unreachable");
			} else {
				meta = "X " + Commons.format(entry.mapX) + " Z " + Commons.format(entry.mapZ) + " - " + I18n.format("warpdrive.navigation.gui.dest_meta",
				                   entry.legs, Commons.format(entry.energy), entry.eta,
				                   Commons.format(entry.distance), Commons.format(entry.maxRange));
			}
			fontRenderer.drawString(trimToWidth(meta, panelWidth - 24 - indent), panelX + indent, y + 11, entry.reachable || entry.current ? COLOR_TEXT_DIM : COLOR_WARN);
		}
		if (destinationRows.size() > rows) {
			fontRenderer.drawString("^v " + (destinationsScroll + 1) + "/" + destinationRows.size(), panelX + panelWidth - 56, panelContentY(), COLOR_TEXT_DIM);
		}
	}

	private void drawShipTab() {
		int y = panelContentY();
		fontRenderer.drawString(I18n.format("warpdrive.navigation.gui.name"), panelX + 10, y + 4, COLOR_LABEL);
		y += 26;
		fontRenderer.drawString(I18n.format("warpdrive.navigation.gui.energy_units"), panelX + 10, y + 4, COLOR_LABEL);
		y += 78;
		drawStatusLine(I18n.format("warpdrive.navigation.gui.tier"), snapshot.getCompoundTag("tier").getString("name"), y);
		y += 12;
		drawStatusLine(I18n.format("warpdrive.navigation.gui.version"), snapshot.getCompoundTag("version").getString("text"), y);
		y += 12;
		drawStatusLine(I18n.format("warpdrive.navigation.gui.assembly"), snapshot.getCompoundTag("assembly").getString("status"), y);
		y += 12;
		drawStatusLine(I18n.format("warpdrive.navigation.gui.upgrades"), snapshot.getCompoundTag("upgrades").getString("status"), y);
	}

	private void drawDimensionsTab() {
		int y = panelContentY();
		fontRenderer.drawString(I18n.format("warpdrive.navigation.gui.front"), panelX + 10, y + 4, COLOR_LABEL);
		fontRenderer.drawString(I18n.format("warpdrive.navigation.gui.back"), panelX + 152, y + 4, COLOR_LABEL);
		y += 24;
		fontRenderer.drawString(I18n.format("warpdrive.navigation.gui.right"), panelX + 10, y + 4, COLOR_LABEL);
		fontRenderer.drawString(I18n.format("warpdrive.navigation.gui.left"), panelX + 152, y + 4, COLOR_LABEL);
		y += 24;
		fontRenderer.drawString(I18n.format("warpdrive.navigation.gui.up"), panelX + 10, y + 4, COLOR_LABEL);
		fontRenderer.drawString(I18n.format("warpdrive.navigation.gui.down"), panelX + 152, y + 4, COLOR_LABEL);
		y += 56;
		drawBoundsDiagram(panelX + 12, y, panelWidth - 24, Math.min(90, panelY + panelHeight - y - 14));
	}

	private void drawMoveTab() {
		int y = panelContentY();
		fontRenderer.drawString(I18n.format("warpdrive.navigation.gui.front"), panelX + 10, y + 4, COLOR_LABEL);
		fontRenderer.drawString(I18n.format("warpdrive.navigation.gui.up"), panelX + 158, y + 4, COLOR_LABEL);
		y += 24;
		fontRenderer.drawString(I18n.format("warpdrive.navigation.gui.right"), panelX + 10, y + 4, COLOR_LABEL);
		y += 150;
		drawStatusLine(I18n.format("warpdrive.navigation.gui.rotation"), I18n.format("warpdrive.navigation.gui.quarter_turns", rotationStepsInput), y);
		y += 12;
		drawPreview(y);
	}

	private void drawDriveTab() {
		int y = panelContentY() + 34;
		drawStatusLine(I18n.format("warpdrive.navigation.gui.region"),
		               snapshot.getBoolean("inHyperspace") ? I18n.format("warpdrive.navigation.gui.region.hyperspace")
		               : snapshot.getBoolean("inSpace") ? I18n.format("warpdrive.navigation.gui.region.space")
		               : I18n.format("warpdrive.navigation.gui.region.planet"), y);
		y += 12;
		if (!driveMovementType.isEmpty()) {
			drawStatusLine(I18n.format("warpdrive.navigation.gui.drive"), I18n.format("warpdrive.navigation.movement_type." + driveMovementType), y);
			y += 12;
		}
		drawPreview(y);
	}

	private void drawPreview(final int yStart) {
		final NBTTagCompound preview = snapshot.getCompoundTag("movementPreview");
		int y = yStart;
		fontRenderer.drawString(I18n.format("warpdrive.navigation.gui.preview"), panelX + 10, y, COLOR_LABEL);
		y += 12;
		final String command = preview.getString("command");
		fontRenderer.drawString(trimToWidth(command, panelWidth - 20), panelX + 10, y, preview.getBoolean("canEngage") ? COLOR_TEXT : COLOR_AMBER);
		y += 12;
		fontRenderer.drawString(trimToWidth(I18n.format("warpdrive.navigation.gui.preview_move",
		                                                 preview.getInteger("effectiveMoveFront"), preview.getInteger("effectiveMoveUp"), preview.getInteger("effectiveMoveRight"),
		                                                 preview.getInteger("effectiveDistance"), preview.getInteger("maximumDistance")), panelWidth - 20),
		                        panelX + 10, y, 0xFFB4C5D6);
		y += 12;
		fontRenderer.drawString(trimToWidth(I18n.format("warpdrive.navigation.gui.preview_energy",
		                                                 Commons.format(preview.getInteger("energyRequired")), Commons.format(preview.getLong("energyStored"))), panelWidth - 20),
		                        panelX + 10, y, 0xFFB4C5D6);
		y += 12;
		if (preview.getBoolean("wouldBeClamped")) {
			fontRenderer.drawString(I18n.format("warpdrive.navigation.gui.preview_clamped"), panelX + 10, y, COLOR_AMBER);
			y += 12;
		}
		final String blocker = preview.getString("blockerKey");
		if (!blocker.isEmpty()) {
			for (final String line : fontRenderer.listFormattedStringToWidth(I18n.format(blocker), panelWidth - 20)) {
				fontRenderer.drawString(line, panelX + 10, y, COLOR_WARN);
				y += 10;
			}
		}
	}

	private void drawBoundsDiagram(final int x, final int y, final int w, final int h) {
		if (h <= 10) {
			return;
		}
		drawRect(x, y, x + w, y + h, 0x44212A35);
		final int centerX = x + w / 2;
		final int centerY = y + h / 2;
		final int front = Math.max(8, Math.min(w / 2 - 8, dimFront * (w / 2 - 8) / Math.max(1, dimFront + dimBack)));
		final int back = Math.max(8, Math.min(w / 2 - 8, dimBack * (w / 2 - 8) / Math.max(1, dimFront + dimBack)));
		final int right = Math.max(8, Math.min(h / 2 - 8, dimRight * (h / 2 - 8) / Math.max(1, dimRight + dimLeft)));
		final int left = Math.max(8, Math.min(h / 2 - 8, dimLeft * (h / 2 - 8) / Math.max(1, dimRight + dimLeft)));
		drawRect(centerX - back, centerY - left, centerX + front, centerY + right, 0x8860A8FF);
		drawRect(centerX - 2, centerY - 2, centerX + 3, centerY + 3, COLOR_AMBER);
		fontRenderer.drawString("F", centerX + front + 4, centerY - 4, COLOR_TEXT);
		fontRenderer.drawString("B", centerX - back - 10, centerY - 4, COLOR_TEXT);
		fontRenderer.drawString("R", centerX - 3, centerY + right + 3, COLOR_TEXT);
		fontRenderer.drawString("L", centerX - 3, centerY - left - 11, COLOR_TEXT);
		fontRenderer.drawString(I18n.format("warpdrive.navigation.gui.vertical", dimUp, dimDown), x + 6, y + h - 12, 0xFFB4C5D6);
	}

	private String routeSummaryText() {
		if (canEngage && !routeLegs.isEmpty()) {
			return routeLegs.get(0).title();
		}
		return routeStatusKey.isEmpty() ? I18n.format("warpdrive.navigation.gui.no_route_plotted") : I18n.format(routeStatusKey);
	}

	private String routeWarningText() {
		if (!routeWarningKey.isEmpty()) {
			return I18n.format(routeWarningKey, routeWarningStep, routeWarningRemaining);
		}
		if (!routeBlockerKey.isEmpty()) {
			return I18n.format(routeBlockerKey);
		}
		return "";
	}

	private String localize(final String text) {
		if (text != null && text.startsWith("warpdrive.")) {
			return I18n.format(text);
		}
		return text == null ? "" : text;
	}

	private void drawStatusLine(final String label, final String value, final int y) {
		fontRenderer.drawString(label, panelX + 10, y, COLOR_TEXT_DIM);
		fontRenderer.drawString(trimToWidth(value == null ? "" : value, panelWidth - 96), panelX + 88, y, COLOR_TEXT);
	}

	private String trimToWidth(final String text, final int maximumWidth) {
		if (fontRenderer.getStringWidth(text) <= maximumWidth) {
			return text;
		}
		return fontRenderer.trimStringToWidth(text, Math.max(1, maximumWidth - fontRenderer.getStringWidth("..."))) + "...";
	}

	private int panelContentY() {
		return panelY + PANEL_TAB_HEIGHT;
	}

	private int destinationListTop() {
		return panelContentY() + 14;
	}

	private float renderedRadius(final MapObject mapObject) {
		final double bodyRadius = Math.max(mapObject.borderRadiusX, mapObject.borderRadiusZ) * mapScale();
		final float minimum = mapObject.hyperspace ? 13.0F : mapObject.space ? 10.0F : 6.0F;
		return (float) clamp(Math.max(minimum, bodyRadius), minimum, mapObject.space || mapObject.hyperspace ? 34.0D : 28.0D);
	}

	private boolean isMapObjectDrawable(final MapObject mapObject) {
		if (mapObject == null) {
			return false;
		}
		if (isHyperspaceMapView()) {
			return mapObject.space && !mapObject.hyperspace;
		}
		return isObjectInCurrentSpaceRegion(mapObject)
		    && !mapObject.space
		    && !mapObject.hyperspace;
	}

	private boolean isObjectInCurrentSpaceRegion(final MapObject mapObject) {
		final MapObject containingSpaceRegion = findContainingSpaceRegion(mapObject);
		return containingSpaceRegion != null && containingSpaceRegion.id.equals(mapSpaceId);
	}

	private boolean isHyperspaceMapView() {
		return snapshot != null && (snapshot.getBoolean("inHyperspace") || isMapHyperspaceView);
	}

	private boolean shouldShowMapViewButtons() {
		return snapshot != null && !snapshot.getBoolean("inHyperspace");
	}

	private static boolean isSameMapStack(final MapObject mapObject1, final MapObject mapObject2) {
		return !mapObject1.space
		    && !mapObject1.hyperspace
		    && !mapObject2.space
		    && !mapObject2.hyperspace
		    && mapObject1.parentId.equals(mapObject2.parentId)
		    && mapObject1.mapX == mapObject2.mapX
		    && mapObject1.mapZ == mapObject2.mapZ;
	}

	private String visibleTargetId(final String id) {
		final MapObject mapObject = visibleTargetFor(id);
		return mapObject == null ? "" : mapObject.id;
	}

	private MapObject visibleTargetFor(final String id) {
		MapObject mapObject = findObject(id);
		while ( mapObject != null
		     && !isMapObjectDrawable(mapObject)
		     && !mapObject.parentId.isEmpty() ) {
			mapObject = findObject(mapObject.parentId);
		}
		return isMapObjectDrawable(mapObject) ? mapObject : null;
	}

	private double mapScale() {
		final double scaleX = (mapWidth - 56) / (maxX - minX);
		final double scaleZ = (mapHeight - 64) / (maxZ - minZ);
		return Math.max(0.0001D, Math.min(scaleX, scaleZ) * mapZoom);
	}

	private int toScreenX(final double x) {
		return (int) Math.round(mapX + mapWidth * 0.5D + (x - viewCenterX) * mapScale());
	}

	private int toScreenY(final double z) {
		return (int) Math.round(mapY + mapHeight * 0.5D + (z - viewCenterZ) * mapScale());
	}

	private int toObjectScreenX(final MapObject mapObject) {
		return toScreenX(mapObject.displayMapX);
	}

	private int toObjectScreenY(final MapObject mapObject) {
		if (mapObject.stackCount <= 1) {
			return toScreenY(mapObject.displayMapZ);
		}
		final double centeredIndex = mapObject.stackIndex - (mapObject.stackCount - 1) * 0.5D;
		return toScreenY(mapObject.displayMapZ) + (int) Math.round(centeredIndex * 28.0D);
	}

	private double fromScreenX(final int x) {
		return viewCenterX + (x - (mapX + mapWidth * 0.5D)) / mapScale();
	}

	private double fromScreenY(final int y) {
		return viewCenterZ + (y - (mapY + mapHeight * 0.5D)) / mapScale();
	}

	private boolean isInsideMap(final int x, final int y) {
		return x >= mapX && x <= mapX + mapWidth && y >= mapY && y <= mapY + mapHeight;
	}

	private boolean isInsidePanelList(final int x, final int y) {
		return x >= panelX && x <= panelX + panelWidth && y >= destinationListTop() && y <= panelY + panelHeight - 12;
	}

	private boolean isOverMapButton(final int x, final int y) {
		for (final GuiButton button : buttonList) {
			if ( isMapCanvasButton(button.id)
			  && button.visible
			  && x >= button.x && x <= button.x + button.width
			  && y >= button.y && y <= button.y + button.height ) {
				return true;
			}
		}
		return false;
	}

	private static boolean isMapCanvasButton(final int buttonId) {
		return buttonId == BUTTON_MAP_FIT
		    || buttonId == BUTTON_MAP_LOCAL
		    || buttonId == BUTTON_MAP_HYPERSPACE;
	}

	private static double clamp(final double value, final double min, final double max) {
		return Math.max(min, Math.min(max, value));
	}

	private MapObject findObject(final String id) {
		if (id == null || id.isEmpty()) {
			return null;
		}
		for (final MapObject mapObject : mapObjects) {
			if (mapObject.id.equals(id)) {
				return mapObject;
			}
		}
		return null;
	}

	private MapObject findSpaceRegion() {
		final MapObject selectedSpaceRegion = findObject(mapSpaceId);
		if ( selectedSpaceRegion != null
		  && selectedSpaceRegion.space
		  && !selectedSpaceRegion.hyperspace ) {
			return selectedSpaceRegion;
		}
		final MapObject currentSpaceRegion = findContainingSpaceRegion(findObject(currentId));
		if (currentSpaceRegion != null) {
			return currentSpaceRegion;
		}
		for (final MapObject mapObject : mapObjects) {
			if (mapObject.space && !mapObject.hyperspace) {
				return mapObject;
			}
		}
		return null;
	}

	private MapObject findContainingSpaceRegion(final MapObject mapObject) {
		MapObject current = mapObject;
		while (current != null && !current.space && !current.parentId.isEmpty()) {
			current = findObject(current.parentId);
		}
		return current != null && current.space && !current.hyperspace ? current : null;
	}

	private static void drawLine(final int x1, final int y1, final int x2, final int y2, final int color) {
		final float alpha = (color >> 24 & 255) / 255.0F;
		final float red = (color >> 16 & 255) / 255.0F;
		final float green = (color >> 8 & 255) / 255.0F;
		final float blue = (color & 255) / 255.0F;
		GlStateManager.disableTexture2D();
		GlStateManager.enableBlend();
		GlStateManager.color(red, green, blue, alpha);
		GL11.glLineWidth(2.0F);
		final Tessellator tessellator = Tessellator.getInstance();
		final BufferBuilder bufferBuilder = tessellator.getBuffer();
		bufferBuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
		bufferBuilder.pos(x1, y1, 0).endVertex();
		bufferBuilder.pos(x2, y2, 0).endVertex();
		tessellator.draw();
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
	}

	private static void drawDiamond(final int centerX, final int centerY, final float radius, final int color) {
		final float alpha = (color >> 24 & 255) / 255.0F;
		final float red = (color >> 16 & 255) / 255.0F;
		final float green = (color >> 8 & 255) / 255.0F;
		final float blue = (color & 255) / 255.0F;
		GlStateManager.disableTexture2D();
		GlStateManager.enableBlend();
		GlStateManager.color(red, green, blue, alpha);
		final Tessellator tessellator = Tessellator.getInstance();
		final BufferBuilder bufferBuilder = tessellator.getBuffer();
		GL11.glLineWidth(2.0F);
		bufferBuilder.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
		bufferBuilder.pos(centerX, centerY - radius, 0).endVertex();
		bufferBuilder.pos(centerX + radius, centerY, 0).endVertex();
		bufferBuilder.pos(centerX, centerY + radius, 0).endVertex();
		bufferBuilder.pos(centerX - radius, centerY, 0).endVertex();
		tessellator.draw();
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
	}

	private static void drawCircle(final int centerX, final int centerY, final float radius, final int color, final int segments) {
		final float alpha = (color >> 24 & 255) / 255.0F;
		final float red = (color >> 16 & 255) / 255.0F;
		final float green = (color >> 8 & 255) / 255.0F;
		final float blue = (color & 255) / 255.0F;
		GlStateManager.disableTexture2D();
		GlStateManager.enableBlend();
		GlStateManager.color(red, green, blue, alpha);
		final Tessellator tessellator = Tessellator.getInstance();
		final BufferBuilder bufferBuilder = tessellator.getBuffer();
		bufferBuilder.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
		bufferBuilder.pos(centerX, centerY, 0).endVertex();
		for (int index = 0; index <= segments; index++) {
			final double angle = Math.PI * 2.0D * index / segments;
			bufferBuilder.pos(centerX + Math.cos(angle) * radius, centerY + Math.sin(angle) * radius, 0).endVertex();
		}
		tessellator.draw();
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
	}

	private static void drawCircleOutline(final int centerX, final int centerY, final float radius, final int color, final int segments) {
		final float alpha = (color >> 24 & 255) / 255.0F;
		final float red = (color >> 16 & 255) / 255.0F;
		final float green = (color >> 8 & 255) / 255.0F;
		final float blue = (color & 255) / 255.0F;
		GlStateManager.disableTexture2D();
		GlStateManager.enableBlend();
		GlStateManager.color(red, green, blue, alpha);
		GL11.glLineWidth(2.0F);
		final Tessellator tessellator = Tessellator.getInstance();
		final BufferBuilder bufferBuilder = tessellator.getBuffer();
		bufferBuilder.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
		for (int index = 0; index < segments; index++) {
			final double angle = Math.PI * 2.0D * index / segments;
			bufferBuilder.pos(centerX + Math.cos(angle) * radius, centerY + Math.sin(angle) * radius, 0).endVertex();
		}
		tessellator.draw();
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
	}

	private void drawIconTexture(final String texture, final int centerX, final int centerY, final float radius, final int color, final float alphaScale) {
		if (texture == null || texture.isEmpty()) {
			return;
		}
		try {
			mc.getTextureManager().bindTexture(new ResourceLocation(texture));
		} catch (final RuntimeException exception) {
			return;
		}
		final float alpha = (color >> 24 & 255) / 255.0F * alphaScale;
		final float red = (color >> 16 & 255) / 255.0F;
		final float green = (color >> 8 & 255) / 255.0F;
		final float blue = (color & 255) / 255.0F;
		final float half = Math.max(5.0F, radius);
		GlStateManager.enableTexture2D();
		GlStateManager.enableBlend();
		GlStateManager.color(red, green, blue, alpha);
		final Tessellator tessellator = Tessellator.getInstance();
		final BufferBuilder bufferBuilder = tessellator.getBuffer();
		bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		bufferBuilder.pos(centerX - half, centerY - half, 0).tex(0.0D, 0.0D).endVertex();
		bufferBuilder.pos(centerX - half, centerY + half, 0).tex(0.0D, 1.0D).endVertex();
		bufferBuilder.pos(centerX + half, centerY + half, 0).tex(1.0D, 1.0D).endVertex();
		bufferBuilder.pos(centerX + half, centerY - half, 0).tex(1.0D, 0.0D).endVertex();
		tessellator.draw();
		GlStateManager.disableBlend();
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}

	private static final class MapObject {
		private final String id;
		private final String parentId;
		private final String name;
		private final boolean virtual;
		private final boolean space;
		private final boolean hyperspace;
		private final int mapX;
		private final int mapZ;
		private double displayMapX;
		private double displayMapZ;
		private final int borderRadiusX;
		private final int borderRadiusZ;
		private final float red;
		private final float green;
		private final float blue;
		private final String iconTexture;
		private int stackIndex;
		private int stackCount = 1;

		private MapObject(final NBTTagCompound tagCompound) {
			id = tagCompound.getString("id");
			parentId = tagCompound.getString("parentId");
			name = Commons.updateEscapeCodes(tagCompound.getString("name").isEmpty() ? id : tagCompound.getString("name")).replace('\n', ' ');
			virtual = tagCompound.getBoolean("virtual");
			space = tagCompound.getBoolean("space");
			hyperspace = tagCompound.getBoolean("hyperspace");
			mapX = tagCompound.getString("parentId").isEmpty() ? tagCompound.getInteger("dimensionCenterX") : tagCompound.getInteger("parentCenterX");
			mapZ = tagCompound.getString("parentId").isEmpty() ? tagCompound.getInteger("dimensionCenterZ") : tagCompound.getInteger("parentCenterZ");
			displayMapX = mapX;
			displayMapZ = mapZ;
			borderRadiusX = tagCompound.getInteger("borderRadiusX");
			borderRadiusZ = tagCompound.getInteger("borderRadiusZ");
			red = tagCompound.getFloat("red");
			green = tagCompound.getFloat("green");
			blue = tagCompound.getFloat("blue");
			iconTexture = tagCompound.getString("iconTexture");
		}
	}

	private static final class LabelBounds {
		private final int x;
		private final int y;
		private final int right;
		private final int bottom;

		private LabelBounds(final int x, final int y, final int right, final int bottom) {
			this.x = x;
			this.y = y;
			this.right = right;
			this.bottom = bottom;
		}

		private boolean intersects(final LabelBounds other) {
			return x < other.right + 2
			    && right + 2 > other.x
			    && y < other.bottom + 2
			    && bottom + 2 > other.y;
		}
	}

	private static final class RouteLeg {
		private final EnumShipNavigationLegType type;
		private final boolean preview;
		private final boolean requiresConfirmation;
		private final String warningKey;
		private final int stepDistance;
		private final int remainingDistance;
		private final int effectiveDistance;
		private final int maximumDistance;
		private final int energyRequired;

		private RouteLeg(final NBTTagCompound tagCompound) {
			type = EnumShipNavigationLegType.get(tagCompound.getString("type"));
			preview = tagCompound.getBoolean("preview");
			requiresConfirmation = tagCompound.getBoolean("requiresConfirmation");
			warningKey = tagCompound.getString("warningKey");
			stepDistance = tagCompound.getInteger("stepDistance");
			remainingDistance = tagCompound.getInteger("remainingDistance");
			effectiveDistance = tagCompound.getInteger("effectiveDistance");
			maximumDistance = tagCompound.getInteger("maximumDistance");
			energyRequired = tagCompound.getInteger("energyRequired");
		}

		private String title() {
			return type == null ? "?" : I18n.format(type.getTitleKey());
		}

		private String description() {
			final String base = type == null ? "" : I18n.format(type.getDescriptionKey());
			if (!warningKey.isEmpty() && remainingDistance > stepDistance && stepDistance > 0) {
				return base + " (" + stepDistance + "/" + remainingDistance + ")";
			}
			return base;
		}
	}

	private static final class DestinationEntry {
		private final String id;
		private final String parentId;
		private final String name;
		private final boolean current;
		private final boolean reachable;
		private final boolean space;
		private final boolean hyperspace;
		private final int legs;
		private final long energy;
		private final int eta;
		private final int maxRange;
		private final int distance;
		private final int mapX;
		private final int mapZ;

		private DestinationEntry(final NBTTagCompound tagCompound) {
			id = tagCompound.getString("id");
			parentId = tagCompound.getString("parentId");
			name = Commons.updateEscapeCodes(tagCompound.getString("name").isEmpty() ? id : tagCompound.getString("name")).replace('\n', ' ');
			current = tagCompound.getBoolean("current");
			reachable = tagCompound.getBoolean("reachable");
			final String type = tagCompound.getString("type");
			space = "space".equals(type);
			hyperspace = "hyperspace".equals(type);
			legs = tagCompound.getInteger("legs");
			energy = tagCompound.getLong("energy");
			eta = tagCompound.getInteger("eta");
			maxRange = tagCompound.getInteger("maxRange");
			distance = tagCompound.getInteger("distance");
			mapX = tagCompound.getInteger("mapX");
			mapZ = tagCompound.getInteger("mapZ");
		}
	}

	private static final class DestinationRow {
		private final DestinationEntry entry;
		private final int depth;

		private DestinationRow(final DestinationEntry entry, final int depth) {
			this.entry = entry;
			this.depth = depth;
		}
	}
}
