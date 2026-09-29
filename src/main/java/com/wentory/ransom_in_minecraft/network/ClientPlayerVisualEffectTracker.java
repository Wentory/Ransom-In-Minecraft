package com.wentory.ransom_in_minecraft.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientPlayerVisualEffectTracker {
    private static final Map<UUID, Integer> EFFECTS = new ConcurrentHashMap<>();

    private ClientPlayerVisualEffectTracker() {}

    public static void accept(PlayerVisualEffectPayload payload) {
        if (payload.effect() == PlayerVisualEffectPayload.CLEAR) EFFECTS.remove(payload.playerId());
        else EFFECTS.put(payload.playerId(), payload.effect());
    }

    public static int get(UUID playerId) {
        return EFFECTS.getOrDefault(playerId, PlayerVisualEffectPayload.CLEAR);
    }

    public static boolean consumeSuccess(UUID playerId) {
        return EFFECTS.remove(playerId, PlayerVisualEffectPayload.SUCCESS);
    }

    public static void clear() {
        EFFECTS.clear();
    }
}
