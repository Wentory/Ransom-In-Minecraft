package com.wentory.ransom_in_minecraft.mixin;

import com.wentory.ransom_in_minecraft.client.MobBanishmentVisuals;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class MobBanishmentRendererMixin {
    @Unique private float ransom$banishmentProgress = -1;

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void ransom$rememberFade(LivingEntity entity, float yaw, float partial, PoseStack pose,
                                     MultiBufferSource buffers, int light, CallbackInfo ci) {
        ransom$banishmentProgress = MobBanishmentVisuals.progress(entity, partial);
    }

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void ransom$blendBody(LivingEntity entity, boolean visible, boolean translucent, boolean glowing,
                                  CallbackInfoReturnable<RenderType> ci) {
        if (ransom$banishmentProgress >= 0 && visible)
            ci.setReturnValue(RenderType.entityTranslucent(
                    ((LivingEntityRenderer) (Object) this).getTextureLocation(entity)));
    }

    @ModifyArg(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"), index = 4)
    private int ransom$fadeBody(int color) {
        if (ransom$banishmentProgress < 0) return color;
        float p = Math.clamp((ransom$banishmentProgress - 0.2F) / 0.8F, 0, 1);
        float opacity = 1 - p * p * (3 - 2 * p);
        int alpha = Math.round((color >>> 24) * opacity);
        return alpha << 24 | color & 0xFFFFFF;
    }
}
