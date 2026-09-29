package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;

/** Shared stationary glitch-particle style: shuffled clusters and solitary lines. */
public final class GlitchParticles {
    private GlitchParticles() {}

    public static void render(PoseStack pose, MultiBufferSource buffers, int seed, float time,
                              int count, float radiusX, float radiusY, float radiusZ, float scale, int alpha) {
        render(pose, buffers, seed, time, count, radiusX, radiusY, radiusZ, scale, alpha, false);
    }

    public static void render(PoseStack pose, MultiBufferSource buffers, int seed, float time,
                              int count, float radiusX, float radiusY, float radiusZ, float scale, int alpha,
                              boolean burst) {
        VertexConsumer consumer = buffers.getBuffer(RansomRenderTypes.boundaryFragments());
        for (int i = 0; i < count; i++) {
            int identity = hash(seed + i * 971);
            float clock = time + Math.floorMod(identity, 12);
            int generation = (int) Math.floor(clock / 12.0F);
            float age = clock - generation * 12.0F;
            if (!burst && age >= 9.0F) continue;
            float fade = burst ? 1.0F : Math.min(1.0F, age / 1.5F) * Math.min(1.0F, (9.0F - age) / 3.0F);
            int opacity = Math.round(alpha * fade);
            int anchor = hash(identity ^ (burst ? 0 : generation * 67));
            float x = signed(anchor) * radiusX;
            float y = signed(hash(anchor + 1)) * radiusY;
            float z = signed(hash(anchor + 2)) * radiusZ;
            if (radiusX > 0 && radiusZ > 0
                    && x * x / (radiusX * radiusX) + z * z / (radiusZ * radiusZ) > 1) continue;
            pose.pushPose();
            pose.translate(x, y, z);
            pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
            boolean single = Math.floorMod(anchor, 10) < 3;
            int pieces = single ? 1 : 3 + Math.floorMod(hash(anchor + 3), 4);
            for (int piece = 0; piece < pieces; piece++) {
                int shape = hash(anchor + piece * 47 + (int) (time / 2.0F) * 131);
                int color = Math.floorMod(hash(anchor + piece * 73), 100);
                int red = color < 2 ? 255 : color < 32 ? 8 : 255;
                int green = color < 2 ? 255 : color < 32 ? 0 : 8;
                int blue = green;
                float left = single ? -0.12F * scale : signed(shape) * 0.14F * scale;
                float bottom = single ? 0 : signed(hash(shape + 1)) * 0.07F * scale;
                float width = (single ? 0.16F + Math.floorMod(shape, 12) * 0.02F
                        : 0.035F + Math.floorMod(shape, 9) * 0.015F) * scale;
                float height = (single ? 0.008F : 0.008F + Math.floorMod(shape >>> 4, 5) * 0.006F) * scale;
                quad(pose, consumer, left, left + width, bottom, bottom + height, red, green, blue, opacity);
            }
            pose.popPose();
        }
    }

    private static void quad(PoseStack pose, VertexConsumer c, float left, float right,
                             float bottom, float top, int red, int green, int blue, int alpha) {
        c.vertex(pose.last().pose(), left, bottom, 0).color(red, green, blue, alpha).endVertex();
        c.vertex(pose.last().pose(), right, bottom, 0).color(red, green, blue, alpha).endVertex();
        c.vertex(pose.last().pose(), right, top, 0).color(red, green, blue, alpha).endVertex();
        c.vertex(pose.last().pose(), left, top, 0).color(red, green, blue, alpha).endVertex();
    }
    private static float signed(int value) { return Math.floorMod(value, 1000) / 499.5F - 1.0F; }
    private static int hash(int value) {
        value ^= value >>> 16; value *= 0x7feb352d; value ^= value >>> 15;
        value *= 0x846ca68b; return value ^ value >>> 16;
    }
}
