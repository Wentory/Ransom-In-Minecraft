package com.wentory.ransom_in_minecraft.mixin;

import com.wentory.ransom_in_minecraft.InfectedZombies;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntity.class, Drowned.class})
public abstract class HijackedWaterTravelMixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void ransom$waterMovement(Vec3 input, CallbackInfo ci) {
        if (!((Object) this instanceof Zombie zombie) || !InfectedZombies.isInfected(zombie)
                || !zombie.isInWater() || !zombie.isControlledByLocalInstance()) return;
        ci.cancel();
        if (zombie instanceof Drowned) {
            LivingEntity target = zombie.getTarget();
            Vec3 motion = Vec3.ZERO;
            if (!InfectedZombies.isStopped(zombie) && !InfectedZombies.isBeingInfected(zombie)
                    && !InfectedZombies.isInfecting(zombie)
                    && target != null && target.isAlive()) {
                Vec3 offset = target.getBoundingBox().getCenter().subtract(zombie.getBoundingBox().getCenter());
                motion = offset.normalize().scale(Math.min(0.45D, offset.length()));
            }
            zombie.setDeltaMovement(motion);
            zombie.move(MoverType.SELF, motion);
        } else {
            // Ground-like horizontal acceleration/drag, with gravity retained so zombies sink.
            zombie.moveRelative(zombie.getSpeed(), input);
            zombie.move(MoverType.SELF, zombie.getDeltaMovement());
            Vec3 velocity = zombie.getDeltaMovement();
            zombie.setDeltaMovement(velocity.x * 0.546D, (velocity.y - zombie.getGravity()) * 0.98D,
                    velocity.z * 0.546D);
        }
    }
}
