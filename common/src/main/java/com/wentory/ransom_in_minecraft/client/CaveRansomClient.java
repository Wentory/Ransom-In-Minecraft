package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.wentory.ransom_in_minecraft.network.CaveRansomInput;
import com.wentory.ransom_in_minecraft.network.CaveRansomState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Interpolated world cloud and Moonlight feedback; gameplay outcomes arrive from the server. */
public final class CaveRansomClient {
    private static final Identifier FACE = texture("jumpscare2.png");
    private static final Identifier CIRCLE = texture("item/crucifix/circle_1.png");
    private static final Identifier MOON = texture("item/crucifix/moon.png");
    private static final Identifier[] STARS = {
            texture("item/crucifix/moonlight_star_tiny.png"), texture("item/crucifix/moonlight_star_small.png"),
            texture("item/crucifix/moonlight_star_normal.png"), texture("item/crucifix/moonlight_star_big.png")};
    private static final Map<UUID, Visual> VISUALS = new HashMap<>();
    private static final java.util.ArrayDeque<Trail> TRAIL = new java.util.ArrayDeque<>();
    private static final int MAX_TRAIL_POINTS = 1024;
    private static final float END_GROW_TICKS = 8;
    private static final float END_SHRINK_TICKS = 16;
    private static final float END_TICKS = END_GROW_TICKS + END_SHRINK_TICKS;
    private static final float END_PEAK_SCALE = 1.8F;
    private record Trail(UUID owner, Vec3 position, long born, int lifetime, int seed) {}
    private static Object currentLevel;
    private static net.minecraft.client.resources.sounds.SoundInstance introSound, introTail;
    private static net.minecraft.client.resources.sounds.SoundInstance failureSound;
    private static final long FAILURE_SCARE_NANOS = 400_000_000L;
    private static final long MOONLIGHT_FLASH_NANOS = 250_000_000L;
    private static long failureStarted;
    private static long shakeStarted;
    private static long flashStarted;
    private static final float SHAKE_SECONDS = .65F;
    private CaveRansomClient() {}

    public static void clear() {
        stopIntro();
        if (introTail != null) Minecraft.getInstance().getSoundManager().stop(introTail);
        introTail = null;
        if (failureSound != null) Minecraft.getInstance().getSoundManager().stop(failureSound);
        failureSound = null;
        failureStarted = 0;
        shakeStarted = 0;
        flashStarted = 0;
        for (Visual visual : VISUALS.values()) stopScream(visual);
        VISUALS.clear(); TRAIL.clear(); currentLevel = Minecraft.getInstance().level;
    }
    static void startIntro() {
        stopIntro();
        introSound = net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(sound("jumpscare_fail"), 1F, 1F);
        Minecraft.getInstance().getSoundManager().play(introSound);
    }
    static void stopIntro() {
        if (introSound != null) Minecraft.getInstance().getSoundManager().stop(introSound);
        introSound = null;
    }
    static void keepIntroTail() { introTail = introSound; introSound = null; }
    static boolean active() {
        var mc = Minecraft.getInstance();
        Visual v = mc.player == null ? null : VISUALS.get(mc.player.getUUID());
        return v != null && v.state.active();
    }
    private static void accept(CaveRansomState p) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (currentLevel != mc.level) clear();
        float now = mc.level.getGameTime();
        Visual v = VISUALS.get(p.owner());
        if (!p.active() && (p.outcome() == 3 || p.outcome() == 4)) {
            if (v != null) stopScream(v);
            VISUALS.remove(p.owner());
            TRAIL.removeIf(trail -> trail.owner().equals(p.owner()));
            if (p.outcome() == 4 && mc.player != null && mc.player.getUUID().equals(p.owner())) {
                stopIntro();
                if (introTail != null) mc.getSoundManager().stop(introTail);
                if (failureSound != null) mc.getSoundManager().stop(failureSound);
                introTail = failureSound = null;
                failureStarted = shakeStarted = flashStarted = 0;
            }
            return;
        }
        if (v == null) {
            if (!p.active()) return;
            v = new Visual(p, now);
            VISUALS.put(p.owner(), v);
            shield(v, now);
            if (p.attacks() == 0 && p.outcome() == 0) moonlightImpact(p.owner(), 0, true);
            if (p.outcome() == 2) startFailure(p.owner());
        } else {
            v.previous = v.position(now);
            if (p.serial() != v.state.serial()) {
                v.reactionAt = now;
                shield(v, now);
                if (p.outcome() == 2) startFailure(p.owner());
                if (p.outcome() == 1) moonlightImpact(p.owner(), 0, false);
            }
            v.state = p;
            v.updated = now;
        }
        if (!p.active()) v.finished = now;
        if (!p.screaming() || !p.active()) {
            stopScream(v);
            v.screamTriggered = false;
        } else if (!v.screamTriggered) {
            v.screamTriggered = true;
            v.scream = new ApproachScream(v);
            mc.getSoundManager().play(v.scream);
        }
    }

    private static void stopScream(Visual visual) {
        if (visual.scream != null) Minecraft.getInstance().getSoundManager().stop(visual.scream);
        visual.scream = null;
    }
    private static void shield(Visual v, float now) {
        var mc = Minecraft.getInstance();
        for (var entity : mc.level.entitiesForRendering()) {
            if (!entity.getUUID().equals(v.state.owner())) continue;
            v.shield = entity.getEyePosition().add(entity.getLookAngle().scale(1.1));
            v.shieldAt = now;
            MoonlightEffects.shield(v.shield);
            break;
        }
    }

    private static void startFailure(UUID owner) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.getUUID().equals(owner)) return;
        if (failureSound != null) mc.getSoundManager().stop(failureSound);
        failureSound = net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                sound("jumpscare2_fail"), 1F, 1F);
        mc.getSoundManager().play(failureSound);
        failureStarted = System.nanoTime();
        shakeStarted = failureStarted + FAILURE_SCARE_NANOS;
        flashStarted = shakeStarted;
    }

    private static void moonlightImpact(UUID owner, long delay, boolean flash) {
        var player = Minecraft.getInstance().player;
        if (player == null || !player.getUUID().equals(owner)) return;
        shakeStarted = System.nanoTime() + delay;
        if (flash) flashStarted = shakeStarted;
    }
    public static void tick() {
        var mc = Minecraft.getInstance();
        if (currentLevel != mc.level) clear();
        if (mc.level == null || mc.isPaused()) return;
        VISUALS.values().removeIf(v -> !v.state.active() && mc.level.getGameTime() - v.finished > END_TICKS);
        long now = mc.level.getGameTime();
        TRAIL.removeIf(point -> now - point.born() >= point.lifetime());
        for (Visual visual : VISUALS.values()) {
            Vec3 position = visual.position(now);
            Vec3 previous = visual.trailPosition;
            visual.trailPosition = position;
            if (!visual.state.active() || previous == null) continue;
            double distance = previous.distanceTo(position);
            // A teleport leaves particles at its ends, never a line through the intervening space.
            if (distance < .02 || distance > 8) continue;
            int samples = Math.min(10, Math.max(1, (int) Math.ceil(distance / .6)));
            for (int i = 0; i < samples; i++) {
                int seed = visual.state.owner().hashCode() ^ (int) now * 971 ^ i * 137;
                TRAIL.addLast(new Trail(visual.state.owner(), previous.lerp(position, (i + .5) / samples),
                        now, 12 + Math.floorMod(seed, 9), seed));
            }
        }
        while (TRAIL.size() > MAX_TRAIL_POINTS) TRAIL.removeFirst();
    }
    public static boolean attack() {
        if(!active()) return false;
        CaveRansomSender.send(new CaveRansomInput(false));
        var mc=Minecraft.getInstance();
        var visual=VISUALS.get(mc.player.getUUID());
        var delta=visual.position(mc.level.getGameTime()).subtract(mc.player.getEyePosition());
        return delta.length()<=mc.player.entityInteractionRange()+.6 && delta.normalize().dot(mc.player.getLookAngle())>=.88;
    }
    public static float[] shake() {
        float age=(System.nanoTime()-shakeStarted)/1_000_000_000F;
        if(shakeStarted==0 || age<0 || age>=SHAKE_SECONDS) return new float[3];
        float fade=1-Math.max(0,age-.06F)/(SHAKE_SECONDS-.06F);fade*=fade;
        return new float[]{(float)Math.cos(age*54F)*4F*fade,-(float)Math.cos(age*68F)*3.2F*fade,(float)Math.sin(age*58F)*1.6F*fade};
    }
    public static void render(PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector collector, net.minecraft.client.renderer.state.level.LevelRenderState frame) {
        var mc = Minecraft.getInstance();
        if(mc.level==null || mc.player==null) return;
        float now = mc.level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 camera = frame.cameraRenderState.pos;
        var buffers = new DeferredBuffers(collector);
        for (var v : VISUALS.values()) {
            Vec3 position = v.position(now);
            boolean owner = mc.player.getUUID().equals(v.state.owner());
            float endingAge = now - v.finished;
            float fade = v.state.active() ? 1 : Math.clamp((END_TICKS - endingAge) / 4F, 0F, 1F);
            float scale = v.state.active() ? 1 : endingScale(endingAge);
            var type = RansomRenderTypes.caveCloud(owner);
            pose.pushPose();
            pose.translate(position.x - camera.x, position.y - camera.y, position.z - camera.z);
            pose.scale(scale, scale, scale);
            GlitchParticles.render(pose, buffers, v.state.owner().hashCode(), now,
                    420, .8F, .9F, .65F, 1.3F, Math.round(230 * fade), false, type);
            buffers.endBatch(type);
            if (owner && position.distanceTo(mc.player.getEyePosition()) < 2.8) {
                pose.mulPose(frame.cameraRenderState.orientation);
                var faceType = RansomRenderTypes.caveSprite(FACE, true);
                float faceAlpha = (float) Math.clamp((2.8 - position.distanceTo(mc.player.getEyePosition())) / 1.5, 0, 1);
                quad(pose, buffers.getBuffer(faceType), .7F, .9F, 0, 0, 1, 1, 255, 255, 255, Math.round(255 * faceAlpha * fade));
                buffers.endBatch(faceType);
            }
            pose.popPose();
            float age = now - v.shieldAt;
            if (v.shield != null && age >= 0 && age < 12) {
                float t = age / 12, ease = 1 - (1 - t) * (1 - t) * (1 - t);
                pose.pushPose();
                pose.translate(v.shield.x - camera.x, v.shield.y - camera.y, v.shield.z - camera.z);
                pose.mulPose(frame.cameraRenderState.orientation);
                pose.mulPose(Axis.ZP.rotationDegrees(ease * 65));
                float half = .15F + ease * .7F;
                shieldLayer(pose, buffers, CIRCLE, half, t);
                shieldLayer(pose, buffers, MOON, half, t);
                pose.popPose();
            }
        }
        for (Trail trail : TRAIL) {
            float age = now - trail.born();
            if (age < 0 || age >= trail.lifetime()) continue;
            float fade = 1 - age / trail.lifetime();
            var type = RansomRenderTypes.caveCloud(mc.player.getUUID().equals(trail.owner()));
            pose.pushPose();
            pose.translate(trail.position().x - camera.x, trail.position().y - camera.y,
                    trail.position().z - camera.z);
            GlitchParticles.render(pose, buffers, trail.seed(), age,
                    9, .32F, .4F, .3F, .9F, Math.round(190 * fade * fade), true, type);
            pose.popPose();
        }
        buffers.endBatch(RansomRenderTypes.caveCloud(true));
        buffers.endBatch(RansomRenderTypes.caveCloud(false));
    }
    private static float endingScale(float ticks) {
        if (ticks < END_GROW_TICKS) {
            float t = Math.clamp(ticks / END_GROW_TICKS, 0F, 1F);
            float ease = t * t * (3 - 2 * t);
            return 1 + (END_PEAK_SCALE - 1) * ease;
        }
        float t = Math.clamp((ticks - END_GROW_TICKS) / END_SHRINK_TICKS, 0F, 1F);
        return END_PEAK_SCALE * (1 - t * t * (3 - 2 * t));
    }

    public static void hud(GuiGraphicsExtractor g) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gui.screen() != null) return;
        Visual v = VISUALS.get(mc.player.getUUID());
        if (v == null) return;
        long failureAge = failureStarted == 0 ? Long.MAX_VALUE : System.nanoTime() - failureStarted;
        if (failureAge < FAILURE_SCARE_NANOS) {
            RansomEncounter.renderCaveFailure(g);
            return;
        }
        if (v.state.active()) g.centeredText(mc.font,
                net.minecraft.network.chat.Component.translatable("ransom.cave.hit"), g.guiWidth() / 2, g.guiHeight() - 63, 0xFF93EFFF);
        long flashAge = flashStarted == 0 ? Long.MAX_VALUE : System.nanoTime() - flashStarted;
        if (flashAge >= 0 && flashAge < MOONLIGHT_FLASH_NANOS) {
            float remaining = 1 - flashAge / (float) MOONLIGHT_FLASH_NANOS;
            int alpha = Math.round(220 * remaining * remaining);
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), alpha << 24 | 0x93EFFF);
        }
    }
    static void downloadStars(GuiGraphicsExtractor g, int ticks) {
        for (int i = 0; i < 24; i++) {
            int phase = Math.floorMod(ticks + i * 7, 24);
            float alpha = (float) Math.sin(phase / 24F * Math.PI);
            GuiCompat.setColor(g,147/255F,239/255F,1,alpha);
            int x = Math.floorMod(i * 137 + 29, g.guiWidth());
            int y = Math.floorMod(i * 83 + 17 - ticks * 2, g.guiHeight());
            GuiCompat.blit(g,STARS[i % 4], x, y, 9 + i % 3 * 3, 9 + i % 3 * 3, 0, 0, 16, 16, 16, 16);
        }
        GuiCompat.setColor(g,1,1,1,1);
    }
    private static void quad(PoseStack pose, VertexConsumer vertices, float x, float y,
                             float u0, float v0, float u1, float v1, int r, int g, int b, int a) {
        vertex(pose, vertices, -x, -y, u0, v1, r, g, b, a);
        vertex(pose, vertices, x, -y, u1, v1, r, g, b, a);
        vertex(pose, vertices, x, y, u1, v0, r, g, b, a);
        vertex(pose, vertices, -x, y, u0, v0, r, g, b, a);
    }
    private static void shieldLayer(PoseStack pose, DeferredBuffers buffers,
                                    Identifier texture, float half, float fade) {
        var type = RansomRenderTypes.caveSprite(texture, false);
        quad(pose, buffers.getBuffer(type), half, half, 0, 0, 1, 1, 147, 239, 255, Math.round(255 * (1 - fade)));
        buffers.endBatch(type);
    }
    private static void vertex(PoseStack p, VertexConsumer v, float x, float y, float u, float w, int r, int g, int b, int a) {
        v.addVertex(p.last().pose(), x, y, 0).setUv(u, w).setColor(r, g, b, a).setLight(0xF000F0).setOverlay(0).setNormal(0,0,1);
    }
    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath("ransom_in_minecraft", "textures/" + name);
    }
    private static final class Visual {
        ApproachScream scream;
        boolean screamTriggered;
        CaveRansomState state;
        Vec3 previous, shield, trailPosition;
        float updated, reactionAt = -100, shieldAt, finished, started;
        Visual(CaveRansomState p, float now) { state = p; previous = new Vec3(p.x(), p.y(), p.z()); updated = started = now; }
        Vec3 position(float now) {
            return previous.lerp(new Vec3(state.x(), state.y(), state.z()), Math.clamp(now - updated, 0, 1));
        }
    }

    private static final class ApproachScream extends net.minecraft.client.resources.sounds.AbstractTickableSoundInstance {
        private final Visual visual;
        ApproachScream(Visual visual) {
            super(sound("cave_screaming"), net.minecraft.sounds.SoundSource.HOSTILE,
                    net.minecraft.util.RandomSource.create());
            this.visual = visual;
            volume = 1;
            pitch = 1;
            looping = false;
            relative = false;
            attenuation = net.minecraft.client.resources.sounds.SoundInstance.Attenuation.LINEAR;
            updatePosition();
        }
        private void updatePosition() {
            var level = Minecraft.getInstance().level;
            if (level == null) return;
            Vec3 position = visual.position(level.getGameTime());
            x = position.x; y = position.y; z = position.z;
        }
        @Override public void tick() {
            if (!visual.state.active() || !visual.state.screaming() || visual.scream != this
                    || Minecraft.getInstance().level == null) { stop(); return; }
            updatePosition();
        }
    }
        public static void initialize(){CaveRansomState.bind(CaveRansomClient::accept);}
    private static net.minecraft.sounds.SoundEvent sound(String id) {
        return net.minecraft.sounds.SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("ransom_in_minecraft",id));
    }
}
