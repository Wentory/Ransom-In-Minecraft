package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;

public record WormInfectionPayload(int value) {
    public static final Class<WormInfectionPayload> TYPE = WormInfectionPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(WormInfectionPayload payload, FriendlyByteBuf buffer) {
        buffer.writeVarInt(payload.value());
    }
    public static WormInfectionPayload decode(FriendlyByteBuf buffer) {
        return new WormInfectionPayload(buffer.readVarInt());
    }

}
