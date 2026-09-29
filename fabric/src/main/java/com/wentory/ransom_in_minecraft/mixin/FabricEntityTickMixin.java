package com.wentory.ransom_in_minecraft.mixin;
import com.wentory.ransom_in_minecraft.platform.FabricHooks;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Mob.class)
public abstract class FabricEntityTickMixin {
    @Inject(method="tick",at=@At("TAIL"))
    private void ransom$tick(CallbackInfo ci){FabricHooks.entityTick((Mob)(Object)this);}
}
