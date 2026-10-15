package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import com.wentory.ransom_in_minecraft.CrucifixBanishmentTiming;
import com.wentory.ransom_in_minecraft.network.CrucifixCirclePayload;
import com.wentory.ransom_in_minecraft.network.CrucifixClientPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = RansomInMinecraft.MODID, value = Dist.CLIENT)
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
    private static ShaderInstance circleShader;
    private static ShaderInstance lightShader;
    static ShaderInstance lightShader() { return lightShader; }

    private CrucifixCircleRenderer() {}

    static ShaderInstance shader() {
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

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || CIRCLES.isEmpty()) return;

        Vec3 camera = event.getCamera().getPosition();
        ResourceLocation dimension = minecraft.level.dimension().location();
        long gameTime = minecraft.level.getGameTime();
        float partial = event.getPartialTick();
        prepareDrawOrder(dimension, camera, gameTime, partial);
        boolean compatible = CrucifixIrisCompatibility.useCompatibleRenderer();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
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

    private static void prepareDrawOrder(ResourceLocation dimension, Vec3 camera, long gameTime, float partial) {
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
                                     MultiBufferSource.BufferSource buffers, Vec3 camera,
                                     float ticks, boolean compatible, boolean shinePass) {
        if (circle.banishment() && ticks >= CrucifixBanishmentTiming.VISUAL_END) return;
        float rotation = ticks * DEGREES_PER_TICK % FULL_TURN;
        float growth = net.minecraft.util.Mth.clamp(ticks / 20, 0, 1);
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
                            compatible ? null : circleShader, shinePass, false, true);
                } finally {
                    pose.popPose();
                }
            }
            CrucifixCircleModel.INSTANCE.render(pose, buffers, seconds, circle.chest(),
                    compatible ? null : circleShader, shinePass, circle.banishment(), false);
        } finally {
            pose.popPose();
        }
    }

    private static final class CircleEntry {
        private final CrucifixCirclePayload payload;
        private final ResourceLocation dimension;
        private final CrucifixBlockIllumination blockIllumination = new CrucifixBlockIllumination();
        private double distanceSquared;
        private float ticks;

        private CircleEntry(CrucifixCirclePayload payload) {
            this.payload = payload;
            dimension = new ResourceLocation(payload.dimension());
        }
    }

    @EventBusSubscriber(modid = RansomInMinecraft.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void reloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)
                    resources -> CrucifixChestGlow.clear());
            event.registerReloadListener(CrucifixCircleModel.INSTANCE);
            event.registerReloadListener(CrucifixProjectionModel.INSTANCE);
        }

        @SubscribeEvent
        public static void shaders(RegisterShadersEvent event) throws IOException {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                            new ResourceLocation(RansomInMinecraft.MODID, "crucifix_light"),
                            DefaultVertexFormat.POSITION_TEX_COLOR), shader -> lightShader = shader);
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                            new ResourceLocation(RansomInMinecraft.MODID, "crucifix_circle"),
                            DefaultVertexFormat.POSITION_TEX_COLOR),
                    shader -> circleShader = shader);
        }
    }
}
