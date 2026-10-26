package com.wentory.ransom_in_minecraft;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class RansomAdvancements {
    private RansomAdvancements() {}

    public static void wonRansomware(ServerPlayer player) {
        award(player, "firewall");
    }

    public static void banished(ServerPlayer player) {
        award(player, "stop_right_there");
    }

    private static void award(ServerPlayer player, String path) {
        var advancement = player.level().getServer().getAdvancements().get(ResourceLocation.fromNamespaceAndPath(RansomInMinecraft.MODID, path));
        if (advancement != null) player.getAdvancements().award(advancement, "success");
    }
}
