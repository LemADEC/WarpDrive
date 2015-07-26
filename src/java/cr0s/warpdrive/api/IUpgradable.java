package cr0s.warpdrive.api;

import java.util.Map;

import cr0s.warpdrive.data.EnumUpgradeTypes;

public interface IUpgradable
{
	public boolean takeUpgrade(EnumUpgradeTypes upgradeType,boolean simulate);
	public Map<EnumUpgradeTypes,Integer> getInstalledUpgrades();
}
