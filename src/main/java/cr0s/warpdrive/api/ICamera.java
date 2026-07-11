package cr0s.warpdrive.api;

public interface ICamera extends IVideoChannel {

	String CAMERA_YAW_TAG = "cameraYaw";
	String CAMERA_PITCH_TAG = "cameraPitch";

	boolean hasCameraOrientation();

	float getCameraYaw();

	float getCameraPitch();

	void setCameraOrientation(final float yaw, final float pitch);
}
