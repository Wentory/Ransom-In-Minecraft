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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class MobBanishmentRendererMixin {
    @Unique private float ransom$progress = -1;
    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void ransom$rememberFade(LivingEntity entity, float yaw, float partial, PoseStack pose,
            MultiBufferSource buffers, int light, CallbackInfo ci) {
        ransom$progress = MobBanishmentVisuals.progress(entity, partial);
    }
    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void ransom$blendBody(LivingEntity entity, boolean visible, boolean translucent, boolean glowing,
            CallbackInfoReturnable<RenderType> ci) {
        if (ransom$progress >= 0 && visible)
            ci.setReturnValue(RenderType.entityTranslucent(((LivingEntityRenderer) (Object) this).getTextureLocation(entity)));
    }
    @org.spongepowered.asm.mixin.injection.ModifyArg(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"), index = 2)
    private int ransom$flashLight(int light) {
        return ransom$progress >= 0 && ransom$progress < .2F ? 0xF000F0 : light;
    }
    @org.spongepowered.asm.mixin.injection.ModifyArg(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"), index = 4)
    private float ransom$flashRed(float original) {
        if (ransom$progress < 0 || ransom$progress >= .2F) return original;
        float flash = net.minecraft.util.Mth.clamp((.2F - ransom$progress) / .1F, 0, 1);
        return net.minecraft.util.Mth.lerp(flash, original, 85 / 255F);
    }
    @org.spongepowered.asm.mixin.injection.ModifyArg(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"), index = 5)
    private float ransom$flashGreen(float original) {
        if (ransom$progress < 0 || ransom$progress >= .2F) return original;
        float flash = net.minecraft.util.Mth.clamp((.2F - ransom$progress) / .1F, 0, 1);
        return net.minecraft.util.Mth.lerp(flash, original, 221 / 255F);
    }
    @org.spongepowered.asm.mixin.injection.ModifyArg(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"), index = 6)
    private float ransom$flashBlue(float original) {
        if (ransom$progress < 0 || ransom$progress >= .2F) return original;
        float flash = net.minecraft.util.Mth.clamp((.2F - ransom$progress) / .1F, 0, 1);
        return net.minecraft.util.Mth.lerp(flash, original, 1F);
    }
    @org.spongepowered.asm.mixin.injection.ModifyArg(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"), index = 7)
    private float ransom$fadeBody(float alpha) {
        if (ransom$progress < 0) return alpha;
        float p = net.minecraft.util.Mth.clamp((ransom$progress - .2F) / .8F, 0, 1);
        return alpha * (1 - p * p * (3 - 2 * p));
    }
}
