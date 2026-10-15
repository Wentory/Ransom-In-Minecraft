package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wentory.ransom_in_minecraft.CrucifixBanishmentTiming;
import com.wentory.ransom_in_minecraft.network.CrucifixCirclePayload;
import com.wentory.ransom_in_minecraft.network.CrucifixClientPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CrucifixCircleRenderer {
    private static final float CHEST_SCALE = 0.65F;
    private static final float TICKS_PER_SECOND = 20.0F;
    private static final float DEGREES_PER_TICK = 5.0F / TICKS_PER_SECOND;
    private static final float FULL_TURN = 360.0F;
    private static final double MAX_DISTANCE_SQUARED = 128.0 * 128.0;
    private static final Map<UUID, CircleEntry> CIRCLES = new LinkedHashMap<>();
    private static final List<CircleEntry> DRAW_ORDER = new ArrayList<>();
    private static final Comparator<CircleEntry> FAR_TO_NEAR =
            Comparator.comparingDouble((CircleEntry circle) -> circle.distanceSquared).reversed();
    private static Object circleShader;

    private CrucifixCircleRenderer() {}
    public static void submit(PoseStack pose,net.minecraft.client.renderer.SubmitNodeCollector collector,
            net.minecraft.client.renderer.state.level.LevelRenderState state){render(new WorldEffectFrame(pose,collector,state));}

    static Object shader() {
        return circleShader;
    }

    public static void accept(CrucifixCirclePayload circle) {
        MoonlightEffects.circle(circle);
        if (circle.visible()) {
            CIRCLES.put(circle.owner(), new CircleEntry(circle));
        } else {
            CIRCLES.remove(circle.owner());
        }
    }

    public static void clear() {
        CIRCLES.clear();
        DRAW_ORDER.clear();
    }

    public static void render(WorldEffectFrame event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || CIRCLES.isEmpty()) return;

        Vec3 camera = event.getCamera().getPosition();
        Identifier dimension = minecraft.level.dimension().identifier();
        long gameTime = minecraft.level.getGameTime();
        float partial = event.partial();
        prepareDrawOrder(dimension, camera, gameTime, partial);
        boolean compatible = CrucifixIrisCompatibility.useCompatibleRenderer();
        PoseStack pose = event.getPoseStack();
        DeferredBuffers buffers = event.buffers();
        try {
            for (CircleEntry entry : DRAW_ORDER) {
                renderCircle(entry.payload, pose, buffers, camera, entry.ticks, compatible, false);
                if (entry.payload.chest()) {
                    entry.blockIllumination.render(entry.payload, entry.ticks, pose, buffers, camera);
                    CrucifixChestGlow.render(entry.payload, entry.ticks, pose, buffers, camera, partial);
                }
                if (entry.payload.banishment()) {
                    entry.blockIllumination.render(entry.payload, entry.ticks, pose, buffers, camera);
                    RansomBanishmentRenderer.render(event, entry.payload, entry.ticks);
                }
                renderCircle(entry.payload, pose, buffers, camera, entry.ticks, compatible, true);
            }
        } finally {
            DRAW_ORDER.clear();
        }
    }

    private static void prepareDrawOrder(Identifier dimension, Vec3 camera, long gameTime, float partial) {
        DRAW_ORDER.clear();
        for (CircleEntry entry : CIRCLES.values()) {
            if (!dimension.equals(entry.dimension)) continue;
            CrucifixCirclePayload circle = entry.payload;
            entry.distanceSquared = camera.distanceToSqr(circle.x(), circle.y(), circle.z());
            if (entry.distanceSquared > MAX_DISTANCE_SQUARED) continue;
            entry.ticks = gameTime - circle.startTick() + partial;
            if (circle.banishment() && entry.ticks >= CrucifixBanishmentTiming.LIGHT_END) continue;
            if (circle.chest() && entry.ticks >= CrucifixBanishmentTiming.VISUAL_END) continue;
            DRAW_ORDER.add(entry);
        }
        DRAW_ORDER.sort(FAR_TO_NEAR);
    }

    private static void renderCircle(CrucifixCirclePayload circle, PoseStack pose,
                                     DeferredBuffers buffers, Vec3 camera,
                                     float ticks, boolean compatible, boolean shinePass) {
        if (circle.banishment() && ticks >= CrucifixBanishmentTiming.VISUAL_END) return;
        float rotation = ticks * DEGREES_PER_TICK % FULL_TURN;
        float growth = Math.clamp(ticks / 20, 0, 1);
        if (circle.chest()) {
            float spinTicks = Math.min(ticks, 60);
            float spinProgress = spinTicks / 60;
            // Ease-out angle with a gradual quadratic drop in speed over three seconds.
            rotation = DEGREES_PER_TICK * ticks
                    + (18 - DEGREES_PER_TICK) * 60 / 3
                    * (1 - (float) Math.pow(1 - spinProgress, 3));
        }
        float seconds = ticks / TICKS_PER_SECOND;
        pose.pushPose();
        try {
            pose.translate(circle.x() - camera.x, circle.y() - camera.y, circle.z() - camera.z);
            pose.mulPose(Axis.YP.rotationDegrees(rotation));
            if (circle.chest()) {
                float scale = 0.04F + (CHEST_SCALE - 0.04F) * (1 - (1 - growth) * (1 - growth));
                pose.scale(scale, 1, scale);
                // Outer squares and their Shine stay on the floor throughout the expansion.
                pose.pushPose();
                try {
                    pose.translate(0, -0.61, 0);
                    CrucifixCircleModel.INSTANCE.render(pose, buffers, seconds, true,
                            compatible, shinePass, false, true);
                } finally {
                    pose.popPose();
                }
            }
            CrucifixCircleModel.INSTANCE.render(pose, buffers, seconds, circle.chest(),
                    compatible, shinePass, circle.banishment(), false);
        } finally {
            pose.popPose();
        }
    }

    private static final class CircleEntry {
        private final CrucifixCirclePayload payload;
        private final Identifier dimension;
        private final CrucifixBlockIllumination blockIllumination = new CrucifixBlockIllumination();
        private double distanceSquared;
        private float ticks;

        private CircleEntry(CrucifixCirclePayload payload) {
            this.payload = payload;
            dimension = Identifier.parse(payload.dimension());
        }
    }

    public static void initialize(){
        CrucifixClientPayloads.bind(CrucifixCircleRenderer::accept,MobBanishmentVisuals::accept);
        CrucifixResourceRegistration.install();
    }
    public static void reload(net.minecraft.server.packs.resources.ResourceManager resources){
        CrucifixCircleModel.INSTANCE.onResourceManagerReload(resources);
        CrucifixProjectionModel.INSTANCE.onResourceManagerReload(resources);
        CrucifixChestGlow.clear();
        RansomShaderPreloader.load();
    }
}
