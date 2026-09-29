package com.wentory.ransom_in_minecraft.mixin;

import com.wentory.ransom_in_minecraft.InfectedZombies;
import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.Drowned;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class InfectedZombieSoundMixin {
    @Inject(method = "playSound(Lnet/minecraft/sounds/SoundEvent;FF)V", at = @At("HEAD"), cancellable = true)
    private void ransom$zombieSound(SoundEvent sound, float volume, float pitch, CallbackInfo ci) {
        if (!((Object) this instanceof Zombie zombie) || !InfectedZombies.isInfected(zombie)) return;
        if (zombie instanceof Drowned) {
            SoundEvent replacement = sound == SoundEvents.DROWNED_AMBIENT ? RansomInMinecraft.DROWNED_IDLE.get()
                    : sound == SoundEvents.DROWNED_AMBIENT_WATER ? RansomInMinecraft.DROWNED_IDLE_WATER.get()
                    : sound == SoundEvents.DROWNED_HURT ? RansomInMinecraft.DROWNED_HURT.get()
                    : sound == SoundEvents.DROWNED_HURT_WATER ? RansomInMinecraft.DROWNED_HURT_WATER.get()
                    : sound == SoundEvents.DROWNED_DEATH ? RansomInMinecraft.DROWNED_DEATH.get()
                    : sound == SoundEvents.DROWNED_DEATH_WATER ? RansomInMinecraft.DROWNED_DEATH_WATER.get() : null;
            if (replacement != null) {
                ci.cancel();
                zombie.playSound(replacement, volume, InfectedZombies.soundPitch(zombie));
            }
            return;
        }
        if (sound == SoundEvents.ZOMBIE_AMBIENT || sound == SoundEvents.ZOMBIE_STEP) {
            ci.cancel();
            return;
        }
        SoundEvent replacement = sound == SoundEvents.ZOMBIE_HURT ? RansomInMinecraft.ZOMBIE_HURT.get()
                : sound == SoundEvents.ZOMBIE_DEATH ? RansomInMinecraft.ZOMBIE_DEATH.get() : null;
        if (replacement == null) return;
        ci.cancel();
        zombie.playSound(replacement, volume, InfectedZombies.soundPitch(zombie));
    }
}
