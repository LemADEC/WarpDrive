/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cr0s.warpdrive.data;

import net.minecraft.entity.Entity;

public class MovingEntity
{
    public double oldX;
    public double oldY;
    public double oldZ;

    public Entity entity;

    public MovingEntity(Entity e)
    {
        entity = e;
        oldX = e.posX;
        oldY = e.posY;
        oldZ = e.posZ;
    }
}
