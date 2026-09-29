package com.wentory.ransom_in_minecraft.mixin;

import com.wentory.ransom_in_minecraft.InfectedCreepers;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class InfectedCreeperDimensionsMixin {
    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void ransom$faceCube(Pose pose, CallbackInfoReturnable<EntityDimensions> ci) {
        if ((Object) this instanceof Creeper creeper && InfectedCreepers.phase(creeper) == 2) {
            ci.setReturnValue(EntityDimensions.scalable(1.0F, 1.0F));
        }
    }
}
