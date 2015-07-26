package cr0s.warpdrive.render;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import org.lwjgl.input.Keyboard;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.machines.BlockMonitor;

public class ClientCameraUtils {
    public static EntityPlayer entityPlayer;
    public static int dimensionId = -666;
    public static int check1_x, check1_y, check1_z;
	public static Block check1_blockId, check2_blockId;
    public static int check2_x, check2_y, check2_z;

    public static void setupViewpoint(EntityPlayer parPlayerEntity, EntityCamera entityCamera, int x1, int y1, int z1, Block block1, int x2, int y2, int z2, Block block2) {
        Minecraft mc = Minecraft.getMinecraft();

        if (parPlayerEntity == null) {
        	WarpDrive.print("[WarpDrive] setupViewpoint with null player => denied");
            return;
        }
        if (entityCamera == null) {
        	WarpDrive.print("[WarpDrive] setupViewpoint with null camera => denied");
            return;
        }

        entityPlayer = parPlayerEntity;
        dimensionId = entityCamera.worldObj.provider.dimensionId;
        check1_x = x1;
        check1_y = y1;
        check1_z = z1;
        check1_blockId = block1;
        check2_x = x2;
        check2_y = y2;
        check2_z = z2;
        check2_blockId = block2;
        
        WarpDrive.debugPrint("Setting viewpoint: " + entityCamera.toString());
        mc.renderViewEntity = entityCamera;
        mc.gameSettings.thirdPersonView = 0;
        WarpDrive.instance.isOverlayEnabled = true;
        
        Keyboard.enableRepeatEvents(true);
    }

    public static void resetViewpoint() {
        Minecraft mc = Minecraft.getMinecraft();
    	if (entityPlayer != null) {
    		mc.renderViewEntity = entityPlayer;
    		entityPlayer = null;
            WarpDrive.debugPrint("Resetting viewpoint");
    	} else {
    		WarpDrive.print("[WarpDrive] reseting viewpoint with invalid player entity ?!?");
    	}
        
    	Keyboard.enableRepeatEvents(false);

        WarpDrive.instance.isOverlayEnabled = false;
        mc.gameSettings.thirdPersonView = 0;
        // TODO: Cannot find 1.7 equivalent
        /*
        mc.gameSettings.setOptionFloatValue(EnumOptions.FOV, WarpDrive.normalFOV);
        mc.gameSettings.setOptionFloatValue(EnumOptions.SENSITIVITY, WarpDrive.normalSensitivity);
        */


        entityPlayer = null;
        dimensionId = -666;
    }
    
    public static boolean isValidContext(World worldObj) {
    	if (worldObj == null || worldObj.provider.dimensionId != dimensionId) {
    		return false;
    	}
    	if (!worldObj.getBlock(check1_x, check1_y, check1_z).isAssociatedBlock(check1_blockId)) {
    		WarpDrive.print("[WarpDrive] checking viewpoint, found invalid block1 at (" + check1_x + ", " + check1_y + ", " + check1_z + ")");
    		return false;
    	}
    	if (!worldObj.getBlock(check2_x, check2_y, check2_z).isAssociatedBlock(check2_blockId)) {
    		WarpDrive.print("[WarpDrive] checking viewpoint, found invalid block2 at (" + check2_x + ", " + check2_y + ", " + check2_z + ")");
    		return false;
    	}
    	return true;
    }
}
