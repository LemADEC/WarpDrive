package cr0s.warpdrive.api.computer;

public interface IShipController extends IMultiBlockCoreOrController {
	
	Object[] getOrientation();
	
	Object[] isInSpace();
	
	Object[] isInHyperspace();
	
	Object[] dim_positive(Object[] arguments);
	
	Object[] dim_negative(Object[] arguments);
	
	Object[] command(Object[] arguments);
	
	Object[] getShipSize();
	
	Object[] movement(Object[] arguments);
	
	Object[] getMaxJumpDistance();
	
	Object[] rotationSteps(Object[] arguments);
	
	Object[] state();
	
	Object[] targetName(Object[] arguments);
}
