package cr0s.warpdrive;

import java.util.Arrays;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.event.FMLConstructionEvent;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class WCDummyContainer extends DummyModContainer {
	public WCDummyContainer() {
		super(new ModMetadata());
		ModMetadata meta = getMetadata();
		meta.modId = "WarpDriveCore";
		meta.name = "WarpDriveCore";
		meta.version = "1.0.0.4";
		meta.credits = "Cr0s";
		meta.authorList = Arrays.asList("cr0s");
		meta.description = "";
		meta.url = "";
		meta.updateUrl = "";
		meta.screenshots = new String[0];
		meta.logoFile = "";
	}

	@Override
	public boolean registerBus(EventBus bus, LoadController controller) {
		bus.register(this);
		return true;
	}

	@Subscribe
	public void modConstruction(FMLConstructionEvent evt) {
	}

	@Subscribe
	public void init(FMLInitializationEvent evt) {
	}

	@Subscribe
	public void preInit(FMLPreInitializationEvent evt) {
	}

	@Subscribe
	public void postInit(FMLPostInitializationEvent evt) {
	}
}
