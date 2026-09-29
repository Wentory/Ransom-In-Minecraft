package com.wentory.ransom_in_minecraft.network;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientPlayerGlitchTracker {
    private static final Set<UUID> PENDING_HITS = ConcurrentHashMap.newKeySet();

    private ClientPlayerGlitchTracker() {}

    public static void accept(PlayerGlitchPayload payload) {
        PENDING_HITS.add(payload.playerId());
    }

    public static boolean consumeHit(UUID playerId) {
        return PENDING_HITS.remove(playerId);
    }

    public static boolean hasPendingHit(UUID playerId) {
        return PENDING_HITS.contains(playerId);
    }

    public static void clear() {
        PENDING_HITS.clear();
    }
}
