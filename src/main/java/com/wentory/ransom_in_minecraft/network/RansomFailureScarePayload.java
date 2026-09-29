package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;

public record RansomFailureScarePayload(int coins, boolean deleteHotbar) {
    public static final Class<RansomFailureScarePayload> TYPE = RansomFailureScarePayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(RansomFailureScarePayload payload, FriendlyByteBuf buffer) {
        buffer.writeVarInt(payload.coins());
        buffer.writeBoolean(payload.deleteHotbar());
    }
    public static RansomFailureScarePayload decode(FriendlyByteBuf buffer) {
        return new RansomFailureScarePayload(buffer.readVarInt(), buffer.readBoolean());
    }

}
