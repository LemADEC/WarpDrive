package cr0s.warpdrive.api.computer;

public interface ILift extends IEnergy {
	
	Object[] enable(Object[] arguments);
	
	Object[] mode(Object[] arguments);
	
	Object[] state();
}
