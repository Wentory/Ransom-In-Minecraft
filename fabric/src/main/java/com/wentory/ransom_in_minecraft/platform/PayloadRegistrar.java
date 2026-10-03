package com.wentory.ransom_in_minecraft.platform;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import java.util.*;
import java.util.function.BiConsumer;

public final class PayloadRegistrar {
    private static final List<Runnable> CLIENT_RECEIVERS=new ArrayList<>();
    public record Context(Player player, java.util.concurrent.Executor executor) {
        public void enqueueWork(Runnable work) { executor.execute(work); }
    }
    public <T extends CustomPacketPayload> void playToServer(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf,T> codec,BiConsumer<T,Context> handler) {
        PayloadTypeRegistry.serverboundPlay().register(type,codec);
        ServerPlayNetworking.registerGlobalReceiver(type,(payload,context) ->
                handler.accept(payload,new Context(context.player(),context.server())));
    }
    public <T extends CustomPacketPayload> void playToClient(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf,T> codec,BiConsumer<T,Context> handler) {
        PayloadTypeRegistry.clientboundPlay().register(type,codec);
        CLIENT_RECEIVERS.add(() -> ClientPayloadReceivers.register(type, handler));
    }
    public static void registerClientReceivers() { CLIENT_RECEIVERS.forEach(Runnable::run); }
}
