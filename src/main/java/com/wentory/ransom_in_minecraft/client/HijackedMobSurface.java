package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Surface passes reuse the animated parent model without changing any positions or scales. */
final class HijackedMobSurface {
    private static final ResourceLocation GLITCH = ResourceLocation.fromNamespaceAndPath(
            RansomInMinecraft.MODID, "textures/glitch.png");

    private HijackedMobSurface() {}

    static <T extends LivingEntity> void render(EntityModel<T> model, ResourceLocation skin,
                                               PoseStack pose, MultiBufferSource buffers,
                                               int light, int tick, int tint) {
        render(model, skin, pose, buffers, light, tick, tint, 0xCCFFFFFF);
    }

    static <T extends LivingEntity> void render(EntityModel<T> model, ResourceLocation skin,
                                               PoseStack pose, MultiBufferSource buffers,
                                               int light, int tick, int tint, int noiseColor) {
        model.renderToBuffer(pose, buffers.getBuffer(RansomRenderTypes.hijackedSurface(skin)),
                light, OverlayTexture.NO_OVERLAY, tint);
        var animated = new AnimatedSheetVertexConsumer(
                buffers.getBuffer(RansomRenderTypes.hijackedSurface(GLITCH)), tick / 2, 6);
        model.renderToBuffer(pose, animated, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, noiseColor);
    }
}
