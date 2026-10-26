package com.wentory.ransom_in_minecraft.mixin;

import com.wentory.ransom_in_minecraft.client.MobBanishmentVisuals;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import com.wentory.ransom_in_minecraft.client.BanishmentVertexConsumer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class MobBanishmentRendererMixin {
    // Modify the renderer's input, not a model invocation replaced by mods such as Apoli.
    @ModifyVariable(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private MultiBufferSource ransom$banishmentBuffers(MultiBufferSource original,
            LivingEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float progress = MobBanishmentVisuals.progress(entity, partial);
        if (progress < 0) return original;
        return type -> new BanishmentVertexConsumer(original.getBuffer(type), progress);
    }

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void ransom$blendBody(LivingEntity entity, boolean visible, boolean translucent, boolean glowing,
                                  CallbackInfoReturnable<RenderType> ci) {
        if (MobBanishmentVisuals.progress(entity, 0) >= 0 && visible)
            ci.setReturnValue(RenderType.entityTranslucent(
                    ((LivingEntityRenderer) (Object) this).getTextureLocation(entity)));
    }

}
