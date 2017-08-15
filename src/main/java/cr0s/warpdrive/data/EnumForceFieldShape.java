package cr0s.warpdrive.data;


import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IForceFieldShape;

import java.util.HashMap;
import java.util.Map;

public enum EnumForceFieldShape implements IForceFieldShape {
	NONE               ("none"),
	SPHERE             ("sphere"),
	CYLINDER_H         ("cylinder_h"),
	CYLINDER_V         ("cylinder_v"),
	CUBE               ("cube"),
	PLANE              ("plane"),
	TUBE               ("tube"),
	TUNNEL             ("tunnel");
	
	public final String unlocalizedName;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumForceFieldShape> ID_MAP = new HashMap<>();
	
	static {
		length = EnumForceFieldShape.values().length;
		for (EnumForceFieldShape enumForceFieldShape : values()) {
			ID_MAP.put(enumForceFieldShape.ordinal(), enumForceFieldShape);
		}
	}
	
	EnumForceFieldShape(String unlocalizedName) {
		this.unlocalizedName = unlocalizedName;
	}
	
	public static EnumForceFieldShape get(final int damage) {
		return ID_MAP.get(damage);
	}
	
	@Override
	public Map<VectorI, Boolean> getVertexes(ForceFieldSetup forceFieldSetup) {
		final VectorI vScale = forceFieldSetup.vMax.clone().translateBack(forceFieldSetup.vMin);
		final boolean isFusionOrInverted = forceFieldSetup.hasFusion || forceFieldSetup.isInverted;
		final int sizeEstimation;
		if (!isFusionOrInverted) {// surface only
			// plane surface    is r^2
			// sphere surface   is 4*PI*r^2
			// cylinder surface is 2*PI*r^2 + 2*PI*r*h = 2*PI*r^2 * 1.5
			// cube surface     is 4*6*r^2
			final int maxRadius = 1 + (int) Math.ceil(Math.max(vScale.x, Math.max(vScale.y, vScale.z)) / 2.0F);
			switch(this) {
			case SPHERE:
				sizeEstimation = (int) Math.ceil(4 * Math.PI * maxRadius * maxRadius);
				break;
			
			case CYLINDER_H:
			case CYLINDER_V:
			case TUBE:
				sizeEstimation = (int) Math.ceil(4 * Math.PI * maxRadius * maxRadius * 1.5F);
				break;
			
			case CUBE:
			case TUNNEL:
				sizeEstimation = 4 * 6 * maxRadius * maxRadius;
				break;
			
			case PLANE:
				sizeEstimation = maxRadius * maxRadius;
				break;
			
			default:
				assert(false);
				sizeEstimation = 8;
				break;
			}
		} else {
			sizeEstimation = vScale.x * vScale.y * vScale.z;
		}
		final Map<VectorI, Boolean> mapVertexes = new HashMap<>(sizeEstimation);
		
		float radius;
		float halfThickness = forceFieldSetup.thickness / 2.0F;
		float radiusInterior2;
		float radiusPerimeter2;
		VectorI vCenter;
		boolean isPerimeter;
		switch(this) {
		case SPHERE:
			radius = forceFieldSetup.vMax.y;
			radiusInterior2 = (radius - halfThickness) * (radius - halfThickness);
			radiusPerimeter2 = (radius + halfThickness) * (radius + halfThickness);
			vCenter = new VectorI(0, 0, 0);
			for (int y = forceFieldSetup.vMin.y; y <= forceFieldSetup.vMax.y; y++) {
				final int y2 = (y - vCenter.y) * (y - vCenter.y);
				for (int x = forceFieldSetup.vMin.x; x <= forceFieldSetup.vMax.x; x++) {
					final int x2 = (x - vCenter.x) * (x - vCenter.x);
					for (int z = forceFieldSetup.vMin.z; z <= forceFieldSetup.vMax.z; z++) {
						final int z2 = (z - vCenter.z) * (z - vCenter.z);
						if (x2 + y2 + z2 <= radiusPerimeter2) {
							isPerimeter = x2 + y2 + z2 >= radiusInterior2;
							if (isPerimeter || isFusionOrInverted) {
								mapVertexes.put(new VectorI(x, y, z), isPerimeter);
							}
						}
					}
				}
			}
			break;
		
		case CYLINDER_H:
			radius = (forceFieldSetup.vMax.y + forceFieldSetup.vMax.z) / 2.0F;
			radiusInterior2 = (radius - halfThickness) * (radius - halfThickness);
			radiusPerimeter2 = (radius + halfThickness) * (radius + halfThickness);
			vCenter = new VectorI(0, 0, 0);
			for (int y = forceFieldSetup.vMin.y; y <= forceFieldSetup.vMax.y; y++) {
				int y2 = (y - vCenter.y) * (y - vCenter.y);
				for (int z = forceFieldSetup.vMin.z; z <= forceFieldSetup.vMax.z; z++) {
					int z2 = (z - vCenter.z) * (z - vCenter.z);
					if (y2 + z2 <= radiusPerimeter2) {
						isPerimeter = y2 + z2 >= radiusInterior2;
						if (isPerimeter || isFusionOrInverted) {
							for (int x = forceFieldSetup.vMin.x; x <= forceFieldSetup.vMax.x; x++) {
								mapVertexes.put(new VectorI(x, y, z), isPerimeter);
							}
						}
					}
				}
			}
			break;
		
		case CYLINDER_V:
			radius = (forceFieldSetup.vMax.x + forceFieldSetup.vMax.y) / 2.0F;
			radiusInterior2 = (radius - halfThickness) * (radius - halfThickness);
			radiusPerimeter2 = (radius + halfThickness) * (radius + halfThickness);
			vCenter = new VectorI(0, 0, 0);
			for (int x = forceFieldSetup.vMin.x; x <= forceFieldSetup.vMax.x; x++) {
				int x2 = (x - vCenter.x) * (x - vCenter.x);
				for (int y = forceFieldSetup.vMin.y; y <= forceFieldSetup.vMax.y; y++) {
					int y2 = (y - vCenter.y) * (y - vCenter.y);
					if (x2 + y2 <= radiusPerimeter2) {
						isPerimeter = x2 + y2 >= radiusInterior2;
						if (isPerimeter || isFusionOrInverted) {
							for (int z = forceFieldSetup.vMin.z; z <= forceFieldSetup.vMax.z; z++) {
								mapVertexes.put(new VectorI(x, y, z), isPerimeter);
							}
						}
					}
				}
			}
			break;
		
		case TUBE:
			radius =(forceFieldSetup.vMax.x + forceFieldSetup.vMax.z) / 2.0F;
			radiusInterior2 = (radius - halfThickness) * (radius - halfThickness);
			radiusPerimeter2 = (radius + halfThickness) * (radius + halfThickness);
			vCenter = new VectorI(0, 0, 0);
			for (int x = forceFieldSetup.vMin.x; x <= forceFieldSetup.vMax.x; x++) {
				int x2 = (x - vCenter.x) * (x - vCenter.x);
				for (int z = forceFieldSetup.vMin.z; z <= forceFieldSetup.vMax.z; z++) {
					int z2 = (z - vCenter.z) * (z - vCenter.z);
					if (x2 + z2 <= radiusPerimeter2) {
						isPerimeter = x2 + z2 >= radiusInterior2;
						if (isPerimeter || isFusionOrInverted) {
							for (int y = forceFieldSetup.vMin.y; y <= forceFieldSetup.vMax.y; y++) {
								mapVertexes.put(new VectorI(x, y, z), isPerimeter);
							}
						}
					}
				}
			}
			break;
		
		case CUBE:
			for (int y = forceFieldSetup.vMin.y; y <= forceFieldSetup.vMax.y; y++) {
				boolean yFace = Math.abs(y - forceFieldSetup.vMin.y) <= halfThickness
				             || Math.abs(y - forceFieldSetup.vMax.y) <= halfThickness;
				for (int x = forceFieldSetup.vMin.x; x <= forceFieldSetup.vMax.x; x++) {
					boolean xFace = Math.abs(x - forceFieldSetup.vMin.x) <= halfThickness
					             || Math.abs(x - forceFieldSetup.vMax.x) <= halfThickness;
					for (int z = forceFieldSetup.vMin.z; z <= forceFieldSetup.vMax.z; z++) {
						boolean zFace = Math.abs(z - forceFieldSetup.vMin.z) <= halfThickness
						             || Math.abs(z - forceFieldSetup.vMax.z) <= halfThickness;
						isPerimeter = xFace || yFace || zFace;
						if (isPerimeter || isFusionOrInverted) {
							mapVertexes.put(new VectorI(x, y, z), isPerimeter);
						}
					}
				}
			}
			break;
		
		case PLANE:
			for (int y = forceFieldSetup.vMin.y; y <= forceFieldSetup.vMax.y; y++) {
				isPerimeter = Math.abs(y - forceFieldSetup.vMin.y) <= halfThickness
				           || Math.abs(y - forceFieldSetup.vMax.y) <= halfThickness;
				if (isPerimeter || isFusionOrInverted) {
					for (int x = forceFieldSetup.vMin.x; x <= forceFieldSetup.vMax.x; x++) {
						for (int z = forceFieldSetup.vMin.z; z <= forceFieldSetup.vMax.z; z++) {
							mapVertexes.put(new VectorI(x, y, z), isPerimeter);
						}
					}
				}
			}
			break;
		
		case TUNNEL:
			for (int y = forceFieldSetup.vMin.y; y <= forceFieldSetup.vMax.y; y++) {
				for (int x = forceFieldSetup.vMin.x; x <= forceFieldSetup.vMax.x; x++) {
					boolean xFace = Math.abs(x - forceFieldSetup.vMin.x) <= halfThickness
					             || Math.abs(x - forceFieldSetup.vMax.x) <= halfThickness;
					for (int z = forceFieldSetup.vMin.z; z <= forceFieldSetup.vMax.z; z++) {
						isPerimeter = xFace
						           || Math.abs(z - forceFieldSetup.vMin.z) <= halfThickness
						           || Math.abs(z - forceFieldSetup.vMax.z) <= halfThickness;
						if (isPerimeter || isFusionOrInverted) {
							mapVertexes.put(new VectorI(x, y, z), isPerimeter);
						}
					}
				}
			}
			break;
		
		default:
			break;
			
		}
		
		if (mapVertexes.size() > sizeEstimation) {
			WarpDrive.logger.warn(String.format("Underestimated memory location lag %d > %d for shape %s with size %s, isFusionOrInverted %s. Please report this to the mod author",
			                                    mapVertexes.size(), sizeEstimation,
			                                    unlocalizedName,
			                                    vScale,
			                                    isFusionOrInverted));
		}
		return mapVertexes;
	}
}
