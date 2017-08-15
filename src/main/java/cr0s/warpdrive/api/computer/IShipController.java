package cr0s.warpdrive.api.computer;

public interface IShipController extends IInterfaced {
	
	Object[] isAssemblyValid();
	
	Object[] getOrientation();
	
	Object[] isInSpace();
	
	Object[] isInHyperspace();
	
	Object[] shipName(Object[] arguments);
	
	Object[] dim_positive(Object[] arguments);
	
	Object[] dim_negative(Object[] arguments);
	
	Object[] getAttachedPlayers();
	
	Object[] energy();
	
	Object[] command(Object[] arguments);
	
	Object[] enable(Object[] arguments);
	
	Object[] getShipSize();
	
	Object[] movement(Object[] arguments);
	
	Object[] getMaxJumpDistance();
	
	Object[] rotationSteps(Object[] arguments);
	
	Object[] targetName(Object[] arguments);
	
	Object[] getEnergyRequired();
}
