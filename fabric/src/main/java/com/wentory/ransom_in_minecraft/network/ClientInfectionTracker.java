package com.wentory.ransom_in_minecraft.network;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Clientbound infection state. Contains no renderer classes, so payload registration is server-safe. */
public final class ClientInfectionTracker {
    private static final Set<UUID> INFECTED = ConcurrentHashMap.newKeySet();

    private ClientInfectionTracker() {}

    public static void accept(InfectionStatusPayload payload) {
        if (payload.infected()) INFECTED.add(payload.playerId());
        else INFECTED.remove(payload.playerId());
    }

    public static boolean isInfected(UUID playerId) {
        return INFECTED.contains(playerId);
    }

    public static void clear() {
        INFECTED.clear();
    }
}
