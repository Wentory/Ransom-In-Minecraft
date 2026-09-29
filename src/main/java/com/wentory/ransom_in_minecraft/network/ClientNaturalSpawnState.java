package com.wentory.ransom_in_minecraft.network;

public final class ClientNaturalSpawnState {
    private static boolean enabled = true;
    private ClientNaturalSpawnState() {}
    public static void accept(NaturalSpawnStatePayload payload) { enabled = payload.enabled(); }
    public static boolean isEnabled() { return enabled; }
    public static void reset() { enabled = true; }
}