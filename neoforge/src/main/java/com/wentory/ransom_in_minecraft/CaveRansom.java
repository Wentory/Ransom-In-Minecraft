package com.wentory.ransom_in_minecraft;

import com.wentory.ransom_in_minecraft.network.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Eight server-authoritative attacks. The cloud is a synchronized visual, not a damageable mob. */
@EventBusSubscriber(modid = RansomInMinecraft.MODID)
public final class CaveRansom {
    private static final int ATTACK_COUNT = 8;
    private static final int REST_TICKS = 30;
    private static final int RETREAT_TICKS = 12;
    private static final double MIN_RETREAT_DISTANCE = 20;
    private static final double MAX_RETREAT_DISTANCE = 30;
    private static final double INITIAL_APPROACH_SPEED = .08;
    private static final double MAX_APPROACH_SPEED = .72;
    private static final double APPROACH_ACCELERATION = .04;
    private static final int FLIGHT_SOUND_INTERVAL_TICKS = 3;
    private static final int SCREAM_LEAD_TICKS = 60;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private CaveRansom() {}
    public static boolean active(ServerPlayer player) { return SESSIONS.containsKey(player.getUUID()); }

    public static void cancelForFullHijack(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return;
        session.outcome = 4;
        stop(player, session);
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1");
        r.playToClient(CaveRansomState.TYPE, CaveRansomState.STREAM_CODEC,
                (p, c) -> c.enqueueWork(p::accept));
        r.playToServer(CaveRansomInput.TYPE, CaveRansomInput.STREAM_CODEC, (p, c) -> c.enqueueWork(() -> {
            if (!(c.player() instanceof ServerPlayer player) || ReplayCompatibility.isReplayServer(player.level().getServer())) return;
            if (p.start()) start(player); else hit(player);
        }));
    }

    private static void start(ServerPlayer player) {
        if (active(player) || !player.isAlive() || player.isSpectator() || FullHijack.active(player)
                || !RansomServerState.isEncounterActive(player) || CrucifixCircleTest.isBanishing(player)) return;
        BlockPos eye = BlockPos.containing(player.getEyePosition());
        if (!CaveRansomEnvironment.isCave(player) || player.isUnderWater()) {
            RansomServerState.finishCrucifixRescue(player); return;
        }
        for (int i = 1; i <= 2; i++) if (player.level().getFluidState(eye.above(i)).is(FluidTags.WATER)) {
            RansomServerState.finishCrucifixRescue(player); return;
        }
        RansomServerState.finishCrucifixRescue(player);
        Session session = new Session(player);
        SESSIONS.put(player.getUUID(), session);
        sync(player, session, true);
    }

    private static void hit(ServerPlayer player) {
        Session s = SESSIONS.get(player.getUUID());
        if (s == null || s.rest > 0 || player.getAttackStrengthScale(0) < .5F) return;
        Vec3 eye = player.getEyePosition(), toCloud = s.position.subtract(eye);
        double reach = player.entityInteractionRange();
        if (toCloud.length() > reach + .6 || toCloud.normalize().dot(player.getLookAngle()) < .88) return;
        player.resetAttackStrengthTicker();
        resolve(player, s, 1);
    }

    private static void resolve(ServerPlayer player, Session s, int outcome) {
        if (CrucifixCircleTest.banishCave(player)) {
            s.outcome = 3;
            stop(player, s);
            return;
        }
        s.attacks++;
        s.outcome = outcome;
        s.serial++;
        s.rest = REST_TICKS;
        if (outcome == 1) {
            Vec3 shield = player.getEyePosition().add(player.getLookAngle().scale(1.1));
            player.level().playSound(null, shield.x, shield.y, shield.z,
                    RansomInMinecraft.CAVE_SHIELD.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1F, 1F);
            player.level().playSound(null, s.position.x, s.position.y, s.position.z,
                    RansomInMinecraft.CAVE_GLITCHING2.get(), net.minecraft.sounds.SoundSource.HOSTILE, 1F, 1F);
        }
        if (outcome == 2) {
            var inventory = player.getInventory();
            var slots = new java.util.ArrayList<Integer>();
            for (int i = 0; i < 9; i++) if (!inventory.getItem(i).isEmpty()) slots.add(i);
            if (slots.isEmpty()) for (int i = 9; i < 36; i++) if (!inventory.getItem(i).isEmpty()) slots.add(i);
            if (!slots.isEmpty()) inventory.setItem(slots.get(player.getRandom().nextInt(slots.size())), net.minecraft.world.item.ItemStack.EMPTY);
            inventory.setChanged();
            player.inventoryMenu.broadcastChanges();
            WormInfection.add(player, 10);
            var damageKey = net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath(
                            RansomInMinecraft.MODID, "cave_ransom"));
            var damageType = player.level().registryAccess()
                    .lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                    .getOrThrow(damageKey);
            player.hurtServer(player.level(), new net.minecraft.world.damagesource.DamageSource(damageType, player.position()), 2.0F);
        }
        if (!active(player)) return;
        s.retreat = s.position.subtract(player.getEyePosition()).normalize();
        if (s.retreat.lengthSqr() < .1) s.retreat = player.getLookAngle();
        float sideAngle = (float) Math.toRadians(10 + player.getRandom().nextDouble() * 10);
        if (player.getRandom().nextBoolean()) sideAngle = -sideAngle;
        s.retreat = s.retreat.yRot(sideAngle);
        s.retreatStart = s.position;
        s.retreatDestination = player.getEyePosition().add(s.retreat.scale(retreatDistance(player)));
        s.retreatAge = 0;
        s.approachSpeed = INITIAL_APPROACH_SPEED;
        sync(player, s, true);
    }

    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || ReplayCompatibility.isReplayServer(player.level().getServer())) return;
        Session s = SESSIONS.get(player.getUUID());
        if (s == null) return;
        if (FullHijack.active(player)) { cancelForFullHijack(player); return; }
        if (!player.isAlive() || player.isSpectator() || !s.dimension.equals(player.level().dimension().identifier())) {
            stop(player, s); return;
        }
        Vec3 previousPosition = s.position;
        if (s.rest > 0) {
            s.rest--;
            if (s.retreatAge < RETREAT_TICKS) {
                double t = ++s.retreatAge / (double) RETREAT_TICKS;
                double ease = 1 - (1 - t) * (1 - t) * (1 - t);
                s.position = s.retreatStart.lerp(s.retreatDestination, ease);
            }
            if (s.rest == 0) {
                if (s.attacks >= ATTACK_COUNT) { stop(player, s); return; }
                if (s.outcome == 1 && player.getRandom().nextInt(3) == 0) {
                    double angle = player.getRandom().nextDouble() * Math.PI * 2;
                    double distance = retreatDistance(player);
                    s.position = player.getEyePosition().add(Math.cos(angle) * distance, .3, Math.sin(angle) * distance);
                    player.level().playSound(null, s.position.x, s.position.y, s.position.z,
                            RansomInMinecraft.CAVE_TELEPORT.get(), net.minecraft.sounds.SoundSource.HOSTILE, 1F, 1F);
                    s.rest = 10;
                    s.outcome = 0;
                }
            }
        } else {
            Vec3 delta = player.getEyePosition().subtract(s.position);
            if (delta.length() <= .65) {
                resolve(player, s, 2);
                if (!active(player)) return;
            }
            else {
                s.approachSpeed = Math.min(MAX_APPROACH_SPEED, s.approachSpeed + APPROACH_ACCELERATION);
                s.position = s.position.add(delta.normalize().scale(Math.min(s.approachSpeed, delta.length())));
            }
        }
        double moved = previousPosition.distanceToSqr(s.position);
        long now = player.level().getGameTime();
        if (moved > .0001 && moved < 64 && now >= s.nextFlightSound) {
            player.level().playSound(null, s.position.x, s.position.y, s.position.z,
                    RansomInMinecraft.CREEPER_TRAIL.get(), net.minecraft.sounds.SoundSource.HOSTILE, 1F, 1F);
            s.nextFlightSound = now + FLIGHT_SOUND_INTERVAL_TICKS;
        }
        sync(player, s, true);
    }

    private static double retreatDistance(ServerPlayer player) {
        return MIN_RETREAT_DISTANCE + player.getRandom().nextDouble() * (MAX_RETREAT_DISTANCE - MIN_RETREAT_DISTANCE);
    }

    private static void sync(ServerPlayer player, Session s, boolean visible) {
        var payload = new CaveRansomState(player.getUUID(), visible,
                s.position.x, s.position.y, s.position.z, s.attacks, s.outcome, s.serial,
                visible && s.rest == 0 && s.position.distanceTo(player.getEyePosition()) <= screamDistance(s.approachSpeed));
        var level = player.level().getServer().getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, s.dimension));
        if (level != null) RansomPackets.sendToPlayersInDimension(level, payload);
        if (level != player.level()) RansomPackets.sendToPlayer(player, payload);
    }
    private static double screamDistance(double speed) {
        double distance = .65;
        for (int tick = 0; tick < SCREAM_LEAD_TICKS; tick++) {
            speed = Math.min(MAX_APPROACH_SPEED, speed + APPROACH_ACCELERATION);
            distance += speed;
        }
        return distance;
    }
    private static void stop(ServerPlayer player, Session s) { sync(player, s, false); SESSIONS.remove(player.getUUID()); }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Session s = SESSIONS.get(player.getUUID()); if (s != null) stop(player, s);
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { SESSIONS.clear(); }
    private static final class Session {
        final net.minecraft.resources.Identifier dimension;
        Vec3 position, retreat, retreatStart, retreatDestination;
        double approachSpeed = INITIAL_APPROACH_SPEED;
        long nextFlightSound;
        int attacks, outcome, serial, retreatAge, rest = REST_TICKS;
        Session(ServerPlayer player) {
            dimension = player.level().dimension().identifier();
            retreat = player.getLookAngle();
            position = player.getEyePosition().add(retreat.scale(3));
            retreatStart = position;
            retreatDestination = player.getEyePosition().add(retreat.scale(retreatDistance(player)));
        }
    }
}
