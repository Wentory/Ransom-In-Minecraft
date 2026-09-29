package com.wentory.ransom_in_minecraft.network;

import java.util.Set;
import java.util.Map;
import net.minecraft.client.Minecraft;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientInfectedZombies {
    private static final Set<UUID> INFECTED = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> WORM_VICTIMS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> STOP_ENDS = new ConcurrentHashMap<>();

    public static void accept(ZombieStopPayload payload) {
        var level = Minecraft.getInstance().level;
        if (level != null) STOP_ENDS.put(payload.zombieId(), level.getGameTime() + payload.ticks());
    }

    public static float stopRemaining(UUID id, float partialTick) {
        var level = Minecraft.getInstance().level;
        Long end = STOP_ENDS.get(id);
        if (level == null || end == null) return 0;
        float remaining = end - level.getGameTime() - partialTick;
        if (remaining <= 0) STOP_ENDS.remove(id);
        return Math.max(0, remaining);
    }

    private ClientInfectedZombies() {}

    public static void accept(InfectedZombiePayload payload) {
        INFECTED.add(payload.zombieId());
    }

    public static boolean contains(UUID zombieId) {
        return INFECTED.contains(zombieId);
    }

    public static void accept(WormVictimPayload payload) {
        if (payload.active()) WORM_VICTIMS.add(payload.zombieId());
        else WORM_VICTIMS.remove(payload.zombieId());
    }

    public static boolean isWormVictim(UUID zombieId) {
        return WORM_VICTIMS.contains(zombieId);
    }

    public static void remove(UUID zombieId) {
        INFECTED.remove(zombieId);
        WORM_VICTIMS.remove(zombieId);
        STOP_ENDS.remove(zombieId);
    }

    public static void clear() {
        INFECTED.clear();
        WORM_VICTIMS.clear();
        STOP_ENDS.clear();
    }
}
