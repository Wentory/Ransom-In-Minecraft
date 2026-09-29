package com.wentory.ransom_in_minecraft.mixin;

import com.wentory.ransom_in_minecraft.InfectedZombieBodyControl;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class ZombieBodyControlMixin {
    @Inject(method = "createBodyControl", at = @At("HEAD"), cancellable = true)
    private void ransom$zombieBodyControl(CallbackInfoReturnable<BodyRotationControl> callback) {
        if ((Object) this instanceof Zombie zombie) {
            callback.setReturnValue(new InfectedZombieBodyControl(zombie));
        }
    }
}
