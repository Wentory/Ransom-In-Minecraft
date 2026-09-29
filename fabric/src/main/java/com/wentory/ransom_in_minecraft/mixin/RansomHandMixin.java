package com.wentory.ransom_in_minecraft.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wentory.ransom_in_minecraft.client.RansomEncounter;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class RansomHandMixin {
    @Shadow private void renderPlayerArm(PoseStack poseStack, SubmitNodeCollector collector, int light,
                                         float equipProgress, float swingProgress, HumanoidArm arm) {}

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void ransom$hideHeldItem(AbstractClientPlayer player, float partialTick, float pitch,
                                     InteractionHand hand, float swingProgress, ItemStack stack,
                                     float equipProgress, PoseStack poseStack, SubmitNodeCollector collector,
                                     int packedLight, CallbackInfo ci) {
        if (!RansomEncounter.handsLocked() || stack.isEmpty()) return;
        ci.cancel();
        if (hand == InteractionHand.MAIN_HAND)
            renderPlayerArm(poseStack, collector, packedLight, equipProgress, swingProgress, player.getMainArm());
    }
}