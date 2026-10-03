package com.wentory.ransom_in_minecraft.network;

import net.minecraft.server.MinecraftServer;

/** Optional Flashback integration without loading its client classes on a server. */
public final class ReplayCompatibility {
    private ReplayCompatibility() {}

    public static boolean isReplayServer(MinecraftServer server) {
        if (server == null) return false;
        for (Class<?> type = server.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getName().equals("com.moulberry.flashback.playback.ReplayServer")) return true;
        }
        return false;
    }
}
