package com.wentory.ransom_in_minecraft.network;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Common-safe storage; populated only by clientbound visual messages. */
public final class ClientFullHijackTracker {
    private static final Set<UUID> PLAYERS = new HashSet<>();
    private ClientFullHijackTracker() {}
    public static void accept(FullHijackVisualPayload payload) {
        if (payload.hijacked()) PLAYERS.add(payload.playerId());
        else PLAYERS.remove(payload.playerId());
    }
    public static boolean contains(UUID playerId) { return PLAYERS.contains(playerId); }
    public static void remove(UUID playerId) { PLAYERS.remove(playerId); }
    public static void clear() { PLAYERS.clear(); }
}
