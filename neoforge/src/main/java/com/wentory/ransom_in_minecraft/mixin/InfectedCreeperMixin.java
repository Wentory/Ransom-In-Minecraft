package com.wentory.ransom_in_minecraft.mixin;

import com.wentory.ransom_in_minecraft.InfectedCreepers;
import com.wentory.ransom_in_minecraft.InfectedZombies;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Creeper.class)
public abstract class InfectedCreeperMixin {
    @Inject(method = "isIgnited", at = @At("HEAD"), cancellable = true)
    private void ransom$customFuse(CallbackInfoReturnable<Boolean> callback) {
        Creeper creeper = (Creeper) (Object) this;
        if (InfectedCreepers.isInfected(creeper) || InfectedZombies.isBeingInfected(creeper)) callback.setReturnValue(false);
    }
    @Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
    private void ransom$customExplosion(CallbackInfo callback) {
        if (InfectedCreepers.isInfected((Creeper) (Object) this)) callback.cancel();
    }
}
