package cr0s.warpdrive.data;

import net.minecraft.util.AxisAlignedBB;
import cr0s.warpdrive.world.JumpgateGenerator;

public class Jumpgate
{
    public String name;
    public int xCoord, yCoord, zCoord;

    public Jumpgate(String name, int x, int y, int z)
    {
        this.name = name;
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
    }
    public Jumpgate(String line)
    {
        String[] params = line.split(":");

        if (params.length < 4)
        {
            return;
        }

        name = params[0];
        xCoord = Integer.parseInt(params[1]);
        yCoord = Integer.parseInt(params[2]);
        zCoord = Integer.parseInt(params[3]);
    }

    public AxisAlignedBB getGateAABB()
    {
        int xmin, ymin, zmin;
        int xmax, ymax, zmax;
        xmin = xCoord - (JumpgateGenerator.GATE_LENGTH_HALF * 2);
        xmax = xCoord + (JumpgateGenerator.GATE_LENGTH_HALF * 2);
        ymin = yCoord - (JumpgateGenerator.GATE_SIZE_HALF);
        ymax = yCoord + (JumpgateGenerator.GATE_SIZE_HALF);
        zmin = zCoord - (JumpgateGenerator.GATE_SIZE_HALF);
        zmax = zCoord + (JumpgateGenerator.GATE_SIZE_HALF);
        return AxisAlignedBB.getBoundingBox(xmin, ymin, zmin, xmax, ymax, zmax);
    }

    @Override
    public String toString()
    {
        return name + ":" + xCoord + ":" + yCoord + ":" + zCoord;
    }

    public String toNiceString()
    {
        return name + " (" + xCoord + "; " + yCoord + "; " + zCoord + ")";
    }
}