package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;

public final class PayloadRegistrar {
    private int nextId;

    public <T> void playToClient(Class<T> type, Object unusedCodec, BiConsumer<T, Context> handler) {
        register(type, NetworkDirection.PLAY_TO_CLIENT, handler);
    }

    public <T> void playToServer(Class<T> type, Object unusedCodec, BiConsumer<T, Context> handler) {
        register(type, NetworkDirection.PLAY_TO_SERVER, handler);
    }

    private <T> void register(Class<T> type, NetworkDirection direction, BiConsumer<T, Context> handler) {
        try {
            Method encode = type.getMethod("encode", type, FriendlyByteBuf.class);
            Method decode = type.getMethod("decode", FriendlyByteBuf.class);
            RansomNetwork.CHANNEL.messageBuilder(type, nextId++, direction)
                    .encoder((payload, buffer) -> invokeEncode(encode, payload, buffer))
                    .decoder(buffer -> invokeDecode(type, decode, buffer))
                    .consumerMainThread((payload, supplier) -> {
                        NetworkEvent.Context networkContext = supplier.get();
                        handler.accept(payload, new Context(networkContext));
                        networkContext.setPacketHandled(true);
                    }).add();
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Invalid Ransom payload " + type.getName(), exception);
        }
    }

    private static void invokeEncode(Method method, Object payload, FriendlyByteBuf buffer) {
        try { method.invoke(null, payload, buffer); }
        catch (ReflectiveOperationException exception) { throw new IllegalStateException("Cannot encode Ransom payload", exception); }
    }

    private static <T> T invokeDecode(Class<T> type, Method method, FriendlyByteBuf buffer) {
        try { return type.cast(method.invoke(null, buffer)); }
        catch (ReflectiveOperationException exception) { throw new IllegalStateException("Cannot decode Ransom payload", exception); }
    }

    public record Context(NetworkEvent.Context connection) {
        public Player player() { return connection.getSender(); }
        public void enqueueWork(Runnable work) { connection.enqueueWork(work); }
    }
}