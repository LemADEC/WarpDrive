package cr0s.warpdrive;

import java.io.File;
import java.util.Map;

import cpw.mods.fml.relauncher.IFMLCallHook;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

public class WCFMLLoadingPlugin implements IFMLLoadingPlugin, IFMLCallHook {
	public static File location;

	@Override
	public String[] getASMTransformerClass() {
		return new String[] { WCClassTransformer.class.getName() };
	}

	@Override
	public String getModContainerClass() {
		return WCDummyContainer.class.getName();
	}

	@Override
	public String getSetupClass() {
		return getClass().getName();
	}

	@Override
	public void injectData(Map<String, Object> data) {
		location = (File) data.get("coremodLocation");
		System.out.println("*** Transformer jar location location.getName: " + location.getName());
	}

	@Override
	public Void call() throws Exception {
		System.out.println("[WCD] call()");
		return null;
	}

	@Override
	public String getAccessTransformerClass() {
		// TODO Auto-generated method stub
		return null;
	}
}