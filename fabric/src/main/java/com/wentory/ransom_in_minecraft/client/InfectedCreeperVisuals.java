package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wentory.ransom_in_minecraft.RansomFabric;
import com.wentory.ransom_in_minecraft.network.WormCreeperPayload;
import com.wentory.ransom_in_minecraft.network.WormCloudPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.monster.creeper.CreeperModel;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;

import com.wentory.ransom_in_minecraft.platform.FabricEvents.SubmitCustomGeometryEvent;
import com.wentory.ransom_in_minecraft.platform.FabricEvents.RenderLivingEvent;
import com.wentory.ransom_in_minecraft.platform.FabricEvents.ClientTickEvent;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InfectedCreeperVisuals {
    private static final Map<UUID, Integer> PHASES = new ConcurrentHashMap<>();
    private static final List<CloudVisual> CLOUDS = new ArrayList<>();
    private static final List<TrailFragment> TRAIL = new ArrayList<>();
    private static final Map<UUID, Vec3> LAST_POSITIONS = new ConcurrentHashMap<>();
    private record TrailFragment(String dimension, Vec3 position, long end, int seed) {}
    private record CloudVisual(String dimension, Vec3 position, long end, int duration, int seed) {}
    private static final Identifier GLITCH = texture("glitch.png");
    private static final Identifier[] FACES = {texture("creeper/creeper.png"), texture("creeper/creeper2.png"),
            texture("creeper/creeper3.png"), texture("creeper/creeper4.png"), texture("creeper/creeper5.png")};
    private InfectedCreeperVisuals() {}
    private static Identifier texture(String path) {
        return Identifier.fromNamespaceAndPath(RansomFabric.MODID, "textures/" + path);
    }
    public static void accept(WormCreeperPayload payload) {
        PHASES.put(payload.entityId(), payload.phase());
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Creeper creeper && entity.getUUID().equals(payload.entityId())) {
                Vec3 center = creeper.getBoundingBox().getCenter();
                com.wentory.ransom_in_minecraft.platform.PersistentData.of(creeper).putInt("ransom_creeper_phase", payload.phase());
                creeper.refreshDimensions();
                creeper.setPos(center.x, center.y - creeper.getBbHeight() * 0.5D, center.z);
                break;
            }
        }
    }
    public static int phase(UUID id) { return PHASES.getOrDefault(id, -1); }
    public static void clear() { PHASES.clear(); CLOUDS.clear(); TRAIL.clear(); LAST_POSITIONS.clear(); }
    public static void accept(WormCloudPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 position = new Vec3(payload.x(), payload.y(), payload.z());
        CLOUDS.add(new CloudVisual(payload.dimension(), position, mc.level.getGameTime() + payload.ticks(),
                payload.ticks(), position.hashCode()));
    }
    public static void remove(UUID id) { PHASES.remove(id); LAST_POSITIONS.remove(id); }

    public static void trailTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;
        long now = mc.level.getGameTime();
        TRAIL.removeIf(fragment -> now >= fragment.end());
        String dimension = mc.level.dimension().identifier().toString();
        for (var entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof Creeper creeper) || phase(creeper.getUUID()) != 2) continue;
            Vec3 center = creeper.getBoundingBox().getCenter();
            Vec3 previous = LAST_POSITIONS.put(creeper.getUUID(), center);
            if (previous == null) previous = center;
            for (int sample = 0; sample < 3; sample++) {
                Vec3 position = previous.lerp(center, sample / 3.0D);
                TRAIL.add(new TrailFragment(dimension, position, now + 12,
                        hash(creeper.getId() * 971 + (int) now * 31 + sample)));
            }
        }
    }


    public static final class Layers {
        public static void initialize() {
        net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof CreeperRenderer creeper && type == net.minecraft.world.entity.EntityTypes.CREEPER) helper.register(new InfectionLayer(creeper));
        });
    }
    }

    private static final class InfectionLayer extends RenderLayer<net.minecraft.client.renderer.entity.state.CreeperRenderState, CreeperModel> {
        InfectionLayer(RenderLayerParent<net.minecraft.client.renderer.entity.state.CreeperRenderState, CreeperModel> parent) { super(parent); }
        @Override public void submit(PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector collector, int light,
 net.minecraft.client.renderer.entity.state.CreeperRenderState state, float yaw, float pitch) {
 if (!(RenderStateEntities.get(state) instanceof Creeper creeper)) return;
 DeferredBuffers buffers = new DeferredBuffers(collector);
 getParentModel().setupAnim(state);
            if (phase(creeper.getUUID()) < 0 || creeper.isInvisible() || creeper.isDeadOrDying()) return;
            pose.pushPose();
            pose.scale(1.035F, 1.035F, 1.035F);
            getParentModel().renderToBuffer(pose, buffers.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(Identifier.withDefaultNamespace("textures/entity/creeper/creeper.png"))),
                    light, OverlayTexture.NO_OVERLAY, 0x76FF1010);
            pose.popPose();
            pose.pushPose();
            pose.scale(1.055F, 1.055F, 1.055F);
            var animated = new AnimatedSheetVertexConsumer(buffers.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucentEmissive(GLITCH)),
                    creeper.tickCount / 2, 6);
            getParentModel().renderToBuffer(pose, animated, 0x00F000F0, OverlayTexture.NO_OVERLAY, 0xCCFFFFFF);
            pose.popPose();
            buffers.endBatch();
        }
    }

    public static void face(RenderLivingEvent.Pre<?, ?, ?> event) {
        if (!(RenderStateEntities.get(event.getRenderState()) instanceof Creeper creeper) || phase(creeper.getUUID()) != 2) return;
        event.setCanceled(true);
        if (creeper.isInvisible() || creeper.isDeadOrDying()) return;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(0, creeper.getBbHeight() * 0.5D, 0);
        pose.mulPose(Minecraft.getInstance().gameRenderer.mainCamera().rotation());
        float time = creeper.tickCount + event.getPartialTick();
        pose.translate(Math.sin(time * 8.7F) * 0.055D, Math.cos(time * 11.3F) * 0.045D, 0);
        pose.mulPose(Axis.ZP.rotation((float) Math.sin(time * 6.1F) * 0.035F));
        DeferredBuffers buffers = new DeferredBuffers(event.getSubmitNodeCollector());
        VertexConsumer consumer = buffers.getBuffer(
                net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucentEmissive(FACES[(creeper.tickCount / 2) % FACES.length]));
        int seed = creeper.getUUID().hashCode() ^ creeper.tickCount / 2;
        for (int band = 0; band < 8; band++) {
            int random = hash(seed + band * 137);
            if ((random & 63) == 0) continue;
            float shift = (random & 3) == 0 ? ((random >>> 4 & 15) - 7) * 0.04F : 0;
            float width = 0.9F * ((random & 7) == 0 ? 1.15F : 1.0F);
            float top = 0.9F - band * 0.225F;
            float bottom = top - 0.225F;
            texturedQuad(pose, consumer, -width + shift, width + shift, bottom, top, band / 8.0F, (band + 1) / 8.0F);
        }
        pose.popPose();
        pose.pushPose();
        pose.translate(0, creeper.getBbHeight() * 0.5D, 0);
        fragments(pose, buffers, creeper.getUUID().hashCode(), 14, 0.9F);
        pose.popPose();
        buffers.endBatch();
    }

    public static void clouds(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        DeferredBuffers buffers = new DeferredBuffers(event.getSubmitNodeCollector());
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        long now = mc.level.getGameTime();
        CLOUDS.removeIf(cloud -> now >= cloud.end());
        for (CloudVisual cloud : CLOUDS) {
            if (!cloud.dimension().equals(mc.level.dimension().identifier().toString())
                    || cloud.position().distanceToSqr(camera) > 64.0D * 64.0D) continue;
            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            pose.translate(cloud.position().x - camera.x, cloud.position().y + 1.0D - camera.y, cloud.position().z - camera.z);
            float fade = Math.max(0, (cloud.end() - now) / (float) cloud.duration());
            float expansion = 1.0F + 0.25F * (1.0F - fade * fade);
            pose.scale(expansion, expansion, expansion);
            // Keep burst anchors fixed while expansion carries them away from the blast.
            GlitchParticles.render(pose, buffers, cloud.seed(),
                    cloud.duration() - (cloud.end() - now), 120,
                    3.0F, 1.95F, 3.0F, 1.0F, Math.round(230 * fade * fade), true);
            pose.popPose();
        }
        for (TrailFragment fragment : TRAIL) {
            if (!fragment.dimension().equals(mc.level.dimension().identifier().toString())
                    || fragment.position().distanceToSqr(camera) > 64.0D * 64.0D) continue;
            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            pose.translate(fragment.position().x - camera.x, fragment.position().y - camera.y,
                    fragment.position().z - camera.z);
            float fade = Math.max(0, (fragment.end() - now) / 12.0F);
            fragments(pose, buffers, fragment.seed(), 7, 0.45F, Math.round(220 * fade));
            pose.popPose();
        }
        buffers.endBatch(RansomRenderTypes.boundaryFragments());
    }

    private static void fragments(PoseStack pose, DeferredBuffers buffers, int seed, int count, float radius) {
        fragments(pose, buffers, seed, count, radius, 200);
    }

    private static void fragments(PoseStack pose, DeferredBuffers buffers, int seed, int count, float radius, int alpha) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        GlitchParticles.render(pose, buffers, seed, level.getGameTime(), count,
                radius, radius * 0.65F, radius, 1.0F, alpha);
    }
    private static void texturedQuad(PoseStack p, VertexConsumer c, float left, float right, float bottom,
                                     float top, float v0, float v1) {
        c.addVertex(p.last().pose(), left, bottom, 0).setColor(-1).setUv(0, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(p.last(), 0, 0, 1);
        c.addVertex(p.last().pose(), right, bottom, 0).setColor(-1).setUv(1, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(p.last(), 0, 0, 1);
        c.addVertex(p.last().pose(), right, top, 0).setColor(-1).setUv(1, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(p.last(), 0, 0, 1);
        c.addVertex(p.last().pose(), left, top, 0).setColor(-1).setUv(0, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(p.last(), 0, 0, 1);
    }
    private static int hash(int x) { x ^= x >>> 16; x *= 0x7feb352d; x ^= x >>> 15; x *= 0x846ca68b; return x ^ x >>> 16; }
}
