package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;

public record PlayerVisualEffectRequestPayload(int effect) {
    public static final Class<PlayerVisualEffectRequestPayload> TYPE = PlayerVisualEffectRequestPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(PlayerVisualEffectRequestPayload payload, FriendlyByteBuf buffer) {
        buffer.writeVarInt(payload.effect());
    }
    public static PlayerVisualEffectRequestPayload decode(FriendlyByteBuf buffer) {
        return new PlayerVisualEffectRequestPayload(buffer.readVarInt());
    }

}
