package com.wentory.ransom_in_minecraft.network;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class PacketDistributor {
    private PacketDistributor() {}
    public static void sendToServer(Object payload) { RansomNetwork.sendToServer(payload); }
    public static void sendToPlayer(ServerPlayer player, Object payload) { RansomNetwork.sendTo(player, payload); }
    public static void sendToAllPlayers(Object payload) { RansomNetwork.sendToAll(payload); }
    public static void sendToPlayersInDimension(ServerLevel level, Object payload) {
        for (ServerPlayer player : level.players()) RansomNetwork.sendTo(player, payload);
    }
    public static void sendToPlayersTrackingEntityAndSelf(Entity entity, Object payload) {
        if (entity.level() instanceof ServerLevel level) sendToPlayersInDimension(level, payload);
    }
}