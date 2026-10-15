package com.wentory.ransom_in_minecraft.client;

import com.wentory.ransom_in_minecraft.CrucifixBanishmentTiming;
import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import com.wentory.ransom_in_minecraft.network.CrucifixCirclePayload;
import com.wentory.ransom_in_minecraft.network.MobBanishmentPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Shared tuning and event-driven emitters; particles outlive their emitting animations. */
@EventBusSubscriber(modid = RansomInMinecraft.MODID, value = Dist.CLIENT)
public final class MoonlightEffects {
    static final float PULSE_CHANCE = 0.25F;
    static final float FADE_IN_TICKS = 4;
    // Speeds are blocks/tick; sizes are half-widths in blocks; lifetimes are ticks.
    static final Profile AMBIENT = new Profile(1, 0.09, 0.008, 0.11F, 80, 30, 0.985, 0.004, 0, 24);
    static final double AMBIENT_SOURCE_RADIUS = 2.7;
    static final double AMBIENT_FADE_END_MIN = 2;
    static final double AMBIENT_FADE_END_MAX = 4;
    static final float ROTATION_CHANCE = 0.25F;
    static final float ROTATION_DEGREES_PER_SECOND = 10;
    static final Profile GEYSER = new Profile(8, 0.62, 0.04, 0.12F, 45, 15, 0.95, 0.003, 0.0015, 12);
    static final double GEYSER_FADE_START_MIN = 2;
    static final double GEYSER_FADE_START_MAX = 3;
    static final double GEYSER_FADE_END_MIN = 3;
    static final double GEYSER_FADE_END_MAX = 6;
    static final Profile CHEST = new Profile(52, 0.12, 0.11, 0.11F, 40, 25, 0.88, 0.005, 0, 18);
    static final Profile MOB = new Profile(18, 0.02, 0.025, 0.08F, 28, 18, 0.94, 0.004, 0, 18);
    static final Profile CRUCIFIX = new Profile(12, 0, 0.10, 0.09F, 30, 20, 0.91, 0, 0, 18);
    private static final double CIRCLE_SOURCE_RADIUS = 2.0;
    private static final int AMBIENT_INTERVAL_TICKS = 3;
    private static final int GEYSER_EMISSION_TICKS = 30;
    private static final int GEYSER_START_TICK = CrucifixBanishmentTiming.CAPTURE_END + 20;
    private static final RandomSource RANDOM = RandomSource.create();
    private static final Map<UUID, CrucifixCirclePayload> CIRCLES = new HashMap<>();
    private static final Map<UUID, Long> CHEST_BURSTS = new HashMap<>();
    private static final Map<UUID, Long> CRUCIFIX_BURSTS = new HashMap<>();
    private static final Map<UUID, Long> MOBS = new HashMap<>();
    private static ClientLevel currentLevel;
    private static long lastTick = Long.MIN_VALUE;

    private MoonlightEffects() {}

    record Profile(int count, double upwardSpeed, double outwardSpeed, float size,
                   int lifeMin, int lifeVariation, double drag, double drift, double spreadAcceleration,
                   float fadeOutTicks) {}

    private static ClientLevel level() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != currentLevel) {
            CIRCLES.clear();
            CHEST_BURSTS.clear();
            CRUCIFIX_BURSTS.clear();
            MOBS.clear();
            lastTick = Long.MIN_VALUE;
            currentLevel = level;
        }
        return level;
    }

    static void circle(CrucifixCirclePayload payload) {
        ClientLevel level = level();
        if (level == null || !level.dimension().location().equals(new ResourceLocation(payload.dimension()))) return;
        if (payload.visible()) {
            if (payload.banishment() || payload.chest()) CIRCLES.put(payload.owner(), payload);
        } else {
            CIRCLES.remove(payload.owner());
            CHEST_BURSTS.remove(payload.owner());
            CRUCIFIX_BURSTS.remove(payload.owner());
        }
    }

    static void mob(MobBanishmentPayload payload) {
        if (level() != null) MOBS.putIfAbsent(payload.id(), payload.start());
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = level();
        if (level == null || minecraft.isPaused()) return;
        long now = level.getGameTime();
        if (lastTick == now) return;
        lastTick = now;
        for (var circle : CIRCLES.values()) {
            long age = now - circle.startTick();
            if (circle.chest() && age >= CrucifixBanishmentTiming.MOON_FADE_START
                    && age < CrucifixBanishmentTiming.VISUAL_END
                    && !Long.valueOf(circle.startTick()).equals(CHEST_BURSTS.get(circle.owner()))) {
                // A crossed threshold tolerates late delivery; repeated syncs cannot replay the burst.
                CHEST_BURSTS.put(circle.owner(), circle.startTick());
                chestBurst(level, circle);
            }
            if (!circle.banishment() || age < 0) continue;
            if (age >= CrucifixBanishmentTiming.MOON_FADE_START && age < CrucifixBanishmentTiming.VISUAL_END
                    && !Long.valueOf(circle.startTick()).equals(CRUCIFIX_BURSTS.get(circle.owner()))) {
                CRUCIFIX_BURSTS.put(circle.owner(), circle.startTick());
                crucifixBurst(level, RansomBanishmentRenderer.projectionPosition(circle, age));
            }
            if (age < CrucifixBanishmentTiming.CAPTURE_END) {
                if (age % AMBIENT_INTERVAL_TICKS == 0) circleStream(level, circle, AMBIENT);
            } else if (age >= GEYSER_START_TICK && age < GEYSER_START_TICK + GEYSER_EMISSION_TICKS) {
                circleStream(level, circle, GEYSER);
            }
        }
        MOBS.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > 4) return true;
            net.minecraft.world.entity.Entity entity = null;
            for (var candidate : level.entitiesForRendering()) {
                if (candidate.getUUID().equals(entry.getKey())) {
                    entity = candidate;
                    break;
                }
            }
            if (entity == null) return false;
            Vec3 center = entity.position().add(0, entity.getBbHeight() / 2, 0);
            for (int i = 0; i < MOB.count(); i++) {
                Vec3 offset = new Vec3((RANDOM.nextDouble() - 0.5) * entity.getBbWidth(),
                        (RANDOM.nextDouble() - 0.5) * entity.getBbHeight(),
                        (RANDOM.nextDouble() - 0.5) * entity.getBbWidth());
                Vec3 velocity = offset.normalize().scale(MOB.outwardSpeed() * variation())
                        .add(0, MOB.upwardSpeed() * variation(), 0);
                spawn(level, center.add(offset), velocity, Vec3.ZERO, MOB);
            }
            return true;
        });
    }

    private static void circleStream(ClientLevel level, CrucifixCirclePayload circle, Profile profile) {
        for (int i = 0; i < profile.count(); i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double radius = Math.sqrt(RANDOM.nextDouble())
                    * (profile == AMBIENT ? AMBIENT_SOURCE_RADIUS : CIRCLE_SOURCE_RADIUS);
            Vec3 outward = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            Vec3 position = new Vec3(circle.x(), circle.y() + 0.12, circle.z()).add(outward.scale(radius));
            Vec3 velocity = outward.scale(profile.outwardSpeed() * 0.15 * variation())
                    .add(0, profile.upwardSpeed() * variation(), 0);
            spawn(level, position, velocity, outward, profile);
        }
    }

    private static void chestBurst(ClientLevel level, CrucifixCirclePayload circle) {
        for (int i = 0; i < CHEST.count(); i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            Vec3 outward = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            Vec3 position = new Vec3(circle.x(), circle.y(), circle.z())
                    .add((RANDOM.nextDouble() - 0.5) * 0.65, RANDOM.nextDouble() * 0.45,
                            (RANDOM.nextDouble() - 0.5) * 0.65);
            // Independent launch speeds create a volume, rather than an expanding flat shell.
            Vec3 velocity = outward.scale(CHEST.outwardSpeed() * (0.2 + RANDOM.nextDouble() * 2.0))
                    .add(0, CHEST.upwardSpeed() * (0.25 + RANDOM.nextDouble() * 1.85), 0);
            spawn(level, position, velocity, Vec3.ZERO, CHEST);
        }
    }

    private static double variation() { return 0.8 + RANDOM.nextDouble() * 0.4; }

    private static void crucifixBurst(ClientLevel level, Vec3 center) {
        for (int i = 0; i < CRUCIFIX.count(); i++) {
            double y = RANDOM.nextDouble() * 2 - 1;
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double horizontal = Math.sqrt(1 - y * y);
            Vec3 direction = new Vec3(horizontal * Math.cos(angle), y, horizontal * Math.sin(angle));
            Vec3 position = center.add(direction.scale(0.2 * Math.cbrt(RANDOM.nextDouble())));
            Vec3 velocity = direction.scale(CRUCIFIX.outwardSpeed() * variation());
            spawn(level, position, velocity, Vec3.ZERO, CRUCIFIX);
        }
    }

    private static void spawn(ClientLevel level, Vec3 position, Vec3 velocity, Vec3 outward, Profile profile) {
        Minecraft.getInstance().particleEngine.add(new MoonlightParticle(level, position, velocity, outward, profile));
    }

    static void shield(Vec3 center) {
        ClientLevel level = level();
        if (level == null) return;
        for (int i = 0; i < 8; i++) {
            Vec3 direction = new Vec3(RANDOM.nextDouble() - .5, RANDOM.nextDouble() - .5, RANDOM.nextDouble() - .5).normalize();
            spawn(level, center.add(direction.scale(.35)), direction.scale(.06), Vec3.ZERO, CRUCIFIX);
        }
    }
}
