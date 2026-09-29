package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;

public record RansomProgressPayload(int coins, int targetCoins, int phaseTicks) {
    public static final Class<RansomProgressPayload> TYPE = RansomProgressPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(RansomProgressPayload payload, FriendlyByteBuf buffer) {
        buffer.writeVarInt(payload.coins());
        buffer.writeVarInt(payload.targetCoins());
        buffer.writeVarInt(payload.phaseTicks());
    }
    public static RansomProgressPayload decode(FriendlyByteBuf buffer) {
        return new RansomProgressPayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

}
