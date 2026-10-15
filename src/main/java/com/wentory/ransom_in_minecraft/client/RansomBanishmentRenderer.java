package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import com.wentory.ransom_in_minecraft.CrucifixBanishmentTiming;
import com.wentory.ransom_in_minecraft.network.CrucifixCirclePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** World-only banishment animation. Timing comes from the server, with no sound playback. */
final class RansomBanishmentRenderer {
    private static final ResourceLocation FACE_1 = texture("jumpscare1.png");
    private static final ResourceLocation FACE_2 = texture("jumpscare2.png");
    private static final ResourceLocation STOP = texture("block.png");
    private static final ResourceLocation CHAIN = texture("item/crucifix/chain.png");
    private static final float CHAIN_HALF_WIDTH = 0.09F;
    private static final float CHAIN_TEXTURE_LENGTH = CHAIN_HALF_WIDTH * 2 * 48 / 3;
    private RansomBanishmentRenderer() {}
    static void render(RenderLevelStageEvent event, CrucifixCirclePayload circle, float age) {
        if (age < CrucifixBanishmentTiming.CAPTURE_END) {
            renderFace(event, circle, age);
            // The face writes its depth first, so chains behind it are occluded.
            renderChains(event, circle, age);
        }
        renderProjection(event, circle, age);
    }

    private static void renderFace(RenderLevelStageEvent event, CrucifixCirclePayload circle, float age) {
        var minecraft = Minecraft.getInstance();
        var camera = event.getCamera().getPosition();
        var pose = event.getPoseStack();
        var buffers = minecraft.renderBuffers().bufferSource();
        float rise = age < 60 ? 0 : age < 70 ? smooth((age - 60) / 10) * 0.6F : 0.6F;
        float sink = age < 70 ? 0 : smooth((age - 70) / 30);
        int glitchTick = (int) age % 36;
        float widthScale = 1, heightScale = 1;
        if (glitchTick == 8) {
            widthScale = 1.55F;
            heightScale = 0.55F;
        } else if (glitchTick == 25) {
            widthScale = 0.65F;
            heightScale = 1.5F;
        }
        float fullHeight = 2.2F * heightScale;
        float bottom = rise - sink * (fullHeight + rise);
        float clipped = Math.max(0, -bottom);
        float visibleHeight = Math.max(0, fullHeight - clipped);
        float shakeX = Mth.sin(age * 3.7F) * 0.09F;
        float shakeZ = Mth.sin(age * 4.9F) * 0.07F;
        pose.pushPose();
        pose.translate(circle.x() - camera.x + shakeX, circle.y() - camera.y + 0.27 + Math.max(0, bottom),
                circle.z() - camera.z + shakeZ);
        pose.mulPose(Axis.YP.rotationDegrees(180 - event.getCamera().getYRot()));
        var type = RansomRenderTypes.banishmentSprite(((int) age / 2 & 1) == 0 ? FACE_1 : FACE_2);
        // Crop the lower part as the face enters the top surface of the sigil.
        quad(pose, buffers.getBuffer(type), 1.1F * widthScale, visibleHeight,
                visibleHeight / fullHeight, 255);
        buffers.endBatch(type);
        pose.popPose();
        for (int i = 0; i < 12; i++) {
            if (((int) age + i * 7) % 17 >= 8) continue;
            float angle = i * Mth.TWO_PI / 12;
            pose.pushPose();
            pose.translate(circle.x() - camera.x + Mth.cos(angle) * (2.0 + i % 2 * 0.5),
                    circle.y() - camera.y + 0.35 + (i % 4) * 0.65,
                    circle.z() - camera.z + Mth.sin(angle) * (2.0 + i % 2 * 0.5));
            pose.mulPose(event.getCamera().rotation());
            var stopType = RansomRenderTypes.crucifixUnlit(STOP, true);
            renderGlitchedStop(pose, buffers.getBuffer(stopType), age, i);
            buffers.endBatch(stopType);
            pose.popPose();
        }
    }

    private static void renderGlitchedStop(PoseStack pose, VertexConsumer consumer, float age, int index) {
        int tick = (int) age;
        int visibleTick = (tick + index * 7) % 17;
        boolean edge = visibleTick == 0 || visibleTick == 7;
        boolean burst = ((tick * 31 + index * 17) % 29) == 0;
        if (edge || burst) {
            float offset = 0.045F + ((tick + index) & 1) * 0.025F;
            stopQuad(pose, consumer, -offset, 255, 35, 45, 210);
            stopQuad(pose, consumer, offset, 35, 100, 255, 210);
        }
        stopQuad(pose, consumer, 0, 255, 255, 255, edge ? 160 : 255);
    }

    private static void stopQuad(PoseStack pose, VertexConsumer consumer, float offset,
                                 int red, int green, int blue, int alpha) {
        stopVertex(pose, consumer, -0.22F + offset, 0, 0, 1, red, green, blue, alpha);
        stopVertex(pose, consumer, 0.22F + offset, 0, 1, 1, red, green, blue, alpha);
        stopVertex(pose, consumer, 0.22F + offset, 0.44F, 1, 0, red, green, blue, alpha);
        stopVertex(pose, consumer, -0.22F + offset, 0.44F, 0, 0, red, green, blue, alpha);
    }

    private static void stopVertex(PoseStack pose, VertexConsumer consumer, float x, float y,
                                   float u, float v, int red, int green, int blue, int alpha) {
        consumer.addVertex(pose.last().pose(), x, y, 0).setUv(u, v).setColor(red, green, blue, alpha);
    }

    private static Vec3 faceCenter(CrucifixCirclePayload circle, float age) {
        float rise = age < 60 ? 0 : age < 70 ? smooth((age - 60) / 10) * 0.6F : 0.6F;
        float sink = age < 70 ? 0 : smooth((age - 70) / 30);
        int tick = (int) age % 36;
        float height = 2.2F * (tick == 8 ? 0.55F : tick == 25 ? 1.5F : 1);
        float bottom = rise - sink * (height + rise);
        float visible = Math.max(0, height - Math.max(0, -bottom));
        return new Vec3(circle.x(),
                circle.y() + 0.27 + Math.max(0, bottom) + visible / 2,
                circle.z());
    }

    private static void renderChains(RenderLevelStageEvent event, CrucifixCirclePayload circle, float age) {
        Vec3 camera = event.getCamera().getPosition();
        Vec3 top = faceCenter(circle, age);
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        var type = RansomRenderTypes.crucifixUnlit(CHAIN, false);
        var consumer = buffers.getBuffer(type);
        for (Vec3 anchor : CrucifixCircleModel.INSTANCE.chainAnchors(age * 0.25F)) {
            Vec3 bottom = anchor.add(circle.x(), circle.y(), circle.z());
            Vec3 delta = bottom.subtract(top);
            double length = delta.length();
            if (length < 0.001) continue;
            var look = event.getCamera().getLookVector();
            Vec3 width = delta.cross(new Vec3(look.x(), look.y(), look.z()));
            if (width.lengthSqr() < 0.00001) width = delta.cross(new Vec3(0, 1, 0));
            if (width.lengthSqr() < 0.00001) width = delta.cross(new Vec3(1, 0, 0));
            width = width.normalize().scale(CHAIN_HALF_WIDTH);
            double phase = age * 0.015;
            double uvStart = (1 - (phase - Math.floor(phase))) % 1;
            double uvEnd = uvStart + length / CHAIN_TEXTURE_LENGTH;
            int segmentCount = (int) Math.ceil(uvEnd);
            // Integer boundaries give a finite loop even when a UV lands on a seam.
            // Never accumulate tiny remainders: those can round to zero-length segments.
            for (int segment = 0; segment < segmentCount; segment++) {
                double start = Math.max(uvStart, segment);
                double end = Math.min(uvEnd, segment + 1);
                if (end - start <= 0.000001) continue;
                double distance = (start - uvStart) * CHAIN_TEXTURE_LENGTH;
                double next = (end - uvStart) * CHAIN_TEXTURE_LENGTH;
                Vec3 a = top.lerp(bottom, distance / length).subtract(camera);
                Vec3 b = top.lerp(bottom, next / length).subtract(camera);
                float v0 = (float) (start - segment);
                float v1 = (float) (end - segment);
                chainVertex(event.getPoseStack(), consumer, a.subtract(width), 0, v0);
                chainVertex(event.getPoseStack(), consumer, a.add(width), 1, v0);
                chainVertex(event.getPoseStack(), consumer, b.add(width), 1, v1);
                chainVertex(event.getPoseStack(), consumer, b.subtract(width), 0, v1);
            }
        }
        buffers.endBatch(type);
    }

    private static void chainVertex(PoseStack pose, VertexConsumer consumer, Vec3 point, float u, float v) {
        consumer.addVertex(pose.last().pose(), (float) point.x, (float) point.y, (float) point.z)
                .setColor(255, 255, 255, 255).setUv(u, v);
    }

    private static void renderProjection(RenderLevelStageEvent event, CrucifixCirclePayload circle, float age) {
        Vec3 camera = event.getCamera().getPosition();
        Vec3 origin = new Vec3(circle.projectionX(), circle.projectionY(), circle.projectionZ());
        Vec3 direction = new Vec3(circle.x() - origin.x, 0, circle.z() - origin.z).normalize();
        Vec3 position = projectionPosition(circle, age);
        float evaporation = 1 - CrucifixBanishmentTiming.fade(age,
                CrucifixBanishmentTiming.MOON_FADE_START, CrucifixBanishmentTiming.MOON_FADE_TICKS);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        try {
            pose.translate(position.x - camera.x, position.y - camera.y, position.z - camera.z);
            float yaw = (float) Math.toDegrees(Math.atan2(direction.x, direction.z));
            pose.mulPose(Axis.YP.rotationDegrees(yaw + Mth.sin(age * 0.04F) * 12));
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(age * 0.065F) * 5));
            float scale = 0.85F * (1 + evaporation * 0.65F);
            pose.scale(scale, scale, scale);
            CrucifixProjectionModel.INSTANCE.render(pose, Minecraft.getInstance().renderBuffers().bufferSource(), evaporation);
        } finally {
            pose.popPose();
        }
    }
    static Vec3 projectionPosition(CrucifixCirclePayload circle, float age) {
        Vec3 origin = new Vec3(circle.projectionX(), circle.projectionY(), circle.projectionZ());
        Vec3 direction = new Vec3(circle.x() - origin.x, 0, circle.z() - origin.z).normalize();
        return origin.lerp(origin.add(direction).add(0, 0.3, 0), smooth(age / 15));
    }
    private static float smooth(float value) {
        float t = Mth.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }
    private static void quad(PoseStack pose, VertexConsumer vertices, float halfWidth,
                             float height, float bottomUv, int alpha) {
        vertex(pose, vertices, -halfWidth, 0, 0, bottomUv, alpha);
        vertex(pose, vertices, halfWidth, 0, 1, bottomUv, alpha);
        vertex(pose, vertices, halfWidth, height, 1, 0, alpha);
        vertex(pose, vertices, -halfWidth, height, 0, 0, alpha);
    }
    private static void vertex(PoseStack pose, VertexConsumer vertices, float x, float y,
                               float u, float v, int alpha) {
        vertices.addVertex(pose.last().pose(), x, y, 0).setColor(255, 255, 255, alpha)
                .setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose.last(), 0, 0, 1);
    }
    private static ResourceLocation texture(String file) {
        return ResourceLocation.fromNamespaceAndPath(RansomInMinecraft.MODID, "textures/" + file);
    }
}
