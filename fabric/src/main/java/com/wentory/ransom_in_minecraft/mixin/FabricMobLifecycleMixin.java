package com.wentory.ransom_in_minecraft.mixin;
import com.wentory.ransom_in_minecraft.platform.FabricHooks;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class FabricMobLifecycleMixin {
    @Inject(method="finalizeSpawn",at=@At("RETURN"))
    private void ransom$spawn(ServerLevelAccessor level,DifficultyInstance difficulty,EntitySpawnReason reason,
                              SpawnGroupData data,CallbackInfoReturnable<SpawnGroupData> ci) {
        FabricHooks.naturalSpawn((Mob)(Object)this,reason);
    }
}
