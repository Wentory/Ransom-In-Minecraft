package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import com.wentory.ransom_in_minecraft.network.ClientInfectionTracker;
import com.wentory.ransom_in_minecraft.network.ClientPlayerGlitchTracker;
import com.wentory.ransom_in_minecraft.network.ClientPlayerVisualEffectTracker;
import com.wentory.ransom_in_minecraft.network.PlayerVisualEffectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.RenderPlayerEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = RansomInMinecraft.MODID, value = Dist.CLIENT)
public final class InfectedPlayerGlitchRenderer {
    private static final ResourceLocation STOP = new ResourceLocation(
            RansomInMinecraft.MODID, "textures/block.png");
    private static final Map<UUID, VisualState> STATES = new HashMap<>();
    private static final int SLICE_TICKS = 3;
    private static final int STRONG_HIT_TICKS = 20;
    private static final int STRONG_SLICE_TICKS = 12;
    private static final int STOP_EXIT_AGE = 34;
    private static boolean renderingSlicePass;

    private InfectedPlayerGlitchRenderer() {}

    static void clear() {
        STATES.clear();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforePlayerRender(RenderPlayerEvent.Pre event) {
        if (renderingSlicePass) return;
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) return;
        UUID playerId = player.getUUID();
        if (player.isDeadOrDying()) {
            STATES.remove(playerId);
            ClientPlayerGlitchTracker.consumeHit(playerId);
            return;
        }
        boolean infected = ClientInfectionTracker.isInfected(playerId);
        int visualEffect = ClientPlayerVisualEffectTracker.get(playerId);
        VisualState existing = STATES.get(playerId);
        if (existing != null && (existing.entity != player || player.tickCount < existing.lastTick)) {
            STATES.remove(playerId);
            existing = null;
        }
        if ((!infected && visualEffect == PlayerVisualEffectPayload.CLEAR
                && !ClientPlayerGlitchTracker.hasPendingHit(playerId)
                && (existing == null || !existing.hasTransientEffect(player.tickCount))) || player.isInvisible()) {
            STATES.remove(playerId);
            return;
        }

        VisualState state = state(player);
        if (!(infected || visualEffect == PlayerVisualEffectPayload.CHAOS
                || state.isStrongSliceActive(player.tickCount)) || !state.isSliceActive(player.tickCount)) return;

        if (!renderSlicedPlayer(event, player, state)) return;
        if (state.stopStartTick != Integer.MIN_VALUE) renderStopOnPlayer(event, player, state);
        renderGlitchParticles(event.getPoseStack(), event.getMultiBufferSource(), state, player.tickCount);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterPlayerRender(RenderPlayerEvent.Post event) {
        if (renderingSlicePass) return;
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) return;
        if (player.isDeadOrDying()) return;
        VisualState state = STATES.get(player.getUUID());
        if (state == null) return;
        if (state.stopStartTick != Integer.MIN_VALUE) {
            renderStopOnPlayer(event, player, state);
        }
        renderGlitchParticles(event.getPoseStack(), event.getMultiBufferSource(), state, player.tickCount);
    }

    static float extraGlitchStrength(AbstractClientPlayer player) {
        VisualState state = state(player);
        if (ClientPlayerVisualEffectTracker.get(player.getUUID()) == PlayerVisualEffectPayload.CHAOS) return 1.0F;
        if (!ClientInfectionTracker.isInfected(player.getUUID())) return 0.0F;
        if (player.tickCount < state.strongHitUntil) {
            float remaining = (state.strongHitUntil - player.tickCount) / (float) STRONG_HIT_TICKS;
            return 0.72F + remaining * 0.28F;
        }
        return player.tickCount < state.ambientUntil ? 0.58F : 0.0F;
    }

    static float successStrength(AbstractClientPlayer player) {
        VisualState state = STATES.get(player.getUUID());
        if (state == null || player.tickCount >= state.successUntil) return 0.0F;
        float remaining = (state.successUntil - player.tickCount) / 8.0F;
        return 0.35F + 0.65F * remaining;
    }

    private static VisualState state(AbstractClientPlayer player) {
        UUID playerId = player.getUUID();
        VisualState state = STATES.get(playerId);
        if (state == null || state.entity != player || player.tickCount < state.lastTick) {
            state = new VisualState(player, playerId.hashCode());
            STATES.put(playerId, state);
        }
        state.tick(player);
        return state;
    }

    private static void renderStopOnPlayer(RenderPlayerEvent event, AbstractClientPlayer player,
                                           VisualState state) {
        int age = player.tickCount - state.stopStartTick;
        float appear = Mth.clamp(age / 2.0F, 0.0F, 1.0F);
        float disappear = Mth.clamp((age - STOP_EXIT_AGE) / 4.0F, 0.0F, 1.0F);
        float visibility = appear * (1.0F - disappear);
        if (visibility <= 0.01F) return;

        float impact = age >= 5 ? 1.0F - Mth.clamp((age - 5.0F) / 7.0F, 0.0F, 1.0F) : 0.0F;
        float size = 1.15F * (1.0F + impact * 0.10F);
        float shakeX = impact * (Math.floorMod(state.stopSeed + age * 17, 9) - 4) * 0.012F;
        float shakeY = impact * (Math.floorMod(state.stopSeed + age * 29, 9) - 4) * 0.012F;

        PoseStack poseStack = event.getPoseStack();
        Vec3 playerPosition = player.getPosition(event.getPartialTick());
        Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 towardCamera = cameraPosition.subtract(playerPosition);
        double horizontalLength = Math.sqrt(towardCamera.x * towardCamera.x + towardCamera.z * towardCamera.z);
        double frontX = horizontalLength > 0.0001D ? towardCamera.x / horizontalLength * 0.38D : 0.0D;
        double frontZ = horizontalLength > 0.0001D ? towardCamera.z / horizontalLength * 0.38D : 0.38D;
        poseStack.pushPose();
        poseStack.translate(frontX + shakeX, 1.08F + shakeY, frontZ);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        VertexConsumer consumer = event.getMultiBufferSource().getBuffer(RenderType.entityTranslucentEmissive(STOP));

        int chroma = Math.round(impact * 6.0F);
        if (chroma > 0) {
            float shift = chroma * 0.012F;
            coloredQuad(poseStack, consumer, 0x00F000F0,
                    -size / 2.0F - shift, size / 2.0F - shift, -size / 2.0F, size / 2.0F, 0.005F,
                    0, 1, 0, 1, 255, 20, 20, Math.round(visibility * 170));
            coloredQuad(poseStack, consumer, 0x00F000F0,
                    -size / 2.0F + shift, size / 2.0F + shift, -size / 2.0F, size / 2.0F, 0.003F,
                    0, 1, 0, 1, 30, 220, 255, Math.round(visibility * 145));
        }

        float distortion = (1.0F - appear) * .11F + disappear * .14F + impact * .07F;
        int slices = 12;
        for (int slice = 0; slice < slices; slice++) {
            float threshold = Math.floorMod(slice * 37 + 11, slices) / (float) slices;
            if (appear < threshold || 1.0F - disappear < threshold) continue;
            float bottom = -size / 2.0F + slice * size / slices;
            float top = -size / 2.0F + (slice + 1) * size / slices;
            float offset = (Math.floorMod(state.stopSeed + slice * 23 + age * 13, 9) - 4) * distortion / 4.0F;
            float vTop = 1.0F - (slice + 1) / (float) slices;
            float vBottom = 1.0F - slice / (float) slices;
            coloredQuad(poseStack, consumer, 0x00F000F0,
                    -size / 2.0F + offset, size / 2.0F + offset, bottom, top, 0.0F,
                    0, 1, vTop, vBottom, 255, 255, 255, Math.round(visibility * 255));
        }
        poseStack.popPose();
    }

    private static void renderGlitchParticles(PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffers,
                                              VisualState state, int tick) {
        if (state.particles.isEmpty()) return;
        VertexConsumer consumer = buffers.getBuffer(RansomRenderTypes.boundaryFragments());
        for (GlitchParticle particle : state.particles) {
            float fadeIn = Mth.clamp(particle.age / 2.0F, 0.0F, 1.0F);
            float fadeOut = Mth.clamp((particle.lifetime - particle.age) / 3.0F, 0.0F, 1.0F);
            float visibility = Math.min(fadeIn, fadeOut);
            if (visibility <= 0.0F) continue;
            int frame = Math.floorMod(tick + particle.frameOffset, 6);
            poseStack.pushPose();
            poseStack.translate(particle.x, particle.y, particle.z);
            poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
            for (int piece = 0; piece < 5; piece++) {
                int pieceSeed = hash(particle.seed + frame * 131 + piece * 977);
                if (Math.floorMod(pieceSeed, 7) == 0) continue;
                float centerX = (Math.floorMod(hash(pieceSeed + 1), 2001) / 2000.0F - 0.5F) * particle.width;
                float centerY = (Math.floorMod(hash(pieceSeed + 2), 2001) / 2000.0F - 0.5F) * particle.height;
                float pieceWidth = particle.width * (0.20F
                        + Math.floorMod(hash(pieceSeed + 3), 1001) / 1600.0F);
                float pieceHeight = particle.height * (0.10F
                        + Math.floorMod(hash(pieceSeed + 4), 1001) / 3500.0F);
                int colorRoll = Math.floorMod(hash(pieceSeed + 5), 10);
                int red = colorRoll < 6 ? 255 : colorRoll < 9 ? 8 : 255;
                int green = colorRoll < 6 ? 12 : colorRoll < 9 ? 0 : 255;
                int blue = colorRoll < 6 ? 12 : colorRoll < 9 ? 0 : 255;
                int alpha = Mth.clamp(Math.round((150 + Math.floorMod(pieceSeed, 91)) * visibility), 0, 240);
                proceduralQuad(poseStack, consumer,
                        centerX - pieceWidth / 2.0F, centerX + pieceWidth / 2.0F,
                        centerY - pieceHeight / 2.0F, centerY + pieceHeight / 2.0F,
                        -0.025F - piece * 0.0005F, red, green, blue, alpha);
            }
            poseStack.popPose();
        }
    }

    private static void proceduralQuad(PoseStack poseStack, VertexConsumer consumer,
                                       float left, float right, float bottom, float top, float z,
                                       int red, int green, int blue, int alpha) {
        consumer.vertex(poseStack.last().pose(), left, bottom, z).color(red, green, blue, alpha).endVertex();
        consumer.vertex(poseStack.last().pose(), right, bottom, z).color(red, green, blue, alpha).endVertex();
        consumer.vertex(poseStack.last().pose(), right, top, z).color(red, green, blue, alpha).endVertex();
        consumer.vertex(poseStack.last().pose(), left, top, z).color(red, green, blue, alpha).endVertex();
    }

    private record ScreenSlice(int bottom, int top, float offsetPixels, float stretch, boolean hidden) {}

    private static boolean renderSlicedPlayer(RenderPlayerEvent.Pre event, AbstractClientPlayer player,
                                              VisualState state) {
        Minecraft minecraft = Minecraft.getInstance();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        int width = minecraft.getWindow().getWidth();
        int height = minecraft.getWindow().getHeight();
        Vec3 cameraPosition = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 playerPosition = player.getPosition(event.getPartialTick());
        Quaternionf cameraRotation = new Quaternionf(minecraft.gameRenderer.getMainCamera().rotation());
        float feet = screenY(playerPosition, cameraPosition, cameraRotation, 0.0F, height);
        float head = screenY(playerPosition, cameraPosition, cameraRotation, 2.0F, height);
        if (!Float.isFinite(feet) || !Float.isFinite(head) || width <= 0 || height <= 0) return false;
        int bottom = Mth.clamp((int) Math.floor(Math.min(feet, head)), 0, height);
        int top = Mth.clamp((int) Math.ceil(Math.max(feet, head)), 0, height);
        int span = top - bottom;
        if (span < 10) return false;

        boolean strong = state.isStrongSliceActive(player.tickCount);
        int count = strong ? 3 + Math.floorMod(hash(state.sliceSeed + 17), 3)
                : 1 + Math.floorMod(hash(state.sliceSeed + 17), 4);
        List<ScreenSlice> slices = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int seed = hash(state.sliceSeed + i * 131);
            int bandHeight = Math.max(3, Math.round(span * (strong ? 0.07F : 0.045F)
                    + span * Math.floorMod(hash(seed + 1), strong ? 71 : 56) / 1000.0F));
            int bandBottom = bottom + Math.floorMod(hash(seed + 2), Math.max(1, span - bandHeight));
            float direction = (seed & 1) == 0 ? -1.0F : 1.0F;
            float offset = direction * (strong
                    ? Math.min(75.0F, Math.max(16.0F, span * 0.18F))
                    : Math.min(40.0F, Math.max(8.0F, span * 0.09F)))
                    * (0.6F + Math.floorMod(hash(seed + 3), 601) / 1000.0F);
            float stretch = (strong ? 0.75F : 0.90F)
                    + Math.floorMod(hash(seed + 4), strong ? 701 : 401) / 1000.0F;
            slices.add(new ScreenSlice(bandBottom, Math.min(top, bandBottom + bandHeight),
                    offset, stretch, Math.floorMod(hash(seed + 5), strong ? 4 : 6) == 0));
        }
        slices.sort(java.util.Comparator.comparingInt(ScreenSlice::bottom));

        float depth = Math.max(0.5F, (float) playerPosition.distanceTo(cameraPosition));
        float focalPixels = Math.max(1.0F,
                height * 0.5F * Math.abs(RenderSystem.getProjectionMatrix().m11()));
        Vector3f right = new Vector3f(1.0F, 0.0F, 0.0F).rotate(cameraRotation);

        // Flush unrelated entity geometry before changing the global scissor state.
        buffers.endBatch();
        if (event.getMultiBufferSource() instanceof MultiBufferSource.BufferSource eventBuffers
                && eventBuffers != buffers) eventBuffers.endBatch();
        renderingSlicePass = true;
        try {
            int cursor = 0;
            for (ScreenSlice slice : slices) {
                int start = Math.max(cursor, slice.bottom());
                if (start >= slice.top()) continue;
                renderBand(event, player, buffers, width, cursor, start, 0.0F, 1.0F,
                        depth, focalPixels, right, cameraRotation);
                if (!slice.hidden()) renderBand(event, player, buffers, width, start, slice.top(),
                        slice.offsetPixels(), slice.stretch(), depth, focalPixels, right, cameraRotation);
                cursor = slice.top();
            }
            renderBand(event, player, buffers, width, cursor, height, 0.0F, 1.0F,
                    depth, focalPixels, right, cameraRotation);
        } finally {
            RenderSystem.disableScissor();
            renderingSlicePass = false;
        }
        return true;
    }

    private static float screenY(Vec3 playerPosition, Vec3 cameraPosition, Quaternionf cameraRotation,
                                 float localY, int framebufferHeight) {
        Vec3 relative = playerPosition.subtract(cameraPosition);
        Vector3f view = new Vector3f((float) relative.x, (float) relative.y + localY,
                (float) relative.z).rotate(new Quaternionf(cameraRotation).conjugate());
        Vector4f clip = new Vector4f(view, 1.0F).mul(RenderSystem.getProjectionMatrix());
        if (clip.w <= 0.0F) return Float.NaN;
        return (clip.y / clip.w + 1.0F) * framebufferHeight * 0.5F;
    }

    private static void renderBand(RenderPlayerEvent.Pre event, AbstractClientPlayer player,
                                   MultiBufferSource.BufferSource buffers, int width, int bottom, int top,
                                   float offsetPixels, float stretch, float depth, float focalPixels,
                                   Vector3f right, Quaternionf cameraRotation) {
        if (top <= bottom) return;
        RenderSystem.enableScissor(0, bottom, width, top - bottom);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        try {
            if (offsetPixels != 0.0F) {
                float shift = offsetPixels * depth / focalPixels;
                pose.translate(right.x * shift, right.y * shift, right.z * shift);
            }
            if (stretch != 1.0F) {
                pose.mulPose(cameraRotation);
                pose.scale(stretch, 1.0F, 1.0F);
                pose.mulPose(new Quaternionf(cameraRotation).conjugate());
            }
            float yaw = Mth.rotLerp(event.getPartialTick(), player.yRotO, player.getYRot());
            event.getRenderer().render(player, yaw, event.getPartialTick(), pose, buffers, event.getPackedLight());
            buffers.endBatch();
        } finally {
            pose.popPose();
        }
    }
    private static void coloredQuad(PoseStack poseStack, VertexConsumer consumer, int light,
                                    float left, float right, float bottom, float top, float z,
                                    float u0, float u1, float vTop, float vBottom,
                                    int red, int green, int blue, int alpha) {
        consumer.vertex(poseStack.last().pose(), left, bottom, z)
                .color(red, green, blue, alpha).uv(u0, vBottom)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(poseStack.last().normal(), 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(poseStack.last().pose(), right, bottom, z)
                .color(red, green, blue, alpha).uv(u1, vBottom)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(poseStack.last().normal(), 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(poseStack.last().pose(), right, top, z)
                .color(red, green, blue, alpha).uv(u1, vTop)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(poseStack.last().normal(), 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(poseStack.last().pose(), left, top, z)
                .color(red, green, blue, alpha).uv(u0, vTop)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(poseStack.last().normal(), 0.0F, 0.0F, 1.0F).endVertex();
    }

    private static int hash(int value) {
        value ^= value >>> 16;
        value *= 0x7feb352d;
        value ^= value >>> 15;
        value *= 0x846ca68b;
        return value ^ value >>> 16;
    }

    private static final class VisualState {
        private final AbstractClientPlayer entity;
        private final int baseSeed;
        private int lastTick = Integer.MIN_VALUE;
        private int nextSliceTick;
        private int sliceUntil;
        private int sliceSeed;
        private int nextAmbientTick;
        private int ambientUntil;
        private int strongHitUntil;
        private int strongSliceUntil;
        private int successUntil;
        private int stopStartTick = Integer.MIN_VALUE;
        private int stopSeed;
        private int previousVisualEffect = PlayerVisualEffectPayload.CLEAR;
        private int particleSerial;
        private final List<GlitchParticle> particles = new ArrayList<>();

        private VisualState(AbstractClientPlayer player, int baseSeed) {
            this.entity = player;
            int tick = player.tickCount;
            this.baseSeed = baseSeed;
            this.nextSliceTick = tick + 12 + Math.floorMod(hash(baseSeed), 24);
            this.nextAmbientTick = tick + 15 + Math.floorMod(hash(baseSeed + 1), 61);
        }

        private void tick(AbstractClientPlayer player) {
            int tick = player.tickCount;
            if (tick == lastTick) return;
            lastTick = tick;

            int visualEffect = ClientPlayerVisualEffectTracker.get(player.getUUID());
            if (visualEffect == PlayerVisualEffectPayload.STOP
                    && previousVisualEffect != PlayerVisualEffectPayload.STOP) {
                stopStartTick = tick;
                stopSeed = hash(baseSeed ^ tick * 43);
            } else if (visualEffect == PlayerVisualEffectPayload.CHAOS) {
                stopStartTick = Integer.MIN_VALUE;
            }
            if (ClientPlayerVisualEffectTracker.consumeSuccess(player.getUUID())) {
                successUntil = tick + 8;
                visualEffect = PlayerVisualEffectPayload.CLEAR;
            }
            previousVisualEffect = visualEffect;

            if (ClientPlayerGlitchTracker.consumeHit(player.getUUID())) {
                strongHitUntil = tick + STRONG_HIT_TICKS;
                strongSliceUntil = tick + STRONG_SLICE_TICKS;
                sliceSeed = hash(baseSeed ^ tick * 113);
                sliceUntil = tick + 2;
                nextSliceTick = tick + 2;
                ambientUntil = Math.max(ambientUntil, tick + 8);
            }
            if (tick < strongSliceUntil && tick >= sliceUntil) {
                sliceSeed = hash(baseSeed ^ tick * 113);
                sliceUntil = tick + 2;
            } else if (visualEffect == PlayerVisualEffectPayload.CHAOS && tick >= sliceUntil
                    && Math.floorMod(hash(baseSeed + tick * 19), 3) != 0) {
                sliceSeed = hash(baseSeed ^ tick * 71);
                sliceUntil = tick + SLICE_TICKS;
            } else if (tick >= nextSliceTick) {
                sliceSeed = hash(baseSeed ^ tick * 31);
                sliceUntil = tick + SLICE_TICKS;
                nextSliceTick = tick + 12 + Math.floorMod(hash(sliceSeed + 2), 30);
            }
            if (tick >= nextAmbientTick) {
                ambientUntil = tick + 2 + Math.floorMod(hash(baseSeed ^ tick), 3);
                nextAmbientTick = tick + 25 + Math.floorMod(hash(baseSeed + tick * 7), 76);
            }

            for (Iterator<GlitchParticle> iterator = particles.iterator(); iterator.hasNext();) {
                GlitchParticle particle = iterator.next();
                particle.age++;
                if (particle.age >= particle.lifetime) {
                    iterator.remove();
                }
            }

            int particleCount = visualEffect == PlayerVisualEffectPayload.CHAOS ? 7
                    : tick < strongHitUntil ? 5
                    : tick < ambientUntil && (tick & 1) == 0 ? 2 : 0;
            for (int i = 0; i < particleCount && particles.size() < 80; i++) spawnParticle(tick);

            if (stopStartTick != Integer.MIN_VALUE && tick - stopStartTick > STOP_EXIT_AGE + 4) {
                stopStartTick = Integer.MIN_VALUE;
            }
        }

        private void spawnParticle(int tick) {
            int seed = hash(baseSeed + tick * 131 + particleSerial++ * 977);
            float x = (Math.floorMod(seed, 2001) / 2000.0F - 0.5F) * 0.95F;
            float y = 0.08F + Math.floorMod(hash(seed + 1), 1901) / 1000.0F;
            float z = (Math.floorMod(hash(seed + 2), 1001) / 1000.0F - 0.5F) * 0.35F;
            float width = 0.12F + Math.floorMod(hash(seed + 3), 1001) / 3500.0F;
            float height = 0.055F + Math.floorMod(hash(seed + 4), 1001) / 6500.0F;
            int lifetime = 7 + Math.floorMod(hash(seed + 5), 8);
            int frameOffset = Math.floorMod(hash(seed + 6), 6);
            particles.add(new GlitchParticle(x, y, z, width, height, lifetime, frameOffset, seed));
        }

        private boolean hasTransientEffect(int tick) {
            return tick < successUntil || tick < strongSliceUntil
                    || stopStartTick != Integer.MIN_VALUE || !particles.isEmpty();
        }

        private boolean isSliceActive(int tick) {
            return tick < sliceUntil;
        }

        private boolean isStrongSliceActive(int tick) {
            return tick < strongSliceUntil;
        }
    }

    private static final class GlitchParticle {
        private final float x;
        private final float y;
        private final float z;
        private final float width;
        private final float height;
        private final int lifetime;
        private final int frameOffset;
        private final int seed;
        private int age;

        private GlitchParticle(float x, float y, float z,
                               float width, float height, int lifetime, int frameOffset, int seed) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.width = width;
            this.height = height;
            this.lifetime = lifetime;
            this.frameOffset = frameOffset;
            this.seed = seed;
        }
    }
}
