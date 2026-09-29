package com.wentory.ransom_in_minecraft.mixin;

import com.wentory.ransom_in_minecraft.InfectedZombies;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class InfectedZombieSunMixin {
    @Inject(method = "burnUndead", at = @At("HEAD"), cancellable = true)
    private void ransom$ignoreSunlight(CallbackInfo callback) {
        if ((Object) this instanceof Zombie zombie && InfectedZombies.isInfected(zombie)) callback.cancel();
    }
}
