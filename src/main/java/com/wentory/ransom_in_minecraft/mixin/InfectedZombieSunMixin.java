package com.wentory.ransom_in_minecraft.mixin;

import com.wentory.ransom_in_minecraft.InfectedZombies;
import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Zombie.class)
public abstract class InfectedZombieSunMixin {
    @Inject(method = "isSunSensitive", at = @At("HEAD"), cancellable = true)
    private void ransom$ignoreSunlight(CallbackInfoReturnable<Boolean> callback) {
        if (InfectedZombies.isInfected((Zombie) (Object) this)) callback.setReturnValue(false);
    }
}
