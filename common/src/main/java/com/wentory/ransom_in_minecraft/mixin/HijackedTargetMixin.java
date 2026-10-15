package com.wentory.ransom_in_minecraft.mixin;
import com.wentory.ransom_in_minecraft.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Mob.class)
public abstract class HijackedTargetMixin {
    @ModifyVariable(method="setTarget",at=@At("HEAD"),argsOnly=true)
    private LivingEntity ransom$ignoreCaptured(LivingEntity target){
        if(!(target instanceof Player player)||!FullHijack.active(player))return target;
        Object mob=this;
        return mob instanceof Zombie zombie&&InfectedZombies.isInfected(zombie)
                ||mob instanceof Creeper creeper&&InfectedCreepers.isInfected(creeper)?null:target;
    }
}
