package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import com.wentory.ransom_in_minecraft.network.ChestInfectionPayload;
import com.wentory.ransom_in_minecraft.network.ChestLostItemPayload;
import com.wentory.ransom_in_minecraft.network.ChestOutcomePayload;
import com.wentory.ransom_in_minecraft.network.ChestQteAdvancePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = RansomInMinecraft.MODID, value = Dist.CLIENT)
public final class RansomChestVisuals {
    private static final Map<String, Set<BlockPos>> INFECTED = new HashMap<>();
    private static final ArrayList<LostEffect> LOST_ITEMS = new ArrayList<>();
    private static final ArrayList<OutcomeEffect> OUTCOMES = new ArrayList<>();
    private static final int LOST_FLIGHT_TICKS = 20;
    private static final int LOST_GLITCH_TICKS = 10;
    private static final int LOST_TOTAL_TICKS = LOST_FLIGHT_TICKS + LOST_GLITCH_TICKS;

    private RansomChestVisuals() {
    }

    public static synchronized void accept(ChestInfectionPayload payload) {
        BlockPos pos = BlockPos.of(payload.blockPos());
        Set<BlockPos> positions = INFECTED.computeIfAbsent(payload.dimension(), ignored -> new HashSet<>());
        if (payload.infected()) positions.add(pos);
        else positions.remove(pos);
    }

    public static synchronized void clear() {
        INFECTED.clear();
        LOST_ITEMS.clear();
        OUTCOMES.clear();
    }

    public static synchronized void showOutcome(ChestOutcomePayload payload) {
        OUTCOMES.add(new OutcomeEffect(payload.dimension(), BlockPos.of(payload.chestPos()),
                payload.outcome(), payload.targetId()));
    }

    public static synchronized void showLostItem(ChestLostItemPayload payload) {
        ResourceLocation id = ResourceLocation.tryParse(payload.itemId());
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return;
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
        if (!stack.isEmpty()) LOST_ITEMS.add(new LostEffect(payload.dimension(),
                BlockPos.of(payload.chestPos()), stack));
    }

    @SubscribeEvent
    public static synchronized void tick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().isPaused()) return;
        for (LostEffect effect : LOST_ITEMS) effect.age++;
        LOST_ITEMS.removeIf(effect -> effect.age >= LOST_TOTAL_TICKS);
        for (OutcomeEffect effect : OUTCOMES) effect.age++;
        OUTCOMES.removeIf(effect -> effect.age >= 32);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        String dimension = minecraft.level.dimension().location().toString();
        Set<BlockPos> positions;
        ArrayList<OutcomeEffect> outcomes;
        synchronized (RansomChestVisuals.class) {
            positions = INFECTED.get(dimension);
            positions = positions == null ? Set.of() : new HashSet<>(positions);
            outcomes = new ArrayList<>(OUTCOMES);
        }

        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        ArrayList<ChestPulse> particlePulses = new ArrayList<>();
        long time = minecraft.level.getGameTime();

        for (Iterator<BlockPos> iterator = positions.iterator(); iterator.hasNext();) {
            BlockPos pos = iterator.next();
            if (!(minecraft.level.getBlockEntity(pos) instanceof ChestBlockEntity chest)) continue;
            if (pos.distToCenterSqr(camera) > 48.0D * 48.0D) continue;
            BlockHitResult sight = minecraft.level.clip(new ClipContext(camera, pos.getCenter(),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player));
            if (sight.getType() == HitResult.Type.BLOCK && !sight.getBlockPos().equals(pos)) continue;
            int seed = hash(pos.getX() * 73471 ^ pos.getY() * 19349663 ^ pos.getZ() * 92821);
            int cycle = 44 + Math.floorMod(seed, 20);
            int pulse = Math.floorMod((int) time + seed, cycle);
            boolean uvPulse = pulse < 2;
            boolean particlePulse = Math.floorMod((int) time + seed * 3, 61) < 13;
            if (uvPulse) renderUvPulse(minecraft, pose, buffers, camera, chest, seed, (int) time, 1.0F, false);
            if (particlePulse) particlePulses.add(new ChestPulse(pos, seed));
        }
        for (OutcomeEffect effect : outcomes) {
            if (!effect.dimension.equals(dimension) || effect.pos.distToCenterSqr(camera) > 48.0D * 48.0D
                    || !(minecraft.level.getBlockEntity(effect.pos) instanceof ChestBlockEntity chest)) continue;
            int seed = hash(effect.pos.hashCode());
            boolean green = effect.outcome == ChestQteAdvancePayload.SAVED_ALL && effect.age >= 6 && effect.age < 9;
            boolean distort = effect.outcome == ChestQteAdvancePayload.SAVED_NONE ? effect.age < 28
                    : effect.outcome == ChestQteAdvancePayload.SAVED_SOME ? effect.age < 24 && effect.age % 2 == 0
                    : effect.age < 5;
            if (green || distort) renderUvPulse(minecraft, pose, buffers, camera, chest, seed,
                    (int) time + effect.age * 37, green ? 0.0F : effect.outcome == ChestQteAdvancePayload.SAVED_NONE ? 4.0F : 2.2F,
                    green);
        }
        if (!particlePulses.isEmpty() || !outcomes.isEmpty()) {
            VertexConsumer particles = buffers.getBuffer(RansomRenderTypes.boundaryFragments());
            for (ChestPulse pulse : particlePulses) {
                BlockPos pos = pulse.pos();
                pose.pushPose();
                pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
                renderParticles(pose, particles, pulse.seed(), (int) time, event.getCamera().rotation());
                pose.popPose();
            }
            for (OutcomeEffect effect : outcomes) {
                if (!effect.dimension.equals(dimension)) continue;
                renderOutcomeParticles(minecraft, pose, particles, camera, effect, event.getCamera().rotation());
            }
            buffers.endBatch(RansomRenderTypes.boundaryFragments());
        }
        renderLostItems(minecraft, event, pose, buffers, camera, dimension);
    }

    private record ChestPulse(BlockPos pos, int seed) {
    }

    private static final class OutcomeEffect {
        private final String dimension;
        private final BlockPos pos;
        private final int outcome;
        private final int targetId;
        private int age;

        private OutcomeEffect(String dimension, BlockPos pos, int outcome, int targetId) {
            this.dimension = dimension;
            this.pos = pos;
            this.outcome = outcome;
            this.targetId = targetId;
        }
    }

    private static void renderUvPulse(Minecraft minecraft, PoseStack pose,
                                      MultiBufferSource.BufferSource buffers, Vec3 camera,
                                      ChestBlockEntity chest, int seed, int tick, float strength, boolean green) {
        float offsetU = (unit(seed + tick * 37) - 0.5F) * 0.025F * strength;
        float offsetV = (unit(seed + tick * 53) - 0.5F) * 0.012F * strength;
        MultiBufferSource shifted = layer -> new ShiftedUvVertexConsumer(buffers.getBuffer(layer), offsetU, offsetV, green);
        BlockPos pos = chest.getBlockPos();
        pose.pushPose();
        pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
        minecraft.getBlockEntityRenderDispatcher().renderItem(chest, pose, shifted,
                LevelRenderer.getLightColor(minecraft.level, pos), OverlayTexture.NO_OVERLAY);
        pose.popPose();
        buffers.endBatch();
    }

    private record ShiftedUvVertexConsumer(VertexConsumer delegate, float offsetU, float offsetV, boolean green)
            implements VertexConsumer {
        @Override public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }
        @Override public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            delegate.setColor(this.green ? 58 : red, this.green ? 255 : green,
                    this.green ? 75 : blue, alpha);
            return this;
        }
        @Override public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u + offsetU, v + offsetV);
            return this;
        }
        @Override public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }
        @Override public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }
        @Override public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }
    }

    private static void renderLostItems(Minecraft minecraft, RenderLevelStageEvent event,
                                        PoseStack pose, MultiBufferSource.BufferSource buffers,
                                        Vec3 camera, String dimension) {
        ArrayList<LostEffect> effects;
        synchronized (RansomChestVisuals.class) {
            effects = new ArrayList<>(LOST_ITEMS);
        }
        for (LostEffect effect : effects) {
            if (!effect.dimension.equals(dimension) || effect.pos.distToCenterSqr(camera) > 48.0D * 48.0D) continue;
            float flight = Math.min(1.0F, effect.age / (float) LOST_FLIGHT_TICKS);
            float easeOut = 1.0F - (1.0F - flight) * (1.0F - flight) * (1.0F - flight);
            int glitchAge = Math.max(0, effect.age - LOST_FLIGHT_TICKS);
            float remaining = glitchAge == 0 ? 1.0F
                    : Math.max(0.0F, (LOST_GLITCH_TICKS - glitchAge) / (float) LOST_GLITCH_TICKS);
            if (remaining <= 0.0F) continue;
            pose.pushPose();
            double jitter = glitchAge > 0 ? (unit(effect.age * 123 + effect.pos.hashCode()) - 0.5F) * 0.16D : 0.0D;
            pose.translate(effect.pos.getX() + 0.5D - camera.x + jitter,
                    effect.pos.getY() + 1.03D + 0.62D * easeOut - camera.y,
                    effect.pos.getZ() + 0.5D - camera.z);
            pose.mulPose(event.getCamera().rotation());
            if (glitchAge == 0 || (glitchAge < 8 && effect.age % 3 != 0)) {
                pose.scale(0.75F * remaining, 0.75F * remaining, 0.75F * remaining);
                minecraft.getItemRenderer().renderStatic(effect.stack, ItemDisplayContext.FIXED,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose, buffers,
                        minecraft.level, effect.pos.hashCode());
            }
            pose.popPose();
            if (glitchAge > 0) {
                pose.pushPose();
                pose.translate(effect.pos.getX() + 0.5D - camera.x,
                        effect.pos.getY() + 1.65D - camera.y, effect.pos.getZ() + 0.5D - camera.z);
                pose.mulPose(event.getCamera().rotation());
                VertexConsumer fragments = buffers.getBuffer(RansomRenderTypes.boundaryFragments());
                for (int i = 0; i < 3 + glitchAge / 2; i++) {
                    int seed = hash(effect.pos.hashCode() + effect.age * 71 + i * 337);
                    float y = (unit(seed) - 0.5F) * 0.38F;
                    float left = (unit(seed + 7) - 0.5F) * 0.40F;
                    float right = left + 0.04F + unit(seed + 13) * 0.11F;
                    addQuad(pose, fragments, left, y, 0.01F, right, y + 0.015F, 0.01F,
                            i % 3 == 0 ? 0 : i % 2 == 0 ? 255 : 230,
                            i % 3 == 0 ? 0 : 20, i % 3 == 0 ? 0 : 20,
                            Math.round(220.0F * remaining));
                }
                pose.popPose();
                buffers.endBatch(RansomRenderTypes.boundaryFragments());
            }
        }
    }

    private static final class LostEffect {
        private final String dimension;
        private final BlockPos pos;
        private final ItemStack stack;
        private int age;

        private LostEffect(String dimension, BlockPos pos, ItemStack stack) {
            this.dimension = dimension;
            this.pos = pos;
            this.stack = stack;
        }
    }

    private static void renderParticles(PoseStack pose, VertexConsumer consumer, int seed, int tick,
                                        Quaternionf cameraRotation) {
        for (int i = 0; i < 4; i++) {
            int particleSeed = hash(seed + i * 104729);
            int age = Math.floorMod(tick + particleSeed, 18);
            float fade = age < 5 ? age / 5.0F : Math.max(0.0F, (18 - age) / 13.0F);
            if (fade <= 0.02F) continue;
            float x = 0.08F + unit(particleSeed + 7) * 0.84F;
            float y = 0.12F + unit(particleSeed + 13) * 0.98F;
            float z = 0.08F + unit(particleSeed + 19) * 0.84F;
            float outside = 0.04F + unit(particleSeed + 31) * 0.10F;
            switch (Math.floorMod(particleSeed, 5)) {
                case 0 -> x = -outside;
                case 1 -> x = 1.0F + outside;
                case 2 -> z = -outside;
                case 3 -> z = 1.0F + outside;
                default -> y = 1.0F + outside;
            }
            float w = 0.018F + unit(particleSeed + 23) * 0.045F;
            float h = 0.022F + unit(particleSeed + 29) * 0.065F;
            boolean black = Math.floorMod(particleSeed, 3) == 0;
            int alpha = Mth.clamp(Math.round(210.0F * fade), 0, 210);
            pose.pushPose();
            pose.translate(x, y, z);
            pose.mulPose(cameraRotation);
            addQuad(pose, consumer, -w, 0, 0, w, h, 0,
                    black ? 0 : 255, black ? 0 : 20, black ? 0 : 16, alpha);
            pose.popPose();
        }
    }

    private static void renderOutcomeParticles(Minecraft minecraft, PoseStack pose, VertexConsumer consumer,
                                               Vec3 camera, OutcomeEffect effect, Quaternionf rotation) {
        int duration = effect.outcome == ChestQteAdvancePayload.SAVED_ALL ? 10
                : effect.outcome == ChestQteAdvancePayload.SAVED_SOME ? 25 : 30;
        if (effect.age >= duration) return;
        int count = effect.outcome == ChestQteAdvancePayload.SAVED_ALL ? 6
                : effect.outcome == ChestQteAdvancePayload.SAVED_SOME ? 13 : 28;
        float fade = Math.min(1.0F, (duration - effect.age) / 7.0F);
        pose.pushPose();
        pose.translate(effect.pos.getX() - camera.x, effect.pos.getY() - camera.y, effect.pos.getZ() - camera.z);
        for (int i = 0; i < count; i++) {
            int seed = hash(effect.pos.hashCode() + i * 19391 + effect.age * 137);
            float x = 0.08F + unit(seed + 3) * 0.84F;
            float y = 0.12F + unit(seed + 5) * 1.0F;
            float z = 0.08F + unit(seed + 7) * 0.84F;
            float outside = 0.06F + unit(seed + 11) * 0.25F;
            switch (Math.floorMod(seed, 5)) {
                case 0 -> x = -outside;
                case 1 -> x = 1.0F + outside;
                case 2 -> z = -outside;
                case 3 -> z = 1.0F + outside;
                default -> y = 1.0F + outside;
            }
            float size = 0.025F + unit(seed + 17) * 0.055F;
            pose.pushPose();
            pose.translate(x, y, z);
            pose.mulPose(rotation);
            addQuad(pose, consumer, -size, -size, 0, size, size, 0,
                    i % 4 == 0 ? 0 : 245, i % 4 == 0 ? 0 : 20, i % 4 == 0 ? 0 : 25,
                    Math.round(220 * fade));
            pose.popPose();
        }
        pose.popPose();
        if (effect.outcome != ChestQteAdvancePayload.SAVED_NONE || effect.targetId < 0
                || effect.age < 8 || effect.age > 22) return;
        Entity target = minecraft.level.getEntity(effect.targetId);
        if (target == null) return;
        float progress = Mth.clamp((effect.age - 8) / 14.0F, 0.0F, 1.0F);
        Vec3 start = effect.pos.getCenter().add(0, 0.55D, 0);
        Vec3 end = target.position().add(0, target.getBbHeight() * 0.65D, 0);
        Vec3 center = start.lerp(end, progress).subtract(camera);
        pose.pushPose();
        pose.translate(center.x, center.y, center.z);
        pose.mulPose(rotation);
        for (int i = 0; i < 14; i++) {
            int seed = hash(effect.pos.hashCode() + effect.age * 451 + i * 73);
            float x = (unit(seed + 3) - 0.5F) * 0.45F;
            float y = (unit(seed + 9) - 0.5F) * 0.45F;
            float w = 0.025F + unit(seed + 15) * 0.10F;
            addQuad(pose, consumer, x, y, 0, x + w, y + 0.025F, 0,
                    i % 3 == 0 ? 0 : 255, 0, 0, 230);
        }
        pose.popPose();
    }

    private static void addQuad(PoseStack pose, VertexConsumer consumer,
                                float minX, float minY, float z, float maxX, float maxY, float z2,
                                int red, int green, int blue, int alpha) {
        consumer.addVertex(pose.last().pose(), minX, minY, z).setColor(red, green, blue, alpha);
        consumer.addVertex(pose.last().pose(), maxX, minY, z2).setColor(red, green, blue, alpha);
        consumer.addVertex(pose.last().pose(), maxX, maxY, z2).setColor(red, green, blue, alpha);
        consumer.addVertex(pose.last().pose(), minX, maxY, z).setColor(red, green, blue, alpha);
    }

    private static int hash(int value) {
        value ^= value >>> 16;
        value *= 0x7feb352d;
        value ^= value >>> 15;
        value *= 0x846ca68b;
        return value ^ value >>> 16;
    }

    private static float unit(int value) {
        return (hash(value) & 0xFFFF) / 65535.0F;
    }
}
