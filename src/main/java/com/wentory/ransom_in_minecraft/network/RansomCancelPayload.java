package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;

public record RansomCancelPayload(boolean cancel) {
    public static final Class<RansomCancelPayload> TYPE = RansomCancelPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(RansomCancelPayload payload, FriendlyByteBuf buffer) {
        buffer.writeBoolean(payload.cancel());
    }
    public static RansomCancelPayload decode(FriendlyByteBuf buffer) {
        return new RansomCancelPayload(buffer.readBoolean());
    }

}
