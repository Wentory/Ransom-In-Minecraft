package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public final class RansomPackets {
    private RansomPackets() {}

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (!ReplayCompatibility.isReplayServer(player.getServer()) && player.connection.hasChannel(payload)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public static void sendToAllPlayers(CustomPacketPayload payload) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || ReplayCompatibility.isReplayServer(server)) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) sendToPlayer(player, payload);
    }

    public static void sendToPlayersInDimension(ServerLevel level, CustomPacketPayload payload) {
        for (ServerPlayer player : level.players()) sendToPlayer(player, payload);
    }

    public static void sendToPlayersTrackingEntityAndSelf(Entity entity, CustomPacketPayload payload) {
        if (!ReplayCompatibility.isReplayServer(entity.getServer())) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload);
        }
    }
}
