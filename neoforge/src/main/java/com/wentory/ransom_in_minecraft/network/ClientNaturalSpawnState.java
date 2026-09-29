package com.wentory.ransom_in_minecraft.network;

public final class ClientNaturalSpawnState {
    private static boolean enabled = true;

    private ClientNaturalSpawnState() {
    }

    public static synchronized void accept(NaturalSpawnStatePayload payload) {
        enabled = payload.enabled();
    }

    public static synchronized boolean isEnabled() {
        return enabled;
    }
}
