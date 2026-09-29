package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;

public record RansomSummonPayload(boolean summon) {
    public static final Class<RansomSummonPayload> TYPE = RansomSummonPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(RansomSummonPayload payload, FriendlyByteBuf buffer) {
        buffer.writeBoolean(payload.summon());
    }
    public static RansomSummonPayload decode(FriendlyByteBuf buffer) {
        return new RansomSummonPayload(buffer.readBoolean());
    }

}
