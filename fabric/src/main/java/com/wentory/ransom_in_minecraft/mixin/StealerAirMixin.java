package com.wentory.ransom_in_minecraft.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class StealerAirMixin {
    @Unique
    private boolean ransom$preservesAir() {
        LivingEntity entity = (LivingEntity) (Object) this;
        return entity instanceof ServerPlayer player
                ? com.wentory.ransom_in_minecraft.network.RansomChestGame.preservesAir(player)
                : entity.level().isClientSide() && com.wentory.ransom_in_minecraft.client.StealerBreathing.preservesAir(entity);
    }

    @Inject(method = {"decreaseAirSupply", "increaseAirSupply"}, at = @At("HEAD"), cancellable = true)
    private void ransom$freezeAir(int air, CallbackInfoReturnable<Integer> cir) {
        if (ransom$preservesAir()) cir.setReturnValue(air);
    }

    @Inject(method = "canBreatheUnderwater", at = @At("HEAD"), cancellable = true)
    private void ransom$breatheDuringStealer(CallbackInfoReturnable<Boolean> cir) {
        if (ransom$preservesAir()) cir.setReturnValue(true);
    }
}
