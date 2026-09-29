package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;

public record RansomFailureArmedPayload(int coins, boolean deleteHotbar) {
    public static final Class<RansomFailureArmedPayload> TYPE = RansomFailureArmedPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(RansomFailureArmedPayload payload, FriendlyByteBuf buffer) {
        buffer.writeVarInt(payload.coins());
        buffer.writeBoolean(payload.deleteHotbar());
    }
    public static RansomFailureArmedPayload decode(FriendlyByteBuf buffer) {
        return new RansomFailureArmedPayload(buffer.readVarInt(), buffer.readBoolean());
    }

}
