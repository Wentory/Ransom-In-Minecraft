package com.wentory.ransom_in_minecraft.mixin;

import com.wentory.ransom_in_minecraft.FullHijack;
import com.wentory.ransom_in_minecraft.InfectedCreepers;
import com.wentory.ransom_in_minecraft.InfectedZombies;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Mob.class)
public abstract class HijackedTargetMixin {
    @ModifyVariable(method = "setTarget", at = @At("HEAD"), argsOnly = true)
    private LivingEntity ransom$ignoreCapturedPlayer(LivingEntity target) {
        if (!(target instanceof Player player) || !FullHijack.active(player)) return target;
        Object mob = this;
        return mob instanceof Zombie zombie && InfectedZombies.isInfected(zombie)
                || mob instanceof Creeper creeper && InfectedCreepers.isInfected(creeper) ? null : target;
    }
}
