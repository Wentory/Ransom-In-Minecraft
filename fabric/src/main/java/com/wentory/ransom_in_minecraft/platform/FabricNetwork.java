package com.wentory.ransom_in_minecraft.platform;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public final class FabricNetwork {
    static net.minecraft.server.MinecraftServer server;
    public static void sendToServer(CustomPacketPayload payload) { ClientPlayNetworking.send(payload); }
    public static void sendToPlayer(ServerPlayer player,CustomPacketPayload payload) { ServerPlayNetworking.send(player,payload); }
    public static void sendToAllPlayers(CustomPacketPayload payload) {
        if (server != null) for (ServerPlayer player:server.getPlayerList().getPlayers()) sendToPlayer(player,payload);
    }
    public static void sendToPlayersInDimension(ServerLevel level,CustomPacketPayload payload) {
        for(ServerPlayer player:level.players()) sendToPlayer(player,payload);
    }
    public static void sendToPlayersTrackingEntityAndSelf(Entity entity,CustomPacketPayload payload) {
        for (ServerPlayer player:PlayerLookup.tracking(entity)) sendToPlayer(player,payload);
        if (entity instanceof ServerPlayer player) sendToPlayer(player,payload);
    }
    public static void sendToPlayersNear(ServerLevel level,ServerPlayer excluded,double x,double y,double z,double radius,CustomPacketPayload payload) {
        for (ServerPlayer player:PlayerLookup.around(level,new net.minecraft.world.phys.Vec3(x,y,z),radius))
            if (player != excluded) sendToPlayer(player,payload);
    }
    private FabricNetwork() {}
}
