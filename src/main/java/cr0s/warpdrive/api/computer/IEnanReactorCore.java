package cr0s.warpdrive.api.computer;

public interface IEnanReactorCore extends IEnergy {
	
	Object[] enable(Object[] arguments);
	
	Object[] release(Object[] arguments);
	
	Object[] releaseRate(Object[] arguments);
	
	Object[] releaseAbove(Object[] arguments);
	
	Object[] state();
}
