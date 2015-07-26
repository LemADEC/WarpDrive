package cr0s.warpdrive.machines;

import net.minecraft.tileentity.TileEntity;
import cr0s.warpdrive.data.Vector3;

public abstract class WarpTE extends TileEntity
{
 	public static final Vector3[] adjacentSideOffsets = new Vector3[6];
 	static
 	{
 		adjacentSideOffsets[0] = new Vector3( 0, 0, 1);
 		adjacentSideOffsets[1] = new Vector3( 0, 0,-1);
 		adjacentSideOffsets[2] = new Vector3( 0, 1, 0);
 		adjacentSideOffsets[3] = new Vector3( 0,-1, 0);
 		adjacentSideOffsets[4] = new Vector3( 1, 0, 0);
 		adjacentSideOffsets[5] = new Vector3(-1, 0, 0);
 	}
 	
	protected static int toInt(double d) {
		return (int) Math.round(d);
	}
	
	protected static int toInt(Object o) {
		return toInt(toDouble(o));
	}
	
	protected static double toDouble(Object o) {
		return Double.parseDouble(o.toString());
	}

	protected static float toFloat(Object o) {
		return Float.parseFloat(o.toString());
	}
	
	protected static boolean toBool(Object o) {
		if (o == null) {
			 return false;
		}
		if (o instanceof Boolean) {
			 return ((Boolean) o);
		}
		if (o.toString() == "true" || o.toString() == "1.0" || o.toString() == "1" || o.toString() == "y" || o.toString() == "yes") {
			return true;
		}
		return false;
	}
	
	protected static int clamp(int a, int min, int max) {
		return Math.min(max, Math.max(a, min));
	}
	
	protected static double clamp(double a, double min, double max) {
		return Math.min(max, Math.max(a, min));
	}
}
