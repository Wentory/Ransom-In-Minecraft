package com.wentory.ransom_in_minecraft.platform;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.function.BiConsumer;
final class ClientPayloadReceivers {
    static <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type, BiConsumer<T, PayloadRegistrar.Context> handler) {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(type,
            (payload, context) -> handler.accept(payload, new PayloadRegistrar.Context(context.player(), context.client())));
    }
}
