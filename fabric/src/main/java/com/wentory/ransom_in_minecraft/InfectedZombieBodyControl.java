package com.wentory.ransom_in_minecraft;

import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.monster.zombie.Zombie;

public final class InfectedZombieBodyControl extends BodyRotationControl {
    private final Zombie zombie;

    public InfectedZombieBodyControl(Zombie zombie) {
        super(zombie);
        this.zombie = zombie;
    }

    @Override
    public void clientTick() {
        if (!InfectedZombies.isInfected(zombie)) {
            super.clientTick();
            return;
        }
        double dx = zombie.getX() - zombie.xo;
        double dz = zombie.getZ() - zombie.zo;
        if (dx * dx + dz * dz > 2.5E-7D) {
            zombie.yBodyRot = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        }
    }
}
