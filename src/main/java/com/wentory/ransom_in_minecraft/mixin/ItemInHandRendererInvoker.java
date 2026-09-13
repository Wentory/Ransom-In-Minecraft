package com.wentory.ransom_in_minecraft.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererInvoker {
    @Invoker("renderPlayerArm")
    void ransom$renderPlayerArm(PoseStack poseStack, MultiBufferSource buffers, int packedLight,
                                float equipProgress, float swingProgress, HumanoidArm arm);
}
